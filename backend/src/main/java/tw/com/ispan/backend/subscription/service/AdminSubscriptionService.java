package tw.com.ispan.backend.subscription.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.subscription.entity.MembershipPlan;
import tw.com.ispan.backend.subscription.entity.OrganizerSubscription;
import tw.com.ispan.backend.subscription.entity.SaasFeature;
import tw.com.ispan.backend.subscription.enums.SubscriptionStatus;
import tw.com.ispan.backend.subscription.repository.MembershipPlanRepository;
import tw.com.ispan.backend.subscription.repository.OrganizerSubscriptionRepository;
import tw.com.ispan.backend.subscription.repository.SaasFeatureRepository;

/**
 * Admin 後台「訂閱方案」業務邏輯（批次 3）。
 *
 * <p>
 * 重用 subscription package 的 MembershipPlan / OrganizerSubscription /
 * SaasFeature。
 * 前端 PlanManage 期望的欄位（billingCycle / price / features[字串] / subscriberCount）
 * 與後端實體（annualFee / defaultFeeRate / plan_feature 多對多）不一致，於 service 層做轉換對齊：
 * </p>
 * <ul>
 * <li>billingCycle 反推：annualFee==null → CUSTOM；annualFee==0 → FREE；其餘 →
 * YEARLY。</li>
 * <li>price = annualFee。</li>
 * <li>features = plan_feature 關聯的 SaasFeature.description 清單。儲存時，
 * 前端傳入的字串若不對應既有 feature 描述則略過（feature 主檔不在此頁建立）。</li>
 * <li>subscriberCount = 該方案 ACTIVE 訂閱數。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSubscriptionService {

    private final MembershipPlanRepository planRepository;
    private final OrganizerSubscriptionRepository subscriptionRepository;
    private final SaasFeatureRepository featureRepository;

    // ── 方案 ──

    private String billingCycle(MembershipPlan p) {
        if (p.getAnnualFee() == null) {
            return "CUSTOM";
        }
        if (p.getAnnualFee().compareTo(BigDecimal.ZERO) == 0) {
            return "FREE";
        }
        return "YEARLY";
    }

    private Map<String, Object> toPlanDto(MembershipPlan p, Map<String, Long> activeCounts) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", p.getPlanId());
        m.put("name", p.getPlanName());
        m.put("billingCycle", billingCycle(p));
        m.put("price", p.getAnnualFee()); // CUSTOM 時為 null，前端顯示「客製報價」
        m.put("isActive", p.getIsActive());
        m.put("description", p.getDescription());

        // 系統預定義功能 ID 列表
        Set<String> systemIds = Set.of(
                "EVENT_PUBLISH", "BASIC_ANALYTICS", "MERCH_STORE", "PROMO_CODE",
                "EVENT_TOP", "ADVANCED_ANALYTICS", "DEDICATED_SUPPORT", "CUSTOM_CONTRACT");

        // 分離系統功能與行銷展示特色
        List<String> systemFeatures = p.getFeatures().stream()
                .filter(f -> f != null && systemIds.contains(f.getFeatureId()))
                .sorted((f1, f2) -> {
                    String id1 = f1.getFeatureId();
                    String id2 = f2.getFeatureId();
                    if (id1 == null)
                        return id2 == null ? 0 : -1;
                    if (id2 == null)
                        return 1;
                    return id1.compareTo(id2);
                })
                .map(f -> f.getFeatureId())
                .collect(Collectors.toList());

        List<String> marketingHighlights = p.getFeatures().stream()
                .filter(f -> f != null && !systemIds.contains(f.getFeatureId()))
                .sorted((f1, f2) -> {
                    String id1 = f1.getFeatureId();
                    String id2 = f2.getFeatureId();
                    if (id1 == null)
                        return id2 == null ? 0 : -1;
                    if (id2 == null)
                        return 1;
                    return id1.compareTo(id2);
                })
                .map(f -> f.getDescription())
                .collect(Collectors.toList());

        if (marketingHighlights.isEmpty()) {
            if ("FREE".equals(p.getPlanId())) {
                marketingHighlights = List.of(
                        "可同時發布 5 場活動",
                        "單場參與人數上限 500 人",
                        "可靈活自訂的報名表單",
                        "電子票券 QR Code 驗票",
                        "全站 SSL 安全加密");
            } else if ("ANNUAL".equals(p.getPlanId())) {
                marketingHighlights = List.of(
                        "可同時發布 20 場活動",
                        "單場參與人數上限 30,000 人",
                        "活動邀請函功能 (批次發送Email)",
                        "報名表單支援上傳自訂檔案",
                        "可開通信託金流代收服務",
                        "支援提前提領部分票款功能");
            } else if ("CUSTOM".equals(p.getPlanId())) {
                marketingHighlights = List.of(
                        "同時發布 20 場以上活動",
                        "單場報名人數超過 30,000 人",
                        "專屬售票頁面與伺服器頻寬保障",
                        "提供客製化金流與 API 串接",
                        "1對1 客服窗口與現場活動支援");
            }
        }

        m.put("systemFeatures", systemFeatures);
        m.put("marketingHighlights", marketingHighlights);

        // 依 featureId 穩定排序：getFeatures() 為無序 Set，未排序會導致前端每次載入順序隨機跳動。
        m.put("features", p.getFeatures().stream()
                .filter(f -> f != null)
                .sorted((f1, f2) -> {
                    String id1 = f1.getFeatureId();
                    String id2 = f2.getFeatureId();
                    if (id1 == null)
                        return id2 == null ? 0 : -1;
                    if (id2 == null)
                        return 1;
                    return id1.compareTo(id2);
                })
                .map(f -> f.getDescription())
                .filter(d -> d != null)
                .collect(Collectors.toList()));
        m.put("subscriberCount", activeCounts.getOrDefault(p.getPlanId(), 0L));
        return m;
    }

    /** 各方案 ACTIVE 訂閱數。 */
    private Map<String, Long> activeSubscriberCounts() {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (OrganizerSubscription s : subscriptionRepository.findAll()) {
            if (s.getStatusCode() == SubscriptionStatus.ACTIVE && s.getPlan() != null) {
                counts.compute(s.getPlan().getPlanId(), (k, v) -> v == null ? 1L : v + 1L);
            }
        }
        return counts;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listPlans() {
        Map<String, Long> counts = activeSubscriberCounts();
        return planRepository.findAll().stream()
                .map(p -> toPlanDto(p, counts))
                .collect(Collectors.toList());
    }

    /** 將前端傳入的功能描述字串對應回既有 SaasFeature（找不到者自動建立）。 */
    private Set<SaasFeature> resolveFeatures(List<String> featureLabels) {
        Set<SaasFeature> result = new LinkedHashSet<>();
        if (featureLabels == null || featureLabels.isEmpty()) {
            return result;
        }
        List<SaasFeature> all = featureRepository.findAll();
        for (String label : featureLabels) {
            if (label == null || label.isBlank()) {
                continue;
            }
            String trimmedLabel = label.trim();
            java.util.Optional<SaasFeature> existingOpt = all.stream()
                    .filter(f -> trimmedLabel.equals(f.getDescription()))
                    .findFirst();
            if (existingOpt.isPresent()) {
                result.add(existingOpt.get());
            } else {
                // 找不到，自動建立新的 feature 主檔
                String newFeatureId = "FEAT_" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + "_"
                        + (int) (Math.random() * 1000);
                SaasFeature newFeat = SaasFeature.builder()
                        .featureId(newFeatureId)
                        .description(trimmedLabel)
                        .isActive(true)
                        .createdAt(LocalDateTime.now())
                        .build();
                featureRepository.save(newFeat);
                result.add(newFeat);
                all.add(newFeat); // 避免同次批次處理時重複建立
                log.info("自動建立新功能標籤: {} ({})", trimmedLabel, newFeatureId);
            }
        }
        return result;
    }

    /** annualFee / billingCycle 反推：CUSTOM → null；FREE → 0；其餘採 price。 */
    private BigDecimal annualFeeFrom(String billingCycle, BigDecimal price) {
        if ("CUSTOM".equals(billingCycle)) {
            return null;
        }
        if ("FREE".equals(billingCycle)) {
            return BigDecimal.ZERO;
        }
        return price != null ? price : BigDecimal.ZERO;
    }

    @Transactional
    public Map<String, Object> createPlan(Map<String, Object> body) {
        String name = str(body.get("name"));
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("方案名稱不可為空");
        }
        String billingCycle = str(body.get("billingCycle"));
        BigDecimal price = num(body.get("price"));

        // 產生方案 ID（plan_id 為 30 字內字串主鍵，無序列；以 PLAN_ 時戳避免衝突）
        String planId = "PLAN_" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();

        Set<SaasFeature> resolvedFeatures;
        if (body.containsKey("systemFeatures") || body.containsKey("marketingHighlights")) {
            List<String> systemFeatures = toStringList(body.get("systemFeatures"));
            List<String> marketingHighlights = toStringList(body.get("marketingHighlights"));
            resolvedFeatures = resolveFeaturesSeparate(systemFeatures, marketingHighlights);
        } else {
            resolvedFeatures = resolveFeatures(toStringList(body.get("features")));
        }
        guardSystemFeatures(planId, resolvedFeatures);

        MembershipPlan p = MembershipPlan.builder()
                .planId(planId)
                .planName(name.trim())
                .annualFee(annualFeeFrom(billingCycle, price))
                .description(str(body.get("description")))
                .isActive(bool(body.get("isActive"), true))
                .features(resolvedFeatures)
                .build();
        planRepository.save(p);
        log.info("Admin 建立訂閱方案 {}", planId);

        Map<String, Long> counts = activeSubscriberCounts();
        return toPlanDto(p, counts);
    }

    @Transactional
    public Map<String, Object> updatePlan(String planId, Map<String, Object> body) {
        MembershipPlan p = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("找不到方案: " + planId));

        if (body.containsKey("name") && str(body.get("name")) != null) {
            p.setPlanName(str(body.get("name")).trim());
        }
        if (body.containsKey("billingCycle") || body.containsKey("price")) {
            String billingCycle = body.containsKey("billingCycle") ? str(body.get("billingCycle")) : billingCycle(p);
            BigDecimal price = body.containsKey("price") ? num(body.get("price")) : p.getAnnualFee();
            p.setAnnualFee(annualFeeFrom(billingCycle, price));
        }
        if (body.containsKey("description")) {
            p.setDescription(str(body.get("description")));
        }
        if (body.containsKey("isActive")) {
            p.setIsActive(bool(body.get("isActive"), p.getIsActive()));
        }
        if (body.containsKey("systemFeatures") || body.containsKey("marketingHighlights")) {
            List<String> systemFeatures = toStringList(body.get("systemFeatures"));
            List<String> marketingHighlights = toStringList(body.get("marketingHighlights"));
            Set<SaasFeature> resolvedFeatures = resolveFeaturesSeparate(systemFeatures, marketingHighlights);
            guardSystemFeatures(planId, resolvedFeatures);
            p.setFeatures(resolvedFeatures);
        } else if (body.containsKey("features")) {
            Set<SaasFeature> resolvedFeatures = resolveFeatures(toStringList(body.get("features")));
            guardSystemFeatures(planId, resolvedFeatures);
            p.setFeatures(resolvedFeatures);
        }
        planRepository.save(p);
        log.info("Admin 更新訂閱方案 {}", planId);

        Map<String, Long> counts = activeSubscriberCounts();
        return toPlanDto(p, counts);
    }

    @Transactional
    public void deletePlan(String planId) {
        MembershipPlan p = planRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("找不到方案: " + planId));
        // 有 ACTIVE 訂閱者不允許刪除，避免孤兒訂閱
        boolean inUse = subscriptionRepository.findAll().stream()
                .anyMatch(s -> s.getPlan() != null && planId.equals(s.getPlan().getPlanId())
                        && s.getStatusCode() == SubscriptionStatus.ACTIVE);
        if (inUse) {
            throw new IllegalArgumentException("此方案仍有訂閱中的組織，請先下架或轉移後再刪除");
        }
        planRepository.delete(p);
        log.info("Admin 刪除訂閱方案 {}", planId);
    }

    // ── 各組織訂閱狀態總覽 ──

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listOrganizationSubscriptions() {
        List<Map<String, Object>> result = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime soon = now.plusDays(30);

        for (OrganizerSubscription s : subscriptionRepository.findAll()) {
            String planName = s.getPlan() != null ? s.getPlan().getPlanName() : "—";
            boolean isFree = s.getPlan() != null && s.getPlan().getAnnualFee() != null
                    && s.getPlan().getAnnualFee().compareTo(BigDecimal.ZERO) == 0;

            // 衍生前端狀態：FREE / EXPIRED / EXPIRING / ACTIVE
            String status;
            if (s.getStatusCode() == SubscriptionStatus.EXPIRED) {
                status = "EXPIRED";
            } else if (isFree) {
                status = "FREE";
            } else if (s.getEndDate() != null && s.getEndDate().isBefore(now)) {
                status = "EXPIRED";
            } else if (s.getEndDate() != null && s.getEndDate().isBefore(soon)) {
                status = "EXPIRING";
            } else {
                status = "ACTIVE";
            }

            // upgrade_type 0=ANNUAL_FEE/1=CUMULATIVE/2=MANUAL；年費型視為自動續約
            boolean autoRenew = s.getUpgradeType() != null
                    && "ANNUAL_FEE".equals(s.getUpgradeType().name());

            Map<String, Object> m = new LinkedHashMap<>();
            m.put("orgId", s.getOrganizer() != null ? s.getOrganizer().getOrganizerId() : null);
            m.put("orgName", s.getOrganizer() != null ? s.getOrganizer().getName() : "—");
            m.put("planName", planName);
            m.put("status", status);
            m.put("startedAt", s.getStartDate());
            m.put("expiresAt", s.getEndDate());
            m.put("autoRenew", autoRenew);
            result.add(m);
        }
        return result;
    }

    // ── 小工具 ──

    private String str(Object o) {
        return o != null ? o.toString() : null;
    }

    private BigDecimal num(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(o.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private boolean bool(Object o, boolean dflt) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o != null) {
            return Boolean.parseBoolean(o.toString());
        }
        return dflt;
    }

    @SuppressWarnings("unchecked")
    private List<String> toStringList(Object o) {
        if (o instanceof List<?> list) {
            return ((List<Object>) list).stream()
                    .map(x -> x != null ? x.toString() : null)
                    .collect(Collectors.toList());
        }
        return List.of();
    }

    private void guardSystemFeatures(String planId, Set<SaasFeature> resolvedFeatures) {
        if (planId == null)
            return;
        List<SaasFeature> allFeatures = featureRepository.findAll();

        if ("FREE".equals(planId)) {
            ensureFeature(resolvedFeatures, allFeatures, "EVENT_PUBLISH");
            ensureFeature(resolvedFeatures, allFeatures, "BASIC_ANALYTICS");
        } else if ("ANNUAL".equals(planId)) {
            ensureFeature(resolvedFeatures, allFeatures, "EVENT_PUBLISH");
            ensureFeature(resolvedFeatures, allFeatures, "BASIC_ANALYTICS");
            ensureFeature(resolvedFeatures, allFeatures, "MERCH_STORE");
            ensureFeature(resolvedFeatures, allFeatures, "PROMO_CODE");
        } else if ("CUSTOM".equals(planId)) {
            ensureFeature(resolvedFeatures, allFeatures, "EVENT_PUBLISH");
            ensureFeature(resolvedFeatures, allFeatures, "BASIC_ANALYTICS");
            ensureFeature(resolvedFeatures, allFeatures, "MERCH_STORE");
            ensureFeature(resolvedFeatures, allFeatures, "PROMO_CODE");
            ensureFeature(resolvedFeatures, allFeatures, "EVENT_TOP");
            ensureFeature(resolvedFeatures, allFeatures, "ADVANCED_ANALYTICS");
            ensureFeature(resolvedFeatures, allFeatures, "DEDICATED_SUPPORT");
            ensureFeature(resolvedFeatures, allFeatures, "CUSTOM_CONTRACT");
        }
    }

    private void ensureFeature(Set<SaasFeature> resolved, List<SaasFeature> all, String featureId) {
        boolean exists = resolved.stream().anyMatch(f -> featureId.equalsIgnoreCase(f.getFeatureId()));
        if (!exists) {
            all.stream()
                    .filter(f -> featureId.equalsIgnoreCase(f.getFeatureId()))
                    .findFirst()
                    .ifPresent(resolved::add);
        }
    }

    private Set<SaasFeature> resolveFeaturesSeparate(List<String> systemFeatures, List<String> marketingHighlights) {
        Set<SaasFeature> result = new LinkedHashSet<>();
        List<SaasFeature> all = featureRepository.findAll();

        // 1. 處理選中的系統核心功能
        if (systemFeatures != null) {
            for (String fid : systemFeatures) {
                if (fid == null || fid.isBlank())
                    continue;
                all.stream()
                        .filter(f -> fid.equalsIgnoreCase(f.getFeatureId()))
                        .findFirst()
                        .ifPresent(result::add);
            }
        }

        // 2. 處理自訂的行銷展示項目
        if (marketingHighlights != null) {
            for (String desc : marketingHighlights) {
                if (desc == null || desc.isBlank())
                    continue;
                String trimmedDesc = desc.trim();

                // 尋找既有描述符合者，避免重複建立
                java.util.Optional<SaasFeature> existingOpt = all.stream()
                        .filter(f -> trimmedDesc.equals(f.getDescription()))
                        .findFirst();
                if (existingOpt.isPresent()) {
                    result.add(existingOpt.get());
                } else {
                    // 自動建立為純行銷展示用的 Feature 主檔
                    String newFeatureId = "MKT_" + Long.toString(System.currentTimeMillis(), 36).toUpperCase() + "_"
                            + (int) (Math.random() * 1000);
                    SaasFeature newFeat = SaasFeature.builder()
                            .featureId(newFeatureId)
                            .description(trimmedDesc)
                            .isActive(true)
                            .createdAt(LocalDateTime.now())
                            .build();
                    featureRepository.save(newFeat);
                    result.add(newFeat);
                    all.add(newFeat);
                }
            }
        }
        return result;
    }
}
