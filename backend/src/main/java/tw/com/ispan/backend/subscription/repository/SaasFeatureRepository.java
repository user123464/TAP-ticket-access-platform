package tw.com.ispan.backend.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.subscription.entity.SaasFeature;

public interface SaasFeatureRepository extends JpaRepository<SaasFeature, String> {
}
