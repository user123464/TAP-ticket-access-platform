package tw.com.ispan.backend.auction.dto.request;

import java.math.BigDecimal;

public record BidCreateRequest(
        BigDecimal bidPrice) {
}
