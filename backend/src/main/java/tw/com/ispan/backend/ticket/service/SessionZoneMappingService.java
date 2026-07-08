package tw.com.ispan.backend.ticket.service;

import java.util.List;

import tw.com.ispan.backend.ticket.dto.request.SessionAllocationBatchRequestDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionAllocationResultDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionHistoryDTO;

public interface SessionZoneMappingService {

    // 廠商 場次/分區/票種 存檔
    void saveSessionAllocations(SessionAllocationBatchRequestDTO request);

    // 讀取某場次的所有有效綁定規則
    SessionAllocationResultDTO getSessionAllocation(Integer sessionId);

    // 找出同主題、同場館，且「已經有設定過票種」的其他場次
    List<SessionHistoryDTO> getHistory(Integer currentSessionId);
}
