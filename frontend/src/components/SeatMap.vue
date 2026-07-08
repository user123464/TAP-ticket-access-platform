<template>
    <div class="seat-map-container" :style="{
        gridTemplateColumns: `repeat(${maxX}, 45px)`,
        gridTemplateRows: `repeat(${maxY}, 45px)`
    }">
        <div v-for="seat in seats" :key="seat.seat_id" class="seat position-relative" :class="{
            'is-disabled': seat.disabled && seat.status !== 3, /* 如果父元件傳來 disabled: true，就套用這個樣式 */
            'is-sold-out': seat.status === 3, /* 專屬的已售出狀態 */
            'is-selected': seat.active,    /* 如果父元件傳來 active: true，就套用這個樣式 */
            'is-walkway': seat.is_walkway,  /* 接收走道標記 */
            'is-dimmed': props.hoverTicketTypeId !== null && seat.ticket_type_id !== props.hoverTicketTypeId, /*  高亮邏輯 */
            'cursor-not-allowed opacity-50': seat.status !== 1,
            'cursor-pointer hover:opacity-80': seat.status === 1
        }" :style="{
            gridColumn: seat.grid_x_index,
            gridRow: seat.grid_y_index,
            // 如果是已售出狀態，移除行內樣式底色賦值
            background: seat.status === 3 ? '' : seat.color /* 如果已售出，就不吃票種底色 */
        }" @mousedown="$emit('seatDown', seat)" @mouseenter="$emit('seatEnter', seat)">

            <!-- 💡 新增：當座位處於 active 狀態 (被選中) 時，顯示打勾圖示 -->
            <i v-if="seat.active" class="bi bi-check-lg position-absolute fw-bold text-white shadow-sm"
                style="font-size: 24px; top: 50%; left: 50%; transform: translate(-50%, -50%); z-index: 10; text-shadow: 0px 1px 2px rgba(0,0,0,0.5);">
            </i>
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
    maxY: Number,
    // 接收 hover 的 ID
    hoverTicketTypeId: {
        type: Number,
        default: null
    }
})

// 2. 定義一個要向老闆大喊的事件 (Emits)，我們把它取名為 'seatClick'
defineEmits(['seatDown', 'seatEnter', 'seatLeave'])
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
    gap: 10px;

    /* 限制整個座位圖的最大寬度，並置中 */
    /* ❌ 刪除這行：不要限制最大寬度，讓外層父元件的 overflow: auto 去處理卷軸 */
    /* max-width: 600px; */
    margin: 20px auto;

    /* 確保網格不會被父層擠壓變形，且座位較少時能保持置中 */
    width: max-content;
    justify-content: center;

    /* 確保拖曳時裡面的文字不會變成藍色的反白狀態 */
    user-select: none;
}

.seat {
    /* 淺灰色的基本座位外觀 */
    background-color: #f1f5f9;
    /* 細緻的淺灰色邊框 */
    border: 1px solid #e2e8f0;
    /* 圓角 */
    border-radius: 4px;
    /* 強制正方形 */
    aspect-ratio: 1/1;

    /* 讓裡面的文字(排/號)絕對置中 */
    display: flex;
    justify-content: center;
    align-items: center;

    /* 滑鼠移上去時變成手指游標 */
    cursor: pointer;
    transition: all 0.2s ease-in-out;
}

/* 滑鼠懸停時 亮藍色 */
.seat:hover {
    background-color: #bfdbfe;
    border-color: #3b82f6;

}


/* 當其他票種被 hover 時，不屬於該票種的座位變暗 */
.seat.is-dimmed {
    opacity: 0.2;
    filter: grayscale(80%);
    transition: all 0.3s ease;
}

/* 已售出的座位可以加上不同的外觀，例如打叉或變灰色 */
.seat.is-disabled {
    background-color: #e2e8f0 !important;
    cursor: not-allowed;
    color: #94a3b8;
}

/* 專屬的已售出樣式：刺眼紅 + 白色打叉 + 封鎖線網底 */
.seat.is-sold-out {
    background-color: #ef4444 !important;
    border-color: #dc2626 !important;
    color: #ffffff !important;
    cursor: not-allowed;
    background-image: none !important;
}

/* 修正原本的 is-disabled (保留給未來可能用到的「系統鎖定」狀態) */
.seat.is-disabled {
    background-color: #e2e8f0 !important;
    cursor: not-allowed;
    color: #94a3b8;
}

/* 被選中樣式 */
.seat.is-selected {
    /* 使用深藍色外層陰影，加粗邊框效果而不改變元素大小 */
    box-shadow: 0 0 0 2px #4f46e5;

    /* 輕微縮放動畫 */
    transform: scale(1.05);

    z-index: 1;
}


/* 走道樣式，讓走道變成「可見的空心格子」 */
.is-walkway {
    /* 只保留防護罩，刪除 background-color 與 border 覆蓋 */
    /* 這樣走道就會自動吃上方 .seat 預設的淺灰底色，恢復完美的網格感 */
    pointer-events: none;

    /* 讓文字變成完全透明，但保留它撐開的體積 */
    color: transparent;
}

/* ==========================================
   狀態覆寫區 (解決游標與 Hover 衝突)
========================================== */

/* 確保狀態不為 1 (不可售) 的座位，游標絕對是禁止符號 */
.cursor-not-allowed {
    cursor: not-allowed !important;
}

/* 確保不可售的座位，滑鼠移上去時「絕對不會」變成亮藍色 */
.cursor-not-allowed:hover {

    transform: none !important;
    /* 禁用放大動畫 */
    box-shadow: none !important;
    /* 禁用陰影 */
}

/* 如果你有用到 Tailwind 的 opacity-50，但專案沒裝 Tailwind，也可以手動補上這個 */
.opacity-50 {
    opacity: 0.5 !important;
}
</style>
