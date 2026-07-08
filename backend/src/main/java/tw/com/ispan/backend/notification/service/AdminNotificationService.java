package tw.com.ispan.backend.notification.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.notification.entity.NotificationLog;
import tw.com.ispan.backend.notification.entity.NotificationTemplate;
import tw.com.ispan.backend.notification.enums.NotificationChannel;
import tw.com.ispan.backend.notification.enums.NotificationStatus;
import tw.com.ispan.backend.notification.repository.NotificationLogRepository;
import tw.com.ispan.backend.notification.repository.NotificationTemplateRepository;

/**
 * Admin 後台「通知模板 / 發送紀錄」業務邏輯（批次 3）。
 *
 * <p>重用 notification package 的 NotificationTemplate / NotificationLog。
 * channel 枚舉 EMAIL(0)/SMS(1) 與前端字串對齊；status PENDING/SENT/FAILED 同名對齊。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminNotificationService {

    private final NotificationTemplateRepository templateRepository;
    private final NotificationLogRepository logRepository;

    // ===================== 模板 =====================

    private Map<String, Object> toTemplateDto(NotificationTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getTemplateId());
        m.put("code", t.getTemplateCode());
        m.put("name", t.getSubject() != null && !t.getSubject().isBlank() ? t.getSubject() : t.getTemplateCode());
        m.put("channel", t.getChannel() != null ? t.getChannel().name() : NotificationChannel.EMAIL.name());
        m.put("subject", t.getSubject());
        m.put("body", t.getBodyTemplate());
        m.put("isActive", t.getIsActive());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTemplates() {
        return templateRepository.findAll().stream()
                .map(this::toTemplateDto)
                .collect(Collectors.toList());
    }

    private NotificationChannel channelOf(Object o) {
        if (o == null) {
            return NotificationChannel.EMAIL;
        }
        try {
            return NotificationChannel.valueOf(o.toString());
        } catch (IllegalArgumentException e) {
            return NotificationChannel.EMAIL;
        }
    }

    @Transactional
    public Map<String, Object> createTemplate(Map<String, Object> body) {
        String code = strOf(body.get("code"));
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("模板代碼不可為空");
        }
        templateRepository.findByTemplateCode(code.trim())
                .ifPresent(x -> { throw new IllegalArgumentException("模板代碼已存在: " + code); });

        // template_id 為 30 字內字串主鍵（無序列）：以 TMPL_ + 時戳避免衝突
        String id = "TMPL_" + Long.toString(System.currentTimeMillis(), 36).toUpperCase();

        NotificationTemplate t = NotificationTemplate.builder()
                .templateId(id)
                .templateCode(code.trim())
                .channel(channelOf(body.get("channel")))
                .subject(strOf(body.get("subject")))
                .bodyTemplate(strOf(body.get("body")) != null ? strOf(body.get("body")) : "")
                .isActive(boolOf(body.get("isActive"), true))
                .build();
        templateRepository.save(t);
        log.info("Admin 建立通知模板 {}", id);
        return toTemplateDto(t);
    }

    @Transactional
    public Map<String, Object> updateTemplate(String id, Map<String, Object> body) {
        NotificationTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到模板: " + id));
        if (body.containsKey("code") && strOf(body.get("code")) != null) {
            t.setTemplateCode(strOf(body.get("code")).trim());
        }
        if (body.containsKey("channel")) {
            t.setChannel(channelOf(body.get("channel")));
        }
        if (body.containsKey("subject")) {
            t.setSubject(strOf(body.get("subject")));
        }
        if (body.containsKey("body")) {
            t.setBodyTemplate(strOf(body.get("body")) != null ? strOf(body.get("body")) : "");
        }
        if (body.containsKey("isActive")) {
            t.setIsActive(boolOf(body.get("isActive"), t.getIsActive()));
        }
        templateRepository.save(t);
        log.info("Admin 更新通知模板 {}", id);
        return toTemplateDto(t);
    }

    @Transactional
    public void deleteTemplate(String id) {
        NotificationTemplate t = templateRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("找不到模板: " + id));
        templateRepository.delete(t);
        log.info("Admin 刪除通知模板 {}", id);
    }

    // ===================== 發送紀錄 =====================

    private Map<String, Object> toLogDto(NotificationLog l) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", l.getLogId());
        m.put("templateCode", l.getTemplate() != null ? l.getTemplate().getTemplateCode() : "—");
        m.put("channel", l.getChannel() != null ? l.getChannel().name() : NotificationChannel.EMAIL.name());
        m.put("recipient", l.getRecipient());
        m.put("status", l.getStatusCode() != null ? l.getStatusCode().name() : NotificationStatus.PENDING.name());
        m.put("error", l.getErrorMsg());
        m.put("createdAt", l.getCreatedAt());
        return m;
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listLogs() {
        return logRepository.findAll().stream()
                .sorted((a, b) -> {
                    if (a.getCreatedAt() == null) return 1;
                    if (b.getCreatedAt() == null) return -1;
                    return b.getCreatedAt().compareTo(a.getCreatedAt());
                })
                .map(this::toLogDto)
                .collect(Collectors.toList());
    }

    /**
     * 重送（STUB）：底層實際寄送 gateway 尚未串接，
     * 這裡將紀錄狀態重置為 PENDING、清除錯誤訊息、清空 sent_at 後回 success。
     */
    @Transactional
    public Map<String, Object> resendLog(Long logId) {
        NotificationLog l = logRepository.findById(logId)
                .orElseThrow(() -> new IllegalArgumentException("找不到發送紀錄: " + logId));
        l.setStatusCode(NotificationStatus.PENDING);
        l.setErrorMsg(null);
        l.setSentAt(null);
        logRepository.save(l);
        log.info("Admin 重送通知（stub）{}", logId);
        return toLogDto(l);
    }

    // ===================== 小工具 =====================

    private String strOf(Object o) {
        return o != null ? o.toString() : null;
    }

    private boolean boolOf(Object o, boolean dflt) {
        if (o instanceof Boolean b) {
            return b;
        }
        if (o != null) {
            return Boolean.parseBoolean(o.toString());
        }
        return dflt;
    }
}
