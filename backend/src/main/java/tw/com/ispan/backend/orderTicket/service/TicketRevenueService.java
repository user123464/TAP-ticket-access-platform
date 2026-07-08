package tw.com.ispan.backend.orderTicket.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderDetailRepository;

/**
 * 票務營收會計服務。
 * 
 * <p>負責全站票務財務統計，提供計算累計總營業額（排除退票門票金額）的邏輯。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketRevenueService {

    private final TicketOrderDetailRepository ticketOrderDetailRepository;

    /**
     * 基本帳務統計 - 計算全站票務總營業額 (排除退票)。
     *
     * @return 總營業額金額
     */
    @Transactional
    public BigDecimal calculateTotalRevenue() {
        log.info("【財務統計】開始計算全站票務總營業額...");

        // 1. 從資料庫撈出所有狀態為「正常 (NORMAL)」的門票明細
        List<TicketOrderDetailBean> liveTicket = ticketOrderDetailRepository
                .findByItemStatus(TicketOrderStatus.NORMAL);
        
        BigDecimal totalRevenue = BigDecimal.ZERO;

        // 2. 累加每張有效票的成交單價
        for (TicketOrderDetailBean ticket : liveTicket) {
            if (ticket.getUnitPrice() != null) {
                totalRevenue = totalRevenue.add(ticket.getUnitPrice());
            }
        }

        log.info("【財務統計】計算完成 - 有效票數={}, 總營業額={}", liveTicket.size(), totalRevenue);
        return totalRevenue;
    }
}
