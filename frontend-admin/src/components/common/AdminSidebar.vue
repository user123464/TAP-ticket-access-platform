<script setup>
/**
 * AdminSidebar.vue — Admin 後台深色側邊欄（動態選單版）
 *
 * 1. 響應式佈局 (Responsive Offcanvas)：桌機 (>=992px) 常駐側欄，手機 (<992px) 側拉抽屜。
 * 2. 避免 Resize 動畫抖動：僅在手動開關時短暫啟用過渡動畫。
 * 3. 選單來源：stores/menu.js（後端 system_resource 樹，GET /api/admin/menus），
 *    本檔只負責遞迴渲染（群組可展開、頁面為連結），選單內容由 DB 與權限決定。
 */
import { ref, onUnmounted, watch } from "vue";
import { storeToRefs } from "pinia";
import { useMenuStore } from "@/stores/menu.js";
import AdminSidebarNode from "@/components/common/AdminSidebarNode.vue";

const props = defineProps({
  isOpen: Boolean, // 控制手機版下側欄的顯示與隱藏
});

const emit = defineEmits(["close"]);

const menuStore = useMenuStore();
const { menus } = storeToRefs(menuStore);

// 僅在使用者手動開關時短暫啟用 0.3 秒過渡動畫，
// 調整視窗大小等非手動情境保持即時切換，避免視覺殘留。
const isTransitioning = ref(false);
let transitionTimeout = null;

watch(
  () => props.isOpen,
  () => {
    isTransitioning.value = true;
    if (transitionTimeout) clearTimeout(transitionTimeout);
    transitionTimeout = setTimeout(() => {
      isTransitioning.value = false;
    }, 350); // 350ms 涵蓋 0.3s 的 CSS 動畫時間
  }
);

onUnmounted(() => {
  if (transitionTimeout) clearTimeout(transitionTimeout);
});
</script>

<template>
  <aside
    class="offcanvas-lg offcanvas-start sidebar d-flex flex-column flex-shrink-0 p-3 border-0"
    :class="{ show: isOpen, 'sidebar-animate': isTransitioning }"
    id="sidebarOffcanvas"
  >
    <!-- 手機版專屬：關閉按鈕 (桌機自動隱藏) -->
    <div class="d-flex d-lg-none justify-content-end mb-2">
      <button
        type="button"
        class="btn-close btn-close-white shadow-none"
        @click="emit('close')"
        aria-label="Close"
      ></button>
    </div>

    <!-- 動態功能選單：遞迴渲染後端選單樹（點擊葉節點關閉手機版抽屜）
         （頂部品牌標題已移除，避免與 AdminNavbar 品牌重複；身分識別交給 Navbar） -->
    <ul class="nav nav-pills flex-column mb-auto gap-2">
      <AdminSidebarNode
        v-for="node in menus"
        :key="node.resourceId"
        :node="node"
        @navigate="emit('close')"
      />
    </ul>
  </aside>

  <!-- 手機版專屬半透明遮罩 (桌機自動隱藏) -->
  <div v-if="isOpen" class="offcanvas-backdrop fade show d-lg-none" @click="emit('close')"></div>
</template>

<style scoped>
.sidebar {
  width: 240px !important;
  /* !important 是為了在桌機版覆蓋 Bootstrap 將 offcanvas-lg 背景寫死為 transparent !important 的設定 */
  background-color: var(--tap-bg-surface) !important;
  height: 100% !important;
  border-right: none !important;
  overflow-y: auto; /* 選單過多或螢幕太矮時，側欄獨立滾動 */
}

/* 側欄滾動條：極窄透明感 */
.sidebar::-webkit-scrollbar {
  width: 4px;
}
.sidebar::-webkit-scrollbar-track {
  background: transparent;
}
.sidebar::-webkit-scrollbar-thumb {
  background: rgba(255, 255, 255, 0.15);
  border-radius: 4px;
}
.sidebar::-webkit-scrollbar-thumb:hover {
  background: rgba(255, 255, 255, 0.3);
}

/* 預設手機版下關閉過渡動畫（防 resize 觸發），僅手動開關時啟用 */
@media (max-width: 991.98px) {
  .sidebar {
    transition: none !important;
  }
  .sidebar.sidebar-animate {
    transition: transform 0.3s ease-in-out, visibility 0.3s ease-in-out !important;
  }
}

.sidebar-divider {
  border-color: rgba(255, 255, 255, 0.1) !important;
}

.sidebar-link {
  color: var(--tap-text-secondary);
  font-weight: 500;
  /* 僅過渡顏色與背景，避開寬度排版過渡防 resize 抖動 */
  transition: background-color 0.2s ease-in-out, color 0.2s ease-in-out;
  border-radius: 6px;
  padding: 0.75rem 1rem;
}

.sidebar-link:hover {
  background-color: var(--tap-bg-hover);
  color: var(--tap-text-primary);
}

/* 匹配當前作用路由（含子路由）時高亮 */
.sidebar-link.router-link-active {
  background-color: var(--tap-primary);
  color: #fff;
  font-weight: bold;
}

.sidebar-brand-subtitle {
  font-size: 0.72rem;
  letter-spacing: 1.5px;
  color: var(--tap-text-secondary);
}
</style>
