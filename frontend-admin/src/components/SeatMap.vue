<template>
    <div class="seat-map-container" :style="{
        gridTemplateColumns: `repeat(${maxX}, 1fr)`,
        gridTemplateRows: `repeat(${maxY}, 1fr)`
    }">
        <div v-for="seat in seats" :key="seat.seat_id" class="seat" :class="{
            'is-disabled': seat.disabled, /* 如果父元件傳來 disabled: true，就套用這個樣式 */
            'is-active': seat.active,    /* 如果父元件傳來 active: true，就套用這個樣式 */
            'is-walkway': seat.is_walkway  /* 接收走道標記 */
        }" :style="{
            gridColumn: seat.grid_x_index,
            gridRow: seat.grid_y_index,
            background: seat.zone_id ? seat.color : ''
        }" @mousedown="handleMouseDown(seat, $event)" @mouseenter="$emit('seatEnter', seat)"
            :data-coord="`X:${seat.grid_x_index}, Y:${seat.grid_y_index}`">
            <span v-if="seats.length < 2000" style="font-size: 10px; 
            text-align: center">
                <!-- {{ seat.physical_name }} -->
            </span>
        </div>
    </div>
</template>

<script setup>
// 1. 接收老闆傳下來的 seats 資料 (Props)
const props = defineProps({
    seats: {
        type: Array,
        required: true
    },
    maxX: Number,
    maxY: Number
})

// 2. 定義一個要向老闆大喊的事件 (Emits)，我們把它取名為 'seatClick'
const emit = defineEmits(['seatDown', 'seatEnter'])

// 3. 封裝 Mousedown 事件，確保完美傳遞 seat 與特殊的 $event 對象
function handleMouseDown(seat, event) {
    emit('seatDown', seat, event);
}
</script>

<style scoped>
.seat-map-container {
    /* 啟動 Grid  */
    display: grid;

    /* 長寬改成動態綁定 */
    /* 定義 5 個直欄，每個一樣寬 */
    /* grid-template-columns: repeat(5, 1fr); */
    /* 定義 5 個橫列，每個一樣高 */
    /* grid-template-rows: repeat(5, 1fr); */

    /* 座位之間的走道縫隙 */
    /* 【資深優化】當總格數變多時，10px 的 gap 會把畫布撐到無限大，改用 2px~4px 在大場地視覺效果最好 */
    gap: 3px;

    /* 限制整個座位圖的最大寬度，並置中 */
    /* max-width: 600px;
    margin: 20px auto; */

    /* 移除原本的 max-width: 600px 限制，讓它由父元件的畫布縮放容器（max-content）來決定大小，否則 10,000 格會擠成原子筆點 */
    width: max-content;
    margin: 0 auto;

    /* ✨ 加上這行，確保拖曳時裡面的文字不會變成藍色的反白狀態 */
    user-select: none;
}

.seat {
    position: relative;
    /* 🔍 確保這一行有加上，讓氣泡能相對於格子定位 */

    /* 淺灰色的基本座位外觀 (自適應深淺主題) */
    background-color: var(--tap-bg-base, #f1f5f9);
    /* 細緻的邊框 */
    border: 1px solid var(--tap-border, #e2e8f0);
    color: var(--tap-text-secondary, #64748b);
    /* 圓角 */
    border-radius: 4px;
    /* 強制正方形 */
    /* aspect-ratio: 1/1; */

    /* 【關鍵效能優化】大場地渲染時，aspect-ratio: 1/1 在某些舊瀏覽器計算 grid 時會有百毫秒級延遲。
       建議在後台編輯器給定一個基礎的寬高（例如 18px 或 20px），這樣畫布縮放、放大縮小才不會卡頓 */
    width: 20px;
    height: 20px;

    display: flex;
    justify-content: center;
    align-items: center;

    cursor: pointer;
    /* 10,000 格子時，移除 transition 動態效果！
       因為 10,000 個 DOM 同時觸發 CSS transition 會強迫瀏覽器不斷進行重繪（Repaint），這是卡頓的主因之一 */
    /* transition: all 0.2s ease-in-out; */
    /* transition: all 0.2s ease-in-out; */
}

/* 滑鼠懸停時使用品牌主色微調 */
.seat:hover {
    background-color: var(--tap-border);
    border-color: var(--tap-primary);
}

/* 被禁用/不可點擊的通用樣式 */
.is-disabled {
    background-color: var(--tap-bg-surface) !important;
    border-color: var(--tap-border) !important;
    color: var(--tap-text-secondary);
    cursor: not-allowed;
}

/* 被選中/高亮的通用樣式 (採用品牌暖橘色) */
.is-active {
    background-color: var(--tap-primary) !important;
    border-color: #f08c65 !important;
    color: #fff !important;
    transform: scale(1.05);
}

/* 走道樣式，在深色主題中保持背景透明以融合畫布 */
.is-walkway {
    background-color: transparent !important;
    border-color: transparent !important;
    pointer-events: none;
    color: transparent;
}

/* 1. 當滑鼠懸停在座位上時，浮現座標氣泡 */
.seat:hover::after {
    content: attr(data-coord);
    /* 👈 核心：直接抓取 HTML 上的 data-coord 文字 */
    position: absolute;
    bottom: 145%;
    /* 🔴 調整：從 135% 改為 145%，因為泡泡變大變高了，往上挪一點避免壓到格子 */
    /* 控制氣泡浮在格子上方的高度 */
    left: 50%;
    transform: translateX(-50%);

    /* 質感外觀設計 */
    background-color: rgba(15, 23, 42, 0.95);
    /* 質感黑底 */
    color: #ffffff;
    /* 純白字 */
    padding: 30px 60px;
    /* 讓懸浮框框體大小 */
    border-radius: 6px;
    /* 🔴 調整：從 4px 改為 6px，配合大泡泡讓圓角更流線 */
    font-size: 60px;
    /* 懸浮框內字體大小 */
    font-weight: bold;
    white-space: nowrap;
    /* 確保文字不換行 */
    z-index: 9999;
    /* 確保不會被隔壁格子擋住 */
    box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.2);
    pointer-events: none;
    /* 🔍 關鍵：防止氣泡干擾滑鼠點擊與拖曳事件 */
}

/* 2. 氣泡下方的小三角形箭頭 */
.seat:hover::before {
    content: "";
    position: absolute;
    bottom: 122%;
    /* 🔴 調整：從 115% 改為 122%，配合上方大泡泡的位置微調 */
    left: 50%;
    transform: translateX(-50%);
    border-width: 6px;
    /* 🔴 調整：從 5px 改為 6px，讓小箭頭稍微變大，比例跟大泡泡更對稱 */
    border-style: solid;
    border-color: rgba(15, 23, 42, 0.95) transparent transparent transparent;
    z-index: 9999;
    pointer-events: none;
}

/* 3. 走道（Walkway）不需要顯示座標氣泡，將其隱藏 */
.is-walkway:hover::after,
.is-walkway:hover::before {
    display: none !important;
}
</style>
