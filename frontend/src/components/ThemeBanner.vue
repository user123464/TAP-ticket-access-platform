<template>
  <div id="carouselExample" class="carousel slide" data-bs-ride="carousel">
    <!-- 中間的左右控制按鈕 -->
    <button class="carousel-control-prev" type="button" data-bs-target="#carouselExample" data-bs-slide="prev">
      <span class="carousel-control-prev-icon"></span>
    </button>
    <button class="carousel-control-next" type="button" data-bs-target="#carouselExample" data-bs-slide="next">
      <span class="carousel-control-next-icon"></span>
    </button>
    <!-- 主要的輪播內容 -->
    <div class="carousel-inner">
      <div
        v-for="(theme, index) in resolvedThemes.slice(0, 5)"
        :key="theme.themeId"
        class="carousel-item"
        :class="{ active: index === 0 }"
        role="button"
        tabindex="0"
        @click="handleBannerClick(theme.themeId)"
        @keydown.enter="handleBannerClick(theme.themeId)"
      >
        <img class="carousel-img d-block w-100" :src="theme.image" :alt="theme.title">
        <div class="carousel-title-wrap">
          <h2 class="carousel-title">{{ theme.title }}</h2>
        </div>
      </div>
    </div>
    <!-- 下方的指示器 -->
    <div class="carousel-indicators">
      <button v-for="(theme, index) in resolvedThemes.slice(0, 5)" :key="theme.themeId" 
              type="button"
              data-bs-target="#carouselExample" 
              :data-bs-slide-to="index" 
              :class="{ active: index === 0 }"></button>
    </div>
  </div>
</template>
<script setup>
import { computed } from 'vue'
import { resolveImageUrl } from '@/utils/imageUrl'

const props = defineProps({
  themes: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['select'])

const resolvedThemes = computed(() => {
  return props.themes.map(theme => ({
    ...theme,
    image: resolveImageUrl(theme.image)
  }))
})

function handleBannerClick(themeId) {
  emit('select', themeId)
}

</script>

<style>
#carouselExample {
  height: 80vh;
}

/* 平板 */
@media (max-width: 768px) {
  #carouselExample {
    height: 60vh;
  }
}

/* 手機 */
@media (max-width: 480px) {
  #carouselExample {
    height: 40vh;
  }
}

/* 關鍵：讓內部吃滿 */
#carouselExample .carousel-inner,
#carouselExample .carousel-item {
  height: 100%;
}

.carousel-item {
  position: relative;
  cursor: pointer;
}

/* 圖片填滿不變形 */
.carousel-img {
  height: 100%;
  object-fit: cover;
}

.carousel-title-wrap {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  padding: 3rem 4rem;
  background: linear-gradient(180deg, rgba(0, 0, 0, 0.65) 0%, rgba(0, 0, 0, 0) 100%);
}

.carousel-title {
  margin: 0;
  color: #fff;
  font-size: clamp(1.75rem, 4vw, 3.5rem);
  font-weight: 700;
  text-shadow: 0 2px 12px rgba(0, 0, 0, 0.35);
}

@media (max-width: 768px) {
  .carousel-title-wrap {
    padding: 2rem 2.5rem;
  }
}

@media (max-width: 480px) {
  .carousel-title-wrap {
    padding: 1.25rem 1rem;
  }

  .carousel-title {
    font-size: 1.5rem;
  }
}
</style>
