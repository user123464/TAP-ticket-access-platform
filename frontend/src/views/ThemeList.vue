<template>
  <header>
    <ThemeBanner :themes="bannerThemes" @select="goTheme" />
  </header>
  <main class="container py-4">
    <div class="d-flex justify-content-between align-items-center mb-4 flex-wrap gap-2">
      <h1 class="mb-0">活動資訊</h1>
      <RouterLink to="/shop/home" class="btn btn-outline-primary d-flex align-items-center gap-2 rounded-pill px-4">
        前往周邊商城 <i class="bi bi-arrow-right"></i>
      </RouterLink>
    </div>
    <div class="row align-items-center">
      <!-- 篩選活動 -->
      <ThemeFilterGroup class="col-12 col-md-6 col-lg-5 py-1" 
                        :options="[{ label: '即將演出', value: 'upcoming' },//依照活動下的所有session的startTime
                                   { label: '最新活動', value: 'latest' }   //依照活動下的所有session的publishTime
                                  ]"  
                        v-model="selectedFilter" 
      />
      <!-- 搜尋活動 -->
      <div class="col-12 col-md-6 col-lg-4 py-1 d-flex">
        <div class="input-group">
          <span class="input-group-text" id="basic-addon1">搜尋活動</span>
          <input type="text" class="form-control" placeholder="輸入活動名稱" aria-label="Username"
            aria-describedby="basic-addon1" v-model="searchKeyword">
        </div>
      </div>
      <!-- 每頁顯示筆數 --><!-- 下拉選單 -->
      <div class="col-12 col-md-6 col-lg-2 py-1">
        <div class="dropdown">
          <button class="btn btn-outline-secondary dropdown-toggle" data-bs-toggle="dropdown">
            每頁 {{ pageSize }} 筆
          </button>
          <ul class="dropdown-menu">
            <li v-for="size in [5, 10, 20]" :key="size">
              <a class="dropdown-item" href="#" @click.prevent="pageSize = size">{{ size }}筆</a>
            </li>
          </ul>
        </div>
      </div>
    </div>
    <div class="row" v-if="!loading">
      <!-- ThemeCard 列表 -->
      <div v-for="theme in themes" :key="theme.themeId" class="col-12 col-md-6 col-lg-4 py-2">
        <ThemeCard @click1="goTheme(theme.themeId)" :themeId="theme.themeId" :image="resolveImageUrl(theme.image)" :title="theme.title"
          :detail="theme.detail" />
      </div>
      <div v-if="themes.length === 0" class="col-12 text-center py-5">
        <p class="text-muted">查無相關活動</p>
      </div>
    </div>
    <div v-else class="text-center py-5">
      <div class="spinner-border text-primary" role="status">
        <span class="visually-hidden">Loading...</span>
      </div>
    </div>
  </main>
  <footer class="container py-3">
    <!-- 分頁元件 -->
    <div class="row">
      <div class="col-12">
        <Paginate
            :page-count="pageCount" 
            active-class="my-active-page"
            prev-text="&lt;" 
            next-text="&gt;"
            v-model="page" 
        />
      </div>
    </div>
  </footer>
</template>


<script setup>
import { ref, onMounted, watch } from 'vue'
import { useRouter } from 'vue-router'
import Paginate from 'vuejs-paginate-next'
import { getThemes } from '@/api/themeSessionApi'
import { resolveImageUrl } from '@/utils/imageUrl'

import ThemeCard from '@/components/ThemeCard.vue'
import ThemeBanner from '@/components/ThemeBanner.vue'
import ThemeFilterGroup from '@/components/ThemeFilterGroup.vue'


// 初始化資料
const themes = ref([])
const bannerThemes = ref([])
const selectedFilter = ref('upcoming')
const searchKeyword = ref('')
const page = ref(1)
const pageSize = ref(10)
const pageCount = ref(0)
const loading = ref(false)


watch([page, pageSize], () => {
  fetchThemes()
})

watch([searchKeyword, selectedFilter], () => {
  // 加入簡單的防抖邏輯
  clearTimeout(searchTimeout)
  searchTimeout = setTimeout(() => {
    page.value = 1
    fetchThemes()
  }, 300)
})

let searchTimeout = null

const fetchThemes = async () => {
  loading.value = true
  try {
    const result = await getThemes(
                                  page.value, 
                                  pageSize.value, 
                                  searchKeyword.value, 
                                  selectedFilter.value
    )
    themes.value = result.content       // 這一頁查到的資料 List
    pageCount.value = result.totalPages // 總頁數
  } catch (e) {
    alert(e?.message);
  } finally {
    loading.value = false
  }
}

const fetchBannerThemes = async () => {
  try {
    const result = await getThemes(1, 5, '', 'upcoming')
    bannerThemes.value = result.content 
  } catch (e) {
    console.error("獲取 Banner 活動失敗:", e)
  }
}

onMounted(() => {
  fetchThemes()
  fetchBannerThemes()
})

// 前往活動詳情頁
const router = useRouter()
function goTheme(themeId) {
  // 滾動到頁面頂部
  window.scrollTo({ top: 0, left: 0, behavior: 'auto' })
  router.push(`/themes/${themeId}`)
}
</script>

<style scoped>

::v-deep .my-active-page a {
  color: #fff;
  background-color: #42b983; /* 自訂的主題綠色 */
  border-color: #42b983;
}

</style>