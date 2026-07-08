package tw.com.ispan.backend.ticket.service;

import java.util.List;

import tw.com.ispan.backend.ticket.dto.request.TicketGenerateRequestDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionTicketCataLogDTO;

public interface TicketService {

    /**
     * [系統核心操作]：正式發布場次配置，將實體座位具現化為票券庫存
     * 
     * @param request 包含場次 ID 的產票請求
     */
    void generateTicketForSession(TicketGenerateRequestDTO request);

    /**
     * [前台購票用] 取得指定場次的所有實體票券與狀態
     * * @param sessionId 場次 ID
     * 
     * @return 扁平化的票券明細 DTO 列表 (供前端 SeatMap 渲染使用)
     */
    SessionTicketCataLogDTO getTicketsBySession(Integer sessionId);

    /**
     * 【舊版 V1：留作紀念與壓測對照用】
     * [前台購票核心] 預扣庫存：使用樂觀鎖機制鎖定座位
     * * @param sessionId 場次 ID (防竄改驗證用)
     * 
     * @param ticketIds 欲購買的實體票券 ID 列表
     * @param userId    解析 JWT 後取得的使用者 ID
     * @throws RuntimeException 當票券已被搶走或發生樂觀鎖衝突時拋出
     */
    void lockTickets(Integer sessionId, List<Long> ticketIds, String userId);

    // 清除幽靈座位
    void releaseGhostSeats();

    // 座位狀態更新 (輕量輪詢 API)
    List<Long> getUnavailableTicketIds(Integer sessionId);

    // 結帳前置檢驗 (Pre-checkout Validation)
    boolean validateLockedTickets(Integer sessionId, List<Long> ticketIds, String userId);
}
