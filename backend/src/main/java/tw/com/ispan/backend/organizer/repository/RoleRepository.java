package tw.com.ispan.backend.organizer.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.Role;

public interface RoleRepository extends JpaRepository<Role, String> {

    // 取得該組織自訂的角色
    List<Role> findByOrganizer(Organizer organizer);

    // Admin RBAC：平台級角色（organizer_id IS NULL）
    List<Role> findByOrganizerIsNull();

    // 取得該組織自訂的角色 + 系統預設角色(organizer IS NULL)
    @Query("SELECT r FROM Role r WHERE r.organizer = :organizer OR r.organizer IS NULL")
    List<Role> findByOrganizerOrSystemRoles(@Param("organizer") Organizer organizer);

    @Query(value = "SELECT NEXT VALUE FOR seq_ROL", nativeQuery = true)
    Long getNextRoleSequenceValue();
}
