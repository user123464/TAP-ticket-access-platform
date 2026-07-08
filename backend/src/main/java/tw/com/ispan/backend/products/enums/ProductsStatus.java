package tw.com.ispan.backend.products.enums;

public enum ProductsStatus {

    Draft((byte) 0, "草稿"),
    Released((byte) 1, "上架"),
    Discontinued((byte) 2, "下架"),
    OutOfStock((byte) 3, "缺貨");

    private final byte code;
    private final String desc;

    ProductsStatus(byte code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public byte getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    public static ProductsStatus fromCode(byte code) {
        for (ProductsStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知狀態代碼: " + code);
    }
}