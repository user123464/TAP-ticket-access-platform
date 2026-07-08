package tw.com.ispan.backend.products.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;

@Entity
@Table(name = "merch_cart")
@Data
public class MerchCart {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "cart_id")
	private Integer cartId;

	// [FK] 鎖定者 ID（防惡意劫持，記錄被誰鎖定）
	@Column(name = "locked_by_user", columnDefinition = "CHAR(10)")
	private String lockBy;

	// FK Product_Variants(variant_id)
	@ManyToOne
	@JoinColumn(name = "variant_id", nullable = false)
	private ProductVariant variant;
	// 商品名稱
	@Column(name = "quantity", nullable = false, length = 100)
	private Integer quantity;
	// 新增時間
	@CreatedDate
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt;
	// 更新時間
	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	// 販售結束時間
	@Column(name = "product_expire_date")
	private LocalDateTime productExpireDate;

}