package tw.com.ispan.backend.organizer.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.organizer.dto.ContractResponse;
import tw.com.ispan.backend.organizer.service.ContractService;
import tw.com.ispan.backend.organizer.service.OrganizerService;

@Slf4j
@RestController
@RequestMapping("/api/organizer")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final OrganizerService organizerService;

    /**
     * 查詢組織的合約清單（限 OWNER 或 ADMIN 操作）
     */
    @GetMapping("/{organizerId}/contracts")
    public ResponseEntity<?> getContracts(
            @AuthenticationPrincipal CustomUserDetails userDetails,
            @PathVariable String organizerId) {
        String userId = userDetails.getUserId();
        log.info("API 查詢組織合約清單，組織ID: {}, 操作者: {}", organizerId, userId);
        try {
            // 權限防禦：必須是 OWNER 或 ADMIN
            organizerService.verifyOwnerOrAdmin(userId, organizerId);

            List<ContractResponse> list = contractService.getContractsByOrganizer(organizerId);
            return ResponseEntity.ok(list);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        }
    }
}
