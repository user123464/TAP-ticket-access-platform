package tw.com.ispan.backend.ticket.listener;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.RabbitMQConfig;
import tw.com.ispan.backend.config.RedisPubSubConfig;
import tw.com.ispan.backend.ticket.dto.message.RedisSsePayloadDTO;
import tw.com.ispan.backend.ticket.dto.message.TicketLockMessageDTO;
import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.ticket.entity.TicketStatus;
import tw.com.ispan.backend.ticket.repository.TicketRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class TicketMessageListener {

    private final TicketRepository ticketRepository;
    private final StringRedisTemplate stringRedisTemplate;// 注入 Redis
    // private final SseService sseService; // 注入 SSE 準備單 server 廣播
    private final ObjectMapper objectMapper; // Spring Boot 內建的 JSON 轉換器，用來把 Redis 傳來的字串變回 DTO

    // 監聽「鎖定座位」的佇列，並非同步更新資料庫
    @RabbitListener(queues = RabbitMQConfig.TICKET_LOCK_QUEUE)
    @Transactional // 記得加上 Transactional，確保資料庫更新的一致性
    public void handleTicketLockMessage(
            TicketLockMessageDTO message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        // 接收的資料以後改 log 檔
        try {
            // ==========================================
            // 🛡️ 冪等性防禦機制 (Idempotency Check)
            // ==========================================
            String idempotencyKey = "MQ:Consumed:TicketLock:" + message.messageId();

            // 去 Redis 貼號碼牌，保留 24 小時即可 (因為 MQ 重複投遞通常發生在短時間內)
            Boolean isFirstTime = stringRedisTemplate.opsForValue()
                    .setIfAbsent(idempotencyKey, "PROCESSED", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstTime)) {
                // 既然之前已經處理過了，這次就假裝處理成功，直接給 ACK 讓 RabbitMQ 把它刪掉
                channel.basicAck(deliveryTag, false);
                return; // 強制中斷方法，絕對不讓它碰到下方的資料庫邏輯
            }

            // ==========================================
            // 正常業務邏輯 (只有 isFirstTime == true 才會走到這)
            // ==========================================
            // 從 DB 撈出這些票
            List<Ticket> tickets = ticketRepository.findBySession_SessionIdAndTicketIdIn(
                    message.sessionId(), message.ticketIds());

            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(15);

            // 更新 DB 狀態
            for (Ticket ticket : tickets) {
                ticket.setStatus(TicketStatus.LOCKED);
                ticket.setLockBy(message.userId());
                ticket.setLockUntil(lockUntil);
            }
            // JPA 會在 Transaction 結束時自動 save (髒檢查)
            // 儲存的資料以後改 log 檔

            // 🌟 處理成功，手動通知 RabbitMQ 刪除該訊息
            // 第二個參數 false 代表不進行批量確認，只確認當前這一條
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("lock-seat message failed: {}", e.getMessage());
            // 🌟 處理失敗，手動通知 RabbitMQ 處理異常
            try {
                // 若發生異常 (例如 DB 當機)，我們必須把 Redis 的防護鎖解開，
                // 否則下次 RabbitMQ 重新派發這條訊息時，會被上面的冪等性機制誤擋！
                String idempotencyKey = "MQ:Consumed:TicketLock:" + message.messageId();
                stringRedisTemplate.delete(idempotencyKey);
                // 通知 RabbitMQ 重新入隊重試
                // basicNack 參數說明：(deliveryTag, multiple, requeue)
                // requeue = true: 代表將訊息重新放回 Queue 排隊重試
                // (注意：若資料庫永久性錯誤會造成無限重試，實戰中後續會導入死信佇列 DLQ)
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                // 錯誤訊息以後改 log 檔
                log.error(" RabbitMQ NACK send failed", ioException);
            }
        }
    }

    // ==========================================
    // 監聽「死信佇列 (DLQ)」，執行 15 分鐘超時釋放邏輯
    // ==========================================
    @RabbitListener(queues = RabbitMQConfig.TICKET_RELEASE_QUEUE)
    @Transactional
    public void handleTicketReleaseMessage(
            TicketLockMessageDTO message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            log.info("⏰ 收到超時檢查任務！準備檢查會員 {} 的座位...", message.userId());

            // 🛡️ 冪等性防禦
            String idempotencyKey = "MQ:Consumed:TicketRelease:" + message.messageId();
            Boolean isFirstTime = stringRedisTemplate.opsForValue()
                    .setIfAbsent(idempotencyKey, "PROCESSED", 24, TimeUnit.HOURS);

            if (Boolean.FALSE.equals(isFirstTime)) {
                channel.basicAck(deliveryTag, false);
                return;// 發現重複消費，直接 return 中斷方法
            }

            // 1. 去資料庫把這些票撈出來
            List<Ticket> tickets = ticketRepository.findBySession_SessionIdAndTicketIdIn(
                    message.sessionId(), message.ticketIds());

            // Redis 的狀態盤 Key
            String statusKey = "Session:" + message.sessionId() + ":Status";
            Map<String, String> redisUpdates = new HashMap<>();

            // SSE 用來收集被釋放的 ID 清單
            List<Long> releasedTicketIds = new ArrayList<>();

            for (Ticket ticket : tickets) {
                // 🌟 核心防呆：如果狀態還是 LOCKED，且擁有者還是這個人，代表他真的沒結帳！
                // (如果狀態已經變成 SOLD，代表結帳組員的 API 已經成功處理了，我們就什麼都不做)
                if (ticket.getStatus() == TicketStatus.LOCKED &&
                        message.userId().equals(ticket.getLockBy())) {
                    log.info("💔 座位 {} 超時未結帳，執行釋放！", ticket.getTicketId());

                    // A. 恢復 DB 狀態
                    ticket.setStatus(TicketStatus.AVAILABLE);
                    ticket.setLockBy(null);
                    ticket.setLockUntil(null);

                    // B. 準備恢復 Redis 狀態 (改回 1: 可售)
                    redisUpdates.put(String.valueOf(ticket.getTicketId()), "1");

                    // C. 刪除 Redis 上的專屬權杖鎖
                    stringRedisTemplate.delete("Ticket:Lock:" + ticket.getTicketId());

                    // 座位被釋放的 ID 放入清單
                    releasedTicketIds.add(ticket.getTicketId());
                }
            }

            // 執行 Redis 批量更新，讓前端的輕量輪詢立刻抓到最新狀態
            if (!redisUpdates.isEmpty()) {
                stringRedisTemplate.opsForHash().putAll(statusKey, redisUpdates);
            }

            // 重要：等 Redis 寫完、且確認這段 try 區塊無異常、即將 Commit 前，才發送批次廣播
            if (!releasedTicketIds.isEmpty()) {
                String sseMessage = String.format("{\"ticketIds\": %s, \"status\": 1}", releasedTicketIds.toString());

                // 透過 Redis 發送全國廣播
                RedisSsePayloadDTO payload = new RedisSsePayloadDTO(message.sessionId(), sseMessage);
                String redisMsg = objectMapper.writeValueAsString(payload);
                stringRedisTemplate.convertAndSend(RedisPubSubConfig.REDIS_TICKET_CHANNEL, redisMsg);
            }

            // 手動確認完成
            channel.basicAck(deliveryTag, false);

        } catch (Exception e) {
            log.error("❌ 處理超時釋放任務失敗: {}", e.getMessage());
            try {
                stringRedisTemplate.delete("MQ:Consumed:TicketRelease:" + message.messageId());
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("❌ RabbitMQ NACK 發送失敗", ioException);
            }
        }
    }
}
