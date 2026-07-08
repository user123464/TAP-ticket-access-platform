<template>
  <div class="product-detail-container" v-loading="loading">
    <div class="d-flex justify-content-between align-items-center mb-4">
      <el-breadcrumb separator="/" class="breadcrumb mb-0">
        <el-breadcrumb-item :to="{ path: '/' }">首頁</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/shop/home' }">商城</el-breadcrumb-item>
        <el-breadcrumb-item>{{ products[0]?.theme?.title || themeTitle || '週邊' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <button @click="goBack" class="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1">
        <i class="bi bi-arrow-left"></i> 返回
      </button>
    </div>

<el-skeleton :loading="loading" animated :count="4">
      <template #template>
        <div style="padding: 14px">
          <el-skeleton-item variant="image" style="width: 270px; height: 270px" />
          <el-skeleton-item variant="p" style="width: 50%; margin-top: 10px;" />
          <el-skeleton-item variant="p" style="width: 80%" />
        </div>
      </template>
      
      <div class="product-grid">
        <div 
          v-for="product in products" 
          :key="product.productId" 
          class="product-card"
          @click="goToDetail(product.productId)"
        >
      
          <div class="product-img-box">
            <img :src="product.mainImage" alt="商品主圖" class="img-size">            
          </div>
  
          <div class="product-info">
            <div class="prod-name">{{ product.productName }}</div>
            
            <div class="prod-sizes">
              <span v-for="size in getProductSizes(product.variants)" :key="size" class="size-tag">
                {{ size }}
              </span>
              <span v-if="getProductSizes(product.variants).length === 0" class="no-size">F</span>
            </div>

            <div class="prod-price">
              <span class="currency">NT$</span>
              <span class="price-num">{{ getMinPrice(product.variants) }}</span>
            </div>
          </div>
        </div>
      </div>
    </el-skeleton>

  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import axios from '@/plugins/axios' 

const router = useRouter()
const route = useRoute()

const products = ref([])
const loading = ref(false)
const themeTitle = ref('')

// 取得所有商品列表
onMounted(async () => {
  loading.value = true
  try {
    const themeId = route.params.id

    const res = await axios.get(`/shop/theme/${themeId}`)
    // 後端回傳的是 List<ProductDTO> 陣列
    if (res.data && Array.isArray(res.data)) {
      products.value = res.data
      console.log(products.value)

      // 該主題底下若沒有任何商品，products[0]?.theme 恆不存在，改打主題 API 補上麵包屑名稱
      if (products.value.length === 0) {
        try {
          const themeRes = await axios.get(`/api/themes/${themeId}`)
          themeTitle.value = themeRes.data?.data?.title || ''
        } catch (themeError) {
          console.error('撈取主題名稱失敗:', themeError)
        }
      }
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

// 點擊卡片跳轉至詳情頁
const goToDetail = (productId) => {
  router.push({
    path: `/shop/product/${productId}`,
    query: { from: 'theme' }
  })
}

const goBack = () => {
  const from = route.query.from
  if (from === 'theme') {
    router.push(`/themes/${route.params.id}`)
  } else {
    router.push('/shop/home')
  }
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
  
  // 1. 收集所有尺寸並去重
  const sizeSet = new Set(variants.map(v => v.productSize).filter(Boolean))
  const sizeList = Array.from(sizeSet)
  
  // 2. 依照常見衣服尺寸排序 (XS -> S -> M -> L -> XL -> XXL)
  const sizeOrder = ['XS', 'S', 'M', 'L', 'XL', 'XXL']
  return sizeList.sort((a, b) => {
    const indexA = sizeOrder.indexOf(a.toUpperCase())
    const indexB = sizeOrder.indexOf(b.toUpperCase())
    if (indexA === -1 && indexB === -1) return a.localeCompare(b) // 找不到的就比英文字母
    if (indexA === -1) return 1
    if (indexB === -1) return -1
    return indexA - indexB
  })
}
</script>

<style scoped>
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
  aspect-ratio: 1 / 1; /* 強制圖片維持正方形 */
  overflow: hidden;
  display: flex;            
  align-items: center;
  background-color: #fff;
}
.product-img-box img {
  width: 100%;
  height: 100%;
  object-fit: contain; /* 確保圖片不變形 */
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
  -webkit-line-clamp: 2; /* 最多顯示兩行品名 */
  -webkit-box-orient: vertical;
  overflow: hidden;
}

/* 仿 OneBoy 尺寸樣式 */
.prod-sizes {
  margin-bottom: 12px;
  min-height: 20px; /* 防止沒有尺寸時排版塌陷 */
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

/* 價格樣式 */
.prod-price {
  color: #b01f1f; /* 仿 OneBoy 的深紅色系調 */
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