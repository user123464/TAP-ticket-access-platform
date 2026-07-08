package tw.com.ispan.backend.products.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.products.entity.MerchCart;

public interface MerchCartRepository extends JpaRepository<MerchCart, Integer> {

    // 透過 userId 搜尋該使用者的所有購物車項目
    List<MerchCart> findByLockBy(String lockBy);

    // 透過 userId 與 variantId 尋找特定商品，用來判斷購物車是否已有該商品
    MerchCart findByLockByAndVariantVariantId(String lockBy, Integer variantId);

    // 刪除特定使用者的特定商品
    void deleteByLockByAndVariantVariantId(String lockBy, Integer variantId);

    // 刪除指定使用者的所有購物車商品
    void deleteAllByLockBy(String lockBy);
}