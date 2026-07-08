<script setup>
/**
 * LoginLayout.vue — 登入頁佈局（無側欄、無導覽列）
 * 深色滿版置中，內容由子路由（LoginView）渲染。
 */
</script>

<template>
  <div class="login-wrapper vh-100 d-flex align-items-center justify-content-center">
    <RouterView />
  </div>
</template>

<style scoped>
.login-wrapper {
  /* 底色再疊一層由上而下的微光暈，避免大面積純色死板 */
  background:
    radial-gradient(ellipse 80% 50% at 50% -10%, rgba(229, 115, 70, 0.12), transparent),
    var(--tap-bg-base);
}
</style>
