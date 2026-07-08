package tw.com.ispan.backend.orderMerch.entity;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import tw.com.ispan.backend.products.entity.Product;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "merch_order_details")
@Data
public class MerchOrderDetailBean {

    // 明細流水號
    @Id
    @Column(name = "m_detail_id", length = 50)
    private String mDetailId;

    // 關聯商城訂單主檔
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "m_order_id", nullable = false, columnDefinition = "CHAR(10)")
    private MerchOrderBean merchOrder;

    // 關聯商品唯一識別碼
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product productProduct;

    // 款式識別碼
    @Column(name = "variant_id", nullable = false)
    private Integer variantId;

    // 購買數量
    @Column(name = "quantity", nullable = false)
    private Integer quantity;

    // 結帳時單價
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    // 明細狀態 (存 Enum String: NORMAL 正常, RETURNED 已退貨)
    @Enumerated(EnumType.STRING)
    @Column(name = "item_status", nullable = false)
    private MerchOrderEnum itemStatus = MerchOrderEnum.NORMAL;

    // 產生明細亂數 ID
    @PrePersist
    public void generatedCustomId() {
        if (this.mDetailId == null) {
            this.mDetailId = UUID
                    .randomUUID()
                    .toString();
        }
    }
}
