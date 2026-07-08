package tw.com.ispan.backend.ticket.dto.response;

import java.math.BigDecimal;

import tw.com.ispan.backend.ticket.entity.Ticket;

public record TicketDetailResponseDTO(
        Long ticketId, // 票券實體 ID (購票結帳時需要)
        Long seatId, // 座位 ID
        Integer zoneId, // 所屬分區 ID
        String zoneName, // 分區名稱 (例如: "VIP 特區 A1")
        Integer ticketTypeId, // 票種 ID
        String ticketName, // 票種名稱 (例如: "VIP 特區 (粉)")
        BigDecimal price, // 票價
        String color, // 票種代表色
        Integer status, // 狀態碼 (前端用 1:可售, 2:鎖定, 3:售出 來判斷顏色)
        Integer physicalRow, // 實體排
        Integer physicalSeat, // 實體號
        Integer gridX, // 網格 X 座標
        Integer gridY // 網格 Y 座標
) {
    /**
     * 靜態工廠方法：負責把深層的 Entity 樹狀結構，攤平成前端好讀的單層 Record
     */
    public static TicketDetailResponseDTO fromEntity(Ticket ticket) {

        // 處理 Enum 狀態轉數字 (假設前端習慣用 1:AVAILABLE, 2:LOCKED, 3:SOLD, 0:RESERVED)
        int statisCode = switch (ticket.getStatus()) {
            case AVAILABLE -> 1;
            case LOCKED -> 2;
            case SOLD -> 3;
            case RESERVED -> 0;
            default -> 0;
        };
        return new TicketDetailResponseDTO(
                ticket.getTicketId(),
                ticket.getSeat().getSeatId(),
                ticket.getSeat().getZone().getZoneId(),
                ticket.getSeat().getZone().getName(),

                // 防呆：如果票券被隱藏或解除綁定，ticketType 可能為 null
                ticket.getTicketType() != null ? ticket.getTicketType().getTicketTypeId() : null,
                ticket.getTicketType() != null ? ticket.getTicketType().getName() : "未綁定",
                ticket.getTicketType() != null ? ticket.getTicketType().getPrice() : BigDecimal.ZERO,
                ticket.getTicketType() != null ? ticket.getTicketType().getColor() : "#cbd5e1",

                statisCode,
                ticket.getSeat().getRowNum(),
                ticket.getSeat().getSeatNum(),
                ticket.getSeat().getXIndex(),
                ticket.getSeat().getYIndex());
    }
}
