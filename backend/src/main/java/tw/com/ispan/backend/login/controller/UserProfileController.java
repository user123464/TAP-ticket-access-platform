package tw.com.ispan.backend.login.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.login.dto.DeletionEligibilityResponse;
import tw.com.ispan.backend.login.dto.UserProfileRequest;
import tw.com.ispan.backend.login.dto.UserProfileResponse;
import tw.com.ispan.backend.login.service.UserProfileService;

@Slf4j
@RestController
@RequestMapping("/api/user/profile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;

    /**
     * 讀取登入使用者的個人資料
     */
    // 自助端點：只操作呼叫者自己的資料，登入即可，不另綁 PROFILE_VIEW 權限
    @GetMapping
    public ResponseEntity<?> getUserProfile(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("未授權的個人資料讀取請求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "認證失敗，請重新登入"));
        }

        try {
            UserProfileResponse response = userProfileService.getUserProfile(userDetails.getUserId());
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("讀取資料失敗，使用者不存在: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("讀取個人資料時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統錯誤，請稍後再試"));
        }
    }

    /**
     * 更新登入使用者的個人資料
     */
    // 自助端點：只更新呼叫者自己的資料，登入即可
    @PutMapping
    public ResponseEntity<?> updateUserProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody UserProfileRequest profileRequest) {
            
        if (userDetails == null) {
            log.warn("未授權的個人資料更新請求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "認證失敗，請重新登入"));
        }

        try {
            UserProfileResponse response = userProfileService.updateUserProfile(userDetails.getUserId(), profileRequest);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException ex) {
            log.warn("更新資料失敗，使用者不存在: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("更新個人資料時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統錯誤，請稍後再試"));
        }
    }

    /**
     * 查詢目前登入使用者是否可以刪除帳號（供前端展開刪除卡片時預先檢查）。
     */
    // 自助端點：只查呼叫者自己的資格，登入即可
    @GetMapping("/deletion-eligibility")
    public ResponseEntity<?> getDeletionEligibility(@AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            log.warn("未授權的刪除資格查詢請求");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("success", false, "message", "認證失敗，請重新登入"));
        }

        try {
            DeletionEligibilityResponse eligibility = userProfileService.getDeletionEligibility(userDetails.getUserId());
            return ResponseEntity.ok(Map.of("success", true, "message", "查詢成功", "data", eligibility));
        } catch (IllegalArgumentException ex) {
            log.warn("查詢刪除資格失敗，使用者不存在: {}", userDetails.getUserId());
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", ex.getMessage()));
        } catch (Exception ex) {
            log.error("查詢刪除資格時發生系統錯誤", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "系統錯誤，請稍後再試"));
        }
    }

}
