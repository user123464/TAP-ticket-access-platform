package tw.com.ispan.backend.location.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.location.entity.Zone;

public interface ZoneRepository extends JpaRepository<Zone, Integer> {

    // 配合前端：載入場地時，找出該「場館」底下的所有「分區」
    // 透過 _ 告訴 JPA：去 Zone 裡面的 location 屬性，找它的 locationId
    List<Zone> findByLocation_LocationId(Integer locationId);

    // 高效的批次刪除功能
    @Modifying
    @Query("DELETE FROM Zone z WHERE z.location.locationId = :locationId")
    void deleteZonesByLocationId(@Param("locationId") Integer locationId);
}
