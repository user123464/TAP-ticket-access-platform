package tw.com.ispan.backend.orderTicket.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import tw.com.ispan.backend.ticket.entity.Ticket;

/**
 * 門票訂單明細實體類 (ORMapping)。
 * 
 * <p>對應資料庫 table: {@code ticket_order_details}。
 * 紀錄購買的每張門票、成交價、實名制入場人資訊、隨機二維碼 Hash、核銷狀態及核銷時間等。</p>
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ticket_order_details")
@Data
public class TicketOrderDetailBean {

    // 明細流水號 ID (使用隨機 UUID String，長度限制 50)
    @Id
    @Column(name = "t_detail_id", length = 50)
    private String tDetailId;

    // 【多對一】關聯門票訂單主檔 (ticket_orders)
    // 設置 FetchType.LAZY 避免 N+1 效能問題
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "t_order_id", nullable = false)
    private TicketOrdersBean ticketOrder;

    // 【多對一】關聯物理票券實體庫存 (ticket)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ticket_id", nullable = false)
    private Ticket ticketTicket;

    // 購買時的成交單價 (防止廠商後續修改票種定價而導致財務歷史錯誤)
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // 實名制：入場人姓名
    @Column(name = "real_name", nullable = false, length = 255)
    private String realName;

    // 實名制：證件字號 (身份證/護照等)
    @Column(name = "identity_number", nullable = false, length = 20)
    private String identityNumber;

    // 門票交易狀態 (存 Enum String 於資料庫: NORMAL 正常, REFUNDED 已退票)
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status")
    private TicketOrderStatus itemStatus = TicketOrderStatus.NORMAL;

    // 驗票專用 QR Code 雜湊值 (唯一約束，長度 255)
    @Column(name = "qr_code_hash", unique = true, length = 255)
    private String qrCodeHash;

    // 現場核銷狀態 (存 Enum String 於資料庫: Unredeemed 未核銷, Redeemed 已核銷, Canceled 已取消)
    @Enumerated(EnumType.STRING)
    @Column(name = "is_used")
    private TicketOrderUse isUsed = TicketOrderUse.Unredeemed;

    // 核銷時間紀錄 (現場核銷成功時寫入)
    @Column(name = "used_at")
    private LocalDateTime usedAt;

    /**
     * 在實體寫入資料庫前，若未設定 ID，自動生成隨機的 UUID 作為主鍵字串。
     */
    @PrePersist
    public void generatedCustomId() {
        if (this.tDetailId == null) {
            this.tDetailId = UUID.randomUUID().toString();
        }
    }
}
