package tw.com.ispan.backend.login.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckEmailResponse {
    private boolean exists;          // 此 Email 是否已註冊
    private String authProvider;     // 已註冊時的登入方式："LOCAL" / "GOOGLE"；未註冊為 null
}
