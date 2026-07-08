package tw.com.ispan.backend.notification.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.notification.entity.NotificationLog;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    List<NotificationLog> findByRecipientUser(User recipientUser);
}
