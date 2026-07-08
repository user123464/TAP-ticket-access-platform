package tw.com.ispan.backend.organizer.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.dto.AdminRoleItem;
import tw.com.ispan.backend.organizer.entity.Permission;
import tw.com.ispan.backend.organizer.entity.Role;
import tw.com.ispan.backend.organizer.entity.RolePermissionTemplate;
import tw.com.ispan.backend.organizer.entity.RoleTemplate;
import tw.com.ispan.backend.organizer.entity.SystemResource;
import tw.com.ispan.backend.organizer.repository.PermissionRepository;
import tw.com.ispan.backend.organizer.repository.RolePermissionTemplateRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;
import tw.com.ispan.backend.organizer.repository.RoleTemplateRepository;
import tw.com.ispan.backend.organizer.repository.SystemResourceRepository;

/**
 * Admin 後台「RBAC 平台層」業務邏輯（批次 2）。
 *
 * <p>重用既有 Role / Permission / SystemResource entity 與 repository，補上：
 * 平台角色清單 + 組織預設角色模板、角色名稱/說明更新、權限矩陣讀寫、三端選單資源讀寫。</p>
 *
 * <p><b>SUPER_ADMIN 保護</b>：矩陣回傳時強制 SUPER_ADMIN = 全部權限；
 * 矩陣更新時忽略前端對 SUPER_ADMIN 的任何變更並強制補回全集（後端把關，不只靠前端 disabled）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminRbacService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionTemplateRepository templateRepository;
    private final RoleTemplateRepository roleTemplateRepository;
    private final SystemResourceRepository systemResourceRepository;
    private final UserRepository userRepository;

    private static final String SUPER_ADMIN = "SUPER_ADMIN";

    /** 系統身分角色：自動指派、且 ORGANIZER 是 B2B 全廠商權限天花板 → Admin 後台唯讀，不可改名/權限/刪除。 */
    private static final Set<String> SYSTEM_IDENTITY_ROLES = Set.of(SUPER_ADMIN, "BUYER", "ORGANIZER");
    /** 內建平台角色：含內部人員預設角色，可編輯權限但不可刪除（避免員工建立流程失去預設角色）。 */
    private static final Set<String> BUILTIN_PLATFORM_ROLES =
            Set.of(SUPER_ADMIN, "BUYER", "ORGANIZER", "ADMIN", "CUSTOMER_SERVICE");

    // ── 角色清單 ──

    @Transactional(readOnly = true)
    public Map<String, Object> getRoles() {
        List<AdminRoleItem> platformRoles = roleRepository.findByOrganizerIsNull().stream()
                .map(r -> {
                    boolean editable = Boolean.TRUE.equals(r.getIsEditable());
                    boolean system = SYSTEM_IDENTITY_ROLES.contains(r.getRoleId());
                    long members = userRepository.countByRoleId(r.getRoleId());
                    return AdminRoleItem.builder()
                            .id(r.getRoleId())
                            .code(r.getRoleId())
                            .name(r.getRoleName())
                            .description(r.getDescription())
                            .memberCount(members)
                            .permissionCount(r.getPermissions() != null ? r.getPermissions().size() : 0)
                            .editable(editable)
                            .category(system ? "SYSTEM" : "INTERNAL")
                            .deletable(!BUILTIN_PLATFORM_ROLES.contains(r.getRoleId()) && editable && members == 0)
                            .permissionIds(r.getPermissions() == null ? List.of()
                                    : r.getPermissions().stream().map(p -> p.getPermissionId()).sorted().collect(Collectors.toList()))
                            .build();
                })
                .collect(Collectors.toList());

        List<AdminRoleItem> roleTemplates = new ArrayList<>();
        for (RoleTemplate t : roleTemplateRepository.findAll()) {
            List<String> permIds = templateRepository.findByTemplateId(t.getTemplateId()).stream()
                    .map(rpt -> rpt.getPermission().getPermissionId())
                    .sorted()
                    .collect(Collectors.toList());
            roleTemplates.add(AdminRoleItem.builder()
                    .id(t.getTemplateId())
                    .code(t.getTemplateId())
                    .name(t.getTemplateName())
                    .description(t.getDescription())
                    .permissionCount(permIds.size())
                    .permissionIds(permIds)
                    .build());
        }

        return Map.of("platformRoles", platformRoles, "roleTemplates", roleTemplates);
    }

    // ── 組織角色模板 CRUD（藍圖：只影響日後新建組織，不回溯既有組織） ──

    /** 新增模板；id 採 TPL+流水號（共用 seq_ROL，不撞角色 id）。 */
    @Transactional
    public String createTemplate(String name, String description, List<String> permissionIds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("模板名稱不可空白");
        }
        String templateId;
        int guard = 0;
        do {
            templateId = String.format("TPL%07d", roleRepository.getNextRoleSequenceValue());
            if (++guard > 100) {
                throw new IllegalStateException("產生模板流水號失敗：連續撞號");
            }
        } while (roleTemplateRepository.existsById(templateId));

        roleTemplateRepository.save(RoleTemplate.builder()
                .templateId(templateId)
                .templateName(name.trim())
                .description(description != null ? description.trim() : null)
                .build());
        replaceTemplatePermissions(templateId, permissionIds);
        log.info("Admin 新增組織角色模板 {}（{}）", templateId, name);
        return templateId;
    }

    /** 修改模板名稱/說明。 */
    @Transactional
    public void updateTemplate(String templateId, String name, String description) {
        RoleTemplate t = roleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("找不到模板: " + templateId));
        if (name != null && !name.isBlank()) {
            t.setTemplateName(name.trim());
        }
        if (description != null) {
            t.setDescription(description.trim());
        }
        roleTemplateRepository.save(t);
        log.info("Admin 更新模板 {} 名稱/說明", templateId);
    }

    /** 刪除模板（DB FK ON DELETE CASCADE 清橋接列；不動既有組織角色）。 */
    @Transactional
    public void deleteTemplate(String templateId) {
        RoleTemplate t = roleTemplateRepository.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("找不到模板: " + templateId));
        roleTemplateRepository.delete(t);
        log.info("Admin 刪除組織角色模板 {}", templateId);
    }

    /** 設定模板的預設權限（重寫 role_permission_template）。 */
    @Transactional
    public void updateTemplatePermissions(String templateId, List<String> permissionIds) {
        if (!roleTemplateRepository.existsById(templateId)) {
            throw new IllegalArgumentException("找不到模板: " + templateId);
        }
        replaceTemplatePermissions(templateId, permissionIds);
        log.info("Admin 更新模板 {} 預設權限（{} 項）", templateId, permissionIds == null ? 0 : permissionIds.size());
    }

    /** 重寫某模板的權限橋接列：清空後依現有權限碼重建（過濾未知碼）。 */
    private void replaceTemplatePermissions(String templateId, List<String> permissionIds) {
        // 先刪後增；必須 flush 強制 DELETE 先落地。否則 Hibernate 預設動作順序為「先 INSERT、後 DELETE」，
        // 當新舊權限有重疊時，重插的 (template_id, permission_id) 會在舊列刪除前先寫入，
        // 撞 UNIQUE(template_id, permission_id) 而拋 DataIntegrityViolationException。
        templateRepository.deleteAll(templateRepository.findByTemplateId(templateId));
        templateRepository.flush();
        if (permissionIds == null || permissionIds.isEmpty()) {
            return;
        }
        for (Permission p : resolvePermissions(permissionIds)) {
            templateRepository.save(RolePermissionTemplate.builder()
                    .templateId(templateId)
                    .permission(p)
                    .build());
        }
    }

    /** 全部權限（依 resourceCode 分組用），供前端權限選擇器。 */
    @Transactional(readOnly = true)
    public List<Map<String, String>> getAllPermissions() {
        return permissionRepository.findAll().stream()
                .map(p -> Map.of(
                        "id", p.getPermissionId(),
                        "description", p.getDescription() != null ? p.getDescription() : p.getPermissionId(),
                        "resourceCode", p.getResourceCode() != null ? p.getResourceCode() : "OTHER"))
                .collect(Collectors.toList());
    }

    /** 更新角色名稱/說明；系統身分角色與不可編輯角色拒絕。 */
    @Transactional
    public void updateRole(String roleId, String name, String description) {
        Role role = requireEditablePlatformRole(roleId);
        if (name != null && !name.isBlank()) {
            role.setRoleName(name.trim());
        }
        if (description != null) {
            role.setDescription(description.trim());
        }
        roleRepository.save(role);
        log.info("Admin 更新角色 {} 名稱/說明", roleId);
    }

    /** 新增自訂內部角色（organizer_id NULL、is_editable=1）。id 採 PRL+流水號（共用 seq_ROL，與 ROL 不撞）。 */
    @Transactional
    public String createPlatformRole(String name, String description, List<String> permissionIds) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("角色名稱不可空白");
        }
        String roleId;
        int guard = 0;
        do {
            Long nextVal = roleRepository.getNextRoleSequenceValue();
            roleId = String.format("PRL%07d", nextVal);
            if (++guard > 100) {
                throw new IllegalStateException("產生角色流水號失敗：連續撞號");
            }
        } while (roleRepository.existsById(roleId));

        Set<Permission> perms = resolvePermissions(permissionIds);
        Role role = Role.builder()
                .roleId(roleId)
                .roleName(name.trim())
                .description(description != null ? description.trim() : "")
                .organizer(null)
                .isEditable(true)
                .permissions(perms)
                .build();
        roleRepository.save(role);
        log.info("Admin 新增內部角色 {}（{}）", roleId, name);
        return roleId;
    }

    /** 設定單一角色權限；系統身分與不可編輯角色拒絕。 */
    @Transactional
    public void updateRolePermissions(String roleId, List<String> permissionIds) {
        Role role = requireEditablePlatformRole(roleId);
        role.setPermissions(resolvePermissions(permissionIds));
        roleRepository.save(role);
        log.info("Admin 更新角色 {} 權限（{} 項）", roleId, permissionIds == null ? 0 : permissionIds.size());
    }

    /** 刪除自訂內部角色；內建角色、有成員指派者拒絕。 */
    @Transactional
    public void deletePlatformRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("找不到角色: " + roleId));
        if (BUILTIN_PLATFORM_ROLES.contains(roleId) || role.getOrganizer() != null
                || !Boolean.TRUE.equals(role.getIsEditable())) {
            throw new IllegalArgumentException("此角色為系統內建或非平台角色，不可刪除");
        }
        if (userRepository.countByRoleId(roleId) > 0) {
            throw new IllegalArgumentException("此角色仍被指派給員工，請先移除指派再刪除");
        }
        roleRepository.delete(role);
        log.info("Admin 刪除內部角色 {}", roleId);
    }

    /** 取可編輯的平台角色：須存在、屬平台層、is_editable=1（即排除系統身分角色）。 */
    private Role requireEditablePlatformRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("找不到角色: " + roleId));
        if (role.getOrganizer() != null) {
            throw new IllegalArgumentException("非平台角色，不可於此編輯");
        }
        if (SYSTEM_IDENTITY_ROLES.contains(roleId) || !Boolean.TRUE.equals(role.getIsEditable())) {
            throw new IllegalArgumentException("系統身分角色為唯讀，不可編輯");
        }
        return role;
    }

    /** 將權限碼解析為 Permission 集合，過濾未知碼。 */
    private Set<Permission> resolvePermissions(List<String> permissionIds) {
        if (permissionIds == null || permissionIds.isEmpty()) {
            return new HashSet<>();
        }
        return new HashSet<>(permissionRepository.findAllById(permissionIds));
    }

    // ── 選單資源 ──

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getResources(String portalParam) {
        PortalType portalType = mapPortal(portalParam);
        return systemResourceRepository.findByPortalTypeOrderBySortOrderAsc(portalType).stream()
                .map(this::toResourceDto)
                .collect(Collectors.toList());
    }

    /** 更新選單資源：持久化 label(name) / path(urlPath) / sortOrder / icon / visible。 */
    @Transactional
    public void updateResource(String resourceId, String label, String path, Integer sortOrder, String icon, Boolean visible) {
        SystemResource res = systemResourceRepository.findById(resourceId)
                .orElseThrow(() -> new IllegalArgumentException("找不到選單資源: " + resourceId));
        if (label != null && !label.isBlank()) {
            res.setName(label.trim());
        }
        if (path != null) {
            res.setUrlPath(path.isBlank() ? null : path.trim());
        }
        if (sortOrder != null) {
            res.setSortOrder(sortOrder);
        }
        if (icon != null) {
            res.setIcon(icon.isBlank() ? null : icon.trim());
        }
        if (visible != null) {
            res.setIsVisible(visible);
        }
        systemResourceRepository.save(res);
        log.info("Admin 更新選單資源 {}", resourceId);
    }

    private Map<String, Object> toResourceDto(SystemResource r) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", r.getResourceId());
        m.put("code", r.getResourceId());
        m.put("label", r.getName());
        m.put("icon", r.getIcon() != null ? r.getIcon() : "");
        m.put("path", r.getUrlPath() != null ? r.getUrlPath() : "");
        m.put("sortOrder", r.getSortOrder());
        m.put("visible", r.getIsVisible() == null || r.getIsVisible());
        return m;
    }

    /** 前端 portal 代碼（ADMIN_LOCAL / B2B / B2C）→ 後端 PortalType。 */
    private PortalType mapPortal(String portal) {
        if (portal == null) {
            return PortalType.ADMIN_LOCAL;
        }
        return switch (portal) {
            case "B2B", "B2B_PORTAL" -> PortalType.B2B_PORTAL;
            case "B2C", "B2C_FRONT" -> PortalType.B2C_FRONT;
            default -> PortalType.ADMIN_LOCAL;
        };
    }
}
