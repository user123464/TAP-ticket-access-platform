package tw.com.ispan.backend.login.dto;

import java.time.LocalDateTime;

/**
 * Admin 後台「使用者列表」精簡投影 DTO。
 *
 * <p>對齊 frontend-admin {@code UserList.vue} 期望欄位。{@code id} 即 User 的字串主鍵
 * （USRXXXXXXX），前端僅作為列表 key 與詳情路由參數使用，不做數值運算。
 * 不回傳 passwordHash / token 等敏感欄位。</p>
 */
public record AdminUserSummary(
        String id,                 // USRXXXXXXX
        String email,
        String name,
        String authProvider,       // LOCAL / GOOGLE（enum 名稱）
        boolean isVerified,        // emailVerifiedAt 是否有值
        boolean isActive,
        LocalDateTime lockedUntil, // 鎖定到期時間（null 表示未鎖定）
        boolean isDeleted,
        LocalDateTime createdAt,
        LocalDateTime lastLoginAt  // 最近一次成功登入時間（無則 null）
) {
}
