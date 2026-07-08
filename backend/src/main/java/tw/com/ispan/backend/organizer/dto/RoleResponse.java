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
public class RoleResponse {
    private String roleId;
    private String roleName;
    private String description;
    private Boolean isEditable;
    private List<String> permissions; // List of permissionIds
}
