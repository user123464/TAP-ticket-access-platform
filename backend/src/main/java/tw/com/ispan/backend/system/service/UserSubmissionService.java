package tw.com.ispan.backend.system.service;

import java.util.Map;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.system.dto.UserSubmissionRequest;
import tw.com.ispan.backend.system.entity.UserSubmission;
import tw.com.ispan.backend.system.enums.FormType;
import tw.com.ispan.backend.system.enums.SubmissionStatus;
import tw.com.ispan.backend.system.repository.UserSubmissionRepository;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserSubmissionService {

    private final UserSubmissionRepository userSubmissionRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    /**
     * 儲存使用者提交的支援請求
     */
    @Transactional
    public void submitContactForm(String userId, UserSubmissionRequest request) {
        log.info("處理聯絡技術支援提交表單. 姓名: {}, 信箱: {}, 登入者ID: {}", request.getName(), request.getEmail(), userId);

        User user = null;
        if (userId != null) {
            user = userRepository.findById(userId).orElse(null);
        }

        // 1. 從序列 seq_USM 獲取下一個 ID，格式化為 USMXXXXXXX
        Long nextVal = userSubmissionRepository.getNextSubmissionSequenceValue();
        String submissionId = String.format("USM%07d", nextVal);

        // 2. 將表單詳細資料序列化為 JSON 儲存於 content
        Map<String, String> details = Map.of(
            "name", request.getName().trim(),
            "email", request.getEmail().trim(),
            "company", request.getCompany() != null ? request.getCompany().trim() : "",
            "category", request.getCategory().trim(),
            "description", request.getDescription().trim()
        );

        String contentJson;
        try {
            contentJson = objectMapper.writeValueAsString(details);
        } catch (Exception e) {
            log.error("序列化技術支援表單時發生錯誤", e);
            throw new RuntimeException("提交技術支援表單失敗，資料處理異常");
        }

        // 3. 建立並儲存實體
        UserSubmission submission = UserSubmission.builder()
                .submissionId(submissionId)
                .user(user)
                .formType(FormType.CONTACT) // 0
                .content(contentJson)
                .statusCode(SubmissionStatus.UNREAD) // 0
                .createdAt(LocalDateTime.now())
                .build();

        userSubmissionRepository.save(submission);
        log.info("技術支援表單已成功儲存. ID: {}", submissionId);
    }
}
