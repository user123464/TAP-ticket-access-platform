package tw.com.ispan.backend.system.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.system.entity.SystemConfig;

public interface SystemConfigRepository extends JpaRepository<SystemConfig, String> {
    Optional<SystemConfig> findByConfigKey(String configKey);
}
