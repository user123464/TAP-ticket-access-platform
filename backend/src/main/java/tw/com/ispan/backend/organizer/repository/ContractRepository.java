package tw.com.ispan.backend.organizer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import tw.com.ispan.backend.organizer.entity.Contract;
import tw.com.ispan.backend.organizer.enums.ContractStatus;

public interface ContractRepository extends JpaRepository<Contract, String> {

    Optional<Contract> findByOrganizer_OrganizerIdAndContractStatus(String organizerId, ContractStatus status);

    /** 查詢某組織的所有合約，依建立時間由新到舊排序 */
    List<Contract> findByOrganizer_OrganizerIdOrderByCreatedAtDesc(String organizerId);

    /** Admin 全平台合約清單（含組織關聯），依建立時間由新到舊排序 */
    @Query("SELECT c FROM Contract c JOIN FETCH c.organizer ORDER BY c.createdAt DESC")
    List<Contract> findAllWithOrganizer();

    /** Admin 財務：取出指定狀態的全部合約（含組織），供建立 organizerId→費率 對照 */
    @Query("SELECT c FROM Contract c JOIN FETCH c.organizer WHERE c.contractStatus = :status")
    List<Contract> findByContractStatusWithOrganizer(ContractStatus status);

    /** 取得 seq_CON 的下一個序列值，用於產生 CONXXXXXXX 格式的主鍵 */
    @Query(value = "SELECT NEXT VALUE FOR seq_CON", nativeQuery = true)
    Long getNextContractSequenceValue();
}
