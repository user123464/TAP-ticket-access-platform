package tw.com.ispan.backend.notification.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.notification.entity.NotificationTemplate;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, String> {
    Optional<NotificationTemplate> findByTemplateCode(String templateCode);
}
