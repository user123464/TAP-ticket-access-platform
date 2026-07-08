package tw.com.ispan.backend.login.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.login.enums.RevokedType;

@Entity
@Table(name = "user_session")
@EntityListeners(AuditingEntityListener.class) // 自動填入建立時間
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Long sessionId; // 自增主鍵 (BIGINT)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 登入的使用者帳號

    @Column(name = "token_jti", nullable = false, unique = true, length = 100)
    private String tokenJti; // JWT 唯一識別碼 (JTI)

    @Enumerated(EnumType.STRING) // 資料庫儲存字串（'B2C_FRONT', 'B2B_PORTAL', 'ADMIN_LOCAL'）
    @Column(name = "portal_type", nullable = false, length = 20)
    private PortalType portalType; // 登入的系統入口

    @Column(name = "ip_address", length = 45)
    private String ipAddress; // 登入時的 IP 位址

    @Column(name = "user_agent", length = 255)
    private String userAgent; // 瀏覽器 User-Agent 資訊

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt; // 登入時間 (自動生成)

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt; // Token 到期時間

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt; // 登出或被踢除時間 (可為空)

    @Enumerated(EnumType.ORDINAL) // 資料庫儲存數字（0=SELF, 1=ADMIN, 2=SYSTEM）
    @Column(name = "revoked_by")
    private RevokedType revokedBy; // 撤銷者類型 (可為空)
}
