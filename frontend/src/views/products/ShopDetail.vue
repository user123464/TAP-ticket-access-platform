<template>
  <div class="product-detail-container" v-loading="loading">
    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-start align-items-sm-center gap-2 mb-4">
      <el-breadcrumb separator="/" class="breadcrumb mb-0 flex-wrap">
        <el-breadcrumb-item :to="{ path: '/' }">首頁</el-breadcrumb-item>
        <el-breadcrumb-item :to="{ path: '/shop/home' }">商城</el-breadcrumb-item>
        <el-breadcrumb-item v-if="product && product.theme" :to="{ path: `/shop/theme/${product.theme.themeId}`, query: route.query }" >{{product.theme.title}}週邊</el-breadcrumb-item>
        <el-breadcrumb-item>{{ product?.productName || '商品詳情' }}</el-breadcrumb-item>
      </el-breadcrumb>
      <button @click="goBack" class="btn btn-outline-secondary btn-sm d-flex align-items-center gap-1 ms-auto ms-sm-0">
        <i class="bi bi-arrow-left"></i> 返回
      </button>
    </div>



    <div v-if="product" class="detail-layout">

      <!- 左側：商品大圖展示區 --  原始>
      <div class="detail-left">
        <div class="main-image-box">          
          <img :src="activeImage" alt="商品詳細大圖" style="padding-top: 40px;">
          <span class="theme-badge" v-if="product.theme">{{ product.theme.title }}</span>
        </div>

      <div class="thumbnail-list" v-if="product.images && product.images.length > 0">
          <div 
            v-for="(imgUrl, index) in product.images" 
            :key="index"
            class="thumbnail-item"
            :class="{ active: activeImage === imgUrl }"
            @click="changeActiveImage(imgUrl)"
          >
            <img :src="imgUrl" alt="商品縮圖">
          </div>
        </div>


      </div>
      

      <!- 右側：規格挑選與購買區 -->
      <div class="detail-right">
        <div class="category-tag">{{ product.category?.categoryName }}</div>
        <h1 class="product-title">{{ product.productName }}</h1>
        <p class="product-sim-desc">{{ product.productSimDescription }}</p>

        <!-- 價格區塊（隨選中規格動態變動） -->
        <div class="price-panel">
          <span class="price-label">售價</span>
          <span class="currency">NT$</span>
          <span class="price-amount">{{ currentPrice }}</span>
        </div>

        <el-divider />

        <!-- 規格選擇 1：顏色 (去重後的顏色列表) -->
        <div class="spec-section">
          <span class="spec-label">選擇顏色：</span>
          <div class="spec-options">
            <button 
              v-for="color in uniqueColors" 
              :key="color"
              class="spec-btn"
              :class="{ active: selectedColor === color }"
              @click="selectColor(color)"
            >
              {{ color }}
            </button>
          </div>
        </div>

        <!-- 規格選擇 2：尺寸 (依據選中顏色，動態篩選出可選的尺寸) -->
        <div class="spec-section" style="margin-top: 20px;">
          <span class="spec-label">選擇尺寸：</span>
          <div class="spec-options">
            <button
              v-for="size in availableSizes"
              :key="size"
              class="spec-btn"
              :class="{
                active: selectedSize === size,
                disabled: isSizeOutOfStock(size)
              }"
              :disabled="isSizeOutOfStock(size)"
              @click="selectSize(size)"
            >
              {{ formatSizeLabel(size) }}
            </button>

          </div>
        </div>

        <!-- 即時庫存提示 -->
        <div class="stock-status-bar" style="margin-top: 20px;">
          <span class="spec-label">庫存狀況：</span>
          <span v-if="currentVariant" class="stock-indicator">
            <el-tag :type="currentVariant.stockQty > 0 ? 'success' : 'danger'">
              {{ currentVariant.stockQty > 0 ? `庫存餘額 ${currentVariant.stockQty} 件` : '商品已售罄 (補貨中)' }}
            </el-tag>
          </span>
          <span v-else class="prompt-text">請先選擇 顏色 與 尺寸</span>
        </div>

        <!-- 數量加減與購買按鈕 -->
        <div class="purchase-action-row" style="margin-top: 30px;">
          <div class="qty-selector">
            <span class="spec-label">購買數量：</span>
            <el-input-number 
              v-model="purchaseQty" 
              :min="1" 
              :max="currentVariant ? currentVariant.stockQty : 1" 
              :disabled="!currentVariant || currentVariant.stockQty === 0"
            />
          </div>

          <!-- 加入購物車按鈕 (具備動態防呆) -->
          <el-button 
            type="primary" 
            size="large" 
            class="add-cart-btn"
            :disabled="!currentVariant || currentVariant.stockQty === 0"
            @click="handleAddToCart"
          >
            {{ getButtonText }}
          </el-button>
        </div>

        <!-- 商品詳細說明區（NVARCHAR MAX 的文字介紹） -->
        <el-tabs class="product-tabs" style="margin-top: 40px;">
          <el-tab-pane label="商品詳情介紹">
            <div class="intro-content">
              <p v-if="product.productDescription">{{ product.productDescription }}</p>
              <p v-else class="no-data"></p>
            </div>
          </el-tab-pane>
          <el-tab-pane label="退換貨須知">
            <div class="intro-content">
              <p>本商城週邊配合活動開賣，除重大瑕疵外不接受尺寸不合退換貨。</p>
            </div>
          </el-tab-pane>
        </el-tabs>

      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, computed, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import axios from '@/plugins/axios' // 走封裝實例：自動帶 Bearer token + 401/403 處理
import { useCartStore } from '../../stores/cart'
import { useToast } from '@/composables/useToast'


const route = useRoute()
const router = useRouter()
const cartStore = useCartStore()
const toast = useToast()

const goBack = () => {
  const from = route.query.from
  if (from === 'theme' && product.value?.theme?.themeId) {
    router.push({
      path: `/shop/theme/${product.value.theme.themeId}`,
      query: { from: 'theme' }
    })
  } else {
    router.push('/shop/home')
  }
}

const product = ref(null)
const loading = ref(false)

// 消費者挑選狀態
const selectedColor = ref('')
const selectedSize = ref('')
const purchaseQty = ref(1)
const activeImage = ref('')

// 頁面初始化：根據 URL 的 productId 向 Java 後端撈取單一商品詳細資料
onMounted(async () => {
  loading.value = true
  const productId = route.params.id

  try {
    // 串接 Java 後端 API (注意：後端可以寫一個針對前台詳情頁的 GET /api/shop/products/{id})
    // 這裡我們暫時先用全局 products 篩選，或直接請求該特定商品
    const res = await axios.get(`/shop/product/${productId}`)
    const found = res.data
    
    if (found) {
      product.value = found
      activeImage.value = found.mainImage
      // 預設自動選中第一個款式的顏色，提升使用者體驗
      if (found.variants && found.variants.length > 0) {
        selectedColor.value = found.variants[0].productColor
      }
      // 只有一個顏色時已由上方邏輯自動選中；只有一個尺寸時自動選取
      if (availableSizes.value.length === 1) {
        selectedSize.value = availableSizes.value[0]
      }
      await cartStore.fetchRemoteCart()
    } else {
      toast.error('找不到該款週邊商品！')
      router.push('/shop/home')
    }
  } catch (error) {
    console.error('撈取商品詳情失敗:', error)
    toast.error('連線後端失敗，無法載入商品。')
  } finally {
    loading.value = false
  }
})

const changeActiveImage = (url) => {
  activeImage.value = url
}

// 1. 萃取出該商品「所有不重複的顏色」清單
const uniqueColors = computed(() => {
  if (!product.value || !product.value.variants) return []
  return [...new Set(product.value.variants.map(v => v.productColor))]
})

// 2. 根據目前選中的顏色，「動態篩選出該顏色擁有的尺寸」
const availableSizes = computed(() => {
  if (!product.value || !product.value.variants || !selectedColor.value) return []
  const allSizes = product.value.variants
    .filter(v => v.productColor === selectedColor.value)
    .map(v => v.productSize)
    return [...new Set(allSizes)];
})

// 3. 核心：根據目前使用者挑選的「顏色 + 尺寸」，精確比對出是哪一個 Variant 實體
const currentVariant = computed(() => {
  if (!product.value || !selectedColor.value || !selectedSize.value) return null
  return product.value.variants.find(
    v => v.productColor === selectedColor.value && v.productSize === selectedSize.value
  ) || null
})

// 4. 動態單價：如果選中規格就顯示該規格價錢，否則顯示區間或最低價
const currentPrice = computed(() => {
  if (currentVariant.value) {
    return Number(currentVariant.value.unitPrice).toFixed(0)
  }
  if (!product.value || !product.value.variants || product.value.variants.length === 0) return '0'
  const prices = product.value.variants.map(v => Number(v.unitPrice))
  return Math.min(...prices).toFixed(0)
})

// 5. 按鈕文字動態防呆
const getButtonText = computed(() => {
  if (!selectedColor.value || !selectedSize.value) return '請先選取顏色與尺寸'
  if (currentVariant.value && currentVariant.value.stockQty === 0) return '此規格已售罄 (缺貨)'
  return '加入購物車🛒'
})

// 切換顏色時，將尺寸清空，逼迫使用者重新選擇，防止邏輯錯誤
const selectColor = (color) => {
  selectedColor.value = color
  selectedSize.value = ''
  purchaseQty.value = 1
  // 若該顏色下只有一個尺寸可選，直接自動選取，省去多一次點擊
  if (availableSizes.value.length === 1) {
    selectedSize.value = availableSizes.value[0]
  }
}

// 顯示層映射：FREE（不分大小寫）顯示為「One Size」，資料與送出值不變
const formatSizeLabel = (size) => {
  if (typeof size === 'string' && size.toUpperCase() === 'FREE') {
    return 'One Size'
  }
  return size
}

const selectSize = (size) => {
  selectedSize.value = size
  purchaseQty.value = 1
}

// 判斷某尺寸在目前選定顏色下是否真的斷貨
const isSizeOutOfStock = (size) => {
  const v = product.value.variants.find(
    item => item.productColor === selectedColor.value && item.productSize === size
  )
  return v ? v.stockQty === 0 : true
}

// 功能 6：執行加入購物車
const handleAddToCart = async () => {
  if (!currentVariant.value) {
    toast.error('請先選取顏色與尺寸！')
    return
  }

  // 商品新增數量若 <= 0, 顯示錯誤
  if (purchaseQty.value <= 0) {
    toast.error('購買數量必須大於 0 件！')
    return
  }

  // 庫存Check
  if (purchaseQty.value > currentVariant.value.stockQty) {
    toast.error(`庫存不足！目前該規格僅剩餘 ${currentVariant.value.stockQty} 件，無法加入購物車。`)
    return
  }

  // 調用 Pinia 全域方法，將主檔、選中款式與數量傳進去
  const result = await cartStore.addToCart(currentVariant.value.variantId, purchaseQty.value)

  if (!result || !result.ok) {
    if (result && result.reason === 'unauthenticated') {
      toast.info('請先登入會員')
      router.push('/login')
    } else {
      toast.error('加入購物車失敗，請稍後再試。')
    }
    return
  }

  // 彈出成功提示
  toast.success(`成功將【${product.value.productName} - ${currentVariant.value.productColor}/${formatSizeLabel(currentVariant.value.productSize)}】 ${purchaseQty.value}件 加入購物車！`)
}
</script>

<style scoped>
.product-detail-container {
  max-width: 1100px;
  margin: 0 auto;
}
.breadcrumb {
  margin-bottom: 25px;
}
.detail-layout {
  display: flex;
  gap: 50px;
  background: #fff;
  padding: 40px;
  border-radius: 8px;
  box-shadow: 0 4px 16px rgba(0,0,0,0.04);
}
.detail-left {
  flex: 1;
}
.main-image-box {
  position: relative;
  width: 100%;
  border: 1px solid #eee;
  border-radius: 4px;
  overflow: hidden;
}
.main-image-box img {
  width: 100%;
  display: block;
}
.theme-badge {
  position: absolute;
  top: 5px;
  left: 10px;
  background-color: #e57346;
  color: #fff;
  font-size: 12px;
  padding: 4px 12px;
  border-radius: 20px;
  font-weight: bold;
  z-index: 5;
}

.thumbnail-list {
  display: flex;
  gap: 12px;
  margin-top: 15px;
  overflow-x: auto; /* 若圖片過多可橫向滾動 */
  padding-bottom: 5px;
}

.thumbnail-item {
  width: 70px;
  height: 100px;
  border: 1px solid #ddd;
  border-radius: 4px;
  cursor: pointer;
  overflow: hidden;
  transition: all 0.2s ease;
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.thumbnail-item img {
  width: 100%;
  height: 100%;
  object-fit: cover; /* 確保圖片不變形地填滿小方格 */
}

/* 滑鼠懸停與選中狀態 */
.thumbnail-item:hover,
.thumbnail-item.active {
  border-color: #409eff; /* 變成 Element Plus 的主題藍色 */
  box-shadow: 0 2px 8px rgba(64, 158, 255, 0.2);
}

.detail-right {
  flex: 1.2;
}
.category-tag {
  font-size: 13px;
  color: #999;
  font-weight: 500;
  text-transform: uppercase;
}
.product-title {
  font-size: 26px;
  color: #222;
  margin: 8px 0 12px 0;
  font-weight: bold;
}
.product-sim-desc {
  font-size: 14px;
  color: #666;
  line-height: 1.6;
}
.price-panel {
  background-color: #fcf8f8;
  padding: 15px 20px;
  border-radius: 4px;
  margin-top: 20px;
  display: flex;
  align-items: baseline;
}
.price-label {
  font-size: 13px;
  color: #888;
  margin-right: 15px;
}
.currency {
  font-size: 14px;
  color: #f56c6c;
  font-weight: bold;
}
.price-amount {
  font-size: 28px;
  color: #f56c6c;
  font-weight: 900;
  margin-left: 4px;
}
.spec-label {
  font-size: 14px;
  color: #555;
  width: 90px;
  display: inline-block;
  font-weight: bold;
}
.spec-options {
  display: inline-flex;
  gap: 10px;
  flex-wrap: wrap;
  vertical-align: top;
}
.spec-btn {
  background: #fff;
  border: 1px solid #dcdfe6;
  padding: 8px 16px;
  font-size: 13px;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  color: #606266;
}
.spec-btn:hover:not(.disabled) {
  border-color: #409eff;
  color: #409eff;
}
.spec-btn.active {
  border-color: #409eff;
  background-color: #ecf5ff;
  color: #409eff;
  font-weight: bold;
  box-shadow: 0 2px 4px rgba(64,158,255,0.1);
}
.spec-btn.disabled {
  background-color: #f5f7fa;
  border-color: #e4e7ed;
  color: #c0c4cc;
  cursor: not-allowed;
  text-decoration: line-through;
}
.prompt-text {
  font-size: 13px;
  color: #909399;
  font-style: italic;
}
.purchase-action-row {
  background: #fafafa;
  padding: 20px;
  border-radius: 4px;
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.qty-selector {
  display: flex;
  align-items: center;
}
.add-cart-btn {
  width: 100%;
  height: 48px !important;
  font-size: 16px !important;
  font-weight: bold !important;
  letter-spacing: 1px;
}
.intro-content {
  padding: 15px 5px;
  font-size: 14px;
  color: #444;
  line-height: 1.8;
  white-space: pre-line; /* 保持換行格式 */
}
.no-data {
  color: #999;
  font-style: italic;
}

@media (max-width: 991.98px) {
  .product-detail-container {
    padding: 12px;
  }
  .detail-layout {
    flex-direction: column;
    padding: 20px;
    gap: 24px;
  }
  .detail-left, .detail-right {
    width: 100%;
    flex: none;
  }
  .spec-section {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
  .spec-label {
    width: auto;
    display: block;
    margin-bottom: 4px;
  }
  .spec-options {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .purchase-action-row {
    padding: 15px;
    gap: 15px;
  }
  .qty-selector {
    display: flex;
    flex-direction: column;
    align-items: flex-start;
    gap: 8px;
  }
}
</style>