package tw.com.ispan.backend.config.security;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import tw.com.ispan.backend.login.entity.User;

@SpringBootTest
public class JwtTokenProviderTest {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Test
    public void testTokenGenerationAndValidation() {
        // Create a dummy user
        User user = User.builder()
                .userId("USR1234567")
                .email("test.jwt@ispan.com")
                .name("JWT Test User")
                .build();

        // 1. Generate Token
        String token = jwtTokenProvider.generateToken(user);
        assertNotNull(token);
        assertTrue(token.length() > 0);

        // 2. Validate Token
        boolean isValid = jwtTokenProvider.validateToken(token);
        assertTrue(isValid);

        // 3. Extract Claims
        String userId = jwtTokenProvider.getUserIdFromToken(token);
        assertEquals("USR1234567", userId);

        String jti = jwtTokenProvider.getJtiFromToken(token);
        assertNotNull(jti);
        assertTrue(jti.length() > 0);
    }

    @Test
    public void testInvalidToken() {
        String invalidToken = "invalid.token.here";
        boolean isValid = jwtTokenProvider.validateToken(invalidToken);
        assertFalse(isValid);
    }
}
