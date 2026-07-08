package tw.com.ispan.backend.login.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import tw.com.ispan.backend.login.entity.User;

public interface UserRepository extends JpaRepository<User, String> {

    // Spring Data JPA 魔術方法：根據 Email 查詢使用者
    Optional<User> findByEmail(String email);

    // 根據 Google OAuth 唯一識別碼 (sub) 查詢使用者
    Optional<User> findByGoogleOauthId(String googleOauthId);

    // 取得資料庫中 seq_USR 的下一個序列值，用於產生 USRXXXXXXX 格式的主鍵
    @Query(value = "SELECT NEXT VALUE FOR seq_USR", nativeQuery = true)
    Long getNextUserSequenceValue();

    /**
     * 確定性軟刪除：直接以原生 UPDATE 將 is_deleted 設為 1。
     * 刻意不走 entity 的 @SQLDelete（其帶有樂觀鎖 row_version 條件，易因版本不符而失敗），
     * 確保刪除帳號一定成功；clear/flush 自動同步持久化內容避免讀到舊狀態。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE [user] SET is_deleted = 1, updated_at = SYSDATETIME() WHERE user_id = :userId", nativeQuery = true)
    int softDeleteById(@Param("userId") String userId);

    /**
     * Admin 後台：列出全部使用者（含已軟刪除者）。
     * 走原生 SQL 刻意繞過 entity 上的 {@code @SQLRestriction("is_deleted = 0")}，
     * 讓後台能看見「已刪除」狀態的帳號（前端會以狀態徽章區分）。依建立時間新到舊排序。
     */
    @Query(value = "SELECT * FROM [user] ORDER BY created_at DESC", nativeQuery = true)
    List<User> findAllIncludingDeleted();

    /**
     * Admin 後台：依字串主鍵查單一使用者（含已軟刪除者），繞過 @SQLRestriction。
     */
    @Query(value = "SELECT * FROM [user] WHERE user_id = :userId", nativeQuery = true)
    Optional<User> findByIdIncludingDeleted(@Param("userId") String userId);

    /**
     * Admin 後台：依 Email 查單一使用者（含已軟刪除者），繞過 @SQLRestriction。
     * 建立員工帳號時用於唯一性檢查——email 欄位於 DB 為唯一鍵，
     * 若僅用 findByEmail 會漏掉「已軟刪除但 email 仍佔用」的帳號而觸發 DB 約束錯誤。
     */
    @Query(value = "SELECT * FROM [user] WHERE email = :email", nativeQuery = true)
    Optional<User> findByEmailIncludingDeleted(@Param("email") String email);

    /**
     * Admin 後台帳號操作：直接以原生 UPDATE 設定 is_active / locked_until，
     * 繞過樂觀鎖與 @SQLRestriction，確保停用/啟用/解鎖一定生效。
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = "UPDATE [user] SET is_active = :isActive, locked_until = :lockedUntil, updated_at = SYSDATETIME() WHERE user_id = :userId", nativeQuery = true)
    int adminUpdateStatus(@Param("userId") String userId,
                          @Param("isActive") boolean isActive,
                          @Param("lockedUntil") java.time.LocalDateTime lockedUntil);

    /** Admin RBAC：統計某平台角色（roleId）被指派給多少使用者（user_role 關聯）。 */
    @Query("SELECT COUNT(u) FROM User u JOIN u.roles r WHERE r.roleId = :roleId")
    long countByRoleId(@Param("roleId") String roleId);

    /** Admin RBAC：內部人員＝持有任一內部人員角色（平台層 organizer 為 null 且 is_editable=true）的帳號。 */
    @Query("SELECT DISTINCT u FROM User u JOIN u.roles r WHERE r.organizer IS NULL AND r.isEditable = true ORDER BY u.createdAt DESC")
    List<User> findStaffUsers();
}

