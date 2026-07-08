package tw.com.ispan.backend.login.service;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class SystemDocumentServiceTest {

    @Autowired
    private SystemDocumentService documentService;

    @Test
    public void testReadDocument_Success() throws IOException {
        // 1. 驗證成功讀取已存在的隱私權政策，且內容不為空
        String content = documentService.readDocument("privacy.md");
        assertNotNull(content);
        assertTrue(content.contains("隱私權政策"));
    }

    @Test
    public void testReadDocument_InvalidFile() {
        // 2. 驗證讀取非系統管理的文件會拋出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            documentService.readDocument("invalid_file.txt");
        });
    }

    @Test
    public void testWriteDocument_PathTraversalDefense() {
        // 3. 驗證目錄穿越或非法檔名會被拒絕並拋出 IllegalArgumentException
        assertThrows(IllegalArgumentException.class, () -> {
            documentService.writeDocument("../malicious.txt", "hacked");
        });
    }
}
