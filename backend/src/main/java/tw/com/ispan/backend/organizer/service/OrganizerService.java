package tw.com.ispan.backend.organizer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.dto.AdminOrganizerSummary;
import tw.com.ispan.backend.organizer.dto.CreateOrganizerRequest;
import tw.com.ispan.backend.organizer.dto.MyOrganizationResponse;
import tw.com.ispan.backend.organizer.dto.SubmitKycRequest;
import tw.com.ispan.backend.organizer.dto.UpdateOrganizerProfileRequest;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.entity.Permission;
import tw.com.ispan.backend.organizer.entity.Role;
import tw.com.ispan.backend.organizer.entity.RoleTemplate;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerStatus;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.organizer.repository.PermissionRepository;
import tw.com.ispan.backend.organizer.repository.RolePermissionTemplateRepository;
import tw.com.ispan.backend.organizer.repository.RoleRepository;
import tw.com.ispan.backend.organizer.repository.RoleTemplateRepository;
import tw.com.ispan.backend.settlements.enums.SettlementEnum;
import tw.com.ispan.backend.settlements.repository.SettlementRepository;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.ThemeRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrganizerService {

    /** 組織 Logo URL 前綴，用來判斷是否為本系統上傳的可刪除檔案 */
    private static final String ORG_AVATAR_URL_PREFIX = "/api/organizer/avatars/";

    private final OrganizerRepository organizerRepository;
    private final OrganizerMemberRepository organizerMemberRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final ContractService contractService;
    private final RoleRepository roleRepository;
    private final RolePermissionTemplateRepository rolePermissionTemplateRepository;
    private final RoleTemplateRepository roleTemplateRepository;
    private final PermissionRepository permissionRepository;
    private final ThemeRepository themeRepository;
    private final SettlementRepository settlementRepository;

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    /**
     * 獲取當前登入者所屬的所有組織 (包含自己創立的 OWNER 以及加入的成員角色)
     */
    /**
     * [Jason] RBAC 待辦 #2：Admin 後台查詢組織清單（KYC 審核用）。
     *
     * <p>
     * 唯讀。傳入 {@code statusOrNull} 為 null 時回傳全部組織，否則僅回傳該
     * KYC 狀態（如 PENDING 待審）。回傳精簡 {@link AdminOrganizerSummary}，
     * 不回傳整個 entity，避免 LAZY 關聯與敏感欄位外洩。
     * </p>
     */
    @Transactional(readOnly = true)
    public List<AdminOrganizerSummary> listOrganizersForAdmin(KycStatus statusOrNull) {
        log.info("Admin 查詢組織清單，KYC 狀態篩選: {}", statusOrNull == null ? "ALL" : statusOrNull);
        List<Organizer> organizers = (statusOrNull == null)
                ? organizerRepository.findAll()
                : organizerRepository.findByKycStatus(statusOrNull);
        return organizers.stream()
                .map(org -> new AdminOrganizerSummary(
                        org.getOrganizerId(),
                        org.getName(),
                        org.getTaxId(),
                        org.getKycStatus() == null ? null : org.getKycStatus().ordinal(),
                        org.getKycStatus() == null ? null : org.getKycStatus().name(),
                        org.getKycRejectionReason(),
                        org.getKycReviewedAt(),
                        org.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<MyOrganizationResponse> getMyOrganizations(String userId) {
        log.info("查詢使用者所屬組織列表，UserId: {}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        List<MyOrganizationResponse> result = new ArrayList<>();

        // 1. 查詢該使用者作為 Owner 的組織 (過濾掉已封存註銷者)
        List<Organizer> ownedOrgs = organizerRepository.findByOwner(user);
        for (Organizer org : ownedOrgs) {
            if (org.getStatus() == OrganizerStatus.ARCHIVED) {
                continue;
            }
            result.add(MyOrganizationResponse.builder()
                    .id(org.getOrganizerId())
                    .name(org.getName())
                    .taxId(org.getTaxId())
                    .role("OWNER")
                    .kycStatus(org.getKycStatus().ordinal())
                    .logo(extractLogoUrl(org.getKycDataJson()))
                    .build());
        }

        // 2. 查詢該使用者作為成員被邀請加入且狀態為 ACCEPTED 的組織 (過濾掉已封存註銷者)
        List<OrganizerMember> memberships = organizerMemberRepository.findByUser(user);
        for (OrganizerMember member : memberships) {
            if (member.getStatus() == OrganizerMemberStatus.ACCEPTED) {
                Organizer org = member.getOrganizer();
                if (org.getStatus() == OrganizerStatus.ARCHIVED) {
                    continue;
                }
                // 去重：Owner 在建立組織時也會被寫入 organizer_member 一筆，
                // 上方已以 OWNER 身分加入清單，此處略過以免同一組織重複出現兩張卡。
                if (org.getOwner().getUserId().equals(userId)) {
                    continue;
                }
                result.add(MyOrganizationResponse.builder()
                        .id(org.getOrganizerId())
                        .name(org.getName())
                        .taxId(org.getTaxId())
                        .role(member.getRole() != null ? member.getRole().getRoleId() : null)
                        .kycStatus(org.getKycStatus().ordinal())
                        .logo(extractLogoUrl(org.getKycDataJson()))
                        .build());
            }
        }

        return result;
    }

    /**
     * 建立新的組織 (草稿狀態)
     */
    @Transactional
    public MyOrganizationResponse createOrganization(String userId, CreateOrganizerRequest request) {
        log.info("建立新組織，Owner: {}, 名稱: {}", userId, request.getName());
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        // 生成 ORGXXXXXXX 格式的主鍵
        Long nextVal = organizerRepository.getNextOrganizerSequenceValue();
        String organizerId = String.format("ORG%07d", nextVal);

        Organizer org = Organizer.builder()
                .organizerId(organizerId)
                .name(request.getName())
                .taxId(request.getTaxId())
                .owner(user)
                .status(OrganizerStatus.ACTIVE)
                .kycStatus(KycStatus.DRAFT)
                .build();

        organizerRepository.save(org);

        // 1. 依角色模板（role_template）複製並建立組織專屬角色（藍圖：複製當下、之後互不影響）。
        //    特例 DEFAULT_ORG_ADMIN → 固定命名 "Admin" 且 is_editable=false。
        //      注意：Owner 由 owner_user_id 認定（verifyOwnerOrAdmin 第一道短路），與此角色名稱無關；
        //      此「名稱=Admin + is_editable=false」是 isOrgAdminRole 對「非 Owner 但持內建管理員角色之成員」的認定依據，
        //      故現行邏輯下勿改名/勿改 is_editable（否則該類成員會喪失管理權）。
        //    其餘模板 → 名稱取 template_name、is_editable=true；新增模板即自動於日後新組織長出對應角色。
        Role adminRole = null;
        for (RoleTemplate template : roleTemplateRepository.findAll()) {
            String templateId = template.getTemplateId();
            if ("DEFAULT_ORG_SCANNER".equals(templateId) || "DEFAULT_ORG_CS".equals(templateId)) {
                // 驗票員與客服模板放在下拉選單供手動新建套用，新組織建立時不預設生成
                continue;
            }
            Set<Permission> perms = rolePermissionTemplateRepository.findByTemplateId(templateId)
                    .stream()
                    .map(t -> t.getPermission())
                    .collect(Collectors.toSet());

            boolean isAdminTemplate = "DEFAULT_ORG_ADMIN".equals(templateId);
            Role role = Role.builder()
                    .roleId(generateUniqueRoleId())
                    .roleName(isAdminTemplate ? "Admin" : template.getTemplateName())
                    .description("")
                    .organizer(org)
                    .isEditable(!isAdminTemplate)
                    .permissions(perms)
                    .build();
            roleRepository.save(role);
            if (isAdminTemplate) {
                adminRole = role;
            }
        }
        if (adminRole == null) {
            throw new IllegalStateException("缺少組織預設角色模板 DEFAULT_ORG_ADMIN，無法建立組織管理員角色");
        }

        // 2. 將擁有者 (Owner) 作為 ACCEPTED 成員加入 organizer_member 表，並指派剛建立的 Admin 角色
        String memberId = generateUniqueMemberId();
        OrganizerMember ownerMember = OrganizerMember.builder()
                .memberId(memberId)
                .organizer(org)
                .user(user)
                .invitedBy(user)
                .role(adminRole)
                .status(OrganizerMemberStatus.ACCEPTED)
                .joinedAt(LocalDateTime.now())
                .build();
        organizerMemberRepository.save(ownerMember);

        // 預先寫入一筆未簽署的免費標準 DRAFT 合約，讓合約頁在 KYC 提交前即可呈現真實預設方案
        contractService.createUnsignedDraftFreeContract(org, user);

        return MyOrganizationResponse.builder()
                .id(org.getOrganizerId())
                .name(org.getName())
                .taxId(org.getTaxId())
                .role("OWNER")
                .kycStatus(org.getKycStatus().ordinal())
                .build();
    }

    /** 註銷阻擋原因碼：名下有進行中(ACTIVE)活動 */
    public static final String DELETE_BLOCK_ACTIVE_EVENTS = "ACTIVE_EVENTS";
    /** 註銷阻擋原因碼：名下有未結算(PENDING)款項 */
    public static final String DELETE_BLOCK_UNSETTLED = "UNSETTLED";

    /**
     * 註銷/封存組織安全檢查：回傳所有阻擋原因碼（空集合代表可註銷）。
     * 規則：名下有進行中(ACTIVE)活動，或有未結算(PENDING)款項，則不允許註銷。
     * 成員不在此檢查內——刪除時會連帶軟刪所有成員（見 {@link #deleteOrganizer}）。
     */
    @Transactional(readOnly = true)
    public List<String> getDeletionBlockReasons(String organizerId) {
        List<String> reasons = new ArrayList<>();
        if (themeRepository.countByOrganizerOrganizerIdAndStatus(organizerId, Status.ACTIVE) > 0) {
            reasons.add(DELETE_BLOCK_ACTIVE_EVENTS);
        }
        if (settlementRepository.countByOrganizerOrganizer_OrganizerIdAndItemStatus(
                organizerId, SettlementEnum.PENDING) > 0) {
            reasons.add(DELETE_BLOCK_UNSETTLED);
        }
        return reasons;
    }

    /**
     * 註銷/封存組織安全檢查（布林版）。無任何阻擋原因即可註銷。
     */
    @Transactional(readOnly = true)
    public boolean checkCanDelete(String organizerId) {
        return getDeletionBlockReasons(organizerId).isEmpty();
    }

    /**
     * 註銷組織 (限 OWNER 操作)
     */
    @Transactional
    public void deleteOrganizer(String userId, String organizerId) {
        log.info("註銷組織請求，發起人: {}, 組織ID: {}", userId, organizerId);
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        // 權限檢查：只有創立者 (Owner) 有權銷毀組織
        if (!org.getOwner().getUserId().equals(userId)) {
            throw new SecurityException("只有組織所有權人 (Owner) 才能註銷此組織");
        }

        // 安全防線校驗（進行中活動 / 未結算款項）
        if (!checkCanDelete(organizerId)) {
            throw new IllegalStateException("偵測到本組織名下仍有「進行中」活動或「尚未結算」之款項，無法刪除。");
        }

        // 連帶軟刪：將該組織所有其他成員（含待確認邀請）狀態設為 REVOKED，
        // 使「刪除組織」=「組織封存 + 成員一併撤銷」，毋須 owner 先逐一清空成員。
        List<OrganizerMember> members = organizerMemberRepository.findByOrganizer(org);
        for (OrganizerMember m : members) {
            if (m.getStatus() == OrganizerMemberStatus.ACCEPTED
                    || m.getStatus() == OrganizerMemberStatus.PENDING) {
                m.setStatus(OrganizerMemberStatus.REVOKED);
                organizerMemberRepository.save(m);
            }
        }

        // 軟刪除：將狀態設為 ARCHIVED (封存)
        org.setStatus(OrganizerStatus.ARCHIVED);
        organizerRepository.save(org);
        log.info("組織已成功封存並撤銷 {} 名成員: {}", members.size(), organizerId);
    }

    /**
     * 驗證使用者是否為該組織的所有權人 (Owner) 或管理員 (ADMIN)
     */
    @Transactional(readOnly = true)
    public void verifyOwnerOrAdmin(String userId, String organizerId) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        // 0. 安全防線：已被註銷封存的組織禁止任何人（包含 Owner）進行普通 B2B 操作存取
        if (org.getStatus() == OrganizerStatus.ARCHIVED) {
            throw new SecurityException("該組織已被註銷封存，無法存取");
        }

        // 1. 如果是創立者 (Owner)，通過驗證
        if (org.getOwner().getUserId().equals(userId)) {
            return;
        }

        // 2. 如果不是 Owner，檢查是否為狀態為 ACCEPTED 且角色為 ADMIN 的成員
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        OrganizerMember member = organizerMemberRepository.findByOrganizerAndUser(org, user)
                .orElseThrow(() -> new SecurityException("您不是此組織的成員，無權執行此操作"));

        if (member.getStatus() != OrganizerMemberStatus.ACCEPTED || !isOrgAdminRole(member.getRole(), org)) {
            throw new SecurityException("權限不足，必須是組織所有者或管理員(Admin)才能執行此操作");
        }
    }

    /**
     * 判斷某角色是否為「該組織內建的管理員角色」。
     *
     * 依 data.sql 規格，組織管理員角色的 role_id 為 ROL+流水號（如 ROL0000001），
     * role_name 為 'Admin'，且為系統內建（is_editable = false）並歸屬於該組織。
     * 因此不能再用 role_id 與字串 "ADMIN" 比對，必須以語意判斷：
     * 1. 角色歸屬於本組織（organizer_id 相符）
     * 2. role_name 等於 'Admin'（不分大小寫）
     * 3. is_editable = false（系統內建、不可被改名/刪除，避免有人新建可編輯的同名角色冒充）
     */
    private boolean isOrgAdminRole(tw.com.ispan.backend.organizer.entity.Role role, Organizer org) {
        if (role == null || role.getOrganizer() == null) {
            return false;
        }
        boolean belongsToOrg = org.getOrganizerId().equals(role.getOrganizer().getOrganizerId());
        boolean namedAdmin = "Admin".equalsIgnoreCase(role.getRoleName());
        boolean systemBuiltIn = Boolean.FALSE.equals(role.getIsEditable());
        return belongsToOrg && namedAdmin && systemBuiltIn;
    }

    /**
     * 組織層權限查核：判斷某 user 在某 organizer 內是否擁有指定 permission。
     * - Owner 視為擁有該組織的全部權限，直接回傳 true。
     * - 否則依「organizer_member → role → role_permission」查核，僅計入 ACCEPTED 成員。
     * 註：此為 service 層查核，不影響登入時載入的 Spring Security authorities。
     */
    @Transactional(readOnly = true)
    public boolean hasOrgPermission(String userId, String organizerId, String permissionId) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        if (org.getOwner().getUserId().equals(userId)) {
            return true;
        }
        return organizerMemberRepository.existsOrgPermission(userId, organizerId, permissionId);
    }

    /**
     * 組織層權限查核（無權即拋例外版本），供需要強制權限的 service 流程使用。
     */
    @Transactional(readOnly = true)
    public void requireOrgPermission(String userId, String organizerId, String permissionId) {
        if (!hasOrgPermission(userId, organizerId, permissionId)) {
            throw new SecurityException("權限不足，缺少組織權限：" + permissionId);
        }
    }

    /**
     * 取得某 user 在某 organizer 內的「有效權限碼集合」。
     *   - Owner：與 {@link #hasOrgPermission} 的 owner 短路一致，視為擁有全部權限（回傳所有 permission_id）。
     *   - 一般成員：僅計入 ACCEPTED 狀態，回傳其組織角色（role_permission）的權限碼；非成員回空集合。
     * 用途：B2B 後台動態選單依「使用者在該組織的角色」過濾，而非登入時載入的平台天花板權限。
     */
    @Transactional(readOnly = true)
    public Set<String> getOrgPermissionIds(String userId, String organizerId) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        if (org.getOwner().getUserId().equals(userId)) {
            return permissionRepository.findAll().stream()
                    .map(p -> p.getPermissionId())
                    .collect(Collectors.toSet());
        }

        User user = userRepository.findById(userId).orElse(null);
        if (user == null) {
            return Set.of();
        }
        return organizerMemberRepository.findByOrganizerAndUser(org, user)
                .filter(m -> m.getStatus() == OrganizerMemberStatus.ACCEPTED && m.getRole() != null)
                .map(m -> m.getRole().getPermissions().stream()
                        .map(p -> p.getPermissionId())
                        .collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    /**
     * 提交 KYC 實名認證資料
     */
    @Transactional
    public void submitKyc(String userId, String organizerId, SubmitKycRequest request) {
        log.info("提交組織 KYC 審核，組織ID: {}, 操作者: {}", organizerId, userId);

        // 1. 權限檢查 (重複使用 verifyOwnerOrAdmin 確保安全)
        verifyOwnerOrAdmin(userId, organizerId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        // 提交者即為線上合約的簽署人
        User submitter = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));

        // 2. 只有 DRAFT 或 REJECTED 狀態下才可以提交
        if (org.getKycStatus() != KycStatus.DRAFT && org.getKycStatus() != KycStatus.REJECTED) {
            throw new IllegalStateException("目前組織狀態不允許提交 KYC 審核");
        }

        // 記住舊 Logo，提交後若 Logo 已被換掉則刪除舊檔
        String oldLogoUrl = extractLogoUrl(org.getKycDataJson());

        // 3. 封裝銀行帳戶 JSON 字串
        Map<String, String> bankInfo = Map.of(
                "bank_code", request.getBankCode().trim(),
                "bank_name", request.getBankName().trim(),
                "account_no", request.getAccountNo().trim(),
                "account_name", request.getAccountName().trim());

        // 4. 封裝 KYC 資料 JSON 字串
        Map<String, String> kycData = new HashMap<>();
        kycData.put("logo_url", request.getLogoUrl() != null ? request.getLogoUrl().trim() : "");
        kycData.put("address", request.getAddress() != null ? request.getAddress().trim() : "");
        kycData.put("phone", request.getPhone() != null ? request.getPhone().trim() : "");
        kycData.put("fax", request.getFax() != null ? request.getFax().trim() : "");
        kycData.put("owner_name", request.getOwnerName().trim());
        kycData.put("owner_id_number", request.getOwnerIdNumber().trim());
        kycData.put("registration_doc_name",
                request.getRegistrationDocName() != null ? request.getRegistrationDocName().trim() : "");
        kycData.put("registration_doc_url",
                request.getRegistrationDocUrl() != null ? request.getRegistrationDocUrl().trim() : "");
        kycData.put("identity_card_name",
                request.getIdentityCardName() != null ? request.getIdentityCardName().trim() : "");
        kycData.put("identity_card_url",
                request.getIdentityCardUrl() != null ? request.getIdentityCardUrl().trim() : "");

        try {
            String bankInfoJson = objectMapper.writeValueAsString(bankInfo);
            String kycDataJson = objectMapper.writeValueAsString(kycData);

            // 更新組織狀態與欄位
            org.setTaxId(request.getTaxId().trim());
            org.setBankAccountInfo(bankInfoJson);
            org.setKycDataJson(kycDataJson);
            org.setKycStatus(KycStatus.PENDING); // 狀態變更為 PENDING (1)

            organizerRepository.save(org);
            log.info("組織 KYC 審核資料提交成功: {}", organizerId);

            // 線上合約簽署：提交即代表已於前端閱讀並同意免費託售合約，建立 DRAFT 合約並記錄簽署人/時間
            // （草稿於平台審核通過後才會轉為 ACTIVE 生效）
            contractService.signDraftFreeContract(org, submitter);

            // 清理被取代的舊 Logo 檔（best-effort，失敗僅記錄不影響提交）
            deleteReplacedOrgAvatarFile(oldLogoUrl, kycData.get("logo_url"));
        } catch (Exception e) {
            log.error("序列化 KYC JSON 發生錯誤", e);
            throw new RuntimeException("提交 KYC 審核失敗，系統內部異常");
        }
    }

    /**
     * 獲取單一組織詳細資料
     */
    @Transactional(readOnly = true)
    public Map<String, Object> getOrganizerDetails(String userId, String organizerId) {
        log.info("查詢單一組織詳細資料，組織ID: {}, 操作者: {}", organizerId, userId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        if (org.getStatus() == OrganizerStatus.ARCHIVED) {
            throw new SecurityException("該組織已被註銷封存，無法存取");
        }

        // 1. 檢查是否為 Owner 或者是狀態為 ACCEPTED 的成員
        boolean isOwner = org.getOwner().getUserId().equals(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該使用者"));
        
        OrganizerMember member = organizerMemberRepository.findByOrganizerAndUser(org, user).orElse(null);
        boolean isAcceptedMember = member != null && member.getStatus() == OrganizerMemberStatus.ACCEPTED;

        if (!isOwner && !isAcceptedMember) {
            throw new SecurityException("您不是此組織的成員，無權執行此操作");
        }

        boolean isAdmin = isOwner || isOrgAdminRole(member.getRole(), org);

        // 2. 封裝並過濾回傳欄位
        if (isAdmin) {
            // 所有者與管理員回傳完整敏感資料
            return Map.of(
                    "id", org.getOrganizerId(),
                    "name", org.getName(),
                    "tax_id", org.getTaxId() != null ? org.getTaxId() : "",
                    "status", org.getStatus().ordinal(),
                    "kyc_status", org.getKycStatus().ordinal(),
                    "bank_account_info", org.getBankAccountInfo() != null ? org.getBankAccountInfo() : "{}",
                    "kyc_data_json", org.getKycDataJson() != null ? org.getKycDataJson() : "{}",
                    "kyc_rejection_reason", org.getKycRejectionReason() != null ? org.getKycRejectionReason() : "",
                    "owner_user_id", org.getOwner().getUserId());
        } else {
            // 普通成員（會計、驗票員等）只回傳非敏感的基本資訊與 Logo，隱蔽敏感 KYC/銀行資訊
            String logoUrl = extractLogoUrl(org.getKycDataJson());
            Map<String, String> safeKycData = new HashMap<>();
            safeKycData.put("logo_url", logoUrl != null ? logoUrl : "");
            
            String safeKycDataJson;
            try {
                safeKycDataJson = objectMapper.writeValueAsString(safeKycData);
            } catch (Exception e) {
                safeKycDataJson = "{}";
            }

            return Map.of(
                    "id", org.getOrganizerId(),
                    "name", org.getName(),
                    "status", org.getStatus().ordinal(),
                    "kyc_status", org.getKycStatus().ordinal(),
                    "kyc_data_json", safeKycDataJson,
                    "owner_user_id", org.getOwner().getUserId());
        }
    }

    /**
     * [Admin 專用] 審核 KYC 通過，並自動建立免費標準合約
     */
    @Transactional
    public void approveKyc(String adminUserId, String organizerId) {
        log.info("管理員審核 KYC 通過，組織ID: {}, 管理員ID: {}", organizerId, adminUserId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        if (org.getKycStatus() != KycStatus.PENDING) {
            throw new IllegalStateException("只有處於 PENDING 狀態的組織可以進行審核");
        }

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該管理員"));

        // 更新狀態為 APPROVED 並恢復組織狀態
        org.setKycStatus(KycStatus.APPROVED);
        if (org.getStatus() == OrganizerStatus.SUSPENDED) {
            org.setStatus(OrganizerStatus.ACTIVE);
            log.info("組織 KYC 重新通過，狀態已從 SUSPENDED 恢復為 ACTIVE: {}", organizerId);
        }
        org.setKycReviewedBy(adminUser);
        org.setKycReviewedAt(LocalDateTime.now());
        organizerRepository.save(org);

        // 將使用者提交時已簽署的 DRAFT 合約轉為 ACTIVE 生效（查無草稿則相容建立）
        contractService.activateContractOnApproval(org, adminUser);
        log.info("組織 KYC 審核通過並成功啟用合約: {}", organizerId);
    }

    /**
     * [Admin 專用] 審核 KYC 退件：記錄退件原因，並將已簽署的 DRAFT 合約退回未簽署狀態（全部重來）
     *
     * @param adminUserId 審核的管理員
     * @param organizerId 組織 ID
     * @param reason      退件原因（顯示給組織修正）
     */
    @Transactional
    public void rejectKyc(String adminUserId, String organizerId, String reason) {
        log.info("管理員審核 KYC 退件，組織ID: {}, 管理員ID: {}", organizerId, adminUserId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        if (org.getKycStatus() != KycStatus.PENDING && org.getKycStatus() != KycStatus.APPROVED) {
            throw new IllegalStateException("只有處於 PENDING 或 APPROVED 狀態的組織可以進行退件或撤銷");
        }

        User adminUser = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該管理員"));

        // 若是由 APPROVED 退回，視為「撤銷認證」：防禦性鎖定。
        // 目前 runtime 未對 org.status 做售票 gating，故不可在仍有進行中(ACTIVE)活動時撤銷，
        // 否則會造成「後台已撤銷、前台仍可售票」的合規漏洞。必須先請該組織下架所有活動。
        if (org.getKycStatus() == KycStatus.APPROVED) {
            long activeEvents = themeRepository.countByOrganizerOrganizerIdAndStatus(organizerId, Status.ACTIVE);
            if (activeEvents > 0) {
                throw new IllegalStateException(
                        "無法撤銷認證：該組織尚有 " + activeEvents + " 個進行中(ACTIVE)活動，"
                                + "請先請組織下架所有活動並確認無未結算款項後再撤銷。");
            }
            org.setStatus(OrganizerStatus.SUSPENDED);
            log.info("組織 KYC 從 APPROVED 被撤銷，已將組織狀態設為 SUSPENDED: {}", organizerId);
        }

        // 更新狀態為 REJECTED 並記錄退件原因與審核人/時間
        org.setKycStatus(KycStatus.REJECTED);
        org.setKycRejectionReason(reason);
        org.setKycReviewedBy(adminUser);
        org.setKycReviewedAt(LocalDateTime.now());
        organizerRepository.save(org);

        // 退件＝全部重來：將 DRAFT 合約退回未簽署，使用者修正後重新提交會再次簽署
        contractService.revertDraftToUnsigned(org);
        log.info("組織 KYC 已退件: {}", organizerId);
    }

    /**
     * 更新組織 Logo URL（只更新 kyc_data_json 中的 logo_url 欄位，不影響其他 KYC 資料）
     * 由 OrgAvatarController 呼叫，Logo 上傳後立即同步儲存到資料庫
     *
     * @param userId      操作者 userId（需為 OWNER 或 ADMIN）
     * @param organizerId 組織 ID
     * @param logoUrl     新的 Logo URL（格式: /api/organizer/avatars/ORG0000001.jpg）
     */
    @Transactional
    public void updateLogoUrl(String userId, String organizerId, String logoUrl) {
        log.info("更新組織 Logo URL，組織: {}, 操作者: {}", organizerId, userId);

        // 權限檢查：必須是 OWNER 或 ADMIN
        verifyOwnerOrAdmin(userId, organizerId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        try {
            // 解析現有的 kyc_data_json（若為空則建立空 Map）
            String existingJson = org.getKycDataJson();
            @SuppressWarnings("unchecked")
            Map<String, String> kycData = (existingJson != null && !existingJson.isBlank())
                    ? objectMapper.readValue(existingJson, Map.class)
                    : new HashMap<>();

            // 記住舊 Logo，覆蓋後若已被換掉則刪除舊檔（Logo 採唯一檔名，不會就地覆蓋）
            String oldLogoUrl = kycData.get("logo_url");
            String newLogoUrl = logoUrl != null ? logoUrl : "";

            // 只更新 logo_url，保留其他所有欄位不變
            kycData.put("logo_url", newLogoUrl);

            org.setKycDataJson(objectMapper.writeValueAsString(kycData));
            organizerRepository.save(org);
            log.info("組織 Logo URL 已更新: {}", newLogoUrl);

            // 清理被取代的舊 Logo 檔（best-effort，失敗僅記錄不影響更新）
            deleteReplacedOrgAvatarFile(oldLogoUrl, newLogoUrl);

        } catch (IOException e) {
            log.error("更新組織 Logo URL 失敗，組織: {}", organizerId, e);
            throw new RuntimeException("Logo URL 更新失敗，請稍後再試");
        }
    }

    /**
     * 儲存組織基本資料（草稿存檔）。
     * 與 submitKyc 的差異：不變更 KYC 狀態、不要求所有 KYC 欄位填妥，供使用者逐步補齊。
     * 合併寫入 kyc_data_json：更新聯絡/負責人欄位，並保留既有 logo_url 與證明文件欄位（Logo 由 /logo 端點另行維護）。
     *
     * @param userId      操作者 userId（需為 OWNER 或 ADMIN）
     * @param organizerId 組織 ID
     * @param request     基本資料內容
     */
    @Transactional
    public void updateOrganizerProfile(String userId, String organizerId, UpdateOrganizerProfileRequest request) {
        log.info("儲存組織基本資料，組織: {}, 操作者: {}", organizerId, userId);

        // 權限檢查：必須是 OWNER 或 ADMIN
        verifyOwnerOrAdmin(userId, organizerId);

        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        try {
            // 1. 合併既有 kyc_data_json，保留 logo_url 與證明文件等欄位不被清掉
            String existingJson = org.getKycDataJson();
            @SuppressWarnings("unchecked")
            Map<String, String> kycData = (existingJson != null && !existingJson.isBlank())
                    ? objectMapper.readValue(existingJson, Map.class)
                    : new HashMap<>();

            kycData.put("address", trimOrEmpty(request.getAddress()));
            kycData.put("phone", trimOrEmpty(request.getPhone()));
            kycData.put("fax", trimOrEmpty(request.getFax()));
            kycData.put("owner_name", trimOrEmpty(request.getOwnerName()));
            kycData.put("owner_id_number", trimOrEmpty(request.getOwnerIdNumber()));

            // 2. 封裝銀行帳戶 JSON
            Map<String, String> bankInfo = new HashMap<>();
            bankInfo.put("bank_code", trimOrEmpty(request.getBankCode()));
            bankInfo.put("bank_name", trimOrEmpty(request.getBankName()));
            bankInfo.put("account_no", trimOrEmpty(request.getAccountNo()));
            bankInfo.put("account_name", trimOrEmpty(request.getAccountName()));

            // 3. 更新欄位並儲存（不變更 kyc_status）
            org.setName(request.getName().trim());
            org.setTaxId(request.getTaxId() != null ? request.getTaxId().trim() : null);
            org.setBankAccountInfo(objectMapper.writeValueAsString(bankInfo));
            org.setKycDataJson(objectMapper.writeValueAsString(kycData));

            organizerRepository.save(org);
            log.info("組織基本資料已儲存: {}", organizerId);
        } catch (IOException e) {
            log.error("儲存組織基本資料失敗，組織: {}", organizerId, e);
            throw new RuntimeException("儲存基本資料失敗，請稍後再試");
        }
    }

    /** 去除前後空白；null 轉為空字串 */
    private String trimOrEmpty(String value) {
        return value != null ? value.trim() : "";
    }

    /**
     * 從 kyc_data_json 取出 logo_url（解析失敗則回傳 null）
     */
    private String extractLogoUrl(String kycDataJson) {
        if (kycDataJson == null || kycDataJson.isBlank())
            return null;
        try {
            @SuppressWarnings("unchecked")
            Map<String, String> kycData = objectMapper.readValue(kycDataJson, Map.class);
            return kycData.get("logo_url");
        } catch (IOException e) {
            return null;
        }
    }

    /**
     * 刪除被取代的舊組織 Logo 實體檔。
     * 只處理本系統上傳的 Logo（URL 以 {@value #ORG_AVATAR_URL_PREFIX} 開頭）；
     * data:（預設/配色 SVG）、外部 URL 一律略過。URL 未變更時不刪除。
     */
    private void deleteReplacedOrgAvatarFile(String oldUrl, String newUrl) {
        if (oldUrl == null || oldUrl.equals(newUrl))
            return;
        if (!oldUrl.startsWith(ORG_AVATAR_URL_PREFIX))
            return;

        // 取出檔名並去除可能殘留的查詢參數
        String fileName = oldUrl.substring(ORG_AVATAR_URL_PREFIX.length());
        int queryIdx = fileName.indexOf('?');
        if (queryIdx >= 0)
            fileName = fileName.substring(0, queryIdx);
        if (fileName.isBlank())
            return;

        try {
            Path avatarDir = Paths.get(documentsDir, "public", "org-logos").toAbsolutePath().normalize();
            Path filePath = avatarDir.resolve(fileName).normalize();

            // 安全防禦：確保只刪除 avatars 目錄下的檔案，防止路徑穿越
            if (!filePath.startsWith(avatarDir)) {
                log.warn("拒絕刪除可疑路徑的舊組織 Logo: {}", fileName);
                return;
            }

            if (Files.deleteIfExists(filePath)) {
                log.info("已刪除被取代的舊組織 Logo: {}", filePath);
            }
        } catch (IOException e) {
            log.warn("刪除舊組織 Logo 檔失敗（略過，不影響更新）: {}", oldUrl, e);
        }
    }

    private String generateUniqueRoleId() {
        String roleId;
        int guard = 0;
        do {
            Long nextVal = roleRepository.getNextRoleSequenceValue();
            roleId = String.format("ROL%07d", nextVal);
            if (++guard > 100) {
                throw new IllegalStateException("產生角色流水號失敗：連續撞號，請聯絡系統管理員");
            }
        } while (roleRepository.existsById(roleId));
        return roleId;
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
