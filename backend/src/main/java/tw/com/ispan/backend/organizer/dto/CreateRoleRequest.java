package tw.com.ispan.backend.organizer.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateRoleRequest {

    @NotBlank(message = "角色名稱不得為空")
    private String roleName;

    private String description;

    private List<String> permissions;
}
