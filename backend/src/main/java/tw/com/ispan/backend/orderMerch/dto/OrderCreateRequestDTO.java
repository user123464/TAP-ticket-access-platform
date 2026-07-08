package tw.com.ispan.backend.orderMerch.dto;

import java.util.List;
import lombok.Data;

// 前端要傳給後端的商城結帳包裹
@Data
public class OrderCreateRequestDTO {

    private String submitToken;
    private String userId;

    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String recipientAddress;
    private String identityNumber;

    private List<DetailRequest> orderMerches;

    @Data
    public static class DetailRequest {
        private Integer productId;
        private Integer variantId;
        private Integer quantity;
    }

}
