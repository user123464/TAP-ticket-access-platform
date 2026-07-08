<script setup>
/**
 * ResourceList.vue — 選單資源管理（模組 8 ★ P1）
 *
 * 管理 System_Resources：三端（B2C / B2B / ADMIN_LOCAL）選單結構。
 * Admin 側欄即由 portal_type='ADMIN_LOCAL' 的資料動態載入（stores/menu.js）。
 *
 * API：
 *   GET /api/admin/rbac/resources?portal={portalType}
 *   PUT /api/admin/rbac/resources/{id}
 */
import { ref, computed, onMounted } from "vue";
import api from "@/plugins/axios.js";
import RbacTabs from "@/components/rbac/RbacTabs.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { success: toastSuccess } = useToast();
const { setAnnouncement } = useSystemBanner();

const activePortal = ref("ADMIN_LOCAL");
const portals = [
  { value: "ADMIN_LOCAL", label: "Admin" },
  { value: "B2B", label: "B2B" },
  { value: "B2C", label: "B2C" },
];

// SWR 快取
const { data: dataAdmin, isLoading: loadingAdmin, refresh: refreshAdmin } = useCachedResource(
  "admin:rbac:resources:ADMIN_LOCAL",
  () => api.get("/api/admin/rbac/resources", { params: { portal: "ADMIN_LOCAL" } }).then(r => r.data.data ?? []),
  { initial: [] }
);

const { data: dataB2b, isLoading: loadingB2b, refresh: refreshB2b } = useCachedResource(
  "admin:rbac:resources:B2B",
  () => api.get("/api/admin/rbac/resources", { params: { portal: "B2B" } }).then(r => r.data.data ?? []),
  { initial: [] }
);

const { data: dataB2c, isLoading: loadingB2c, refresh: refreshB2c } = useCachedResource(
  "admin:rbac:resources:B2C",
  () => api.get("/api/admin/rbac/resources", { params: { portal: "B2C" } }).then(r => r.data.data ?? []),
  { initial: [] }
);

const rows = computed(() => {
  if (activePortal.value === "ADMIN_LOCAL") return dataAdmin.value;
  if (activePortal.value === "B2B") return dataB2b.value;
  return dataB2c.value;
});

const isLoading = computed(() => {
  if (activePortal.value === "ADMIN_LOCAL") return loadingAdmin.value;
  if (activePortal.value === "B2B") return loadingB2b.value;
  return loadingB2c.value;
});

const refresh = () => {
  if (activePortal.value === "ADMIN_LOCAL") return refreshAdmin();
  if (activePortal.value === "B2B") return refreshB2b();
  return refreshB2c();
};

const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

const fetchResources = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入選單資源失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchResources);

const switchPortal = (portal) => {
  activePortal.value = portal;
  fetchResources();
};

// --- 搜尋與篩選邏輯 ---
const keyword = ref("");
const filterVisible = ref(""); // '' 全部 / 'true' 顯示 / 'false' 隱藏

const hasExtraFilters = computed(() => !!filterVisible.value);
const clearExtraFilters = () => {
  filterVisible.value = "";
};

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value.filter((r) => {
    // 關鍵字過濾：資源代碼、顯示名稱、路徑
    if (
      kw &&
      !r.code.toLowerCase().includes(kw) &&
      !r.label.toLowerCase().includes(kw) &&
      !(r.path ?? "").toLowerCase().includes(kw)
    ) {
      return false;
    }
    // 顯示於選單狀態過濾
    if (filterVisible.value) {
      const isVisible = filterVisible.value === "true";
      if (r.visible !== isVisible) {
        return false;
      }
    }
    return true;
  });
});

// 預設依排序 (sortOrder) 升冪排序
const sortedRows = computed(() => [...filteredRows.value].sort((a, b) => a.sortOrder - b.sortOrder));

const columns = [
  { key: "sortOrder", label: "排序", width: "70px", sortable: true },
  { key: "code", label: "資源代碼", width: "150px", sortable: true },
  { key: "label", label: "顯示名稱", width: "150px", sortable: true },
  { key: "icon", label: "圖示", width: "200px", sortable: true },
  { key: "path", label: "路徑", sortable: true },
  { key: "visible", label: "顯示", width: "90px", sortable: true },
];

// --- 編輯 ---
const editing = ref(null);
const showEdit = ref(false);
const saving = ref(false);

const openEdit = (row) => {
  editing.value = { ...row };
  showEdit.value = true;
};

const handleSave = async () => {
  saving.value = true;
  try {
    await api.put(`/api/admin/rbac/resources/${editing.value.id}`, editing.value);
    await fetchResources();
  } catch (error) {
    saving.value = false;
    if (error.response) {
      setAnnouncement(error.response.data?.message ?? "儲存失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
    return;
  }
  saving.value = false;
  showEdit.value = false;
  toastSuccess("選單資源已更新");
};
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-shield-lock me-2" style="color: var(--tap-primary)"></i>權限管理</h4>
    </div>

    <RbacTabs />

    <!-- Portal 切換（比照全站二級頁籤：上方細分隔線＋等寬橘框 pills） -->
    <div class="tab-container-nested mb-3">
      <ul class="nav nav-pills gap-3 mb-0">
        <li v-for="portal in portals" :key="portal.value" class="nav-item">
          <button
            type="button"
            class="nav-link py-1 px-3"
            :class="{ active: activePortal === portal.value }"
            @click="switchPortal(portal.value)"
          >
            {{ portal.label }}
          </button>
        </li>
      </ul>
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      keyword-placeholder="搜尋 資源代碼 / 顯示名稱 / 路徑"
      :show-date-range="false"
      :has-extra-filters="hasExtraFilters"
      @clear="clearExtraFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterVisible" class="form-select form-select-sm" title="顯示狀態">
          <option value="">全部狀態</option>
          <option value="true">顯示於選單</option>
          <option value="false">隱藏於選單</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="sortedRows" :loading="loading" emptyText="此端尚未配置選單資源" actions-width="60px">
      <template #cell-code="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-icon="{ value }">
        <i class="bi me-2" :class="value"></i><code class="small text-tap-secondary">{{ value }}</code>
      </template>
      <template #cell-path="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-visible="{ value }">
        <StatusBadge :variant="value ? 'success' : 'secondary'" :label="value ? '顯示' : '隱藏'" />
      </template>

      <template #actions="{ row }">
        <button type="button" class="btn btn-sm btn-icon btn-outline-primary" title="編輯選單資源" @click="openEdit(row)"><i class="bi bi-pencil-square"></i></button>
      </template>
    </DataTable>

    <!-- 編輯 Modal -->
    <BaseModal v-model:show="showEdit" :title="`編輯選單資源：${editing?.code ?? ''}`">
      <template v-if="editing">
        <div class="row g-3">
          <div class="col-8">
            <label class="form-label small fw-semibold">顯示名稱</label>
            <input v-model.trim="editing.label" type="text" class="form-control" />
          </div>
          <div class="col-4">
            <label class="form-label small fw-semibold">排序</label>
            <input v-model.number="editing.sortOrder" type="number" min="1" class="form-control" />
          </div>
          <div class="col-6">
            <label class="form-label small fw-semibold">圖示（bootstrap-icons class）</label>
            <div class="input-group">
              <span class="input-group-text"><i class="bi" :class="editing.icon"></i></span>
              <input v-model.trim="editing.icon" type="text" class="form-control" placeholder="bi-..." />
            </div>
          </div>
          <div class="col-6">
            <label class="form-label small fw-semibold">路徑</label>
            <input v-model.trim="editing.path" type="text" class="form-control" />
          </div>
          <div class="col-12">
            <div class="form-check form-switch">
              <input v-model="editing.visible" class="form-check-input" type="checkbox" id="resVisible" />
              <label class="form-check-label small" for="resVisible">顯示於選單</label>
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
  </div>
</template>
