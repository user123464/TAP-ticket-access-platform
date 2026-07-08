package tw.com.ispan.backend.auction.entity;

import java.math.BigDecimal;
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
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;

@Entity
@Table(name = "auction")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Auction {

    // PK
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "auction_id")
    private Integer auctionId;

    // FK theme(theme_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "theme_id", nullable = false)
    private Theme theme;

    // 狀態
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status = Status.DRAFT;

    // 標題
    @Column(name = "title", nullable = false, length = 150)
    private String title;

    // 詳情
    @Column(name = "detail", columnDefinition = "NVARCHAR(MAX)")
    private String detail;

    // 圖片
    @Column(name = "image", length = 255)
    private String image;

    // 起標價
    @Column(name = "start_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal startPrice;

    // 直購價
    @Column(name = "buyout_price", precision = 10, scale = 2)
    private BigDecimal buyoutPrice;

    // 目前價格
    @Column(name = "current_price", precision = 10, scale = 2)
    private BigDecimal currentPrice;

    // 開始時間
    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    // 結束時間
    @Column(name = "end_time", nullable = false)
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