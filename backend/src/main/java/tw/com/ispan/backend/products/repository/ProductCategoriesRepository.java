package tw.com.ispan.backend.products.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.products.entity.ProductCategories;

public interface ProductCategoriesRepository extends JpaRepository<ProductCategories, Integer> {
}