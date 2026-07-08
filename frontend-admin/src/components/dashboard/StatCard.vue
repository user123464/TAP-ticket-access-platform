<script setup>
/**
 * StatCard.vue — Dashboard 統計數字卡片
 */
defineProps({
  icon: {
    type: String,
    required: true, // bootstrap-icons class，如 'bi-people'
  },
  label: {
    type: String,
    required: true,
  },
  value: {
    type: [Number, String],
    default: 0,
  },
  // 圖示強調色
  variant: {
    type: String,
    default: "primary", // primary / success / danger / warning / info
  },
  loading: {
    type: Boolean,
    default: false,
  },
  // 點擊整卡導向的路由（可省略）
  to: {
    type: String,
    default: "",
  },
});
</script>

<template>
  <component
    :is="to ? 'RouterLink' : 'div'"
    :to="to || undefined"
    class="card border shadow-sm rounded-4 text-decoration-none"
    :class="{ 'hover-card': to }"
  >
    <div class="card-body p-2 px-3">
      <div class="d-flex align-items-center gap-2 mb-1 text-truncate">
        <i class="bi fs-5" :class="[icon, `text-${variant}`]"></i>
        <span class="small text-tap-secondary text-truncate">{{ label }}</span>
      </div>
      <div class="fs-5 fw-bold text-truncate">
        <span v-if="loading" class="placeholder-glow"><span class="placeholder px-4 rounded-2"></span></span>
        <template v-else>{{ value }}</template>
      </div>
    </div>
  </component>
</template>

<style scoped>
</style>
