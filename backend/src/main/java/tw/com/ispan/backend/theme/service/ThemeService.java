package tw.com.ispan.backend.theme.service;

import java.util.List;

import org.springframework.data.domain.Page;

import org.springframework.web.multipart.MultipartFile;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeCreateRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeQueryRequest;
import tw.com.ispan.backend.theme.dto.request.ThemeUpdateRequest;
import tw.com.ispan.backend.theme.dto.response.OrganizerThemeResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeImageResource;
import tw.com.ispan.backend.theme.dto.response.ThemeListResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeResponse;

public interface ThemeService {

    // 前台，查詢所有active的主題
    Page<ThemeListResponse> getThemes(ThemeQueryRequest request);

    // 前台，查詢特定themeId的主題
    ThemeResponse getActiveThemeById(Integer themeId);

    // 後台，查詢特定organizerId的主題
    List<OrganizerThemeResponse> getThemesByOrganizerId(String organizerId);

    // 後台，新增主題
    ThemeResponse createTheme(ThemeCreateRequest request);

    // 後台，更新主題
    ThemeResponse updateTheme(Integer themeId, ThemeUpdateRequest request);

    // 後台，更新主題狀態 (草稿->公開，草稿->軟刪除)
    ThemeResponse updateStatus(Integer themeId, StatusUpdateRequest request);

    // 圖片：上傳 (取得路徑)
    ThemeResponse uploadThemeImage(Integer themeId, MultipartFile file);

    // 圖片：更新
    ThemeResponse updateImage(Integer themeId, ThemeUpdateRequest request);

    // 圖片：讀取
    ThemeImageResource serveThemeImage(String fileName);

    // 後台排程 更新狀態
    void archiveThemesJob();

}