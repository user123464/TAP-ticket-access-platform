<script setup>
/**
 * ContractList.vue — 合約列表（模組 5 ★ P1）
 *
 * 篩選 contract_type（公版/客製）與 contract_status。
 * 入口：編輯公版範本（/admin/billing/contracts/template）、建立客製合約（/admin/billing/contracts/new）。
 * API：GET /api/admin/contracts
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import { CONTRACT_TYPE_META, CONTRACT_STATUS_META, formatFee } from "@/constants/contract.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";
import BillingTabs from "@/components/billing/BillingTabs.vue";

const { setAnnouncement } = useSystemBanner();

// SWR 快取：建立時立刻帶回上次列表（不閃白），背景再 refresh 取最新
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:contracts",
  () => api.get("/api/admin/contracts").then((r) => r.data.data ?? []),
  { initial: [] }
);
const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

const keyword = ref(""); // 組織名稱
const filterType = ref("");
const filterStatus = ref("");
const fromDate = ref("");
const toDate = ref("");

const fetchContracts = async () => {
  try {
    await refresh();
  } catch (error) {
    // 後端回錯(4xx/5xx)：仍走原本錯誤提示，不吞錯；保留快取舊資料避免閃白
    if (error.response) {
      setAnnouncement("載入合約清單失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  }
};

onMounted(fetchContracts);

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((c) => {
      if (kw && !c.orgName.toLowerCase().includes(kw)) return false;
      if (filterType.value !== "" && c.contractType !== Number(filterType.value)) return false;
      if (filterStatus.value !== "" && c.contractStatus !== Number(filterStatus.value)) return false;
      
      // 日期過濾 (依據 signedAt)
      if (fromDate.value && (!c.signedAt || dayjs(c.signedAt).isBefore(dayjs(fromDate.value).startOf('day')))) return false;
      if (toDate.value && (!c.signedAt || dayjs(c.signedAt).isAfter(dayjs(toDate.value).endOf('day')))) return false;
      
      return true;
    })
    .map((c) => ({
      ...c,
      fee: c.feeValue, // mapping the fee column key to feeValue numeric value for custom client-side sorting in DataTable
    }))
    .sort((a, b) => b.id.localeCompare(a.id));
});

const hasExtraFilters = computed(() => filterType.value !== "" || filterStatus.value !== "" || fromDate.value !== "" || toDate.value !== "");
const clearFilters = () => {
  filterType.value = "";
  filterStatus.value = "";
  fromDate.value = "";
  toDate.value = "";
};

const columns = [
  { key: "id", label: "編號", width: "140px", sortable: true },
  { key: "orgName", label: "簽約組織", sortable: true },
  { key: "contractType", label: "類型", width: "100px", sortable: true },
  { key: "fee", label: "費率", width: "80px", sortable: true },
  { key: "version", label: "版本", width: "80px", sortable: true },
  { key: "signedAt", label: "簽署日", sortable: true, width: "100px" },
  { key: "expiresAt", label: "到期日", sortable: true, width: "100px" },
  { key: "contractStatus", label: "狀態", width: "100px", sortable: true },
];

const formatDate = (t) => (t ? dayjs(t).format("YYYY-MM-DD") : "—");

// 匯出：對應目前篩選後的清單，類型/費率/狀態轉成可讀文字
const EXPORT_COLUMNS = {
  id: "編號",
  orgName: "簽約組織",
  contractTypeText: "類型",
  feeText: "費率",
  version: "版本",
  signedAt: "簽署日",
  expiresAt: "到期日",
  contractStatusText: "狀態",
};
const exportRows = computed(() =>
  filteredRows.value.map((c) => ({
    id: c.id,
    orgName: c.orgName,
    contractTypeText: CONTRACT_TYPE_META[c.contractType]?.label ?? "未知",
    feeText: formatFee(c.feeType, c.feeValue),
    version: c.version,
    signedAt: formatDate(c.signedAt),
    expiresAt: formatDate(c.expiresAt),
    contractStatusText: CONTRACT_STATUS_META[c.contractStatus]?.label ?? "未知",
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-box-seam me-2" style="color: var(--tap-primary)"></i>訂閱與合約</h4>
      <div class="d-flex gap-2">
        <RouterLink to="/admin/billing/contracts/template" class="btn btn-sm btn-outline-primary">
          <i class="bi bi-pencil-square me-1"></i>編輯公版範本
        </RouterLink>
        <RouterLink to="/admin/billing/contracts/new" class="btn btn-sm btn-primary">
          <i class="bi bi-plus-lg me-1"></i>建立客製合約
        </RouterLink>
      </div>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <BillingTabs />
      <ExportButtons file-name="合約清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="合約清單" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      v-model:fromDate="fromDate"
      v-model:toDate="toDate"
      keywordPlaceholder="搜尋簽約組織..."
      showDateRange
      date-label="簽署日"
      :hasExtraFilters="hasExtraFilters"
      @clear="clearFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterType" class="form-select form-select-sm">
          <option value="">全部類型</option>
          <option value="1">公版合約</option>
          <option value="2">客製合約</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm">
          <option value="">全部狀態</option>
          <option value="0">草稿</option>
          <option value="1">生效中</option>
          <option value="2">已到期</option>
          <option value="3">已終止</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="沒有符合條件的合約" actions-width="60px">
      <template #cell-orgName="{ value }">
        <div class="text-truncate min-w-0" style="max-width: 200px" :title="value">{{ value }}</div>
      </template>
      <template #cell-contractType="{ value }">
        <StatusBadge :dot="false" :variant="CONTRACT_TYPE_META[value]?.variant" :label="CONTRACT_TYPE_META[value]?.label ?? '未知'" />
      </template>
      <template #cell-fee="{ row }"><span class="small">{{ formatFee(row.feeType, row.feeValue) }}</span></template>
      <template #cell-signedAt="{ value }"><span class="small">{{ formatDate(value) }}</span></template>
      <template #cell-expiresAt="{ value }"><span class="small">{{ formatDate(value) }}</span></template>
      <template #cell-contractStatus="{ value }">
        <StatusBadge :variant="CONTRACT_STATUS_META[value]?.variant" :label="CONTRACT_STATUS_META[value]?.label ?? '未知'" />
      </template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/billing/contracts/${row.id}`" class="btn btn-sm btn-icon btn-outline-primary" title="查看合約">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
:deep(.table th),
:deep(.table td) {
  padding-left: 8px !important;
  padding-right: 8px !important;
}
:deep(.table th:first-child),
:deep(.table td:first-child) {
  padding-left: 16px !important;
}
:deep(.table th:last-child),
:deep(.table td:last-child) {
  padding-right: 16px !important;
}
</style>
