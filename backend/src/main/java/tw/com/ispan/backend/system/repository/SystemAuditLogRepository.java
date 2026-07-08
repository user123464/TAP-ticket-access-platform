package tw.com.ispan.backend.system.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.system.entity.SystemAuditLog;

public interface SystemAuditLogRepository extends JpaRepository<SystemAuditLog, Long> {
    List<SystemAuditLog> findByTenantIdOrderByCreatedAtDesc(String tenantId);
    List<SystemAuditLog> findByActionUserIdOrderByCreatedAtDesc(String actionUserId);
}
