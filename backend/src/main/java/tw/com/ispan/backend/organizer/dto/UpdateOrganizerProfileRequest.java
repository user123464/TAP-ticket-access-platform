package tw.com.ispan.backend.organizer.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 儲存組織基本資料（草稿存檔，不提交審核、不變更 KYC 狀態）。
 * 僅組織名稱為必填，其餘欄位允許留空逐步補齊；Logo 由 /logo 端點另行持久化，本端點不更動。
 */
@Data
public class UpdateOrganizerProfileRequest {

    @NotBlank(message = "組織名稱不能為空")
    @Size(max = 100, message = "組織名稱長度不能超過 100 字")
    private String name;

    @Size(max = 20, message = "統一編號長度不能超過 20 字")
    private String taxId;

    // 撥款銀行帳戶資訊
    private String bankCode;
    private String bankName;
    private String accountNo;
    private String accountName;

    // 聯絡與負責人資訊（寫入 kyc_data_json）
    private String phone;
    private String fax;
    private String address;
    private String ownerName;
    private String ownerIdNumber;
}
