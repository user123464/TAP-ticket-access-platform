package tw.com.ispan.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

import tw.com.ispan.backend.ticket.service.RedisMessageReceiver;

@Configuration
public class RedisPubSubConfig {

    // 定義 Redis 廣播頻道的名稱
    public static final String REDIS_TICKET_CHANNEL = "ticket:session:updates";

    /**
     * 1. 建立 Redis 訊息監聽組合櫃
     * 它是負責在背景不斷調度執行緒、傾聽 Redis 廣播的核心引擎
     */
    @Bean
    public RedisMessageListenerContainer redisContainer(
            RedisConnectionFactory connectionFactory,
            MessageListenerAdapter listenerAdapter) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);

        // 🌟 讓這個貨櫃訂閱指定的頻道 (Topic)
        // 當該頻道有訊息時，交給我們自訂的 listenerAdapter 處理
        container.addMessageListener(listenerAdapter, new PatternTopic(REDIS_TICKET_CHANNEL));

        return container;
    }

    /**
     * 2. 訊息監聽適配器
     * 它的作用是把 Redis 掉下來的原始訊息，轉交給我們指定的 Java 類別與方法
     * 這裡我們指定轉交給一個叫做 "receiveRedisMessage" 的方法（我們下一小步會寫）
     */
    @Bean
    public MessageListenerAdapter listenerAdapter(RedisMessageReceiver receiver) {
        // 參數一：誰要來收信 (自訂的 Component)
        // 參數二：收信後要呼叫該物件的哪個方法名字
        return new MessageListenerAdapter(receiver, "receiveRedisMessage");
    }
}
