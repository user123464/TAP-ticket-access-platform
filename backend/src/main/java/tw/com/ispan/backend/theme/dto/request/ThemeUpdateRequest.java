package tw.com.ispan.backend.theme.dto.request;

public record ThemeUpdateRequest(
    String title,
    String detail,
    String image
) {
    
}
