package tw.com.ispan.backend.location.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.location.entity.Location;

public interface LocationRepository extends JpaRepository<Location, Integer> {

    // 1. 前台消費者 / 購票端呼叫：只撈出正常開賣中的場地
    List<Location> findByIsDeletedFalse();

    // 2. 管理員回收桶功能：專門撈出已經被軟刪除的場地，用來做歷史稽核或提供「一鍵還原」
    List<Location> findByIsDeletedTrue();

    // 3. 管理員主儀表板呼叫：直接使用內建的 findAll()
    // 拔掉全域過濾後，預設的 findAll() 與 findById() 就會「包含已刪除與未刪除」的所有場地，
    // 讓管理員擁有上帝視角，完美掌控整個系統的歷史場館模板！
}
