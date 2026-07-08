package tw.com.ispan.backend.config.security;

import java.io.IOException;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.service.RedisService;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final CustomUserDetailsService customUserDetailsService;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            // 1. 從 Request 中解析 JWT Token (Bearer)
            String jwt = getJwtFromRequest(request);

            // 2. 驗證 Token 合法性
            if (StringUtils.hasText(jwt) && jwtTokenProvider.validateToken(jwt)) {

                // 3. 提取 userId 與 jti，並向 Redis 驗證該 Token 是否仍有效（未被撤銷）
                String userId = jwtTokenProvider.getUserIdFromToken(jwt);
                String jti = jwtTokenProvider.getJtiFromToken(jwt);

                if (!redisService.isSessionActive(userId, jti)) {
                    log.warn("檢測到無效或已被註銷的 JWT Token，拒絕認證 (UserId: {}, JTI: {})", userId, jti);
                    // 已經登出或失效的 Token，不設定 Authentication，讓後續的 Security 攔截器擋下

                    // ==========================================
                    // ⚠️ 壓測/Debug 修改區塊開始 (測試完可刪除)
                    // 強制清除上下文，避免隱性權限衝突導致 403
                    // ==========================================
                    SecurityContextHolder.clearContext();
                    // ==========================================
                    // ⚠️ 壓測/Debug 修改區塊結束
                    // ==========================================

                } else {
                    // 4. 載入使用者詳細資料
                    UserDetails userDetails = customUserDetailsService.loadUserByUserId(userId);

                    // 5. 封裝成 Spring Security 的認證物件
                    UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));

                    // 6. 設定到 SecurityContextHolder，宣告此請求驗證通過
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                }
            }
        } catch (Exception ex) {
            log.error("無法在 Security Context 中設定使用者認證", ex);

            // ==========================================
            // ⚠️ 壓測/Debug 修改區塊開始 (測試完可刪除)
            // 發生解析異常時，也要確保上下文被清空
            // ==========================================
            SecurityContextHolder.clearContext();
            // ==========================================
            // ⚠️ 壓測/Debug 修改區塊結束
            // ==========================================
        }

        // 繼續執行過濾器鏈
        filterChain.doFilter(request, response);
    }

    /**
     * 從 Request Header 的 Authorization 中解析 JWT Token (Bearer)
     */
    private String getJwtFromRequest(HttpServletRequest request) {
        String bearerToken = request.getHeader("Authorization");
        if (StringUtils.hasText(bearerToken) && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }
}
