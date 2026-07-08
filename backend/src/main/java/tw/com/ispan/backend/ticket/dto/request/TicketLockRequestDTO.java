package tw.com.ispan.backend.ticket.dto.request;

import java.util.List;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

//票券鎖定請求
public record TicketLockRequestDTO(

                @NotNull(message = "場次ID不可為空") Integer sessionId,

                // 因為前端傳來的是陣列 (map 出來的結果)，所以必須用 List 接收
                @NotEmpty(message = "尚未選擇任何票券") List<Long> ticketIds) {
}
