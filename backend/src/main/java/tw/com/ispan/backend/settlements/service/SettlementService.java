package tw.com.ispan.backend.settlements.service;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tw.com.ispan.backend.orderMerch.entity.MerchOrderBean;
import tw.com.ispan.backend.orderMerch.repository.MerchOrderRepository;
import tw.com.ispan.backend.orderTicket.entity.TicketOrdersBean;
import tw.com.ispan.backend.orderTicket.repository.TicketOrderRepository;
import tw.com.ispan.backend.organizer.entity.Contract;
import tw.com.ispan.backend.organizer.enums.ContractStatus;
import tw.com.ispan.backend.organizer.enums.FeeType;
import tw.com.ispan.backend.organizer.repository.ContractRepository;
import tw.com.ispan.backend.organizer.repository.OrganizerRepository;
import tw.com.ispan.backend.settlements.dto.SettlementDTO;

@Service
@Transactional
public class SettlementService {

    private final TicketOrderRepository ticketOrderRepository;
    private final MerchOrderRepository merchOrderRepository;
    private final ContractRepository contractRepository;
    private final OrganizerRepository organizerRepository;

    private static final BigDecimal DEFAULT_PERCENTAGE = new BigDecimal("5"); // 預設 5% 抽成

    public SettlementService(TicketOrderRepository ticketOrderRepository,
                             MerchOrderRepository merchOrderRepository,
                             ContractRepository contractRepository,
                             OrganizerRepository organizerRepository) {
        this.ticketOrderRepository = ticketOrderRepository;
        this.merchOrderRepository = merchOrderRepository;
        this.contractRepository = contractRepository;
        this.organizerRepository = organizerRepository;
    }

    /**
     * 查詢指定主辦方的即時結算紀錄，並依狀態過濾
     */
    public List<SettlementDTO> getSettlements(String organizerId, String status) {
        if ("待處裡".equals(status)) {
            return new ArrayList<>();
        }

        // 查詢主辦方名稱
        String organizerName = organizerRepository.findById(organizerId)
                .map(o -> o.getName())
                .orElse("");

        // 查詢主辦方的生效合約
        Contract contract = contractRepository.findByOrganizer_OrganizerIdAndContractStatus(organizerId, ContractStatus.ACTIVE)
                .orElse(null);

        // 查詢付款成功的票務訂單
        List<TicketOrdersBean> ticketOrders = ticketOrderRepository.findPaidOrdersByOrganizer(organizerId);

        // 查詢付款成功的商品訂單
        List<MerchOrderBean> merchOrders = merchOrderRepository.findPaidOrdersByOrganizer(organizerId);

        List<SettlementDTO> dtos = new ArrayList<>();

        for (TicketOrdersBean to : ticketOrders) {
            dtos.add(convertTicketOrderToDTO(to, contract, organizerName, organizerId));
        }

        for (MerchOrderBean mo : merchOrders) {
            dtos.add(convertMerchOrderToDTO(mo, contract, organizerName, organizerId));
        }

        // 按建立時間降序排列
        dtos.sort((a, b) -> {
            LocalDateTime ta = a.getCreatedAt();
            LocalDateTime tb = b.getCreatedAt();
            if (ta == null && tb == null) return 0;
            if (ta == null) return 1;
            if (tb == null) return -1;
            return tb.compareTo(ta);
        });

        return dtos;
    }

    /**
     * 匯出結算紀錄為 CSV 格式 (附帶 UTF-8 BOM 以防 Excel 亂碼)
     */
    public byte[] exportSettlementsCsv(String organizerId, String status) {
        List<SettlementDTO> list = getSettlements(organizerId, status);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // 寫入 UTF-8 BOM
            baos.write(new byte[] { (byte) 0xEF, (byte) 0xBB, (byte) 0xBF });

            try (PrintWriter writer = new PrintWriter(baos, false, StandardCharsets.UTF_8)) {
                // 標題列
                writer.println("結算帳單 ID,營利種類,結算對象,當期總營業額,實際撥款金額,撥款狀態,撥款完成時間,訂單建立時間");

                for (SettlementDTO s : list) {
                    writer.printf("%s,%s,%s,%s,%s,%s,%s,%s\n",
                            escapeCsv(s.getSettlementId()),
                            escapeCsv(s.getRevenueType()),
                            escapeCsv(s.getOrganizerName()),
                            s.getTotalOrdersAmount() != null ? s.getTotalOrdersAmount().toString() : "0.00",
                            s.getFinalPayoutAmount() != null ? s.getFinalPayoutAmount().toString() : "0.00",
                            escapeCsv(s.getItemStatus()),
                            s.getProcessedAt() != null ? s.getProcessedAt().toString() : "-",
                            s.getCreatedAt() != null ? s.getCreatedAt().toString() : "");
                }
                writer.flush();
            }
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("匯出報表失敗: " + e.getMessage(), e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private SettlementDTO convertTicketOrderToDTO(TicketOrdersBean order, Contract contract, String organizerName, String organizerId) {
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        
        // 計算手續費
        BigDecimal fee = BigDecimal.ZERO;
        if (contract != null) {
            if (contract.getFeeType() == FeeType.FIXED_PER_TICKET) {
                int count = order.getOrderDetail() != null ? order.getOrderDetail().size() : 0;
                fee = contract.getFeeValue().multiply(BigDecimal.valueOf(count));
            } else {
                fee = totalAmount.multiply(contract.getFeeValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
        } else {
            fee = totalAmount.multiply(DEFAULT_PERCENTAGE).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        BigDecimal finalPayout = totalAmount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
        
        return SettlementDTO.builder()
                .settlementId(order.getTOrderId())
                .organizerId(organizerId)
                .organizerName(organizerName)
                .revenueType("票務")
                .periodStart(order.getCreateAt() != null ? order.getCreateAt().toLocalDate() : null)
                .periodEnd(order.getCreateAt() != null ? order.getCreateAt().toLocalDate() : null)
                .totalOrdersAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .finalPayoutAmount(finalPayout)
                .itemStatus("已撥款")
                .processedAt(order.getCreateAt()) // 用 createAt 作為付款完成時間的預設參考
                .createdAt(order.getCreateAt())
                .build();
    }

    private SettlementDTO convertMerchOrderToDTO(MerchOrderBean order, Contract contract, String organizerName, String organizerId) {
        BigDecimal totalAmount = order.getTotalAmount() != null ? order.getTotalAmount() : BigDecimal.ZERO;
        
        // 計算手續費
        BigDecimal fee = BigDecimal.ZERO;
        if (contract != null) {
            if (contract.getFeeType() == FeeType.FIXED_PER_TICKET) {
                int count = order.getMerchOrderDetails() != null ? order.getMerchOrderDetails().size() : 0;
                fee = contract.getFeeValue().multiply(BigDecimal.valueOf(count));
            } else {
                fee = totalAmount.multiply(contract.getFeeValue()).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
            }
        } else {
            fee = totalAmount.multiply(DEFAULT_PERCENTAGE).divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);
        }
        BigDecimal finalPayout = totalAmount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
        
        return SettlementDTO.builder()
                .settlementId(order.getMOrderId())
                .organizerId(organizerId)
                .organizerName(organizerName)
                .revenueType("商品")
                .periodStart(order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null)
                .periodEnd(order.getCreatedAt() != null ? order.getCreatedAt().toLocalDate() : null)
                .totalOrdersAmount(totalAmount.setScale(2, RoundingMode.HALF_UP))
                .finalPayoutAmount(finalPayout)
                .itemStatus("已撥款")
                .processedAt(order.getPaidAt() != null ? order.getPaidAt() : order.getCreatedAt())
                .createdAt(order.getCreatedAt())
                .build();
    }
}
