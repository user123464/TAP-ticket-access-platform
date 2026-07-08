package tw.com.ispan.backend.ticket.controller;

import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.ticket.dto.request.TicketGenerateRequestDTO;
import tw.com.ispan.backend.ticket.dto.request.TicketLockRequestDTO;
import tw.com.ispan.backend.ticket.dto.response.SessionTicketCataLogDTO;
import tw.com.ispan.backend.ticket.service.TicketRedisService;
import tw.com.ispan.backend.ticket.service.TicketService;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/ticket")
public class TicketController {

    private final TicketService ticketService;

    private final TicketRedisService ticketRedisService;// 🌟 改注入 RedisService

    // 依照sessionID 綁定的場地生成票券
    @PostMapping("/generate-inventory")
    public Response generateTickets(@Valid @RequestBody TicketGenerateRequestDTO request) {
        try {
            ticketService.generateTicketForSession(request);
            return Response.success("票券庫存產生成功！");
        } catch (IllegalStateException e) {
            // 攔截 Service 第一道防線：目前有正在結帳或已售出的票
            // 特別攔截自訂的金流防呆例外
            return Response.error(e.getMessage());
        } catch (DataIntegrityViolationException e) {
            // 攔截資料庫最後防線：試圖刪除曾經有過訂單紀錄的 AVAILABLE 票
            return Response.error("發布失敗：此場次已有歷史訂單紀錄。為保護金流完整性，禁止重新發布或刪除現有票券庫存！");
        } catch (Exception e) {
            // 真正的未知系統錯誤
            e.printStackTrace();
            return Response.error("發生未知錯誤，請聯繫平台管理員");
        }
    }

    // 取得指定場次的所有實體票券與結構 (供前台座位圖使用) (從 Redis 讀取)
    @GetMapping("/session/{sessionId}")
    public Response getTicketBySession(@PathVariable Integer sessionId) {
        // // 改從 Redis 讀取
        try {
            // 完美接收包含 SVG 的大禮包
            SessionTicketCataLogDTO catalog = ticketRedisService.getSessionCatalogFromCache(sessionId);
            return Response.success("型錄獲取成功", catalog);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("無法取得場次型錄資料");
        }
        // 原版:讀 DB
        // try {
        // SessionTicketCataLogDTO tickets =
        // ticketService.getTicketsBySession(sessionId);
        // return Response.success("票券查詢成功", tickets);
        // } catch (Exception e) {
        // e.printStackTrace();
        // return Response.error("無法取得場次票券資料：" + e.getMessage());
        // }
    }

    // 搶票API
    @PostMapping("/lock")
    public Response lockTickets(
            @Valid @RequestBody TicketLockRequestDTO request,
            // 透過 @AuthenticationPrincipal 直接取得攔截器解析好的 User ID
            @AuthenticationPrincipal CustomUserDetails user) {

        // ==========================================
        // ⚠️ 壓測後門邏輯：自動核發測試身分
        // 如果有登入就用登入的 ID；如果是 k6 壓測打進來的 (user == null)，就給固定 ID USR000003
        // ==========================================
        String userId = (user != null)
                ? user.getUserId()
                : "USR0000003";
        // 呼叫你的核心服務
        try {
            // 這裡不再需要處理 JWT 字串，直接將參數傳給 Service
            ticketService.lockTickets(request.sessionId(), request.ticketIds(), userId);
            // 如果沒拋出異常，代表 15 分鐘的樂觀鎖鎖定成功！
            return Response.success("座位鎖定成功！請於 15 分鐘內完成結帳。");

        } catch (ObjectOptimisticLockingFailureException e) {
            // 攔截樂觀鎖衝突：兩人同時 Update 同一筆紀錄，第二人會觸發此例外
            return Response.error("晚了一步，您選擇的座位已被鎖定，請重新選擇！");

        } catch (IllegalStateException | IllegalArgumentException e) {
            // 攔截 Service 層拋出的商業邏輯錯誤 (狀態不對、場次不符等)
            // 將 Service 寫的防呆訊息直接轉發給前端
            return Response.error(e.getMessage());

        } catch (Exception e) {
            // 真正的未知錯誤
            e.printStackTrace();
            return Response.error("未知錯誤" + e.getMessage());
        }
    }

    // 座位狀態即時更新 (輕量輪詢 API) (從 Redis 讀取)
    @GetMapping("/session/{sessionId}/status")
    public Response getUnavailableSeatStatus(@PathVariable Integer sessionId) {
        try {
            // 原本打 DB 的 `ticketService.getUnavailableTicketIds`
            // 現在改由 Redis 的 Hash 盤處理，速度提升數十倍
            List<Long> unavailableIds = ticketRedisService.getUnavailableTicketIdsFromCache(sessionId);
            return Response.success("狀態更新成功", unavailableIds);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("獲取最新狀態失敗");
        }
        // 原版:讀DB
        // try {
        // List<Long> unavailableIds = ticketService.getUnavailableTicketIds(sessionId);
        // return Response.success("狀態更新成功", unavailableIds);
        // } catch (Exception e) {
        // e.printStackTrace();
        // return Response.error("獲取最新狀態失敗");
        // }
    }

    // 結帳前置檢驗
    @PostMapping("/validate")
    public Response validateCart(
            @Valid @RequestBody TicketLockRequestDTO request,
            @AuthenticationPrincipal CustomUserDetails user) {
        try {
            boolean isValid = ticketService.validateLockedTickets(request.sessionId(), request.ticketIds(),
                    user.getUserId());

            if (isValid) {
                return Response.success("驗證通過，訂單有效");
            } else {
                // 回傳錯誤，前端接到後就會觸發 cartStore.clearCart() 並且踢回選位畫面
                return Response.error("您的保留時間已過期或失效，請重新選購！");
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("驗證失敗，請稍後再試");
        }
    }
}
