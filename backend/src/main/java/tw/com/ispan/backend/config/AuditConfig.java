package tw.com.ispan.backend.config;

import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;

@Configuration
public class AuditConfig {

    @Bean
    public AuditorAware<String> auditorProvide() {
        return () -> {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

            if (authentication == null || !authentication.isAuthenticated()
                    || "anonymousUser".equals(authentication.getPrincipal())) {
                // 如果未登入或為匿名訪問，則預設返回系統操作帳號
                return Optional.of("SYSTEM");
            }

            Object principal = authentication.getPrincipal();
            if (principal instanceof CustomUserDetails) {
                CustomUserDetails userDetails = (CustomUserDetails) principal;
                return Optional.of(userDetails.getUserId());
            }

            return Optional.of(authentication.getName());
        };
    }

}
