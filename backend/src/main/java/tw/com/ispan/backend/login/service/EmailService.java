package tw.com.ispan.backend.login.service;

import java.util.Properties;

import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;
    private final SystemDocumentService documentService;

    /**
     * 動態從外部載入全域發信人設定檔，若讀取失敗則使用內建安全預設值 (防呆防護)
     */
    private String[] getMailConfig() {
        String fromEmail = "no-reply@tap.com";
        String fromName = "TAP 票務平台";
        try {
            String content = documentService.readDocument("mail/mail-config.properties");
            Properties props = new Properties();
            try (java.io.StringReader reader = new java.io.StringReader(content)) {
                props.load(reader);
                fromEmail = props.getProperty("app.mail.from-email", fromEmail).trim();
                fromName = props.getProperty("app.mail.from-name", fromName).trim();
            }
        } catch (Exception e) {
            log.warn("無法載入外部 mail/mail-config.properties 配置，將使用系統預設發信人資訊", e);
        }
        return new String[] { fromEmail, fromName };
    }

    /**
     * 寄送 HTML 格式的驗證碼郵件
     */
    public void sendOtpEmail(String toEmail, String otpCode) {
        log.info("開始寄送驗證信至 Email: {}, Code: {}", toEmail, otpCode);
        try {
            // 從外部文件系統動態讀取最新版本的 email 樣板
            String rawTemplate = documentService.readDocument("mail/email-template.html");

            // 替換驗證碼預留欄位
            String htmlContent = rawTemplate.replace("{{otpCode}}", otpCode);

            // 獲取全域發信人資訊
            String[] mailConfig = getMailConfig();
            String configFromEmail = mailConfig[0];
            String configFromName = mailConfig[1];

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(configFromEmail, configFromName, "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject("【TAP 票務平台】一次性安全驗證碼");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("驗證信寄送成功，Email: {}", toEmail);

        } catch (Exception e) {
            log.error("驗證信寄送失敗，Email: {}", toEmail, e);
            throw new RuntimeException("郵件寄送系統異常，請稍後再試", e);
        }
    }

    /**
     * 寄送登入雙重驗證 (2FA) 驗證碼郵件，使用可編輯的 login-2fa-template.html 樣板
     */
    public void send2faEmail(String toEmail, String otpCode) {
        log.info("開始寄送 2FA 驗證信至 Email: {}", toEmail);
        try {
            String rawTemplate = documentService.readDocument("mail/login-2fa-template.html");
            String htmlContent = rawTemplate.replace("{{otpCode}}", otpCode);

            String[] mailConfig = getMailConfig();
            String configFromEmail = mailConfig[0];
            String configFromName = mailConfig[1];

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(configFromEmail, configFromName, "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject("【TAP 票務平台】登入安全驗證碼");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("2FA 驗證信寄送成功，Email: {}", toEmail);

        } catch (Exception e) {
            log.error("2FA 驗證信寄送失敗，Email: {}", toEmail, e);
            throw new RuntimeException("郵件寄送系統異常，請稍後再試", e);
        }
    }

    /**
     * 寄送組織所有權「轉移認證」信。被轉移人點擊信中連結即接受轉移、成為新 owner。
     *
     * @param toEmail   被轉移人信箱
     * @param orgName   組織名稱
     * @param fromName  發起轉移的現任 owner 名稱
     * @param acceptUrl 接受轉移的認證連結
     */
    public void sendTransferEmail(String toEmail, String orgName, String fromName, String acceptUrl) {
        log.info("開始寄送組織所有權轉移認證信至 Email: {}, 組織: {}", toEmail, orgName);
        try {
            // 從外部文件系統動態讀取最新版本的 transfer 樣板
            String rawTemplate = documentService.readDocument("mail/transfer-template.html");

            // 替換佔位符
            String htmlContent = rawTemplate
                    .replace("{{orgName}}", orgName)
                    .replace("{{fromName}}", fromName != null ? fromName : "")
                    .replace("{{acceptUrl}}", acceptUrl);

            // 獲取全域發信人資訊
            String[] mailConfig = getMailConfig();
            String configFromEmail = mailConfig[0];
            String configFromName = mailConfig[1];

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(configFromEmail, configFromName, "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject("【TAP 票務平台】組織所有權轉移確認信");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("組織所有權轉移認證信寄送成功，Email: {}", toEmail);

        } catch (Exception e) {
            log.error("組織所有權轉移認證信寄送失敗，Email: {}", toEmail, e);
            throw new RuntimeException("郵件寄送系統異常，請稍後再試", e);
        }
    }

    /**
     * 寄送組織加入邀請信
     */
    public void sendInviteEmail(String toEmail, String orgName, String inviteUrl) {
        log.info("開始寄送組織邀請信至 Email: {}, 組織: {}", toEmail, orgName);
        try {
            // 從外部文件系統動態讀取最新版本的 invite 樣板
            String rawTemplate = documentService.readDocument("mail/invite-template.html");

            // 替換佔位符
            String htmlContent = rawTemplate
                    .replace("{{orgName}}", orgName)
                    .replace("{{inviteUrl}}", inviteUrl);

            // 獲取全域發信人資訊
            String[] mailConfig = getMailConfig();
            String configFromEmail = mailConfig[0];
            String configFromName = mailConfig[1];

            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(new InternetAddress(configFromEmail, configFromName, "UTF-8"));
            helper.setTo(toEmail);
            helper.setSubject("【TAP 票務平台】合作夥伴組織加入邀請信");
            helper.setText(htmlContent, true);

            mailSender.send(message);
            log.info("組織邀請信寄送成功，Email: {}", toEmail);

        } catch (Exception e) {
            log.error("組織邀請信寄送失敗，Email: {}", toEmail, e);
            throw new RuntimeException("郵件寄送系統異常，請稍後再試", e);
        }
    }
}
