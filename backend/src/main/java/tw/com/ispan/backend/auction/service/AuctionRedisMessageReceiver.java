package tw.com.ispan.backend.auction.service;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.auction.dto.message.AuctionBidEventDTO;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionRedisMessageReceiver {

    private final AuctionSseService auctionSseService;
    private final ObjectMapper objectMapper;

    public void receiveRedisMessage(String message) {
        try {
            AuctionBidEventDTO payload = objectMapper.readValue(message, AuctionBidEventDTO.class);
            auctionSseService.broadcastToAuction(payload.auctionId(), payload);
        } catch (Exception e) {
            log.error("解析拍賣 Redis 訊息失敗", e);
        }
    }
}