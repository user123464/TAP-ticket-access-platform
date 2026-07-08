<script setup>
// 共用的「標題 + 返回鈕 + 頁籤導覽 + 子路由」版面殼層。
// 由 SettingsLayout / OrgSettingsLayout / TeamLayout 共用，消除三份重複的 nav-tabs 樣式與結構。
defineProps({
  title: { type: String, required: true },
  titleIcon: { type: String, default: '' }, // 完整 icon class，例如 'bi-gear-fill text-primary'
  backTo: { type: [String, Object], required: true },
  backText: { type: String, default: '返回' },
  // 頁籤陣列：[{ to, label, icon }]
  tabs: { type: Array, default: () => [] },
  maxWidth: { type: String, default: '1200px' }
});
</script>

<template>
  <div class="container py-5" :style="{ maxWidth }">
    <!-- 頁面標題 + 返回按鈕 -->
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-4">
      <h2 class="fw-bold d-flex align-items-center gap-2 mb-0" style="font-size: 1.75rem;">
        <i v-if="titleIcon" class="bi" :class="titleIcon"></i> {{ title }}
      </h2>
      <RouterLink :to="backTo" class="btn btn-outline-secondary btn-sm rounded-3 fw-semibold px-3 py-2 d-inline-flex align-items-center">
        <i class="bi bi-arrow-left me-2"></i>{{ backText }}
      </RouterLink>
    </div>

    <!-- 頁籤導覽 (利用 router-link-active 自動高亮) -->
    <ul class="nav nav-tabs mb-4 border-bottom-gray flex-nowrap tabs-scroll">
      <li class="nav-item" v-for="tab in tabs" :key="typeof tab.to === 'string' ? tab.to : tab.label">
        <RouterLink class="nav-link d-inline-flex align-items-center gap-2 text-nowrap" :to="tab.to">
          <i class="bi" :class="tab.icon"></i>{{ tab.label }}
        </RouterLink>
      </li>
    </ul>

    <!-- 子路由畫面掛載 -->
    <RouterView />
  </div>
</template>

<style scoped>
.border-bottom-gray {
  border-bottom: 1px solid var(--tap-light-gray, #e2e8f0) !important;
}

/* 窄螢幕：頁籤不換行，改水平捲動（隱藏捲軸） */
.tabs-scroll {
  overflow-x: auto;
  overflow-y: hidden;
  -webkit-overflow-scrolling: touch;
  scrollbar-width: none;
}
.tabs-scroll::-webkit-scrollbar {
  display: none;
}
.tabs-scroll .nav-item {
  flex-shrink: 0;
}

.nav-tabs {
  border-bottom: none;
}

.nav-tabs .nav-link {
  border: none;
  color: var(--tap-light-blue, #64748b);
  padding: 0.75rem 1.25rem;
  font-size: 0.95rem;
  position: relative;
  background: transparent;
  transition: color 0.2s;
  font-weight: 500;
}

.nav-tabs .nav-link:hover {
  color: var(--tap-primary, #e57346);
  border: none;
}

.nav-tabs :deep(.router-link-active) {
  color: var(--tap-primary, #e57346) !important;
  font-weight: 700 !important;
  border: none;
  background: transparent;
}

.nav-tabs :deep(.router-link-active)::after {
  content: '';
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  height: 3px;
  background-color: var(--tap-primary, #e57346);
  border-radius: 3px 3px 0 0;
}

.btn-outline-secondary {
  color: var(--tap-light-blue, #64748b);
  border-color: var(--tap-light-gray, #e2e8f0);
  background-color: transparent;
}

.btn-outline-secondary:hover {
  background-color: var(--tap-light-gray, #e2e8f0);
  border-color: var(--tap-light-gray, #e2e8f0);
  color: var(--tap-dark-blue, #1e293b);
}
</style>
