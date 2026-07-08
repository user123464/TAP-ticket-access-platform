package tw.com.ispan.backend.system.dto;

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
public class UserSubmissionRequest {

    @NotBlank(message = "聯絡姓名不能為空")
    private String name;

    @NotBlank(message = "電子信箱不能為空")
    @Email(message = "電子信箱格式不正確")
    private String email;

    private String company;

    @NotBlank(message = "問題分類不能為空")
    private String category;

    @NotBlank(message = "問題描述不能為空")
    private String description;
}
