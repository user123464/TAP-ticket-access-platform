package tw.com.ispan.backend.login.exception;

/**
 * 帳號刪除被阻擋（例如使用者仍綁定主辦方組織，需先退出組織）。
 * 控制器會將此例外對應為 409 Conflict，並回傳 blocked 旗標供前端導向處理。
 */
public class AccountDeletionBlockedException extends RuntimeException {
    public AccountDeletionBlockedException(String message) {
        super(message);
    }
}
