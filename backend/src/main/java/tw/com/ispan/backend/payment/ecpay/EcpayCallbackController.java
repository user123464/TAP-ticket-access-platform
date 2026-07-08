package tw.com.ispan.backend.payment.ecpay;

import java.util.Map;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderMerch.service.MerchOrderService;
import tw.com.ispan.backend.orderTicket.service.OrderTicketService;
import tw.com.ispan.backend.subscription.service.SubscriptionService;

@RestController
@Slf4j
@RequestMapping("/api")
public class EcpayCallbackController {
    /**
     * 接收綠界科技 Server-to-Server 的付款狀態通知 Webhook。
     */
    private final OrderTicketService orderTicketService;
    private final MerchOrderService merchOrderService;
    private final SubscriptionService subscriptionService;

    // 建構子注入
    EcpayCallbackController(OrderTicketService orderTicketService, MerchOrderService merchOrderService,
            SubscriptionService subscriptionService) {
        this.orderTicketService = orderTicketService;
        this.merchOrderService = merchOrderService;
        this.subscriptionService = subscriptionService;
    }

    @PostMapping(value = "/checkout/ecpay-callback", consumes = org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    public String ecpayCallback(@RequestParam Map<String, String> params) {
        String merchantTradeNo = params.get("MerchantTradeNo");
        if (merchantTradeNo != null && merchantTradeNo.startsWith("SAP")) {
            return subscriptionService.handleEcpayCallback(params);
        }
        if (merchantTradeNo != null && merchantTradeNo.startsWith("MAP")) {
            return merchOrderService.handleEcpayCallback(params);
        }
        return orderTicketService.handleEcpayCallback(params);
    }
}
