package tw.com.ispan.backend.organizer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.organizer.dto.AdminOrganizerDetail;
import tw.com.ispan.backend.organizer.dto.AdminOrganizerListItem;
import tw.com.ispan.backend.organizer.entity.Contract;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.entity.OrganizerMember;
import tw.com.ispan.backend.organizer.enums.KycStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerMemberStatus;
import tw.com.ispan.backend.organizer.enums.OrganizerStatus;
import tw.com.ispan.backend.organizer.repository.ContractRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerMemberRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.subscription.entity.OrganizerSubscription;
import tw.com.ispan.backend.subscription.repository.OrganizerSubscriptionRepository;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.ThemeRepository;

/**
 * Admin 後台「組織詳情 / 狀態」業務邏輯。
 *
 * <p>地基批次：重用 {@link OrganizerRepository} 等既有 repository，提供
 * 對齊前端的組織清單、組織詳情（資料 + 成員 + 合約 + 訂閱）與狀態切換
 * （暫停 / 恢復 / 封存）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrganizerService {

    private final OrganizerRepository organizerRepository;
    private final OrganizerMemberRepository organizerMemberRepository;
    private final ContractRepository contractRepository;
    private final OrganizerSubscriptionRepository organizerSubscriptionRepository;
    private final ThemeRepository themeRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    /** 受保護的 KYC 文件位元組（供 Admin 下載/預覽端點使用）。type: DOC=商業登記 / ID=負責人身分證。 */
    public record KycFile(byte[] data, String contentType, String fileName) {
    }

    /**
     * Admin 組織清單。statusOrNull 為 null 回傳全部，否則依 KYC 狀態篩選（沿用 KYC 待審清單語意）。
     * 回傳含 status / memberCount，對齊前端 OrganizerList。
     */
    @Transactional(readOnly = true)
    public List<AdminOrganizerListItem> listOrganizers(KycStatus statusOrNull) {
        List<Organizer> organizers = (statusOrNull == null)
                ? organizerRepository.findAll()
                : organizerRepository.findByKycStatus(statusOrNull);

        return organizers.stream()
                .map(org -> new AdminOrganizerListItem(
                        org.getOrganizerId(),
                        org.getName(),
                        org.getTaxId(),
                        org.getStatus() == null ? null : org.getStatus().ordinal(),
                        org.getKycStatus() == null ? null : org.getKycStatus().ordinal(),
                        countAcceptedMembers(org),
                        org.getCreatedAt()))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public AdminOrganizerDetail getDetail(String organizerId) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        Map<String, String> kycData = parseKycData(org.getKycDataJson());
        Map<String, String> bankInfo = parseKycData(org.getBankAccountInfo());

        // 成員（僅 ACCEPTED）
        List<AdminOrganizerDetail.Member> members = organizerMemberRepository.findByOrganizer(org).stream()
                .filter(m -> m.getStatus() == OrganizerMemberStatus.ACCEPTED && m.getUser() != null)
                .map(this::toMember)
                .collect(Collectors.toList());

        // 合約（新到舊）
        List<AdminOrganizerDetail.ContractRow> contracts = contractRepository
                .findByOrganizer_OrganizerIdOrderByCreatedAtDesc(organizerId).stream()
                .map(this::toContractRow)
                .collect(Collectors.toList());

        // 訂閱摘要：優先取 ACTIVE，否則取任一筆最新
        AdminOrganizerDetail.Subscription subscription = resolveSubscription(org);

        // 認證文件：僅當實際有檔名時才提供下載 path（受保護端點，依文件類型分流）
        String registrationDocName = emptyToNull(kycData.get("registration_doc_name"));
        String registrationDocUrl = registrationDocName == null ? null
                : "/api/admin/organizers/" + org.getOrganizerId() + "/kyc/file?type=DOC";
        String identityCardName = emptyToNull(kycData.get("identity_card_name"));
        String identityCardUrl = identityCardName == null ? null
                : "/api/admin/organizers/" + org.getOrganizerId() + "/kyc/file?type=ID";

        // 審核人：kyc_reviewed_by 為 LAZY User，取其姓名（可能尚未審核）
        String reviewedBy = org.getKycReviewedBy() != null ? org.getKycReviewedBy().getName() : null;

        return new AdminOrganizerDetail(
                org.getOrganizerId(),
                org.getName(),
                org.getTaxId(),
                org.getStatus() == null ? null : org.getStatus().ordinal(),
                org.getKycStatus() == null ? null : org.getKycStatus().ordinal(),
                emptyToNull(kycData.get("phone")),
                emptyToNull(kycData.get("address")),
                org.getCreatedAt(),
                emptyToNull(kycData.get("owner_name")),
                emptyToNull(kycData.get("owner_id_number")),
                registrationDocName,
                registrationDocUrl,
                identityCardName,
                identityCardUrl,
                emptyToNull(bankInfo.get("bank_code")),
                emptyToNull(bankInfo.get("bank_name")),
                emptyToNull(bankInfo.get("account_no")),
                emptyToNull(bankInfo.get("account_name")),
                reviewedBy,
                org.getKycReviewedAt(),
                emptyToNull(org.getKycRejectionReason()),
                members,
                contracts,
                subscription,
                themeRepository.countByOrganizerOrganizerIdAndStatus(org.getOrganizerId(), Status.ACTIVE));
    }

    private static String emptyToNull(String v) {
        return (v == null || v.isBlank()) ? null : v;
    }

    /**
     * 讀取指定組織的 KYC 認證文件原始位元組（供 Admin 受保護下載端點使用）。
     * 檔案相對路徑存於 kyc_data_json（{@code registration_doc_url} / {@code identity_card_url}），
     * 由 {@link KycFileService} 寫入，位於 {@code documents/secure/kyc/...}。
     *
     * @param organizerId 組織 ID
     * @param type        "DOC"（商業登記文件）或 "ID"（負責人身分證）
     * @return KycFile（含位元組、content-type、檔名），查無檔案回 null
     */
    @Transactional(readOnly = true)
    public KycFile readKycFile(String organizerId, String type) {
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));

        Map<String, String> kycData = parseKycData(org.getKycDataJson());
        String relativePath;
        if ("DOC".equalsIgnoreCase(type)) {
            relativePath = kycData.get("registration_doc_url");
        } else if ("ID".equalsIgnoreCase(type)) {
            relativePath = kycData.get("identity_card_url");
        } else {
            throw new IllegalArgumentException("不支援的 KYC 文件類型: " + type);
        }
        if (relativePath == null || relativePath.isBlank()) {
            return null;
        }

        // 鋼鐵防禦：解析後須落在 documents 根目錄內，杜絕路徑穿越
        Path docsRoot = Paths.get(documentsDir).toAbsolutePath().normalize();
        Path target = docsRoot.resolve(relativePath).toAbsolutePath().normalize();
        if (!target.startsWith(docsRoot)) {
            log.warn("偵測到非法的 KYC 文件路徑 organizerId={}, path={}", organizerId, relativePath);
            return null;
        }
        try {
            if (!Files.exists(target)) {
                return null;
            }
            byte[] data = Files.readAllBytes(target);
            String fileName = target.getFileName().toString();
            return new KycFile(data, resolveContentType(fileName), fileName);
        } catch (IOException e) {
            log.warn("讀取 KYC 文件失敗 organizerId={}, type={}", organizerId, type, e);
            return null;
        }
    }

    private static String resolveContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".pdf")) {
            return "application/pdf";
        }
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    /**
     * 切換組織狀態。statusOrdinal：0=ACTIVE(恢復) / 1=SUSPENDED(暫停) / 2=ARCHIVED(封存)。
     */
    @Transactional
    public void changeStatus(String organizerId, int statusOrdinal) {
        OrganizerStatus[] values = OrganizerStatus.values();
        if (statusOrdinal < 0 || statusOrdinal >= values.length) {
            throw new IllegalArgumentException("無效的組織狀態值: " + statusOrdinal);
        }
        Organizer org = organizerRepository.findById(organizerId)
                .orElseThrow(() -> new IllegalArgumentException("找不到該組織"));
        org.setStatus(values[statusOrdinal]);
        organizerRepository.save(org);
        log.info("組織狀態已更新 organizerId={}, status={}", organizerId, values[statusOrdinal]);
    }

    // ── private helpers ──

    private long countAcceptedMembers(Organizer org) {
        return organizerMemberRepository.findByOrganizer(org).stream()
                .filter(m -> m.getStatus() == OrganizerMemberStatus.ACCEPTED)
                .count();
    }

    private AdminOrganizerDetail.Member toMember(OrganizerMember m) {
        return new AdminOrganizerDetail.Member(
                m.getUser().getUserId(),
                m.getUser().getName(),
                m.getUser().getEmail(),
                m.getRole() != null ? m.getRole().getRoleName() : "MEMBER",
                m.getJoinedAt());
    }

    private AdminOrganizerDetail.ContractRow toContractRow(Contract c) {
        return new AdminOrganizerDetail.ContractRow(
                c.getContractId(),
                c.getContractType() != null ? c.getContractType().ordinal() : null,
                c.getContractStatus() != null ? c.getContractStatus().ordinal() : null,
                c.getFeeType() != null ? c.getFeeType().ordinal() : null,
                c.getFeeValue(),
                c.getSignedAt(),
                c.getValidTo());
    }

    private AdminOrganizerDetail.Subscription resolveSubscription(Organizer org) {
        List<OrganizerSubscription> subs = organizerSubscriptionRepository.findByOrganizer(org);
        if (subs == null || subs.isEmpty()) {
            return null;
        }
        OrganizerSubscription chosen = subs.stream()
                .filter(s -> s.getStatusCode() != null && s.getStatusCode().name().equals("ACTIVE"))
                .findFirst()
                .orElse(subs.get(0));
        String planName = chosen.getPlan() != null ? chosen.getPlan().getPlanName() : null;
        return new AdminOrganizerDetail.Subscription(
                planName,
                chosen.getStatusCode() != null ? chosen.getStatusCode().name() : null,
                chosen.getStartDate(),
                chosen.getEndDate());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> parseKycData(String kycDataJson) {
        if (kycDataJson == null || kycDataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(kycDataJson, Map.class);
        } catch (Exception e) {
            log.warn("解析組織 kyc_data_json 失敗，回傳空 Map", e);
            return Map.of();
        }
    }
}
