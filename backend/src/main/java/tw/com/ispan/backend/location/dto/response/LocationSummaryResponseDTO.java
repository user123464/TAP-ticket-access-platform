package tw.com.ispan.backend.location.dto.response;

import tw.com.ispan.backend.location.entity.Location;

// 用於列表：只需場館 ID 、名字、地址、總容量、軟刪除狀態
public record LocationSummaryResponseDTO(
        Integer id,
        String name,
        String address,
        Integer totalCapacity,
        Boolean isDeleted) {
    public static LocationSummaryResponseDTO fromEntity(Location entity) {
        return new LocationSummaryResponseDTO(
                entity.getLocationId(),
                entity.getName(),
                entity.getAddress(),
                entity.getTotalCapacity(),
                entity.getIsDeleted());
    }
}
