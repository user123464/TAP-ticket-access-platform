package tw.com.ispan.backend.config.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;

import jakarta.servlet.ServletException;
import tw.com.ispan.backend.login.service.RedisService;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private CustomUserDetailsService customUserDetailsService;

    @Mock
    private RedisService redisService;

    @InjectMocks
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockFilterChain filterChain;

    @BeforeEach
    public void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        filterChain = new MockFilterChain();
        SecurityContextHolder.clearContext(); // 確保每個測試開始前安全上下文是乾淨的
    }

    @AfterEach
    public void tearDown() {
        SecurityContextHolder.clearContext(); // 測試結束後清理
    }

    @Test
    public void testFilter_ValidToken_Success() throws ServletException, IOException {
        String token = "valid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        // 模擬：Token 驗證通過
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("USR12345");
        when(jwtTokenProvider.getJtiFromToken(token)).thenReturn("session-uuid-111");

        // 模擬：Redis 中 Session 仍有效
        when(redisService.isSessionActive("USR12345", "session-uuid-111")).thenReturn(true);

        UserDetails mockUserDetails = mock(UserDetails.class);
        when(mockUserDetails.getAuthorities()).thenReturn(Collections.emptyList());
        when(customUserDetailsService.loadUserByUserId("USR12345")).thenReturn(mockUserDetails);

        // 執行過濾器
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // 驗證：SecurityContextHolder 內確實成功寫入了該登入者的身分
        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals(mockUserDetails, SecurityContextHolder.getContext().getAuthentication().getPrincipal());
    }

    @Test
    public void testFilter_RevokedToken_ShouldNotAuthenticate() throws ServletException, IOException {
        String token = "revoked.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        // 模擬：Token 驗證通過，但 JTI 快取被撤銷了
        when(jwtTokenProvider.validateToken(token)).thenReturn(true);
        when(jwtTokenProvider.getUserIdFromToken(token)).thenReturn("USR12345");
        when(jwtTokenProvider.getJtiFromToken(token)).thenReturn("session-uuid-222");

        when(redisService.isSessionActive("USR12345", "session-uuid-222")).thenReturn(false);

        // 執行過濾器
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // 驗證：Security Context 必須保持為空，不予認證
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testFilter_NoToken_ShouldPassThrough() throws ServletException, IOException {
        // 模擬：請求沒有攜帶 Authorization Header
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // 驗證：Security Context 保持為空
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    public void testFilter_InvalidToken_ShouldNotAuthenticate() throws ServletException, IOException {
        String token = "invalid.jwt.token";
        request.addHeader("Authorization", "Bearer " + token);

        // 模擬：Token 格式錯誤或已過期
        when(jwtTokenProvider.validateToken(token)).thenReturn(false);

        // 執行過濾器
        jwtAuthenticationFilter.doFilter(request, response, filterChain);

        // 驗證：Security Context 保持為空
        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
