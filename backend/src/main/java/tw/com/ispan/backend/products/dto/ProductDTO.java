package tw.com.ispan.backend.products.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import tw.com.ispan.backend.products.enums.ProductsStatus;

@Data
public class ProductDTO {
    private Integer productId;
    private String productName;
    private String productSimDescription;
    private String productDescription;
    private String mainImage;
    private List<String> images;
    private List<VariantDTO> variants;

    // 🔥 補上這兩個內部類別（或獨立檔案），對齊前端 product.category.categoryName 與 product.theme.title
    private CategoryInner category;
    private ThemeInner theme;

    private ProductsStatus status; // 2. 補上 status 欄位
    private Integer themeId; // 3. 補上 themeId 欄位
    private Integer categoryId; // 5. 補上 categoryId 欄位

    @Data
    @AllArgsConstructor
    public static class CategoryInner {
        private Integer categoryId;
        private String categoryName;
    }

    @Data
    @AllArgsConstructor
    public static class ThemeInner {
        private Integer themeId;
        private String title;
    }

}
