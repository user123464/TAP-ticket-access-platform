package tw.com.ispan.backend.auction.service.impl;

import java.math.BigDecimal;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.data.redis.core.StringRedisTemplate;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.auction.dto.message.BidConfirmedMessageDTO;
import tw.com.ispan.backend.auction.entity.Auction;
import tw.com.ispan.backend.auction.entity.Bid;
import tw.com.ispan.backend.auction.repository.AuctionRepository;
import tw.com.ispan.backend.auction.repository.BidRepository;
import tw.com.ispan.backend.auction.enums.BidPersistenceResult;
import tw.com.ispan.backend.auction.service.BidPersistenceService;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.theme.enums.Status;

@Service
@RequiredArgsConstructor
public class BidPersistenceServiceImpl implements BidPersistenceService {

    private static final String REDIS_CURRENT_PRICE_KEY_PREFIX = "auction:currentPrice:";

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final UserRepository userRepository;
    private final StringRedisTemplate stringRedisTemplate;

    @Override
    @Transactional
    public BidPersistenceResult persistConfirmedBid(BidConfirmedMessageDTO message) {
        Auction lockedAuction = auctionRepository.findByIdForUpdate(message.auctionId())
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        User user = userRepository.findById(message.userId())
                .orElseThrow(() -> new RuntimeException("User not found"));

        BigDecimal currentPrice = lockedAuction.getCurrentPrice() != null
                ? lockedAuction.getCurrentPrice()
                : lockedAuction.getStartPrice();

        int compareResult = currentPrice.compareTo(message.bidPrice());
        if (compareResult > 0) {
            return BidPersistenceResult.STALE;
        }
        if (compareResult == 0) {
            return BidPersistenceResult.DUPLICATE;
        }

        lockedAuction.setCurrentPrice(message.bidPrice());

        Bid bid = new Bid();
        bid.setAuction(lockedAuction);
        bid.setUser(user);
        bid.setBidPrice(message.bidPrice());
        bidRepository.save(bid);

        if (lockedAuction.getBuyoutPrice() != null
                && message.bidPrice().compareTo(lockedAuction.getBuyoutPrice()) == 0) {
            lockedAuction.setStatus(Status.ARCHIVED);
        }

        registerRedisSync(message.auctionId(), message.bidPrice());
        return BidPersistenceResult.APPLIED;
    }

    private void registerRedisSync(Integer auctionId, BigDecimal bidPrice) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            syncRedisCurrentPrice(auctionId, bidPrice);
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                syncRedisCurrentPrice(auctionId, bidPrice);
            }
        });
    }

    private void syncRedisCurrentPrice(Integer auctionId, BigDecimal bidPrice) {
        try {
            stringRedisTemplate.opsForValue().set(currentPriceKey(auctionId), bidPrice.toPlainString());
        } catch (Exception e) {
            throw new RuntimeException("Redis current price sync failed", e);
        }
    }

    private String currentPriceKey(Integer auctionId) {
        return REDIS_CURRENT_PRICE_KEY_PREFIX + auctionId;
    }
}