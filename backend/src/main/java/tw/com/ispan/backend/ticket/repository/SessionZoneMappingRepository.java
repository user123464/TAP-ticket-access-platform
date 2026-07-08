package tw.com.ispan.backend.ticket.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.ticket.entity.SessionZoneMapping;

public interface SessionZoneMappingRepository
                extends JpaRepository<SessionZoneMapping, Long> {

        // 找出某個場次目前所有「活著」的綁定規則
        List<SessionZoneMapping> findBySession_SessionIdAndIsDeletedFalse(Integer sessionId);

        // 透過自然鍵 (場次ID + 分區ID) 找出唯一的綁定紀錄 (包含可能已經被軟刪除的)
        Optional<SessionZoneMapping> findBySession_SessionIdAndZone_ZoneId(
                        Integer sessionId,
                        Integer zoneId);
}
