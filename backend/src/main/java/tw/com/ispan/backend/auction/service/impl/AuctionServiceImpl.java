package tw.com.ispan.backend.auction.service.impl;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.auction.dto.request.AuctionCreateRequest;
import tw.com.ispan.backend.auction.dto.request.AuctionUpdateRequest;
import tw.com.ispan.backend.auction.dto.response.AuctionResponse;
import tw.com.ispan.backend.auction.entity.Auction;
import tw.com.ispan.backend.auction.repository.AuctionRepository;
import tw.com.ispan.backend.auction.service.AuctionService;
import tw.com.ispan.backend.auction.service.BidRedisGateService;
import tw.com.ispan.backend.theme.entity.Theme;
import tw.com.ispan.backend.theme.enums.Status;
import tw.com.ispan.backend.theme.repository.ThemeRepository;
import tw.com.ispan.backend.theme.dto.response.ThemeImageResource;
import tw.com.ispan.backend.theme.service.ThemeImageStorageService;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class AuctionServiceImpl implements AuctionService {

    private final AuctionRepository auctionRepository;
    private final ThemeRepository themeRepository;
    private final ThemeImageStorageService imageStorageService;
    private final BidRedisGateService bidRedisGateService;

    @Override
    @Transactional
    public AuctionResponse createAuction(Integer themeId, AuctionCreateRequest request) {
        // 1. 驗證主題 ID
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }

        // 2. 取得主題資料
        Theme theme = themeRepository.findById(themeId)
                .orElseThrow(() -> new RuntimeException("Theme not found"));

        // 3. 組裝新的競標資料
        Auction auction = new Auction();
        auction.setTheme(theme);
        auction.setTitle(request.title());
        auction.setDetail(request.detail());
        auction.setImage(request.image());
        auction.setStartPrice(request.startPrice());
        auction.setBuyoutPrice(request.buyoutPrice());
        auction.setCurrentPrice(request.startPrice());
        auction.setStartTime(request.startTime());
        auction.setEndTime(request.endTime());
        auction.setStatus(Status.DRAFT);

        // 4. 儲存並回傳
        auctionRepository.save(auction);

        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional
    public AuctionResponse startAuction(Integer auctionId) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 讀取拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 3. 只允許 DRAFT 狀態開始
        if (auction.getStatus() != Status.DRAFT) {
            throw new IllegalStateException("只有 DRAFT 狀態可以開始競標");
        }

        // 4. 檢查是否落在可開始的時間區間
        LocalDateTime now = LocalDateTime.now();
        if (now.isBefore(auction.getStartTime())) {
            throw new IllegalStateException("尚未到競標開始時間");
        }
        if (!now.isBefore(auction.getEndTime())) {
            throw new IllegalStateException("競標已過期，無法開始");
        }

        // 5. 切換為 ACTIVE
        auction.setStatus(Status.ACTIVE);
        if (auction.getCurrentPrice() == null) {
            auction.setCurrentPrice(auction.getStartPrice());
        }
        bidRedisGateService.initializeAuction(auction);
        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional
    public AuctionResponse endAuction(Integer auctionId) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 取得拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 3. 已結束則直接回傳
        if (auction.getStatus() == Status.ARCHIVED) {
            bidRedisGateService.archiveAuction(auctionId);
            return AuctionResponse.fromEntity(auction);
        }

        // 4. 切換為 ARCHIVED
        auction.setStatus(Status.ARCHIVED);
        bidRedisGateService.archiveAuction(auctionId);
        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional(readOnly = true)
    public BigDecimal getCurrentPrice(Integer auctionId) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 讀取拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        // 3. currentPrice 為空時回退到 startPrice
        return auction.getCurrentPrice() != null ? auction.getCurrentPrice() : auction.getStartPrice();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionResponse> getAuctionsByThemeId(Integer themeId) {
        // 1. 驗證主題 ID
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }

        // 2. 查詢主題底下的競標列表
        return auctionRepository.findByThemeThemeId(themeId)
                .stream()
                .map(AuctionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionResponse> getActiveAuctionsByThemeId(Integer themeId) {
        // 1. 驗證主題 ID
        if (themeId == null) {
            throw new IllegalArgumentException("themeId 不可為空");
        }

        // 2. 查詢主題底下公開中的競標列表
        return auctionRepository.findByThemeThemeIdAndStatus(themeId, Status.ACTIVE)
                .stream()
                .map(AuctionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public AuctionResponse getAuctionById(Integer auctionId) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 讀取單筆拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AuctionResponse> getAuctionsByOrganizerId(String organizerId) {
        // 1. 驗證廠商 ID
        if (organizerId == null || organizerId.isBlank()) {
            throw new IllegalArgumentException("organizerId 不可為空");
        }

        // 2. 查詢該廠商底下的競標列表
        return auctionRepository.findByThemeOrganizerOrganizerId(organizerId)
                .stream()
                .map(AuctionResponse::fromEntity)
                .toList();
    }

    @Override
    @Transactional
    public AuctionResponse updateAuction(Integer auctionId, AuctionUpdateRequest request) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 取得拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("找不到 Auction"));

        // 3. 只允許 DRAFT 狀態編輯
        if (auction.getStatus() != Status.DRAFT) {
            throw new IllegalStateException("只有 DRAFT 狀態的競標商品可以編輯");
        }

        // 4. 更新可編輯欄位
        auction.setTitle(request.title());
        auction.setDetail(request.detail());
        auction.setImage(request.image());
        auction.setStartPrice(request.startPrice());
        auction.setBuyoutPrice(request.buyoutPrice());
        auction.setStartTime(request.startTime());
        auction.setEndTime(request.endTime());

        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional
    public AuctionResponse publishAuction(Integer auctionId) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 取得拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("找不到 Auction"));

        // 3. 只允許 DRAFT 狀態發佈
        if (auction.getStatus() != Status.DRAFT) {
            throw new IllegalStateException("只有 DRAFT 狀態的競標商品可以發佈");
        }

        // 4. 檢查是否已超過結束時間
        LocalDateTime now = LocalDateTime.now();
        if (now.isAfter(auction.getEndTime())) {
            throw new IllegalStateException("競標已過期，無法發佈");
        }

        // 5. 切換為 ACTIVE
        auction.setStatus(Status.ACTIVE);
        bidRedisGateService.initializeAuction(auction);
        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional
    public AuctionResponse deleteAuction(Integer auctionId) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 取得拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("找不到 Auction"));

        // 3. 已刪除則直接回傳
        if (auction.getStatus() == Status.DELETED) {
            return AuctionResponse.fromEntity(auction);
        }

        // 4. 切換為 DELETED
        auction.setStatus(Status.DELETED);
        bidRedisGateService.archiveAuction(auctionId);
        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional
    public AuctionResponse uploadAuctionImage(Integer auctionId, MultipartFile file) {
        // 1. 驗證拍賣 ID
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }

        // 2. 取得拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("找不到 Auction"));

        // 3. 儲存圖片並更新圖片路徑
        String imageUrl = imageStorageService.storeImage("auction-images", "auction_" + auctionId, file,
                "/api/auctions/images/", "auction", String.valueOf(auctionId), null);
        auction.setImage(imageUrl);
        auctionRepository.save(auction);

        return AuctionResponse.fromEntity(auction);
    }

    @Override
    @Transactional
    public AuctionResponse updateImage(Integer auctionId, AuctionUpdateRequest request) {
        // 1. 驗證拍賣 ID 與圖片內容
        if (auctionId == null) {
            throw new IllegalArgumentException("auctionId 不可為空");
        }
        if (request == null || request.image() == null) {
            throw new IllegalArgumentException("圖片不可為空");
        }

        // 2. 取得拍賣資料
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("找不到 Auction"));

        // 3. 更新圖片路徑
        auction.setImage(request.image());
        auctionRepository.save(auction);

        return AuctionResponse.fromEntity(auction);
    }

    @Override
    public ThemeImageResource serveAuctionImage(String fileName) {
        // 圖片讀取交給共用的檔案儲存服務處理
        return imageStorageService.serveImage("auction-images", fileName);
    }
}
