package tw.com.ispan.backend.refund.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.refund.dto.RefundRequestDTO;
import tw.com.ispan.backend.refund.service.RefundRequestService;

/**
 * 主辦方退款申請審核控制器 (B2B 後台端點)。
 *
 * <p>提供主辦方查詢名下的退票/退貨申請清單，並執行核准 (觸發既有退款服務)
 * 或駁回 (附駁回原因)。organizerId 由路徑帶入，核准/駁回時會比對申請單的
 * organizerId 防止跨組織操作。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/org/{organizerId}/refund-requests")
@RequiredArgsConstructor
public class OrgRefundRequestController {

    private final RefundRequestService refundRequestService;

    /** 駁回申請的請求體 (駁回原因選填)。 */
    @Data
    public static class RejectBody {
        private String note;
    }

    /**
     * 查詢該主辦方的退款申請清單 (新到舊，含購買人聯絡資訊/品項/金額/購買時間)。
     */
    @GetMapping
    public ResponseEntity<Response> getRefundRequests(@PathVariable String organizerId) {
        try {
            List<RefundRequestDTO> result = refundRequestService.getRequestsByOrganizer(organizerId);
            return ResponseEntity.ok(Response.success("查詢成功", result));
        } catch (Exception ex) {
            log.error("查詢主辦方退款申請時發生系統錯誤 - organizerId={}", organizerId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 核准退款申請：委派既有退款服務執行實際退款，成功才標記 APPROVED。
     */
    @PostMapping("/{requestId}/approve")
    public ResponseEntity<Response> approve(
            @PathVariable String organizerId,
            @PathVariable Integer requestId) {
        try {
            String message = refundRequestService.approve(requestId, organizerId);
            return ResponseEntity.ok(Response.success(message, null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Response.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("核准退款申請時發生系統錯誤 - requestId={}", requestId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 駁回退款申請 (可附駁回原因)。被駁回後會員可再次申請。
     */
    @PostMapping("/{requestId}/reject")
    public ResponseEntity<Response> reject(
            @PathVariable String organizerId,
            @PathVariable Integer requestId,
            @RequestBody(required = false) RejectBody body) {
        try {
            String message = refundRequestService.reject(requestId, organizerId,
                    body != null ? body.getNote() : null);
            return ResponseEntity.ok(Response.success(message, null));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Response.error(ex.getMessage()));
        } catch (Exception ex) {
            log.error("駁回退款申請時發生系統錯誤 - requestId={}", requestId, ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }
}
