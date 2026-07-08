package tw.com.ispan.backend.ticket.controller;

import java.util.UUID;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.ticket.service.SseService;

@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
public class TicketSseController {

    private final SseService sseService;

    /**
     * 前端連線端點
     * 網址範例：GET /api/sse/subscribe/1
     * userId 跟 JWT 拿
     * 決定直接不查 JWT ，另外生成一個訪客 ID
     * 🌟 關鍵：produces 必須明確指定為 text/event-stream
     */

    @GetMapping(value = "subscribe/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribe(
            @PathVariable Integer sessionId) {

        // 隨機生成一個訪客 ID (例如: Guest-123e4567...)，交給總機記錄
        String guestId = "Guest-" + UUID.randomUUID().toString().substring(0, 8);

        // 直接將連線交給總機處理，並把 Emitter 回傳給 Spring MVC 保持連線
        return sseService.subscribe(sessionId, guestId);
    }

}
