<script setup>
// 404 頁 — 深色置中版
</script>

<template>
  <div class="vh-100 d-flex flex-column align-items-center justify-content-center text-center">
    <div class="display-1 fw-bold" style="color: var(--tap-primary)">404</div>
    <p class="fs-5 text-tap-secondary mb-4">找不到這個頁面</p>
    <RouterLink to="/admin/dashboard" class="btn btn-primary px-4">回到 Dashboard</RouterLink>
  </div>
</template>
