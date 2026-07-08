package tw.com.ispan.backend.products.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.products.enums.ProductsStatus;
import tw.com.ispan.backend.theme.entity.Theme;

@Data
@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class) // 啟用 Spring Data JPA 的審計功能（自動紀錄時間）
public class Product {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "product_id")
	private Integer productId;

	// FK Theme(theme_id)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "theme_id", nullable = false)
	private Theme theme;

	// FK Product_Categories(category_id)
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "category_id", nullable = false)
	private ProductCategories category;
	// 商品名稱
	@Column(name = "product_name", nullable = false, length = 100)
	private String productName;
	// 商品描述(簡)
	@Column(name = "product_sim_description", nullable = false, length = 100)
	private String productSimDescription;
	// 商品詳細描述
	@Column(name = "product_description", columnDefinition = "NVARCHAR(MAX)")
	private String productDescription;
	// 狀態,「枚舉類型」（常簡寫為 enum），用來定義一個只能包含特定預設值的資料型態。(0:草稿;1:上架;2:下架;3:缺貨)
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status")
	private ProductsStatus status = ProductsStatus.Draft;
	// 新增時間
	@CreatedDate
	@Column(name = "created_at", nullable = false)
	private LocalDateTime createdAt = LocalDateTime.now();;
	// 更新時間
	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
	// 上架時間
	@Column(name = "released_at")
	private LocalDateTime releasedAt;
	// 販售結束時間
	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	// 偉宏協助添加 : 一對多關連到商品訂單明細
	@OneToMany(mappedBy = "productProduct", cascade = CascadeType.ALL)
	private List<MerchOrderDetailBean> merchOrderDetails;
	@OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
	private List<ProductVariant> variants;

	@JsonIgnoreProperties("product")
	@OneToMany(mappedBy = "product", cascade = { CascadeType.ALL }, fetch = FetchType.EAGER)
	private List<ProductImages> productImages;

	public List<ProductImages> getProductImages() {
		return productImages;
	}

	public void setProductImages(List<ProductImages> productImages) {
		this.productImages = productImages;
	}

}