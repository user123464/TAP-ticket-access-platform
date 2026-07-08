package tw.com.ispan.backend.settlements.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository; // Jason added
import tw.com.ispan.backend.orderTicket.repository.TicketOrderDetailRepository;
import tw.com.ispan.backend.organizer.entity.Contract;
import tw.com.ispan.backend.organizer.enums.ContractStatus;
import tw.com.ispan.backend.organizer.enums.FeeType;
import tw.com.ispan.backend.organizer.repository.ContractRepository;
import tw.com.ispan.backend.settlements.dto.FinanceDashboardDto;
import tw.com.ispan.backend.settlements.dto.SettlementRowDto;
import tw.com.ispan.backend.subscription.enums.SubscriptionStatus;
import tw.com.ispan.backend.subscription.repository.OrganizerSubscriptionRepository;

/**
 * Admin 後台「財務結算」業務邏輯（批次 2 新建）。
 *
 * <p><b>資料來源與簡化說明</b>：</p>
 * <ul>
 *   <li>交易額（GMV）：票務 + 週邊商品雙金流即時彙總。
 *       票務路徑：TicketOrderDetail d → 票券 t → 場次 s → 主題 th → 組織 o，
 *       金額取 {@code unitPrice}，時間以父訂單 {@code createAt} 篩選。
 *       週邊路徑（Jason added）：MerchOrderDetail d → Product p → Theme th → 組織 o，
 *       金額取 {@code unitPrice × quantity}，時間以 merch_orders.createdAt 篩選；
 *       item_status=NORMAL（排除退貨）。
 *       未使用 {@code merchant_settlements}（撥款紀錄），Admin 需要「即時應收」。</li>
 *   <li>手續費：依各組織「生效中(ACTIVE)合約」的 {@code feeType + feeValue} 計算；
 *       PERCENTAGE → GMV × value%；FIXED_PER_TICKET → 筆數 × value。無生效合約者
 *       退回平台預設 5% 抽成，確保報表不漏列。</li>
 *   <li>訂閱摘要：以 {@code organizer_subscription} 統計 ACTIVE / 30 天內到期 / 已到期(EXPIRED)。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminFinanceService {

    private final TicketOrderDetailRepository ticketOrderDetailRepository;
    private final MerchOrderRepository merchOrderRepository; // Jason added: 週邊訂單彙總
    private final ContractRepository contractRepository;
    private final OrganizerSubscriptionRepository organizerSubscriptionRepository;

    private static final BigDecimal DEFAULT_PERCENTAGE = new BigDecimal("5"); // 無合約時的預設抽成 %

    // ── 儀表板 ──

    @Transactional(readOnly = true)
    public FinanceDashboardDto getDashboard(int months) {
        int span = (months <= 0) ? 6 : (months > 24 ? 24 : months);
        YearMonth thisMonth = YearMonth.now();
        YearMonth firstMonth = thisMonth.minusMonths(span - 1L);
        LocalDateTime from = firstMonth.atDay(1).atStartOfDay();
        LocalDateTime to = thisMonth.plusMonths(1).atDay(1).atStartOfDay(); // 含本月

        // 月趨勢：票務 + 週邊雙金流合計（在 Java 端彙總，避免 DB 廠商特定日期函式）
        Map<YearMonth, BigDecimal> monthlyGmv = new HashMap<>();
        // 票務明細月趨勢
        for (Object[] row : ticketOrderDetailRepository.findOrderDateAndAmount(from, to)) {
            LocalDateTime createdAt = (LocalDateTime) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (createdAt == null || amount == null) continue;
            YearMonth ym = YearMonth.from(createdAt);
            monthlyGmv.merge(ym, amount, (v1, v2) -> v1.add(v2));
        }
        // Jason added: 週邊訂單明細月趨勢（unitPrice × quantity）
        for (Object[] row : merchOrderRepository.findMerchOrderDateAndAmount(from, to)) {
            LocalDateTime createdAt = (LocalDateTime) row[0];
            BigDecimal amount = (BigDecimal) row[1];
            if (createdAt == null || amount == null) continue;
            YearMonth ym = YearMonth.from(createdAt);
            monthlyGmv.merge(ym, amount, (v1, v2) -> v1.add(v2));
        }
        List<String> trendLabels = new ArrayList<>();
        List<BigDecimal> trendData = new ArrayList<>();
        for (int i = 0; i < span; i++) {
            YearMonth ym = firstMonth.plusMonths(i);
            trendLabels.add(ym.getMonthValue() + "月");
            trendData.add(monthlyGmv.getOrDefault(ym, BigDecimal.ZERO));
        }

        // 本月各組織 GMV + 手續費（甜甜圈 + 統計卡）
        LocalDateTime monthFrom = thisMonth.atDay(1).atStartOfDay();
        LocalDateTime monthTo = thisMonth.plusMonths(1).atDay(1).atStartOfDay();
        Map<String, FeeRule> feeRules = buildFeeRuleMap();

        // Jason added: 以 orgId 為 key 合併票務 + 週邊 GMV（count 分別加總）
        Map<String, long[]> orgCombined = new HashMap<>();   // [ticketCount, totalCount]
        Map<String, BigDecimal> orgGmvMap = new HashMap<>();
        Map<String, String> orgNameMap = new HashMap<>();

        // 票務彙總
        for (Object[] row : ticketOrderDetailRepository.aggregateGmvByOrganizer(monthFrom, monthTo)) {
            String orgId = (String) row[0];
            String orgName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            BigDecimal gmv = (BigDecimal) row[3];
            orgNameMap.put(orgId, orgName);
            orgCombined.merge(orgId, new long[]{count}, (a, b) -> new long[]{a[0] + b[0]});
            orgGmvMap.merge(orgId, gmv, (v1, v2) -> v1.add(v2));
        }
        // Jason added: 週邊 GMV 併入同一組織的累計
        for (Object[] row : merchOrderRepository.aggregateMerchGmvByOrganizer(monthFrom, monthTo)) {
            String orgId = (String) row[0];
            String orgName = (String) row[1];
            long count = ((Number) row[2]).longValue();
            BigDecimal gmv = (BigDecimal) row[3];
            orgNameMap.put(orgId, orgName);
            orgCombined.merge(orgId, new long[]{count}, (a, b) -> new long[]{a[0] + b[0]});
            orgGmvMap.merge(orgId, gmv, (v1, v2) -> v1.add(v2));
        }

        // 依 GMV 降序排序後產生甜甜圈 + 統計卡數字
        List<Map.Entry<String, BigDecimal>> sortedEntries = new ArrayList<>(orgGmvMap.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<String> feeLabels = new ArrayList<>();
        List<BigDecimal> feeData = new ArrayList<>();
        BigDecimal monthlyFeeTotal = BigDecimal.ZERO;
        BigDecimal monthlyGmvTotal = BigDecimal.ZERO;
        int rank = 0;
        BigDecimal othersFee = BigDecimal.ZERO;
        for (Map.Entry<String, BigDecimal> entry : sortedEntries) {
            String orgId = entry.getKey();
            BigDecimal gmv = entry.getValue();
            long count = orgCombined.getOrDefault(orgId, new long[]{0})[0];
            String orgName = orgNameMap.getOrDefault(orgId, orgId);
            BigDecimal fee = calcFee(feeRules.get(orgId), gmv, count);
            monthlyFeeTotal = monthlyFeeTotal.add(fee);
            monthlyGmvTotal = monthlyGmvTotal.add(gmv);
            // 前 4 名單列，其餘併入「其他」
            if (rank < 4) {
                feeLabels.add(orgName);
                feeData.add(fee);
            } else {
                othersFee = othersFee.add(fee);
            }
            rank++;
        }
        if (othersFee.signum() > 0) {
            feeLabels.add("其他");
            feeData.add(othersFee);
        }

        return FinanceDashboardDto.builder()
                .trend(FinanceDashboardDto.SeriesDto.builder().labels(trendLabels).data(trendData).build())
                .feeShare(FinanceDashboardDto.SeriesDto.builder().labels(feeLabels).data(feeData).build())
                .monthlyFee(scale(monthlyFeeTotal))
                .monthlyGmv(scale(monthlyGmvTotal))
                .subscription(buildSubscriptionSummary())
                .build();
    }

    // ── 手續費明細 ──

    @Transactional(readOnly = true)
    public List<SettlementRowDto> getSettlement(LocalDate fromDate, LocalDate toDate) {
        LocalDateTime from = fromDate.atStartOfDay();
        LocalDateTime to = toDate.plusDays(1).atStartOfDay(); // 含結束日整天
        Map<String, FeeRule> feeRules = buildFeeRuleMap();

        // Jason added: 以 orgId 為 key 合併票務 + 週邊 GMV
        Map<String, long[]> orgCountMap = new HashMap<>();
        Map<String, BigDecimal> orgGmvMap = new HashMap<>();
        Map<String, String> orgNameMap = new HashMap<>();

        // 票務彙總
        for (Object[] row : ticketOrderDetailRepository.aggregateGmvByOrganizer(from, to)) {
            String orgId = (String) row[0];
            orgNameMap.put(orgId, (String) row[1]);
            long count = ((Number) row[2]).longValue();
            BigDecimal gmv = (BigDecimal) row[3];
            orgCountMap.merge(orgId, new long[]{count}, (a, b) -> new long[]{a[0] + b[0]});
            orgGmvMap.merge(orgId, gmv, (v1, v2) -> v1.add(v2));
        }
        // Jason added: 週邊 GMV 併入
        for (Object[] row : merchOrderRepository.aggregateMerchGmvByOrganizer(from, to)) {
            String orgId = (String) row[0];
            orgNameMap.put(orgId, (String) row[1]);
            long count = ((Number) row[2]).longValue();
            BigDecimal gmv = (BigDecimal) row[3];
            orgCountMap.merge(orgId, new long[]{count}, (a, b) -> new long[]{a[0] + b[0]});
            orgGmvMap.merge(orgId, gmv, (v1, v2) -> v1.add(v2));
        }

        // 依 GMV 降序排序後組裝回應列表
        List<Map.Entry<String, BigDecimal>> sortedEntries = new ArrayList<>(orgGmvMap.entrySet());
        sortedEntries.sort((a, b) -> b.getValue().compareTo(a.getValue()));

        List<SettlementRowDto> rows = new ArrayList<>();
        for (Map.Entry<String, BigDecimal> entry : sortedEntries) {
            String orgId = entry.getKey();
            BigDecimal gmv = entry.getValue();
            long count = orgCountMap.getOrDefault(orgId, new long[]{0})[0];
            String orgName = orgNameMap.getOrDefault(orgId, orgId);
            FeeRule rule = feeRules.get(orgId);
            BigDecimal fee = calcFee(rule, gmv, count);
            rows.add(SettlementRowDto.builder()
                    .orgId(orgId)
                    .orgName(orgName)
                    .orderCount(count)
                    .gmv(scale(gmv))
                    .feeType(rule != null ? rule.frontFeeType() : 1)
                    .feeValue(rule != null ? rule.feeValue() : DEFAULT_PERCENTAGE)
                    .fee(scale(fee))
                    .build());
        }
        return rows;
    }

    // ── helpers ──

    /** 建立 organizerId → 生效合約費率規則。 */
    private Map<String, FeeRule> buildFeeRuleMap() {
        Map<String, FeeRule> map = new HashMap<>();
        for (Contract c : contractRepository.findByContractStatusWithOrganizer(ContractStatus.ACTIVE)) {
            if (c.getOrganizer() == null) continue;
            map.putIfAbsent(c.getOrganizer().getOrganizerId(),
                    new FeeRule(c.getFeeType(), c.getFeeValue()));
        }
        return map;
    }

    private BigDecimal calcFee(FeeRule rule, BigDecimal gmv, long count) {
        if (gmv == null) gmv = BigDecimal.ZERO;
        if (rule == null) {
            // 無合約：預設 5% 抽成
            return gmv.multiply(DEFAULT_PERCENTAGE).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        if (rule.feeType() == FeeType.FIXED_PER_TICKET) {
            return rule.feeValue().multiply(BigDecimal.valueOf(count));
        }
        return gmv.multiply(rule.feeValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
    }

    private FinanceDashboardDto.SubscriptionSummary buildSubscriptionSummary() {
        long active = organizerSubscriptionRepository.findAll().stream()
                .filter(s -> s.getStatusCode() == SubscriptionStatus.ACTIVE)
                .count();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime in30 = now.plusDays(30);
        long expiringSoon = organizerSubscriptionRepository
                .findByStatusCodeAndEndDateBefore(SubscriptionStatus.ACTIVE, in30).stream()
                .filter(s -> s.getEndDate() != null && s.getEndDate().isAfter(now))
                .count();
        long expired = organizerSubscriptionRepository.findAll().stream()
                .filter(s -> s.getStatusCode() == SubscriptionStatus.EXPIRED)
                .count();
        return FinanceDashboardDto.SubscriptionSummary.builder()
                .active(active)
                .expiringSoon(expiringSoon)
                .expired(expired)
                .build();
    }

    private BigDecimal scale(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    /** organizer 的費率規則；frontFeeType 1=百分比 / 2=固定。 */
    private record FeeRule(FeeType feeType, BigDecimal feeValue) {
        int frontFeeType() {
            return feeType == FeeType.FIXED_PER_TICKET ? 2 : 1;
        }
    }
}
