package tw.com.ispan.backend.refund.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.entity.MerchOrderDetailBean;
import tw.com.ispan.backend.orderMerch.enums.MerchOrderEnum;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;
import tw.com.ispan.backend.orderMerch.service.MerchOrderService;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;
import tw.com.ispan.backend.orderTicket.service.OrderTicketService;
import tw.com.ispan.backend.refund.dto.RefundRequestDTO;
import tw.com.ispan.backend.refund.entity.RefundRequestBean;
import tw.com.ispan.backend.refund.enums.RefundRequestStatus;
import tw.com.ispan.backend.refund.enums.RefundRequestType;
import tw.com.ispan.backend.refund.repository.RefundRequestRepository;

/**
 * 退款申請審核服務 (退票/退貨審核制)。
 *
 * <p>會員送出申請 (PENDING) → 主辦方於 B2B 後台核准或駁回；
 * 核准時才委派既有的 {@link OrderTicketService#refundTicket} /
 * {@link MerchOrderService#refundMerch} 執行實際退款，退款邏輯完全沿用不重寫。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RefundRequestService {

    private final RefundRequestRepository refundRequestRepository;
    private final TicketOrderRepository ticketOrderRepository;
    private final MerchOrderRepository merchOrderRepository;
    private final OrderTicketService orderTicketService;
    private final MerchOrderService merchOrderService;

    /**
     * 會員建立退款申請。
     *
     * @throws IllegalArgumentException 驗證失敗時 (訊息可直接回給前端)
     */
    @Transactional
    public RefundRequestBean create(String userId, RefundRequestType type, String orderId, String detailId,
            String reason) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("請填寫申退理由");
        }
        // 防重複：同一明細已有待審核申請
        if (refundRequestRepository.existsByDetailIdAndStatus(detailId, RefundRequestStatus.PENDING)) {
            throw new IllegalArgumentException("此項目已有待審核的退款申請，請耐心等候主辦方處理");
        }

        String organizerId;
        if (type == RefundRequestType.TICKET) {
            organizerId = validateTicketAndGetOrganizer(userId, orderId, detailId);
        } else {
            organizerId = validateMerchAndGetOrganizer(userId, orderId, detailId);
        }

        RefundRequestBean request = new RefundRequestBean();
        request.setRequestType(type);
        request.setOrderId(orderId);
        request.setDetailId(detailId);
        request.setUserId(userId);
        request.setOrganizerId(organizerId);
        request.setReason(reason.trim());
        request.setStatus(RefundRequestStatus.PENDING);

        RefundRequestBean saved = refundRequestRepository.save(request);
        log.info("【退款申請】建立成功 - requestId={}, type={}, detailId={}, userId={}", saved.getRequestId(), type,
                detailId, userId);
        return saved;
    }

    /**
     * 主辦方核准申請：委派既有退款服務執行實際退款，成功才標記 APPROVED。
     *
     * @return 處理結果訊息
     */
    @Transactional
    public String approve(Integer requestId, String organizerId) {
        RefundRequestBean request = getPendingRequestForOrganizer(requestId, organizerId);

        // 委派既有退款服務 (內含已核銷/退票期限等防呆，失敗時申請維持 PENDING)
        String result;
        if (request.getRequestType() == RefundRequestType.TICKET) {
            result = orderTicketService.refundTicket(request.getOrderId(), request.getDetailId());
        } else {
            result = merchOrderService.refundMerch(request.getOrderId(), request.getDetailId());
        }

        if (result == null || !result.contains("成功")) {
            log.warn("【退款申請】核准失敗，退款服務拒絕 - requestId={}, result={}", requestId, result);
            throw new IllegalArgumentException(result != null ? result : "退款執行失敗");
        }

        request.setStatus(RefundRequestStatus.APPROVED);
        request.setProcessedAt(LocalDateTime.now());
        log.info("【退款申請】已核准並完成退款 - requestId={}", requestId);
        return "已核准退款申請，" + result;
    }

    /**
     * 主辦方駁回申請。被駁回後會員可再次申請 (防重複只擋 PENDING)。
     */
    @Transactional
    public String reject(Integer requestId, String organizerId, String note) {
        RefundRequestBean request = getPendingRequestForOrganizer(requestId, organizerId);

        request.setStatus(RefundRequestStatus.REJECTED);
        request.setRejectNote(note != null && !note.trim().isEmpty() ? note.trim() : null);
        request.setProcessedAt(LocalDateTime.now());
        log.info("【退款申請】已駁回 - requestId={}, note={}", requestId, note);
        return "已駁回退款申請";
    }

    /**
     * B2B 端：查主辦方的申請清單 (扁平化帶出聯絡資訊/品項/金額/購買時間)。
     */
    @Transactional
    public List<RefundRequestDTO> getRequestsByOrganizer(String organizerId) {
        return refundRequestRepository.findByOrganizerIdOrderByCreatedAtDesc(organizerId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * 會員端：查自己的申請清單。
     */
    @Transactional
    public List<RefundRequestDTO> getRequestsByUser(String userId) {
        return refundRequestRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    // ── 私有輔助 ──

    private RefundRequestBean getPendingRequestForOrganizer(Integer requestId, String organizerId) {
        RefundRequestBean request = refundRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該筆退款申請"));
        if (organizerId != null && !organizerId.equals(request.getOrganizerId())) {
            throw new IllegalArgumentException("您無權處理此退款申請");
        }
        if (request.getStatus() != RefundRequestStatus.PENDING) {
            throw new IllegalArgumentException("此申請已處理過 (目前狀態：" + request.getStatus() + ")");
        }
        return request;
    }

    /** 驗證票務明細屬於該會員且可退，並回推主辦方 ID。 */
    private String validateTicketAndGetOrganizer(String userId, String orderId, String detailId) {
        TicketOrdersBean order = ticketOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該訂單"));
        if (order.getUserId() == null || !order.getUserId().getUserId().equals(userId)) {
            throw new IllegalArgumentException("您無權對此訂單申請退款");
        }

        TicketOrderDetailBean target = null;
        if (order.getOrderDetail() != null) {
            for (TicketOrderDetailBean d : order.getOrderDetail()) {
                if (d.getTDetailId().equals(detailId)) {
                    target = d;
                    break;
                }
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("找不到該筆門票明細");
        }
        if (target.getItemStatus() != TicketOrderStatus.NORMAL) {
            throw new IllegalArgumentException("此門票目前狀態無法申請退票");
        }
        if (target.getIsUsed() == TicketOrderUse.Redeemed) {
            throw new IllegalArgumentException("此門票已核銷進場，無法申請退票");
        }

        if (target.getTicketTicket() != null && target.getTicketTicket().getSession() != null
                && target.getTicketTicket().getSession().getTheme() != null
                && target.getTicketTicket().getSession().getTheme().getOrganizer() != null) {
            return target.getTicketTicket().getSession().getTheme().getOrganizer().getOrganizerId();
        }
        throw new IllegalArgumentException("無法識別此門票的主辦方，請聯繫客服");
    }

    /** 驗證商城明細屬於該會員且可退，並回推主辦方 ID。 */
    private String validateMerchAndGetOrganizer(String userId, String orderId, String detailId) {
        MerchOrderBean order = merchOrderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該訂單"));
        if (order.getUserId() == null || !order.getUserId().getUserId().equals(userId)) {
            throw new IllegalArgumentException("您無權對此訂單申請退款");
        }

        MerchOrderDetailBean target = null;
        if (order.getMerchOrderDetails() != null) {
            for (MerchOrderDetailBean d : order.getMerchOrderDetails()) {
                if (d.getMDetailId().equals(detailId)) {
                    target = d;
                    break;
                }
            }
        }
        if (target == null) {
            throw new IllegalArgumentException("找不到該筆商品明細");
        }
        if (target.getItemStatus() != MerchOrderEnum.NORMAL) {
            throw new IllegalArgumentException("此商品目前狀態無法申請退貨");
        }

        if (target.getProductProduct() != null && target.getProductProduct().getTheme() != null
                && target.getProductProduct().getTheme().getOrganizer() != null) {
            return target.getProductProduct().getTheme().getOrganizer().getOrganizerId();
        }
        throw new IllegalArgumentException("無法識別此商品的主辦方，請聯繫客服");
    }

    /** 扁平化申請單為 DTO，一併帶出訂單聯絡資訊/品項摘要/金額/購買時間。 */
    private RefundRequestDTO toDTO(RefundRequestBean request) {
        RefundRequestDTO.RefundRequestDTOBuilder builder = RefundRequestDTO.builder()
                .requestId(request.getRequestId())
                .requestType(request.getRequestType().name())
                .status(request.getStatus().name())
                .orderId(request.getOrderId())
                .detailId(request.getDetailId())
                .reason(request.getReason())
                .rejectNote(request.getRejectNote())
                .createdAt(request.getCreatedAt())
                .processedAt(request.getProcessedAt());

        try {
            if (request.getRequestType() == RefundRequestType.TICKET) {
                fillTicketInfo(builder, request);
            } else {
                fillMerchInfo(builder, request);
            }
        } catch (Exception ex) {
            // 訂單關聯資料異常時仍回傳申請單本身，不讓整頁掛掉
            log.warn("【退款申請】組裝 DTO 附加資訊失敗 - requestId={}", request.getRequestId(), ex);
        }
        return builder.build();
    }

    private void fillTicketInfo(RefundRequestDTO.RefundRequestDTOBuilder builder, RefundRequestBean request) {
        TicketOrdersBean order = ticketOrderRepository.findById(request.getOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        builder.applicantName(order.getContactName())
                .applicantEmail(order.getContactEmail())
                .applicantPhone(order.getContactPhone())
                .purchasedAt(order.getCreateAt());

        if (order.getOrderDetail() != null) {
            for (TicketOrderDetailBean d : order.getOrderDetail()) {
                if (d.getTDetailId().equals(request.getDetailId())) {
                    builder.amount(d.getUnitPrice());
                    StringBuilder sb = new StringBuilder();
                    if (d.getTicketTicket() != null && d.getTicketTicket().getSession() != null) {
                        if (d.getTicketTicket().getSession().getTheme() != null) {
                            sb.append(d.getTicketTicket().getSession().getTheme().getTitle());
                        }
                        if (d.getTicketTicket().getSession().getTitle() != null) {
                            if (sb.length() > 0) {
                                sb.append(" - ");
                            }
                            sb.append(d.getTicketTicket().getSession().getTitle());
                        }
                    }
                    if (d.getRealName() != null) {
                        if (sb.length() > 0) {
                            sb.append("（入場人：").append(d.getRealName()).append("）");
                        } else {
                            sb.append("入場人：").append(d.getRealName());
                        }
                    }
                    builder.itemSummary(sb.length() > 0 ? sb.toString() : "門票");
                    break;
                }
            }
        }
    }

    private void fillMerchInfo(RefundRequestDTO.RefundRequestDTOBuilder builder, RefundRequestBean request) {
        MerchOrderBean order = merchOrderRepository.findById(request.getOrderId()).orElse(null);
        if (order == null) {
            return;
        }
        builder.applicantName(order.getRecipientName())
                .applicantEmail(order.getRecipientEmail())
                .applicantPhone(order.getRecipientPhone())
                .purchasedAt(order.getCreatedAt());

        if (order.getMerchOrderDetails() != null) {
            for (MerchOrderDetailBean d : order.getMerchOrderDetails()) {
                if (d.getMDetailId().equals(request.getDetailId())) {
                    String name = d.getProductProduct() != null ? d.getProductProduct().getProductName() : "商品";
                    builder.itemSummary(name + " x" + d.getQuantity());
                    if (d.getUnitPrice() != null && d.getQuantity() != null) {
                        builder.amount(d.getUnitPrice().multiply(BigDecimal.valueOf(d.getQuantity())));
                    }
                    break;
                }
            }
        }
    }
}
