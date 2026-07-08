package tw.com.ispan.backend.orderTicket.controller;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.orderMerch.dto.MemberMerchOrderDTO;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;
import tw.com.ispan.backend.orderTicket.dto.MemberTicketOrderDTO;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;
import tw.com.ispan.backend.orderTicket.service.OrderTicketService;
import tw.com.ispan.backend.orderMerch.service.MerchOrderQueryService;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * 會員「我的訂單」查詢控制器 (B2C 前台自助端點)。
 *
 * <p>
 * 提供登入會員查詢自己名下的票務訂單與商城訂單清單，供 /member/orders 頁面使用。
 * 僅需登入即可存取（比照 UserProfileController 的自助端點慣例），
 * 不額外綁定管理權限；查詢範圍固定收斂在呼叫者自己的 userId，不接受外部指定他人 ID。
 * </p>
 */
@Slf4j
@RestController
@RequestMapping("/api/member")
@RequiredArgsConstructor
public class MemberOrderController {

    private final TicketOrderRepository ticketOrderRepository;
    private final MerchOrderRepository merchOrderRepository;
    private final OrderTicketService orderTicketService;
    private final MerchOrderQueryService merchOrderQueryService;

    /**
     * 查詢登入會員的票務訂單清單 (新到舊)。
     */
    @GetMapping("/ticket-orders")
    public ResponseEntity<Response> getMyTicketOrders(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("未授權的會員票務訂單查詢請求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Response.error("認證失敗，請重新登入"));
        }

        try {
            List<TicketOrdersBean> orders = ticketOrderRepository.findByMemberUserId(userDetails.getUserId());
            List<MemberTicketOrderDTO> result = orders.stream()
                    .map(this::toMemberTicketOrderDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Response.success("查詢成功", result));
        } catch (Exception ex) {
            log.error("查詢會員票務訂單時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 查詢登入會員的商城訂單清單 (新到舊)。
     */
    @GetMapping("/merch-orders")
    public ResponseEntity<Response> getMyMerchOrders(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("未授權的會員商城訂單查詢請求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Response.error("認證失敗，請重新登入"));
        }

        try {
            List<MerchOrderBean> orders = merchOrderRepository.findByMemberUserId(userDetails.getUserId());
            List<MemberMerchOrderDTO> result = orders.stream()
                    .map(this::toMemberMerchOrderDTO)
                    .collect(Collectors.toList());
            return ResponseEntity.ok(Response.success("查詢成功", result));
        } catch (Exception ex) {
            log.error("查詢會員商城訂單時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 查詢登入會員的特定票務訂單明細。
     */
    @GetMapping("/ticket-orders/{tOrderId}")
    public ResponseEntity<Response> getMyTicketOrderDetail(
            @PathVariable String tOrderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("未授權的會員票務訂單明細查詢請求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Response.error("認證失敗，請重新登入"));
        }

        try {
            TicketOrdersBean order = ticketOrderRepository.findById(tOrderId).orElse(null);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.error("找不到該訂單"));
            }

            // 安全性檢查：確認訂單屬於當前登入會員
            if (order.getUserId() == null || !order.getUserId().getUserId().equals(userDetails.getUserId())) {
                log.warn("會員 {} 嘗試越權查詢訂單 {}", userDetails.getUserId(), tOrderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Response.error("您無權查看此訂單"));
            }

            tw.com.ispan.backend.orderTicket.dto.OrderResponseDTO result = orderTicketService.getTicketOrderById(tOrderId);
            return ResponseEntity.ok(Response.success("查詢成功", result));
        } catch (Exception ex) {
            log.error("查詢會員票務訂單明細時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }

    /**
     * 查詢登入會員的特定商城訂單明細。
     */
    @GetMapping("/merch-orders/{mOrderId}")
    public ResponseEntity<Response> getMyMerchOrderDetail(
            @PathVariable String mOrderId,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("未授權的會員商城訂單明細查詢請求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Response.error("認證失敗，請重新登入"));
        }

        try {
            MerchOrderBean order = merchOrderRepository.findById(mOrderId).orElse(null);
            if (order == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Response.error("找不到該訂單"));
            }

            // 安全性檢查：確認訂單屬於當前登入會員
            if (order.getUserId() == null || !order.getUserId().getUserId().equals(userDetails.getUserId())) {
                log.warn("會員 {} 嘗試越權查詢訂單 {}", userDetails.getUserId(), mOrderId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Response.error("您無權查看此訂單"));
            }

            tw.com.ispan.backend.orderMerch.dto.OrderResponseDTO result = merchOrderQueryService.getMerchOrderById(mOrderId);
            return ResponseEntity.ok(Response.success("查詢成功", result));
        } catch (Exception ex) {
            log.error("查詢會員商城訂單明細時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Response.error("系統錯誤，請稍後再試"));
        }
    }

    // ── 私有轉換方法：扁平化實體為列表用 DTO ──

    private MemberTicketOrderDTO toMemberTicketOrderDTO(TicketOrdersBean order) {
        String activityTitle = null;
        String sessionTitle = null;
        int ticketCount = 0;

        if (order.getOrderDetail() != null) {
            ticketCount = order.getOrderDetail().size();
            for (TicketOrderDetailBean detail : order.getOrderDetail()) {
                if (detail.getTicketTicket() != null && detail.getTicketTicket().getSession() != null) {
                    sessionTitle = detail.getTicketTicket().getSession().getTitle();
                    if (detail.getTicketTicket().getSession().getTheme() != null) {
                        activityTitle = detail.getTicketTicket().getSession().getTheme().getTitle();
                    }
                    break;
                }
            }
        }

        return new MemberTicketOrderDTO(
                order.getTOrderId(),
                activityTitle,
                sessionTitle,
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getCreateAt(),
                ticketCount);
    }

    private MemberMerchOrderDTO toMemberMerchOrderDTO(MerchOrderBean order) {
        String itemSummary = null;
        int totalQuantity = 0;

        if (order.getMerchOrderDetails() != null && !order.getMerchOrderDetails().isEmpty()) {
            List<MerchOrderDetailBean> details = order.getMerchOrderDetails();

            for (MerchOrderDetailBean detail : details) {
                int qty = detail.getQuantity() != null ? detail.getQuantity() : 0;
                totalQuantity += qty;
            }

            StringBuilder sb = new StringBuilder();
            int shown = 0;
            for (MerchOrderDetailBean detail : details) {
                if (shown >= 2) {
                    sb.append(" 等");
                    break;
                }
                String name = detail.getProductProduct() != null
                        ? detail.getProductProduct().getProductName()
                        : "商品";
                if (shown > 0) {
                    sb.append("、");
                }
                sb.append(name).append(" x").append(detail.getQuantity());
                shown++;
            }
            itemSummary = sb.toString();
        }

        return new MemberMerchOrderDTO(
                order.getMOrderId(),
                itemSummary,
                totalQuantity,
                order.getTotalAmount(),
                order.getPaymentStatus(),
                order.getCreatedAt());
    }
}
