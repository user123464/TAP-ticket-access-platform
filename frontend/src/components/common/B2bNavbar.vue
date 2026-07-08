<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { storeToRefs } from 'pinia';
import { Dropdown } from 'bootstrap';
import SupportModal from '@/components/common/SupportModal.vue';
import BaseAvatar from '@/components/common/BaseAvatar.vue';
import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';
import { getOrgDefaultLogo } from '@/constants/avatar.js';

const BACKEND_URL = import.meta.env.VITE_BACKEND_API || 'http://localhost:8080';

// 後端頭像 URL（/api/...）需補上 backend base；data:/http 等其他來源原樣回傳
const resolveLogo = (logo) => {
  if (!logo) return logo;
  return logo.startsWith('/api/') ? BACKEND_URL + logo : logo;
};

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();
const workspaceStore = useWorkspaceStore();

const isLoggedIn = computed(() => authStore.isLoggedIn);
const userName = computed(() => authStore.userName);
const userAvatar = computed(() => authStore.userAvatar);
// 組織清單唯一來源：workspace store（來自後端 my-organizations，不再寫死假資料）
const { myOrgs: userOrgs } = storeToRefs(workspaceStore);
const showSupport = ref(false);

const dropdownRef = ref(null);
let dropdownInstance = null;

// 解析當前路由參數中的 organizerId
const currentOrgId = computed(() => route.params.organizerId);

// 獲取當前組織詳細資料
const currentOrg = computed(() => {
  if (!currentOrgId.value) return null;
  return userOrgs.value.find(org => org.id === currentOrgId.value) || {
    id: currentOrgId.value,
    name: '未知主辦單位',
    logo: getOrgDefaultLogo('Unknown')
  };
});

// 其他可切換的組織列表 (不包含當前所屬組織)
const otherOrgs = computed(() => {
  if (!currentOrgId.value) return userOrgs.value;
  return userOrgs.value.filter(org => org.id !== currentOrgId.value);
});

// 載入組織列表 (僅登入狀態，向後端拉取；store 已載入則不重複請求)
const loadOrgs = () => {
  if (isLoggedIn.value) {
    workspaceStore.fetchMyOrgs();
  }
};

// 處理登出 (調用 Pinia Store，將頁面導向 B2B 登入頁面)
const handleLogout = async () => {
  await authStore.logout('/org/login');
};

// 切換組織（落點交由組織根 beforeEnter 依 firstAccessiblePath 決定）
const switchOrg = (orgId) => {
  router.push(`/org/${orgId}`);
};

watch(isLoggedIn, async (newVal) => {
  if (newVal) {
    await nextTick();
    if (dropdownRef.value) {
      dropdownInstance = new Dropdown(dropdownRef.value);
    }
  } else {
    if (dropdownInstance) {
      dropdownInstance.dispose();
      dropdownInstance = null;
    }
  }
});

onMounted(async () => {
  loadOrgs();

  // 若初始掛載時已是登入狀態，手動初始化選單以防 watch 未觸發
  if (isLoggedIn.value) {
    await nextTick();
    if (dropdownRef.value && !dropdownInstance) {
      dropdownInstance = new Dropdown(dropdownRef.value);
    }
  }
});

onUnmounted(() => {
  if (dropdownInstance) {
    dropdownInstance.dispose();
  }
});
</script>

<template>
  <header>
    <nav class="navbar navbar-expand-lg navbar-light bg-white border-top border-primary border-5 shadow-sm py-2" style="position: relative; z-index: 1030;">
      <div class="container px-4">
        <!-- Logo 與品牌名稱 -->
        <div class="d-flex align-items-center gap-3">
          <RouterLink class="navbar-brand d-flex align-items-center gap-2" to="/org/select">
            <img src="@/assets/images/logo.png" alt="TAP Logo" class="brand-logo" />
            <span class="brand-name">TAP</span>
            <span class="brand-subtitle text-muted fs-6 fw-normal ms-1 d-none d-sm-inline">主辦方平台</span>
          </RouterLink>
        </div>

        <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu"
          aria-controls="navMenu" aria-expanded="false" aria-label="Toggle navigation">
          <span class="navbar-toggler-icon"></span>
        </button>

        <div class="collapse navbar-collapse" id="navMenu">
          <div class="navbar-nav ms-auto d-flex align-items-start align-items-lg-center gap-3">
            <RouterLink class="nav-link b2b-nav-link" to="/">
              <i class="bi bi-box-arrow-up-right me-1"></i> 前往活動前台
            </RouterLink>
            <RouterLink class="nav-link b2b-nav-link" to="/org/docs/guide">
              <i class="bi bi-question-circle me-1"></i> 使用說明
            </RouterLink>
            <a class="nav-link b2b-nav-link" href="#" @click.prevent="showSupport = true">
              <i class="bi bi-headset me-1"></i> 聯絡支援
            </a>

            <!-- 未登入 -->
            <RouterLink v-if="!isLoggedIn" class="nav-link b2b-nav-link" to="/org/login">
              <i class="bi bi-box-arrow-in-right me-1"></i> 登入
            </RouterLink>

            <!-- 已登入 (綜合下拉選單) -->
            <div v-else class="nav-item dropdown d-flex align-items-center">
              <a ref="dropdownRef" class="nav-link text-dark dropdown-toggle d-flex align-items-center gap-2" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                <BaseAvatar :src="userAvatar" :seed="userName" size="28" alt="Avatar" />
                <span class="fw-semibold">
                  <span>{{ userName }}</span>
                </span>
              </a>
              
              <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2 p-2" style="min-width: 240px;">
                <!-- 當前組織資訊區 -->
                <li v-if="currentOrg" class="px-3 py-2 bg-light rounded-3 mb-2">
                  <div class="small text-muted mb-1">當前主辦單位</div>
                  <div class="fw-bold text-dark d-flex align-items-center gap-2">
                    <img :src="resolveLogo(currentOrg.logo)" class="rounded-circle border" style="width: 20px; height: 20px; object-fit: cover;" />
                    <span class="text-truncate" style="max-width: 160px;">{{ currentOrg.name }}</span>
                  </div>
                  <div class="small text-secondary mt-1">代碼: {{ currentOrg.id }}</div>
                </li>

                <!-- 切換組織選項 -->
                <template v-if="userOrgs.length > 0">
                  <li v-if="currentOrg"><hr class="dropdown-divider"></li>
                  <li>
                    <h6 class="dropdown-header px-2 text-muted">切換主辦空間</h6>
                  </li>
                  <!-- 其他組織列表 (只顯示前三個，防超量失控) -->
                  <li v-for="org in otherOrgs.slice(0, 3)" :key="org.id">
                    <a class="dropdown-item d-flex align-items-center gap-2 py-2 rounded-2" href="#" @click.prevent="switchOrg(org.id)">
                      <img :src="resolveLogo(org.logo)" class="rounded-circle border" style="width: 18px; height: 18px; object-fit: cover;" />
                      <span class="text-truncate" style="max-width: 180px;">{{ org.name }}</span>
                    </a>
                  </li>
                  <!-- 統一收納為一個動態入口，兼顧「查看全部」與「管理建立」的需求 -->
                  <li>
                    <RouterLink class="dropdown-item d-flex align-items-center gap-2 py-2 rounded-2 text-secondary" to="/org/select">
                      <i class="bi bi-grid-1x2"></i>
                      <span>{{ userOrgs.length > 3 ? `查看全部 ${userOrgs.length} 個主辦單位...` : '管理 / 建立主辦單位...' }}</span>
                    </RouterLink>
                  </li>
                </template>

                <li><hr class="dropdown-divider"></li>
                
                <!-- 個人帳號管理 -->
                <li>
                  <RouterLink class="dropdown-item d-flex align-items-center gap-2 py-2 rounded-2" to="/org/settings/profile">
                    <i class="bi bi-person-gear"></i>個人帳號管理
                  </RouterLink>
                </li>
                <li>
                  <a class="dropdown-item text-danger d-flex align-items-center gap-2 py-2 rounded-2" href="#" @click.prevent="handleLogout">
                    <i class="bi bi-box-arrow-right"></i>登出主辦方平台
                  </a>
                </li>
              </ul>
            </div>

          </div>
        </div>
      </div>
    </nav>

    <!-- 聯絡支援 Modal -->
    <SupportModal v-model:show="showSupport" />
  </header>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400..800&display=swap');

.brand-name {
  font-family: 'Syne', sans-serif;
  font-weight: 700;
  font-size: 1.4rem;
  letter-spacing: -0.5px;
  color: var(--tap-dark-blue, #1e293b) !important;
}

.brand-logo {
  height: 32px;
  width: auto;
  object-fit: contain;
}

.b2b-nav-link {
  color: var(--tap-light-blue, #64748b) !important;
  font-weight: 500;
  transition: color 0.2s ease-in-out;
}
.b2b-nav-link:hover, 
.b2b-nav-link.router-link-active {
  color: var(--tap-primary, #e57346) !important;
}

/* 確保下拉選單在小螢幕上不會偏離，且與 B2C 一致靠左對齊 */
@media (max-width: 991.98px) {
  .navbar-nav .dropdown {
    position: relative;
  }
  .navbar-nav .dropdown-menu {
    position: absolute !important;
    right: auto !important;
    left: 0 !important;
  }
}
</style>
