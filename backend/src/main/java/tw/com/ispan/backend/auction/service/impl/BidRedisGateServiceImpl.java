package tw.com.ispan.backend.auction.service.impl;

import java.math.BigDecimal;
import java.time.ZoneId;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.auction.entity.Auction;
import tw.com.ispan.backend.auction.repository.AuctionRepository;
import tw.com.ispan.backend.auction.enums.BidGateResult;
import tw.com.ispan.backend.auction.service.BidRedisGateService;
import tw.com.ispan.backend.theme.enums.Status;

@Service
@RequiredArgsConstructor
public class BidRedisGateServiceImpl implements BidRedisGateService {

    private static final String REDIS_AUCTION_STATE_PREFIX = "auction:bid:state:";

    private static final DefaultRedisScript<String> RESERVE_BID_SCRIPT;
    private static final String RESERVE_BID_LUA_SCRIPT = """
            local status = redis.call('HGET', KEYS[1], 'status')
            if not status or status ~= 'ACTIVE' then
              return 'NOT_ACTIVE'
            end

            local startTime = tonumber(redis.call('HGET', KEYS[1], 'startTime'))
            local endTime = tonumber(redis.call('HGET', KEYS[1], 'endTime'))
            local currentPrice = tonumber(redis.call('HGET', KEYS[1], 'currentPrice'))
            local buyoutPriceText = redis.call('HGET', KEYS[1], 'buyoutPrice')
            local lastBidderUserId = redis.call('HGET', KEYS[1], 'lastBidderUserId')

            local time = redis.call('TIME')
            local nowMillis = (tonumber(time[1]) * 1000) + math.floor(tonumber(time[2]) / 1000)

            if nowMillis < startTime or nowMillis >= endTime then
              return 'EXPIRED'
            end

            if lastBidderUserId and lastBidderUserId == ARGV[1] then
              return 'DUPLICATE_USER'
            end

            if tonumber(ARGV[2]) <= currentPrice then
              return 'LOWER_THAN_CURRENT'
            end

            redis.call('HSET', KEYS[1],
              'currentPrice', ARGV[2],
              'lastBidderUserId', ARGV[1])

            if buyoutPriceText and buyoutPriceText ~= '' and tonumber(ARGV[2]) >= tonumber(buyoutPriceText) then
              redis.call('HSET', KEYS[1], 'status', 'ARCHIVED')
              return 'BUYOUT'
            end

            return 'ACCEPTED'
            """;

    static {
        RESERVE_BID_SCRIPT = new DefaultRedisScript<>();
        RESERVE_BID_SCRIPT.setResultType(String.class);
        RESERVE_BID_SCRIPT.setScriptText(RESERVE_BID_LUA_SCRIPT);
    }

    private final StringRedisTemplate stringRedisTemplate;
    private final AuctionRepository auctionRepository;

    @Override
    public void initializeAuction(Auction auction) {
        String key = auctionStateKey(auction.getAuctionId());
        BigDecimal currentPrice = auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartPrice();

        stringRedisTemplate.opsForHash().put(key, "status", auction.getStatus().name());
        stringRedisTemplate.opsForHash().put(key, "startTime", String.valueOf(toMillis(auction.getStartTime())));
        stringRedisTemplate.opsForHash().put(key, "endTime", String.valueOf(toMillis(auction.getEndTime())));
        stringRedisTemplate.opsForHash().put(key, "currentPrice", currentPrice.toPlainString());
        if (auction.getBuyoutPrice() != null) {
            stringRedisTemplate.opsForHash().put(key, "buyoutPrice", auction.getBuyoutPrice().toPlainString());
        } else {
            stringRedisTemplate.opsForHash().delete(key, "buyoutPrice");
        }
        stringRedisTemplate.opsForHash().delete(key, "lastBidderUserId");
    }

    @Override
    public BidGateResult reserveBid(Integer auctionId, String userId, BigDecimal bidPrice) {
      ensureAuctionInitialized(auctionId);

        String result = stringRedisTemplate.execute(
                RESERVE_BID_SCRIPT,
                List.of(auctionStateKey(auctionId)),
                userId,
                bidPrice.toPlainString());

        if (result == null) {
            return BidGateResult.NOT_ACTIVE;
        }

        return switch (result) {
            case "ACCEPTED" -> BidGateResult.ACCEPTED;
            case "BUYOUT" -> BidGateResult.BUYOUT;
            case "EXPIRED" -> BidGateResult.EXPIRED;
            case "DUPLICATE_USER" -> BidGateResult.DUPLICATE_USER;
            case "LOWER_THAN_CURRENT" -> BidGateResult.LOWER_THAN_CURRENT;
            default -> BidGateResult.NOT_ACTIVE;
        };
    }

    @Override
    public void archiveAuction(Integer auctionId) {
        stringRedisTemplate.delete(auctionStateKey(auctionId));
    }

    private String auctionStateKey(Integer auctionId) {
        return REDIS_AUCTION_STATE_PREFIX + auctionId;
    }

    private void ensureAuctionInitialized(Integer auctionId) {
      String key = auctionStateKey(auctionId);
      if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(key))) {
        return;
      }

      Auction auction = auctionRepository.findById(auctionId).orElse(null);
      if (auction == null || auction.getStatus() != Status.ACTIVE) {
        return;
      }

      initializeAuction(auction);
    }

    private long toMillis(java.time.LocalDateTime dateTime) {
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
    }
}