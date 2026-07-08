package tw.com.ispan.backend.ticket.scheduler;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.ticket.service.TicketService;

@Component // 必須交給 Spring 容器管理
@RequiredArgsConstructor
public class TicketCleanupScheduler {

    private final TicketService ticketService;

    /**
     * 幽靈座位清道夫排程
     * fixedDelay = 60000 代表：前一次掃描結束後，間隔 1 分鐘 (60000 毫秒) 再次執行
     */
    // @Scheduled(fixedDelay = 60000)
    public void cleanExpiredTicket() {
        try {
            ticketService.releaseGhostSeats();
        } catch (Exception e) {
            // 排程任務是沒有前端可以回傳錯誤的，所以遇到例外一定要自己 Catch 下來並印出 Log，
            // 否則例外拋出會導致這個排程線程死掉。
            System.out.println("[排程任務失敗] 清理幽靈座位時發生錯誤: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
