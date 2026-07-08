package tw.com.ispan.backend.organizer.entity;

import java.util.HashSet;
import java.util.Set;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.login.enums.PortalType;
import tw.com.ispan.backend.organizer.enums.ResourceType;

@Entity
@Table(name = "[system_resource]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemResource {

    @Id
    @Column(name = "resource_id", length = 30)
    private String resourceId;

    @Enumerated(EnumType.STRING)
    @Column(name = "portal_type", length = 20, nullable = false)
    private PortalType portalType;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private SystemResource parent;

    @OneToMany(mappedBy = "parent", fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @OrderBy("sortOrder ASC")
    @Builder.Default
    private Set<SystemResource> children = new HashSet<>();

    @Enumerated(EnumType.ORDINAL)
    @Column(name = "resource_type", nullable = false)
    private ResourceType resourceType;

    @Column(name = "name", length = 100, nullable = false)
    private String name;

    @Column(name = "url_path", length = 255)
    private String urlPath;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "permission_id")
    private Permission permission;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;

    // [Jason] 選單圖示（bootstrap-icons class），DB 驅動選單後由 RBAC 編輯頁可改
    @Column(name = "icon", length = 50)
    private String icon;

    // [Jason] 是否顯示於選單（false=隱藏，例如 BUTTON 級資源不進側欄）
    @Column(name = "is_visible", nullable = false)
    @Builder.Default
    private Boolean isVisible = true;
}
