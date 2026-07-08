package tw.com.ispan.backend.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyCodeRequest {
    @NotBlank(message = "電子信箱不可為空")
    @Email(message = "電子信箱格式不正確")
    private String email;

    @NotBlank(message = "驗證碼不可為空")
    @Pattern(regexp = "^\\d{6}$", message = "驗證碼必須為 6 位數字")
    private String code;
}
