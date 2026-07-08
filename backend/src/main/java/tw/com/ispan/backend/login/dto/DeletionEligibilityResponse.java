package tw.com.ispan.backend.login.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 帳號刪除資格檢查結果。
 * allowed=false 時，reason 說明原因，blockingOrgs 列出使用者仍為運行中所有權人的組織清單，
 * 供前端顯示並引導使用者先轉移所有權或註銷組織。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeletionEligibilityResponse {
    private boolean allowed;
    private String reason;
    private List<BlockingOrg> blockingOrgs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BlockingOrg {
        private String orgId;
        private String orgName;
    }
}
