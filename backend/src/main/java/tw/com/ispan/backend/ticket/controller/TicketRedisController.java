package tw.com.ispan.backend.ticket.controller;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.ticket.service.TicketRedisService;

// 票務 Redis API

@RestController
@RequestMapping("/api/ticket/redis")
@RequiredArgsConstructor
public class TicketRedisController {

    private final TicketRedisService ticketRedisService;

    // 快取預熱 API (場次公開時呼叫)
    @PostMapping("/warmup/{sessionId}")
    public Response warmUpCache(@PathVariable Integer sessionId) {
        try {
            ticketRedisService.warmUpSessionCache(sessionId);
            return Response.success("場次 " + sessionId + " 快取預熱成功！已準備好迎接搶票流量。");

        } catch (IllegalArgumentException e) {
            return Response.error(e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("快取預熱發生未知錯誤，請查看伺服器 Log");
        }
    }
}
