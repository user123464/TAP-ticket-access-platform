package tw.com.ispan.backend.login.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TwoFactorSettingRequest {

    @NotNull(message = "缺少啟用狀態參數")
    private Boolean enable;

    // 僅在 enable=true 時需要：寄至信箱的設定驗證碼
    private String code;
}
