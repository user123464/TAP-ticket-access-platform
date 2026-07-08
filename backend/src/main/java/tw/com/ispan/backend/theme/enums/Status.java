package tw.com.ispan.backend.theme.enums;

public enum Status {
    DRAFT("草稿"),
    ACTIVE("公開"),
    ARCHIVED("已結束"),
    DELETED("已刪除");

    private final String desc;

    Status(String desc) {
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

}