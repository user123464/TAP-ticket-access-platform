package tw.com.ispan.backend.login.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import tw.com.ispan.backend.login.dto.LoginRequest;
import tw.com.ispan.backend.login.dto.RegisterRequest;
import tw.com.ispan.backend.login.repository.UserRepository;
import tw.com.ispan.backend.login.service.RedisService;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RedisService redisService;

    private String testEmail = "auth.test.controller@ispan.com";
    private String testPassword = "securePassword123";

    private void mockOtpVerified(String email) {
        redisService.setWithTtl("otp:verified:" + email, "true", 600);
    }

    @BeforeEach
    public void setup() {
        // 清除測試資料
        userRepository.findByEmail(testEmail).ifPresent(u -> userRepository.delete(u));
        redisService.delete("otp:verified:" + testEmail);
    }

    @Test
    public void testRegisterEndpointSuccess() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .name("Controller測試員")
                .build();

        mockOtpVerified(testEmail);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("註冊成功"));
    }

    @Test
    public void testRegisterEndpointInvalidEmailFails() throws Exception {
        RegisterRequest request = RegisterRequest.builder()
                .email("invalid-email-format")
                .password(testPassword)
                .name("錯誤測試")
                .build();

        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    public void testLoginAndLogoutEndpointsSuccess() throws Exception {
        // 1. 先註冊
        RegisterRequest regReq = RegisterRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .name("Controller測試員")
                .build();

        mockOtpVerified(testEmail);
        mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(regReq)))
                .andExpect(status().isCreated());

        // 2. 登入
        LoginRequest loginReq = LoginRequest.builder()
                .email(testEmail)
                .password(testPassword)
                .build();

        String responseBody = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(loginReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value(testEmail))
                .andExpect(jsonPath("$.name").value("Controller測試員"))
                .andReturn().getResponse().getContentAsString();

        // 提取 Token
        String token = objectMapper.readTree(responseBody).get("accessToken").asText();

        // 3. 登出
        mockMvc.perform(post("/api/auth/logout")
                .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("登出成功"));
    }
}
