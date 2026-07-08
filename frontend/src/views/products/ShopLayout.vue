<template>
  <div class="shop-layout">
    <!-- <header class="shop-header">
      <div class="header-container">
        <div class="shop-logo" @click="$router.push('/shop/home')">
          <div v-if="product">
            <span>{{ product.theme?.title }}</span>
          </div>
          <span class="sub-brand">週邊商城</span>
        </div> -->

        <!-- <div class="header-cart" @click="$router.push('/shop/cart')">
          <el-badge :value="cartStore.totalCount" :max="99" class="cart-badge" :hidden="cartStore.totalCount === 0">
            <div class="cart-icon-btn">
              <el-icon :size="24"><ShoppingCart /></el-icon>
              <span class="cart-text">購物車</span>
            </div>
          </el-badge>
        </div> -->
      <!-- </div> -->
    <!-- </header> -->

    <main class="shop-main">
      <router-view></router-view>
    </main>

  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios';


// 訂閱全域購物車狀態，頂部 Badge 會自動隨 LocalStorage 連動更新
const productList = ref([]);
const loading = ref(false)
const route = useRoute()
const router = useRouter()
import { ElMessage } from 'element-plus'

onMounted(async () =>{
  loading.value = true
  const themeId = route.params.themeId
  try{
    const res = await axios.get(`/shop/home`)
    const found = res.data

    if (found) {
      productList.value = found
    }else {
      ElMessage.error('找不到該款週邊商品！')
      router.push('/shop/home')
    }
    

  }
  catch (error) {
    console.error('撈取商品詳情失敗:', error)
    ElMessage.error('連線後端失敗，無法載入商品。')
  } finally {
    loading.value = false
  }
})

</script>

<style scoped>
.shop-layout {
  min-height: 100vh;
  display: flex;
  Shu: flex;
  flex-direction: column;
  background-color: #f5f5f5;
}
.shop-header {
  background-color: #ffffff;
  box-shadow: 0 2px 8px rgba(0,0,0,0.06);
  position: sticky;
  top: 0;
  z-index: 1000;
}
.header-container {
  max-width: 1200px;
  margin: 0 auto;
  height: 70px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 20px;
}
.shop-logo {
  cursor: pointer;
}
.brand-name {
  font-size: 26px;
  font-weight: 900;
  letter-spacing: 1px;
  color: #1a1a1a;
}
.sub-brand {
  font-size: 14px;
  color: #777;
  margin-left: 8px;
  border-left: 1px solid #ccc;
  padding-left: 8px;
}
.shop-nav span {
  margin: 0 15px;
  font-size: 15px;
  color: #444;
  cursor: pointer;
  font-weight: 500;
  transition: color 0.3s;
}
.shop-nav span:hover, .shop-nav span.active {
  color: #409EFF;
}
.back-to-admin {
  color: #e6a23c !important;
  font-size: 13px !important;
}
.header-cart {
  cursor: pointer;
  padding: 5px 10px;
  border-radius: 20px;
  transition: background 0.3s;
}
.header-cart:hover {
  background-color: #f0f2f5;
}
.cart-icon-btn {
  display: flex;
  align-items: center;
  color: #333;
}
.cart-text {
  margin-left: 6px;
  font-size: 14px;
  font-weight: bold;
}
.shop-main {
  flex: 1;
  max-width: 1200px;
  width: 100%;
  margin: 20px auto;
  padding: 0 20px;
  box-sizing: border-box;
}

</style>