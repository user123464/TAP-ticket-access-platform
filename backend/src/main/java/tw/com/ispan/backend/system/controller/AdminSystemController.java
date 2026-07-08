package tw.com.ispan.backend.system.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.system.service.AdminSystemService;
import org.springframework.beans.factory.annotation.Value;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Admin 後台「系統管理」REST 端點（批次 3，全新建立）——稽核 / 公告 / 客訴 / 字典 / 設定 / 媒體 / 排程。
 *
 * <p>路徑前綴 {@code /api/admin/system}。各端點以方法級 {@code @PreAuthorize} 授權。
 * 回傳 {@code {status:"success", data:...}} / {@code {status:"success", message:...}}。
 * 通知模板 / 發送紀錄拆至 {@link tw.com.ispan.backend.notification.controller.AdminNotificationController}
 * （但路徑仍掛在 /api/admin/system/* 下，依前端頁面為準）。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/system")
@RequiredArgsConstructor
public class AdminSystemController {

    private final AdminSystemService adminSystemService;

    @Value("${app.prometheus.url:http://localhost:9090/api/v1}")
    private String prometheusUrl;

    private static final HttpClient httpClient = HttpClient.newHttpClient();

    // ── 稽核日誌 ──
    @GetMapping("/audit-logs")
    @PreAuthorize("hasAuthority('AUDIT_VIEW')")
    public ResponseEntity<?> auditLogs(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to,
            @RequestParam(value = "action", required = false) String action) {
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminSystemService.listAuditLogs(from, to, action)));
        } catch (Exception e) {
            log.error("查詢稽核日誌失敗", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "查詢稽核日誌失敗"));
        }
    }

    // ── 系統公告 ──
    @GetMapping("/announcements")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public ResponseEntity<?> listAnnouncements() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminSystemService.listAnnouncements()));
    }

    @PostMapping("/announcements")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public ResponseEntity<?> createAnnouncement(
            @AuthenticationPrincipal CustomUserDetails user,
            @RequestBody Map<String, Object> body) {
        try {
            var dto = adminSystemService.createAnnouncement(body, user != null ? user.getUserId() : null);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "message", "公告已建立", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public ResponseEntity<?> updateAnnouncement(@PathVariable String id, @RequestBody Map<String, Object> body) {
        try {
            var dto = adminSystemService.updateAnnouncement(id, body);
            return ResponseEntity.ok(Map.of("status", "success", "message", "公告已更新", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/announcements/{id}")
    @PreAuthorize("hasAuthority('ANNOUNCEMENT_MANAGE')")
    public ResponseEntity<?> deleteAnnouncement(@PathVariable String id) {
        try {
            adminSystemService.deleteAnnouncement(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "公告已刪除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 客訴 / 回饋 ──
    @GetMapping("/submissions")
    @PreAuthorize("hasAuthority('SUBMISSION_VIEW')")
    public ResponseEntity<?> listSubmissions() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminSystemService.listSubmissions()));
    }

    @PutMapping("/submissions/{id}/status")
    @PreAuthorize("hasAuthority('SUBMISSION_VIEW')")
    public ResponseEntity<?> updateSubmissionStatus(
            @AuthenticationPrincipal CustomUserDetails user,
            @PathVariable String id,
            @RequestBody Map<String, Object> body) {
        try {
            String status = body.get("status") != null ? body.get("status").toString() : null;
            adminSystemService.updateSubmissionStatus(id, status, user != null ? user.getUserId() : null);
            return ResponseEntity.ok(Map.of("status", "success", "message", "客訴狀態已更新"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 資料字典 ──
    @GetMapping("/dictionaries")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    public ResponseEntity<?> listDictionaries() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminSystemService.listDictionaries()));
    }

    @PostMapping("/dictionaries")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    public ResponseEntity<?> createDictionary(@RequestBody Map<String, Object> body) {
        try {
            var dto = adminSystemService.createDictionary(body);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("status", "success", "message", "字典項已建立", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @PutMapping("/dictionaries/{id}")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    public ResponseEntity<?> updateDictionary(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        try {
            var dto = adminSystemService.updateDictionary(id, body);
            return ResponseEntity.ok(Map.of("status", "success", "message", "字典項已更新", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @DeleteMapping("/dictionaries/{id}")
    @PreAuthorize("hasAuthority('DICT_MANAGE')")
    public ResponseEntity<?> deleteDictionary(@PathVariable Long id) {
        try {
            adminSystemService.deleteDictionary(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "字典項已刪除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 系統設定 ──
    @GetMapping("/configs")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<?> listConfigs() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminSystemService.listConfigs()));
    }

    @PutMapping("/configs/{key}")
    @PreAuthorize("hasAuthority('SYSTEM_CONFIG')")
    public ResponseEntity<?> updateConfig(@PathVariable String key, @RequestBody Map<String, Object> body) {
        try {
            String value = body.get("value") != null ? body.get("value").toString() : null;
            var dto = adminSystemService.updateConfig(key, value);
            return ResponseEntity.ok(Map.of("status", "success", "message", "設定已更新", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 媒體檔案 ──
    @GetMapping("/media")
    @PreAuthorize("hasAuthority('MEDIA_MANAGE')")
    public ResponseEntity<?> listMedia() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminSystemService.listMedia()));
    }

    @DeleteMapping("/media/{id}")
    @PreAuthorize("hasAuthority('MEDIA_MANAGE')")
    public ResponseEntity<?> deleteMedia(@PathVariable Long id) {
        try {
            adminSystemService.deleteMedia(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "檔案已刪除"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 排程任務 ──
    @GetMapping("/jobs")
    @PreAuthorize("hasAuthority('JOB_MANAGE')")
    public ResponseEntity<?> listJobs() {
        return ResponseEntity.ok(Map.of("status", "success", "data", adminSystemService.listJobs()));
    }

    @PostMapping("/jobs/{id}/trigger")
    @PreAuthorize("hasAuthority('JOB_MANAGE')")
    public ResponseEntity<?> triggerJob(@PathVariable String id) {
        try {
            var dto = adminSystemService.triggerJob(id);
            return ResponseEntity.ok(Map.of("status", "success", "message", "已觸發排程", "data", dto));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    // ── 系統即時監控 Prometheus 代理 ──
    // 授權 DASHBOARD_VIEW：此代理供 dashboard 的「系統健康度」widget 使用，
    // 屬「進後台看總覽」的一部分，與使用者管理(USER_VIEW)解耦，避免只有 DASHBOARD_VIEW
    // 的角色(如場地專員)因 widget 輪詢 403 而被踢回登入頁。
    @GetMapping("/health/prometheus")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ResponseEntity<?> getPrometheusMetric(@RequestParam("query") String query) {
        log.info("API 代理查詢 Prometheus query={}", query);
        try {
            String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
            String targetUrl = prometheusUrl + "/query?query=" + encodedQuery;

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(targetUrl))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return ResponseEntity.status(response.statusCode())
                    .header("Content-Type", "application/json")
                    .body(response.body());
        } catch (Exception e) {
            log.error("代理 Prometheus 查詢失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "代理查詢監控數據失敗"));
        }
    }
}
