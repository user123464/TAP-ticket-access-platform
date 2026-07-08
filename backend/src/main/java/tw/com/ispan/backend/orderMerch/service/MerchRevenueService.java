package tw.com.ispan.backend.orderMerch.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchRevenueService {

    private final MerchOrderRepository merchOrderRepository;

    // 計算非退貨商品的總營業額
    @Transactional
    public BigDecimal calculateTotalRevenue() {
        List<MerchOrderBean> orders = merchOrderRepository.findAll();
        BigDecimal totalRevenue = BigDecimal.ZERO;

        for (MerchOrderBean order : orders) {
            if (order.getMerchOrderDetails() != null) {
                for (MerchOrderDetailBean detail : order.getMerchOrderDetails()) {
                    if (detail.getItemStatus().equals(MerchOrderEnum.NORMAL)) {
                        BigDecimal subtotal = detail.getUnitPrice().multiply(BigDecimal.valueOf(detail.getQuantity()));
                        totalRevenue = totalRevenue.add(subtotal);
                    }
                }
            }
        }
        return totalRevenue;
    }
}
