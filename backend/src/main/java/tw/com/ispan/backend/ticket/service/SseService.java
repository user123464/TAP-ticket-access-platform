package tw.com.ispan.backend.ticket.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface SseService {
    // 聽眾連線 (訂閱頻道)，當前端進入 SVG 座位圖畫面時，會呼叫這個方法
    SseEmitter subscribe(Integer sessionId, String userId);

    // 發送全頻道廣播，當 RabbitMQ 扣位成功，或死信佇列釋放座位時，會呼叫這個方法
    void broadcastToSession(Integer sessionId, Object message);

}
