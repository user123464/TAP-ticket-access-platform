package tw.com.ispan.backend.auction.controller;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.auction.dto.request.AuctionCreateRequest;
import tw.com.ispan.backend.auction.dto.request.AuctionUpdateRequest;
import tw.com.ispan.backend.auction.dto.response.AuctionResponse;
import tw.com.ispan.backend.auction.service.AuctionService;
import tw.com.ispan.backend.theme.dto.ApiResponse;
import tw.com.ispan.backend.theme.dto.response.ThemeImageResource;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class AuctionController {

    private final AuctionService auctionService;

    // ===== 前台用戶相關端點 =====

    // 前台：查詢指定主題下的所有競標
    @GetMapping("/themes/{themeId}/auctions")
    public ResponseEntity<ApiResponse<List<AuctionResponse>>> getAuctionsByThemeId(@PathVariable Integer themeId) {
        List<AuctionResponse> auctions = auctionService.getActiveAuctionsByThemeId(themeId);
        return ResponseEntity.ok(ApiResponse.success(auctions));
    }

    // 前台：查詢單一競標商品
    @GetMapping("/auctions/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> getAuctionById(@PathVariable Integer auctionId) {
        AuctionResponse auction = auctionService.getAuctionById(auctionId);
        return ResponseEntity.ok(ApiResponse.success(auction));
    }

    // 前台：查詢目前價格
    @GetMapping("/auctions/{auctionId}/current-price")
    public ResponseEntity<ApiResponse<Map<String, BigDecimal>>> getCurrentPrice(@PathVariable Integer auctionId) {
        BigDecimal currentPrice = auctionService.getCurrentPrice(auctionId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("currentPrice", currentPrice)));
    }

    // ===== 後台廠商管理相關端點 =====

    // 後台廠商：查詢指定主題下的所有競標
    @GetMapping("/org/themes/{themeId}/auctions")
    public ResponseEntity<ApiResponse<List<AuctionResponse>>> getAuctionsByThemeIdForOrganizer(
            @PathVariable Integer themeId) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.getAuctionsByThemeId(themeId)));
    }

    // 後台廠商：新增競標商品
    @PostMapping("/org/themes/{themeId}/auctions")
    public ResponseEntity<ApiResponse<AuctionResponse>> createAuction(
        @PathVariable Integer themeId,
        @RequestBody AuctionCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.createAuction(themeId, request)));
    }

    // 後台廠商：更新競標商品
    @PutMapping("/org/auctions/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> updateAuction(
            @PathVariable Integer auctionId,
            @RequestBody AuctionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.updateAuction(auctionId, request)));
    }

    // 後台廠商：發佈競標（DRAFT → ACTIVE）
    @PutMapping("/org/auctions/{auctionId}/publish")
    public ResponseEntity<ApiResponse<AuctionResponse>> publishAuction(@PathVariable Integer auctionId) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.publishAuction(auctionId)));
    }

    // 後台廠商：結束競標（ACTIVE → ARCHIVED）
    @PutMapping("/org/auctions/{auctionId}/end")
    public ResponseEntity<ApiResponse<AuctionResponse>> endAuction(@PathVariable Integer auctionId) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.endAuction(auctionId)));
    }

    // 後台廠商：刪除競標（改狀態為 DELETED）
    @DeleteMapping("/org/auctions/{auctionId}")
    public ResponseEntity<ApiResponse<AuctionResponse>> deleteAuction(@PathVariable Integer auctionId) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.deleteAuction(auctionId)));
    }

    // 後台廠商：圖片上傳（取得路徑）
    @PreAuthorize("hasAuthority('EVENT_EDIT')")
    @PostMapping("/org/auctions/upload-image")
    public ResponseEntity<ApiResponse<AuctionResponse>> uploadAuctionImage(
            @RequestParam Integer auctionId,
            @RequestParam MultipartFile file) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.uploadAuctionImage(auctionId, file)));

    }

    // 後台廠商：圖片更新
    @PutMapping("/org/auctions/{auctionId}/image")
    public ResponseEntity<ApiResponse<AuctionResponse>> updateAuctionImage(
            @PathVariable Integer auctionId,
            @RequestBody AuctionUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success(auctionService.updateImage(auctionId, request)));
    }

    // 圖片：讀取
    @GetMapping("/auctions/images/{fileName}")
    public ResponseEntity<byte[]> serveAuctionImage(
            @PathVariable String fileName,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch) {
        try {
            ThemeImageResource resource = auctionService.serveAuctionImage(fileName);

            String etag = "\"" + resource.lastModified() + "\"";
            if (etag.equals(ifNoneMatch)) {
                return ResponseEntity.status(HttpStatus.NOT_MODIFIED).build(); // 回傳 304
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(resource.mimeType()))
                    .header(HttpHeaders.ETAG, etag)
                    .cacheControl(CacheControl.noCache())
                    .body(resource.data()); // 回傳 200

        } catch (IllegalArgumentException e) {
            return ResponseEntity.notFound().build(); // 回傳 404
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build(); // 回傳 403
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build(); // 回傳 500
        }
    }
}