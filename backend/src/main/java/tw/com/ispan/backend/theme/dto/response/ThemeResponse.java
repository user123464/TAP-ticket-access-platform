package tw.com.ispan.backend.theme.dto.response;

import tw.com.ispan.backend.theme.entity.Theme;

public record ThemeResponse(
    Integer themeId,
    String title,
    String detail,
    String image
) {
    public static ThemeResponse fromEntity(Theme theme) {

        return new ThemeResponse(
            theme.getThemeId(),
            theme.getTitle(),
            theme.getDetail(),
            theme.getImage()
        );
    }
}