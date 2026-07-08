package tw.com.ispan.backend.auction.dto.request;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AuctionUpdateRequest(
    String title,
    String detail,
    String image,
    BigDecimal startPrice,
    BigDecimal buyoutPrice,
    LocalDateTime startTime,
    LocalDateTime endTime
) {}
