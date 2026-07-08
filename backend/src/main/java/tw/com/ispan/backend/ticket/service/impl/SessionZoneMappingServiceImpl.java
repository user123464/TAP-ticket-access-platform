package tw.com.ispan.backend.ticket.service.impl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.location.entity.Zone;
import tw.com.ispan.backend.location.repository.ZoneRepository;
import tw.com.ispan.backend.theme.entity.Session;
import tw.com.ispan.backend.theme.repository.SessionRepository;
import tw.com.ispan.backend.ticket.dto.request.SessionAllocationBatchRequestDTO;
import tw.com.ispan.backend.ticket.dto.request.TicketTypeAllocationDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionAllocationResultDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionHistoryDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionZoneMappingResponseDTO;
import tw.com.ispan.backend.ticket.entity.SessionZoneMapping;
import tw.com.ispan.backend.ticket.entity.TicketType;
import tw.com.ispan.backend.ticket.repository.SessionZoneMappingRepository;
import tw.com.ispan.backend.ticket.repository.TicketTypeRepository;
import tw.com.ispan.backend.ticket.service.SessionZoneMappingService;

@Service
@RequiredArgsConstructor
public class SessionZoneMappingServiceImpl implements SessionZoneMappingService {

    private final SessionRepository sessionRepository;
    private final TicketTypeRepository ticketTypeRepository;
    private final SessionZoneMappingRepository mappingRepository;
    private final ZoneRepository zoneRepository;// 用於建立與 Zone 的關聯代理

    // 廠商 場次/分區/票種 存檔
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveSessionAllocations(SessionAllocationBatchRequestDTO request) {
        // 1. 驗證場次是否存在
        Session session = sessionRepository.findById(request.sessionId())
                .orElseThrow(() -> new RuntimeException("找不到指定的場次 (ID: " + request.sessionId() + ")"));

        // 2. 撈出資料庫目前該場次現存的所有綁定規則，並收集成一個 Set 準備用來做排除比對
        List<SessionZoneMapping> oldMapings = mappingRepository
                .findBySession_SessionIdAndIsDeletedFalse(request.sessionId());
        Set<Long> untouchedMappingIds = new HashSet<>();
        oldMapings.forEach(m -> untouchedMappingIds.add(m.getMappingId()));

        // 3. 開始拆解前端傳進來的票種配置大包包
        // TicketTypeAllocationDTO是SessionZoneMapping的內部record
        for (TicketTypeAllocationDTO typeDTO : request.ticketTypes()) {

            // A. 處理票種本體 (TicketType)
            TicketType ticketType;
            if (typeDTO.ticketTypeId() == null) {
                // 如果沒有票種 ID -> 新增票種字典
                ticketType = new TicketType();
                ticketType.setTheme(session.getTheme());// 綁定到該場次隸屬的主題
            } else {
                // 如果有票種 ID -> 撈出既有票種準備更新
                ticketType = ticketTypeRepository.findById(typeDTO.ticketTypeId())
                        .orElseThrow(() -> new RuntimeException("找不到指定的票種，無法更新"));
            }
            ticketType.setName(typeDTO.name());
            ticketType.setPrice(typeDTO.price());
            ticketType.setColor(typeDTO.color() != null ? typeDTO.color() : "#ef4444");

            // 存檔票種，取得持久化後的 ID
            TicketType savedType = ticketTypeRepository.save(ticketType);

            // B. 處理分區綁定邏輯 (SessionZoneMapping)
            for (Integer zoneId : typeDTO.boundZoneIds()) {
                // 檢查此 (Session + Zone) 是否曾在資料庫留下足跡？
                SessionZoneMapping mapping = mappingRepository
                        .findBySession_SessionIdAndZone_ZoneId(request.sessionId(), zoneId)
                        .orElseGet(() -> {
                            // 沒足跡 -> 全新創立一筆配對
                            SessionZoneMapping newMapping = new SessionZoneMapping();
                            newMapping.setSession(session);
                            // 利用 EntityManager.getReference 或 Repository 取得 Zone 的代理物件，防呆且高效
                            Zone zoneProxy = zoneRepository.findById(zoneId)
                                    .orElseThrow(() -> new RuntimeException("找不到實體分區 ID: " + zoneId));
                            newMapping.setZone(zoneProxy);
                            return newMapping;
                        });
                // 更新配置欄位
                mapping.setTicketType(savedType);
                mapping.setIsEnabled(typeDTO.isEnabled() != null ? typeDTO.isEnabled() : true);
                mapping.setIsDeleted(false);// 確保它是活著的

                SessionZoneMapping savedMapping = mappingRepository.save(mapping);

                // 關鍵防線：如果這筆資料被更新了，就從「未移除集合」中踢除
                untouchedMappingIds.remove(savedMapping.getMappingId());
            }
        }

        // 4. 清理戰場：如果舊的 Mapping ID 依然殘留在這個 Set 裡，代表廠商在前端把這一區的配對洗掉了
        if (!untouchedMappingIds.isEmpty()) {
            for (Long expireId : untouchedMappingIds) {
                mappingRepository.findById(expireId).ifPresent(m -> {
                    m.setIsDeleted(true);// 執行軟刪除
                    mappingRepository.save(m);
                });
            }
        }
    }

    // 讀取某場次的所有有效綁定規則
    @Override
    @Transactional(readOnly = true) // 標示為唯讀，提升資料庫查詢效能
    public SessionAllocationResultDTO getSessionAllocation(Integer sessionId) {

        // 1. 撈出場次實體，為了取得場地的 SVG
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("找不到指定的場次 (ID: " + sessionId + ")"));

        // 從 Session 關聯的 Location 取得 boundSvg
        // (假設你的 Session Entity 裡面有 @ManyToOne private Location location;)
        String svgContent = session.getLocation().getBoundSvg();

        // 從 session 的 location 實體直接取得場館名稱
        String locationName = session.getLocation().getName();

        // ：撈出該場館所有的分區，並轉成 Map<zoneId, seatCount>
        // 假設你的 ZoneRepository 有寫
        // List<Zone> findByLocation_LocationId(Integer locationId);
        List<Zone> allZones = zoneRepository.findByLocation_LocationId(session.getLocation().getLocationId());

        Map<Integer, Integer> capacities = new HashMap<>();
        for (Zone zone : allZones) {
            // 過濾掉 STAGE，只把真實販售的區域加入對照表
            if (!"STAGE".equals(zone.getName())) {
                capacities.put(zone.getZoneId(), zone.getSeatCount() != null ? zone.getSeatCount() : 0);
            }
        }

        // 2. 撈出綁定規則
        // A. 透過 Repository 撈出該場次所有「未被軟刪除」的綁定規則
        List<SessionZoneMapping> mappings = mappingRepository.findBySession_SessionIdAndIsDeletedFalse(sessionId);
        // B. 透過 Java Stream API，完美轉型成我們之前寫好的超強 ResponseDTO
        List<SessionZoneMappingResponseDTO> mappingDTOs = mappings.stream()
                .map(SessionZoneMappingResponseDTO::fromEntity)
                .collect(Collectors.toList());
        // 3. 把 SVG 和 陣列一起裝進外盒回傳！
        // 把 capacities 也裝進包裹裡回傳
        return new SessionAllocationResultDTO(svgContent, mappingDTOs, capacities, locationName);
    }

    // 載入同主題同場地舊資料
    @Override
    @Transactional(readOnly = true)
    public List<SessionHistoryDTO> getHistory(Integer currentSessionId) {
        // 1. 取得當前場次，藉此獲得 Theme ID 與 Location ID
        Session currentSession = sessionRepository.findById(currentSessionId)
                .orElseThrow(() -> new RuntimeException("找不到當前場次"));

        Integer themeId = currentSession.getTheme().getThemeId();
        Integer locationId = currentSession.getLocation().getLocationId();

        // 2. 找出同主題、同場地，但「不是當前場次」的所有兄弟場次
        // (需在 SessionRepository 補上這個方法，或用 @Query 寫)
        List<Session> brotherSession = sessionRepository
                .findByTheme_ThemeIdAndLocation_LocationIdAndSessionIdNot(themeId, locationId, currentSessionId);
        // 3. 過濾出「真的有設定過綁定規則」的場次，並轉成 DTO
        return brotherSession.stream()
                .filter(session -> {
                    // 檢查這個場次在 Mapping 表裡面有沒有活著的資料
                    List<SessionZoneMapping> maps = mappingRepository
                            .findBySession_SessionIdAndIsDeletedFalse(session.getSessionId());
                    return !maps.isEmpty();
                })
                .map(session -> new SessionHistoryDTO(session.getSessionId(), session.getTitle()))
                .collect(Collectors.toList());
    }

}
