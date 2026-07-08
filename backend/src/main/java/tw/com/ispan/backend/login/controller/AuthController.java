package tw.com.ispan.backend.login.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.JwtTokenProvider;
import tw.com.ispan.backend.login.dto.AuthResponse;
import tw.com.ispan.backend.login.dto.GoogleLoginRequest;
import tw.com.ispan.backend.login.dto.LoginRequest;
import tw.com.ispan.backend.login.dto.TwoFactorLoginRequest;
import tw.com.ispan.backend.login.dto.RegisterRequest;
import tw.com.ispan.backend.login.dto.VerifyCodeRequest;
import tw.com.ispan.backend.login.dto.ResetPasswordRequest;
import tw.com.ispan.backend.login.service.AuthService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.GrantedAuthority;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final JwtTokenProvider jwtTokenProvider;

    /**
     * 獲取當前登入使用者資訊
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMe(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "未登入"));
        }

        String roleCode = null;
        // 同時收集「實際擁有的權限碼」(非 ROLE_ 開頭的 authority)，供前端側欄直接依權限過濾選單，
        // 不必再從選單樹反推（避免側欄項目因選單種子結構而漏顯示）。
        java.util.List<String> permissions = new java.util.ArrayList<>();
        if (userDetails.getAuthorities() != null) {
            for (GrantedAuthority authority : userDetails.getAuthorities()) {
                String auth = authority.getAuthority();
                if (auth.startsWith("ROLE_")) {
                    String role = auth.substring(5);
                    if (roleCode == null
                            && ("SUPER_ADMIN".equals(role) || "ADMIN".equals(role) || "CUSTOMER_SERVICE".equals(role))) {
                        roleCode = role;
                    }
                } else {
                    permissions.add(auth);
                }
            }
        }

        return ResponseEntity.ok(Map.of(
                "id", userDetails.getUserId(),
                "username", userDetails.getUsername(),
                "name", userDetails.getName(),
                "email", userDetails.getUsername(),
                "roleCode", roleCode != null ? roleCode : "",
                "avatarUrl", userDetails.getAvatarUrl() != null ? userDetails.getAvatarUrl() : "",
                "permissions", permissions,
                "mustChangePassword", userDetails.getMustChangePassword() != null ? userDetails.getMustChangePassword() : false
        ));
    }

    /**
     * 註冊帳號
     */
    @PostMapping("/register")
    public ResponseEntity<?> registerUser(@Valid @RequestBody RegisterRequest registerRequest) {
        log.info("接收到註冊 API 請求，Email: {}", registerRequest.getEmail());
        try {
            authService.register(registerRequest);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("success", true, "message", "註冊成功"));
        } catch (IllegalArgumentException ex) {
            log.warn("註冊 API 業務例外: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("註冊 API 系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統發生錯誤，請稍後再試"));
        }
    }

    /**
     * 登入驗證
     */
    @PostMapping("/login")
    public ResponseEntity<?> loginUser(@Valid @RequestBody LoginRequest loginRequest, HttpServletRequest request) {
        log.info("接收到登入 API 請求，Email: {}", loginRequest.getEmail());

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            AuthResponse response = authService.login(loginRequest, ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.warn("登入 API 驗證未通過: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("登入 API 系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統發生錯誤，請稍後再試"));
        }
    }

    /**
     * Google 第三方登入（前端 Google Identity Services 取得 ID Token 後送入驗證）
     */
    @PostMapping("/google")
    public ResponseEntity<?> googleLogin(@Valid @RequestBody GoogleLoginRequest googleRequest, HttpServletRequest request) {
        log.info("接收到 Google 登入 API 請求");

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            AuthResponse response = authService.loginWithGoogle(googleRequest.getIdToken(), ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.warn("Google 登入未通過: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("Google 登入 API 系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統發生錯誤，請稍後再試"));
        }
    }

    /**
     * 2FA 登入第二步：驗證寄至信箱的登入驗證碼
     */
    @PostMapping("/login/2fa")
    public ResponseEntity<?> loginTwoFactor(@Valid @RequestBody TwoFactorLoginRequest twoFactorRequest, HttpServletRequest request) {
        log.info("接收到 2FA 登入第二步驗證請求，Email: {}", twoFactorRequest.getEmail());

        String ipAddress = getClientIp(request);
        String userAgent = request.getHeader("User-Agent");

        try {
            AuthResponse response = authService.completeTwoFactorLogin(
                    twoFactorRequest.getEmail(), twoFactorRequest.getCode(), ipAddress, userAgent);
            return ResponseEntity.ok(response);
        } catch (RuntimeException ex) {
            log.warn("2FA 登入驗證未通過: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("2FA 登入 API 系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統發生錯誤，請稍後再試"));
        }
    }

    /**
     * 登出
     */
    @PostMapping("/logout")
    public ResponseEntity<?> logoutUser(@RequestHeader("Authorization") String authHeader) {
        log.info("接收到登出 API 請求");
        try {
            authService.logout(authHeader);
            return ResponseEntity.ok(Map.of("success", true, "message", "登出成功"));
        } catch (IllegalArgumentException ex) {
            log.warn("登出 API 參數例外: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("登出 API 系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "登出處理時發生錯誤"));
        }
    }

    /**
     * 登出所有其他裝置
     */
    @PostMapping("/logout-others")
    public ResponseEntity<?> logoutOthers(@RequestHeader("Authorization") String authHeader) {
        log.info("接收到登出其他裝置 API 請求");
        try {
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "無效的認證標頭"));
            }
            String token = authHeader.substring(7);
            String userId = jwtTokenProvider.getUserIdFromToken(token);
            String jti = jwtTokenProvider.getJtiFromToken(token);

            authService.logoutOtherDevices(userId, jti);
            return ResponseEntity.ok(Map.of("success", true, "message", "已成功登出其他裝置"));
        } catch (Exception ex) {
            log.error("登出其他裝置 API 系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "處理登出其他裝置時發生錯誤"));
        }
    }

    /**
     * 檢查 Email 是否已被註冊 (用於前端分流)
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        log.info("接收到檢查 Email 註冊狀態請求，Email: {}", email);
        // 回傳 exists 與 authProvider，讓前端能將 Google 帳號導向 Google 登入而非密碼步驟
        return ResponseEntity.ok(authService.checkEmail(email));
    }

    /**
     * 發送驗證碼 OTP
     */
    @PostMapping("/send-code")
    public ResponseEntity<?> sendVerificationCode(@RequestParam String email) {
        log.info("接收到發送驗證碼請求，Email: {}", email);
        try {
            authService.sendOtp(email);
            return ResponseEntity.ok(Map.of("success", true, "message", "驗證碼已寄送至您的信箱"));
        } catch (Exception ex) {
            log.error("發送驗證碼發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "發送驗證碼失敗，請稍後再試"));
        }
    }

    /**
     * 校驗驗證碼 OTP
     */
    @PostMapping("/verify-code")
    public ResponseEntity<?> verifyCode(@Valid @RequestBody VerifyCodeRequest verifyRequest) {
        log.info("接收到驗證驗證碼請求，Email: {}", verifyRequest.getEmail());
        boolean success = authService.verifyOtp(verifyRequest.getEmail(), verifyRequest.getCode());
        if (success) {
            return ResponseEntity.ok(Map.of("success", true, "message", "驗證碼校驗成功"));
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "驗證碼不正確或已過期"));
        }
    }

    /**
     * 忘記密碼與重設密碼
     */
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@Valid @RequestBody ResetPasswordRequest resetRequest) {
        log.info("接收到重設密碼請求，Email: {}", resetRequest.getEmail());
        try {
            authService.resetPassword(resetRequest.getEmail(), resetRequest.getCode(), resetRequest.getPassword());
            return ResponseEntity.ok(Map.of("success", true, "message", "密碼重設成功"));
        } catch (IllegalArgumentException ex) {
            log.warn("重設密碼失敗: {}", ex.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("重設密碼發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統發生錯誤，請稍後再試"));
        }
    }

    /**
     * 輔助方法：獲取 Client 的真實 IP 位址
     */
    private String getClientIp(HttpServletRequest request) {
        String ipAddress = request.getHeader("X-Forwarded-For");
        if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
            ipAddress = request.getRemoteAddr();
        } else {
            // 多層代理時，取第一個非 unknown 的 IP
            ipAddress = ipAddress.split(",")[0].trim();
        }
        return ipAddress;
    }
}
