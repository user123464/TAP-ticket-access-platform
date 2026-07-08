package tw.com.ispan.backend.organizer.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SubmitKycRequest {

    // 組織統編
    @NotBlank(message = "統一編號不能為空")
    private String taxId;

    // 銀行資訊
    @NotBlank(message = "銀行代碼不能為空")
    private String bankCode;

    @NotBlank(message = "銀行名稱與分行不能為空")
    private String bankName;

    @NotBlank(message = "撥款帳號不能為空")
    private String accountNo;

    @NotBlank(message = "戶名不能為空")
    private String accountName;

    // 聯絡與身份資訊
    private String phone;
    private String fax;
    private String address;

    @NotBlank(message = "負責人姓名不能為空")
    private String ownerName;

    @NotBlank(message = "負責人身分證字號不能為空")
    private String ownerIdNumber;

    // 檔案與 Logo 路徑 (上傳至後端後拿到的相對路徑)
    private String logoUrl;
    private String registrationDocName;
    private String registrationDocUrl;
    private String identityCardName;
    private String identityCardUrl;
}
