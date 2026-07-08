package tw.com.ispan.backend.products.dto;

import tw.com.ispan.backend.products.enums.ProductsStatus;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductSaveDTO {
    private Integer themeId; // 前端下拉選單傳過來的活動ID
    private Integer categoryId; // 前端下拉選單傳過來的分類ID
    private String userId; // 建立者的使用者帳號（廠商ID）
    private String productName;
    private String productSimDescription;
    private String productDescription;
    private ProductsStatus status; // DRAFT, PUBLISHED 等

    private List<VariantDTO> variants; // 同步新增的多個規格款式
    private List<String> images;

    @Data
    public static class VariantDTO {
        private Integer id;
        private String orgSkuNo;
        private String productSize;
        private String productColor;
        private BigDecimal unitPrice;
        private Integer stockQty;
        private String barcode;
        private ProductsStatus status;
    }
}