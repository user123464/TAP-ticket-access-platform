package tw.com.ispan.backend.organizer.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Admin 後台「組織詳情」DTO，對齊 frontend-admin {@code OrganizerDetail.vue}。
 *
 * <p>包含組織基本資料、成員、合約、訂閱摘要。聯絡電話 / 地址取自
 * kyc_data_json，敏感的銀行帳戶資訊不外洩。</p>
 */
public record AdminOrganizerDetail(
        String orgId,
        String name,
        String taxId,
        Integer status,
        Integer kycStatus,
        String phone,
        String address,
        LocalDateTime createdAt,
        // ── KYC 審核所需欄位（自 kyc_data_json / bank_account_info / kyc_* 欄位萃取）──
        String ownerName,
        String ownerIdNumber,
        // 認證文件：名稱 + 受保護的下載 API path（前端以帶 JWT 的 axios 取 blob 開啟）
        String registrationDocName,
        String registrationDocUrl,
        String identityCardName,
        String identityCardUrl,
        // 收款銀行帳戶
        String bankCode,
        String bankName,
        String accountNo,
        String accountName,
        // 審核歷史
        String reviewedBy,
        LocalDateTime reviewedAt,
        String rejectReason,
        List<Member> members,
        List<ContractRow> contracts,
        Subscription subscription,
        // 進行中(ACTIVE)活動數：供前端 KYC 撤銷防禦性鎖定（>0 時禁止撤銷認證）
        long activeEventCount
) {
    public record Member(
            String userId,
            String name,
            String email,
            String roleName,
            LocalDateTime joinedAt
    ) {
    }

    public record ContractRow(
            String id,
            Integer contractType,
            Integer contractStatus,
            Integer feeType,
            BigDecimal feeValue,
            LocalDateTime signedAt,
            LocalDateTime expiresAt
    ) {
    }

    public record Subscription(
            String planName,
            String status,        // ACTIVE / EXPIRED
            LocalDateTime startedAt,
            LocalDateTime expiresAt
    ) {
    }
}
