package tw.com.ispan.backend.login.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.backend.login.dto.UserProfileRequest;
import tw.com.ispan.backend.login.dto.UserProfileResponse;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.repository.UserRepository;

@SpringBootTest
@Transactional
public class UserProfileServiceTest {

    @Autowired
    private UserProfileService userProfileService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private jakarta.persistence.EntityManager entityManager;

    private final String testEmail = "profile.test.service@ispan.com";
    private String savedUserId;

    @BeforeEach
    public void setup() {
        // 清理殘留資料
        userRepository.findByEmail(testEmail).ifPresent(u -> userRepository.delete(u));
        userRepository.flush();

        // 註冊一個測試使用者
        User user = User.builder()
                .userId("USR9999901")
                .email(testEmail)
                .passwordHash("hashedPassword")
                .name("個人檔案測試員")
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .isDeleted(false)
                .build();

        User savedUser = userRepository.save(user);
        savedUserId = savedUser.getUserId();

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    public void testGetUserProfileSuccess() {
        // 執行查詢
        UserProfileResponse response = userProfileService.getUserProfile(savedUserId);

        // 驗證回傳的個人資料
        assertNotNull(response);
        assertEquals(savedUserId, response.getUserId());
        assertEquals(testEmail, response.getEmail());
        assertEquals("個人檔案測試員", response.getName());
        assertEquals("LOCAL", response.getAuthProvider());
        assertNotNull(response.getCreatedAt());
        assertNotNull(response.getUpdatedAt());
    }

    @Test
    public void testGetUserProfileUserNotFoundThrows() {
        // 查詢一個不存在的 ID，應拋出例外
        assertThrows(IllegalArgumentException.class, () -> {
            userProfileService.getUserProfile("USR0000000");
        });
    }

    @Test
    public void testUpdateUserProfileSuccess() {
        // 準備修改的資料
        UserProfileRequest updateRequest = UserProfileRequest.builder()
                .name("個人檔案已修改")
                .phone("0912-345-678")
                .gender("M")
                .birthDate(LocalDate.of(1990, 5, 20))
                .address("台北市信義區")
                .avatarUrl("https://example.com/avatar.png")
                .build();

        // 執行更新
        UserProfileResponse response = userProfileService.updateUserProfile(savedUserId, updateRequest);
        entityManager.flush();
        entityManager.clear();

        // 1. 驗證服務回傳值
        assertNotNull(response);
        assertEquals("個人檔案已修改", response.getName());
        assertEquals("0912-345-678", response.getPhone());
        assertEquals("M", response.getGender());
        assertEquals(LocalDate.of(1990, 5, 20), response.getBirthDate());
        assertEquals("台北市信義區", response.getAddress());
        assertEquals("https://example.com/avatar.png", response.getAvatarUrl());

        // 2. 驗證資料庫實際狀態
        User dbUser = userRepository.findById(savedUserId).orElseThrow();
        assertEquals("個人檔案已修改", dbUser.getName());
        assertEquals("0912-345-678", dbUser.getPhone());
        assertEquals("M", dbUser.getGender());
        assertEquals(LocalDate.of(1990, 5, 20), dbUser.getBirthDate());
        assertEquals("台北市信義區", dbUser.getAddress());
        assertEquals("https://example.com/avatar.png", dbUser.getAvatarUrl());
    }

    @Test
    public void testUpdateUserProfileUserNotFoundThrows() {
        UserProfileRequest updateRequest = UserProfileRequest.builder()
                .name("不會成功的修改")
                .build();

        // 更新一個不存在的 ID，應拋出例外
        assertThrows(IllegalArgumentException.class, () -> {
            userProfileService.updateUserProfile("USR0000000", updateRequest);
        });
    }
}
