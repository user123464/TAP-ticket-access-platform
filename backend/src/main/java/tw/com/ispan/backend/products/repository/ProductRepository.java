package tw.com.ispan.backend.products.repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.products.entity.Product;
import tw.com.ispan.backend.products.enums.ProductsStatus;

public interface ProductRepository extends JpaRepository<Product, Integer>, ProductDAO {
        // 未來如果有按活動、按分類篩選商品的需求，可以在這裡擴充方法
        // 例如：List<Product> findByThemeThemeId(Integer themeId);

        List<Product> findByTheme_Organizer_OrganizerId(String organizerId);

        List<Product> findByTheme_ThemeId(Integer themeId);

        List<Product> findByTheme_ThemeIdAndStatus(
                        Integer themeId,
                        ProductsStatus status);

        Optional<Product> findById(Integer productId);

        @Modifying
        @Query("UPDATE Product p SET p.status = :status WHERE p.productId IN :productId AND p.theme.organizer.organizerId = :organizerId")
        int updateStatusByOrganizer(
                        @Param("status") String status,
                        @Param("productId") List<Integer> productId,
                        @Param("organizerId") String organizerId);

        @Query(value = "select distinct p from Product p join p.variants v where p.productName like :productName and v.unitPrice > :unitPrice")
        public List<Product> method1(@Param("productName") String productName,
                        @Param("unitPrice") BigDecimal unitPrice);

}
