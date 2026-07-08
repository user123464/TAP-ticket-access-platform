package tw.com.ispan.backend.login.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 後台「使用者詳情」DTO，對齊 frontend-admin {@code UserDetail.vue}。
 *
 * <p>包含個資、所屬組織與角色、登入嘗試紀錄。不回傳密碼雜湊與各種 token。</p>
 */
public record AdminUserDetail(
        String id,
        String email,
        String name,
        String phone,
        String authProvider,
        boolean isVerified,
        boolean isActive,
        LocalDateTime lockedUntil,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        List<PlatformRole> platformRoles,
        List<OrgRole> organizations,
        List<LoginAttemptRow> loginAttempts
) {
    /** 使用者持有的平台角色（organizer 為 null）。editable=true 者為可加掛/取下的內部人員角色。 */
    public record PlatformRole(
            String id,
            String name,
            boolean editable
    ) {
    }

    /** 使用者所屬組織與其角色 */
    public record OrgRole(
            String orgId,
            String orgName,
            String roleName
    ) {
    }

    /** 單筆登入嘗試紀錄 */
    public record LoginAttemptRow(
            Long id,
            LocalDateTime time,
            String ip,
            boolean success,
            String failReason
    ) {
    }
}
