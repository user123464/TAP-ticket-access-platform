package tw.com.ispan.backend.refund.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.refund.dto.RefundRequestDTO;
import tw.com.ispan.backend.refund.enums.RefundRequestType;
import tw.com.ispan.backend.refund.service.RefundRequestService;

/**
 * 會員退款申請控制器 (B2C 前台自助端點)。
 *
 * <p>提供登入會員對自己的票務/商城訂單明細送出退款申請 (附申退理由)，
 * 以及查詢自己的申請紀錄 (供 /member/orders 頁面標示「審核中」「已駁回」)。
 * 比照 MemberOrderController：僅需登入，範圍固定收斂在呼叫者自己的 userId。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/member/refund-requests")
@RequiredArgsConstructor
public class MemberRefundRequestController {

    private final RefundRequestService refundRequestService;

    /** 建立退款申請的請求體。 */
    @Data
    public static class CreateRefundRequestBody {
        private String type;     // TICKET / MERCH
        private String orderId;
        private String detailId;
        private String reason;
    }

    /**
     * 會員送出退款申請 (退票/退貨)。
     */
    @PostMapping
    public ResponseEntity<Response> createRefundRequest(
            @RequestBody CreateRefundRequestBody body,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Response.error("認證失敗，請重新登入"));
        }

        try {
            RefundRequestType type = RefundRequestType.valueOf(body.getType());
            refundRequestService.create(userDetails.getUserId(), type, body.getOrderId(), body.getDetailId(),
                    body.getReason());
            return ResponseEntity.ok(Response.success("已送出退款申請，請等候主辦方審核", null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Response.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("建立退款申請時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 查詢登入會員自己的退款申請紀錄 (新到舊)。
     */
    @GetMapping
    public ResponseEntity<Response> getMyRefundRequests(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Response.error("認證失敗，請重新登入"));
        }

        try {
            List<RefundRequestDTO> result = refundRequestService.getRequestsByUser(userDetails.getUserId());
            return ResponseEntity.ok(Response.success("查詢成功", result));
        } catch (Exception ex) {
            log.error("查詢會員退款申請時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }
}
