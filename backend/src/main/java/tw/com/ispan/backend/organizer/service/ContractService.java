package tw.com.ispan.backend.organizer.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.organizer.dto.ContractResponse;
import tw.com.ispan.backend.organizer.entity.Contract;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.organizer.enums.ContractStatus;
import tw.com.ispan.backend.organizer.enums.ContractType;
import tw.com.ispan.backend.organizer.enums.FeeType;
import tw.com.ispan.backend.organizer.repository.ContractRepository;
import tw.com.ispan.backend.system.repository.SystemConfigRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class ContractService {

    private final ContractRepository contractRepository;
    private final SystemConfigRepository systemConfigRepository;

    /**
     * 查詢某組織的所有合約（依建立時間由新到舊）。權限檢查由呼叫端負責。
     */
    @Transactional(readOnly = true)
    public List<ContractResponse> getContractsByOrganizer(String organizerId) {
        return contractRepository.findByOrganizer_OrganizerIdOrderByCreatedAtDesc(organizerId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /** Entity → 回應 DTO，enum 一律轉 ordinal 數字碼 */
    private ContractResponse toResponse(Contract c) {
        return ContractResponse.builder()
                .contractId(c.getContractId())
                .contractType(c.getContractType() != null ? c.getContractType().ordinal() : null)
                .feeType(c.getFeeType() != null ? c.getFeeType().ordinal() : null)
                .feeValue(c.getFeeValue())
                .validFrom(c.getValidFrom())
                .validTo(c.getValidTo())
                .contractStatus(c.getContractStatus() != null ? c.getContractStatus().ordinal() : null)
                .signedAt(c.getSignedAt())
                .signedByName(c.getSignedBy() != null ? c.getSignedBy().getName() : null)
                .createdAt(c.getCreatedAt())
                .build();
    }

    /**
     * 組織建立時，預先寫入一筆「尚未簽署」的免費標準方案 DRAFT 合約。
     * 讓合約頁面在 KYC 提交前即可呈現真實的預設方案（尚未簽署狀態），無需前端造假。
     * signedAt / signedBy 留空（DB CHECK 僅要求非 DRAFT 才需簽署資訊）；validFrom 暫填，生效時重設。
     *
     * @param organizer 新建立的組織
     * @param createdBy 建立組織的使用者
     */
    @Transactional
    public Contract createUnsignedDraftFreeContract(Organizer organizer, User createdBy) {
        log.info("組織建立，預先寫入未簽署的免費標準 DRAFT 合約，組織ID: {}", organizer.getOrganizerId());

        // 防呆：已存在任何合約則略過（避免重複建立）
        boolean hasAny = !contractRepository
                .findByOrganizer_OrganizerIdOrderByCreatedAtDesc(organizer.getOrganizerId())
                .isEmpty();
        if (hasAny) {
            log.info("組織 {} 已有合約紀錄，略過預建 DRAFT", organizer.getOrganizerId());
            return null;
        }

        Long nextVal = contractRepository.getNextContractSequenceValue();
        Contract draft = Contract.builder()
                .contractId(String.format("CON%07d", nextVal))
                .organizer(organizer)
                .contractType(ContractType.FREE_STANDARD)
                .feeType(getDefaultFeeType())
                .feeValue(getDefaultFeeValue())
                .validFrom(LocalDateTime.now()) // 暫填；提交簽署 / 核准生效時會重設
                .contractStatus(ContractStatus.DRAFT)
                .createdBy(createdBy)
                .build();

        return contractRepository.save(draft);
    }

    @Transactional
    public Contract createActiveFreeContract(Organizer organizer, User approvedByUser) {
        log.info("自動建立免費標準生效合約，組織ID: {}, 審核人: {}", organizer.getOrganizerId(), approvedByUser.getUserId());

        // 檢查是否已經有一份生效中的合約
        contractRepository.findByOrganizer_OrganizerIdAndContractStatus(organizer.getOrganizerId(), ContractStatus.ACTIVE)
            .ifPresent(c -> {
                throw new IllegalStateException("該組織已經有一份生效中的合約");
            });

        // 產生 CONXXXXXXX 格式的主鍵（使用 DB sequence seq_CON，避免 count()+1 撞號）
        Long nextVal = contractRepository.getNextContractSequenceValue();
        String contractId = String.format("CON%07d", nextVal);

        Contract contract = Contract.builder()
            .contractId(contractId)
            .organizer(organizer)
            .contractType(ContractType.FREE_STANDARD)
            .feeType(getDefaultFeeType())
            .feeValue(getDefaultFeeValue())
            .validFrom(LocalDateTime.now())
            .contractStatus(ContractStatus.ACTIVE)
            .signedAt(LocalDateTime.now())
            .signedBy(organizer.getOwner()) // 預設由建立組織者簽署
            .createdBy(approvedByUser)      // 由審核通過的管理員建立
            .build();

        return contractRepository.save(contract);
    }

    /**
     * 使用者於 KYC 提交時線上簽署免費標準合約：建立（或於重新提交時更新）一筆 DRAFT 合約，
     * 並記錄簽署人與簽署時間。草稿需待平台審核通過後才會轉為 ACTIVE 生效。
     *
     * @param organizer 組織
     * @param signer    實際按下「同意」並提交的使用者（簽署人）
     */
    @Transactional
    public Contract signDraftFreeContract(Organizer organizer, User signer) {
        log.info("使用者線上簽署免費標準合約（草稿），組織ID: {}, 簽署人: {}", organizer.getOrganizerId(), signer.getUserId());

        // 已有生效中合約 → 不需再簽（相容既有資料）
        Optional<Contract> active = contractRepository
                .findByOrganizer_OrganizerIdAndContractStatus(organizer.getOrganizerId(), ContractStatus.ACTIVE);
        if (active.isPresent()) {
            log.info("組織 {} 已有生效合約，略過草稿簽署", organizer.getOrganizerId());
            return active.get();
        }

        // 已有草稿（重新提交情境）→ 更新簽署資訊；否則建立新草稿
        Contract draft = contractRepository
                .findByOrganizer_OrganizerIdAndContractStatus(organizer.getOrganizerId(), ContractStatus.DRAFT)
                .orElse(null);

        if (draft == null) {
            Long nextVal = contractRepository.getNextContractSequenceValue();
            draft = Contract.builder()
                    .contractId(String.format("CON%07d", nextVal))
                    .organizer(organizer)
                    .contractType(ContractType.FREE_STANDARD)
                    .feeType(getDefaultFeeType())
                    .feeValue(getDefaultFeeValue())
                    .validFrom(LocalDateTime.now()) // 草稿暫填；生效時會重設為核准當下
                    .contractStatus(ContractStatus.DRAFT)
                    .createdBy(signer)
                    .build();
        }

        draft.setSignedAt(LocalDateTime.now());
        draft.setSignedBy(signer);
        return contractRepository.save(draft);
    }

    /**
     * KYC 退件時，將該組織尚未生效的 DRAFT 合約退回「未簽署」狀態（清空簽署人與簽署時間），
     * 使「退件＝合約退回草稿、全部重來」名實相符。使用者修正後重新提交時會再次簽署。
     *
     * 若為撤銷情境 (APPROVED -> REJECTED)，將連帶把 ACTIVE 合約退回 DRAFT 未簽署狀態。
     *
     * @param organizer 組織
     */
    @Transactional
    public void revertDraftToUnsigned(Organizer organizer) {
        contractRepository
                .findByOrganizer_OrganizerIdAndContractStatus(organizer.getOrganizerId(), ContractStatus.DRAFT)
                .ifPresent(draft -> {
                    if (draft.getSignedAt() != null || draft.getSignedBy() != null) {
                        log.info("KYC 退件，清空組織 {} DRAFT 合約簽署資訊", organizer.getOrganizerId());
                        draft.setSignedAt(null);
                        draft.setSignedBy(null);
                        contractRepository.save(draft);
                    }
                });
        
        contractRepository
                .findByOrganizer_OrganizerIdAndContractStatus(organizer.getOrganizerId(), ContractStatus.ACTIVE)
                .ifPresent(active -> {
                    log.info("KYC 撤銷，將組織 {} 的 ACTIVE 合約退回 DRAFT 未簽署", organizer.getOrganizerId());
                    active.setContractStatus(ContractStatus.DRAFT);
                    active.setSignedAt(null);
                    active.setSignedBy(null);
                    contractRepository.save(active);
                });
    }

    /**
     * 平台審核通過時，將已簽署的 DRAFT 合約轉為 ACTIVE 生效。
     * 若查無 DRAFT（相容舊資料 / 異常路徑），退回建立一份由 owner 簽署的 ACTIVE 合約。
     *
     * @param organizer      組織
     * @param approvedByUser 審核通過的管理員
     */
    @Transactional
    public Contract activateContractOnApproval(Organizer organizer, User approvedByUser) {
        log.info("審核通過，啟用組織合約，組織ID: {}, 審核人: {}", organizer.getOrganizerId(), approvedByUser.getUserId());

        // 已有生效中合約 → 不重複啟用
        Optional<Contract> active = contractRepository
                .findByOrganizer_OrganizerIdAndContractStatus(organizer.getOrganizerId(), ContractStatus.ACTIVE);
        if (active.isPresent()) {
            log.info("組織 {} 已有生效合約，略過啟用", organizer.getOrganizerId());
            return active.get();
        }

        Contract draft = contractRepository
                .findByOrganizer_OrganizerIdAndContractStatus(organizer.getOrganizerId(), ContractStatus.DRAFT)
                .orElse(null);

        // 相容路徑：沒有草稿（舊流程或異常）就直接建立已簽署的生效合約
        if (draft == null) {
            log.warn("組織 {} 無 DRAFT 合約，改用相容路徑建立 ACTIVE 合約", organizer.getOrganizerId());
            return createActiveFreeContract(organizer, approvedByUser);
        }

        // 確保簽署欄位齊全（DB CHECK 約束要求非 DRAFT 必須有 signed_at / signed_by）
        if (draft.getSignedAt() == null || draft.getSignedBy() == null) {
            draft.setSignedAt(LocalDateTime.now());
            draft.setSignedBy(organizer.getOwner());
        }
        draft.setContractStatus(ContractStatus.ACTIVE);
        draft.setValidFrom(LocalDateTime.now());
        return contractRepository.save(draft);
    }

    private FeeType getDefaultFeeType() {
        return systemConfigRepository.findByConfigKey("CONTRACT_DEFAULT_FEE_TYPE")
                .map(cfg -> {
                    try {
                        int typeVal = Integer.parseInt(cfg.getConfigValue());
                        return typeVal == 1 ? FeeType.FIXED_PER_TICKET : FeeType.PERCENTAGE;
                    } catch (NumberFormatException e) {
                        if ("FIXED_PER_TICKET".equalsIgnoreCase(cfg.getConfigValue())) {
                            return FeeType.FIXED_PER_TICKET;
                        }
                        return FeeType.PERCENTAGE;
                    }
                })
                .orElse(FeeType.PERCENTAGE);
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
