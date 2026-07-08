<script setup>
/**
 * Submissions.vue — 客訴 / 回饋處理（模組 9 ★ P2）
 *
 * 查看 User_Submissions，更新處理狀態 UNREAD → IN_PROGRESS → RESOLVED。
 * 含 B2B 客製合約洽詢（type=CONTACT）—— 客服標記 IN_PROGRESS 即進入線下簽約流程。
 *
 * API：GET /api/admin/system/submissions、PUT .../{id}/status { status }
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import CountBadge from "@/components/common/CountBadge.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import OperationsTabs from "@/components/operations/OperationsTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:system:submissions",
  () => api.get("/api/admin/system/submissions").then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && rows.value.length === 0);
const filterStatus = ref("");
const filterType = ref("");

const hasExtraFilters = computed(() => !!filterType.value || !!filterStatus.value);
const clearExtraFilters = () => {
  filterType.value = "";
  filterStatus.value = "";
};

const STATUS_META = {
  UNREAD: { label: "未讀", variant: "danger" },
  IN_PROGRESS: { label: "處理中", variant: "warning" },
  RESOLVED: { label: "已解決", variant: "success" },
};
const TYPE_LABELS = { COMPLAINT: "客訴", FEEDBACK: "建議回饋", CONTACT: "合作洽詢" };

const fetchSubmissions = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入客訴回饋失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchSubmissions);

const keyword = ref("");
const fromDate = ref("");
const toDate = ref("");

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value.filter((s) => {
    if (filterStatus.value && s.status !== filterStatus.value) return false;
    if (filterType.value && s.type !== filterType.value) return false;
    if (kw && !s.subject.toLowerCase().includes(kw) && !s.fromName.toLowerCase().includes(kw) && !s.fromEmail.toLowerCase().includes(kw)) return false;
    if (fromDate.value && dayjs(s.createdAt).isBefore(dayjs(fromDate.value).startOf('day'))) return false;
    if (toDate.value && dayjs(s.createdAt).isAfter(dayjs(toDate.value).endOf('day'))) return false;
    return true;
  });
});

const unreadCount = computed(() => rows.value.filter((s) => s.status === "UNREAD").length);
const inProgressCount = computed(() => rows.value.filter((s) => s.status === "IN_PROGRESS").length);

// --- 詳情 + 狀態更新 ---
const viewing = ref(null);
const showDetail = ref(false);
const updating = ref(false);

const openDetail = (row) => {
  viewing.value = row;
  showDetail.value = true;
  // 開啟未讀客訴時自動標記為處理中
  if (row.status === "UNREAD") updateStatus(row, "IN_PROGRESS", true);
};

const updateStatus = async (row, status, silent = false) => {
  if (!silent) updating.value = true;
  try {
    await api.put(`/api/admin/system/submissions/${row.id}/status`, { status });
    row.status = status;
    if (viewing.value?.id === row.id) viewing.value = { ...viewing.value, status };
    await refresh();
    if (!silent) toast.success("客訴狀態已更新");
  } catch (error) {
    if (error.response && !silent) {
      toast.error(error.response.data?.message ?? "更新失敗");
    }
  } finally {
    if (!silent) updating.value = false;
  }
};

const columns = [
  { key: "type", label: "類型", sortable: true, width: "100px" },
  { key: "subject", label: "主旨", sortable: true },
  { key: "fromName", label: "來自", sortable: true, width: "190px" },
  { key: "createdAt", label: "時間", sortable: true, width: "160px" },
  { key: "status", label: "狀態", sortable: true, width: "100px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");

// 匯出：對應目前篩選後的清單，類型/狀態/時間轉成可讀文字
const EXPORT_COLUMNS = {
  typeText: "類型",
  subject: "主旨",
  fromName: "來自",
  fromEmail: "Email",
  createdAt: "時間",
  statusText: "狀態",
};
const exportRows = computed(() =>
  filteredRows.value.map((s) => ({
    typeText: TYPE_LABELS[s.type] ?? s.type,
    subject: s.subject,
    fromName: s.fromName,
    fromEmail: s.fromEmail,
    createdAt: formatTime(s.createdAt),
    statusText: STATUS_META[s.status]?.label ?? s.status,
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-headset me-2" style="color: var(--tap-primary)"></i>客服與查詢</h4>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <OperationsTabs />
      <ExportButtons file-name="客訴回饋清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="客訴回饋" :disabled="loading" />
    </div>

    <!-- 狀態篩選（二級頁籤）：上方淡分隔線建立父子層級 -->
    <div class="tab-container-nested mb-3">
      <ul class="nav nav-pills gap-3 mb-0">
        <li class="nav-item"><button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === '' }" @click="filterStatus = ''">全部</button></li>
        <li class="nav-item">
          <CountBadge :count="unreadCount">
            <button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === 'UNREAD' }" @click="filterStatus = 'UNREAD'">未讀</button>
          </CountBadge>
        </li>
        <li class="nav-item">
          <CountBadge :count="inProgressCount">
            <button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === 'IN_PROGRESS' }" @click="filterStatus = 'IN_PROGRESS'">處理中</button>
          </CountBadge>
        </li>
        <li class="nav-item"><button type="button" class="nav-link py-1 px-3" :class="{ active: filterStatus === 'RESOLVED' }" @click="filterStatus = 'RESOLVED'">已解決</button></li>
      </ul>
    </div>

    <!-- 搜尋與過濾 -->
    <SearchBar
      v-model:keyword="keyword"
      keyword-placeholder="搜尋主旨 / 姓名 / Email"
      :show-date-range="true"
      date-label="提交時間"
      v-model:from-date="fromDate"
      v-model:to-date="toDate"
      :has-extra-filters="hasExtraFilters"
      @clear="clearExtraFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterType" class="form-select form-select-sm" title="類型篩選">
          <option value="">全部類型</option>
          <option value="COMPLAINT">客訴</option>
          <option value="FEEDBACK">建議回饋</option>
          <option value="CONTACT">合作洽詢</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm" title="狀態篩選">
          <option value="">全部狀態</option>
          <option value="UNREAD">未讀</option>
          <option value="IN_PROGRESS">處理中</option>
          <option value="RESOLVED">已解決</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="沒有符合條件的客訴回饋">
      <template #cell-type="{ value }">
        <span class="badge rounded-pill" style="background-color: var(--tap-bg-hover)">{{ TYPE_LABELS[value] ?? value }}</span>
      </template>
      <template #cell-subject="{ value }"><span class="fw-semibold">{{ value }}</span></template>
      <template #cell-fromName="{ row }">
        <div class="min-w-0">
          <div class="small fw-semibold text-truncate" :title="row.fromName">{{ row.fromName }}</div>
          <div class="small text-tap-secondary text-truncate" :title="row.fromEmail">{{ row.fromEmail }}</div>
        </div>
      </template>
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-status="{ value }">
        <StatusBadge :variant="STATUS_META[value]?.variant" :label="STATUS_META[value]?.label ?? value" />
      </template>

      <template #actions="{ row }">
        <button type="button" class="btn btn-sm btn-icon btn-outline-primary" :title="row.status === 'RESOLVED' ? '查看' : '處理'" @click="openDetail(row)">
          <i class="bi bi-pencil-square"></i>
        </button>
      </template>
    </DataTable>

    <!-- 詳情 + 狀態流轉 -->
    <BaseModal v-model:show="showDetail" :title="viewing?.subject ?? '客訴詳情'" size="modal-lg">
      <template v-if="viewing">
        <div class="d-flex align-items-center gap-2 mb-3">
          <span class="badge rounded-pill" style="background-color: var(--tap-bg-hover)">{{ TYPE_LABELS[viewing.type] }}</span>
          <StatusBadge :variant="STATUS_META[viewing.status]?.variant" :label="STATUS_META[viewing.status]?.label" />
        </div>
        <div class="small text-tap-secondary mb-1">來自 {{ viewing.fromName }}（{{ viewing.fromEmail }}） · {{ formatTime(viewing.createdAt) }}</div>
        <div class="border rounded-3 p-3 mt-2" style="border-color: var(--tap-border) !important; background-color: var(--tap-bg-base)">
          {{ viewing.content }}
        </div>
        <div v-if="viewing.type === 'CONTACT'" class="alert mt-3 mb-0 small d-flex align-items-center gap-2" style="background-color: var(--tap-bg-hover)">
          <i class="bi bi-info-circle" style="color: var(--tap-primary)"></i>
          合作洽詢標記「處理中」後即進入線下簽約流程，完成後於合約管理建立客製合約。
        </div>
      </template>
      <template #footer>
        <button type="button" class="btn btn-outline-warning" :disabled="updating || viewing?.status === 'IN_PROGRESS'" @click="updateStatus(viewing, 'IN_PROGRESS')">標記處理中</button>
        <button type="button" class="btn btn-success" :disabled="updating || viewing?.status === 'RESOLVED'" @click="updateStatus(viewing, 'RESOLVED')">
          <span v-if="updating" class="spinner-border spinner-border-sm me-2"></span>標記已解決
        </button>
      </template>
    </BaseModal>
  </div>
</template>
