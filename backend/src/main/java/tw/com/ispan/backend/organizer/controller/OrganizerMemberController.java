package tw.com.ispan.backend.organizer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.dto.InviteMemberRequest;
import tw.com.ispan.backend.organizer.dto.MemberResponse;
import tw.com.ispan.backend.organizer.dto.PermissionDto;
import tw.com.ispan.backend.organizer.service.OrganizerMemberService;
import tw.com.ispan.backend.organizer.service.RoleService;

@Slf4j
@RestController
@RequestMapping("/api/organizer/{organizerId}")
@RequiredArgsConstructor
public class OrganizerMemberController {

    private final OrganizerMemberService organizerMemberService;
    private final RoleService roleService;

    /**
     * 獲取組織自訂角色可用的「權限天花板」清單。
     * 集合 = 平台角色 'ORGANIZER' 所持有的 permission。
     * 驗證：登入 + Owner/Admin（由 RoleService.getOrganizerPermissionCeiling 內部
     * verifyOwnerOrAdmin 處理）。
     */
    @GetMapping("/permissions")
    public ResponseEntity<?> getOrganizerPermissions(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 查詢組織可用權限天花板，操作者: {}, 組織: {}", userId, organizerId);
        try {
            List<PermissionDto> permissions = roleService.getOrganizerPermissionCeiling(userId, organizerId);
            return ResponseEntity.ok(Map.of("data", permissions));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 獲取組織成員名單
     */
    @GetMapping("/members")
    public ResponseEntity<?> getMembers(@PathVariable String organizerId) {
        log.info("API 查詢組織成員列表，組織ID: {}", organizerId);
        try {
            List<MemberResponse> responses = organizerMemberService.getMembers(organizerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "data", responses));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 發送加入邀請
     */
    @PostMapping("/invite")
    public ResponseEntity<?> inviteMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @Valid @RequestBody InviteMemberRequest request) {
        String userId = userDetails.getUserId();
        log.info("API 邀請組織成員，操作者: {}, 組織ID: {}", userId, organizerId);
        try {
            organizerMemberService.inviteMember(userId, organizerId, request);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "邀請信發送成功"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 修改組織成員角色權限
     */
    @PutMapping("/members/{memberId}")
    public ResponseEntity<?> updateMemberRole(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @PathVariable String memberId,
            @RequestBody Map<String, String> body) {
        String executorId = userDetails.getUserId();
        String roleId = body.get("role_id");
        log.info("API 修改成員角色，操作者: {}, 組織ID: {}, 被操作成員: {}, 目標角色: {}", executorId, organizerId, memberId, roleId);

        if (roleId == null || roleId.trim().isEmpty()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "角色ID不能為空"));
        }

        try {
            organizerMemberService.updateMemberRole(executorId, organizerId, memberId, roleId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "成員角色已成功更新"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 剔除組織成員
     */
    @DeleteMapping("/members/{memberId}")
    public ResponseEntity<?> removeMember(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId,
            @PathVariable String memberId) {
        String executorId = userDetails.getUserId();
        log.info("API 剔除成員，操作者: {}, 組織ID: {}, 被移出成員: {}", executorId, organizerId, memberId);
        try {
            organizerMemberService.removeMember(executorId, organizerId, memberId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "成員已成功移出組織"));
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }

    /**
     * 成員自主退出組織
     */
    @PostMapping("/leave")
    public ResponseEntity<?> leaveOrganization(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 主動退出組織，操作者: {}, 組織ID: {}", userId, organizerId);
        try {
            organizerMemberService.leaveOrganization(userId, organizerId);
            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "message", "您已成功退出該組織"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
