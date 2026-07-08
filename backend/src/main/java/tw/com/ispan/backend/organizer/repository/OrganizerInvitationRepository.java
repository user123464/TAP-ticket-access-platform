package tw.com.ispan.backend.organizer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.organizer.entity.OrganizerInvitation;

public interface OrganizerInvitationRepository extends JpaRepository<OrganizerInvitation, Long> {

    List<OrganizerInvitation> findByEmailAndStatus(String email, Integer status);

    Optional<OrganizerInvitation> findByInviteToken(String inviteToken);
}
