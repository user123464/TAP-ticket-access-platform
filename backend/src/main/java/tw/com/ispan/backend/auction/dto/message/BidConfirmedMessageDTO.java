package tw.com.ispan.backend.auction.dto.message;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record BidConfirmedMessageDTO(
        String messageId,
        Integer auctionId,
        String userId,
        String userName,
        BigDecimal bidPrice,
        LocalDateTime eventTime) {
}