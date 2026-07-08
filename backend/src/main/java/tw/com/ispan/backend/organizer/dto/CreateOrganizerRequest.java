package tw.com.ispan.backend.organizer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateOrganizerRequest {
    @NotBlank(message = "組織名稱不能為空")
    private String name;
    private String taxId;
}
