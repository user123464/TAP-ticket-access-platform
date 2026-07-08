package tw.com.ispan.backend.system.repository;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import tw.com.ispan.backend.login.entity.User;
import tw.com.ispan.backend.system.entity.UserSubmission;
import tw.com.ispan.backend.system.enums.SubmissionStatus;

public interface UserSubmissionRepository extends JpaRepository<UserSubmission, String> {
    List<UserSubmission> findByUser(User user);
    List<UserSubmission> findByStatusCodeOrderByCreatedAtDesc(SubmissionStatus statusCode);

    // 取得資料庫中 seq_USM 的下一個序列值，用於產生 USMXXXXXXX 格式的主鍵
    @Query(value = "SELECT NEXT VALUE FOR seq_USM", nativeQuery = true)
    Long getNextSubmissionSequenceValue();
}
