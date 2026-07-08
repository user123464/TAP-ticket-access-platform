package tw.com.ispan.backend.subscription.controller;

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
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.subscription.service.AdminSubscriptionService;

/**
 * Admin 後台「訂閱方案」REST 端點（批次 3）。
 *
 * <p>路徑前綴 {@code /api/admin/subscription}。
 * 方案 CRUD 授權 {@code PLAN_MANAGE}；組織訂閱總覽授權 {@code PLAN_MANAGE}（亦可 ORGANIZER_VIEW）。
 * 回傳 {@code {status:"success", data:...}} / {@code {status:"success", message:...}}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/subscription")
@RequiredArgsConstructor
public class AdminSubscriptionController {

    private final AdminSubscriptionService adminSubscriptionService;

    // ── 方案 ──

    @GetMapping("/plans")
    @PreAuthorize("hasAuthority('PLAN_MANAGE')")
    public ResponseEntity<?> listPlans() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminSubscriptionService.listPlans()));
    }

    @PostMapping("/plans")
    @PreAuthorize("hasAuthority('PLAN_MANAGE')")
    public ResponseEntity<?> createPlan(@RequestBody Map<String, Object> body) {
        try {
            var dto = adminSubscriptionService.createPlan(body);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "message", "方案已建立", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("建立訂閱方案失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "建立方案失敗，請稍後再試"));
        }
    }

    @PutMapping("/plans/{id}")
    @PreAuthorize("hasAuthority('PLAN_MANAGE')")
    public ResponseEntity<?> updatePlan(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            var dto = adminSubscriptionService.updatePlan(id, body);
            return ResponseEntity.ok(Map.of("status", "success", "message", "方案已更新", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/plans/{id}")
    @PreAuthorize("hasAuthority('PLAN_MANAGE')")
    public ResponseEntity<?> deletePlan(@PathVariable String id) {
        try {
            adminSubscriptionService.deletePlan(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "方案已刪除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 各組織訂閱狀態總覽 ──

    @GetMapping("/organizations")
    @PreAuthorize("hasAuthority('PLAN_MANAGE')")
    public ResponseEntity<?> listOrganizations() {
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", adminSubscriptionService.listOrganizationSubscriptions()));
    }
}
