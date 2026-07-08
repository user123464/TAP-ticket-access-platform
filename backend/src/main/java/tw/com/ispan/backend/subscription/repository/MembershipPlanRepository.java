package tw.com.ispan.backend.subscription.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import tw.com.ispan.backend.subscription.entity.MembershipPlan;

public interface MembershipPlanRepository extends JpaRepository<MembershipPlan, String> {
}
