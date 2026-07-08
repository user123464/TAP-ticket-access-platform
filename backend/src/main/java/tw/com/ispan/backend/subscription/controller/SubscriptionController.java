package tw.com.ispan.backend.subscription.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.subscription.dto.MembershipPlanDto;
import tw.com.ispan.backend.subscription.dto.OrganizerSubscriptionDto;
import tw.com.ispan.backend.subscription.enums.SubscriptionUpgradeType;
import tw.com.ispan.backend.subscription.service.SubscriptionService;

@RestController
@RequestMapping("/api/saas")
@RequiredArgsConstructor
public class SubscriptionController {

    private final SubscriptionService subscriptionService;

    /**
     * 獲取所有有效方案清單
     */
    @GetMapping("/plans")
    public ResponseEntity<List<MembershipPlanDto>> getPlans() {
        return ResponseEntity.ok(subscriptionService.getAllPlans());
    }

    /**
     * 查詢主辦方當前生效的訂閱
     */
    @GetMapping("/subscription/active")
    public ResponseEntity<OrganizerSubscriptionDto> getActiveSubscription(
            @RequestParam("organizerId") String organizerId) {
        return ResponseEntity.ok(subscriptionService.getActiveSubscription(organizerId));
    }

    /**
     * 主辦方手動變更/升級訂閱
     */
    @PostMapping("/subscription/subscribe")
    public ResponseEntity<OrganizerSubscriptionDto> subscribe(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody SubscribeRequest request) {
        
        SubscriptionUpgradeType upgradeEnum;
        try {
            upgradeEnum = SubscriptionUpgradeType.valueOf(request.getUpgradeType().toUpperCase());
        } catch (Exception e) {
            throw new IllegalArgumentException("無效的升級類型，必須是 ANNUAL_FEE、CUMULATIVE 或 MANUAL");
        }

        OrganizerSubscriptionDto dto = subscriptionService.subscribePlan(
                userDetails.getUserId(),
                request.getOrganizerId(),
                request.getPlanId(),
                upgradeEnum
        );
        return ResponseEntity.ok(dto);
    }

    /**
     * 年費方案付款：建立綠界付款交易，回傳自動提交的 HTML 表單供前端跳轉。
     * 實際開通在綠界付款成功的 S2S callback 後才發生。
     */
    @PostMapping("/subscription/checkout")
    public ResponseEntity<Map<String, String>> checkout(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @RequestBody CheckoutRequest request) {

        String ecpayForm = subscriptionService.createSubscriptionCheckout(
                userDetails.getUserId(),
                request.getOrganizerId(),
                request.getPlanId()
        );
        return ResponseEntity.ok(Map.of("ecpayForm", ecpayForm));
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SubscribeRequest {
        private String organizerId;
        private String planId;
        private String upgradeType;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CheckoutRequest {
        private String organizerId;
        private String planId;
    }
}
