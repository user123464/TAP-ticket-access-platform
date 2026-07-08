package tw.com.ispan.backend.organizer.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.organizer.entity.SystemResource;

public interface SystemResourceRepository extends JpaRepository<SystemResource, String> {
    List<SystemResource> findByPortalTypeOrderBySortOrderAsc(PortalType portalType);
    List<SystemResource> findByParentIsNullOrderBySortOrderAsc();
    List<SystemResource> findByPortalTypeAndParentIsNullOrderBySortOrderAsc(PortalType portalType);
}
