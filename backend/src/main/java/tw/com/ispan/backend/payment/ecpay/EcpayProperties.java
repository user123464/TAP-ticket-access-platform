package tw.com.ispan.backend.payment.ecpay;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "ecpay")
@Setter
@Getter
public class EcpayProperties {

    private String merchantId;
    private String hashKey;
    private String hashIv;
    private String apiUrl;

    private String returnUrl;
    private String orderResultUrl;
    private String clientBackUrl;
}
