package tw.com.ispan.backend.organizer.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.entity.Role;

public interface OrganizerMemberRepository extends JpaRepository<OrganizerMember, String> {

  // 檢查是否有成員指派此角色
  boolean existsByRole(Role role);

  // 根據組織查詢所有成員
  List<OrganizerMember> findByOrganizer(Organizer organizer);

  // 根據使用者查詢其加入的所有組織關聯
  List<OrganizerMember> findByUser(User user);

  // 根據組織與使用者，查詢唯一的成員記錄 (用於檢查該使用者是否已在該組織中)
  Optional<OrganizerMember> findByOrganizerAndUser(Organizer organizer, User user);

  // 根據邀請驗證 Token 查詢唯一的成員記錄
  Optional<OrganizerMember> findByInviteToken(String inviteToken);

  // 取得資料庫中 seq_MBR 的下一個序列值，用於產生 MBRXXXXXXX 格式的主鍵
  @Query(value = "SELECT NEXT VALUE FOR seq_MBR", nativeQuery = true)
  Long getNextMemberSequenceValue();

  /**
   * 查核「某 user 在某 organizer 內，透過其組織成員角色，是否擁有指定 permission」。
   * 路徑：organizer_member → role → role_permission。
   * 僅計入狀態為 ACCEPTED 的成員。回傳是否存在符合的權限授予記錄。
   */
  @Query("""
      SELECT COUNT(p) > 0 FROM OrganizerMember m
      JOIN m.role r
      JOIN r.permissions p
      WHERE m.organizer.organizerId = :organizerId
        AND m.user.userId = :userId
        AND m.status = tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus.ACCEPTED
        AND p.permissionId = :permissionId
      """)
  boolean existsOrgPermission(@Param("userId") String userId,
      @Param("organizerId") String organizerId,
      @Param("permissionId") String permissionId);
}
