<template>
    <div class="container py-4">
        <div class="row">

            <div class="col-12 col-lg-8 mb-4">
                <div class="bg-white rounded border shadow-sm p-3 p-lg-4 d-flex flex-column position-relative h-100">

                    <div class="flex-grow-1 d-flex align-items-center justify-content-center bg-light rounded p-4"
                        style="min-height: 650px;" :style="{ pointerEvents: bookingStage === 'svg' ? 'auto' : 'none' }">
                        <div class="svg-consumer-wrapper w-100 text-center svg-transition" v-html="locationSvg"
                            @click="handleSvgClick" @mousemove="handleSvgMouseMove" @mouseleave="handleSvgMouseLeave"
                            ref="svgContainer" :class="{ 'is-ready': isSvgReady }"></div>
                    </div>
                    <div v-if="showTooltip && bookingStage === 'svg'"
                        class="position-fixed bg-dark text-white p-3 rounded shadow-lg"
                        style="z-index: 1100; pointer-events: none; min-width: 200px"
                        :style="{ top: tooltipY + 'px', left: tooltipX + 'px' }">
                        <div class="fw-bold border-bottom border-secondary pb-2 mb-2 text-info">📍 {{
                            currentTooltipData.zoneName }}</div>
                        <div class="small mb-1 text-secondary">剩餘票種：</div>
                        <div v-for="(ticket, id) in currentTooltipData.tickets" :key="id"
                            class="d-flex justify-content-between small mb-1">
                            <span>{{ ticket.name }} (${{ ticket.price }})</span>
                            <span :class="ticket.available > 0 ? 'text-success fw-bold' : 'text-danger'">
                                剩 {{ ticket.available }} 席
                            </span>
                        </div>
                    </div>

                    <div v-if="bookingStage === 'grid'"
                        class="position-absolute top-0 start-0 w-100 h-100 d-flex align-items-center justify-content-center z-3 animate-fade-in p-2 p-md-4"
                        style="background-color: rgba(0, 0, 0, 0.4); backdrop-filter: blur(4px)"
                        @click="backToSvgStage">
                        <div class="bg-white d-flex flex-column shadow-lg slide-up-overlay seat-modal-container h-100"
                            style="border-radius: 12px; overflow: hidden" @click.stop>

                            <div class="d-flex flex-wrap justify-content-between align-items-center p-2 p-sm-3 border-bottom bg-light gap-2"
                                style="min-height: 70px; height: auto;">
                                <h5 class="fw-bold mb-0 text-primary text-truncate-custom flex-grow-1" :title="currentZoneName"
                                    style="min-width: 0;">
                                    {{ currentZoneName }}
                                </h5>
                                <div class="d-flex align-items-center gap-2 gap-sm-3 flex-shrink-0">
                                    <div class="bg-white border border-secondary-subtle rounded shadow-sm px-2 px-sm-3 d-flex align-items-center justify-content-center"
                                        style="height: 38px; min-width: 130px; color: #475569;">
                                        <span class="fw-bold transition-all"
                                            :class="hoveredSeat ? 'text-dark' : 'text-muted opacity-50'"
                                            style="letter-spacing: 1px; font-variant-numeric: tabular-nums; font-size: 14px;">
                                            {{ hoveredSeat ? hoveredSeat.physical_row : '--' }} 排 {{ hoveredSeat ?
                                                hoveredSeat.physical_seat : '--' }} 號
                                        </span>
                                    </div>
                                    <button class="btn btn-outline-secondary btn-sm fw-bold px-2 px-sm-3 border-0 flex-shrink-0"
                                        @click="backToSvgStage">
                                        ✕ 關閉
                                    </button>
                                </div>
                            </div>

                            <div class="flex-grow-1 d-flex flex-column align-items-center p-3 overflow-auto">
                                <div class="w-100 flex-grow-1" style="overflow: auto; padding: 10px">
                                    <SeatMap :seats="filteredSeats" :maxX="activeZoneMaxX" :maxY="activeZoneMaxY"
                                        @seatDown="handleSeatSelect" @seatEnter="handleSeatEnter" />
                                </div>
                            </div>
                        </div>
                    </div>
                </div>
            </div>

            <div class="col-12 col-lg-4">
                <div class="sticky-top" style="top: 24px">
                    <div class="bg-white rounded border shadow-sm p-3 mb-3">
                        <div class="d-flex justify-content-between align-items-center border-bottom pb-2 mb-3">
                            <h6 class="fw-bold mb-0 text-secondary">剩餘空位</h6>
                            <span class="badge bg-success animate-pulse">即時更新</span>
                        </div>

                        <div class="d-flex flex-column gap-2">
                            <div v-for="ticket in ticketTypes" :key="ticket.ticket_type_id"
                                @mouseenter="handleTicketHover(ticket.ticket_type_id)" @mouseleave="handleTicketLeave"
                                @click="handleTicketClick(ticket.ticket_type_id)"
                                class="d-flex justify-content-between align-items-center p-2 rounded border-start border-4 transition-all"
                                :class="getGlobalTicketCount(ticket.ticket_type_id) === 0 ? 'bg-light opacity-75 grayscale' : 'bg-white shadow-sm cursor-pointer hover-scale'"
                                :style="{ borderStartColor: getGlobalTicketCount(ticket.ticket_type_id) === 0 ? '#94a3b8' : ticket.color }">
                                <div>
                                    <span class="fw-bold d-block"
                                        :class="getGlobalTicketCount(ticket.ticket_type_id) === 0 ? 'text-muted' : 'text-dark'">
                                        {{ ticket.name }}
                                    </span>
                                    <span class="small fw-bold"
                                        :class="getGlobalTicketCount(ticket.ticket_type_id) > 0 ? 'text-success' : 'text-danger'">
                                        {{ getGlobalTicketCount(ticket.ticket_type_id) > 0 ? `剩餘
                                        ${getGlobalTicketCount(ticket.ticket_type_id)} 席` : "已售罄" }}
                                    </span>
                                </div>
                                <span class="fw-bold"
                                    :class="getGlobalTicketCount(ticket.ticket_type_id) === 0 ? 'text-muted' : 'text-primary'">
                                    NT$ {{ ticket.price.toLocaleString() }}
                                </span>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>

        <div class="position-fixed bottom-0 end-0 m-4 animate-fade-in" style="z-index: 1040;">
            <button class="btn rounded-pill shadow-lg px-4 py-3 d-flex align-items-center gap-2 transition-all"
                :class="selectedSeats.length > 0 ? 'btn-danger' : 'btn-secondary'"
                :disabled="selectedSeats.length === 0" style="border: none;" @click="showCartModal = true">
                <i class="bi bi-cart3 fs-5"></i>
                <span class="fw-bold fs-5">前往結帳</span>
                <span class="badge bg-white rounded-circle ms-2 d-flex align-items-center justify-content-center"
                    style="width: 28px; height: 28px; font-size: 14px;"
                    :class="selectedSeats.length > 0 ? 'text-danger' : 'text-secondary'">
                    {{ selectedSeats.length }}
                </span>
            </button>
        </div>

        <div v-if="showCartModal"
            class="position-fixed top-0 start-0 w-100 h-100 d-flex justify-content-center align-items-center animate-fade-in"
            style="background-color: rgba(0, 0, 0, 0.6); z-index: 1050; backdrop-filter: blur(2px);"
            @click="showCartModal = false">

            <div class="bg-white rounded-3 shadow-lg d-flex flex-column slide-up-overlay" style="width: 420px;"
                @click.stop>
                <div class="p-3 border-bottom position-relative text-center">
                    <h5 class="fw-bold mb-0 text-dark">結帳清單</h5>
                    <button
                        class="btn text-muted position-absolute top-50 end-0 translate-middle-y p-0 me-3 border-0 fs-5"
                        style="background: transparent;" @click="showCartModal = false">
                        ✕
                    </button>
                </div>

                <div class="overflow-auto p-3" style="background-color: #f8fafc; height: 380px;">
                    <div v-if="selectedSeats.length === 0"
                        class="text-muted text-center h-100 d-flex align-items-center justify-content-center small">
                        購物車是空的
                    </div>

                    <div v-else v-for="seat in selectedSeats" :key="seat.seat_id"
                        class="d-flex justify-content-between align-items-center border-bottom border-light bg-white p-3 mb-2 rounded shadow-sm"
                        style="height: 75px;">
                        <div>
                            <div class="fw-bold text-dark mb-1">{{ seat.physical_row }} 排 - {{ seat.physical_seat }} 號
                            </div>
                            <div class="small fw-bold" :style="{ color: seat.ticketColor }">{{ seat.ticketName }}</div>
                        </div>
                        <div class="d-flex align-items-center gap-3">
                            <span class="fw-bold text-dark fs-6">NT${{ seat.price.toLocaleString() }}</span>
                            <button class="btn btn-sm text-secondary p-0 border-0 fs-4"
                                @click="removeSeatSelection(seat)">
                                ✕
                            </button>
                        </div>
                    </div>
                </div>

                <div class="p-4 border-top bg-white rounded-bottom">
                    <div class="d-flex justify-content-between align-items-center mb-4 border-bottom pb-3">
                        <span class="fw-bold text-dark">總計 :</span>
                        <h4 class="fw-bold text-danger mb-0">${{ totalPrice.toLocaleString() }}</h4>
                    </div>
                    <button class="btn w-100 fw-bold py-2 shadow-sm fs-5"
                        style="background-color: #1e293b; color: #fff; letter-spacing: 2px;" @click="submitOrder">
                        確認付款
                    </button>
                </div>
            </div>
        </div>

    </div>
</template>

<script setup>
import { ref, computed, onMounted, nextTick, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router"; // 引入路由跳轉
import { useTicketCartStore } from "@/stores/ticketCartStore"; //引入購票pinia
import SeatMap from "@/components/SeatMap.vue";
import Swal from "sweetalert2";
import axiosapi from "@/plugins/axios";

// ==========================================
// 1. 核心狀態 (State)
// ==========================================
const router = useRouter(); //路由

const cartStore = useTicketCartStore(); //實體化 Store

const bookingStage = ref("svg"); // 'svg' (選區) 或 'grid' (選位)
const selectedZoneId = ref(null);
const selectedSeats = ref([]); // 消費者已挑選的購物車清單

const svgContainer = ref(null); //讓 Vue 抓得到 SVG 的 DOM 節點

// 模擬從後端 API 載入的該場次完整劃位資料 (包含 SVG 與攤平的 SessionSeats)
const locationSvg = ref("");
const allSessionSeats = ref([]);
const ticketTypes = ref([]);

// 防連點機制 (Submit 按鈕與 Loading 狀態連動)
const isSubmitting = ref(false);

// 計算屬性：取得當前選取分區的名稱(改從 SVG 爬出來的字典拿)
const currentZoneName = computed(() => {
    //沒有抓到zoneId回傳空字串
    if (!selectedZoneId.value || !zoneMetaData.value[selectedZoneId.value]) return "";
    return zoneMetaData.value[selectedZoneId.value].name;
});

// 計算屬性：計算總金額
const totalPrice = computed(() => {
    return selectedSeats.value.reduce((sum, seat) => sum + seat.price, 0);
});

const hoveredSeat = ref(null); // 紀錄目前滑鼠指著的座位

// ==========================================
// 購物車 Modal 顯示控制
// ==========================================
const showCartModal = ref(false);

// ==========================================
// 座位 Hover 互動邏輯
// ==========================================
function handleSeatEnter(seat) {
    if (seat.is_walkway) return; // 滑到走道不顯示
    hoveredSeat.value = seat;
}

// ==========================================
// 統計全場該票種的總餘票 (即時監控大腦)
// ==========================================
function getGlobalTicketCount(ticketTypeId) {
    return allSessionSeats.value.filter((seat) => seat.ticket_type_id === ticketTypeId && seat.status === 1 && !seat.is_walkway).length;
}

// ==========================================
// 2. 漸進式渲染核心過濾器 (The Filter Engine)
// ==========================================
// 🚀 關鍵效能優化點：只將屬於該點擊分區的座位過濾出來，丟給 SeatMap.vue。
const filteredSeats = computed(() => {
    if (!selectedZoneId.value) return [];
    return allSessionSeats.value.filter((seat) => seat.zone_id === selectedZoneId.value);
});

// 動態計算當前分區的網格最大邊界，防範 CSS Grid 變形坍塌
const activeZoneMaxX = computed(() => {
    return filteredSeats.value.reduce((max, s) => (s.grid_x_index > max ? s.grid_x_index : max), 0);
});
const activeZoneMaxY = computed(() => {
    return filteredSeats.value.reduce((max, s) => (s.grid_y_index > max ? s.grid_y_index : max), 0);
});

// ==========================================
// 動態監控大腦：分區售票狀態統計
// ==========================================
const zoneStats = computed(() => {
    const stats = {};
    allSessionSeats.value.forEach((seat) => {
        if (seat.is_walkway) return; // 略過走道

        // 初始化該區的統計物件
        if (!stats[seat.zone_id]) {
            // 從 SVG 提取 metadata 中的名稱與顏色！
            // 名稱依然可以從 SVG metadata 抓，但顏色我們不用它的了
            const meta = zoneMetaData.value[seat.zone_id] || { name: "未知區", color: "#cbd5e1" };

            stats[seat.zone_id] = {
                zoneName: meta.name,
                // 優先使用後端綁定的真實票種顏色！
                zoneColor: seat.color || "#cbd5e1",
                total: 0,
                available: 0,
                tickets: {}, // 記錄各票種的剩餘數量
            };
        }

        const zStat = stats[seat.zone_id];
        zStat.total++;
        if (seat.status === 1) zStat.available++; // status === 1 代表可售

        // 統計票種細節
        if (seat.ticket_type_id) {
            if (!zStat.tickets[seat.ticket_type_id]) {
                const tType = ticketTypes.value.find((t) => t.ticket_type_id === seat.ticket_type_id);
                zStat.tickets[seat.ticket_type_id] = {
                    name: tType ? tType.name : "未知",
                    price: tType ? tType.price : 0,
                    available: 0,
                };
            }
            // 狀態為 1 (可售) 該票種的庫存才 +1
            if (seat.status === 1) {
                zStat.tickets[seat.ticket_type_id].available++;
            }
        }
    });
    return stats;
});

// ==========================================
// 策略 A：SVG 視覺加權渲染 (灰階與半透明)
// ==========================================
function updateSvgVisuals() {
    if (!svgContainer.value) return;
    const paths = svgContainer.value.querySelectorAll("[data-zone-id]");

    paths.forEach((path) => {
        const zIdStr = path.getAttribute("data-zone-id");
        if (!zIdStr) return;

        const zId = parseInt(zIdStr, 10);
        const stats = zoneStats.value[zId];
        if (!stats) return;

        // 動態替換名稱
        // 在標準的 SVG 中，<text> 標籤通常會緊跟在 <path> 的後面
        const nextNode = path.nextElementSibling;
        const isTextNode = nextNode && nextNode.tagName.toLowerCase() === "text";

        // 🌟 破案關鍵：引入你辛苦挖出來的 zoneMetaData
        const meta = zoneMetaData.value[zId];

        // 🌟 雙重保險：優先使用資料庫票種的顏色，如果沒有，就用 SVG 挖出來的顏色
        const finalColor = stats.zoneColor || (meta ? meta.color : "#cbd5e1");
        const finalName = stats.zoneName || (meta ? meta.name : `分區 ${zId}`);

        if (isTextNode) {
            nextNode.textContent = finalName; // 換成資料庫的區塊名稱
        }
        // 動態替換顏色與狀態

        // 💡 修正點 1：確保即使該區 total 為 0 (異常資料)，也不會導致計算錯誤
        const ratio = (stats && stats.total > 0) ? (stats.available / stats.total) : 0;

        // 判斷庫存並上色
        if (stats.available === 0) {
            // 完售：強制灰階且不可點擊
            path.style.fill = "#cbd5e1";
            path.style.stroke = "#94a3b8"; // 順便蓋掉邊框
            path.style.strokeWidth = "2px"; // 還原預設粗細
            path.style.cursor = "not-allowed";
            path.style.pointerEvents = "none"; // 連 Hover 都不觸發

            if (isTextNode) {
                nextNode.textContent = finalName + " (完售)";
                nextNode.style.fill = "#64748b"; // 完售時的文字顏色
            }
        } else {
            // 可售狀態：使用雙重保險算出來的 finalColor
            path.style.fill = finalColor;
            path.style.stroke = finalColor; // 順便蓋掉邊框，讓色彩更飽滿
            path.style.strokeWidth = "2px"; // 還原預設粗細

            if (ratio < 0.1) {
                path.style.opacity = "0.4"; // 快賣完(< 10%)變半透明
            } else {
                path.style.opacity = "1";
            }

            // 強制將可售區域的文字改為深色
            // 推薦使用 #1e293b (Slate-800) 深灰藍色，視覺上會比純黑 #000000 更有質感且不生硬
            nextNode.style.fill = "#1e293b";
        }
    });
}

// ==========================================
// 策略 B：Hover Tooltip 互動邏輯
// ==========================================
const showTooltip = ref(false);
const tooltipX = ref(0);
const tooltipY = ref(0);
const currentTooltipData = ref({});

function handleSvgMouseMove(event) {
    const target = event.target;
    const zIdStr = target.getAttribute("data-zone-id");

    if (zIdStr) {
        const zId = parseInt(zIdStr, 10);
        const stats = zoneStats.value[zId];
        if (stats && stats.available > 0) {
            currentTooltipData.value = stats;
            showTooltip.value = true;
            // 讓 Tooltip 跟隨滑鼠，加上偏移量避免遮擋游標
            tooltipX.value = event.clientX + 15;
            tooltipY.value = event.clientY + 15;
            return;
        }
    }
    showTooltip.value = false;
}

function handleSvgMouseLeave() {
    showTooltip.value = false;
}

// ==========================================
// 3. 互動邏輯 (Interactions)
// ==========================================

// A 階段：點擊 SVG 分區入口
function handleSvgClick(event) {
    const target = event.target;
    const zoneIdStr = target.getAttribute("data-zone-id");

    if (!zoneIdStr) return; // 點到空白裝飾區不反應

    const zoneId = parseInt(zoneIdStr, 10);

    // 檢查這區是否還有可售座位（防禦性設計）
    const hasAvailableSeats = allSessionSeats.value.some((s) => s.zone_id === zoneId && s.status === 1 && s.is_walkway === false);
    if (!hasAvailableSeats) {
        Swal.fire("很遺憾", "該區域的座位已全部售罄！請選擇其他區域。", "info");
        return;
    }

    // 通過檢查，將靈魂 ID 鎖定，流暢切換到網格微觀視角
    selectedZoneId.value = zoneId;
    bookingStage.value = "grid";

    // 切換視角的瞬間，手動強制把 Tooltip 關掉
    showTooltip.value = false;
}

// B 階段：消費者點擊實體格子進行選位
function handleSeatSelect(seat) {
    if (seat.is_walkway) return; // 點到走道不反應

    // 狀態 1：已被他人搶走 (status !== 1)
    // 後端狀態碼 (1: 可售, 2: 鎖定中, 3: 已售出, 0: 硬體保留/不可見)
    if (seat.status !== 1) return; //不是"可售" -> 直接跳過不要互動

    // 狀態 2：該座位已被目前消費者選中 -> 執行「取消選取 (Toggle Off)」
    const alreadySelectedIndex = selectedSeats.value.findIndex((s) => s.ticket_id === seat.ticket_id);
    if (alreadySelectedIndex > -1) {
        selectedSeats.value.splice(alreadySelectedIndex, 1);
        seat.color = seat.backupColor; // 恢復該座位原本的票種顏色
        seat.active = false; // 💡 加上這行
        return;
    }

    // 狀態 3：消費者欲選取新座位 -> 檢查是否超過限購張數 (防刷票)
    if (selectedSeats.value.length >= 4) {
        Swal.fire("購買數量已達上限", "每筆交易上限為 4 張票券。", "warning");
        return;
    }

    // 順利加入購物車
    const ticketConfig = ticketTypes.value.find((t) => t.ticket_type_id === seat.ticket_type_id);

    // 暫存原色，方便隨時 Toggle 取消
    seat.backupColor = seat.color;
    // seat.color = "#22c55e"; // 🌟 統一改為綠色，提供極佳的選中視覺反饋

    // 💡 關鍵：加上 active 狀態，讓 SeatMap.vue 知道要顯示打勾
    seat.active = true;

    seat.price = ticketConfig ? ticketConfig.price : 0;
    seat.ticketName = ticketConfig ? ticketConfig.name : "一般票";
    seat.ticketColor = ticketConfig ? ticketConfig.color : "#cbd5e1";

    selectedSeats.value.push(seat);
}

// 購物車面板移除按鈕連動
function removeSeatSelection(seat) {
    // 🌟 核心防呆：先檢查這張票到底在不在購物車裡？
    const index = selectedSeats.value.findIndex((s) => s.ticket_id === seat.ticket_id);
    if (index === -1) return; // 不在購物車裡就直接中斷

    // 執行移除
    // 🌟 修正點：將 slice 改為 splice，才能真正把資料從陣列中剔除！
    selectedSeats.value.splice(index, 1);

    // 連動還原左側網格的顏色
    const targetInList = allSessionSeats.value.find((s) => s.ticket_id === seat.ticket_id);
    if (targetInList && targetInList.backupColor) {
        targetInList.color = targetInList.backupColor;
        targetInList.active = false; // 💡 加上這行
    }

    // 防呆：如果購物車清空了，自動關閉彈窗
    if (selectedSeats.value.length === 0) {
        showCartModal.value = false;
    }
}

// 將函式加上 async，以便使用 await
async function backToSvgStage() {
    selectedZoneId.value = null;
    bookingStage.value = "svg";// 1. 先切換狀態，Vue 開始重新渲染 SVG DOM

    // 新增這段，等待 Vue 將地圖 DOM 重新「生」出來
    await nextTick();

    // DOM 準備好了，命令 JS 重新染上正確的票種顏色與完售狀態
    updateSvgVisuals();

    // 真正退出分區時，才重置看板的最後記憶
    hoveredSeat.value = null;
}

// ==========================================
// 右側票種卡片連動 SVG 互動邏輯
// ==========================================
const hoveredTicketTypeId = ref(null);

function handleTicketHover(ticketTypeId) {
    hoveredTicketTypeId.value = ticketTypeId;
    // 找出所有綁定這個票種的分區，並為它們的 SVG path 加上高亮特效
    if (!svgContainer.value) return;
    const paths = svgContainer.value.querySelectorAll("[data-zone-id]");


    paths.forEach(path => {
        const zId = parseInt(path.getAttribute("data-zone-id"), 10);
        // 檢查這個區塊的票種，是否剛好是目前 hover 的票種
        const isTargetZone = allSessionSeats.value.some(
            s => s.zone_id === zId && s.ticket_type_id === ticketTypeId
        );

        if (isTargetZone) {
            // ✨ 核心優化：僅加粗外框，並換成極具質感的深色刺眼外框（或白框，依背景而定）
            path.style.strokeWidth = "5px";
            path.style.stroke = "#334155"; // 或者是 "#ffffff"，深色外框能強烈突顯區塊

            // 提升當前 path 的層級，確保加粗的外框不會被隔壁的區塊遮擋
            // ❌ 把這行刪除，不要更動 SVG 的 DOM 順序！
            // path.parentElement.appendChild(path);
        } else {
            // 其餘分區完全保持原有的 fill 和 opacity，絕對不改動它們，徹底根除閃爍
            const stats = zoneStats.value[zId];
            if (stats) {
                path.style.strokeWidth = "2px";
                path.style.stroke = stats.available === 0 ? "#94a3b8" : stats.zoneColor;
            }
        }
    });
}

function handleTicketLeave() {
    hoveredTicketTypeId.value = null;
    // 移開時，呼叫原本寫好的 SVG 上色函式，讓一切恢復原狀
    updateSvgVisuals();
}

function handleTicketClick(ticketTypeId) {
    // 找一個屬於這個票種，且還有剩餘座位的分區來進入
    const targetZone = allSessionSeats.value.find(
        s => s.ticket_type_id === ticketTypeId &&
            s.status === 1 &&
            !s.is_walkway);

    if (targetZone) {
        // 模擬點擊該分區
        selectedZoneId.value = targetZone.zone_id;
        bookingStage.value = "grid";
        showTooltip.value = false;
        // 清除 hover 特效
        handleTicketLeave();
    } else {
        Swal.fire("已售罄", "該票種目前沒有任何可選的分區座位！", "info");
    }
}

// ==========================================
// 提取大腦：從 SVG DOM 中反向抓取分區名稱與顏色
// ==========================================
const zoneMetaData = ref({}); // 用來存 { 1: { name: '一樓搖滾區', color: '#ef4444' } }

function extractZoneInfoFromSvg() {
    if (!svgContainer.value) return;
    const paths = svgContainer.value.querySelectorAll("[data-zone-id]");
    const metadata = {};

    paths.forEach((p) => {
        const zId = parseInt(p.getAttribute("data-zone-id"), 10);

        // 1. 抓顏色：優先抓 stroke(邊框純色)，沒有的話抓 fill(可能帶透明度)，再沒有就給預設值
        // 🌟 進階抓色邏輯：支援 inline style 與 attribute 兩種格式
        const color = p.getAttribute("stroke") || p.getAttribute("fill") ||
            p.style.stroke || p.getAttribute("stroke") || "#cbd5e1";

        // 2. 抓名稱：找下一個 <text> 標籤的內容
        let name = `分區 ${zId}`;
        const nextNode = p.nextElementSibling;
        if (nextNode && nextNode.tagName.toLowerCase() === "text") {
            name = nextNode.textContent;
        }
        metadata[zId] = { name, color };
    });

    zoneMetaData.value = metadata;
}

// ==========================================
// 輕量級最新狀態輪詢
// ==========================================
// 功用：只向後端拿取「已售出 / 鎖定中」的座位 ID，更新前端狀態，不重整頁面
async function fetchLatestSeatStatus(sessionId) {
    try {
        // 🌟假設後端 Endpoint: GET /api/ticket/session/{sessionId}/status
        // 回傳格式範例: { success: true, data: [5001, 5002, 5008] } // 🌟 這裡後端改回傳 Ticket IDs
        const response = await axiosapi.get(`/api/ticket/session/${sessionId}/status`);

        if (response.data.success) {
            const unavailableSeatsIds = response.data.data;

            // 更新本地大腦 allSessionSeats 的狀態
            allSessionSeats.value.forEach(seat => {
                // 判斷陣列是否包含這個票券 ID
                if (unavailableSeatsIds.includes(seat.ticket_id)) {
                    seat.status = 2; // 如果在名單內，代表被搶走了                
                    removeSeatSelection(seat);// 從購物車踢出
                } else {
                    // 🌟 關鍵補強：如果在名單外，確保狀態是可售的 (解決 15 分鐘釋放後，畫面沒變回來的問題)
                    // 注意：走道 (is_walkway) 或原本就是硬體保留的位子，不要亂蓋掉
                    if (!seat.is_walkway && seat.status !== 0) {
                        seat.status = 1;
                    }
                }
            });
            // 強制等待 Vue 把 zoneStats (Computed) 重新計算完畢
            await nextTick();
            // 強制重繪 SVG 顏色
            updateSvgVisuals();
        }
    } catch (error) {
        console.error("無法更新最新座位狀態:", error);
    }
}
// ==========================================
// SSE 即時廣播收音機
// ==========================================
let eventSource = null;

function setupSSE() {
    //最終決定：直接開放API 不執行身分驗證

    // 我們使用原生的 EventSource API 建立連線
    // 直接抓取 Axios 設定好的基底網址
    const backendUrl = axiosapi.defaults.baseURL
    const sseUrl = `${backendUrl}/api/sse/subscribe/${sessionId.value}`;

    eventSource = new EventSource(sseUrl);

    // 1. 監聽連線成功事件 (對應後端的 name("connect"))
    eventSource.addEventListener('connect', (event) => {
        console.log('✅ SSE 電台連線成功:', event.data);
    })
    // 2. 🌟 監聽座位狀態更新事件 (對應後端的 name("seat-update"))
    eventSource.addEventListener('seat-update', async (event) => {
        try {
            // 解析後端傳來的 JSON 字串： {"ticketIds": [976,977], "status": 1}
            const updateData = JSON.parse(event.data);
            console.log('🔔 收到座位狀態更新廣播:', updateData);

            // 執行狀態更新邏輯
            handleSeatStatusUpdate(updateData.ticketIds, updateData.status);
        } catch (error) {
            console.error("❌ 解析 SSE 廣播資料失敗:", error);
        }
    });

    // 3. 監聽連線錯誤 (例如網路斷線)
    eventSource.onerror = (error) => {
        console.error('⚠️ SSE 連線中斷，瀏覽器將在背景自動重連...', error);
        // EventSource 天生具備自動重連能力，通常我們不需要在這裡寫複雜的重連邏輯
    }
}

// ==========================================
// SSE 狀態更新器
// ==========================================
async function handleSeatStatusUpdate(targetTicketIds, newStatus) {
    let hasChanges = false;

    // 1. 更新本地大腦 allSessionSeats 的狀態
    allSessionSeats.value.forEach(seat => {
        // 如果這個座位在廣播名單內
        if (targetTicketIds.includes(seat.ticket_id)) {
            seat.status = newStatus;
            hasChanges = true;
            // 如果狀態變成鎖定(2) 或 已售出(3)
            if (newStatus !== 1) {
                // 如果這個座位剛好在我的購物車裡，把它踢出去！
                removeSeatSelection(seat);
            }
        }
    });
    // 2. 如果真的有改到狀態，才去觸發畫面重繪，節省效能
    if (hasChanges) {
        await nextTick();
        updateSvgVisuals(); // 呼叫原本就有的重繪 SVG 函數
    }
}

// ==========================================
// 4. 初始化與訂單發送
// ==========================================
const route = useRoute(); //路由準備拉網址
const sessionName = ref("");
const locationName = ref("");
// 路由有帶入 sessionId，/themes/1/session/1/book-ticket     
const sessionId = computed(() => route.params.sessionId);
const isSvgReady = ref(false); // 控制 SVG 顯示時機

onMounted(async () => {
    try {
        //一進入頁面，立刻清空 Pinia 裡可能殘留的舊訂單資料
        cartStore.clearCart();

        // 呼叫後端 API 取得完整場地大禮包
        const response = await axiosapi.get(`/api/ticket/session/${sessionId.value}`);

        if (response.data.success) {
            // 解構後端傳來的大禮包 (SessionTicketCataLogDTO)
            const catalog = response.data.data;

            // 1. 設定場次與場地名稱 (若你的 UI 有用到)
            sessionName.value = catalog.sessionName;
            locationName.value = catalog.locationName;

            // 2. 設定 SVG 地圖
            locationSvg.value = catalog.locationSvg;

            // 3. 初步攤平資料
            const rawSeats = catalog.tickets.map(t => ({
                ticket_id: t.ticketId,
                seat_id: t.seatId,
                zone_id: t.zoneId,
                zone_name: t.zoneName,
                ticket_type_id: t.ticketTypeId,
                ticketName: t.ticketName,// 注意這個前端是用 camelCase
                price: t.price,
                color: t.color,
                status: t.status,
                physical_row: t.physicalRow,
                physical_seat: t.physicalSeat,
                grid_x_index: t.gridX,
                grid_y_index: t.gridY,
                // 前端用狀態 0 判斷走道
                is_walkway: t.status === 0
            }));

            // 分區座標正規化 (消除大片空白)
            const zoneBounds = {};

            // 步驟 A: 掃描每個分區，找出它們的「最左上角」界線 (minX, minY)
            rawSeats.forEach(seat => {
                if (!zoneBounds[seat.zone_id]) {
                    zoneBounds[seat.zone_id] = { minX: Infinity, minY: Infinity };
                }
                if (seat.grid_x_index < zoneBounds[seat.zone_id].minX) {
                    zoneBounds[seat.zone_id].minX = seat.grid_x_index;
                }
                if (seat.grid_y_index < zoneBounds[seat.zone_id].minY) {
                    zoneBounds[seat.zone_id].minY = seat.grid_y_index;
                }
            });

            // 步驟 B: 將該區的所有絕對座標，平移為「貼齊邊界」的相對座標
            rawSeats.forEach(seat => {
                // 減去最小值並 +1，確保網格永遠從第一格 (1, 1) 開始畫
                seat.grid_x_index = seat.grid_x_index - zoneBounds[seat.zone_id].minX + 1;
                seat.grid_y_index = seat.grid_y_index - zoneBounds[seat.zone_id].minY + 1;
            });

            // 將壓縮完美的乾淨資料指派給響應式變數
            allSessionSeats.value = rawSeats;

            // 4. 動態萃取獨一無二的「票種清單」(供右側面板顯示)
            // 透過 Map 來確保票種不重複
            const uniqueTickets = new Map();
            allSessionSeats.value.forEach(seat => {
                // 過濾掉未綁定票種的座位 (或走道)
                if (seat.ticket_type_id && !uniqueTickets.has(seat.ticket_type_id)) {
                    uniqueTickets.set(seat.ticket_type_id, {
                        ticket_type_id: seat.ticket_type_id,
                        name: seat.ticketName,
                        price: seat.price,
                        color: seat.color
                    });
                }
            });
            ticketTypes.value = Array.from(uniqueTickets.values());

            // 5. 等待 Vue 把 SVG 畫到畫面上後，執行狀態渲染
            await nextTick();

            // 6. 把 SVG 的名字跟顏色挖出來
            extractZoneInfoFromSvg();

            // 💡 修正點 2：在去問後端最新狀態之前，就先讓 SVG 顯示出來！
            // 這樣就算 fetchLatestSeatStatus 發生例外狀況，或者是全場完售導致某些邏輯中斷，
            // 消費者至少還是能看到灰色的場地圖，而不是一片空白。
            isSvgReady.value = true;

            // 手動先塗一次底色 (基於一開始拿到的 allSessionSeats)
            updateSvgVisuals();

            // 7. 完美融合動靜資料：呼叫輕量輪詢 API，拿取「現在」的最新狀態！
            await fetchLatestSeatStatus(sessionId.value);

            // 8. 底板畫完後，開啟 SSE 收音機，開始監聽即時變化！
            setupSSE();
        } else {
            Swal.fire("讀取失敗", response.data.message, "error");
        }
    } catch (error) {
        console.error("無法取得場次資料:", error);
        Swal.fire("系統錯誤", "無法連線到售票伺服器", "error");
    }

});
// 離開頁面時
onUnmounted(() => {
    // 關閉收音機：離開頁面時，一定要把水管切斷，以免佔用記憶體
    if (eventSource) {
        eventSource.close();
        console.log('🔌 離開頁面，已關閉 SSE 連線');
    }
})


//預扣庫存與送出訂單
async function submitOrder() {
    if (selectedSeats.value.length === 0) return; //沒有買票不能結帳

    // 開啟防連點保護
    isSubmitting.value = true

    // 🔒 先快照要下單的座位（deep copy）。
    // 原因：送出鎖位請求後、await 回來之前，SSE 會收到「自己這筆鎖位(status 2)」的廣播，
    // handleSeatStatusUpdate 會把這些座位從 selectedSeats 移除。若之後才用 selectedSeats
    // 存購物車，會存進空陣列 → 結帳頁購物車為空被導走、跳不到綠界。改用此快照即可免疫。
    const seatsToBook = selectedSeats.value.map(seat => ({ ...seat }));

    const orderPayload = {
        sessionId: sessionId.value, // 使用 computed 的 value
        ticketIds: seatsToBook.map((s) => s.ticket_id),
    };
    console.log("🚀 準備發送鎖定請求 Payload：", JSON.stringify(orderPayload));

    try {
        // 🌟假設後端 Endpoint: POST /api/ticket/lock
        // JWT Token 通常會由 axios 攔截器 (Interceptor) 自動帶在 Header 中
        const response = await axiosapi.post(`/api/ticket/lock`, orderPayload);

        if (response.data.success) {
            // 鎖定成功：用鎖位前的快照存購物車（不受 SSE 清空 selectedSeats 影響）
            cartStore.saveTickets(
                seatsToBook,
                sessionId.value
            );

            // 關閉購物車彈窗
            showCartModal.value = false;

            Swal.fire({
                title: "座位鎖定成功！",
                text: "請於 15 分鐘內完成結帳，否則座位將被釋出。",
                icon: "success",
                timer: 2000,
                showConfirmButton: false
            }).then(() => {
                //🌟跳轉至結帳頁面
                router.push({ name: 'PaymentTicket' });
            });
        } else {
            // 鎖定失敗：手速太慢，座位被別人搶走了 (後端樂觀鎖阻擋或 Redis 扣減失敗)
            Swal.fire("晚了一步", response.data.message || "您選擇的部分座位已被鎖定，請重新選擇。", "warning");

            // 🌟 觸發輕量級 API 更新畫面，把別人買走的位子反灰
            await fetchLatestSeatStatus(sessionId.value);
        }
    } catch (error) {
        console.error("鎖定請求失敗:", error);
        Swal.fire("系統繁忙", "目前搶票人數過多，請稍後再試！", "error");
    } finally {
        // 解除按鈕鎖定
        isSubmitting.value = false;
    }
}
</script>

<style scoped>
/* ==========================================
   SVG 淡入動畫處理
========================================== */
/* 初始狀態：預設為完全透明，並設定 0.5 秒的淡入過渡效果 */
.svg-transition {
    opacity: 0;
    transition: opacity 0.5s ease-in-out;
}

/* 準備就緒狀態：當 Vue 掛載上 is-ready class 時，透明度變為 1 */
.svg-transition.is-ready {
    opacity: 1;
}


.svg-consumer-wrapper :deep(path):hover {
    opacity: 0.75;
    transition: opacity 0.15s ease-in-out;
}

/* 強制釋放 SVG 的寬度限制，讓它隨容器放大，並保持比例 */
.svg-consumer-wrapper :deep(svg) {
    width: 100% !important;
    height: auto !important;
    /* 取消 max-width，改用 max-height 確保地圖不會超過大半個螢幕，導致結帳按鈕被推太下去 */
    max-height: 65vh !important;

    /* 確保 SVG 的 path 支援我們加上的特效 */
    transition: all 0.2s ease-in-out;
}

/* 確保 SVG 的文字永遠清晰可見 */
.svg-consumer-wrapper :deep(text) {
    /* 加上微弱的文字陰影，提升與淺色背景的對比度 */
    /* text-shadow: 0px 1px 3px rgba(0, 0, 0, 0.6); */
    /* 確保文字不會阻擋滑鼠去感應下方的 path 區塊 */
    pointer-events: none;

}

.sticky-top {
    z-index: 1020;
}

/* 讓右側票種卡片有可點擊的提示，且滑過時微微放大 */
.cursor-pointer {
    cursor: pointer;
}

.hover-scale:hover {
    transform: scale(1.02);
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06) !important;
}


/* 讓購物車出現平滑的進入動畫 */
@keyframes fadeIn {
    from {
        opacity: 0;
        transform: translateY(10px);
    }

    to {
        opacity: 1;
        transform: translateY(0);
    }
}

.animate-fade-in {
    animation: fadeIn 0.3s ease-out forwards;
}

/* 讓 Overlay 視窗有由下往上滑出的動畫，不會顯得突兀 */
.slide-up-overlay {
    animation: slideUp 0.3s cubic-bezier(0.25, 0.8, 0.25, 1) forwards;
}

@keyframes slideUp {
    from {
        transform: translateY(100%);
        opacity: 0;
    }

    to {
        transform: translateY(0);
        opacity: 1;
    }
}

/* SVG Hover 特效加強 */
.svg-consumer-wrapper :deep(.zone-path:hover) {
    stroke-width: 3px !important;
    filter: brightness(0.9);
}

/* 完售時的灰階特效 */
.grayscale {
    filter: grayscale(100%);
}

/* 即時更新呼吸燈 */
@keyframes pulse {
    0% {
        opacity: 1;
    }

    50% {
        opacity: 0.5;
    }

    100% {
        opacity: 1;
    }
}

.animate-pulse {
    animation: pulse 2s infinite;
}

.seat-modal-container {
    width: 95% !important;
    max-width: 900px;
}
@media (min-width: 768px) {
    .seat-modal-container {
        width: 85% !important;
    }
}

.text-truncate-custom {
    max-width: 150px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
}
@media (max-width: 576px) {
    .text-truncate-custom {
        max-width: 110px;
    }
}
</style>
