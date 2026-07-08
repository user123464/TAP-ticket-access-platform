<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue';
import { useRoute } from 'vue-router';
import { Dropdown } from 'bootstrap';
import BaseAvatar from '@/components/common/BaseAvatar.vue';
import { useAuthStore } from '@/stores/auth.js';
import { useCartStore } from '@/stores/cart.js';

const authStore = useAuthStore();
const cartStore = useCartStore();
const route = useRoute();

// 依目前路由是否位於 /shop 開頭，切換「主題購票」/「周邊商城」入口
const isInShop = computed(() => route.path.startsWith('/shop'));

const isLoggedIn = computed(() => authStore.isLoggedIn);
const userName = computed(() => authStore.userName);
const userAvatar = computed(() => authStore.userAvatar);

const dropdownRef = ref(null);
let dropdownInstance = null;

// 調用 Pinia Store 的登出功能 (內含實體 API 發送與本地清除)
const handleLogout = async () => {
  await authStore.logout();
};

watch(isLoggedIn, async (newVal) => {
  if (newVal) {
    await nextTick();
    if (dropdownRef.value) {
      dropdownInstance = new Dropdown(dropdownRef.value);
    }
    cartStore.fetchRemoteCart();
  } else {
    if (dropdownInstance) {
      dropdownInstance.dispose();
      dropdownInstance = null;
    }
    // 登出時清空本地購物車暫存
    cartStore.cartItems = [];
  }
});

onMounted(async () => {
  // 若初始掛載時已是登入狀態，手動初始化選單以防 watch 未觸發
  if (isLoggedIn.value) {
    await nextTick();
    if (dropdownRef.value && !dropdownInstance) {
      dropdownInstance = new Dropdown(dropdownRef.value);
    }
    cartStore.fetchRemoteCart();
  }
});

onUnmounted(() => {
  if (dropdownInstance) {
    dropdownInstance.dispose();
  }
});
</script>

<template>
  <div class="b2c-wrapper">
    <header>
      <nav class="navbar navbar-expand-lg navbar-dark bg-primary "> <!-- navbar-expand-lg RWD 斷點控制 -->
        <div class="container px-4"><!-- container左右留白 -->
          <RouterLink class="navbar-brand d-flex align-items-center gap-2" to="/">
            <!-- <i class="bi bi-ticket-perforated"></i> 活動首頁 -->
            <img src="@/assets/images/logo-light.png" alt="TAP Logo" class="brand-logo" />
            <span class="brand-name">TAP</span>
          </RouterLink>
          <!-- 漢堡按鈕（關鍵） -->
          <button class="navbar-toggler" type="button" data-bs-toggle="collapse" data-bs-target="#navMenu"
            aria-controls="navMenu" aria-expanded="false" aria-label="Toggle navigation">
            <span class="navbar-toggler-icon"></span>
          </button>
          <!-- 可收合內容 -->
          <div class="collapse navbar-collapse" id="navMenu">
            <div class="navbar-nav ms-auto d-flex gap-3 align-items-center">
              <RouterLink class="nav-link text-white d-flex align-items-center px-2 me-2" to="/shop/cart" title="購物車">
                <span class="position-relative d-inline-block">
                  <i class="bi bi-cart fs-5"></i>
                  <span
                    v-if="cartStore.totalCount > 0"
                    class="position-absolute top-0 start-100 translate-middle badge rounded-pill bg-danger shadow-sm z-1"
                    style="font-size: 0.65rem; padding: 0.25em 0.5em;"
                  >
                    {{ cartStore.totalCount > 99 ? '99+' : cartStore.totalCount }}
                  </span>
                </span>
              </RouterLink>

              <!-- 未登入 -->
              <RouterLink v-if="!isLoggedIn" class="nav-link text-white" to="/login">
                <i class="bi bi-person-circle"></i> 登入
              </RouterLink>

              <!-- 已登入 (頭像下拉選單) -->
              <div v-else class="nav-item dropdown d-flex align-items-center">
                <a ref="dropdownRef" class="nav-link text-white dropdown-toggle d-flex align-items-center gap-2" href="#" role="button" data-bs-toggle="dropdown" aria-expanded="false">
                  <BaseAvatar :src="userAvatar" :seed="userName" size="28" alt="Avatar" class="border border-white" />
                  <span>{{ userName }}</span>
                </a>
                <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2">
                  <li>
                    <RouterLink class="dropdown-item" to="/member/orders">
                      <i class="bi bi-receipt me-2"></i>訂單查詢
                    </RouterLink>
                  </li>
                  <li>
                    <RouterLink class="dropdown-item" to="/settings/profile">
                      <i class="bi bi-person-gear me-2"></i>個人資料管理
                    </RouterLink>
                  </li>
                  <li>
                    <RouterLink class="dropdown-item" to="/settings/account">
                      <i class="bi bi-shield-lock me-2"></i>帳號管理
                    </RouterLink>
                  </li>
                  <li>
                    <hr class="dropdown-divider">
                  </li>
                  <li>
                    <a class="dropdown-item text-danger" href="#" @click.prevent="handleLogout">
                      <i class="bi bi-box-arrow-right me-2"></i>登出
                    </a>
                  </li>
                </ul>
              </div>
            </div>
          </div>
        </div>
      </nav>
    </header>

    <main>
      <RouterView />
    </main>


    <footer class="text-bg-info mt-5 py-4">
      <div class="container px-4">

        <div class="row">

          <!-- 1. 系統名稱 -->
          <div class="col-md-3 mb-3 mb-md-0">
            <h5 class="fw-bold">TAP售票系統</h5>
            <p class="text-secondary small">
              提供活動、票券、商城整合平台
            </p>
          </div>

          <!-- 2. 會員服務 -->
          <div class="col-md-3 mb-3 mb-md-0">
            <h6 class="fw-bold">會員服務</h6>
            <div class="d-flex gap-5">
              <ul class="list-unstyled mb-0">
                <li>
                  <RouterLink to="/" class="text-secondary text-decoration-none small">活動首頁</RouterLink>
                </li>
                <li>
                  <RouterLink to="/member/orders" class="text-secondary text-decoration-none small">訂單查詢</RouterLink>
                </li>
                <li>
                  <RouterLink to="/themes" class="text-secondary text-decoration-none small">探索活動</RouterLink>
                </li>
              </ul>
              <ul class="list-unstyled mb-0">
                <li>
                  <RouterLink to="/docs/terms" class="text-secondary text-decoration-none small">服務條款</RouterLink>
                </li>
                <li>
                  <RouterLink to="/docs/privacy" class="text-secondary text-decoration-none small">隱私權政策</RouterLink>
                </li>
              </ul>
            </div>
          </div>

          <!-- 3. 主辦方專區 -->
          <div class="col-md-3 mb-3 mb-md-0">
            <h6 class="fw-bold">主辦方專區</h6>
            <ul class="list-unstyled">
              <li>
                <RouterLink to="/org/docs/guide" class="text-secondary text-decoration-none small">如何成為主辦方</RouterLink>
              </li>
              <li>
                <RouterLink to="/org/login" class="text-secondary text-decoration-none small">主辦方登入 / 註冊</RouterLink>
              </li>
            </ul>
          </div>

          <!-- 4. 聯絡我們 -->
          <div class="col-md-3">
            <h6 class="fw-bold">聯絡我們</h6>
            <p class="text-secondary mb-1 small">Email: hi@ticket.com</p>
            <p class="text-secondary small">Tel: 0800-123-456</p>
          </div>

        </div>

        <hr class="border-secondary">

        <div class="text-center text-secondary small">
          © 2026 TAP Ticket System. All rights reserved.
        </div>

      </div>
    </footer>
  </div>
</template>





<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Syne:wght@400..800&display=swap');

/* Brand 字型與 Logo 樣式 */
.brand-name {
  font-family: 'Syne', sans-serif;
  font-weight: 700;
  font-size: 1.4rem;
  letter-spacing: -0.5px;
  color: #ffffff !important;
  /* B2C 橘色背景導覽列上使用純白文字 */
}

.brand-logo {
  height: 32px;
  width: auto;
  object-fit: contain;
}

/* 讓頭像下拉選單在手機版 (窄版) 依然維持絕對定位浮動，不撐開導覽列 */
@media (max-width: 991.98px) {
  .navbar-nav .dropdown {
    position: relative;
  }

  .navbar-nav .dropdown-menu {
    position: absolute !important;
    right: auto;
    left: 0;
  }
}
</style>
