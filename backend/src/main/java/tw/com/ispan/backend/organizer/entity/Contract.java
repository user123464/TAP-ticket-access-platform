package tw.com.ispan.backend.organizer.entity;

import java.math.BigDecimal;
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
import tw.com.ispan.backend.organizer.enums.ContractStatus;
import tw.com.ispan.backend.organizer.enums.ContractType;
import tw.com.ispan.backend.organizer.enums.FeeType;

@Entity
@Table(name = "[contract]")
@EntityListeners(AuditingEntityListener.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @Column(name = "contract_id", columnDefinition = "CHAR(10)")
    private String contractId; // CON0000001

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "contract_type", nullable = false)
    private ContractType contractType; // 0=FREE_STANDARD, 1=ANNUAL_FEE, 2=CUSTOM

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "fee_type", nullable = false)
    private FeeType feeType; // 0=PERCENTAGE, 1=FIXED_PER_TICKET

    @Column(name = "fee_value", nullable = false, precision = 10, scale = 4)
    private BigDecimal feeValue;

    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_to")
    private LocalDateTime validTo;

    @Builder.Default
    @Enumerated(EnumType.ORDINAL)
    @Column(name = "contract_status", nullable = false)
    private ContractStatus contractStatus = ContractStatus.DRAFT;

    @Column(name = "signed_at")
    private LocalDateTime signedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "signed_by_user_id")
    private User signedBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

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
