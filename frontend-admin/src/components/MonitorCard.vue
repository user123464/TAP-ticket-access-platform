<template>
    <div class="card h-100 monitor-card shadow-sm">
        <div class="card-body p-4 d-flex flex-column">

            <div class="d-flex justify-content-between align-items-center mb-4">
                <h5 class="card-title mb-0 d-flex align-items-center fw-bold">
                    <i :class="['bi', icon, 'me-2', 'icon-accent']"></i>
                    {{ title }}
                </h5>

                <a v-if="grafanaUrl" :href="grafanaUrl" target="_blank" class="btn btn-sm btn-grafana">
                    查看詳情 <i class="bi bi-box-arrow-up-right ms-1"></i>
                </a>
            </div>

            <div class="monitor-content flex-grow-1">
                <slot></slot>
            </div>

        </div>
    </div>
</template>

<script setup>
// 定義父層可以傳進來的屬性 (Props)
defineProps({
    title: {
        type: String,
        required: true
    },
    icon: {
        type: String,
        default: 'bi-activity'
    },
    grafanaUrl: {
        type: String,
        default: ''
    }
});
</script>

<style scoped>
/* 🎨 深度還原你們系統的 Dark Mode 質感 */
.monitor-card {
    /* 這裡的背景色抓取了你們系統卡片的深灰色澤，若有全域變數(如 var(--bg-dark))可直接替換 */
    background-color: #242a38;
    border: 1px solid #363d4d;
    /* 微弱的邊框，讓卡片從背景中浮現 */
    border-radius: 8px;
    color: #f3f4f6;
    /* 主要文字顏色為亮白灰色 */
    transition: transform 0.2s ease, box-shadow 0.2s ease;
}

/* 滑鼠懸停時的微互動，增加 SaaS 平台的高級感 */
.monitor-card:hover {
    transform: translateY(-2px);
    box-shadow: 0 6px 16px rgba(0, 0, 0, 0.4) !important;
}

.card-title {
    color: #9ca3af;
    /* 標題用次要的灰色，讓中間的數字成為視覺焦點 */
    font-size: 1.1rem;
}

.icon-accent {
    color: #3b82f6;
    /* 點綴色：保持你們系統的亮藍色 */
    font-size: 1.25rem;
}

/* 客製化深色模式下的按鈕 */
.btn-grafana {
    border-color: #4b5563;
    color: #9ca3af;
    background-color: transparent;
    font-size: 0.8rem;
    padding: 0.25rem 0.5rem;
    transition: all 0.2s;
}

.btn-grafana:hover {
    background-color: #374151;
    color: #ffffff;
    border-color: #6b7280;
}
</style>