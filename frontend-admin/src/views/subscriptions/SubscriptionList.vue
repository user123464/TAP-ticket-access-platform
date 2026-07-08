<script setup>
/**
 * SubscriptionList.vue — 各組織訂閱狀態總覽（模組「訂閱方案」★ P2）
 *
 * 全平台組織的訂閱方案、起訖日、續約狀態列表，供營運掌握 SaaS 收入狀況。
 * API：GET /api/admin/subscription/organizations
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import BillingTabs from "@/components/billing/BillingTabs.vue";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

// SWR 快取：建立時立刻帶回上次列表（不閃白），背景再 refresh 取最新
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:subscription-orgs",
  () => api.get("/api/admin/subscription/organizations").then((r) => r.data.data ?? []),
  { initial: [] }
);
const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

const keyword = ref("");
const filterStatus = ref("");
const filterPlan = ref("");
const fromDate = ref("");
const toDate = ref("");

// 訂閱狀態（依到期日衍生）
const SUB_STATUS_META = {
  ACTIVE: { label: "訂閱中", variant: "success" },
  EXPIRING: { label: "即將到期", variant: "warning" },
  EXPIRED: { label: "已到期", variant: "danger" },
  FREE: { label: "免費版", variant: "secondary" },
};

const fetchSubs = async () => {
  try {
    await refresh();
  } catch (error) {
    // 後端回錯(4xx/5xx)：仍走原本錯誤提示，不吞錯；保留快取舊資料避免閃白
    if (error.response) {
      setAnnouncement("載入訂閱總覽失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  }
};

onMounted(fetchSubs);

// 動態從資料中提取方案選項
const planOptions = computed(() => {
  const set = new Set();
  rows.value.forEach((s) => {
    if (s.planName && s.planName !== "—") {
      set.add(s.planName);
    }
  });
  return [...set].sort();
});

const clearFilters = () => {
  filterStatus.value = "";
  filterPlan.value = "";
  fromDate.value = "";
  toDate.value = "";
};

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value.filter((s) => {
    if (kw && !s.orgName.toLowerCase().includes(kw) && !(s.orgId ?? "").toLowerCase().includes(kw)) return false;
    if (filterStatus.value && s.status !== filterStatus.value) return false;
    
    // 方案篩選
    if (filterPlan.value && s.planName !== filterPlan.value) return false;
    
    // 日期篩選
    if (fromDate.value && s.startedAt && dayjs(s.startedAt).isBefore(dayjs(fromDate.value).startOf('day'))) return false;
    if (toDate.value && s.expiresAt && dayjs(s.expiresAt).isAfter(dayjs(toDate.value).endOf('day'))) return false;
    
    return true;
  });
});

const columns = [
  { key: "orgId", label: "組織編號", sortable: true, width: "150px" },
  { key: "orgName", label: "組織名稱", sortable: true },
  { key: "planName", label: "方案", sortable: true, width: "150px" },
  { key: "startedAt", label: "訂閱期間", sortable: true, width: "220px" },
  { key: "status", label: "狀態", sortable: true, width: "100px" },
];

const formatDate = (t) => (t ? dayjs(t).format("YYYY-MM-DD") : "—");

// 匯出：對應目前篩選後的清單，期間/狀態組成可讀文字
const EXPORT_COLUMNS = {
  orgId: "組織編號",
  orgName: "組織名稱",
  planName: "方案",
  period: "訂閱期間",
  statusText: "狀態",
};
const exportRows = computed(() =>
  filteredRows.value.map((s) => ({
    orgId: s.orgId,
    orgName: s.orgName,
    planName: s.planName,
    period: `${formatDate(s.startedAt)} ～ ${s.expiresAt ? formatDate(s.expiresAt) : "無期限"}`,
    statusText: SUB_STATUS_META[s.status]?.label ?? s.status,
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-box-seam me-2" style="color: var(--tap-primary)"></i>訂閱與合約</h4>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <BillingTabs />
      <ExportButtons file-name="訂閱總覽" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="訂閱總覽" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar 
      v-model:keyword="keyword" 
      v-model:fromDate="fromDate"
      v-model:toDate="toDate"
      showDateRange
      date-label="訂閱期間"
      keywordPlaceholder="搜尋組織名稱 / 編號"
      :hasExtraFilters="filterStatus !== '' || filterPlan !== '' || fromDate !== '' || toDate !== ''"
      @clear="clearFilters"
    >
      <!-- 方案篩選 -->
      <div class="col-6 col-md-auto">
        <select v-model="filterPlan" class="form-select form-select-sm" title="方案篩選">
          <option value="">全部方案</option>
          <option v-for="plan in planOptions" :key="plan" :value="plan">{{ plan }}</option>
        </select>
      </div>

      <!-- 狀態篩選 -->
      <div class="col-6 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm" title="狀態篩選">
          <option value="">全部狀態</option>
          <option value="ACTIVE">訂閱中</option>
          <option value="EXPIRING">即將到期</option>
          <option value="EXPIRED">已到期</option>
          <option value="FREE">免費版</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" row-key="orgId" :loading="loading" emptyText="沒有符合條件的訂閱" actions-width="80px">
      <template #cell-startedAt="{ row }">
        <span class="small">{{ formatDate(row.startedAt) }} ～ {{ row.expiresAt ? formatDate(row.expiresAt) : "無期限" }}</span>
      </template>

      <template #cell-status="{ value }">
        <StatusBadge :variant="SUB_STATUS_META[value]?.variant" :label="SUB_STATUS_META[value]?.label ?? value" />
      </template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/organizers/${row.orgId}`" class="btn btn-sm btn-icon btn-outline-primary" title="查看組織">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>
