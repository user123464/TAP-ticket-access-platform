package tw.com.ispan.backend.organizer.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.service.AdminContractService;
import tw.com.ispan.backend.organizer.service.AdminContractService.TemplateView;

/**
 * Admin 後台「合約管理」REST 端點（批次 2）。
 *
 * <p>路徑前綴 {@code /api/admin/contracts}，授權採方法級 {@code @PreAuthorize}
 * （CONTRACT_VIEW / CONTRACT_CREATE / CONTRACT_EDIT，data.sql 已掛 ADMIN+SUPER_ADMIN）。
 * 回傳格式比照其他 Admin controller：{@code {status:"success", data:...}}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/contracts")
@RequiredArgsConstructor
public class AdminContractController {

    private final AdminContractService adminContractService;

    /** 全平台合約清單，支援 contract_type / contract_status 篩選（前端編碼）。 */
    @GetMapping
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<?> list(
            @RequestParam(value = "contract_type", required = false) Integer contractType,
            @RequestParam(value = "contract_status", required = false) Integer contractStatus) {
        log.info("API Admin 查詢合約清單 type={}, status={}", contractType, contractStatus);
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminContractService.listContracts(contractType, contractStatus)));
        } catch (Exception e) {
            log.error("查詢合約清單失敗", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** 公版範本讀取（須放在 {id} 之前以避免被動態路徑吃掉）。 */
    @GetMapping("/template")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<?> getTemplate() {
        TemplateView t = adminContractService.getTemplate();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "data", Map.of("version", t.version(), "contentMd", t.contentMd())));
    }

    /** 公版範本儲存（產生新版本）。 */
    @PostMapping("/template")
    @PreAuthorize("hasAuthority('CONTRACT_EDIT')")
    public ResponseEntity<?> saveTemplate(@RequestBody Map<String, String> body) {
        try {
            TemplateView t = adminContractService.saveTemplate(body.get("contentMd"));
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "公版範本已儲存新版本 " + t.version(),
                    "data", Map.of("version", t.version(), "contentMd", t.contentMd())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("儲存公版範本失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "範本儲存失敗，請稍後再試"));
        }
    }

    /** 建立客製合約（multipart：.md 內容 + 已簽署 PDF）。 */
    @PostMapping("/custom")
    @PreAuthorize("hasAuthority('CONTRACT_CREATE')")
    public ResponseEntity<?> createCustom(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam("orgId") String orgId,
            @RequestParam(value = "feeType", required = false) Integer feeType,
            @RequestParam("feeValue") BigDecimal feeValue,
            @RequestParam(value = "expiresAt", required = false) String expiresAt,
            @RequestParam("contentMd") String contentMd,
            @RequestParam("pdf") MultipartFile pdf) {
        log.info("API Admin 建立客製合約 orgId={}, 操作者={}", orgId, userDetails.getUserId());
        try {
            LocalDate expires = (expiresAt != null && !expiresAt.isBlank()) ? LocalDate.parse(expiresAt) : null;
            var detail = adminContractService.createCustomContract(
                    userDetails.getUserId(), orgId, feeType, feeValue, expires, contentMd, pdf);
            return ResponseEntity.status(HttpStatus.CREATED).body(Map.of(
                    "status", "success",
                    "message", "客製合約建立成功",
                    "data", detail));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("建立客製合約失敗 orgId={}", orgId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "建立客製合約失敗，請稍後再試"));
        }
    }

    /** 客製合約 PDF 下載（受 CONTRACT_VIEW 保護）。 */
    @GetMapping("/{id}/pdf")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<?> downloadPdf(@PathVariable String id) {
        try {
            byte[] data = adminContractService.readCustomPdf(id);
            if (data == null) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.APPLICATION_PDF)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + id + ".pdf\"")
                    .body(data);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /** 合約詳情（須放在最後，避免吃掉 /template 等靜態子路徑）。 */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CONTRACT_VIEW')")
    public ResponseEntity<?> detail(@PathVariable String id) {
        log.info("API Admin 查詢合約詳情 id={}", id);
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminContractService.getContractDetail(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            log.error("查詢合約詳情失敗 id={}", id, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢合約詳情失敗"));
        }
    }
}
