package tw.com.ispan.backend.ticket.dto.request;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

// 單一票種及其綁定的所有分區 ID
public record TicketTypeAllocationDTO(
        Integer ticketTypeId, // 修改既有票種時有值；若為新創票種則為 null

        @NotBlank(message = "票種名稱不可為空白") String name,

        @NotNull(message = "票價不可為空") @DecimalMin(value = "0.0", message = "票價不能為負數") BigDecimal price,

        String color,

        Boolean isEnabled, // 控制消費者是否能看到/購買

        @NotEmpty(message = "每個票種至少必須綁定一個分區") List<Integer> boundZoneIds // 對齊前端的 boundZoneIds 陣列
) {
}