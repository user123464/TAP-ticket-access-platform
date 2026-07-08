package tw.com.ispan.backend.organizer.dto;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {
    private String memberId;        // MBR000000X
    private String userId;          // USR000000X (UI 負責人鎖定依賴)
    private String name;            // 成員姓名
    private String email;           // 信箱
    private String roleId;          // 角色：ADMIN / FINANCE / STAFF 等
    private Integer status;         // 狀態：0=PENDING, 1=ACCEPTED, 2=REJECTED, 3=REVOKED
    private LocalDateTime joinedAt; // 加入時間
    private LocalDateTime invitedAt;// 邀請時間 (UI 顯示依賴)
}
