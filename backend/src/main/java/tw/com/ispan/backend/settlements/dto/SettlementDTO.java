package tw.com.ispan.backend.settlements.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementDTO {
    
    @JsonProperty("settlement_id")
    private String settlementId;

    @JsonProperty("revenue_type")
    private String revenueType; // "票務" 或 "商品"
    
    @JsonProperty("organizer_id")
    private String organizerId;
    
    @JsonProperty("organizer_name")
    private String organizerName;
    
    @JsonProperty("period_start")
    private LocalDate periodStart;
    
    @JsonProperty("period_end")
    private LocalDate periodEnd;
    
    @JsonProperty("total_orders_amount")
    private BigDecimal totalOrdersAmount;
    
    @JsonProperty("final_payout_amount")
    private BigDecimal finalPayoutAmount;
    
    @JsonProperty("item_status")
    private String itemStatus; // "已撥款" 或 "待處裡"
    
    @JsonProperty("processed_at")
    private LocalDateTime processedAt;
    
    @JsonProperty("created_at")
    private LocalDateTime createdAt;
}
