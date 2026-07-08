package tw.com.ispan.backend.organizer.dto;

import java.time.LocalDateTime;

/**
 * Admin 後台「組織列表」DTO，對齊 frontend-admin {@code OrganizerList.vue}。
 *
 * <p>欄位名稱使用 {@code orgId}（非 entity 的 organizerId）以對齊前端，
 * 並帶 {@code status} 與 {@code memberCount} 供列表篩選與顯示。</p>
 */
public record AdminOrganizerListItem(
        String orgId,
        String name,
        String taxId,
        Integer status,       // OrganizerStatus 序數：0=ACTIVE,1=SUSPENDED,2=ARCHIVED
        Integer kycStatus,    // KycStatus 序數：0=DRAFT,1=PENDING,2=APPROVED,3=REJECTED
        long memberCount,
        LocalDateTime createdAt
) {
}
