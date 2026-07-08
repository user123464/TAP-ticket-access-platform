package tw.com.ispan.backend.ticket.dto.request;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

//廠商建立/修改「單一票種」的請求 DTO
public record TicketTypeSaveRequestDTO(
        Integer ticketTypeId, // 如果是新增，前端傳 null；如果是修改，傳 ID

        @NotNull(message = "活動主題 ID 不能為空") Integer themeId, // 告訴後端這是屬於哪個「活動主題」的票種

        @NotBlank(message = "票種名稱不能為空白") @Size(max = 50, message = "票種名稱不可超過 50 字") String name, // 票種名稱 (如:
                                                                                                 // 搖滾A區)

        @NotNull(message = "票價不能為空") @DecimalMin(value = "0.0", message = "票價不能為負數") // 防呆：避免廠商填錯變成送錢
        @Digits(integer = 8, fraction = 2, message = "票價格式錯誤") BigDecimal price, // 票價 (注意型別對齊 Entity 的
                                                                                 // BigDecimal)

        @NotBlank(message = "票種顏色不能為空白") @Pattern(regexp = "^#([A-Fa-f0-9]{6})$", message = "顏色格式錯誤") String color // 票種代表色
) {
}
