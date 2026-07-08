package tw.com.ispan.backend.products.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.transaction.Transactional;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.products.dto.CartResponseDTO;
import tw.com.ispan.backend.products.entity.MerchCart;
import tw.com.ispan.backend.products.repository.MerchCartRepository;
import tw.com.ispan.backend.products.repository.ProductVariantRepository;

@RestController
@RequestMapping("/shop/cart")
@CrossOrigin(origins = "*")
public class CartController {

    private final MerchCartRepository merchCartRepository;
    private final ProductVariantRepository productVariantRepository;

    public CartController(MerchCartRepository merchCartRepository, ProductVariantRepository productVariantRepository) {
        this.merchCartRepository = merchCartRepository;
        this.productVariantRepository = productVariantRepository;
    }

    // 1. 取得該 userId 的專屬購物車清單
    @GetMapping
    public ResponseEntity<?> getCartByLockBy(@AuthenticationPrincipal CustomUserDetails user) {

        System.out.println("user = " + user);

        if (user == null) {
            System.out.println("❌ [購物車錯誤]：當前請求未攜帶 Token 或 Token 已過期，無法取得 user 資訊！");
            return ResponseEntity.status(401).body("請先登入會員");
        }

        List<MerchCart> cartList = merchCartRepository.findByLockBy(user.getUserId());

        List<CartResponseDTO> result = cartList.stream()
                .map(cart -> {

                    CartResponseDTO dto = new CartResponseDTO();

                    dto.setCartId(cart.getCartId());

                    dto.setProductId(
                            cart.getVariant().getProduct().getProductId());

                    dto.setVariantId(
                            cart.getVariant().getVariantId());

                    dto.setProductName(
                            cart.getVariant().getProduct().getProductName());

                    dto.setOrgSkuNo(
                            cart.getVariant().getOrgSkuNo());

                    dto.setProductColor(
                            cart.getVariant().getProductColor());

                    dto.setProductSize(
                            cart.getVariant().getProductSize());

                    dto.setUnitPrice(
                            cart.getVariant().getUnitPrice());

                    dto.setStockQty(
                            cart.getVariant().getStockQty());

                    dto.setQuantity(
                            cart.getQuantity());

                    dto.setMainImage(cart.getVariant().getProduct().getProductImages().stream()
                            .filter((img) -> img.getIsMain() != null && img.getIsMain())
                            .findFirst().map((img) -> img.getImageUrl())
                            .orElse(""));

                    return dto;
                })
                .toList();

        return ResponseEntity.ok(result);
    }

    // 2.1 新增商品
    @PostMapping("/add")
    public ResponseEntity<?> addToCart(@AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CartPayload payload) {

        if (user == null) {
            System.out.println("❌ [購物車錯誤]：當前請求未攜帶 Token 或 Token 已過期，無法取得 user 資訊！");
            return ResponseEntity.status(401).body("請先登入會員");
        }

        System.out.println("✅ [收到購物車請求] 使用者ID: " + user.getUserId()
                + ", 商品規格ID: " + payload.getVariantId()
                + ", 數量: " + payload.getQuantity());

        if (payload.getVariantId() == null || payload.getQuantity() == null) {
            return ResponseEntity.badRequest().body("請求參數不完整");
        }

        // 檢查購物車內是否已有該規格商品
        MerchCart existingCart = merchCartRepository.findByLockByAndVariantVariantId(user.getUserId(),
                payload.getVariantId());

        if (existingCart != null) {

            // 如果有了，直接疊加數量
            existingCart.setQuantity(
                    existingCart.getQuantity() + payload.getQuantity());
            merchCartRepository.save(existingCart);
        } else {
            // 如果沒有，新建立一筆
            MerchCart newCart = new MerchCart();
            newCart.setLockBy(user.getUserId());
            newCart.setQuantity(payload.getQuantity());
            // 透過傳過來的 variantId 去撈實體設定關聯
            productVariantRepository.findById(payload.getVariantId()).ifPresent(newCart::setVariant);
            newCart.setCreatedAt(LocalDateTime.now());

            merchCartRepository.save(newCart);
        }

        return ResponseEntity.ok("購物車同步成功");
    }

    // 2.2 更新商品
    @PostMapping("/update")
    public ResponseEntity<?> updateCartQuantity(@AuthenticationPrincipal CustomUserDetails user,
            @RequestBody CartPayload payload) {

        MerchCart existingCart = merchCartRepository.findByLockByAndVariantVariantId(user.getUserId(),
                payload.getVariantId());

        if (existingCart != null) {
            // 直接「覆蓋」數量，而不是用 + 號疊加
            existingCart.setQuantity(payload.getQuantity());
            merchCartRepository.save(existingCart);
            return ResponseEntity.ok("購物車數量更新成功");
        } else {
            return ResponseEntity.status(404).body("購物車內無此商品");
        }
    }

    // 3. 從購物車刪除特定商品
    @DeleteMapping("/remove/{variantId}")
    @Transactional // 涉及到 delete 操作，必須加上事務註解
    public ResponseEntity<?> removeFromCart(@AuthenticationPrincipal CustomUserDetails user,
            @PathVariable Integer variantId) {
        merchCartRepository.deleteByLockByAndVariantVariantId(user.getUserId(), variantId);
        return ResponseEntity.ok("商品已從購物車移除");
    }

}

// 簡單定義前端傳來的 JSON 結構，避免 DTO 噴錯誤
class CartPayload {
    @JsonProperty("variantId") // 💡 強制指定前端 JSON 欄位名稱
    private Integer variantId;

    @JsonProperty("quantity") // 💡 強制指定前端 JSON 欄位名稱
    private Integer quantity;

    public Integer getVariantId() {
        return variantId;
    }

    public void setVariantId(Integer variantId) {
        this.variantId = variantId;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }
}