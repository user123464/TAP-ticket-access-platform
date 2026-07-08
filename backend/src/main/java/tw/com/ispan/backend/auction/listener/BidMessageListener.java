package tw.com.ispan.backend.auction.listener;

import java.io.IOException;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Component;

import com.rabbitmq.client.Channel;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.auction.dto.message.BidConfirmedMessageDTO;
import tw.com.ispan.backend.auction.enums.BidPersistenceResult;
import tw.com.ispan.backend.auction.service.BidEventPublisher;
import tw.com.ispan.backend.auction.service.BidPersistenceService;
import tw.com.ispan.backend.config.RabbitMQConfig;

@Slf4j
@Component
@RequiredArgsConstructor
public class BidMessageListener {

    private final BidEventPublisher bidEventPublisher;
    private final BidPersistenceService bidPersistenceService;

    @RabbitListener(queues = RabbitMQConfig.BID_CONFIRMATION_QUEUE)
    public void handleBidConfirmation(
            BidConfirmedMessageDTO message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long deliveryTag) {
        try {
            // 先持久化再決定是否發布事件：STALE / DUPLICATE 的出價直接 ack 並 return，
            // 不發布 BidConfirmedEvent，避免重複/過期出價觸發重複的通知與推播。
            // （auction3 曾將 publish 移到持久化之前，會讓重複出價也發事件，語意與其
            //   commit 訊息「避免重複調用」相反，故合併時保留此正確順序。）
            BidPersistenceResult result = bidPersistenceService.persistConfirmedBid(message);
            if (result == BidPersistenceResult.STALE || result == BidPersistenceResult.DUPLICATE) {
                channel.basicAck(deliveryTag, false);
                return;
            }

            bidEventPublisher.publishBidConfirmedEvent(message);
            channel.basicAck(deliveryTag, false);
        } catch (Exception e) {
            log.error("Bid confirmation listener failed: {}", e.getMessage(), e);
            try {
                channel.basicNack(deliveryTag, false, true);
            } catch (IOException ioException) {
                log.error("RabbitMQ NACK send failed", ioException);
            }
        }
    }
}