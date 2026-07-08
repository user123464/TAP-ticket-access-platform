<script setup>
/**
 * MediaFiles.vue — 檔案管理（模組 9 ★ P2）
 *
 * 查看已上傳媒體檔案（Media_Files）：KYC 文件、合約 PDF、活動圖等。
 * 搜尋 + 類型篩選 + 預覽 + 刪除（孤兒檔清理）。
 *
 * API：GET /api/admin/system/media、DELETE .../{id}
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import MaintenanceTabs from "@/components/system/MaintenanceTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:system:media",
  () => api.get("/api/admin/system/media").then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && rows.value.length === 0);
const keyword = ref("");
const filterType = ref("");
const filterCategory = ref("");
const fromDate = ref("");
const toDate = ref("");

const hasExtraFilters = computed(() => !!filterType.value || !!filterCategory.value || !!fromDate.value || !!toDate.value);
const clearExtraFilters = () => {
  filterType.value = "";
  filterCategory.value = "";
  fromDate.value = "";
  toDate.value = "";
};

const TYPE_META = {
  IMAGE: { label: "圖片", icon: "bi-file-earmark-image", variant: "info" },
  PDF: { label: "PDF", icon: "bi-file-earmark-pdf", variant: "danger" },
  DOC: { label: "文件", icon: "bi-file-earmark-text", variant: "secondary" },
};

const fetchMedia = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入媒體檔案失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchMedia);

const categories = computed(() => [...new Set(rows.value.map((m) => m.category).filter(Boolean))]);

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((m) => {
      if (kw && !String(m.fileName ?? "").toLowerCase().includes(kw) && !String(m.uploadedBy ?? "").toLowerCase().includes(kw)) return false;
      if (filterType.value && m.type !== filterType.value) return false;
      if (filterCategory.value && m.category !== filterCategory.value) return false;
      if (fromDate.value && dayjs(m.createdAt).isBefore(dayjs(fromDate.value).startOf("day"))) return false;
      if (toDate.value && dayjs(m.createdAt).isAfter(dayjs(toDate.value).endOf("day"))) return false;
      return true;
    })
    .sort((a, b) => dayjs(b.createdAt).valueOf() - dayjs(a.createdAt).valueOf());
});

// --- 預覽 ---
const previewFile = ref(null);
const showPreview = ref(false);
const previewImgError = ref(false); // 圖片載入失敗（破圖）時降級為「無法預覽」區塊

const BACKEND_URL = import.meta.env.VITE_BACKEND_API || "http://localhost:8080";
const resolvedPreviewUrl = computed(() => {
  const url = previewFile.value?.url;
  if (!url) return "";
  if (url.startsWith("/api/")) {
    return BACKEND_URL + url;
  }
  return url;
});

const openPreview = (row) => { previewFile.value = row; previewImgError.value = false; showPreview.value = true; };

// --- 刪除 ---
const deleting = ref(null);
const removing = ref(false);
const handleDelete = async () => {
  removing.value = true;
  try {
    await api.delete(`/api/admin/system/media/${deleting.value.id}`);
    await fetchMedia();
    toast.success("檔案已刪除");
    deleting.value = null;
  } catch (error) {
    if (error.response) toast.error(error.response.data?.message ?? "刪除失敗");
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    removing.value = false;
  }
};

const columns = [
  { key: "fileName", label: "檔名", sortable: true },
  { key: "type", label: "類型", width: "100px", sortable: true },
  { key: "category", label: "用途", width: "110px", sortable: true },
  { key: "sizeKb", label: "大小", sortable: true, width: "100px" },
  { key: "uploadedBy", label: "上傳者", width: "140px", sortable: true },
  { key: "createdAt", label: "上傳時間", sortable: true, width: "160px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
const formatSize = (kb) => (kb >= 1024 ? `${(kb / 1024).toFixed(1)} MB` : `${kb} KB`);

// 匯出：對應目前篩選後的清單，類型/大小/時間轉成可讀文字
const EXPORT_COLUMNS = {
  fileName: "檔名",
  typeText: "類型",
  category: "用途",
  size: "大小",
  uploadedBy: "上傳者",
  createdAt: "上傳時間",
};
const exportRows = computed(() =>
  filteredRows.value.map((m) => ({
    fileName: m.fileName,
    typeText: TYPE_META[m.type]?.label ?? m.type,
    category: m.category,
    size: formatSize(m.sizeKb),
    uploadedBy: m.uploadedBy,
    createdAt: formatTime(m.createdAt),
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-tools me-2" style="color: var(--tap-primary)"></i>系統維護</h4>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <MaintenanceTabs />
      <ExportButtons file-name="媒體檔案清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="媒體檔案" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar 
      v-model:keyword="keyword" 
      keywordPlaceholder="搜尋檔名或上傳者..."
      :showDateRange="true"
      dateLabel="上傳時間"
      v-model:fromDate="fromDate"
      v-model:toDate="toDate"
      :has-extra-filters="hasExtraFilters"
      @clear="clearExtraFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterType" class="form-select form-select-sm">
          <option value="">全部類型</option>
          <option value="IMAGE">圖片</option>
          <option value="PDF">PDF</option>
          <option value="DOC">文件</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="filterCategory" class="form-select form-select-sm">
          <option value="">全部用途</option>
          <option v-for="cat in categories" :key="cat" :value="cat">{{ cat }}</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="沒有符合條件的檔案" actions-width="110px">
      <template #cell-fileName="{ row }">
        <i class="bi me-2" :class="TYPE_META[row.type]?.icon" :style="{ color: 'var(--tap-primary)' }"></i>
        <span class="fw-semibold">{{ row.fileName }}</span>
      </template>
      <template #cell-type="{ value }">
        <span class="badge rounded-pill" style="background-color: var(--tap-bg-hover)">{{ TYPE_META[value]?.label ?? value }}</span>
      </template>
      <template #cell-category="{ value }"><span class="small">{{ value }}</span></template>
      <template #cell-sizeKb="{ value }"><span class="small">{{ formatSize(value) }}</span></template>
      <template #cell-uploadedBy="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>

      <template #actions="{ row }">
        <button type="button" class="btn btn-sm btn-icon btn-outline-primary" @click="openPreview(row)" title="預覽">
          <i class="bi bi-eye"></i>
        </button>
        <button type="button" class="btn btn-sm btn-icon btn-outline-danger" @click="deleting = row" title="刪除">
          <i class="bi bi-trash"></i>
        </button>
      </template>
    </DataTable>

    <!-- 預覽 -->
    <BaseModal v-model:show="showPreview" :title="previewFile?.fileName ?? '檔案預覽'" size="modal-lg">
      <div v-if="previewFile?.type === 'IMAGE' && previewFile?.url && !previewImgError" class="text-center">
        <img :src="resolvedPreviewUrl" :alt="previewFile.fileName" class="img-fluid rounded-3" @error="previewImgError = true" />
      </div>
      <div v-else class="text-center py-5 text-tap-secondary">
        <i class="bi" :class="previewImgError ? 'bi-image-alt' : TYPE_META[previewFile?.type]?.icon" style="font-size: 3rem"></i>
        <div class="mt-2">{{ previewImgError ? "圖片無法載入（來源失效）" : (previewFile?.url ? "此檔案類型無法內嵌預覽" : "檔案內容由後端 Media_Files API 對接後提供") }}</div>
        <a v-if="previewFile?.url" :href="resolvedPreviewUrl" target="_blank" class="btn btn-sm btn-outline-primary mt-3">開新分頁開啟</a>
      </div>
    </BaseModal>

    <!-- 刪除確認 -->
    <ConfirmDialog
      v-if="deleting"
      :show="!!deleting"
      title="刪除檔案"
      :message="`確定刪除「${deleting.fileName}」？刪除後引用此檔的記錄將無法顯示，請確認為孤兒檔。`"
      confirmText="刪除"
      variant="danger"
      :loading="removing"
      @update:show="deleting = null"
      @confirm="handleDelete"
    />
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
