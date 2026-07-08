<template>
    <!-- [Admin Dark Theme 適配 by Jason] 還原方式：移除 venue-dark class、
         background-color 改回 #f8fafc（原淺色），並刪除 <style> 末尾的深色適配區塊。 -->
    <div class="container-fluid py-2 d-flex flex-column venue-dark venue-editor-shell"
        style="background-color: var(--tap-bg-base);"
        @mouseup="stopPainting" @mouseleave="stopPainting">

        <div class="border-bottom pb-2 px-3 mb-2">
            <!-- 第一列：標題獨佔一行，右上角提供返回場地列表入口 -->
            <div class="d-flex justify-content-between align-items-center mb-2">
                <h4 class="fw-bold mb-0 text-primary">
                    場地模板編輯器 <span v-if="editMode === 'svg'" class="fs-6 text-muted">(SVG 綁定模式)</span>
                </h4>
                <button class="btn btn-sm btn-outline-secondary fw-semibold text-nowrap flex-shrink-0"
                    @click="goBack('/admin/venues')">
                    <i class="bi bi-arrow-left me-1"></i>返回
                </button>
            </div>

            <!-- 第二列：左側為場館輸入與席次，右側為操作按鈕；空間不足時整組換行而非拆散按鈕 -->
            <div class="d-flex justify-content-between align-items-center flex-wrap gap-2">
                <div class="d-flex align-items-center flex-wrap gap-2">
                    <div class="input-group input-group-sm" style="width: 220px;">
                        <span class="input-group-text text-tap-secondary">場館名稱</span>
                        <input type="text" v-model="locationName" class="form-control" placeholder="例如：台北小巨蛋" />
                    </div>

                    <div class="input-group input-group-sm" style="width: 320px;">
                        <span class="input-group-text text-tap-secondary">場館地址</span>
                        <input type="text" v-model="locationAddress" class="form-control" placeholder="例如：台北市南京東路..." />
                    </div>

                    <span v-if="xLength > 0" class="badge text-bg-primary fs-6">總席次: {{ totalCapacity }} 席</span>
                </div>

                <div class="d-flex gap-2 flex-shrink-0">
                    <button v-if="editMode === 'grid'"
                        class="btn btn-sm btn-outline-primary fw-bold shadow-sm text-nowrap animate-fade-in"
                        @click="enterSvgMode">
                        <i class="bi bi-arrow-right-short me-1"></i>開始 SVG 綁定
                    </button>

                    <template v-else>
                        <button class="btn btn-sm btn-outline-secondary fw-bold shadow-sm text-nowrap"
                            @click="backToGrid">
                            <i class="bi bi-arrow-left-short me-1"></i>返回網格繪製
                        </button>
                        <button class="btn btn-sm btn-primary fw-bold shadow-sm text-white text-nowrap"
                            @click="saveTemplate" :disabled="isSubmitting">
                            <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-1" role="status"
                                aria-hidden="true"></span>
                            <i v-else class="bi bi-save me-1"></i>
                            {{ isSubmitting ? '儲存中...' : '儲存模板' }}
                        </button>
                        <button class="btn btn-sm btn-success fw-bold shadow-sm text-white text-nowrap"
                            @click="saveAsNewTemplate" :disabled="isSubmitting">
                            <i class="bi bi-files me-1"></i>另存新檔
                        </button>
                    </template>
                </div>
            </div>
        </div>

        <div class="row g-2 px-3 flex-grow-1" style="min-height: 0; max-height: 100%;">
            <div class="col-12 col-lg-8 mb-4 mb-lg-0 h-100 venue-canvas-col">
                <div class="card border shadow-sm position-relative d-flex flex-column h-100"
                    style="overflow: hidden; min-height: 0; max-height: 100%;">
                    <template v-if="editMode === 'grid'">
                        <div v-if="xLength === 0 || yLength === 0" class="m-auto text-muted text-center">
                            <h5 class="fw-bold">畫布尚未初始化</h5>
                            <p>請在右側設定場館的最大 X 與 Y 網格數，並點擊產生畫布。</p>
                        </div>
                        <div v-else class="w-100 h-100 d-flex justify-content-center" ref="viewportRef"
                            style="overflow: hidden">
                            <div ref="canvasRef"
                                style="display: block; width: max-content; padding-top: 40px; padding-bottom: 40px">
                                <SeatMap :seats="seatList" :maxX="xLength" :maxY="yLength" @seatDown="handleSeatDown"
                                    @seatEnter="handleSeatEnter" />
                            </div>
                        </div>
                        <button v-if="xLength > 0" class="btn btn-sm btn-light border shadow-sm position-absolute"
                            style="top: 20px; right: 20px; z-index: 10" @click="resetView">
                            <i class="bi bi-arrow-clockwise me-1"></i>重置視角
                        </button>
                    </template>

                    <template v-else>
                        <div class="w-100 h-100 p-4 d-flex align-items-center justify-content-center"
                            style="overflow: auto; background-color: var(--tap-bg-base)">
                            <div v-if="!rawSvgString" class="text-muted text-center">
                                <h5>等待上傳 SVG 原始碼</h5>
                                <p>請在右側貼上由設計師提供的 SVG XML 代碼</p>
                            </div>
                            <div v-else class="svg-interaction-wrapper w-100 text-center" v-html="currentBoundSvg"
                                @click="handleSvgClick" ref="svgContainer"></div>
                        </div>
                    </template>
                </div>
            </div>

            <div class="col-12 col-lg-4 h-100">
                <div class="d-flex flex-column h-100 gap-2" style="min-height: 0; max-height: 100%; overflow-y: auto;">
                    <div v-if="editMode === 'grid'" class="card p-3 border shadow-sm flex-shrink-0">
                        <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                            <h6 class="fw-bold mb-0">Step 1: 畫布控制</h6>
                            <span v-if="xLength > 0" class="badge bg-primary">目前: {{ xLength }} x {{ yLength }}</span>
                        </div>
                        <div v-if="xLength === 0" class="row g-2">
                            <div class="col-6">
                                <label class="form-label small text-muted mb-1">最大欄數 (X)</label>
                                <input v-model="tempX" type="number" class="form-control form-control-sm"
                                    placeholder="例如: 20" />
                            </div>
                            <div class="col-6">
                                <label class="form-label small text-muted mb-1">最大排數 (Y)</label>
                                <input v-model="tempY" type="number" class="form-control form-control-sm"
                                    placeholder="例如: 15" />
                            </div>
                            <div class="col-12">
                                <button class="btn btn-sm btn-outline-primary w-100 text-nowrap"
                                    @click="generateCanvas">產生畫布</button>
                            </div>
                        </div>
                        <div v-else>
                            <div class="row g-2 align-items-center mb-2">
                                <div class="col-8">
                                    <div class="input-group input-group-sm">
                                        <span class="input-group-text bg-light text-muted border-end-0">向右增加</span>
                                        <input v-model="addXCount" type="number" class="form-control text-center px-1"
                                            min="1" />
                                        <span class="input-group-text bg-light text-muted border-start-0">欄</span>
                                    </div>
                                </div>
                                <div class="col-4">
                                    <button class="btn btn-sm btn-primary w-100 fw-bold" @click="extendX">延伸 X</button>
                                </div>
                            </div>
                            <div class="row g-2 align-items-center mb-3">
                                <div class="col-8">
                                    <div class="input-group input-group-sm">
                                        <span class="input-group-text bg-light text-muted border-end-0">向下增加</span>
                                        <input v-model="addYCount" type="number" class="form-control text-center px-1"
                                            min="1" />
                                        <span class="input-group-text bg-light text-muted border-start-0">排</span>
                                    </div>
                                </div>
                                <div class="col-4">
                                    <button class="btn btn-sm btn-primary w-100 fw-bold" @click="extendY">延伸 Y</button>
                                </div>
                            </div>
                            <button class="btn btn-sm btn-outline-danger w-100 mt-1 fw-bold" @click="resetCanvas">
                                <i class="bi bi-trash3 me-1"></i>捨棄並重置整張畫布
                            </button>
                        </div>
                    </div>
                    <div v-else class="card p-3 border shadow-sm flex-shrink-0">
                        <h6 class="fw-bold border-bottom pb-2 mb-3">Step 1: 匯入場地平面圖</h6>
                        <label class="form-label small text-muted">貼上 SVG 原始碼 (XML)</label>
                        <textarea v-model="rawSvgString" class="form-control form-control-sm font-monospace" rows="8"
                            placeholder="&lt;svg ...&gt; ... &lt;/svg&gt;"></textarea>
                        <div class="mt-2 p-2 bg-light rounded text-muted" style="font-size: 11px">
                            <i class="bi bi-lightbulb text-warning me-1"></i>提示：貼上後左側將即時預覽，點擊區塊即可進行綁定。
                        </div>
                    </div>

                    <div class="card p-3 border shadow-sm d-flex flex-column flex-grow-1" style="min-height: 200px">
                        <div
                            class="d-flex justify-content-between align-items-center mb-3 border-bottom pb-2 gap-2 flex-shrink-0">
                            <h6 class="fw-bold mb-0 text-truncate">Step 2: {{ editMode === "grid" ? "建立分區" : "點選要綁定的分區"
                            }}</h6>
                            <button v-if="editMode === 'grid'"
                                class="btn btn-sm btn-outline-primary fw-bold text-nowrap flex-shrink-0"
                                @click="addZone">
                                <i class="bi bi-plus-lg me-1"></i>新增分區
                            </button>
                        </div>

                        <div class="d-flex flex-column gap-2 flex-grow-1 px-2 py-1 custom-scrollbar"
                            style="overflow-y: auto; overflow-x: hidden; min-height: 0;">

                            <div v-if="zoneList.length === 0"
                                class="text-muted small text-center py-4 border rounded bg-light">
                                尚未建立任何分區
                            </div>

                            <div v-else v-for="zone in zoneList" :key="zone.zone_id"
                                class="border rounded p-2 cursor-pointer transition-all d-flex justify-content-between align-items-center zone-card"
                                :class="{
                                    'border-primary border-2 shadow-sm is-selected-zone': currentZoneId === zone.zone_id,
                                    'border-light-subtle': currentZoneId !== zone.zone_id
                                }" @click="
                                    currentTool = 'paint';
                                currentZoneId = zone.zone_id;
                                ">
                                <div class="d-flex align-items-center gap-2 flex-grow-1" style="min-width: 0">
                                    <input type="color" v-model="zone.color" class="zone-color-input flex-shrink-0"
                                        :disabled="zone.name === 'STAGE'" title="點擊選擇分區顏色" @click.stop
                                        @change="syncZoneColor(zone)" />

                                    <input v-model="zone.name" type="text"
                                        class="form-control form-control-sm border-0 fw-bold px-1 bg-transparent"
                                        :style="{
                                            minWidth: '0',
                                            pointerEvents: zone.name === 'STAGE' ? 'none' : 'auto'
                                        }" placeholder="分區名稱" :disabled="zone.name === 'STAGE'" />

                                    <span v-if="zone.name === 'STAGE'"
                                        class="badge bg-dark ms-1 flex-shrink-0 text-nowrap"
                                        style="font-size: 0.7rem;">系統保留</span>
                                </div>

                                <div class="d-flex align-items-center gap-2 flex-shrink-0 ms-2">
                                    <span v-if="zone.name !== 'STAGE'" class="text-primary fw-bold small text-nowrap">{{
                                        getSeatCount(zone.zone_id) }} 席</span>

                                    <button v-if="editMode === 'grid' && zone.name !== 'STAGE'"
                                        class="btn btn-sm btn-icon btn-outline-danger border-0 py-0 px-2"
                                        @click.stop="removeZone(zone.zone_id)">
                                        <i class="bi bi-trash"></i>
                                    </button>
                                </div>
                            </div>

                        </div>
                    </div>

                    <div v-if="editMode === 'grid'" class="card p-3 border shadow-sm flex-shrink-0">
                        <h6 class="fw-bold border-bottom pb-2 mb-3">Step 3: 畫筆工具箱</h6>
                        <div class="btn-group btn-group-sm w-100" role="group">
                            <input type="radio" class="btn-check" name="tools" id="tool-hand" value="hand"
                                v-model="currentTool" />
                            <label class="btn btn-outline-secondary fw-semibold text-nowrap px-2" for="tool-hand">
                                <i class="bi bi-hand-index-thumb me-1"></i>拖曳
                            </label>

                            <input type="radio" class="btn-check" name="tools" id="tool-paint" value="paint"
                                v-model="currentTool" />
                            <label class="btn btn-outline-primary fw-semibold text-nowrap px-2" for="tool-paint">
                                <i class="bi bi-brush me-1"></i>油漆刷
                            </label>

                            <input type="radio" class="btn-check" name="tools" id="tool-erase" value="erase"
                                v-model="currentTool" />
                            <label class="btn btn-outline-danger fw-semibold text-nowrap px-2" for="tool-erase">
                                <i class="bi bi-eraser me-1"></i>橡皮擦
                            </label>
                        </div>
                        <div class="mt-2 text-muted small">
                            操作方式：
                            <span v-if="currentTool === 'hand'">拖曳與縮放地圖</span>
                            <span v-else-if="currentTool === 'paint'">塗佈分區座位</span>
                            <span v-else>消除實體座位</span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </div>

    <!-- 另存新檔命名輸入框（受控模式 ConfirmDialog，帶輸入欄位；預設值＝原名稱+時間戳） -->
    <ConfirmDialog v-model:show="showSaveAsDialog" title="另存為新場地" confirm-text="確定另存" cancel-text="取消" variant="success"
        @confirm="confirmSaveAsNewTemplate" @cancel="showSaveAsDialog = false">
        <div>
            <label class="form-label small text-tap-secondary mb-1">新場地名稱</label>
            <input type="text" v-model="saveAsNewName" class="form-control form-control-sm" placeholder="請輸入新場地名稱" />
            <div v-if="saveAsNameError" class="text-danger small mt-1">{{ saveAsNameError }}</div>
        </div>
    </ConfirmDialog>
</template>

<script setup>
// 在最上方引入Panzoom套件與 nextTick
import Panzoom from "@panzoom/panzoom";
import { ref, nextTick, watch, computed, onMounted } from "vue";
import SeatMap from "@/components/SeatMap.vue";
import axiosapi from "@/plugins/axios";
import { useRouter, useRoute, onBeforeRouteLeave } from "vue-router";
import { useToast } from "@/composables/useToast.js";
import { useConfirm } from "@/composables/useConfirm.js";
import { useGoBack } from "@/composables/useGoBack.js";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";

const route = useRoute();
const router = useRouter();
const toast = useToast();
const { confirm } = useConfirm();
const { goBack } = useGoBack();

// 新增場地ID綁定 (改為 ref，這樣另存新檔時才能順利清空變數)
const locationId = ref(null);
// 新增場地名稱綁定
const locationName = ref("");
//// 新增地址綁定
const locationAddress = ref("");

// 動態計算總容量：排除未綁定分區，以及屬於「STAGE(舞台)」的格子
const totalCapacity = computed(() => {
    // 1. 先從分區清單中，找出所有名稱包含 STAGE 的分區 ID
    const stageZoneIds = zoneList.value
        .filter((z) => z.name.toUpperCase().includes("STAGE"))
        .map((z) => z.zone_id);

    // 2. 計算座位數：必須有綁定分區 (!== null) 且 分區 ID 不在 STAGE 的名單內
    return seatList.value.filter((seat) =>
        seat.zone_id !== null && !stageZoneIds.includes(seat.zone_id)
    ).length;
});

// 控制存檔按鈕的 loading 狀態
const isSubmitting = ref(false);

// --- SVG 模式所需的變數 ---
const editMode = ref("grid"); // 'grid' 或 'svg'
// 右側文字框綁定：管理員最初貼上、永遠乾淨的原始檔
const rawSvgString = ref("");
// 左側畫布真正渲染與互動的工作檔
const currentBoundSvg = ref("");
// 打包用的SVG容器
const svgContainer = ref(null);

// --- 新增：模式切換函式 ---
function enterSvgMode() {
    if (totalCapacity.value === 0) {
        toast.info("請先在畫布上標記分區座位，再進行 SVG 綁定。");
        return;
    }
    editMode.value = "svg";
    currentTool.value = "paint"; // 切換過去後，強制讓工具變成油漆刷
}

async function backToGrid() {
    const ok = await confirm({
        title: "確定要返回嗎？",
        message: "退回網格將會解除目前所有的 SVG 綁定進度！",
        confirmText: "確定返回",
        cancelText: "取消",
        variant: "danger",
    });

    if (ok) {
        // 左側工作檔還原成最純淨的原始底圖，解除所有 data-zone-id 綁定
        currentBoundSvg.value = rawSvgString.value;
        editMode.value = "grid";

        // 關鍵：重新啟動 Panzoom！
        // setupPanzoom 裡面已經有 await nextTick()，所以它會完美等待 DOM 渲染完畢
        setupPanzoom();
    }
}

// --- SVG 點擊綁定邏輯 (Event Delegation) ---
function handleSvgClick(event) {
    // 防呆，必須選擇分區
    if (!currentZoneId.value) {
        toast.info("請先在右側選擇一個分區作為綁定目標");
        return;
    }

    const target = event.target;
    // 排除點到 SVG 背景或文字標籤
    if (target.tagName === "svg" || target.tagName === "text" || target.classList.contains("svg-interaction-wrapper")) return;

    const selectedZone = zoneList.value.find((z) => z.zone_id === currentZoneId.value);
    if (!selectedZone) return;

    // 優化：檢查該區塊是否已經綁定了「目前手上的分區」
    // 注意：getAttribute 回傳的是字串，所以要把 currentZoneId 也轉成字串來比對
    const existingZoneId = target.getAttribute("data-zone-id");

    if (existingZoneId === String(currentZoneId.value)) {
        // 如果相同 -> 執行「清除綁定」
        target.removeAttribute("data-zone-id");
        target.style.fill = "#f1f5f9"; // 恢復 SVG 預設的淺灰底色
        console.log(`🔄 已解除區塊綁定 (移除 Zone ID: ${existingZoneId})`);
        return; // 清除完就結束，不往下跑綁定邏輯
    }

    // 如果不同或尚未綁定 -> 寫入屬性並變更顏色
    target.setAttribute("data-zone-id", selectedZone.zone_id);
    target.style.fill = selectedZone.color;
    target.style.transition = "fill 0.3s ease";
    target.style.cursor = "pointer";

    console.log(`成功將區塊綁定至：${selectedZone.name} (Zone ID: ${selectedZone.zone_id})`);
}

// SVG：監聽右側原始底圖的變化
watch(rawSvgString, (newRawValue) => {
    // 當管理員更換右側原始碼時，左側畫布立刻切換成新的底圖結構
    currentBoundSvg.value = newRawValue;
    console.log("🔄 偵測到原始底圖變更，左側畫布已同步更新結構");
})

// ==========================================
// 模擬從後端讀取舊的場地模板資料 (編輯/檢視模式)
// ==========================================
onMounted(async () => {

    // 1. 動態抓取網址上的 ID
    const targetId = route.params.id; // 假設我們要拿剛剛在 Postman 建立的 1 號場地

    // 2. 判斷是否為編輯模式 (排除無 ID 或 ID 為 'new' 的狀況)
    // 如果你的路由把 /venues/new 跟 /venues/:id/edit 共用同一個組件，這裡就會發揮作用
    const isEditMode = targetId != null && targetId !== 'new';

    // 用 targetId 判斷
    if (isEditMode) {

        try {
            // 1. 用 targetId 向後端發送 GET 請求
            const response = await axiosapi.get(`/api/locations/${targetId}`);

            if (response.data.success) {
                // 拿到我們在 Service 組裝好的 data
                const savedData = response.data.data;

                // 2. 恢復基礎設定
                locationId.value = savedData.id;
                locationName.value = savedData.name; //取得名稱
                locationAddress.value = savedData.address || "";
                xLength.value = savedData.gridMaxX || 10;
                yLength.value = savedData.gridMaxY || 10;
                tempX.value = xLength.value;
                tempY.value = yLength.value;

                // SVG 相關恢復邏輯
                //雙軌分流載入
                if (savedData.rawSvg) {
                    // 右側文字框永遠保持乾淨的原始底圖
                    rawSvgString.value = savedData.rawSvg;
                }

                // 左側優先載入綁定後的工作檔，讓管理員可以繼續編輯
                if (savedData.boundSvg) {
                    currentBoundSvg.value = savedData.boundSvg;
                    editMode.value = 'svg';
                } else if (savedData.rawSvg) {
                    // 如果從未綁定過，左側畫布就先拿原始底圖當預設
                    currentBoundSvg.value = savedData.rawSvg;
                    editMode.value = 'svg'
                }

                let tempIdCounter = 1;
                const loadedSeats = [];
                const loadedZones = [];

                // 3. 鋪設全白地基
                for (let y = 1; y <= yLength.value; y++) {
                    for (let x = 1; x <= xLength.value; x++) {
                        loadedSeats.push({
                            seat_id: `temp_${tempIdCounter++}`,
                            physical_name: `${y} - ${x}`,
                            grid_x_index: x,
                            grid_y_index: y,
                            zone_id: null,
                            color: "#ffffff",
                            is_enabled: false,
                            is_deleted: 1,
                        });
                    }
                }

                // 4. 把資料庫的分區與座位蓋上去
                if (savedData.zones) {
                    let zoneIndex = 0;
                    //如果有 zones 包裹，則打開包裹
                    savedData.zones.forEach((zone) => {
                        const fallbackColor = colorPalette[zoneIndex % colorPalette.length];
                        const finalColor = zone.color || fallbackColor;
                        zoneIndex++;

                        loadedZones.push({
                            zone_id: zone.id,  // 對應後端的 id
                            name: zone.name,
                            color: finalColor,
                        });

                        if (zone.seats) {
                            //如果有 seats 包裹，則打開包裹
                            zone.seats.forEach((seatPos) => {
                                // 透過 xIndex 和 yIndex 找到對應的白格子把它塗色
                                const targetSeat = loadedSeats.find(
                                    (s) => s.grid_x_index === seatPos.xIndex && s.grid_y_index === seatPos.yIndex
                                );
                                //找到對應座位後，把後端ID填上去(save在沒有ID時會自動變成insert)
                                if (targetSeat) {
                                    targetSeat.seat_id = seatPos.id;
                                    targetSeat.zone_id = zone.id;
                                    targetSeat.color = finalColor;
                                    targetSeat.is_deleted = 0;
                                    targetSeat.is_enabled = true;
                                }
                            });
                        }
                    });
                }

                // 檢查後端傳來的分區中，是否包含 STAGE，如果沒有就強制注入一個
                const hasStage = loadedZones.some(z => z.name === "STAGE");
                if (!hasStage) {
                    loadedZones.unshift({
                        zone_id: tempZoneIdCounter--,
                        name: "STAGE",
                        color: "#94a3b8"
                    });
                }

                zoneList.value = loadedZones;
                seatList.value = loadedSeats;

                // 5. 渲染畫布
                await nextTick();
                setupPanzoom();
                isLoaded.value = true;
            } else {
                console.log(response.data);//檢查錯誤訊息
                toast.error(response.data.message || "讀取失敗");
                isLoaded.value = true;
            }
        } catch (error) {
            console.log("讀取場地資料失敗:", error);//檢查錯誤訊息
            toast.error("無法讀取場地資料");
            isLoaded.value = true;
        }
    } else {
        // --- 新增模式：完全空白 ---
        // 把長寬都歸零，讓畫面保持等待狀態
        xLength.value = 0;
        yLength.value = 0;

        // 輸入框預設為 0，讓使用者自己決定長寬
        tempX.value = 0;
        tempY.value = 0;

        editMode.value = "grid";

        // 座位清單保持全空
        seatList.value = [];

        // 預設只給一個 STAGE 分區
        zoneList.value = [
            {
                zone_id: tempZoneIdCounter--,
                name: "STAGE",
                color: "#94a3b8"
            }
        ];
        // 因為畫布是空的，不需要啟動 Panzoom
        isLoaded.value = true;
    }
});

// ==========================================
// 啟動與重置 Panzoom 邏輯
// ==========================================
// 準備給 Panzoom 用的 Ref 與實例變數
const viewportRef = ref(null);
const canvasRef = ref(null);
let panzoomInstance = null;
let wheelHandler = null; // 用來記錄滾輪監聽器，以便隨時清除

async function setupPanzoom() {
    // 必須等待 Vue 把 DOM 真正渲染到畫面上，才能抓到 canvasRef
    await nextTick();

    if (canvasRef.value && viewportRef.value) {
        // 如果有舊的實例，除了銷毀實例，還要「拔掉舊的監聽器」！
        if (panzoomInstance) {
            panzoomInstance.destroy();
            if (wheelHandler) {
                viewportRef.value.removeEventListener("wheel", wheelHandler);
            }
        }
        // 初始化 Panzoom
        panzoomInstance = Panzoom(canvasRef.value, {
            maxScale: 3, // 最大放大 3 倍
            minScale: 0.1, // 最小縮小到 0.1 倍
            startScale: 1, // 初始比例
            cursor: "grab", // 滑鼠游標樣式

            // 動態判定初始狀態：如果是拖曳模式才顯示手掌、才允許拖曳
            cursor: currentTool.value === "hand" ? "grab" : "default",
            disablePan: currentTool.value !== "hand",
            // 讓 Panzoom 放過所有 class 叫做 seat 的元素
            excludeClass: currentTool.value === "hand" ? "allow-drag-dummy" : "seat",
        });

        // 紀錄新的監聽器，並綁定上去
        wheelHandler = panzoomInstance.zoomWithWheel;
        viewportRef.value.addEventListener("wheel", wheelHandler);
    }
    // ==========================================
    // 監聽工具切換，動態改變 Panzoom 行為
    // ==========================================
    watch(currentTool, (newTool) => {
        if (panzoomInstance) {
            if (newTool === "hand") {
                // 切換到拖曳：解鎖移動、游標變手掌、解鎖座位拖曳限制
                panzoomInstance.setOptions({
                    cursor: "grab",
                    disablePan: false,
                    excludeClass: "allow-drag-dummy",
                });
            } else {
                // 切換到筆刷/橡皮擦：鎖定移動、游標變回普通箭頭、恢復座位防拖曳封印
                panzoomInstance.setOptions({
                    cursor: "default",
                    disablePan: true,
                    excludeClass: "seat",
                });
            }
        }
    });
}
// 供「重置視角」按鈕呼叫
function resetView() {
    if (panzoomInstance) {
        panzoomInstance.zoom(1); // 比例回到 1
        panzoomInstance.pan(0, 0); // 座標回到 X:0, Y:0 (即頂部置中)
    }
}

// ==========================================
// 1. 資料大腦 (State)
// ==========================================
// 畫布的實際大小
const xLength = ref(0);
const yLength = ref(0);

// 供右側輸入框暫存用的變數
const tempX = ref(0);
const tempY = ref(0);

const startSeatForRange = ref(null); // 紀錄兩點上色的起點

// 分區清單 (對應 DB 的 zone 表)
// 預設載入時，直接給予一個不可變動的 STAGE 基準區
const zoneList = ref([
    {
        zone_id: -1,
        name: "STAGE",
        color: "#94a3b8",
    }

]);

// 實體座位清單 (對應 DB 的 seat 表)
const seatList = ref([]);

// 目前選用的工具 ('hand' = 拖曳畫面, 'paint' = 畫分區, 'erase' = 拆除座位, 'lock' = 保留席不開放)
const currentTool = ref("hand"); // 預設工具改為 hand
const currentZoneId = ref(null); // 如果工具是 paint，這裡會記錄拿著哪個分區的油漆刷

// ==========================================
// 2. 畫布初始化邏輯
// ==========================================
function generateCanvas() {
    // 防呆：確認輸入的數字合法
    if (tempX.value <= 0 || tempY.value <= 0) {
        toast.error("X 與 Y 必須是大於 0 的數字");
        return;
    }
    //執行畫布繪製
    fillCanvas();
}

// 鋪滿白格子的函式
function fillCanvas() {
    xLength.value = tempX.value;
    yLength.value = tempY.value;
    seatList.value = [];

    let tempIdCounter = 1;

    // 雙重迴圈：鋪滿基礎白色格子
    for (let y = 1; y <= yLength.value; y++) {
        for (let x = 1; x <= xLength.value; x++) {
            seatList.value.push({
                seat_id: `temp_${tempIdCounter++}`, // 暫時的假 ID
                physical_name: `${y},${x}`, // 預設名稱，後續可讓管理員點擊修改
                grid_x_index: x,
                grid_y_index: y,
                zone_id: null, // 尚未綁定分區
                color: "#ffffff", // 預設純白底色，代表空地/走道
                is_enabled: false, // 預設不讓廠商使用 (因為還沒劃分區)
                is_deleted: 1, // 預設為 1 (走道)，等被油漆刷塗到才變成 0 (實體座位)
            });
        }
    }
    setupPanzoom();
}

// ==========================================
// 畫布重置
// ==========================================
async function resetCanvas() {
    const ok = await confirm({
        title: "確定要重置畫布嗎？",
        message: "這將會清空目前畫布上所有的分區座位資料！",
        confirmText: "確定重置",
        cancelText: "取消",
        variant: "danger",
    });
    if (ok) {
        // 畫布的實際大小
        xLength.value = 0;
        yLength.value = 0;

        // 供右側輸入框暫存用的變數
        tempX.value = 0;
        tempY.value = 0;

        // 分區清單 (對應 DB 的 zone 表)
        zoneList.value = [
            {
                zone_id: -1,
                name: "STAGE",
                color: "#94a3b8",
            }
        ];

        // 實體座位清單 (對應 DB 的 seat 表)
        seatList.value = [];

        // 把工具箱與油漆刷也恢復預設狀態
        currentTool.value = "hand";
        currentZoneId.value = null;
        toast.success("畫布已恢復初始狀態");
    }
}

// ==========================================
// 延伸畫布
// ==========================================
// 供畫面輸入「要加幾欄/幾排」用的反應式變數
const addXCount = ref(1);
const addYCount = ref(1);

// 1. 向右延伸 (增加直欄)
function extendX() {
    if (addXCount.value <= 0) return;

    const oldX = xLength.value; // 原本的寬度 (例如: 5)
    const newX = oldX + addXCount.value; // 新的總寬度 (例如: 5 + 3 = 8)

    // 取得目前陣列裡最大的 ID 計數，避免 temp_ID 重複
    let tempIdCounter = seatList.value.length + 1;

    // 外層迴圈：Y 軸（排數）應該從 1 跑 到目前的總排數 (yLength.value)
    for (let y = 1; y <= yLength.value; y++) {
        // 內層迴圈：X 軸（欄數）應該從哪裡開始？跑到哪裡結束？
        // 提示：應該從 (oldX + 1) 開始，一路上升到 (newX)
        for (let x = oldX + 1; x <= newX; x++) {
            // 迴圈內部：把新格子 push 進去
            seatList.value.push({
                seat_id: `temp_${tempIdCounter++}`,
                physical_name: `${y} - ${x}`,
                grid_x_index: x, // 這裡會是 6, 7, 8
                grid_y_index: y, // 這裡會是 1, 2, 3, 4, 5
                zone_id: null,
                color: "#ffffff",
                is_enabled: false,
                is_deleted: 1,
            });
        }
    }
    // 最後核心：把大腦裡的實際總寬度更新成新寬度，SeatMap 才會跟著變形！
    xLength.value = newX;
    // setupPanzoom();
}

// 2. 向下延伸 (增加橫列)
function extendY() {
    if (addYCount.value <= 0) return;

    const oldY = yLength.value; // 原本的高度 (例如: 5)
    const newY = oldY + addYCount.value; // 新的總高度 (例如: 5 + 3 = 8)

    // 取得目前陣列裡最大的 ID 計數，避免 temp_ID 重複
    let tempIdCounter = seatList.value.length + 1;
    //Y 軸從 oldY + 1 開始往下蓋
    for (let y = oldY + 1; y <= newY; y++) {
        // X 軸從 1 到目前的總寬度
        for (let x = 1; x <= xLength.value; x++) {
            // 迴圈內部：把新格子 push 進去
            seatList.value.push({
                seat_id: `temp_${tempIdCounter++}`,
                physical_name: `${y} - ${x}`,
                grid_x_index: x, // 這裡會是 1, 2, 3, 4, 5
                grid_y_index: y, // 這裡會是 6, 7, 8
                zone_id: null,
                color: "#ffffff",
                is_enabled: false,
                is_deleted: 1,
            });
        }
    }
    // 最後核心：把大腦裡的實際總高度更新成新高度，SeatMap 才會跟著變形！
    yLength.value = newY;
    //setupPanzoom();
}

// ==========================================
// 3. 畫布點擊互動邏輯
// ==========================================
//當滑鼠點擊左側畫布

//狀態檢查：是否正在按住滑鼠左鍵
const isPaintingDrag = ref(false);

// 統一的著色執行中心 (isDrag 參數決定要不要 Toggle)
function applyPaintAction(seat, isDrag) {
    // 防護罩：如果是拖曳模式，點到座位不反應
    if (currentTool.value === "hand") return;

    //先檢查現在的模式

    // 工具 1：橡皮擦模式
    if (currentTool.value === "erase") {
        seat.zone_id = null;
        seat.color = "#ffffff";
        seat.is_deleted = 1; // 變回走道
        seat.is_enabled = false;
        return; // 擦完就結束這回合，不要往下跑油漆刷的邏輯了
    }

    // 工具 2：油漆刷模式
    if (currentTool.value === "paint") {
        // 防呆機制：如果管理員還沒選擇任何分區油漆刷，就先擋下來
        if (!currentZoneId.value) {
            toast.info("請先在右側選擇一個分區作為油漆刷！");
            return;
        }

        // 用 .find() 從 zoneList.value 中，
        // 找出 zone_id 等於 currentZoneId.value 的那個分區物件
        const selectedZone = zoneList.value.find((z) => z.zone_id === currentZoneId.value);

        if (!selectedZone) return; // 沒找到就中斷

        // 判斷邏輯：如果這個座位已經被塗成現在手上的分區了
        if (seat.zone_id === currentZoneId.value) {
            //檢查是否在拖曳中，拖曳中 -> 遇到同色就直接略過 (return) 不取消
            if (isDrag) return;
            //不是拖曳 -> 取消著色
            // 取消綁定：恢復成「白色走道」的預設值
            seat.zone_id = null; // 取消分區綁定
            seat.color = "#ffffff"; // 恢復成白色
            seat.is_deleted = 1; // 標記為走道 (不存在的實體座位)
            seat.is_enabled = false; // 走道當然不能給廠商賣
        } else {
            // 與油漆刷不同顏色 -> 塗上新的分區、顏色，並轉換狀態為實體座位
            seat.zone_id = selectedZone.zone_id; //變成該分區
            seat.color = selectedZone.color; //塗上分區顏色
            seat.is_deleted = 0; // 變成實體座位
            seat.is_enabled = true; // 預設開放給廠商使用
        }
    }
}

// 按照兩點XY座標差執行區域上色
function applyRangePaintAction(endSeat) {
    // 防呆機制：如果管理員還沒選擇任何分區油漆刷，就先擋下來
    if (!currentZoneId.value) {
        Swal.fire("請先選擇分區", "請先在右側點擊一個分區卡片作為油漆刷！", "warning");
        return;
    }
    const selectedZone = zoneList.value.find((z) => z.zone_id === currentZoneId.value);
    if (!selectedZone) return;

    // 算出矩形邊界
    const minX = Math.min(startSeatForRange.value.grid_x_index, endSeat.grid_x_index);
    const maxX = Math.max(startSeatForRange.value.grid_x_index, endSeat.grid_x_index);
    const minY = Math.min(startSeatForRange.value.grid_y_index, endSeat.grid_y_index);
    const maxY = Math.max(startSeatForRange.value.grid_y_index, endSeat.grid_y_index);

    // 【資深心法】效能關鍵：不要在雙重迴圈裡用 .find() 找座位（那會變成 O(N^2)）
    // 直接遍歷一次 flat 陣列，過濾出在矩形範圍內的座位進行批量修改 O(N)
    seatList.value.forEach((seat) => {
        if (
            seat.grid_x_index >= minX &&
            seat.grid_x_index <= maxX &&
            seat.grid_y_index >= minY &&
            seat.grid_y_index <= maxY
        ) {
            // 工具如果是 erase 則清空，如果是 paint 則上色
            if (currentTool.value === "erase") {
                seat.zone_id = null;
                seat.color = "#ffffff";
                seat.is_deleted = 1;
                seat.is_enabled = false;
            } else if (currentTool.value === "paint") {
                seat.zone_id = selectedZone.zone_id;
                seat.color = selectedZone.color;
                seat.is_deleted = 0;
                seat.is_enabled = true;
            }
        }
    });
    // 【重要】染色完畢後，記得把起點清空，這樣下一次 Shift+點擊 才能重新定位新起點！
    startSeatForRange.value = null;
    // 染色完畢，清空起點
    Swal.fire("提示", "區塊批量上色完成", "success");
}

//監控滑鼠狀態
// 1. 滑鼠「按下去」的瞬間 (相當於原來的單擊，也是拖曳的起點)
function handleSeatDown(seat, event) {
    if (currentTool.value === "hand") return; // 拖曳畫布模式不處理

    // 檢查是否觸發兩點區塊上色 (例如按住 Shift 鍵且已有起點)
    if (event && event.shiftKey && startSeatForRange.value) {
        applyRangePaintAction(seat);
        return;
    }
    // 如果沒有按住 Shift，或是第一次點擊，則將目前點擊設為起點
    startSeatForRange.value = seat;

    isPaintingDrag.value = true; // 宣告：開始進入拖曳塗色狀態
    applyPaintAction(seat, false); // 第一下視為單擊，傳入 isDrag = false (允許切換/取消)
}
// 2. 滑鼠「滑進」另一個座位時
function handleSeatEnter(seat) {
    // 只有在「按住左鍵」的狀態下經過座位，才觸發塗色
    if (isPaintingDrag.value) {
        applyPaintAction(seat, true); // 拖曳經過，傳入 isDrag = true (強制覆蓋，不取消)
    }
}
// 3. 滑鼠「放開」時
function stopPainting() {
    isPaintingDrag.value = false; // 宣告：結束拖曳塗色狀態
}

// ==========================================
// 平台共用色票庫
// ==========================================
const colorPalette = [
    "#ef4444", // 1. 鮮紅 (Red)
    "#f97316", // 2. 活力橘 (Orange)
    "#eab308", // 3. 琥珀黃 (Yellow) - 提升在淺色背景下的明度與對比
    "#10b981", // 4. 翡翠綠 (Emerald)
    "#06b6d4", // 5. 天空青 (Cyan)
    "#3b82f6", // 6. 皇家藍 (Blue)
    "#6366f1", // 7. 靛藍 (Indigo)
    "#8b5cf6", // 8. 迷幻紫 (Purple)
    "#d946ef", // 9. 魅力洋紅 (Fuchsia)
    "#ec4899", // 10. 玫瑰粉紅 (Pink)
];

// ==========================================
// 新增分區 (Zone) 邏輯
// ==========================================

// 專屬於 Zone 的負數計數器 (從 -2 開始，保留 -1 給預設的 STAGE)
let tempZoneIdCounter = -2;

function addZone() {
    // 依序取用色票
    const nextColor = colorPalette[zoneList.value.length % colorPalette.length];

    const tempId = tempZoneIdCounter--;

    // 直接推入 zoneList 陣列，給予預設名稱
    zoneList.value.push({
        zone_id: tempId,
        name: "新分區",
        color: nextColor,
    });

    // 自動幫管理員切換成「油漆刷」並拿起這個新分區
    currentTool.value = "paint";
    currentZoneId.value = tempId;
}

// 分區換色：透過原生色票選擇器改色後，同步刷新該分區已塗的座位顏色
function syncZoneColor(zone) {
    seatList.value.forEach((seat) => {
        if (seat.zone_id === zone.zone_id) {
            seat.color = zone.color;
        }
    });
}

// 刪除分區邏輯
function removeZone(targetId) {
    // 防呆：檢查是否試圖刪除 STAGE
    const targetZone = zoneList.value.find(z => z.zone_id === targetId);
    if (targetZone && targetZone.name === "STAGE") {
        toast.error("STAGE 為座位距離計算的核心基準點，不可刪除！");
        return;
    }

    // 1. 清除畫面上已經被塗成這個區域的座位
    seatList.value.forEach((seat) => {
        //檢查待刪除分區有沒有綁定的座位
        if (seat.zone_id === targetId) {
            seat.zone_id = null;
            seat.color = "#ffffff";
            seat.is_enabled = false;
            seat.is_deleted = 1;
        }
    });

    // 2. 從陣列中過濾掉這筆票種 (只保留 ID 不等於 targetId 的區域)
    zoneList.value = zoneList.value.filter((z) => z.zone_id !== targetId);

    // 3. 防呆機制：如果刪除的剛好是目前拿在手上的油漆刷，就把手清空
    if (currentZoneId.value === targetId) {
        currentZoneId.value = null;
    }
}

// //===============================
// // 計算某個票種目前被綁定了幾個座位
// //===============================
function getSeatCount(zoneId) {
    // 從 seats 陣列中過濾出 zone_id 相符的座位，然後回傳長度
    return seatList.value.filter((seat) => seat.zone_id === zoneId).length;
}

// ==========================================
// 儲存與打包資料
// ==========================================
async function saveTemplate() {
    // 1. 基礎防呆檢查
    if (!locationName.value.trim()) {
        toast.error("請輸入場館名稱！");
        return;
    }
    if (zoneList.value.length === 0) {
        toast.error("請至少建立一個分區！");
        return;
    }
    if (totalCapacity.value === 0) {
        toast.error("請至少在畫布上標記一個實體座位！");
        return;
    }

    // 🌟 打開 Loading 狀態，按鈕會變成轉圈圈並鎖死
    isSubmitting.value = true;

    try {
        // 2. 開始打包 JSON
        // 過濾掉沒用的白格子 (走道)，只留下有被塗色的座位
        const validSeats = seatList.value.filter((seat) => seat.zone_id !== null);

        // =========================================================
        // 步驟 A：找出 STAGE 邊界，以此為中心基準
        // =========================================================
        const stageSeats = validSeats.filter((seat) => {
            const zone = zoneList.value.find((z) => z.zone_id === seat.zone_id);
            return zone && zone.name.toUpperCase().includes("STAGE");
        });

        let stageMinX = Infinity, stageMaxX = -Infinity;
        let stageMinY = Infinity, stageMaxY = -Infinity;

        if (stageSeats.length > 0) {
            stageSeats.forEach(s => {
                if (s.grid_x_index < stageMinX) stageMinX = s.grid_x_index;
                if (s.grid_x_index > stageMaxX) stageMaxX = s.grid_x_index;
                if (s.grid_y_index < stageMinY) stageMinY = s.grid_y_index;
                if (s.grid_y_index > stageMaxY) stageMaxY = s.grid_y_index;
            });
        }

        // =========================================================
        // 步驟 B：將所有座位歸類到四大方位 (NORTH, SOUTH, EAST, WEST)
        // =========================================================
        const regionGroups = { 'NORTH': [], 'SOUTH': [], 'EAST': [], 'WEST': [] };

        validSeats.forEach(seat => {
            const zone = zoneList.value.find(z => z.zone_id === seat.zone_id);
            if (!zone || zone.name.toUpperCase().includes("STAGE")) return;

            let region = 'SOUTH'; // 預設南方

            // 📍 優先判定：讀取管理者在 Zone 名稱上標註的方位
            if (zone.name.includes('北')) region = 'NORTH';
            else if (zone.name.includes('南')) region = 'SOUTH';
            else if (zone.name.includes('東')) region = 'EAST';
            else if (zone.name.includes('西')) region = 'WEST';
            else if (stageSeats.length > 0) {
                // 📍 幾何判定：如果沒寫名字，根據相對舞台的座標自動分類
                if (seat.grid_y_index < stageMinY) region = 'NORTH';
                else if (seat.grid_y_index > stageMaxY) region = 'SOUTH';
                else if (seat.grid_x_index < stageMinX) region = 'WEST';
                else if (seat.grid_x_index > stageMaxX) region = 'EAST';
            }

            seat.region = region; // 貼上大區標籤
            regionGroups[region].push(seat); // 放入對應的大區陣列中
        });

        // =========================================================
        // 步驟 C：四大區獨立運算引擎 (全區排號累加)
        // =========================================================
        const globalNumberingMap = {}; // 用來記錄最終每一格座標對應的 { rowNum, seatNum }

        ['NORTH', 'SOUTH', 'EAST', 'WEST'].forEach(region => {
            const regionSeats = regionGroups[region];
            if (regionSeats.length === 0) return;

            let getRowCoord, getSeatCoord, sortRowDir, sortSeatDir;

            // 核心設定：定義各方位的「排」與「號」是看 X 還是 Y，以及起點方向
            // 原則：第一排永遠是最靠近舞台的那一排！
            switch (region) {
                case 'NORTH':
                    getRowCoord = (s) => s.grid_y_index;  // 北區的排看 Y 軸
                    getSeatCoord = (s) => s.grid_x_index; // 座位號看 X 軸
                    sortRowDir = -1; // 越靠近舞台 (Y越大) 排數越小 -> 第一排
                    sortSeatDir = 1; // 座位號左到右累加
                    break;
                case 'SOUTH':
                    getRowCoord = (s) => s.grid_y_index;
                    getSeatCoord = (s) => s.grid_x_index;
                    sortRowDir = 1;  // 越靠近舞台 (Y越小) 排數越小 -> 第一排
                    sortSeatDir = 1;
                    break;
                case 'WEST':
                    getRowCoord = (s) => s.grid_x_index;  // 西區的排看 X 軸 (垂直發展)
                    getSeatCoord = (s) => s.grid_y_index; // 座位號看 Y 軸
                    sortRowDir = -1; // 越靠近舞台 (X越大) 排數越小 -> 第一排
                    sortSeatDir = 1; // 座位號上到下累加
                    break;
                case 'EAST':
                    getRowCoord = (s) => s.grid_x_index;
                    getSeatCoord = (s) => s.grid_y_index;
                    sortRowDir = 1;  // 越靠近舞台 (X越小) 排數越小 -> 第一排
                    sortSeatDir = 1;
                    break;
            }

            // 1. 抓出該大區內所有不重複的「排座標」，並依照方向排序
            const uniqueRows = [...new Set(regionSeats.map(getRowCoord))].sort((a, b) => (a - b) * sortRowDir);

            // 2. 針對每一排進行處理
            uniqueRows.forEach((rowVal, rowIndex) => {
                const rowNum = rowIndex + 1; // 大區內排數累加 (跨分區接軌的關鍵)

                // 抓出該排所有的座位，依照座位號方向排序
                const seatsInRow = regionSeats.filter(s => getRowCoord(s) === rowVal);
                seatsInRow.sort((a, b) => (getSeatCoord(a) - getSeatCoord(b)) * sortSeatDir);

                // 為這排的位子發放座位號 (seatNum)
                seatsInRow.forEach((seat, seatIndex) => {
                    globalNumberingMap[`${seat.grid_x_index}_${seat.grid_y_index}`] = {
                        rowNum: rowNum,
                        seatNum: seatIndex + 1
                    };
                });
            });
        });

        // =========================================================
        // 步驟 D：將計算好的結果組裝成後端需要的 ZonePayload
        // =========================================================
        const zonePayload = zoneList.value.map((zone) => {
            // 舞台不需要送出實體座位讓前台購買
            if (zone.name.toUpperCase().includes("STAGE")) return null;

            const seatsInThisZone = validSeats.filter((seat) => seat.zone_id === zone.zone_id);
            if (seatsInThisZone.length === 0) return null;

            return {
                id: zone.zone_id,
                name: zone.name,
                color: zone.color,
                seats: seatsInThisZone.map((seat) => {
                    // 從字典中提取剛剛算好的排與號
                    const numbering = globalNumberingMap[`${seat.grid_x_index}_${seat.grid_y_index}`];
                    return {
                        rowNum: numbering ? numbering.rowNum : 1,
                        seatNum: numbering ? numbering.seatNum : 1,
                        xIndex: seat.grid_x_index,
                        yIndex: seat.grid_y_index,
                    };
                }),
            };
        }).filter(Boolean);

        // =========================================================
        // 最後步驟：打包與送出 API
        // =========================================================
        // --- 取得最新工作檔 SVG ---
        // 1. 取得「已經被點擊綁定過」的最新 SVG 結構
        let boundSvg = rawSvgString.value; //預設先拿原始字串兜底
        if (editMode.value === "svg" && svgContainer.value) {
            // 重要：從畫面上抓取左側最新被塗色、注入 data-zone-id 的 DOM 結構
            boundSvg = svgContainer.value.innerHTML;
        }

        // 3. 最終大包裝 (Location)
        const payload = {
            id: locationId.value,
            name: locationName.value,
            address: locationAddress.value,// 把 address 放進 payload，這裡可以先給個空字串或綁定另一個 ref 輸入框
            totalCapacity: totalCapacity.value,
            gridMaxX: xLength.value,
            gridMaxY: yLength.value,
            zones: zonePayload,
            // 雙軌分流打包，送給後端儲存
            rawSvg: rawSvgString.value, // 右側框裡那包絕對乾淨的原始碼
            boundSvg: boundSvg, // 左側畫面上最新互動完的工作檔
        };

        // 印出結果檢查
        console.log("準備送給後端的 JSON Payload:", JSON.stringify(payload, null, 2));

        // 4. 真正呼叫後端 API

        // 發送 POST 請求 (假設你的 Spring Boot 跑在 8080，若是用 proxy 請調整路徑)
        //使用 axiosapi的baseURL: import.meta.env.VITE_BACKEND_API
        //其中 VITE_BACKEND_API=http://localhost:8080
        const response = await axiosapi.post("/api/locations", payload);

        // 判斷我們後端寫好的 success 欄位
        if (response.data.success) {
            toast.success(response.data.message || "場地模板已儲存");
            isDirty.value = false;
            // 儲存成功後跳轉回場地列表頁
            router.push('/admin/venues');
        } else {
            // 後端擋下來的錯誤 (例如資料格式不對)
            toast.error(response.data.message || "儲存失敗，請檢查輸入內容");
        }
    } catch (error) {
        // 網路斷線或伺服器 500 錯誤
        console.log("API 請求失敗:", error);
        toast.error("無法連線到伺服器，請稍後再試");
    } finally {
        // 🌟 無論成功或失敗，最後一定要把 Loading 狀態解除
        isSubmitting.value = false;
    }

}

// ==========================================
// 另存為新場地 (Clone)
// ==========================================
// 另存新檔命名對話框的狀態（需求 #15：改為可編輯輸入框，預設值＝原名稱+時間戳）
const showSaveAsDialog = ref(false);
const saveAsNewName = ref("");
const saveAsNameError = ref("");

async function saveAsNewTemplate() {
    // 對齊全站「無 SweetAlert2」規範：彈出可編輯命名輸入框，
    // 預設值沿用原本「原名稱 + 時間戳記」的產生邏輯，讓使用者確認/修改後才儲存。

    // 基礎防呆檢查
    if (!locationName.value.trim()) {
        toast.error("請先輸入場館名稱，再進行另存新檔！");
        return;
    }

    // 取得當下時間，並格式化為 YYYYMMDD_HHmm (例如：20260704_1120)
    const now = new Date();
    const year = now.getFullYear();
    const month = String(now.getMonth() + 1).padStart(2, '0');
    const day = String(now.getDate()).padStart(2, '0');
    const hour = String(now.getHours()).padStart(2, '0');
    const minute = String(now.getMinutes()).padStart(2, '0');
    const timeStamp = `${year}${month}${day}_${hour}${minute}`;

    // 預設新名稱為「原名稱 + 時間戳記」，開放使用者於彈窗中編輯
    saveAsNewName.value = `${locationName.value.trim()} (${timeStamp})`;
    saveAsNameError.value = "";
    showSaveAsDialog.value = true;
}

// 使用者於命名輸入框確認後才真正執行另存
async function confirmSaveAsNewTemplate() {
    const newName = saveAsNewName.value.trim();
    if (!newName) {
        saveAsNameError.value = "請輸入新場地名稱，才能另存新檔";
        return; // 輸入為空時不可確認，保留對話框開啟並顯示提示
    }

    showSaveAsDialog.value = false;

    // 1. 套用使用者確認（或編輯）後的新名稱
    locationName.value = newName;

    // 2. 清空當前追蹤的 ID，讓 payload.id 變成 null，
    //    後端便會判定這是一筆【全新場地】而執行 Insert
    locationId.value = null;

    // 3. 直接重用既有的儲存邏輯送出（isSubmitting 防連點沿用 saveTemplate 內部邏輯）
    await saveTemplate();
}

const isDirty = ref(false);
const isLoaded = ref(false);

// 監聽各項編輯欄位與數據以追蹤是否修改
watch(
    [locationName, locationAddress, rawSvgString, xLength, yLength],
    () => {
        if (isLoaded.value) {
            isDirty.value = true;
        }
    }
);

watch(
    zoneList,
    () => {
        if (isLoaded.value) {
            isDirty.value = true;
        }
    },
    { deep: true }
);

watch(
    seatList,
    () => {
        if (isLoaded.value) {
            isDirty.value = true;
        }
    },
    { deep: true }
);

// 路由離開守衛：如有未儲存的變更，彈出防呆確認提示
onBeforeRouteLeave(async (to, from, next) => {
    if (isDirty.value) {
        const ok = await confirm({
            title: "確定要離開嗎？",
            message: "您有尚未儲存的編輯內容，離開將會遺失目前所有的編輯進度！",
            confirmText: "確定離開",
            cancelText: "取消",
            variant: "danger",
        });
        if (ok) {
            next();
        } else {
            next(false);
        }
    } else {
        next();
    }
});
</script>

<style scoped>
/* 分區色票選擇器：包裝原生 <input type="color"> 成小巧的圓形色票 */
.zone-color-input {
    width: 22px;
    height: 22px;
    padding: 0;
    border: 1px solid var(--tap-border);
    border-radius: 50%;
    background: transparent;
    cursor: pointer;
    overflow: hidden;
}

.zone-color-input:disabled {
    cursor: not-allowed;
    opacity: 0.85;
}

/* 移除各瀏覽器原生色塊的內距與邊框，讓圓形填滿 */
.zone-color-input::-webkit-color-swatch-wrapper {
    padding: 0;
}

.zone-color-input::-webkit-color-swatch {
    border: none;
    border-radius: 50%;
}

.zone-color-input::-moz-color-swatch {
    border: none;
    border-radius: 50%;
}

/* 隱藏 number 輸入框右側預設的上下箭頭 */
input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

/* ========================================================================
   【響應式外殼 — 由 Jason 加掛】
   桌機（≥lg）：鎖定單一視窗高度、overflow:hidden，維持左右兩欄各自捲動。
   手機（<lg）：解除固定高度改自然流動，讓上下堆疊的工具區不被裁掉
   （原本 height:calc(100vh-100px)+overflow:hidden 會使堆疊後的工具區被推出畫面消失）。
   還原：把此區塊刪除，並將根 <div> 的 inline style 補回
   height: calc(100vh - 100px); overflow: hidden;（class venue-editor-shell 亦可移除）。
   ======================================================================== */
.venue-editor-shell {
    height: auto;
    overflow: visible;
}

@media (min-width: 992px) {
    .venue-editor-shell {
        height: calc(100vh - 100px);
        overflow: hidden;
    }
}

/* 手機：左右兩欄改上下堆疊時，給畫布區一個有界高度，工具區自然接在下方 */
@media (max-width: 991.98px) {
    .venue-canvas-col {
        min-height: 60vh;
    }
}

/* ========================================================================
   【Admin Dark Theme 適配 — 由 Jason 加掛】
   說明：僅重新著色版面外觀（卡片/文字/邊框/輸入框/浮動鈕），
   未更動任何功能邏輯（Panzoom / 塗佈 / 分區 / 儲存）與座位資料色。
   scoped 樣式不會外溢到子元件 SeatMap，故座位外觀維持原設計。

   === 完整還原成原本淺色調，共需復原 3 處（缺一不可）===
   1. 本 <style> 區塊：將下方整段刪除。
   2. 根 <div> 的 class：移除 "venue-dark"。
   3. 根 <div> 的 inline style：background-color 由 var(--tap-bg-base) 改回 #f8fafc。
   （另：height 由 100vh 改為 100% 是為了嵌入 Admin 後台內容區，與配色無關，
     如需獨立整頁可改回 100vh。）
   ======================================================================== */

/* 卡片 / 畫布底色：白 → 深色 surface 層 */
.venue-dark :deep(.bg-white) {
    background-color: var(--tap-bg-surface) !important;
}

/* 次級底色（input-group 標籤、空狀態占位）：淺灰 → 懸停層 */
.venue-dark :deep(.bg-light) {
    background-color: var(--tap-bg-hover) !important;
}

/* 次要文字：Bootstrap text-muted 在深色上對比不足 → 改用設計系統次要文字色 */
.venue-dark :deep(.text-muted) {
    color: var(--tap-text-secondary) !important;
}

/* 各式邊框與分隔線融入深色 */
.venue-dark :deep(.border),
.venue-dark :deep(.border-bottom),
.venue-dark :deep(.border-top),
.venue-dark :deep(.border-light-subtle) {
    border-color: var(--tap-border) !important;
}

/* 浮動「重置視角」按鈕：淺色 btn-light → 深色玻璃感，融入畫布 */
.venue-dark :deep(.btn-light) {
    background-color: var(--tap-bg-hover);
    border-color: var(--tap-border) !important;
    color: var(--tap-text-primary);
}

.venue-dark :deep(.btn-light:hover) {
    background-color: var(--tap-border);
    color: var(--tap-text-primary);
}

/* 分區卡片內的刪除鈕（btn-light + text-danger）：保留紅色圖示 */
.venue-dark :deep(.btn-light.text-danger) {
    color: var(--bs-danger) !important;
    background-color: transparent;
}

/* ========================================================================
   【分區卡片選中狀態優化 — 深色主題適配】
   ======================================================================== */

/* 基礎卡片動畫過渡 */
.zone-card {
    transition: all 0.25s ease-in-out !important;
}

/* 當卡片被選中（激活）時的進階特效 */
.venue-dark :deep(.is-selected-zone) {
    /* 1. 強制讓邊框使用亮藍色，並加粗至 2px */
    border-color: #3b82f6 !important;
    border-width: 2px !important;

    /* 2. 核心：在深色背景下注入亮藍色的「微光外發光」效果，這是視覺變明顯的關鍵 */
    /* 改成雙層光暈：第一層讓邊緣更銳利，第二層做外擴光暈 */
    box-shadow: 0 0 0 1px #3b82f6, 0 0 12px rgba(59, 130, 246, 0.6) !important;

    /* 3. 微幅提升被選中卡片的背景亮度，讓它從深色背景中「浮」出來 */
    background-color: var(--tap-bg-hover) !important;

    /* 4. 加上一點點輕微的放大位移，創造按鈕被按下去的實體回饋感 */
    transform: translateY(-1px) scale(1.01);
}

/* 額外加分：當滑鼠懸停在未選中的分區卡片上時，給予溫和的邊框提示 */
.venue-dark :deep(.zone-card:hover:not(.is-selected-zone)) {
    border-color: rgba(148, 163, 184, 0.5) !important;
    background-color: rgba(255, 255, 255, 0.03) !important;
}
</style>
