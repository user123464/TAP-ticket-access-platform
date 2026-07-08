package tw.com.ispan.backend.organizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MyOrganizationResponse {
    private String id;        // ORG000000X
    private String name;      // 組織名稱
    private String taxId;     // 統編
    private String role;      // OWNER / ADMIN / FINANCE
    private Integer kycStatus;// 0=DRAFT, 1=PENDING...
    private String logo;      // Logo URL（取自 kyc_data_json.logo_url，可能為 null）
}
