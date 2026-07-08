package tw.com.ispan.backend.theme.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import tw.com.ispan.backend.location.entity.Location;
import tw.com.ispan.backend.theme.enums.Status;

@Entity
@Table(name = "session")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Session {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "session_id")
    private Integer sessionId;

    // FK Theme(theme_id)
    @ManyToOne
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    // FK Location(location_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id", nullable = false)
    private Location location;

    // 狀態
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.DRAFT;

    // 標題
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    // 詳細內容
    @Column(name = "detail", columnDefinition = "NVARCHAR(MAX)")
    private String detail;

    // 公開時間
    @Column(name = "publish_time")
    private LocalDateTime publishTime;

    // 販售開始時間
    @Column(name = "selling_start_time")
    private LocalDateTime sellingStartTime;
    
    // 販售結束時間
    @Column(name = "selling_end_time")
    private LocalDateTime sellingEndTime;

    // 場次開始時間
    @Column(name = "start_time")
    private LocalDateTime startTime;

    // 場次結束時間
    @Column(name = "end_time")
    private LocalDateTime endTime;

    // 建立時間
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    // 更新時間
    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

}