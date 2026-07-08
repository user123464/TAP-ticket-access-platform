package tw.com.ispan.backend.login.service;

import java.util.Collections;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

/**
 * 驗證 Google 登入回傳的 ID Token。
 * 使用 Google 官方函式庫，會自動驗證簽章、過期時間，並確認 audience 與本系統 client-id 相符。
 */
@Slf4j
@Component
public class GoogleTokenVerifier {

    @Value("${google.oauth.client-id}")
    private String clientId;

    private GoogleIdTokenVerifier verifier;

    @PostConstruct
    public void init() {
        this.verifier = new GoogleIdTokenVerifier.Builder(new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(clientId))
                .build();
        log.info("GoogleTokenVerifier 已初始化，audience client-id 結尾: ...{}",
                clientId.length() > 12 ? clientId.substring(clientId.length() - 12) : clientId);
    }

    /**
     * 驗證 ID Token，成功回傳 payload，失敗丟出 RuntimeException。
     */
    public GoogleIdToken.Payload verify(String idTokenString) {
        try {
            GoogleIdToken idToken = verifier.verify(idTokenString);
            if (idToken == null) {
                log.warn("Google ID Token 驗證失敗：簽章不符或 audience 不正確");
                throw new RuntimeException("Google 登入驗證失敗，請重新嘗試");
            }
            return idToken.getPayload();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Google ID Token 驗證發生例外", ex);
            throw new RuntimeException("Google 登入驗證失敗，請重新嘗試");
        }
    }
}
