package tw.com.ispan.backend.system.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.system.dto.UserSubmissionRequest;
import tw.com.ispan.backend.system.service.UserSubmissionService;

@Slf4j
@RestController
@RequestMapping("/api/submissions")
@RequiredArgsConstructor
public class UserSubmissionController {

    private final UserSubmissionService userSubmissionService;

    /**
     * 提交技術支援/聯絡表單
     */
    @PostMapping
    public ResponseEntity<?> submitContactForm(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserSubmissionRequest request) {
        
        log.info("收到技術支援表單提交請求");
        try {
            String userId = userDetails != null ? userDetails.getUserId() : null;
            userSubmissionService.submitContactForm(userId, request);
            return ResponseEntity.ok(Map.of("success", true, "message", "您的請求已成功送出！"));
        } catch (Exception ex) {
            log.error("處理技術支援表單提交時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統錯誤，請稍後再試"));
        }
    }
}
