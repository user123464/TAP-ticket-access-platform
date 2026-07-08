package tw.com.ispan.backend.organizer;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.backend.config.security.CustomUserDetailsService;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.dto.SystemResourceDto;
import tw.com.ispan.backend.organizer.entity.Permission;
import tw.com.ispan.backend.organizer.entity.Role;
import tw.com.ispan.backend.organizer.entity.SystemResource;
import tw.com.ispan.backend.organizer.enums.ResourceType;
import tw.com.ispan.backend.organizer.repository.PermissionRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;
import tw.com.ispan.backend.organizer.repository.SystemResourceRepository;
import tw.com.ispan.backend.organizer.service.SystemResourceService;

@SpringBootTest
@Transactional
public class SystemResourceIntegrationTest {

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private RoleRepository roleRepository;

        @Autowired
        private PermissionRepository permissionRepository;

        @Autowired
        private SystemResourceRepository systemResourceRepository;

        @Autowired
        private SystemResourceService systemResourceService;

        @Autowired
        private CustomUserDetailsService userDetailsService;

        @Test
        public void testUserAuthoritiesLoadingAndMenuTree() {
                // 1. 建立測試權限
                Permission viewUserPerm = Permission.builder()
                                .permissionId("USER_VIEW_TEST")
                                .resourceCode("USER_TEST")
                                .actionCode("VIEW_TEST")
                                .description("查看使用者")
                                .build();
                Permission savedPerm = permissionRepository.save(viewUserPerm);

                // 2. 建立測試角色，並賦予該權限
                Role adminRole = Role.builder()
                                .roleId("ROLE_ADMIN_TEST")
                                .roleName("測試管理員")
                                .isEditable(false)
                                .permissions(Collections.singleton(savedPerm))
                                .build();
                Role savedRole = roleRepository.save(adminRole);

                // 3. 建立測試 User，並分配角色
                User user = User.builder()
                                .userId("USR_TEST1")
                                .email("test.security@example.com")
                                .passwordHash("password_hash")
                                .name("Security Test User")
                                .authProvider(AuthProvider.LOCAL)
                                .isActive(true)
                                .isDeleted(false)
                                .roles(Collections.singleton(savedRole))
                                .build();
                userRepository.save(user);

                // 4. 建立系統資源：一個需要權限的，一個不需要權限的
                SystemResource publicMenu = SystemResource.builder()
                                .resourceId("MENU_PUBLIC")
                                .portalType(PortalType.ADMIN_LOCAL)
                                .resourceType(ResourceType.PAGE) // [Jason] 葉節點用 PAGE：Phase 0 起空 MENU 群組會被過濾
                                .name("首頁")
                                .sortOrder(1)
                                .build();
                systemResourceRepository.save(publicMenu);

                SystemResource securedMenu = SystemResource.builder()
                                .resourceId("MENU_SECURED")
                                .portalType(PortalType.ADMIN_LOCAL)
                                .resourceType(ResourceType.PAGE) // [Jason] 葉節點用 PAGE：Phase 0 起空 MENU 群組會被過濾
                                .name("使用者管理")
                                .permission(savedPerm)
                                .sortOrder(2)
                                .build();
                systemResourceRepository.save(securedMenu);

                // 5. 測試 CustomUserDetailsService 載入 User，並驗證 GrantedAuthorities
                UserDetails userDetails = userDetailsService.loadUserByUsername("test.security@example.com");
                assertNotNull(userDetails);
                assertTrue(userDetails instanceof CustomUserDetails);
                CustomUserDetails customDetails = (CustomUserDetails) userDetails;

                // 驗證權限包含角色 ID ("ROLE_ROLE_ADMIN_TEST") 與 權限 ID ("USER_VIEW_TEST")
                Set<String> perms = customDetails.getPermissions();
                assertTrue(perms.contains("USER_VIEW_TEST"));

                // 6. 測試 MenuTree 取得與權限過濾
                // 用戶有 VIEW 權限，應該可以取得 publicMenu (MENU_PUBLIC) 和 securedMenu (MENU_SECURED)
                List<SystemResourceDto> menuTreeWithPerm = systemResourceService.getMenuTree(PortalType.ADMIN_LOCAL,
                                perms);
                boolean hasPublic = menuTreeWithPerm.stream()
                                .anyMatch(dto -> dto.getResourceId().equals("MENU_PUBLIC"));
                boolean hasSecured = menuTreeWithPerm.stream()
                                .anyMatch(dto -> dto.getResourceId().equals("MENU_SECURED"));
                assertTrue(hasPublic, "擁有權限時應包含公共選單");
                assertTrue(hasSecured, "擁有權限時應包含受保護選單");

                // 如果換成空權限列表，應該只能拿到不需權限的 publicMenu，拿不到 securedMenu
                List<SystemResourceDto> menuTreeNoPerm = systemResourceService.getMenuTree(PortalType.ADMIN_LOCAL,
                                Collections.emptySet());
                boolean hasPublicNoPerm = menuTreeNoPerm.stream()
                                .anyMatch(dto -> dto.getResourceId().equals("MENU_PUBLIC"));
                boolean hasSecuredNoPerm = menuTreeNoPerm.stream()
                                .anyMatch(dto -> dto.getResourceId().equals("MENU_SECURED"));
                assertTrue(hasPublicNoPerm, "無權限時應包含公共選單");
                assertFalse(hasSecuredNoPerm, "無權限時不應包含受保護選單");
        }
}
