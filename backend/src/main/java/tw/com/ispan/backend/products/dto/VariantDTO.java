package tw.com.ispan.backend.products.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class VariantDTO {
        private Integer variantId;
        private String productColor;
        private String productSize;
        private BigDecimal unitPrice;
        private Integer stockQty;
        private String orgSkuNo;
}