<script setup>
// 全域 Toast 容器：固定於右下角浮動，不擠壓版面。於 App.vue 掛載一次即可全站共用。
import { useToast } from '@/composables/useToast';

const { toasts, remove } = useToast();

// 各型別對應的圖示與樣式 class
const config = {
  success: { icon: 'bi-check-circle-fill', cls: 'toast-success' },
  error: { icon: 'bi-exclamation-circle-fill', cls: 'toast-error' },
  info: { icon: 'bi-info-circle-fill', cls: 'toast-info' }
};
</script>

<template>
  <div class="toast-stack" aria-live="polite" aria-atomic="false">
    <transition-group name="toast">
      <div
        v-for="t in toasts"
        :key="t.id"
        class="toast-item shadow-sm"
        :class="config[t.type]?.cls"
        role="status"
      >
        <i class="bi flex-shrink-0" :class="config[t.type]?.icon"></i>
        <span class="toast-msg">{{ t.message }}</span>
        <button type="button" class="toast-close" @click="remove(t.id)" aria-label="關閉通知">
          <i class="bi bi-x-lg"></i>
        </button>
      </div>
    </transition-group>
  </div>
</template>

<style scoped>
.toast-stack {
  position: fixed;
  right: 1.25rem;
  bottom: 1.25rem;
  z-index: 1090; /* 高於 Bootstrap modal(1055)，確保操作回饋永遠可見 */
  display: flex;
  flex-direction: column;
  gap: 0.6rem;
  max-width: min(360px, calc(100vw - 2rem));
  pointer-events: none;
}

.toast-item {
  pointer-events: auto;
  display: flex;
  align-items: flex-start;
  gap: 0.6rem;
  padding: 0.75rem 0.9rem;
  border-radius: 12px;
  background: #ffffff;
  border: 1px solid var(--tap-light-gray, #e2e8f0);
  border-left-width: 4px;
  font-size: 0.9rem;
  line-height: 1.45;
  color: var(--tap-dark-blue, #1e293b);
}

.toast-item > .bi:first-child {
  font-size: 1.1rem;
  margin-top: 1px;
}

.toast-msg {
  flex: 1 1 auto;
  word-break: break-word;
}

.toast-close {
  flex-shrink: 0;
  border: none;
  background: transparent;
  color: var(--tap-light-blue, #64748b);
  font-size: 0.8rem;
  line-height: 1;
  padding: 2px;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.15s, background-color 0.15s;
}
.toast-close:hover {
  color: var(--tap-dark-blue, #1e293b);
  background-color: var(--tap-light-white, #f8fafc);
}

/* 型別配色：僅左側色條與圖示著色，主體維持乾淨白底 */
.toast-success { border-left-color: #10b981; }
.toast-success > .bi:first-child { color: #10b981; }

.toast-error { border-left-color: #ef4444; }
.toast-error > .bi:first-child { color: #ef4444; }

.toast-info { border-left-color: var(--tap-primary, #e57346); }
.toast-info > .bi:first-child { color: var(--tap-primary, #e57346); }

/* 進出場動畫：自右側滑入、淡出 */
.toast-enter-active,
.toast-leave-active {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}
.toast-enter-from {
  opacity: 0;
  transform: translateX(20px);
}
.toast-leave-to {
  opacity: 0;
  transform: translateX(20px);
}
.toast-leave-active {
  position: absolute;
  right: 0;
}
</style>
