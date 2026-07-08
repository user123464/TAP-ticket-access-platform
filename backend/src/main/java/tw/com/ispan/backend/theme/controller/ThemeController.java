package tw.com.ispan.backend.theme.controller;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.theme.dto.ApiResponse;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeCreateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeQueryRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeUpdateRequest;
import tw.com.ispan.backend.theme.dto.response.OrganizerThemeResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeImageResource;
import tw.com.ispan.backend.theme.dto.response.ThemeListResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeResponse;
import tw.com.ispan.backend.theme.service.ThemeService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
@CrossOrigin
public class ThemeController {

    private final ThemeService themeService;

    // 前台：查詢所有 active themes
    @GetMapping("/themes")
    public ResponseEntity<ApiResponse<Page<ThemeListResponse>>> getThemes(ThemeQueryRequest request) {
        return ResponseEntity.ok(ApiResponse.success(themeService.getThemes(request)));
    }

    // 前台：查詢單一 theme
    @GetMapping("/themes/{themeId}")
    public ResponseEntity<ApiResponse<ThemeResponse>> findByThemeId(@PathVariable Integer themeId) {
        return ResponseEntity.ok(ApiResponse.success(themeService.getActiveThemeById(themeId)));
    }

    // 後台：查詢 organizer themes
    @GetMapping("/org/organizers/{organizerId}/themes")
    public ResponseEntity<ApiResponse<List<OrganizerThemeResponse>>> getThemesByOrganizerId(
            @PathVariable String organizerId) {
        return ResponseEntity.ok(ApiResponse.success(themeService.getThemesByOrganizerId(organizerId)));
    }

    // 後台：新增 theme
    @PreAuthorize("hasAuthority('EVENT_CREATE')")
    @PostMapping("/org/themes")
    public ResponseEntity<ApiResponse<ThemeResponse>> createTheme(@RequestBody ThemeCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(themeService.createTheme(request)));
    }

    // 後台：更新 theme
    @PreAuthorize("hasAuthority('EVENT_EDIT')")
    @PutMapping("/org/themes/{themeId}")
    public ResponseEntity<ApiResponse<ThemeResponse>> updateTheme(
            @PathVariable Integer themeId,
            @RequestBody ThemeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(themeService.updateTheme(themeId, request)));
    }

    // 後台：發布/下架
    @PreAuthorize("hasAuthority('EVENT_PUBLISH')")
    @PutMapping("/org/themes/{themeId}/status")
    public ResponseEntity<ApiResponse<ThemeResponse>> updateStatus(
            @PathVariable Integer themeId,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(themeService.updateStatus(themeId, request)));
    }

    // 圖片：上傳 (取得路徑)
    @PreAuthorize("hasAuthority('EVENT_EDIT')")
    @PostMapping("/org/themes/upload-image")
    public ResponseEntity<ApiResponse<ThemeResponse>> uploadThemeImage(
            @RequestParam Integer themeId,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(themeService.uploadThemeImage(themeId, file)));

    }

    // 圖片：更新
    @PutMapping("/org/themes/{themeId}/image")
    public ResponseEntity<ApiResponse<ThemeResponse>> updateImage(
            @PathVariable Integer themeId,
            @RequestBody ThemeUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(themeService.updateImage(themeId, request)));
    }

    // 圖片：讀取
    @GetMapping("/themes/images/{fileName}")
    public ResponseEntity<byte[]> serveThemeImage(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            // 呼叫 Service 載入資源
            ThemeImageResource resource = themeService.serveThemeImage(fileName);

            // 說明：ETag 是一個用於 HTTP 協議的標頭，用來標識資源的版本。
            // 當客戶端第一次請求資源時，伺服器會返回資源的內容以及一個 ETag 標頭。
            // 客戶端在後續的請求中，可以將這個 ETag 值放在 If-None-Match 標頭中，告訴伺服器它已經有這個版本的資源。
            // 如果伺服器發現資源沒有改變，就會返回 304 Not Modified，告訴客戶端使用緩存的版本，而不是重新下載整個資源。
            String etag = "\"" + resource.lastModified() + "\"";
            if (etag.equals(ifNoneMatch)) {
                // 用於創建一個 HTTP 響應，狀態碼為 304 Not Modified。
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build(); // 回傳 304
            }
            // 說明：Cache-Control 是一個 HTTP 標頭，用來控制資源的緩存行為。
            // no-cache 指示瀏覽器在使用緩存的資源之前，必須向伺服器驗證資源是否已經更新。
            // 這意味著即使資源已經被緩存，瀏覽器仍然需要向伺服器發送請求，以確保它使用的是最新版本的資源。
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resource.mimeType()))
                    .header(HttpHeaders.ETAG, etag)
                    .cacheControl(CacheControl.noCache())
                    .body(resource.data()); // 回傳 200

        } catch (IllegalArgumentException e) { // 說明：IllegalArgumentException 是一個 Java 標準的例外類型，通常在方法接收到不合法或不正確的參數時拋出。
            return ResponseEntity.notFound().build(); // 回傳 404
        } catch (SecurityException e) { // 說明：SecurityException 是一個 Java
                                        // 標準的例外類型，通常在程式嘗試執行不被允許的操作時拋出，例如訪問受保護的資源或執行受限制的操作。
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 回傳 403
        } catch (Exception e) { // 說明：Exception 捕獲了所有未被前面特定 catch 塊捕獲的異常。
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 回傳 500
        }
    }

}