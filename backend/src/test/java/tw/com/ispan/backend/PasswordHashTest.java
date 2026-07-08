package tw.com.ispan.backend;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

/**
 * 臨時維護工具：密碼雜湊生成器
 * 
 * 【用途】
 * 在開發與後續維護期間，有時候我們需要手動去資料庫 (`data.sql` 或直接操作 SQL)
 * 插入或修改使用者的密碼。由於我們的系統使用 BCrypt 進行加密，無法直接將明文密碼寫入資料庫，
 * 否則登入時會報錯（解析失敗或比對失敗）。
 * 
 * 【使用方式】
 * 1. 將下方 `encode("你的密碼")` 中的字串換成你想要加密的明文。
 * 2. 在 IDE 中直接點擊執行這個 Test 方法（或使用 `mvnw test -Dtest=PasswordHashTest`）。
 * 3. 複製 Console 印出來的字串（格式為 $2a$10$...）。
 * 4. 將複製出來的字串貼到資料庫 `password_hash` 欄位中。
 * 
 * 註：雖然此工具不屬於核心業務邏輯，但在進行資料庫資料預建 (Seed Data) 
 * 或是除錯管理員帳號時非常實用，因此保留此檔案供維護使用。
 */
public class PasswordHashTest {
    
    @Test
    public void generateHash() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        
        // 將這裡的 Aa123456 替換成你想加密的密碼
        String rawPassword = "Aa123456";
        String encodedPassword = encoder.encode(rawPassword);
        
        System.out.println("=================================================");
        System.out.println("【明文密碼】: " + rawPassword);
        System.out.println("【BCRYPT 雜湊結果】: " + encodedPassword);
        System.out.println("=================================================");
    }
}
