package tw.com.ispan.backend.location.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;

import tw.com.ispan.backend.location.entity.Seat;

// 巢狀的 Seat 回應
public record SeatResponseDTO(
        Long id,
        Integer rowNum,
        Integer seatNum,

        @JsonProperty("xIndex") // 確保回傳給前端是小寫 x
        Integer xIndex,

        @JsonProperty("yIndex") // 確保回傳給前端是小寫 y
        Integer yIndex) {
    public static SeatResponseDTO fromEntity(Seat entity) {
        return new SeatResponseDTO(
                entity.getSeatId(),
                entity.getRowNum(),
                entity.getSeatNum(),
                entity.getXIndex(),
                entity.getYIndex());
    }
}