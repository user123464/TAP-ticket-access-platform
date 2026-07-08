<script setup>
import SystemBanner from '@/components/common/SystemBanner.vue';
import ToastContainer from '@/components/common/ToastContainer.vue';
import ConfirmDialog from '@/components/common/ConfirmDialog.vue';
</script>

<template>
  <div>
    <!-- 全站系統公告條（navbar 上方，斷線/維護時顯示）-->
    <SystemBanner />
    <RouterView></RouterView>
    <!-- 全站共用的浮動 Toast 通知（操作回饋）-->
    <ToastContainer />
    <!-- 全站共用的確認對話框（危險操作）-->
    <ConfirmDialog />
  </div>
</template>

<style scoped></style>
