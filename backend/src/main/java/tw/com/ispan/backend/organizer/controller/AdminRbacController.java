package tw.com.ispan.backend.organizer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.organizer.service.AdminRbacService;

/**
 * Admin 後台「RBAC 平台層」REST 端點（批次 2）。
 *
 * <p>路徑前綴 {@code /api/admin/rbac}，授權 ROLE_VIEW / ROLE_MANAGE / RESOURCE_MANAGE。
 * 回傳格式 {@code {status:"success", data:...}}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/rbac")
@RequiredArgsConstructor
public class AdminRbacController {

    private final AdminRbacService adminRbacService;

    // ── 角色 ──

    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<?> getRoles() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminRbacService.getRoles()));
    }

    @PutMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> updateRole(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            adminRbacService.updateRole(id, body.get("name"), body.get("description"));
            return ResponseEntity.ok(Map.of("status", "success", "message", "角色已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PostMapping("/roles")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> createRole(@RequestBody Map<String, Object> body) {
        try {
            String name = body.get("name") != null ? body.get("name").toString() : null;
            String description = body.get("description") != null ? body.get("description").toString() : null;
            String id = adminRbacService.createPlatformRole(name, description, permissionIds(body));
            return ResponseEntity.ok(Map.of("status", "success", "message", "角色已建立", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/roles/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> updateRolePermissions(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            adminRbacService.updateRolePermissions(id, permissionIds(body));
            return ResponseEntity.ok(Map.of("status", "success", "message", "角色權限已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> deleteRole(@PathVariable String id) {
        try {
            adminRbacService.deletePlatformRole(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "角色已刪除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 權限清單（供權限選擇器） ──

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<?> getPermissions() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminRbacService.getAllPermissions()));
    }

    // ── 組織角色模板（藍圖：只影響日後新建組織） ──

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> body) {
        try {
            String name = body.get("name") != null ? body.get("name").toString() : null;
            String description = body.get("description") != null ? body.get("description").toString() : null;
            String id = adminRbacService.createTemplate(name, description, permissionIds(body));
            return ResponseEntity.ok(Map.of("status", "success", "message", "模板已建立", "id", id));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("建立模板失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "建立模板失敗，請稍後再試"));
        }
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> updateTemplate(@PathVariable String id, @RequestBody Map<String, String> body) {
        try {
            adminRbacService.updateTemplate(id, body.get("name"), body.get("description"));
            return ResponseEntity.ok(Map.of("status", "success", "message", "模板已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> deleteTemplate(@PathVariable String id) {
        try {
            adminRbacService.deleteTemplate(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "模板已刪除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/templates/{id}/permissions")
    @PreAuthorize("hasAuthority('ROLE_MANAGE')")
    public ResponseEntity<?> updateTemplatePermissions(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            adminRbacService.updateTemplatePermissions(id, permissionIds(body));
            return ResponseEntity.ok(Map.of("status", "success", "message", "模板權限已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            // 避免例外於 error dispatch 遺失認證而被包成誤導性的 403；明確回 500 + 訊息
            log.error("更新模板權限失敗 templateId={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "更新模板權限失敗，請稍後再試"));
        }
    }

    // ── 選單資源 ──

    @GetMapping("/resources")
    @PreAuthorize("hasAuthority('ROLE_VIEW')")
    public ResponseEntity<?> getResources(@RequestParam(value = "portal", required = false) String portal) {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminRbacService.getResources(portal)));
    }

    @PutMapping("/resources/{id}")
    @PreAuthorize("hasAuthority('RESOURCE_MANAGE')")
    public ResponseEntity<?> updateResource(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            String label = body.get("label") != null ? body.get("label").toString() : null;
            String path = body.get("path") != null ? body.get("path").toString() : null;
            Integer sortOrder = body.get("sortOrder") instanceof Number n ? n.intValue() : null;
            String icon = body.get("icon") != null ? body.get("icon").toString() : null;
            Boolean visible = body.get("visible") instanceof Boolean b ? b : null;
            adminRbacService.updateResource(id, label, path, sortOrder, icon, visible);
            return ResponseEntity.ok(Map.of("status", "success", "message", "選單資源已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** 從 request body 取出 permissions 陣列（容錯非 List 與非字串元素）。 */
    private static List<String> permissionIds(Map<String, Object> body) {
        Object raw = body.get("permissions");
        if (raw instanceof List<?> list) {
            return list.stream().filter(java.util.Objects::nonNull).map(o -> o.toString()).toList();
        }
        return List.of();
    }
}
