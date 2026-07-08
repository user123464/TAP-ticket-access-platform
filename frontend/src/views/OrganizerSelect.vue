<script setup>
import { ref, onMounted } from 'vue';
import { storeToRefs } from 'pinia';
import { useRouter } from 'vue-router';
import BaseModal from '@/components/common/BaseModal.vue';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast';
import { useWorkspaceStore } from '@/stores/workspace.js';

const BACKEND_URL = import.meta.env.VITE_BACKEND_API || 'http://localhost:8080';

const router = useRouter();
const toast = useToast();
const userName = ref('');
// 組織清單唯一來源：workspace store
const workspaceStore = useWorkspaceStore();
const { myOrgs: userOrgs } = storeToRefs(workspaceStore);

// 後端頭像 URL（/api/...）需補上 backend base；data:/http 等其他來源原樣回傳
const resolveLogo = (logo) => {
  if (!logo) return logo;
  return logo.startsWith('/api/') ? BACKEND_URL + logo : logo;
};

// 建立組織 Modal 狀態
const showCreateModal = ref(false);
const newOrgName = ref('');
const isCreating = ref(false); // 提交期間鎖定按鈕，防止連點重複建立組織

// 載入組織列表（自 workspace store 強制向後端重新拉取，確保啟動台為最新狀態）
const loadOrgs = async () => {
  userName.value = localStorage.getItem('user_name') || '主辦方成員';
  await workspaceStore.fetchMyOrgs(true);
};

// 選擇組織進入控制台（落點交由組織根 beforeEnter 依 firstAccessiblePath 決定）
const enterOrg = (orgId) => {
  router.push(`/org/${orgId}`);
};

// 提交建立新組織
const submitCreateOrg = async () => {
  const orgName = newOrgName.value.trim();
  if (!orgName || isCreating.value) return;

  isCreating.value = true;
  try {
    // 呼叫後端建立組織 API
    const res = await axios.post('/api/organizer', { name: orgName });
    const newOrg = res.data; // 回傳 MyOrganizationResponse

    // 1. 新增至組織清單唯一來源（store），側欄/導覽列/守衛即時同步
    workspaceStore.upsertOrg(newOrg);

    // 2. 重置與關閉
    newOrgName.value = '';
    showCreateModal.value = false;

    // 3. 建立成功後進入該組織根（落點交由 beforeEnter 依 firstAccessiblePath 決定）
    router.push(`/org/${newOrg.id}`);
  } catch (error) {
    toast.error('建立失敗：' + (error.response?.data?.message || error.message));
  } finally {
    isCreating.value = false;
  }
};

onMounted(() => {
  loadOrgs();
});
</script>

<template>
  <div class="container py-5" style="max-width: 960px;">
    <div class="text-center mb-5">
      <h2 class="fw-bold mb-2">歡迎回來，{{ userName }}！</h2>
      <p class="text-secondary fs-6">請選擇您要管理的工作空間，或建立一個新的組織。</p>
    </div>

    <!-- 組織卡片網格 -->
    <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
      <!-- 既有組織 -->
      <div v-for="org in userOrgs" :key="org.id" class="col">
        <div class="card h-100 border rounded-4 shadow-sm hover-card cursor-pointer" @click="enterOrg(org.id)">
          <div class="card-body p-4 d-flex flex-column align-items-center text-center">
            <img :src="resolveLogo(org.logo)" alt="Org Logo" class="rounded-circle border mb-3" style="width: 70px; height: 70px; object-fit: cover; background-color: #f8f9fa;" />
            <h5 class="fw-bold text-dark text-truncate w-100 mb-1" :title="org.name">{{ org.name }}</h5>
            <span class="badge bg-light text-secondary border mb-3" style="font-size: 0.75rem;">管理員</span>
            <div class="mt-auto pt-2 text-primary small fw-semibold">
              進入管理後台 <i class="bi bi-arrow-right ms-1"></i>
            </div>
          </div>
        </div>
      </div>

      <!-- 建立新組織按鈕卡片 -->
      <div class="col">
        <div class="card h-100 border border-dashed rounded-4 shadow-none hover-card-dashed cursor-pointer" @click="showCreateModal = true">
          <div class="card-body p-4 d-flex flex-column align-items-center justify-content-center text-center py-5">
            <div class="create-icon-wrapper mb-3 text-secondary d-flex align-items-center justify-content-center rounded-circle" style="width: 60px; height: 60px; background-color: #f1f5f9;">
              <i class="bi bi-plus-lg fs-3"></i>
            </div>
            <h5 class="fw-bold text-secondary mb-1">建立新組織</h5>
            <p class="text-muted small mb-0">點擊建立新的活動主辦單位</p>
          </div>
        </div>
      </div>
    </div>

    <!-- 建立新組織的彈窗 -->
    <BaseModal v-model:show="showCreateModal" title="建立新組織空間">
      <form @submit.prevent="submitCreateOrg">
        <div class="mb-3 text-start">
          <label for="orgNameInput" class="form-label fw-semibold text-secondary">組織/主辦空間名稱</label>
          <input
            type="text"
            class="form-control form-control-lg focus-ring animate-fade-in"
            id="orgNameInput"
            v-model="newOrgName"
            required
            :disabled="isCreating"
            placeholder="請輸入組織名稱"
            autofocus
          />
          <div class="form-text small text-muted mt-2">
            建立後即可進入主辦空間建立活動草稿。您隨時可以前往「組織設定」補齊認證資料進行實名驗證（KYC）。
          </div>
        </div>
        <div class="d-flex justify-content-end gap-2 mt-4">
          <button type="button" class="btn btn-light px-3 py-2 rounded-3 border fw-semibold" :disabled="isCreating" @click="showCreateModal = false">取消</button>
          <button
            type="submit"
            class="btn btn-primary px-4 py-2 rounded-3 text-white fw-bold shadow-sm btn-loading-keep-width"
            :class="{ 'is-loading': isCreating }"
            :disabled="!newOrgName.trim() || isCreating"
          >
            <span v-if="isCreating" class="spinner-border spinner-border-sm btn-loading-keep-width__spinner" role="status" aria-hidden="true"></span>
            <span class="btn-loading-keep-width__label">建立並進入後台</span>
          </button>
        </div>
      </form>
    </BaseModal>
  </div>
</template>

<style scoped>
.cursor-pointer {
  cursor: pointer;
}

.border-dashed {
  border-style: dashed !important;
  border-width: 2px !important;
  border-color: #cbd5e1 !important;
}

.hover-card {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.hover-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0, 0, 0, 0.08) !important;
  border-color: var(--tap-primary, #e57346) !important;
}

.hover-card-dashed {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
  background-color: #f8fafc;
}

.hover-card-dashed:hover {
  border-color: var(--tap-primary, #e57346) !important;
  background-color: #ffffff;
}

.hover-card-dashed:hover .create-icon-wrapper {
  color: var(--tap-primary, #e57346) !important;
  background-color: rgba(229, 115, 70, 0.1) !important;
}
</style>
