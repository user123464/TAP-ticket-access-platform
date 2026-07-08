package tw.com.ispan.backend.organizer.controller;

import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.organizer.service.AdminOrganizerService;

/**
 * Admin 後台「組織詳情 / 狀態」REST 端點。
 *
 * <p>地基批次：詳情（組織資料 + 成員 + 合約 + 訂閱摘要）與狀態切換
 * （暫停=1 / 恢復=0 / 封存=2）。清單 GET {@code /api/admin/organizers} 仍由
 * {@link AdminKycController} 提供（避免同路徑重複映射）；本 controller 只掛子路徑。</p>
 *
 * <p>授權：查詢用 {@code ORGANIZER_VIEW}；狀態異動用 {@code ORGANIZER_EDIT}
 * （data.sql 已有此權限碼）。回傳格式比照其他 Admin controller。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/organizers")
@RequiredArgsConstructor
public class AdminOrganizerController {

    private final AdminOrganizerService adminOrganizerService;

    @GetMapping("/{organizerId}")
    @PreAuthorize("hasAuthority('ORGANIZER_VIEW')")
    public ResponseEntity<?> getDetail(@PathVariable String organizerId) {
        log.info("API 管理員查詢組織詳情 organizerId={}", organizerId);
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminOrganizerService.getDetail(organizerId)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("查詢組織詳情失敗 organizerId={}", organizerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢組織詳情失敗"));
        }
    }

    /**
     * KYC 認證文件下載/預覽（受 ORGANIZER_VIEW 保護）。
     * type：DOC=商業登記文件 / ID=負責人身分證。前端以帶 JWT 的 axios 取 blob 後開新分頁。
     */
    @GetMapping("/{organizerId}/kyc/file")
    @PreAuthorize("hasAuthority('ORGANIZER_VIEW')")
    public ResponseEntity<?> downloadKycFile(
            @PathVariable String organizerId,
            @RequestParam("type") String type) {
        log.info("API 管理員下載 KYC 文件 organizerId={}, type={}", organizerId, type);
        try {
            AdminOrganizerService.KycFile file = adminOrganizerService.readKycFile(organizerId, type);
            if (file == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(file.contentType()))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + file.fileName() + "\"")
                    .body(file.data());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("下載 KYC 文件失敗 organizerId={}, type={}", organizerId, type, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "下載 KYC 文件失敗"));
        }
    }

    @PostMapping("/{organizerId}/status")
    @PreAuthorize("hasAuthority('ORGANIZER_EDIT')")
    public ResponseEntity<?> changeStatus(
            @PathVariable String organizerId,
            @RequestBody Map<String, Object> body) {
        Object statusVal = body.get("status");
        log.info("API 管理員切換組織狀態 organizerId={}, status={}", organizerId, statusVal);
        if (statusVal == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "缺少 status 參數"));
        }
        try {
            int statusOrdinal = (statusVal instanceof Number n)
                    ? n.intValue()
                    : Integer.parseInt(statusVal.toString());
            adminOrganizerService.changeStatus(organizerId, statusOrdinal);
            return ResponseEntity.ok(Map.of("status", "success", "message", "組織狀態已更新"));
        } catch (NumberFormatException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "status 必須是數字"));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("切換組織狀態失敗 organizerId={}", organizerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "切換組織狀態失敗"));
        }
    }
}
