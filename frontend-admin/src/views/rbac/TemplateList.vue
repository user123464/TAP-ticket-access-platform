<script setup>
/**
 * TemplateList.vue — 組織角色模板管理（權限管理 ▸ 組織角色模板）
 *
 * 模板＝新組織建立時的角色藍圖：只影響日後新建組織，不回溯既有組織。
 * 支援新增 / 改名說明 / 刪除 / 編輯預設權限（共用 PermissionPicker）。
 * API：GET /api/admin/rbac/roles（roleTemplates）、/permissions；
 *      POST/PUT/DELETE /templates、PUT /templates/{id}/permissions
 */
import { ref, computed, onMounted } from "vue";
import api from "@/plugins/axios.js";
import RbacTabs from "@/components/rbac/RbacTabs.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import PermissionPicker from "@/components/rbac/PermissionPicker.vue";
import { useToast } from "@/composables/useToast";
import { useConfirm } from "@/composables/useConfirm";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { success: toastSuccess } = useToast();
const { confirm } = useConfirm();
const { setAnnouncement } = useSystemBanner();

const { data: rolesData, isLoading, refresh } = useCachedResource(
  "admin:rbac:roles",
  () => api.get("/api/admin/rbac/roles").then((r) => r.data.data ?? {}),
  { initial: { platformRoles: [], roleTemplates: [] } }
);
const { data: permissions, refresh: refreshPermissions } = useCachedResource(
  "admin:rbac:permissions",
  () => api.get("/api/admin/rbac/permissions").then((r) => r.data.data ?? []),
  { initial: [] }
);

const templates = computed(() => rolesData.value?.roleTemplates ?? []);
const loading = computed(() => isLoading.value && templates.value.length === 0);

const fetchTemplates = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) setAnnouncement("載入角色模板失敗，請稍後再試。", "danger");
  }
};
onMounted(() => {
  fetchTemplates();
  // 權限清單同樣要在進頁時主動抓取，否則只會在視窗重新聚焦時才補抓（模板權限彈窗會先顯示「無可用權限」假象）。
  refreshPermissions().catch(() => {});
});

// ── 新增 / 編輯名稱說明 ──
const editing = ref(null); // { id?, name, description, isNew }
const showEdit = ref(false);
const saving = ref(false);

const openCreate = () => {
  editing.value = { name: "", description: "", isNew: true };
  showEdit.value = true;
};
const openEdit = (tpl) => {
  editing.value = { id: tpl.id, name: tpl.name, description: tpl.description, isNew: false };
  showEdit.value = true;
};

const handleSave = async () => {
  if (!editing.value.name?.trim()) {
    setAnnouncement("模板名稱不可空白。", "warning");
    return;
  }
  saving.value = true;
  try {
    if (editing.value.isNew) {
      await api.post("/api/admin/rbac/templates", {
        name: editing.value.name,
        description: editing.value.description,
      });
    } else {
      await api.put(`/api/admin/rbac/templates/${editing.value.id}`, {
        name: editing.value.name,
        description: editing.value.description,
      });
    }
    await fetchTemplates();
  } catch (error) {
    saving.value = false;
    if (error.response) setAnnouncement(error.response.data?.message ?? "儲存失敗，請稍後再試。", "danger");
    return;
  }
  saving.value = false;
  showEdit.value = false;
  toastSuccess(editing.value.isNew ? "模板已建立" : "模板已更新");
};

// ── 刪除 ──
const handleDelete = async (tpl) => {
  const ok = await confirm({
    title: "刪除角色模板",
    message: `確定刪除模板「${tpl.name}」？只影響日後新建組織，既有組織不受影響。`,
    confirmText: "刪除",
    variant: "danger",
  });
  if (!ok) return;
  try {
    await api.delete(`/api/admin/rbac/templates/${tpl.id}`);
    await fetchTemplates();
    toastSuccess("模板已刪除");
  } catch (error) {
    if (error.response) setAnnouncement(error.response.data?.message ?? "刪除失敗，請稍後再試。", "danger");
  }
};

// ── 預設權限編輯（共用 PermissionPicker） ──
const showPicker = ref(false);
const pickerTpl = ref(null);
const pickerSaving = ref(false);

const openPermissions = (tpl) => {
  pickerTpl.value = tpl;
  showPicker.value = true;
};
const savePermissions = async (ids) => {
  if (!pickerTpl.value) return;
  pickerSaving.value = true;
  try {
    await api.put(`/api/admin/rbac/templates/${pickerTpl.value.id}/permissions`, { permissions: ids });
    await fetchTemplates();
  } catch (error) {
    pickerSaving.value = false;
    if (error.response) setAnnouncement(error.response.data?.message ?? "更新權限失敗，請稍後再試。", "danger");
    return;
  }
  pickerSaving.value = false;
  showPicker.value = false;
  toastSuccess("模板預設權限已更新");
};
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-shield-lock me-2" style="color: var(--tap-primary)"></i>權限管理</h4>
    </div>

    <RbacTabs />

    <div class="d-flex align-items-center justify-content-between mb-2">
      <div class="fw-bold">
        組織角色模板管理
      </div>
      <button type="button" class="btn btn-sm btn-primary fw-semibold" @click="openCreate">
        <i class="bi bi-plus-lg me-1"></i>新增模板
      </button>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <div v-else class="row g-3">
      <div v-for="tpl in templates" :key="tpl.id" class="col-12 col-md-4">
        <div class="card border shadow-sm rounded-4 h-100">
          <div class="card-body d-flex flex-column">
            <div class="d-flex align-items-center justify-content-between mb-2">
              <div class="fw-bold">{{ tpl.name }}</div>
              <code class="small">{{ tpl.code }}</code>
            </div>
            <p class="small text-tap-secondary flex-grow-1 mb-3">{{ tpl.description }}</p>
            <div class="d-flex align-items-center justify-content-between">
              <span class="small text-tap-secondary"><i class="bi bi-key me-1"></i>{{ tpl.permissionCount }} 項預設權限</span>
              <div class="d-flex gap-2">
                <!-- 編輯名稱說明按鈕 -->
                <button
                  type="button"
                  class="btn btn-sm btn-icon"
                  :class="tpl.editable !== false ? 'btn-outline-primary' : 'btn-locked'"
                  :disabled="tpl.editable === false"
                  :title="tpl.editable !== false ? '編輯名稱/說明' : '系統內建，不可編輯'"
                  @click="openEdit(tpl)"
                >
                  <i class="bi bi-pencil-square"></i>
                </button>
                <!-- 預設權限設定按鈕（齒輪） -->
                <button
                  type="button"
                  class="btn btn-sm btn-icon btn-outline-primary"
                  title="預設權限設定"
                  @click="openPermissions(tpl)"
                >
                  <i class="bi bi-gear"></i>
                </button>
                <!-- 刪除模板按鈕 -->
                <button
                  type="button"
                  class="btn btn-sm btn-icon"
                  :class="tpl.deletable !== false ? 'btn-outline-danger' : 'btn-locked'"
                  :disabled="tpl.deletable === false"
                  :title="tpl.deletable !== false ? '刪除模板' : '內建模板，不可刪除'"
                  @click="handleDelete(tpl)"
                >
                  <i class="bi bi-trash3"></i>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
      <div v-if="templates.length === 0" class="col-12 text-center text-tap-secondary py-4">尚無角色模板</div>
    </div>

    <!-- 新增 / 編輯名稱說明 -->
    <BaseModal v-model:show="showEdit" :title="editing?.isNew ? '新增角色模板' : `編輯模板：${editing?.id ?? ''}`">
      <template v-if="editing">
        <div class="mb-3">
          <label class="form-label small fw-semibold">模板名稱</label>
          <input v-model.trim="editing.name" type="text" class="form-control" placeholder="例如：組織財務" />
        </div>
        <div>
          <label class="form-label small fw-semibold">模板說明</label>
          <textarea v-model.trim="editing.description" class="form-control" rows="3"></textarea>
        </div>
      </template>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary" :disabled="saving" @click="showEdit = false">取消</button>
        <button type="button" class="btn btn-primary position-relative" :disabled="saving" @click="handleSave">
          <span :class="{ 'invisible': saving }">{{ editing?.isNew ? "建立" : "儲存" }}</span>
          <span v-if="saving" class="spinner-border spinner-border-sm position-absolute top-50 start-50 translate-middle" role="status"></span>
        </button>
      </template>
    </BaseModal>

    <!-- 預設權限編輯 -->
    <PermissionPicker
      v-model:show="showPicker"
      :title="`預設權限：${pickerTpl?.name ?? ''}`"
      :permissions="permissions"
      :model-value="pickerTpl?.permissionIds ?? []"
      :saving="pickerSaving"
      @save="savePermissions"
    />
  </div>
</template>

<style scoped>
/* 鎖定按鈕樣式：灰色 icon + 灰色虛線外框 + 停用點擊 */
.btn-locked,
.btn-locked:disabled {
  border: 1.5px dashed rgba(255, 255, 255, 0.28) !important;
  color: rgba(255, 255, 255, 0.4) !important;
  background-color: rgba(255, 255, 255, 0.03) !important;
  cursor: not-allowed !important;
  pointer-events: none !important;
}
</style>
