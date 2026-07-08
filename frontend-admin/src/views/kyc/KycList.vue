<script setup>
/**
 * KycList.vue — KYC 實名審核列表（模組 4 ★ P0）
 *
 * 預設篩選 kyc_status=1（待審核），依提交時間排序（最早提交優先處理）。
 * API：GET /api/admin/organizers?status={PENDING|APPROVED|REJECTED}
 *   後端回傳 AdminOrganizerListItem（orgId/name/taxId/status/kycStatus/memberCount/createdAt）；
 *   無 submittedAt 欄位，提交時間以 createdAt 代之。
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import CountBadge from "@/components/common/CountBadge.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import OrganizerTabs from "@/components/organizers/OrganizerTabs.vue";
import { KYC_STATUS, KYC_STATUS_META } from "@/constants/kyc.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

// 狀態頁籤：預設「待審核」。value 為數字碼，name 為後端 KycStatus 枚舉名稱（query 用）
const activeStatus = ref(KYC_STATUS.PENDING);
const statusTabs = [
  { value: KYC_STATUS.PENDING, name: "PENDING", label: "待審核" },
  { value: KYC_STATUS.APPROVED, name: "APPROVED", label: "已通過" },
  { value: KYC_STATUS.REJECTED, name: "REJECTED", label: "已退件" },
  { value: null, name: null, label: "全部" },
];

const { data: dataPending, isLoading: loadingPending, refresh: refreshPending } = useCachedResource(
  "admin:kyc:PENDING",
  () => api.get("/api/admin/organizers", { params: { status: "PENDING" } }).then(r => (r.data.data ?? []).map((r) => ({ ...r, submittedAt: r.submittedAt ?? r.createdAt }))),
  { initial: [] }
);

const { data: dataApproved, isLoading: loadingApproved, refresh: refreshApproved } = useCachedResource(
  "admin:kyc:APPROVED",
  () => api.get("/api/admin/organizers", { params: { status: "APPROVED" } }).then(r => (r.data.data ?? []).map((r) => ({ ...r, submittedAt: r.submittedAt ?? r.createdAt }))),
  { initial: [] }
);

const { data: dataRejected, isLoading: loadingRejected, refresh: refreshRejected } = useCachedResource(
  "admin:kyc:REJECTED",
  () => api.get("/api/admin/organizers", { params: { status: "REJECTED" } }).then(r => (r.data.data ?? []).map((r) => ({ ...r, submittedAt: r.submittedAt ?? r.createdAt }))),
  { initial: [] }
);

const { data: dataAll, isLoading: loadingAll, refresh: refreshAll } = useCachedResource(
  "admin:kyc:ALL",
  () => api.get("/api/admin/organizers").then(r => (r.data.data ?? []).map((r) => ({ ...r, submittedAt: r.submittedAt ?? r.createdAt }))),
  { initial: [] }
);

const rows = computed(() => {
  if (activeStatus.value === KYC_STATUS.PENDING) return dataPending.value;
  if (activeStatus.value === KYC_STATUS.APPROVED) return dataApproved.value;
  if (activeStatus.value === KYC_STATUS.REJECTED) return dataRejected.value;
  return dataAll.value;
});

const isLoading = computed(() => {
  if (activeStatus.value === KYC_STATUS.PENDING) return loadingPending.value;
  if (activeStatus.value === KYC_STATUS.APPROVED) return loadingApproved.value;
  if (activeStatus.value === KYC_STATUS.REJECTED) return loadingRejected.value;
  return loadingAll.value;
});

const refresh = () => {
  if (activeStatus.value === KYC_STATUS.PENDING) return refreshPending();
  if (activeStatus.value === KYC_STATUS.APPROVED) return refreshApproved();
  if (activeStatus.value === KYC_STATUS.REJECTED) return refreshRejected();
  return refreshAll();
};

const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

// 搜尋（組織名稱 / 統編 / 編號）與日期區間
const keyword = ref("");
const fromDate = ref("");
const toDate = ref("");

const fetchList = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入 KYC 清單失敗，請稍後再試。", "danger");
    }
  }
};

const switchTab = (status) => {
  activeStatus.value = status;
  fetchList();
};

onMounted(fetchList);

// 依提交時間升冪（最早提交優先處理）+ 關鍵字過濾 + 日期過濾
// 後端 list 僅提供 createdAt（無獨立 submittedAt 欄位），以 createdAt 作為提交時間代理。
const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((r) => {
      if (kw && !r.name.toLowerCase().includes(kw) && !(r.taxId ?? "").includes(kw) && !(r.orgId ?? "").toLowerCase().includes(kw)) return false;
      if (fromDate.value && dayjs(r.createdAt).isBefore(dayjs(fromDate.value).startOf('day'))) return false;
      if (toDate.value && dayjs(r.createdAt).isAfter(dayjs(toDate.value).endOf('day'))) return false;
      return true;
    })
    .sort((a, b) => new Date(a.createdAt) - new Date(b.createdAt));
});

const columns = [
  { key: "orgId", label: "組織編號", sortable: true, width: "140px" },
  { key: "name", label: "組織名稱", sortable: true },
  { key: "taxId", label: "統一編號", sortable: true, width: "120px" },
  { key: "submittedAt", label: "提交時間", sortable: true, width: "180px" },
  { key: "kycStatus", label: "狀態", sortable: true, width: "110px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");

// 匯出：對應目前篩選後的清單，時間/狀態轉成可讀文字
const EXPORT_COLUMNS = {
  orgId: "組織編號",
  name: "組織名稱",
  taxId: "統一編號",
  submittedAt: "提交時間",
  kycStatusText: "狀態",
};
const exportRows = computed(() =>
  filteredRows.value.map((r) => ({
    orgId: r.orgId,
    name: r.name,
    taxId: r.taxId,
    submittedAt: formatTime(r.submittedAt),
    kycStatusText: KYC_STATUS_META[r.kycStatus]?.label ?? "未知",
  }))
);
</script>

<template>
  <div>
    <!-- 頁面標題 -->
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-building me-2" style="color: var(--tap-primary)"></i>組織管理</h4>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <OrganizerTabs />
      <ExportButtons file-name="KYC審核清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="KYC審核" :disabled="loading" />
    </div>

    <!-- 狀態頁籤（二級）+ 搜尋：上方淡分隔線建立父子層級 -->
    <div class="tab-container-nested mb-3">
      <ul class="nav nav-pills gap-3 mb-0">
        <li v-for="tab in statusTabs" :key="String(tab.value)" class="nav-item">
          <CountBadge v-if="tab.value === KYC_STATUS.PENDING" :count="dataPending?.length ?? 0">
            <button
              type="button"
              class="nav-link py-1 px-3"
              :class="{ active: activeStatus === tab.value }"
              @click="switchTab(tab.value)"
            >
              {{ tab.label }}
            </button>
          </CountBadge>
          <button
            v-else
            type="button"
            class="nav-link py-1 px-3"
            :class="{ active: activeStatus === tab.value }"
            @click="switchTab(tab.value)"
          >
            {{ tab.label }}
          </button>
        </li>
      </ul>
    </div>

    <!-- 搜尋與過濾（含下拉選單同步狀態） -->
    <SearchBar
      v-model:keyword="keyword"
      keyword-placeholder="搜尋組織名稱 / 統編 / 編號"
      :show-date-range="true"
      date-label="提交時間"
      v-model:from-date="fromDate"
      v-model:to-date="toDate"
    >
      <div class="col-6 col-md-auto">
        <select class="form-select form-select-sm" :value="activeStatus ?? ''" @change="switchTab($event.target.value ? Number($event.target.value) : null)">
          <option value="">全部狀態</option>
          <option :value="KYC_STATUS.PENDING">待審核</option>
          <option :value="KYC_STATUS.APPROVED">已通過</option>
          <option :value="KYC_STATUS.REJECTED">已退件</option>
        </select>
      </div>
    </SearchBar>

    <!-- 列表 -->
    <DataTable :columns="columns" :rows="filteredRows" row-key="orgId" :loading="loading" emptyText="目前沒有符合條件的 KYC 申請" actions-width="90px">
      <template #cell-submittedAt="{ value }">
        <span class="small">{{ formatTime(value) }}</span>
      </template>

      <template #cell-kycStatus="{ value }">
        <StatusBadge :variant="KYC_STATUS_META[value]?.variant" :label="KYC_STATUS_META[value]?.label ?? '未知'" />
      </template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/organizers/kyc/${row.orgId}`" class="btn btn-sm btn-icon btn-outline-primary" :title="row.kycStatus === KYC_STATUS.PENDING ? '審核' : '查看'">
          <i class="bi bi-pencil-square"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>
