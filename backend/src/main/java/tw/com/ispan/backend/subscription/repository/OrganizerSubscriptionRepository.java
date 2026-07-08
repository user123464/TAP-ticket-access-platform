package tw.com.ispan.backend.subscription.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.subscription.entity.OrganizerSubscription;
import tw.com.ispan.backend.subscription.enums.SubscriptionStatus;

public interface OrganizerSubscriptionRepository extends JpaRepository<OrganizerSubscription, String> {
    
    List<OrganizerSubscription> findByOrganizer(Organizer organizer);

    Optional<OrganizerSubscription> findByOrganizerAndStatusCode(Organizer organizer, SubscriptionStatus statusCode);

    List<OrganizerSubscription> findByStatusCodeAndEndDateBefore(SubscriptionStatus statusCode, LocalDateTime dateTime);

    @Query(value = "SELECT NEXT VALUE FOR seq_SUB", nativeQuery = true)
    Long getNextSubscriptionSequenceValue();
}
