package tw.com.ispan.backend.location.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.location.entity.Seat;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    // 配合前端：一次撈出整個場館的所有物理座位 (畫 SeatMap 網格必備)
    // 跨兩層表查詢：Seat -> Zone -> Location -> locationId
    List<Seat> findByZone_Location_LocationId(Integer locationId);

    // 配合前端：當管理員在右側點擊 🗑️ 刪除某個分區時，必須連帶清空底下的座位
    void deleteByZone_ZoneId(Integer zoneId);

    // 高效的批次刪除功能
    @Modifying
    @Query("DELETE FROM Seat s WHERE s.zone.location.locationId = :locationId")
    void deleteSeatsByLocationId(@Param("locationId") Integer locationId);

    // 找出某個分區底下，所有實體座位
    // 用途：當我們知道搖滾區被綁定成 4500 元時，用這個方法把搖滾區所有的椅子全撈出來印成票。
    List<Seat> findByZone_ZoneId(Integer zoneId);

}
