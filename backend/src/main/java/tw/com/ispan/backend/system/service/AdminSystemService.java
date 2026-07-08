package tw.com.ispan.backend.system.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.system.entity.MediaFile;
import tw.com.ispan.backend.system.entity.ScheduledJob;
import tw.com.ispan.backend.system.entity.SystemAnnouncement;
import tw.com.ispan.backend.system.entity.SystemAuditLog;
import tw.com.ispan.backend.system.entity.SystemConfig;
import tw.com.ispan.backend.system.entity.SystemDictionary;
import tw.com.ispan.backend.system.entity.UserSubmission;
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.enums.JobStatus;
import tw.com.ispan.backend.system.enums.SubmissionStatus;
import tw.com.ispan.backend.system.repository.MediaFileRepository;
import tw.com.ispan.backend.system.repository.ScheduledJobRepository;
import tw.com.ispan.backend.system.repository.SystemAnnouncementRepository;
import tw.com.ispan.backend.system.repository.SystemAuditLogRepository;
import tw.com.ispan.backend.system.repository.SystemConfigRepository;
import tw.com.ispan.backend.system.repository.SystemDictionaryRepository;
import tw.com.ispan.backend.system.repository.UserSubmissionRepository;

/**
 * Admin 後台「系統管理」業務邏輯（批次 3）——稽核 / 公告 / 客訴 / 字典 / 設定 / 媒體 / 排程。
 *
 * <p>各子模組重用既有 system package 的 entity / repository。
 * 前端頁面欄位與後端枚舉/欄位的對齊均在本層處理（見各方法註解）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminSystemService {

    private final SystemAuditLogRepository auditLogRepository;
    private final SystemAnnouncementRepository announcementRepository;
    private final UserSubmissionRepository submissionRepository;
    private final SystemDictionaryRepository dictionaryRepository;
    private final SystemConfigRepository configRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ScheduledJobRepository scheduledJobRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DATE = DateTimeFormatter.ISO_LOCAL_DATE;

    // ===================== 稽核日誌 =====================

    /**
     * 稽核日誌查詢。後端 AuditActionType（AUTH/IAM/ORGANIZER/FINANCIAL/SYSTEM/CONTENT）
     * 與前端動作篩選（CREATE/UPDATE/DELETE/REVIEW/LOGIN）語意不同：
     * 這裡將 enum 名稱直接作為 action 回傳，action 參數做大小寫不敏感的名稱比對（找不到則不過濾）。
     * 時間以 created_at 落在 [from 00:00, to 23:59:59] 篩選。
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAuditLogs(String from, String to, String action) {
        LocalDateTime fromDt = (from != null && !from.isBlank())
                ? LocalDate.parse(from, DATE).atStartOfDay() : null;
        LocalDateTime toDt = (to != null && !to.isBlank())
                ? LocalDate.parse(to, DATE).atTime(23, 59, 59) : null;

        return auditLogRepository.findAll().stream()
                .filter(l -> fromDt == null || (l.getCreatedAt() != null && !l.getCreatedAt().isBefore(fromDt)))
                .filter(l -> toDt == null || (l.getCreatedAt() != null && !l.getCreatedAt().isAfter(toDt)))
                .filter(l -> action == null || action.isBlank()
                        || (l.getActionType() != null && l.getActionType().name().equalsIgnoreCase(action)))
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toAuditDto)
                .collect(Collectors.toList());
    }

    private Map<String, Object> toAuditDto(SystemAuditLog l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getAuditId());
        m.put("operator", l.getActionUserId());
        m.put("action", l.getActionType() != null ? l.getActionType().name() : null);
        m.put("target", (l.getTargetTable() != null ? l.getTargetTable() : "")
                + (l.getTargetId() != null ? " " + l.getTargetId() : ""));
        m.put("detail", l.getActionDetail());
        m.put("ip", l.getIpAddress());
        m.put("createdAt", l.getCreatedAt());
        return m;
    }

    // ===================== 系統公告 =====================

    // 前端 portal 代碼 ↔ DB target_portal
    private String portalFeToDb(String fe) {
        if (fe == null) return "ALL";
        return switch (fe) {
            case "B2C" -> "B2C_FRONT";
            case "B2B" -> "B2B_PORTAL";
            case "ADMIN" -> "ADMIN_LOCAL";
            default -> "ALL";
        };
    }

    private String portalDbToFe(String db) {
        if (db == null) return "ALL";
        return switch (db) {
            case "B2C_FRONT" -> "B2C";
            case "B2B_PORTAL" -> "B2B";
            case "ADMIN_LOCAL" -> "ADMIN";
            default -> "ALL";
        };
    }

    private LocalDateTime parseDt(Object o) {
        if (o == null) return null;
        String s = o.toString();
        if (s.isBlank()) return null;
        // 前端 datetime-local 形如 2026-06-15T00:00
        return LocalDateTime.parse(s.length() == 16 ? s + ":00" : s);
    }

    private Map<String, Object> toAnnouncementDto(SystemAnnouncement a) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", a.getAnnouncementId());
        m.put("title", a.getTitle());
        m.put("targetPortal", portalDbToFe(a.getTargetPortal()));
        m.put("isActive", a.getIsPublished());
        m.put("startAt", a.getPublishedAt());
        m.put("endAt", a.getExpiresAt());
        m.put("content", a.getContent());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAnnouncements() {
        return announcementRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toAnnouncementDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createAnnouncement(Map<String, Object> body, String userId) {
        String title = strOf(body.get("title"));
        if (title == null || title.isBlank()) {
            throw new IllegalArgumentException("公告標題不可為空");
        }
        // announcement_id 為 CHAR(10)，seq_ANN 取下一號 → ANNXXXXXXX
        Long seq = nextAnnSequence();
        String id = String.format("ANN%07d", seq);

        boolean published = boolOf(body.get("isActive"), false);
        SystemAnnouncement a = SystemAnnouncement.builder()
                .announcementId(id)
                .title(title.trim())
                .content(strOf(body.get("content")) != null ? strOf(body.get("content")) : "")
                .targetPortal(portalFeToDb(strOf(body.get("targetPortal"))))
                .isPublished(published)
                .publishedAt(parseDt(body.get("startAt")))
                .expiresAt(parseDt(body.get("endAt")))
                .createdBy(userId != null ? userRepository.findById(userId).orElse(null) : null)
                .build();
        announcementRepository.save(a);
        log.info("Admin 建立公告 {}", id);
        return toAnnouncementDto(a);
    }

    @Transactional
    public Map<String, Object> updateAnnouncement(String id, Map<String, Object> body) {
        SystemAnnouncement a = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到公告: " + id));
        if (body.containsKey("title") && strOf(body.get("title")) != null) {
            a.setTitle(strOf(body.get("title")).trim());
        }
        if (body.containsKey("content")) {
            a.setContent(strOf(body.get("content")) != null ? strOf(body.get("content")) : "");
        }
        if (body.containsKey("targetPortal")) {
            a.setTargetPortal(portalFeToDb(strOf(body.get("targetPortal"))));
        }
        if (body.containsKey("isActive")) {
            a.setIsPublished(boolOf(body.get("isActive"), a.getIsPublished()));
        }
        if (body.containsKey("startAt")) {
            a.setPublishedAt(parseDt(body.get("startAt")));
        }
        if (body.containsKey("endAt")) {
            a.setExpiresAt(parseDt(body.get("endAt")));
        }
        announcementRepository.save(a);
        log.info("Admin 更新公告 {}", id);
        return toAnnouncementDto(a);
    }

    @Transactional
    public void deleteAnnouncement(String id) {
        SystemAnnouncement a = announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到公告: " + id));
        announcementRepository.delete(a);
        log.info("Admin 刪除公告 {}", id);
    }

    private Long nextAnnSequence() {
        // 無法保證 seq_ANN 一定存在於所有環境，退而求其次：以現有最大號 +1
        long max = announcementRepository.findAll().stream()
                .map(a -> a.getAnnouncementId())
                .filter(s -> s != null && s.startsWith("ANN"))
                .map(s -> {
                    try {
                        return Long.parseLong(s.substring(3));
                    } catch (NumberFormatException e) {
                        return 0L;
                    }
                })
                .max((a, b) -> a.compareTo(b))
                .orElse(0L);
        return max + 1;
    }

    // ===================== 客訴 / 回饋 =====================

    private String submissionTypeLabel(UserSubmission s) {
        // FormType CONTACT/FEEDBACK → 前端 type（CONTACT / FEEDBACK）
        return s.getFormType() != null ? s.getFormType().name() : null;
    }

    private Map<String, Object> toSubmissionDto(UserSubmission s) {
        User u = s.getUser();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", s.getSubmissionId());
        m.put("type", submissionTypeLabel(s));
        m.put("subject", s.getFormType() != null ? s.getFormType().name() : "提交");
        m.put("fromName", u != null ? u.getName() : "—");
        m.put("fromEmail", u != null ? u.getEmail() : "—");
        m.put("status", s.getStatusCode() != null ? s.getStatusCode().name() : SubmissionStatus.UNREAD.name());
        m.put("content", s.getContent());
        m.put("createdAt", s.getCreatedAt());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSubmissions() {
        return submissionRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toSubmissionDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void updateSubmissionStatus(String id, String status, String handlerUserId) {
        UserSubmission s = submissionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到客訴: " + id));
        SubmissionStatus newStatus;
        try {
            newStatus = SubmissionStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException e) {
            throw new IllegalArgumentException("不支援的狀態: " + status);
        }
        s.setStatusCode(newStatus);
        if (newStatus != SubmissionStatus.UNREAD) {
            s.setHandledAt(LocalDateTime.now());
            if (handlerUserId != null) {
                s.setHandledBy(userRepository.findById(handlerUserId).orElse(null));
            }
        }
        submissionRepository.save(s);
        log.info("Admin 更新客訴狀態 {} → {}", id, status);
    }

    // ===================== 資料字典 =====================

    private Map<String, Object> toDictDto(SystemDictionary d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getDictId());
        m.put("dictType", d.getDictType());
        m.put("dictCode", d.getDictCode());
        m.put("dictLabel", d.getDictLabel());
        m.put("sortOrder", d.getSortOrder());
        m.put("isActive", d.getIsActive());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDictionaries() {
        return dictionaryRepository.findAll().stream()
                .map(this::toDictDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createDictionary(Map<String, Object> body) {
        String type = strOf(body.get("dictType"));
        String code = strOf(body.get("dictCode"));
        if (type == null || type.isBlank() || code == null || code.isBlank()) {
            throw new IllegalArgumentException("dict_type 與 dict_code 不可為空");
        }
        dictionaryRepository.findByDictTypeAndDictCode(type.trim(), code.trim())
                .ifPresent(x -> { throw new IllegalArgumentException("此 dict_type + dict_code 已存在"); });

        SystemDictionary d = SystemDictionary.builder()
                .dictType(type.trim())
                .dictCode(code.trim())
                .dictLabel(strOf(body.get("dictLabel")) != null ? strOf(body.get("dictLabel")) : code.trim())
                .sortOrder(intOf(body.get("sortOrder"), 0))
                .isActive(boolOf(body.get("isActive"), true))
                .build();
        dictionaryRepository.save(d);
        log.info("Admin 建立字典項 {}/{}", type, code);
        return toDictDto(d);
    }

    @Transactional
    public Map<String, Object> updateDictionary(Long id, Map<String, Object> body) {
        SystemDictionary d = dictionaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到字典項: " + id));
        if (body.containsKey("dictType") && strOf(body.get("dictType")) != null) {
            d.setDictType(strOf(body.get("dictType")).trim());
        }
        if (body.containsKey("dictCode") && strOf(body.get("dictCode")) != null) {
            d.setDictCode(strOf(body.get("dictCode")).trim());
        }
        if (body.containsKey("dictLabel") && strOf(body.get("dictLabel")) != null) {
            d.setDictLabel(strOf(body.get("dictLabel")).trim());
        }
        if (body.containsKey("sortOrder")) {
            d.setSortOrder(intOf(body.get("sortOrder"), d.getSortOrder()));
        }
        if (body.containsKey("isActive")) {
            d.setIsActive(boolOf(body.get("isActive"), d.getIsActive()));
        }
        dictionaryRepository.save(d);
        log.info("Admin 更新字典項 {}", id);
        return toDictDto(d);
    }

    @Transactional
    public void deleteDictionary(Long id) {
        SystemDictionary d = dictionaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到字典項: " + id));
        dictionaryRepository.delete(d);
        log.info("Admin 刪除字典項 {}", id);
    }

    // ===================== 系統設定 =====================

    /**
     * 前端依 group/type 分組顯示。後端 SystemConfig 沒有 group/type 欄位，
     * 這裡以 config_key 的點分前綴推導 group，並以值內容粗略推導 type。
     */
    private Map<String, Object> toConfigDto(SystemConfig c) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("key", c.getConfigKey());
        m.put("value", c.getConfigValue());
        m.put("group", deriveGroup(c.getConfigKey()));
        m.put("description", c.getDescription());
        m.put("type", deriveType(c.getConfigValue()));
        return m;
    }

    private String deriveGroup(String key) {
        if (key == null) return "一般設定";
        String prefix = key.contains(".") ? key.substring(0, key.indexOf('.'))
                : (key.contains("_") ? key.substring(0, key.indexOf('_')) : key);
        return prefix.isBlank() ? "一般設定" : prefix.toUpperCase();
    }

    private String deriveType(String value) {
        if (value == null) return "text";
        if ("true".equalsIgnoreCase(value) || "false".equalsIgnoreCase(value)) return "boolean";
        return value.matches("-?\\d+") ? "number" : "text";
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listConfigs() {
        return configRepository.findAll().stream()
                .map(this::toConfigDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> updateConfig(String key, String value) {
        SystemConfig c = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new IllegalArgumentException("找不到設定: " + key));
        c.setConfigValue(value != null ? value : "");
        configRepository.save(c);
        log.info("Admin 更新系統設定 {}", key);
        return toConfigDto(c);
    }

    // ===================== 媒體檔案 =====================

    private Map<String, Object> toMediaDto(MediaFile f) {
        String type = f.getFileType() == FileType.DOCUMENT ? "DOC" : "IMAGE";
        // 由 file_url 推導較精細的 PDF 類型供前端圖示
        String url = f.getFileUrl();
        if (url != null && url.toLowerCase().endsWith(".pdf")) {
            type = "PDF";
        }
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getFileId());
        m.put("fileName", deriveFileName(url));
        m.put("type", type);
        m.put("category", f.getRelatedTable());
        m.put("sizeKb", f.getFileSizeKb());
        m.put("uploadedBy", f.getUploader() != null ? f.getUploader().getUserId() : "—");
        m.put("url", url);
        m.put("createdAt", f.getCreatedAt());
        return m;
    }

    private String deriveFileName(String url) {
        if (url == null || url.isBlank()) return "(未命名)";
        int idx = url.lastIndexOf('/');
        return idx >= 0 && idx < url.length() - 1 ? url.substring(idx + 1) : url;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listMedia() {
        return mediaFileRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toMediaDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMedia(Long id) {
        MediaFile f = mediaFileRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到檔案: " + id));
        mediaFileRepository.delete(f);
        log.info("Admin 刪除媒體檔 {}", id);
    }

    // ===================== 排程任務 =====================

    private Map<String, Object> toJobDto(ScheduledJob j) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", j.getJobId());
        m.put("name", j.getDescription() != null ? j.getDescription() : j.getJobCode());
        m.put("cron", j.getJobCode()); // 實體無 cron 欄位，以 job_code 代填
        m.put("lastRunAt", j.getLastRunAt());
        m.put("nextRunAt", j.getNextRunAt());
        m.put("lastStatus", j.getStatusCode() != null ? j.getStatusCode().name() : JobStatus.IDLE.name());
        m.put("error", j.getErrorMsg());
        m.put("enabled", true); // 實體無 enabled 欄位，皆視為啟用
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listJobs() {
        return scheduledJobRepository.findAll().stream()
                .map(this::toJobDto)
                .collect(Collectors.toList());
    }

    /**
     * 手動觸發排程（STUB）：底層實際排程執行邏輯尚未存在，
     * 這裡僅更新 last_run_at = now 並將狀態標記為 SUCCESS、清除錯誤訊息後回 success。
     */
    @Transactional
    public Map<String, Object> triggerJob(String jobId) {
        ScheduledJob j = scheduledJobRepository.findById(jobId)
                .orElseThrow(() -> new IllegalArgumentException("找不到排程: " + jobId));
        j.setLastRunAt(LocalDateTime.now());
        j.setStatusCode(JobStatus.SUCCESS);
        j.setErrorMsg(null);
        scheduledJobRepository.save(j);
        log.info("Admin 手動觸發排程（stub）{}", jobId);
        return toJobDto(j);
    }

    // ===================== 小工具 =====================

    private String strOf(Object o) {
        return o != null ? o.toString() : null;
    }

    private boolean boolOf(Object o, boolean dflt) {
        if (o instanceof Boolean b) return b;
        if (o != null) return Boolean.parseBoolean(o.toString());
        return dflt;
    }

    private int intOf(Object o, int dflt) {
        if (o instanceof Number n) return n.intValue();
        if (o != null) {
            try {
                return Integer.parseInt(o.toString());
            } catch (NumberFormatException e) {
                return dflt;
            }
        }
        return dflt;
    }
}
