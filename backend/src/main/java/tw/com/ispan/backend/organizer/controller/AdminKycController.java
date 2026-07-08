package tw.com.ispan.backend.organizer.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.service.AdminOrganizerService;
import tw.com.ispan.backend.organizer.service.OrganizerService;

/**
 * Admin 後台「主辦方 KYC 審核」REST 端點。
 *
 * <p>用途：RBAC 待辦 #2，將 {@link OrganizerService} 既有的 KYC 審核業務邏輯
 * （approveKyc / rejectKyc）expose 成 HTTP 端點。先前種子已定義
 * {@code ORGANIZER_KYC_REVIEW} 權限碼卻無端點可用，本 controller 補上對接。</p>
 *
 * <p>授權：使用方法級安全 {@code @PreAuthorize("hasAuthority('ORGANIZER_KYC_REVIEW')")}，
 * 權限碼即 data.sql 中掛在 ADMIN / SUPER_ADMIN 角色上的 permission_id。
 * 審核管理員 ID 由 JWT 解析後的 {@link CustomUserDetails#getUserId()} 取得，
 * 不由前端傳入，避免偽冒審核人。</p>
 *
 * <p>路徑前綴 {@code /api/admin/organizers}（複數 organizers 但帶 admin 前綴），
 * 刻意與 SecurityConfig 中已被 permitAll 的 {@code /api/organizers/**}（組員公開查詢）
 * 區隔，落在 anyRequest().authenticated() 範圍內，再交由 @PreAuthorize 把關。</p>
 *
 * @author [Jason]
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/organizers")
@RequiredArgsConstructor
public class AdminKycController {

    private final OrganizerService organizerService;
    private final AdminOrganizerService adminOrganizerService;

    /**
     * [Jason] RBAC 待辦 #2：Admin 後台組織清單（KYC 待審清單）。
     *
     * <p>授權 {@code ORGANIZER_VIEW}（data.sql 已掛在 ADMIN / SUPER_ADMIN）。
     * 可選 query param {@code status}（如 {@code ?status=PENDING}）依 KYC 狀態篩選，
     * 未帶則回傳全部。狀態值不合法回 400。為 approve/reject 端點提供「先看清單再審核」入口。</p>
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ORGANIZER_VIEW')")
    public ResponseEntity<?> listOrganizers(
            @RequestParam(value = "status", required = false) String status) {
        log.info("API 管理員查詢組織清單，KYC 狀態篩選: {}", status);
        try {
            KycStatus statusFilter = null;
            if (status != null && !status.trim().isEmpty()) {
                statusFilter = KycStatus.valueOf(status.trim().toUpperCase());
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminOrganizerService.listOrganizers(statusFilter)
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "無效的 KYC 狀態: " + status));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 審核 KYC 通過（自動建立免費標準合約，由 service 內部處理）。
     */
    @PostMapping("/{organizerId}/kyc/approve")
    @PreAuthorize("hasAuthority('ORGANIZER_KYC_REVIEW')")
    public ResponseEntity<?> approveKyc(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String adminUserId = userDetails.getUserId();
        log.info("API 管理員審核 KYC 通過，組織ID: {}, 操作者: {}", organizerId, adminUserId);
        try {
            organizerService.approveKyc(adminUserId, organizerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "KYC 審核已通過"
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
     * 審核 KYC 退件，退件原因從 request body 的 {@code reason} 取得。
     */
    @PostMapping("/{organizerId}/kyc/reject")
    @PreAuthorize("hasAuthority('ORGANIZER_KYC_REVIEW')")
    public ResponseEntity<?> rejectKyc(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @RequestBody Map<String, String> body) {
        String adminUserId = userDetails.getUserId();
        String reason = body.get("reason");
        log.info("API 管理員審核 KYC 退件，組織ID: {}, 操作者: {}", organizerId, adminUserId);

        if (reason == null || reason.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "退件原因不能為空"));
        }

        try {
            organizerService.rejectKyc(adminUserId, organizerId, reason);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "KYC 已退件"
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
