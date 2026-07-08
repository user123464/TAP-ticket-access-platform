<script setup>
/**
 * BaseAvatar.vue — 頭像元件（Admin 離線版）
 *
 * 與 frontend 原版的差異：原版載入失敗時 fallback 到 Dicebear 外部 API，
 * 但 Admin 部署於離線內網無法連外，故 fallback 改為本地渲染的字首縮寫色塊。
 */
import { ref, watch, computed } from "vue";

const props = defineProps({
  src: {
    type: String,
    default: "",
  },
  name: {
    type: String,
    default: "",
  },
  size: {
    type: [Number, String],
    default: 40,
  },
  alt: {
    type: String,
    default: "Avatar",
  },
});

const isLoadError = ref(false);

watch(
  () => props.src,
  () => {
    isLoadError.value = false;
  }
);

const BACKEND_URL = import.meta.env.VITE_BACKEND_API || 'http://localhost:8080';
const resolvedSrc = computed(() => {
  if (!props.src) return "";
  if (props.src.startsWith("/api/")) {
    return BACKEND_URL + props.src;
  }
  return props.src;
});

const showImage = computed(() => resolvedSrc.value && !isLoadError.value);

// 取名稱第一個字（中文名取首字、英文名取首字母大寫）
const initial = computed(() => {
  const trimmed = (props.name || "").trim();
  return trimmed ? trimmed.charAt(0).toUpperCase() : "?";
});

// 依名稱雜湊出固定背景色，同一人永遠同色
const bgColor = computed(() => {
  const palette = ["#e57346", "#10b981", "#6366f1", "#0ea5e9", "#f59e0b", "#ec4899"];
  let hash = 0;
  for (const ch of props.name || "") {
    hash = (hash * 31 + ch.codePointAt(0)) >>> 0;
  }
  return palette[hash % palette.length];
});

const handleLoadError = () => {
  isLoadError.value = true;
};
</script>

<template>
  <img
    v-if="showImage"
    :src="resolvedSrc"
    :alt="alt"
    class="base-avatar rounded-circle border shadow-sm"
    :style="{
      width: size + 'px',
      height: size + 'px',
      objectFit: 'cover',
      backgroundColor: 'var(--tap-bg-hover)',
    }"
    @error="handleLoadError"
  />
  <span
    v-else
    class="base-avatar rounded-circle border shadow-sm d-inline-flex align-items-center justify-content-center fw-bold text-white"
    :style="{
      width: size + 'px',
      height: size + 'px',
      backgroundColor: bgColor,
      fontSize: size * 0.45 + 'px',
      lineHeight: 1,
    }"
    :aria-label="alt"
  >
    {{ initial }}
  </span>
</template>

<style scoped>
.base-avatar {
  transition: all 0.2s ease-in-out;
  user-select: none;
  flex-shrink: 0;
}
</style>
