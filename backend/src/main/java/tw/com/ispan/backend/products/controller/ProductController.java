package tw.com.ispan.backend.products.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import tw.com.ispan.backend.products.dto.ProductDTO;
import tw.com.ispan.backend.products.dto.VariantDTO;
import tw.com.ispan.backend.products.entity.Product;

import tw.com.ispan.backend.products.enums.ProductsStatus;
import tw.com.ispan.backend.products.repository.ProductRepository;

@RestController
@RequestMapping("")
@CrossOrigin(origins = "*")
public class ProductController {

    private final ProductRepository productRepository;

    public ProductController(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    /**
     * 首頁全部商品
     */
    @GetMapping("/shop/home")
    public ResponseEntity<?> getAllProducts() {

        List<ProductDTO> dtoList = productRepository.findAll().stream()

                .filter(product -> product.getStatus() == ProductsStatus.Released)

                .map(this::convertToDTO)

                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * 商品詳細頁
     */
    @GetMapping("/shop/product/{productId}")
    public ResponseEntity<?> getProductDetail(
            @PathVariable Integer productId) {

        Product product = productRepository.findById(productId).orElse(null);

        if (product == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(convertToDTO(product));
    }

    /**
     * 同一活動(theme)所有商品
     */
    @GetMapping("/shop/theme/{themeId}")
    public ResponseEntity<?> getProductsByTheme(
            @PathVariable Integer themeId) {

        List<ProductDTO> dtoList = productRepository
                .findByTheme_ThemeId(themeId)
                .stream()

                .filter(product -> product.getStatus() == ProductsStatus.Released)

                .map(this::convertToDTO)

                .collect(Collectors.toList());

        return ResponseEntity.ok(dtoList);
    }

    /**
     * Entity -> DTO
     */
    private ProductDTO convertToDTO(Product product) {

        ProductDTO productDTO = new ProductDTO();

        productDTO.setProductId(product.getProductId());
        productDTO.setProductName(product.getProductName());
        productDTO.setProductSimDescription(product.getProductSimDescription());
        productDTO.setProductDescription(product.getProductDescription());
        productDTO.setStatus(product.getStatus());

        // ---------------- Theme ----------------
        if (product.getTheme() != null) {

            productDTO.setThemeId(product.getTheme().getThemeId()); // product/10

            ProductDTO.ThemeInner themeInner = new ProductDTO.ThemeInner(
                    product.getTheme().getThemeId(),
                    product.getTheme().getTitle());

            productDTO.setTheme(themeInner);
        }

        // ---------------- Category ----------------
        if (product.getCategory() != null) {

            productDTO.setCategoryId(product.getCategory().getCategoryId());

            ProductDTO.CategoryInner catInner = new ProductDTO.CategoryInner(
                    product.getCategory().getCategoryId(),
                    product.getCategory().getCategoryName());

            productDTO.setCategory(catInner);
        }

        // ---------------- Images ----------------

        List<String> images = new ArrayList<>();

        if (product.getProductImages() != null) {

            images = product.getProductImages().stream()

                    .map(img -> img.getImageUrl())

                    .collect(Collectors.toList());
        }

        String mainImage = "";

        if (product.getProductImages() != null &&
                !product.getProductImages().isEmpty()) {

            mainImage = product.getProductImages().stream()

                    .filter(img -> Boolean.TRUE.equals(img.getIsMain()))

                    .map(img -> img.getImageUrl())

                    .findFirst()

                    .orElse(images.get(0));
        }

        productDTO.setMainImage(mainImage);
        productDTO.setImages(images);

        // ---------------- Variants ----------------

        List<VariantDTO> variantDTOs = new ArrayList<>();

        if (product.getVariants() != null) {

            variantDTOs = product.getVariants().stream()

                    .filter(v -> v.getStatus() == ProductsStatus.Released)

                    .map(v -> {

                        VariantDTO dto = new VariantDTO();

                        dto.setVariantId(v.getVariantId());
                        dto.setProductColor(v.getProductColor());
                        dto.setProductSize(v.getProductSize());
                        dto.setUnitPrice(v.getUnitPrice());
                        dto.setStockQty(v.getStockQty());
                        dto.setOrgSkuNo(v.getOrgSkuNo());

                        return dto;

                    }).collect(Collectors.toList());
        }

        productDTO.setVariants(variantDTOs);

        return productDTO;
    }

}