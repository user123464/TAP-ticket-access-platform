package tw.com.ispan.backend.auction.service;

import tw.com.ispan.backend.auction.dto.message.BidConfirmedMessageDTO;

public interface BidEventPublisher {

    void publishBidConfirmedEvent(BidConfirmedMessageDTO message);
}