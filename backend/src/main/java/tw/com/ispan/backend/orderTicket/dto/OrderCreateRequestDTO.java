package tw.com.ispan.backend.orderTicket.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Data;

/**
 * 建立購票訂單時，前端向後端傳遞的 Request 封裝對象。
 * 
 * <p>此 DTO 設計上只包含購票標識和實名資訊，不包含價格，
 * 價格的校驗和獲取在後端通過數據庫查詢，確保交易安全性，防止篡改價格。</p>
 */
@Data
public class OrderCreateRequestDTO {

    // 用於防範重複提交的唯一 Token，由 Redis 提供超時鎖定
    private String submitToken;
    
    // 聯絡人姓名
    private String contactName;
    
    // 聯絡人信箱
    private String contactEmail;
    
    // 聯絡人手機號碼
    private String contactPhone;
    
    // 欲購買的門票與實名制詳情列表
    private List<DetailRequest> orderTickets;

    /**
     * 單張門票購票請求。
     */
    @Data
    public static class DetailRequest {
        // [FK] 對應實體 Ticket 票券 ID
        private Long ticketId;
        
        // 該張門票的使用人真實姓名 (實名制)
        private String realName;
        
        // 該張門票的使用人身份證字號/護照號碼 (實名制)
        private String identityNumber;

        private Integer sessionId;
        private Integer ticketIds;
        private BigDecimal unitPrice;
    }
}
