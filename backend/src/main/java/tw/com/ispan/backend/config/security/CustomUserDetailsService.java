package tw.com.ispan.backend.config.security;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("找不到該信箱對應的使用者: " + email));
        return new CustomUserDetails(user, getAuthorities(user), getPermissionIds(user));
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserByUserId(String userId) throws UsernameNotFoundException {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("找不到該識別碼對應的使用者: " + userId));
        return new CustomUserDetails(user, getAuthorities(user), getPermissionIds(user));
    }

    private Collection<GrantedAuthority> getAuthorities(User user) {
        Set<GrantedAuthority> authorities = new HashSet<>();
        if (user.getRoles() != null) {
            for (tw.com.ispan.backend.organizer.entity.Role role : user.getRoles()) {
                authorities.add(new SimpleGrantedAuthority("ROLE_" + role.getRoleId()));
                if (role.getPermissions() != null) {
                    for (tw.com.ispan.backend.organizer.entity.Permission perm : role.getPermissions()) {
                        authorities.add(new SimpleGrantedAuthority(perm.getPermissionId()));
                    }
                }
            }
        }
        return authorities;
    }

    /**
     * 蒐集使用者所有「權限 ID」集合（不含 Spring 角色 authority）。
     * 直接取自 role.getPermissions()，不靠 authority 字串前綴判斷——
     * 因權限本身可能命名為 ROLE_VIEW / ROLE_MANAGE（RBAC 管理權），
     * 若用 "ROLE_" 前綴過濾會誤刪這些權限，導致權限管理選單在側欄消失。
     */
    private Set<String> getPermissionIds(User user) {
        Set<String> permissionIds = new HashSet<>();
        if (user.getRoles() != null) {
            for (tw.com.ispan.backend.organizer.entity.Role role : user.getRoles()) {
                if (role.getPermissions() != null) {
                    for (tw.com.ispan.backend.organizer.entity.Permission perm : role.getPermissions()) {
                        permissionIds.add(perm.getPermissionId());
                    }
                }
            }
        }
        return permissionIds;
    }

    public static class CustomUserDetails implements UserDetails {
        private final User user;
        private final Collection<? extends GrantedAuthority> authorities;
        private final Set<String> permissions;

        public CustomUserDetails(User user, Collection<? extends GrantedAuthority> authorities,
                Set<String> permissions) {
            this.user = user;
            this.authorities = authorities;
            this.permissions = permissions;
        }

        public String getUserId() {
            return user.getUserId();
        }

        public String getName() {
            return user.getName();
        }

        public String getAvatarUrl() {
            return user.getAvatarUrl();
        }

        public Boolean getMustChangePassword() {
            return user.getMustChangePassword();
        }

        public Set<String> getPermissions() {
            return permissions;
        }

        @Override
        public Collection<? extends GrantedAuthority> getAuthorities() {
            return this.authorities;
        }

        @Override
        public String getPassword() {
            // 回傳資料庫儲存的密碼雜湊值
            return user.getPasswordHash();
        }

        @Override
        public String getUsername() {
            // 在我們的系統中，登入識別碼是 Email
            return user.getEmail();
        }

        @Override
        public boolean isAccountNonExpired() {
            return true; // 帳號未過期
        }

        @Override
        public boolean isAccountNonLocked() {
            // 🔒 驗證防爆破鎖定機制：
            // 如果 lockedUntil 不為 null 且時間在目前系統時間之後，代表帳號被鎖定中，回傳 false
            if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
                return false;
            }
            return true; // 未被鎖定
        }

        @Override
        public boolean isCredentialsNonExpired() {
            return true; // 密碼憑證未過期
        }

        @Override
        public boolean isEnabled() {
            // 帳號必須是啟用狀態 (isActive) 且未被軟刪除 (!isDeleted)
            return Boolean.TRUE.equals(user.getIsActive()) && !Boolean.TRUE.equals(user.getIsDeleted());
        }
    }
}
