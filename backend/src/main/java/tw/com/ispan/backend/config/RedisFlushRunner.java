package tw.com.ispan.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * 在後端啟動時自動清空 Redis 快取數據的 Runner，
 * 適合在開發環境（比照資料庫 SQL 初始化重置）時使用。
 */
@Component
public class RedisFlushRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(RedisFlushRunner.class);

    private final StringRedisTemplate redisTemplate;
    private final AppRedisProperties appRedisProperties;

    public RedisFlushRunner(StringRedisTemplate redisTemplate, AppRedisProperties appRedisProperties) {
        this.redisTemplate = redisTemplate;
        this.appRedisProperties = appRedisProperties;
    }

    @Override
    public void run(String... args) throws Exception {
        if (appRedisProperties.isFlushOnStartup()) {
            log.info("Redis flush-on-startup is enabled. Flushing Redis database...");
            try {
                redisTemplate.execute((RedisConnection connection) -> {
                    connection.serverCommands().flushDb();
                    return null;
                });
                log.info("Redis database flushed successfully.");
            } catch (Exception e) {
                log.error("Failed to flush Redis database during startup: {}", e.getMessage(), e);
            }
        } else {
            log.info("Redis flush-on-startup is disabled.");
        }
    }
}
