<script setup>
/**
 * FinanceDashboard.vue — 財務宏觀儀表板（模組 6 ★ P1）
 *
 * 區塊：全平台總交易額趨勢（折線）/ 各廠商抽成佔比（甜甜圈）/
 *       本月平台應收手續費 / SaaS 訂閱狀態總覽
 * API：GET /api/admin/finance/dashboard?months=6|12
 */
import { ref, computed, watch, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import FinanceChart from "@/components/dashboard/FinanceChart.vue";
import StatCard from "@/components/dashboard/StatCard.vue";
import FinanceTabs from "@/components/finance/FinanceTabs.vue";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

const months = ref(6);

const EMPTY_DASHBOARD = {
  trend: { labels: [], data: [] },
  feeShare: { labels: [], data: [] },
  monthlyFee: 0,
  monthlyGmv: 0,
  subscription: { active: 0, expiringSoon: 0, expired: 0 },
};

// SWR 快取：單一快取鍵，藉由 params.months 動態抓取
const { data: dashboard, isLoading, refresh } = useCachedResource(
  "admin:finance:dashboard",
  () => api.get("/api/admin/finance/dashboard", { params: { months: months.value } }).then(r => r.data.data ?? EMPTY_DASHBOARD),
  { initial: EMPTY_DASHBOARD }
);

const loading = computed(() => isLoading.value && (dashboard.value?.monthlyGmv ?? 0) === 0);

const fetchDashboard = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入財務儀表板失敗，請稍後再試。", "danger");
    }
  }
};

watch(months, fetchDashboard);

onMounted(fetchDashboard);

const formatCurrency = (n) => `NT$ ${Number(n ?? 0).toLocaleString("zh-Hant-TW")}`;

// 各廠商手續費佔比：甜甜圈在組織數少時留白醜且資訊量低，
// 改用排行橫條列表——依金額由高到低，顯示實際金額與佔比 %。
const feeShareRows = computed(() => {
  const labels = dashboard.value?.feeShare?.labels ?? [];
  const data = dashboard.value?.feeShare?.data ?? [];
  const total = data.reduce((s, v) => s + Number(v ?? 0), 0);
  return labels
    .map((label, i) => ({
      label,
      value: Number(data[i] ?? 0),
      pct: total > 0 ? Math.round((Number(data[i] ?? 0) / total) * 100) : 0,
    }))
    .sort((a, b) => b.value - a.value)
    .slice(0, 5);
});

// 最新手續費結算（取後端 dashboard 回傳的 recentSettlements，最多 5 筆；
// 後端尚未提供時優雅顯示空狀態，不阻斷頁面）
const recentSettlements = computed(() => (dashboard.value?.recentSettlements ?? []).slice(0, 5));

// 結算「期間」屬靜態屬性 → 顯示到日；無值則破折號
const formatSettleDate = (t) => (t ? dayjs(t).format("YYYY-MM-DD") : "—");
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-cash-stack me-2" style="color: var(--tap-primary)"></i>財務管理</h4>
    </div>

    <div class="mb-3"><FinanceTabs /></div>

    <!-- 統計卡片 -->
    <div class="row g-3 mb-3">
      <div class="col-12 col-sm-6 col-xl-4">
        <StatCard icon="bi-graph-up-arrow" label="本月平台交易總額" :value="formatCurrency(dashboard.monthlyGmv)" variant="info" :loading="loading" />
      </div>
      <div class="col-12 col-sm-6 col-xl-4">
        <StatCard icon="bi-cash-coin" label="本月平台應收手續費" :value="formatCurrency(dashboard.monthlyFee)" variant="success" :loading="loading" />
      </div>
      <div class="col-12 col-sm-6 col-xl-4">
        <StatCard icon="bi-box-seam" label="訂閱中組織" :value="dashboard.subscription.active" variant="primary" :loading="loading" />
      </div>
    </div>

    <div class="row g-3">
      <!-- 趨勢折線圖 -->
      <div class="col-12 col-xl-8">
        <div class="card border shadow-sm rounded-4 h-100">
          <div class="card-body d-flex flex-column h-100">
            <div class="fw-bold mb-3"><i class="bi bi-graph-up me-2" style="color: var(--tap-primary)"></i>全平台總交易額趨勢</div>
            <div class="flex-grow-1 d-flex align-items-center justify-content-center my-3">
              <FinanceChart class="w-100" label="總交易額" :labels="dashboard.trend.labels" :data="dashboard.trend.data" :height="220" :show-legend="false" />
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
                <input type="range" id="finance-months-slider" class="form-range" min="3" max="12" step="1" style="width: 120px;" v-model.number="months" />
              </div>
            </div>
          </div>

          <!-- 最新手續費結算（操作型工作台：圖表下方直接看到核心數據） -->
          <div class="card-body border-top pt-3" style="border-color: var(--tap-border) !important">
            <div class="fw-bold mb-2 small">
              <i class="bi bi-list-columns-reverse me-2" style="color: var(--tap-primary)"></i>最新手續費結算
            </div>
            <div v-if="recentSettlements.length" class="table-responsive">
              <table class="table table-sm align-middle mb-0 small">
                <thead>
                  <tr class="text-tap-secondary">
                    <th>組織</th>
                    <th>結算期間</th>
                    <th class="text-end">交易額</th>
                    <th class="text-end">手續費</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="(s, i) in recentSettlements" :key="s.id ?? i">
                    <td class="text-truncate" :title="s.orgName">{{ s.orgName ?? "—" }}</td>
                    <td>{{ formatSettleDate(s.periodDate ?? s.settledAt) }}</td>
                    <td class="text-end">{{ formatCurrency(s.gmv) }}</td>
                    <td class="text-end text-success">{{ formatCurrency(s.fee) }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div v-else class="text-tap-secondary small py-2">
              尚無結算紀錄，或前往
              <RouterLink to="/admin/finance/settlement">手續費明細</RouterLink> 查看完整資料。
            </div>
          </div>
        </div>
      </div>

      <!-- 抽成佔比 + 訂閱總覽 -->
      <div class="col-12 col-xl-4">
        <div class="h-100 d-flex flex-column gap-3">
          <div class="card border shadow-sm rounded-4 flex-grow-1">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-bar-chart-line me-2" style="color: var(--tap-primary)"></i>本月手續費佔比前5組織</div>
              <div v-if="feeShareRows.length === 0" class="text-center text-tap-secondary py-4">
                <i class="bi bi-inbox fs-3 d-block mb-2"></i>本月尚無手續費資料
              </div>
              <div v-else class="d-flex flex-column gap-2">
                <div v-for="row in feeShareRows" :key="row.label">
                  <div class="d-flex justify-content-between align-items-center small mb-1">
                    <span class="text-truncate me-2" :title="row.label">{{ row.label }}</span>
                    <span class="text-tap-secondary flex-shrink-0">{{ formatCurrency(row.value) }}・{{ row.pct }}%</span>
                  </div>
                  <div class="progress" style="height: 6px">
                    <div class="progress-bar" role="progressbar" :style="{ width: row.pct + '%', backgroundColor: 'var(--tap-primary)' }"></div>
                  </div>
                </div>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm rounded-4">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-box-seam me-2" style="color: var(--tap-primary)"></i>訂閱狀態總覽</div>
              <div class="d-flex justify-content-between text-center">
                <div>
                  <div class="fs-4 fw-bold text-success">{{ dashboard.subscription.active }}</div>
                  <div class="small text-tap-secondary">訂閱中</div>
                </div>
                <div>
                  <div class="fs-4 fw-bold text-warning">{{ dashboard.subscription.expiringSoon }}</div>
                  <div class="small text-tap-secondary">30 天內到期</div>
                </div>
                <div>
                  <div class="fs-4 fw-bold text-tap-secondary">{{ dashboard.subscription.expired }}</div>
                  <div class="small text-tap-secondary">已到期</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
