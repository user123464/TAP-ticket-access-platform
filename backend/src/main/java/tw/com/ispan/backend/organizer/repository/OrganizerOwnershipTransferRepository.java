package tw.com.ispan.backend.organizer.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import tw.com.ispan.backend.organizer.entity.OrganizerOwnershipTransfer;
import tw.com.ispan.backend.organizer.enums.OrganizerTransferStatus;

public interface OrganizerOwnershipTransferRepository
        extends JpaRepository<OrganizerOwnershipTransfer, String> {

    // 依認證 Token 查唯一一筆轉移紀錄
    Optional<OrganizerOwnershipTransfer> findByToken(String token);

    // 查某組織目前是否已有待處理（PENDING）的轉移；用於避免重複發起與折疊區顯示
    Optional<OrganizerOwnershipTransfer> findByOrganizer_OrganizerIdAndStatus(
            String organizerId, OrganizerTransferStatus status);

    // 取得 seq_TRF 的下一個序列值，用於產生 TRFXXXXXXX 主鍵
    @Query(value = "SELECT NEXT VALUE FOR seq_TRF", nativeQuery = true)
    Long getNextTransferSequenceValue();
}
