package tw.com.ispan.backend.orderTicket.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;

/**
 * 門票訂單主檔數據庫訪問介面。
 * 
 * <p>
 * 提供對訂單主檔進行 CRUD 操作的方法，包含用於定時排程清理過期訂單的特定條件查詢。
 * </p>
 */
public interface TicketOrderRepository extends JpaRepository<TicketOrdersBean, String> {

    @Query("""
            SELECT DISTINCT o
            FROM TicketOrdersBean o
            JOIN o.orderDetail d
            JOIN d.ticketTicket t
            JOIN t.session s
            JOIN s.theme th
            JOIN th.organizer org
            WHERE org.organizerId = :organizerId
              AND o.paymentStatus = 'PAID'
            ORDER BY o.createAt DESC
            """)
    List<TicketOrdersBean> findPaidOrdersByOrganizer(@Param("organizerId") String organizerId);

    /**
     * 找出符合特定付款狀態，且訂單建立時間在指定臨界點之前的訂單。
     *
     * <p>
     * 此方法主要用於排程任務，掃描出「待付款 (UNPAID)」且超過 15 分鐘未完成交易的訂單。
     * </p>
     *
     * @param paymentStatus 付款狀態 (例如 UNPAID)
     * @param dateTime      臨界點時間
     * @return 符合過期條件的訂單列表
     */
    List<TicketOrdersBean> findByPaymentStatusAndCreateAtBefore(String paymentStatus, LocalDateTime dateTime);

    List<TicketOrdersBean> findByPaymentStatus(String paymentStatus);

    Optional<TicketOrdersBean> findByMerchantTradeNo(String merchantTradeNo);

    // 給前台用：找出某個會員買過的所有訂單 (預留)
    // List<TicketOrdersBean> findByUserId(String UserId);

    // Jason added: 會員「我的訂單」— 依登入使用者查詢票務訂單，依建立時間新到舊排序
    @Query("""
            SELECT o
            FROM TicketOrdersBean o
            WHERE o.userId.userId = :userId
            ORDER BY o.createAt DESC
            """)
    List<TicketOrdersBean> findByMemberUserId(@Param("userId") String userId);

}
