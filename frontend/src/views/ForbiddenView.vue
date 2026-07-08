<script setup>
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { useAuthStore } from '@/stores/auth.js';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

const cameFromB2b = ref(false);

onMounted(() => {
  // 是否來自 B2B 後台：守衛被擋時會帶 ?b2b=1（整頁重載時最可靠）；退而求其次看瀏覽器上一頁路徑。
  // 僅用於下方「返回首頁 / 切換帳號」兩顆按鈕的情境導向，不影響「返回上一頁」。
  const prevPath = (window.history.state && window.history.state.back) || '';
  cameFromB2b.value = route.query.b2b === '1' || prevPath.includes('/org');
});

// 返回上一頁：單純觸發瀏覽器「上一頁」，回到使用者剛剛真正所在的畫面（語意最直覺）。
// 僅在「真的沒有上一頁」（直接開啟此連結／新分頁）時，才依情境退回首頁，避免按了沒反應。
const handleBack = () => {
  if (window.history.length > 1) {
    router.go(-1);
  } else {
    router.replace(cameFromB2b.value ? '/org/select' : '/');
  }
};

const goHome = () => {
  if (cameFromB2b.value) {
    router.push('/org/select');
  } else {
    router.push('/');
  }
};

const handleSwitchAccount = async () => {
  // 依來源網頁類型決定登出後導向的登入頁面
  const targetLoginPath = cameFromB2b.value ? '/org/login' : '/login';
  await authStore.logout(targetLoginPath);
};
</script>

<template>
  <div class="forbidden-wrapper d-flex align-items-center justify-content-center px-3">
    <div class="card border-0 rounded-4 shadow-lg p-5 text-center bg-white modal-premium" style="max-width: 480px; width: 100%;">
      <!-- 403 鎖頭插圖 / 圖示 -->
      <div class="icon-container mb-4 mx-auto d-flex align-items-center justify-content-center bg-danger-subtle rounded-circle text-danger" style="width: 80px; height: 80px;">
        <i class="bi bi-shield-lock-fill fs-1"></i>
      </div>
      
      <!-- 標題 -->
      <h2 class="fw-bold text-dark-blue mb-2">403 拒絕存取</h2>
      <p class="text-secondary small mb-4 px-2" style="line-height: 1.6;">
        很抱歉，您目前的帳號沒有存取此頁面的權限。如果您嘗試進入主辦方後台，請確認您已受邀加入該組織，或聯絡該空間的管理員為您指派對應的角色。
      </p>

      <!-- 按鈕群組 -->
      <div class="d-flex flex-column gap-2">
        <button @click="handleBack" class="btn btn-premium-unlocked rounded-3 py-2.5 fw-bold text-white shadow-sm transition-premium d-flex align-items-center justify-content-center gap-1">
          <i class="bi bi-arrow-left-short fs-5"></i>
          返回上一頁
        </button>
        <button @click="goHome" class="btn btn-cancel-modern rounded-3 py-2.5 fw-semibold d-flex align-items-center justify-content-center gap-2">
          <i class="bi" :class="cameFromB2b ? 'bi-grid-1x2' : 'bi-house-door'"></i>
          {{ cameFromB2b ? '返回主辦空間選擇頁' : '返回首頁' }}
        </button>
        <button @click="handleSwitchAccount" class="btn btn-link-modern rounded-3 py-2 mt-2 fw-semibold">
          <i class="bi bi-person-x me-1"></i>
          切換帳號登入
        </button>
      </div>
    </div>
  </div>
</template>

<style scoped>
.forbidden-wrapper {
  min-height: 100vh;
  background-color: #f8fafc;
}

.modal-premium {
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 10px 10px -5px rgba(0, 0, 0, 0.02) !important;
}

.text-dark-blue {
  color: #1e293b;
}

.icon-container {
  animation: lock-bounce 1s ease-in-out infinite alternate;
}

@keyframes lock-bounce {
  0% { transform: translateY(0); }
  100% { transform: translateY(-5px); }
}

/* 現代按鈕設計與轉場 */
.transition-premium {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-cancel-modern {
  background: #ffffff;
  border: 1px solid #e2e8f0 !important;
  color: #64748b;
  font-size: 0.925rem;
  font-weight: 600;
  padding: 10px 24px;
  border-radius: 12px;
  transition: all 0.2s;
  cursor: pointer;
}
.btn-cancel-modern:hover {
  background: #f8fafc;
  color: #334155;
  border-color: #cbd5e1 !important;
}

.btn-premium-unlocked {
  background: var(--tap-primary, #e57346);
  color: #ffffff;
  border: 1px solid transparent;
  font-size: 0.925rem;
  box-shadow: 0 4px 14px rgba(229, 115, 70, 0.25);
  cursor: pointer;
  border-radius: 12px !important;
}
.btn-premium-unlocked:hover {
  background: #d45f32;
  box-shadow: 0 6px 20px rgba(229, 115, 70, 0.35);
  transform: translateY(-1px);
}
.btn-premium-unlocked:active {
  transform: translateY(0);
}

.btn-link-modern {
  background: transparent;
  border: none;
  color: #64748b;
  font-size: 0.875rem;
  transition: color 0.2s;
  cursor: pointer;
}
.btn-link-modern:hover {
  color: #1e293b;
}
</style>
