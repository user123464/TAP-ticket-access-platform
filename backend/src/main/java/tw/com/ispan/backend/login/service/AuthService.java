package tw.com.ispan.backend.login.service;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.JwtTokenProvider;
import tw.com.ispan.backend.login.dto.AuthResponse;
import tw.com.ispan.backend.login.dto.CheckEmailResponse;
import tw.com.ispan.backend.login.dto.LoginRequest;
import tw.com.ispan.backend.login.dto.RegisterRequest;
import tw.com.ispan.backend.login.dto.UserSessionResponse;
import java.util.stream.Collectors;
import tw.com.ispan.backend.login.entity.LoginAttempt;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.entity.UserSession;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.enums.LoginFailureReason;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.login.enums.RevokedType;
import tw.com.ispan.backend.login.repository.LoginAttemptRepository;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.login.repository.UserSessionRepository;
import java.util.List;
import tw.com.ispan.backend.organizer.entity.OrganizerInvitation;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerInvitationRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final LoginAttemptRepository loginAttemptRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RedisService redisService;
    private final EmailService emailService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final RoleRepository roleRepository;
    private final OrganizerInvitationRepository organizerInvitationRepository;
    private final OrganizerMemberRepository organizerMemberRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationInSeconds;

    /**
     * 發送 6 位數一次性驗證碼 (OTP)
     */
    public void sendOtp(String email) {
        // 1. 生成 6 位隨機數字
        String otpCode = String.format("%06d", new Random().nextInt(1000000));
        log.info("為信箱 {} 生成驗證碼: {}", email, otpCode);

        // 2. 存入 Redis 快取，有效期限 300 秒 (5 分鐘)
        String key = "otp:code:" + email;
        redisService.setWithTtl(key, otpCode, 300);

        // 3. 呼叫 Email 服務寄信
        emailService.sendOtpEmail(email, otpCode);
    }

    /**
     * 校驗驗證碼，若正確則寫入 10 分鐘內有效的短期驗證憑證
     */
    public boolean verifyOtp(String email, String code) {
        String key = "otp:code:" + email;
        String cachedCode = redisService.get(key);

        if (cachedCode != null && cachedCode.equals(code)) {
            // 驗證成功，寫入安全憑證 (有效期 10 分鐘)
            String verifiedKey = "otp:verified:" + email;
            redisService.setWithTtl(verifiedKey, "true", 600);

            // 單次有效：驗證成功後立刻移除驗證碼，防止重放攻擊
            redisService.delete(key);
            log.info("Email {} 驗證碼校驗成功，安全憑證已生效", email);
            return true;
        }

        log.warn("Email {} 驗證碼校驗失敗", email);
        return false;
    }

    /**
     * 重設密碼邏輯
     */
    @Transactional
    public void resetPassword(String email, String code, String newPassword) {
        log.info("開始處理重設密碼，Email: {}", email);

        // 1. 檢查使用者是否存在
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("找不到該帳號的使用者"));

        // 2. 檢查安全憑證是否存在於 Redis (確認先前已通過驗證碼校驗)
        String verifiedKey = "otp:verified:" + email;
        String isVerified = redisService.get(verifiedKey);
        if (!"true".equals(isVerified)) {
            log.warn("重設密碼拒絕，Email {} 尚未通過驗證碼校驗或驗證已逾時", email);
            throw new IllegalArgumentException("驗證已逾時或無效，請重新獲取驗證碼");
        }

        // 3. 更新密碼
        String encodedPassword = passwordEncoder.encode(newPassword);
        user.setPasswordHash(encodedPassword);
        userRepository.save(user);

        // 4. 清除安全憑證與快取
        redisService.delete(verifiedKey);
        log.info("Email {} 密碼重設成功", email);
    }

    /**
     * 註冊帳號邏輯 (增強安全性：需檢驗 Redis 安全憑證)
     */
    /**
     * 為一般使用者(B2C)指派預設平台角色 BUYER。
     * 註冊與 Google 首次登入建立帳號時呼叫，使其 authorities 含 BUYER 對應的權限
     * (PROFILE_VIEW/EDIT、OWN_ORDER_VIEW 等)。
     */
    private void assignDefaultBuyerRole(User user) {
        roleRepository.findById("BUYER").ifPresentOrElse(
                buyer -> {
                    user.getRoles().add(buyer);
                    userRepository.save(user);
                },
                () -> log.warn("找不到平台角色 BUYER，無法為使用者 {} 指派預設角色", user.getUserId()));
    }

    @Transactional
    public void register(RegisterRequest request) {
        log.info("開始處理使用者註冊請求，Email: {}", request.getEmail());

        // 1. 檢查 Email 是否已被使用
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            log.warn("註冊失敗，Email 已被使用: {}", request.getEmail());
            throw new IllegalArgumentException("此電子信箱已被註冊");
        }

        // 2. 檢查 Redis 安全憑證是否有效
        String verifiedKey = "otp:verified:" + request.getEmail();
        String isVerified = redisService.get(verifiedKey);
        if (!"true".equals(isVerified)) {
            log.warn("註冊被拒絕，Email {} 尚未通過驗證碼校驗或已逾時", request.getEmail());
            throw new IllegalArgumentException("信箱驗證已逾時，請重新獲取驗證碼");
        }

        // 3. 從 seq_USR 獲取下一個序列值，並格式化為 USRXXXXXXX (長度 10)
        Long nextVal = userRepository.getNextUserSequenceValue();
        String userId = String.format("USR%07d", nextVal);
        log.info("產生新使用者 ID: {}", userId);

        // 4. 密碼加密
        String encodedPassword = passwordEncoder.encode(request.getPassword());

        // 5. 建立並儲存 User 實體
        User user = User.builder()
                .userId(userId)
                .email(request.getEmail())
                .passwordHash(encodedPassword)
                .name(request.getName())
                .phone(request.getPhone())
                .gender(request.getGender())
                .birthDate(request.getBirthDate())
                .address(request.getAddress())
                .authProvider(AuthProvider.LOCAL)
                .isActive(true)
                .isDeleted(false)
                .build();

        userRepository.save(user);
        assignDefaultBuyerRole(user); // 一般註冊者預設指派 BUYER 平台角色

        // 處理未註冊前的組織成員邀請，自動將新註冊使用者與組織進行關聯綁定
        processPendingInvitations(user);

        // 6. 註冊成功，清除安全憑證
        redisService.delete(verifiedKey);
        log.info("使用者註冊成功，Email: {}", request.getEmail());
    }

    /**
     * 檢查並處理待處理的組織邀請，自動關聯新註冊用戶至邀請組織。
     */
    private void processPendingInvitations(User user) {
        log.info("開始檢查信箱 {} 是否有待處理的組織邀請...", user.getEmail());
        try {
            // 查詢所有針對該 Email 的 PENDING (0) 狀態暫存邀請
            List<OrganizerInvitation> invites = organizerInvitationRepository.findByEmailAndStatus(user.getEmail(), 0);
            for (OrganizerInvitation invite : invites) {
                // 檢查邀請是否過期
                if (invite.getInviteTokenExpires() != null && invite.getInviteTokenExpires().isBefore(LocalDateTime.now())) {
                    log.warn("邀請已過期，組織: {}, 過期時間: {}", invite.getOrganizer().getName(), invite.getInviteTokenExpires());
                    invite.setStatus(2); // 2=EXPIRED
                    organizerInvitationRepository.save(invite);
                    continue;
                }

                log.info("發現有效邀請：組織: {}, 角色: {}，執行自動加入組織綁定...", 
                        invite.getOrganizer().getName(), 
                        invite.getRole() != null ? invite.getRole().getRoleName() : "無特定角色");

                // 使用序列生成成員 ID MBRXXXXXXX
                Long nextVal = organizerMemberRepository.getNextMemberSequenceValue();
                String memberId = String.format("MBR%07d", nextVal);

                // 建立正式的組織成員關係，狀態直接設為已接受 (ACCEPTED)
                OrganizerMember member = OrganizerMember.builder()
                        .memberId(memberId)
                        .organizer(invite.getOrganizer())
                        .user(user)
                        .invitedBy(invite.getInvitedBy())
                        .role(invite.getRole())
                        .status(OrganizerMemberStatus.ACCEPTED)
                        .joinedAt(LocalDateTime.now())
                        .build();

                organizerMemberRepository.save(member);

                // 將暫存邀請記錄更新為已接受 (1)
                invite.setStatus(1); // 1=ACCEPTED
                organizerInvitationRepository.save(invite);
                
                log.info("使用者 {} 已成功自動綁定加入組織: {}", user.getEmail(), invite.getOrganizer().getName());
            }
        } catch (Exception e) {
            // 容錯防禦：避免自動關聯階段的問題影響使用者的核心註冊主流程
            log.error("自動綁定組織邀請發生異常，Email: {}", user.getEmail(), e);
        }
    }

    /**
     * 登入驗證邏輯 (同步載入 Redis Session)
     */
    @Transactional
    public AuthResponse login(LoginRequest request, String ipAddress, String userAgent) {
        log.info("開始處理登入驗證，Email: {}, IP: {}", request.getEmail(), ipAddress);

        // 1. 防暴力破解檢查：確認過去 15 分鐘內該 Email 的失敗次數
        LocalDateTime lockTimeLimit = LocalDateTime.now().minusMinutes(15);
        long failCount = loginAttemptRepository.countByEmailAndSuccessFalseAndAttemptedAtAfter(request.getEmail(), lockTimeLimit);
        
        if (failCount >= 5) {
            log.warn("登入拒絕，該 Email 在 15 分鐘內失敗達 {} 次，帳號處於鎖定狀態: {}", failCount, request.getEmail());
            logFailedAttempt(request.getEmail(), ipAddress, LoginFailureReason.ACCOUNT_LOCKED);
            throw new RuntimeException("帳號因連續登入失敗已被鎖定，請於 15 分鐘後再試");
        }

        // 2. 尋找使用者
        Optional<User> userOpt = userRepository.findByEmail(request.getEmail());
        if (userOpt.isEmpty()) {
            log.warn("登入失敗，找不到使用者: {}", request.getEmail());
            logFailedAttempt(request.getEmail(), ipAddress, LoginFailureReason.USER_NOT_FOUND);
            throw new RuntimeException("帳號或密碼錯誤");
        }

        User user = userOpt.get();

        // 3. 檢查帳號是否鎖定（針對資料庫中 lockedUntil 的額外確認）
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            log.warn("登入失敗，帳號鎖定中，解鎖時間: {}, Email: {}", user.getLockedUntil(), request.getEmail());
            logFailedAttempt(request.getEmail(), ipAddress, LoginFailureReason.ACCOUNT_LOCKED);
            throw new RuntimeException("帳號因連續登入失敗已被鎖定，請於 15 分鐘後再試");
        }

        // 4. 檢查帳號是否停用或被刪除
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            log.warn("登入失敗，帳號已停用或被刪除，Email: {}", request.getEmail());
            logFailedAttempt(request.getEmail(), ipAddress, LoginFailureReason.ACCOUNT_DISABLED);
            throw new RuntimeException("此帳號已被停用，請聯絡系統管理員");
        }

        // 5. 驗證密碼
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            log.warn("登入失敗，密碼錯誤，Email: {}", request.getEmail());
            
            // 累計失敗次數 (包含這一次)
            long currentFails = failCount + 1;
            if (currentFails >= 5) {
                user.setLockedUntil(LocalDateTime.now().plusMinutes(15));
                userRepository.save(user);
                log.warn("密碼錯誤達到 5 次，帳號已被鎖定 15 分鐘，Email: {}", request.getEmail());
            }
            
            logFailedAttempt(request.getEmail(), ipAddress, LoginFailureReason.WRONG_PASSWORD);
            throw new RuntimeException("帳號或密碼錯誤");
        }

        // 6. 密碼正確：清除鎖定狀態 (若有)
        if (user.getLockedUntil() != null) {
            user.setLockedUntil(null);
            userRepository.save(user);
        }

        // 7. 若帳號已啟用 2FA，則不直接發 Token，改為寄出登入驗證碼並要求第二步驗證
        if (Boolean.TRUE.equals(user.getIsTwoFactorEnabled())) {
            sendTwoFactorOtp(user.getEmail(), "2fa:login:");
            log.info("帳號 {} 已啟用 2FA，已寄出登入驗證碼，等待第二步驗證", user.getEmail());
            return AuthResponse.builder()
                    .twoFactorRequired(true)
                    .email(user.getEmail())
                    .build();
        }

        // 8. 無 2FA：記錄成功嘗試並直接發放會話
        recordSuccessfulLogin(user.getEmail(), ipAddress);
        return issueSession(user, ipAddress, userAgent);
    }

    /**
     * 完成 2FA 登入第二步：驗證寄至信箱的登入驗證碼，正確則發放 JWT。
     */
    @Transactional
    public AuthResponse completeTwoFactorLogin(String email, String code, String ipAddress, String userAgent) {
        log.info("開始處理 2FA 登入第二步驗證，Email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("帳號或驗證碼錯誤"));

        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            throw new RuntimeException("此帳號已被停用，請聯絡系統管理員");
        }

        // 校驗登入驗證碼
        String key = "2fa:login:" + email;
        String cachedCode = redisService.get(key);
        if (cachedCode == null || !cachedCode.equals(code)) {
            log.warn("2FA 登入驗證失敗，Email: {}", email);
            throw new RuntimeException("驗證碼不正確或已過期");
        }
        redisService.delete(key); // 單次有效

        recordSuccessfulLogin(email, ipAddress);
        return issueSession(user, ipAddress, userAgent);
    }

    /**
     * 產生 6 位數驗證碼，存入 Redis (5 分鐘) 並寄送 2FA 信件。
     * keyPrefix 區分用途：登入用 "2fa:login:"、設定啟用用 "2fa:setup:"
     */
    private void sendTwoFactorOtp(String email, String keyPrefix) {
        String otpCode = String.format("%06d", new java.util.Random().nextInt(1000000));
        redisService.setWithTtl(keyPrefix + email, otpCode, 300);
        emailService.send2faEmail(email, otpCode);
    }

    /**
     * 寄送「啟用 2FA」設定用驗證碼給目前登入的使用者。
     */
    @Transactional(readOnly = true)
    public void sendTwoFactorSetupOtp(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));
        assertLocalAccountForTwoFactor(user);
        sendTwoFactorOtp(user.getEmail(), "2fa:setup:");
        log.info("已寄送啟用 2FA 設定驗證碼至 {}", user.getEmail());
    }

    /**
     * Google 等第三方帳號的安全性由提供者本身的兩步驟驗證保護，不提供本系統 App 層 2FA。
     */
    private void assertLocalAccountForTwoFactor(User user) {
        if (user.getAuthProvider() != AuthProvider.LOCAL) {
            throw new IllegalArgumentException("此帳號使用第三方快速登入，帳號安全由 Google 兩步驟驗證保護，無須在本系統啟用 2FA");
        }
    }

    /**
     * 啟用或關閉 2FA。啟用時需驗證寄至信箱的設定驗證碼；關閉時直接生效（使用者已登入）。
     */
    @Transactional
    public void setTwoFactorEnabled(String userId, boolean enable, String code) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        if (enable) {
            assertLocalAccountForTwoFactor(user);
            String key = "2fa:setup:" + user.getEmail();
            String cachedCode = redisService.get(key);
            if (cachedCode == null || !cachedCode.equals(code)) {
                throw new IllegalArgumentException("驗證碼不正確或已過期");
            }
            redisService.delete(key);
            user.setIsTwoFactorEnabled(true);
            log.info("使用者 {} 已啟用 2FA", userId);
        } else {
            user.setIsTwoFactorEnabled(false);
            log.info("使用者 {} 已關閉 2FA", userId);
        }
        userRepository.save(user);
    }

    /**
     * 共用：記錄一筆成功登入嘗試。
     */
    private void recordSuccessfulLogin(String email, String ipAddress) {
        LoginAttempt successAttempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .success(true)
                .failureReason(null)
                .build();
        loginAttemptRepository.save(successAttempt);
    }

    /**
     * Google 第三方登入：驗證 ID Token、找出或建立使用者，並發放本系統 JWT。
     * - 已綁定 googleOauthId 者直接登入。
     * - email 已存在的本地帳號自動補綁定 googleOauthId（帳號合併）。
     * - 全新使用者則建立 authProvider=GOOGLE 的帳號。
     */
    @Transactional
    public AuthResponse loginWithGoogle(String idTokenString, String ipAddress, String userAgent) {
        log.info("開始處理 Google 登入，IP: {}", ipAddress);

        // 1. 驗證 Google ID Token（簽章、過期、audience）
        GoogleIdToken.Payload payload = googleTokenVerifier.verify(idTokenString);
        String googleSub = payload.getSubject();
        String email = payload.getEmail();
        Boolean emailVerified = payload.getEmailVerified();
        String name = (String) payload.get("name");
        String picture = (String) payload.get("picture");

        if (email == null || googleSub == null) {
            throw new RuntimeException("Google 帳號未提供必要資訊，無法登入");
        }

        // 2. 依 googleOauthId 找使用者
        User user = userRepository.findByGoogleOauthId(googleSub).orElse(null);

        // 3. 找不到則嘗試以 email 比對既有帳號，進行合併綁定
        if (user == null) {
            user = userRepository.findByEmail(email).orElse(null);
            if (user != null) {
                log.info("Google 登入：email {} 已有本地帳號，補綁定 googleOauthId", email);
                user.setGoogleOauthId(googleSub);
                userRepository.save(user);
            }
        }

        // 4. 全新使用者則建立帳號
        if (user == null) {
            Long nextVal = userRepository.getNextUserSequenceValue();
            String userId = String.format("USR%07d", nextVal);
            log.info("Google 登入：建立新使用者 {} (email: {})", userId, email);
            user = User.builder()
                    .userId(userId)
                    .email(email)
                    .name(name != null && !name.isBlank() ? name : email.split("@")[0])
                    .avatarUrl(picture)
                    .authProvider(AuthProvider.GOOGLE)
                    .googleOauthId(googleSub)
                    .emailVerifiedAt(Boolean.TRUE.equals(emailVerified) ? LocalDateTime.now() : null)
                    .isActive(true)
                    .isDeleted(false)
                    .build();
            user = userRepository.save(user);
            assignDefaultBuyerRole(user); // Google 首次登入建立的帳號預設指派 BUYER
        }

        // 5. 檢查帳號是否停用或被刪除
        if (!Boolean.TRUE.equals(user.getIsActive()) || Boolean.TRUE.equals(user.getIsDeleted())) {
            log.warn("Google 登入失敗，帳號已停用或被刪除，Email: {}", email);
            throw new RuntimeException("此帳號已被停用，請聯絡系統管理員");
        }

        // 6. 記錄成功登入並發放會話（Google 帳號不套用 2FA Email 驗證）
        recordSuccessfulLogin(user.getEmail(), ipAddress);
        return issueSession(user, ipAddress, userAgent);
    }

    /**
     * 共用：為已通過驗證的使用者產生 JWT、建立 UserSession、同步 Redis，並回傳 AuthResponse。
     */
    private AuthResponse issueSession(User user, String ipAddress, String userAgent) {
        // 1. 產生 JWT Token
        String token = jwtTokenProvider.generateToken(user);
        String jti = jwtTokenProvider.getJtiFromToken(token);

        // 2. 建立並儲存登入會話 UserSession (資料庫審計)
        UserSession session = UserSession.builder()
                .user(user)
                .tokenJti(jti)
                .portalType(PortalType.B2C_FRONT)
                .ipAddress(ipAddress)
                .userAgent(userAgent)
                .expiresAt(LocalDateTime.now().plusSeconds(jwtExpirationInSeconds))
                .build();

        userSessionRepository.save(session);

        // 3. 同步快取至 Redis (高併發快速校驗)
        redisService.addSession(user.getUserId(), jti, jwtExpirationInSeconds);
        log.info("使用者登入成功，已同步 JTI 至 Redis 集合 (UserId: {}, JTI: {})", user.getUserId(), jti);

        // 4. 解析使用者角色
        String roleCode = null;
        if (user.getRoles() != null) {
            for (tw.com.ispan.backend.organizer.entity.Role role : user.getRoles()) {
                String id = role.getRoleId();
                if ("SUPER_ADMIN".equals(id) || "ADMIN".equals(id) || "CUSTOMER_SERVICE".equals(id)) {
                    roleCode = id;
                    break;
                }
            }
            if (roleCode == null && !user.getRoles().isEmpty()) {
                roleCode = user.getRoles().iterator().next().getRoleId();
            }
        }

        // 5. 回傳認證結果
        return AuthResponse.builder()
                .accessToken(token)
                .tokenType("Bearer")
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .avatarUrl(user.getAvatarUrl())
                .roleCode(roleCode)
                .mustChangePassword(user.getMustChangePassword() != null ? user.getMustChangePassword() : false)
                .build();
    }

    /**
     * 登出撤銷 Token 邏輯 (同步撤銷 Redis 快取)
     */
    @Transactional
    public void logout(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            log.warn("登出失敗，無效的 Authorization Header");
            throw new IllegalArgumentException("無效的認證標頭");
        }

        String token = authHeader.substring(7);

        // 驗證 Token 是否合法
        if (!jwtTokenProvider.validateToken(token)) {
            log.warn("登出失敗，JWT Token 驗證無效");
            throw new IllegalArgumentException("無效的 JWT Token");
        }

        // 從 Token 取得 jti 與 userId
        String jti = jwtTokenProvider.getJtiFromToken(token);
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        
        // 1. 尋找資料庫對應會話並標記註銷
        UserSession session = userSessionRepository.findByTokenJti(jti)
                .orElseThrow(() -> {
                    log.warn("登出失敗，資料庫中找不到該 jti: {}", jti);
                    return new RuntimeException("無效的登入會話，可能已過期或不存在");
                });

        if (session.getRevokedAt() == null) {
            session.setRevokedAt(LocalDateTime.now());
            session.setRevokedBy(RevokedType.SELF);
            userSessionRepository.save(session);
        }

        // 2. 同步移出 Redis 快取
        redisService.revokeSession(userId, jti);
        log.info("使用者登出成功，已註銷資料庫會話並清除 Redis 快取 (UserId: {}, JTI: {})", userId, jti);
    }

    /**
     * 登出所有其他裝置 (保留目前連線的 Token)
     */
    @Transactional
    public void logoutOtherDevices(String userId, String currentJti) {
        log.info("使用者 {} 發起登出所有其他裝置請求，當前 JTI: {}", userId, currentJti);

        // 1. 清空 Redis 中該使用者的所有會話
        redisService.revokeAllSessions(userId);

        // 2. 將當前使用中的 JTI 重新放回，防止自己被登出
        redisService.addSession(userId, currentJti, jwtExpirationInSeconds);

        // 3. 在 SQL 資料庫中，將該使用者除了當前 JTI 以外的所有有效會話標記為撤銷
        userSessionRepository.findByUserUserIdAndRevokedAtIsNull(userId).stream()
                .filter(session -> !session.getTokenJti().equals(currentJti))
                .forEach(session -> {
                    session.setRevokedAt(LocalDateTime.now());
                    session.setRevokedBy(RevokedType.SYSTEM); // 系統撤銷
                    userSessionRepository.save(session);
                });
                
        log.info("已完成使用者 {} 其它裝置的登出清理", userId);
    }

    /**
     * 撤銷某使用者的「所有」登入會話（Redis + DB），用於刪除帳號等情境。
     */
    @Transactional
    public void revokeAllUserSessions(String userId) {
        // 先處理資料庫會話（在交易內，可隨交易 rollback）
        userSessionRepository.findByUserUserIdAndRevokedAtIsNull(userId)
                .forEach(session -> {
                    session.setRevokedAt(LocalDateTime.now());
                    session.setRevokedBy(RevokedType.SYSTEM);
                    userSessionRepository.save(session);
                });
        // 最後才清 Redis（非交易性、無法 rollback）：放最後可避免「Redis 已清但 DB rollback」
        // 造成的全站 403 殭屍狀態
        redisService.revokeAllSessions(userId);
        log.info("已撤銷使用者 {} 的所有登入會話", userId);
    }

    /**
     * 獲取目前使用者的所有有效登入會話
     */
    @Transactional(readOnly = true)
    public List<UserSessionResponse> getActiveSessions(String userId, String currentJti) {
        log.info("查詢使用者 {} 的有效登入會話, 目前 JTI: {}", userId, currentJti);
        LocalDateTime now = LocalDateTime.now();
        return userSessionRepository.findByUserUserIdAndRevokedAtIsNull(userId).stream()
                .filter(session -> session.getExpiresAt().isAfter(now))
                .filter(session -> redisService.isSessionActive(userId, session.getTokenJti()))
                .map(session -> UserSessionResponse.builder()
                        .sessionId(session.getSessionId())
                        .ipAddress(session.getIpAddress())
                        .userAgent(session.getUserAgent())
                        .createdAt(session.getCreatedAt())
                        .expiresAt(session.getExpiresAt())
                        .isCurrent(session.getTokenJti().equals(currentJti))
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * 撤銷指定會話
     */
    @Transactional
    public void revokeSession(String userId, Long sessionId) {
        log.info("使用者 {} 請求撤銷會話 sessionId: {}", userId, sessionId);
        UserSession session = userSessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該登入會話"));

        if (!session.getUser().getUserId().equals(userId)) {
            throw new IllegalArgumentException("無權操作此會話");
        }

        if (session.getRevokedAt() == null) {
            session.setRevokedAt(LocalDateTime.now());
            session.setRevokedBy(RevokedType.SELF);
            userSessionRepository.save(session);
        }

        redisService.revokeSession(userId, session.getTokenJti());
        log.info("已完成使用者 {} 裝置會話 {} 的登出清理", userId, sessionId);
    }

    /**
     * 檢查 Email 是否已被使用
     */
    @Transactional(readOnly = true)
    public boolean isEmailRegistered(String email) {
        return userRepository.findByEmail(email).isPresent();
    }

    /**
     * 檢查 Email 的註冊狀態與登入方式（供前端分流：Google 帳號不應走密碼登入）。
     */
    @Transactional(readOnly = true)
    public CheckEmailResponse checkEmail(String email) {
        return userRepository.findByEmail(email)
                .map(u -> CheckEmailResponse.builder()
                        .exists(true)
                        .authProvider(u.getAuthProvider() != null ? u.getAuthProvider().name() : "LOCAL")
                        .build())
                .orElse(CheckEmailResponse.builder().exists(false).authProvider(null).build());
    }

    /**
     * 私有輔助方法：寫入登入失敗紀錄
     */
    private void logFailedAttempt(String email, String ipAddress, LoginFailureReason reason) {
        LoginAttempt attempt = LoginAttempt.builder()
                .email(email)
                .ipAddress(ipAddress)
                .attemptedAt(LocalDateTime.now())
                .success(false)
                .failureReason(reason)
                .build();
        loginAttemptRepository.save(attempt);
    }
}
