package tw.com.ispan.backend.auction.service.impl;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.auction.service.AuctionSseService;
import tw.com.ispan.backend.auction.dto.message.AuctionBidEventDTO;

@Slf4j
@Service
public class AuctionSseServiceImpl implements AuctionSseService {

    private final Map<Integer, List<SseEmitter>> emittersMap = new ConcurrentHashMap<>();

    @Override
    public SseEmitter subscribe(Integer auctionId) {
        SseEmitter emitter = new SseEmitter(3600000L);
        emittersMap.computeIfAbsent(auctionId, key -> new CopyOnWriteArrayList<>()).add(emitter);
        log.info("拍賣 {} 有新的 SSE 訂閱", auctionId);

        emitter.onCompletion(() -> removeEmitter(auctionId, emitter));
        emitter.onTimeout(() -> removeEmitter(auctionId, emitter));
        emitter.onError(error -> removeEmitter(auctionId, emitter));

        try {
            emitter.send(SseEmitter.event()
                    .name("connect")
                    .data("已連線至拍賣 " + auctionId + " 即時更新頻道"));
        } catch (IOException e) {
            log.error("建立拍賣 SSE 連線失敗", e);
            emitter.completeWithError(e);
        }

        return emitter;
    }

    @Override
    public void broadcastToAuction(Integer auctionId, Object message) {
        List<SseEmitter> sessionEmitters = emittersMap.get(auctionId);
        if (sessionEmitters == null || sessionEmitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : sessionEmitters) {
            try {
                if (message instanceof AuctionBidEventDTO dto) {
                    emitter.send(SseEmitter.event()
                            .id(dto.eventId())
                            .name("bid-update")
                            .data(dto));
                } else {
                    emitter.send(SseEmitter.event()
                            .name("bid-update")
                            .data(message));
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }
    }

    private void removeEmitter(Integer auctionId, SseEmitter emitter) {
        List<SseEmitter> auctionEmitters = emittersMap.get(auctionId);
        if (auctionEmitters == null) {
            return;
        }

        auctionEmitters.remove(emitter);
        log.info("拍賣 {} 有 SSE 訂閱離開", auctionId);

        if (auctionEmitters.isEmpty()) {
            emittersMap.remove(auctionId);
        }
    }
}