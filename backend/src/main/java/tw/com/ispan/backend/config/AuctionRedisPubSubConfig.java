package tw.com.ispan.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import tw.com.ispan.backend.auction.service.AuctionRedisMessageReceiver;

/**
 * Redis Pub/Sub 設定類別
 *
 * 用途：
 * 1. 建立 Redis 訊息監聽容器
 * 2. 訂閱拍賣相關的頻道（auction:bid:updates）
 * 3. 當 Redis 發佈訊息時，轉交給 Receiver 處理
 *
 * 常見應用：
 * - 即時競標更新
 * - 即時通知
 * - WebSocket / 推播前置事件
 */
@Configuration
public class AuctionRedisPubSubConfig {

    /**
     * Redis Pub/Sub 使用的頻道名稱
     *
     * 這個 channel 用來傳遞「競標更新事件」
     * 例如：
     * - 有人出價
     * - 更新最高價
     */
    public static final String REDIS_AUCTION_BID_CHANNEL = "auction:bid:updates";

    /**
     * Redis 訊息監聽容器（核心）
     *
     * 功能：
     * - 負責持續監聽 Redis channel
     * - 當有訊息 publish 時，自動觸發 listener
     *
     * @param connectionFactory Redis 連線工廠（由 Spring Boot 自動配置）
     * @param auctionListenerAdapter 將訊息轉交給指定方法的 adapter
     */
    @Bean
    public RedisMessageListenerContainer auctionRedisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter auctionListenerAdapter) {

        // 建立 Redis Listener 容器（類似事件監聽器管理器）
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();

        // 設定 Redis 連線來源
        container.setConnectionFactory(connectionFactory);

        // 訂閱指定 channel（可用 PatternTopic 支援 pattern）
        // 當有人 publish 到 auction:bid:updates，就會觸發 listener
        container.addMessageListener(
                auctionListenerAdapter,
                new PatternTopic(REDIS_AUCTION_BID_CHANNEL)
        );

        return container;
    }

    /**
     * MessageListenerAdapter（訊息轉接器）
     *
     * 功能：
     * - 把 Redis 收到的 message
     * - 轉成呼叫 Java method
     *
     * 這裡設定：
     * receiver.receiveRedisMessage(message)
     *
     * @param receiver 自訂的訊息處理類別（真正處理邏輯的地方）
     */
    @Bean
    public MessageListenerAdapter auctionListenerAdapter(AuctionRedisMessageReceiver receiver) {

        // 第二個參數："receiveRedisMessage"
        // 代表收到訊息後會呼叫這個 method
        return new MessageListenerAdapter(receiver, "receiveRedisMessage");
    }
}