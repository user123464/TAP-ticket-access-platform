package tw.com.ispan.backend.ticket.service.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.ticket.dto.response.SessionTicketCataLogDTO;
import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.ticket.entity.TicketStatus;
import tw.com.ispan.backend.ticket.repository.TicketRepository;
import tw.com.ispan.backend.ticket.service.TicketRedisService;
import tw.com.ispan.backend.ticket.service.TicketService;

// 票務： Redis 相關功能
@Slf4j
@Service
@RequiredArgsConstructor
public class TicketRedisServiceImpl implements TicketRedisService {

    // 哨兵欄位：標記該場次狀態盤已預熱過但實際上沒有任何票券（避免與真實 ticketId 欄位混淆，且不參與可用性判斷）
    private static final String EMPTY_MARKER_FIELD = "__EMPTY__";

    private final TicketService ticketService;

    private final TicketRepository ticketRepository;// 票券查詢資料用
    // 用 StringRedisTemplate 配合 ObjectMapper (Jackson)，
    // 存進去的資料會是乾淨的 JSON 與純文字
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper; // Spring Boot 內建的 JSON 轉換工具

    // 內部小工具：狀態轉數字 (跟你 DTO 裡的邏輯一致)
    private int convertStatusToInt(TicketStatus status) {
        return switch (status) {
            case AVAILABLE -> 1;
            case LOCKED -> 2;
            case SOLD -> 3;
            case RESERVED -> 0;
            default -> 0;
        };
    }

    // 票券預熱
    @Override
    @Transactional(readOnly = true) // 只需要讀取 DB，不開啟髒檢查
    public void warmUpSessionCache(Integer sessionId) {
        log.info("開始執行場次 {} 的 Redis 快取預熱...", sessionId);

        // 1. 撈取 DB 中該場次的所有票券
        // 🌟 注意：空票種（尚未產生庫存 / 已全數下架）不視為錯誤，型錄仍需回傳場地 SVG 供前端顯示場地圖
        List<Ticket> tickets = ticketRepository.findBySession_SessionId(sessionId);

        // 2. 定義 Redis Keys
        String catalogKey = "Session:" + sessionId + ":Catalog";
        String statusKey = "Session:" + sessionId + ":Status";

        // 3. 清空舊快取 (Wipe and Rewrite 策略，避免重覆點擊預熱殘留髒資料)
        stringRedisTemplate.delete(List.of(catalogKey, statusKey));

        // ==========================================
        // 4. 準備動態狀態盤 (Hash) 資料
        // ==========================================
        // 將 Map 的 Key 和 Value 都宣告為 String，配合 StringRedisTemplate
        Map<String, String> statusMap = new HashMap<>();

        for (Ticket ticket : tickets) {
            // 將 Enum 轉換為前端看得懂的數字狀態 (1:可售, 2:鎖定, 3:售出)
            int statusCode = convertStatusToInt(ticket.getStatus());

            statusMap.put(
                    String.valueOf(ticket.getTicketId()), // Hash 的 Field (Ticket ID)
                    String.valueOf(statusCode)); // Hash 的 Value (狀態碼)
        }

        // 🌟 空票種防呆：Redis Hash 不允許寫入空 Map（putAll 對空 Map 會噴例外）
        // 塞入一個哨兵欄位標記「已完成預熱、確實沒有票券」，避免下次請求誤判 Cache Miss 而重複打 DB 預熱
        if (statusMap.isEmpty()) {
            statusMap.put(EMPTY_MARKER_FIELD, "0");
        }

        // 🚀 核心優化：使用 putAll 一次性寫入 Redis，不要放在迴圈裡 put！
        stringRedisTemplate.opsForHash().putAll(statusKey, statusMap);

        // ==========================================
        // 5. 準備靜態型錄 (String + JSON) 資料
        // ==========================================
        try {
            // 🌟 核心優化：直接呼叫原本寫好的 DB Service 拿到完美包含 SVG 的大禮包！
            SessionTicketCataLogDTO fullCatalog = ticketService.getTicketsBySession(sessionId);

            // 將整個大禮包轉換為 JSON 字串
            String catalogJson = objectMapper.writeValueAsString(fullCatalog);

            // 寫入 Redis
            stringRedisTemplate.opsForValue().set(catalogKey, catalogJson);

        } catch (Exception e) {
            log.error("快取預熱 JSON 序列化失敗", e);
            throw new RuntimeException("快取預熱失敗：資料轉換錯誤");
        }
        log.info("場次 {} 預熱完成！...", sessionId);
    }

    // 獲取場次座位大禮包 (動靜分離：從 String 讀取靜態 JSON)
    // 反序列化時，告訴 Jackson 我們要還原的是 SessionTicketCataLogDTO
    @Override
    public SessionTicketCataLogDTO getSessionCatalogFromCache(Integer sessionId) {
        String catalogKey = "Session:" + sessionId + ":Catalog";

        // 從 Redis 讀取 JSON 字串
        String catalogJson = stringRedisTemplate.opsForValue().get(catalogKey);

        // 防呆機制 (Cache Miss)：如果 Redis 剛好沒有這筆資料（例如被閃退、清除或忘記預熱）
        if (catalogJson == null) {
            log.warn("[Cache Miss] Redis 找不到場次 {} 的型錄，執行後端自動兜底...", sessionId);
            // 呼叫你原本去 DB 撈資料的舊 Service 方法（保持舊 API 的 SessionTicketCataLogDTO 結構或直接查 DB）
            // 這裡為了保持架構乾淨，如果發生 Miss，最安全的是直接拋錯提示管理員預熱，或者即時觸發一次預熱
            warmUpSessionCache(sessionId);
            catalogJson = stringRedisTemplate.opsForValue().get(catalogKey);
        }
        try {
            // 還原的型別：直接還原成 SessionTicketCataLogDTO.class
            return objectMapper.readValue(catalogJson, SessionTicketCataLogDTO.class);
        } catch (Exception e) {
            log.error("Redis 型錄反序列化失敗", e);
            throw new RuntimeException("讀取場次型錄失敗");
        }
    }

    // 輕量級狀態輪詢 (高頻更新：從 Hash 讀取，並在記憶體過濾)
    @Override
    public List<Long> getUnavailableTicketIdsFromCache(Integer sessionId) {
        String statusKey = "Session:" + sessionId + ":Status";

        // 核心優化：一次性拿回該場次所有的 Field 和 Value (Map<TicketId, Status>)
        Map<Object, Object> statusMap = stringRedisTemplate.opsForHash().entries(statusKey);

        // 如果快取不存在，觸發自動預熱
        if (statusMap.isEmpty()) {
            log.warn("[Cache Miss] Redis 找不到場次 {} 的狀態盤，執行自動預熱...", sessionId);
            warmUpSessionCache(sessionId);
            statusMap = stringRedisTemplate.opsForHash().entries(statusKey);
        }

        List<Long> unavailableTicketIds = new ArrayList<>();

        // 走訪 Map，只要狀態「不是 1 (AVAILABLE)」的，全部收集起來
        for (Map.Entry<Object, Object> entry : statusMap.entrySet()) {
            String ticketIdStr = (String) entry.getKey();
            String statusStr = (String) entry.getValue();

            // 🌟 跳過空票種哨兵欄位，避免被誤判為票券 ID
            if (EMPTY_MARKER_FIELD.equals(ticketIdStr)) {
                continue;
            }

            int statusCode = Integer.parseInt(statusStr);
            // 2: 鎖定中, 3: 已售出 (也就是前端畫圖需要反灰、禁止點擊的座位)
            if (statusCode == 2 || statusCode == 3) {
                unavailableTicketIds.add(Long.parseLong(ticketIdStr));
            }
        }
        return unavailableTicketIds;
    }

}
