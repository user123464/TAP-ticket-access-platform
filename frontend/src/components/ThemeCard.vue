<template>
  <!-- card h-100 讓全部卡片同一高度 -->
  <!-- shadow-sm 小陰影 -->
  <!-- style="cursor: pointer 滑鼠移到這個元素上時，游標變成「手指點擊狀」 -->
  <div class="card theme-card shadow-sm" @click="goTheme">
    <!-- 圖片 -->
    <img :src="image" class="card-img-top card-img" />
    <!-- 內容 -->
    <div class="card-body">
      <h5 class="card-title">{{ title }}</h5>
      <p class="card-text text-muted detail-clamp">{{ detail }}</p>
    </div>
  </div>
</template>

<script setup>
const props = defineProps(['id', 'image', 'title', 'detail'])
const emits = defineEmits(['click1'])

function goTheme() {
  emits('click1', props.id)
}
</script>

<style scoped>
.theme-card {
  height: 360px;
  position: relative;
  overflow: hidden;
  cursor: pointer;
  border: 1px solid rgba(31, 41, 55, 0.08);
  transition: transform 220ms ease, box-shadow 220ms ease, border-color 220ms ease;
}

.card-body {
  overflow: hidden;
}

/* 圖片統一比例（很重要） */
.card-img {
  aspect-ratio: 16 / 9;
  object-fit: cover;
  transform: scale(1);
  transition: transform 260ms ease;
}

.card-title {
  transition: color 220ms ease;
}

.theme-card:hover,
.theme-card:focus-within {
  transform: translateY(-6px);
  box-shadow: 0 14px 30px rgba(15, 23, 42, 0.18) !important;
  border-color: rgba(176, 112, 44, 0.4);
}

.theme-card:hover .card-img,
.theme-card:focus-within .card-img {
  transform: scale(1.06);
}

.theme-card:hover .card-title,
.theme-card:focus-within .card-title {
  color: #a35b17;
}

.detail-clamp {
  overflow: hidden;
  display: -webkit-box;
  line-clamp: 3;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}

@media (max-width: 768px) {
  .theme-card {
    height: auto;
    min-height: 360px;
  }
}

@media (max-width: 480px) {
  .theme-card {
    min-height: 0;
  }
}

@media (prefers-reduced-motion: reduce) {
  .theme-card,
  .card-img,
  .card-title {
    transition: none;
  }
}
</style>