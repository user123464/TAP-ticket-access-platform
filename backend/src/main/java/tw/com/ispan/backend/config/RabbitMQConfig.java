package tw.com.ispan.backend.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    // ==========================================
    // 鎖定票券 相關常數
    // ==========================================
    // 1. 定義交換機名稱 (郵局)
    public static final String TICKET_EXCHANGE = "ticket.exchange";
    // 2. 定義佇列名稱 (信箱)
    public static final String TICKET_LOCK_QUEUE = "ticket.lock.queue";
    // 3. 定義路由鍵 (郵遞區號)
    public static final String TICKET_LOCK_ROUTING_KEY = "ticket.lock";

    @Bean
    public DirectExchange ticketExchange() {
        return new DirectExchange(TICKET_EXCHANGE);
    }

    @Bean
    public Queue ticketLockQueue() {
        // true 代表持久化，RabbitMQ 重啟後佇列還在
        return new Queue(TICKET_LOCK_QUEUE);
    }

    @Bean
    public Binding ticketLockBinding(Queue ticketLockQueue, DirectExchange ticketExchange) {
        // 將信箱與郵局綁定，並設定郵遞區號
        return BindingBuilder.bind(ticketLockQueue)
                .to(ticketExchange)
                .with(TICKET_LOCK_ROUTING_KEY);

    }

    // 🌟 核心：將訊息轉換為 JSON 格式，方便跨語言與管理面板閱讀
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    // ==========================================
    // 延遲釋放座位 (DLQ) 相關常數
    // ==========================================
    public static final String TICKET_WAIT_QUEUE = "ticket.wait.queue"; // 等待房 (沒有消費者)
    public static final String TICKET_WAIT_ROUTING_KEY = "ticket.wait";

    public static final String TICKET_RELEASE_QUEUE = "ticket.release.queue"; // 死信佇列 (DLQ)
    public static final String TICKET_RELEASE_ROUTING_KEY = "ticket.release";

    // ==========================================
    // 競標出價 相關常數
    // ==========================================
    public static final String BID_EXCHANGE = "bid.exchange";
    public static final String BID_CONFIRMATION_QUEUE = "bid.confirmation.queue";
    public static final String BID_CONFIRMATION_ROUTING_KEY = "bid.confirmation";

    @Bean
    public DirectExchange bidExchange() {
        return new DirectExchange(BID_EXCHANGE);
    }

    @Bean
    public Queue bidConfirmationQueue() {
        return new Queue(BID_CONFIRMATION_QUEUE, true);
    }

    @Bean
    public Binding bidConfirmationBinding(Queue bidConfirmationQueue, DirectExchange bidExchange) {
        return BindingBuilder.bind(bidConfirmationQueue)
                .to(bidExchange)
                .with(BID_CONFIRMATION_ROUTING_KEY);
    }

    // 1. 建立等待房 (Wait Queue)
    @Bean
    public Queue ticketWaitQueue() {
        Map<String, Object> args = new HashMap<>();
        // 遺囑 1：當訊息死掉時，請交給哪個交換機處理？
        args.put("x-dead-letter-exchange", TICKET_EXCHANGE);
        // 遺囑 2：轉交時，請貼上哪個新的路由鍵？(讓它流向 DLQ)
        args.put("x-dead-letter-routing-key", TICKET_RELEASE_ROUTING_KEY);
        // 遺囑 3：壽命多長？(15分鐘 = 900,000 毫秒)
        // ⚠️ 測試期間，建議先改成 10000 (10秒)，才不用在螢幕前苦等 15 分鐘！
        args.put("x-message-ttl", 900000); // 15 分鐘

        // 裝備 4：開啟 Lazy 模式，應付海量堆積不爆記憶體
        // RabbitMQ 3.12+ 已移除 Lazy Queue 的特殊模式。
        // Broker 會自動依照記憶體狀況管理訊息，因此一般不需要再設定
        // x-queue-mode=lazy
        // args.put("x-queue-mode", "lazy");

        return new Queue(TICKET_WAIT_QUEUE, true, false, false, args);
    }

    // 2. 建立真正的死信佇列 (DLQ)
    @Bean
    public Queue ticketReleaseQueue() {
        return new Queue(TICKET_RELEASE_QUEUE, true);
    }

    // 3. 綁定等待房到交換機
    @Bean
    public Binding ticketWaitBinding() {
        return BindingBuilder.bind(ticketWaitQueue())
                .to(ticketExchange())
                .with(TICKET_WAIT_ROUTING_KEY);
    }

    // 4. 綁定死信佇列到交換機
    @Bean
    public Binding ticketReleaseBinding() {
        return BindingBuilder.bind(ticketReleaseQueue())
                .to(ticketExchange())
                .with(TICKET_RELEASE_ROUTING_KEY);
    }
}
