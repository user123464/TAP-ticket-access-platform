package tw.com.ispan.backend.notification.controller;

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
import tw.com.ispan.backend.notification.service.AdminNotificationService;

/**
 * Admin 後台「通知模板 / 發送紀錄」REST 端點（批次 3，全新建立）。
 *
 * <p>路徑掛在 {@code /api/admin/system} 下（依前端 system 頁面為準）：
 * 模板 {@code /templates}（TEMPLATE_MANAGE）、發送紀錄 {@code /notifications}（NOTIF_LOG_VIEW）。
 * 回傳 {@code {status:"success", data:...}} / {@code {status:"success", message:...}}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminNotificationController {

    private final AdminNotificationService adminNotificationService;

    // ── 通知模板 ──
    @GetMapping("/templates")
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    public ResponseEntity<?> listTemplates() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminNotificationService.listTemplates()));
    }

    @PostMapping("/templates")
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    public ResponseEntity<?> createTemplate(@RequestBody Map<String, Object> body) {
        try {
            var dto = adminNotificationService.createTemplate(body);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "message", "模板已建立", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    public ResponseEntity<?> updateTemplate(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            var dto = adminNotificationService.updateTemplate(id, body);
            return ResponseEntity.ok(Map.of("status", "success", "message", "模板已更新", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/templates/{id}")
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    public ResponseEntity<?> deleteTemplate(@PathVariable String id) {
        try {
            adminNotificationService.deleteTemplate(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "模板已刪除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 發送紀錄 ──
    @GetMapping("/notifications")
    @PreAuthorize("hasAuthority('NOTIF_LOG_VIEW')")
    public ResponseEntity<?> listNotifications() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminNotificationService.listLogs()));
    }

    @PostMapping("/notifications/{id}/resend")
    @PreAuthorize("hasAuthority('NOTIF_LOG_VIEW')")
    public ResponseEntity<?> resend(@PathVariable Long id) {
        try {
            var dto = adminNotificationService.resendLog(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "已排入重送", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
