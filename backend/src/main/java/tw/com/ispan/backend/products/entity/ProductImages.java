package tw.com.ispan.backend.products.entity;

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
@Table(name = "product_images")
@Data
public class ProductImages {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "image_id")
	private Integer imageId;

	// FK Products(product_id)
	@ManyToOne
	@JoinColumn(name = "product_id", nullable = false)
	private Product product;
	// 商品名稱
	@Column(name = "image_url", columnDefinition = "NVARCHAR(1000)")
	private String imageUrl;
	// 商品描述(簡)
	@Column(name = "sort_order")
	private Integer sortOrder;
	// 商品詳細描述
	@Column(name = "is_main")
	private Boolean isMain;

}