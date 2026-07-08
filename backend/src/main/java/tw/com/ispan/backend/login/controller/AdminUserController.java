package tw.com.ispan.backend.login.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.service.AdminUserService;

/**
 * Admin 後台「使用者管理」REST 端點。
 *
 * <p>地基批次：清單（搜尋 + 篩選）、詳情（個資 + 登入紀錄 + 所屬組織角色）、
 * 帳號操作（啟用 / 停用 / 解鎖 / 軟刪除）。</p>
 *
 * <p>授權：查詢類用 {@code USER_VIEW}，異動類用 {@code USER_EDIT}
 * （data.sql 已掛在 ADMIN / SUPER_ADMIN）。回傳格式比照 AdminKycController：
 * {@code {status:"success", data:...}}，錯誤回 {@code {success:false, message:...}}。
 * 路徑前綴 {@code /api/admin/users} 落在 SecurityConfig 的 authenticated 範圍。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<?> listUsers(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "verified", required = false) String verified,
            @RequestParam(value = "authProvider", required = false) String authProvider) {
        log.info("API 管理員查詢使用者清單 keyword={}, status={}, verified={}, authProvider={}",
                keyword, status, verified, authProvider);
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminUserService.listUsers(keyword, status, verified, authProvider)));
        } catch (Exception e) {
            log.error("查詢使用者清單失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢使用者清單失敗"));
        }
    }

    /** 內部人員清單（持有內部人員角色的帳號）。 */
    @GetMapping("/staff")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<?> listStaff() {
        try {
            return ResponseEntity.ok(Map.of("status", "success", "data", adminUserService.listStaff()));
        } catch (Exception e) {
            log.error("查詢內部人員清單失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢內部人員清單失敗"));
        }
    }

    /**
     * 建立員工帳號（平台角色 + 初始密碼）。僅限 SUPER_ADMIN：
     * 開立 ADMIN/客服 帳號屬高敏感操作，且本端點不可建立 SUPER_ADMIN（後端再把關，見 service）。
     */
    @PostMapping
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> createUser(@RequestBody Map<String, String> body) {
        log.info("API SUPER_ADMIN 建立員工帳號 email={}, role={}", body.get("email"), body.get("roleId"));
        try {
            var detail = adminUserService.createStaffAccount(
                    body.get("email"), body.get("name"), body.get("password"), body.get("roleId"));
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "message", "員工帳號建立成功", "data", detail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("建立員工帳號失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "建立員工帳號失敗，請稍後再試"));
        }
    }

    /** 加掛內部人員角色給帳號（支撐臨時加權）。 */
    @PostMapping("/{userId}/roles")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> assignRole(@PathVariable String userId, @RequestBody Map<String, String> body) {
        try {
            var detail = adminUserService.assignRole(userId, body.get("roleId"));
            return ResponseEntity.ok(Map.of("status", "success", "message", "角色已加掛", "data", detail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** 取下帳號的內部人員角色。 */
    @DeleteMapping("/{userId}/roles/{roleId}")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> removeRole(@PathVariable String userId, @PathVariable String roleId) {
        try {
            var detail = adminUserService.removeRole(userId, roleId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "角色已取下", "data", detail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAuthority('USER_VIEW')")
    public ResponseEntity<?> getUser(@PathVariable String userId) {
        log.info("API 管理員查詢使用者詳情 userId={}", userId);
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminUserService.getUserDetail(userId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("查詢使用者詳情失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢使用者詳情失敗"));
        }
    }

    @PostMapping("/{userId}/activate")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> activate(@PathVariable String userId) {
        return runAction(userId, "activate");
    }

    @PostMapping("/{userId}/deactivate")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> deactivate(@PathVariable String userId) {
        return runAction(userId, "deactivate");
    }

    @PostMapping("/{userId}/unlock")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> unlock(@PathVariable String userId) {
        return runAction(userId, "unlock");
    }

    @PostMapping("/{userId}/delete")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> delete(@PathVariable String userId) {
        return runAction(userId, "delete");
    }

    @PostMapping("/{userId}/restore")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> restore(@PathVariable String userId) {
        return runAction(userId, "restore");
    }

    @PostMapping("/{userId}/force-logout")
    @PreAuthorize("hasAuthority('USER_EDIT')")
    public ResponseEntity<?> forceLogout(@PathVariable String userId) {
        return runAction(userId, "force-logout");
    }

    /** 重設員工密碼為預設密碼 Aa123456，並強制下次登入修改密碼。 */
    @PostMapping("/{userId}/reset-password")
    @PreAuthorize("hasRole('SUPER_ADMIN')")
    public ResponseEntity<?> resetPassword(@PathVariable String userId) {
        log.info("API SUPER_ADMIN 重設員工密碼 userId={}", userId);
        try {
            adminUserService.resetStaffPassword(userId);
            return ResponseEntity.ok(Map.of("status", "success", "message", "員工密碼已重設為預設密碼 Aa123456"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("重設員工密碼失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "重設員工密碼失敗，請稍後再試"));
        }
    }

    /** 共用：執行帳號操作並回傳統一格式 */
    private ResponseEntity<?> runAction(String userId, String action) {
        log.info("API 管理員帳號操作 userId={}, action={}", userId, action);
        try {
            switch (action) {
                case "activate" -> adminUserService.activate(userId);
                case "deactivate" -> adminUserService.deactivate(userId);
                case "unlock" -> adminUserService.unlock(userId);
                case "delete" -> adminUserService.softDelete(userId);
                case "restore" -> adminUserService.restore(userId);
                case "force-logout" -> adminUserService.forceLogout(userId);
                default -> {
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                            .body(Map.of("success", false, "message", "未知的操作"));
                }
            }
            return ResponseEntity.ok(Map.of("status", "success", "message", "操作成功"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("帳號操作失敗 userId={}, action={}", userId, action, e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "操作失敗，請稍後再試"));
        }
    }
}
