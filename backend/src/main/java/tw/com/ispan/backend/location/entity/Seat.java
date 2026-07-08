package tw.com.ispan.backend.location.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "seat", uniqueConstraints = {
        // 防護 1：同一個分區內，絕對不可能出現兩張 "1排1號" 的票
        @UniqueConstraint(name = "UQ_Zone_Seat_Num", columnNames = { "zone_id", "row_num", "seat_num" }),
        // 防護 2：同一個分區內，網格的 X, Y 座標絕對不能疊在一起
        @UniqueConstraint(name = "UQ_Zone_Grid_Index", columnNames = { "zone_id", "x_index", "y_index" })
})
public class Seat {

    // [PK] 座位 ID
    @Id
    // 策略改為 SEQUENCE，並指定一個生成器名稱
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "seat_seq_gen")
    // 2. 定義生成器：對應資料庫的 seat_seq。
    // 效能參數 allocationSize：這決定了 Batch 的大小，
    // 設為 50 代表 Hibernate 會一次跟 DB 拿 50 個 ID放記憶體，
    // 然後一次打包 50 個 INSERT 丟進資料庫。
    @SequenceGenerator(name = "seat_seq_gen", sequenceName = "seat_seq", allocationSize = 50)
    @Column(name = "seat_id")
    private Long seatId; // 因為 SQL 改成 BIGINT，這裡的型態要改成 Long

    // [FK] 關聯物理分區 ID
    // fetch = FetchType.LAZY 可以在查詢 Seat 時先不載入 Zone，效能較佳
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    // 排數，如：15
    @Column(name = "row_num", nullable = false)
    private Integer rowNum;

    // 座位號碼，如：30
    @Column(name = "seat_num", nullable = false)
    private Integer seatNum;

    // 座位網格CSS Grid X 座標
    @Column(name = "x_index", nullable = false)
    private Integer xIndex;

    // 座位網格CSS Grid Y 座標
    @Column(name = "y_index", nullable = false)
    private Integer yIndex;

    // -- 建立「Seat (座位)」資料表
    // CREATE TABLE seat (
    // seat_id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR seat_seq, -- [PK] 座位 ID
    // zone_id INT NOT NULL, -- [FK] 關聯物理分區 ID
    // row_num INT NOT NULL, -- 排數，如：15
    // seat_num INT NOT NULL, -- 座位號碼，如：30
    // x_index INT INT NOT NULL, -- 座位網格CSS Grid X 座標
    // y_index INT INT NOT NULL -- 座位網格CSS Grid Y 座標

    // -- [防護] 確保同分區沒有重複的排號，且網格位置不重疊
    // CONSTRAINT UQ_Zone_Seat_Num UNIQUE (zone_id, row_num, seat_num),
    // CONSTRAINT UQ_Zone_Grid_Index UNIQUE (zone_id, x_index, y_index)
    // );
}
