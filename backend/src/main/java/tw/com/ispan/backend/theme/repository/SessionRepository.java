package tw.com.ispan.backend.theme.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.theme.entity.Session;
import tw.com.ispan.backend.theme.enums.Status;

public interface SessionRepository extends JpaRepository<Session, Integer> {

    // 前台的sessions (ACTIVE)
    @EntityGraph(attributePaths = { "location" })
    List<Session> findByThemeThemeIdAndStatus(Integer themeId, Status status);

    // 後台的sessions (ThemeId)
    @EntityGraph(attributePaths = { "location" })
    List<Session> findByThemeThemeId(Integer themeId);

    // 用來更新狀態的的查詢
    List<Session> findByEndTimeBeforeAndStatus(
            LocalDateTime time,
            Status status);

    // admin場地編輯：檢查場館是否已經被使用
    boolean existsByLocationLocationId(Integer locationId);

    // 廠商載入同主題同場地其他session的票種設定
    List<Session> findByTheme_ThemeIdAndLocation_LocationIdAndSessionIdNot(
            Integer themeId,
            Integer locationId,
            Integer currentSessionId);

    // 用來將Session的status ACTIVE-> ARCHIVED         
    @Modifying
    @Query("""
                UPDATE Session s
                SET s.status = 'ARCHIVED'
                WHERE s.endTime < :now
                AND s.status = 'ACTIVE'
            """)
        int archiveExpired(@Param("now") LocalDateTime now);

}