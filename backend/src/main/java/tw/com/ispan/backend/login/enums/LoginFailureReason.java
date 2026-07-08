package tw.com.ispan.backend.login.enums;

public enum LoginFailureReason {
    WRONG_PASSWORD, // 序數為0
    USER_NOT_FOUND, // 序數為1
    ACCOUNT_LOCKED, // 序數為2
    ACCOUNT_DISABLED // 序數為3
}
