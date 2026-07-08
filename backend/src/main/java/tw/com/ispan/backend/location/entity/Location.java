package tw.com.ispan.backend.location.entity;

import org.hibernate.annotations.SQLDelete;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "location")
@SQLDelete(sql = "UPDATE location SET is_deleted = 1 WHERE location_id = ?")
// 覆寫 delete 行為
// 讓呼叫 repository.delete() 時自動轉成 UPDATE 語法
public class Location {

    // [PK] 場館 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Integer locationId;

    // 場館名稱，如：台北小巨蛋
    @Column(name = "name", columnDefinition = "NVARCHAR(100)", nullable = false)
    private String name;

    // 最大容納人數或單位總數
    @Column(name = "total_capacity", nullable = false)
    private Integer totalCapacity = 0;

    // 地址
    @Column(name = "address", columnDefinition = "NVARCHAR(200)")
    private String address;

    // 網格畫布最大寬(maxX)
    @Column(name = "grid_max_x", nullable = false)
    private Integer gridMaxX = 0;

    // 網格畫布最大高(maxY)
    @Column(name = "grid_max_y", nullable = false)
    private Integer gridMaxY = 0;

    // SVG原始檔(物理基礎)
    @Column(name = "raw_svg", columnDefinition = "NVARCHAR(MAX)")
    private String rawSvg;

    // SVG工作檔(已帶有 data-zone-id)
    @Column(name = "bound_svg", columnDefinition = "NVARCHAR(MAX)")
    private String boundSvg;

    // 軟刪除標記 (0: 正常, 1: 已刪除)
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // -- 建立「Location (場地/場館)」資料表
    // CREATE TABLE location (
    // location_id INT IDENTITY(1,1) PRIMARY KEY, -- [PK] 場館 ID
    // name NVARCHAR(100) NOT NULL, -- 場館名稱，如：台北小巨蛋
    // total_capacity INT NOT NULL DEFAULT 0, -- 最大容納人數或單位總數
    // grid_max_x INT NOT NULL DEFAULT 0, -- 網格畫布最大寬(maxX)
    // grid_max_y INT NOT NULL DEFAULT 0, -- 網格畫布最大高(maxY)
    // raw_svg NVARCHAR(max) NULL, -- SVG原始檔(物理基礎)
    // bound_svg NVARCHAR(max) NULL, -- SVG工作檔(已帶有 data-zone-id)
    // is_deleted BIT NOT NULL DEFAULT 0 -- 軟刪除標記 (0: 正常, 1: 已刪除)
    // );
}
