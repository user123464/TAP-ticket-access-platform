<script setup>
/**
 * AdminLayout.vue — Admin 後台主佈局（深色三欄式）
 *
 * 佈局結構承襲 B2B OrganizerDashboardLayout 的設計：
 * 1. 滿版獨立滾動：wrapper 採 vh-100 overflow-hidden 鎖死，
 *    右側主內容 main 為獨立 overflow-auto 滾動區，Navbar 與 Sidebar 永不滑動。
 * 2. RWD 左側頁籤式開關：窄版 (<992px) 以 fixed 頁籤按鈕喚出側欄抽屜。
 *
 * 與 B2B 不同：無組織切換 / KYC Banner，選單改由 menu store 動態載入。
 */
import { ref, onMounted } from "vue";
import { useMenuStore } from "@/stores/menu.js";
import { useAuthStore } from "@/stores/auth.js";
import AdminNavbar from "@/components/common/AdminNavbar.vue";
import AdminSidebar from "@/components/common/AdminSidebar.vue";

const isMobileSidebarOpen = ref(false);

const menuStore = useMenuStore();
const authStore = useAuthStore();

onMounted(() => {
  // 進入後台即載入動態選單（RBAC 過濾由後端處理）
  menuStore.fetchMenus();
});
</script>

<template>
  
  <div class="d-flex flex-column vh-100 overflow-hidden admin-wrapper position-relative">
    <!-- 手機版專屬：左側懸浮頁籤按鈕 (桌機自動隱藏) -->
    <button
      v-if="!authStore.user?.mustChangePassword"
      class="btn sidebar-trigger-btn d-lg-none"
      :class="{ 'sidebar-open': isMobileSidebarOpen }"
      @click="isMobileSidebarOpen = true"
      aria-label="開啟管理選單"
    >
      <i class="bi bi-chevron-right fs-5"></i>
    </button>

    <!-- 頂部導覽列 -->
    <AdminNavbar />

    <!-- 中間側欄與內容區 -->
    <div class="d-flex flex-grow-1 overflow-hidden admin-body">
      <!-- 側邊欄 -->
      <AdminSidebar v-if="!authStore.user?.mustChangePassword" :isOpen="isMobileSidebarOpen" @close="isMobileSidebarOpen = false" />

      <!-- 主內容區 (獨立滾動) -->
      <main class="flex-grow-1 overflow-auto min-w-0 d-flex flex-column">
        <div class="flex-grow-1 admin-compact">
          <RouterView />
        </div>
      </main>
    </div>
  </div>

</template>

<style scoped>
.admin-wrapper {
  background-color: var(--tap-bg-base);
}

/* 限制最小高度防彈性盒佈局溢出 */
.admin-body {
  min-height: 0;
}

/* 手機版懸浮左側頁籤按鈕 */
.sidebar-trigger-btn {
  position: fixed;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1035; /* 介於 Navbar (1030) 與 Offcanvas 遮罩 (1040) 之間 */
  background-color: var(--tap-bg-surface);
  color: var(--tap-text-primary);
  border: none;
  border-radius: 0 10px 10px 0;
  width: 28px;
  height: 70px;
  box-shadow: 3px 0 10px rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s ease-in-out;
}

/* 側欄開啟時按鈕滑出螢幕外隱藏，防遮罩下誤觸 */
.sidebar-trigger-btn.sidebar-open {
  transform: translateY(-50%) translateX(-100%);
  opacity: 0;
  pointer-events: none;
}

/* 僅在支援 hover 的設備上套用懸浮動畫，避免行動端 Sticky Hover 殘留 */
@media (hover: hover) {
  .sidebar-trigger-btn:hover {
    background-color: var(--tap-primary);
    color: #fff;
    width: 34px;
  }
}

.sidebar-trigger-btn:active {
  background-color: var(--tap-primary);
  color: #fff;
  width: 34px;
}

/* 側欄關閉後焦點歸還按鈕時，強制覆寫回預設樣式防殘留高亮 */
.sidebar-trigger-btn:focus,
.sidebar-trigger-btn:focus-visible {
  background-color: var(--tap-bg-surface);
  color: var(--tap-text-primary);
  width: 28px;
  outline: none;
  box-shadow: 3px 0 10px rgba(0, 0, 0, 0.5);
}
</style>
