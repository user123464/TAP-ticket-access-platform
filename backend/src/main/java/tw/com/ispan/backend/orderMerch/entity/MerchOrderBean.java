package tw.com.ispan.backend.orderMerch.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "merch_orders")
@Data
public class MerchOrderBean {

    // 商城訂單編號
    @Id
    @Column(name = "m_order_id", columnDefinition = "CHAR(10)")
    private String mOrderId;

    // 購買者
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, columnDefinition = "CHAR(10)")
    private User userId;

    // 訂單總金額
    @Column(name = "total_amount", nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    // 付款時間
    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 付款狀態 (UNPAID 待付款, PAID 已付款, FAILED 支付失敗)
    @Column(name = "payment_status")
    private String paymentStatus = "UNPAID";

    // 訂單建立時間
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 綠界交易編號 (MerchantTradeNo)
    @Column(name = "merchant_trade_no", length = 20, unique = true)
    private String merchantTradeNo;

    @Column(name = "recipient_name", length = 50)
    private String recipientName;

    @Column(name = "recipient_phone", length = 20)
    private String recipientPhone;

    @Column(name = "recipient_email", length = 100)
    private String recipientEmail;

    @Column(name = "recipient_address", length = 255)
    private String recipientAddress;

    @Column(name = "identity_number", length = 20)
    private String identityNumber;

    @OneToMany(mappedBy = "merchOrder", fetch = FetchType.LAZY, cascade = { CascadeType.PERSIST, CascadeType.MERGE })
    private List<MerchOrderDetailBean> merchOrderDetails;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    // 訂單編號UUID亂碼生成
    @PrePersist
    public void generateCustomId() {
        if (this.mOrderId == null) {
            this.mOrderId = UUID
                    .randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 10)
                    .toUpperCase();
        }

        if (this.merchantTradeNo == null) {
            this.merchantTradeNo = "MAP" + UUID.randomUUID()
                    .toString()
                    .replace("-", "")
                    .substring(0, 17)
                    .toUpperCase();
            ;
        }
    }

    public void cancelDetails() {
        if (this.merchOrderDetails != null) {
            for (MerchOrderDetailBean detail : this.merchOrderDetails) {
                detail.setItemStatus(MerchOrderEnum.CANCELLED);
            }
        }
    }
}
