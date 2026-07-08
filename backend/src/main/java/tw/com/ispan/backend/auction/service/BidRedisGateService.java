package tw.com.ispan.backend.auction.service;

import java.math.BigDecimal;

import tw.com.ispan.backend.auction.entity.Auction;
import tw.com.ispan.backend.auction.enums.BidGateResult;

public interface BidRedisGateService {

    void initializeAuction(Auction auction);

    BidGateResult reserveBid(Integer auctionId, String userId, BigDecimal bidPrice);

    void archiveAuction(Integer auctionId);
}