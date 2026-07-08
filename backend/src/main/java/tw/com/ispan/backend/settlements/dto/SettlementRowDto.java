package tw.com.ispan.backend.settlements.dto;

import java.math.BigDecimal;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin 廠商手續費明細列，欄位對齊 frontend-admin {@code FeeSettlement.vue}。
 * feeType 採前端編碼：1=百分比抽成，2=每筆固定。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementRowDto {
    private String orgId;
    private String orgName;
    private long orderCount;
    private BigDecimal gmv;       // 期間交易總額
    private Integer feeType;      // 1=百分比 / 2=固定
    private BigDecimal feeValue;  // 費率值（% 或 元/筆）
    private BigDecimal fee;       // 平台應收手續費
}
