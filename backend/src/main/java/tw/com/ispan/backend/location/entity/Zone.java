package tw.com.ispan.backend.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "zone")
public class Zone {

    // [PK] 分區 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "zone_id")
    private Integer zoneId;

    // [FK] 關聯場館 ID
    // 使用 @ManyToOne 建立與 Location 的多對一關聯
    // fetch = FetchType.LAZY 可以在查詢 Zone 時先不載入 Location，效能較佳
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    // 物理區域名稱，如：黃3B區
    @Column(name = "name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String name;

    // 分區顏色
    @Column(name = "color", columnDefinition = "CHAR(7)", nullable = false)
    private String color = "#ffffff";

    @Column(name = "seat_count")
    private Integer seatCount;

    // -- 建立「Zone (分區)」資料表
    // CREATE TABLE zone (
    // zone_id INT IDENTITY(1,1) PRIMARY KEY, -- [PK] 分區 ID
    // location_id INT NOT NULL, -- [FK] 關聯場館 ID
    // name NVARCHAR(50) NOT NULL, -- 物理區域名稱，如：黃3B區
    // color char(7) -- 分區顏色
    // );
}
