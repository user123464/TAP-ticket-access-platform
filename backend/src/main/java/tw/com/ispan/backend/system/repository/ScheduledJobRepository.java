package tw.com.ispan.backend.system.repository;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.system.entity.ScheduledJob;

public interface ScheduledJobRepository extends JpaRepository<ScheduledJob, String> {
    Optional<ScheduledJob> findByJobCode(String jobCode);
}
