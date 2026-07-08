package tw.com.ispan.backend.login.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.dto.AdminStaffSummary;
import tw.com.ispan.backend.login.dto.AdminUserDetail;
import tw.com.ispan.backend.login.dto.AdminUserSummary;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.repository.LoginAttemptRepository;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.entity.Role;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;

/**
 * Admin 後台「使用者管理」業務邏輯。
 *
 * <p>地基批次：重用 {@link UserRepository} / {@link LoginAttemptRepository} 與
 * 組織關聯 repository，提供清單查詢（含篩選）、詳情（個資 + 登入紀錄 + 所屬組織角色）
 * 與帳號操作（啟用 / 停用 / 解鎖 / 軟刪除）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final OrganizerRepository organizerRepository;
    private final OrganizerMemberRepository organizerMemberRepository;
    private final AuthService authService;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    /** 與註冊一致：每個帳號掛 BUYER 基礎角色（PROFILE/OWN_ORDER 等），再加上平台角色 */
    private static final String BASELINE_ROLE = "BUYER";
    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    /**
     * 列出使用者（含已軟刪除），於記憶體中套用搜尋與篩選條件。
     *
     * @param keyword      email / 姓名 模糊比對（不分大小寫；null/空白表示不限）
     * @param status       active / locked / disabled / deleted（null/空白表示不限）
     * @param verified     "true"/"false"（null/空白表示不限）
     * @param authProvider LOCAL / GOOGLE（null/空白表示不限）
     */
    @Transactional(readOnly = true)
    public List<AdminUserSummary> listUsers(String keyword, String status, String verified, String authProvider) {
        List<User> users = userRepository.findAllIncludingDeleted();
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        LocalDateTime now = LocalDateTime.now();

        return users.stream()
                .filter(u -> kw.isEmpty()
                        || (u.getEmail() != null && u.getEmail().toLowerCase().contains(kw))
                        || (u.getName() != null && u.getName().toLowerCase().contains(kw)))
                .filter(u -> status == null || status.isBlank() || statusOf(u, now).equals(status))
                .filter(u -> verified == null || verified.isBlank()
                        || Boolean.parseBoolean(verified) == (u.getEmailVerifiedAt() != null))
                .filter(u -> authProvider == null || authProvider.isBlank()
                        || (u.getAuthProvider() != null && u.getAuthProvider().name().equalsIgnoreCase(authProvider)))
                .map(this::toSummary)
                .collect(Collectors.toList());
    }

    /**
     * 由 SUPER_ADMIN 直接建立「員工帳號」（平台角色），並設定初始密碼。
     *
     * <p>與一般註冊不同：不走信箱 OTP 驗證流程，管理員建立即視為已驗證
     * （emailVerifiedAt 設為當下），帳號直接啟用可登入。指派 BUYER 基礎角色
     * 外加所選平台角色（ADMIN / CUSTOMER_SERVICE）。為避免提權，本介面不可建立
     * SUPER_ADMIN，亦不可指派任何組織級角色。</p>
     *
     * @return 建立後的帳號詳情（與詳情頁同結構）
     */
    @Transactional
    public AdminUserDetail createStaffAccount(String email, String name, String rawPassword, String roleId) {
        // 1. 欄位驗證
        String normalizedEmail = email == null ? "" : email.trim().toLowerCase();
        if (normalizedEmail.isBlank() || !EMAIL_PATTERN.matcher(normalizedEmail).matches()) {
            throw new IllegalArgumentException("請輸入有效的 Email");
        }
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("請輸入姓名");
        }
        if (rawPassword == null || rawPassword.length() < 8) {
            throw new IllegalArgumentException("初始密碼至少需 8 碼");
        }
        if (roleId == null || roleId.isBlank()) {
            throw new IllegalArgumentException("請選擇平台角色");
        }

        // 2. Email 唯一（含已軟刪除帳號，避免唯一鍵衝突）
        if (userRepository.findByEmailIncludingDeleted(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("此電子信箱已被使用");
        }

        // 3. 取角色，須為「內部人員角色」：平台層（organizer 為 null）且 is_editable=true，
        //    藉此排除系統身分角色（SUPER_ADMIN / BUYER / ORGANIZER，皆 is_editable=0，避免提權）與組織級角色。
        Role platformRole = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("找不到角色：" + roleId));
        if (platformRole.getOrganizer() != null || !Boolean.TRUE.equals(platformRole.getIsEditable())) {
            throw new IllegalArgumentException("僅可指派內部人員角色（不可為系統身分角色或組織級角色）");
        }
        Role baselineRole = roleRepository.findById(BASELINE_ROLE)
                .orElseThrow(() -> new IllegalArgumentException("系統缺少基礎角色 " + BASELINE_ROLE));

        // 4. 產生 userId、加密密碼、建立帳號
        Long nextVal = userRepository.getNextUserSequenceValue();
        String userId = String.format("USR%07d", nextVal);
        User user = User.builder()
                .userId(userId)
                .email(normalizedEmail)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .name(name.trim())
                .authProvider(AuthProvider.LOCAL)
                .emailVerifiedAt(LocalDateTime.now()) // 管理員建立 → 視為已驗證，免走 OTP
                .isActive(true)
                .isDeleted(false)
                .mustChangePassword(true)
                .build();
        userRepository.save(user);

        user.getRoles().add(baselineRole);
        user.getRoles().add(platformRole);
        userRepository.save(user);

        log.info("SUPER_ADMIN 建立員工帳號成功 userId={}, email={}, role={}", userId, normalizedEmail, roleId);
        return getUserDetail(userId);
    }

    @Transactional(readOnly = true)
    public AdminUserDetail getUserDetail(String userId) {
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        // 所屬組織與角色：Owner 視為 OWNER；其餘以 ACCEPTED 成員的組織角色名稱呈現
        List<AdminUserDetail.OrgRole> orgRoles = new ArrayList<>();
        for (Organizer org : organizerRepository.findByOwner(user)) {
            orgRoles.add(new AdminUserDetail.OrgRole(org.getOrganizerId(), org.getName(), "OWNER"));
        }
        for (OrganizerMember member : organizerMemberRepository.findByUser(user)) {
            if (member.getStatus() == OrganizerMemberStatus.ACCEPTED && member.getOrganizer() != null) {
                Organizer org = member.getOrganizer();
                // 略過已以 OWNER 收錄的同一組織，避免重複
                boolean alreadyOwner = orgRoles.stream().anyMatch(r -> r.orgId().equals(org.getOrganizerId()));
                if (alreadyOwner) {
                    continue;
                }
                String roleName = member.getRole() != null ? member.getRole().getRoleName() : "MEMBER";
                orgRoles.add(new AdminUserDetail.OrgRole(org.getOrganizerId(), org.getName(), roleName));
            }
        }

        // 平台角色（organizer 為 null）：editable=true 者為可加掛/取下的內部人員角色
        List<AdminUserDetail.PlatformRole> platformRoles = user.getRoles().stream()
                .filter(r -> r.getOrganizer() == null)
                .sorted((a, b) -> a.getRoleId().compareTo(b.getRoleId()))
                .map(r -> new AdminUserDetail.PlatformRole(
                        r.getRoleId(), r.getRoleName(), Boolean.TRUE.equals(r.getIsEditable())))
                .collect(Collectors.toList());

        // 登入嘗試紀錄（依該帳號 email 查詢，已是時間降序）
        List<AdminUserDetail.LoginAttemptRow> attempts = loginAttemptRepository
                .findByEmailOrderByAttemptedAtDesc(user.getEmail())
                .stream()
                .map(a -> new AdminUserDetail.LoginAttemptRow(
                        a.getAttemptId(),
                        a.getAttemptedAt(),
                        a.getIpAddress(),
                        Boolean.TRUE.equals(a.getSuccess()),
                        a.getFailureReason() != null ? a.getFailureReason().name() : null))
                .collect(Collectors.toList());

        return new AdminUserDetail(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getPhone(),
                user.getAuthProvider() != null ? user.getAuthProvider().name() : null,
                user.getEmailVerifiedAt() != null,
                Boolean.TRUE.equals(user.getIsActive()),
                user.getLockedUntil(),
                Boolean.TRUE.equals(user.getIsDeleted()),
                user.getCreatedAt(),
                lastLoginAt(user.getEmail()),
                platformRoles,
                orgRoles,
                attempts);
    }

    /** Admin RBAC：列出內部人員（持有內部人員角色的帳號），含其內部角色名稱。 */
    @Transactional(readOnly = true)
    public List<AdminStaffSummary> listStaff() {
        return userRepository.findStaffUsers().stream()
                .map(u -> new AdminStaffSummary(
                        u.getUserId(),
                        u.getEmail(),
                        u.getName(),
                        Boolean.TRUE.equals(u.getIsActive()),
                        u.getLockedUntil(),
                        Boolean.TRUE.equals(u.getIsDeleted()),
                        u.getCreatedAt(),
                        lastLoginAt(u.getEmail()),
                        u.getRoles().stream()
                                .filter(r -> r.getOrganizer() == null && Boolean.TRUE.equals(r.getIsEditable()))
                                .map(r -> r.getRoleName())
                                .sorted()
                                .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    /** 加掛內部人員角色給帳號（支撐臨時加權）。僅限內部人員角色（平台層、is_editable=true）。 */
    @Transactional
    public AdminUserDetail assignRole(String userId, String roleId) {
        User user = requireUser(userId);
        Role role = requireInternalRole(roleId);
        user.getRoles().add(role);
        userRepository.save(user);
        log.info("管理員為帳號 {} 加掛角色 {}", userId, roleId);
        return getUserDetail(userId);
    }

    /** 取下帳號的內部人員角色。不可移除基礎角色 BUYER；僅限內部人員角色。 */
    @Transactional
    public AdminUserDetail removeRole(String userId, String roleId) {
        if (BASELINE_ROLE.equals(roleId)) {
            throw new IllegalArgumentException("不可移除基礎角色");
        }
        User user = requireUser(userId);
        requireInternalRole(roleId);
        user.getRoles().removeIf(r -> r.getRoleId().equals(roleId));
        userRepository.save(user);
        log.info("管理員為帳號 {} 取下角色 {}", userId, roleId);
        return getUserDetail(userId);
    }

    /** 取內部人員角色：須存在、平台層（organizer 為 null）且 is_editable=true（排除系統身分與組織角色，防提權）。 */
    private Role requireInternalRole(String roleId) {
        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new IllegalArgumentException("找不到角色：" + roleId));
        if (role.getOrganizer() != null || !Boolean.TRUE.equals(role.getIsEditable())) {
            throw new IllegalArgumentException("僅可加掛/取下內部人員角色（不可為系統身分或組織級角色）");
        }
        return role;
    }

    /** 啟用帳號（is_active = true，保留鎖定狀態不動） */
    @Transactional
    public void activate(String userId) {
        User user = requireUser(userId);
        userRepository.adminUpdateStatus(userId, true, user.getLockedUntil());
    }

    /** 停用帳號（is_active = false）並強制登出 */
    @Transactional
    public void deactivate(String userId) {
        User user = requireUser(userId);
        assertNotOwnerOfActiveOrg(user, "停用");
        userRepository.adminUpdateStatus(userId, false, user.getLockedUntil());
        authService.revokeAllUserSessions(userId);
    }

    /** 解除鎖定（清空 locked_until，保留 is_active） */
    @Transactional
    public void unlock(String userId) {
        User user = requireUser(userId);
        userRepository.adminUpdateStatus(userId, Boolean.TRUE.equals(user.getIsActive()), null);
    }

    /** 軟刪除帳號（is_deleted = 1）。Option A：擋下組織 owner、撤銷其成員資格、強制登出。 */
    @Transactional
    public void softDelete(String userId) {
        User user = requireUser(userId);

        // Owner 防呆（Option A）：是任何非封存組織的 owner 即不允許刪除。
        assertNotOwnerOfActiveOrg(user, "刪除");

        // Member（Option A）：撤銷該使用者所有 ACCEPTED 成員資格，阻斷其進入各組織後台。
        for (OrganizerMember m : organizerMemberRepository.findByUser(user)) {
            if (m.getStatus() == OrganizerMemberStatus.ACCEPTED) {
                m.setStatus(OrganizerMemberStatus.REVOKED);
                organizerMemberRepository.save(m);
            }
        }

        userRepository.softDeleteById(userId);
        authService.revokeAllUserSessions(userId);
    }

    /**
     * Option A：使用者若為任何「非封存(ARCHIVED)」組織的擁有者，不允許停用/刪除。
     * owner_user_id 為單值，owner 必為唯一擁有者；須先封存該組織或轉移擁有權後再處置。
     */
    private void assertNotOwnerOfActiveOrg(User user, String actionLabel) {
        for (Organizer org : organizerRepository.findByOwner(user)) {
            if (org.getStatus() != OrganizerStatus.ARCHIVED) {
                throw new IllegalArgumentException(
                        "無法" + actionLabel + "：此帳號為組織「" + org.getName() + "」的擁有者。"
                                + "請先封存該組織或將擁有權轉移後再操作。");
            }
        }
    }

    /** 恢復帳號（is_deleted = 0） */
    @Transactional
    public void restore(String userId) {
        User user = requireUser(userId);
        user.setIsDeleted(false);
        userRepository.save(user);
    }

    /** 強制登出（撤銷所有會話） */
    @Transactional
    public void forceLogout(String userId) {
        requireUser(userId);
        authService.revokeAllUserSessions(userId);
    }

    // ── private helpers ──

    private User requireUser(String userId) {
        return userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));
    }

    private AdminUserSummary toSummary(User u) {
        return new AdminUserSummary(
                u.getUserId(),
                u.getEmail(),
                u.getName(),
                u.getAuthProvider() != null ? u.getAuthProvider().name() : null,
                u.getEmailVerifiedAt() != null,
                Boolean.TRUE.equals(u.getIsActive()),
                u.getLockedUntil(),
                Boolean.TRUE.equals(u.getIsDeleted()),
                u.getCreatedAt(),
                lastLoginAt(u.getEmail()));
    }

    private LocalDateTime lastLoginAt(String email) {
        return loginAttemptRepository.findByEmailOrderByAttemptedAtDesc(email).stream()
                .filter(a -> Boolean.TRUE.equals(a.getSuccess()))
                .map(la -> la.getAttemptedAt())
                .findFirst()
                .orElse(null);
    }

    private String statusOf(User u, LocalDateTime now) {
        if (Boolean.TRUE.equals(u.getIsDeleted())) {
            return "deleted";
        }
        if (u.getLockedUntil() != null && u.getLockedUntil().isAfter(now)) {
            return "locked";
        }
        if (!Boolean.TRUE.equals(u.getIsActive())) {
            return "disabled";
        }
        return "active";
    }

    /** 重設內部人員密碼為預設密碼 Aa123456，並設定下次登入強制修改。 */
    @Transactional
    public void resetStaffPassword(String userId) {
        User user = userRepository.findByIdIncludingDeleted(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));
        user.setPasswordHash(passwordEncoder.encode("Aa123456"));
        user.setMustChangePassword(true);
        userRepository.save(user);
        log.info("管理員已將使用者 {} 密碼重設為預設值 Aa123456，並設定強制下次登入變更", userId);
    }
}
