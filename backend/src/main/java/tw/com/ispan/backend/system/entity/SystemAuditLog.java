package tw.com.ispan.backend.system.entity;

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
import tw.com.ispan.backend.system.enums.AuditActionType;

@Entity
@Table(name = "[system_audit_log]")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "audit_id")
    private Long auditId;

    @Column(name = "action_user_id", columnDefinition = "CHAR(10)")
    private String actionUserId;

    @Column(name = "tenant_id", columnDefinition = "CHAR(10)")
    private String tenantId;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "action_type", nullable = false)
    private AuditActionType actionType;

    @Column(name = "action_detail", length = 100, nullable = false)
    private String actionDetail;

    @Column(name = "target_table", length = 50, nullable = false)
    private String targetTable;

    @Column(name = "target_id", length = 50, nullable = false)
    private String targetId;

    @Column(name = "old_value", columnDefinition = "NVARCHAR(MAX)")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "NVARCHAR(MAX)")
    private String newValue;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
