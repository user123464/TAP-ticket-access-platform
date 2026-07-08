<script setup>
/**
 * PlaceholderView.vue — P1/P2 模組佔位頁
 * 路由骨架先行，功能依分階段開發計畫陸續替換本頁。
 */
import { useRoute } from "vue-router";

const route = useRoute();
</script>

<template>
  <div class="d-flex flex-column align-items-center justify-content-center text-center py-5 my-5">
    <i class="bi bi-cone-striped display-3 text-tap-secondary mb-3"></i>
    <h4 class="fw-bold">此模組開發中</h4>
    <p class="text-tap-secondary mb-0">
      路由 <code>{{ route.path }}</code> 已預留，功能將於後續開發階段（P1 / P2）上線。
    </p>
  </div>
</template>
