package tw.com.ispan.backend.system.entity;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.system.enums.JobStatus;

@Entity
@Table(name = "[scheduled_job]")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScheduledJob {

    @Id
    @Column(name = "job_id", length = 30)
    private String jobId;

    @Column(name = "job_code", length = 50, nullable = false, unique = true)
    private String jobCode;

    @Column(name = "description", length = 255)
    private String description;

    @Builder.Default
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "status_code", nullable = false)
    private JobStatus statusCode = JobStatus.IDLE;

    @Column(name = "last_run_at")
    private LocalDateTime lastRunAt;

    @Column(name = "next_run_at")
    private LocalDateTime nextRunAt;

    @Column(name = "error_msg", length = 255)
    private String errorMsg;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}
