package tw.com.ispan.backend.organizer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.enums.KycStatus;

public interface OrganizerRepository extends JpaRepository<Organizer, String> {

    // Spring Data JPA 魔術查詢方法：根據擁有者 (User) 查詢其名下的所有組織
    List<Organizer> findByOwner(User owner);

    /**
     * [Jason] RBAC 待辦 #2：Admin 後台依 KYC 狀態查組織（如待審 PENDING）。
     * 「全部」情境改用 JpaRepository 內建的 findAll()，不另開無條件查詢。
     */
    List<Organizer> findByKycStatus(KycStatus kycStatus);

    // Admin 儀表板：依 KYC 狀態統計筆數（如待審 PENDING 數）
    long countByKycStatus(KycStatus kycStatus);

    // 取得資料庫中 seq_ORG 的下一個序列值，用於產生 ORGXXXXXXX 格式的主鍵
    @Query(value = "SELECT NEXT VALUE FOR seq_ORG", nativeQuery = true)
    Long getNextOrganizerSequenceValue();
}
