package tw.com.ispan.backend.organizer.service;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.organizer.dto.CreateRoleRequest;
import tw.com.ispan.backend.organizer.dto.PermissionDto;
import tw.com.ispan.backend.organizer.dto.RoleResponse;
import tw.com.ispan.backend.organizer.dto.RoleTemplateResponse;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.Permission;
import tw.com.ispan.backend.organizer.entity.Role;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.organizer.repository.PermissionRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;
import tw.com.ispan.backend.organizer.repository.RoleTemplateRepository;
import tw.com.ispan.backend.organizer.repository.RolePermissionTemplateRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class RoleService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final OrganizerRepository organizerRepository;
    private final OrganizerMemberRepository organizerMemberRepository;
    private final OrganizerService organizerService; // 供權限驗證使用
    private final RoleTemplateRepository roleTemplateRepository;
    private final RolePermissionTemplateRepository rolePermissionTemplateRepository;

    /**
     * 取得系統定義的所有 Permission
     */
    @Transactional(readOnly = true)
    public List<Permission> getAllPermissions() {
        return permissionRepository.findAll();
    }

    /**
     * 取得「組織自訂角色可用的權限天花板」。
     *
     * 集合 = 平台角色 'ORGANIZER' 所持有的 permission（data.sql 中 ORGANIZER 角色的
     * role_permission），
     * 涵蓋 ORG_* / EVENT_* / TICKET_TYPE_* / LOCATION_* / PROMOTION_* / MERCH_* /
     * ORDER_VIEW 等。
     * 組織自訂角色的權限不應超出此天花板。驗證（登入 + Owner/Admin）由 verifyOwnerOrAdmin 負責。
     */
    @Transactional(readOnly = true)
    public List<PermissionDto> getOrganizerPermissionCeiling(String userId, String organizerId) {
        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        Role organizerRole = roleRepository.findById("ORGANIZER")
                .orElseThrow(() -> new IllegalStateException("系統平台角色 ORGANIZER 不存在，請確認種子資料"));

        return organizerRole.getPermissions().stream()
                .sorted((a, b) -> a.getPermissionId().compareTo(b.getPermissionId()))
                .map(p -> PermissionDto.builder()
                        .permissionId(p.getPermissionId())
                        .resourceCode(p.getResourceCode())
                        .actionCode(p.getActionCode())
                        .description(p.getDescription())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 獲取組織所有的角色 (包含系統預設 + 組織自訂)
     */
    @Transactional(readOnly = true)
    public List<RoleResponse> getRolesForOrganizer(String userId, String organizerId) {
        log.info("查詢組織角色列表，組織ID: {}, 操作者: {}", organizerId, userId);

        // 權限檢查 (Owner or Admin)
        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到組織"));

        List<Role> roles = new java.util.ArrayList<>(roleRepository.findByOrganizer(org));
        roleRepository.findById("ORGANIZER").ifPresent(roles::add);

        return roles.stream().map(role -> RoleResponse.builder()
                .roleId(role.getRoleId())
                .roleName(role.getRoleName())
                .description(role.getDescription())
                .isEditable(role.getIsEditable())
                .permissions(role.getPermissions().stream()
                        .map(p -> p.getPermissionId())
                        .collect(Collectors.toList()))
                .build()).collect(Collectors.toList());
    }

    /**
     * 建立自訂角色
     */
    @Transactional
    public RoleResponse createRole(String userId, String organizerId, CreateRoleRequest request) {
        log.info("建立自訂角色，組織ID: {}, 角色名稱: {}", organizerId, request.getRoleName());

        organizerService.verifyOwnerOrAdmin(userId, organizerId);
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到組織"));

        // 採 schema/data.sql 規格：組織自訂角色 ID = "ROL" + 7 碼零補流水號 (如 ROL0000001)
        // 以 seq_ROL 序列產生，並對極端撞號情形做防禦性重試
        String roleId;
        int guard = 0;
        do {
            Long nextVal = roleRepository.getNextRoleSequenceValue();
            roleId = String.format("ROL%07d", nextVal);
            if (++guard > 100) {
                throw new IllegalStateException("產生角色流水號失敗：連續撞號，請聯絡系統管理員");
            }
        } while (roleRepository.existsById(roleId));

        Set<Permission> perms = new HashSet<>();
        if (request.getPermissions() != null) {
            perms = new HashSet<>(permissionRepository.findAllById(request.getPermissions()));
        }

        Role newRole = Role.builder()
                .roleId(roleId)
                .roleName(request.getRoleName())
                .description("")
                .organizer(org)
                .isEditable(true)
                .permissions(perms)
                .build();

        roleRepository.save(newRole);

        return RoleResponse.builder()
                .roleId(newRole.getRoleId())
                .roleName(newRole.getRoleName())
                .description(newRole.getDescription())
                .isEditable(newRole.getIsEditable())
                .permissions(
                        newRole.getPermissions().stream().map(p -> p.getPermissionId()).collect(Collectors.toList()))
                .build();
    }

    /**
     * 更新自訂角色權限
     */
    @Transactional
    public void updateRolePermissions(String userId, String organizerId, String roleId, List<String> permissionIds) {
        log.info("更新角色權限，組織ID: {}, 角色ID: {}", organizerId, roleId);

        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("找不到角色"));

        // 系統角色不允許編輯 (OWNER, ADMIN等)
        if (!role.getIsEditable() || role.getOrganizer() == null) {
            // 特例：FINANCE 可能在系統預設裡，但需要允許改權限。
            // 這裡為了簡化，只要 isEditable = true 就可改。
            if (!role.getIsEditable()) {
                throw new IllegalStateException("該角色不可修改");
            }
        } else if (!role.getOrganizer().getOrganizerId().equals(organizerId)) {
            throw new SecurityException("無權修改其他組織的角色");
        }

        Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(permissionIds));
        role.setPermissions(perms);
        roleRepository.save(role);
    }

    /**
     * 更新自訂角色名稱
     */
    @Transactional
    public void updateRoleName(String userId, String organizerId, String roleId, String newName) {
        log.info("更新角色名稱，組織ID: {}, 角色ID: {}, 新名稱: {}", organizerId, roleId, newName);
        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("找不到角色"));

        if (!role.getIsEditable() || role.getOrganizer() == null) {
            throw new IllegalStateException("該角色不可修改");
        } else if (!role.getOrganizer().getOrganizerId().equals(organizerId)) {
            throw new SecurityException("無權修改其他組織的角色");
        }

        if (newName == null || newName.trim().isEmpty()) {
            throw new IllegalArgumentException("角色名稱不能為空");
        }

        role.setRoleName(newName.trim());
        roleRepository.save(role);
    }

    /**
     * 批量更新角色權限 (前端一次儲存所有權限矩陣)
     */
    @Transactional
    public void bulkUpdateRolePermissions(String userId, String organizerId,
            Map<String, List<String>> rolePermissionsMap) {
        log.info("批量更新角色權限，組織ID: {}", organizerId);
        organizerService.verifyOwnerOrAdmin(userId, organizerId);
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到組織"));

        List<Role> roles = roleRepository.findByOrganizerOrSystemRoles(org);

        for (Role role : roles) {
            // 只有可編輯的角色允許變更權限
            if (role.getIsEditable() && rolePermissionsMap.containsKey(role.getRoleId())) {
                List<String> permIds = rolePermissionsMap.get(role.getRoleId());
                Set<Permission> perms = new HashSet<>(permissionRepository.findAllById(permIds));
                role.setPermissions(perms);
                roleRepository.save(role);
            }
        }
    }

    /**
     * 刪除自訂角色
     */
    @Transactional
    public void deleteRole(String userId, String organizerId, String roleId) {
        log.info("刪除角色，組織ID: {}, 角色ID: {}", organizerId, roleId);
        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("找不到角色"));

        if (!role.getIsEditable() || role.getOrganizer() == null
                || !role.getOrganizer().getOrganizerId().equals(organizerId)) {
            throw new IllegalStateException("無法刪除系統預設或不屬於該組織的角色");
        }

        // 檢查是否有任何成員正指派此角色，避免資料庫外鍵約束異常
        if (organizerMemberRepository.existsByRole(role)) {
            throw new IllegalStateException("此角色目前正被指派給組織成員，請先移轉成員角色再行刪除。");
        }

        roleRepository.delete(role);
    }

    /**
     * 取得組織可用的預設角色模板清單
     */
    @Transactional(readOnly = true)
    public List<RoleTemplateResponse> getRoleTemplates(String userId, String organizerId) {
        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        return roleTemplateRepository.findAll().stream()
                .map(t -> {
                    List<String> permIds = rolePermissionTemplateRepository.findByTemplateId(t.getTemplateId()).stream()
                            .map(rpt -> rpt.getPermission().getPermissionId())
                            .sorted()
                            .collect(Collectors.toList());
                    return RoleTemplateResponse.builder()
                            .templateId(t.getTemplateId())
                            .templateName(t.getTemplateName())
                            .description(t.getDescription())
                            .permissions(permIds)
                            .build();
                })
                .collect(Collectors.toList());
    }
}
