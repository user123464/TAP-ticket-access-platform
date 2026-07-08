package tw.com.ispan.backend.organizer.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.organizer.dto.SystemResourceDto;
import tw.com.ispan.backend.organizer.entity.SystemResource;
import tw.com.ispan.backend.organizer.repository.SystemResourceRepository;

@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class SystemResourceService {

    private final SystemResourceRepository systemResourceRepository;

    public List<SystemResourceDto> getMenuTree(PortalType portalType, Set<String> userPermissions) {
        // 1. 撈出該 PortalType 下所有的根節點 (parent 為 null)
        List<SystemResource> rootResources = systemResourceRepository.findByPortalTypeAndParentIsNullOrderBySortOrderAsc(portalType);

        // 2. 遞迴建構選單樹並進行權限過濾
        return rootResources.stream()
                .filter(res -> hasPermission(res, userPermissions) && isVisible(res))
                .map(res -> convertToDto(res, userPermissions))
                .filter(this::isNotEmptyGroup)
                .collect(Collectors.toList());
    }

    private boolean hasPermission(SystemResource resource, Set<String> userPermissions) {
        // 如果 resource 不需要權限，則公開顯示
        if (resource.getPermission() == null) {
            return true;
        }
        // 如果需要權限，檢查用戶是否擁有該權限 ID
        return userPermissions.contains(resource.getPermission().getPermissionId());
    }

    // [Jason] is_visible=0（如 BUTTON 級資源）不進側欄；null 視為顯示（相容舊資料）
    private boolean isVisible(SystemResource resource) {
        return resource.getIsVisible() == null || resource.getIsVisible();
    }

    // [Jason] 空群組過濾：MENU 群組過濾後若無任何可見子項則視為空殼、不顯示；PAGE/BUTTON 等葉節點一律保留
    private boolean isNotEmptyGroup(SystemResourceDto dto) {
        if ("MENU".equals(dto.getResourceType())) {
            return dto.getChildren() != null && !dto.getChildren().isEmpty();
        }
        return true;
    }

    private SystemResourceDto convertToDto(SystemResource resource, Set<String> userPermissions) {
        SystemResourceDto dto = SystemResourceDto.builder()
                .resourceId(resource.getResourceId())
                .portalType(resource.getPortalType().name())
                .resourceType(resource.getResourceType().name())
                .name(resource.getName())
                .urlPath(resource.getUrlPath())
                .permissionId(resource.getPermission() != null ? resource.getPermission().getPermissionId() : null)
                .sortOrder(resource.getSortOrder())
                .icon(resource.getIcon())
                .visible(resource.getIsVisible() == null || resource.getIsVisible())
                .build();

        // 遞迴過濾並處理子節點（含可見性過濾與空群組剔除）
        if (resource.getChildren() != null && !resource.getChildren().isEmpty()) {
            List<SystemResourceDto> filteredChildren = resource.getChildren().stream()
                    .filter(child -> hasPermission(child, userPermissions) && isVisible(child))
                    .map(child -> convertToDto(child, userPermissions))
                    .filter(this::isNotEmptyGroup)
                    .sorted((c1, c2) -> Integer.compare(c1.getSortOrder(), c2.getSortOrder()))
                    .collect(Collectors.toList());
            dto.setChildren(filteredChildren);
        } else {
            dto.setChildren(new ArrayList<>());
        }

        return dto;
    }
}
