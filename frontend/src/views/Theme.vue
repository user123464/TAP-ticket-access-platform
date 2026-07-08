<template>
  <section class="theme-page pb-5">
    <div class="theme-hero">
      <div class="img-box">
        <img :src="resolveImageUrl(theme.image)" :alt="theme.title">
      </div>
    </div>

    <div class="container theme-shell px-3 px-lg-4">
      <div class="theme-intro-card shadow-sm">
        <p class="theme-eyebrow mb-2">活動資訊</p>
        <h1 class="theme-title mb-3">{{ theme.title}}</h1>
        <p class="theme-detail mb-0">
          {{ theme.detail}}
        </p>
      </div>

      <!-- 活動周邊：僅在該主題有商城商品時顯示 -->
      <section v-if="shopProducts.length" class="merch-section mt-5">
        <div class="section-heading mb-4 d-flex justify-content-between align-items-end flex-wrap gap-2">
          <p class="theme-eyebrow mb-2">活動周邊</p>
          <RouterLink :to="{ path: `/shop/theme/${route.params.themeId}`, query: { from: 'theme' } }" class="merch-view-all">
            查看全部<i class="bi bi-arrow-right ms-1"></i>
          </RouterLink>
        </div>
        <div class="merch-grid">
          <div
            v-for="product in shopProducts.slice(0, 8)"
            :key="product.productId"
            class="merch-card"
            @click="goToProduct(product.productId)"
          >
            <div class="merch-img-box">
              <img :src="product.mainImage" alt="商品主圖">
            </div>
            <div class="merch-info">
              <div class="merch-name">{{ product.productName }}</div>
              <div class="merch-price">
                <span class="currency">NT$</span>
                <span class="price-num">{{ getMinPrice(product.variants) }}</span>
              </div>
            </div>
          </div>
        </div>
      </section>

      <section class="session-section mt-5">
        <div class="section-heading mb-4">
          <p class="theme-eyebrow mb-2">Sessions</p>
        </div>

        <div v-if="sessions.length" class="session-list">
          <div v-for="session in sessions" :key="session.sessionId" class="py-2">
            <Session :session="session" />
          </div>
        </div>

        <div v-else class="empty-state">
          目前尚無可顯示的場次資訊。
        </div>
      </section>

      <!-- 限定競標 -->
      <section v-if="auctions.length" class="auction-section mt-5">
        <div class="section-heading mb-4">
          <p class="theme-eyebrow mb-2">限定競標</p>
        </div>
        <div class="row g-3">
          <div
            v-for="auction in auctions"
            :key="auction.auctionId"
            class="col-12 col-sm-6 col-lg-4"
          >
            <AuctionCard :auction="auction" />
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import axios from '@/plugins/axios.js';
import { ref, onMounted, onUnmounted } from 'vue';
import Session from '@/components/Session.vue';
import AuctionCard from '@/components/AuctionCard.vue'
import { resolveImageUrl } from '@/utils/imageUrl'
import { getAuctions } from '@/api/auctionApi'

import { useRoute, useRouter } from 'vue-router';
const route = useRoute()
const router = useRouter()

// 初始化資料
const theme = ref({})
const sessions = ref([])
const auctions = ref([])
const shopProducts = ref([])
let auctionCleanupTimer = null


const fetchTheme = async () => {
  try {
    const res = await axios.get(`/api/themes/${route.params.themeId}`)
    theme.value = res.data.data
  } catch (e) {
    console.log(e);
  }
}

const fetchSessions = async () => {
  try {
    const res = await axios.get(
      `/api/themes/${route.params.themeId}/sessions`
    )
    sessions.value = res.data.data
  } catch (e) {
    console.log(e)
  }
}


const fetchActiveAuctions = async () => {
  try {
    auctions.value = await getAuctions(route.params.themeId)
  } catch (e) {
    console.log(e)
  }
}

const removeExpiredAuctions = () => {
  const now = Date.now()
  auctions.value = auctions.value.filter((auction) => {
    if (!auction.endTime) return true
    return new Date(auction.endTime).getTime() > now
  })
}

// 撈取該主題底下的周邊商品，用於活動頁的商城入口與展示區
const fetchShopProducts = async () => {
  try {
    const res = await axios.get(`/shop/theme/${route.params.themeId}`)
    if (res.data && Array.isArray(res.data)) {
      shopProducts.value = res.data
    }
  } catch (e) {
    console.log(e)
  }
}

// 尋找該款商品所有規格中的最低價格（沿用 ShopHome.vue / ShopTheme.vue 的作法）
const getMinPrice = (variants) => {
  if (!variants || variants.length === 0) return '0'
  const prices = variants.map(v => Number(v.unitPrice))
  return Math.min(...prices).toFixed(0)
}

const goToProduct = (productId) => {
  router.push({
    path: `/shop/product/${productId}`,
    query: { from: 'theme' }
  })
}

onMounted(() => {
  fetchTheme()
  fetchSessions()
  fetchActiveAuctions()
  fetchShopProducts()
  auctionCleanupTimer = setInterval(() => {
    removeExpiredAuctions()
  }, 1000)
})

onUnmounted(() => {
  clearInterval(auctionCleanupTimer)
})

</script>


<style scoped>
.theme-page {
  background:
    radial-gradient(circle at top, rgba(201, 160, 84, 0.14), transparent 32%),
    linear-gradient(180deg, #fbf7f1 0%, #fffdf9 38%, #ffffff 100%);
}

.theme-shell {
  margin-top: 2rem;
  position: relative;
  z-index: 1;
}

.theme-hero {
  position: relative;
}

.img-box {
  width: 100%;
  height: 60vh;
  min-height: 320px;
  overflow: hidden;
}

.img-box img {
  height: 100%;
  width: 100%;
  object-fit: cover;
  filter: saturate(1.05);
}

.theme-intro-card {
  max-width: 1120px;
  margin: 0 auto;
  padding: 2rem;
  border-radius: 1.75rem;
  background: rgba(255, 253, 249, 0.95);
  border: 1px solid rgba(92, 67, 37, 0.12);
  backdrop-filter: blur(10px);
}

.theme-eyebrow {
  font-size: 0.78rem;
  letter-spacing: 0.22em;
  text-transform: uppercase;
  color: #9a6b2f;
  font-weight: 700;
}

.theme-title {
  font-size: clamp(2rem, 5vw, 3.4rem);
  line-height: 1.1;
  color: #2f2418;
  font-weight: 800;
}

.theme-detail {
  width: 100%;
  max-width: none;
  font-size: 1.02rem;
  line-height: 1.8;
  color: #5f5348;
}

.session-section {
  max-width: 1120px;
  margin: 0 auto;
}

.section-heading {
  padding-top: 1.25rem;
  border-top: 1px solid rgba(92, 67, 37, 0.14);
}

.section-title {
  font-size: clamp(1.6rem, 3vw, 2.2rem);
  font-weight: 800;
  color: #2f2418;
}

.auction-section {
  max-width: 1120px;
  margin: 0 auto;
}

.shop-entry-btn {
  border-color: #e57346;
  color: #e57346;
}

.shop-entry-btn:hover {
  background-color: #e57346;
  border-color: #e57346;
  color: #fff;
}

.merch-section {
  max-width: 1120px;
  margin: 0 auto;
}

.merch-view-all {
  font-size: 0.9rem;
  color: #9a6b2f;
  text-decoration: none;
  font-weight: 600;
}

.merch-view-all:hover {
  color: #e57346;
}

.merch-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(200px, 1fr));
  gap: 20px;
}

.merch-card {
  background: #fff;
  border-radius: 12px;
  overflow: hidden;
  cursor: pointer;
  transition: transform 0.3s, box-shadow 0.3s;
  border: 1px solid rgba(92, 67, 37, 0.1);
}

.merch-card:hover {
  transform: translateY(-5px);
  box-shadow: 0 10px 20px rgba(0,0,0,0.08);
}

.merch-img-box {
  width: 100%;
  aspect-ratio: 1 / 1;
  overflow: hidden;
  background-color: #f5f5f5;
}

.merch-img-box img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.merch-info {
  padding: 12px 10px;
  text-align: left;
}

.merch-name {
  font-size: 14px;
  color: #555;
  margin-bottom: 8px;
  line-height: 1.4;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.merch-price {
  color: #b01f1f;
  font-family: 'Arial', sans-serif;
}

.merch-price .currency {
  font-size: 13px;
  font-weight: bold;
  margin-right: 2px;
}

.merch-price .price-num {
  font-size: 18px;
  font-weight: bold;
}

.empty-state {
  padding: 2rem;
  border-radius: 1.25rem;
  background: rgba(255, 248, 238, 0.9);
  color: #7b6754;
  text-align: center;
  border: 1px dashed rgba(154, 107, 47, 0.3);
}

@media (max-width: 991.98px) {
  .theme-shell {
    margin-top: 1.5rem;
  }

  .theme-intro-card {
    padding: 1.5rem;
  }
}

@media (max-width: 767.98px) {
  .img-box {
    height: 32vh;
    min-height: 240px;
  }

  .theme-shell {
    margin-top: 1rem;
  }

  .theme-intro-card {
    padding: 1.25rem;
    border-radius: 1.25rem;
  }

  .theme-detail {
    font-size: 0.98rem;
    line-height: 1.7;
  }
}
</style>