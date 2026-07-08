package tw.com.ispan.backend.theme.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;

public interface ThemeRepository extends JpaRepository<Theme, Integer> {
    // 給前台用的，查詢所有active的主題
    List<Theme> findByStatus(Status status);

    // 給前台用的，查詢特定themeId且狀態為ACTIVE的主題
    Optional<Theme> findByThemeIdAndStatus(Integer id, Status status);

    // 給後台廠商用的，查詢特定organizerId的主題
    List<Theme> findByOrganizerOrganizerId(String organizerId);

    // Admin 防呆用：統計某組織在特定狀態（如 ACTIVE）的活動數
    long countByOrganizerOrganizerIdAndStatus(String organizerId, Status status);

    // 前台查詢，依照session的startTime排序
    @Query("""
                SELECT t
                FROM Theme t
                WHERE t.status = 'ACTIVE'
                AND (:keyword IS NULL OR t.title LIKE CONCAT('%', :keyword, '%'))
                AND EXISTS (
                    SELECT 1
                    FROM Session s
                    WHERE s.theme = t
                    AND s.status = 'ACTIVE'
                )
                ORDER BY (
                    SELECT MIN(s.startTime)
                    FROM Session s
                    WHERE s.theme = t
                    AND s.status = 'ACTIVE'
                ) ASC
            """)
    Page<Theme> findUpcomingThemes(@Param("keyword") String keyword, Pageable pageable);

    // 前台查詢，依照session的publishTime排序
    @Query("""
                SELECT t
                FROM Theme t
                WHERE t.status = 'ACTIVE'
                AND (:keyword IS NULL OR t.title LIKE CONCAT('%', :keyword, '%'))
                AND EXISTS (
                    SELECT 1
                    FROM Session s
                    WHERE s.theme = t
                    AND s.status = 'ACTIVE'
                )
                ORDER BY (
                    SELECT MAX(s.publishTime)
                    FROM Session s
                    WHERE s.theme = t
                    AND s.status = 'ACTIVE'
                ) DESC
            """)
    Page<Theme> findLatestThemes(@Param("keyword") String keyword, Pageable pageable);

    // 把「目前是 ACTIVE、且至少有一筆場次、但底下完全沒有 ACTIVE 場次」的主題封存。
    @Modifying
    @Query("""
                UPDATE Theme t
                SET t.status = 'ARCHIVED'
                WHERE t.status = 'ACTIVE'
                AND EXISTS (
                    SELECT 1 FROM Session s
                    WHERE s.theme = t
                )
                AND NOT EXISTS (
                    SELECT 1 FROM Session s
                    WHERE s.theme = t
                    AND s.status = 'ACTIVE'
                )
            """)
    int archiveThemesWithoutActiveSessions();

}