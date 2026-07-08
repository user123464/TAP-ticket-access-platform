package tw.com.ispan.backend.auction.service.impl;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.auction.dto.message.BidConfirmedMessageDTO;
import tw.com.ispan.backend.auction.dto.response.BidResponse;
import tw.com.ispan.backend.auction.enums.BidGateResult;
import tw.com.ispan.backend.auction.repository.BidRepository;
import tw.com.ispan.backend.auction.service.AuctionSseService;
import tw.com.ispan.backend.auction.service.AuctionService;
import tw.com.ispan.backend.auction.service.BidRedisGateService;
import tw.com.ispan.backend.auction.service.BidService;
import tw.com.ispan.backend.config.RabbitMQConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidServiceImpl implements BidService {

    private final BidRepository bidRepository;
    private final AuctionSseService auctionSseService;
    private final AuctionService auctionService;
    private final RabbitTemplate rabbitTemplate;
        private final BidRedisGateService bidRedisGateService;

    @Override
    @Transactional
        public void placeBid(Integer auctionId, String userId, String userName, BigDecimal bidPrice) {
        // 1. 基本參數驗證
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("userId 不可為空");
        }
        if (bidPrice == null || bidPrice.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("bidPrice 必須大於0");
        }

        // 2. Redis 原子競價判斷
        // 這裡使用 Redis 來做出價的原子性檢查，避免多個使用者同時出價造成資料不一致
        BidGateResult gateResult = bidRedisGateService.reserveBid(auctionId, userId, bidPrice);
        if (gateResult == BidGateResult.NOT_ACTIVE) {
            throw new IllegalStateException("拍賣未啟用");
        }
        if (gateResult == BidGateResult.EXPIRED) {
            auctionService.endAuction(auctionId);
            throw new IllegalStateException("不在出價時間內");
        }
        if (gateResult == BidGateResult.DUPLICATE_USER) {
            throw new IllegalStateException("不可連續出價");
        }
        if (gateResult == BidGateResult.LOWER_THAN_CURRENT) {
            throw new IllegalArgumentException("必須高於目前價格");
        }
        
        // 3. 組成已確認事件，丟進 RabbitMQ
        try {
            BidConfirmedMessageDTO message = new BidConfirmedMessageDTO(
                    UUID.randomUUID().toString(),
                    auctionId,
                    userId,
                    userName,
                    bidPrice,
                    java.time.LocalDateTime.now());

            CorrelationData correlationData = new CorrelationData(message.messageId());
            // 設定回呼函式，當訊息被成功送達 RabbitMQ 後，才會進行後續的資料庫更新
            rabbitTemplate.convertAndSend(
                    RabbitMQConfig.BID_EXCHANGE,
                    RabbitMQConfig.BID_CONFIRMATION_ROUTING_KEY,
                    message,
                    correlationData);

            if (gateResult == BidGateResult.BUYOUT) {
                auctionService.endAuction(auctionId);
            }
        } catch (Exception ex) {
            throw new RuntimeException("出價送出失敗", ex);
        }
    }

    @Override
    public SseEmitter subscribeBidStream(Integer auctionId) {
        // SSE 訂閱交給專門的即時推播服務處理
        return auctionSseService.subscribe(auctionId);
    }

    @Override
    public List<BidResponse> getBidHistory(Integer auctionId) {
        // 由最新到最舊回傳歷史出價
        return bidRepository.findByAuctionAuctionIdOrderByCreatedAtDesc(auctionId)
                .stream().map(BidResponse::fromEntity).toList();
    }
}