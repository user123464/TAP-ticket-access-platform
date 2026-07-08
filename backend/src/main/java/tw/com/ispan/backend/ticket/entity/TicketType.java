package tw.com.ispan.backend.ticket.entity;

import java.math.BigDecimal;

import org.hibernate.annotations.SQLDelete;

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
import tw.com.ispan.backend.theme.entity.Theme;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ticket_type")
@SQLDelete(sql = "UPDATE ticket_type SET is_deleted = 1 WHERE ticket_type_id = ?")
// 將刪除變為更新軟刪除狀態
public class TicketType {

    // [PK] 票種 ID
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ticket_type_id")
    private Integer ticketTypeId;

    // [FK] 關聯活動主題 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    @Column(name = "name", columnDefinition = "NVARCHAR(50)", nullable = false)
    private String name;

    @Column(name = "price", precision = 10, scale = 2, nullable = false)
    private BigDecimal price;

    // 對齊資料庫防呆：確保一定有顏色
    @Column(name = "color", columnDefinition = "CHAR(7)", nullable = false)
    private String color = "#ef4444";

    // 刪除 isEnabled 欄位 (上架控制移交給 Mapping 表)
    // @Column(name = "is_enabled", nullable = false)
    // private Boolean isEnabled = true;

    // 軟刪除欄位 (搭配上面的 @SQLDelete)
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // -- 建立「Ticket_Type (票種)」資料表
    // CREATE TABLE ticket_type (
    // ticket_type_id INT IDENTITY(1,1) PRIMARY KEY, -- [PK] 票種 ID（自增主鍵）
    // theme_id INT NOT NULL, -- [FK] 關聯活動主題 ID
    // name NVARCHAR(50) NOT NULL, -- 廠商自訂名稱，如：搖滾A區、3800看台
    // price DECIMAL(10, 2) NOT NULL, -- 票價金額
    // color CHAR(7), -- 票種顏色
    // is_deleted BIT DEFAULT 0, -- 軟刪除
    // );
}
