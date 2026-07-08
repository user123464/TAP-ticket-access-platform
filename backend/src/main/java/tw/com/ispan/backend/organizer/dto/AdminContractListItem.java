package tw.com.ispan.backend.organizer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin 全平台合約清單列項，欄位對齊 frontend-admin {@code ContractList.vue}。
 *
 * <p>注意：此處的 enum 數字碼採「前端編碼」，與後端 entity ordinal 不同：
 * <ul>
 *   <li>contractType：1=公版（FREE_STANDARD / ANNUAL_FEE），2=客製（CUSTOM）</li>
 *   <li>feeType：1=百分比抽成（PERCENTAGE），2=每筆固定（FIXED_PER_TICKET）</li>
 *   <li>contractStatus：0=草稿，1=生效中，2=已到期，3=已終止</li>
 * </ul>
 * 轉換由 {@code AdminContractService} 負責，避免污染既有 B2B 用的 {@code ContractResponse}。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractListItem {
    private String id;            // 合約字串主鍵 CONxxxxxxx
    private String orgId;
    private String orgName;
    private Integer contractType; // 前端碼：1=公版 / 2=客製
    private Integer contractStatus; // 前端碼：0草稿/1生效/2到期/3終止
    private Integer feeType;      // 前端碼：1=百分比 / 2=固定
    private BigDecimal feeValue;
    private String version;       // 公版顯示版本快照（如 v2.1）；客製顯示 "custom"
    private LocalDateTime signedAt;
    private LocalDateTime expiresAt;
}
