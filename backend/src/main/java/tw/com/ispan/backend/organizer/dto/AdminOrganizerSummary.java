package tw.com.ispan.backend.organizer.dto;

import java.time.LocalDateTime;

/**
 * Admin 後台組織清單的精簡投影 DTO。
 *
 * <p>用途：RBAC 待辦 #2 KYC 審核流程的「待審清單」查詢。供
 * {@code GET /api/admin/organizers} 回傳，僅暴露審核管理員需要的欄位，
 * 不回傳整個 {@code Organizer} entity，避免 LAZY 關聯觸發與
 * bankAccountInfo / kycDataJson 等敏感欄位外洩。</p>
 *
 * @author [Jason]
 */
public record AdminOrganizerSummary(
        String organizerId,        // ORG000000X
        String name,               // 組織名稱
        String taxId,              // 統一編號 (可能為 null)
        Integer kycStatus,         // KycStatus 序數：0=DRAFT,1=PENDING,2=APPROVED,3=REJECTED
        String kycStatusName,      // KycStatus 名稱，方便前端直接顯示
        String kycRejectionReason, // 退件原因 (僅 REJECTED 時有值)
        LocalDateTime kycReviewedAt, // 審核通過/退件時間 (尚未審核為 null)
        LocalDateTime createdAt    // 組織建立時間，可作為「提交先後」排序依據
) {
}
