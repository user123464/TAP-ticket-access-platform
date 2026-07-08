package tw.com.ispan.backend.ticket.dto.response;

import java.math.BigDecimal;

import tw.com.ispan.backend.ticket.entity.SessionZoneMapping;

//場次/票種 中介表 回傳 
public record SessionZoneMappingResponseDTO(
        Long mappingId, // 補上 PK，前端如果要切換 isEnabled 或刪除時會用到

        Integer sessionId, // 場次ID
        String sessionTitle, // 場次標題

        Integer zoneId,
        String zoneName, // 如果能從 Entity 拿到 Zone 的名字，建議加在這裡

        Integer ticketTypeId,
        String ticketTypeName, // 票種名稱 (e.g., 搖滾區)
        BigDecimal price, // 票價 (e.g., 4500)
        String color, // 顏色 (前端畫座位圖時馬上就能用)

        Boolean isEnabled
// isDeleted已經在repository被過濾，不用傳
) {
    public static SessionZoneMappingResponseDTO fromEntity(SessionZoneMapping entity) {
        return new SessionZoneMappingResponseDTO(
                entity.getMappingId(),
                entity.getSession().getSessionId(),
                entity.getSession().getTitle(),
                entity.getZone().getZoneId(),
                entity.getZone().getName(),
                entity.getTicketType().getTicketTypeId(),
                entity.getTicketType().getName(),
                entity.getTicketType().getPrice(),
                entity.getTicketType().getColor(),
                entity.getIsEnabled());
    }
}
