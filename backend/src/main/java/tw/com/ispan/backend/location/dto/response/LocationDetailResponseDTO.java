package tw.com.ispan.backend.location.dto.response;

import java.util.List;

import tw.com.ispan.backend.location.entity.Location;

// 用於詳情：包含完整資訊與分區
public record LocationDetailResponseDTO(
        Integer id,
        String name,
        Integer totalCapacity,
        String address,
        Integer gridMaxX, // 補上 X 最大值
        Integer gridMaxY, // 補上 Y 最大值
        String rawSvg, // 補上原始 SVG
        String boundSvg, // 補上工作檔 SVG
        List<ZoneResponseDTO> zones) {
    // 改為接收額外組裝好的 zones
    public static LocationDetailResponseDTO fromEntity(Location entity, List<ZoneResponseDTO> zones) {
        return new LocationDetailResponseDTO(
                entity.getLocationId(),
                entity.getName(),
                entity.getTotalCapacity(),
                entity.getAddress(),
                entity.getGridMaxX(), // 從實體拿資料
                entity.getGridMaxY(), // 從實體拿資料
                entity.getRawSvg(), // 從實體拿原始 SVG
                entity.getBoundSvg(), // 從實體拿工作檔 SVG
                zones);
    }
}
