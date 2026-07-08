package tw.com.ispan.backend.ticket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.ticket.entity.TicketStatus;

public interface TicketRepository extends JpaRepository<Ticket, Long> {

        // 安全防線：檢查該場次是否已經有票券進入「鎖定中(LOCKED)」或「已售出(SOLD)」的狀態
        boolean existsBySession_SessionIdAndStatusIn(Integer sessionId, List<TicketStatus> status);

        // 刪除該場次目前所有處於「可售(AVAILABLE)」狀態的舊票券
        // 用途：Wipe and Rewrite。如果廠商改了配置重新按存檔，要先清空舊的空票，再印新票，避免重複產生
        @Modifying
        long deleteBySession_SessionIdAndStatus(
                        Integer sessionId,
                        TicketStatus status);

        // 【後台管理用/前台畫圖用】找出場次的所有票券（含已售出、鎖定中）
        // 優化：直接使用 _SessionId 傳入 Integer，省去在 Service 層先撈 Session 物件的麻煩
        // 修正：補上 _SessionId，對應 Integer 參數
        List<Ticket> findBySession_SessionId(Integer sessionId);

        // 【前台購票用】找出場次中特定狀態的票券（例如 status = 1 可售）
        // 修正：將 Integer status 改為 TicketStatus status
        // 修正：補上 _SessionId，並確保使用 TicketStatus 型態
        List<Ticket> findBySession_SessionIdAndStatus(Integer sessionId, TicketStatus status);

        // 【前台購票用】根據「場次」與「票種 ID」尋找特定狀態的票券
        // 修正：參數對齊方法名稱 (Integer sessionId, Integer ticketTypeId, TicketStatus status)
        @Modifying
        @Query("DELETE FROM Ticket t WHERE t.session.sessionId = :sessionId AND t.status = :status")
        int deleteBySessionIdAndStatus(
                        @Param("sessionId") Integer sessionId,
                        @Param("status") TicketStatus status);

        // 【後台票務訂單明細串接用】根據 「票種 ID」尋找特定狀態的票券
        Optional<Ticket> findFirstByTicketType_TicketTypeIdAndStatus(Integer ticketTypeId, TicketStatus status);

        // 【前台購票用核心】利用票券 PK 與場次 ID 精準撈取使用者想買的票
        // 這個查詢會自動利用聯合索引，效能極高，同時也是防護「越權竄改」的第一道牆
        List<Ticket> findBySession_SessionIdAndTicketIdIn(Integer sessionId, List<Long> ticketIds);

        /**
         * [排程專用] 批量釋放過期的幽靈座位
         * 
         * @return 受到影響（被更新）的資料筆數
         */
        @Modifying
        @Query("UPDATE Ticket t SET t.status = :availableStatus, t.lockBy = null, t.lockUntil = null " +
                        "WHERE t.status = :lockedStatus AND t.lockUntil <= :now")
        int releaseExpiredLockedTickets(
                        @Param("availableStatus") TicketStatus availableStatus,
                        @Param("lockedStatus") TicketStatus lockedStatus,
                        @Param("now") LocalDateTime now);

        // 在 TicketRepository 中加入：
        // 核心優化：只 Select ticketId 單一欄位，避開整張表的 mapping，極大化節省記憶體與頻寬
        @Query("SELECT t.ticketId FROM Ticket t WHERE t.session.sessionId = :sessionId AND t.status IN :statuses")
        List<Long> findTicketIdsBySessionIdAndStatuses(
                        @Param("sessionId") Integer sessionId,
                        @Param("statuses") List<TicketStatus> statuses);
}
