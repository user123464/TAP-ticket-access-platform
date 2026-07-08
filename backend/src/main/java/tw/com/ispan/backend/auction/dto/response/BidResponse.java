package tw.com.ispan.backend.auction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import tw.com.ispan.backend.auction.entity.Bid;

public record BidResponse(
        Integer bidId,
        Integer auctionId,
        String userId,
        String userName,
        BigDecimal bidPrice,
        LocalDateTime createdAt) {

    public static BidResponse fromEntity(Bid bid) {
        return new BidResponse(
                bid.getBidId(),
                bid.getAuction().getAuctionId(),
                bid.getUser().getUserId(),
                bid.getUser().getName(),
                bid.getBidPrice(),
                bid.getCreatedAt());
    }
}
