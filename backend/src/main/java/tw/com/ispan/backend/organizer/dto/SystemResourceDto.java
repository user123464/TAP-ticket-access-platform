package tw.com.ispan.backend.organizer.dto;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemResourceDto {
    private String resourceId;
    private String portalType;
    private String resourceType;
    private String name;
    private String urlPath;
    private String permissionId;
    private Integer sortOrder;
    private String icon;       // [Jason] 選單圖示（bootstrap-icons class）
    private Boolean visible;   // [Jason] 是否顯示於選單

    @Builder.Default
    private List<SystemResourceDto> children = new ArrayList<>();
}
