package tw.com.ispan.backend.products.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.products.entity.ProductImages;

public interface ProductImageRepository extends JpaRepository<ProductImages, Integer> {
}