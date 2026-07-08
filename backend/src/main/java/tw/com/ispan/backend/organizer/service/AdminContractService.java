package tw.com.ispan.backend.organizer.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.organizer.dto.AdminContractDetail;
import tw.com.ispan.backend.organizer.dto.AdminContractListItem;
import tw.com.ispan.backend.organizer.entity.Contract;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.enums.ContractStatus;
import tw.com.ispan.backend.organizer.enums.ContractType;
import tw.com.ispan.backend.organizer.enums.FeeType;
import tw.com.ispan.backend.organizer.repository.ContractRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.system.entity.SystemConfig;
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.repository.SystemConfigRepository;
import tw.com.ispan.backend.system.service.MediaFileRecorder;

/**
 * Admin 後台「合約管理」業務邏輯（批次 2）。
 *
 * <p>重用既有 {@link Contract} entity / {@link ContractRepository} 與 enum，
 * 另補上 Admin 全平台清單 / 詳情 / 公版範本讀寫 / 客製合約建立。</p>
 *
 * <p><b>公版範本存放方式</b>：範本 markdown 內容可能很長，
 * {@code SystemConfig.config_value} 僅 VARCHAR(255) 放不下，故採「檔案 + 設定指標」：
 * markdown 存於 {@code documents/contracts/template/template_{version}.md}，
 * 目前版本字串存於 {@link SystemConfig}（config_key = {@code CONTRACT_TEMPLATE_VERSION}）。
 * 每次儲存版號 +1（v1 -> v2 ...），舊檔保留，達成「新版本不影響既有合約版本快照」語意。</p>
 *
 * <p><b>客製合約檔案</b>：比照 {@code OrgAvatarController} 的存檔慣例，
 * .md 與已簽署 PDF 存於 {@code documents/contracts/custom/{contractId}.(md|pdf)}，
 * PDF 透過本服務的 {@code GET /api/admin/contracts/{id}/pdf}（受 CONTRACT_VIEW 保護）下載。</p>
 *
 * <p><b>數字碼轉換</b>：對外一律用「前端編碼」——
 * contractType 1=公版/2=客製；feeType 1=百分比/2=固定；contractStatus 0草稿/1生效/2到期/3終止。
 * 與後端 entity ordinal 不同（見各 mapXxx 方法），刻意不動 B2B 用的 {@code ContractResponse}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminContractService {

    private final ContractRepository contractRepository;
    private final OrganizerRepository organizerRepository;
    private final UserRepository userRepository;
    private final SystemConfigRepository systemConfigRepository;
    private final MediaFileRecorder mediaFileRecorder;

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    private static final String TEMPLATE_VERSION_KEY = "CONTRACT_TEMPLATE_VERSION";
    private static final String DEFAULT_TEMPLATE_MD = "# TAP 平台服務合約（公版）\n\n"
            + "## 第一條　合約目的\n"
            + "主辦單位（以下稱乙方）同意使用 TAP 售票平台（以下稱甲方）提供之售票服務。\n\n"
            + "## 第二條　服務費率\n"
            + "甲方依乙方每筆成功交易金額收取 {{fee_value}}% 平台服務費。\n\n"
            + "## 第三條　結算與撥款\n"
            + "1. 結算週期為每自然月。\n"
            + "2. 甲方於次月 10 個工作天內完成撥款。\n\n"
            + "## 第四條　合約期間\n"
            + "本合約自簽署日起生效，效期一年，期滿未終止自動續約。";

    // ── 清單 ──

    @Transactional(readOnly = true)
    public List<AdminContractListItem> listContracts(Integer contractTypeFilter, Integer contractStatusFilter) {
        String templateVersion = currentTemplateVersion();
        return contractRepository.findAllWithOrganizer().stream()
                .map(c -> toListItem(c, templateVersion))
                .filter(item -> contractTypeFilter == null || item.getContractType().equals(contractTypeFilter))
                .filter(item -> contractStatusFilter == null || item.getContractStatus().equals(contractStatusFilter))
                .toList();
    }

    // ── 詳情 ──

    @Transactional(readOnly = true)
    public AdminContractDetail getContractDetail(String contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("找不到合約: " + contractId));

        Organizer org = c.getOrganizer();
        boolean isCustom = c.getContractType() == ContractType.CUSTOM;
        String templateVersion = currentTemplateVersion();

        String contentMd;
        String pdfUrl = null;
        if (isCustom) {
            contentMd = readCustomMd(contractId);
            if (Files.exists(customFilePath(contractId, "pdf"))) {
                pdfUrl = "/api/admin/contracts/" + contractId + "/pdf";
            }
        } else {
            // 公版：以目前範本內容作為版本快照呈現（簡化：未保存逐版快照，見 class doc）
            contentMd = readTemplateMd(templateVersion);
        }

        return AdminContractDetail.builder()
                .id(c.getContractId())
                .orgId(org != null ? org.getOrganizerId() : null)
                .orgName(org != null ? org.getName() : null)
                .contractType(mapTypeToFront(c.getContractType()))
                .contractStatus(mapStatusToFront(c.getContractStatus()))
                .feeType(mapFeeTypeToFront(c.getFeeType()))
                .feeValue(c.getFeeValue())
                .version(isCustom ? "custom" : templateVersion)
                .signedAt(c.getSignedAt())
                .signedBy(c.getSignedBy() != null ? c.getSignedBy().getName() : null)
                .expiresAt(c.getValidTo())
                .pdfUrl(pdfUrl)
                .contentMd(contentMd)
                .build();
    }

    // ── 公版範本 ──

    @Transactional(readOnly = true)
    public TemplateView getTemplate() {
        String version = currentTemplateVersion();
        return new TemplateView(version, readTemplateMd(version), getDefaultFeeTypeFront(), getDefaultFeeValue());
    }

    /** 儲存新版本：版號 +1，寫入新檔，更新 SystemConfig 指標。 */
    @Transactional
    public TemplateView saveTemplate(String contentMd) {
        if (contentMd == null || contentMd.isBlank()) {
            throw new IllegalArgumentException("合約內容不可為空");
        }
        String nextVersion = nextTemplateVersion(currentTemplateVersion());
        try {
            Path file = templateFilePath(nextVersion);
            Files.createDirectories(file.getParent());
            Files.writeString(file, contentMd, StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("寫入公版合約範本失敗", e);
            throw new IllegalStateException("範本儲存失敗，請稍後再試");
        }
        SystemConfig cfg = systemConfigRepository.findByConfigKey(TEMPLATE_VERSION_KEY)
                .orElseGet(() -> SystemConfig.builder()
                        .configId("CFG_CONTRACT_TPL")
                        .configKey(TEMPLATE_VERSION_KEY)
                        .description("公版合約範本目前版本")
                        .build());
        cfg.setConfigValue(nextVersion);
        systemConfigRepository.save(cfg);
        log.info("公版合約範本已儲存新版本: {}", nextVersion);
        return new TemplateView(nextVersion, contentMd, getDefaultFeeTypeFront(), getDefaultFeeValue());
    }

    // ── 客製合約 ──

    @Transactional
    public AdminContractDetail createCustomContract(String adminUserId, String orgId, Integer feeTypeFront,
            BigDecimal feeValue, LocalDate expiresAt, String contentMd, MultipartFile pdf) {

        if (orgId == null || orgId.isBlank()) {
            throw new IllegalArgumentException("請選擇簽約組織");
        }
        if (feeValue == null || feeValue.signum() < 0) {
            throw new IllegalArgumentException("請填寫有效費率");
        }
        if (contentMd == null || contentMd.isBlank()) {
            throw new IllegalArgumentException("合約內容不可為空");
        }
        if (pdf == null || pdf.isEmpty()) {
            throw new IllegalArgumentException("客製合約須上傳已簽署的紙本掃描 PDF");
        }
        if (!"application/pdf".equalsIgnoreCase(pdf.getContentType())) {
            throw new IllegalArgumentException("僅支援上傳 PDF 格式的已簽署合約");
        }

        Organizer org = organizerRepository.findById(orgId)
                .orElseThrow(() -> new IllegalArgumentException("找不到組織: " + orgId));
        User admin = userRepository.findById(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("找不到操作者"));

        Long nextVal = contractRepository.getNextContractSequenceValue();
        String contractId = String.format("CON%07d", nextVal);

        Contract contract = Contract.builder()
                .contractId(contractId)
                .organizer(org)
                .contractType(ContractType.CUSTOM)
                .feeType(mapFeeTypeToEntity(feeTypeFront))
                .feeValue(feeValue)
                .validFrom(LocalDateTime.now())
                .validTo(expiresAt != null ? expiresAt.atTime(23, 59, 59) : null)
                .contractStatus(ContractStatus.ACTIVE) // 線下已簽署完成 → 直接生效
                .signedAt(LocalDateTime.now())
                .signedBy(org.getOwner())              // 線下簽約以 owner 為簽署人
                .createdBy(admin)
                .build();
        contractRepository.save(contract);

        // 存檔：.md + .pdf
        try {
            Path mdPath = customFilePath(contractId, "md");
            Files.createDirectories(mdPath.getParent());
            Files.writeString(mdPath, contentMd, StandardCharsets.UTF_8);
            Path pdfPath = customFilePath(contractId, "pdf");
            Files.write(pdfPath, pdf.getBytes());
            mediaFileRecorder.record(adminUserId, "contract", contractId, FileType.DOCUMENT,
                    "/api/admin/contracts/" + contractId + "/pdf", pdfPath);
        } catch (IOException e) {
            log.error("客製合約檔案儲存失敗 contractId={}", contractId, e);
            throw new IllegalStateException("合約檔案儲存失敗，請稍後再試");
        }
        log.info("客製合約建立成功 contractId={}, orgId={}, 操作者={}", contractId, orgId, adminUserId);

        return getContractDetail(contractId);
    }

    // ── mapping helpers（entity ordinal ↔ 前端編碼）──

    /** contractType：CUSTOM(2)→2(客製)；其餘(FREE_STANDARD/ANNUAL_FEE)→1(公版) */
    private int mapTypeToFront(ContractType t) {
        return t == ContractType.CUSTOM ? 2 : 1;
    }

    /** feeType：PERCENTAGE(0)→1；FIXED_PER_TICKET(1)→2 */
    private int mapFeeTypeToFront(FeeType f) {
        return f == FeeType.FIXED_PER_TICKET ? 2 : 1;
    }

    private FeeType mapFeeTypeToEntity(Integer front) {
        return (front != null && front == 2) ? FeeType.FIXED_PER_TICKET : FeeType.PERCENTAGE;
    }

    /** contractStatus：DRAFT0→0；ACTIVE1→1；TERMINATED2→3(已終止)；EXPIRED3→2(已到期) */
    private int mapStatusToFront(ContractStatus s) {
        return switch (s) {
            case DRAFT -> 0;
            case ACTIVE -> 1;
            case TERMINATED -> 3;
            case EXPIRED -> 2;
        };
    }

    private AdminContractListItem toListItem(Contract c, String templateVersion) {
        boolean isCustom = c.getContractType() == ContractType.CUSTOM;
        Organizer org = c.getOrganizer();
        return AdminContractListItem.builder()
                .id(c.getContractId())
                .orgId(org != null ? org.getOrganizerId() : null)
                .orgName(org != null ? org.getName() : null)
                .contractType(mapTypeToFront(c.getContractType()))
                .contractStatus(mapStatusToFront(c.getContractStatus()))
                .feeType(mapFeeTypeToFront(c.getFeeType()))
                .feeValue(c.getFeeValue())
                .version(isCustom ? "custom" : templateVersion)
                .signedAt(c.getSignedAt())
                .expiresAt(c.getValidTo())
                .build();
    }

    // ── template version / file helpers ──

    private String currentTemplateVersion() {
        return systemConfigRepository.findByConfigKey(TEMPLATE_VERSION_KEY)
                .map(cfg -> cfg.getConfigValue())
                .filter(v -> v != null && !v.isBlank())
                .orElse("v1");
    }

    /** v1 -> v2 -> v3 ... 解析失敗則退回 v2 */
    private String nextTemplateVersion(String current) {
        try {
            int n = Integer.parseInt(current.replaceAll("[^0-9]", ""));
            return "v" + (n + 1);
        } catch (NumberFormatException e) {
            return "v2";
        }
    }

    private Path templateFilePath(String version) {
        return Paths.get(documentsDir, "contracts", "template", "template_" + version + ".md")
                .toAbsolutePath().normalize();
    }

    private String readTemplateMd(String version) {
        Path file = templateFilePath(version);
        try {
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("讀取公版合約範本失敗 version={}", version, e);
        }
        return DEFAULT_TEMPLATE_MD;
    }

    private Path customFilePath(String contractId, String ext) {
        return Paths.get(documentsDir, "contracts", "custom", contractId + "." + ext)
                .toAbsolutePath().normalize();
    }

    private String readCustomMd(String contractId) {
        Path file = customFilePath(contractId, "md");
        try {
            if (Files.exists(file)) {
                return Files.readString(file, StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            log.warn("讀取客製合約內容失敗 contractId={}", contractId, e);
        }
        return "（找不到合約內容檔案）";
    }

    /** 讀取客製合約 PDF 原始位元組（供下載端點使用）。查無檔案回 null。 */
    @Transactional(readOnly = true)
    public byte[] readCustomPdf(String contractId) {
        Contract c = contractRepository.findById(contractId)
                .orElseThrow(() -> new IllegalArgumentException("找不到合約: " + contractId));
        if (c.getContractType() != ContractType.CUSTOM) {
            return null;
        }
        Path file = customFilePath(contractId, "pdf");
        try {
            if (Files.exists(file)) {
                return Files.readAllBytes(file);
            }
        } catch (IOException e) {
            log.warn("讀取客製合約 PDF 失敗 contractId={}", contractId, e);
        }
        return null;
    }

    /** 範本回應投影：version + contentMd */
    public record TemplateView(
            String version,
            String contentMd,
            Integer defaultFeeType,
            BigDecimal defaultFeeValue
    ) {}

    private Integer getDefaultFeeTypeFront() {
        return systemConfigRepository.findByConfigKey("CONTRACT_DEFAULT_FEE_TYPE")
                .map(cfg -> {
                    try {
                        int typeVal = Integer.parseInt(cfg.getConfigValue());
                        return typeVal == 1 ? 2 : 1;
                    } catch (NumberFormatException e) {
                        if ("FIXED_PER_TICKET".equalsIgnoreCase(cfg.getConfigValue())) {
                            return 2;
                        }
                        return 1;
                    }
                })
                .orElse(1);
    }

    private BigDecimal getDefaultFeeValue() {
        return systemConfigRepository.findByConfigKey("CONTRACT_DEFAULT_FEE_VALUE")
                .map(cfg -> {
                    try {
                        return new BigDecimal(cfg.getConfigValue());
                    } catch (NumberFormatException e) {
                        return new BigDecimal("5.0000");
                    }
                })
                .orElse(new BigDecimal("5.0000"));
    }
}
