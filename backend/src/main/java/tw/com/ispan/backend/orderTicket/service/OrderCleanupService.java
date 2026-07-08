package tw.com.ispan.backend.orderTicket.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.RedisPubSubConfig;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import tw.com.ispan.backend.ticket.dto.message.RedisSsePayloadDTO;
import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.ticket.entity.TicketStatus;
import tw.com.ispan.backend.ticket.repository.TicketRepository;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderDetailRepository;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;

/**
 * 訂單清理服務 (過期未付款釋放)。
 * 
 * <p>
 * 提供給定時排程任務調用，掃描超過付款期限的幽靈訂單，並自動將座位釋放回庫存中，防止座位被惡意佔用。
 * 同步更新 Redis 快取與發送 SSE 實時廣播，確保其他顧客能即時看見釋出之座位並進行購買。
 * </p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class OrderCleanupService {

    private final TicketOrderRepository ticketOrderRepository;
    private final TicketOrderDetailRepository ticketOrderDetailRepository;
    private final TicketRepository ticketRepository;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    /**
     * 釋出單一未付款的訂單座位，將訂單標記為指定的目標狀態（如 FAILED），並還原座位庫存為 AVAILABLE。
     * 同步更新 Redis 快取狀態並發送 SSE 廣播。
     *
     * @param tOrderId     訂單編號
     * @param targetStatus 目標狀態 (如 FAILED)
     */
    @Transactional
    public void releaseOrder(String tOrderId, String targetStatus) {
        TicketOrdersBean order = ticketOrderRepository.findById(tOrderId).orElse(null);
        if (order == null) {
            log.warn("【釋放訂單】查無訂單: {}", tOrderId);
            return;
        }

        // 僅允許對待付款 (UNPAID) 狀態的訂單進行釋放與標記，避免覆蓋已付款 (PAID) 的訂單
        if (!"UNPAID".equals(order.getPaymentStatus())) {
            log.info("【釋放訂單】訂單 {} 狀態為 {}，無需釋放", tOrderId, order.getPaymentStatus());
            return;
        }

        log.info("【釋放訂單】開始釋放訂單: {}，將狀態更改為: {}", tOrderId, targetStatus);
        order.setPaymentStatus(targetStatus);
        if ("FAILED".equals(targetStatus)) {
            order.setFailedAt(LocalDateTime.now());
        }
        ticketOrderRepository.save(order);

        Integer sessionId = null;
        List<Long> releasedTicketIds = new ArrayList<>();

        if (order.getOrderDetail() != null) {
            for (TicketOrderDetailBean detail : order.getOrderDetail()) {
                Ticket ticket = detail.getTicketTicket();
                if (ticket != null
                        && (ticket.getStatus() == TicketStatus.SOLD || ticket.getStatus() == TicketStatus.LOCKED)) {
                    ticket.setStatus(TicketStatus.AVAILABLE);
                    ticketRepository.save(ticket);
                    log.info("【釋放訂單】已還原票券 ID: {} 狀態為 AVAILABLE", ticket.getTicketId());

                    if (sessionId == null && ticket.getSession() != null) {
                        sessionId = ticket.getSession().getSessionId();
                    }
                    releasedTicketIds.add(ticket.getTicketId());
                }
                detail.setItemStatus(TicketOrderStatus.CANCELLED);
                detail.setIsUsed(TicketOrderUse.UNESTABLISHED);
                ticketOrderDetailRepository.save(detail);
            }
        }

        // 同步更新 Redis 快取並發送 SSE 廣播，通知其他購票者該座位已被釋出 (status: 1)
        if (sessionId != null && !releasedTicketIds.isEmpty()) {
            try {
                String statusKey = "Session:" + sessionId + ":Status";
                Map<String, String> redisUpdates = new HashMap<>();
                for (Long tid : releasedTicketIds) {
                    redisUpdates.put(String.valueOf(tid), "1");
                    stringRedisTemplate.delete("Ticket:Lock:" + tid);
                }
                stringRedisTemplate.opsForHash().putAll(statusKey, redisUpdates);

                String sseMessage = String.format("{\"ticketIds\": %s, \"status\": 1}", releasedTicketIds.toString());
                RedisSsePayloadDTO payload = new RedisSsePayloadDTO(sessionId, sseMessage);
                String redisMsg = objectMapper.writeValueAsString(payload);
                stringRedisTemplate.convertAndSend(RedisPubSubConfig.REDIS_TICKET_CHANNEL, redisMsg);
                log.info("【釋放訂單】Redis 與 SSE 廣播釋放成功，sessionId={}, ticketIds={}", sessionId, releasedTicketIds);
            } catch (Exception e) {
                log.error("【釋放訂單】同步更新 Redis 或發送 SSE 廣播失敗", e);
            }
        }
    }

    /**
     * 排程定時清理調用：釋出所有逾時 15 分鐘未付款的訂單與座位。
     */
    @Transactional
    public void releaseExpiredOrders() {
        LocalDateTime limitTime = LocalDateTime.now().minusMinutes(15);
        List<TicketOrdersBean> expiredOrders = ticketOrderRepository.findByPaymentStatusAndCreateAtBefore("UNPAID",
                limitTime);

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("【排程清理】掃描到 {} 筆逾時未付款訂單...", expiredOrders.size());
        for (TicketOrdersBean order : expiredOrders) {
            try {
                releaseOrder(order.getTOrderId(), "FAILED");
            } catch (Exception e) {
                log.error("【排程清理】釋放訂單 {} 失敗: {}", order.getTOrderId(), e.getMessage());
            }
        }
        log.info("【排程清理】過期未付款訂單處理完畢。");
    }
}
