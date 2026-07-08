package tw.com.ispan.backend.subscription.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.subscription.entity.MembershipPlan;
import tw.com.ispan.backend.subscription.entity.OrganizerSubscription;
import tw.com.ispan.backend.subscription.enums.SubscriptionStatus;
import tw.com.ispan.backend.subscription.enums.SubscriptionUpgradeType;
import tw.com.ispan.backend.subscription.repository.MembershipPlanRepository;
import tw.com.ispan.backend.subscription.repository.OrganizerSubscriptionRepository;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {

    private final OrganizerSubscriptionRepository organizerSubscriptionRepository;
    private final MembershipPlanRepository membershipPlanRepository;

    /**
     * 每日凌晨 1 點執行：檢查已過期的訂閱方案，並自動將其降級回預設的 FREE 免費方案
     */
    @Scheduled(cron = "0 0 1 * * ?")
    @Transactional
    public void checkExpiredSubscriptions() {
        log.info("開始執行訂閱過期掃描任務...");
        LocalDateTime now = LocalDateTime.now();
        List<OrganizerSubscription> expiredList = organizerSubscriptionRepository
                .findByStatusCodeAndEndDateBefore(SubscriptionStatus.ACTIVE, now);

        if (expiredList.isEmpty()) {
            log.info("未偵測到任何已過期的訂閱。");
            return;
        }

        MembershipPlan freePlan = membershipPlanRepository.findById("FREE").orElse(null);
        if (freePlan == null) {
            log.error("資料庫中缺少預設的 FREE 方案種子資料，過期降級排程中止");
            return;
        }

        for (OrganizerSubscription sub : expiredList) {
            log.info("訂閱過期處理：訂閱ID {}, 組織: {}, 原方案: {}", 
                    sub.getSubscriptionId(), sub.getOrganizer().getOrganizerId(), sub.getPlan().getPlanId());

            // 1. 將舊訂閱標記為 EXPIRED
            sub.setStatusCode(SubscriptionStatus.EXPIRED);
            organizerSubscriptionRepository.save(sub);

            // 2. 自動指派一個新的活躍 FREE 訂閱 (免費方案永不過期)
            Long nextVal = organizerSubscriptionRepository.getNextSubscriptionSequenceValue();
            String newSubId = String.format("SUB%07d", nextVal);

            OrganizerSubscription freeSub = OrganizerSubscription.builder()
                    .subscriptionId(newSubId)
                    .organizer(sub.getOrganizer())
                    .plan(freePlan)
                    .statusCode(SubscriptionStatus.ACTIVE)
                    .upgradeType(SubscriptionUpgradeType.MANUAL)
                    .startDate(now)
                    .endDate(null)
                    .build();

            organizerSubscriptionRepository.save(freeSub);
            log.info("組織 {} 降級為免費 FREE 方案，新訂閱ID: {}", sub.getOrganizer().getOrganizerId(), newSubId);
        }
        log.info("訂閱過期掃描任務處理完成，共處理 {} 筆。", expiredList.size());
    }
}
