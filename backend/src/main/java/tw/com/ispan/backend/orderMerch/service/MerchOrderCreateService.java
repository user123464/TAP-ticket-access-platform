package tw.com.ispan.backend.orderMerch.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.orderMerch.dto.OrderCreateRequestDTO;
import tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;
import tw.com.ispan.backend.payment.ecpay.EcpayPaymentService;
import tw.com.ispan.backend.products.entity.Product;
import tw.com.ispan.backend.products.entity.ProductVariant;
import tw.com.ispan.backend.products.repository.ProductRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchOrderCreateService {

    private final MerchOrderRepository merchOrderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate redisTemplate;
    private final EcpayPaymentService ecpayPaymentService;
    private final MerchOrderQueryService merchOrderQueryService;

    // 建立商品訂單功能 (安全接收自訂的 userId)
    @Transactional
    public OrderResponseDTO createOrder(OrderCreateRequestDTO requestDTO, String userId) {
        // (1) 拿出前端給的防重下單號碼牌
        String token = requestDTO.getSubmitToken();

        // (2) 拿去 Redis 上登記，鎖定 24 小時以避免重複點擊
        Boolean isFirstTime = redisTemplate.opsForValue().setIfAbsent("MerchOrderToken:" + token, "LOCKED", 24,
                TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isFirstTime)) {
            throw new RuntimeException("訂單處理中，請勿連續點擊結帳按鈕！");
        }

        // (3) 尋找買家會員是否存在
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("錯誤：此會員不存在！"));

        // 1. 建立訂單主檔
        MerchOrderBean order = new MerchOrderBean();
        order.setUserId(user);
        order.setCreatedAt(LocalDateTime.now());

        order.setTotalAmount(BigDecimal.ZERO);
        order.setPaymentStatus("UNPAID"); // 預設設為未付款
        order.setRecipientName(requestDTO.getRecipientName());
        order.setRecipientPhone(requestDTO.getRecipientPhone());
        order.setRecipientEmail(requestDTO.getRecipientEmail());
        order.setRecipientAddress(requestDTO.getRecipientAddress());
        order.setIdentityNumber(requestDTO.getIdentityNumber());

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<MerchOrderDetailBean> details = new ArrayList<>();

        StringBuilder itemNameBuilder = new StringBuilder();

        // 2. 建立訂單明細
        for (OrderCreateRequestDTO.DetailRequest reqDetail : requestDTO.getOrderMerches()) {
            Product product = productRepository.findById(reqDetail.getProductId())
                    .orElseThrow(() -> new RuntimeException("錯誤：找不到該商品！"));

            ProductVariant matchedVariant = null;
            System.out.println("========== Variant Debug ==========");
            System.out.println("Request ProductId = " + reqDetail.getProductId());
            System.out.println("Request VariantId = " + reqDetail.getVariantId());

            System.out.println("Variant Count = " + product.getVariants().size());
            if (product.getVariants() != null) {
                for (ProductVariant variant : product.getVariants()) {
                    System.out.println(
                            "DB VariantId = " + variant.getVariantId());
                    if (variant.getVariantId().equals(reqDetail.getVariantId())) {
                        System.out.println("找到 Variant !");
                        matchedVariant = variant;
                        break;
                    }
                }
            }

            if (matchedVariant == null) {
                throw new RuntimeException("錯誤：找不到該商品的規格或款式！");
            }

            // 檢查庫存數量
            if (matchedVariant.getStockQty() < reqDetail.getQuantity()) {
                throw new RuntimeException("錯誤：" + product.getProductName() + " 庫存不足！");
            }

            // 扣減庫存
            matchedVariant.setStockQty(matchedVariant.getStockQty() - reqDetail.getQuantity());

            // 建立明細 Bean
            MerchOrderDetailBean detail = new MerchOrderDetailBean();
            detail.setMerchOrder(order);
            detail.setProductProduct(product);
            detail.setVariantId(reqDetail.getVariantId());
            detail.setQuantity(reqDetail.getQuantity());
            detail.setUnitPrice(matchedVariant.getUnitPrice());
            detail.setItemStatus(MerchOrderEnum.UNPAID);

            // 計算小計金額並累加到總額
            BigDecimal subtotal = matchedVariant.getUnitPrice().multiply(BigDecimal.valueOf(reqDetail.getQuantity()));
            totalAmount = totalAmount.add(subtotal);

            details.add(detail);

            itemNameBuilder.append(product.getProductName())
                    .append(" x")
                    .append(reqDetail.getQuantity())
                    .append("#NT$")
                    .append(matchedVariant.getUnitPrice())
                    .append("#");
        }

        String itemName = itemNameBuilder.toString();

        order.setTotalAmount(totalAmount);
        order.setMerchOrderDetails(details);

        // 儲存主檔，明細將會經由 CascadeType.PERSIST / MERGE 自動寫入資料庫
        MerchOrderBean savedOrder = merchOrderRepository.save(order);

        OrderResponseDTO responseDTO = merchOrderQueryService.convertToResponseDTO(savedOrder);

        // 7. 產生綠界支付 HTML 表單
        try {
            String ecpayForm = ecpayPaymentService.generateAioCreditForm(
                    savedOrder.getMerchantTradeNo(),
                    totalAmount.intValue(),
                    "Merch_Order",
                    itemName,
                    "?mOrderId=" + savedOrder.getMOrderId());

            responseDTO.setEcpayForm(ecpayForm);
            log.info("【建立商品訂單】成功為訂單 {} 產生綠界 HTML 支付表單", savedOrder.getMOrderId());
        } catch (Exception e) {
            log.error("【建立商品訂單】產生綠界支付表單失敗", e);
        }

        return responseDTO;
    }

    /**
     * 每分鐘檢查一次未付款的訂單，若超過 15 分鐘則將其狀態改為 FAILED。
     */
    @Transactional
    @Scheduled(fixedRate = 60000)
    public void checkUnpaidOrders() {
        List<MerchOrderBean> list = merchOrderRepository.findByPaymentStatus("UNPAID");

        List<MerchOrderBean> updated = new ArrayList<>();
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);

        for (MerchOrderBean order : list) {
            if (order.getCreatedAt().isBefore(cutoff)) {
                order.setPaymentStatus("FAILED");
                order.setFailedAt(LocalDateTime.now());
                order.cancelDetails();

                updated.add(order);

                log.info("訂單 {} 超時未付款 → FAILED", order.getMOrderId());
            }
        }
        if (!updated.isEmpty()) {
            merchOrderRepository.saveAll(updated);
        }
    }
}