package tw.com.ispan.backend.location.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import tw.com.ispan.backend.location.dto.request.LocationCreateRequestDTO;
import tw.com.ispan.backend.location.dto.request.LocationCreateRequestDTO.SeatSaveDTO;
import tw.com.ispan.backend.location.dto.request.LocationCreateRequestDTO.ZoneSaveDTO;
import tw.com.ispan.backend.location.dto.response.LocationDetailResponseDTO;
import tw.com.ispan.backend.location.dto.response.LocationSummaryResponseDTO;
import tw.com.ispan.backend.location.dto.response.ZoneResponseDTO;
import tw.com.ispan.backend.location.entity.Location;
import tw.com.ispan.backend.location.entity.Seat;
import tw.com.ispan.backend.location.entity.Zone;
import tw.com.ispan.backend.location.repository.LocationRepository;
import tw.com.ispan.backend.location.repository.SeatRepository;
import tw.com.ispan.backend.location.repository.ZoneRepository;
import tw.com.ispan.backend.location.service.LocationService;
import tw.com.ispan.backend.theme.repository.SessionRepository;

@Service
@RequiredArgsConstructor
public class LocationServiceImpl implements LocationService {

    // 注入需要的 Repositories
    private final LocationRepository locationRepository;
    private final ZoneRepository zoneRepository;
    private final SeatRepository seatRepository;

    private final SessionRepository sessionRepository;

    /**
     * 實作：建立場地模板
     * 加上 @Transactional 確保：要麼全部新增成功，要麼全部回滾(Rollback)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLocationTemplate(LocationCreateRequestDTO request) {

        Location location;

        if (request.getId() == null) {
            // 沒有傳ID ->【新增模式】: 這是全新的場地，直接建立
            location = new Location();
        } else {
            // 【修改模式】: 前端傳了 ID 試圖更新現有場地
            // 安全鎖：檢查這個場地是否已經被任何「場次」使用？
            // 實作上可能是：boolean isInUse =
            // sessionRepository.existsByLocationId(request.getId());
            boolean isUsed = sessionRepository.existsByLocationLocationId(request.getId());
            // boolean isUsed = false;

            if (isUsed) {
                // 如果已經被使用，絕對禁止覆寫，直接拋出例外中斷 Transaction！
                throw new IllegalStateException("此場地已被活動使用，為保護票券資料完整性，禁止修改實體座位！請使用「另存新檔」功能。");
            }

            // 如果沒有被使用 (isInUse == false)，代表它還是個草稿，可以安全地 Wipe and Rewrite
            location = locationRepository.findById(request.getId())
                    .orElseThrow(() -> new RuntimeException("找不到指定的場地進行更新"));

            // 安全地刪除舊座位與分區
            // 先刪除所有舊的座位，再刪除所有舊的分區 (順序不可錯，否則會違反外鍵約束)
            seatRepository.deleteSeatsByLocationId(location.getLocationId());
            zoneRepository.deleteZonesByLocationId(location.getLocationId());
        }

        // 1. 更新/儲存 最高層：場地 (Location)
        location.setName(request.getName());
        location.setAddress(request.getAddress());
        location.setTotalCapacity(request.getTotalCapacity());
        location.setGridMaxX(request.getGridMaxX());
        location.setGridMaxY(request.getGridMaxY());
        location.setRawSvg(request.getRawSvg());
        location.setBoundSvg(request.getBoundSvg());

        Location savedLocation = locationRepository.save(location);

        // 準備一個 List，用來收集所有的 Seat，以便最後批次寫入
        List<Seat> allSeatsToInsert = new ArrayList<>();

        // a. 取得前端傳來的最新 SVG 結構
        String currentBoundSvg = request.getBoundSvg();

        // 2. 拆解第二層：分區 (Zone)
        if (request.getZones() != null) {
            for (ZoneSaveDTO zoneDTO : request.getZones()) {
                // A. 建立並儲存分區實體 (JPA 會自動產生真實的 PK)
                Zone zone = new Zone();
                zone.setLocation(savedLocation); // 綁定剛存好的場地
                zone.setName(zoneDTO.getName());
                zone.setColor(zoneDTO.getColor());

                // b. 存入資料庫，取得帶有「真實 ID」的實體
                Zone savedZone = zoneRepository.save(zone);

                // c. 替換分區ID邏輯：如果是新分區 (前端傳來的 ID 是負數)
                if (currentBoundSvg != null && zoneDTO.getId() != null && zoneDTO.getId() < 0) {
                    // 把 SVG 裡面的 data-zone-id="-1" 換成 data-zone-id="真實ID"
                    String oldTarget = "data-zone-id=\"" + zoneDTO.getId() + "\"";
                    String newTarget = "data-zone-id=\"" + savedZone.getZoneId() + "\"";

                    currentBoundSvg = currentBoundSvg.replace(oldTarget, newTarget);
                }

                // 3. 拆解最底層：座位 (Seat)
                if (zoneDTO.getSeats() != null) {
                    zone.setSeatCount(zoneDTO.getSeats().size());
                    for (SeatSaveDTO seatDTO : zoneDTO.getSeats()) {
                        Seat seat = new Seat();
                        seat.setZone(savedZone);// 綁定剛存好的分區
                        seat.setRowNum(seatDTO.getRowNum());
                        seat.setSeatNum(seatDTO.getSeatNum());
                        seat.setXIndex(seatDTO.getXIndex());
                        seat.setYIndex(seatDTO.getYIndex());

                        // 暫時不存進 DB，先放進集合裡
                        allSeatsToInsert.add(seat);
                    }
                }
            }

            // d. 迴圈結束後，把「更新過真實 ID」的 SVG 重新塞回 Location，最後再存檔
            location.setBoundSvg(currentBoundSvg);
            locationRepository.save(location);
        }
        // 4. 批次寫入 (Batch Insert)
        // 配合 application.properties 的 batch_size=50，光速寫入幾千個座位！
        if (!allSeatsToInsert.isEmpty()) {
            seatRepository.saveAll(allSeatsToInsert);
        }
    }

    /**
     * 實作：查詢場地詳細資訊 (免 N+1 高效組裝模式)
     * 加上 readOnly = true 可以稍微提升資料庫查詢效能
     */
    @Override
    @Transactional(readOnly = true)
    public LocationDetailResponseDTO getLocationDetail(Integer locationId) {

        // 1. 取得 Location 主體 (第 1 次 SQL)
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("找不到指定的場地 (ID: " + locationId + ")"));

        // 2. 取得該場地所有分區 (第 2 次 SQL)
        List<Zone> zones = zoneRepository.findByLocation_LocationId(locationId);

        // 3. 取得該場地所有座位，一次性撈出 (第 3 次 SQL)
        List<Seat> allSeats = seatRepository.findByZone_Location_LocationId(locationId);

        // 4. 利用 Map 將座位依照 zoneId 分群 (記憶體內極速分類)
        Map<Integer, List<Seat>> seatMap = allSeats.stream()
                .collect(Collectors.groupingBy(s -> s.getZone().getZoneId()));

        // 5. 組合：讓 Zone 去裝載屬於自己的 Seat
        List<ZoneResponseDTO> zoneDTOs = zones.stream()
                .map(zone -> ZoneResponseDTO.fromEntity(
                        zone,
                        seatMap.getOrDefault(zone.getZoneId(), List.of())))// 拿不到就給空集合
                .toList();
        // 6. 最終組合：讓 Location 去裝載已經組合好的 Zones
        return LocationDetailResponseDTO.fromEntity(location, zoneDTOs);
    }

    // 取得所有場地列表 (簡要資訊)
    @Override
    public List<LocationSummaryResponseDTO> getAllLocations() {
        // 1. 從資料庫撈出所有場地，並依 ID 由大到小排序 (新場地在最上面)
        List<Location> locations = locationRepository.findAll(
                Sort.by(Sort.Direction.ASC, "locationId"));

        // 2. 透過 Java Stream API，將每一個 Location Entity 轉換成 DTO，打包成 List
        return locations.stream()
                .map(LocationSummaryResponseDTO::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * 實作：切換場地的隱藏/公開狀態 (軟刪除切換)
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void toogleLocationDeletedStatus(Integer locationId) {
        // 1. 找出目標場地，找不到就拋出例外
        Location location = locationRepository.findById(locationId)
                .orElseThrow(() -> new RuntimeException("找不到指定的場地 (ID: " + locationId + ")，無法變更狀態"));

        // 2. 將狀態反轉：如果原本是 true 就變 false，原本是 false 就變 true
        // 這樣一支 API 就能同時搞定「隱藏」與「重新公開」
        boolean currentStatus = location.getIsDeleted();
        location.setIsDeleted(!currentStatus);

        // 3. 儲存回資料庫
        locationRepository.save(location);

        // 可選：在主機主控台印出日誌方便除錯
        System.out.println("場地 ID " + locationId + " 狀態已變更為: " + (location.getIsDeleted() ? "已隱藏" : "已公開"));
    }

}
