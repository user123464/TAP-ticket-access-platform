package tw.com.ispan.backend.auction.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.auction.dto.request.BidCreateRequest;
import tw.com.ispan.backend.auction.dto.response.BidResponse;
import tw.com.ispan.backend.auction.service.BidService;
import tw.com.ispan.backend.config.security.CustomUserDetailsService.CustomUserDetails;
import tw.com.ispan.backend.theme.dto.ApiResponse;

@RequiredArgsConstructor
@RestController
@RequestMapping("/api")
public class BidController {

    private final BidService bidService;

    // 前台：查詢競標出價紀錄
    @GetMapping("/auctions/{auctionId}/bids")
    public ResponseEntity<ApiResponse<List<BidResponse>>> getBidHistory(
            @PathVariable Integer auctionId) {
        return ResponseEntity.ok(ApiResponse.success(bidService.getBidHistory(auctionId)));
    }

    // 前台：訂閱競標出價事件
    @GetMapping(value = "/auctions/{auctionId}/bids/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter subscribeBidStream(@PathVariable Integer auctionId) {
        return bidService.subscribeBidStream(auctionId);
    }

    // 前台：出價 (重點接口)
    @PostMapping("/auctions/{auctionId}/bids")
    public ResponseEntity<ApiResponse<String>> placeBid(
            @PathVariable Integer auctionId,
            @RequestBody BidCreateRequest request,
            @AuthenticationPrincipal CustomUserDetails userDetails) {
        if (userDetails == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.fail("請先登入後再出價"));
        }
        String userId = userDetails.getUserId();
        String userName = userDetails.getName();
        bidService.placeBid(auctionId, userId, userName, request.bidPrice());
        return ResponseEntity.ok(ApiResponse.success("出價已受理，等待確認"));
    }
}
