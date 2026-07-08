package tw.com.ispan.backend.orderMerch.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderMerch.dto.OrderCreateRequestDTO;
import tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;
import tw.com.ispan.backend.payment.ecpay.EcpayPaymentService;
import tw.com.ispan.backend.products.repository.MerchCartRepository;

/**
 * 商品訂單外觀服務 (Facade Pattern)。
 * 
 * <p>
 * 作為前台與後台的統一門戶，內部將具體業務邏輯路由至各自專責的子業務服務。
 * 這種設計優化了代碼的可讀性與可維護性，同時免除修改控制器及其他調用端代碼。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MerchOrderService {

    private final MerchOrderCreateService merchOrderCreateService;
    private final MerchOrderQueryService merchOrderQueryService;
    private final MerchRefundService merchRefundService;
    private final MerchRevenueService merchRevenueService;

    private final MerchOrderRepository merchOrderRepository;
    private final MerchCartRepository merchCartRepository;
    private final EcpayPaymentService ecpayPaymentService;

    /**
     * 建立商品訂單功能
     */
    public OrderResponseDTO createOrder(OrderCreateRequestDTO requestDTO, String userId) {
        return merchOrderCreateService.createOrder(requestDTO, userId);
    }

    /**
     * 售後服務：退貨退款 (回補庫存與扣減總金額)
     */
    public String refundMerch(String mOrderId, String mDetailId) {
        return merchRefundService.refundMerch(mOrderId, mDetailId);
    }

    /**
     * 計算非退貨商品的總營業額
     */
    public BigDecimal calculateTotalRevenue() {
        return merchRevenueService.calculateTotalRevenue();
    }

    /**
     * 從資料庫取出所有 merch_orders 及其對應的明細
     */
    public List<Map<String, Object>> getAllMerches() {
        return merchOrderQueryService.getAllMerches();
    }

    /**
     * 根據綠界交易號查詢資料庫對應的商品訂單 ID (mOrderId)，用於 Redirect 跳轉前端。
     */
    public String getMOrderIdByMerchantTradeNo(String merchantTradeNo) {
        return merchOrderQueryService.getMOrderIdByMerchantTradeNo(merchantTradeNo);
    }

    /**
     * 根據訂單編號，查詢單筆商品訂單詳情。
     */
    public OrderResponseDTO getMerchOrderById(String mOrderId) {
        return merchOrderQueryService.getMerchOrderById(mOrderId);
    }

    /**
     * 處理綠界科技商品付款狀態的 Server-to-Server Webhook 回調。
     */
    @Transactional
    public String handleEcpayCallback(Map<String, String> params) {
        log.info("【綠界商品回調】收到付款通知參數: {}", params);

        // 1. 驗證 CheckMacValue 簽章安全性
        if (!ecpayPaymentService.verifyCheckMacValue(params)) {
            log.warn("【綠界商品回調】簽章驗證失敗！");
            return "0|FAIL";
        }

        String merchantTradeNo = params.get("MerchantTradeNo");
        String rtnCode = params.get("RtnCode"); // RtnCode "1" 代表付款成功

        // 必須以 merchant_trade_no 查詢
        MerchOrderBean order = merchOrderRepository.findByMerchantTradeNo(merchantTradeNo).orElse(null);
        if (order == null) {
            log.warn("【綠界商品回調】找不到對應商品訂單（交易單號: {}）", merchantTradeNo);
            return "0|FAIL";
        }

        if ("1".equals(rtnCode)) {
            // 付款成功
            if ("UNPAID".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("PAID");
                order.setPaidAt(LocalDateTime.now());
                if (order.getMerchOrderDetails() != null) {
                    for (MerchOrderDetailBean d : order.getMerchOrderDetails()) {
                        d.setItemStatus(MerchOrderEnum.NORMAL);
                    }
                }
                merchOrderRepository.save(order);

                // 清空此會員購物車
                merchCartRepository.deleteAllByLockBy(order.getUserId().getUserId());
                log.info("【綠界商品回調】商品訂單 {} 已付款，並清空購物車", order.getMOrderId());
            }
            return "1|OK";
        } else {
            // 付款失敗
            log.warn("【綠界商品回調】付款失敗，RtnCode: {}, RtnMsg: {}", rtnCode, params.get("RtnMsg"));
            order.setPaymentStatus("FAILED");
            order.cancelDetails();
            merchOrderRepository.save(order);
            return "1|OK"; // 即使付款失敗也回傳 1|OK 給綠界避免其重發
        }
    }

    /**
     * 當使用者從綠界頁面主動取消付款時，執行此釋放與標記邏輯。
     */
    @Transactional
    public void handleCancelOrder(String mOrderId) {
        log.info("【綠界取消】使用者主動取消商品訂單，訂單編號: {}", mOrderId);
        merchOrderRepository.findById(mOrderId).ifPresent(order -> {
            order.setPaymentStatus("FAILED");
            order.cancelDetails();
            merchOrderRepository.save(order);
        });
    }
}
