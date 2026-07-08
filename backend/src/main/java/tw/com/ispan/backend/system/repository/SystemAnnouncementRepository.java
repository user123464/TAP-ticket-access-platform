package tw.com.ispan.backend.system.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.system.entity.SystemAnnouncement;

public interface SystemAnnouncementRepository extends JpaRepository<SystemAnnouncement, String> {
    List<SystemAnnouncement> findByTargetPortalOrderByCreatedAtDesc(String targetPortal);
}
