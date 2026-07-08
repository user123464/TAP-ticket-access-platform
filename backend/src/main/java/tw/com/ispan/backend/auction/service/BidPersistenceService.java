package tw.com.ispan.backend.auction.service;

import tw.com.ispan.backend.auction.dto.message.BidConfirmedMessageDTO;
import tw.com.ispan.backend.auction.enums.BidPersistenceResult;

public interface BidPersistenceService {

    BidPersistenceResult persistConfirmedBid(BidConfirmedMessageDTO message);
}