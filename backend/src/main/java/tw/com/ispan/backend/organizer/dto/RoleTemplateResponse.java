package tw.com.ispan.backend.organizer.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoleTemplateResponse {
    private String templateId;
    private String templateName;
    private String description;
    private List<String> permissions; // List of permissionIds
}
