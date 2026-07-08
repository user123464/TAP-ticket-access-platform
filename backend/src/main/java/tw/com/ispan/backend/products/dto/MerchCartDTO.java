package tw.com.ispan.backend.products.dto;

import java.time.LocalDateTime;

import lombok.Data;

@Data
public class MerchCartDTO {
    private Integer cartId;
    private String userId;
    private Integer variantId;
    private Integer quantity;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime productExpireDate;

}
