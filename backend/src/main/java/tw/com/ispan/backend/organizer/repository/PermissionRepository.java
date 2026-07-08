package tw.com.ispan.backend.organizer.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.organizer.entity.Permission;

public interface PermissionRepository extends JpaRepository<Permission, String> {
}
