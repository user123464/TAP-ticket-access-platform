<script setup>
import { computed } from 'vue';
import { useRoute } from 'vue-router';
import { useSmartBack } from '@/composables/useSmartBack';

const route = useRoute();
const { goBack } = useSmartBack();

// 以「使用者嘗試前往的網址」判斷語境：404 的當前路徑即為使用者想去的目標，
// 比瀏覽歷史更可靠（直接貼錯網址進來時仍能正確分流）。同時兼顧 SPA 內跳轉的來源。
const isB2bContext = computed(() => {
  if (route.path.startsWith('/org')) return true;
  const prevPath = (window.history.state && window.history.state.back) || '';
  return prevPath.includes('/org');
});

const homePath = computed(() => (isB2bContext.value ? '/org/select' : '/'));
const homeText = computed(() => (isB2bContext.value ? '返回主辦空間' : '返回活動首頁'));
const homeIcon = computed(() => (isB2bContext.value ? 'bi-grid-1x2' : 'bi-house-door'));
</script>

<template>
  <div class="notfound-wrapper d-flex align-items-center justify-content-center px-3">
    <div class="card border-0 rounded-4 shadow-lg p-5 text-center bg-white modal-premium" style="max-width: 480px; width: 100%;">
      <!-- 404 圖示 -->
      <div class="icon-container mb-4 mx-auto d-flex align-items-center justify-content-center bg-primary-subtle rounded-circle text-primary" style="width: 80px; height: 80px;">
        <i class="bi bi-compass fs-1"></i>
      </div>

      <!-- 標題 -->
      <h2 class="fw-bold text-dark-blue mb-2">404 找不到頁面</h2>
      <p class="text-secondary small mb-4 px-2" style="line-height: 1.6;">
        很抱歉，您所尋找的頁面不存在、已被移除，或網址輸入有誤。您可以返回上一頁，或回到活動首頁繼續探索。
      </p>

      <!-- 按鈕群組 -->
      <div class="d-flex flex-column gap-2">
        <button @click="goBack" class="btn btn-premium rounded-3 py-2.5 fw-bold text-white shadow-sm transition-premium d-flex align-items-center justify-content-center gap-1">
          <i class="bi bi-arrow-left-short fs-5"></i>
          返回上一頁
        </button>
        <RouterLink :to="homePath" class="btn btn-cancel-modern rounded-3 py-2.5 fw-semibold d-flex align-items-center justify-content-center gap-2">
          <i class="bi" :class="homeIcon"></i>
          {{ homeText }}
        </RouterLink>
      </div>
    </div>
  </div>
</template>

<style scoped>
.notfound-wrapper {
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
  animation: compass-bounce 1s ease-in-out infinite alternate;
}

@keyframes compass-bounce {
  0% { transform: translateY(0); }
  100% { transform: translateY(-5px); }
}

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

.btn-premium {
  background: var(--tap-primary, #e57346);
  color: #ffffff;
  border: 1px solid transparent;
  font-size: 0.925rem;
  box-shadow: 0 4px 14px rgba(229, 115, 70, 0.25);
  cursor: pointer;
  border-radius: 12px !important;
}
.btn-premium:hover {
  background: #d45f32;
  box-shadow: 0 6px 20px rgba(229, 115, 70, 0.35);
  transform: translateY(-1px);
}
.btn-premium:active {
  transform: translateY(0);
}
</style>
