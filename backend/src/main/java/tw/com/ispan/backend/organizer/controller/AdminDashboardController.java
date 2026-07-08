package tw.com.ispan.backend.organizer.controller;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
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
import tw.com.ispan.backend.login.repository.LoginAttemptRepository;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.settlements.dto.FinanceDashboardDto;
import tw.com.ispan.backend.settlements.service.AdminFinanceService;

/**
 * Admin 後台儀表板統計端點。
 *
 * <p>地基批次：聚合平台層級的核心統計數字，供 frontend-admin 的
 * {@code DashboardView.vue} 顯示統計卡片。全部以既有 repository 的 count 查詢實作，
 * 不新增 service。</p>
 *
 * <p>授權 {@code DASHBOARD_VIEW}（進入後台看平台概況的最低門檻，與「使用者管理」的
 * USER_VIEW 解耦；data.sql 已掛在 ADMIN / SUPER_ADMIN / CUSTOMER_SERVICE，其餘自訂內部
 * 角色可自行勾選）。回傳欄位對齊前端 {@code stats}：userCount / orgCount /
 * pendingKycCount / todayLoginCount，其餘前端有預設值的欄位（手續費、客訴…）
 * 屬後續批次，這裡先給 0 佔位避免前端 undefined。</p>
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final OrganizerRepository organizerRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final AdminFinanceService adminFinanceService;

    @GetMapping("/stats")
    @PreAuthorize("hasAuthority('DASHBOARD_VIEW')")
    public ResponseEntity<?> getStats(@RequestParam(value = "months", defaultValue = "6") int months) {
        log.info("API 管理員查詢儀表板統計 months={}", months);
        try {
            long userCount = userRepository.count();
            long orgCount = organizerRepository.count();
            long pendingKycCount = organizerRepository.countByKycStatus(KycStatus.PENDING);
            long approvedOrgCount = organizerRepository.countByKycStatus(KycStatus.APPROVED);
            LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
            long todayLoginCount = loginAttemptRepository.countBySuccessTrueAndAttemptedAtAfter(startOfToday);

            // 財務數據重用 AdminFinanceService（與 /finance 同源），避免 dashboard 另接一套或顯示假值
            FinanceDashboardDto finance = adminFinanceService.getDashboard(months);

            // 使用 LinkedHashMap 以容許 0 值並保留欄位順序（Map.of 不可放重複/特定情境較不彈性）
            Map<String, Object> stats = new LinkedHashMap<>();
            stats.put("userCount", userCount);
            stats.put("orgCount", orgCount);
            stats.put("pendingKycCount", pendingKycCount);
            stats.put("approvedOrgCount", approvedOrgCount);
            stats.put("todayLoginCount", todayLoginCount);
            // 真實財務：本月應收手續費 + 月交易額趨勢（折線）+ 訂閱狀態總覽
            stats.put("monthlyFee", finance.getMonthlyFee());
            stats.put("trend", finance.getTrend());
            stats.put("subscription", finance.getSubscription());
            // 以下欄位屬後續客服 / 合約批次，先給 0 佔位讓前端卡片有值
            stats.put("pendingSubmissionCount", 0);
            stats.put("expiringContractCount", 0);

            return ResponseEntity.ok(stats);
        } catch (Exception e) {
            log.error("查詢儀表板統計失敗", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "查詢統計資料失敗"));
        }
    }
}
