package tw.com.ispan.backend.ticket.dto.response;

import java.util.List;

public record SessionTicketCataLogDTO(
        Integer sessionId,
        String sessionName, // 順便把場次名稱送給前端，標題可以直接用
        String locationName, // 場地名稱 (例如：台北小巨蛋)
        String locationSvg, // 全場只有這一份 SVG！
        List<TicketDetailResponseDTO> tickets // 裡面裝著 10,000 張轉換好的實體票
) {
}
