package tw.com.ispan.backend.theme.service.impl;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.nio.file.StandardCopyOption;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeCreateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeQueryRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeUpdateRequest;
import tw.com.ispan.backend.theme.dto.response.OrganizerThemeResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeImageResource;
import tw.com.ispan.backend.theme.dto.response.ThemeListResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeResponse;
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.ThemeRepository;
// [Jason] 跨組織 row-level 隔離查核（RBAC 缺口 #1）
import tw.com.ispan.backend.theme.security.OrgRowSecurityGuard;
import tw.com.ispan.backend.theme.service.ThemeService;
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.service.MediaFileRecorder;

@RequiredArgsConstructor
@Service
public class ThemeServiceImpl implements ThemeService {

    private final ThemeRepository themeRepository;
    private final OrganizerRepository organizerRepository;
    // [Jason] 跨組織隔離：在寫/讀操作前反查「登入者是否為資料所屬組織具該權限的成員」
    private final OrgRowSecurityGuard orgGuard;
    private final MediaFileRecorder mediaFileRecorder;

    // 讀取圖片的目錄
    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    @Override // 前台，查詢所有ACTIVE主題（分頁）
    public Page<ThemeListResponse> getThemes(ThemeQueryRequest request) {
        // 防止 request 為 null 導致後續 request.page() 觸發 NPE。
        if (request == null) {
            throw new IllegalArgumentException("查詢條件不可為空");
        }
        // 檢查前端傳來的 page(頁碼)與size(每頁數量)，若前端傳來null或不合理數值
        // 前端傳來的頁碼從1開始，轉換為Service需要的從0開始，並設定預設值為0
        int page = (request.page() == null || request.page() < 1) ? 0 : request.page() - 1;
        // 前端傳來的每頁數量，設定預設值為10，且上限為100
        int size = (request.size() == null || request.size() <= 0) ? 10 : Math.min(request.size(), 100);

        // PageRequest是 Spring Data JPA提供的工具類，
        // 用來幫你「建立分頁條件」，例如 PageRequest.of(0, 10)
        Pageable pageable = PageRequest.of(page, size);

        // 切換查詢邏輯：根據 filter 參數決定查詢方式
        Page<Theme> themePage;
        // filter 分類查詢（最新/即將登場）
        if ("latest".equals(request.filter())) {
            themePage = themeRepository.findLatestThemes(request.keyword(), pageable);
        } else {
            themePage = themeRepository.findUpcomingThemes(request.keyword(), pageable);
        }
        // 轉 DTO + 保留分頁
        return themePage.map(ThemeListResponse::fromEntity);
    }

    @Override // 前台，查詢 單一 theme
    public ThemeResponse getActiveThemeById(Integer themeId) {
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }
        Theme theme = themeRepository.findByThemeIdAndStatus(themeId, Status.ACTIVE)
                .orElseThrow(() -> new RuntimeException("找不到 Theme"));

        return ThemeResponse.fromEntity(theme);
    }

    @Override // 後台，查詢特定organizerId的所有主題
    public List<OrganizerThemeResponse> getThemesByOrganizerId(String organizerId) {

        // [Jason] 跨組織隔離：後台讀取某組織的活動清單（含草稿），須為該組織具 EVENT_VIEW 的成員，
        // 否則 A 廠商可帶 B 廠商的 organizerId 偷看 B 的草稿活動。
        orgGuard.requireOrgPermission(organizerId, "EVENT_VIEW");

        List<Theme> themes = themeRepository.findByOrganizerOrganizerId(organizerId);
        List<OrganizerThemeResponse> themeListResponses = themes.stream()
                .map(theme -> OrganizerThemeResponse.fromEntity(theme))
                .toList();

        return themeListResponses;
    }

    @Override
    @Transactional
    public ThemeResponse createTheme(ThemeCreateRequest request) {

        String organizerId = request.organizerId();
        if (organizerId == null || organizerId.isBlank()) {
            throw new IllegalArgumentException("organizer not found");
        }

        // 1. 先檢查權限，不合規就直接阻擋，不浪費 DB 查詢
        orgGuard.requireOrgPermission(organizerId, "EVENT_CREATE");

        // 2. 權限通過後，再查資料庫
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new RuntimeException("Organizer not found"));

        Theme theme = new Theme();

        theme.setOrganizer(organizer);
        theme.setTitle(request.title());
        theme.setDetail(request.detail());
        // 不立即將 image 存入，先儲存以取得 themeId，再處理檔案改名/覆寫
        theme.setStatus(Status.DRAFT);

        themeRepository.save(theme); // 儲存以產生 themeId

        // 若前端傳入剛上傳的暫存圖片路徑，將檔案改名為 theme_{themeId}.{ext}
        if (request.image() != null && request.image().startsWith("/api/themes/images/")) {
            String uploadedFileName = Paths.get(request.image()).getFileName().toString();
            try {
                Path uploadDir = Paths.get(documentsDir, "public", "theme-images").toAbsolutePath().normalize();
                Path source = uploadDir.resolve(uploadedFileName).toAbsolutePath().normalize();

                if (Files.exists(source) && source.startsWith(uploadDir)) {
                    String ext = uploadedFileName.contains(".") ? uploadedFileName.substring(uploadedFileName.lastIndexOf('.') + 1) : "";
                    String newFileName = "theme_" + theme.getThemeId() + "." + ext;
                    Path target = uploadDir.resolve(newFileName).toAbsolutePath().normalize();

                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                    theme.setImage("/api/themes/images/" + newFileName);
                    themeRepository.save(theme);
                }
            } catch (IOException e) {
                throw new RuntimeException("圖片儲存失敗", e);
            }
        }

        return ThemeResponse.fromEntity(theme);
    }

    @Override
    @Transactional
    public ThemeResponse updateTheme(Integer themeId, ThemeUpdateRequest request) {
        // 1. 基礎參數校驗 (防 NPE)
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }
        if (request == null) {
            throw new IllegalArgumentException("請求內容不可為空");
        }
        if (request.title() == null || request.title().isBlank()) {
            throw new IllegalArgumentException("主題名稱不可為空");
        }

        // 2. 取得資料
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new RuntimeException("找不到 Theme"));

        // 3. 權限檢查（必須在修改前）
        orgGuard.requireOrgPermission(theme.getOrganizer().getOrganizerId(), "EVENT_EDIT");

        // 4. 更新屬性（若有新上傳圖片，先將檔案改名為 theme-{themeId}.{ext}）
        theme.setTitle(request.title());
        theme.setDetail(request.detail());

        if (request.image() != null && request.image().startsWith("/api/themes/images/")) {
            String uploadedFileName = Paths.get(request.image()).getFileName().toString();
            try {
                Path uploadDir = Paths.get(documentsDir, "public", "theme-images").toAbsolutePath().normalize();
                Path source = uploadDir.resolve(uploadedFileName).toAbsolutePath().normalize();

                if (Files.exists(source) && source.startsWith(uploadDir)) {
                    String ext = uploadedFileName.contains(".") ? uploadedFileName.substring(uploadedFileName.lastIndexOf('.') + 1) : "";
                    String newFileName = "theme_" + theme.getThemeId() + "." + ext;
                    Path target = uploadDir.resolve(newFileName).toAbsolutePath().normalize();

                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                    theme.setImage("/api/themes/images/" + newFileName);
                } else {
                    // 非上傳路徑，直接存傳入值（可能是已存在的路徑）
                    theme.setImage(request.image());
                }
            } catch (IOException e) {
                throw new RuntimeException("圖片儲存失敗", e);
            }
        } else {
            theme.setImage(request.image());
        }

        // 5. 存檔
        themeRepository.save(theme);

        return ThemeResponse.fromEntity(theme);
    }

    @Override
    @Transactional
    public ThemeResponse updateStatus(Integer themeId, StatusUpdateRequest request) {
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }
        if (request == null || request.status() == null) {
            throw new IllegalArgumentException("狀態不可為空");
        }

        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new RuntimeException("找不到 Theme"));

        // 權限檢查（必須在修改前）
        orgGuard.requireOrgPermission(theme.getOrganizer().getOrganizerId(), "EVENT_PUBLISH");

        theme.setStatus(request.status());
        themeRepository.save(theme);

        return ThemeResponse.fromEntity(theme);
    }

    @Override // 圖片：上傳 (取得路徑)
    @Transactional
    public ThemeResponse uploadThemeImage(Integer themeId, MultipartFile file) {
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("請選擇要上傳的圖片檔案");
        }

        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new RuntimeException("找不到 Theme"));

        // 跨組織隔離：只能編輯「自己組織」的活動，須具 EVENT_EDIT
        orgGuard.requireOrgPermission(theme.getOrganizer().getOrganizerId(), "EVENT_EDIT");

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png")
                && !contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("只支援上傳 JPG 或 PNG 格式的圖片");
        }
        if (file.getSize() > 2 * 1024 * 1024) { // 2MB
            throw new IllegalArgumentException("圖片大小不能超過 2MB");
        }

        try {
            // 取得圖片儲存目錄
            Path uploadDir = Paths.get(documentsDir, "public", "theme-images").toAbsolutePath().normalize();
            // 建立目錄
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }
            // 固定唯一檔名（覆蓋舊圖）
            String ext = contentType.contains("png") ? "png" : "jpg";
            // 主題圖片檔名格式：theme_主題ID.副檔名
            String fileName = "theme_" + themeId + "." + ext;

            // 先刪舊檔
            Files.deleteIfExists(uploadDir.resolve("theme_" + themeId + ".jpg"));
            Files.deleteIfExists(uploadDir.resolve("theme_" + themeId + ".png"));
            
            // 取得檔案的絕對路徑
            Path filePath = uploadDir.resolve(fileName);

            // 安全防禦：防止路徑穿越
            Path docsRoot = Paths.get(documentsDir).toAbsolutePath().normalize();
            if (!filePath.startsWith(docsRoot)) {
                throw new SecurityException("非法的檔案路徑");
            }
            // 將圖片寫入檔案
            Files.write(filePath, file.getBytes()); 
            // 圖片對外提供的存取路徑
            String imageUrl = "/api/themes/images/" + fileName;
            theme.setImage(imageUrl);
            themeRepository.save(theme);
            mediaFileRecorder.record(null, "theme", String.valueOf(themeId), FileType.IMAGE, imageUrl, filePath);

            return ThemeResponse.fromEntity(theme);

        } catch (IOException e) {
            throw new RuntimeException("圖片儲存失敗", e);
        }
    }

    @Override
    @Transactional // 圖片：更新
    public ThemeResponse updateImage(Integer themeId, ThemeUpdateRequest request) {
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }
        if (request == null || request.image() == null) {
            throw new IllegalArgumentException("圖片不可為空");
        }

        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new RuntimeException("找不到 Theme"));

        // 跨組織隔離：只能編輯「自己組織」的活動，須具 EVENT_EDIT
        orgGuard.requireOrgPermission(theme.getOrganizer().getOrganizerId(), "EVENT_EDIT");

        // 若傳入的是上傳後的臨時路徑，改名為 theme-{themeId}.{ext}
        if (request.image().startsWith("/api/themes/images/")) {
            String uploadedFileName = Paths.get(request.image()).getFileName().toString();
            try {
                Path uploadDir = Paths.get(documentsDir, "public", "theme-images").toAbsolutePath().normalize();
                Path source = uploadDir.resolve(uploadedFileName).toAbsolutePath().normalize();

                if (Files.exists(source) && source.startsWith(uploadDir)) {
                    String ext = uploadedFileName.contains(".") ? uploadedFileName.substring(uploadedFileName.lastIndexOf('.') + 1) : "";
                    String newFileName = "theme-" + theme.getThemeId() + "." + ext;
                    Path target = uploadDir.resolve(newFileName).toAbsolutePath().normalize();

                    Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);

                    theme.setImage("/api/themes/images/" + newFileName);
                } else {
                    theme.setImage(request.image());
                }
            } catch (IOException e) {
                throw new RuntimeException("圖片儲存失敗", e);
            }
        } else {
            theme.setImage(request.image());
        }

        themeRepository.save(theme);

        return ThemeResponse.fromEntity(theme);
    }

    @Override // 圖片：讀取
    public ThemeImageResource serveThemeImage(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("檔名不可為空");
        }

        try {
            Path uploadDir = Paths.get(documentsDir, "public", "theme-images").toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(fileName).toAbsolutePath().normalize();

            // 1. 安全防禦：路徑穿越防護
            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("非法的存取路徑");
            }
            // 2. 檢查檔案是否存在
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                throw new IllegalArgumentException("找不到圖片檔案");
            }
            // 3. 只允許圖片類型
            String mimeType = URLConnection.guessContentTypeFromName(filePath.getFileName().toString());
            if (mimeType == null || !mimeType.startsWith("image/")) {
                throw new SecurityException("非法的檔案類型");
            }

            byte[] data = Files.readAllBytes(filePath);
            long lastModified = Files.getLastModifiedTime(filePath).toMillis();

            return new ThemeImageResource(data, mimeType, lastModified);
        } catch (IOException e) {
            throw new RuntimeException("讀取圖片失敗", e);
        }
    }

    // 後台排程 更新狀態
    @Override
    @Transactional
    public void archiveThemesJob() {
        themeRepository.archiveThemesWithoutActiveSessions();
    }
}