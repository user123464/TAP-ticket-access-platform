package tw.com.ispan.backend.refund.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 退款申請單 DTO (B2B 審核頁清單用)。
 *
 * <p>除申請單本身欄位外，一併扁平化帶出購買人聯絡資訊、品項摘要、金額與購買時間，
 * 讓主辦方在審核頁一眼看到申請全貌。</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundRequestDTO {
    private Integer requestId;
    private String requestType;   // TICKET / MERCH
    private String status;        // PENDING / APPROVED / REJECTED
    private String orderId;
    private String detailId;
    private String reason;        // 申退理由
    private String rejectNote;    // 駁回原因
    private LocalDateTime createdAt;    // 申請時間
    private LocalDateTime processedAt;  // 審核時間

    // 購買人聯絡資訊 (票務取訂單聯絡人；商城取收件人)
    private String applicantName;
    private String applicantEmail;
    private String applicantPhone;

    // 品項摘要 (票務：活動/場次/入場人；商城：商品名 x 數量)
    private String itemSummary;

    // 退款金額 (票務：單價；商城：單價 x 數量)
    private BigDecimal amount;

    // 購買 (下單) 時間
    private LocalDateTime purchasedAt;
}
