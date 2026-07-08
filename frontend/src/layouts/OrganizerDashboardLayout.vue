<script setup>
/**
 * OrganizerDashboardLayout.vue - B2B 主辦方後台工作區佈局
 * 
 * 核心佈局特色：
 * 1. 滿版獨立滾動：
 *    整體 wrapper 採 vh-100 overflow-hidden 鎖死。
 *    中間為兩欄式彈性佈局，右側主內容 main 為獨立 overflow-auto 滾動區。
 *    Navbar 與 Sidebar 永不滑動，維持工具型軟體的高操作效能。
 * 
 * 2. RWD 左側頁籤式開關：
 *    窄版下以 fixed 頁籤按鈕取代頂部按鈕，釋放版面。
 *    對按鈕 focus / hover / active 與 transition 進行微調以防行動端事件殘留。
 */
import { ref, computed, watch, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { storeToRefs } from 'pinia';
import { useWorkspaceStore } from '@/stores/workspace.js';
import B2bNavbar from '@/components/common/B2bNavbar.vue';
import B2bSidebar from '@/components/common/B2bSidebar.vue';

const route = useRoute();
const isMobileSidebarOpen = ref(false);

const workspaceStore = useWorkspaceStore();
const { kycStatus, currentOrgId } = storeToRefs(workspaceStore);

onMounted(() => {
  if (route.params.organizerId) {
    workspaceStore.setOrgId(route.params.organizerId);
  }
});

// 當組織 ID 切換時重新載入狀態至 Pinia Store
watch(() => route.params.organizerId, (newId) => {
  if (newId) {
    workspaceStore.setOrgId(newId);
  }
});

// 判斷是否在組織設定區塊（profile / contracts / subscription 等頁籤）
// 此區塊內隱藏 KYC 橫幅：profile 頁本身已有相同提示（避免重複），且設定頁切換頁籤時
// 不希望表頭因橫幅出現／消失而上下抖動；進入設定區塊已能看到組織設定，不影響通知效果
const isOrgSettingsPage = computed(() => route.path.includes('/settings/'));

// Banner 的樣式配置
const kycBannerConfig = computed(() => {
  switch (kycStatus.value) {
    case 0: // DRAFT
      return {
        class: 'alert-secondary bg-secondary-subtle text-secondary-emphasis',
        icon: 'bi-info-circle-fill text-secondary',
        title: '尚未提交實名驗證 (KYC)',
        description: '您的組織帳戶目前處於草稿階段。請填妥基本資料並上傳證明文件提交驗證。通過驗證前，系統暫時限制「發布售票」與「提現結算」功能。'
      };
    case 1: // PENDING
      return {
        class: 'alert-warning bg-warning-subtle text-warning-emphasis',
        icon: 'bi-clock-history text-warning',
        title: '實名驗證等待審查中',
        description: '我們已收到您的實名認證申請，目前處於等待審查中狀態，管理團隊將於 1-3 個工作天內完成審核。在通過審核前，發布售票及提現功能暫時鎖定。'
      };
    case 3: // REJECTED
      return {
        class: 'alert-danger bg-danger text-white border-0',
        icon: 'bi-exclamation-octagon-fill text-white',
        title: '實名驗證退件',
        description: '您的驗證申請未通過審核，請前往查看退件原因並重新上傳正確文件以重新送審。'
      };
    default:
      return null;
  }
});
</script>

<template>
  <div class="d-flex flex-column vh-100 overflow-hidden b2b-dashboard-wrapper text-dark position-relative">
    <!-- 手機版專屬：左側懸浮頁籤按鈕 (桌機自動隱藏) -->
    <!-- 為防止側欄開啟時按鈕在遮罩下引發多餘觸控，此按鈕在 isMobileSidebarOpen 為 true 時會滑入螢幕左側外隱藏 -->
    <button 
      class="btn sidebar-trigger-btn d-lg-none" 
      :class="{ 'sidebar-open': isMobileSidebarOpen }"
      @click="isMobileSidebarOpen = true"
      aria-label="開啟管理選單"
    >
      <i class="bi bi-chevron-right fs-5"></i>
    </button>

    <!-- 頂部導覽列 -->
    <B2bNavbar />

    <!-- 中間側欄與內容區 -->
    <div class="d-flex flex-grow-1 overflow-hidden dashboard-body">
      <!-- 側邊欄 -->
      <B2bSidebar :isOpen="isMobileSidebarOpen" @close="isMobileSidebarOpen = false" />

      <!-- 主內容區 (獨立滾動) -->
      <main class="flex-grow-1 overflow-auto bg-light min-w-0 d-flex flex-column">
        <!-- 子頁面渲染區 -->
        <div class="flex-grow-1 p-4">
          
          <!-- KYC 警告橫幅 Banner (在組織設定區塊自動隱藏：避免重複並防止切換頁籤時表頭抖動) -->
          <div v-if="kycBannerConfig && !isOrgSettingsPage" class="alert border-0 shadow-sm rounded-4 p-3.5 mb-4 d-flex align-items-center justify-content-between text-start" :class="kycBannerConfig.class" role="alert">
            <div class="d-flex align-items-center gap-3">
              <i class="bi fs-4" :class="kycBannerConfig.icon"></i>
              <div>
                <strong class="d-block mb-1">{{ kycBannerConfig.title }}</strong>
                <span class="small opacity-90">{{ kycBannerConfig.description }}</span>
              </div>
            </div>
            <RouterLink :to="`/org/${currentOrgId}/settings/profile`" class="btn btn-sm btn-light fw-bold rounded-3 ms-3 text-nowrap focus-ring shadow-sm border">
              立即前往設定
            </RouterLink>
          </div>

          <RouterView />
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
.b2b-dashboard-wrapper {
  background-color: var(--tap-light-white, #f8fafc) !important;
}

/* 中間側欄與內容區，限制最小高度防彈性盒佈局溢出 */
.dashboard-body {
  min-height: 0;
}

/* 手機版懸浮左側頁籤按鈕 */
.sidebar-trigger-btn {
  position: fixed;
  left: 0;
  top: 50%;
  transform: translateY(-50%);
  z-index: 1035; /* 介於 Navbar (1030) 與 Offcanvas 遮罩 (1040) 之間 */
  background-color: var(--tap-dark-blue, #1e293b);
  color: #fff;
  border: none;
  border-radius: 0 10px 10px 0; /* 右側圓角，呈現拉環質感 */
  width: 28px;
  height: 70px;
  box-shadow: 3px 0 10px rgba(0, 0, 0, 0.2);
  display: flex;
  justify-content: center;
  align-items: center;
  transition: all 0.2s ease-in-out;
}

/* 當側欄開啟時，按鈕迅速往左滑出螢幕外隱藏 */
.sidebar-trigger-btn.sidebar-open {
  transform: translateY(-50%) translateX(-100%);
  opacity: 0;
  pointer-events: none;
}

/* 僅在支援 hover 的設備 (如桌機) 上套用懸浮動畫，避免行動端觸控產生 Sticky Hover 殘留 */
@media (hover: hover) {
  .sidebar-trigger-btn:hover {
    background-color: var(--tap-primary, #e57346);
    color: #fff;
    width: 34px; /* 懸浮時向右探出 */
  }
}

/* 手指按下瞬間的視覺回饋 */
.sidebar-trigger-btn:active {
  background-color: var(--tap-primary, #e57346);
  color: #fff;
  width: 34px;
}

/* 覆寫 Focus 與 Focus-visible 樣式。
   作用：當手機版側欄被關閉時，瀏覽器會自動將焦點 (Focus) 歸還給 trigger 按鈕。
   為防按鈕因此殘留全域的 focus 橘色高亮與變寬樣式，在此處將焦點狀態強制覆寫回預設深藍色/28px寬度。 */
.sidebar-trigger-btn:focus,
.sidebar-trigger-btn:focus-visible {
  background-color: var(--tap-dark-blue, #1e293b);
  color: #fff;
  width: 28px;
  outline: none;
  box-shadow: 3px 0 10px rgba(0, 0, 0, 0.2);
}
</style>
