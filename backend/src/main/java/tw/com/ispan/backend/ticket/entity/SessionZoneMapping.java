package tw.com.ispan.backend.ticket.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.location.entity.Zone;
import tw.com.ispan.backend.theme.entity.Session;

@Getter
@Setter
@NoArgsConstructor
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "session_zone_mapping", uniqueConstraints = {
        // 防呆：同一場次、同一區，只能有一種票種規則
        @UniqueConstraint(name = "UQ_Session_Zone", columnNames = { "session_id", "zone_id" })
})
// 新增 SQLDelete，因為這個綁定規則也有可能被廠商刪除 (解除綁定)
@SQLDelete(sql = "UPDATE session_zone_mapping SET is_deleted = 1 WHERE mapping_id = ?")
public class SessionZoneMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mapping_id")
    private Long mappingId;// 這裡用 Long (對應 DB 的 BIGINT)

    // 與場次的關聯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    // 與分區的關聯
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "zone_id", nullable = false)
    private Zone zone;

    // 因為票種是商業核心，這裡我們用 ManyToOne 關聯起來，方便取用 Name 和 Price
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id", nullable = false)
    private TicketType ticketType;

    // 控制消費者是否能看到/購買
    @Column(name = "is_enabled", nullable = false)
    private Boolean isEnabled = true;

    // 控制後台是否還要載入這條綁定規則
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    // 稽核欄位
    @CreatedDate
    @Column(name = "created_at", updatable = false) // 建立後就不該被修改
    protected LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", columnDefinition = "CHAR(10)", updatable = false)
    protected String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    protected LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", columnDefinition = "CHAR(10)")
    protected String updatedBy;

    // -- 分區-票種 中介表
    // CREATE TABLE session_zone_mapping(
    // mapping_id BIGINT IDENTITY(1,1) PRIMARY KEY, --唯一識別碼
    // session_id INT NOT NULL, --[FK]對應演唱會場次ID
    // zone_id INT NOT NULL, --[FK]對應實體分區ID
    // ticket_type_id INT NOT NULL, --[FK]對應票種ID
    // is_enabled BIT NOT NULL DEFAULT 1, -- 票種是否上架 (0: 前端完全不顯示, 1: 上架)
    // is_deleted BIT NOT NULL DEFAULT 0, -- 軟刪除標記 (0: 正常, 1: 已刪除)
    // --審計欄位 是誰/何時改了票價綁定--
    // created_at DATETIME2, -- 建立時間
    // created_by CHAR(10), -- [FK] 建立人
    // updated_at DATETIME2, -- 最後更新時間
    // updated_by CHAR(10)
    // };
}
