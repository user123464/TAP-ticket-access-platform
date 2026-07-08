package tw.com.ispan.backend.theme.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.theme.entity.Session;
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.ThemeRepository;
import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.ticket.entity.TicketStatus;
import tw.com.ispan.backend.ticket.entity.TicketType;
import tw.com.ispan.backend.ticket.repository.TicketRepository;
import tw.com.ispan.backend.ticket.repository.TicketTypeRepository;

/**
 * Admin 後台「活動查詢（唯讀）」業務邏輯（批次 3）。
 *
 * <p>重用 theme package 的 Theme / Session / TicketType / Ticket，
 * 將後端 {@link Status}（DRAFT/ACTIVE/ARCHIVED/DELETED 字串枚舉）轉換為
 * 前端 EventList/EventDetail 期望的整數狀態碼：
 * 0=草稿 1=審核中 2=售票中 3=已結束 4=已取消。</p>
 *
 * <p>後端枚舉沒有「審核中(1)」這一狀態，故僅產生 0/2/3/4；前端篩選 1 時自然無資料。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminEventService {

    private final ThemeRepository themeRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final TicketRepository ticketRepository;

    /** Theme.Status → 前端整數狀態碼。 */
    private int mapStatus(Status status) {
        if (status == null) {
            return 0;
        }
        return switch (status) {
            case DRAFT -> 0;
            case ACTIVE -> 2;     // 公開 → 售票中
            case ARCHIVED -> 3;   // 已結束
            case DELETED -> 4;    // 已刪除 → 已取消
        };
    }

    /** 取活動最早場次開始時間。 */
    private LocalDateTime earliestStart(Theme t) {
        return t.getSessions().stream()
                .map(s -> s.getStartTime())
                .filter(s -> s != null)
                .min((a, b) -> a.compareTo(b))
                .orElse(null);
    }

    /** 取活動的票種清單（未軟刪除）。 */
    private List<TicketType> ticketTypesOf(Theme t) {
        return ticketTypeRepository.findAll().stream()
                .filter(tt -> Boolean.FALSE.equals(tt.getIsDeleted()))
                .filter(tt -> tt.getTheme() != null && tt.getTheme().getThemeId().equals(t.getThemeId()))
                .toList();
    }

    /** 計算某活動的總票量與已售量（依 Ticket 庫存狀態）。 */
    private int[] computeTicketProgress(Theme t) {
        int total = 0;
        int sold = 0;
        for (Session s : t.getSessions()) {
            List<Ticket> tickets = ticketRepository.findBySession_SessionId(s.getSessionId());
            for (Ticket tk : tickets) {
                total++;
                if (tk.getStatus() == TicketStatus.SOLD) {
                    sold++;
                }
            }
        }
        return new int[] { sold, total };
    }

    /** 全平台活動清單。 */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listEvents() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Theme t : themeRepository.findAll()) {
            int[] progress = computeTicketProgress(t);
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", t.getThemeId());
            m.put("title", t.getTitle());
            m.put("orgId", t.getOrganizer() != null ? t.getOrganizer().getOrganizerId() : null);
            m.put("orgName", t.getOrganizer() != null ? t.getOrganizer().getName() : "—");
            m.put("status", mapStatus(t.getStatus()));
            m.put("ticketSold", progress[0]);
            m.put("ticketTotal", progress[1]);
            m.put("startAt", earliestStart(t));
            m.put("createdAt", t.getCreatedAt());
            result.add(m);
        }
        return result;
    }

    /** 活動詳情 + 票種售況。 */
    @Transactional(readOnly = true)
    public Map<String, Object> getEventDetail(Integer themeId) {
        Theme t = themeRepository.findById(themeId)
                .orElseThrow(() -> new IllegalArgumentException("找不到活動: " + themeId));

        Session first = t.getSessions().stream()
                .filter(s -> s.getStartTime() != null)
                .min((a, b) -> a.getStartTime().compareTo(b.getStartTime()))
                .orElse(t.getSessions().isEmpty() ? null : t.getSessions().get(0));

        // 各票種售況：彙整本活動所有場次的 Ticket，依票種 ID 分組計算 sold/total
        Map<Integer, int[]> byType = new LinkedHashMap<>(); // ticketTypeId -> [sold, total]
        for (Session s : t.getSessions()) {
            for (Ticket tk : ticketRepository.findBySession_SessionId(s.getSessionId())) {
                if (tk.getTicketType() == null) {
                    continue;
                }
                int[] acc = byType.computeIfAbsent(tk.getTicketType().getTicketTypeId(), k -> new int[2]);
                acc[1]++; // total
                if (tk.getStatus() == TicketStatus.SOLD) {
                    acc[0]++; // sold
                }
            }
        }
        List<Map<String, Object>> ticketTypes = new ArrayList<>();
        for (TicketType tt : ticketTypesOf(t)) {
            int[] acc = byType.getOrDefault(tt.getTicketTypeId(), new int[2]);
            Map<String, Object> tm = new LinkedHashMap<>();
            tm.put("name", tt.getName());
            tm.put("price", tt.getPrice() != null ? tt.getPrice() : BigDecimal.ZERO);
            tm.put("sold", acc[0]);
            tm.put("total", acc[1]);
            ticketTypes.add(tm);
        }

        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getThemeId());
        m.put("title", t.getTitle());
        m.put("orgId", t.getOrganizer() != null ? t.getOrganizer().getOrganizerId() : null);
        m.put("orgName", t.getOrganizer() != null ? t.getOrganizer().getName() : "—");
        m.put("status", mapStatus(t.getStatus()));
        m.put("venue", first != null && first.getLocation() != null ? first.getLocation().getName() : null);
        m.put("startAt", first != null ? first.getStartTime() : earliestStart(t));
        m.put("endAt", first != null ? first.getEndTime() : null);
        m.put("createdAt", t.getCreatedAt());
        m.put("description", t.getDetail());
        m.put("ticketTypes", ticketTypes);
        return m;
    }
}
