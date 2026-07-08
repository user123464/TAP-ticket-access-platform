package tw.com.ispan.backend.theme.dto.response;

import java.time.LocalDateTime;

import tw.com.ispan.backend.theme.entity.Session;

public record SessionResponse(
    Integer sessionId,
    Integer locationId,
    String locationName,
    String title,
    String detail,
    String status,
    String statusDesc,
    LocalDateTime publishTime,
    LocalDateTime sellingStartTime,
    LocalDateTime sellingEndTime,
    LocalDateTime startTime,
    LocalDateTime endTime
) {
    public static SessionResponse fromEntity(Session session) {
        return new SessionResponse(
            session.getSessionId(),
            session.getLocation().getLocationId(),
            session.getLocation().getName(),
            session.getTitle(),
            session.getDetail(),
            session.getStatus().name(),
            session.getStatus().getDesc(),
            session.getPublishTime(),
            session.getSellingStartTime(),
            session.getSellingEndTime(),
            session.getStartTime(),
            session.getEndTime()
        );
    }
}
