package tw.com.ispan.backend.orderMerch.service;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;
import tw.com.ispan.backend.products.entity.Product;
import tw.com.ispan.backend.products.entity.ProductVariant;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchRefundService {

    private final MerchOrderRepository merchOrderRepository;

    // 售後服務：退貨退款 (回補庫存與扣減總金額)
    @Transactional
    public String refundMerch(String mOrderId, String mDetailId) {
        MerchOrderBean order = merchOrderRepository.findById(mOrderId)
                .orElseThrow(() -> new RuntimeException("退貨失敗：找不到此訂單！"));

        MerchOrderDetailBean targetDetail = null;
        if (order.getMerchOrderDetails() != null) {
            for (MerchOrderDetailBean d : order.getMerchOrderDetails()) {
                if (d.getMDetailId().equals(mDetailId)) {
                    targetDetail = d;
                    break;
                }
            }
        }

        if (targetDetail == null) {
            return "退貨失敗：找不到該筆商品明細！";
        }

        if (targetDetail.getItemStatus().equals(MerchOrderEnum.RETURNED)) {
            return "退貨失敗：此商品已辦理退貨！";
        }

        // 修改明細狀態為已退貨
        targetDetail.setItemStatus(MerchOrderEnum.RETURNED);

        // 回補商品規格庫存
        Product product = targetDetail.getProductProduct();
        if (product != null && product.getVariants() != null) {
            System.out.println("Product = " + product.getProductName());
            for (ProductVariant variant : product.getVariants()) {
                System.out.println(
                        "DB Variant = " + variant.getVariantId());
                if (variant.getVariantId().equals(targetDetail.getVariantId())) {
                    variant.setStockQty(variant.getStockQty() + targetDetail.getQuantity());
                    break;
                }
            }
        }

        // 扣減總金額
        BigDecimal refundAmount = targetDetail.getUnitPrice().multiply(BigDecimal.valueOf(targetDetail.getQuantity()));
        order.setTotalAmount(order.getTotalAmount().subtract(refundAmount));

        merchOrderRepository.save(order);

        return "退貨成功！已取消該項商品，訂單總額已自動扣減。";
    }
}
