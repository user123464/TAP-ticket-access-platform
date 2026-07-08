package tw.com.ispan.backend.organizer.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.service.MediaFileRecorder;

@Slf4j
@Service
@RequiredArgsConstructor
public class KycFileService {

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    private final MediaFileRecorder mediaFileRecorder;

    private static final long MAX_FILE_SIZE = 15 * 1024 * 1024; // 15MB

    private static final List<String> ALLOWED_DOC_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final List<String> ALLOWED_DOC_CONTENT_TYPES = Arrays.asList("application/pdf", "image/jpeg", "image/png");

    private static final List<String> ALLOWED_ID_EXTENSIONS = Arrays.asList("pdf", "jpg", "jpeg", "png");
    private static final List<String> ALLOWED_ID_CONTENT_TYPES = Arrays.asList("application/pdf", "image/jpeg", "image/png");

    /**
     * 儲存上傳的 KYC 檔案並進行安全檢查
     * 
     * @param organizerId 組織ID
     * @param file        上傳的檔案
     * @param fileType    檔案類型 ("DOC" 代表公司登記文件, "ID" 代表負責人身分證)
     * @param uploaderId  實際操作上傳的使用者 ID（用於 media_file 紀錄）
     * @return 儲存後的相對路徑 (例如: kyc/ORG0000002_DOC_1718600000.pdf)
     */
    public String storeKycFile(String organizerId, MultipartFile file, String fileType, String uploaderId) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上傳的檔案不能為空");
        }

        // 1. 驗證檔案大小
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("檔案大小不能超過 15MB");
        }

        // 2. 獲取原副檔名並轉為小寫
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.contains(".")) {
            throw new IllegalArgumentException("非法的檔案名稱或缺少副檔名");
        }
        String extension = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();

        // 3. 驗證檔案副檔名與 Content-Type
        String contentType = file.getContentType();
        if (contentType == null) {
            throw new IllegalArgumentException("無法識別的檔案類型 (ContentType 為空)");
        }

        if ("DOC".equalsIgnoreCase(fileType)) {
            if (!ALLOWED_DOC_EXTENSIONS.contains(extension) || !ALLOWED_DOC_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("營利事業登記證只支援 PDF, JPG, PNG 格式");
            }
        } else if ("ID".equalsIgnoreCase(fileType)) {
            if (!ALLOWED_ID_EXTENSIONS.contains(extension) || !ALLOWED_ID_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("負責人身分證影本只支援 PDF, JPG, PNG 格式");
            }
        } else if ("LOGO".equalsIgnoreCase(fileType)) {
            if (!ALLOWED_ID_EXTENSIONS.contains(extension) || !ALLOWED_ID_CONTENT_TYPES.contains(contentType)) {
                throw new IllegalArgumentException("組織大頭貼只支援 JPG, PNG 格式");
            }
        } else {
            throw new IllegalArgumentException("不支援的 KYC 檔案類型: " + fileType);
        }

        // 4. 定義基礎目標資料夾與相對路徑前綴 (安全權限隔離分流)
        // 如果未來需要讓主辦方能「點擊下載/預覽」確認自己上傳的內容，就必須像前面建議的，新增一個有權限驗證的下載 API 來回傳檔案串流。
        Path targetDir;
        String relativePathPrefix;
        if ("DOC".equalsIgnoreCase(fileType)) {
            targetDir = Paths.get(documentsDir, "secure", "kyc", "org-docs").toAbsolutePath().normalize();
            relativePathPrefix = "secure/kyc/org-docs/";
        } else if ("ID".equalsIgnoreCase(fileType)) {
            targetDir = Paths.get(documentsDir, "secure", "kyc", "owner-ids").toAbsolutePath().normalize();
            relativePathPrefix = "secure/kyc/owner-ids/";
        } else if ("LOGO".equalsIgnoreCase(fileType)) {
            targetDir = Paths.get(documentsDir, "public", "org-logos").toAbsolutePath().normalize();
            relativePathPrefix = "public/org-logos/";
        } else {
            targetDir = Paths.get(documentsDir, "secure", "kyc").toAbsolutePath().normalize();
            relativePathPrefix = "secure/kyc/";
        }
        
        // 5. 生成安全的唯一檔名，防止覆蓋他人檔案，也抹除原檔名的路徑穿越隱憂
        String safeFileName = String.format("%s_%s_%d.%s", 
                organizerId, 
                fileType.toUpperCase(), 
                System.currentTimeMillis(), 
                extension);

        Path targetFilePath = targetDir.resolve(safeFileName).toAbsolutePath().normalize();

        // 6. 鋼鐵防禦：防範路徑穿越 (Directory Traversal)
        Path docsRoot = Paths.get(documentsDir).toAbsolutePath().normalize();
        if (!targetFilePath.startsWith(docsRoot)) {
            throw new SecurityException("偵測到非法的路徑穿越嘗試！");
        }

        // 7. 建立目錄 (若不存在)
        if (!Files.exists(targetDir)) {
            Files.createDirectories(targetDir);
            log.info("已建立 KYC 儲存目錄: {}", targetDir);
        }

        // 8. 儲存檔案
        Files.copy(file.getInputStream(), targetFilePath, StandardCopyOption.REPLACE_EXISTING);
        log.info("成功儲存 KYC 檔案，組織: {}, 類型: {}, 路徑: {}", organizerId, fileType, targetFilePath);

        // 回傳對應 documentsDir 的相對路徑，便於後續載入
        String relativePath = relativePathPrefix + safeFileName;
        FileType mediaFileType = "LOGO".equalsIgnoreCase(fileType) ? FileType.IMAGE : FileType.DOCUMENT;
        mediaFileRecorder.record(uploaderId, "organizer", organizerId, mediaFileType, relativePath, targetFilePath);
        return relativePath;
    }
}
