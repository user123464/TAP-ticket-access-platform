package tw.com.ispan.backend.config.security;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;

@SpringBootTest
@Transactional // 測試結束後自動 Rollback，保持資料庫乾淨
public class CustomUserDetailsServiceTest {

    @Autowired
    private CustomUserDetailsService userDetailsService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testLoadUserByUsername_Success() {
        // 1. 建立並存入測試使用者
        User user = User.builder()
                .userId("USR9999901")
                .email("test.detail@ispan.com")
                .passwordHash("hashed_password_xyz")
                .name("測試用詳細資料使用者")
                .isActive(true)
                .isDeleted(false)
                .build();
        userRepository.save(user);

        entityManager.flush();
        entityManager.clear(); // 強制清空快取

        // 2. 透過 Service 載入 UserDetails
        UserDetails userDetails = userDetailsService.loadUserByUsername("test.detail@ispan.com");
        assertNotNull(userDetails);
        assertEquals("test.detail@ispan.com", userDetails.getUsername());
        assertEquals("hashed_password_xyz", userDetails.getPassword());
        assertTrue(userDetails.isEnabled());
        assertTrue(userDetails.isAccountNonLocked());

        // 3. 驗證自定義擴充的欄位是否可正常讀取
        assertTrue(userDetails instanceof CustomUserDetailsService.CustomUserDetails);
        CustomUserDetailsService.CustomUserDetails customDetails = (CustomUserDetailsService.CustomUserDetails) userDetails;
        assertEquals("USR9999901", customDetails.getUserId());
        assertEquals("測試用詳細資料使用者", customDetails.getName());
    }

    @Test
    public void testLoadUserByUsername_UserNotFound() {
        // 驗證找不到使用者時必須拋出 UsernameNotFoundException
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("nonexistent@ispan.com");
        });
    }

    @Test
    public void testAccountLocked() {
        // 建立一個 lockedUntil 在未來（15分鐘後）的鎖定帳號
        User user = User.builder()
                .userId("USR9999902")
                .email("locked.user@ispan.com")
                .passwordHash("hashed_pwd")
                .name("被鎖定的使用者")
                .lockedUntil(LocalDateTime.now().plusMinutes(15))
                .isActive(true)
                .isDeleted(false)
                .build();
        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        // 驗證 isAccountNonLocked 回傳 false
        UserDetails userDetails = userDetailsService.loadUserByUsername("locked.user@ispan.com");
        assertNotNull(userDetails);
        assertFalse(userDetails.isAccountNonLocked());
    }

    @Test
    public void testAccountDisabled() {
        // 建立一個 isActive = false 的停用帳號
        User user = User.builder()
                .userId("USR9999903")
                .email("disabled.user@ispan.com")
                .passwordHash("hashed_pwd")
                .name("被停用的使用者")
                .isActive(false)
                .isDeleted(false)
                .build();
        userRepository.save(user);

        entityManager.flush();
        entityManager.clear();

        // 驗證 isEnabled 回傳 false
        UserDetails userDetails = userDetailsService.loadUserByUsername("disabled.user@ispan.com");
        assertNotNull(userDetails);
        assertFalse(userDetails.isEnabled());
    }
}
