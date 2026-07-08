package tw.com.ispan.backend.auction.scheduler;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.auction.entity.Auction;
import tw.com.ispan.backend.auction.repository.AuctionRepository;
import tw.com.ispan.backend.theme.enums.Status;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuctionExpiryScheduler {

    private final AuctionRepository auctionRepository;

    /**
        * 每 5 分鐘將已到期且仍為 ACTIVE 的拍賣改成 ARCHIVED。
     */
    @Scheduled(cron = "0 */5 * * * ?")
    @Transactional
    public void closeExpiredAuctions() {
        try {
            List<Auction> expiredAuctions = auctionRepository
                    .findByEndTimeBeforeAndStatus(LocalDateTime.now(), Status.ACTIVE);

            if (expiredAuctions.isEmpty()) {
                return;
            }

            expiredAuctions.forEach(auction -> auction.setStatus(Status.ARCHIVED));
            auctionRepository.saveAll(expiredAuctions);

            log.info("拍賣到期關閉完成，共 {} 筆", expiredAuctions.size());
        } catch (Exception e) {
            log.error("拍賣到期關閉排程失敗", e);
        }
    }
}