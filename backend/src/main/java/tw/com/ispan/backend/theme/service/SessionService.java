package tw.com.ispan.backend.theme.service;

import java.util.List;

import tw.com.ispan.backend.theme.dto.request.SessionCreateRequest;
import tw.com.ispan.backend.theme.dto.request.SessionUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.dto.response.SessionResponse;


public interface SessionService {
    //前台的sessions (ACTIVE)
    List<SessionResponse> getActiveSessionsByThemeId(Integer themeId);

    //後台的sessions (ThemeId)
    List<SessionResponse> getSessionsByThemeId(Integer themeId);

    //後台，新增場次
    SessionResponse createSession(Integer themeId, SessionCreateRequest request);

    //後台，更新場次
    SessionResponse updateSession(Integer sessionId, SessionUpdateRequest request);

    //後台，更新場次狀態 (草稿->公開，草稿->軟刪除)
    SessionResponse updateStatus(Integer sessionId, StatusUpdateRequest request);

    // 排程：將已過期且仍為 ACTIVE 的場次封存
    void archiveExpiredSessionsJob();

}