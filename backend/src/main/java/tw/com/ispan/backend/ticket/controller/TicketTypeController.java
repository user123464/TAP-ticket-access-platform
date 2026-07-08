package tw.com.ispan.backend.ticket.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.ticket.dto.request.SessionAllocationBatchRequestDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionAllocationResultDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionHistoryDTO;
import tw.com.ispan.backend.ticket.service.SessionZoneMappingService;

@RestController
@RequestMapping("/api/ticket-type")
@RequiredArgsConstructor
public class TicketTypeController {

    private final SessionZoneMappingService mappingService;

    /**
     * 批次儲存/發布場次的分區票種配置
     * 對應前端 TicketTypeEdit.vue 的 saveAllocation() 動作
     */
    @PostMapping("/allocations")
    public Response saveAllocations(@Valid @RequestBody SessionAllocationBatchRequestDTO request) {
        try {
            // 呼叫 Service 執行精密的比對與更新邏輯
            mappingService.saveSessionAllocations(request);

            // 成功時，回傳前端期待的成功訊息
            return Response.success("場次票種配置發布成功！", null);
        } catch (IllegalArgumentException e) {
            // 針對資料驗證或邏輯錯誤，回傳清楚的提示
            return Response.error("資料格式錯誤：" + e.getMessage());
        } catch (Exception e) {
            // 針對不可預期的系統錯誤
            e.printStackTrace();
            return Response.error("發布失敗，請聯絡系統管理員：" + e.getMessage());
        }
    }

    /**
     * 讀取某個場次的分區票種配置
     * 對應前端載入 TicketTypeEdit.vue 時的初始化動作
     */
    @GetMapping("/allocations/{sessionId}")
    public Response getAllocations(@PathVariable Integer sessionId) {
        try {
            // 呼叫 Service 取得 DTO 列表
            SessionAllocationResultDTO allocations = mappingService.getSessionAllocation(sessionId);
            // 成功時，將資料包在 data 欄位回傳
            return Response.success("讀取配置成功", allocations);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("讀取配置失敗：" + e.getMessage());
        }
    }

    // 找出同主題、同場館，且「已經有設定過票種」的其他場次
    @GetMapping("/allocations/history/{sessionId}")
    public Response getHistory(@PathVariable Integer sessionId) {
        try {
            List<SessionHistoryDTO> history = mappingService.getHistory(sessionId);
            return Response.success("取得歷史場次成功", history);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("取得歷史場次失敗：" + e.getMessage());
        }
    }
}
