package tw.com.ispan.backend.login.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.login.entity.UserSession;

public interface UserSessionRepository extends JpaRepository<UserSession, Long> {

    // 根據 JWT 的 JTI 識別碼查詢會話記錄 (用於登入驗證時檢查 Token 是否被撤銷)
    Optional<UserSession> findByTokenJti(String tokenJti);

    // 根據使用者 ID 查詢所有未被撤銷的會話記錄
    List<UserSession> findByUserUserIdAndRevokedAtIsNull(String userId);
}

