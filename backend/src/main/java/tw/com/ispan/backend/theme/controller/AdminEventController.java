package tw.com.ispan.backend.theme.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.theme.service.AdminEventService;

/**
 * Admin 後台「活動查詢（唯讀）」REST 端點（批次 3）。
 *
 * <p>路徑前綴 {@code /api/admin/events}，授權 {@code EVENT_VIEW}。
 * 唯讀：僅提供清單與詳情，不提供任何修改端點。
 * 回傳格式比照其他 Admin controller：{@code {status:"success", data:...}}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/events")
@RequiredArgsConstructor
public class AdminEventController {

    private final AdminEventService adminEventService;

    /** 全平台活動清單。 */
    @GetMapping
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public ResponseEntity<?> list() {
        log.info("API Admin 查詢活動清單");
        return ResponseEntity.ok(Map.of("status", "success", "data", adminEventService.listEvents()));
    }

    /** 活動詳情 + 票種售況。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('EVENT_VIEW')")
    public ResponseEntity<?> detail(@PathVariable Integer id) {
        log.info("API Admin 查詢活動詳情 id={}", id);
        try {
            return ResponseEntity.ok(Map.of("status", "success", "data", adminEventService.getEventDetail(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
