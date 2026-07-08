package tw.com.ispan.backend.theme.service;

import java.io.IOException;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.service.MediaFileRecorder;
import tw.com.ispan.backend.theme.dto.response.ThemeImageResource;

@Service
@RequiredArgsConstructor
public class ThemeImageStorageService {

    @Value("${app.documents.dir:./documents}")
    private String documentsDir;

    private final MediaFileRecorder mediaFileRecorder;

    /**
     * @param urlPrefix    對外提供的存取路徑前綴（例如 "/api/auctions/images/"），回傳值 = urlPrefix + 實際檔名
     * @param relatedTable media_file 的 related_table（例如 "auction"）
     * @param uploaderId   實際操作上傳的使用者 ID，可為 null
     * @return 圖片對外提供的完整存取 URL
     */
    public String storeImage(String folder, String baseFileName, MultipartFile file, String urlPrefix,
            String relatedTable, String relatedId, String uploaderId) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("請選擇要上傳的圖片檔案");
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.equals("image/jpeg") && !contentType.equals("image/png")
                && !contentType.equals("image/jpg"))) {
            throw new IllegalArgumentException("只支援上傳 JPG 或 PNG 格式的圖片");
        }
        if (file.getSize() > 2 * 1024 * 1024) { // 2MB
            throw new IllegalArgumentException("圖片大小不能超過 2MB");
        }

        try {
            Path uploadDir = Paths.get(documentsDir, "public", folder).toAbsolutePath().normalize();
            if (!Files.exists(uploadDir)) {
                Files.createDirectories(uploadDir);
            }

            String ext = contentType.contains("png") ? "png" : "jpg";
            String fileName = baseFileName + "." + ext;

            // 刪除可能存在的舊檔 (jpg/png)
            Files.deleteIfExists(uploadDir.resolve(baseFileName + ".jpg"));
            Files.deleteIfExists(uploadDir.resolve(baseFileName + ".png"));

            Path filePath = uploadDir.resolve(fileName);

            Path docsRoot = Paths.get(documentsDir).toAbsolutePath().normalize();
            if (!filePath.startsWith(docsRoot)) {
                throw new SecurityException("非法的檔案路徑");
            }

            Files.write(filePath, file.getBytes());

            String imageUrl = urlPrefix + fileName;
            mediaFileRecorder.record(uploaderId, relatedTable, relatedId, FileType.IMAGE, imageUrl, filePath);
            return imageUrl;
        } catch (IOException e) {
            throw new RuntimeException("圖片儲存失敗", e);
        }
    }

    public ThemeImageResource serveImage(String folder, String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("檔名不可為空");
        }

        try {
            Path uploadDir = Paths.get(documentsDir, "public", folder).toAbsolutePath().normalize();
            Path filePath = uploadDir.resolve(fileName).toAbsolutePath().normalize();

            if (!filePath.startsWith(uploadDir)) {
                throw new SecurityException("非法的存取路徑");
            }
            if (!Files.exists(filePath) || Files.isDirectory(filePath)) {
                throw new IllegalArgumentException("找不到圖片檔案");
            }

            String mimeType = URLConnection.guessContentTypeFromName(filePath.getFileName().toString());
            if (mimeType == null || !mimeType.startsWith("image/")) {
                throw new SecurityException("非法的檔案類型");
            }

            byte[] data = Files.readAllBytes(filePath);
            long lastModified = Files.getLastModifiedTime(filePath).toMillis();

            return new ThemeImageResource(data, mimeType, lastModified);
        } catch (IOException e) {
            throw new RuntimeException("讀取圖片失敗", e);
        }
    }
}
