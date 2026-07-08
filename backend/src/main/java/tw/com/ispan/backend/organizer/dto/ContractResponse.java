package tw.com.ispan.backend.organizer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 組織合約查詢回應。
 * enum 一律以 ordinal（數字碼）回傳，與前端狀態對照一致：
 * contractType 0=FREE_STANDARD/1=ANNUAL_FEE/2=CUSTOM；
 * feeType 0=PERCENTAGE/1=FIXED_PER_TICKET；
 * contractStatus 0=DRAFT/1=ACTIVE/2=TERMINATED/3=EXPIRED。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContractResponse {
    private String contractId;
    private Integer contractType;
    private Integer feeType;
    private BigDecimal feeValue;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private Integer contractStatus;
    private LocalDateTime signedAt;
    private String signedByName;   // 簽署人姓名（取自 signed_by_user_id 關聯，可能為 null）
    private LocalDateTime createdAt;
}
