package tw.com.ispan.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 自定義的 Redis 配置屬性類別，用於自動產生 IDE 配置元數據（metadata），
 * 消除 properties 檔案中 'unknown property' 的警告。
 */
@Component
@ConfigurationProperties(prefix = "app.redis")
public class AppRedisProperties {

    /**
     * 是否在後端啟動時自動清空 Redis 快取數據
     */
    private boolean flushOnStartup = false;

    public boolean isFlushOnStartup() {
        return flushOnStartup;
    }

    public void setFlushOnStartup(boolean flushOnStartup) {
        this.flushOnStartup = flushOnStartup;
    }
}
