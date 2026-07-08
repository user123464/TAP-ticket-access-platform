package tw.com.ispan.backend.theme.dto.request;

import tw.com.ispan.backend.theme.enums.Status;

public record StatusUpdateRequest(
        Status status
    ) {

}
