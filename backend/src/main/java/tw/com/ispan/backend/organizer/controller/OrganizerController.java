package tw.com.ispan.backend.organizer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.dto.CreateOrganizerRequest;
import tw.com.ispan.backend.organizer.dto.MyOrganizationResponse;
import tw.com.ispan.backend.organizer.dto.SubmitKycRequest;
import tw.com.ispan.backend.organizer.dto.UpdateOrganizerProfileRequest;
import tw.com.ispan.backend.organizer.service.KycFileService;
import tw.com.ispan.backend.organizer.service.OrganizerMemberService;
import tw.com.ispan.backend.organizer.service.OrganizerService;
import tw.com.ispan.backend.organizer.service.OrganizerTransferService;

@Slf4j
@RestController
@RequestMapping("/api/organizer")
@RequiredArgsConstructor
public class OrganizerController {

    private final OrganizerService organizerService;
    private final OrganizerMemberService organizerMemberService;
    private final OrganizerTransferService organizerTransferService;
    private final KycFileService kycFileService;

    /**
     * 獲取當前登入者所屬的所有組織 (Owner + Member)
     */
    @GetMapping("/my-organizations")
    public ResponseEntity<?> getMyOrganizations(@AuthenticationPrincipal CustomUserDetails userDetails) {
        String userId = userDetails.getUserId();
        log.info("API 查詢所屬組織列表，操作者: {}", userId);
        try {
            List<MyOrganizationResponse> list = organizerService.getMyOrganizations(userId);
            return ResponseEntity.ok(list);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 建立新的組織 (草稿起步)
     */
    @PostMapping
    public ResponseEntity<?> createOrganization(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @Valid @RequestBody CreateOrganizerRequest request) {
        String userId = userDetails.getUserId();
        log.info("API 建立新組織，操作者: {}", userId);
        try {
            MyOrganizationResponse response = organizerService.createOrganization(userId, request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 檢查組織是否可以被註銷 (安全防線)
     */
    @GetMapping("/{organizerId}/deletion-check")
    public ResponseEntity<?> checkCanDelete(@PathVariable String organizerId) {
        log.info("API 檢查組織註銷安全性，組織ID: {}", organizerId);
        boolean canDelete = organizerService.checkCanDelete(organizerId);
        if (canDelete) {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "has_active_events", false,
                            "reason", "可以正常進行組織註銷"
                    )
            ));
        } else {
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", Map.of(
                            "has_active_events", true,
                            "reason", "偵測到本組織名下仍有「進行中」活動或「尚未結算」之款項，無法刪除。"
                    )
            ));
        }
    }

    /**
     * 註銷/封存組織
     */
    @DeleteMapping("/{organizerId}")
    public ResponseEntity<?> deleteOrganizer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 註銷組織，操作者: {}, 組織ID: {}", userId, organizerId);
        try {
            organizerService.deleteOrganizer(userId, organizerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "組織已成功封存註銷"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 危險區域一次性狀態：角色(OWNER/MEMBER)、是否可刪除＋阻擋原因、是否有待處理的所有權轉移。
     * 供前端折疊區展開時讀取，決定顯示「退出」或「轉移＋刪除」及鈕的禁用狀態。
     */
    @GetMapping("/{organizerId}/danger-zone")
    public ResponseEntity<?> getDangerZoneStatus(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 查詢組織危險區域狀態，組織ID: {}, 操作者: {}", organizerId, userId);
        try {
            Map<String, Object> status = organizerTransferService.getDangerZoneStatus(userId, organizerId);
            return ResponseEntity.ok(Map.of("status", "success", "data", status));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 發起組織所有權轉移 (限 OWNER；目標須為本組織成員)。建立待確認紀錄並寄出認證信。
     */
    @PostMapping("/{organizerId}/transfer-owner")
    public ResponseEntity<?> initiateTransfer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @RequestBody Map<String, String> body) {
        String userId = userDetails.getUserId();
        String targetUserId = body.get("targetUserId");
        log.info("API 發起所有權轉移，組織ID: {}, 操作者: {}, 目標: {}", organizerId, userId, targetUserId);
        try {
            organizerTransferService.initiateTransfer(userId, organizerId, targetUserId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "已寄出所有權轉移確認信，待對方接受後即完成轉移"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 取消待處理的組織所有權轉移 (限 OWNER)。
     */
    @PostMapping("/{organizerId}/transfer-owner/cancel")
    public ResponseEntity<?> cancelTransfer(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 取消所有權轉移，組織ID: {}, 操作者: {}", organizerId, userId);
        try {
            organizerTransferService.cancelTransfer(userId, organizerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "已取消所有權轉移"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 接受組織所有權轉移 (被轉移人信箱點擊 URL 啟用端點)。
     * 由瀏覽器新分頁開啟，回傳品牌化 HTML 結果頁（而非 JSON），避免 demo 時露出原始 JSON。
     */
    @GetMapping("/accept-transfer")
    public ResponseEntity<String> acceptTransfer(@RequestParam String token) {
        log.info("API 接受組織所有權轉移，驗證 Token: {}", token);
        try {
            organizerTransferService.acceptTransfer(token);
            return htmlResultPage(true, "所有權轉移成功",
                    "您已成功接手該組織的所有權，現在是組織的新擁有者。請重新登入系統以套用最新權限。");
        } catch (Exception e) {
            return htmlResultPage(false, "無法完成所有權轉移", e.getMessage());
        }
    }

    /**
     * 接受加入組織的邀請 (信箱點擊 URL 啟用端點)。
     * 同樣由瀏覽器新分頁開啟，回傳品牌化 HTML 結果頁。
     */
    @GetMapping("/accept-invite")
    public ResponseEntity<String> acceptInvite(@RequestParam String token) {
        log.info("API 接受加入組織邀請，驗證 Token: {}", token);
        try {
            organizerMemberService.acceptInvite(token);
            return htmlResultPage(true, "已成功加入組織",
                    "您已成功加入該主辦組織。請重新登入系統即可開始協作。");
        } catch (Exception e) {
            return htmlResultPage(false, "無法接受邀請", e.getMessage());
        }
    }

    /** 前端登入頁，供 email 連結結果頁的「前往登入」按鈕導向；
     *  正式站由環境變數 FRONTEND_URL 提供，本機預設 http://localhost:5173 */
    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * 產生品牌化的 HTML 結果頁（成功/失敗），供 email 連結在瀏覽器新分頁開啟時呈現，
     * 取代原始 JSON 回應。風格對齊 mail 樣板（橘色品牌、置中卡片）。
     */
    private ResponseEntity<String> htmlResultPage(boolean success, String title, String message) {
        String safeTitle = escapeHtml(title);
        String safeMessage = escapeHtml(message);
        String buttonText = success ? "前往登入" : "返回登入";

        String iconHtml = success ? """
            <svg class="animate-svg success" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 52 52">
                <circle class="checkmark-circle" cx="26" cy="26" r="25" fill="none"/>
                <path class="checkmark-check" fill="none" d="M14.1 27.2l7.1 7.2 16.7-16.8"/>
            </svg>
            """ : """
            <svg class="animate-svg error" xmlns="http://www.w3.org/2000/svg" viewBox="0 0 52 52">
                <circle class="checkmark-circle-error" cx="26" cy="26" r="25" fill="none"/>
                <path class="checkmark-x-1" fill="none" d="M16 16 36 36"/>
                <path class="checkmark-x-2" fill="none" d="M36 16 16 36"/>
            </svg>
            """;

        String html = """
            <!DOCTYPE html>
            <html lang="zh-Hant">
            <head>
                <meta charset="UTF-8">
                <meta name="viewport" content="width=device-width, initial-scale=1.0">
                <title>【TAP 票務平台】{{title}}</title>
                <link rel="preconnect" href="https://fonts.googleapis.com">
                <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
                <link href="https://fonts.googleapis.com/css2?family=Noto+Sans+TC:wght@400;500;700&family=Outfit:wght@400;600;800&display=swap" rel="stylesheet">
                <style>
                    :root {
                        --brand-primary: #e57346;
                        --brand-gradient: linear-gradient(135deg, #ff8c53 0%, #e05e26 100%);
                        --success-color: #10b981;
                        --error-color: #ef4444;
                        --bg-color: #f8fafc;
                        --text-main: #0f172a;
                        --text-muted: #64748b;
                    }

                    * {
                        box-sizing: border-box;
                        margin: 0;
                        padding: 0;
                    }

                    body {
                        font-family: 'Outfit', 'Noto Sans TC', -apple-system, BlinkMacSystemFont, sans-serif;
                        background-color: var(--bg-color);
                        background-image: radial-gradient(circle at top, #fffaf7 0%, #f1f5f9 100%);
                        min-height: 100vh;
                        display: flex;
                        align-items: center;
                        justify-content: center;
                        padding: 24px;
                    }

                    .container {
                        width: 100%;
                        max-width: 480px;
                        perspective: 1000px;
                    }

                    .card {
                        background: rgba(255, 255, 255, 0.9);
                        backdrop-filter: blur(12px);
                        -webkit-backdrop-filter: blur(12px);
                        border: 1px solid rgba(255, 255, 255, 0.7);
                        border-radius: 24px;
                        padding: 48px 32px;
                        text-align: center;
                        box-shadow: 0 20px 40px rgba(15, 23, 42, 0.06), 0 1px 3px rgba(15, 23, 42, 0.02);
                        animation: slideUp 0.6s cubic-bezier(0.16, 1, 0.3, 1) forwards;
                        opacity: 0;
                    }

                    .brand {
                        font-family: 'Outfit', 'Noto Sans TC', sans-serif;
                        font-size: 26px;
                        font-weight: 800;
                        background: var(--brand-gradient);
                        -webkit-background-clip: text;
                        -webkit-text-fill-color: transparent;
                        letter-spacing: -0.5px;
                        margin-bottom: 32px;
                        display: inline-block;
                    }

                    /* SVG Icon Animations */
                    .icon-wrapper {
                        margin-bottom: 24px;
                        display: flex;
                        justify-content: center;
                    }

                    .animate-svg {
                        width: 80px;
                        height: 80px;
                        border-radius: 50%;
                        display: block;
                        stroke-width: 4;
                        stroke: #fff;
                        stroke-miterlimit: 10;
                        box-shadow: inset 0px 0px 0px transparent;
                        margin: 0 auto;
                    }

                    .animate-svg.success {
                        animation: fillSuccess .4s ease-in-out .4s forwards, scaleUp .3s ease-in-out .9s both;
                    }

                    .animate-svg.error {
                        animation: fillError .4s ease-in-out .4s forwards, scaleUp .3s ease-in-out .9s both;
                    }

                    .checkmark-circle {
                        stroke-dasharray: 166;
                        stroke-dashoffset: 166;
                        stroke-width: 4;
                        stroke-miterlimit: 10;
                        stroke: var(--success-color);
                        fill: none;
                        animation: stroke .6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
                    }

                    .checkmark-circle-error {
                        stroke-dasharray: 166;
                        stroke-dashoffset: 166;
                        stroke-width: 4;
                        stroke-miterlimit: 10;
                        stroke: var(--error-color);
                        fill: none;
                        animation: stroke .6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
                    }

                    .checkmark-check {
                        transform-origin: 50% 50%;
                        stroke-dasharray: 48;
                        stroke-dashoffset: 48;
                        stroke: #fff;
                        animation: stroke .3s cubic-bezier(0.65, 0, 0.45, 1) .8s forwards;
                    }

                    .checkmark-x-1, .checkmark-x-2 {
                        transform-origin: 50% 50%;
                        stroke-dasharray: 48;
                        stroke-dashoffset: 48;
                        stroke: #fff;
                        animation: stroke .3s cubic-bezier(0.65, 0, 0.45, 1) .8s forwards;
                    }

                    .title {
                        font-size: 22px;
                        font-weight: 700;
                        color: var(--text-main);
                        margin-bottom: 12px;
                        letter-spacing: -0.2px;
                    }

                    .message {
                        font-size: 15px;
                        line-height: 1.7;
                        color: var(--text-muted);
                        margin-bottom: 36px;
                        padding: 0 8px;
                    }

                    .btn-action {
                        display: inline-block;
                        padding: 14px 40px;
                        font-size: 15px;
                        font-weight: 700;
                        color: #ffffff;
                        text-decoration: none;
                        background: var(--brand-gradient);
                        border-radius: 14px;
                        box-shadow: 0 8px 20px rgba(224, 94, 38, 0.2);
                        transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
                        border: none;
                        cursor: pointer;
                        outline: none;
                    }

                    .btn-action:hover {
                        transform: translateY(-2px);
                        box-shadow: 0 12px 24px rgba(224, 94, 38, 0.35);
                    }

                    .btn-action:active {
                        transform: translateY(1px);
                        box-shadow: 0 4px 10px rgba(224, 94, 38, 0.2);
                    }

                    .footer {
                        margin-top: 32px;
                        font-size: 12px;
                        color: #94a3b8;
                        letter-spacing: 0.2px;
                    }

                    @keyframes stroke {
                        100% {
                            stroke-dashoffset: 0;
                        }
                    }

                    @keyframes scaleUp {
                        0%, 100% {
                            transform: none;
                        }
                        50% {
                            transform: scale3d(1.1, 1.1, 1);
                        }
                    }

                    @keyframes fillSuccess {
                        100% {
                            box-shadow: inset 0px 0px 0px 40px var(--success-color);
                        }
                    }

                    @keyframes fillError {
                        100% {
                            box-shadow: inset 0px 0px 0px 40px var(--error-color);
                        }
                    }

                    @keyframes slideUp {
                        from {
                            opacity: 0;
                            transform: translateY(24px);
                        }
                        to {
                            opacity: 1;
                            transform: translateY(0);
                        }
                    }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="card">
                        <span class="brand">TAP 票務平台</span>

                        <div class="icon-wrapper">
                            {{icon}}
                        </div>

                        <h1 class="title">{{title}}</h1>
                        <p class="message">{{message}}</p>

                        <a href="{{frontendUrl}}/login" class="btn-action">{{buttonText}}</a>

                        <div class="footer">
                            <p>&copy; 2026 Ticket Access Platform. All rights reserved.</p>
                        </div>
                    </div>
                </div>
            </body>
            </html>
            """
            .replace("{{title}}", safeTitle)
            .replace("{{icon}}", iconHtml)
            .replace("{{message}}", safeMessage)
            .replace("{{frontendUrl}}", frontendUrl)
            .replace("{{buttonText}}", buttonText);

        return ResponseEntity.status(success ? HttpStatus.OK : HttpStatus.BAD_REQUEST)
                .contentType(new MediaType("text", "html", java.nio.charset.StandardCharsets.UTF_8))
                .body(html);
    }

    /** 最小化的 HTML 跳脫，避免後端訊息中的特殊字元破壞結果頁版面 */
    private String escapeHtml(String raw) {
        if (raw == null) {
            return "";
        }
        return raw.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    /**
     * 上傳 KYC 檔案 (限 OWNER 或 ADMIN 操作)
     */
    @PostMapping("/{organizerId}/kyc/upload")
    public ResponseEntity<?> uploadKycFile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @RequestParam("file") MultipartFile file,
            @RequestParam("fileType") String fileType) {
        String userId = userDetails.getUserId();
        log.info("API 收到上傳 KYC 檔案請求，組織: {}, 類型: {}, 操作者: {}", organizerId, fileType, userId);
        try {
            // 1. 權限防禦：檢查是否為 Owner 或 ADMIN
            organizerService.verifyOwnerOrAdmin(userId, organizerId);

            // 2. 儲存檔案
            String filePath = kycFileService.storeKycFile(organizerId, file, fileType, userId);

            // 3. 回傳儲存的相對路徑與成功訊息
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "filePath", filePath,
                    "message", "檔案上傳成功"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 提交 KYC 審核資料 (限 OWNER 或 ADMIN 操作)
     */
    @PostMapping("/{organizerId}/kyc/submit")
    public ResponseEntity<?> submitKyc(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @Valid @RequestBody SubmitKycRequest request) {
        String userId = userDetails.getUserId();
        log.info("API 收到提交 KYC 審核請求，組織: {}, 操作者: {}", organizerId, userId);
        try {
            organizerService.submitKyc(userId, organizerId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "已成功提交實名驗證申請！"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 更新組織 Logo URL (限 OWNER 或 ADMIN 操作)
     * 由前端「儲存基本資料」呼叫，將 Logo 寫入 kyc_data_json.logo_url 並刪除被取代的舊檔
     */
    @PutMapping("/{organizerId}/logo")
    public ResponseEntity<?> updateLogo(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @RequestBody Map<String, String> body) {
        String userId = userDetails.getUserId();
        log.info("API 更新組織 Logo，組織ID: {}, 操作者: {}", organizerId, userId);
        try {
            String logoUrl = body.get("logoUrl");
            organizerService.updateLogoUrl(userId, organizerId, logoUrl);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "logoUrl", logoUrl != null ? logoUrl : "",
                    "message", "Logo 已更新"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 儲存組織基本資料 (草稿存檔，不提交審核；限 OWNER 或 ADMIN 操作)
     */
    @PutMapping("/{organizerId}")
    public ResponseEntity<?> updateOrganizerProfile(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @Valid @RequestBody UpdateOrganizerProfileRequest request) {
        String userId = userDetails.getUserId();
        log.info("API 儲存組織基本資料，組織ID: {}, 操作者: {}", organizerId, userId);
        try {
            organizerService.updateOrganizerProfile(userId, organizerId, request);
            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "基本資料已儲存"
            ));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 獲取單一組織詳細資料 (限 OWNER 或 ADMIN 操作)
     */
    @GetMapping("/{organizerId}")
    public ResponseEntity<?> getOrganizerDetails(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 查詢單一組織詳細資料，組織ID: {}, 操作者: {}", organizerId, userId);
        try {
            Map<String, Object> details = organizerService.getOrganizerDetails(userId, organizerId);
            return ResponseEntity.ok(details);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(org.springframework.web.bind.MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidationExceptions(org.springframework.web.bind.MethodArgumentNotValidException ex) {
        String errorMessage = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage())
                .findFirst()
                .orElse("資料驗證失敗");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("success", false, "message", errorMessage));
    }
}
