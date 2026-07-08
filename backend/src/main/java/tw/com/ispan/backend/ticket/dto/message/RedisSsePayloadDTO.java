package tw.com.ispan.backend.ticket.dto.message;

// 紀錄跨伺服器廣播需要的情報
public record RedisSsePayloadDTO(
        Integer sessionId,
        String jsonMessage
// 裡面裝我們原本打包好的座位更新 JSON，
// 例如 {"ticketIds": [976], "status": 2}
) {
}
