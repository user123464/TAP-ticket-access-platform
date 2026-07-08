package tw.com.ispan.backend.subscription.dto;

import java.math.BigDecimal;
import java.util.Set;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembershipPlanDto {
    private String planId;
    private String planName;
    private BigDecimal annualFee;
    private BigDecimal cumulativeThreshold;
    private BigDecimal defaultFeeRate;
    private String description;
    private Boolean isActive;
    private Set<String> features;
    private java.util.List<String> marketingHighlights;
}
