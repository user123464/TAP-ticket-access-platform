package tw.com.ispan.backend.orderTicket.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderDetailRepository;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;
import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.theme.entity.Session;
import tw.com.ispan.backend.theme.entity.Theme;

/**
 * 系統管理後台「唯讀訂單查詢」業務邏輯服務。
 *
 * <p>專門提供系統管理員檢視全平台門票訂單與其明細的唯讀功能。
 * 由於後端資料庫的訂單主檔無直接的狀態碼欄位，這裡會依照該筆訂單所屬明細的退票狀況，
 * 在 Service 層進行動態彙總並推導出前端對應的狀態代碼（例如：全部退票為已退款，否則視為已完成）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderService {

    private final TicketOrderRepository orderRepository;
    private final TicketOrderDetailRepository detailRepository;

    /**
     * 依據明細的退票情形推導前端狀態碼。
     * 
     * <p>狀態定義：0=待付款, 1=已付款, 2=已完成, 3=已取消, 4=已退款。
     * 本系統明細若全部被標記為 REFUNDED 則推導為 4 (已退款)，否則為 2 (已完成)。</p>
     * 
     * @param details 訂單明細列表
     * @return 狀態代碼數字
     */
    private int deriveStatus(List<TicketOrderDetailBean> details) {
        if (details == null || details.isEmpty()) {
            return 2;
        }
        // 檢查是否全部明細的狀態都是已退票 (REFUNDED)
        boolean allRefunded = details.stream()
                .allMatch(d -> d.getItemStatus() == TicketOrderStatus.REFUNDED);
        return allRefunded ? 4 : 2;
    }

    /**
     * 從訂單明細往上回溯尋找關聯的「活動主題標題」、「主辦單位名稱」及「活動 ID」。
     * 
     * <p>關聯路徑：明細 (d) → 票券 (t) → 場次 (s) → 主題 (th) → 組織 (o)</p>
     *
     * @param details 訂單明細列表
     * @return 包含三個元素之陣列 [活動主題名稱, 主辦單位名稱, 活動ID字串]
     */
    private String[] eventInfo(List<TicketOrderDetailBean> details) {
        for (TicketOrderDetailBean d : details) {
            Ticket tk = d.getTicketTicket();
            if (tk == null) {
                continue;
            }
            Session s = tk.getSession();
            if (s == null) {
                continue;
            }
            Theme th = s.getTheme();
            if (th == null) {
                continue;
            }
            String title = th.getTitle();
            String orgName = th.getOrganizer() != null ? th.getOrganizer().getName() : "—";
            String eventId = th.getThemeId() != null ? String.valueOf(th.getThemeId()) : null;
            return new String[] { title, orgName, eventId };
        }
        return new String[] { "—", "—", null };
    }

    /**
     * 獲取全平台訂單清單 (供後台列表展示)。
     * 
     * @return 訂單 Map 列表
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrders() {
        log.info("【管理員後台】執行 listOrders 查詢全站訂單列表...");
        List<Map<String, Object>> result = new ArrayList<>();
        
        // 1. 獲取所有訂單主檔
        for (TicketOrdersBean o : orderRepository.findAll()) {
            // 2. 查詢每筆訂單的所有明細
            List<TicketOrderDetailBean> details = detailRepository.findByTicketOrder_tOrderId(o.getTOrderId());
            String[] ev = eventInfo(details);
            
            // 3. 以 LinkedHashMap 保持 JSON 欄位之固定排序
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", o.getTOrderId());
            m.put("buyerName", o.getContactName());
            m.put("buyerEmail", o.getContactEmail());
            m.put("eventTitle", ev[0]);
            m.put("amount", o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
            m.put("qty", details.size());
            m.put("status", deriveStatus(details));
            m.put("createdAt", o.getCreateAt());
            result.add(m);
        }
        return result;
    }

    /**
     * 獲取單筆訂單詳情 (包括買家資料、所有明細、以及簡易事件時間軸)。
     *
     * @param orderId 訂單編號
     * @return 訂單詳細資訊 Map
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrderDetail(String orderId) {
        log.info("【管理員後台】執行 getOrderDetail 查詢單筆訂單 - id={}", orderId);
        
        // 1. 獲取訂單主檔，找不到則拋出異常
        TicketOrdersBean o = orderRepository.findById(orderId)
                .orElseThrow(() -> new IllegalArgumentException("找不到訂單: " + orderId));
                
        // 2. 獲取對應的明細列表
        List<TicketOrderDetailBean> details = detailRepository.findByTicketOrder_tOrderId(orderId);
        String[] ev = eventInfo(details);

        // 3. 處理每張購票座位與票種詳情
        List<Map<String, Object>> tickets = new ArrayList<>();
        for (TicketOrderDetailBean d : details) {
            Ticket tk = d.getTicketTicket();
            String typeName = tk != null && tk.getTicketType() != null ? tk.getTicketType().getName() : "—";
            String seat = "—";
            if (tk != null && tk.getSeat() != null) {
                var st = tk.getSeat();
                if (st.getRowNum() != null && st.getSeatNum() != null) {
                    seat = st.getRowNum() + " 排 " + st.getSeatNum() + " 號";
                } else {
                    seat = "#" + st.getSeatId();
                }
            }
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("ticketNo", d.getTDetailId());
            tm.put("typeName", typeName);
            tm.put("seat", seat);
            tm.put("price", d.getUnitPrice() != null ? d.getUnitPrice() : BigDecimal.ZERO);
            tickets.add(tm);
        }

        // 4. 金流時間軸：因為資料庫沒有特別開狀態流向紀錄表，此處返回訂單創立時間的預設事件節點
        List<Map<String, Object>> timeline = new ArrayList<>();
        Map<String, Object> created = new LinkedHashMap<>();
        created.put("time", o.getCreateAt());
        created.put("event", "建立訂單");
        timeline.add(created);

        // 5. 彙整所有欄位回傳
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", o.getTOrderId());
        m.put("buyerName", o.getContactName());
        m.put("buyerEmail", o.getContactEmail());
        m.put("buyerPhone", o.getContactPhone());
        m.put("eventId", ev[2]);
        m.put("eventTitle", ev[0]);
        m.put("orgName", ev[1]);
        m.put("status", deriveStatus(details));
        m.put("amount", o.getTotalAmount() != null ? o.getTotalAmount() : BigDecimal.ZERO);
        m.put("paymentMethod", null); // 實體資料庫中無此付款方式細部記錄欄位
        m.put("createdAt", o.getCreateAt());
        m.put("tickets", tickets);
        m.put("timeline", timeline);
        return m;
    }
}
