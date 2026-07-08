package tw.com.ispan.backend.auction.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.auction.entity.Bid;

public interface BidRepository extends JpaRepository<Bid, Integer> {

	// 顯示某拍賣的出價紀錄（最新在前）
	@EntityGraph(attributePaths = {"user"})
	List<Bid> findByAuctionAuctionIdOrderByCreatedAtDesc(Integer auctionId);

	// 查某會員的出價紀錄（最新在前）
	List<Bid> findByUserUserIdOrderByCreatedAtDesc(String userId);

	// 取得目前最高出價（同價時取最早出價）
	Optional<Bid> findTopByAuctionAuctionIdOrderByBidPriceDescCreatedAtAsc(Integer auctionId);

	// 取得最新一筆出價（用於防止同一人連續刷價）
	@EntityGraph(attributePaths = {"user"})
	Optional<Bid> findTopByAuctionAuctionIdOrderByCreatedAtDesc(Integer auctionId);
}
