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
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;

@Entity
@Table(name = "[organizer_member]")
@EntityListeners(AuditingEntityListener.class) // 用來自動填入 invited_at
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrganizerMember {

    @Id
    @Column(name = "member_id", columnDefinition = "CHAR(10)")
    private String memberId; // 主鍵，格式如 MBR0000001

    // ── 關聯的組織 ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer; // 屬於哪一個組織

    // ── 關聯的使用者成員 ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 對應的系統使用者帳號

    // ── 關聯的邀請者 ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "invited_by", nullable = false)
    private User invitedBy; // 誰發送的邀請

    // ── 角色 ──
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "role_id")
    private Role role;

    @Column(name = "invite_token", length = 255)
    private String inviteToken; // 邀請驗證用的 Token

    @Column(name = "invite_token_expires")
    private LocalDateTime inviteTokenExpires; // 邀請 Token 的過期時間

    @Builder.Default
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status", nullable = false)
    private OrganizerMemberStatus status = OrganizerMemberStatus.PENDING; // 邀請狀態：0=PENDING, 1=ACCEPTED, 2=REJECTED,
                                                                          // 3=REVOKED

    // ── 時間欄位 ──
    @CreatedDate
    @Column(name = "invited_at", nullable = false, updatable = false)
    private LocalDateTime invitedAt; // 邀請發送時間 (自動填入)

    @Column(name = "joined_at")
    private LocalDateTime joinedAt; // 實際加入組織的時間 (接受邀請時填入)
}
