package tw.com.ispan.backend.ticket.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.ticket.dto.message.RedisSsePayloadDTO;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisMessageReceiver {

    private final SseService sseService;
    // Spring Boot 內建的 JSON 轉換神器，用來把 Redis 傳來的字串變回 DTO
    private final ObjectMapper objectMapper;

    /**
     * 🎧 接收 Redis 廣播的方法
     * ⚠️ 注意：這個方法名稱 "receiveRedisMessage" 必須與在 RedisPubSubConfig
     * 裡的 listenerAdapter 中設定的名字一模一樣！
     */
    public void receiveRedisMessage(String message) {
        try {
            // 1. 將 Redis 傳來的純文字 JSON 拆解回我們的包裹 DTO
            RedisSsePayloadDTO payload = objectMapper.readValue(message, RedisSsePayloadDTO.class);
            log.info("📡 收到 Redis 全國廣播！準備推播給本機場次 {} 的觀眾...", payload.sessionId());

            // 2. 將包裹裡的內容，轉交給本機的 SSE 總機進行廣播
            // 這樣不管消費者連到哪一台伺服器，該台伺服器的 SseService 都會幫忙推播！
            sseService.broadcastToSession(payload.sessionId(), payload.jsonMessage());
        } catch (Exception e) {
            log.error("❌ 解析 Redis 廣播訊息失敗: {}", e.getMessage(), e);
        }

    }
}
