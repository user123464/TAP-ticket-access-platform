package tw.com.ispan.backend.ticket.entity;

import java.time.LocalDateTime;
import java.util.List;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.SequenceGenerator;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.location.entity.Seat;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.theme.entity.Session;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ticket", uniqueConstraints = {
        @UniqueConstraint(name = "UQ_Session_Seat", columnNames = { "session_id", "seat_id" })
})
public class Ticket {

    // [PK] 票券 ID（自增主鍵）
    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "ticket_seq_gen")
    @SequenceGenerator(name = "ticket_seq_gen", sequenceName = "ticket_seq", allocationSize = 50)
    @Column(name = "ticket_id")
    private Long ticketId;

    // [FK] 關聯場次 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false)
    private Session session;

    // [FK] 關聯物理座位 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    // [FK] 關聯廠商票種 ID
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_type_id")
    private TicketType ticketType;

    // 狀態 (移交給 enum TicketStatus)
    // 預設為 1 (AVAILABLE 可售)
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private TicketStatus status = TicketStatus.AVAILABLE;

    // 樂觀鎖版號（搭配 JPA @Version），防超賣機制
    @Version
    @Column(name = "version", nullable = false)
    private Integer version = 0;

    // [FK] 鎖定者 ID（防惡意劫持，記錄被誰鎖定）
    @Column(name = "locked_by_user", columnDefinition = "CHAR(10)")
    private String lockBy;

    // 鎖定到期時間（寫入當下 +15 分鐘，超時未轉 3: 已售出 由排程還原庫存）
    @Column(name = "locked_until")
    private LocalDateTime lockUntil;

    // 偉宏協助新增一對多關聯 : 票務訂單明細
    @OneToMany(mappedBy = "ticketTicket", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<TicketOrderDetailBean> ticketOrderDetails;

    // -- 建立「Ticket (票券庫存)」資料表 (場次 + 座位 + 票種)
    // CREATE TABLE ticket (
    // ticket_id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR ticket_seq, -- [PK] 票券
    // ID（自增主鍵）
    // session_id INT NOT NULL, -- [FK] 關聯場次 ID
    // -- 這裡必須跟著 Seat 表升級成 BIGINT
    // seat_id BIGINT NOT NULL, -- [FK] 關聯物理座位 ID
    // ticket_type_id INT, -- [FK] 關聯廠商票種 ID
    // status TINYINT DEFAULT 1, -- 狀態 (1: 可售, 2: 鎖定中, 3: 已售出, 0: 硬體保留/不可見)
    // version INT DEFAULT 0, -- 樂觀鎖版號（搭配 JPA @Version），防超賣機制
    // locked_by_user CHAR(10), -- [FK] 鎖定者 ID（防惡意劫持，記錄被誰鎖定）
    // locked_until DATETIME2 NULL -- 鎖定到期時間（寫入當下 +10 分鐘，超時未轉 3: 已售出 由排程還原庫存）
    // CONSTRAINT UQ_Session_Seat UNIQUE (session_id, seat_id)
    // );
}
