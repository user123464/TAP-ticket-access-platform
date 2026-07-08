package tw.com.ispan.backend.orderTicket.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.service.OrderTicketService;

/**
 * 訂單定時清理排程任務。
 * 
 * <p>定期（每分鐘）掃描數據庫中狀態為 UNPAID（待付款）且建立時間超過 15 分鐘的幽靈訂單，
 * 自動將其狀態標記為 FAILED（已失敗，並寫入失敗時間 failed_at），並將訂單所鎖定的票券座位釋放回 AVAILABLE（可售），
 * 以防座位被長時間惡意佔用而影響正常銷售。</p>
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderCleanupScheduler {

    // 注入訂單外觀服務門面 (Facade)
    private final OrderTicketService orderTicketService;

    /**
     * 定時掃描並清理過期訂單與還原座位庫存。
     * 
     * <p>每 60,000 毫秒 (即每分鐘) 觸發一次，採用 fixedDelay 確保上一次執行結束後才開啟下一次，避免重複執行。</p>
     */
    @Scheduled(fixedDelay = 60000)
    public void cleanExpiredOrders() {
        log.info("【排程任務】啟動：開始掃描逾期 15 分鐘未付款訂單...");
        try {
            // 調用外觀服務執行清理釋放邏輯
            orderTicketService.releaseExpiredOrders();
            log.info("【排程任務】結束：逾期訂單清理掃描完成。");
        } catch (Exception e) {
            log.error("【排程任務失敗】清理逾期未付款訂單時發生錯誤: {}", e.getMessage(), e);
        }
    }
}
