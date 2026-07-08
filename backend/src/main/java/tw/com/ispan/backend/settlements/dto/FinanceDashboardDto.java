package tw.com.ispan.backend.settlements.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin 財務宏觀儀表板回應，欄位對齊 frontend-admin {@code FinanceDashboard.vue}。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FinanceDashboardDto {

    private SeriesDto trend;     // 全平台月交易額趨勢（折線）
    private SeriesDto feeShare;  // 本月各廠商手續費佔比（甜甜圈）
    private BigDecimal monthlyFee; // 本月平台應收手續費
    private BigDecimal monthlyGmv; // 本月平台交易總額 (GMV)
    private SubscriptionSummary subscription;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeriesDto {
        private List<String> labels;
        private List<BigDecimal> data;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscriptionSummary {
        private long active;
        private long expiringSoon; // 30 天內到期
        private long expired;
    }
}
