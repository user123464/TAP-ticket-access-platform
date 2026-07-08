package tw.com.ispan.backend.login.controller;

import java.io.IOException;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.service.SystemDocumentService;

@Slf4j
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
public class SystemDocumentController {

    private final SystemDocumentService documentService;

    /**
     * 讀取特定系統文件 (免驗證，前台可直接存取)
     * code 可以是: privacy.md, terms.md, guide.md, email-template.html
     */
    @GetMapping("/{code}")
    public ResponseEntity<?> getDocument(@PathVariable String code) {
        log.info("接收到讀取文件請求: {}", code);
        try {
            String content = documentService.readDocument(code);
            return ResponseEntity.ok(Map.of("success", true, "code", code, "content", content));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("success", false, "message", "文件不存在: " + code));
        }
    }

    /**
     * 編輯更新特定系統文件 (需 TEMPLATE_MANAGE 權限)。
     *
     * 權限碼選用 TEMPLATE_MANAGE（NOTIFICATION/MANAGE，「通知範本管理」，ADMIN+SUPER_ADMIN）：
     * 本端點管理的文件含 email-template.html（即通知範本），故沿用最貼近的現有權限碼，
     * 不另立新碼。注意 privacy/terms/guide 屬政策文件，語意上略寬；
     * 未來若要更精準可新增專屬 DOCUMENT_MANAGE 碼（屬延後待辦）。
     * GET 仍為公開讀取（見 SecurityConfig），僅寫入收斂為此權限。
     */
    @PreAuthorize("hasAuthority('TEMPLATE_MANAGE')")
    @PutMapping("/{code}")
    public ResponseEntity<?> updateDocument(@PathVariable String code, @RequestBody Map<String, String> body) {
        String content = body.get("content");
        if (content == null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", "內容不能為空"));
        }
        log.info("接收到更新文件請求: {}", code);
        try {
            documentService.writeDocument(code, content);
            return ResponseEntity.ok(Map.of("success", true, "message", "文件更新成功"));
        } catch (IllegalArgumentException | SecurityException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("success", false, "message", e.getMessage()));
        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("success", false, "message", "更新文件時發生錯誤"));
        }
    }
}
