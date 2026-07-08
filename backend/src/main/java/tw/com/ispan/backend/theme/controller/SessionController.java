package tw.com.ispan.backend.theme.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.theme.dto.ApiResponse;
import tw.com.ispan.backend.theme.dto.request.SessionCreateRequest;
import tw.com.ispan.backend.theme.dto.request.SessionUpdateRequest;
import tw.com.ispan.backend.theme.dto.request.StatusUpdateRequest;
import tw.com.ispan.backend.theme.dto.response.SessionResponse;
import tw.com.ispan.backend.theme.service.SessionService;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class SessionController {

    private final SessionService sessionService;

    // 前台，查詢active sessions
    @GetMapping("/themes/{themeId}/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessions(@PathVariable Integer themeId) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getActiveSessionsByThemeId(themeId)));
    }

    // 後台，查詢sessions
    @GetMapping("/org/themes/{themeId}/sessions")
    public ResponseEntity<ApiResponse<List<SessionResponse>>> getActiveSessionsByThemeId(
            @PathVariable Integer themeId) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.getSessionsByThemeId(themeId)));
    }

    // 後台，新增session (隸屬活動，需 EVENT_EDIT)
    @PreAuthorize("hasAuthority('EVENT_EDIT')")
    @PostMapping("/org/themes/{themeId}/sessions")
    public ResponseEntity<ApiResponse<SessionResponse>> createSession(
            @PathVariable Integer themeId,
            @RequestBody SessionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.createSession(themeId, request)));
    }

    // 後台，更新session
    @PutMapping("/org/sessions/{sessionId}")
    public ResponseEntity<ApiResponse<SessionResponse>> updateSession(
            @PathVariable Integer sessionId,
            @RequestBody SessionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.updateSession(sessionId, request)));
    }

    // 後台，更新場次狀態 (草稿->公開，草稿->軟刪除)
    @PutMapping("/org/sessions/{sessionId}/status")
    public ResponseEntity<ApiResponse<SessionResponse>> updateStatus(
            @PathVariable Integer sessionId,
            @RequestBody StatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(sessionService.updateStatus(sessionId, request)));
    }

}