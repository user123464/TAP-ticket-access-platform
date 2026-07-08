package tw.com.ispan.backend.location.dto.request;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

// 1. 最外層的主容器 (接收場館資訊)
@Data
public class LocationCreateRequestDTO {
    private Integer id; // 場館ID

    private String name;// 場館名稱
    private Integer totalCapacity;// 最大容納人數或單位總數
    private Integer gridMaxX; // 網格畫布最大寬(maxX)
    private Integer gridMaxY; // 網格畫布最大高(maxY)

    private String rawSvg;// SVG原始檔(物理基礎)
    private String boundSvg;// SVG工作檔(已帶有 data-zone-id)

    private String address; // 地址

    // 一個場館底下，包著一個「分區清單」
    private List<ZoneSaveDTO> zones;

    // 分區內部類別
    // 2. 第二層容器 (接收分區資訊)
    // 必須加上 public static
    // 這樣在其他 Service 想單獨使用它時，
    // 只要呼叫 LocationCreateRequestDTO.ZoneSaveDTO 即可
    @Data
    public static class ZoneSaveDTO {
        // 加上 id 欄位 (如果是新增，前端傳 null；如果是修改，前端傳數字)
        private Integer id;
        private String name;
        private String color;

        // 一個分區底下，包著一個「座位清單」
        private List<SeatSaveDTO> seats;
    }

    // 座位內部類別
    // 3. 最底層容器 (接收單一個座位的座標與排號)
    @Data
    public static class SeatSaveDTO {
        private Integer rowNum;
        private Integer seatNum;

        // 加上 @JsonProperty 強制對應前端的 key
        @JsonProperty("xIndex")
        private Integer xIndex;

        // 加上 @JsonProperty 強制對應前端的 key
        @JsonProperty("yIndex")
        private Integer yIndex;
    }
}
