<script setup>
import B2bNavbar from '@/components/common/B2bNavbar.vue';
import B2bFooter from '@/components/common/B2bFooter.vue';
</script>

<template>
  <div class="d-flex flex-column min-vh-100 b2b-portal-wrapper text-dark">
    <!-- 頂部導覽列 -->
    <B2bNavbar />

    <!-- 子頁面渲染區 -->
    <main class="flex-grow-1">
      <RouterView />
    </main>

    <!-- 頁尾 -->
    <B2bFooter />
  </div>
</template>

<style scoped>
.b2b-portal-wrapper {
  background-color: var(--tap-light-white, #f8fafc) !important;
}
</style>
