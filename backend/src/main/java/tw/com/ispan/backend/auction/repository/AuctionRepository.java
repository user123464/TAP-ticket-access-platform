package tw.com.ispan.backend.auction.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import tw.com.ispan.backend.auction.entity.Auction;
import tw.com.ispan.backend.theme.enums.Status;

public interface AuctionRepository extends JpaRepository<Auction, Integer> {

	// 給前台用的，查詢特定狀態的拍賣
	List<Auction> findByStatus(Status status);

	// 給前台用的，查詢特定auctionId且狀態為指定值的拍賣
	Optional<Auction> findByAuctionIdAndStatus(Integer auctionId, Status status);

	// 給後台廠商用的，透過 theme 查詢特定 organizerId 的拍賣
	List<Auction> findByThemeOrganizerOrganizerId(String organizerId);

	// 查詢指定主題底下的所有拍賣
	List<Auction> findByThemeThemeId(Integer themeId);

	// 查詢指定主題底下且為指定狀態的拍賣
	List<Auction> findByThemeThemeIdAndStatus(Integer themeId, Status status);

	// 用來更新狀態的查詢
	List<Auction> findByEndTimeBeforeAndStatus(LocalDateTime time, Status status);

	// 競價時鎖定同一筆拍賣，避免同時出價造成 currentPrice 被覆蓋
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("SELECT a FROM Auction a WHERE a.auctionId = :auctionId")
	Optional<Auction> findByIdForUpdate(@Param("auctionId") Integer auctionId);
}
