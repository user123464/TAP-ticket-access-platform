package tw.com.ispan.backend.auction.service;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.multipart.MultipartFile;

import tw.com.ispan.backend.auction.dto.request.AuctionCreateRequest;
import tw.com.ispan.backend.auction.dto.request.AuctionUpdateRequest;
import tw.com.ispan.backend.auction.dto.response.AuctionResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeImageResource;

public interface AuctionService {

   
    // 開始競標
    AuctionResponse startAuction(Integer auctionId);

    // 結束競標
    AuctionResponse endAuction(Integer auctionId);

    // 查詢目前價格
    BigDecimal getCurrentPrice(Integer auctionId);

    // 查詢指定主題下的所有拍賣
    List<AuctionResponse> getAuctionsByThemeId(Integer themeId);

    // 查詢指定主題下的公開拍賣
    List<AuctionResponse> getActiveAuctionsByThemeId(Integer themeId);

    // 查詢單一競標商品
    AuctionResponse getAuctionById(Integer auctionId);

    // ===== 新增的 4 個後台管理方法 =====

     // 建立競標商品
    AuctionResponse createAuction(Integer themeId,AuctionCreateRequest request);


    // 後台廠商：查詢自己的所有競標商品
    List<AuctionResponse> getAuctionsByOrganizerId(String organizerId);

    // 後台：編輯競標商品（只允許 DRAFT 狀態編輯）
    AuctionResponse updateAuction(Integer auctionId, AuctionUpdateRequest request);

    // 後台：發佈競標（改狀態為 ACTIVE）
    AuctionResponse publishAuction(Integer auctionId);

    // 後台：刪除競標（改狀態為 DELETED）
    AuctionResponse deleteAuction(Integer auctionId);

    // Auction 圖片：上傳 (取得路徑)
    AuctionResponse uploadAuctionImage(Integer auctionId, MultipartFile file);

    // Auction 圖片：更新
    AuctionResponse updateImage(Integer auctionId, AuctionUpdateRequest request);

    // Auction 圖片：讀取
    ThemeImageResource serveAuctionImage(String fileName);
}
