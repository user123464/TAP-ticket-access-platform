package tw.com.ispan.backend.orderTicket.enums;

/**
 * 門票現場核銷使用狀態列舉。
 * 
 * <p>表示該張門票在現場檢票時的狀態（未核銷、已核銷、已取消）。</p>
 */
public enum TicketOrderUse {

    /** 門票未使用，等待核銷進場。 */
    Unredeemed(1, "未核銷"),
    
    /** 門票已在現場掃描成功，完成核銷進場。 */
    Redeemed(2, "已核銷"),
    
    /** 門票已因退票而註銷，無法再被掃描使用。 */
    Canceled(3, "已取消"),

    ///** 訂單未成立，可能因付款失敗或其他原因而被取消。 */
    UNESTABLISHED(3, "未成立");

    // 狀態碼
    private final int code;
    
    // 狀態中文說明描述
    private final String desc;

    // 建構子
    private TicketOrderUse(int code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    /**
     * 獲取狀態碼。
     */
    public int getCode() {
        return code;
    }

    /**
     * 獲取狀態說明。
     */
    public String getDesc() {
        return desc;
    }
}
