package tw.com.ispan.backend.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    
    private String accessToken;
    
    @Builder.Default
    private String tokenType = "Bearer";
    
    private String userId;
    private String email;
    private String name;
    private String avatarUrl;
    private String roleCode;
    private Boolean mustChangePassword;

    // 若帳號已啟用 2FA，登入第一步密碼正確後回傳此旗標（此時 accessToken 為 null），
    // 前端需引導使用者輸入寄至信箱的驗證碼，再呼叫 /api/auth/login/2fa 完成登入。
    @Builder.Default
    private Boolean twoFactorRequired = false;
}
