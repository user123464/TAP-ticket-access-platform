<template>
    <div class="container-fluid d-flex flex-column ticket-type-container">
        <!-- Header -->
        <div class="d-flex flex-column flex-md-row justify-content-between align-items-stretch align-items-md-center gap-2 border-bottom pb-2 px-2 mb-2 flex-shrink-0">
            <div class="d-flex align-items-center gap-3 flex-wrap">
                <h4 class="fw-bold mb-0 text-primary">
                    演唱會票種與區域綁定
                </h4>

                <span class="badge bg-secondary fs-6">
                    場館模板：{{ locationName }}
                </span>
            </div>

            <button class="btn btn-primary fw-bold px-4 w-100 w-md-auto" style="max-width: 280px;" @click="saveAllocation">
                <i class="bi bi-floppy"></i> 儲存並發布售票配置
            </button>
        </div>

        <!-- Main -->
        <div class="row flex-grow-1 gx-3" style="min-height:0;">

            <!-- 左側 -->
            <div class="col-lg-8 h-lg-100-left">

                <div class="bg-white rounded border shadow-sm h-100 d-flex flex-column overflow-hidden">

                    <!-- 已移除智慧畫布說明 -->

                    <div class="flex-grow-1 d-flex justify-content-center align-items-center overflow-hidden">

                        <div class="svg-interaction-wrapper w-100 h-100 text-center" v-html="svgContent"
                            @click="handleSvgClick" ref="svgContainer"></div>

                    </div>

                </div>

            </div>

            <!-- 右側 -->
            <div class="col-lg-4 h-lg-100-right">

                <div class="bg-white rounded border shadow-sm h-100 d-flex flex-column p-3" style="min-height:0;">

                    <!-- 歷史配置 -->
                    <div v-if="historySessions.length > 0" class="mb-3 p-3 bg-light rounded border flex-shrink-0">
                        <label class="form-label small fw-bold text-muted mb-1">
                            <i class="bi bi-arrow-clockwise"></i>快速匯入歷史配置
                        </label>

                        <select class="form-select form-select-sm" v-model="selectedHistorySessionId"
                            @change="handleImportSelect">
                            <option value="" disabled>
                                -- 選擇同主題的其他場次 --
                            </option>

                            <option v-for="session in historySessions" :key="session.sessionId"
                                :value="session.sessionId">
                                {{ session.sessionTitle }}
                            </option>
                        </select>

                        <div class="small text-muted mt-2" style="font-size:12px;">
                            <i class="bi bi-lightbulb"></i> 匯入後將覆蓋目前畫面，存檔時會產生全新票種，不影響舊資料。
                        </div>

                    </div>

                    <!-- 標題 -->
                    <div
                        class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3 flex-shrink-0">

                        <div>

                            <h6 class="fw-bold mb-1">
                                設定營運票種
                            </h6>

                            <p class="small text-muted mb-0">
                                請先建立票種，再點選卡片作為油漆色塊
                            </p>

                        </div>

                        <button class="btn btn-sm btn-outline-primary text-nowrap flex-shrink-0" @click="addTicketType">
                            + 新增
                        </button>

                    </div>

                    <!-- Scroll -->
                    <div class="flex-grow-1 overflow-auto pe-2 custom-scrollbar">

                        <div v-if="ticketTypes.length === 0"
                            class="text-center text-muted border rounded bg-light py-5">
                            <i class="bi bi-ticket-perforated"></i>尚未建立任何販售票種
                        </div>

                        <div v-else v-for="ticket in ticketTypes" :key="ticket.ticket_type_id"
                            class="ticket-card border rounded p-3 mb-3 position-relative" :class="currentActiveTicketTypeId === ticket.ticket_type_id
                                ? 'border-primary border-2 shadow'
                                : 'border-light-subtle bg-light-subtle'
                                " @click="currentActiveTicketTypeId = ticket.ticket_type_id">

                            <div class="position-absolute top-0 start-0 h-100" :style="{
                                width: '6px',
                                backgroundColor: ticket.color,
                                borderRadius: '4px 0 0 4px'
                            }"></div>

                            <div class="ms-3">

                                <div class="row g-2 align-items-center mb-3">

                                    <div class="col-7 d-flex align-items-center gap-2">

                                        <input type="color" v-model="ticket.color"
                                            class="color-picker-circle flex-shrink-0" @change="updateSvgColors">

                                        <input v-model="ticket.name"
                                            class="form-control form-control-sm border-0 bg-transparent fw-bold"
                                            placeholder="票種名稱">

                                    </div>

                                    <div class="col-5">

                                        <div class="input-group input-group-sm">

                                            <span class="input-group-text border-0 bg-transparent text-primary fw-bold">
                                                票價
                                            </span>

                                            <input type="number" v-model="ticket.price"
                                                class="form-control form-control-sm border-0 bg-transparent text-end fw-bold text-primary">

                                        </div>

                                    </div>

                                </div>

                                <div class="d-flex justify-content-between align-items-center border-top pt-2">

                                    <div class="form-check mb-0">

                                        <input class="form-check-input" type="checkbox" v-model="ticket.is_enabled"
                                            :id="'check-' + ticket.ticket_type_id">

                                        <label class="form-check-label small" :for="'check-' + ticket.ticket_type_id">
                                            開放購買
                                        </label>

                                    </div>

                                    <div class="d-flex align-items-center gap-3">

                                        <span class="badge bg-primary-subtle text-primary">
                                            已劃位
                                            {{ getBoundSeatCount(ticket.ticket_type_id) }}
                                            席
                                            ({{ getBoundZoneCount(ticket.ticket_type_id) }}區)
                                        </span>

                                        <button class="btn btn-sm text-danger border-0 p-0"
                                            @click.stop="removeTicketType(ticket.ticket_type_id)">
                                            <i class="bi bi-trash"></i>
                                        </button>

                                    </div>

                                </div>

                            </div>

                        </div>

                    </div>

                </div>

            </div>

        </div>

    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, nextTick } from "vue";
import Swal from "sweetalert2";
import axiosapi from "@/plugins/axios";
import { useRouter, useRoute } from "vue-router";
import Panzoom from "@panzoom/panzoom";

const router = useRouter();
// 宣告 route 物件
const route = useRoute();
// ==========================================
// 1. 核心狀態 (State)
// ==========================================
//目前正在編輯的場次(實務上通常從 vue-router 的 route.params 取得)
const currentSessionId = ref(parseInt(route.params.sessionId, 10));
//目前在編輯的主辦方
const currentOrg = ref(route.params.organizerId)

const ticketTypes = ref([]);
const currentActiveTicketTypeId = ref(null); // 目前選中的「油漆桶」票種 ID

// 儲存「分區 ID」與「票種 ID」的對照字典 (Key: zoneId, Value: ticketTypeId)
const zoneToTicketTypeMap = ref({});

// 儲存各分區座位容量的對照表 (Key: zoneId, Value: seatCount)
const zoneCapacities = ref({});

const colorPalette = [
    "#ef4444", "#f97316", "#eab308", "#84cc16", "#22c55e",
    "#10b981", "#14b8a6", "#06b6d4", "#0ea5e9", "#3b82f6",
    "#6366f1", "#8b5cf6", "#a855f7", "#d946ef", "#ec4899",
    "#f43f5e", "#78716c", "#52525b", "#3f3f46", "#1e293b"
];
let tempIdCounter = -1;

const historySessions = ref([]);
const selectedHistorySessionId = ref("");

// 儲存場館名稱的變數，預設給給載入中提示
const locationName = ref("載入中...")
// ==========================================
// 2. SVG 相關
// ==========================================
// 載入SVG時的渲染
const svgContainer = ref(null);
const svgContent = ref("");// 存放後端回傳的 SVG 字串

// 根據對照表，為 SVG 區塊重新上色
function updateSvgColors() {
    // 防呆：如果畫布還沒準備好就退出
    if (!svgContainer.value) return;

    // 1. 取得畫布內所有帶有 data-zone-id 屬性的區塊 (通常是 <path> 或 <polygon>)
    const zones = svgContainer.value.querySelectorAll('[data-zone-id]');

    zones.forEach(zone => {
        // 取得這個區塊的 ID
        const zoneId = parseInt(zone.getAttribute('data-zone-id'), 10);
        // 去我們的對照表查：這個區塊被綁定了哪個票種？
        const boundTicketTypeId = zoneToTicketTypeMap.value[zoneId];

        // 關鍵：強制清除 SVG 原本帶來的 fill 屬性，確保我們的設定能生效
        zone.removeAttribute('fill');
        zone.style.removeProperty('fill');

        if (boundTicketTypeId) {
            // 如果有綁定，去票種陣列裡把顏色找出來
            const ticketType = ticketTypes.value.find(t => t.ticket_type_id === boundTicketTypeId);
            if (ticketType) {
                // 塗上票種專屬的顏色
                zone.style.fill = ticketType.color;
                // 把透明度設為 1 (完全不透明)，飽和度拉滿
                zone.style.opacity = '1';
            }
        } else {
            // 未綁定的區塊，強制變成明顯的淺灰色
            zone.style.fill = '#e5e7eb'; // ⚠️ 修正：SVG 必須用 fill
            zone.style.opacity = '1';    // ⚠️ 修正：不要設透明，讓它飽和
        }
        // 順便加上游標樣式，提示使用者這個區塊是可以點擊的
        zone.style.cursor = 'pointer';
        // 加上一點 transition 讓顏色切換有動畫感 (選用)
        zone.style.transition = 'fill 0.3s ease';
    });
}

let panzoomInstance = null;

function initPanzoom() {
    nextTick(() => {
        if (!svgContainer.value) return;
        const svgElement = svgContainer.value.querySelector("svg");
        if (!svgElement) return;

        if (panzoomInstance) {
            panzoomInstance.destroy();
        }

        panzoomInstance = Panzoom(svgElement, {
            maxScale: 8,
            minScale: 0.5,
            canvas: true, // Ensure CSS transform is applied correctly on SVG children/attributes
            beforeMouseDown: (event) => {
                // 只允許滑鼠中鍵 (button === 1) 進行拖曳平移，左鍵等其餘鍵不觸發平移
                return event.button !== 1;
            }
        });

        // Enable wheel zooming on the parent container
        const parent = svgContainer.value.parentElement;
        parent.removeEventListener("wheel", handleWheelZoom);
        parent.addEventListener("wheel", handleWheelZoom, { passive: false });

        // 防止中鍵按下時瀏覽器彈出預設的「自動滾動」功能
        parent.removeEventListener("mousedown", handleMiddleMouseDown);
        parent.addEventListener("mousedown", handleMiddleMouseDown);
    });
}

function handleWheelZoom(event) {
    if (panzoomInstance) {
        event.preventDefault();
        panzoomInstance.zoomWithWheel(event);
    }
}

function handleMiddleMouseDown(event) {
    if (event.button === 1) {
        event.preventDefault();
    }
}

// SVG 點擊綁定
function handleSvgClick(event) {
    const target = event.target;
    const zoneIdStr = target.getAttribute("data-zone-id");

    // 排除點擊到舞台、文字或其他無效裝飾路徑
    if (!zoneIdStr) return;

    const zoneId = parseInt(zoneIdStr, 10);

    // 防呆：確認有先點選右側票種
    if (!currentActiveTicketTypeId.value) {
        Swal.fire("請選擇票種", "請先在右側點擊一個票種卡片，再點擊地圖區塊進行顏色綁定！", "warning");
        return;
    }

    const selectedTicket = ticketTypes.value.find((t) => t.ticket_type_id === currentActiveTicketTypeId.value);
    if (!selectedTicket) return;

    // Toggle 邏輯：如果該區塊已經綁定了目前選中的票種 -> 解除綁定
    if (zoneToTicketTypeMap.value[zoneId] === currentActiveTicketTypeId.value) {
        delete zoneToTicketTypeMap.value[zoneId];
        target.style.fill = "#e5e7eb"; // 變回未綁定預設淺灰
        target.style.opacity = "1";    // 確保透明度恢復
    } else {
        // 建立綁定或覆蓋舊綁定
        zoneToTicketTypeMap.value[zoneId] = currentActiveTicketTypeId.value;
        target.style.fill = selectedTicket.color; // 渲染成票種顏色
    }
}

// 統計該票種綁定了幾個 SVG 區塊
function getBoundZoneCount(ticketTypeId) {
    return Object.values(zoneToTicketTypeMap.value).filter((id) => id === ticketTypeId).length;
}

// 統計該票種目前綁定了多少個「實體座位」
function getBoundSeatCount(ticketTypeId) {
    let totalSeats = 0;
    // 遍歷目前的「畫布綁定對照表」
    Object.keys(zoneToTicketTypeMap.value).forEach((zoneIdStr) => {
        const zoneId = parseInt(zoneIdStr, 10);
        // 如果這個分區綁定的是目標票種
        if (zoneToTicketTypeMap.value[zoneId] === ticketTypeId) {
            // 直接去字典查這個分區有幾個位子
            totalSeats += zoneCapacities.value[zoneId] || 0;
        }
    });
    return totalSeats;
}

// ==========================================
// 3. 票種增刪邏輯
// ==========================================
function addTicketType() {
    const nextColor = colorPalette[ticketTypes.value.length % colorPalette.length];
    const newId = tempIdCounter--;
    ticketTypes.value.push({
        ticket_type_id: newId,
        name: "新營運票種",
        price: 0,
        color: nextColor,
        is_enabled: true,
    });
    currentActiveTicketTypeId.value = newId; // 自動聚焦新票種
}

function removeTicketType(targetId) {
    // 1. 同步清除對照表裡綁定該票種的分區
    Object.keys(zoneToTicketTypeMap.value).forEach((zoneId) => {
        if (zoneToTicketTypeMap.value[zoneId] === targetId) {
            delete zoneToTicketTypeMap.value[zoneId];
            // 同步將畫布上的該 SVG 元素染回灰色
            if (svgContainer.value) {
                const path = svgContainer.value.querySelector(`[data-zone-id="${zoneId}"]`);
                if (path) path.style.fill = "#f1f5f9";
            }
        }
    });

    // 2. 從陣列移除
    ticketTypes.value = ticketTypes.value.filter((t) => t.ticket_type_id !== targetId);
    if (currentActiveTicketTypeId.value === targetId) currentActiveTicketTypeId.value = null;
}

// ==========================================
// 4. 初始化與存檔打包
// ==========================================

// 向後端要該場次綁定資料
async function fetchAllocations() {
    try {
        const response = await axiosapi.get(`/api/ticket-type/allocations/${currentSessionId.value}`);

        if (response.data.success) {
            // 解構出後端傳來的大禮包 解構出新增的 zoneCapacities 順便把 locationName 拿出來
            const { boundSvg,
                mappings,
                zoneCapacities: fetchedCapacities,
                locationName: fetchLocationName } = response.data.data;

            // 將場館名稱寫入 ref
            locationName.value = fetchLocationName || "未命名場館";
            // 將後端的 分區-座位數量 字典直接塞給 Vue 的響應式變數
            zoneCapacities.value = fetchedCapacities || {};

            // 1. 把 SVG 塞進畫布 (假設你有一個 ref 叫 svgContainerHtml 綁在畫面上)
            // 你原本用來渲染 SVG 的變數，請把它替換成 boundSvg
            if (boundSvg) {
                // 這裡替換成實際綁定 v-html 的變數名稱
                svgContent.value = boundSvg;
            }

            // 2. 重組綁定資料 (迴圈的對象改成 mappings)
            const uniqueTicketTypes = new Map();
            const newZoneMap = {};

            // 遍歷後端傳來的每一筆綁定紀錄
            mappings.forEach(alloc => {
                // A. 重建 SVG 塗色用的對照表 (zoneId -> ticketTypeId)
                newZoneMap[alloc.zoneId] = alloc.ticketTypeId;

                // B. 重建右側的票種清單 (利用 Map 確保票種不會重複新增)
                if (!uniqueTicketTypes.has(alloc.ticketTypeId)) {
                    uniqueTicketTypes.set(alloc.ticketTypeId, {
                        ticket_type_id: alloc.ticketTypeId,
                        name: alloc.ticketTypeName,
                        price: alloc.price,
                        color: alloc.color,
                        is_enabled: alloc.isEnabled
                    });
                }
            });
            // 將轉換好的資料塞回 Vue 的響應式變數
            ticketTypes.value = Array.from(uniqueTicketTypes.values());
            zoneToTicketTypeMap.value = newZoneMap;

            // 確保 DOM 更新後，重新執行 SVG 塗色邏輯
            nextTick(() => {
                updateSvgColors(); // 呼叫你原本寫好的 SVG 上色 function
                initPanzoom();
            });

            // 防呆：先檢查陣列有沒有東西
            if (ticketTypes.value.length > 0) {
                // 預設將油漆刷拿在第一個票種上
                currentActiveTicketTypeId.value = ticketTypes.value[0].ticket_type_id;
            } else {
                currentActiveTicketTypeId.value = null;
            }
            console.log(response.data.data);

        } else {
            Swal.fire("讀取失敗", response.data.message, "error");
        }
    } catch (error) {
        const errorMsg = error.response?.data?.message || error.message || "未知錯誤";
        Swal.fire("無法取得票種配置", errorMsg, "warning");
        console.error("無法取得票種配置:", error);
    }
}


// 畫面掛載時觸發讀取
onMounted(() => {
    fetchAllocations();
    fetchHistorySessions();
});

onUnmounted(() => {
    if (panzoomInstance) {
        panzoomInstance.destroy();
    }
});

//  儲存/發布配置
async function saveAllocation() {
    if (ticketTypes.value.length === 0) {
        Swal.fire("缺少資料", "請至少建立一個票種！", "warning");
        return;
    }

    // 防呆：檢查是否所有 SVG 區塊都已經綁定票種
    if (svgContainer.value) {
        const totalZones = svgContainer.value.querySelectorAll("[data-zone-id]").length;
        const boundZones = Object.keys(zoneToTicketTypeMap.value).length;

        if (boundZones < totalZones) {
            Swal.fire({
                icon: "warning",
                title: "尚未設定完成",
                text: `還有 ${totalZones - boundZones} 個分區「未綁定」票種！請確認地圖上所有區塊皆已填色。`,
                confirmButtonColor: "#3b82f6",
            });
            return; // 擋下存檔動作
        }
    }

    // 組裝後端 DTO 結構
    // 廠商不用傳送任何 seat 資訊，只要把票種陣列打包，並在裏面附加它綁定了哪些 zoneId 即可
    const payload = {
        sessionId: currentSessionId.value,// 使用當前的 Session ID
        ticketTypes: ticketTypes.value.map((t) => ({
            // 只有真正大於 0 的資料庫 ID 才傳數字，否則傳 null 給後端新增
            ticketTypeId: (t.ticket_type_id && t.ticket_type_id > 0) ? parseInt(t.ticket_type_id, 10) : null,
            name: t.name,
            price: t.price,
            isEnabled: t.is_enabled,
            color: t.color,
            //  重點：過濾並收集所有綁定到此票種的分區 ID 陣列
            boundZoneIds: Object.keys(zoneToTicketTypeMap.value)
                .filter((zoneId) => zoneToTicketTypeMap.value[zoneId] === t.ticket_type_id)
                .map((zoneId) => parseInt(zoneId, 10)),
        })),
    };

    // 偵錯秘訣：在發送請求前，把 Payload 印出來看！
    // 仔細看印出來的 JSON，確認 sessionId, ticketTypeId, 裡面有沒有混入 {...} 
    console.log("送出的 Payload 結構：", JSON.parse(JSON.stringify(payload)));

    // 串接後端 API
    try {
        Swal.fire({
            title: '儲存中...',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        const response = await axiosapi.post(`/api/ticket-type/allocations`, payload);

        if (response.data.success) {
            Swal.fire("儲存成功", "已成功將分區規則發布！後端將自動批次更新實體座位票價。", "success");

            await publishTicketInventory();

            // 存檔成功後，建議重新拉取一次資料，確保前端拿到的 ticket_type_id 是最新的 (特別是剛新增的票種)
            // fetchAllocations();
            // 存檔成功，返回主題列表
            router.push(`/org/${currentOrg.value}/themes`);
        } else {
            Swal.fire("儲存失敗", response.data.message, "error");
        }
    } catch (error) {
        console.log("存檔失敗:", error);
        // 如果是 @Valid 擋下來的錯誤，可以從 error.response.data 拿錯誤訊息
        const errorMsg = error.response?.data?.message || "無法連線到伺服器";
        Swal.fire("系統錯誤", errorMsg, "error");
    }
}

// ==========================================
// 5. 匯入舊場次配置邏輯 (ID 剝離術)
// ==========================================

// 獲取歷史場次清單
async function fetchHistorySessions() {
    try {
        const response = await axiosapi.get(`/api/ticket-type/allocations/history/${currentSessionId.value}`);
        if (response.data.success) {
            historySessions.value = response.data.data;
        }
    } catch (error) {
        console.error("無法取得歷史場次清單:", error);
    }
}

// 監聽選單切換事件
function handleImportSelect() {
    if (selectedHistorySessionId.value) {
        Swal.fire({
            title: '確定要匯入嗎？',
            text: "這將會覆蓋您目前畫面上未存檔的進度！",
            icon: 'warning',
            showCancelButton: true,
            confirmButtonColor: '#3b82f6',
            cancelButtonColor: '#6c757d',
            confirmButtonText: '確定匯入',
            cancelButtonText: '取消'
        }).then((result) => {
            if (result.isConfirmed) {
                // 呼叫我們之前寫好的魔法函式！
                importFromOldSession(selectedHistorySessionId.value);
            } else {
                // 如果按取消，把選單切回預設值
                selectedHistorySessionId.value = "";
            }
        });
    }
}

// 選中舊場次後，觸發這個函式並傳入舊場次的 ID
async function importFromOldSession(oldSessionId) {
    try {
        Swal.fire({
            title: '載入舊配置中...',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        // 1. 去後端撈取舊場次的配置
        const response = await axiosapi.get(`/api/ticket-type/allocations/${oldSessionId}`);

        if (response.data.success) {
            const { mappings } = response.data.data;

            // 2. 準備一本「舊 ID -> 新假 ID」的翻譯字典
            const idTranslationMap = {};
            const importedTicketTypes = [];
            const newZoneMap = {};

            // 3. 開始拆解並「洗掉實體 ID」
            mappings.forEach(alloc => {
                const oldId = alloc.ticketTypeId;

                // 如果這個舊票種還沒被我們翻譯過，就幫它產生一個全新的負數 ID
                if (!idTranslationMap[oldId]) {
                    const newTempId = tempIdCounter--; // 給予新的假 ID
                    idTranslationMap[oldId] = newTempId; // 記錄到翻譯字典

                    // 把洗白過的新票種塞進陣列
                    importedTicketTypes.push({
                        ticket_type_id: newTempId, // ✨ 關鍵：使用負數假 ID
                        name: alloc.ticketTypeName,
                        price: alloc.price,
                        color: alloc.color,
                        is_enabled: alloc.isEnabled
                    });
                }

                // 4. 將原本的分區綁定，改為指向新的假 ID
                newZoneMap[alloc.zoneId] = idTranslationMap[oldId];
            });

            // 5. 將洗白後的資料正式覆蓋到畫面狀態上
            ticketTypes.value = importedTicketTypes;
            zoneToTicketTypeMap.value = newZoneMap;

            // 6. 重新渲染 SVG 顏色
            nextTick(() => {
                updateSvgColors();
                initPanzoom();
            });

            // 預設拿起第一支油漆刷
            if (ticketTypes.value.length > 0) {
                currentActiveTicketTypeId.value = ticketTypes.value[0].ticket_type_id;
            } else {
                currentActiveTicketTypeId.value = null;
            }

            Swal.fire("匯入成功", "已載入舊配置！請注意，存檔後將產生全新的票種，不會影響舊場次。", "success");

        } else {
            Swal.fire("匯入失敗", response.data.message, "error");
        }
    } catch (error) {
        console.error("匯入舊配置失敗:", error);
        Swal.fire("系統錯誤", "無法匯入舊配置", "error");
    }
}

// 正式發布庫存，未來要移到場次的 "公開" 按鈕上
// 觸發「發布並生成實體票券」的函式
async function publishTicketInventory() {
    try {
        // 跳出加載中動畫，因為產票牽涉到大批量寫入，需要給使用者提示
        Swal.fire({
            title: '正在具現化實體票券庫存...',
            text: '系統正在為每個座位產生安全憑證，請稍候。',
            allowOutsideClick: false,
            didOpen: () => Swal.showLoading()
        });

        // 打包我們剛建好的 Record DTO 結構
        const packet = {
            sessionId: currentSessionId.value // 動態從網址或 ref 取得的場次 ID
        };
        // 呼叫你剛剛寫好的 Controller 端點
        const response = await axiosapi.post('/api/ticket/generate-inventory', packet);

        if (response.data.success) {
            await Swal.fire({
                icon: 'success',
                title: '發布成功！',
                text: '所有實體座位已成功具現化為票券庫存，場次已進入可售狀態。',
                confirmButtonColor: '#3b82f6'
            });

            // ***臨時塞入 Redis 預熱功能***
            try {
                // 💡 加入阻擋操作的 Loading 動畫
                Swal.fire({
                    title: '預熱中...',
                    text: '正在將票券載入快取伺服器，請勿關閉視窗。',
                    allowOutsideClick: false,
                    didOpen: () => Swal.showLoading()
                });

                const warmup = await axiosapi.post(`/api/ticket/redis/warmup/${currentSessionId.value}`)
                if (warmup.data.success) {
                    await Swal.fire({
                        icon: 'success',
                        title: '預熱成功！',
                        text: '預熱完畢，準備販售',
                        confirmButtonColor: '#3b82f6'
                    });
                } else {
                    // 若後端回傳失敗，替換成錯誤訊息
                    await Swal.fire('預熱失敗', warmup.data.message, 'error');
                }
            } catch (error) {
                console.log("預熱發生錯誤:", error);
                await Swal.fire('預熱異常', '快取伺服器無回應，請聯絡系統管理員。', 'error');
            }
            //*****Redis 預熱功能結束******   

            // 成功後完美降落，跳轉回主題列表頁面
            router.push(`/org/${currentOrg.value}/themes`);
        } else {
            Swal.fire('發布失敗', response.data.message, 'error');
        }
    } catch (error) {
        console.error('產票系統發生錯誤:', error);
        const errorMsg = error.response?.data?.message || '無法連線到產票伺服器';
        Swal.fire('系統錯誤', errorMsg, 'error');
    }
}
</script>

<style scoped>
.ticket-card {
    transition: all 0.2s ease-in-out;
}

.ticket-card:hover {
    transform: translateY(-2px);
}

.cursor-pointer {
    cursor: pointer;
}

.svg-interaction-wrapper :deep(path):hover {
    filter: brightness(0.95);
    stroke: #3b82f6 !important;
    stroke-width: 2px !important;
}

input[type="number"]::-webkit-inner-spin-button,
input[type="number"]::-webkit-outer-spin-button {
    -webkit-appearance: none;
    margin: 0;
}

/* 美化調色盤的捲動條 */
.custom-scrollbar::-webkit-scrollbar {
    width: 6px;
}

.custom-scrollbar::-webkit-scrollbar-track {
    background: #f1f5f9;
    border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb {
    background: #cbd5e1;
    border-radius: 4px;
}

.custom-scrollbar::-webkit-scrollbar-thumb:hover {
    background: #94a3b8;
}

/* ==========================================
   強制釋放 SVG 的寬度限制，讓它隨容器放大，並保持比例
   ========================================== */
.svg-interaction-wrapper :deep(svg) {
    width: 100% !important;
    max-width: 900px !important;
    height: auto !important;
}

/* 自訂圓形色票輸入框外觀 */
.color-picker-circle {
    width: 24px;
    height: 24px;
    padding: 0;
    border: 2px solid #ffffff;
    border-radius: 50%;
    cursor: pointer;
    box-shadow: 0 0 0 1px #cbd5e1;
    /* 外圈淺灰邊線，增加立體感 */
    background-color: transparent;
    outline: none;
}

/* 隱藏不同瀏覽器自帶的內部小框線，並強制內部色塊也變成圓形 */
.color-picker-circle::-webkit-color-swatch-wrapper {
    padding: 0;
}

.color-picker-circle::-webkit-color-swatch {
    border: none;
    border-radius: 50%;
}

.color-picker-circle::-moz-color-swatch {
    border: none;
    border-radius: 50%;
}

/* 隱藏不同瀏覽器自帶的內部小框線，並強制內部色塊也變成圓形 */
.color-picker-circle::-webkit-color-swatch-wrapper {
    padding: 0;
}

.color-picker-circle::-webkit-color-swatch {
    border: none;
    border-radius: 50%;
}

.color-picker-circle::-moz-color-swatch {
    border: none;
    border-radius: 50%;
}

.ticket-type-container {
    background: #f8fafc;
    padding: 16px;
    height: auto;
    overflow: visible;
}
@media (min-width: 992px) {
    .ticket-type-container {
        height: calc(100vh - 115px);
        overflow: hidden;
    }
    .h-lg-100-left, .h-lg-100-right {
        height: 100% !important;
    }
}
@media (max-width: 991.98px) {
    .h-lg-100-left {
        height: 55vh !important;
        min-height: 380px;
        margin-bottom: 16px;
    }
    .h-lg-100-right {
        height: auto !important;
        min-height: auto;
    }
}
</style>
