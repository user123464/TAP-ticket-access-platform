package tw.com.ispan.backend.theme.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.service.OrganizerService;

/**
 * [Jason] 跨組織 row-level 隔離查核 helper（RBAC 缺口 #1）。
 *
 * 原因：Theme/Session 的後台端點 URL 不帶 organizerId，原本只有
 * {@code @PreAuthorize('EVENT_*')} —— 那只能擋「非廠商」，擋不住 A 廠商
 * 拿自己的權限去改/讀 B 廠商的活動或場次（水平越權）。
 *
 * 本 helper 從 SecurityContextHolder 取得「登入者本人」（身分來自 JWT，不可偽造），
 * 再委派給 {@link OrganizerService#requireOrgPermission} 反查
 * 「這位本人是不是『資料所屬組織』的 ACCEPTED 成員、且具該權限」。
 * 因此前端送來的 organizerId 只是宣稱，真正的信任邊界在這裡的反查。
 *
 * 設計取捨：採「service 內讀 SecurityContext」而非改組員 controller/service 的方法簽章，
 * 以最小幅度介入組員（Theme 系列）檔案。
 */
@Component
@RequiredArgsConstructor
public class OrgRowSecurityGuard {

    private final OrganizerService organizerService;

    /**
     * 確認「目前登入者」在指定組織內擁有指定權限；不足則拋 SecurityException。
     *
     * @param organizerId 資料所屬組織（由 entity 回推，或由可信反查後的請求帶入）
     * @param permissionId data.sql 權限碼，如 EVENT_VIEW / EVENT_EDIT / EVENT_PUBLISH / EVENT_CREATE
     */
    public void requireOrgPermission(String organizerId, String permissionId) {
        organizerService.requireOrgPermission(currentUserId(), organizerId, permissionId);
    }

    /** 取得目前登入者 userId；未通過認證則拋 SecurityException。 */
    public String currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof CustomUserDetails userDetails)) {
            throw new SecurityException("未通過身分驗證，無法執行此操作");
        }
        return userDetails.getUserId();
    }
}
