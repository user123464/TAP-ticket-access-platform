package tw.com.ispan.backend.orderTicket.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.service.AdminOrderService;

/**
 * 系統管理後台 - 唯讀訂單查詢控制端點。
 *
 * <p>路徑前綴為 {@code /api/admin/orders}。
 * 限制只有具備 {@code ORDER_VIEW} 權限的人員可調用，提供唯讀的全站訂單列表與訂單詳細金流時間軸查詢，不提供任何修改或刪除端點。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/orders")
@RequiredArgsConstructor
public class AdminOrderController {

    // 注入專責後台訂單邏輯的 Service
    private final AdminOrderService adminOrderService;

    /**
     * 獲取全平台的所有門票訂單清單。
     * 
     * <p>API: GET /api/admin/orders</p>
     *
     * @return 封裝了狀態碼與全站訂單列表的響應體
     */
    @GetMapping
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public ResponseEntity<?> list() {
        log.info("【管理員後台】調用 API 查詢全站訂單清單...");
        return ResponseEntity.ok(Map.of("status", "success", "data", adminOrderService.listOrders()));
    }

    /**
     * 根據訂單編號，查詢特定訂單的詳情。
     * 
     * <p>包含買家資料、所有門票明細、以及簡易金流時間軸。
     * API: GET /api/admin/orders/{id}</p>
     *
     * @param id 訂單編號
     * @return 封裝了狀態碼與詳細訂單資訊的響應體，若找不到訂單則返回 404
     */
    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ORDER_VIEW')")
    public ResponseEntity<?> detail(@PathVariable String id) {
        log.info("【管理員後台】調用 API 查詢特定訂單詳情 - id={}", id);
        try {
            return ResponseEntity.ok(Map.of("status", "success", "data", adminOrderService.getOrderDetail(id)));
        } catch (IllegalArgumentException e) {
            log.error("【管理員後台】查詢訂單失敗 - id={}, error={}", id, e.getMessage());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
