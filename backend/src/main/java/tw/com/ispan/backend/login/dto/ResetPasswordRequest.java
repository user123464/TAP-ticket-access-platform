package tw.com.ispan.backend.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ResetPasswordRequest {
    @NotBlank(message = "電子信箱不可為空")
    @Email(message = "電子信箱格式不正確")
    private String email;

    @NotBlank(message = "驗證碼不可為空")
    private String code;

    @NotBlank(message = "新密碼不可為空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼必須包含至少一個小寫字母、一個大寫字母和一個數字，且長度至少為8個字元")
    private String password;
}
