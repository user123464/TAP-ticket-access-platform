<template>
  <div class="container-xl py-4 position-relative">
    <!-- [edit by Jason] 功能權限鎖定 (Feature Gate) 遮罩提示 -->
    <div
      v-if="!isKycApproved"
      class="kyc-gate-overlay rounded-4 p-5 text-center d-flex flex-column align-items-center justify-content-center"
    >
      <div
        class="icon-wrapper mb-3 bg-danger-subtle text-danger rounded-circle d-flex align-items-center justify-content-center animate-bounce"
        style="width: 70px; height: 70px"
      >
        <i class="bi bi-shield-lock-fill fs-2"></i>
      </div>
      <h4 class="fw-bold text-dark mb-2">財務結算功能已鎖定</h4>
      <p
        class="text-secondary small mb-4 mx-auto"
        style="max-width: 420px; line-height: 1.6"
      >
        根據金流合規要求，主辦方必須通過
        <strong>實名驗證 (KYC)</strong>
        審核，方可開通結算帳單查詢、財務明細與提現功能。
      </p>
      <RouterLink
        :to="`/org/${currentOrgId}/settings/profile`"
        class="btn btn-primary px-4 py-2 rounded-3 text-white fw-bold shadow-sm focus-ring"
      >
        前往實名驗證補件
      </RouterLink>
    </div>

    <!-- 原始頁面內容 (未驗證時以高斯模糊處理) -->
    <div :class="{ 'kyc-locked-blur': !isKycApproved }">
      <div class="w-100">
        <!-- 頂部功能列與篩選器 -->
        <header class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2">
          <h3 class="fw-bold mb-0 text-dark">帳務結算</h3>
          <div class="d-flex flex-wrap gap-1 align-items-center justify-content-end filter-container">
            <!-- 搜尋文字框 -->
            <div class="search-box-wrapper">
              <input
                v-model="searchQuery"
                type="text"
                class="form-control form-control-sm search-input animate-all"
                placeholder="搜尋訂單編號..."
              />
            </div>
            
            <!-- 營利種類篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-sm dropdown-toggle border animate-all"
                :class="{
                  'btn-outline-secondary': selectedRevenueType === '全部',
                  'btn-revenue-ticket fw-bold': selectedRevenueType === '票務',
                  'btn-revenue-merch fw-bold': selectedRevenueType === '商品'
                }"
                data-bs-toggle="dropdown"
              >
                營利種類: {{ selectedRevenueType }}
              </button>
              <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown">
                <li>
                  <a class="dropdown-item d-flex align-items-center gap-2 text-secondary" @click="selectedRevenueType = '全部'">
                    <span class="dot-indicator bg-secondary"></span>全部
                  </a>
                </li>
                <li v-if="availableRevenueTypes.includes('票務')">
                  <a class="dropdown-item d-flex align-items-center gap-2 item-revenue-ticket fw-semibold" @click="selectedRevenueType = '票務'">
                    <span class="dot-indicator bg-royal-blue"></span>票務
                  </a>
                </li>
                <li v-if="availableRevenueTypes.includes('商品')">
                  <a class="dropdown-item d-flex align-items-center gap-2 item-revenue-merch fw-semibold" @click="selectedRevenueType = '商品'">
                    <span class="dot-indicator bg-forest-teal"></span>商品
                  </a>
                </li>
              </ul>
            </div>

            <!-- 年份篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-sm btn-outline-secondary dropdown-toggle animate-all"
                data-bs-toggle="dropdown"
              >
                年份: {{ selectedYear }}
              </button>
              <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown">
                <li>
                  <a class="dropdown-item" @click="selectedYear = '全部'">全部</a>
                </li>
                <li v-for="year in availableYears" :key="year">
                  <a class="dropdown-item" @click="selectedYear = year">{{ year }}年</a>
                </li>
              </ul>
            </div>

            <!-- 月份篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-sm btn-outline-secondary dropdown-toggle animate-all"
                data-bs-toggle="dropdown"
              >
                月份: {{ selectedMonth }}
              </button>
              <ul class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown">
                <li>
                  <a class="dropdown-item" @click="selectedMonth = '全部'">全部</a>
                </li>
                <li v-for="month in availableMonths" :key="month">
                  <a class="dropdown-item" @click="selectedMonth = month">{{ month }}月</a>
                </li>
              </ul>
            </div>

            <!-- 主辦方篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-sm btn-outline-secondary dropdown-toggle animate-all"
                data-bs-toggle="dropdown"
              >
                主辦方: {{ selectedOrganizer }}
              </button>
              <ul
                class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
              >
                <li>
                  <a class="dropdown-item" @click="selectedOrganizer = '全部'"
                    >全部</a
                  >
                </li>
                <li v-for="name in uniqueOrgNames" :key="name">
                  <a class="dropdown-item" @click="selectedOrganizer = name"
                    >{{ name }}</a
                  >
                </li>
              </ul>
            </div>

            <!-- 狀態篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-sm btn-outline-secondary dropdown-toggle animate-all"
                data-bs-toggle="dropdown"
              >
                狀態: {{ selectedStatus }}
              </button>
              <ul
                class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
              >
                <li>
                  <a class="dropdown-item" @click="selectedStatus = '全部'"
                    >全部</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedStatus = '已撥款'"
                    >已撥款</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedStatus = '待處裡'"
                    >待處裡</a
                  >
                </li>
              </ul>
            </div>

            <!-- 每頁筆數下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-sm btn-outline-secondary dropdown-toggle animate-all"
                data-bs-toggle="dropdown"
              >
                每頁 {{ pageSize }} 筆
              </button>
              <ul
                class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
              >
                <li>
                  <a class="dropdown-item" @click="pageSize = 10">10筆</a>
                </li>
                <li>
                  <a class="dropdown-item" @click="pageSize = 20">20筆</a>
                </li>
                <li>
                  <a class="dropdown-item" @click="pageSize = 30">30筆</a>
                </li>
              </ul>
            </div>

            <!-- 匯出報表按鈕 -->
            <button
              class="btn btn-sm btn-primary text-white d-flex align-items-center gap-2 fw-semibold animate-all"
              @click="exportReport"
            >
              <i class="bi bi-file-earmark-arrow-down-fill"></i>
              匯出報表
            </button>
          </div>
        </header>

        <!-- 營利分析儀表板圖表與營業額統計 (New!) -->
        <section class="row g-3 mb-4">
          <!-- 左側：數據統計卡與營利佔比分析 -->
          <div class="col-lg-7">
            <div class="card border-0 shadow-sm rounded-4 h-100 bg-white">
              <div class="card-body p-4 d-flex flex-column justify-content-between">
                <div>
                  <h5 class="fw-bold text-dark mb-3 d-flex align-items-center gap-2">
                    <i class="bi bi-calculator-fill text-primary"></i> 營業額結算
                  </h5>
                  <div class="row g-2">
                    <!-- 總營業額 -->
                    <div class="col-6 col-sm-3 col-lg-6 col-xl-3">
                      <div class="p-2 rounded-3 bg-primary-subtle bg-opacity-25 border border-primary-subtle border-opacity-50">
                        <div class="d-flex align-items-center gap-1.5 text-primary mb-1">
                          <i class="bi bi-cash-coin small"></i>
                          <span class="small fw-semibold" style="font-size: 0.7rem;">總營業額</span>
                        </div>
                        <h5 class="fw-bold text-dark mb-0">${{ stats.totalGmv }}</h5>
                      </div>
                    </div>
                    <!-- 實際撥款 -->
                    <div class="col-6 col-sm-3 col-lg-6 col-xl-3">
                      <div class="p-2 rounded-3 bg-success-subtle bg-opacity-25 border border-success-subtle border-opacity-50">
                        <div class="d-flex align-items-center gap-1.5 text-success mb-1">
                          <i class="bi bi-wallet2 small"></i>
                          <span class="small fw-semibold" style="font-size: 0.7rem;">實際撥款額</span>
                        </div>
                        <h5 class="fw-bold text-dark mb-0">${{ stats.totalPayout }}</h5>
                      </div>
                    </div>
                    <!-- 平台手續費 -->
                    <div class="col-6 col-sm-3 col-lg-6 col-xl-3">
                      <div class="p-2 rounded-3 bg-warning-subtle bg-opacity-25 border border-warning-subtle border-opacity-50">
                        <div class="d-flex align-items-center gap-1.5 text-warning mb-1">
                          <i class="bi bi-percent small"></i>
                          <span class="small fw-semibold" style="font-size: 0.7rem;">平台手續費</span>
                        </div>
                        <h5 class="fw-bold text-dark mb-0">${{ stats.totalFee }}</h5>
                      </div>
                    </div>
                    <!-- 訂單筆數 -->
                    <div class="col-6 col-sm-3 col-lg-6 col-xl-3">
                      <div class="p-2 rounded-3 bg-secondary-subtle bg-opacity-25 border border-secondary-subtle border-opacity-50">
                        <div class="d-flex align-items-center gap-1.5 text-secondary mb-1">
                          <i class="bi bi-cart-check small"></i>
                          <span class="small fw-semibold" style="font-size: 0.7rem;">交易訂單</span>
                        </div>
                        <h5 class="fw-bold text-dark mb-0">{{ stats.orderCount }} 筆</h5>
                      </div>
                    </div>
                  </div>
                </div>
                
                <!-- 佔比圓形環狀圖 -->
                <div class="mt-4 pt-3 border-top d-flex align-items-center justify-content-between flex-wrap gap-2">
                  <div class="d-flex align-items-center gap-3">
                    <div class="donut-chart-container position-relative">
                      <!-- SVG Donut Chart -->
                      <svg width="76" height="76" viewBox="0 0 100 100">
                        <circle cx="50" cy="50" r="40" fill="transparent" stroke="#f1f3f5" stroke-width="12" />
                        <circle
                          v-if="stats.ticketGmv > 0"
                          cx="50"
                          cy="50"
                          r="40"
                          fill="transparent"
                          stroke="#0d6efd"
                          stroke-width="12"
                          :stroke-dasharray="`${donutChartData.ticketDash} 251.3`"
                          stroke-dashoffset="0"
                          transform="rotate(-90 50 50)"
                        />
                        <circle
                          v-if="stats.merchGmv > 0"
                          cx="50"
                          cy="50"
                          r="40"
                          fill="transparent"
                          stroke="#0dcaf0"
                          stroke-width="12"
                          :stroke-dasharray="`${donutChartData.merchDash} 251.3`"
                          :stroke-dashoffset="-donutChartData.ticketDash"
                          transform="rotate(-90 50 50)"
                        />
                      </svg>
                      <!-- Center Text -->
                      <div class="donut-center-text">
                        <span class="text-secondary" style="font-size: 0.55rem; scale: 0.85;">總計</span>
                        <span class="fw-bold text-dark" style="font-size: 0.75rem;">${{ Math.round(Number(stats.totalGmv)) }}</span>
                      </div>
                    </div>
                    
                    <div class="d-flex flex-column gap-1">
                      <div class="d-flex align-items-center gap-2" style="font-size: 0.75rem;">
                        <span class="legend-dot bg-primary"></span>
                        <span class="text-secondary">票務:</span>
                        <span class="fw-bold text-dark">${{ Number(stats.ticketGmv).toFixed(2) }} ({{ donutChartData.ticketPct }}%)</span>
                      </div>
                      <div class="d-flex align-items-center gap-2" style="font-size: 0.75rem;">
                        <span class="legend-dot bg-info"></span>
                        <span class="text-secondary">商品:</span>
                        <span class="fw-bold text-dark">${{ Number(stats.merchGmv).toFixed(2) }} ({{ donutChartData.merchPct }}%)</span>
                      </div>
                    </div>
                  </div>
                  
                  <div class="text-secondary small" style="font-size: 0.7rem;">
                    * 佔比依據當前篩選數據即時計算
                  </div>
                </div>
              </div>
            </div>
          </div>

          <!-- 右側：營業額月度趨勢 -->
          <div class="col-lg-5">
            <div class="card border-0 shadow-sm rounded-4 h-100 bg-white">
              <div class="card-body p-4 d-flex flex-column justify-content-between">
                <div>
                  <h5 class="fw-bold text-dark mb-1 d-flex align-items-center gap-2">
                    <i class="bi bi-graph-up-arrow text-info"></i> 撥款趨勢
                  </h5>
                  <p class="text-secondary mb-3" style="font-size: 0.7rem;">按月份統計實際撥款金額變動</p>
                </div>
                
                <!-- Bar Chart Wrapper -->
                <div class="chart-wrapper mt-auto pt-2">
                  <div v-if="monthlyTrendData.length > 0">
                    <svg width="100%" height="95" viewBox="0 0 300 100" preserveAspectRatio="none">
                      <!-- Grid lines -->
                      <line x1="0" y1="20" x2="300" y2="20" stroke="#f8f9fa" stroke-width="1" />
                      <line x1="0" y1="50" x2="300" y2="50" stroke="#f8f9fa" stroke-width="1" />
                      <line x1="0" y1="80" x2="300" y2="80" stroke="#e9ecef" stroke-width="1.5" />
                      
                      <!-- Bars -->
                      <g v-for="(bar, index) in monthlyTrendData" :key="index">
                        <!-- Bar rect with gradient fill -->
                        <rect
                          :x="index * (300 / monthlyTrendData.length) + (300 / monthlyTrendData.length) * 0.2"
                          :y="80 - bar.heightPct"
                          :width="(300 / monthlyTrendData.length) * 0.6"
                          :height="bar.heightPct"
                          fill="url(#trendGradient)"
                          rx="3"
                          class="chart-bar"
                        />
                        <!-- Amount text (tooltip style) -->
                        <text
                          :x="index * (300 / monthlyTrendData.length) + (300 / monthlyTrendData.length) * 0.5"
                          :y="75 - bar.heightPct"
                          text-anchor="middle"
                          fill="#495057"
                          style="font-size: 6.5px; font-weight: bold;"
                          v-if="bar.heightPct > 15"
                        >
                          ${{ Math.round(Number(bar.amount)) }}
                        </text>
                        <!-- Month label -->
                        <text
                          :x="index * (300 / monthlyTrendData.length) + (300 / monthlyTrendData.length) * 0.5"
                          y="94"
                          text-anchor="middle"
                          fill="#6c757d"
                          style="font-size: 8px; font-weight: 500;"
                        >
                          {{ bar.label }}
                        </text>
                      </g>

                      <!-- Gradient Defs -->
                      <defs>
                        <linearGradient id="trendGradient" x1="0" y1="0" x2="0" y2="1">
                          <stop offset="0%" stop-color="#0d6efd" stop-opacity="0.9" />
                          <stop offset="100%" stop-color="#0dcaf0" stop-opacity="0.6" />
                        </linearGradient>
                      </defs>
                    </svg>
                  </div>
                  <div v-else class="text-center py-4 text-secondary small">
                    暫無營業額月份趨勢數據
                  </div>
                </div>
              </div>
            </div>
          </div>
        </section>

        <!-- 表格主體 -->
        <main>
          <div class="table-responsive bg-white rounded shadow-sm border">
            <table class="table table-hover table-bordered align-middle mb-0">
              <thead class="table-light">
                <tr class="text-center">
                  <th scope="col" class="py-2">結算帳單 ID</th>
                  <th scope="col" class="py-2">營利種類</th>
                  <th scope="col" class="py-2">主辦方</th>
                  <th scope="col" class="py-2">當期總營業額</th>
                  <th scope="col" class="py-2">實際撥款金額</th>
                  <th scope="col" class="py-2">撥款狀態</th>
                  <th scope="col" class="py-2">撥款完成時間</th>
                  <th scope="col" class="py-2">訂單建立時間</th>
                </tr>
              </thead>
              <tbody class="text-center">
                <tr 
                  v-for="data in tableData" 
                  :key="data.settlement_id"
                  @click="data.revenue_type === '商品' ? selectedOrder = data : null"
                  :style="data.revenue_type === '商品' ? 'cursor: pointer;' : ''"
                  class="settlement-row"
                >
                  <td class="fw-semibold text-secondary">{{ data.settlement_id }}</td>
                  <td>
                    <span
                      class="badge rounded-pill px-2.5 py-1 text-uppercase animate-all"
                      :class="
                        data.revenue_type === '票務'
                          ? 'badge-revenue-ticket'
                          : 'badge-revenue-merch'
                      "
                      style="font-size: 0.72rem; font-weight: 700; letter-spacing: 0.5px;"
                    >
                      {{ data.revenue_type || '票務' }}
                    </span>
                  </td>
                  <td>{{ data.organizer_name || data.organizer_id }}</td>
                  <td class="fw-bold text-dark">${{ data.total_orders_amount }}</td>
                  <td class="fw-bold text-success">${{ data.final_payout_amount }}</td>
                  <td>
                    <span
                      class="badge rounded-pill px-2.5 py-1 d-inline-flex align-items-center"
                      :class="
                        data.item_status === '已撥款'
                          ? 'badge-soft-success'
                          : 'badge-soft-danger'
                      "
                    >
                      <span
                        class="status-dot me-2"
                        :class="
                          data.item_status === '已撥款'
                            ? 'bg-success'
                            : 'bg-danger'
                        "
                      ></span>
                      {{ data.item_status }}
                    </span>
                  </td>
                  <td class="text-muted">
                    {{
                      data.item_status === "已撥款" && data.processed_at
                        ? data.processed_at.replace("T", " ").substring(0, 19)
                        : "-"
                    }}
                  </td>
                  <td class="text-muted">
                    {{
                      data.created_at
                        ? data.created_at.replace("T", " ").substring(0, 19)
                        : "-"
                    }}
                  </td>
                </tr>
                <tr v-if="filteredSettlements.length === 0">
                  <td colspan="8" class="text-center py-4 text-secondary fs-7">
                    查無符合條件的結算訂單資料。
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
          <MerchDetail :order="selectedOrder" />
        </main>
      </div>
    </div>
  </div>
</template>



<script setup>
// =========================================================================
// [edit by Jason] 功能權限鎖定 (Feature Gate) 實作
// =========================================================================

import axios from "@/plugins/axios.js";
import { ref, computed, watch, onMounted } from "vue";
import { storeToRefs } from "pinia";
import { useWorkspaceStore } from "@/stores/workspace.js";

const workspaceStore = useWorkspaceStore();
const { isKycApproved, currentOrgId, myOrgs } = storeToRefs(workspaceStore);

// 每頁顯示筆數
const pageSize = ref(10);

// 篩選主辦方：全部、特定組織名稱
const selectedOrganizer = ref("全部");

// 篩選狀態：全部、已撥款、待處裡
const selectedStatus = ref("全部");

// 新增：篩選營利種類、年份、月份
const selectedRevenueType = ref("全部");
const selectedYear = ref("全部");
const selectedMonth = ref("全部");

// 新增：搜尋關鍵字
const searchQuery = ref("");

const selectedOrder = ref(null);

// 結算紀錄資料庫數據
const settlements = ref([]);

// 取得結算紀錄的函數 (POST 請求對接 SettlementController)
const fetchSettlements = async () => {
  try {
    if (selectedOrganizer.value === "全部") {
      if (myOrgs.value.length === 0) {
        await workspaceStore.fetchMyOrgs();
      }
      
      const promises = myOrgs.value.map(org => 
        axios.post("/api/settlements/query", {
          organizerId: org.id,
          status: selectedStatus.value,
        }).catch(err => {
          console.error(`無法取得組織 ${org.name} 的結算資料:`, err);
          return { data: [] };
        })
      );
      
      const results = await Promise.all(promises);
      const merged = results.flatMap(res => res.data);
      merged.sort((a, b) => new Date(b.created_at || 0) - new Date(a.created_at || 0));
      settlements.value = merged;
    } else {
      const targetOrg = myOrgs.value.find(org => org.name === selectedOrganizer.value);
      if (!targetOrg) return;
      const response = await axios.post("/api/settlements/query", {
        organizerId: targetOrg.id,
        status: selectedStatus.value,
      });
      settlements.value = response.data;
    }
  } catch (error) {
    console.error("無法取得結算資料:", error);
  }
};

// 輔助函式：取得某筆紀錄的年份與月份
const getRecordYear = (item) => {
  if (!item.created_at) return null;
  return new Date(item.created_at).getFullYear().toString();
};

const getRecordMonth = (item) => {
  if (!item.created_at) return null;
  return (new Date(item.created_at).getMonth() + 1).toString();
};

// ── 連動（串接）篩選選單選項邏輯 ──

// 1. 營利種類選單選項連動
const availableRevenueTypes = computed(() => {
  const types = new Set();
  settlements.value.forEach(item => {
    const yMatch = selectedYear.value === "全部" || getRecordYear(item) === selectedYear.value;
    const mMatch = selectedMonth.value === "全部" || getRecordMonth(item) === selectedMonth.value;
    if (yMatch && mMatch && item.revenue_type) {
      types.add(item.revenue_type);
    }
  });
  return [...types];
});

// 2. 年份選單選項連動
const availableYears = computed(() => {
  const years = new Set();
  settlements.value.forEach(item => {
    const tMatch = selectedRevenueType.value === "全部" || item.revenue_type === selectedRevenueType.value;
    const mMatch = selectedMonth.value === "全部" || getRecordMonth(item) === selectedMonth.value;
    if (tMatch && mMatch) {
      const y = getRecordYear(item);
      if (y) years.add(y);
    }
  });
  return [...years].sort((a, b) => b - a);
});

// 3. 月份選單選項連動
const availableMonths = computed(() => {
  const months = new Set();
  settlements.value.forEach(item => {
    const tMatch = selectedRevenueType.value === "全部" || item.revenue_type === selectedRevenueType.value;
    const yMatch = selectedYear.value === "全部" || getRecordYear(item) === selectedYear.value;
    if (tMatch && yMatch) {
      const m = getRecordMonth(item);
      if (m) months.add(m);
    }
  });
  return [...months].sort((a, b) => a - b);
});

// 監聽連動，若選中值在可用清單外則重置為 "全部"
watch(availableRevenueTypes, (newTypes) => {
  if (selectedRevenueType.value !== "全部" && !newTypes.includes(selectedRevenueType.value)) {
    selectedRevenueType.value = "全部";
  }
});

watch(availableYears, (newYears) => {
  if (selectedYear.value !== "全部" && !newYears.includes(selectedYear.value)) {
    selectedYear.value = "全部";
  }
});

watch(availableMonths, (newMonths) => {
  if (selectedMonth.value !== "全部" && !newMonths.includes(selectedMonth.value)) {
    selectedMonth.value = "全部";
  }
});

// ── 資料過濾 ──
const filteredSettlements = computed(() => {
  return settlements.value.filter(item => {
    // 營利種類篩選
    if (selectedRevenueType.value !== "全部" && item.revenue_type !== selectedRevenueType.value) {
      return false;
    }
    // 年份篩選
    if (selectedYear.value !== "全部" && getRecordYear(item) !== selectedYear.value) {
      return false;
    }
    // 月份篩選
    if (selectedMonth.value !== "全部" && getRecordMonth(item) !== selectedMonth.value) {
      return false;
    }
    // 關鍵字搜尋 (訂單號)
    if (searchQuery.value.trim() !== "") {
      const query = searchQuery.value.trim().toLowerCase();
      const orderId = (item.settlement_id || "").toLowerCase();
      if (!orderId.includes(query)) {
        return false;
      }
    }
    return true;
  });
});

// ── 營業額統計 ──
const stats = computed(() => {
  let totalGmv = 0;
  let totalPayout = 0;
  let totalFee = 0;
  let ticketGmv = 0;
  let merchGmv = 0;
  let ticketCount = 0;
  let merchCount = 0;

  filteredSettlements.value.forEach(item => {
    const amt = Number(item.total_orders_amount) || 0;
    const payout = Number(item.final_payout_amount) || 0;
    const fee = amt - payout;

    totalGmv += amt;
    totalPayout += payout;
    totalFee += fee;

    if (item.revenue_type === '票務') {
      ticketGmv += amt;
      ticketCount++;
    } else if (item.revenue_type === '商品') {
      merchGmv += amt;
      merchCount++;
    }
  });

  return {
    totalGmv: totalGmv.toFixed(2),
    totalPayout: totalPayout.toFixed(2),
    totalFee: totalFee.toFixed(2),
    orderCount: filteredSettlements.value.length,
    ticketGmv,
    merchGmv,
    ticketCount,
    merchCount
  };
});

// ── 佔比圓環圖 ──
const donutChartData = computed(() => {
  const t = stats.value.ticketGmv;
  const m = stats.value.merchGmv;
  const total = t + m;
  if (total === 0) {
    return { ticketPct: '0.0', merchPct: '0.0', ticketDash: 0, merchDash: 251.3 };
  }
  const tPct = (t / total) * 100;
  const mPct = (m / total) * 100;

  const circ = 251.3;
  const tDash = (t / total) * circ;
  const mDash = circ - tDash;

  return {
    ticketPct: tPct.toFixed(1),
    merchPct: mPct.toFixed(1),
    ticketDash: tDash,
    merchDash: mDash
  };
});

// ── 月度趨勢圖 ──
const monthlyTrendData = computed(() => {
  const groups = {};
  filteredSettlements.value.forEach(item => {
    if (!item.created_at) return;
    const date = new Date(item.created_at);
    const key = `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}`;
    groups[key] = (groups[key] || 0) + (Number(item.final_payout_amount) || 0);
  });

  const sortedKeys = Object.keys(groups).sort();
  if (sortedKeys.length === 0) return [];

  const maxVal = Math.max(...Object.values(groups), 1);
  return sortedKeys.map(key => {
    const val = groups[key];
    const heightPct = (val / maxVal) * 65; // 最大高度 65px
    return {
      label: key.split('-')[1] + '月',
      amount: val.toFixed(2),
      heightPct: heightPct
    };
  });
});

// 匯出報表函數 (套用過濾後的 filteredSettlements)
const exportReport = () => {
  if (filteredSettlements.value.length === 0) return;
  try {
    let csvContent = "\uFEFF"; // UTF-8 BOM
    csvContent += "結算帳單 ID,營利種類,結算對象,當期總營業額,實際撥款金額,撥款狀態,撥款完成時間,訂單建立時間\n";
    
    filteredSettlements.value.forEach(s => {
      const escapeCsv = (val) => {
        if (val === null || val === undefined) return "";
        let str = String(val);
        if (str.includes(",") || str.includes("\"") || str.includes("\n") || str.includes("\r")) {
          return `"${str.replace(/"/g, '""')}"`;
        }
        return str;
      };
      
      const processedAt = s.item_status === "已撥款" ? (s.processed_at || "-") : "-";
      csvContent += `${escapeCsv(s.settlement_id)},${escapeCsv(s.revenue_type)},${escapeCsv(s.organizer_name || s.organizer_id)},${escapeCsv(s.total_orders_amount)},${escapeCsv(s.final_payout_amount)},${escapeCsv(s.item_status)},${escapeCsv(processedAt)},${escapeCsv(s.created_at)}\n`;
    });
    
    const blob = new Blob([csvContent], { type: "text/csv;charset=utf-8;" });
    const url = window.URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    const filename = selectedOrganizer.value === "全部" 
      ? `settlement_report_all.csv` 
      : `settlement_report_${selectedOrganizer.value}.csv`;
    link.setAttribute("download", filename);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  } catch (error) {
    console.error("匯出報表失敗:", error);
  }
};

// 載入當前登入者所屬的組織清單
onMounted(async () => {
  await workspaceStore.fetchMyOrgs();
});

// 過濾重複的組織名稱，取得唯一的組織名稱清單
const uniqueOrgNames = computed(() => {
  if (!myOrgs.value) return [];
  const names = myOrgs.value.map(org => org.name).filter(Boolean);
  return [...new Set(names)];
});

// 監聽狀態、所選主辦方或當前組織ID改變時重新讀取
watch(
  [selectedStatus, selectedOrganizer, currentOrgId],
  () => {
    fetchSettlements();
  },
  { immediate: true }
);

// 訂單表格資料來源 (套用分頁)
const tableData = computed(() => {
  return filteredSettlements.value.slice(0, pageSize.value);
});
</script>

<style scoped>
/* 下拉選單柔和動畫 */
.custom-dropdown {
  border-radius: 8px;
  padding: 0.5rem;
}
.dropdown-item {
  border-radius: 6px;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
  margin-bottom: 2px;
}
.dropdown-item:hover {
  background-color: #f8f9fa;
  color: #0d6efd;
  transform: translateX(4px);
}

/* 狀態徽章視覺化 */
.badge-soft-success {
  background-color: #d1e7dd;
  color: #0f5132;
  font-weight: 600;
  letter-spacing: 0.5px;
  border: 1px solid rgba(15, 81, 50, 0.25);
  box-shadow: 0 2px 4px rgba(15, 81, 50, 0.08);
  font-size: 0.75rem;
}
.badge-soft-danger {
  background-color: #f8d7da;
  color: #842029;
  font-weight: 600;
  letter-spacing: 0.5px;
  border: 1px solid rgba(132, 32, 41, 0.25);
  box-shadow: 0 2px 4px rgba(132, 32, 41, 0.08);
  font-size: 0.75rem;
}
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  box-shadow: 0 0 4px currentColor;
}

/* Feature Gate 鎖定樣式 */
.kyc-gate-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  z-index: 10;
  background-color: rgba(248, 250, 252, 0.85);
  backdrop-filter: blur(4px);
}

.kyc-locked-blur {
  pointer-events: none;
  user-select: none;
  filter: blur(5px);
  opacity: 0.45;
}

.animate-bounce {
  animation: bounce 1s ease-in-out infinite alternate;
}

@keyframes bounce {
  from {
    transform: translateY(0);
  }
  to {
    transform: translateY(-4px);
  }
}

/* 表格欄位文字縮小與防折行 */
.table th {
  font-size: 0.8rem !important;
  white-space: nowrap;
  vertical-align: middle;
  padding: 0.5rem 0.4rem !important;
}
.table td {
  font-size: 0.75rem !important;
  white-space: nowrap;
  vertical-align: middle;
  padding: 0.5rem 0.4rem !important;
}

/* 篩選欄位微調以適應多欄單行 */
.filter-container .btn {
  font-size: 0.75rem !important;
  padding: 0.25rem 0.5rem !important;
  border-radius: 6px;
}
.search-box-wrapper {
  max-width: 150px;
}
.search-input {
  font-size: 0.75rem;
  padding: 0.25rem 0.5rem;
  height: 31px;
  border-radius: 6px;
}

/* 微縮指示器 */
.dot-indicator {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

/* 圓環圖中心樣式 */
.donut-chart-container {
  width: 76px;
  height: 76px;
}
.donut-center-text {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  line-height: 1.1;
}
.legend-dot {
  display: inline-block;
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

/* 動畫與效果 */
.animate-all {
  transition: all 0.25s ease-in-out;
}
.chart-bar {
  transition: height 0.5s ease-in-out, y 0.5s ease-in-out;
}
.chart-bar:hover {
  opacity: 0.95;
  filter: brightness(1.1);
}
.settlement-row {
  transition: background-color 0.15s ease-in-out;
}
.settlement-row:hover {
  background-color: #f8fafc !important;
}
.fs-7 {
  font-size: 0.85rem;
}

/* 營利種類自訂按鈕與項目樣式 - 更加舒適與高級 */
.btn-revenue-ticket {
  color: #2563eb !important;
  border-color: #bfdbfe !important;
  background-color: #f0f6ff !important;
}
.btn-revenue-ticket:hover, .btn-revenue-ticket:focus {
  color: #1d4ed8 !important;
  border-color: #3b82f6 !important;
  background-color: #dbeafe !important;
}

.btn-revenue-merch {
  color: #0d9488 !important;
  border-color: #99f6e4 !important;
  background-color: #f0fdfa !important;
}
.btn-revenue-merch:hover, .btn-revenue-merch:focus {
  color: #0f766e !important;
  border-color: #14b8a6 !important;
  background-color: #ccfbf1 !important;
}

.item-revenue-ticket {
  color: #2563eb !important;
}
.item-revenue-ticket:hover {
  background-color: #f0f6ff !important;
  color: #1d4ed8 !important;
}

.item-revenue-merch {
  color: #0d9488 !important;
}
.item-revenue-merch:hover {
  background-color: #f0fdfa !important;
  color: #0f766e !important;
}

.bg-royal-blue {
  background-color: #2563eb !important;
  box-shadow: 0 0 4px rgba(37, 99, 235, 0.4);
}

.bg-forest-teal {
  background-color: #0d9488 !important;
  box-shadow: 0 0 4px rgba(13, 148, 136, 0.4);
}

/* 表格內的 Badge 樣式，彩度更溫和舒服 */
.badge-revenue-ticket {
  background-color: #eff6ff !important;
  color: #2563eb !important;
  border: 1px solid #bfdbfe !important;
}
.badge-revenue-merch {
  background-color: #f0fdfa !important;
  color: #0d9488 !important;
  border: 1px solid #99f6e4 !important;
}
</style>