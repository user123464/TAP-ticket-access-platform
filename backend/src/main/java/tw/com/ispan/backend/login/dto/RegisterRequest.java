package tw.com.ispan.backend.login.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegisterRequest {

    @NotBlank(message = "電子信箱不能為空")
    @Email(message = "電子信箱格式不正確")
    @Size(max = 100, message = "電子信箱長度不能超過 100 個字元")
    private String email;

    @NotBlank(message = "密碼不能為空")
    @Pattern(regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$", message = "密碼必須包含至少一個小寫字母、一個大寫字母和一個數字，且長度至少為8個字元")
    private String password;

    @NotBlank(message = "姓名不能為空")
    @Size(max = 50, message = "姓名長度不能超過 50 個字元")
    private String name;

    @Size(max = 20, message = "電話長度不能超過 20 個字元")
    private String phone;

    @Size(max = 1, message = "性別格式不正確")
    private String gender; // M=男, F=女, O=其他

    private LocalDate birthDate;

    @Size(max = 255, message = "地址長度不能超過 255 個字元")
    private String address;

    private String avatarUrl;
}
