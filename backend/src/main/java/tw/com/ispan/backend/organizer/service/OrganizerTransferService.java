package tw.com.ispan.backend.organizer.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.login.service.EmailService;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.entity.OrganizerOwnershipTransfer;
import tw.com.ispan.backend.organizer.entity.Role;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerTransferStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerOwnershipTransferRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;

/**
 * 組織「危險區域」服務：彙整折疊區狀態 + 所有權轉移（含 email 認證）流程。
 *
 * <p>
 * 轉移採與組織邀請（{@link OrganizerMemberService#inviteMember}）相同的 token + email
 * 認證模式：owner 發起後寄信給被轉移人，被轉移人點擊信中連結才真正易主。易主後原 owner
 * 與新 owner 皆保有「ACCEPTED + Admin 角色」成員列，與 {@code createOrganization} 建立組織時
 * owner 的不變式一致（owner 身分本身仍以 {@code organizer.owner_user_id} 認定）。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerTransferService {

    private final OrganizerRepository organizerRepository;
    private final OrganizerMemberRepository organizerMemberRepository;
    private final OrganizerOwnershipTransferRepository transferRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final OrganizerService organizerService;

    /** 後端對外網址（accept-transfer 認證連結用；與 acceptInvite 同樣指向後端 API）。
     *  正式站由環境變數 BACKEND_URL 提供，本機預設 http://localhost:8080 */
    @Value("${app.backend-url}")
    private String backendUrl;

    /**
     * 折疊區「危險區域」一次性狀態：角色、是否可刪除＋阻擋原因、是否有待處理轉移。
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getDangerZoneStatus(String userId, String organizerId) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        boolean isOwner = org.getOwner().getUserId().equals(userId);

        Map<String, Object> result = new HashMap<>();
        result.put("role", isOwner ? "OWNER" : "MEMBER");

        // 刪除阻擋原因只對 owner 有意義（只有 owner 看得到刪除鈕）
        List<String> blockReasons = isOwner ? organizerService.getDeletionBlockReasons(organizerId) : List.of();
        result.put("blockReasons", blockReasons);
        result.put("canDelete", isOwner && blockReasons.isEmpty());

        // 待處理的所有權轉移（供轉移鈕禁用 + 顯示「取消轉移」）
        Map<String, Object> pendingTransfer = null;
        if (isOwner) {
            OrganizerOwnershipTransfer pending = transferRepository
                    .findByOrganizer_OrganizerIdAndStatus(organizerId, OrganizerTransferStatus.PENDING)
                    .orElse(null);
            if (pending != null) {
                pendingTransfer = new HashMap<>();
                pendingTransfer.put("targetUserId", pending.getToUser().getUserId());
                pendingTransfer.put("targetName", pending.getToUser().getName());
                pendingTransfer.put("expiresAt", pending.getTokenExpires());
            }
        }
        result.put("pendingTransfer", pendingTransfer);

        return result;
    }

    /**
     * 發起所有權轉移：owner-only，目標須為本組織 ACCEPTED 成員，且不可已有待處理轉移。
     * 建立 PENDING 紀錄並寄送認證信給目標。
     */
    @Transactional
    public void initiateTransfer(String userId, String organizerId, String targetUserId) {
        log.info("發起組織所有權轉移，組織: {}, 發起人: {}, 目標: {}", organizerId, userId, targetUserId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        if (!org.getOwner().getUserId().equals(userId)) {
            throw new SecurityException("只有組織所有權人 (Owner) 才能轉移所有權");
        }
        if (targetUserId == null || targetUserId.equals(userId)) {
            throw new IllegalArgumentException("請選擇一位其他成員作為接手人");
        }

        User target = userRepository.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的接手成員"));

        OrganizerMember targetMember = organizerMemberRepository.findByOrganizerAndUser(org, target)
                .orElseThrow(() -> new IllegalArgumentException("接手人必須是本組織的成員"));
        if (targetMember.getStatus() != OrganizerMemberStatus.ACCEPTED) {
            throw new IllegalStateException("接手人尚未加入或已離開組織，無法承接所有權");
        }

        if (transferRepository.findByOrganizer_OrganizerIdAndStatus(
                organizerId, OrganizerTransferStatus.PENDING).isPresent()) {
            throw new IllegalStateException("已有一筆待確認的所有權轉移，請先取消後再重新發起");
        }

        User from = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到發起人"));

        String token = UUID.randomUUID().toString();
        OrganizerOwnershipTransfer transfer = OrganizerOwnershipTransfer.builder()
                .transferId(generateUniqueTransferId())
                .organizer(org)
                .fromUser(from)
                .toUser(target)
                .token(token)
                .tokenExpires(LocalDateTime.now().plusHours(24))
                .status(OrganizerTransferStatus.PENDING)
                .build();
        transferRepository.save(transfer);

        String acceptUrl = backendUrl + "/api/organizer/accept-transfer?token=" + token;
        emailService.sendTransferEmail(target.getEmail(), org.getName(), from.getName(), acceptUrl);
        log.info("所有權轉移已建立並寄出認證信，Token: {}", token);
    }

    /**
     * 接受所有權轉移（被轉移人點擊信中連結）：校驗 Token → 易主 → 維持雙方 Admin 成員列。
     */
    @Transactional
    public void acceptTransfer(String token) {
        log.info("接受組織所有權轉移，驗證 Token: {}", token);

        OrganizerOwnershipTransfer transfer = transferRepository.findByToken(token)
                .orElseThrow(() -> new IllegalArgumentException("無效或不存在的轉移憑證"));

        if (transfer.getStatus() != OrganizerTransferStatus.PENDING) {
            throw new IllegalStateException("此轉移已被處理，請勿重複操作");
        }
        if (transfer.getTokenExpires().isBefore(LocalDateTime.now())) {
            transfer.setStatus(OrganizerTransferStatus.EXPIRED);
            transferRepository.save(transfer);
            throw new IllegalArgumentException("轉移連結已過期，請聯絡原組織所有人重新發起");
        }

        Organizer org = transfer.getOrganizer();
        User from = transfer.getFromUser();
        User to = transfer.getToUser();

        // 防禦：發起時的 owner 必須仍是當前 owner，否則此連結已失效
        if (!org.getOwner().getUserId().equals(from.getUserId())) {
            transfer.setStatus(OrganizerTransferStatus.EXPIRED);
            transferRepository.save(transfer);
            throw new IllegalStateException("組織所有權已變更，本轉移連結失效");
        }

        Role adminRole = findOrgAdminRole(org);

        // 易主
        org.setOwner(to);
        organizerRepository.save(org);

        // 新 owner 與原 owner 皆保有 ACCEPTED + Admin 成員列（與建立組織時 owner 的不變式一致；
        // owner 身分本身另由 owner_user_id 認定）。原 owner 即「降為一般成員（Admin 角色）」。
        upsertAcceptedAdminMember(org, to, adminRole);
        upsertAcceptedAdminMember(org, from, adminRole);

        transfer.setStatus(OrganizerTransferStatus.ACCEPTED);
        transferRepository.save(transfer);
        log.info("組織 {} 所有權已由 {} 轉移給 {}", org.getOrganizerId(), from.getUserId(), to.getUserId());
    }

    /**
     * 取消待處理的所有權轉移（owner-only）。
     */
    @Transactional
    public void cancelTransfer(String userId, String organizerId) {
        log.info("取消組織所有權轉移，組織: {}, 操作者: {}", organizerId, userId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));
        if (!org.getOwner().getUserId().equals(userId)) {
            throw new SecurityException("只有組織所有權人 (Owner) 才能取消轉移");
        }

        OrganizerOwnershipTransfer transfer = transferRepository
                .findByOrganizer_OrganizerIdAndStatus(organizerId, OrganizerTransferStatus.PENDING)
                .orElseThrow(() -> new IllegalStateException("目前沒有待取消的所有權轉移"));
        transfer.setStatus(OrganizerTransferStatus.CANCELLED);
        transferRepository.save(transfer);
    }

    // ── 私有輔助 ──

    /** 取得該組織的內建 Admin 角色（name=Admin 且 is_editable=false）。 */
    private Role findOrgAdminRole(Organizer org) {
        return roleRepository.findByOrganizer(org).stream()
                .filter(r -> "Admin".equalsIgnoreCase(r.getRoleName())
                        && Boolean.FALSE.equals(r.getIsEditable()))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("找不到組織的內建 Admin 角色，無法完成轉移"));
    }

    /** 將某 user 設為該組織的 ACCEPTED + Admin 成員；不存在則新建成員列。 */
    private void upsertAcceptedAdminMember(Organizer org, User user, Role adminRole) {
        OrganizerMember member = organizerMemberRepository.findByOrganizerAndUser(org, user).orElse(null);
        if (member == null) {
            member = OrganizerMember.builder()
                    .memberId(generateUniqueMemberId())
                    .organizer(org)
                    .user(user)
                    .invitedBy(user)
                    .role(adminRole)
                    .status(OrganizerMemberStatus.ACCEPTED)
                    .joinedAt(LocalDateTime.now())
                    .build();
        } else {
            member.setRole(adminRole);
            member.setStatus(OrganizerMemberStatus.ACCEPTED);
            if (member.getJoinedAt() == null) {
                member.setJoinedAt(LocalDateTime.now());
            }
        }
        organizerMemberRepository.save(member);
    }

    private String generateUniqueTransferId() {
        String transferId;
        int guard = 0;
        do {
            Long nextVal = transferRepository.getNextTransferSequenceValue();
            transferId = String.format("TRF%07d", nextVal);
            if (++guard > 100) {
                throw new IllegalStateException("產生轉移流水號失敗：連續撞號，請聯絡系統管理員");
            }
        } while (transferRepository.existsById(transferId));
        return transferId;
    }

    private String generateUniqueMemberId() {
        String memberId;
        int guard = 0;
        do {
            Long nextVal = organizerMemberRepository.getNextMemberSequenceValue();
            memberId = String.format("MBR%07d", nextVal);
            if (++guard > 100) {
                throw new IllegalStateException("產生成員流水號失敗：連續撞號，請聯絡系統管理員");
            }
        } while (organizerMemberRepository.existsById(memberId));
        return memberId;
    }
}
