package tw.com.ispan.backend.ticket.dto.request;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

//廠商批次發布「場次分區票種配置」的總請求 DTO
//前端傳來的結構是「一個 Session 包含多個 TicketType，每個 TicketType 包含多個 ZoneId」
public record SessionAllocationBatchRequestDTO(
        @NotNull(message = "場次 ID 不可為空") Integer sessionId,

        @NotEmpty(message = "必須至少配置一個票種") @Valid // 確保巢狀內部的 Record 也能觸發 Validation 檢查
        List<TicketTypeAllocationDTO> ticketTypes) {
}
