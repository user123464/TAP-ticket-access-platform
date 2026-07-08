package tw.com.ispan.backend.orderMerch.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.orderMerch.dto.OrderCreateRequestDTO;
import tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderMerch.service.MerchOrderService;

@RestController
@RequestMapping("/api")
public class MerchOrderController {

    private final MerchOrderService merchOrderService;

    public MerchOrderController(MerchOrderService merchOrderService) {
        this.merchOrderService = merchOrderService;
    }

    // 建立商品訂單
    @PostMapping("/checkout/merch")
    public ResponseEntity<?> checkout(@RequestBody OrderCreateRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            OrderResponseDTO response = merchOrderService.createOrder(requestDTO, user.getUserId());
            return ResponseEntity.ok(Response.success("建立成功", response));
        } catch (RuntimeException e) {
            e.printStackTrace();
            return ResponseEntity.status(409).body(Response.error(e.getMessage()));
        }
    }

    // 售後退貨
    @PostMapping("/merch/{mOrderId}/refund/{mDetailId}")
    public ResponseEntity<?> refundMerch(
            @PathVariable String mOrderId,
            @PathVariable String mDetailId) {

        String resultMessage = merchOrderService.refundMerch(mOrderId, mDetailId);

        if (resultMessage.contains("失敗")) {
            return ResponseEntity.badRequest().body(Map.of("message", resultMessage));
        }
        return ResponseEntity.ok(Map.of("message", resultMessage));
    }

    // 後台營收統計
    @GetMapping("/revenue/merch")
    public ResponseEntity<?> getRevenue() {
        BigDecimal totalRevenue = merchOrderService.calculateTotalRevenue();
        return ResponseEntity.ok(Map.of("totalRevenue", totalRevenue));
    }

    // 取得所有商品訂單與明細
    @GetMapping("/merch")
    public ResponseEntity<?> getAllMerches() {
        return ResponseEntity.ok(merchOrderService.getAllMerches());
    }

    // 取得特定商品訂單與明細
    @GetMapping("/merch/{mOrderId}")
    public ResponseEntity<?> getMerchOrderById(@PathVariable String mOrderId) {
        OrderResponseDTO order = merchOrderService.getMerchOrderById(mOrderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

}
