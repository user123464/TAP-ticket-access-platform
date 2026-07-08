package tw.com.ispan.backend.orderTicket.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import lombok.Data;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * 訂單建立成功或查詢時，後端響應前端的 Response DTO。
 * 
 * <p>採用巢狀結構包裝訂單主檔與明細，且對多表關聯進行了扁平化處理，
 * 避免直接序列化 JPA 實體時，由於雙向關聯引起的 JSON 無限循環嵌套 (Infinite Recursion) 
 * 以及 Session 關閉後的 LazyLoading 延遲加載異常。</p>
 */
@Data
public class OrderResponseDTO {

    // 訂單主檔編號
    @JsonProperty("tOrderId")
    private String tOrderId;
    
    // 消費者支付總額 (含 100 元系統服務費)
    private BigDecimal totalAmount;
    
    // 聯絡人姓名
    private String contactName;
    
    // 訂單成立時間
    private LocalDateTime createAt;
    
    // 訂單付款狀態
    private String paymentStatus;
    
    // 訂單明細列表
    private List<OrderDetailResponseDTO> orderDetailTickets;
    
    // 綠界支付金流 HTML Form 表單代碼 (若需要立即發起付款)
    private String ecpayForm;

    /**
     * 訂單明細響應 DTO，包含門票及實名制資訊。
     */
    @Data
    public static class OrderDetailResponseDTO {
        // 明細流水號 ID
        @JsonProperty("tDetailId")
        private String tDetailId;
        
        // 成交單價
        private BigDecimal unitPrice;
        
        // 入場人真實姓名
        private String realName;
        
        // 明細訂單狀態 (NORMAL 正常, REFUNDED 已退票)
        private TicketOrderStatus itemStatus;
        
        // 驗票專用隨機產生的 12 碼大寫 QR Code Hash
        private String qrCodeHash;
        
        // 核銷使用狀態 (Unredeemed 未核銷, Redeemed 已核銷, Canceled 已取消)
        private TicketOrderUse isUsed = TicketOrderUse.Unredeemed;
        
        // 實際現場核銷時間
        private LocalDateTime usedAt;

        // ── 扁平化關聯欄位 ──
        // 為了避免前端在載入時查詢複雜的多關聯，在 Transaction 內由 Service 直接扁平化倒過來
        
        // 物理座位資訊 (例如: "3排15號")
        private String seatInfo;
        
        // 票種 ID (如搖滾區、看台區)
        private Integer ticketTypeId;

        // 票種名稱 (例如: "搖滾A區")
        private String ticketTypeName;
        
        // 活動場次 ID
        private Integer sessionId;

        // 活動主題標題 (例如: "五月天演唱會")
        private String activityTitle;
        
        // 活動主辦方/組織 ID (格式如 ORG0000001)
        private String organizerId;
    }
}
