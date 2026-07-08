package tw.com.ispan.backend.location.controller;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.common.dto.Response;
import tw.com.ispan.backend.location.dto.request.LocationCreateRequestDTO;
import tw.com.ispan.backend.location.dto.response.LocationDetailResponseDTO;
import tw.com.ispan.backend.location.dto.response.LocationSummaryResponseDTO;
import tw.com.ispan.backend.location.service.LocationService;

//場地管理 API 控制器
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/locations")
public class LocationController {
    // 依賴反轉：這裡注入的是 Interface，而不是 Impl 實作類別
    // Spring Boot 啟動時會自動找到 LocationServiceImpl 並把它塞進來
    private final LocationService locationService;

    // 建立場地模板 (批次寫入分區與座位)對應前端的 POST /api/locations
    @PostMapping
    public Response saveLocation(@RequestBody LocationCreateRequestDTO request) {
        try {
            // 呼叫 Service 執行業務邏輯 (如果出錯，Service 會拋出 Exception)
            locationService.saveLocationTemplate(request);

            // 成功時，回傳統一的 Response 格式給前端的 Swal.fire 顯示
            return Response.success("場地模板與座位建立成功！", null);
        } catch (Exception e) {
            // 捕捉到錯誤時，回傳帶有 false 旗標的 Response
            // 在實務上，這裡的 catch 未來可以被「全域例外處理器」取代，讓 Controller 更乾淨
            return Response.error("場地建立失敗：" + e.getMessage());
        }
    }

    // 查詢場地列表
    @GetMapping
    public Response getAllLocations() {

        try {
            // 呼叫 Service 取得 DTO 列表
            List<LocationSummaryResponseDTO> locationList = locationService.getAllLocations();

            return Response.success("查詢成功", locationList);
        } catch (Exception e) {
            return Response.error("查詢失敗：" + e.getMessage());
        }

    }

    // 查詢場地詳細資訊 (包含底下的分區與座位)
    @GetMapping("/{locationId}")
    public Response getLocationDetail(@PathVariable Integer locationId) {
        try {
            // 呼叫 Service 進行免 N+1 的高效查詢與組裝
            LocationDetailResponseDTO detailDTO = locationService.getLocationDetail(locationId);

            return Response.success("查詢成功", detailDTO);

        } catch (Exception e) {
            return Response.error(e.getMessage());
        }
    }

    /**
     * 變更場地狀態 (隱藏/公開)
     * 對應前端的 PATCH /api/locations/{locationId}/toggle-status
     */
    @PatchMapping("/{locationId}/toggle-status")
    public Response toggleLocationStatus(@PathVariable Integer locationId) {
        try {
            // 呼叫 Service 執行狀態反轉
            locationService.toogleLocationDeletedStatus(locationId);

            return Response.success("場地狀態更新成功！", null);
        } catch (Exception e) {
            e.printStackTrace();
            return Response.error("變更場地狀態失敗：" + e.getMessage());
        }
    }
}
