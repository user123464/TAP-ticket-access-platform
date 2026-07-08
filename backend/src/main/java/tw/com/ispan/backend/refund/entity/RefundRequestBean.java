package tw.com.ispan.backend.refund.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.backend.refund.enums.RefundRequestStatus;
import tw.com.ispan.backend.refund.enums.RefundRequestType;

/**
 * 退款申請單實體 (ORMapping)。
 *
 * <p>對應資料庫 table: {@code refund_request}。
 * 退票/退貨審核制：會員送出申請 (PENDING)，主辦方於 B2B 後台核准 (APPROVED，
 * 此時才呼叫既有退款服務執行實際退款) 或駁回 (REJECTED)。
 * 票務與商城共用同一張表，以 requestType 區分。</p>
 */
@Entity
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "refund_request")
@Data
public class RefundRequestBean {

    // 申請單流水號
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "request_id")
    private Integer requestId;

    // 申請類型 (TICKET 退票 / MERCH 退貨)
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 10)
    private RefundRequestType requestType;

    // 訂單主檔 ID (tOrderId 或 mOrderId)
    @Column(name = "order_id", nullable = false, columnDefinition = "CHAR(10)")
    private String orderId;

    // 訂單明細 ID (tDetailId 或 mDetailId)
    @Column(name = "detail_id", nullable = false, length = 50)
    private String detailId;

    // 申請會員 userId (不建 FK 關聯，僅存識別碼)
    @Column(name = "user_id", nullable = false, columnDefinition = "CHAR(10)")
    private String userId;

    // 主辦方 organizerId (建立申請時由訂單關聯鏈推得，供 B2B 端過濾)
    @Column(name = "organizer_id", nullable = false, columnDefinition = "CHAR(10)")
    private String organizerId;

    // 申退理由
    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    // 審核狀態
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 10)
    private RefundRequestStatus status = RefundRequestStatus.PENDING;

    // 駁回原因 (駁回時填寫，可空)
    @Column(name = "reject_note", length = 500)
    private String rejectNote;

    // 申請時間
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 審核時間 (核准/駁回時寫入)
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
        if (this.status == null) {
            this.status = RefundRequestStatus.PENDING;
        }
    }
}
