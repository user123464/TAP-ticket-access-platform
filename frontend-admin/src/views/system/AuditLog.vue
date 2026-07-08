<script setup>
/**
 * AuditLog.vue — 稽核日誌（模組 9 ★ P2）
 *
 * 搜尋（操作者/目標）+ 篩選（動作類型）+ 時間範圍查詢。供安全稽核與問題追溯。
 * API：GET /api/admin/system/audit-logs?from=&to=&action=
 */
import { ref, computed, watch, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

const defaultFromDate = dayjs().subtract(7, "day").format("YYYY-MM-DD");
const defaultToDate = dayjs().format("YYYY-MM-DD");

const keyword = ref("");
const filterAction = ref("");
const fromDate = ref(defaultFromDate);
const toDate = ref(defaultToDate);

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:system:audit-logs",
  () => api.get("/api/admin/system/audit-logs", {
    params: { from: fromDate.value, to: toDate.value, action: filterAction.value || undefined },
  }).then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && rows.value.length === 0);

// 動作類型顯示對照
const ACTION_META = {
  AUTH: { label: "身份認證", variant: "secondary" },
  IAM: { label: "存取控制", variant: "warning" },
  ORGANIZER: { label: "商戶組織", variant: "primary" },
  FINANCIAL: { label: "財務結算", variant: "success" },
  SYSTEM: { label: "系統維護", variant: "info" },
  CONTENT: { label: "內容審核", variant: "danger" },
};

const fetchLogs = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入稽核日誌失敗，請稍後再試。", "danger");
    }
  }
};

watch([fromDate, toDate, filterAction], fetchLogs);

onMounted(fetchLogs);

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((l) => {
      if (kw && !String(l.operator ?? "").toLowerCase().includes(kw) && !String(l.target ?? "").toLowerCase().includes(kw)) return false;
      if (filterAction.value && l.action !== filterAction.value) return false;
      return true;
    })
    .sort((a, b) => dayjs(b.createdAt).valueOf() - dayjs(a.createdAt).valueOf());
});

const hasExtraFilters = computed(() => {
  return filterAction.value !== "" || fromDate.value !== defaultFromDate || toDate.value !== defaultToDate;
});

const clearFilters = () => {
  filterAction.value = "";
  fromDate.value = defaultFromDate;
  toDate.value = defaultToDate;
};

const columns = [
  { key: "createdAt", label: "時間", sortable: true, width: "160px" },
  { key: "operator", label: "操作者", width: "100px", sortable: true },
  { key: "action", label: "動作", width: "100px", sortable: true },
  { key: "target", label: "目標", sortable: true },
  { key: "detail", label: "說明", width: "200px" },
  { key: "ip", label: "IP", width: "100px", sortable: true },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");

// 匯出：對應目前篩選後的清單，時間/動作轉成可讀文字
const EXPORT_COLUMNS = {
  createdAt: "時間",
  operator: "操作者",
  actionText: "動作",
  target: "目標",
  detail: "說明",
  ip: "IP",
};
const exportRows = computed(() =>
  filteredRows.value.map((l) => ({
    createdAt: formatTime(l.createdAt),
    operator: l.operator,
    actionText: ACTION_META[l.action]?.label ?? l.action,
    target: l.target,
    detail: l.detail,
    ip: l.ip,
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-clipboard-check me-2" style="color: var(--tap-primary)"></i>稽核日誌</h4>
      <ExportButtons file-name="稽核日誌" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="稽核日誌" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      keywordPlaceholder="搜尋操作者或目標..."
      :showDateRange="true"
      dateLabel="時間"
      v-model:fromDate="fromDate"
      v-model:toDate="toDate"
      :hasExtraFilters="hasExtraFilters"
      @clear="clearFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterAction" class="form-select form-select-sm">
          <option value="">全部動作</option>
          <option value="AUTH">身份認證</option>
          <option value="IAM">存取控制</option>
          <option value="ORGANIZER">商戶組織</option>
          <option value="FINANCIAL">財務結算</option>
          <option value="SYSTEM">系統維護</option>
          <option value="CONTENT">內容審核</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" :page-size="25" emptyText="此區間沒有稽核紀錄">
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-operator="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-action="{ value }">
        <StatusBadge :dot="false" :variant="ACTION_META[value]?.variant ?? 'secondary'" :label="ACTION_META[value]?.label ?? value" />
      </template>
      <template #cell-detail="{ value }"><span class="small">{{ value }}</span></template>
      <template #cell-ip="{ value }"><span class="small text-tap-secondary">{{ value }}</span></template>
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
:deep(.table td .d-flex) {
  gap: 12px !important;
}
</style>
