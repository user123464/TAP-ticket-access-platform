package tw.com.ispan.backend.notification.entity;

import java.time.LocalDateTime;

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
import tw.com.ispan.backend.notification.enums.NotificationChannel;

@Entity
@Table(name = "[notification_template]")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationTemplate {

    @Id
    @Column(name = "template_id", length = 30)
    private String templateId;

    @Column(name = "template_code", length = 50, nullable = false, unique = true)
    private String templateCode;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "channel", nullable = false)
    private NotificationChannel channel;

    @Column(name = "subject", length = 150)
    private String subject;

    @Column(name = "body_template", nullable = false, columnDefinition = "NVARCHAR(MAX)")
    private String bodyTemplate;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Version
    @org.hibernate.annotations.Generated(event = {org.hibernate.generator.EventType.INSERT, org.hibernate.generator.EventType.UPDATE})
    @Column(name = "row_version", insertable = false, updatable = false, columnDefinition = "timestamp")
    private byte[] rowVersion;
}
