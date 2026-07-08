package tw.com.ispan.backend.orderMerch.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import com.fasterxml.jackson.annotation.JsonProperty;

// 後端回傳給前端的商城訂單總摘要
@Data
public class OrderResponseDTO {

    @JsonProperty("mOrderId")
    private String mOrderId;
    private String userId;
    private BigDecimal totalAmount;
    private String paymentStatus;
    private String ecpayForm;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String recipientName;
    private String recipientPhone;
    private String recipientEmail;
    private String recipientAddress;
    private String identityNumber;
    private List<OrderDetailResponseDTO> orderDetailMerches;

    @Data
    public static class OrderDetailResponseDTO {
        @JsonProperty("mDetailId")
        private String mDetailId;
        private ProductDTO productProduct;
        private Integer variantId;
        private Integer quantity;
        private BigDecimal unitPrice;
        private MerchOrderEnum itemStatus = MerchOrderEnum.NORMAL;
        private Integer productId;
        private String productName;
    }

    @Data
    public static class ProductDTO {
        private Integer productId;
        private String productName;
        private List<ProductVariantDTO> variants;
    }

    @Data
    public static class ProductVariantDTO {
        private Integer variantId;
        private String productSize;
        private String productColor;
        private BigDecimal unitPrice;
    }
}
