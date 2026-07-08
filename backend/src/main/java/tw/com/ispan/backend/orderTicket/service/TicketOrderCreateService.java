package tw.com.ispan.backend.orderTicket.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.RedisPubSubConfig;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.orderTicket.dto.OrderCreateRequestDTO;
import tw.com.ispan.backend.orderTicket.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderDetailRepository;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;
import tw.com.ispan.backend.payment.ecpay.EcpayPaymentService;
import tw.com.ispan.backend.ticket.dto.message.RedisSsePayloadDTO;
import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.ticket.entity.TicketStatus;
import tw.com.ispan.backend.ticket.repository.TicketRepository;

/**
 * 訂單建立服務。
 * 
 * <p>
 * 處理客戶提交購票結帳的業務，包含 Redis 冪等性控制、JPA 效能優化、座位鎖定校驗、樂觀鎖防超賣等核心邏輯。
 * 同步更新 Redis 快取狀態盤與發送 SSE 實時廣播，確保前後台座位狀態即時同步。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketOrderCreateService {

    private final TicketOrderRepository ticketOrderRepository;
    private final TicketOrderDetailRepository ticketOrderDetailRepository;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final TicketOrderQueryService ticketOrderQueryService;
    private final ObjectMapper objectMapper;
    private final EcpayPaymentService ecpayPaymentService;

    // 注入 JPA 實體管理器，用於獲取 Proxy 對象
    @PersistenceContext
    private EntityManager entityManager;

    /**
     * 建立訂單功能 (包含冪等性控制、樂觀鎖校驗與 Redis/SSE 實時同步)。
     *
     * @param requestDTO 前端傳入之訂單建立請求 DTO
     * @param userId     當前登入會員之 ID
     * @return 建立成功後之訂單摘要 DTO
     */
    @Transactional
    public OrderResponseDTO createOrder(OrderCreateRequestDTO requestDTO, String userId) {
        log.info("【建立訂單】開始處理 - userId={}, submitToken={}", userId, requestDTO.getSubmitToken());

        // 1. 冪等性控制 : 檢查並鎖定前端 submitToken，防範網絡延遲導致使用者重複點擊結帳重複扣款
        String token = requestDTO.getSubmitToken();
        Boolean isFirstTime = stringRedisTemplate.opsForValue().setIfAbsent(
                "OrderToken:" + token, "LOCKED", 24, TimeUnit.HOURS);

        if (Boolean.FALSE.equals(isFirstTime)) {
            log.warn("【建立訂單】重複提交的請求已被攔截 - submitToken={}", token);
            throw new RuntimeException("訂單處理中，請勿連續點擊結帳按鈕！");
        }

        // 2. 效能優化 : 利用 JPA Proxy (getReference) 避免直接查詢 User 表，提昇寫入效能
        User loginUser = entityManager.getReference(User.class, userId);

        // 3. 建立訂單主檔 (TicketOrdersBean)
        TicketOrdersBean order = new TicketOrdersBean();
        order.setUserId(loginUser);
        order.setContactName(requestDTO.getContactName());
        order.setContactEmail(requestDTO.getContactEmail());
        order.setContactPhone(requestDTO.getContactPhone());
        order.setPaymentStatus("UNPAID"); // 預設狀態為待付款
        order.setTotalAmount(BigDecimal.ZERO); // 預設金額為 0，稍後累加

        String merchantTradeNo = "T" + UUID.randomUUID().toString().replace("-", "").substring(0, 19);
        order.setMerchantTradeNo(merchantTradeNo);

        // 預先儲存訂單以取得訂單編號，方便後續明細關聯的外鍵寫入
        TicketOrdersBean saveOrder = ticketOrderRepository.save(order);

        BigDecimal totalAmount = BigDecimal.ZERO;
        List<TicketOrderDetailBean> saveDetail = new ArrayList<>();

        StringBuilder itemNameBuilder = new StringBuilder();

        Integer sessionId = null;
        List<Long> ticketIds = new ArrayList<>();

        // 4. 循環建立並處理訂單明細 (TicketOrderDetailBean)
        for (OrderCreateRequestDTO.DetailRequest reqDetail : requestDTO.getOrderTickets()) {
            TicketOrderDetailBean detail = new TicketOrderDetailBean();
            detail.setTicketOrder(saveOrder);

            // 根據傳入的 ticketId 找出對應票券實體
            Ticket ticketEntity = ticketRepository.findById(reqDetail.getTicketId())
                    .orElseThrow(() -> new RuntimeException("錯誤：查無此票券！"));

            // 驗證票券是否為已被鎖定狀態 (防範繞過前端購物車直接向 API 發送的惡意/無效請求)
            if (ticketEntity.getStatus() != TicketStatus.LOCKED) {
                log.warn("【建立訂單】座位未被鎖定，無法結帳 - ticketId={}", reqDetail.getTicketId());
                throw new RuntimeException("錯誤：該座位未被鎖定，無法結帳！");
            }

            detail.setTicketTicket(ticketEntity);

            // 獲取場次 ID 用於 Redis/SSE 同步
            if (sessionId == null && ticketEntity.getSession() != null) {
                sessionId = ticketEntity.getSession().getSessionId();
            }
            ticketIds.add(ticketEntity.getTicketId());

            // 以票種之定價為成交價
            BigDecimal realPrice = ticketEntity.getTicketType().getPrice();
            detail.setUnitPrice(realPrice);

            // 更新票的狀態為「已售出」，避免重複購買 (配合 Ticket.java 之 @Version 樂觀鎖防超賣)
            ticketEntity.setStatus(TicketStatus.SOLD);

            // 設定實名制資訊與預設門票狀態
            detail.setRealName(reqDetail.getRealName());
            detail.setIdentityNumber(reqDetail.getIdentityNumber());
            detail.setItemStatus(TicketOrderStatus.UNPAID); // 預設為待付款狀態
            detail.setIsUsed(TicketOrderUse.Unredeemed); // 未核銷

            // 產生 12 碼的隨機大寫 QR Code Hash，供驗票核銷使用
            detail.setQrCodeHash(UUID.randomUUID().toString()
                    .replace("-", "")
                    .substring(0, 12)
                    .toUpperCase());

            totalAmount = totalAmount.add(realPrice);

            itemNameBuilder
                    .append(ticketEntity.getTicketType().getName())
                    .append(" x1#");

            // 儲存明細實體並加進列表中
            saveDetail.add(ticketOrderDetailRepository.save(detail));
        }

        // 加上系統服務費 100 元 (符合前端 Vue 計算的總金額)
        totalAmount = totalAmount.add(new BigDecimal("100"));
        itemNameBuilder.append("系統服務費 x1");
        String itemName = itemNameBuilder.toString();

        // JPA 交易髒檢查 (Dirty Checking)：只需修改屬性，交易提交時會自動同步回資料庫，無需重複呼叫 save()
        saveOrder.setTotalAmount(totalAmount);

        // 5. 實時同步 Redis 快取狀態並發送 SSE 全國廣播 (防止 Ghost Lock 發生)
        if (sessionId != null && !ticketIds.isEmpty()) {
            try {
                String statusKey = "Session:" + sessionId + ":Status";
                Map<String, String> redisUpdates = new HashMap<>();
                for (Long tid : ticketIds) {
                    // 更新 Redis 狀態盤狀態為 3 (SOLD 已售出)
                    redisUpdates.put(String.valueOf(tid), "3");
                    // 主動刪除 Redis 上的暫時性購物車鎖
                    stringRedisTemplate.delete("Ticket:Lock:" + tid);
                }
                stringRedisTemplate.opsForHash().putAll(statusKey, redisUpdates);

                // 透過 Redis Pub/Sub 發送 SSE 廣播，通知同場次所有其他使用者瀏覽器將此座位即時畫反灰 (status: 3)
                String sseMessage = String.format("{\"ticketIds\": %s, \"status\": 3}", ticketIds.toString());
                RedisSsePayloadDTO payload = new RedisSsePayloadDTO(sessionId, sseMessage);
                String redisMsg = objectMapper.writeValueAsString(payload);
                stringRedisTemplate.convertAndSend(RedisPubSubConfig.REDIS_TICKET_CHANNEL, redisMsg);

                log.info("【建立訂單】Redis 快取狀態同步成功，已廣播 SOLD 狀態 - sessionId={}, ticketIds={}", sessionId, ticketIds);
            } catch (Exception e) {
                log.error("【建立訂單】同步更新 Redis 或發送 SSE 廣播失敗", e);
            }
        }

        // 6. 轉換為 nested DTO 格式返回給前端
        OrderResponseDTO responseDTO = ticketOrderQueryService.convertToResponseDTO(saveOrder, saveDetail);

        // 7. 產生綠界支付 HTML 表單 (強制測試模式)
        try {
            String ecpayForm = ecpayPaymentService.generateAioCreditForm(
                    saveOrder.getMerchantTradeNo(),
                    totalAmount.intValue(),
                    "Concert_Ticket",
                    itemName,
                    "?tOrderId=" + saveOrder.getTOrderId());

            responseDTO.setEcpayForm(ecpayForm);

            log.info("【建立訂單】成功為訂單 {} 產生綠界 HTML 支付表單", saveOrder.getTOrderId());

        } catch (Exception e) {
            e.printStackTrace();
            log.error("【建立訂單】產生綠界支付表單失敗", e);
            throw e;
        }
        log.info("【建立訂單】訂單建立成功 - orderId={}, totalAmount={}", saveOrder.getTOrderId(), totalAmount);
        return responseDTO;
    }

}
