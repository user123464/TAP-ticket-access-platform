package tw.com.ispan.backend.location.dto.response;

import java.util.List;

import tw.com.ispan.backend.location.entity.Seat;
import tw.com.ispan.backend.location.entity.Zone;

// 巢狀的 Zone 回應
public record ZoneResponseDTO(
        Integer id,
        String name,
        String color,
        Integer seatCount,
        List<SeatResponseDTO> seats

) {
    // 這裡不自動從 entity 抓 seats，改由 Service 傳入
    public static ZoneResponseDTO fromEntity(Zone entity, List<Seat> seatsInThisZone) {
        return new ZoneResponseDTO(
                entity.getZoneId(),
                entity.getName(),
                entity.getColor(),
                entity.getSeatCount(),
                seatsInThisZone.stream().map(SeatResponseDTO::fromEntity).toList());
    }
}
