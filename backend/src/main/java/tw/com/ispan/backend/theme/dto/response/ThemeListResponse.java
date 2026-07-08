package tw.com.ispan.backend.theme.dto.response;

import tw.com.ispan.backend.theme.entity.Theme;

public record ThemeListResponse(
    Integer themeId,
    String title,
    String detail,
    String image
) {
    public static ThemeListResponse fromEntity(Theme theme) {

        return new ThemeListResponse(
            theme.getThemeId(),
            theme.getTitle(),
            theme.getDetail(),
            theme.getImage()
        );
    }
}