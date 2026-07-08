package tw.com.ispan.backend.login.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChangePasswordRequest {

    private String currentPassword;

    @NotBlank(message = "新密碼不能為空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼必須包含至少一個小寫字母、一個大寫字母和一個數字，且長度至少為8個字元")
    private String newPassword;
}
