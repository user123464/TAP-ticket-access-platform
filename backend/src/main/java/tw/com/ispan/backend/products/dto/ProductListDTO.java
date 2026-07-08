package tw.com.ispan.backend.products.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import lombok.AllArgsConstructor;
import lombok.Data;
import tw.com.ispan.backend.products.entity.Product;
import tw.com.ispan.backend.products.entity.ProductVariant;
import tw.com.ispan.backend.products.enums.ProductsStatus;

/**
 * 後台商品總覽用 DTO。
 * 在 Service 的交易範圍內由 Product 實體組裝，避免將帶有 LAZY 關聯（theme/category/variants）
 * 的實體交給 Jackson 序列化，導致 LazyInitializationException("no session") 與雙向關聯無限遞迴。
 * 形狀刻意對齊前端 ProductList.vue 既有用法（theme.title / category.categoryName / variants[]）。
 */
@Data
public class ProductListDTO {

    private Integer productId;
    private String productName;
    private ProductsStatus status;
    private LocalDateTime createdAt;
    private ThemeInfo theme;
    private CategoryInfo category;
    private List<VariantInfo> variants;
    // [fix by Jason] 帶出所屬組織 ID，供前端做「只顯示本組織商品」的保險過濾（配合後端已依 organizerId 篩選）
    private String organizerId;

    public static ProductListDTO from(Product p) {
        ProductListDTO dto = new ProductListDTO();
        dto.setProductId(p.getProductId());
        dto.setProductName(p.getProductName());
        dto.setStatus(p.getStatus());
        dto.setCreatedAt(p.getCreatedAt());
        if (p.getTheme() != null) {
            dto.setTheme(new ThemeInfo(p.getTheme().getThemeId(), p.getTheme().getTitle()));
        }
        if (p.getCategory() != null) {
            dto.setCategory(new CategoryInfo(p.getCategory().getCategoryId(), p.getCategory().getCategoryName()));
        }
        if (p.getVariants() != null) {
            dto.setVariants(p.getVariants().stream().map(VariantInfo::from).collect(Collectors.toList()));
        }
        return dto;
    }

    @Data
    @AllArgsConstructor
    public static class ThemeInfo {
        private Integer themeId;
        private String title;
    }

    @Data
    @AllArgsConstructor
    public static class CategoryInfo {
        private Integer categoryId;
        private String categoryName;
    }

    @Data
    public static class VariantInfo {
        private Integer variantId;
        private String orgSkuNo;
        private String productSize;
        private String productColor;
        private BigDecimal unitPrice;
        private Integer stockQty;
        private String barcode;
        private ProductsStatus status;

        public static VariantInfo from(ProductVariant v) {
            VariantInfo vi = new VariantInfo();
            vi.setVariantId(v.getVariantId());
            vi.setOrgSkuNo(v.getOrgSkuNo());
            vi.setProductSize(v.getProductSize());
            vi.setProductColor(v.getProductColor());
            vi.setUnitPrice(v.getUnitPrice());
            vi.setStockQty(v.getStockQty());
            vi.setBarcode(v.getBarcode());
            vi.setStatus(v.getStatus());
            return vi;
        }
    }
}
