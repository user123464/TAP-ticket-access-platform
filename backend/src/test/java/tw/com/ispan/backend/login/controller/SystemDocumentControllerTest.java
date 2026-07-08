package tw.com.ispan.backend.login.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class SystemDocumentControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    public void testGetDocument_PublicAccess() throws Exception {
        // 1. 驗證不需要 Token 即可存取 GET API
        mockMvc.perform(get("/api/documents/privacy.md"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.content").exists());
    }

    @Test
    public void testGetDocument_InvalidFile() throws Exception {
        // 2. 驗證請求非法檔案會回傳 400 Bad Request
        mockMvc.perform(get("/api/documents/hack.sh"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    public void testUpdateDocument_Unauthorized() throws Exception {
        // 3. 驗證未帶認證 Token 進行 PUT 修改時，必須被 Spring Security 攔截並回傳 403 Forbidden
        mockMvc.perform(put("/api/documents/privacy.md")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"# New Content\"}"))
                .andExpect(status().isForbidden());
    }
}
