package tw.com.ispan.backend.orderTicket.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderDetailRepository;
import tw.com.ispan.backend.ticket.entity.TicketStatus;

/**
 * 售後退票服務。
 * 
 * <p>處理客戶取消門票、辦理退票的業務邏輯，包含退票截止期限校驗、門票明細與票券庫存狀態還原、以及訂單總額即時扣減。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketRefundService {

    private final TicketOrderDetailRepository ticketOrderDetailRepository;

    /**
     * 售後服務 - 取消退票 (扣減主檔總額並還原座位庫存)。
     *
     * @param tOrderId  訂單主檔 ID
     * @param tDetailId 欲退票的門票明細 ID
     * @return 退票處理結果字串
     */
    @Transactional
    public String refundTicket(String tOrderId, String tDetailId) {
        log.info("【售後退票】收到退票申請 - orderId={}, detailId={}", tOrderId, tDetailId);

        // 1. 查找該訂單下所有狀態仍為「正常(NORMAL)」的明細
        List<TicketOrderDetailBean> details = ticketOrderDetailRepository
                .findByTicketOrder_tOrderIdAndItemStatus(tOrderId, TicketOrderStatus.NORMAL);
        TicketOrderDetailBean targetDetail = null;

        // 2. 匹配要退票的目標明細
        for (TicketOrderDetailBean d : details) {
            if (d.getTDetailId().equals(tDetailId)) {
                targetDetail = d;
                break;
            }
        }

        // 3. 安全性檢驗與防呆
        if (targetDetail == null) {
            log.warn("【售後退票】退票失敗，找不到有效門票明細 - detailId={}", tDetailId);
            return "退票失敗：找不到該筆有效的門票明細！";
        }
        if (targetDetail.getIsUsed().equals(TicketOrderUse.Redeemed)) {
            log.warn("【售後退票】退票失敗，門票已核銷進場 - detailId={}", tDetailId);
            return "退票失敗：此門票已核銷進場，無法辦理退票！";
        }
        if (targetDetail.getIsUsed().equals(TicketOrderUse.Canceled)) {
            log.warn("【售後退票】退票失敗，門票已是退票取消狀態 - detailId={}", tDetailId);
            return "退票失敗：此門票已辦理退票！";
        }

        // 4. 驗證退票期限：活動開始前才可退票
        if (targetDetail.getTicketTicket() != null && targetDetail.getTicketTicket().getSession() != null) {
            tw.com.ispan.backend.theme.entity.Session session = targetDetail.getTicketTicket().getSession();
            if (session.getStartTime() != null && LocalDateTime.now().isAfter(session.getStartTime())) {
                log.warn("【售後退票】退票失敗，活動已開始 - sessionStartTime={}, currentTime={}", 
                        session.getStartTime(), LocalDateTime.now());
                return "退票失敗：該活動場次已開始，無法辦理退票！";
            }
        }

        // 5. 修改明細狀態為「已退票 (REFUNDED)」及「已取消 (Canceled)」
        targetDetail.setItemStatus(TicketOrderStatus.REFUNDED);
        targetDetail.setIsUsed(TicketOrderUse.Canceled);

        // 6. 將票券座位庫存還原為「可售 (AVAILABLE)」，釋放給其他消費者購買
        if (targetDetail.getTicketTicket() != null) {
            targetDetail.getTicketTicket().setStatus(TicketStatus.AVAILABLE);
            log.info("【售後退票】已還原座位庫存為 AVAILABLE - ticketId={}", targetDetail.getTicketTicket().getTicketId());
        }

        // 7. 扣減訂單主檔總額 (配合 JPA Dirty Checking 髒檢查，自動於事務提交時同步至 DB)
        TicketOrdersBean order = targetDetail.getTicketOrder();
        if (order != null && order.getTotalAmount() != null) {
            order.setTotalAmount(order.getTotalAmount().subtract(targetDetail.getUnitPrice()));
            log.info("【售後退票】已扣減訂單金額 - orderId={}, 扣除={}, 新總額={}", 
                    order.getTOrderId(), targetDetail.getUnitPrice(), order.getTotalAmount());
        }

        log.info("【售後退票】退票成功 - detailId={}", tDetailId);
        return "退票成功！已取消該張門票，訂單總額已自動扣減。";
    }
}
