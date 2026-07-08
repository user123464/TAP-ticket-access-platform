package tw.com.ispan.backend.organizer.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.login.service.EmailService;
import tw.com.ispan.backend.organizer.dto.InviteMemberRequest;
import tw.com.ispan.backend.organizer.dto.MemberResponse;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.entity.OrganizerInvitation;
import tw.com.ispan.backend.organizer.entity.Role;
import tw.com.ispan.backend.organizer.repository.OrganizerInvitationRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerMemberService {

    private final OrganizerRepository organizerRepository;
    private final OrganizerMemberRepository organizerMemberRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final EmailService emailService;
    private final OrganizerService organizerService; // 組織層權限查核
    private final OrganizerInvitationRepository organizerInvitationRepository;

    /** 後端對外網址；正式站由環境變數 BACKEND_URL 提供，本機預設 http://localhost:8080 */
    @Value("${app.backend-url}")
    private String backendUrl;

    /** 前端對外網址；正式站由環境變數 FRONTEND_URL 提供，本機預設 http://localhost:5173 */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * 獲取該組織的所有成員列表
     */
    @Transactional(readOnly = true)
    public List<MemberResponse> getMembers(String organizerId) {
        log.info("獲取組織成員列表，OrganizerId: {}", organizerId);
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));
        List<OrganizerMember> list = organizerMemberRepository.findByOrganizer(org);
        return list.stream()
                .map(m -> MemberResponse.builder()
                        .memberId(m.getMemberId())
                        .userId(m.getUser().getUserId())
                        .name(m.getUser().getName())
                        .email(m.getUser().getEmail())
                        .roleId(m.getRole() != null ? m.getRole().getRoleId() : null)
                        .status(m.getStatus().ordinal())
                        .joinedAt(m.getJoinedAt())
                        .invitedAt(m.getInvitedAt())
                        .build())
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * 發送加入組織的邀請 (產生安全 Token 並寄發 Email)
     */
    @Transactional
    public void inviteMember(String userId, String organizerId, InviteMemberRequest request) {
        log.info("發起成員邀請，組織ID: {}, 邀請人: {}, 被邀請信箱: {}", organizerId, userId, request.getEmail());
        
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        // 權限檢查：Owner 或具備 ORG_MEMBER_MANAGE 組織權限的成員才能邀請
        // （改用組織層權限查核，取代原本以 role_id 字串比對 "ADMIN" 的錯誤判斷）
        organizerService.requireOrgPermission(userId, organizerId, "ORG_MEMBER_MANAGE");

        // 尋找受邀使用者是否存在
        Optional<User> inviteeOpt = userRepository.findByEmail(request.getEmail());
        if (inviteeOpt.isEmpty()) {
            log.info("受邀信箱 {} 尚未註冊，建立暫存邀請並發送註冊引導郵件", request.getEmail());
            
            String inviteToken = UUID.randomUUID().toString();
            LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
            User sender = userRepository.findById(userId).orElseThrow();
            Role role = request.getRoleId() != null ? roleRepository.findById(request.getRoleId()).orElse(null) : null;
            
            // 檢查是否已有該 Email 對同組織的 PENDING 暫存邀請，有的話直接更新
            List<OrganizerInvitation> existingInvites = organizerInvitationRepository.findByEmailAndStatus(request.getEmail(), 0);
            Optional<OrganizerInvitation> sameOrgInviteOpt = existingInvites.stream()
                    .filter(i -> i.getOrganizer().getOrganizerId().equals(organizerId))
                    .findFirst();
            
            OrganizerInvitation inviteToSave;
            if (sameOrgInviteOpt.isPresent()) {
                inviteToSave = sameOrgInviteOpt.get();
                inviteToSave.setRole(role);
                inviteToSave.setInvitedBy(sender);
                inviteToSave.setInviteToken(inviteToken);
                inviteToSave.setInviteTokenExpires(expiresAt);
            } else {
                inviteToSave = OrganizerInvitation.builder()
                        .email(request.getEmail())
                        .organizer(org)
                        .role(role)
                        .invitedBy(sender)
                        .inviteToken(inviteToken)
                        .inviteTokenExpires(expiresAt)
                        .status(0)
                        .build();
            }
            organizerInvitationRepository.save(inviteToSave);

            String registerUrl = frontendUrl + "/login?inviteToken=" + inviteToken + "&email=" + request.getEmail();
            emailService.sendInviteEmail(request.getEmail(), org.getName(), registerUrl);
            return;
        }
        User invitee = inviteeOpt.get();

        // 生成安全驗證 Token (有效期 24 小時)
        String inviteToken = UUID.randomUUID().toString();
        LocalDateTime expiresAt = LocalDateTime.now().plusHours(24);
        User sender = userRepository.findById(userId).orElseThrow();

        OrganizerMember memberToSave;

        // 檢查是否已在組織中 (或是已發送過邀請)
        Optional<OrganizerMember> existingMemberOpt = organizerMemberRepository.findByOrganizerAndUser(org, invitee);
        if (existingMemberOpt.isPresent()) {
            OrganizerMember existingMember = existingMemberOpt.get();
            if (existingMember.getStatus() == OrganizerMemberStatus.ACCEPTED) {
                throw new IllegalArgumentException("此使用者已經是該組織成員");
            } else {
                // 如果是 PENDING (待審核) 或 REVOKED (已撤銷)，允許直接更新 Token、過期時間與角色，實現「修改並重新發送」
                existingMember.setRole(request.getRoleId() != null ? roleRepository.findById(request.getRoleId()).orElse(null) : null);
                existingMember.setInviteToken(inviteToken);
                existingMember.setInviteTokenExpires(expiresAt);
                existingMember.setInvitedBy(sender);
                existingMember.setStatus(OrganizerMemberStatus.PENDING);
                memberToSave = existingMember;
            }
        } else {
            // 生成唯一的 MBRXXXXXXX 成員流水號
            Long nextVal = organizerMemberRepository.getNextMemberSequenceValue();
            String memberId = String.format("MBR%07d", nextVal);

            memberToSave = OrganizerMember.builder()
                    .memberId(memberId)
                    .organizer(org)
                    .user(invitee)
                    .invitedBy(sender)
                    .role(request.getRoleId() != null ? roleRepository.findById(request.getRoleId()).orElse(null) : null)
                    .inviteToken(inviteToken)
                    .inviteTokenExpires(expiresAt)
                    .status(OrganizerMemberStatus.PENDING)
                    .build();
        }

        organizerMemberRepository.save(memberToSave);

        // 發送邀請信
        String inviteUrl = backendUrl + "/api/organizer/accept-invite?token=" + inviteToken;
        emailService.sendInviteEmail(invitee.getEmail(), org.getName(), inviteUrl);
        log.info("邀請記錄建立完成，且信件已寄出。Token: {}", inviteToken);
    }

    /**
     * 接受加入組織的邀請 (校驗 Token 並啟用狀態)
     */
    @Transactional
    public void acceptInvite(String token) {
        log.info("使用者點擊接受邀請，驗證 Token: {}", token);
        
        OrganizerMember member = organizerMemberRepository.findByInviteToken(token)
                .orElseThrow(() -> new IllegalArgumentException("無效或不存在的邀請憑證"));

        if (member.getStatus() != OrganizerMemberStatus.PENDING) {
            throw new IllegalStateException("此邀請已被處理，請勿重複操作");
        }

        if (member.getInviteTokenExpires().isBefore(LocalDateTime.now())) {
            member.setStatus(OrganizerMemberStatus.REVOKED);
            organizerMemberRepository.save(member);
            throw new IllegalArgumentException("邀請連結已過期，請聯絡管理員重新發送邀請");
        }

        // 驗證成功：變更狀態為已接受，填入加入時間
        member.setStatus(OrganizerMemberStatus.ACCEPTED);
        member.setJoinedAt(LocalDateTime.now());
        member.setInviteToken(null); 
        member.setInviteTokenExpires(null);
        
        organizerMemberRepository.save(member);
        log.info("使用者 {} 已成功加入組織 {}", member.getUser().getUserId(), member.getOrganizer().getName());
    }

    /**
     * 修改成員的角色
     */
    @Transactional
    public void updateMemberRole(String executorId, String organizerId, String memberId, String targetRoleId) {
        log.info("執行修改角色，操作者: {}, 組織: {}, 被修改成員: {}, 目標角色: {}", executorId, organizerId, memberId, targetRoleId);
        
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        // 權限檢查：只有 Owner 可以修改其它成員角色
        if (!org.getOwner().getUserId().equals(executorId)) {
            throw new SecurityException("只有組織所有者 (Owner) 有權限修改成員角色");
        }

        OrganizerMember member = organizerMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的成員記錄"));

        member.setRole(roleRepository.findById(targetRoleId).orElseThrow(() -> new IllegalArgumentException("無效的角色")));
        organizerMemberRepository.save(member);
    }

    /**
     * 將成員移出組織 (KICK / DELETE)
     */
    @Transactional
    public void removeMember(String executorId, String organizerId, String memberId) {
        log.info("移出組織成員，操作者: {}, 組織: {}, 成員ID: {}", executorId, organizerId, memberId);
        
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        OrganizerMember member = organizerMemberRepository.findById(memberId)
                .orElseThrow(() -> new IllegalArgumentException("找不到指定的成員記錄"));

        // 權限檢查：只有 Owner 可以移除成員
        if (!org.getOwner().getUserId().equals(executorId)) {
            throw new SecurityException("只有組織所有權人 (Owner) 才能將成員移出組織");
        }

        // 將狀態變更為 REVOKED (已撤銷)
        member.setStatus(OrganizerMemberStatus.REVOKED);
        organizerMemberRepository.save(member);
    }

    /**
     * 主動退出組織
     */
    @Transactional
    public void leaveOrganization(String userId, String organizerId) {
        log.info("成員自主退出組織，使用者: {}, 組織ID: {}", userId, organizerId);
        
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        // Owner 防禦：組織負責人不可直接退出，必須先註銷組織或將所有權轉移給他人
        if (org.getOwner().getUserId().equals(userId)) {
            throw new IllegalStateException("您是組織所有權人，無法直接退出！請選擇轉移組織所有權或註銷組織");
        }

        User user = userRepository.findById(userId).orElseThrow();
        OrganizerMember member = organizerMemberRepository.findByOrganizerAndUser(org, user)
                .orElseThrow(() -> new IllegalArgumentException("您不是該組織的成員"));

        if (member.getStatus() != OrganizerMemberStatus.ACCEPTED) {
            throw new IllegalStateException("您尚未加入或已離開該組織");
        }

        member.setStatus(OrganizerMemberStatus.REVOKED); 
        organizerMemberRepository.save(member);
    }
}
