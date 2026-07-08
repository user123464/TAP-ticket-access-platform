<script setup>
/**
 * NotificationTemplates.vue — 通知模板 CRUD（模組 9 ★ P2）
 *
 * 管理 Email/SMS 模板，支援變數插值即時預覽：
 * 編輯時以範例資料即時渲染 {{user_name}} 等占位符，所見即所得。
 *
 * API：GET /api/admin/system/templates、POST/PUT/DELETE .../{id}
 */
import { ref, computed, onMounted } from "vue";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
import NotificationTabs from "@/components/system/NotificationTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

// SWR 快取
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:system:templates",
  () => api.get("/api/admin/system/templates").then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && rows.value.length === 0);

const keyword = ref("");
const filterChannel = ref("");
const filterActive = ref("");

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value
    .filter((r) => {
      if (kw && !String(r.code ?? "").toLowerCase().includes(kw) && !String(r.name ?? "").toLowerCase().includes(kw)) return false;
      if (filterChannel.value && r.channel !== filterChannel.value) return false;
      if (filterActive.value !== "") {
        const activeBool = filterActive.value === "true";
        if (r.isActive !== activeBool) return false;
      }
      return true;
    })
    .sort((a, b) => String(a.code ?? "").localeCompare(String(b.code ?? ""), "zh-Hant"));
});

const clearFilters = () => {
  filterChannel.value = "";
  filterActive.value = "";
};

const CHANNEL_META = { EMAIL: { label: "Email", variant: "info" }, SMS: { label: "SMS", variant: "primary" } };

// 預覽用範例變數值
const SAMPLE_VARS = {
  user_name: "王小明",
  org_name: "相信音樂股份有限公司",
  event_title: "五月天 25 週年巡迴",
  order_no: "ORD20260612001",
  amount: "13,600",
  reject_reason: "商業登記文件影像模糊",
};

const fetchTemplates = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入通知模板失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchTemplates);

// --- 編輯 + 即時預覽 ---
const editing = ref(null);
const showEdit = ref(false);
const saving = ref(false);

const blank = () => ({ id: null, code: "", name: "", channel: "EMAIL", subject: "", body: "", isActive: true });

const openCreate = () => { editing.value = blank(); showEdit.value = true; };
const openEdit = (row) => { editing.value = { ...row }; showEdit.value = true; };

// 變數插值渲染：把 {{key}} 換成範例值
const interpolate = (text) =>
  (text ?? "").replace(/\{\{\s*(\w+)\s*\}\}/g, (m, key) => SAMPLE_VARS[key] ?? `「${key}?」`);

const previewSubject = computed(() => interpolate(editing.value?.subject));
const previewBody = computed(() => interpolate(editing.value?.body));

// 顯示變數占位符字面（避免在 template 直接寫 }} 被 Vue 編譯器誤判為插值結尾）
const varTag = (key) => `{{${key}}}`;

const handleSave = async () => {
  saving.value = true;
  const payload = { ...editing.value };
  try {
    if (payload.id) await api.put(`/api/admin/system/templates/${payload.id}`, payload);
    else await api.post("/api/admin/system/templates", payload);
    await fetchTemplates();
    toast.success(payload.id ? "模板已更新" : "模板已建立");
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
    await api.delete(`/api/admin/system/templates/${deleting.value.id}`);
    await fetchTemplates();
    toast.success("模板已刪除");
    deleting.value = null;
  } catch (error) {
    if (error.response) toast.error(error.response.data?.message ?? "刪除失敗");
  } finally {
    removing.value = false;
  }
};

const columns = [
  { key: "code", label: "模板代碼", width: "180px", sortable: true },
  { key: "name", label: "名稱", sortable: true },
  { key: "channel", label: "管道", width: "100px", sortable: true },
  { key: "isActive", label: "狀態", width: "100px", sortable: true },
];
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-megaphone me-2" style="color: var(--tap-primary)"></i>通知與公告</h4>
      <button type="button" class="btn btn-sm btn-primary" @click="openCreate"><i class="bi bi-plus-lg me-1"></i>新增模板</button>
    </div>

    <NotificationTabs />

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      keywordPlaceholder="搜尋模板代碼或名稱..."
      :hasExtraFilters="filterChannel !== '' || filterActive !== ''"
      @clear="clearFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterChannel" class="form-select form-select-sm">
          <option value="">全部管道</option>
          <option value="EMAIL">Email</option>
          <option value="SMS">SMS</option>
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

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="尚無通知模板" actions-width="110px">
      <template #cell-code="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-name="{ value }"><span class="fw-semibold">{{ value }}</span></template>
      <template #cell-channel="{ value }">
        <StatusBadge :dot="false" :variant="CHANNEL_META[value]?.variant" :label="CHANNEL_META[value]?.label ?? value" />
      </template>
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

    <!-- 編輯 + 即時預覽 -->
    <BaseModal v-model:show="showEdit" :title="editing?.id ? '編輯模板' : '新增模板'" size="modal-xl">
      <template v-if="editing">
        <div class="row g-3">
          <!-- 左：編輯 -->
          <div class="col-12 col-lg-6">
            <div class="row g-2">
              <div class="col-6">
                <label class="form-label small fw-semibold">模板代碼</label>
                <input v-model.trim="editing.code" type="text" class="form-control" placeholder="KYC_APPROVED" />
              </div>
              <div class="col-6">
                <label class="form-label small fw-semibold">管道</label>
                <select v-model="editing.channel" class="form-select">
                  <option value="EMAIL">Email</option>
                  <option value="SMS">SMS</option>
                </select>
              </div>
              <div class="col-12">
                <label class="form-label small fw-semibold">名稱</label>
                <input v-model.trim="editing.name" type="text" class="form-control" />
              </div>
              <div v-if="editing.channel === 'EMAIL'" class="col-12">
                <label class="form-label small fw-semibold">主旨</label>
                <input v-model="editing.subject" type="text" class="form-control" />
              </div>
              <div class="col-12">
                <label class="form-label small fw-semibold">內容</label>
                <textarea v-model="editing.body" class="form-control font-monospace" rows="8"></textarea>
              </div>
              <div class="col-12">
                <div class="form-check form-switch">
                  <input v-model="editing.isActive" class="form-check-input" type="checkbox" id="tplActive" />
                  <label class="form-check-label small" for="tplActive">啟用</label>
                </div>
              </div>
            </div>
          </div>

          <!-- 右：即時預覽 -->
          <div class="col-12 col-lg-6">
            <label class="form-label small fw-semibold"><i class="bi bi-eye me-1"></i>即時預覽（以範例變數渲染）</label>
            <div class="border rounded-3 p-3" style="border-color: var(--tap-border) !important; background-color: var(--tap-bg-base)">
              <div v-if="editing.channel === 'EMAIL' && editing.subject" class="fw-bold pb-2 mb-2 border-bottom" style="border-color: var(--tap-border) !important">
                {{ previewSubject }}
              </div>
              <div class="small" style="white-space: pre-wrap">{{ previewBody || "（內容預覽）" }}</div>
            </div>
            <div class="small text-tap-secondary mt-2">
              可用變數：<code v-for="(v, k) in SAMPLE_VARS" :key="k" class="me-1">{{ varTag(k) }}</code>
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
      title="刪除模板"
      :message="`確定刪除模板「${deleting.name}」（${deleting.code}）？`"
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
