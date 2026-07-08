<script setup>
import AuthCard from '@/components/auth/AuthCard.vue';
</script>

<template>
  <div class="d-flex align-items-center justify-content-center p-3 py-5" style="min-height: calc(100vh - 160px);">
    <div class="container" style="max-width: 1100px;">
      <div class="row align-items-center">
        
        <!-- 左側資訊區 (直接放於灰底背景上) -->
        <div class="col-md-7 p-4 p-md-5 d-flex flex-column justify-content-center">
          <h2 class="fw-bold mb-4" style="color: #212529; font-family: 'Georgia', serif;">
            Grow Your Business <br>With TAP Platform.
          </h2>
          <p class="text-dark mb-4 fs-6 lh-lg">
            為活動主辦方打造的全方位票務管理系統。從售票、驗票到即時數據分析，我們提供一站式的雲端解決方案，幫助您輕鬆舉辦每場精彩活動。
          </p>
          <ul class="list-unstyled text-primary" style="font-size: 0.95rem; font-weight: 600">
            <li class="mb-3 d-flex align-items-center">
              <span class="bg-primary text-white rounded-circle d-inline-flex justify-content-center align-items-center me-3" style="width: 20px; height: 20px; font-size: 0.75rem;">✓</span>
              即時票務與報表分析，掌握銷售脈動
            </li>
            <li class="mb-3 d-flex align-items-center">
              <span class="bg-primary text-white rounded-circle d-inline-flex justify-content-center align-items-center me-3" style="width: 20px; height: 20px; font-size: 0.75rem;">✓</span>
              高效驗票與入場管理，加速通關流程
            </li>
            <li class="mb-3 d-flex align-items-center">
              <span class="bg-primary text-white rounded-circle d-inline-flex justify-content-center align-items-center me-3" style="width: 20px; height: 20px; font-size: 0.75rem;">✓</span>
              內建周邊商城與透明金流結算
            </li>
          </ul>
        </div>

        <!-- 右側登入卡片 (獨立的白底陰影中卡片) -->
        <div class="col-md-5 d-flex justify-content-center p-3">
          <div class="w-100 bg-white rounded-4 shadow-sm border p-4 p-md-5" style="max-width: 420px;">
            
            <div class="d-flex align-items-center justify-content-between mb-4">
              <!-- 左側 Logo + TAP -->
              <div class="d-flex align-items-center gap-2">
                <img src="@/assets/images/logo.png" alt="TAP Logo" class="card-brand-logo" />
                <span class="card-brand-name">TAP</span>
              </div>
              <!-- 右側 主辦方登入文字 -->
              <div class="text-end">
                <h5 class="fw-bold mb-0 text-dark">主辦方登入</h5>
                <small class="text-muted" style="font-size: 0.75rem;">Organizer Login</small>
              </div>
            </div>

            <!-- 登入卡片主體 (寫死高度 320px) -->
            <div class="w-100 d-flex flex-column justify-content-center" style="height: 320px;">
              <AuthCard />
            </div>
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.card-brand-name {
  font-family: 'Syne', sans-serif;
  font-weight: 700;
  font-size: 1.75rem;
  letter-spacing: -0.5px;
}

.card-brand-logo {
  height: 38px;
  width: auto;
  object-fit: contain;
}
</style>
