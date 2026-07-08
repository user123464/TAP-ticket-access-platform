package tw.com.ispan.backend.orderTicket.enums;

/**
 * 門票訂單明細狀態列舉。
 * 
 * <p>表示單張門票在業務生命週期中的狀態，如正常或已退票。</p>
 */
public enum TicketOrderStatus {

    /** 正常有效狀態。 */
    NORMAL(1, "正常"),
    
    /** 已辦理退票狀態。 */
    REFUNDED(2, "已退票"),

    /** 已辦理取消狀態。 */
    CANCELLED(3, "已取消"),

    /** 待付款狀態。 */
    UNPAID(4, "待付");

    // 狀態碼
    private final int code;
    
    // 狀態說明描述
    private final String descr;

    // 建構子
    TicketOrderStatus(int code, String desc) {
        this.code = code;
        this.descr = desc;
    }

    /**
     * 獲取狀態碼。
     */
    public int getCode() {
        return code;
    }

    /**
     * 獲取狀態描述。
     */
    public String getDescription() {
        return descr;
    }
}