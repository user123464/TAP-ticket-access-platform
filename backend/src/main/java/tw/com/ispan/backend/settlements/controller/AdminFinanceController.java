package tw.com.ispan.backend.settlements.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.settlements.service.AdminFinanceService;

/**
 * Admin 後台「財務結算」REST 端點（批次 2 新建）。
 *
 * <p>路徑前綴 {@code /api/admin/finance}，授權 {@code SETTLEMENT_VIEW}
 * （data.sql 已掛 ADMIN+SUPER_ADMIN）。回傳格式 {@code {status:"success", data:...}}。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/finance")
@RequiredArgsConstructor
public class AdminFinanceController {

    private final AdminFinanceService adminFinanceService;

    /** 財務宏觀儀表板：月趨勢 / 各廠商手續費佔比 / 本月應收 / 訂閱摘要。 */
    @GetMapping("/dashboard")
    @PreAuthorize("hasAuthority('SETTLEMENT_VIEW')")
    public ResponseEntity<?> dashboard(@RequestParam(value = "months", defaultValue = "6") int months) {
        log.info("API Admin 財務儀表板 months={}", months);
        try {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminFinanceService.getDashboard(months)));
        } catch (Exception e) {
            log.error("查詢財務儀表板失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢財務儀表板失敗"));
        }
    }

    /** 廠商手續費明細 + 合計，依交易區間查詢。 */
    @GetMapping("/settlement")
    @PreAuthorize("hasAuthority('SETTLEMENT_VIEW')")
    public ResponseEntity<?> settlement(
            @RequestParam(value = "from", required = false) String from,
            @RequestParam(value = "to", required = false) String to) {
        log.info("API Admin 手續費明細 from={}, to={}", from, to);
        try {
            LocalDate fromDate;
            LocalDate toDate;

            if (from == null || from.isBlank()) {
                fromDate = LocalDate.now().withDayOfMonth(1);
            } else {
                fromDate = LocalDate.parse(from);
            }

            if (to == null || to.isBlank()) {
                toDate = LocalDate.now().with(java.time.temporal.TemporalAdjusters.lastDayOfMonth());
            } else {
                toDate = LocalDate.parse(to);
            }

            if (toDate.isBefore(fromDate)) {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body(Map.of("success", false, "message", "結束日不可早於起始日"));
            }
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", adminFinanceService.getSettlement(fromDate, toDate)));
        } catch (java.time.format.DateTimeParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "日期格式錯誤，請使用 YYYY-MM-DD"));
        } catch (Exception e) {
            log.error("查詢手續費明細失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢手續費明細失敗"));
        }
    }
}
