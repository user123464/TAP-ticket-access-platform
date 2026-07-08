package tw.com.ispan.backend.settlements.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.settlements.entity.SettlementBean;
import tw.com.ispan.backend.settlements.enums.SettlementEnum;

public interface SettlementRepository extends JpaRepository<SettlementBean, String> {

    List<SettlementBean> findByOrganizerOrganizer_OrganizerIdOrderByCreatedAtDesc(String organizerId);

    // 組織註銷防呆用：統計某組織在特定撥款狀態（如 PENDING 未結算）的結算筆數
    long countByOrganizerOrganizer_OrganizerIdAndItemStatus(String organizerId, SettlementEnum itemStatus);
}
