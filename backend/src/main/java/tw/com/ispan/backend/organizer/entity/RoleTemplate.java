package tw.com.ispan.backend.organizer.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 組織預設角色模板主表（DEFAULT_ORG_*）。
 *
 * <p>新組織建立時依此生成組織角色（藍圖：複製當下、之後互不影響）。
 * 取代原本寫死在 {@code AdminRbacService.TEMPLATE_META} 的名稱/說明，
 * 讓模板可由 Admin 後台新增/修改/刪除。權限明細仍存於 {@code role_permission_template}。</p>
 */
@Entity
@Table(name = "role_template")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RoleTemplate {

    @Id
    @Column(name = "template_id", length = 50)
    private String templateId;

    @Column(name = "template_name", length = 50, nullable = false)
    private String templateName;

    @Column(name = "description", length = 255)
    private String description;
}
