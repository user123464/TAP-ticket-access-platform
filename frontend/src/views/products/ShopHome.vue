<template>
  <div class="shop-home">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <el-breadcrumb separator="/" class="breadcrumb mb-0">
        <el-breadcrumb-item :to="{ path: '/' }">首頁</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/shop/home' }">商城</el-breadcrumb-item>
      </el-breadcrumb>
      <RouterLink to="/themes" class="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1">
        <i class="bi bi-arrow-left"></i> 返回首頁
      </RouterLink>
    </div>

    <!-- 篩選與搜尋 Bar -->
    <div class="filter-bar d-flex flex-wrap gap-3 align-items-center mb-4 p-3 bg-white rounded shadow-sm">
      <div class="filter-item flex-grow-1" style="min-width: 200px;">
        <el-input
          v-model="searchKeyword"
          placeholder="搜尋商品名稱..."
          clearable
        >
          <template #prefix>
            <el-icon><Search /></el-icon>
          </template>
        </el-input>
      </div>
      <div class="filter-item" style="width: 220px;">
        <el-select v-model="selectedThemeId" placeholder="按活動主題分類" clearable style="width: 100%;">
          <el-option
            v-for="theme in uniqueThemes"
            :key="theme.themeId"
            :label="theme.title"
            :value="theme.themeId"
          />
        </el-select>
      </div>
      <div class="filter-item" style="width: 220px;">
        <el-select v-model="selectedCategoryId" placeholder="按商品類型分類" clearable style="width: 100%;">
          <el-option
            v-for="cat in uniqueCategories"
            :key="cat.categoryId"
            :label="cat.categoryName"
            :value="cat.categoryId"
          />
        </el-select>
      </div>
    </div>

    <el-skeleton :loading="loading" animated :count="4">
      <template #template>
        <div style="padding: 14px">
          <el-skeleton-item variant="image" style="width: 270px; height: 270px" />
          <el-skeleton-item variant="p" style="width: 50%; margin-top: 10px;" />
          <el-skeleton-item variant="p" style="width: 80%" />
        </div>
      </template>
      
      <div v-if="filteredProducts.length > 0" class="product-grid">
        <div 
          v-for="product in filteredProducts" 
          :key="product.productId" 
          class="product-card"
          @click="goToDetail(product.productId)"
        >
          <div class="product-img-box">
            <img :src="product.mainImage" alt="商品主圖">            
          </div>
          
          <div class="product-info">
            <div class="prod-name">{{ product.productName }}</div>
            
            <div class="prod-sizes">
              <span v-for="size in getProductSizes(product.variants)" :key="size" class="size-tag">
                {{ size }}
              </span>
              <span v-if="getProductSizes(product.variants).length === 0" class="no-size">One Size</span>
            </div>

            <div class="prod-price">
              <span class="currency">NT$</span>
              <span class="price-num">{{ getMinPrice(product.variants) }}</span>
            </div>
          </div>
        </div>
      </div>

      <div v-else class="empty-state">
        <el-empty description="查無相關商品" />
      </div>
    </el-skeleton>
  </div>
</template>

<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Search } from '@element-plus/icons-vue'
import axios from '@/plugins/axios' 

const router = useRouter()
const products = ref([])
const loading = ref(false)

const searchKeyword = ref('')
const selectedThemeId = ref('')
const selectedCategoryId = ref('')

// 動態從商品列表萃取不重複的主題選項
const uniqueThemes = computed(() => {
  const map = new Map()
  products.value.forEach(p => {
    if (p.theme && p.theme.themeId) {
      map.set(p.theme.themeId, p.theme.title)
    }
  })
  return Array.from(map.entries()).map(([themeId, title]) => ({ themeId, title }))
})

// 動態從商品列表萃取不重複的分類選項
const uniqueCategories = computed(() => {
  const map = new Map()
  products.value.forEach(p => {
    if (p.category && p.category.categoryId) {
      map.set(p.category.categoryId, p.category.categoryName)
    }
  })
  return Array.from(map.entries()).map(([categoryId, categoryName]) => ({ categoryId, categoryName }))
})

// 過濾後的商品列表
const filteredProducts = computed(() => {
  return products.value.filter(p => {
    const matchKeyword = !searchKeyword.value || p.productName.toLowerCase().includes(searchKeyword.value.toLowerCase())
    const matchTheme = !selectedThemeId.value || p.theme?.themeId === selectedThemeId.value
    const matchCategory = !selectedCategoryId.value || p.category?.categoryId === selectedCategoryId.value
    return matchKeyword && matchTheme && matchCategory
  })
})

// 取得所有商品列表
onMounted(async () => {
  loading.value = true
  try {
    const res = await axios.get(`/shop/home`)
    if (res.data && Array.isArray(res.data)) {
      products.value = res.data
    } else {
      ElMessage.error('無法載入商品資料')
    }
  } catch (error) {
    console.error('撈取商品列表失敗:', error)
    ElMessage.error('連線後端失敗，無法載入商品。')
  } finally {
    loading.value = false
  }
})

// 點擊卡片跳轉至詳情頁，帶上來源 query 參數
const goToDetail = (productId) => {
  router.push({
    path: `/shop/product/${productId}`,
    query: { from: 'shop' }
  })
}

// 尋找該款商品所有規格中的最低價格
const getMinPrice = (variants) => {
  if (!variants || variants.length === 0) return '0'
  const prices = variants.map(v => Number(v.unitPrice))
  return Math.min(...prices).toFixed(0)
}

// 提取該商品所有款式中「不重複」的尺寸，並依特定順序排序
const getProductSizes = (variants) => {
  if (!variants || variants.length === 0) return []
  
  const sizeSet = new Set(variants.map(v => v.productSize).filter(Boolean))
  const sizeList = Array.from(sizeSet)
  
  const sizeOrder = ['XS', 'S', 'M', 'L', 'XL', 'XXL', '3XL']
  return sizeList.sort((a, b) => {
    const indexA = sizeOrder.indexOf(a.toUpperCase())
    const indexB = sizeOrder.indexOf(b.toUpperCase())
    if (indexA === -1 && indexB === -1) return a.localeCompare(b)
    if (indexA === -1) return 1
    if (indexB === -1) return -1
    return indexA - indexB
  })
}
</script>

<style scoped>
.filter-bar {
  border: 1px solid rgba(0, 0, 0, 0.05);
}
.filter-item {
  display: flex;
  align-items: center;
}
.empty-state {
  padding: 40px 0;
}
.product-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(270px, 1fr));
  gap: 30px;
  padding: 20px;
}
.product-card {
  background: #fff;
  border-radius: 4px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
}
.product-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.08);
}
.product-img-box {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background-color: #f5f5f5;
}
.product-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}
.product-info {
  padding: 15px 10px;
  text-align: left;
}
.prod-name {
  font-size: 14px;
  color: #555;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}
.prod-sizes {
  margin-bottom: 12px;
  min-height: 20px;
}
.size-tag {
  font-size: 12px;
  color: #888;
  margin-right: 8px;
  text-transform: uppercase;
}
.no-size {
  font-size: 12px;
  color: #bbb;
}
.prod-price {
  color: #b01f1f;
  font-family: 'Arial', sans-serif;
}
.currency { 
  font-size: 13px; 
  font-weight: bold;
  margin-right: 2px;
}
.price-num { 
  font-size: 18px; 
  font-weight: bold;
}
</style>