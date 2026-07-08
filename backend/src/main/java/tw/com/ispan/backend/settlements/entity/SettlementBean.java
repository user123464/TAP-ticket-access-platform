package tw.com.ispan.backend.settlements.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.settlements.enums.SettlementEnum;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "merchant_settlements")
@Data
public class SettlementBean {

    // 結算帳單流水號
    @Id
    @Column(name = "settlement_id", length = 50)
    private String settlementId;

    // 關聯結算對象
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "organizer_id", nullable = false, columnDefinition = "CHAR(10)")
    private Organizer organizerOrganizer;

    // 帳期起
    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    // 帳期止
    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    // 該期間總營業額
    @Column(name = "total_orders_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalOrdersAmount;

    // 實際撥款金額
    @Column(name = "final_payout_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal finalPayoutAmount;

    // 撥款狀態 (存 Enum String: PENDING 待處理, PAID 已撥款)
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    private SettlementEnum itemStatus = SettlementEnum.PENDING; // PENDING: 待處理, PAID: 已撥款

    // 撥款完成時間
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    // 訂單建立時間
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}
