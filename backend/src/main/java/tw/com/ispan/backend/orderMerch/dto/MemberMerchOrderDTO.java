package tw.com.ispan.backend.orderMerch.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 會員「我的訂單 - 商城」清單用的輕量 DTO。
 *
 * <p>
 * 供 {@code GET /api/member/merch-orders} 使用，僅回傳列表頁需要顯示的欄位。
 * </p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemberMerchOrderDTO {

    // 訂單編號
    @JsonProperty("mOrderId")
    private String mOrderId;

    // 品項摘要 (例如: "黑色 T 恤 x2、束口袋 x1")
    private String itemSummary;

    // 該訂單購買總件數
    private Integer totalQuantity;

    // 訂單總金額
    private BigDecimal totalAmount;

    // 付款狀態 (UNPAID / PAID / FAILED)
    private String paymentStatus;

    // 訂單建立時間
    private LocalDateTime createdAt;
}
