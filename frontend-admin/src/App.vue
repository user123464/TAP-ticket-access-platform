<script setup>
// App 根元件：實際版面由路由對應的 Layout（AdminLayout / LoginLayout）決定
// 全域通知系統（從 frontend 移植，深色版）統一在此掛載一次：
//   - SystemBanner：系統錯誤 / 狀態提示橫幅（最上方）
//   - ToastContainer：成功 / 提示（右下角）
//   - ConfirmDialog：危險操作確認（全域 useConfirm）
import SystemBanner from "@/components/common/SystemBanner.vue";
import ToastContainer from "@/components/common/ToastContainer.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
</script>

<template>
  <SystemBanner />
  <RouterView />
  <ToastContainer />
  <ConfirmDialog global />
</template>
