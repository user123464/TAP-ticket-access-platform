package tw.com.ispan.backend.ticket.dto.message;

import java.util.List;

// 要在 RabbitMQ 傳遞的「訊息包裹」
// 紀錄要告訴資料庫的情報：誰、在哪個場次、鎖了哪些票
// 新增 messageId 作為冪等性防護的唯一憑證
public record TicketLockMessageDTO(
                String messageId,
                Integer sessionId,
                List<Long> ticketIds,
                String userId) {
}
