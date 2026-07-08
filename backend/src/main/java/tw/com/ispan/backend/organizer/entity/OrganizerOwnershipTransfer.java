package tw.com.ispan.backend.organizer.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.organizer.enums.OrganizerTransferStatus;

/**
 * 組織所有權轉移紀錄。
 *
 * <p>
 * owner 發起轉移時建立一筆 PENDING 並寄送認證信給被轉移人；被轉移人點擊信中連結
 * （accept-transfer）後才真正易主。沿用組織邀請（{@link OrganizerMember} 的
 * invite_token）相同的 token + email 認證模式，但獨立成表以保留稽核軌跡。
 * </p>
 */
@Entity
@Table(name = "[organizer_ownership_transfer]")
@EntityListeners(AuditingEntityListener.class) // 自動填入 created_at
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerOwnershipTransfer {

    @Id
    @Column(name = "transfer_id", columnDefinition = "CHAR(10)")
    private String transferId; // 主鍵，格式如 TRF0000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer; // 被轉移的組織

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "from_user_id", nullable = false)
    private User fromUser; // 發起轉移的現任 owner

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "to_user_id", nullable = false)
    private User toUser; // 被轉移的目標成員

    @Column(name = "token", length = 255, nullable = false)
    private String token; // email 認證 Token

    @Column(name = "token_expires", nullable = false)
    private LocalDateTime tokenExpires; // Token 過期時間

    @Builder.Default
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private OrganizerTransferStatus status = OrganizerTransferStatus.PENDING;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
