package tw.com.ispan.backend.login.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.backend.login.dto.AuthResponse;
import tw.com.ispan.backend.login.dto.LoginRequest;
import tw.com.ispan.backend.login.dto.RegisterRequest;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.entity.UserSession;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.login.repository.UserSessionRepository;

@SpringBootTest
@Transactional
public class AuthServiceTest {

    @Autowired
    private AuthService authService;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserSessionRepository userSessionRepository;

    @Autowired
    private tw.com.ispan.backend.config.security.JwtTokenProvider jwtTokenProvider;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    @Autowired
    private RedisService redisService;

    private String testEmail = "auth.test.service@ispan.com";
    private String testPassword = "securePassword123";

    private void mockOtpVerified(String email) {
        redisService.setWithTtl("otp:verified:" + email, "true", 600);
    }

    @org.junit.jupiter.api.BeforeEach
    public void setup() {
        userRepository.findByEmail(testEmail).ifPresent(u -> userRepository.delete(u));
        userRepository.flush();
        redisService.delete("otp:verified:" + testEmail);
    }

    @Test
    public void testUserRegisterSuccess() {
        // 1. 執行註冊
        RegisterRequest request = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .name("認證服務測試員")
                .build();

        mockOtpVerified(testEmail);
        authService.register(request);
        entityManager.flush();
        entityManager.clear();

        // 2. 驗證資料庫是否有此筆資料且 ID 格式符合 USRXXXXXXX
        Optional<User> userOpt = userRepository.findByEmail(testEmail);
        assertTrue(userOpt.isPresent());
        User user = userOpt.get();
        assertEquals("認證服務測試員", user.getName());
        assertNotNull(user.getUserId());
        assertTrue(user.getUserId().startsWith("USR"));
        assertEquals(10, user.getUserId().length());
    }

    @Test
    public void testUserRegisterDuplicateEmailFails() {
        // 1. 執行第一次註冊
        RegisterRequest request = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .name("認證服務測試員")
                .build();
        mockOtpVerified(testEmail);
        authService.register(request);
        entityManager.flush();
        entityManager.clear();

        // 2. 使用相同 Email 再次註冊，應拋出例外
        RegisterRequest dupRequest = RegisterRequest.builder()
                .email(testEmail)
                .password("otherPassword")
                .name("複製人")
                .build();

        assertThrows(IllegalArgumentException.class, () -> {
            authService.register(dupRequest);
        });
    }

    @Test
    public void testUserLoginSuccess() {
        // 1. 註冊一個使用者
        RegisterRequest regReq = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .name("認證服務測試員")
                .build();
        mockOtpVerified(testEmail);
        authService.register(regReq);
        entityManager.flush();
        entityManager.clear();

        // 2. 執行登入
        LoginRequest loginReq = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        AuthResponse response = authService.login(loginReq, "127.0.0.1", "Mozilla/5.0");

        // 3. 驗證登入結果與 Session
        assertNotNull(response);
        assertNotNull(response.getAccessToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(testEmail, response.getEmail());
        assertEquals("認證服務測試員", response.getName());

        // 4. 驗證是否在資料庫寫入 UserSession
        String jti = jwtTokenProvider.getJtiFromToken(response.getAccessToken());
        Optional<UserSession> sessionOpt = userSessionRepository.findByTokenJti(jti);
        assertTrue(sessionOpt.isPresent());
        // 直接撈取最新建立的 Session
        assertFalse(userSessionRepository.findAll().isEmpty());
    }

    @Test
    public void testUserLoginFailedAndLockout() {
        // 1. 註冊使用者
        RegisterRequest regReq = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .name("認證服務測試員")
                .build();
        mockOtpVerified(testEmail);
        authService.register(regReq);
        entityManager.flush();
        entityManager.clear();

        // 2. 連續 5 次使用錯誤密碼登入
        LoginRequest wrongLoginReq = LoginRequest.builder()
                .email(testEmail)
                .password("wrongPassword")
                .build();

        for (int i = 0; i < 4; i++) {
            assertThrows(RuntimeException.class, () -> {
                authService.login(wrongLoginReq, "127.0.0.1", "Mozilla/5.0");
            });
        }

        // 第 5 次錯誤登入，應觸發鎖定
        assertThrows(RuntimeException.class, () -> {
            authService.login(wrongLoginReq, "127.0.0.1", "Mozilla/5.0");
        });

        // 3. 驗證資料庫中的 user 是否已被鎖定
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        assertNotNull(user.getLockedUntil());
        assertTrue(user.getLockedUntil().isAfter(LocalDateTime.now()));

        // 4. 此時再以「正確密碼」登入，應因為帳號鎖定而被拒絕
        LoginRequest rightLoginReq = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        Exception exception = assertThrows(RuntimeException.class, () -> {
            authService.login(rightLoginReq, "127.0.0.1", "Mozilla/5.0");
        });
        assertTrue(exception.getMessage().contains("鎖定"));
    }

    @Test
    public void testUserLogout() {
        // 1. 註冊並登入
        RegisterRequest regReq = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .name("認證服務測試員")
                .build();
        mockOtpVerified(testEmail);
        authService.register(regReq);
        entityManager.flush();
        entityManager.clear();

        LoginRequest loginReq = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();
        AuthResponse response = authService.login(loginReq, "127.0.0.1", "Mozilla/5.0");

        String authHeader = "Bearer " + response.getAccessToken();

        // 2. 執行登出
        authService.logout(authHeader);

        // 3. 驗證資料庫的 Session 是否已經有註銷日期
        // 由於我們沒法直接拿到 jti，我們可以先解密或直接從資料庫撈取這個使用者的最後一筆 session
        User user = userRepository.findByEmail(testEmail).orElseThrow();
        UserSession latestSession = userSessionRepository.findAll().stream()
                .filter(s -> s.getUser().getUserId().equals(user.getUserId()))
                .findFirst()
                .orElseThrow();

        assertNotNull(latestSession.getRevokedAt());
        assertEquals(tw.com.ispan.backend.login.enums.RevokedType.SELF, latestSession.getRevokedBy());
    }
}
