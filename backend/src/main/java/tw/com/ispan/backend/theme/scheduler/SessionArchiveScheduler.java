package tw.com.ispan.backend.theme.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.theme.service.SessionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionArchiveScheduler {

    private final SessionService sessionService;

    /**
     * 每 5 分鐘執行場次封存：將已過期且仍為 ACTIVE 的場次封存。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void archiveExpiredSessions() {
        try {
            sessionService.archiveExpiredSessionsJob();
        } catch (Exception e) {
            log.error("場次封存排程失敗", e);
        }
    }
}
