package tw.com.ispan.backend.theme.dto.request;

public record ThemeQueryRequest(
    Integer page,
    Integer size,
    String keyword,
    String filter
) {}