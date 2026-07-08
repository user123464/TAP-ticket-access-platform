<script setup>
/**
 * Dictionaries.vue — 資料字典管理（模組 9 ★ P2）
 *
 * 管理 dict_type / dict_code / dict_label 對照，供全平台下拉選單與狀態顯示共用。
 * 依 dict_type 分組瀏覽，可新增/編輯/刪除字典項。
 *
 * API：GET /api/admin/system/dictionaries、POST/PUT/DELETE .../{id}
 */
import { ref, computed, watch, onMounted } from "vue";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
import MaintenanceTabs from "@/components/system/MaintenanceTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:system:dictionaries",
  () => api.get("/api/admin/system/dictionaries").then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && rows.value.length === 0);
const filterType = ref("");
const filterActive = ref("");

const fetchDict = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入資料字典失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchDict);

const dictTypes = computed(() => [...new Set(rows.value.map((r) => r.dictType))]);
const keyword = ref("");

const hasExtraFilters = computed(() => !!filterType.value || !!filterActive.value);
const clearExtraFilters = () => {
  filterType.value = "";
  filterActive.value = "";
};

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value.filter((r) => {
    if (kw && 
        !String(r.dictCode ?? "").toLowerCase().includes(kw) && 
        !String(r.dictLabel ?? "").toLowerCase().includes(kw)
    ) return false;
    if (filterType.value && r.dictType !== filterType.value) return false;
    if (filterActive.value !== "") {
      const activeBool = filterActive.value === "true";
      if (r.isActive !== activeBool) return false;
    }
    return true;
  }).sort((a, b) => {
    if (a.dictType !== b.dictType) return a.dictType.localeCompare(b.dictType);
    return a.sortOrder - b.sortOrder;
  });
});

// --- 編輯 ---
const editing = ref(null);
const showEdit = ref(false);
const saving = ref(false);

const openCreate = () => { editing.value = { id: null, dictType: filterType.value || (dictTypes.value[0] ?? ""), dictCode: "", dictLabel: "", sortOrder: 1, isActive: true }; showEdit.value = true; };
const openEdit = (row) => { editing.value = { ...row }; showEdit.value = true; };

const handleSave = async () => {
  saving.value = true;
  const payload = { ...editing.value };
  try {
    if (payload.id) await api.put(`/api/admin/system/dictionaries/${payload.id}`, payload);
    else await api.post("/api/admin/system/dictionaries", payload);
    await fetchDict();
    toast.success(payload.id ? "字典項已更新" : "字典項已建立");
    filterType.value = payload.dictType;
    showEdit.value = false;
  } catch (error) {
    if (error.response) toast.error(error.response.data?.message ?? "儲存失敗");
  } finally {
    saving.value = false;
  }
};

// --- 刪除 ---
const deleting = ref(null);
const removing = ref(false);
const handleDelete = async () => {
  removing.value = true;
  try {
    await api.delete(`/api/admin/system/dictionaries/${deleting.value.id}`);
    await fetchDict();
    toast.success("字典項已刪除");
    deleting.value = null;
  } catch (error) {
    if (error.response) toast.error(error.response.data?.message ?? "刪除失敗");
  } finally {
    removing.value = false;
  }
};

const columns = [
  { key: "dictType", label: "字典類型", width: "240px", sortable: true },
  { key: "sortOrder", label: "排序", width: "80px", sortable: true },
  { key: "dictCode", label: "代碼", width: "120px", sortable: true },
  { key: "dictLabel", label: "顯示文字 (dict_label)", sortable: true },
  { key: "isActive", label: "狀態", width: "100px", sortable: true },
];
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-tools me-2" style="color: var(--tap-primary)"></i>系統維護</h4>
      <button type="button" class="btn btn-sm btn-primary" @click="openCreate"><i class="bi bi-plus-lg me-1"></i>新增字典項</button>
    </div>

    <MaintenanceTabs />

    <SearchBar
      v-model:keyword="keyword"
      keywordPlaceholder="搜尋字典代碼或顯示文字..."
      :hasExtraFilters="hasExtraFilters"
      @clear="clearExtraFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterType" class="form-select form-select-sm">
          <option value="">全部字典類型</option>
          <option v-for="type in dictTypes" :key="type" :value="type">{{ type }}</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="filterActive" class="form-select form-select-sm">
          <option value="">全部狀態</option>
          <option value="true">啟用</option>
          <option value="false">停用</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="尚無符合條件的字典項" actions-width="110px">
      <template #cell-dictCode="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-dictLabel="{ value }"><span class="fw-semibold">{{ value }}</span></template>
      <template #cell-isActive="{ value }">
        <StatusBadge :variant="value ? 'success' : 'secondary'" :label="value ? '啟用' : '停用'" />
      </template>

      <template #actions="{ row }">
        <button type="button" class="btn btn-sm btn-icon btn-outline-primary" @click="openEdit(row)" title="編輯">
          <i class="bi bi-pencil-square"></i>
        </button>
        <button type="button" class="btn btn-sm btn-icon btn-outline-danger" @click="deleting = row" title="刪除">
          <i class="bi bi-trash"></i>
        </button>
      </template>
    </DataTable>

    <!-- 編輯 -->
    <BaseModal v-model:show="showEdit" :title="editing?.id ? '編輯字典項' : '新增字典項'">
      <template v-if="editing">
        <div class="row g-3">
          <div class="col-12">
            <label class="form-label small fw-semibold">類型 (dict_type)</label>
            <input v-model.trim="editing.dictType" type="text" class="form-control" placeholder="PAYMENT_METHOD" />
          </div>
          <div class="col-8">
            <label class="form-label small fw-semibold">代碼 (dict_code)</label>
            <input v-model.trim="editing.dictCode" type="text" class="form-control" placeholder="CREDIT_CARD" />
          </div>
          <div class="col-4">
            <label class="form-label small fw-semibold">排序</label>
            <input v-model.number="editing.sortOrder" type="number" min="1" class="form-control" />
          </div>
          <div class="col-12">
            <label class="form-label small fw-semibold">顯示文字 (dict_label)</label>
            <input v-model.trim="editing.dictLabel" type="text" class="form-control" placeholder="信用卡" />
          </div>
          <div class="col-12">
            <div class="form-check form-switch">
              <input v-model="editing.isActive" class="form-check-input" type="checkbox" id="dictActive" />
              <label class="form-check-label small" for="dictActive">啟用</label>
            </div>
          </div>
        </div>
      </template>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary" :disabled="saving" @click="showEdit = false">取消</button>
        <button type="button" class="btn btn-primary position-relative" :disabled="saving" @click="handleSave">
          <span :class="{ 'invisible': saving }">儲存</span>
          <span v-if="saving" class="spinner-border spinner-border-sm position-absolute top-50 start-50 translate-middle" role="status"></span>
        </button>
      </template>
    </BaseModal>

    <!-- 刪除確認 -->
    <ConfirmDialog
      v-if="deleting"
      :show="!!deleting"
      title="刪除字典項"
      :message="`確定刪除「${deleting.dictLabel}」（${deleting.dictCode}）？請確認無業務資料依賴此項。`"
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
