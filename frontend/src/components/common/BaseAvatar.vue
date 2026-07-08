<script setup>
import { ref, watch, computed } from 'vue';
import Avatar from 'vue-boring-avatars';
import { AVATAR_COLORS } from '@/constants/avatar.js';

const BACKEND_URL = import.meta.env.VITE_BACKEND_API || 'http://localhost:8080';

const props = defineProps({
  avatarType: {
    type: String,
    default: 'preset', // 'upload' | 'preset'
    validator: (value) => ['upload', 'preset'].includes(value)
  },
  src: {
    type: String,
    default: ''
  },
  seed: {
    type: String,
    default: 'default'
  },
  size: {
    type: [Number, String],
    default: 40
  },
  alt: {
    type: String,
    default: 'Avatar'
  },
  colors: {
    type: Array,
    default: () => AVATAR_COLORS
  }
});

const isLoadError = ref(false);

const isCustomImage = computed(() => {
  if (!props.src || isLoadError.value) return false;
  if (props.src.startsWith('boring:')) return false;
  if (props.src === 'default' || props.src === 'preset') return false;
  return props.src.startsWith('data:image/') || props.src.includes('/') || props.src.includes('.');
});

// 若 src 是後端頭像 API 路徑，自動補上 backend base URL
const resolvedSrc = computed(() => {
  if (!props.src) return '';
  if (props.src.startsWith('/api/')) {
    return BACKEND_URL + props.src;
  }
  return props.src;
});

const boringSeed = computed(() => {
  if (props.src && props.src.startsWith('boring:')) {
    return props.src.split('boring:')[1];
  }
  return props.seed;
});

watch(() => [props.src, props.avatarType], () => {
  isLoadError.value = false;
});

const handleLoadError = () => {
  isLoadError.value = true;
};
</script>

<template>
  <img
    v-if="isCustomImage"
    :src="resolvedSrc"
    :alt="alt"
    class="base-avatar rounded-circle border shadow-sm"
    :style="{
      width: size + 'px',
      height: size + 'px',
      objectFit: 'cover',
      backgroundColor: '#f8f9fa'
    }"
    @error="handleLoadError"
  />
  <Avatar
    v-else
    :size="Number(size)"
    variant="beam"
    :name="boringSeed"
    :colors="colors"
    class="base-avatar rounded-circle border shadow-sm"
    :style="{
      width: size + 'px',
      height: size + 'px',
      objectFit: 'cover',
      backgroundColor: '#f8f9fa'
    }"
  />
</template>

<style scoped>
.base-avatar {
  transition: all 0.2s ease-in-out;
}
</style>
