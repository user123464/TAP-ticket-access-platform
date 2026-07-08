package tw.com.ispan.backend.ticket.dto.response;

import java.util.List;
import java.util.Map;

//回傳給前端「票種編輯頁面」的完整大禮包
public record SessionAllocationResultDTO(
        // 1. 整個場地的 SVG 字串 (超級大，所以只傳一次)
        String boundSvg,

        // 2. 所有的分區綁定規則陣列
        List<SessionZoneMappingResponseDTO> mappings,

        // 3. 該場地所有分區的「座位數對照表」 (Key: zoneId, Value: seatCount)
        Map<Integer, Integer> zoneCapacities,

        // 4. 場地模板名稱
        String locationName) {
}
