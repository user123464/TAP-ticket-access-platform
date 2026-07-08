package tw.com.ispan.backend.orderTicket.service;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.dto.OrderResponseDTO;
import tw.com.ispan.backend.orderTicket.dto.OrderResponseDTO.OrderDetailResponseDTO;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;
import tw.com.ispan.backend.ticket.entity.Ticket;

/**
 * 訂單查詢與 DTO 轉換服務。
 * 
 * <p>提供前台/後台獲取門票訂單與明細的業務邏輯，並負責將實體 Bean 映射轉換為 DTO。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketOrderQueryService {

    // 注入訂單主檔 Repository
    private final TicketOrderRepository ticketOrderRepository;

    /**
     * 以 JPA 從資料庫抓取所有的門票訂單與明細，並以巢狀結構 (OrderResponseDTO) 回傳。
     *
     * @return 門票訂單與明細的 DTO 列表
     */
    @Transactional
    public List<OrderResponseDTO> getAllTickets() {
        log.info("【查詢訂單】開始獲取全站門票訂單...");
        
        // 1. 從資料庫查詢所有訂單主檔
        List<TicketOrdersBean> orders = ticketOrderRepository.findAll();
        List<OrderResponseDTO> result = new ArrayList<>();
        
        // 2. 逐筆將訂單主檔與其對應的明細轉換為前端所需的 DTO 結構
        for (TicketOrdersBean order : orders) {
            result.add(convertToResponseDTO(order, order.getOrderDetail()));
        }
        
        log.info("【查詢訂單】成功獲取 {} 筆訂單資料", result.size());
        return result;
    }

    /**
     * 根據訂單編號，查詢單筆票券訂單詳情 (轉換為 OrderResponseDTO)。
     *
     * @param tOrderId 訂單編號
     * @return 轉換後的門票訂單 DTO，若不存在則為 null
     */
    @Transactional
    public OrderResponseDTO getTicketOrderById(String tOrderId) {
        log.info("【查詢單筆訂單】開始獲取門票訂單... id={}", tOrderId);
        return ticketOrderRepository.findById(tOrderId)
                .map(order -> convertToResponseDTO(order, order.getOrderDetail()))
                .orElse(null);
    }

    /**
     * 內部與外部輔助工具：將資料庫 Bean 屬性手動映射倒進 DTO 物件中。
     *
     * <p>在有 @Transactional 事務的環境中調用此方法，可以安全地讀取 Lazy 關聯資料，
     * 避免因 Session 關閉而拋出 LazyInitializationException，同時對關聯進行扁平化處理，避免循環參照問題。</p>
     *
     * @param order   訂單主檔實體
     * @param details 訂單明細實體列表
     * @return 轉換後的門票訂單 DTO
     */
    public OrderResponseDTO convertToResponseDTO(TicketOrdersBean order, List<TicketOrderDetailBean> details) {
        OrderResponseDTO dto = new OrderResponseDTO();
        
        // 映射訂單主檔基本屬性
        dto.setTOrderId(order.getTOrderId());
        dto.setTotalAmount(order.getTotalAmount());
        dto.setContactName(order.getContactName());
        dto.setCreateAt(order.getCreateAt());
        dto.setPaymentStatus(order.getPaymentStatus());

        List<OrderDetailResponseDTO> detailDTOs = new ArrayList<>();
        if (details != null) {
            for (TicketOrderDetailBean b : details) {
                OrderDetailResponseDTO dDto = new OrderDetailResponseDTO();
                
                // 映射訂單明細基本屬性
                dDto.setTDetailId(b.getTDetailId());
                dDto.setUnitPrice(b.getUnitPrice());
                dDto.setRealName(b.getRealName());
                if (b.getItemStatus() == TicketOrderStatus.CANCELLED) {
                    dDto.setIsUsed(TicketOrderUse.UNESTABLISHED);
                } else {
                    dDto.setIsUsed(b.getIsUsed());
                }
                dDto.setItemStatus(b.getItemStatus());
                dDto.setQrCodeHash(b.getQrCodeHash());
                dDto.setUsedAt(b.getUsedAt());

                // 安全地讀取延遲加載 (Lazy) 的 Ticket 關聯，並提取前端所需要的扁平化欄位
                if (b.getTicketTicket() != null) {
                    Ticket t = b.getTicketTicket();
                    
                    // 1. 提取座位排數與號數
                    if (t.getSeat() != null) {
                        dDto.setSeatInfo(t.getSeat().getRowNum() + "排" + t.getSeat().getSeatNum() + "號");
                    }
                    
                    // 2. 提取票種 ID 與名稱
                    if (t.getTicketType() != null) {
                        dDto.setTicketTypeId(t.getTicketType().getTicketTypeId());
                        dDto.setTicketTypeName(t.getTicketType().getName());
                    }
                    
                    // 3. 提取場次 ID 與活動主辦方 ID 與活動主題標題
                    if (t.getSession() != null) {
                        dDto.setSessionId(t.getSession().getSessionId());
                        if (t.getSession().getTheme() != null) {
                            dDto.setActivityTitle(t.getSession().getTheme().getTitle());
                            if (t.getSession().getTheme().getOrganizer() != null) {
                                dDto.setOrganizerId(t.getSession().getTheme().getOrganizer().getOrganizerId());
                            }
                        }
                    }
                }
                detailDTOs.add(dDto);
            }
        }
        dto.setOrderDetailTickets(detailDTOs);
        return dto;
    }
}
