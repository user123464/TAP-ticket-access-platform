package tw.com.ispan.backend.ticket.dto.response;

import java.math.BigDecimal;

import tw.com.ispan.backend.ticket.entity.TicketType;

// 回傳給前端顯示用的票種 DTO
public record TicketTypeResponseDTO(
        Integer ticketTypeId,
        String name,
        BigDecimal price,
        String color
// 省略了 isDeleted，因為在 Service 層撈取資料時，我們通常會直接用
// ticketTypeRepository.findByThemeIdAndIsDeletedFalse(themeId)
// 只把「活著的」票種撈出來轉成 DTO 丟給前端。
// 既然傳過去的都是活著的，前端當然就不需要知道 isDeleted 這個欄位了。
) {
    // 實作從 Entity 轉換為 DTO 的方法
    public static TicketTypeResponseDTO fromEntity(TicketType entity) {
        return new TicketTypeResponseDTO(
                entity.getTicketTypeId(),
                entity.getName(),
                entity.getPrice(),
                entity.getColor());
    }
}
