package tw.com.ispan.backend.orderTicket.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tw.com.ispan.backend.orderTicket.entity.TicketOrderDetailBean;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderStatus;
import tw.com.ispan.backend.orderTicket.enums.TicketOrderUse;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderDetailRepository;

/**
 * 現場驗票核銷服務。
 * 
 * <p>提供前台/主辦方在現場掃描 QR Code 進行驗票核銷的業務邏輯，包含防重複使用驗證以及狀態更新。</p>
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class TicketCheckInService {

    private final TicketOrderDetailRepository ticketOrderDetailRepository;

    /**
     * 現場驗票核銷。
     *
     * @param qrCodeHash 門票二維碼雜湊值
     * @return 驗票結果訊息字串
     */
    @Transactional
    public String checkInTicket(String qrCodeHash) {
        log.info("【驗票核銷】收到驗票請求 - qrCodeHash={}", qrCodeHash);

        // 1. 根據 QR Code Hash 尋找對應的訂單明細
        TicketOrderDetailBean detail = ticketOrderDetailRepository.findByQrCodeHash(qrCodeHash);
        if (detail == null) {
            log.warn("【驗票核銷】驗票失敗，系統無此 QR Code - qrCodeHash={}", qrCodeHash);
            return "驗票失敗：此 QR Code 在系統中不存在！";
        }

        // 2. 驗證是否已被核銷過
        if (detail.getIsUsed().equals(TicketOrderUse.Redeemed)) {
            log.warn("【驗票核銷】驗票失敗，門票已被重複使用 - qrCodeHash={}, usedAt={}", qrCodeHash, detail.getUsedAt());
            return "驗票失敗：此門票已於 " + detail.getUsedAt() + " 核銷進場，請勿重複使用！";
        }

        // 3. 驗證是否已退票/取消
        if (detail.getIsUsed().equals(TicketOrderUse.Canceled)) {
            log.warn("【驗票核銷】驗票失敗，此門票已取消退票 - qrCodeHash={}", qrCodeHash);
            return "驗票失敗：此門票已辦理退票";
        }

        // 4. 更新明細狀態為「已核銷/已使用」，並記錄當前核銷時間
        detail.setIsUsed(TicketOrderUse.Redeemed);
        detail.setUsedAt(LocalDateTime.now());
        detail.setItemStatus(TicketOrderStatus.NORMAL); // 確保狀態為正常
        
        // 存回資料庫
        ticketOrderDetailRepository.save(detail);

        log.info("【驗票核銷】核銷成功 - detailId={}, realName={}", detail.getTDetailId(), detail.getRealName());
        return "驗票成功! 入場人: " + detail.getRealName() + "，歡迎入場!";
    }
}
