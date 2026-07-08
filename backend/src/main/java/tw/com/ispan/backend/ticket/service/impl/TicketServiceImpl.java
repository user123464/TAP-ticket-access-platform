package tw.com.ispan.backend.ticket.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.RabbitMQConfig;
import tw.com.ispan.backend.config.RedisPubSubConfig;
import tw.com.ispan.backend.location.entity.Seat;
import tw.com.ispan.backend.location.repository.SeatRepository;
import tw.com.ispan.backend.theme.entity.Session;
import tw.com.ispan.backend.theme.repository.SessionRepository;
import tw.com.ispan.backend.ticket.dto.message.RedisSsePayloadDTO;
import tw.com.ispan.backend.ticket.dto.message.TicketLockMessageDTO;
import tw.com.ispan.backend.ticket.dto.request.TicketGenerateRequestDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionTicketCataLogDTO;
import tw.com.ispan.backend.ticket.dto.response.TicketDetailResponseDTO;
import tw.com.ispan.backend.ticket.entity.SessionZoneMapping;
import tw.com.ispan.backend.ticket.entity.Ticket;
import tw.com.ispan.backend.ticket.entity.TicketStatus;
import tw.com.ispan.backend.ticket.repository.SessionZoneMappingRepository;
import tw.com.ispan.backend.ticket.repository.TicketRepository;
import tw.com.ispan.backend.ticket.service.TicketService;

@Slf4j
@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;

    private final SessionRepository sessionRepository;

    private final SessionZoneMappingRepository mappingRepository;

    private final SeatRepository seatRepository;

    private final RabbitTemplate rabbitTemplate; // 注入 RabbitTemplate 使用 RabbitMQ

    // private final SseService sseService; // 準備單 server 廣播功能

    private final ObjectMapper objectMapper; // Spring Boot 內建的 JSON 轉換器，用來把 Redis 傳來的字串變回 DTO

    private final StringRedisTemplate stringRedisTemplate; // 使用redise功能
    // 預先編譯 Lua 腳本 (放在靜態常數中，提升效能，不用每次呼叫都重新建立)
    private static final DefaultRedisScript<Long> LOCK_SCRIPT;

    static {
        LOCK_SCRIPT = new DefaultRedisScript<>();
        // 載入上面寫好的 Lua 邏輯
        LOCK_SCRIPT.setScriptText(
                "for i = 3, #ARGV do\n" +
                        "    local status = redis.call('HGET', KEYS[1], ARGV[i])\n" +
                        "    if not status or status ~= '1' then\n" +
                        "        return 0\n" +
                        "    end\n" +
                        "end\n" +
                        "for i = 3, #ARGV do\n" +
                        "    local ticketId = ARGV[i]\n" +
                        "    redis.call('HSET', KEYS[1], ticketId, '2')\n" +
                        "    redis.call('SET', 'Ticket:Lock:' .. ticketId, ARGV[1], 'EX', ARGV[2])\n" +
                        "end\n" +
                        "return 1");
        // 設定 Lua 回傳的型別：Spring Data Redis 接收數字一律用 Long.class
        LOCK_SCRIPT.setResultType(Long.class);
    }

    // 🌟 1. 初始化 RabbitTemplate 的確認機制
    @PostConstruct
    public void initRabbitTemplate() {
        // 設定「發送確認」回調：確保訊息有抵達 Exchange
        rabbitTemplate.setConfirmCallback((correlationData, ack, cause) -> {

            if (ack) {
                // 成功抵達 (實戰中通常不特別印 Log 避免洗頻，但開發期可留著)
                log.info("message arrived success to RabbitMQ Exchange, ID: {}",
                        correlationData != null ? correlationData.getId() : "null");
            } else {
                log.error("message arrivel failed caused by: {}", cause);
                // 實戰中這裡可以觸發 Redis 鎖的解除，或寫入發送失敗的 Log 表
            }
            // 設定「退回回調」：當 Exchange 找不到 Queue 時觸發
            rabbitTemplate.setReturnsCallback(returned -> {
                log.error("message returned: {}, RoutingKey: {}", returned.getMessage(), returned.getRoutingKey());
            });
        });
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void generateTicketForSession(TicketGenerateRequestDTO request) {

        Integer sessionId = request.sessionId();

        // 1. 驗證場次是否存在
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("找不到指定的場次 (ID: " + sessionId + ")"));

        // 2. 安全防護：檢查是否有正在被結帳 (LOCKED) 或已結帳 (SOLD) 的票
        // 如果有，代表此場次已正式開賣，絕對不能讓系統執行 Wipe and Rewrite！
        boolean hasActiveTickets = ticketRepository.existsBySession_SessionIdAndStatusIn(
                sessionId,
                List.of(TicketStatus.LOCKED, TicketStatus.SOLD));
        if (hasActiveTickets) {
            throw new IllegalStateException("此活動場次已有票券售出或被消費者放入購物車中，為保護金流與座位安全，禁止重新發布票券庫存！");
        }

        // 3. Wipe and Rewrite：將該場次中，所有還是「可售 (AVAILABLE)」的舊票券全數刪除
        ticketRepository.deleteBySessionIdAndStatus(sessionId, TicketStatus.AVAILABLE);

        // 4. 撈出該場次目前最新、活著的綁定規則 (Mapping)
        List<SessionZoneMapping> latestMappings = mappingRepository
                .findBySession_SessionIdAndIsDeletedFalse(sessionId);

        if (latestMappings.isEmpty()) {
            throw new RuntimeException("此場次尚未設定任何票種與分區，無法產生票券！");
        }

        // 準備一個大陣列，用來裝即將印出的上萬張票
        List<Ticket> newTickets = new ArrayList<>();

        // 5. 開始根據規則印票
        for (SessionZoneMapping mappings : latestMappings) {

            // 防呆：跳過未啟用、或還沒綁定票種的區域
            if (mappings.getIsEnabled() == null || !mappings.getIsEnabled() || mappings.getTicketType() == null) {
                continue;
            }
            // mapping 規則 撈出該分區所有「活著的」實體座位
            List<Seat> targetSeats = seatRepository.findByZone_ZoneId(mappings.getZone().getZoneId());

            // 將每一個位子具現化成一張票
            for (Seat seat : targetSeats) {
                Ticket ticket = new Ticket();
                ticket.setSession(session);
                ticket.setTicketType(mappings.getTicketType());// 綁定廠商決定的票價與票種名稱
                ticket.setSeat(seat);
                ticket.setStatus(TicketStatus.AVAILABLE);// 初始化為可售

                newTickets.add(ticket);
            }
        }
        // 6. 防呆檢查：如果算完發現一張票都沒有
        if (newTickets.isEmpty()) {
            throw new RuntimeException("該場次雖有配置規則，但對應的分區內沒有任何實體座位，無法產生票券！");
        }
        // 7. 光速批次寫入資料庫 (Batch Insert)
        ticketRepository.saveAll(newTickets);
    }

    @Override
    @Transactional(readOnly = true) // 效能優化：唯讀模式，關閉 JPA 髒檢查
    public SessionTicketCataLogDTO getTicketsBySession(Integer sessionId) {
        // 1. 先撈出場次資訊 (為了取得名稱與關聯的 SVG)
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new RuntimeException("找不到指定的場次"));

        // 2. 撈出該場次所有的實體票券
        List<Ticket> tickets = ticketRepository.findBySession_SessionId(sessionId);

        // 3. 將票券實體轉換為前端需要的扁平化 DTO
        // 利用 Java Stream API 與我們剛寫好的 fromEntity 工廠方法，瞬間完成轉換
        List<TicketDetailResponseDTO> ticketDTOs = tickets.stream()
                .map(TicketDetailResponseDTO::fromEntity)
                .toList();// Java 16+ 的簡潔寫法

        // 4. 準備 SVG (這裡請確認你的實體類結構，通常是 Session -> Location -> Svg)
        // 假設你的 SVG 存在 Location 實體中，這裡用 getter 取出：
        String svgContent = session.getLocation().getBoundSvg();

        // 5. 將所有資料打包進 Wrapper DTO 回傳
        return new SessionTicketCataLogDTO(
                session.getSessionId(),
                session.getTitle(),
                session.getLocation().getName(),
                svgContent,
                ticketDTOs);
    }

    // 【舊版 V1：留作紀念與壓測對照用】搶票
    @Transactional(rollbackFor = Exception.class)
    private void lockTicketsLegacyDbVersion(Integer sessionId, List<Long> ticketIds, String userId) {
        // 1. 查 (Fetch & Verify)：一次性批量撈出實體，解決 N+1 效能問題
        List<Ticket> tickets = ticketRepository.findBySession_SessionIdAndTicketIdIn(sessionId, ticketIds);

        // 防竄改驗證：撈出來的數量跟前端傳來的數量不一致，代表有票不屬於該場次，或票券ID是偽造的
        if (tickets.size() != ticketIds.size()) {
            throw new IllegalStateException("非法操作：部分票券不存在或不屬於該場次");
        }

        // 準備好 15 分鐘後的過期時間
        LocalDateTime lockExpireTime = LocalDateTime.now().plusMinutes(15);

        // 2 & 3. 驗與改 (Status Check & Mutate State)：在一個迴圈內搞定
        for (Ticket ticket : tickets) {
            // 驗：只要有一張票不是 AVAILABLE，直接中斷交易 (Rollback)
            if (ticket.getStatus() != TicketStatus.AVAILABLE) {
                // 這裡拋出的例外，會被你在 Controller 寫好的 catch 攔截並轉成好看的錯誤訊息
                throw new IllegalStateException("晚了一步，您選擇的部分座位已被鎖定或售出！");
            }

            // 改：更新狀態、押上鎖定者、押上到期時間
            ticket.setStatus(TicketStatus.LOCKED); // 🌟 絕對不能漏掉改狀態！
            ticket.setLockBy(userId);
            ticket.setLockUntil(lockExpireTime);
        }

        // 4. 存 (Persistence)：
        // 由於 @Transactional 的 Dirty Checking 機制，走到這裡方法結束時，
        // JPA 會自動拋出 UPDATE 語句並檢查 @Version。
        // 如果在這 0.01 秒內有別人先 Update 了同一張票，
        // 這裡底層會自動拋出 ObjectOptimisticLockingFailureException
    }

    // 【新版 V2：目前對外服役的版本】
    // 【升級版】高併發搶票 API：使用 Redis + Lua 腳本預扣庫存
    @Override
    public void lockTickets(Integer sessionId, List<Long> ticketIds, String userId) {

        // 1. 準備 KEYS 參數 (對應 Lua 的 KEYS[1])
        String statusKey = "Session:" + sessionId + ":Status";
        List<String> keys = List.of(statusKey);

        // 2. 準備 ARGV 參數 (對應 Lua 的 ARGV[1], ARGV[2], ARGV[3]...)
        List<String> args = new ArrayList<>();
        args.add(userId);// ARGV[1]: 使用者 ID
        args.add("900");// ARGV[2]: 鎖定時間 (15分鐘 = 900秒)

        // 把所有的 Ticket ID 轉成字串塞進去 (變成 ARGV[3], ARGV[4]...)
        for (Long id : ticketIds) {
            args.add(String.valueOf(id));
        }

        // 3. 🚀 一鍵發射！執行 Lua 腳本
        // 這裡會瞬間在 Redis 內部完成所有的驗證與寫入，期間完全不會被其他請求干擾
        Long result = stringRedisTemplate.execute(LOCK_SCRIPT, keys, args.toArray(new Object[0]));

        // 4. 判斷回傳結果
        if (result == null || result == 0L) {
            // Lua 回傳 0，代表票已經被搶走了
            // 這裡拋出例外，會被 Controller 捕捉並回傳 "晚了一步..." 給前端
            throw new IllegalStateException("晚了一步，您選擇的部分座位剛被搶走，請重新選擇！");
        }
        // 如果順利走到這裡，代表 Lua 回傳 1，搶票成功！15 分鐘鎖定完成！
        // ==========================================
        // 4. Lua 秒殺成功！發送非同步訊息給 RabbitMQ 去更新 SQL Server
        // ==========================================
        // 冪等性1. 產生這條訊息的唯一身分證 (Message ID)
        String uniqueMessageId = UUID.randomUUID().toString();
        // 冪等性2. 將 messageId 裝進包裹
        TicketLockMessageDTO message = new TicketLockMessageDTO(
                uniqueMessageId, sessionId, ticketIds, userId);

        // 🌟 2. 建立這筆訊息的唯一追蹤碼
        CorrelationData correlationData = new CorrelationData(UUID.randomUUID().toString());

        // 把包裹丟進郵局 (Exchange)，並貼上郵遞區號 (Routing Key)
        // 🌟 3. 發送時帶上 correlationData
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TICKET_EXCHANGE,
                RabbitMQConfig.TICKET_LOCK_ROUTING_KEY,
                message,
                correlationData);

        // ==========================================
        // 5. 同時發送一封「15分鐘後定時檢查」的信件到等待房
        // ==========================================
        // 重新產生一個新的 MessageID，因為這是另一個獨立任務
        String releaseMessageId = UUID.randomUUID().toString();
        TicketLockMessageDTO releaseMessage = new TicketLockMessageDTO(
                releaseMessageId, sessionId, ticketIds, userId);

        CorrelationData releaseCorrelation = new CorrelationData(releaseMessageId);

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.TICKET_EXCHANGE,
                RabbitMQConfig.TICKET_WAIT_ROUTING_KEY,
                releaseMessage,
                releaseCorrelation);

        // ==========================================
        // 6. 發送 SSE 廣播，通知這個場次的所有人：這些座位被鎖定了！
        // 批次包裝 SSE 廣播，並透過 Redis 發送全國廣播
        // ==========================================
        // 將 ticketIds 轉成 JSON 陣列格式，例如: {"ticketIds": [976,977], "status": 2}
        String sseMessage = String.format("{\"ticketIds\": %s, \"status\": 2}", ticketIds.toString());

        try {
            // A. 把要跨服傳遞的資訊包裝進 DTO
            RedisSsePayloadDTO payload = new RedisSsePayloadDTO(sessionId, sseMessage);
            // B. 轉成 JSON 字串
            String redisMsg = objectMapper.writeValueAsString(payload);
            // C. 對準頻道，一鍵發射！
            stringRedisTemplate.convertAndSend(RedisPubSubConfig.REDIS_TICKET_CHANNEL, redisMsg);
        } catch (Exception e) {
            log.error("❌ 發送 Redis 廣播失敗", e);
        }
        // 寄完信直接回傳給 Controller，不用等 DB 更新，消費者體感 0 延遲！
    }

    // 釋放過期的幽靈座位
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseGhostSeats() {
        // 取得現在時間
        LocalDateTime now = LocalDateTime.now();

        // 執行批量更新，將狀態為 LOCKED 且 lockUntil 小於等於現在時間的票，全部洗回 AVAILABLE
        int releasedCount = ticketRepository.releaseExpiredLockedTickets(
                TicketStatus.AVAILABLE,
                TicketStatus.LOCKED,
                now);

        if (releasedCount > 0) {
            // 在業界，這類背景執行的任務通常會印出 Log，方便日後維運與追蹤
            // 之後換成 @Slf4j 寫 log 檔
            System.out.println("[排程任務] 成功清理並釋放了 " + releasedCount + " 個逾期未結帳的幽靈座位。時間：" + now);
        }
    }

    // 座位狀態更新 (輕量輪詢 API)
    @Override
    @Transactional(readOnly = true) // readOnly = true，不開啟髒檢查，提升查詢效能
    public List<Long> getUnavailableTicketIds(Integer sessionId) {
        // 找出已被鎖定 (LOCKED) 或已經賣掉 (SOLD) 的票
        return ticketRepository.findTicketIdsBySessionIdAndStatuses(
                sessionId,
                List.of(TicketStatus.LOCKED, TicketStatus.SOLD));
    }

    // 舊版：結帳前置檢驗 (Pre-checkout Validation)
    @Transactional(readOnly = true) // 依然是純查詢，加上 readOnly
    private boolean validateLockedTicketsLegacyDbVersion(Integer sessionId, List<Long> ticketIds, String userId) {
        List<Ticket> tickets = ticketRepository.findBySession_SessionIdAndTicketIdIn(sessionId, ticketIds);

        // 防呆 1：數量不符 (票券被竄改)
        if (tickets.size() != ticketIds.size()) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        // 防呆 2：嚴格檢查每一張票的狀態、擁有者、以及是否過期
        for (Ticket ticket : tickets) {
            // 🌟 加入這四行最強照妖鏡
            log.info("🎫 正在檢驗票券 ID: {}", ticket.getTicketId());
            log.info("🔍 狀態檢查 - DB狀態: {}, 預期: LOCKED", ticket.getStatus());
            log.info("🔍 擁有者檢查 - DB: '{}', JWT: '{}'", ticket.getLockBy(), userId);
            log.info("🔍 過期檢查 - DB時間: {}, 現在時間: {}", ticket.getLockUntil(), now);
            if (ticket.getStatus() != TicketStatus.LOCKED || // 不是鎖定中
                    !userId.equals(ticket.getLockBy()) || // 不是這個人鎖定的
                    ticket.getLockUntil() == null || // 鎖定時間異常
                    ticket.getLockUntil().isBefore(now)// 已經超過 15 分鐘
            ) {
                return false;
            }
        }
        return true; // 全部通過驗證，可以安心結帳！
    }

    // 結帳前置檢驗 (Pre-checkout Validation) - 🌟 Redis 升級版
    @Override
    public boolean validateLockedTickets(Integer sessionId, List<Long> ticketIds, String userId) {
        // 防呆 1：數量檢查 (避免前端傳空陣列進來直接通過)
        if (ticketIds == null || ticketIds.isEmpty()) {
            return false;
        }

        // 防呆 2：去 Redis 檢查每一張票的「過期鎖」是否還活著，並且擁有者是不是這個人
        for (Long ticketId : ticketIds) {
            String lockKey = "Ticket:Lock:" + ticketId;

            // 從 Redis 拿出這把鎖的主人
            String lockUserId = stringRedisTemplate.opsForValue().get(lockKey);

            // 情境 A：鎖不見了 (lockedUserId == null) -> 代表 15 分鐘到了，票被釋放了
            // 情境 B：鎖還在，但名字不對 (!userId.equals) -> 代表票是別人的
            if (lockUserId == null || !userId.equals(lockUserId)) {
                return false;
            }
        }
        return true; // 全部通過驗證，可以安心結帳！
    }
}