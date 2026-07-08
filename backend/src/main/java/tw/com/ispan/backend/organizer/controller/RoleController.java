package tw.com.ispan.backend.organizer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.dto.CreateRoleRequest;
import tw.com.ispan.backend.organizer.dto.RoleResponse;
import tw.com.ispan.backend.organizer.dto.RoleTemplateResponse;
import tw.com.ispan.backend.organizer.service.RoleService;

@Slf4j
@RestController
@RequestMapping("/api/organizer/{organizerId}/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    /**
     * 獲取組織所有的角色清單
     */
    @GetMapping
    public ResponseEntity<?> getRoles(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 查詢組織角色列表，操作者: {}, 組織: {}", userId, organizerId);
        try {
            List<RoleResponse> roles = roleService.getRolesForOrganizer(userId, organizerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", roles
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 獲取組織可用的預設角色模板清單
     */
    @GetMapping("/templates")
    public ResponseEntity<?> getRoleTemplates(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 查詢組織預設角色模板，操作者: {}, 組織: {}", userId, organizerId);
        try {
            List<RoleTemplateResponse> templates = roleService.getRoleTemplates(userId, organizerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", templates
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 建立自訂角色
     */
    @PostMapping
    public ResponseEntity<?> createRole(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @Valid @RequestBody CreateRoleRequest request) {
        String userId = userDetails.getUserId();
        log.info("API 建立自訂角色，操作者: {}, 組織: {}", userId, organizerId);
        try {
            RoleResponse newRole = roleService.createRole(userId, organizerId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "data", newRole,
                    "message", "自訂角色建立成功"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新自訂角色的權限
     */
    @PutMapping("/{roleId}/permissions")
    public ResponseEntity<?> updateRolePermissions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @PathVariable String roleId,
            @RequestBody Map<String, List<String>> body) {
        String userId = userDetails.getUserId();
        List<String> permissionIds = body.get("permissions");
        log.info("API 更新角色權限，操作者: {}, 角色: {}", userId, roleId);
        try {
            roleService.updateRolePermissions(userId, organizerId, roleId, permissionIds);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "角色權限更新成功"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新自訂角色名稱
     */
    @PutMapping("/{roleId}/name")
    public ResponseEntity<?> updateRoleName(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @PathVariable String roleId,
            @RequestBody Map<String, String> body) {
        String userId = userDetails.getUserId();
        String newName = body.get("name");
        log.info("API 更新角色名稱，操作者: {}, 角色: {}, 新名稱: {}", userId, roleId, newName);
        try {
            roleService.updateRoleName(userId, organizerId, roleId, newName);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "角色名稱更新成功"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 批量更新所有角色的權限配置矩陣
     */
    @PutMapping("/permissions/bulk")
    public ResponseEntity<?> bulkUpdateRolePermissions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @RequestBody Map<String, List<String>> rolePermissionsMap) {
        String userId = userDetails.getUserId();
        log.info("API 批量更新角色權限，操作者: {}", userId);
        try {
            roleService.bulkUpdateRolePermissions(userId, organizerId, rolePermissionsMap);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "所有角色權限配置保存成功"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 刪除自訂角色
     */
    @DeleteMapping("/{roleId}")
    public ResponseEntity<?> deleteRole(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @PathVariable String roleId) {
        String userId = userDetails.getUserId();
        log.info("API 刪除角色，操作者: {}, 角色: {}", userId, roleId);
        try {
            roleService.deleteRole(userId, organizerId, roleId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "角色已成功刪除"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
