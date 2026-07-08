package tw.com.ispan.backend.login.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.dto.DeletionEligibilityResponse;
import tw.com.ispan.backend.login.dto.UserProfileRequest;
import tw.com.ispan.backend.login.dto.UserProfileResponse;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.enums.AuthProvider;
import tw.com.ispan.backend.login.exception.AccountDeletionBlockedException;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserProfileService {

    /** 頭像 URL 前綴，用來判斷是否為本系統上傳的可刪除檔案 */
    private static final String AVATAR_URL_PREFIX = "/api/user/avatars/";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final OrganizerMemberRepository organizerMemberRepository;
    private final OrganizerRepository organizerRepository;
    private final AuthService authService;

    /**
     * 刪除帳號（軟刪除）。
     * 1. 若使用者是某個「運行中」（非 ARCHIVED）組織的所有權人，阻擋刪除，要求先轉移所有權或註銷組織。
     * 2. 若使用者僅是其他組織的一般成員（非 owner），刪帳號時自動退出這些組織。
     * 3. 對 User 執行軟刪除（@SQLDelete 會將 is_deleted 設為 1），並撤銷其所有登入會話。
     */
    @Transactional
    public void deleteAccount(String userId) {
        log.info("開始處理刪除帳號，使用者 ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        // 1. 資格檢查：是否為運行中組織的所有權人（與 GET .../deletion-eligibility 共用同一判斷邏輯）
        DeletionEligibilityResponse eligibility = evaluateDeletionEligibility(user);
        if (!eligibility.isAllowed()) {
            log.warn("刪除帳號遭阻擋，使用者 {} 原因: {}", userId, eligibility.getReason());
            throw new AccountDeletionBlockedException(eligibility.getReason());
        }

        // 2. 純 member（非 owner）身分：刪帳號時自動退出所有組織成員資格。
        //    不透過 OrganizerMemberService.leaveOrganization()，因為該方法：
        //    (a) 對 owner 會直接拋錯（此處已於步驟 1 排除運行中 owner 的情形）；
        //    (b) 僅接受 ACCEPTED 狀態，PENDING/REVOKED 記錄會導致其拋出 IllegalStateException。
        //    故此處直接批次將該使用者所有 organizer_member 記錄標記為 REVOKED。
        List<OrganizerMember> memberships = organizerMemberRepository.findByUser(user);
        if (!memberships.isEmpty()) {
            memberships.forEach(m -> m.setStatus(OrganizerMemberStatus.REVOKED));
            organizerMemberRepository.saveAll(memberships);
            log.info("使用者 {} 已自動退出 {} 筆組織成員資格", userId, memberships.size());
        }

        // 3. 先以確定性原生 UPDATE 完成軟刪除（is_deleted=1）。
        //    必須在撤銷會話「之前」確定成功，否則 Redis 會話已被清空但帳號未刪，
        //    會造成使用者全站 403 的殭屍狀態。
        int affected = userRepository.softDeleteById(userId);
        if (affected == 0) {
            throw new IllegalStateException("刪除帳號失敗：找不到可刪除的帳號");
        }
        log.info("使用者 {} 已軟刪除", userId);

        // 4. 軟刪除成功後，撤銷其所有登入會話，使既有 Token 立即失效
        authService.revokeAllUserSessions(userId);
    }

    /**
     * 查詢帳號刪除資格（供前端在展開刪除卡片時預先檢查，不執行任何刪除動作）。
     */
    @Transactional(readOnly = true)
    public DeletionEligibilityResponse getDeletionEligibility(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));
        return evaluateDeletionEligibility(user);
    }

    /**
     * 共用判斷邏輯：使用者是否可以刪除帳號。
     * 阻擋條件：使用者為某個「運行中」（狀態非 ARCHIVED，即尚未註銷）組織的所有權人。
     * 純一般成員（非 owner）不會被阻擋，刪除時會自動退出組織。
     */
    private DeletionEligibilityResponse evaluateDeletionEligibility(User user) {
        List<Organizer> ownedOrgs = organizerRepository.findByOwner(user);
        List<Organizer> blockingOrgs = ownedOrgs.stream()
                .filter(o -> o.getStatus() != OrganizerStatus.ARCHIVED)
                .collect(Collectors.toList());

        if (blockingOrgs.isEmpty()) {
            return DeletionEligibilityResponse.builder()
                    .allowed(true)
                    .reason(null)
                    .blockingOrgs(Collections.emptyList())
                    .build();
        }

        String orgNames = blockingOrgs.stream()
                .map(o -> o.getName())
                .collect(Collectors.joining("、"));
        String reason = "您目前是以下組織的所有權人，請先轉移所有權或註銷組織後再刪除帳號：" + orgNames;

        List<DeletionEligibilityResponse.BlockingOrg> blockingOrgInfos = blockingOrgs.stream()
                .map(o -> DeletionEligibilityResponse.BlockingOrg.builder()
                        .orgId(o.getOrganizerId())
                        .orgName(o.getName())
                        .build())
                .collect(Collectors.toList());

        return DeletionEligibilityResponse.builder()
                .allowed(false)
                .reason(reason)
                .blockingOrgs(blockingOrgInfos)
                .build();
    }

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    /**
     * 驗證目前密碼是否正確（供設定頁即時檢查用）。
     * 僅限本地帳號；非本地帳號或無密碼一律回傳 false。
     */
    @Transactional(readOnly = true)
    public boolean verifyCurrentPassword(String userId, String currentPassword) {
        if (currentPassword == null || currentPassword.isEmpty()) {
            return false;
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            return false;
        }
        return passwordEncoder.matches(currentPassword, user.getPasswordHash());
    }

    /**
     * 變更密碼（需登入）。
     * 僅限本地帳號（authProvider=LOCAL）；Google 帳號無本地密碼，拒絕變更。
     * 會先驗證目前密碼，再以 BCrypt 雜湊更新新密碼。
     */
    @Transactional
    public void changePassword(String userId, String currentPassword, String newPassword) {
        log.info("開始處理變更密碼，使用者 ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        // 1. Google 等第三方帳號無本地密碼，不允許在本系統變更
        if (user.getAuthProvider() != AuthProvider.LOCAL || user.getPasswordHash() == null) {
            log.warn("變更密碼遭拒，使用者 {} 為第三方登入帳號", userId);
            throw new IllegalArgumentException("此帳號使用第三方快速登入，無法在本系統變更密碼");
        }

        // 2. 驗證目前密碼（若處於強制修改密碼狀態，則允許免驗證目前密碼，方便首次登入/重設後的體驗）
        boolean skipCurrentVerification = Boolean.TRUE.equals(user.getMustChangePassword());
        if (!skipCurrentVerification) {
            if (currentPassword == null || !passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
                log.warn("變更密碼失敗，使用者 {} 目前密碼錯誤", userId);
                throw new IllegalArgumentException("目前密碼不正確");
            }
        }

        // 3. 新密碼不可與舊密碼相同
        if (passwordEncoder.matches(newPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("新密碼不可與目前密碼相同");
        }

        // 4. 更新密碼
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setMustChangePassword(false);
        userRepository.save(user);
        log.info("使用者 {} 密碼變更成功", userId);
    }

    /**
     * 取得特定使用者的個人資料
     */
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile(String userId) {
        log.info("讀取使用者個人資料，ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));
                
        return convertToResponse(user);
    }

    /**
     * 更新特定使用者的個人資料
     */
    @Transactional
    public UserProfileResponse updateUserProfile(String userId, UserProfileRequest request) {
        log.info("開始更新使用者個人資料，ID: {}", userId);
        
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        // 記住舊頭像，儲存成功後若已被換掉則刪除舊檔（頭像採唯一檔名，不會就地覆蓋）
        String oldAvatarUrl = user.getAvatarUrl();

        // 更新允許修改的欄位
        user.setName(request.getName());
        user.setPhone(request.getPhone());
        // gender 欄位在 DB 有 CHECK (gender IS NULL OR gender IN ('M','F','O')) 約束，
        // 前端「不透露」選項會送出空字串 ""，需正規化為 null 以免違反約束造成 500。
        String gender = request.getGender();
        user.setGender((gender == null || gender.isBlank()) ? null : gender);
        user.setBirthDate(request.getBirthDate());
        user.setAddress(request.getAddress());
        user.setAvatarUrl(request.getAvatarUrl());

        User updatedUser = userRepository.save(user);
        log.info("使用者個人資料更新成功，ID: {}", userId);

        // 清理被取代的舊頭像（best-effort，失敗僅記錄不影響更新結果）
        deleteReplacedAvatarFile(oldAvatarUrl, updatedUser.getAvatarUrl());

        return convertToResponse(updatedUser);
    }

    /**
     * 刪除被取代的舊頭像實體檔。
     * 只處理本系統上傳的頭像（URL 以 {@value #AVATAR_URL_PREFIX} 開頭）；
     * boring:、svg data URL、外部 URL 一律略過。URL 未變更時不刪除。
     */
    private void deleteReplacedAvatarFile(String oldUrl, String newUrl) {
        if (oldUrl == null || oldUrl.equals(newUrl)) return;
        if (!oldUrl.startsWith(AVATAR_URL_PREFIX)) return;

        // 取出檔名並去除可能殘留的查詢參數
        String fileName = oldUrl.substring(AVATAR_URL_PREFIX.length());
        int queryIdx = fileName.indexOf('?');
        if (queryIdx >= 0) fileName = fileName.substring(0, queryIdx);
        if (fileName.isBlank()) return;

        try {
            Path avatarDir = Paths.get(documentsDir, "public", "user-avatars").toAbsolutePath().normalize();
            Path filePath = avatarDir.resolve(fileName).normalize();

            // 安全防禦：確保只刪除 avatars 目錄下的檔案，防止路徑穿越
            if (!filePath.startsWith(avatarDir)) {
                log.warn("拒絕刪除可疑路徑的舊頭像: {}", fileName);
                return;
            }

            if (Files.deleteIfExists(filePath)) {
                log.info("已刪除被取代的舊頭像: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("刪除舊頭像檔失敗（略過，不影響個人資料更新）: {}", oldUrl, e);
        }
    }

    /**
     * 私有輔助方法：將 User 實體安全地轉換成 UserProfileResponse DTO
     */
    private UserProfileResponse convertToResponse(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .phone(user.getPhone())
                .gender(user.getGender())
                .birthDate(user.getBirthDate())
                .address(user.getAddress())
                .avatarUrl(user.getAvatarUrl())
                .authProvider(user.getAuthProvider() != null ? user.getAuthProvider().name() : null)
                .isTwoFactorEnabled(Boolean.TRUE.equals(user.getIsTwoFactorEnabled()))
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}
