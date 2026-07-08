package tw.com.ispan.backend.login.service;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class SystemDocumentService {

    private final ResourceLoader resourceLoader;

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    private static final List<String> MANAGED_FILES = Arrays.asList(
        "mail/email-template.html",
        "mail/login-2fa-template.html",
        "mail/invite-template.html",
        "mail/transfer-template.html",
        "mail/mail-config.properties",
        "privacy.md",
        "terms.md",
        "guide.md",
        "contract_free.md"
    );

    /**
     * 專案啟動時自動執行：檢查外部目錄與檔案，不存在則從 Classpath 複製預設檔案。
     */
    @PostConstruct
    public void init() {
        try {
            Path baseDir = Paths.get(documentsDir).toAbsolutePath().normalize();
            Path systemDir = baseDir.resolve("system");
            if (!Files.exists(systemDir)) {
                Files.createDirectories(systemDir);
                log.info("已建立外部文件管理資料夾: {}", systemDir);
            }

            for (String fileName : MANAGED_FILES) {
                initFile(systemDir, fileName, "classpath:templates/" + fileName);
            }
        } catch (IOException e) {
            log.error("初始化文件系統失敗", e);
        }
    }

    private void initFile(Path dirPath, String fileName, String classpathLocation) throws IOException {
        Path targetFile = dirPath.resolve(fileName);
        if (!Files.exists(targetFile)) {
            // 確保目標子資料夾 (例如 mail/) 存在
            Files.createDirectories(targetFile.getParent());
            
            Resource resource = resourceLoader.getResource(classpathLocation);
            if (resource.exists()) {
                try (InputStream is = resource.getInputStream()) {
                    Files.copy(is, targetFile, StandardCopyOption.REPLACE_EXISTING);
                    log.info("已初始化預設檔案至外部目錄: {}", targetFile);
                }
            } else {
                Files.createFile(targetFile);
                log.warn("找不到預設 ClassPath 檔案 {}，已建立空白檔案", classpathLocation);
            }
        }
    }

    /**
     * 讀取外部檔案的內容
     */
    public String readDocument(String fileName) throws IOException {
        if (!MANAGED_FILES.contains(fileName)) {
            throw new IllegalArgumentException("非系統管理的文件類型: " + fileName);
        }
        Path filePath = Paths.get(documentsDir).resolve("system").resolve(fileName).toAbsolutePath().normalize();
        if (!Files.exists(filePath)) {
            throw new IOException("找不到指定文件: " + fileName);
        }
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    /**
     * 更新寫入外部檔案的內容，並確保路徑安全防止路徑穿越攻擊
     */
    public void writeDocument(String fileName, String content) throws IOException {
        if (!MANAGED_FILES.contains(fileName)) {
            throw new IllegalArgumentException("非系統管理的文件類型: " + fileName);
        }
        Path parentPath = Paths.get(documentsDir).resolve("system").toAbsolutePath().normalize();
        Path filePath = parentPath.resolve(fileName).toAbsolutePath().normalize();

        // 確保寫入的絕對路徑是在 documents/system 目錄的樹狀層級之下，不可寫入外部如 C:/windows (安全防禦)
        if (!filePath.startsWith(parentPath)) {
            throw new SecurityException("非法的文件存取路徑");
        }
        Files.writeString(filePath, content, StandardCharsets.UTF_8);
        log.info("外部檔案已更新: {}", filePath);
    }
}
