package tw.com.ispan.backend.organizer.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 權限資料傳輸物件。
 * 用於 GET /api/organizer/{organizerId}/permissions 回傳組織自訂角色的權限天花板。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PermissionDto {
    private String permissionId;
    private String resourceCode;
    private String actionCode;
    private String description;
}
