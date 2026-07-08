<script setup>
/**
 * FeeSettlement.vue — 廠商手續費明細 + 匯出（模組 6 ★ P1）
 *
 * 依 Contracts.fee_type + fee_value 計算平台應收；可選時間範圍，
 * CSV / Excel 匯出（useExport / xlsx），供月結對帳。
 *
 * API：GET /api/admin/finance/settlement?from=&to=
 */
import { ref, computed, watch, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import FinanceTabs from "@/components/finance/FinanceTabs.vue";
import { formatFee } from "@/constants/contract.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

// 時間範圍：預設本月
const fromDate = ref(dayjs().startOf("month").format("YYYY-MM-DD"));
const toDate = ref(dayjs().endOf("month").format("YYYY-MM-DD"));
const keyword = ref("");

// 交易總額篩選範圍
const minGmv = ref("");
const maxGmv = ref("");

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:finance:settlement",
  () => api.get("/api/admin/finance/settlement", {
    params: { from: fromDate.value, to: toDate.value },
  }).then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

const fetchSettlement = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入手續費明細失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchSettlement);

// 監聽日期變動自動查詢
watch([fromDate, toDate], fetchSettlement);

const resetGmvRange = () => {
  minGmv.value = "";
  maxGmv.value = "";
};

// 前端篩選 (組織名稱、組織編號、交易總額範圍)
const filteredRows = computed(() => {
  let result = rows.value;
  
  const kw = keyword.value.trim().toLowerCase();
  if (kw) {
    result = result.filter((r) => 
      (r.orgName && r.orgName.toLowerCase().includes(kw)) ||
      (r.orgId && r.orgId.toLowerCase().includes(kw))
    );
  }
  
  result = result.filter((r) => {
    const val = r.gmv ?? 0;
    const min = (minGmv.value === "" || minGmv.value === null || minGmv.value === undefined) ? 0 : Number(minGmv.value);
    const max = (maxGmv.value === "" || maxGmv.value === null || maxGmv.value === undefined) ? Infinity : Number(maxGmv.value);
    return val >= min && val <= max;
  });
  
  return result;
});

const totalFee = computed(() => filteredRows.value.reduce((sum, r) => sum + (r.fee ?? 0), 0));
const totalGmv = computed(() => filteredRows.value.reduce((sum, r) => sum + (r.gmv ?? 0), 0));

const columns = [
  { key: "orgId", label: "組織編號", sortable: true, width: "130px" },
  { key: "orgName", label: "廠商名稱", sortable: true, width: "220px" },
  { key: "orderCount", label: "交易筆數", sortable: true, width: "110px" },
  { key: "gmv", label: "交易總額", sortable: true, width: "150px" },
  // 費率以 feeValue 數值排序（同費率型態間可比較），顯示仍用 formatFee 組出完整文字
  { key: "feeValue", label: "費率", sortable: true, width: "120px" },
  { key: "fee", label: "平台應收手續費", sortable: true, width: "160px" },
];

// 匯出欄位對照（key → 中文標題）
const EXPORT_COLUMNS = {
  orgId: "組織編號",
  orgName: "廠商名稱",
  orderCount: "交易筆數",
  gmv: "交易總額",
  feeRateText: "費率",
  fee: "平台應收手續費",
};

// 已整理成可匯出格式的資料列（對應目前搜尋篩選後的清單，費率轉成完整文字欄位）
const exportRows = computed(() =>
  filteredRows.value.map((r) => ({ ...r, feeRateText: formatFee(r.feeType, r.feeValue) }))
);

const fileName = computed(() => `手續費明細_${fromDate.value}_${toDate.value}`);

const formatCurrency = (n) => `NT$ ${Number(n ?? 0).toLocaleString("zh-Hant-TW")}`;

const formatCurrencyShort = (v) => {
  const val = Number(v ?? 0);
  if (val >= 1000000) return `${(val / 1000000).toFixed(1)}M`;
  if (val >= 1000) return `${(val / 1000).toFixed(0)}K`;
  return `${val}`;
};
</script>

<template>
  <div>
    <!-- 標題列 -->
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-cash-stack me-2" style="color: var(--tap-primary)"></i>財務管理</h4>
    </div>

    <!-- 頁籤列：匯出功能與「費用結算」標籤同列、右側對齊 -->
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-3">
      <FinanceTabs />
      <ExportButtons :file-name="fileName" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="手續費明細" :disabled="loading" />
    </div>

    <!-- 搜尋與篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      v-model:fromDate="fromDate"
      v-model:toDate="toDate"
      keywordPlaceholder="搜尋廠商名稱、組織編號..."
      showDateRange
      date-label="交易期間"
      :hasExtraFilters="(minGmv !== '' && minGmv !== null && minGmv !== undefined) || (maxGmv !== '' && maxGmv !== null && maxGmv !== undefined)"
      @clear="resetGmvRange"
    >
      <!-- 交易額區間輸入框 -->
      <div class="col-12 col-md-auto">
        <div class="input-group input-group-sm">
          <span class="input-group-text bg-transparent text-tap-secondary">
            <i class="bi bi-cash-coin me-1"></i>交易額
          </span>
          <input 
            type="number" 
            class="form-control text-center" 
            style="width: 80px;" 
            v-model.number="minGmv" 
            placeholder="Min"
            min="0"
          />
          <span class="input-group-text bg-transparent text-tap-secondary border-start-0 border-end-0 px-2">至</span>
          <input 
            type="number" 
            class="form-control text-center" 
            style="width: 80px;" 
            v-model.number="maxGmv" 
            placeholder="Max"
            min="0"
          />
        </div>
      </div>
    </SearchBar>

    <!-- 合計 -->
    <div class="row g-3 mb-3">
      <div class="col-6">
        <div class="card border shadow-sm rounded-4">
          <div class="card-body py-3 d-flex align-items-center justify-content-between">
            <span class="small text-tap-secondary">期間交易總額</span>
            <span class="fw-bold fs-5">{{ formatCurrency(totalGmv) }}</span>
          </div>
        </div>
      </div>
      <div class="col-6">
        <div class="card border shadow-sm rounded-4">
          <div class="card-body py-3 d-flex align-items-center justify-content-between">
            <span class="small text-tap-secondary">平台應收手續費合計</span>
            <span class="fw-bold fs-5 text-success">{{ formatCurrency(totalFee) }}</span>
          </div>
        </div>
      </div>
    </div>

    <!-- 明細 -->
    <DataTable :columns="columns" :rows="filteredRows" row-key="orgId" :loading="loading" emptyText="此期間沒有交易紀錄" actions-width="60px">
      <template #cell-orgName="{ value }">
        <div class="text-truncate min-w-0" style="max-width: 220px" :title="value">{{ value }}</div>
      </template>
      <template #cell-orderCount="{ value }">{{ Number(value).toLocaleString() }}</template>
      <template #cell-gmv="{ value }">{{ formatCurrency(value) }}</template>
      <template #cell-feeValue="{ row }"><span class="small">{{ formatFee(row.feeType, row.feeValue) }}</span></template>
      <template #cell-fee="{ value }"><span class="fw-semibold text-success">{{ formatCurrency(value) }}</span></template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/organizers/${row.orgId}`" class="btn btn-sm btn-icon btn-outline-primary" title="查看組織">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
/* 雙向拉桿樣式（與 /admin/venues 場地列表一致） */
.dual-range-container input[type="range"]::-webkit-slider-runnable-track {
  background: transparent;
  border: none;
}
.dual-range-container input[type="range"]::-moz-range-track {
  background: transparent;
  border: none;
}

/* Chrome/Safari/Opera/Edge */
.dual-range-container input[type="range"]::-webkit-slider-thumb {
  pointer-events: auto;
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--tap-primary);
  border: 1.5px solid #fff;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  transition: transform 0.1s;
}
.dual-range-container input[type="range"]::-webkit-slider-thumb:active {
  transform: scale(1.3);
}

/* Firefox */
.dual-range-container input[type="range"]::-moz-range-thumb {
  pointer-events: auto;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--tap-primary);
  border: 1.5px solid #fff;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  transition: transform 0.1s;
}
.dual-range-container input[type="range"]::-moz-range-thumb:active {
  transform: scale(1.3);
}

@media (min-width: 768px) {
  :deep(.card-body > .row.align-items-center) {
    flex-wrap: nowrap !important;
  }
}
</style>
