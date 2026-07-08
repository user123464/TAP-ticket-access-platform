package tw.com.ispan.backend.login.controller;

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
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.service.MediaFileRecorder;

/**
 * 使用者頭像管理 Controller
 * - POST /api/user/avatar          → 上傳頭像（需登入，儲存至 documents/user/avatars/）
 * - GET  /api/user/avatars/{file}  → 公開讀取頭像圖片（已在 SecurityConfig 放行）
 *
 * 快取策略：ETag（以檔案最後修改時間為版本）+ Cache-Control: no-cache
 * - 瀏覽器每次使用前向伺服器驗證 ETag；ETag 未變則回 304（零傳輸），變則回 200 + 新圖片
 * - 不需要在 URL 加 ?v=timestamp
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class AvatarUploadController {

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    private final MediaFileRecorder mediaFileRecorder;

    private static final long MAX_FILE_SIZE = 2 * 1024 * 1024; // 2MB
    private static final List<String> ALLOWED_TYPES = List.of("image/jpeg", "image/png", "image/jpg");

    /**
     * 上傳使用者頭像圖片
     * 接收 multipart/form-data，存至 documents/user/avatars/{userId}_{timestamp}.{ext}
     * 回傳可公開訪問的圖片 URL（格式: /api/user/avatars/{fileName}）
     */
    @PostMapping("/avatar")
    public ResponseEntity<?> uploadAvatar(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestParam MultipartFile file) {

        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "認證失敗，請重新登入"));
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
            Path avatarDir = Paths.get(documentsDir, "public", "user-avatars").toAbsolutePath().normalize();
            if (!Files.exists(avatarDir)) {
                Files.createDirectories(avatarDir);
                log.info("已建立頭像儲存目錄: {}", avatarDir);
            }

            // 以 userId + 時間戳為唯一檔名，每次上傳都產生新 URL：
            // 1) 前端 <img> 的 src 字串會跟著變 → SPA 立即重新渲染，不再需要 ?v= 破快取
            // 2) 不覆蓋舊圖 → 在使用者按「儲存」前，資料庫仍指向舊檔，重新整理不會誤更新全域頭像
            // 舊檔的清理改由 UserProfileService 在儲存成功時執行（比對舊→新 URL 後刪除被取代的檔）
            String ext = "image/png".equals(contentType) ? "png" : "jpg";
            String fileName = userDetails.getUserId() + "_" + System.currentTimeMillis() + "." + ext;
            Path filePath = avatarDir.resolve(fileName);

            // 安全防禦：確保目標路徑在 documents 目錄樹下，防止路徑穿越
            Path docsRoot = Paths.get(documentsDir).toAbsolutePath().normalize();
            if (!filePath.startsWith(docsRoot)) {
                log.warn("偵測到非法路徑穿越攻擊嘗試，userId: {}", userDetails.getUserId());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("success", false, "message", "非法的檔案路徑"));
            }

            Files.write(filePath, file.getBytes());
            log.info("頭像上傳成功，userId: {}，檔案: {}", userDetails.getUserId(), filePath);

            String avatarUrl = "/api/user/avatars/" + fileName;
            mediaFileRecorder.record(userDetails.getUserId(), "user", userDetails.getUserId(),
                    FileType.IMAGE, avatarUrl, filePath);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "avatarUrl", avatarUrl,
                    "message", "頭像上傳成功"));

        } catch (IOException e) {
            log.error("頭像檔案儲存失敗，userId: {}", userDetails.getUserId(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "上傳失敗，請稍後再試"));
        }
    }

    /**
     * 公開讀取頭像圖片（已在 SecurityConfig 對 GET /api/user/avatars/** 放行）
     * 範例: GET /api/user/avatars/USR0000001_1718700000000.jpg
     *
     * 快取機制：
     * - 檔名含時間戳、內容不再變動，ETag（檔案最後修改時間）僅作為 304 驗證之用
     * - 若瀏覽器帶 If-None-Match 且 ETag 相符 → 回 304 Not Modified（無圖片資料傳輸）
     */
    @GetMapping("/avatars/{fileName}")
    public ResponseEntity<byte[]> serveAvatar(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            Path avatarDir = Paths.get(documentsDir, "public", "user-avatars").toAbsolutePath().normalize();
            Path filePath = avatarDir.resolve(fileName).toAbsolutePath().normalize();

            // 安全防禦：確保訪問路徑在 avatars 目錄下，防止路徑穿越
            if (!filePath.startsWith(avatarDir)) {
                log.warn("偵測到路徑穿越嘗試，fileName: {}", fileName);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                return ResponseEntity.notFound().build();
            }

            // 只允許提供圖片類型（防止暴露非圖片檔案）
            String mimeType = URLConnection.guessContentTypeFromName(filePath.getFileName().toString());
            if (mimeType == null || !mimeType.startsWith("image/")) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            // ETag = 檔案最後修改時間毫秒，用雙引號包覆（HTTP 規範要求）
            FileTime lastModified = Files.getLastModifiedTime(filePath);
            String etag = "\"" + lastModified.toMillis() + "\"";

            // 瀏覽器快取命中：ETag 相同 → 304 Not Modified，不傳圖片資料
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
            log.error("讀取頭像失敗，fileName: {}", fileName, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
}
