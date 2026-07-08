package tw.com.ispan.backend.orderMerch.enums;

public enum MerchOrderEnum {
    
    NORMAL(1,"正常"),
    RETURNED(2,"已退貨"),
    CANCELLED(3,"已取消"),
    UNPAID(4,"待付");

    private final int code;
    private final String descr;

    MerchOrderEnum(int code, String descr) {
        this.code = code;
        this.descr = descr;
    }

    public int getCode() {
        return code;
    }

    public String getDescr() {
        return descr;
    }
}