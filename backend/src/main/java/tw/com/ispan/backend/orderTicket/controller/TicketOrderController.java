package tw.com.ispan.backend.orderTicket.controller;

import java.math.BigDecimal;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.orderTicket.dto.OrderCreateRequestDTO;
import tw.com.ispan.backend.orderTicket.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderTicket.service.OrderTicketService;
import tw.com.ispan.backend.orderMerch.service.MerchOrderService;
import tw.com.ispan.backend.subscription.service.SubscriptionService;

@RestController
@Slf4j
@RequestMapping("/api")
public class TicketOrderController {

    private final OrderTicketService orderTicketService;
    private final MerchOrderService merchOrderService;
    private final SubscriptionService subscriptionService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    // 建構子注入
    TicketOrderController(OrderTicketService orderTicketService, MerchOrderService merchOrderService,
            SubscriptionService subscriptionService) {
        this.orderTicketService = orderTicketService;
        this.merchOrderService = merchOrderService;
        this.subscriptionService = subscriptionService;
    }

    /**
     * 窗口 1：建立購票訂單。
     *
     * <p>
     * API: POST /api/checkout/ticket
     * 對應前端 Vue 頁面購物車點擊結帳的行為。包含防重複點擊的冪等性控制。
     * </p>
     *
     * @param requestDTO 前端傳入之訂單與實名制資訊包裹
     * @param user       自 Spring Security 獲取之當前登入會員詳情
     * @return 統一響應包裝，若成功則帶回 OrderResponseDTO 訂單結果
     */
    @PostMapping("/checkout/ticket")
    public Response checkout(
            @RequestBody OrderCreateRequestDTO requestDTO,
            @AuthenticationPrincipal CustomUserDetails user) {

        try {

            OrderResponseDTO response = orderTicketService.createOrder(requestDTO, user.getUserId());

            System.out.println("Controller收到DTO:");
            System.out.println(response);
            System.out.println(response.getEcpayForm());

            return Response.success("建立成功", response);

        } catch (RuntimeException e) {

            return Response.error(e.getMessage());
        }
    }

    /**
     * 窗口 2：現場門票掃描與核銷。
     *
     * <p>
     * API: POST /api/checkin?qrCodeHash=xxx
     * 供現場工作人員手持設備掃描門票 QR Code 進行驗票進場。
     * </p>
     *
     * @param qrCodeHash 門票雜湊值
     * @return 驗票狀態響應體，失败返回 400，成功返回 200 帶歡迎詞
     */
    @PostMapping("/checkin")
    public ResponseEntity<?> checkInTicket(@RequestParam String qrCodeHash) {
        String resultMessage = orderTicketService.checkInTicket(qrCodeHash);

        // 如果回傳文字包含"失敗"，則以 Bad Request (400) 響應，否則 OK (200)
        if (resultMessage.contains("失敗")) {
            return ResponseEntity.badRequest().body(Map.of("message", resultMessage));
        }
        return ResponseEntity.ok(Map.of("message", resultMessage));
    }

    /**
     * 窗口 3：售後退票。
     *
     * <p>
     * API: POST /api/{tOrderId}/refund/{tDetailId}
     * 根據路徑上的訂單編號與門票明細編號辦理退票手續。
     * </p>
     *
     * @param tOrderId  訂單主檔編號
     * @param tDetailId 門票明細編號
     * @return 退票狀態響應體，失敗返回 400，成功返回 200
     */
    @PostMapping("/{tOrderId}/refund/{tDetailId}")
    public ResponseEntity<?> refundTicket(
            @PathVariable String tOrderId,
            @PathVariable String tDetailId) {

        String resultMessage = orderTicketService.refundTicket(tOrderId, tDetailId);

        if (resultMessage.contains("失敗")) {
            return ResponseEntity.badRequest().body(Map.of("message", resultMessage));
        }
        return ResponseEntity.ok(Map.of("message", resultMessage));
    }

    /**
     * 窗口 4：商戶後台營收統計。
     *
     * <p>
     * API: GET /api/orders/revenue/ticket
     * 用於向商家展示當前全站（排除退票後）累計售票總營業額。
     * </p>
     *
     * @return 營業額 JSON 響應體
     */
    @GetMapping("/revenue/ticket")
    public ResponseEntity<?> getRevenue() {
        BigDecimal totalRevenue = orderTicketService.calculateTotalRevenue();
        // 將統計出的金額包裝成 JSON 送給前端
        return ResponseEntity.ok(Map.of("totalRevenue", totalRevenue));
    }

    /**
     * 窗口 5：獲取所有門票訂單與明細。
     * 
     * <p>
     * API: GET /api/tickets
     * </p>
     *
     * @return 所有嵌套結構的訂單清單
     */
    @GetMapping("/tickets")
    public ResponseEntity<?> getAllTickets() {
        return ResponseEntity.ok(orderTicketService.getAllTickets());
    }

    /**
     * 窗口 5.5：獲取特定門票訂單與明細。
     * 
     * <p>
     * API: GET /api/tickets/{tOrderId}
     * </p>
     *
     * @param tOrderId 訂單編號
     * @return 特定訂單詳情或 404
     */
    @GetMapping("/tickets/{tOrderId}")
    public ResponseEntity<?> getTicketOrderById(@PathVariable String tOrderId) {
        OrderResponseDTO order = orderTicketService.getTicketOrderById(tOrderId);
        if (order == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(order);
    }

    /**
     * 窗口 7：接收用戶付款完成後從綠界重定向回商店的瀏覽器跳轉。
     *
     * @param params 綠界以 POST/GET 傳回的支付狀態摘要
     * @return 重定向跳轉回前端成功頁面 RedirectView
     */
    @RequestMapping(value = "/checkout/ecpay-redirect", method = { RequestMethod.POST, RequestMethod.GET })
    public org.springframework.web.servlet.view.RedirectView ecpayRedirect(@RequestParam Map<String, String> params) {
        String merchantTradeNo = params.get("MerchantTradeNo");
        String rtnCode = params.get("RtnCode");

        if (merchantTradeNo != null && merchantTradeNo.startsWith("SAP")) {
            String organizerId = subscriptionService.getOrganizerIdByTradeNo(merchantTradeNo);
            String status = "1".equals(rtnCode) ? "success" : "failed";
            log.info("【綠界重定向】年費方案付款結果={}，導回組織 {} 訂閱頁", status, organizerId);
            return new org.springframework.web.servlet.view.RedirectView(
                    frontendUrl + "/org/" + organizerId + "/settings/subscription?status=" + status);
        }

        if (merchantTradeNo != null && merchantTradeNo.startsWith("MAP")) {
            String mOrderId = merchOrderService.getMOrderIdByMerchantTradeNo(merchantTradeNo);

            if ("1".equals(rtnCode)) {
                log.info("【綠界重定向】商品付款完成，重定向用戶回前端成功頁面。訂單編號: {}, 綠界交易號: {}", mOrderId, merchantTradeNo);
                return new org.springframework.web.servlet.view.RedirectView(
                        frontendUrl + "/paymentSuccess?mOrderId=" + mOrderId + "&type=merch");
            } else {
                log.info("【綠界重定向】商品付款失敗，重定向用戶回前端失敗頁面。訂單編號: {}, RtnCode: {}", mOrderId, rtnCode);
                return new org.springframework.web.servlet.view.RedirectView(
                        frontendUrl + "/paymentMerch?status=failed&orderId=" + mOrderId);
            }
        } else {
            String tOrderId = orderTicketService.getTOrderIdByMerchantTradeNo(merchantTradeNo);
            if ("1".equals(rtnCode)) {
                log.info("【綠界重定向】票券付款完成，重定向用戶回前端成功頁面。訂單編號: {}, 綠界交易號: {}", tOrderId, merchantTradeNo);
                return new org.springframework.web.servlet.view.RedirectView(
                        frontendUrl + "/paymentSuccess?tOrderId=" + tOrderId + "&type=ticket");
            } else {
                log.info("【綠界重定向】票券付款失敗，重定向用戶回前端失敗頁面。訂單編號: {}, RtnCode: {}", tOrderId, rtnCode);
                return new org.springframework.web.servlet.view.RedirectView(
                        frontendUrl + "/paymentTicket?status=failed&orderId=" + tOrderId);
            }
        }
    }

    /**
     * 接收用戶在綠界支付頁面點擊取消或返回商店的瀏覽器跳轉。
     */
    @GetMapping("/checkout/ecpay-cancel")
    public org.springframework.web.servlet.view.RedirectView ecpayCancel(
            @RequestParam(required = false) String tOrderId,
            @RequestParam(required = false) String mOrderId) {
        if (mOrderId != null) {
            log.info("【綠界取消】用戶點選返回商店，取消商品訂單: {}", mOrderId);
            merchOrderService.handleCancelOrder(mOrderId);
            return new org.springframework.web.servlet.view.RedirectView(
                    frontendUrl + "/paymentMerch?status=canceled&orderId=" + mOrderId);
        }
        log.info("【綠界取消】用戶點選返回商店，取消票券訂單: {}", tOrderId);
        orderTicketService.handleCancelOrder(tOrderId);
        return new org.springframework.web.servlet.view.RedirectView(
                frontendUrl + "/paymentTicket?status=canceled&orderId=" + tOrderId);
    }
}
