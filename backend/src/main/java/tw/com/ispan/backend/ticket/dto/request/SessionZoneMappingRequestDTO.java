package tw.com.ispan.backend.ticket.dto.request;

import jakarta.annotation.Nullable;
import jakarta.validation.constraints.NotNull;

//場次/票種 中介表
public record SessionZoneMappingRequestDTO(
        // 補上 PK：如果有值代表「修改既有綁定」，如果是 null 代表「新增綁定」
        @Nullable Long mappingId,
        @NotNull(message = "場次ID不可為空") Integer sessionId, // [FK]場次ID
        @NotNull(message = "分區ID不可為空") Integer zoneId, // [FK]物理分區ID
        @NotNull(message = "票種ID不可為空") Integer ticketTypeId, // [FK]票種ID
        Boolean isEnabled // 控制消費者是否能看到/購買(預設給 true，防呆)

// 稽核欄位交由@EntityListeners(AuditingEntityListener.class)填入
) {
}
