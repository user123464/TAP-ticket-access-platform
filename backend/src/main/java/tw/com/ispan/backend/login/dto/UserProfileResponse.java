package tw.com.ispan.backend.login.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileResponse {
    private String userId;
    private String email;
    private String name;
    private String phone;
    private String gender;
    private LocalDate birthDate;
    private String address;
    private String avatarUrl;
    private String authProvider; // 例如 "LOCAL" 或 "GOOGLE"
    private Boolean isTwoFactorEnabled; // 是否已啟用登入雙重驗證
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
