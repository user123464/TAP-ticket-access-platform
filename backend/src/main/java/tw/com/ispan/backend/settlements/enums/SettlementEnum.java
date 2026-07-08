package tw.com.ispan.backend.settlements.enums;

public enum SettlementEnum {
    PENDING(0, "待處理"),
    PAID(1, "已撥款");

    private final int value;
    private final String description;

    private SettlementEnum(int value, String desc) {
        this.value = value;
        this.description = desc;
    }

    public int getValue() {
        return value;
    }

    public String getDescription() {
        return description;
    }
}
