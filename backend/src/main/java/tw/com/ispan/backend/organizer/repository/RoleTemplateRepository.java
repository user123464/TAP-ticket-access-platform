package tw.com.ispan.backend.organizer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.organizer.entity.RoleTemplate;

public interface RoleTemplateRepository extends JpaRepository<RoleTemplate, String> {
}
