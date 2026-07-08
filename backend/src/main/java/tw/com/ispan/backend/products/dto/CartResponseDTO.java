package tw.com.ispan.backend.products.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class CartResponseDTO {

    private Integer cartId;

    private Integer productId;

    private Integer variantId;

    private String productName;

    private String mainImage;

    private String orgSkuNo;

    private String productColor;

    private String productSize;

    private BigDecimal unitPrice;

    private Integer stockQty;

    private Integer quantity;
}