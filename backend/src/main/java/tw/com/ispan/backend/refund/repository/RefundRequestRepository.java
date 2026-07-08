package tw.com.ispan.backend.refund.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.refund.entity.RefundRequestBean;
import tw.com.ispan.backend.refund.enums.RefundRequestStatus;

/**
 * 退款申請單 Repository。
 */
public interface RefundRequestRepository extends JpaRepository<RefundRequestBean, Integer> {

    // B2B 端：查主辦方的申請清單 (新到舊)
    List<RefundRequestBean> findByOrganizerIdOrderByCreatedAtDesc(String organizerId);

    // 會員端：查自己的申請 (新到舊，前端用來標示「審核中」「已駁回」)
    List<RefundRequestBean> findByUserIdOrderByCreatedAtDesc(String userId);

    // 防重複申請：同一明細已有待審核申請時不允許再送
    boolean existsByDetailIdAndStatus(String detailId, RefundRequestStatus status);
}
