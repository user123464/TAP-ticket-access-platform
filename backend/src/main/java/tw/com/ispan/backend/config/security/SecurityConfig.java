package tw.com.ispan.backend.config.security;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import lombok.RequiredArgsConstructor;

@Configuration
@EnableWebSecurity // 啟用 Spring Security 的網頁防護功能
@EnableMethodSecurity(prePostEnabled = true) // 啟用方法級安全 (@PreAuthorize / @PostAuthorize)
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                // 1. 關閉 CSRF (因為我們是前後端分離的 RESTful API，使用 JWT，不依賴 Cookie，無須防範 CSRF)
                                .csrf(csrf -> csrf.disable())

                                // 2. 啟用跨域設定 (CORS) 並套用我們下方定義的規則
                                .cors(Customizer.withDefaults())

                                // CSP 串接綠界與外部字型/樣式使用
                                .headers(headers -> headers
                                                .contentSecurityPolicy(csp -> csp
                                                                .policyDirectives(
                                                                                "default-src 'self'; " +
                                                                                                "script-src 'self' 'unsafe-inline' https://payment-stage.ecpay.com.tw https://gpayment-stage.ecpay.com.tw; " +
                                                                                                "style-src 'self' 'unsafe-inline' https://fonts.googleapis.com; " +
                                                                                                "font-src 'self' data: https://fonts.gstatic.com; " +
                                                                                                "frame-src 'self' https://payment-stage.ecpay.com.tw; " +
                                                                                                "form-action 'self' https://payment-stage.ecpay.com.tw")))

                                // 3. 設定會話管理為「無狀態」 (Stateless)
                                // 告訴 Spring Security 絕對不要建立或使用 HttpSession 來存放使用者狀態
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                                // 4. 設定 API 存取權限規則
                                .authorizeHttpRequests(auth -> auth
                                                // 🚪 允許免登入即可存取的「白名單」路徑
                                                .requestMatchers("/api/auth/**").permitAll() // 包含登入、註冊等 API
                                                .requestMatchers("/api/sse/**").permitAll() // 放行 SSE 廣播
                                                // ==========================================
                                                // ⚠️ 壓測專用後門：暫時放行搶票鎖定 API
                                                .requestMatchers("/api/ticket/lock/**").permitAll()
                                                // (測試結束後記得刪除或註解掉這行！)
                                                // ==========================================
                                                // 🎟️ 購票型錄公開讀取：讓未登入使用者也能瀏覽場次票種與場地圖 (GET only)
                                                .requestMatchers(HttpMethod.GET, "/api/ticket/session/**").permitAll()
                                                .requestMatchers("/actuator/**").permitAll() // 放行效能數據監測 API
                                                .requestMatchers("/api/organizer/accept-invite").permitAll() // 放行接受邀請端點，讓點擊
                                                                                                             // Email
                                                                                                             // 連結可以直接處理
                                                .requestMatchers("/api/organizer/accept-transfer").permitAll() // 放行接受所有權轉移端點，讓點擊
                                                                                                               // Email
                                                                                                               // 連結可以直接處理
                                                .requestMatchers("/api/checkout/ecpay-callback").permitAll() // 放行綠界科技付款回調
                                                                                                             // API
                                                .requestMatchers("/api/checkout/ecpay-redirect").permitAll() // 放行綠界科技付款成功重定向
                                                                                                             // API
                                                .requestMatchers("/api/checkout/ecpay-cancel").permitAll() // 放行綠界科技付款取消/返回
                                                                                                           // API
                                                .requestMatchers("/api/checkout/merch/ecpay-callback").permitAll() // 放行綠界商品付款回調
                                                                                                                   // API
                                                .requestMatchers(HttpMethod.GET, "/api/documents/**").permitAll() // 放行文件讀取
                                                                                                                  // API
                                                .requestMatchers(HttpMethod.POST, "/api/submissions").permitAll() // 放行技術支援表單提交
                                                .requestMatchers(HttpMethod.GET, "/api/user/avatars/**").permitAll() // 放行使用者頭像圖片（公開讀取）
                                                .requestMatchers(HttpMethod.GET, "/api/organizer/avatars/**")
                                                .permitAll() // 放行組織 Logo 圖片（公開讀取）
                                                .requestMatchers(HttpMethod.GET, "/api/themes/images/**").permitAll() // 放行主題圖片（公開讀取）
                                                // 🎨 公開「讀取」端點：前台瀏覽活動主題與場次 (GET only)
                                                // 寫操作 (POST/PUT/PATCH/DELETE) 一律落入 anyRequest().authenticated()
                                                .requestMatchers(HttpMethod.GET, "/api/themes/**").permitAll()
                                                // 競標 暫時測試開放 後續需要登入使用者
                                                .requestMatchers(HttpMethod.GET, "/api/auctions/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/auctions/**").authenticated()
                                                // 後台 競標
                                                .requestMatchers(HttpMethod.GET, "/api/org/auctions/**").authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/org/auctions/**")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.PUT, "/api/org/auctions/**").authenticated()
                                                // 前台瀏覽場館資訊 (GET only)；場館的建立/修改/狀態切換需登入
                                                .requestMatchers(HttpMethod.GET, "/api/locations/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/shop/**").permitAll()
                                                .requestMatchers(HttpMethod.GET, "/api/org/**").permitAll()

                                                // 🔒 其餘所有請求（含上述路徑的寫操作）皆需通過身分驗證
                                                .anyRequest().authenticated())

                                // 5. 插入自訂的 JWT 過濾器
                                // 讓它在標準的帳號密碼過濾器 (UsernamePasswordAuthenticationFilter) 之前執行
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        /**
         * 密碼加密器 Bean
         * 採用業界標準的 BCrypt 強雜湊演算法。在使用者註冊時加密密碼，登入時進行比對。
         */
        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        /**
         * 認證管理器 Bean
         * 這是 Spring Security 的核心引擎。在我們手動撰寫登入 API 時，會呼叫它來完成使用者身分比對。
         */
        @Bean
        public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
                        throws Exception {
                return authenticationConfiguration.getAuthenticationManager();
        }

        /**
         * 全域跨域 CORS 設定 (防止前後端分離開發時的瀏覽器跨域阻擋)
         */
        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                // 允許的前端網址來源模式 (使用 Pattern 可以支援 allowCredentials 攜帶憑證)
                configuration.setAllowedOriginPatterns(List.of("*"));
                // 允許的 HTTP 方法
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                // 允許攜帶的 Header 欄位
                configuration.setAllowedHeaders(
                                List.of("Authorization", "Content-Type", "Cache-Control", "Accept",
                                                "X-Requested-With"));
                // 允許瀏覽器讀取的 Response Header
                configuration.setExposedHeaders(List.of("Authorization"));
                // 允許攜帶登入憑證 (Cookie / Auth Header)
                configuration.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                // 套用到所有路徑
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }
}
