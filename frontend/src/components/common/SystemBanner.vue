<script setup>
// 全站系統公告條：位於整頁最頂端（navbar 上方），顯示時把整頁往下推，隱藏時不占版面。
// 於 App.vue 的 RouterView 之前掛載一次。
import { computed } from 'vue';
import { bannerState, useSystemBanner } from '@/composables/useSystemBanner';

const { dismiss } = useSystemBanner();

const iconClass = computed(() => ({
  warning: 'bi-megaphone-fill',
  danger: 'bi-wifi-off',
  info: 'bi-info-circle-fill'
}[bannerState.type] || 'bi-info-circle-fill'));
</script>

<template>
  <transition name="banner">
    <div v-if="bannerState.show" class="system-banner" :class="`sb-${bannerState.type}`" role="alert">
      <div class="banner-inner">
        <span class="banner-msg">
          <i class="bi me-2" :class="iconClass"></i>{{ bannerState.message }}
        </span>
      </div>
      <button
        v-if="bannerState.dismissible"
        type="button"
        class="banner-close"
        @click="dismiss"
        aria-label="關閉系統公告"
      >
        <i class="bi bi-x-lg"></i>
      </button>
    </div>
  </transition>
</template>

<style scoped>
.system-banner {
  width: 100%;
  font-size: 0.9rem;
  line-height: 1.4;
  position: relative;
}

.banner-inner {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0.6rem 3rem;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 1rem;
}

.banner-msg {
  text-align: center;
}

.banner-close {
  position: absolute;
  top: 50%;
  right: 1.5rem;
  transform: translateY(-50%);
  border: none;
  background: transparent;
  color: inherit;
  opacity: 0.7;
  font-size: 0.8rem;
  line-height: 1;
  padding: 4px;
  cursor: pointer;
  border-radius: 4px;
  transition: opacity 0.15s;
}
.banner-close:hover {
  opacity: 1;
}

/* 型別配色 */
.sb-warning {
  background-color: #faeeda;
  color: #854f0b;
}
.sb-danger {
  background-color: #ef4444;
  color: #ffffff;
}
.sb-info {
  background-color: rgba(229, 115, 70, 0.12);
  color: var(--tap-primary, #e57346);
}

/* 展開 / 收合動畫（高度與透明度）*/
.banner-enter-active,
.banner-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}
.banner-enter-from,
.banner-leave-to {
  opacity: 0;
  transform: translateY(-100%);
}
</style>
