package tw.com.ispan.backend.config.security;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.login.entity.User;

@Slf4j
@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String jwtSecret;

    @Value("${jwt.expiration}")
    private long jwtExpirationInSeconds;

    private SecretKey key;

    @PostConstruct
    public void init() {
        // JJWT 0.12+ Keys.hmacShaKeyFor
        this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 產生 JWT Token
     * 以 userId 作為 Subject，隨機 UUID 作為 jti (配合 UserSession 欄位)
     */
    public String generateToken(User user) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + (jwtExpirationInSeconds * 1000));
        String jti = UUID.randomUUID().toString();

        return Jwts.builder()
                .subject(user.getUserId())
                .claim("jti", jti)
                .issuedAt(now)
                .expiration(expiryDate)
                .signWith(key)
                .compact();
    }

    /**
     * 驗證 JWT Token 是否有效
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(key)
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (SignatureException e) {
            log.error("無效的 JWT 簽章: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            log.error("無效的 JWT 格式: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            log.error("已過期的 JWT Token: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            log.error("不支援的 JWT Token: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            log.error("JWT claims 字串為空: {}", e.getMessage());
        } catch (JwtException e) {
            log.error("JWT 驗證失敗: {}", e.getMessage());
        }
        return false;
    }

    /**
     * 從 Token 中解析出 UserId (Subject)
     */
    public String getUserIdFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    /**
     * 從 Token 中解析出 jti (Session UUID)
     */
    public String getJtiFromToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.get("jti", String.class);
    }
}
