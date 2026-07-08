package tw.com.ispan.backend.auction.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import tw.com.ispan.backend.auction.dto.response.BidResponse;

public interface BidService {

    // 出價（含驗證與更新；結果改由 SSE 通知）
    void placeBid(Integer auctionId, String userId, String userName, BigDecimal bidPrice);

    // 訂閱即時出價更新
    SseEmitter subscribeBidStream(Integer auctionId);

    // 查某競標的出價紀錄（最新在前）
    List<BidResponse> getBidHistory(Integer auctionId);
}
