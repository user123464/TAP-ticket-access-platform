package tw.com.ispan.backend.location.service;

import java.util.List;

import tw.com.ispan.backend.location.dto.request.LocationCreateRequestDTO;
import tw.com.ispan.backend.location.dto.response.LocationDetailResponseDTO;
import tw.com.ispan.backend.location.dto.response.LocationSummaryResponseDTO;

public interface LocationService {

    // 建立/覆寫場地模板
    void saveLocationTemplate(LocationCreateRequestDTO request);

    // 取得所有場地列表 (簡要資訊)
    List<LocationSummaryResponseDTO> getAllLocations();

    // 查詢場地詳細資訊 (包含底下的分區與座位)
    LocationDetailResponseDTO getLocationDetail(Integer locationId);

    // 切換場地的隱藏/公開狀態 (軟刪除切換)
    void toogleLocationDeletedStatus(Integer locationId);
}
