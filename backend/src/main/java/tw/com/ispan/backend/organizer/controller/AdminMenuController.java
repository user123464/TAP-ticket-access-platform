package tw.com.ispan.backend.organizer.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.organizer.dto.SystemResourceDto;
import tw.com.ispan.backend.organizer.service.SystemResourceService;

/**
 * Admin 後台動態選單端點。
 *
 * <p>地基批次：前端 frontend-admin 的 menu store 呼叫 {@code GET /api/admin/menus}
 * 取得側邊欄選單樹。此 controller 是既有 {@link SystemResourceController#getMenu}
 * 的 Admin 專用 alias，固定以 {@link PortalType#ADMIN_LOCAL} 查詢，
 * 並沿用登入使用者的權限集合（{@link CustomUserDetails#getPermissions()}）做過濾，
 * 不需要前端帶 portalType / organizerId 參數。</p>
 *
 * <p>回傳結構與 {@link SystemResourceDto} 一致（resourceId / name / urlPath /
 * permissionId / children…），對齊前端 menu.js。落在 SecurityConfig 的
 * {@code anyRequest().authenticated()} 範圍，僅需登入即可，選單內容再由角色權限過濾。</p>
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminMenuController {

    private final SystemResourceService systemResourceService;

    @GetMapping("/menus")
    public ResponseEntity<List<SystemResourceDto>> getAdminMenus(
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        List<SystemResourceDto> menuTree = systemResourceService.getMenuTree(
                PortalType.ADMIN_LOCAL, userDetails.getPermissions());
        return ResponseEntity.ok(menuTree);
    }
}
