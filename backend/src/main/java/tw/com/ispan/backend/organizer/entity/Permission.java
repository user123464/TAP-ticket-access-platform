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

@Entity
@Table(name = "[permission]")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @Column(name = "permission_id", length = 50)
    private String permissionId;

    @Column(name = "resource_code", length = 50, nullable = false)
    private String resourceCode;

    @Column(name = "action_code", length = 50, nullable = false)
    private String actionCode;

    @Column(name = "description", length = 100)
    private String description;
}
