package tw.com.ispan.backend.organizer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Admin RBAC 角色列項，對齊 frontend-admin {@code RoleList.vue}。
 * 平台角色 id 與 code 同為 roleId 字串（如 SUPER_ADMIN）；模板則用 templateId。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRoleItem {
    private String id;
    private String code;
    private String name;
    private String description;
    private Long memberCount;     // 模板無此值時為 null
    private Integer permissionCount; // 模板無此值時為 null
    private Boolean editable;     // is_editable：可就地改名/權限/刪除
    private String category;      // SYSTEM（系統身分，唯讀）｜INTERNAL（內部人員角色，可編輯）
    private Boolean deletable;    // 非內建且無成員時可刪除（模板恆 null）
    private List<String> permissionIds; // 供前端權限選擇器預填
}
