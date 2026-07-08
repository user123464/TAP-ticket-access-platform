package tw.com.ispan.backend.login.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorLoginRequest {

    @NotBlank(message = "電子信箱不能為空")
    @Email(message = "電子信箱格式不正確")
    private String email;

    @NotBlank(message = "請輸入驗證碼")
    private String code;
}
