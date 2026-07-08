package tw.com.ispan.backend.theme.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.theme.service.ThemeService;

@Slf4j
@Component
@RequiredArgsConstructor
public class ThemeArchiveScheduler {

    private final ThemeService themeService;

    /**
     * 每 5 分鐘執行主題封存：將「至少有一筆場次、但沒有任何 ACTIVE 場次」的 ACTIVE 主題封存。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    public void archiveInactiveThemes() {
        try {
            themeService.archiveThemesJob();
        } catch (Exception e) {
            log.error("主題封存排程失敗", e);
        }
    }
}
