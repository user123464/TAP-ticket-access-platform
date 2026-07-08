package tw.com.ispan.backend.subscription.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "[membership_plan]")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlan {

    @Id
    @Column(name = "plan_id", length = 30)
    private String planId;

    @Column(name = "plan_name", length = 50, nullable = false)
    private String planName;

    @Column(name = "annual_fee", precision = 10, scale = 2)
    private BigDecimal annualFee;

    @Column(name = "cumulative_threshold", precision = 15, scale = 2)
    private BigDecimal cumulativeThreshold;

    @Column(name = "default_fee_rate", precision = 5, scale = 2)
    private BigDecimal defaultFeeRate;

    @Column(name = "description", length = 500)
    private String description;

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

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "plan_feature",
        joinColumns = @JoinColumn(name = "plan_id"),
        inverseJoinColumns = @JoinColumn(name = "feature_id")
    )
    @Builder.Default
    private Set<SaasFeature> features = new HashSet<>();
}
