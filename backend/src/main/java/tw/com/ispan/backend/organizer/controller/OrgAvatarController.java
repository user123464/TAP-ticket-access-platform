package tw.com.ispan.backend.organizer.controller;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.FileTime;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.service.OrganizerService;
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.service.MediaFileRecorder;

/**
 * 組織頭像（Logo）管理 Controller
 * 與使用者頭像採用相同的架構：唯一檔名（不覆蓋）+ ETag 快取
 *
 * - POST /api/organizer/{organizerId}/avatar  → 上傳 Logo（需登入，需為 OWNER 或 ADMIN）
 * - GET  /api/organizer/avatars/{fileName}    → 公開讀取 Logo（SecurityConfig 放行）
 *
 * 檔案存放路徑：documents/org/avatars/{organizerId}_{timestamp}.{ext}
 * 快取策略：ETag（檔案最後修改時間）+ Cache-Control: no-cache
 */
@Slf4j
@RestController
@RequestMapping("/api/organizer")
@RequiredArgsConstructor
public class OrgAvatarController {

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    private final OrganizerService organizerService;
    private final MediaFileRecorder mediaFileRecorder;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/jpg");

    /**
     * 上傳組織 Logo
     * 需為組織 OWNER 或 ADMIN，存至 documents/org/avatars/{organizerId}.{ext}
     * 回傳可公開訪問的 URL（格式: /api/organizer/avatars/{fileName}）
     */
    @PostMapping("/{organizerId}/avatar")
    public ResponseEntity<?> uploadOrgAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @RequestParam MultipartFile file) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "認證失敗，請重新登入"));
        }

        // 權限防禦：必須是 OWNER 或 ADMIN
        try {
            organizerService.verifyOwnerOrAdmin(userDetails.getUserId(), organizerId);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }

        if (file == null || file.isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "請選擇要上傳的圖片檔案"));
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "只支援上傳 JPG 或 PNG 格式的圖片"));
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "圖片大小不能超過 2MB"));
        }

        try {
            Path avatarDir = Paths.get(documentsDir, "public", "org-logos").toAbsolutePath().normalize();
            if (!Files.exists(avatarDir)) {
                Files.createDirectories(avatarDir);
                log.info("已建立組織頭像儲存目錄: {}", avatarDir);
            }

            // 以 organizerId + 時間戳為唯一檔名，每次上傳都產生新 URL：
            // 1) 前端 <img> 的 src 字串會跟著變 → 立即重新渲染（解決第二次上傳預覽不更新）
            // 2) 不覆蓋舊圖 → 未儲存前仍指向舊檔，重新整理不會誤更新（解決未儲存卻變更全域）
            // 註：組織走 KYC/localStorage 流程，舊檔暫不在此清理，留待日後排程掃描
            String ext = "image/png".equals(contentType) ? "png" : "jpg";
            String fileName = organizerId + "_" + System.currentTimeMillis() + "." + ext;
            Path filePath = avatarDir.resolve(fileName);

            // 安全防禦：路徑穿越防護
            Path docsRoot = Paths.get(documentsDir).toAbsolutePath().normalize();
            if (!filePath.startsWith(docsRoot)) {
                log.warn("偵測到非法路徑穿越嘗試，organizerId: {}", organizerId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "非法的檔案路徑"));
            }

            Files.write(filePath, file.getBytes());
            log.info("組織 Logo 上傳成功，organizerId: {}，檔案: {}", organizerId, filePath);

            String avatarUrl = "/api/organizer/avatars/" + fileName;
            mediaFileRecorder.record(userDetails.getUserId(), "organizer", organizerId,
                    FileType.IMAGE, avatarUrl, filePath);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "avatarUrl", avatarUrl,
                    "message", "Logo 上傳成功"));

        } catch (IOException e) {
            log.error("組織 Logo 儲存失敗，organizerId: {}", organizerId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "上傳失敗，請稍後再試"));
        }
    }

    /**
     * 公開讀取組織 Logo（已在 SecurityConfig 對 GET /api/organizer/avatars/** 放行）
     * 範例: GET /api/organizer/avatars/ORG0000001_1718700000000.jpg
     *
     * 快取機制：檔名含時間戳、內容不再變動，ETag 僅作為 304 驗證之用
     */
    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<byte[]> serveOrgAvatar(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            Path avatarDir = Paths.get(documentsDir, "public", "org-logos").toAbsolutePath().normalize();
            Path filePath = avatarDir.resolve(fileName).toAbsolutePath().normalize();

            // 安全防禦：路徑穿越防護
            if (!filePath.startsWith(avatarDir)) {
                log.warn("偵測到路徑穿越嘗試，fileName: {}", fileName);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 只允許圖片類型
            String mimeType = URLConnection.guessContentTypeFromName(filePath.getFileName().toString());
            if (mimeType == null || !mimeType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // ETag = 檔案最後修改時間毫秒
            FileTime lastModified = Files.getLastModifiedTime(filePath);
            String etag = "\"" + lastModified.toMillis() + "\"";

            // 快取命中 → 304 Not Modified
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build();
            }

            byte[] data = Files.readAllBytes(filePath);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(mimeType))
                    .header(HttpHeaders.ETAG, etag)
                    .cacheControl(CacheControl.noCache())
                    .body(data);

        } catch (IOException e) {
            log.error("讀取組織 Logo 失敗，fileName: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
