package tw.com.ispan.backend.organizer.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerStatus;

@Entity
@Table(name = "[organizer]")
@EntityListeners(AuditingEntityListener.class) // 啟用 Spring Data JPA 的時間審計監聽器
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Organizer {

    @Id
    @Column(name = "organizer_id", columnDefinition = "CHAR(10)")
    private String organizerId; // 主鍵，格式如 ORG0000001

    // ── 核心關係映射 (ManyToOne) ──
    @ManyToOne(fetch = FetchType.LAZY) // 採用懶載入，避免查詢 Organizer 時自動撈出整個 User 導致效能損耗
    @JoinColumn(name = "owner_user_id", nullable = false) // 對應資料表中的 owner_user_id 欄位，且必填
    private User owner; // 組織擁有者

    @Column(name = "name", nullable = false, length = 100)
    private String name; // 廠商/組織名稱

    @Column(name = "tax_id", length = 20)
    private String taxId; // 統一編號 (可為空)

    @Builder.Default
    @Enumerated(EnumType.ORDINAL) // 資料庫儲存數字 (0, 1, 2)
    @Column(name = "status", nullable = false)
    private OrganizerStatus status = OrganizerStatus.ACTIVE; // 預設為 ACTIVE

    @Builder.Default
    @Enumerated(EnumType.ORDINAL) // 資料庫儲存數字 (0, 1, 2, 3)
    @Column(name = "kyc_status", nullable = false)
    private KycStatus kycStatus = KycStatus.DRAFT; // 預設為 DRAFT

    @Column(name = "kyc_data_json", columnDefinition = "NVARCHAR(MAX)")
    private String kycDataJson; // KYC 審核所需要的 JSON 格式欄位

    @Column(name = "bank_account_info", columnDefinition = "NVARCHAR(MAX)")
    private String bankAccountInfo; // 撥款帳戶 (預留欄位，App 層加密儲存)

    @Column(name = "kyc_rejection_reason", length = 500)
    private String kycRejectionReason; // KYC 審核被拒絕的原因

    @Column(name = "kyc_reviewed_at")
    private LocalDateTime kycReviewedAt; // 審核通過/拒絕的時間

    @ManyToOne(fetch = FetchType.LAZY) // 同樣使用懶載入
    @JoinColumn(name = "kyc_reviewed_by") // 對應資料庫的 kyc_reviewed_by 欄位 (可為空，草稿時無審核人)
    private User kycReviewedBy; // KYC 審核人

    // ── 自動審計時間欄位 ──
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 建立時間 (自動填入，不允許更新)

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt; // 最後更新時間 (自動更新)

    // ── SQL Server 樂觀鎖 ──
    @Version
    @org.hibernate.annotations.Generated(event = {org.hibernate.generator.EventType.INSERT, org.hibernate.generator.EventType.UPDATE})
    @Column(name = "row_version", insertable = false, updatable = false, columnDefinition = "timestamp")
    private byte[] rowVersion; // 用於預防同時修改資料的樂觀鎖
}
