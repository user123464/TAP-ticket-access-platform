<script setup>
/**
 * ScheduledJobs.vue — 排程任務監控（模組 9 ★ P2）
 *
 * 查看排程狀態、上次執行、下次排程、錯誤訊息；可手動觸發執行。
 * API：GET /api/admin/system/jobs、POST .../{id}/trigger
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import MaintenanceTabs from "@/components/system/MaintenanceTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:system:jobs",
  () => api.get("/api/admin/system/jobs").then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && rows.value.length === 0);

const keyword = ref("");
const filterStatus = ref("");
const filterEnabled = ref("");

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((r) => {
      if (kw && !String(r.name ?? "").toLowerCase().includes(kw)) return false;
      if (filterStatus.value && r.lastStatus !== filterStatus.value) return false;
      if (filterEnabled.value !== "") {
        const isEnabled = filterEnabled.value === "true";
        if (r.enabled !== isEnabled) return false;
      }
      return true;
    })
    .sort((a, b) => String(a.name ?? "").localeCompare(String(b.name ?? ""), "zh-Hant"));
});

const clearFilters = () => {
  filterStatus.value = "";
  filterEnabled.value = "";
};

const STATUS_META = {
  IDLE: { label: "閒置", variant: "secondary" },
  RUNNING: { label: "執行中", variant: "info" },
  SUCCESS: { label: "成功", variant: "success" },
  FAILED: { label: "失敗", variant: "danger" },
};

const fetchJobs = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入排程任務失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchJobs);

// --- 手動觸發 ---
const triggeringId = ref(null);

const handleTrigger = async (row) => {
  triggeringId.value = row.id;
  try {
    await api.post(`/api/admin/system/jobs/${row.id}/trigger`);
    await fetchJobs();
    toast.success("已觸發排程");
  } catch (error) {
    if (error.response) {
      toast.error(error.response.data?.message ?? "觸發失敗");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    triggeringId.value = null;
  }
};

const columns = [
  { key: "name", label: "任務名稱", sortable: true },
  { key: "cron", label: "排程 (cron)", width: "180px", sortable: true },
  { key: "lastRunAt", label: "上次執行", width: "140px", sortable: true },
  { key: "nextRunAt", label: "下次排程", width: "140px", sortable: true },
  { key: "lastStatus", label: "上次結果", width: "100px", sortable: true },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-tools me-2" style="color: var(--tap-primary)"></i>系統維護</h4>
    </div>

    <MaintenanceTabs />

    <SearchBar
      v-model:keyword="keyword"
      keywordPlaceholder="搜尋任務名稱..."
      :hasExtraFilters="filterStatus !== '' || filterEnabled !== ''"
      @clear="clearFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm">
          <option value="">全部執行結果</option>
          <option value="IDLE">閒置 (IDLE)</option>
          <option value="RUNNING">執行中 (RUNNING)</option>
          <option value="SUCCESS">成功 (SUCCESS)</option>
          <option value="FAILED">失敗 (FAILED)</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="filterEnabled" class="form-select form-select-sm">
          <option value="">全部狀態</option>
          <option value="true">已啟用</option>
          <option value="false">已停用</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="尚無排程任務" actions-width="70px">
      <template #cell-name="{ row }">
        <span class="small">{{ row.name }}</span>
        <i v-if="!row.enabled" class="bi bi-pause-circle text-tap-secondary ms-2" title="已停用"></i>
      </template>
      <template #cell-cron="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-lastRunAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-nextRunAt="{ row }">
        <span class="small">{{ row.enabled ? formatTime(row.nextRunAt) : "—" }}</span>
      </template>
      <template #cell-lastStatus="{ row }">
        <StatusBadge :variant="STATUS_META[row.lastStatus]?.variant" :label="STATUS_META[row.lastStatus]?.label ?? row.lastStatus" />
        <i v-if="row.error" class="bi bi-exclamation-triangle-fill text-danger ms-1" :title="row.error"></i>
      </template>

      <template #actions="{ row }">
        <button type="button" class="btn btn-sm btn-icon btn-outline-primary" :disabled="triggeringId === row.id || row.lastStatus === 'RUNNING'" title="立即執行" @click="handleTrigger(row)">
          <span v-if="triggeringId === row.id" class="spinner-border spinner-border-sm"></span>
          <i v-else class="bi bi-play-fill"></i>
        </button>
      </template>
    </DataTable>

    <!-- 失敗任務的錯誤明細 -->
    <div v-if="rows.some((j) => j.error)" class="mt-3">
      <div class="fw-bold mb-2 small"><i class="bi bi-exclamation-triangle-fill text-danger me-1"></i>近期錯誤</div>
      <div v-for="job in rows.filter((j) => j.error)" :key="job.id" class="alert alert-danger py-2 small mb-2">
        <strong>{{ job.name }}</strong>（{{ formatTime(job.lastRunAt) }}）：{{ job.error }}
      </div>
    </div>
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
