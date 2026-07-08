package tw.com.ispan.backend.theme.dto.response;

import java.time.LocalDateTime;

import tw.com.ispan.backend.theme.entity.Theme;

public record OrganizerThemeResponse(
    Integer themeId,
    String title,
    String detail,
    String image,
    String status,
    String statusDesc,
    LocalDateTime createdAt,
    LocalDateTime updatedAt
) {
    public static OrganizerThemeResponse fromEntity(Theme theme) {

        return new OrganizerThemeResponse(
            theme.getThemeId(),
            theme.getTitle(),
            theme.getDetail(),
            theme.getImage(),
            theme.getStatus().name(),
            theme.getStatus().getDesc(),
            theme.getCreatedAt(),
            theme.getUpdatedAt()
        );
    }
}