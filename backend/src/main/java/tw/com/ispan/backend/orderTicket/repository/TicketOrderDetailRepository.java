package tw.com.ispan.backend.orderTicket.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.transaction.Transactional;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.ticket.entity.Ticket;

/**
 * 門票訂單明細數據庫訪問介面。
 * 
 * <p>提供對門票明細進行 CRUD 操作的方法，包含 JPA 命名查詢、自定義 JPQL 批量更新與跨表多維財務統計報表查詢。</p>
 */
public interface TicketOrderDetailRepository extends JpaRepository<TicketOrderDetailBean, String> {

    /**
     * 根據票務訂單主檔編號，查詢所有相關的訂單明細。
     *
     * @param tOrderId 訂單主檔編號
     * @return 訂單明細列表
     */
    List<TicketOrderDetailBean> findByTicketOrder_tOrderId(String tOrderId);

    /**
     * 根據訂單編號與門票狀態 (例如: NORMAL 正常)，篩選出有效的明細。
     *
     * @param tOrderId   訂單主檔編號
     * @param itemStatus 門票狀態 (例如 NORMAL 正常, REFUNDED 已退票)
     * @return 訂單明細列表
     */
    List<TicketOrderDetailBean> findByTicketOrder_tOrderIdAndItemStatus(String tOrderId, TicketOrderStatus itemStatus);

    /**
     * 根據入場人身份證字號查詢購票明細，常用於實名制重複購票校驗或後台檢索。
     *
     * @param identityNumber 證件字號
     * @return 訂單明細列表
     */
    List<TicketOrderDetailBean> findByIdentityNumber(String identityNumber);

    /**
     * 根據驗票用 QR Code 雜湊值查詢訂單明細，用於現場掃描驗票。
     *
     * @param qrCodeHash QR Code 雜湊值
     * @return 訂單明細，找不到則為 null
     */
    TicketOrderDetailBean findByQrCodeHash(String qrCodeHash);

    /**
     * 查找所有狀態為 NORMAL 的有效訂單明細，用於營收統計。
     *
     * @param NORMAL 正常狀態列舉
     * @return 正常門票明細列表
     */
    List<TicketOrderDetailBean> findByItemStatus(TicketOrderStatus NORMAL);

    /**
     * 根據票種 ID 修改明細對應的票券參考 (批量更新)。
     * 
     * <p>修復：原參數 ticketTypeId 類型為 String，但實體主鍵 TicketType.ticketTypeId 實際為 Integer，已修復為 Integer 類型。</p>
     *
     * @param newTicket    新的票券實體
     * @param ticketTypeId 票種 ID (Integer)
     */
    @Transactional
    @Modifying
    @Query("""
            UPDATE TicketOrderDetailBean d
            SET d.ticketTicket = :newTicket
            WHERE d.ticketTicket.ticketType.ticketTypeId = :ticketTypeId
            """)
    void updateStatusByTicketTypeName(@Param("newTicket") Ticket newTicket,
            @Param("ticketTypeId") Integer ticketTypeId);

    /**
     * Admin 財務結算統計：依主辦方組織彙總某時間區間內的「成交筆數（明細數）」與「交易總金額（GMV）」。
     *
     * <p>跨 5 張表進行聯接查詢：明細(d) → 票券(t) → 場次(s) → 主題(th) → 組織(o)，
     * 並限定父訂單成立時間 createAt 在 from 到 to 之間。</p>
     *
     * @param from 區間起點時間
     * @param to   區間終點時間
     * @return 彙總數據列表，每筆為 [organizerId(String), orgName(String), count(Long), gmv(BigDecimal)]
     */
    @Query("""
            SELECT o.organizerId, o.name, COUNT(d), COALESCE(SUM(d.unitPrice), 0)
            FROM TicketOrderDetailBean d
            JOIN d.ticketTicket t
            JOIN t.session s
            JOIN s.theme th
            JOIN th.organizer o
            WHERE d.ticketOrder.createAt >= :from AND d.ticketOrder.createAt < :to
            GROUP BY o.organizerId, o.name
            ORDER BY COALESCE(SUM(d.unitPrice), 0) DESC
            """)
    List<Object[]> aggregateGmvByOrganizer(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    /**
     * Admin 財務趨勢圖表：取出某時間區間內每筆交易明細的 [訂單成立時間, 成交單價]。
     * 
     * <p>在 Service 層以 Java 邏輯動態彙總月/日趨勢，以維持跨資料庫（如 SQL Server / MySQL）的可攜性，避免使用特定 SQL 函數。</p>
     *
     * @param from 區間起點時間
     * @param to   區間終點時間
     * @return 交易紀錄列表，每筆包含 [createAt(LocalDateTime), unitPrice(BigDecimal)]
     */
    @Query("""
            SELECT d.ticketOrder.createAt, d.unitPrice
            FROM TicketOrderDetailBean d
            WHERE d.ticketOrder.createAt >= :from AND d.ticketOrder.createAt < :to
            """)
    List<Object[]> findOrderDateAndAmount(@Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);
}
