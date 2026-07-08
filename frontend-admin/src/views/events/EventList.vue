<script setup>
/**
 * EventList.vue — 全平台活動查詢列表（模組 7 ★ P2，唯讀）
 *
 * 設計原則：SaaS 多租戶營運權限下放 B2B 端，Admin 不修改特定活動（避免資料污染與越權），
 * 此頁僅供全平台搜尋/篩選偵錯用。
 *
 * API：GET /api/admin/events
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import OperationsTabs from "@/components/operations/OperationsTabs.vue";
import { EVENT_STATUS_META } from "@/constants/event.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

// SWR 快取：建立時立刻帶回上次列表（不閃白），背景再 refresh 取最新
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:events",
  () => api.get("/api/admin/events").then((r) => r.data.data ?? []),
  { initial: [] }
);
const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

const keyword = ref(""); // 活動名稱 / 主辦組織
const filterStatus = ref("");
const fromDate = ref("");
const toDate = ref("");

const fetchEvents = async () => {
  try {
    await refresh();
  } catch (error) {
    // 後端回錯(4xx/5xx)：仍走原本錯誤提示，不吞錯；保留快取舊資料避免閃白
    if (error.response) {
      setAnnouncement("載入活動清單失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  }
};

onMounted(fetchEvents);

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((e) => {
      if (kw && !e.title.toLowerCase().includes(kw) && !e.orgName.toLowerCase().includes(kw)) return false;
      if (filterStatus.value !== "" && e.status !== Number(filterStatus.value)) return false;
      if (fromDate.value && dayjs(e.startAt).isBefore(dayjs(fromDate.value).startOf('day'))) return false;
      if (toDate.value && dayjs(e.startAt).isAfter(dayjs(toDate.value).endOf('day'))) return false;
      return true;
    })
    // 補上 ticketPct 數值欄，讓「售票進度」可排序
    .map((e) => ({ ...e, ticketPct: ticketPct(e) }));
});

const columns = [
  { key: "id", label: "活動編號", sortable: true, width: "100px" },
  { key: "title", label: "活動名稱", sortable: true },
  { key: "orgName", label: "主辦組織", sortable: true, width: "200px" },
  { key: "ticketPct", label: "售票進度", sortable: true, width: "100px" },
  { key: "startAt", label: "活動時間", sortable: true, width: "150px" },
  { key: "status", label: "狀態", sortable: true, width: "100px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
const ticketPct = (row) => (row.ticketTotal ? Math.round((row.ticketSold / row.ticketTotal) * 100) : 0);

// 匯出：對應目前篩選後的清單，售票進度/時間/狀態轉成可讀文字
const EXPORT_COLUMNS = {
  id: "活動編號",
  title: "活動名稱",
  orgName: "主辦組織",
  ticket: "售票進度",
  startAt: "活動時間",
  statusText: "狀態",
};
const exportRows = computed(() =>
  filteredRows.value.map((e) => ({
    id: e.id,
    title: e.title,
    orgName: e.orgName,
    ticket: `${e.ticketSold ?? 0}/${e.ticketTotal ?? 0}（${ticketPct(e)}%）`,
    startAt: formatTime(e.startAt),
    statusText: EVENT_STATUS_META[e.status]?.label ?? "未知",
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-headset me-2" style="color: var(--tap-primary)"></i>客服與查詢</h4>
      <span class="badge rounded-pill" style="background-color: var(--tap-bg-hover)"><i class="bi bi-eye me-1"></i>唯讀</span>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <OperationsTabs />
      <ExportButtons file-name="活動清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="活動清單" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      keyword-placeholder="搜尋活動名稱 / 主辦組織"
      :show-date-range="true"
      date-label="活動時間"
      v-model:from-date="fromDate"
      v-model:to-date="toDate"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm">
          <option value="">全部狀態</option>
          <option value="0">草稿</option>
          <option value="1">審核中</option>
          <option value="2">售票中</option>
          <option value="3">已結束</option>
          <option value="4">已取消</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="沒有符合條件的活動" actions-width="90px">
      <template #cell-title="{ value }">
        <div class="fw-semibold text-truncate" :title="value">{{ value }}</div>
      </template>
      <template #cell-orgName="{ value }">
        <div class="text-truncate" :title="value">{{ value }}</div>
      </template>
      <template #cell-ticketPct="{ row }">
        <div class="small mb-1">{{ ticketPct(row) }}%</div>
        <div class="progress" style="height: 4px">
          <div class="progress-bar bg-primary" :style="{ width: ticketPct(row) + '%' }"></div>
        </div>
      </template>
      <template #cell-startAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-status="{ value }">
        <StatusBadge :variant="EVENT_STATUS_META[value]?.variant" :label="EVENT_STATUS_META[value]?.label ?? '未知'" />
      </template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/operations/events/${row.id}`" class="btn btn-sm btn-icon btn-outline-primary" title="查看">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>
