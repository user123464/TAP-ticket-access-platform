package tw.com.ispan.backend.auction.dto.response;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import tw.com.ispan.backend.auction.entity.Auction;

public record AuctionResponse(
        Integer auctionId,
        String organizerId,
        Integer themeId,
        String status,
        String statusDesc,
        String title,
        String detail,
        String image,
        BigDecimal startPrice,
        BigDecimal buyoutPrice,
        BigDecimal currentPrice,
        LocalDateTime startTime,
        LocalDateTime endTime) {

    public static AuctionResponse fromEntity(Auction auction) {
        return new AuctionResponse(
                auction.getAuctionId(),
                auction.getTheme().getOrganizer().getOrganizerId(),
                auction.getTheme().getThemeId(),
                auction.getStatus().name(),
                auction.getStatus().getDesc(),
                auction.getTitle(),
                auction.getDetail(),
                auction.getImage(),
                auction.getStartPrice(),
                auction.getBuyoutPrice(),
                auction.getCurrentPrice(),
                auction.getStartTime(),
                auction.getEndTime());
    }
}
