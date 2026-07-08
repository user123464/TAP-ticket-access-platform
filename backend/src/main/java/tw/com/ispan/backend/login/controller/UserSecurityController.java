package tw.com.ispan.backend.login.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.List;
import tw.com.ispan.backend.config.security.JwtTokenProvider;
import tw.com.ispan.backend.login.dto.UserSessionResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.login.dto.ChangePasswordRequest;
import tw.com.ispan.backend.login.dto.TwoFactorSettingRequest;
import tw.com.ispan.backend.login.exception.AccountDeletionBlockedException;
import tw.com.ispan.backend.login.service.AuthService;
import tw.com.ispan.backend.login.service.UserProfileService;

/**
 * 帳號安全相關操作（需登入）：變更密碼、2FA 開關。
 */
@Slf4j
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserSecurityController {

    private final UserProfileService userProfileService;
    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 變更登入密碼（僅限本地帳號）
     */
    @PutMapping("/password")
    public ResponseEntity<?> changePassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {

        if (userDetails == null) {
            return unauthorized();
        }

        try {
            userProfileService.changePassword(
                    userDetails.getUserId(), request.getCurrentPassword(), request.getNewPassword());
            return ResponseEntity.ok(Map.of("success", true, "message", "密碼變更成功"));
        } catch (IllegalArgumentException ex) {
            log.warn("變更密碼失敗: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("變更密碼時發生系統錯誤", ex);
            return serverError();
        }
    }

    /**
     * 即時驗證目前密碼是否正確（設定頁變更密碼用，回傳 { valid: boolean }）
     */
    @PostMapping("/password/verify")
    public ResponseEntity<?> verifyCurrentPassword(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody ChangePasswordRequest request) {

        if (userDetails == null) {
            return unauthorized();
        }
        boolean valid = userProfileService.verifyCurrentPassword(
                userDetails.getUserId(), request.getCurrentPassword());
        return ResponseEntity.ok(Map.of("valid", valid));
    }

    /**
     * 寄送「啟用 2FA」設定驗證碼至目前登入使用者的信箱
     */
    @PostMapping("/2fa/send-code")
    public ResponseEntity<?> sendTwoFactorSetupCode(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return unauthorized();
        }
        try {
            authService.sendTwoFactorSetupOtp(userDetails.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "驗證碼已寄送至您的信箱"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("寄送 2FA 設定驗證碼時發生系統錯誤", ex);
            return serverError();
        }
    }

    /**
     * 啟用 / 關閉 2FA（啟用需提供寄至信箱的驗證碼）
     */
    @PutMapping("/2fa")
    public ResponseEntity<?> setTwoFactor(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody TwoFactorSettingRequest request) {

        if (userDetails == null) {
            return unauthorized();
        }
        try {
            authService.setTwoFactorEnabled(userDetails.getUserId(), request.getEnable(), request.getCode());
            String msg = Boolean.TRUE.equals(request.getEnable()) ? "雙重驗證已啟用" : "雙重驗證已關閉";
            return ResponseEntity.ok(Map.of("success", true, "message", msg));
        } catch (IllegalArgumentException ex) {
            log.warn("設定 2FA 失敗: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("設定 2FA 時發生系統錯誤", ex);
            return serverError();
        }
    }

    /**
     * 刪除帳號（軟刪除）。若仍綁定主辦方組織則回傳 409 與 blocked 旗標。
     */
    @DeleteMapping("/account")
    public ResponseEntity<?> deleteAccount(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return unauthorized();
        }
        try {
            userProfileService.deleteAccount(userDetails.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "帳號已刪除"));
        } catch (AccountDeletionBlockedException ex) {
            log.warn("刪除帳號被阻擋: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("success", false, "blocked", true, "message", ex.getMessage()));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("刪除帳號時發生系統錯誤", ex);
            return serverError();
        }
    }

    /**
     * 獲取目前使用者的有效登入裝置會話
     */
    @GetMapping("/sessions")
    public ResponseEntity<?> getActiveSessions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestHeader("Authorization") String authHeader) {
        if (userDetails == null) {
            return unauthorized();
        }
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "無效的認證標頭"));
        }
        try {
            String token = authHeader.substring(7);
            String currentJti = jwtTokenProvider.getJtiFromToken(token);
            List<UserSessionResponse> sessions = authService.getActiveSessions(userDetails.getUserId(), currentJti);
            return ResponseEntity.ok(sessions);
        } catch (Exception ex) {
            log.error("查詢登入裝置列表時發生系統錯誤", ex);
            return serverError();
        }
    }

    /**
     * 登出指定裝置
     */
    @DeleteMapping("/sessions/{sessionId}")
    public ResponseEntity<?> revokeSession(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable Long sessionId) {
        if (userDetails == null) {
            return unauthorized();
        }
        try {
            authService.revokeSession(userDetails.getUserId(), sessionId);
            return ResponseEntity.ok(Map.of("success", true, "message", "已成功登出該裝置"));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("登出特定裝置時發生系統錯誤", ex);
            return serverError();
        }
    }

    /**
     * 登出所有裝置
     */
    @DeleteMapping("/sessions")
    public ResponseEntity<?> revokeAllSessions(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return unauthorized();
        }
        try {
            authService.revokeAllUserSessions(userDetails.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "已成功登出所有裝置"));
        } catch (Exception ex) {
            log.error("登出所有裝置時發生系統錯誤", ex);
            return serverError();
        }
    }

    private ResponseEntity<?> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", "認證失敗，請重新登入"));
    }

    private ResponseEntity<?> serverError() {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("success", false, "message", "系統錯誤，請稍後再試"));
    }
}
