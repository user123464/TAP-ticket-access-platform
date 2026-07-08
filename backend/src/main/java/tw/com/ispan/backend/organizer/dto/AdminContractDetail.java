package tw.com.ispan.backend.organizer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin 合約詳情，欄位對齊 frontend-admin {@code ContractDetail.vue}。
 * 數字碼採前端編碼（見 {@link AdminContractListItem}）。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminContractDetail {
    private String id;
    private String orgId;
    private String orgName;
    private Integer contractType; // 1=公版 / 2=客製
    private Integer contractStatus;
    private Integer feeType;
    private BigDecimal feeValue;
    private String version;
    private LocalDateTime signedAt;
    private String signedBy;      // 簽署人姓名
    private LocalDateTime expiresAt;
    private String pdfUrl;        // 客製合約：已簽署 PDF 下載連結（公版為 null）
    private String contentMd;     // 合約內容 markdown（版本快照 / 客製上傳內容）
}
