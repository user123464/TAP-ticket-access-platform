package tw.com.ispan.backend.theme.dto.request;

public record ThemeCreateRequest(
        String title,
        String detail,
        String image,
        String organizerId) {

}
