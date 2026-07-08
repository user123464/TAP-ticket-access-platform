package tw.com.ispan.backend.login.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.login.enums.LoginFailureReason;

@Entity
@Table(name = "login_attempt")
@EntityListeners(AuditingEntityListener.class) // 自動填入嘗試時間
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attempt_id")
    private Long attemptId; // 自增主鍵 (BIGINT)

    @Column(name = "email", nullable = false, length = 100)
    private String email; // 嘗試登入的信箱

    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress; // 嘗試登入的 IP 位址

    @Column(name = "success", nullable = false)
    private Boolean success; // 是否登入成功 (true=成功, false=失敗)

    @Enumerated(EnumType.ORDINAL) // 資料庫儲存數字 (0=WRONG_PASSWORD, 1=USER_NOT_FOUND...)
    @Column(name = "failure_reason")
    private LoginFailureReason failureReason; // 失敗原因 (若成功則為 null)

    @CreatedDate
    @Column(name = "attempted_at", nullable = false, updatable = false)
    private LocalDateTime attemptedAt; // 登入嘗試時間 (自動生成)
}
