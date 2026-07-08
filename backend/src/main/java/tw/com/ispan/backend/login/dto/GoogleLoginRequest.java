package tw.com.ispan.backend.login.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GoogleLoginRequest {

    @NotBlank(message = "缺少 Google ID Token")
    private String idToken; // 前端 Google Identity Services 取得的 ID Token (JWT)
}
