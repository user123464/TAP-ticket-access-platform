package tw.com.ispan.backend.orderTicket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 會員「我的訂單 - 票務」清單用的輕量 DTO。
 *
 * <p>
 * 供 {@code GET /api/member/ticket-orders} 使用，僅回傳列表頁需要顯示的欄位，
 * 避免直接序列化 JPA 實體造成的雙向關聯無限遞迴與 LazyLoading 例外。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberTicketOrderDTO {

    // 訂單編號
    @JsonProperty("tOrderId")
    private String tOrderId;

    // 活動主題名稱 (例如: "五月天演唱會")
    private String activityTitle;

    // 場次名稱
    private String sessionTitle;

    // 訂單總金額
    private BigDecimal totalAmount;

    // 付款狀態 (UNPAID / PAID / FAILED / EXPIRED)
    private String paymentStatus;

    // 訂單建立時間
    private LocalDateTime createAt;

    // 該訂單包含的票券張數
    private Integer ticketCount;
}
