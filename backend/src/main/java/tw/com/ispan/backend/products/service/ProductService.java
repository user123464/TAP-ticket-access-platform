package tw.com.ispan.backend.products.service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.products.dto.OptionDTO;
import tw.com.ispan.backend.products.dto.ProductSaveDTO;
import tw.com.ispan.backend.products.entity.Product;
import tw.com.ispan.backend.products.entity.ProductCategories;
import tw.com.ispan.backend.products.entity.ProductImages;
import tw.com.ispan.backend.products.entity.ProductVariant;
import tw.com.ispan.backend.products.repository.ProductCategoriesRepository;
import tw.com.ispan.backend.products.repository.ProductImageRepository;
import tw.com.ispan.backend.products.repository.ProductRepository;
import tw.com.ispan.backend.products.repository.ProductVariantRepository;
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.repository.ThemeRepository;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductVariantRepository productVariantRepository;

    private final ThemeRepository themeRepository;

    private final ProductCategoriesRepository categoriesRepository;

    private final ProductRepository productRepository;

    private final ProductImageRepository productImagesRepository;

    // 1. 取得活動下拉選單
    public List<OptionDTO> getThemeOptions() {
        return themeRepository.findAll().stream()
                .map(theme -> new OptionDTO(theme.getThemeId(), theme.getTitle()))
                .collect(Collectors.toList());
    }

    // 2. 取得分類下拉選單
    public List<OptionDTO> getCategoryOptions() {
        return categoriesRepository.findAll().stream()
                .map(cat -> new OptionDTO(cat.getCategoryId(), cat.getCategoryName())) // 假設欄位名為 categoryName
                .collect(Collectors.toList());
    }

    // 3. 新增商品（包含多款式）
    public Product saveProduct(ProductSaveDTO dto) {
        return saveProduct(dto, null);
    }

    // 修改商品（包含多款式）
    @Transactional // 確保主表、子表若有一方失敗，全部 Rollback 回滾
    public Product saveProduct(ProductSaveDTO dto, Integer productId) {
        // 找尋對應的父實體
        Theme theme = themeRepository.findById(dto.getThemeId())
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的活動主題"));
        ProductCategories category = categoriesRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的商品分類"));
        List<ProductVariant> existingVariants = productVariantRepository.findByProductProductId(productId);

        // 轉換 Product 主檔
        Product product = new Product();
        product.setProductId(productId);
        product.setTheme(theme);
        product.setCategory(category);
        product.setProductName(dto.getProductName());
        product.setProductSimDescription(dto.getProductSimDescription());
        product.setProductDescription(dto.getProductDescription());
        product.setStatus(dto.getStatus());

        productRepository.save(product);

        // 處理多個款式（Variants）
        if (dto.getVariants() != null) {
            for (ProductSaveDTO.VariantDTO vDto : dto.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setVariantId(vDto.getId());
                variant.setProduct(product); // 關鍵：設定外鍵關聯回到主檔
                variant.setOrgSkuNo(vDto.getOrgSkuNo());
                variant.setProductSize(vDto.getProductSize());
                variant.setProductColor(vDto.getProductColor());
                variant.setUnitPrice(vDto.getUnitPrice());
                variant.setStockQty(vDto.getStockQty());
                variant.setBarcode(vDto.getBarcode());
                variant.setStatus(vDto.getStatus());

                productVariantRepository.save(variant);
            }

            for (ProductVariant variant : existingVariants) {
                boolean existsInDTO = dto.getVariants().stream()
                        .anyMatch(vDto -> vDto.getId() != null && vDto.getId().equals(variant.getVariantId()));
                if (!existsInDTO) {
                    productVariantRepository.delete(variant);
                }
            }
        }

        if (dto.getImages() != null && !dto.getImages().isEmpty()) {
            for (int i = 0; i < dto.getImages().size(); i++) {
                ProductImages image = new ProductImages();
                image.setProduct(product);
                image.setImageUrl(dto.getImages().get(i));
                image.setSortOrder(i + 1);
                image.setIsMain(i == 0);
                productImagesRepository.save(image);
            }
        }

        // 存入資料庫（由於設定了 CascadeType.ALL，款式會一起被寫入資料庫）
        return productRepository.findById(product.getProductId())
                .orElseThrow(() -> new IllegalArgumentException("找不到商品"));
    }

    // 取得所有商品（用於後台總覽表格）
    public List<Product> getAllProducts(String organizerId) {

        return productRepository.findByTheme_Organizer_OrganizerId(organizerId);

    }

    // 複製商品
    @Transactional
    public Product updateProduct(Integer productId, ProductSaveDTO dto) {

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("商品不存在"));

        Theme theme = themeRepository.findById(dto.getThemeId())
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的活動主題"));
        ProductCategories category = categoriesRepository.findById(dto.getCategoryId())
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的商品分類"));

        // 防止修改別人的商品
        // if (!product.getOrganizer().getOrganizerId().equals(organizerId)) {
        // throw new RuntimeException("無權限修改此商品");
        // }

        // 更新欄位
        product.setTheme(theme);
        product.setCategory(category);
        product.setProductName(dto.getProductName());
        product.setProductSimDescription(dto.getProductSimDescription());
        product.setProductDescription(dto.getProductDescription());
        product.setStatus(dto.getStatus());
        List<ProductVariant> variants = new ArrayList<>();
        if (dto.getVariants() != null) {
            for (ProductSaveDTO.VariantDTO vDto : dto.getVariants()) {
                ProductVariant variant = new ProductVariant();
                variant.setProduct(product); // 關鍵：設定外鍵關聯回到主檔
                variant.setOrgSkuNo(vDto.getOrgSkuNo());
                variant.setProductSize(vDto.getProductSize());
                variant.setProductColor(vDto.getProductColor());
                variant.setUnitPrice(vDto.getUnitPrice());
                variant.setStockQty(vDto.getStockQty());
                variant.setBarcode(vDto.getBarcode());
                variant.setStatus(vDto.getStatus());
                variants.add(variant);
            }
        }
        product.setVariants(variants);

        return productRepository.save(product);
    }

    // 刪除商品
    @Transactional
    public void deleteProduct(Integer productId) {

        productRepository.deleteById(productId);

    }

    public Product getProduct(Integer productId) {
        return productRepository.findById(productId)
                .orElseThrow(() -> new IllegalArgumentException("找不到商品"));
    }

    // 批次更新商品狀態
    @Transactional
    public void batchUpdateStatus(String organizerId, List<Integer> productId, String status) {

        if (productId == null || productId.isEmpty()) {
            throw new IllegalArgumentException("請至少選擇一項商品");
        }

        List<String> allowStatus = List.of("1", "2");
        if (!allowStatus.contains(status)) {
            throw new IllegalArgumentException("商品狀態錯誤");
        }

        int updatedRows = productRepository.updateStatusByOrganizer(
                status,
                productId,
                organizerId);

        if (updatedRows == 0) {
            throw new IllegalArgumentException("未找到可更新的商品，或無此操作權限");
        }

    }

    // 💡 僅保留這一個回傳 List 的乾淨方法，供 Controller 呼叫
    public List<Product> getOrganizerProductsWithoutPage(String organizerId) {
        return productRepository.findByTheme_Organizer_OrganizerId(organizerId);
    }

}