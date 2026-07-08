package tw.com.ispan.backend.organizer.enums;

/**
 * 組織所有權轉移狀態。以 ORDINAL 儲存（對齊 schema TINYINT 0~3）。
 *
 * <ul>
 *   <li>PENDING(0)：已發起，等待被轉移人 email 認證接受。</li>
 *   <li>ACCEPTED(1)：被轉移人已接受，所有權已易主。</li>
 *   <li>CANCELLED(2)：原 owner 主動取消。</li>
 *   <li>EXPIRED(3)：Token 逾期未接受。</li>
 * </ul>
 */
public enum OrganizerTransferStatus {
    PENDING,
    ACCEPTED,
    CANCELLED,
    EXPIRED
}
