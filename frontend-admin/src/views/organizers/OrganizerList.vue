<script setup>
/**
 * OrganizerList.vue — 組織列表（模組 3 ★ P1）
 *
 * 搜尋（名稱/統編）+ 篩選（status / kyc_status）。
 * API：GET /api/admin/organizers（後端未就緒時 fallback mock）
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import OrganizerTabs from "@/components/organizers/OrganizerTabs.vue";
import { ORG_STATUS_META } from "@/constants/organizer.js";
import { KYC_STATUS_META } from "@/constants/kyc.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

// SWR 快取：建立時立刻帶回上次列表（不閃白），背景再 refresh 取最新
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:organizers",
  () => api.get("/api/admin/organizers").then((r) => r.data.data ?? []),
  { initial: [] }
);
// spinner 只在「完全沒有快取可顯示」時出現
const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

const keyword = ref(""); // 名稱 / 統編 / 編號
const filterStatus = ref(""); // '' / 0 / 1 / 2
const filterKyc = ref(""); // '' / 0~3
const fromDate = ref("");
const toDate = ref("");

const fetchOrgs = async () => {
  try {
    await refresh();
  } catch (error) {
    // 後端回錯(4xx/5xx)：仍走原本錯誤提示，不吞錯；保留快取舊資料避免閃白
    if (error.response) {
      setAnnouncement("載入組織清單失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  }
};

onMounted(fetchOrgs);

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value.filter((o) => {
    if (kw && !o.name.toLowerCase().includes(kw) && !(o.taxId ?? "").includes(kw) && !(o.orgId ?? "").toLowerCase().includes(kw)) return false;
    if (filterStatus.value !== "" && o.status !== Number(filterStatus.value)) return false;
    if (filterKyc.value !== "" && o.kycStatus !== Number(filterKyc.value)) return false;

    if (fromDate.value) {
      if (dayjs(o.createdAt).isBefore(dayjs(fromDate.value).startOf('day'))) return false;
    }
    if (toDate.value) {
      if (dayjs(o.createdAt).isAfter(dayjs(toDate.value).endOf('day'))) return false;
    }

    return true;
  });
});

const columns = [
  { key: "orgId", label: "組織編號", sortable: true, width: "140px" },
  { key: "name", label: "組織名稱", sortable: true },
  { key: "taxId", label: "統一編號", sortable: true, width: "120px" },
  { key: "memberCount", label: "成員數", sortable: true, width: "80px" },
  { key: "kycStatus", label: "KYC", sortable: true, width: "110px" },
  { key: "status", label: "狀態", sortable: true, width: "100px" },
  { key: "createdAt", label: "建立時間", sortable: true, width: "110px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD") : "—");

// 匯出：對應目前篩選後的清單，KYC/狀態/時間轉成可讀文字
const EXPORT_COLUMNS = {
  orgId: "組織編號",
  name: "組織名稱",
  taxId: "統一編號",
  memberCount: "成員數",
  kycText: "KYC",
  statusText: "狀態",
  createdAt: "建立時間",
};
const exportRows = computed(() =>
  filteredRows.value.map((o) => ({
    orgId: o.orgId,
    name: o.name,
    taxId: o.taxId,
    memberCount: o.memberCount,
    kycText: KYC_STATUS_META[o.kycStatus]?.label ?? "未知",
    statusText: ORG_STATUS_META[o.status]?.label ?? "未知",
    createdAt: formatTime(o.createdAt),
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-building me-2" style="color: var(--tap-primary)"></i>組織管理</h4>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <OrganizerTabs />
      <ExportButtons file-name="組織清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="組織清單" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      keyword-placeholder="搜尋組織名稱 / 統編 / 編號"
      :show-date-range="true"
      date-label="建立時間"
      v-model:from-date="fromDate"
      v-model:to-date="toDate"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterKyc" class="form-select form-select-sm" title="KYC 狀態">
          <option value="">全部 KYC</option>
          <option value="0">草稿</option>
          <option value="1">待審核</option>
          <option value="2">已通過</option>
          <option value="3">已退件</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm" title="組織狀態">
          <option value="">全部狀態</option>
          <option value="0">正常</option>
          <option value="1">暫停</option>
          <option value="2">封存</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" row-key="orgId" :loading="loading" emptyText="沒有符合條件的組織" actions-width="90px">
      <template #cell-name="{ row }">
        <div class="text-truncate" style="max-width: 100%;" :title="row.name">{{ row.name }}</div>
      </template>
      <template #cell-kycStatus="{ value }">
        <StatusBadge :variant="KYC_STATUS_META[value]?.variant" :label="KYC_STATUS_META[value]?.label ?? '未知'" />
      </template>
      <template #cell-status="{ value }">
        <StatusBadge :variant="ORG_STATUS_META[value]?.variant" :label="ORG_STATUS_META[value]?.label ?? '未知'" />
      </template>
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/organizers/${row.orgId}`" class="btn btn-sm btn-icon btn-outline-primary" title="詳情">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>
