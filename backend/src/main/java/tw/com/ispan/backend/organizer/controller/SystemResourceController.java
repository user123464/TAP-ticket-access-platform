package tw.com.ispan.backend.organizer.controller;

import java.util.List;
import java.util.Set;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.organizer.dto.SystemResourceDto;
import tw.com.ispan.backend.organizer.service.OrganizerService;
import tw.com.ispan.backend.organizer.service.SystemResourceService;

@RestController
@RequestMapping("/api/resources")
@RequiredArgsConstructor
public class SystemResourceController {

    private final SystemResourceService systemResourceService;
    // [Jason] RBAC #4b：B2B 選單需依「使用者在該組織的角色權限」過濾
    private final OrganizerService organizerService;

    @GetMapping("/menu")
    public ResponseEntity<List<SystemResourceDto>> getMenu(
            @RequestParam("portalType") PortalType portalType,
            @RequestParam(value = "organizerId", required = false) String organizerId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {

        // [Jason] RBAC #4b：B2B_PORTAL 改用「該組織內角色」的權限集合過濾選單（per-org），
        // 而非登入時載入的平台天花板（ORGANIZER 全集），才能反映 Admin/會計等不同組織角色實際可見的頁面。
        // 其餘 portal（B2C / ADMIN_LOCAL）維持以登入權限過濾。
        Set<String> permissions = (portalType == PortalType.B2B_PORTAL
                && organizerId != null && !organizerId.isBlank())
                ? organizerService.getOrgPermissionIds(userDetails.getUserId(), organizerId)
                : userDetails.getPermissions();
        List<SystemResourceDto> menuTree = systemResourceService.getMenuTree(portalType, permissions);
        return ResponseEntity.ok(menuTree);
    }
}
