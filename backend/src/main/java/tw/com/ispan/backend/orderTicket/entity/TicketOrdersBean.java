package tw.com.ispan.backend.orderTicket.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.backend.login.entity.User;

/**
 * 門票訂單主檔實體類 (ORMapping)。
 * 
 * <p>
 * 對應資料庫 table: {@code ticket_orders}。
 * 儲存購票訂單基本資訊（如編號、聯絡人資訊、支付總額、付款狀態與訂單建立時間）。
 * </p>
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "ticket_orders")
@Data
public class TicketOrdersBean {

    // 票務訂單編號 (CHAR(10) 固定長度)
    @Id
    @Column(name = "t_order_id", columnDefinition = "CHAR(10)")
    private String tOrderId;

    // 【多對一】關聯購票會員 (User 表)，使用 Lazy 延遲加載優化
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "CHAR(10)")
    private User userId;

    // 消費者支付總金額 (定點小數，保留兩位小數，如 1000.00)
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // 購票聯絡人姓名
    @Column(name = "contact_name", nullable = false, length = 50)
    private String contactName;

    // 購票聯絡人電子信箱
    @Column(name = "contact_email", nullable = false, length = 100)
    private String contactEmail;

    // 購票聯絡人手機號碼
    @Column(name = "contact_phone", nullable = false, length = 20)
    private String contactPhone;

    // 付款狀態 (UNPAID 待付款, PAID 已付款, FAILED 支付失敗, EXPIRED 訂單逾期)
    @Column(name = "payment_status", length = 20)
    private String paymentStatus = "UNPAID";

    // 訂單付款失敗時間 (交易失敗時觸發並記錄時間)
    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    // 訂單建立時間 (由資料庫預設時間戳記自動寫入，不允許程式手動 INSERT 或 UPDATE)
    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createAt;

    /**
     * 綠界交易編號 (MerchantTradeNo)
     *
     * 每次建立付款時產生，
     * 綠界付款完成後會透過 Callback 回傳此欄位，
     * 用來查詢並更新對應訂單。
     */
    @Column(name = "merchant_trade_no", length = 20, unique = true)
    private String merchantTradeNo;

    // 【一對多】關聯門票明細清單。採用級聯保存/更新，便於與主檔一同保存
    @OneToMany(mappedBy = "ticketOrder", cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<TicketOrderDetailBean> orderDetail;

    /**
     * 在實體寫入資料庫前，若未設定 ID，自動隨機產生一個 10 碼的大寫字母/數字 ID。
     * 
     * <p>
     * 這作為對外的訂單編號 (例如: E9A4B2960E)，方便用戶記憶和查驗。
     * </p>
     */
    @PrePersist
    public void generateCustomId() {

        if (this.tOrderId == null) {
            this.tOrderId = UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();
        }

        if (this.merchantTradeNo == null) {
            this.merchantTradeNo = "TAP" + System.currentTimeMillis();
        }
    }
}
