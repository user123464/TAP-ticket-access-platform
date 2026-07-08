package tw.com.ispan.backend.organizer.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class InviteMemberRequest {
    @NotBlank(message = "受邀人電子信箱不能為空")
    @Email(message = "電子信箱格式不正確")
    private String email;

    @NotBlank(message = "邀請指派的角色不能為空")
    private String roleId; // 例如 ADMIN / FINANCE / STAFF 等
}
