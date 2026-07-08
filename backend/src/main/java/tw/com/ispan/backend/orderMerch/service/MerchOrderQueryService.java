package tw.com.ispan.backend.orderMerch.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import org.springframework.stereotype.Service;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO.OrderDetailResponseDTO;
import tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO.ProductDTO;
import tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO.ProductVariantDTO;
import tw.com.ispan.backend.products.entity.Product;
import tw.com.ispan.backend.products.entity.ProductVariant;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;

@Service
@Slf4j
@RequiredArgsConstructor
public class MerchOrderQueryService {

    private final MerchOrderRepository merchOrderRepository;

    /**
     * 根據訂單編號，查詢單筆商品訂單詳情。
     */
    @Transactional
    public OrderResponseDTO getMerchOrderById(String mOrderId) {
        return merchOrderRepository.findById(mOrderId)
                .map(this::convertToResponseDTO)
                .orElse(null);
    }

    /**
     * 根據綠界交易號查詢資料庫對應的商品訂單 ID (mOrderId)，用於 Redirect 跳轉前端。
     */
    public String getMOrderIdByMerchantTradeNo(String merchantTradeNo) {
        return merchOrderRepository.findByMerchantTradeNo(merchantTradeNo)
                .map(o -> o.getMOrderId())
                .orElse(merchantTradeNo);
    }

    /**
     * 從資料庫取出所有 merch_orders 及其對應的明細
     */
    @Transactional
    public List<Map<String, Object>> getAllMerches() {
        List<MerchOrderBean> orders = merchOrderRepository.findAll();
        List<Map<String, Object>> result = new ArrayList<>();
        for (MerchOrderBean order : orders) {
            if (order.getMerchOrderDetails() != null) {
                for (MerchOrderDetailBean d : order.getMerchOrderDetails()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("m_order_id", order.getMOrderId());
                    map.put("user_id", order.getUserId() != null ? order.getUserId().getName() : "陳先生");
                    map.put("total_amount", order.getTotalAmount());
                    map.put("paid_at", order.getPaidAt() != null ? order.getPaidAt().toString().substring(0, 10) : "");
                    map.put("created_at",
                            order.getCreatedAt() != null ? order.getCreatedAt().toString().substring(0, 10) : "");
                    map.put("recipient_name", order.getRecipientName() != null ? order.getRecipientName() : "");
                    map.put("recipient_phone", order.getRecipientPhone() != null ? order.getRecipientPhone() : "");
                    map.put("recipient_email", order.getRecipientEmail() != null ? order.getRecipientEmail() : "");
                    map.put("recipient_address", order.getRecipientAddress() != null ? order.getRecipientAddress() : "");
                    map.put("identity_number", order.getIdentityNumber() != null ? order.getIdentityNumber() : "");

                    String itemStatusStr = "正常";
                    if (d.getItemStatus() == MerchOrderEnum.RETURNED) {
                        itemStatusStr = "退貨";
                    } else if (d.getItemStatus() == MerchOrderEnum.CANCELLED) {
                        itemStatusStr = "取消";
                    } else if (d.getItemStatus() == MerchOrderEnum.UNPAID) {
                        itemStatusStr = "待付";
                    }
                    map.put("item_status", itemStatusStr);
                    map.put("m_detail_id", d.getMDetailId());
                    map.put("product_id",
                            d.getProductProduct() != null ? d.getProductProduct().getProductId().toString() : "");
                    map.put("variant_id", d.getVariantId() != null ? d.getVariantId().toString() : "");
                    map.put("quantity", d.getQuantity());
                    map.put("unit_price", d.getUnitPrice());

                    // Organizer ID from product
                    String orgId = "ORG0000001";
                    Integer themeId = 1;
                    if (d.getProductProduct() != null && d.getProductProduct().getTheme() != null) {
                        themeId = d.getProductProduct().getTheme().getThemeId();
                        if (d.getProductProduct().getTheme().getOrganizer() != null) {
                            orgId = d.getProductProduct().getTheme().getOrganizer().getOrganizerId();
                        }
                    }
                    map.put("organizer_id", orgId);
                    map.put("theme_id", themeId);

                    result.add(map);
                }
            }
        }
        return result;
    }

    /**
     * 轉換工具：將 Bean 轉為 DTO (公開以供其他 Service 使用，如 CreateService)
     */
    public OrderResponseDTO convertToResponseDTO(MerchOrderBean order) {
        OrderResponseDTO dto = new OrderResponseDTO();
        dto.setMOrderId(order.getMOrderId());
        dto.setUserId(order.getUserId().getUserId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setPaymentStatus(order.getPaymentStatus());
        dto.setPaidAt(order.getPaidAt());
        dto.setCreatedAt(order.getCreatedAt());
        dto.setRecipientName(order.getRecipientName());
        dto.setRecipientPhone(order.getRecipientPhone());
        dto.setRecipientEmail(order.getRecipientEmail());
        dto.setRecipientAddress(order.getRecipientAddress());
        dto.setIdentityNumber(order.getIdentityNumber());

        List<OrderDetailResponseDTO> detailDTOs = new ArrayList<>();
        if (order.getMerchOrderDetails() != null) {
            for (MerchOrderDetailBean b : order.getMerchOrderDetails()) {
                OrderDetailResponseDTO dDto = new OrderDetailResponseDTO();
                dDto.setMDetailId(b.getMDetailId());
                
                // Populate ProductDTO to prevent infinite recursion/loops
                Product productEntity = b.getProductProduct();
                if (productEntity != null) {
                    ProductDTO pDto = new ProductDTO();
                    pDto.setProductId(productEntity.getProductId());
                    pDto.setProductName(productEntity.getProductName());
                    
                    List<ProductVariantDTO> vDtos = new ArrayList<>();
                    if (productEntity.getVariants() != null) {
                        for (ProductVariant v : productEntity.getVariants()) {
                            ProductVariantDTO vDto = new ProductVariantDTO();
                            vDto.setVariantId(v.getVariantId());
                            vDto.setProductSize(v.getProductSize());
                            vDto.setProductColor(v.getProductColor());
                            vDto.setUnitPrice(v.getUnitPrice());
                            vDtos.add(vDto);
                        }
                    }
                    pDto.setVariants(vDtos);
                    dDto.setProductProduct(pDto);
                    
                    dDto.setProductId(productEntity.getProductId());
                    dDto.setProductName(productEntity.getProductName());
                }
                
                dDto.setVariantId(b.getVariantId());
                dDto.setQuantity(b.getQuantity());
                dDto.setUnitPrice(b.getUnitPrice());
                dDto.setItemStatus(b.getItemStatus());
                detailDTOs.add(dDto);
            }
        }
        dto.setOrderDetailMerches(detailDTOs);
        return dto;
    }
}
