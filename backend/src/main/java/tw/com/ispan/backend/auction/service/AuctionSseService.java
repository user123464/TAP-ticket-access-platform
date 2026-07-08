package tw.com.ispan.backend.auction.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AuctionSseService {
    // 訂閱拍賣事件
    SseEmitter subscribe(Integer auctionId);

    // 廣播拍賣事件
    void broadcastToAuction(Integer auctionId, Object message);
}