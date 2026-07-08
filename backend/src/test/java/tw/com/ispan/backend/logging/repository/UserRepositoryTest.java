package tw.com.ispan.backend.logging.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.repository.UserRepository;

@SpringBootTest
public class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    public void testUserCRUD() {
        // 1. 建立一個測試用的 User 物件 (使用 Builder 模式)
        User user = User.builder()
                .userId("USR9999999") // 主鍵
                .email("test.teacher@ispan.com")
                .passwordHash("hashed_password_sample")
                .name("測試老師")
                .authProvider(AuthProvider.LOCAL)
                .isTwoFactorEnabled(false)
                .isActive(true)
                .isDeleted(false)
                .build();

        // 2. 儲存到資料庫 (Create)
        User savedUser = userRepository.save(user);
        assertNotNull(savedUser);
        assertEquals("test.teacher@ispan.com", savedUser.getEmail());

        // 驗證自動審計：檢查 createdAt 與 updatedAt 是否自動被填入時間
        assertNotNull(savedUser.getCreatedAt());
        assertNotNull(savedUser.getUpdatedAt());

        // 3. 根據 Email 查詢 (Read)
        Optional<User> foundOpt = userRepository.findByEmail("test.teacher@ispan.com");
        assertTrue(foundOpt.isPresent());
        User foundUser = foundOpt.get();
        assertEquals("測試老師", foundUser.getName());

        // 4. 修改使用者姓名 (Update)
        foundUser.setName("測試老師-已修改");
        User updatedUser = userRepository.save(foundUser);
        assertEquals("測試老師-已修改", updatedUser.getName());

        // 5. 軟刪除使用者 (Delete)
        // 注意：因為上一步更新了資料，資料庫中的 row_version 已經自動更新，但 Java 記憶體中的 updatedUser 物件仍持有舊版本號。
        // 我們必須重新自資料庫查詢最新的 User 實體（取得最新 row_version），再進行刪除，否則會觸發樂觀鎖異常
        // (ObjectOptimisticLockingFailureException)。
        User userToDelete = userRepository.findById(updatedUser.getUserId()).get();
        userRepository.delete(userToDelete);

        // 6. 再次查詢驗證軟刪除
        // 由於我們加了 @SQLRestriction("is_deleted = 0")，在此查詢應該找不到該使用者了
        Optional<User> deletedOpt = userRepository.findById("USR9999999");
        assertFalse(deletedOpt.isPresent());
    }
}
