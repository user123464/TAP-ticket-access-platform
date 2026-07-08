<script setup>
/**
 * B2bSidebar.vue - 主辦方後台側邊欄導航元件
 * 
 * 核心設計說明：
 * 1. 響應式佈局 (Responsive Offcanvas)：在桌機版 (>=992px) 做為常駐側欄，在手機版 (<992px) 做為側拉抽屜。
 * 2. 避免 Resize 動畫抖動 (wasOpened)：監聽手動開啟狀態，避免在手動調整瀏覽器視窗大小時觸發過渡動畫造成視覺殘留。
 * 3. 獨立滾動與美化：側欄獨立支援 overflow-y: auto，以防未來管理選單項目過多造成垂直截斷，並美化滾動條以符合後台精緻美感。
 */
import { computed, ref, onMounted, onUnmounted, watch } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useWorkspaceStore } from '@/stores/workspace.js';
// [Jason] RBAC：側欄直接渲染後端「依該組織角色過濾後」的選單樹（取代寫死的 8 個 <li>）
import { useMenuStore } from '@/stores/menu.js';
import B2bSidebarNode from '@/components/common/B2bSidebarNode.vue';

const props = defineProps({
  isOpen: Boolean // 控制手機版下側欄的顯示與隱藏
});

const emit = defineEmits(['close']);

// 僅在使用者手動觸發開啟或關閉時，短暫啟用 0.3 秒過渡動畫。
// 在非手動觸發（如調整瀏覽器視窗大小）時，保持 0 秒即時切換，避免畫面尺寸改變時出現過渡動畫。
const isTransitioning = ref(false);
let transitionTimeout = null;

watch(() => props.isOpen, () => {
  isTransitioning.value = true;
  if (transitionTimeout) clearTimeout(transitionTimeout);
  transitionTimeout = setTimeout(() => {
    isTransitioning.value = false;
  }, 350); // 350ms 涵蓋 0.3s 的 CSS 動畫時間
});

const route = useRoute();
const organizerId = computed(() => route.params.organizerId);

// 組織清單唯一來源：workspace store（與導覽列、路由守衛共用同一份清單）
const workspaceStore = useWorkspaceStore();
const { myOrgs: userOrgs } = storeToRefs(workspaceStore);
const currentOrg = computed(() => {
  if (!organizerId.value) return null;
  return userOrgs.value.find(org => org.id === organizerId.value) || {
    id: organizerId.value,
    name: '未知主辦單位'
  };
});

// [Jason] RBAC：menuTree 為後端「依該組織角色過濾後」的選單樹，只含可見項，
// 側欄直接遞迴渲染即可，不再需要逐項 canSee 判斷。
const menuStore = useMenuStore();
const { menuTree } = storeToRefs(menuStore);

onMounted(() => {
  // 確保清單已載入（store 已載入則不重複請求）
  workspaceStore.fetchMyOrgs();
  // 載入後端依「該組織角色」過濾後的 B2B 選單樹（供 canSee 判斷；失敗時 store 沿用快取/不誤判）
  if (organizerId.value) menuStore.fetchMenu(organizerId.value);
});

// [Jason] RBAC #4b：選單為 per-org，切換組織時重新拉取對應選單
watch(organizerId, (newId) => {
  if (newId) menuStore.fetchMenu(newId);
});

onUnmounted(() => {
  if (transitionTimeout) clearTimeout(transitionTimeout);
});
</script>

<template>
  <aside 
    class="offcanvas-lg offcanvas-start sidebar d-flex flex-column flex-shrink-0 p-3 text-white border-0" 
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

    <!-- 頂部組織資訊與後台標籤 -->
    <!-- 為防止長名稱與短名稱切換時導致頂部高度變更、造成版面微小跳動，此容器設定固定高度與 flex 垂直置中 -->
    <div class="mb-4 text-center pb-3 border-bottom border-white border-opacity-10 text-white">
      <div 
        class="fw-bold px-2 d-flex align-items-center justify-content-center sidebar-brand-name" 
        :class="currentOrg && currentOrg.name.length > 8 ? 'fs-6' : 'fs-5'"
        :title="currentOrg?.name"
      >
        <span 
          class="text-truncate text-start"
        >
          {{ currentOrg ? currentOrg.name : '未知主辦單位' }}
        </span>
      </div>
      <div class="small text-white-50 mt-1 fw-semibold text-uppercase sidebar-brand-subtitle">
        後台管理中心
      </div>
    </div>
    
    <!-- 功能選單：遞迴分層渲染（靜態群組分類，不折疊） -->
    <ul class="nav nav-pills flex-column mb-auto gap-2">
      <B2bSidebarNode
        v-for="node in menuTree"
        :key="node.resourceId"
        :node="node"
        :organizer-id="organizerId"
        @navigate="emit('close')"
      />
    </ul>

  </aside>

  <!-- 手機版專屬半透明遮罩 (桌機自動隱藏) -->
  <div 
    v-if="isOpen" 
    class="offcanvas-backdrop fade show d-lg-none" 
    @click="emit('close')"
  ></div>
</template>

<style scoped>
.sidebar {
  width: 240px !important;
  /* 此處使用 !important 是為了在桌機版覆蓋 Bootstrap 內建將 offcanvas-lg 寫死為 transparent !important 的設定 */
  background-color: var(--tap-dark-blue, #1e293b) !important;
  height: 100% !important;
  border-right: none !important;
  overflow-y: auto; /* 當左側標籤超長或螢幕太矮時，側欄支援獨立滾動 */
}

/* 美化側欄滾動條，使其呈現極窄、精緻的透明感，不影響整體設計美感 */
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

/* 預設手機版下關閉過渡動畫（防 resize 觸發動畫），僅在手動觸發 transition 時啟用動畫 */
@media (max-width: 991.98px) {
  .sidebar {
    transition: none !important;
  }
  .sidebar.sidebar-animate {
    transition: transform 0.3s ease-in-out, visibility 0.3s ease-in-out !important;
  }
}

.sidebar-link {
  color: var(--tap-light-gray, #e2e8f0);
  font-weight: 500;
  /* 僅對顏色和背景進行動畫過渡，避開對寬度與排版的過渡，以防止調整螢幕寬度時發生排版拉扯抖動 */
  transition: background-color 0.2s ease-in-out, color 0.2s ease-in-out;
  border-radius: 6px;
  padding: 0.75rem 1rem;
}

.sidebar-link:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}

/* 匹配當前作用路由（子路由）時高亮 */
.sidebar-link.router-link-active {
  background-color: var(--tap-primary, #e57346);
  color: #fff;
  font-weight: bold;
}

/* 組織名稱容器，固定高度防止切換公司時高度抖動 */
.sidebar-brand-name {
  height: 32px;
}

/* 後台管理中心副標題精緻排版 */
.sidebar-brand-subtitle {
  font-size: 0.72rem;
  letter-spacing: 1.5px;
}
</style>
