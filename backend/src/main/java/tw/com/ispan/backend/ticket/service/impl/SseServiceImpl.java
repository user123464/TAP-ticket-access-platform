package tw.com.ispan.backend.ticket.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.ticket.service.SseService;

@Slf4j
@Service
public class SseServiceImpl implements SseService {
    // ==========================================
    // 📖 電台總機的「頻道名冊」
    // Key: 演唱會場次 ID (sessionId)
    // Value: 正在看這場演唱會的聽眾連線清單 (List<SseEmitter>)
    // ==========================================

    private final Map<Integer, List<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    /**
     * 🎧 功能一：聽眾連線 (訂閱頻道)
     * 當前端進入 SVG 座位圖畫面時，會呼叫這個方法
     */
    @Override
    public SseEmitter subscribe(Integer sessionId, String userId) {
        // 1. 建立一個連線物件 (SseEmitter)。
        // 參數是 Timeout 時間 (毫秒)。0 代表永不超時，但實戰中建議設個合理值(例如 1 小時: 3600000L)，
        // 避免幽靈連線永遠佔用記憶體。
        SseEmitter emitter = new SseEmitter(3600000L);

        // 2. 把這位聽眾加入對應的頻道名單中
        // computeIfAbsent: 如果這個 sessionId 還沒有人聽，就幫它建立一個全新的 CopyOnWriteArrayList
        emittersMap.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("📡 會員 {} 進入了場次 {} 的廣播頻道", userId, sessionId);

        // 3. 註冊「連線中斷」的善後處理 (超級重要！防止 Memory Leak)
        // 當前端關閉瀏覽器、跳轉頁面、或網路斷線時，Spring Boot 會自動觸發這些方法
        emitter.onCompletion(() -> removeEmitter(sessionId, emitter, userId));
        emitter.onTimeout(() -> removeEmitter(sessionId, emitter, userId));
        emitter.onError((e) -> removeEmitter(sessionId, emitter, userId));

        // 4. 連線成功後，立刻隨便發送一條招呼語給前端
        // 這是實戰小技巧：強迫建立連線並發送 HTTP Header，
        // 有些代理伺服器(如 Nginx)需要看到第一筆資料才算連線成功
        try {
            emitter.send(SseEmitter.event()
                    .name("connect")// 事件名稱
                    .data("連線成功，您已進入場次 " + sessionId + " 頻道"));// 傳送的資料
        } catch (IOException e) {
            log.error("❌ 建立連線時發送招呼語失敗", e);
            emitter.completeWithError(e);
        }
        return emitter; // 把這個連線物件交給 Controller，Controller 會把它 return 給前端
    }

    /**
     * 🧹 內部方法：清理斷線的聽眾
     */
    private void removeEmitter(Integer sessionId, SseEmitter emitter, String userId) {
        List<SseEmitter> sessionEmitters = emittersMap.get(sessionId);
        if (sessionEmitters != null) {
            sessionEmitters.remove(emitter);
            log.info("🔌 會員 {} 已離開場次 {} 的廣播頻道", userId, sessionId);

            // 可選優化：如果這個頻道的人都走光了，就把這個頻道的空 List 也刪掉，節省記憶體
            if (sessionEmitters.isEmpty()) {
                emittersMap.remove(sessionId);
            }
        }
    }

    /**
     * 📢 功能二：發送全頻道廣播
     * 當 RabbitMQ 扣位成功，或死信佇列釋放座位時，會呼叫這個方法
     */
    @Override
    public void broadcastToSession(Integer sessionId, Object message) {
        List<SseEmitter> sessionEmitters = emittersMap.get(sessionId);

        // 如果這個場次剛好沒有半個人在看，就什麼都不做
        if (sessionEmitters == null || sessionEmitters.isEmpty()) {
            return;
        }

        log.info("📢 對場次 {} 廣播座位更新，目前聽眾數: {}", sessionId, sessionEmitters.size());

        // 遍歷這個頻道的所有聽眾，逐一發送訊息
        for (SseEmitter emitter : sessionEmitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("seat-update")// 統一事件名稱，前端會監聽這個名字
                        .data(message));// 這裡通常會塞一個 DTO 或 JSON 字串 (包含座位 ID 與新狀態)
            } catch (IOException e) {
                // 如果發送失敗 (例如對方網路突然斷了，但還沒觸發 onTimeout)
                // 我們就手動把這個連線標記為失敗，它會自動觸發上面的 onError 去執行 removeEmitter
                emitter.completeWithError(e);
            }
        }
    }

}
