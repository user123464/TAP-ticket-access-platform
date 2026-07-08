<script setup>
/**
 * NotificationLogs.vue — 通知發送紀錄（模組 9 ★ P2）
 *
 * 查看通知狀態 PENDING / SENT / FAILED，失敗可觸發重送。
 * API：GET /api/admin/system/notifications、POST .../{id}/resend
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import CountBadge from "@/components/common/CountBadge.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import NotificationTabs from "@/components/system/NotificationTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:system:notifications",
  () => api.get("/api/admin/system/notifications").then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && rows.value.length === 0);
const filterStatus = ref("");

const STATUS_META = {
  PENDING: { label: "待發送", variant: "warning" },
  SENT: { label: "已送達", variant: "success" },
  FAILED: { label: "未送達", variant: "danger" },
};

const fetchLogs = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入發送紀錄失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchLogs);

const keyword = ref("");
const filterChannel = ref("");
const fromDate = ref("");
const toDate = ref("");

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((l) => {
      if (kw && 
          !String(l.recipient ?? "").toLowerCase().includes(kw) && 
          !String(l.error ?? "").toLowerCase().includes(kw) && 
          !String(l.templateCode ?? "").toLowerCase().includes(kw)
      ) return false;
      if (filterChannel.value && l.channel !== filterChannel.value) return false;
      if (filterStatus.value && l.status !== filterStatus.value) return false;
      
      // 日期篩選
      if (fromDate.value && l.createdAt && dayjs(l.createdAt).isBefore(dayjs(fromDate.value).startOf('day'))) return false;
      if (toDate.value && l.createdAt && dayjs(l.createdAt).isAfter(dayjs(toDate.value).endOf('day'))) return false;
      
      return true;
    })
    .sort((a, b) => dayjs(b.createdAt).valueOf() - dayjs(a.createdAt).valueOf());
});
const failedCount = computed(() => rows.value.filter((l) => l.status === "FAILED").length);

const clearFilters = () => {
  filterChannel.value = "";
  fromDate.value = "";
  toDate.value = "";
};

// --- 重送 ---
const resendingId = ref(null);

const handleResend = async (row) => {
  resendingId.value = row.id;
  try {
    await api.post(`/api/admin/system/notifications/${row.id}/resend`);
    row.status = "PENDING";
    row.error = null;
    toast.success("已排入重送");
  } catch (error) {
    if (error.response) {
      toast.error(error.response.data?.message ?? "重送失敗");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    resendingId.value = null;
  }
};

const columns = [
  { key: "createdAt", label: "時間", sortable: true, width: "160px" },
  { key: "templateCode", label: "模板", width: "180px", sortable: true },
  { key: "channel", label: "管道", width: "80px", sortable: true },
  { key: "recipient", label: "收件對象", width: "180px", sortable: true },
  { key: "status", label: "狀態", width: "110px", sortable: true },
  { key: "error", label: "錯誤訊息", sortable: true },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");

// 匯出：對應目前篩選後的清單，時間/狀態轉成可讀文字
const EXPORT_COLUMNS = {
  createdAt: "時間",
  templateCode: "模板",
  channel: "管道",
  recipient: "收件對象",
  statusText: "狀態",
  error: "錯誤訊息",
};
const exportRows = computed(() =>
  filteredRows.value.map((l) => ({
    createdAt: formatTime(l.createdAt),
    templateCode: l.templateCode,
    channel: l.channel,
    recipient: l.recipient,
    statusText: STATUS_META[l.status]?.label ?? l.status,
    error: l.error || "",
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-megaphone me-2" style="color: var(--tap-primary)"></i>通知與公告</h4>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <NotificationTabs />
      <ExportButtons file-name="通知發送紀錄" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="通知發送紀錄" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      v-model:fromDate="fromDate"
      v-model:toDate="toDate"
      showDateRange
      date-label="發送時間"
      keywordPlaceholder="搜尋收件對象、模板代碼或錯誤訊息..."
      :hasExtraFilters="filterChannel !== '' || fromDate !== '' || toDate !== ''"
      @clear="clearFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterChannel" class="form-select form-select-sm">
          <option value="">全部管道</option>
          <option value="EMAIL">Email</option>
          <option value="SMS">SMS</option>
        </select>
      </div>
    </SearchBar>

    <!-- 狀態篩選（二級頁籤）：上方淡分隔線建立父子層級 -->
    <div class="tab-container-nested mb-3">
      <ul class="nav nav-pills gap-3 mb-0">
        <li class="nav-item"><button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === '' }" @click="filterStatus = ''">全部</button></li>
        <li class="nav-item"><button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === 'PENDING' }" @click="filterStatus = 'PENDING'">待發送</button></li>
        <li class="nav-item"><button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === 'SENT' }" @click="filterStatus = 'SENT'">已送達</button></li>
        <li class="nav-item">
          <CountBadge :count="failedCount">
            <button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === 'FAILED' }" @click="filterStatus = 'FAILED'">未送達</button>
          </CountBadge>
        </li>
      </ul>
    </div>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" :page-size="25" emptyText="沒有符合條件的發送紀錄" actions-width="60px">
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-templateCode="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-recipient="{ value }"><span class="small">{{ value }}</span></template>
      <template #cell-status="{ value }">
        <StatusBadge :variant="STATUS_META[value]?.variant" :label="STATUS_META[value]?.label ?? value" />
      </template>
      <template #cell-error="{ value }"><span class="small text-danger">{{ value || "—" }}</span></template>

      <template #actions="{ row }">
        <button
          v-if="row.status === 'FAILED'"
          type="button"
          class="btn btn-sm btn-icon btn-outline-primary"
          :disabled="resendingId === row.id"
          @click="handleResend(row)"
          title="重送"
        >
          <span v-if="resendingId === row.id" class="spinner-border spinner-border-sm"></span>
          <i v-else class="bi bi-arrow-clockwise"></i>
        </button>
        <span v-else class="small text-tap-secondary">—</span>
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
:deep(.table td .d-flex) {
  gap: 12px !important;
}
</style>
