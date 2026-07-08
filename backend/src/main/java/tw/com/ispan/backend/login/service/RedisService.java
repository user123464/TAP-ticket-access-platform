package tw.com.ispan.backend.login.service;

import java.util.concurrent.TimeUnit;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedisService {

    private final StringRedisTemplate redisTemplate;

    /**
     * 寫入 Key-Value 且設定 TTL (存活秒數)
     */
    public void setWithTtl(String key, String value, long timeoutInSeconds) {
        redisTemplate.opsForValue().set(key, value, timeoutInSeconds, TimeUnit.SECONDS);
    }

    /**
     * 讀取 Key 的值
     */
    public String get(String key) {
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * 刪除 Key
     */
    public void delete(String key) {
        redisTemplate.delete(key);
    }

    /**
     * 將會話 JTI 加入使用者的有效 Session 集合中，並設定 TTL
     */
    public void addSession(String userId, String jti, long expireSeconds) {
        String key = "active_sessions:" + userId;
        redisTemplate.opsForSet().add(key, jti);
        redisTemplate.expire(key, expireSeconds, TimeUnit.SECONDS);
        log.info("已將 Session JTI 加入 Redis 快取: {}, 使用者: {}", jti, userId);
    }

    /**
     * 檢查使用者的該會話 JTI 是否仍有效
     */
    public boolean isSessionActive(String userId, String jti) {
        String key = "active_sessions:" + userId;
        return Boolean.TRUE.equals(redisTemplate.opsForSet().isMember(key, jti));
    }

    /**
     * 撤銷/移除使用者的特定會話 JTI (例如一般登出)
     */
    public void revokeSession(String userId, String jti) {
        String key = "active_sessions:" + userId;
        redisTemplate.opsForSet().remove(key, jti);
        log.info("已在 Redis 中撤銷使用者的特定 Session: {}, 使用者: {}", jti, userId);
    }

    /**
     * 註銷該使用者的所有會話 (例如登出所有其他裝置，或被強制下線)
     */
    public void revokeAllSessions(String userId) {
        String key = "active_sessions:" + userId;
        redisTemplate.delete(key);
        log.info("已清空使用者在 Redis 中的所有 Session 紀錄，使用者: {}", userId);
    }
}
