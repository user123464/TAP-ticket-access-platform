package tw.com.ispan.backend.theme.entity;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.organizer.entity.Organizer;
import tw.com.ispan.backend.theme.enums.Status;

@Entity
@Table(name = "Theme")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Theme {
    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "theme_id")
    private Integer themeId;

    // FK Session(session_id)
    @OneToMany(mappedBy = "theme", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<Session> sessions = new ArrayList<>();

    // FK Organizer(organizer_id)
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "organizer_id", nullable = false)
    private Organizer organizer;

    // 狀態
    @Enumerated(EnumType.STRING)
    private Status status = Status.DRAFT;

    // 標題
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    // 詳細內容
    @Column(name = "detail", columnDefinition = "NVARCHAR(MAX)")
    private String detail;

    // 圖片
    @Column(name = "image", length = 255)
    private String image;

    // 建立時間
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 更新時間
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
}