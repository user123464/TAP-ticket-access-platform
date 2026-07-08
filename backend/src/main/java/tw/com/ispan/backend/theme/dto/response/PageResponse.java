package tw.com.ispan.backend.theme.dto.response;

import java.util.List;

public record PageResponse<T>(
    List<T> data,
    int page,
    int size,
    long totalElements,
    int totalPages
) {}
