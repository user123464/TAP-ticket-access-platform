package tw.com.ispan.backend.settlements.controller;

import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.Data;
import tw.com.ispan.backend.settlements.dto.SettlementDTO;
import tw.com.ispan.backend.settlements.service.SettlementService;

@RestController
@RequestMapping("/api/settlements")
public class SettlementController {

    private final SettlementService settlementService;

    public SettlementController(SettlementService settlementService) {
        this.settlementService = settlementService;
    }

    /**
     * POST 方式查詢結算清單
     */
    @PostMapping("/query")
    public ResponseEntity<List<SettlementDTO>> querySettlements(@RequestBody QueryRequest request) {
        List<SettlementDTO> result = settlementService.getSettlements(request.getOrganizerId(), request.getStatus());
        return ResponseEntity.ok(result);
    }

    /**
     * POST 方式匯出報表為 CSV
     */
    @PostMapping("/export")
    public ResponseEntity<byte[]> exportSettlements(@RequestBody QueryRequest request) {
        byte[] csvData = settlementService.exportSettlementsCsv(request.getOrganizerId(), request.getStatus());
        
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"settlement_report.csv\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csvData);
    }

    @Data
    public static class QueryRequest {
        private String organizerId;
        private String status;
    }
}
