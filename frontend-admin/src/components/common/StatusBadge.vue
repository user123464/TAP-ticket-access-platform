<script setup>
/**
 * StatusBadge.vue — 狀態標籤
 *
 * variant 對應系統狀態色：success(通過/啟用)、danger(退件/停用)、
 * warning(待審/處理中)、secondary(草稿/封存)、info(唯讀資訊)、primary(強調)
 */
import { computed } from "vue";

const props = defineProps({
  variant: {
    type: String,
    default: "secondary",
    validator: (v) => ["success", "danger", "warning", "secondary", "info", "primary"].includes(v),
  },
  label: {
    type: String,
    required: true,
  },
  // 顯示狀態圓點
  dot: {
    type: Boolean,
    default: true,
  },
});

// 深色背景上用 subtle 底 + emphasis 文字，對比清楚不刺眼
const badgeClass = computed(
  () => `bg-${props.variant}-subtle text-${props.variant}-emphasis border border-${props.variant}-subtle`
);
</script>

<template>
  <span class="badge rounded-pill fw-semibold d-inline-flex align-items-center gap-1" :class="badgeClass">
    <i v-if="dot" class="bi bi-circle-fill" style="font-size: 0.45em"></i>
    {{ label }}
  </span>
</template>
