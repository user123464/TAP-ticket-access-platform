package tw.com.ispan.backend.auction.service.impl;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.auction.dto.message.AuctionBidEventDTO;
import tw.com.ispan.backend.auction.dto.message.BidConfirmedMessageDTO;
import tw.com.ispan.backend.auction.dto.response.BidResponse;
import tw.com.ispan.backend.auction.service.BidEventPublisher;
import tw.com.ispan.backend.config.AuctionRedisPubSubConfig;

@Slf4j
@Service
@RequiredArgsConstructor
public class BidEventPublisherImpl implements BidEventPublisher {

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public void publishBidConfirmedEvent(BidConfirmedMessageDTO message) {
        try {
            AuctionBidEventDTO event = new AuctionBidEventDTO(
                    message.messageId(),
                    message.auctionId(),
                    new BidResponse(
                            null,
                            message.auctionId(),
                            message.userId(),
                            message.userName(),
                            message.bidPrice(),
                            message.eventTime()),
                    message.bidPrice(),
                    message.eventTime());

            stringRedisTemplate.convertAndSend(
                    AuctionRedisPubSubConfig.REDIS_AUCTION_BID_CHANNEL,
                    objectMapper.writeValueAsString(event));
        } catch (Exception e) {
            log.error("Publish bid confirmed event failed for message {}", message.messageId(), e);
            throw new RuntimeException("Publish bid confirmed event failed", e);
        }
    }
}