package tw.com.ispan.backend.organizer.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.organizer.entity.RolePermissionTemplate;

public interface RolePermissionTemplateRepository extends JpaRepository<RolePermissionTemplate, Long> {
    List<RolePermissionTemplate> findByTemplateId(String templateId);
}
