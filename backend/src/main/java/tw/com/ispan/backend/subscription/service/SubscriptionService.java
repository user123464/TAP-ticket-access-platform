package tw.com.ispan.backend.subscription.service;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import jakarta.persistence.EntityManager;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.organizer.service.OrganizerService;
import tw.com.ispan.backend.payment.ecpay.EcpayPaymentService;
import tw.com.ispan.backend.subscription.dto.MembershipPlanDto;
import tw.com.ispan.backend.subscription.dto.OrganizerSubscriptionDto;
import tw.com.ispan.backend.subscription.entity.MembershipPlan;
import tw.com.ispan.backend.subscription.entity.OrganizerSubscription;
import tw.com.ispan.backend.subscription.enums.SubscriptionStatus;
import tw.com.ispan.backend.subscription.enums.SubscriptionUpgradeType;
import tw.com.ispan.backend.subscription.repository.MembershipPlanRepository;
import tw.com.ispan.backend.subscription.repository.OrganizerSubscriptionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionService {

    /** Redis 待付款意圖前綴：值為 "organizerId|planId"，TTL 1 小時 */
    private static final String INTENT_KEY_PREFIX = "SubPayIntent:";
    /** Redis 已處理付款冪等鎖前綴，避免綠界重送造成重複開通 */
    private static final String PROCESSED_KEY_PREFIX = "SubPaidProcessed:";
    /** 年費付款交易編號前綴 (SaaS Annual Payment)，與 MAP/T 區隔 */
    private static final String TRADE_NO_PREFIX = "SAP";

    private final OrganizerSubscriptionRepository organizerSubscriptionRepository;
    private final MembershipPlanRepository membershipPlanRepository;
    private final OrganizerRepository organizerRepository;
    private final OrganizerService organizerService;
    private final EntityManager entityManager;
    private final EcpayPaymentService ecpayPaymentService;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * 獲取所有有效訂閱方案及其功能列表
     */
    @Transactional(readOnly = true)
    public List<MembershipPlanDto> getAllPlans() {
        log.info("查詢所有有效訂閱方案");
        return membershipPlanRepository.findAll().stream()
                .filter(plan -> Boolean.TRUE.equals(plan.getIsActive()))
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    /**
     * 獲取主辦方當前生效的訂閱。若無任何訂閱，則自動為其初始化一個預設的 BRONZE (免費級) 訂閱。
     */
    @Transactional
    public OrganizerSubscriptionDto getActiveSubscription(String organizerId) {
        log.info("查詢主辦方活躍訂閱，OrganizerId: {}", organizerId);
        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該主辦方: " + organizerId));

        Optional<OrganizerSubscription> activeSubOpt = organizerSubscriptionRepository
                .findByOrganizerAndStatusCode(organizer, SubscriptionStatus.ACTIVE);

        if (activeSubOpt.isEmpty()) {
            log.info("主辦方 {} 尚無活躍訂閱，自動初始化預設 FREE 訂閱", organizerId);
            MembershipPlan freePlan = membershipPlanRepository.findById("FREE")
                    .orElseThrow(() -> new IllegalStateException("資料庫中缺少預設的 FREE 方案種子資料"));

            Long nextVal = organizerSubscriptionRepository.getNextSubscriptionSequenceValue();
            String subId = String.format("SUB%07d", nextVal);

            OrganizerSubscription newSub = OrganizerSubscription.builder()
                    .subscriptionId(subId)
                    .organizer(organizer)
                    .plan(freePlan)
                    .statusCode(SubscriptionStatus.ACTIVE)
                    .upgradeType(SubscriptionUpgradeType.MANUAL)
                    .startDate(LocalDateTime.now())
                    .endDate(null) // 預設免費方案永不過期
                    .build();

            OrganizerSubscription savedSub = organizerSubscriptionRepository.saveAndFlush(newSub);
            entityManager.refresh(savedSub);
            return convertToDto(savedSub);
        }

        return convertToDto(activeSubOpt.get());
    }

    /**
     * 主辦方手動變更/升級訂閱方案。
     */
    @Transactional
    public OrganizerSubscriptionDto subscribePlan(String userId, String organizerId, String planId, SubscriptionUpgradeType upgradeType) {
        log.info("主辦方升級/變更訂閱，操作者: {}, 組織: {}, 目標方案: {}, 類型: {}", userId, organizerId, planId, upgradeType);
        
        // 1. 安全校驗：校驗用戶是否為該組織的 Owner 或 ADMIN
        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        Organizer organizer = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該主辦方: " + organizerId));

        MembershipPlan targetPlan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("找不到目標訂閱方案: " + planId));

        if (!targetPlan.getIsActive()) {
            throw new IllegalStateException("該方案已被停用，無法訂閱");
        }

        return activateSubscription(organizer, targetPlan, upgradeType);
    }

    /**
     * 核心開通邏輯：將現有 ACTIVE 訂閱設為 EXPIRED，並建立一筆新的 ACTIVE 訂閱。
     *
     * <p>
     * 本方法<b>不做</b>任何權限校驗，呼叫端須自行確保來源可信：
     * {@link #subscribePlan} 在呼叫前已做 {@code verifyOwnerOrAdmin}；
     * 綠界年費付款 callback 則在建立付款意圖時已驗過擁有者，且此處以簽章＋Redis 意圖為信任依據。
     * </p>
     */
    private OrganizerSubscriptionDto activateSubscription(Organizer organizer, MembershipPlan targetPlan,
            SubscriptionUpgradeType upgradeType) {
        // 1. 將現有的 ACTIVE 訂閱設為 EXPIRED
        Optional<OrganizerSubscription> currentActiveOpt = organizerSubscriptionRepository
                .findByOrganizerAndStatusCode(organizer, SubscriptionStatus.ACTIVE);

        if (currentActiveOpt.isPresent()) {
            OrganizerSubscription current = currentActiveOpt.get();
            current.setStatusCode(SubscriptionStatus.EXPIRED);
            current.setEndDate(LocalDateTime.now());
            organizerSubscriptionRepository.save(current);
            log.info("已將原有活躍訂閱 {} 標記為過期", current.getSubscriptionId());
        }

        // 2. 建立新的訂閱紀錄
        Long nextVal = organizerSubscriptionRepository.getNextSubscriptionSequenceValue();
        String subId = String.format("SUB%07d", nextVal);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endDate = null;
        if (upgradeType == SubscriptionUpgradeType.ANNUAL_FEE) {
            endDate = now.plusYears(1); // 年費方案效期 1 年
        }

        OrganizerSubscription newSubscription = OrganizerSubscription.builder()
                .subscriptionId(subId)
                .organizer(organizer)
                .plan(targetPlan)
                .statusCode(SubscriptionStatus.ACTIVE)
                .upgradeType(upgradeType)
                .startDate(now)
                .endDate(endDate)
                .build();

        OrganizerSubscription saved = organizerSubscriptionRepository.saveAndFlush(newSubscription);
        entityManager.refresh(saved);
        log.info("訂閱升級成功，生成訂閱編號: {}", subId);

        return convertToDto(saved);
    }

    /**
     * 為主辦方年費方案建立綠界付款，回傳自動提交的 HTML 表單字串。
     *
     * <p>
     * 與 {@link #subscribePlan} 不同：此處<b>不</b>立即開通，僅在 Redis 暫存待付款意圖，
     * 待綠界 S2S callback ({@link #handleEcpayCallback}) 回傳付款成功後才真正啟用方案。
     * </p>
     */
    @Transactional(readOnly = true)
    public String createSubscriptionCheckout(String userId, String organizerId, String planId) {
        log.info("建立年費方案綠界付款，操作者: {}, 組織: {}, 目標方案: {}", userId, organizerId, planId);

        // 1. 安全校驗：僅組織 Owner 或 ADMIN 可付款升級
        organizerService.verifyOwnerOrAdmin(userId, organizerId);

        organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該主辦方: " + organizerId));

        MembershipPlan targetPlan = membershipPlanRepository.findById(planId)
                .orElseThrow(() -> new IllegalArgumentException("找不到目標訂閱方案: " + planId));

        if (!targetPlan.getIsActive()) {
            throw new IllegalStateException("該方案已被停用，無法訂閱");
        }

        BigDecimal annualFee = targetPlan.getAnnualFee();
        if (annualFee == null || annualFee.compareTo(BigDecimal.ZERO) <= 0) {
            // FREE (0) 走降級、CUSTOM (null) 走聯絡我們，皆不應進入付款流程
            throw new IllegalStateException("此方案無需付款升級（免費方案請直接切換，客製方案請來信洽詢）");
        }

        // 2. 產生交易編號並在 Redis 暫存待付款意圖 (organizerId|planId)，TTL 1 小時
        String merchantTradeNo = TRADE_NO_PREFIX + System.currentTimeMillis();
        redisTemplate.opsForValue().set(
                INTENT_KEY_PREFIX + merchantTradeNo,
                organizerId + "|" + planId,
                Duration.ofHours(1));

        // 3. 「返回商店」導回組織訂閱頁並標記為取消
        String clientBackUrl = frontendUrl + "/org/" + organizerId + "/settings/subscription?status=canceled";

        int amount = annualFee.intValue();
        String itemName = targetPlan.getPlanName() + "#1#" + amount;

        return ecpayPaymentService.generateAioCreditForm(
                merchantTradeNo, amount, "TAP_Subscription", itemName, null, clientBackUrl);
    }

    /**
     * 處理綠界年費付款的 Server-to-Server callback：驗簽通過且付款成功時才開通方案。
     */
    @Transactional
    public String handleEcpayCallback(Map<String, String> params) {
        log.info("【綠界年費回調】收到付款通知參數: {}", params);

        // 1. 驗證 CheckMacValue 簽章
        if (!ecpayPaymentService.verifyCheckMacValue(params)) {
            log.warn("【綠界年費回調】簽章驗證失敗！");
            return "0|FAIL";
        }

        String merchantTradeNo = params.get("MerchantTradeNo");
        String rtnCode = params.get("RtnCode"); // "1" 代表付款成功

        // 2. 讀取待付款意圖；查無代表過期或偽造，回 1|OK 讓綠界停止重送
        String intent = redisTemplate.opsForValue().get(INTENT_KEY_PREFIX + merchantTradeNo);
        if (intent == null) {
            log.warn("【綠界年費回調】找不到對應付款意圖（交易單號: {}），可能已過期或重複通知", merchantTradeNo);
            return "1|OK";
        }

        if (!"1".equals(rtnCode)) {
            log.warn("【綠界年費回調】付款失敗，不開通。RtnCode: {}, RtnMsg: {}", rtnCode, params.get("RtnMsg"));
            return "1|OK";
        }

        // 3. 冪等鎖：只有首次成功通知才真正開通，避免綠界重送造成重複建立 ACTIVE 訂閱
        Boolean firstTime = redisTemplate.opsForValue()
                .setIfAbsent(PROCESSED_KEY_PREFIX + merchantTradeNo, "1", Duration.ofHours(1));
        if (!Boolean.TRUE.equals(firstTime)) {
            log.info("【綠界年費回調】交易 {} 已處理過，略過重複開通", merchantTradeNo);
            return "1|OK";
        }

        String[] parts = intent.split("\\|", 2);
        String organizerId = parts[0];
        String planId = parts.length > 1 ? parts[1] : null;

        Organizer organizer = organizerRepository.findById(organizerId).orElse(null);
        MembershipPlan targetPlan = planId == null ? null : membershipPlanRepository.findById(planId).orElse(null);
        if (organizer == null || targetPlan == null) {
            log.warn("【綠界年費回調】意圖資料失效，organizerId: {}, planId: {}", organizerId, planId);
            return "1|OK";
        }

        activateSubscription(organizer, targetPlan, SubscriptionUpgradeType.ANNUAL_FEE);
        log.info("【綠界年費回調】組織 {} 已完成年費方案 {} 開通", organizerId, planId);
        return "1|OK";
    }

    /**
     * 供綠界瀏覽器重定向使用：以交易編號讀 Redis 意圖取得 organizerId，以組回組織訂閱頁網址。
     */
    public String getOrganizerIdByTradeNo(String merchantTradeNo) {
        String intent = redisTemplate.opsForValue().get(INTENT_KEY_PREFIX + merchantTradeNo);
        if (intent == null) {
            return null;
        }
        return intent.split("\\|", 2)[0];
    }

    @Transactional(readOnly = true)
    public boolean hasFeature(String organizerId, String featureId) {
        Organizer organizer = organizerRepository.findById(organizerId).orElse(null);
        if (organizer == null || organizer.getKycStatus() != tw.com.ispan.backend.organizer.enums.KycStatus.APPROVED) {
            log.warn("主辦方 {} 尚未通過 KYC 審查，無權使用任何特徵功能", organizerId);
            return false;
        }

        // 使用 getActiveSubscription 可保證無訂閱時會自動初始化預設方案，防止 null 指針
        OrganizerSubscriptionDto activeSub = getActiveSubscription(organizerId);
        if (activeSub == null) {
            return false;
        }
        MembershipPlan plan = membershipPlanRepository.findById(activeSub.getPlanId()).orElse(null);
        if (plan == null) {
            return false;
        }
        return plan.getFeatures().stream()
                .anyMatch(feat -> feat.getFeatureId().equalsIgnoreCase(featureId) && feat.getIsActive());
    }

    private MembershipPlanDto convertToDto(MembershipPlan plan) {
        Set<String> featureDescriptions = plan.getFeatures().stream()
                .filter(feat -> feat.getIsActive() && feat.getDescription() != null)
                .map(feat -> feat.getDescription())
                .collect(Collectors.toSet());

        return MembershipPlanDto.builder()
                .planId(plan.getPlanId())
                .planName(plan.getPlanName())
                .annualFee(plan.getAnnualFee())
                .cumulativeThreshold(plan.getCumulativeThreshold())
                .defaultFeeRate(plan.getDefaultFeeRate())
                .description(plan.getDescription())
                .isActive(plan.getIsActive())
                .features(featureDescriptions)
                .marketingHighlights(getMarketingHighlights(plan))
                .build();
    }

    private OrganizerSubscriptionDto convertToDto(OrganizerSubscription sub) {
        return OrganizerSubscriptionDto.builder()
                .subscriptionId(sub.getSubscriptionId())
                .organizerId(sub.getOrganizer().getOrganizerId())
                .planId(sub.getPlan().getPlanId())
                .planName(sub.getPlan().getPlanName())
                .statusCode(sub.getStatusCode().name())
                .upgradeType(sub.getUpgradeType().name())
                .startDate(sub.getStartDate())
                .endDate(sub.getEndDate())
                .build();
    }

    private List<String> getMarketingHighlights(MembershipPlan p) {
        Set<String> systemIds = Set.of(
            "EVENT_PUBLISH", "BASIC_ANALYTICS", "MERCH_STORE", "PROMO_CODE",
            "EVENT_TOP", "ADVANCED_ANALYTICS", "DEDICATED_SUPPORT", "CUSTOM_CONTRACT"
        );

        List<String> highlights = p.getFeatures().stream()
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

        if (highlights.isEmpty()) {
            if ("FREE".equals(p.getPlanId())) {
                return List.of(
                    "可同時發布 5 場活動",
                    "單場參與人數上限 500 人",
                    "可靈活自訂的報名表單",
                    "電子票券 QR Code 驗票",
                    "全站 SSL 安全加密"
                );
            } else if ("ANNUAL".equals(p.getPlanId())) {
                return List.of(
                    "可同時發布 20 場活動",
                    "單場參與人數上限 30,000 人",
                    "活動邀請函功能 (批次發送Email)",
                    "報名表單支援上傳自訂檔案",
                    "可開通信託金流代收服務",
                    "支援提前提領部分票款功能"
                );
            } else if ("CUSTOM".equals(p.getPlanId())) {
                return List.of(
                    "同時發布 20 場以上活動",
                    "單場報名人數超過 30,000 人",
                    "專屬售票頁面與伺服器頻寬保障",
                    "提供客製化金流與 API 串接",
                    "1對1 客服窗口與現場活動支援"
                );
            }
        }
        return highlights;
    }
}
