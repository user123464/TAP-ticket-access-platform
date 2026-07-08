package tw.com.ispan.backend.organizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.dto.CreateOrganizerRequest;
import tw.com.ispan.backend.organizer.dto.CreateRoleRequest;
import tw.com.ispan.backend.organizer.dto.MemberResponse;
import tw.com.ispan.backend.organizer.dto.MyOrganizationResponse;
import tw.com.ispan.backend.organizer.dto.RoleResponse;
import tw.com.ispan.backend.organizer.entity.Permission;
import tw.com.ispan.backend.organizer.repository.PermissionRepository;
import tw.com.ispan.backend.organizer.service.OrganizerMemberService;
import tw.com.ispan.backend.organizer.service.OrganizerService;
import tw.com.ispan.backend.organizer.service.RoleService;

@SpringBootTest
@Transactional
public class TeamManagementIntegrationTest {

    @Autowired
    private RoleService roleService;

    @Autowired
    private OrganizerService organizerService;

    @Autowired
    private OrganizerMemberService organizerMemberService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PermissionRepository permissionRepository;

    @Test
    public void testRoleAndMemberLifecycle() {
        // 1. 建立測試用的 User (Owner)
        User owner = User.builder()
                .userId("T_OWN_1")
                .email("testowner@example.com")
                .passwordHash("hash")
                .name("Test Owner")
                .authProvider(AuthProvider.LOCAL)
                .gender("M")
                .birthDate(LocalDate.of(1990, 1, 1))
                .isActive(true)
                .build();
        userRepository.save(owner);

        // 2. 建立測試用的 User (Member)
        User memberUser = User.builder()
                .userId("T_MEM_1")
                .email("testmember@example.com")
                .passwordHash("hash")
                .name("Test Member")
                .authProvider(AuthProvider.LOCAL)
                .gender("F")
                .birthDate(LocalDate.of(1995, 1, 1))
                .isActive(true)
                .build();
        userRepository.save(memberUser);

        // 3. Owner 建立一個新組織
        CreateOrganizerRequest createOrgReq = new CreateOrganizerRequest();
        createOrgReq.setName("Test Team Org");
        createOrgReq.setTaxId("12345678");
        MyOrganizationResponse orgResponse = organizerService.createOrganization(owner.getUserId(), createOrgReq);
        String orgId = orgResponse.getId();
        assertNotNull(orgId);

        // 4. 驗證新組織可以取到系統預設角色 (至少要有 ADMIN, FINANCE 等)
        List<RoleResponse> initialRoles = roleService.getRolesForOrganizer(owner.getUserId(), orgId);
        assertFalse(initialRoles.isEmpty(), "新組織應該要能取到系統預設角色");

        // 5. 新增測試用的 Permission
        Permission perm = new Permission();
        perm.setPermissionId("TICKET_MANAGE");
        perm.setActionCode("MANAGE");
        perm.setResourceCode("TICKET");
        perm.setDescription("管理票種");
        permissionRepository.save(perm);

        // 6. Owner 新增一個自訂角色「現場驗票員」
        CreateRoleRequest createRoleReq = new CreateRoleRequest();
        createRoleReq.setRoleName("現場驗票員");
        createRoleReq.setDescription("僅能驗票");
        createRoleReq.setPermissions(List.of("TICKET_MANAGE")); // 假設系統有 TICKET_MANAGE
        RoleResponse newRole = roleService.createRole(owner.getUserId(), orgId, createRoleReq);
        assertNotNull(newRole.getRoleId());
        assertEquals("現場驗票員", newRole.getRoleName());
        assertTrue(newRole.getPermissions().contains("TICKET_MANAGE"));

        // 7. Owner 發送邀請給 Member，身分為新建立的自訂角色
        tw.com.ispan.backend.organizer.dto.InviteMemberRequest inviteReq = new tw.com.ispan.backend.organizer.dto.InviteMemberRequest();
        inviteReq.setEmail(memberUser.getEmail());
        inviteReq.setRoleId(newRole.getRoleId());
        organizerMemberService.inviteMember(owner.getUserId(), orgId, inviteReq);

        // 8. 驗證成員清單中有這名受邀者 (狀態應為 PENDING)
        List<MemberResponse> members = organizerMemberService.getMembers(orgId);
        Optional<MemberResponse> invitedMemberOpt = members.stream()
                .filter(m -> m.getEmail().equals(memberUser.getEmail())).findFirst();
        assertTrue(invitedMemberOpt.isPresent());
        MemberResponse invitedMember = invitedMemberOpt.get();
        assertEquals(0, invitedMember.getStatus()); // 0 = PENDING
        assertEquals(newRole.getRoleId(), invitedMember.getRoleId());

        // 9. 變更邀請中的角色為 ADMIN
        organizerMemberService.updateMemberRole(owner.getUserId(), orgId, invitedMember.getMemberId(), "ADMIN");
        members = organizerMemberService.getMembers(orgId);
        MemberResponse updatedMember = members.stream()
                .filter(m -> m.getMemberId().equals(invitedMember.getMemberId())).findFirst().get();
        assertEquals("ADMIN", updatedMember.getRoleId());

        // 10. 移除該成員
        organizerMemberService.removeMember(owner.getUserId(), orgId, invitedMember.getMemberId());
        members = organizerMemberService.getMembers(orgId);
        MemberResponse revokedMember = members.stream()
                .filter(m -> m.getMemberId().equals(invitedMember.getMemberId())).findFirst().get();
        assertEquals(3, revokedMember.getStatus()); // 3 = REVOKED

        // 11. 刪除自訂角色
        roleService.deleteRole(owner.getUserId(), orgId, newRole.getRoleId());
        List<RoleResponse> finalRoles = roleService.getRolesForOrganizer(owner.getUserId(), orgId);
        assertFalse(finalRoles.stream().anyMatch(r -> r.getRoleId().equals(newRole.getRoleId())), "自訂角色應該已被刪除");
    }
}
