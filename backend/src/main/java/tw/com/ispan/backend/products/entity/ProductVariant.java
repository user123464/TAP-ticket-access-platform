package tw.com.ispan.backend.products.entity;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Data;
import tw.com.ispan.backend.products.enums.ProductsStatus;

@Entity
@Table(name = "product_variants")
@Data
public class ProductVariant {

	// 款式識別碼
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "variant_id")
	private Integer variantId;

	// FK Products(product_id) 商品唯一識別碼
	@ManyToOne(fetch = FetchType.EAGER)
	@JoinColumn(name = "product_id", nullable = false)
	@JsonIgnore // 防止 JSON 無限循環
	// @JsonBackReference
	// @JsonIgnoreProperties("variants") // 👈 忽略 Product 裡面的 variants 欄位即可，其餘保留
	private Product product;
	// 廠商商品編號
	@Column(name = "org_skuno", columnDefinition = "NVARCHAR(20)")
	private String orgSkuNo;
	// 商品尺寸
	@Column(name = "product_size", length = 50)
	private String productSize;
	// 商品顏色
	@Column(name = "product_color", length = 50)
	private String productColor;
	// 銷售單價
	@Column(name = "unit_price", nullable = false)
	private BigDecimal unitPrice;
	// 商品庫存
	@Column(name = "stock_qty", nullable = false)
	private Integer stockQty;
	// 國際條碼 (唯一)
	@Column(name = "barcode", length = 50)
	private String barcode;
	// 狀態,「枚舉類型」（常簡寫為 enum），用來定義一個只能包含特定預設值的資料型態。(0:草稿;1:上架;2:下架;3:缺貨)
	@Enumerated(EnumType.ORDINAL)
	@Column(name = "status")
	private ProductsStatus status = ProductsStatus.Draft;

}