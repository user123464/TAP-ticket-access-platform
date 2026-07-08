package tw.com.ispan.backend.subscription;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.EntityManager;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.subscription.dto.MembershipPlanDto;
import tw.com.ispan.backend.subscription.dto.OrganizerSubscriptionDto;
import tw.com.ispan.backend.subscription.enums.SubscriptionUpgradeType;
import tw.com.ispan.backend.subscription.scheduler.SubscriptionScheduler;
import tw.com.ispan.backend.subscription.service.SubscriptionService;

@SpringBootTest
@Transactional
public class SubscriptionIntegrationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private OrganizerRepository organizerRepository;

    @Autowired
    private SubscriptionService subscriptionService;

    @Autowired
    private SubscriptionScheduler subscriptionScheduler;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    public void testSaaSPlanAndSubscriptionLifecycle() {
        // 1. 準備測試帳號與主辦方
        User owner = User.builder()
                .userId("USR_SUB_01")
                .email("sub.owner@example.com")
                .passwordHash("hash")
                .name("Subscription Test Owner")
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .isDeleted(false)
                .build();
        userRepository.save(owner);

        Organizer organizer = Organizer.builder()
                .organizerId("ORG_SUB_TS")
                .name("測試訂閱主辦方")
                .taxId("99887766")
                .owner(owner)
                .status(OrganizerStatus.ACTIVE)
                .kycStatus(KycStatus.DRAFT)
                .build();
        Organizer savedOrg = organizerRepository.saveAndFlush(organizer);
        entityManager.refresh(savedOrg);

        // 2. 測試獲取所有方案
        List<MembershipPlanDto> plans = subscriptionService.getAllPlans();
        assertFalse(plans.isEmpty(), "系統應至少存在預設方案");
        assertTrue(plans.stream().anyMatch(p -> p.getPlanId().equals("FREE")), "應包含 FREE 方案");
        assertTrue(plans.stream().anyMatch(p -> p.getPlanId().equals("ANNUAL")), "應包含 ANNUAL 方案");

        // 3. 測試獲取活躍訂閱 (雖然沒有通過 KYC，但訂閱與合約分離，自動指派預設的 FREE 方案)
        OrganizerSubscriptionDto activeSub = subscriptionService.getActiveSubscription("ORG_SUB_TS");
        assertNotNull(activeSub);
        assertEquals("FREE", activeSub.getPlanId());
        assertEquals("ACTIVE", activeSub.getStatusCode());
        assertEquals("MANUAL", activeSub.getUpgradeType());
        assertNull(activeSub.getEndDate(), "免費方案無到期日");

        // 4. 測試安全防線校驗 (未通過 KYC，所以 hasFeature 直接回傳 false)
        assertFalse(subscriptionService.hasFeature("ORG_SUB_TS", "EVENT_PUBLISH"), "未通過 KYC 審核時，應無發布活動功能權限");
        assertFalse(subscriptionService.hasFeature("ORG_SUB_TS", "MERCH_STORE"), "未通過 KYC 審核時，應無週邊商城功能權限");

        // 5. 變更 KYC 狀態為已審查通過 (APPROVED)，以啟用特徵使用權限
        savedOrg.setKycStatus(KycStatus.APPROVED);
        savedOrg.setKycReviewedBy(owner);
        savedOrg.setKycReviewedAt(LocalDateTime.now());
        organizerRepository.saveAndFlush(savedOrg);
        entityManager.clear(); // 清理 L1 快取，強制同步資料庫

        // 通過 KYC 後，依據 FREE 方案，EVENT_PUBLISH 應解鎖，但無 MERCH_STORE
        assertTrue(subscriptionService.hasFeature("ORG_SUB_TS", "EVENT_PUBLISH"), "通過 KYC 且為 FREE 方案時應解鎖活動發布");
        assertFalse(subscriptionService.hasFeature("ORG_SUB_TS", "MERCH_STORE"), "FREE 方案不應解鎖週邊商城");

        // 6. 測試手動升級至 ANNUAL 方案 (年費型)
        OrganizerSubscriptionDto annualSub = subscriptionService.subscribePlan(
                "USR_SUB_01",
                "ORG_SUB_TS",
                "ANNUAL",
                SubscriptionUpgradeType.ANNUAL_FEE
        );
        assertNotNull(annualSub);
        assertEquals("ANNUAL", annualSub.getPlanId());
        assertEquals("ACTIVE", annualSub.getStatusCode());
        assertEquals("ANNUAL_FEE", annualSub.getUpgradeType());
        assertNotNull(annualSub.getEndDate(), "年費方案必須有到期日");

        // 通過 KYC 且升級至 ANNUAL 後，週邊商城功能 (MERCH_STORE) 應解鎖
        assertTrue(subscriptionService.hasFeature("ORG_SUB_TS", "MERCH_STORE"), "ANNUAL 方案應解鎖週邊商城");

        // 7. 測試排程：到期自動降級至 FREE
        // 人為修改訂閱起訖日為過去時間，模擬過期且維持 start_date <= end_date
        jdbcTemplate.update(
                "UPDATE [organizer_subscription] SET start_date = ?, end_date = ? WHERE subscription_id = ?",
                LocalDateTime.now().minusYears(1).minusDays(2),
                LocalDateTime.now().minusDays(1),
                annualSub.getSubscriptionId()
        );
        entityManager.flush();
        entityManager.clear();

        // 執行過期降級排程
        subscriptionScheduler.checkExpiredSubscriptions();

        // 檢查當前活躍訂閱是否又自動降回 FREE 且舊訂閱變為 EXPIRED
        OrganizerSubscriptionDto postExpiryActiveSub = subscriptionService.getActiveSubscription("ORG_SUB_TS");
        assertEquals("FREE", postExpiryActiveSub.getPlanId(), "過期後應降級為 FREE");
        assertEquals("ACTIVE", postExpiryActiveSub.getStatusCode());
    }
}
