<script setup>
/**
 * DashboardView.vue — 平台總覽（模組 1 ★ P0）
 *
 * 區塊：平台統計卡片 / 快捷入口 / 財務宏觀儀表板（空殼圖表）
 *       系統級公告改由全域 useSystemBanner 顯示，本頁不再硬編碼靜態公告。
 * API：GET /api/admin/dashboard/stats（後端未就緒時 fallback mock）
 * 
 * ---
 * ## 改動紀錄
 * - 引入響應式 `months` 變數（預設 6）並傳遞給 API 查詢參數。
 * - 加入 `watch(months)` 監聽變數變化以觸發 API 自動重刷。
 * - 於財務卡片右下角加入互動式 `range` 滑桿，以平衡下方區塊佈局。
 * - 將圖表高度調整為 220px 以優化卡片填滿度。
 */
import { ref, computed, watch, onMounted } from "vue";
import api from "@/plugins/axios.js";
import StatCard from "@/components/dashboard/StatCard.vue";
import FinanceChart from "@/components/dashboard/FinanceChart.vue";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";
import SystemHealthWidget from "@/components/dashboard/SystemHealthWidget.vue";

const { setAnnouncement } = useSystemBanner();

const months = ref(6);

// SWR 快取
const { data: stats, isLoading, refresh } = useCachedResource(
  "admin:dashboard:stats",
  () => api.get("/api/admin/dashboard/stats", { params: { months: months.value } }).then((r) => r.data),
  {
    initial: {
      userCount: 0,
      orgCount: 0,
      approvedOrgCount: 0,
      pendingKycCount: 0,
      todayLoginCount: 0,
      pendingSubmissionCount: 0,
      expiringContractCount: 0,
      monthlyFee: 0,
      subscription: { active: 0, expiringSoon: 0, expired: 0 },
      trend: { labels: [], data: [] },
    },
  }
);

const loading = computed(() => isLoading.value && (stats.value?.userCount ?? 0) === 0);

// 全平台總交易額趨勢
const trendLabels = computed(() => stats.value?.trend?.labels ?? []);
const trendData = computed(() => stats.value?.trend?.data ?? []);

watch(months, async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入儀表板統計失敗，請稍後再試。", "danger");
    }
  }
});

onMounted(async () => {
  try {
    await refresh();
  } catch (error) {
    // 有回應(4xx/5xx)：顯示錯誤狀態橫幅，不假裝成功；斷線情境由 axios 攔截器處理
    if (error.response) {
      setAnnouncement("載入儀表板統計失敗，請稍後再試。", "danger");
    }
  }
});

const formatCurrency = (n) => `NT$ ${Number(n ?? 0).toLocaleString("zh-Hant-TW")}`;
</script>

<template>
  <div>
    <!-- 頁面標題 -->
    <div class="d-flex align-items-center justify-content-between mb-4">
      <h4 class="fw-bold mb-0"><i class="bi bi-speedometer2 me-2" style="color: var(--tap-primary)"></i>平台總覽</h4>
    </div>

    <!-- 平台統計卡片 -->
    <div class="row g-3 mb-3">
      <div class="col-12 col-sm-6 col-xl-3">
        <StatCard icon="bi-people" label="使用者總數" :value="(stats.userCount ?? 0).toLocaleString()" variant="primary" :loading="loading" />
      </div>
      <div class="col-12 col-sm-6 col-xl-3">
        <StatCard icon="bi-building" label="組織總數" :value="(stats.orgCount ?? 0).toLocaleString()" variant="info" :loading="loading" />
      </div>
      <div class="col-12 col-sm-6 col-xl-3">
        <StatCard icon="bi-patch-check-fill" label="已核准組織" :value="(stats.approvedOrgCount ?? 0).toLocaleString()" variant="success" :loading="loading" />
      </div>
      <div class="col-12 col-sm-6 col-xl-3">
        <StatCard icon="bi-box-arrow-in-right" label="今日登入數" :value="(stats.todayLoginCount ?? 0).toLocaleString()" variant="warning" :loading="loading" />
      </div>
    </div>

    <!-- 快捷入口 -->
    <div class="row g-3 mb-3">
      <div class="col-12 col-md-4">
        <RouterLink to="/admin/organizers/kyc" class="card border shadow-sm rounded-4 hover-card text-decoration-none h-100">
          <div class="card-body d-flex align-items-center justify-content-between">
            <div>
              <div class="fw-bold mb-1"><i class="bi bi-patch-check me-2 text-warning"></i>待審 KYC</div>
              <div class="small text-tap-secondary">{{ stats.pendingKycCount }} 件等待審核</div>
            </div>
            <i class="bi bi-chevron-right text-tap-secondary"></i>
          </div>
        </RouterLink>
      </div>
      <div class="col-12 col-md-4">
        <RouterLink to="/admin/operations/submissions" class="card border shadow-sm rounded-4 hover-card text-decoration-none h-100">
          <div class="card-body d-flex align-items-center justify-content-between">
            <div>
              <div class="fw-bold mb-1"><i class="bi bi-headset me-2 text-danger"></i>未處理客訴</div>
              <div class="small text-tap-secondary">{{ stats.pendingSubmissionCount }} 件待處理</div>
            </div>
            <i class="bi bi-chevron-right text-tap-secondary"></i>
          </div>
        </RouterLink>
      </div>
      <div class="col-12 col-md-4">
        <RouterLink to="/admin/billing/contracts" class="card border shadow-sm rounded-4 hover-card text-decoration-none h-100">
          <div class="card-body d-flex align-items-center justify-content-between">
            <div>
              <div class="fw-bold mb-1"><i class="bi bi-file-earmark-text me-2 text-info"></i>即將到期合約</div>
              <div class="small text-tap-secondary">{{ stats.expiringContractCount }} 份 30 天內到期</div>
            </div>
            <i class="bi bi-chevron-right text-tap-secondary"></i>
          </div>
        </RouterLink>
      </div>
    </div>

    <!-- 財務與交易趨勢 -->
    <div class="row g-3">
      <div class="col-12 col-xl-8">
        <div class="card border shadow-sm rounded-4 h-100">
          <div class="card-body d-flex flex-column h-100">
            <div class="fw-bold mb-3"><i class="bi bi-graph-up-arrow me-2" style="color: var(--tap-primary)"></i>平台財務與交易趨勢</div>
            <div class="row g-3 flex-grow-1">
              <!-- 左欄：折線圖 -->
              <div class="col-12 col-lg-8 d-flex flex-column justify-content-between">
                <div class="flex-grow-1 d-flex align-items-center justify-content-center my-2">
                  <FinanceChart class="w-100" label="總交易額" :labels="trendLabels" :data="trendData" :height="200" :show-legend="false" />
                </div>
                <div class="d-flex align-items-center justify-content-between mt-2 px-1">
                  <!-- 左下角自訂圖例 -->
                  <div class="d-flex align-items-center gap-2">
                    <span class="d-inline-block rounded-circle" style="width: 10px; height: 10px; background-color: var(--tap-primary)"></span>
                    <span class="small text-tap-secondary">總交易額</span>
                  </div>
                  
                  <!-- 右下角拉桿 -->
                  <div class="d-flex align-items-center gap-2">
                    <span class="small text-tap-secondary text-nowrap">近 {{ months }} 個月</span>
                    <input type="range" id="trend-months-slider" class="form-range" min="3" max="12" step="1" style="width: 120px;" v-model.number="months" />
                  </div>
                </div>
              </div>

              <!-- 右欄：平台應收手續費 & 訂閱狀態 -->
              <div class="col-12 col-lg-4 financial-panel-divider ps-lg-4 d-flex flex-column justify-content-between">
                <div>
                  <div class="small fw-semibold text-tap-secondary mb-2">本月平台應收手續費</div>
                  <div class="fs-3 fw-bold text-success mb-2">{{ formatCurrency(stats.monthlyFee) }}</div>
                  <div class="small text-tap-secondary mb-3 pb-3 border-bottom" style="font-size: 0.75rem; line-height: 1.35;">
                    依商戶合約費率即時估算之平台服務收益。
                  </div>

                  <!-- 訂閱狀態總覽 -->
                  <div class="fw-semibold small mb-2 text-tap-secondary">訂閱狀態總覽</div>
                  <div class="d-flex justify-content-between text-center mb-3">
                    <div>
                      <div class="fs-6 fw-bold text-success">{{ stats.subscription?.active ?? 0 }}</div>
                      <div class="small text-tap-secondary" style="font-size: 0.7rem;">訂閱中</div>
                    </div>
                    <div>
                      <div class="fs-6 fw-bold text-warning">{{ stats.subscription?.expiringSoon ?? 0 }}</div>
                      <div class="small text-tap-secondary" style="font-size: 0.7rem;">即將到期</div>
                    </div>
                    <div>
                      <div class="fs-6 fw-bold text-tap-secondary">{{ stats.subscription?.expired ?? 0 }}</div>
                      <div class="small text-tap-secondary" style="font-size: 0.7rem;">已到期</div>
                    </div>
                  </div>
                </div>

                <div class="pt-2">
                  <RouterLink to="/admin/finance" class="btn btn-outline-secondary btn-sm w-100">
                    前往財務管理 <i class="bi bi-arrow-right ms-1"></i>
                  </RouterLink>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 右側：系統健康度 -->
      <div class="col-12 col-xl-4">
        <SystemHealthWidget />
      </div>
    </div>
  </div>
</template>

<style scoped>
@media (min-width: 992px) {
  .financial-panel-divider {
    border-left: 1px solid var(--tap-border);
  }
}
</style>
