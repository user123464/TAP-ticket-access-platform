package tw.com.ispan.backend.products.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.products.entity.ProductVariant;

public interface ProductVariantRepository extends JpaRepository<ProductVariant, Integer> {
    // 繼承 JpaRepository 後，已內建 findById(Integer id)，可用於查詢特定的 variantId

    List<ProductVariant> findByProductProductId(Integer productId);
}