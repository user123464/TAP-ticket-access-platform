package tw.com.ispan.backend.auction.dto.message;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import tw.com.ispan.backend.auction.dto.response.BidResponse;

public record AuctionBidEventDTO(
        String eventId,
        Integer auctionId,
        BidResponse latestBid,
        BigDecimal currentPrice,
        LocalDateTime serverTime) {
}