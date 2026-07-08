package tw.com.ispan.backend.login.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import tw.com.ispan.backend.login.entity.LoginAttempt;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    // ── 登入安全防禦核心查詢 ──
    // 計算某信箱在特定時間（例如過去 10 分鐘）之後的登入失敗次數。
    // 用於比對是否達到暴力破解防禦門檻，以進行帳號暫時鎖定或出驗證碼。
    long countByEmailAndSuccessFalseAndAttemptedAtAfter(String email, LocalDateTime time);

    // 查詢某信箱的所有登入嘗試紀錄，依時間降序排列 (最新在最前)
    List<LoginAttempt> findByEmailOrderByAttemptedAtDesc(String email);

    // Admin 儀表板：統計指定時間點之後「成功」登入的次數（今日登入數）
    long countBySuccessTrueAndAttemptedAtAfter(LocalDateTime time);
}
