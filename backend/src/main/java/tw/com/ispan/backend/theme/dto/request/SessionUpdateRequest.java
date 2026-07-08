package tw.com.ispan.backend.theme.dto.request;

import java.time.LocalDateTime;

public record SessionUpdateRequest(
    
    String title,
    String detail,
    LocalDateTime startTime,
    LocalDateTime endTime,
    LocalDateTime sellingStartTime,
    LocalDateTime sellingEndTime
) {
    
}