package tw.com.ispan.backend.logging.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import tw.com.ispan.backend.login.entity.LoginAttempt;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.entity.UserSession;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.enums.LoginFailureReason;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.login.enums.RevokedType;
import tw.com.ispan.backend.login.repository.LoginAttemptRepository;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.login.repository.UserSessionRepository;

@SpringBootTest
public class UserSessionRepositoryTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private UserSessionRepository userSessionRepository;

        @Autowired
        private LoginAttemptRepository loginAttemptRepository;

        @Autowired
        private EntityManager entityManager;

        @Test
        @Transactional
        public void testUserSessionAndLoginAttemptCRUD() {
                // ── 1. 建立測試使用者 ──
                User user = User.builder()
                                .userId("USR7777701")
                                .email("test.jason@ispan.com")
                                .name("詹森同學")
                                .authProvider(AuthProvider.LOCAL)
                                .passwordHash("hashed_pwd")
                                .isActive(true)
                                .isDeleted(false)
                                .isTwoFactorEnabled(false)
                                .build();
                userRepository.save(user);

                // ── 2. 建立登入會話 UserSession (C) ──
                UserSession session = UserSession.builder()
                                .user(user)
                                .tokenJti("unique-jwt-jti-123456")
                                .portalType(PortalType.B2C_FRONT)
                                .ipAddress("127.0.0.1")
                                .userAgent("Mozilla/5.0")
                                .expiresAt(LocalDateTime.now().plusDays(1))
                                .build();
                UserSession savedSession = userSessionRepository.save(session);

                assertNotNull(savedSession.getSessionId()); // 驗證自增主鍵已生成
                assertNotNull(savedSession.getCreatedAt()); // 驗證自動審計時間

                // ── 3. 測試會話查詢 (R) ──
                entityManager.flush();
                entityManager.clear(); // 清空快取，確保真的從資料庫查詢

                Optional<UserSession> sessionOpt = userSessionRepository.findByTokenJti("unique-jwt-jti-123456");
                assertTrue(sessionOpt.isPresent());
                UserSession sessionInDb = sessionOpt.get();
                assertEquals(PortalType.B2C_FRONT, sessionInDb.getPortalType());
                assertNull(sessionInDb.getRevokedAt()); // 剛登入時撤銷時間應為 null

                // ── 4. 測試模擬登出/撤銷會話 (U) ──
                sessionInDb.setRevokedAt(LocalDateTime.now());
                sessionInDb.setRevokedBy(RevokedType.SELF);
                UserSession updatedSession = userSessionRepository.saveAndFlush(sessionInDb);

                assertNotNull(updatedSession.getRevokedAt());
                assertEquals(RevokedType.SELF, updatedSession.getRevokedBy());

                // ── 5. 測試登入嘗試紀錄 LoginAttempt (C) ──
                // 模擬 3 次失敗，1 次成功
                LoginAttempt attempt1 = LoginAttempt.builder()
                                .email("test.jason@ispan.com")
                                .ipAddress("127.0.0.1")
                                .success(false)
                                .failureReason(LoginFailureReason.WRONG_PASSWORD)
                                .build();
                loginAttemptRepository.save(attempt1);

                LoginAttempt attempt2 = LoginAttempt.builder()
                                .email("test.jason@ispan.com")
                                .ipAddress("127.0.0.1")
                                .success(false)
                                .failureReason(LoginFailureReason.WRONG_PASSWORD)
                                .build();
                loginAttemptRepository.save(attempt2);

                LoginAttempt attempt3 = LoginAttempt.builder()
                                .email("test.jason@ispan.com")
                                .ipAddress("127.0.0.1")
                                .success(false)
                                .failureReason(LoginFailureReason.ACCOUNT_LOCKED)
                                .build();
                loginAttemptRepository.save(attempt3);

                LoginAttempt attemptSuccess = LoginAttempt.builder()
                                .email("test.jason@ispan.com")
                                .ipAddress("127.0.0.1")
                                .success(true)
                                .build();
                loginAttemptRepository.save(attemptSuccess);

                entityManager.flush();
                entityManager.clear();

                // ── 6. 測試失敗次數統計與排序查詢 (R) ──
                // 計算過去 10 分鐘內的失敗次數
                LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
                long failedCount = loginAttemptRepository
                                .countByEmailAndSuccessFalseAndAttemptedAtAfter("test.jason@ispan.com", tenMinutesAgo);
                assertEquals(3, failedCount); // 應該有 3 次失敗

                // 計算過去 10 分鐘內「其他信箱」的失敗次數 (應該為 0)
                long otherFailedCount = loginAttemptRepository
                                .countByEmailAndSuccessFalseAndAttemptedAtAfter("other@test.com", tenMinutesAgo);
                assertEquals(0, otherFailedCount);

                // 查詢該信箱的所有嘗試紀錄，驗證時間排序是否為最新在最前
                List<LoginAttempt> attempts = loginAttemptRepository
                                .findByEmailOrderByAttemptedAtDesc("test.jason@ispan.com");
                assertEquals(4, attempts.size());
                assertTrue(attempts.get(0).getSuccess()); // 最新的一筆應該是成功的那次
                assertFalse(attempts.get(1).getSuccess()); // 倒數第二筆應該是失敗
        }
}
