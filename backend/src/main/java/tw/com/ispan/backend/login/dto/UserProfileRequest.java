package tw.com.ispan.backend.login.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileRequest {

    @NotBlank(message = "姓名不能為空")
    @Size(max = 50, message = "姓名長度不能超過 50 個字元")
    private String name;

    @Size(max = 20, message = "電話長度不能超過 20 個字元")
    private String phone;

    @Size(max = 1, message = "性別格式不正確")
    private String gender; // M=男, F=女, O=其他, 或者為空代表不透露

    private LocalDate birthDate;

    @Size(max = 255, message = "地址長度不能超過 255 個字元")
    private String address;

    @Size(max = 255, message = "頭像連結長度不能超過 255 個字元")
    private String avatarUrl;
}
