package tw.com.ispan.backend.system.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.io.IOException;

import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.system.entity.MediaFile;
import tw.com.ispan.backend.system.enums.FileType;
import tw.com.ispan.backend.system.repository.MediaFileRepository;

/**
 * 統一補寫 media_file 紀錄的共用服務。
 * 各上傳端點（頭像/KYC/主辦方Logo/主題/拍賣/合約...）存檔成功後呼叫 record()，
 * 讓 Admin 媒體庫頁面能撈到所有上傳過的檔案。失敗時只記 log，不影響原上傳流程。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MediaFileRecorder {

    private final MediaFileRepository mediaFileRepository;
    private final UserRepository userRepository;

    public void record(String uploaderId, String relatedTable, String relatedId,
            FileType fileType, String fileUrl, Path storedFilePath) {
        try {
            Integer sizeKb = null;
            if (storedFilePath != null && Files.exists(storedFilePath)) {
                sizeKb = (int) Math.ceil(Files.size(storedFilePath) / 1024.0);
            }

            MediaFile.MediaFileBuilder builder = MediaFile.builder()
                    .relatedTable(relatedTable)
                    .relatedId(relatedId)
                    .fileType(fileType)
                    .fileUrl(fileUrl)
                    .fileSizeKb(sizeKb);

            if (uploaderId != null) {
                userRepository.findById(uploaderId).ifPresent(builder::uploader);
            }

            mediaFileRepository.save(builder.build());
        } catch (IOException | RuntimeException e) {
            log.warn("media_file 補寫失敗（不影響原上傳流程），relatedTable={}, relatedId={}, fileUrl={}",
                    relatedTable, relatedId, fileUrl, e);
        }
    }
}
