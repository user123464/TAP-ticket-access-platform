package tw.com.ispan.backend.login.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 後台「內部人員」清單投影 DTO，對齊 frontend-admin {@code StaffList.vue}。
 *
 * <p>僅含持有內部人員角色（organizer 為 null 且 is_editable=1）的帳號，
 * 額外帶出其內部角色名稱清單供列表呈現。</p>
 */
public record AdminStaffSummary(
        String id,
        String email,
        String name,
        boolean isActive,
        LocalDateTime lockedUntil,
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt,
        List<String> roleNames
) {
}
