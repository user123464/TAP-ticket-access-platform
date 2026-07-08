package tw.com.ispan.backend.login.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.SQLRestriction;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.organizer.entity.Role;

@Entity
@Table(name = "[user]")
@SQLDelete(sql = "UPDATE [user] SET is_deleted = 1 WHERE user_id = ? AND row_version = ?")
@SQLRestriction("is_deleted = 0")
@EntityListeners(AuditingEntityListener.class) // 啟用自動審計監聽
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @Column(name = "user_id", columnDefinition = "CHAR(10)")
    private String userId; // 主鍵，格式如 USR0000001

    @Column(name = "email", nullable = false, length = 100)
    private String email;

    @Column(name = "password_hash", length = 255)
    private String passwordHash;

    @Column(name = "name", nullable = false, length = 50)
    private String name;

    @Column(name = "phone", length = 20)
    private String phone;

    @Column(name = "gender", columnDefinition = "CHAR(1)")
    private String gender; // M=男, F=女, O=其他

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Column(name = "address", length = 255)
    private String address;

    @Column(name = "avatar_url", length = 255)
    private String avatarUrl;

    @Builder.Default
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Column(name = "google_oauth_id", length = 100)
    private String googleOauthId;

    @Column(name = "email_verified_at")
    private LocalDateTime emailVerifiedAt;

    @Column(name = "email_verify_token", length = 255)
    private String emailVerifyToken;

    @Column(name = "email_verify_expires")
    private LocalDateTime emailVerifyExpires;

    @Column(name = "pwd_reset_token", length = 255)
    private String pwdResetToken;

    @Column(name = "pwd_reset_expires")
    private LocalDateTime pwdResetExpires;

    @Column(name = "locked_until")
    private LocalDateTime lockedUntil;

    @Builder.Default
    @Column(name = "is_two_factor_enabled", nullable = false)
    private Boolean isTwoFactorEnabled = false;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Builder.Default
    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @Builder.Default
    @Column(name = "must_change_password", nullable = false)
    private Boolean mustChangePassword = false;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role", joinColumns = @JoinColumn(name = "user_id"), inverseJoinColumns = @JoinColumn(name = "role_id"))
    @Builder.Default
    private Set<Role> roles = new HashSet<>();

    @Version
    @org.hibernate.annotations.Generated(event = {org.hibernate.generator.EventType.INSERT, org.hibernate.generator.EventType.UPDATE})
    @Column(name = "row_version", insertable = false, updatable = false, columnDefinition = "timestamp")
    private byte[] rowVersion; // SQL Server RowVersion 用於樂觀鎖
}
