package tw.com.ispan.backend.ticket.service;

import java.util.List;

import tw.com.ispan.backend.ticket.dto.response.SessionTicketCataLogDTO;

// 票務： Redis 相關功能
public interface TicketRedisService {

    // 票券預熱
    void warmUpSessionCache(Integer sessionId);

    // 1. 從 Redis 獲取場次座位大禮包 (Catalog)
    SessionTicketCataLogDTO getSessionCatalogFromCache(Integer sessionId);

    // 2. 從 Redis 獲取不可售/已鎖定的 ticketId 列表 (Status 輪詢)
    List<Long> getUnavailableTicketIdsFromCache(Integer sessionId);
}
