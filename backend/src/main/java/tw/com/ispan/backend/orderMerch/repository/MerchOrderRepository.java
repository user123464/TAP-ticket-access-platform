package tw.com.ispan.backend.orderMerch.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;

public interface MerchOrderRepository extends JpaRepository<MerchOrderBean, String> {

        @Query("""
                        SELECT DISTINCT o
                        FROM MerchOrderBean o
                        JOIN o.merchOrderDetails d
                        JOIN d.productProduct p
                        JOIN p.theme th
                        JOIN th.organizer org
                        WHERE org.organizerId = :organizerId
                          AND o.paymentStatus = 'PAID'
                        ORDER BY o.createdAt DESC
                        """)
        List<MerchOrderBean> findPaidOrdersByOrganizer(@Param("organizerId") String organizerId);

        Optional<MerchOrderBean> findByMerchantTradeNo(String merchantTradeNo);

        List<MerchOrderBean> findByPaymentStatus(String paymentStatus);

        // Jason added: Admin 財務 — 依組織彙總 merch GMV
        // 路徑：MerchOrderDetail d → Product p → Theme th → Organizer o
        // 以 merch_orders.created_at 篩選區間；item_status=1(NORMAL) 排除退貨明細
        // 回傳每列：[organizerId(String), orgName(String), count(Long), gmv(BigDecimal)]
        @Query("""
                        SELECT o.organizerId, o.name,
                               COUNT(d),
                               COALESCE(SUM(d.unitPrice * d.quantity), 0)
                        FROM MerchOrderDetailBean d
                        JOIN d.productProduct p
                        JOIN p.theme th
                        JOIN th.organizer o
                        WHERE d.merchOrder.createdAt >= :from
                          AND d.merchOrder.createdAt < :to
                          AND d.itemStatus = tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum.NORMAL
                        GROUP BY o.organizerId, o.name
                        ORDER BY COALESCE(SUM(d.unitPrice * d.quantity), 0) DESC
                        """)
        List<Object[]> aggregateMerchGmvByOrganizer(@Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Jason added: Admin 財務儀表板月趨勢 — 取出各 merch 明細的 [訂單成立時間, 小計]
        // 於 Service 層逐月加總，避免使用 DB 廠商特定日期函式
        @Query("""
                        SELECT d.merchOrder.createdAt,
                               d.unitPrice * d.quantity
                        FROM MerchOrderDetailBean d
                        WHERE d.merchOrder.createdAt >= :from
                          AND d.merchOrder.createdAt < :to
                          AND d.itemStatus = tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum.NORMAL
                        """)
        List<Object[]> findMerchOrderDateAndAmount(@Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);

        // Jason added: 會員「我的訂單」— 依登入使用者查詢商城訂單，依建立時間新到舊排序
        @Query("""
                        SELECT o
                        FROM MerchOrderBean o
                        WHERE o.userId.userId = :userId
                        ORDER BY o.createdAt DESC
                        """)
        List<MerchOrderBean> findByMemberUserId(@Param("userId") String userId);
}
