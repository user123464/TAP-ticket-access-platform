package tw.com.ispan.backend.theme.dto.response;

public record ThemeImageResource(byte[] data, String mimeType, long lastModified) {
}
