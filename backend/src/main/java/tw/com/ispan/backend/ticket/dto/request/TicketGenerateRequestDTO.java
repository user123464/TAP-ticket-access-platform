package tw.com.ispan.backend.ticket.dto.request;

import jakarta.validation.constraints.NotNull;

public record TicketGenerateRequestDTO(
        @NotNull(message = "場次 ID 不可為空") Integer sessionId
// 實務上，發布庫存是一個重大操作，
// 有時會要求使用者輸入二次確認的密碼或確認碼
// (如果你們沒有這個需求，只需 sessionId 即可)
// private String confirmCode;
) {

}
