package tw.com.ispan.backend.products.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.products.dto.BatchStatusDTO;
import tw.com.ispan.backend.products.dto.OptionDTO;
import tw.com.ispan.backend.products.dto.ProductDTO;
import tw.com.ispan.backend.products.dto.ProductListDTO;
import tw.com.ispan.backend.products.dto.ProductListDTO.CategoryInfo;
import tw.com.ispan.backend.products.dto.ProductListDTO.ThemeInfo;
import tw.com.ispan.backend.products.dto.ProductListDTO.VariantInfo;
import tw.com.ispan.backend.products.dto.ProductSaveDTO;
import tw.com.ispan.backend.products.dto.VariantDTO;
import tw.com.ispan.backend.products.entity.Product;
import tw.com.ispan.backend.products.repository.ProductRepository;
import tw.com.ispan.backend.products.service.ProductService;

@RestController
@RequestMapping("/api/org/{organizerId}/admin")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class MerchAdminController {

    private final ProductService productService;
    private final ProductRepository productRepository;

    // 廠商新增商品時的活動下拉選單 (themeId -> title)
    @GetMapping("/themes/options")
    public ResponseEntity<List<OptionDTO>> getThemeOptions() {
        return ResponseEntity.ok(productService.getThemeOptions());
    }

    // 分類下拉選單
    @GetMapping("/categories/options")
    public ResponseEntity<List<OptionDTO>> getCategoryOptions() {
        return ResponseEntity.ok(productService.getCategoryOptions());
    }

    // 廠商新增商品與款式
    @PostMapping("/products")
    public ResponseEntity<?> createProduct(@RequestBody ProductSaveDTO productSaveDTO) {
        try {
            Product savedProduct = productService.saveProduct(productSaveDTO);
            return ResponseEntity.ok(savedProduct);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    // 廠商複製商品與款式

    @GetMapping("/products/{productId}")
    public ResponseEntity<ProductDTO> getProduct(
            @PathVariable Integer productId) {
        return ResponseEntity.ok(convertToDTO(productService.getProduct(productId)));
    }

    // 修改商品與款式
    @PutMapping("/products/{productId}")
    public ResponseEntity<?> updateProduct(
            @RequestBody ProductSaveDTO productSaveDTO,
            @PathVariable Integer productId) {
        try {
            Product updateProduct = productService.saveProduct(productSaveDTO, productId);
            return ResponseEntity.ok(updateProduct);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    private ProductDTO convertToDTO(Product product) {
        ProductDTO dto = new ProductDTO();
        dto.setProductId(product.getProductId());
        dto.setProductName(product.getProductName());
        dto.setProductSimDescription(product.getProductSimDescription());
        dto.setProductDescription(product.getProductDescription());
        dto.setImages(product.getProductImages().stream().map(im -> im.getImageUrl()).toList());
        dto.setCategoryId(product.getCategory().getCategoryId());
        dto.setCategory(new ProductDTO.CategoryInner(
                product.getCategory().getCategoryId(),
                product.getCategory().getCategoryName()));
        dto.setTheme(new ProductDTO.ThemeInner(product.getTheme().getThemeId(), product.getTheme().getTitle()));
        dto.setThemeId(product.getTheme().getThemeId());
        dto.setStatus(product.getStatus());
        // 轉換款式列表
        List<VariantDTO> variantDTOs = product.getVariants().stream().map(variant -> {
            VariantDTO vDto = new VariantDTO();
            vDto.setVariantId(variant.getVariantId());
            vDto.setUnitPrice(variant.getUnitPrice());
            vDto.setStockQty(variant.getStockQty());
            vDto.setProductColor(variant.getProductColor());
            vDto.setProductSize(variant.getProductSize());
            vDto.setOrgSkuNo(variant.getOrgSkuNo());
            return vDto;
        }).toList();
        dto.setVariants(variantDTOs);
        return dto;
    }

    // 刪除商品與款式
    @DeleteMapping("/products/{productId}")
    public ResponseEntity<?> deleteProduct(
            @PathVariable Integer productId) {

        productService.deleteProduct(productId);

        return ResponseEntity.ok().build();
    }
    // @GetMapping("/products/{productId}")
    // public ResponseEntity<ProductDTO> getProduct(
    // @PathVariable Integer productId) {

    // return ResponseEntity.ok(productService.getProduct(productId));
    // }

    // 廠商商品總覽（表格呈現用）
    @GetMapping("/products")
    public ResponseEntity<List<ProductListDTO>> getAdminProducts(@PathVariable String organizerId) {
        // [fix by Jason] 資料隔離修正：原本用 productRepository.findAll() 會撈出「全站所有組織」的商品，
        // 導致任一組織（含新帳號）進商品後台都看得到別人的商品。
        // 改為依網址 path 上的 organizerId 篩選，只回傳該組織（theme -> organizer）的商品。
        List<Product> products = productRepository.findByTheme_Organizer_OrganizerId(organizerId); // 依組織篩選

        // 使用 stream 進行手動型態轉換
        List<ProductListDTO> dtoList = products.stream().map(product -> {
            ProductListDTO dto = new ProductListDTO();
            dto.setProductId(product.getProductId());
            dto.setProductName(product.getProductName());
            dto.setCategory(
                    new CategoryInfo(product.getCategory().getCategoryId(), product.getCategory().getCategoryName()));
            dto.setCreatedAt(product.getCreatedAt());
            dto.setStatus(product.getStatus());
            dto.setTheme(new ThemeInfo(product.getTheme().getThemeId(), product.getTheme().getTitle()));
            // [fix by Jason] 帶出商品所屬組織 ID（theme -> organizer），供前端保險過濾
            if (product.getTheme() != null && product.getTheme().getOrganizer() != null) {
                dto.setOrganizerId(product.getTheme().getOrganizer().getOrganizerId());
            }
            List<VariantInfo> variantInfos = product.getVariants().stream().map((v) -> {
                return VariantInfo.from(v);
            }).toList();
            dto.setVariants(variantInfos);
            // ...依此類推，把所有欄位 set 進去
            return dto;
        }).collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    // 批量上/下架
    @PutMapping("/batch-status")
    public ResponseEntity<?> batchUpdateStatus(
            @PathVariable String organizerId,
            @RequestBody BatchStatusDTO dto) {
        try {
            productService.batchUpdateStatus(organizerId, dto.getProductId(), dto.getStatus());
            return ResponseEntity.ok().body("批量更新成功");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }

    }

    // 關鍵字搜尋
    @GetMapping
    public ResponseEntity<List<Product>> getProducts(@PathVariable String organizerId) {

        List<Product> products = productService.getOrganizerProductsWithoutPage(organizerId);
        return ResponseEntity.ok(products);
    }
}