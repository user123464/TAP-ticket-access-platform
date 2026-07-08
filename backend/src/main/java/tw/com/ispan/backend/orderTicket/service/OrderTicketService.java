package tw.com.ispan.backend.orderTicket.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.dto.OrderCreateRequestDTO;
import tw.com.ispan.backend.orderTicket.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;
import tw.com.ispan.backend.payment.ecpay.EcpayPaymentService;

/**
 * 門票訂單外觀服務 (Facade Pattern)。
 * 
 * <p>
 * 作為前台與後台的統一門戶，內部不包含具體業務邏輯，
 * 而是將調用路由至各自專責的子業務服務。這種設計既優化了代碼的可讀性，
 * 又保證了現有系統串接的完整相容性，免除修改控制器及其他調用端代碼。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderTicketService {

    private final TicketOrderCreateService ticketOrderCreateService;
    private final TicketCheckInService ticketCheckInService;
    private final TicketRefundService ticketRefundService;
    private final TicketRevenueService ticketRevenueService;
    private final TicketOrderQueryService ticketOrderQueryService;
    private final OrderCleanupService orderCleanupService;
    private final EcpayPaymentService ecpayPaymentService;
    private final TicketOrderRepository ticketOrderRepository;

    /**
     * 窗口 1：建立購票訂單。
     *
     * @param requestDTO 購票請求資料
     * @param userId     用戶 ID
     * @return 訂單結果 DTO
     */
    public OrderResponseDTO createOrder(OrderCreateRequestDTO requestDTO, String userId) {
        return ticketOrderCreateService.createOrder(requestDTO, userId);
    }

    /**
     * 窗口 2：現場驗票核銷。
     *
     * @param qrCodeHash QR Code 的雜湊值
     * @return 核銷結果狀態訊息
     */
    public String checkInTicket(String qrCodeHash) {
        return ticketCheckInService.checkInTicket(qrCodeHash);
    }

    /**
     * 窗口 3：售後退票。
     *
     * @param tOrderId  訂單編號
     * @param tDetailId 門票明細編號
     * @return 退票結果狀態訊息
     */
    public String refundTicket(String tOrderId, String tDetailId) {
        return ticketRefundService.refundTicket(tOrderId, tDetailId);
    }

    /**
     * 窗口 4：基本帳務系統 - 計算全站票務總營業額 (排除退票)。
     *
     * @return 總營業額
     */
    public BigDecimal calculateTotalRevenue() {
        return ticketRevenueService.calculateTotalRevenue();
    }

    /**
     * 窗口 5：獲取全站門票訂單與明細。
     *
     * @return 門票訂單嵌套結構的列表
     */
    public List<OrderResponseDTO> getAllTickets() {
        return ticketOrderQueryService.getAllTickets();
    }

    /**
     * 根據訂單編號，查詢單筆票券訂單詳情。
     *
     * @param tOrderId 訂單編號
     * @return 門票訂單 DTO，若不存在則為 null
     */
    public OrderResponseDTO getTicketOrderById(String tOrderId) {
        return ticketOrderQueryService.getTicketOrderById(tOrderId);
    }

    /**
     * 窗口 6：自動釋出逾時未付款的訂單與座位 (排程清理調用)。
     */
    public void releaseExpiredOrders() {
        orderCleanupService.releaseExpiredOrders();
    }

    /**
     * 窗口 7：處理綠界科技的 Server-to-Server 成功付款回調。
     *
     * @param params 綠界回傳的 POST 參數
     * @return 傳回給綠界的響應字串（成功為 "1|OK"，失敗為 "0|FAIL"）
     */
    @org.springframework.transaction.annotation.Transactional
    public String handleEcpayCallback(Map<String, String> params) {
        log.info("【綠界回調】收到付款通知參數: {}", params);

        // 1. 驗證 CheckMacValue 簽章安全性
        if (!ecpayPaymentService.verifyCheckMacValue(params)) {
            log.warn("【綠界回調】簽章驗證失敗！");
            return "0|FAIL";
        }

        String merchantTradeNo = params.get("MerchantTradeNo");
        String rtnCode = params.get("RtnCode"); // RtnCode "1" 代表付款成功

        // 必須以 merchant_trade_no 查詢，因為 t_order_id 是資料庫主鍵，與綠界交易號不同
        TicketOrdersBean order = ticketOrderRepository.findByMerchantTradeNo(merchantTradeNo).orElse(null);
        if (order == null) {
            log.warn("【綠界回調】找不到對應訂單（交易單號: {}）", merchantTradeNo);
            return "0|FAIL";
        }

        if ("1".equals(rtnCode)) {
            // 付款成功
            if ("UNPAID".equals(order.getPaymentStatus())) {
                order.setPaymentStatus("PAID");
                if (order.getOrderDetail() != null) {
                    for (TicketOrderDetailBean d : order.getOrderDetail()) {
                        d.setItemStatus(TicketOrderStatus.NORMAL);
                    }
                }
                ticketOrderRepository.save(order);
                log.info("【綠界回調】訂單 {} 付款成功，狀態已修改為 PAID，且明細狀態已更新為 NORMAL", order.getTOrderId());
            }
            return "1|OK";
        } else {
            // 付款失敗
            log.warn("【綠界回調】付款失敗，RtnCode: {}, RtnMsg: {}", rtnCode, params.get("RtnMsg"));
            // 釋放座位並改為 FAILED (傳入正確的資料庫主鍵 tOrderId)
            orderCleanupService.releaseOrder(order.getTOrderId(), "FAILED");
            return "1|OK"; // 即使付款失敗也回傳 1|OK 給綠界避免其重發，但我們內部狀態已設為失敗
        }
    }

    /**
     * 根據綠界交易號查詢資料庫對應的訂單 ID (tOrderId)，用於 Redirect 跳轉前端。
     */
    public String getTOrderIdByMerchantTradeNo(String merchantTradeNo) {
        return ticketOrderRepository.findByMerchantTradeNo(merchantTradeNo)
                .map(o -> o.getTOrderId())
                .orElse(merchantTradeNo);
    }

    /**
     * 窗口 8：當使用者從綠界頁面主動點選返回商店或取消付款時，執行此釋放與標記邏輯。
     *
     * @param tOrderId 訂單編號
     */
    public void handleCancelOrder(String tOrderId) {
        log.info("【綠界取消】使用者主動取消訂單或返回商店，訂單編號: {}", tOrderId);
        orderCleanupService.releaseOrder(tOrderId, "FAILED");
    }
}
