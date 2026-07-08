<script setup>
/**
 * RoleList.vue — 平台角色管理（模組 8）
 *
 * 兩區塊：
 *   系統身分角色（category=SYSTEM：BUYER/ORGANIZER/SUPER_ADMIN）— 唯讀，可展開看權限，不可改名/權限/刪除。
 *   內部人員角色（category=INTERNAL：ADMIN/CUSTOMER_SERVICE/自訂）— 可新增、改名/說明、就地編輯權限、刪除自訂。
 *
 * 權限編輯走共用 PermissionPicker（不跳頁）。組織角色模板已移至 /admin/rbac/templates。
 * API：GET /api/admin/rbac/roles、/permissions；POST/PUT/DELETE /roles、PUT /roles/{id}/permissions
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

const platformRoles = computed(() => rolesData.value?.platformRoles ?? []);
const systemRoles = computed(() => platformRoles.value.filter((r) => r.category === "SYSTEM"));
const internalRoles = computed(() => platformRoles.value.filter((r) => r.category === "INTERNAL"));
const loading = computed(() => isLoading.value && platformRoles.value.length === 0);

const fetchRoles = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) setAnnouncement("載入角色清單失敗，請稍後再試。", "danger");
  }
};
onMounted(() => {
  fetchRoles();
  // 權限清單也要在進頁時就主動抓取；否則 useCachedResource 只會在「視窗重新聚焦」時才補抓，
  // 造成剛進頁時 permissions 為空、權限設定彈窗顯示「無可用權限」的假象（並非後端慢）。
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
const openEdit = (role) => {
  editing.value = { id: role.id, name: role.name, description: role.description, isNew: false };
  showEdit.value = true;
};

const handleSave = async () => {
  if (!editing.value.name?.trim()) {
    setAnnouncement("角色名稱不可空白。", "warning");
    return;
  }
  saving.value = true;
  try {
    if (editing.value.isNew) {
      await api.post("/api/admin/rbac/roles", {
        name: editing.value.name,
        description: editing.value.description,
      });
    } else {
      await api.put(`/api/admin/rbac/roles/${editing.value.id}`, {
        name: editing.value.name,
        description: editing.value.description,
      });
    }
    await fetchRoles();
  } catch (error) {
    saving.value = false;
    if (error.response) setAnnouncement(error.response.data?.message ?? "儲存失敗，請稍後再試。", "danger");
    return;
  }
  saving.value = false;
  showEdit.value = false;
  toastSuccess(editing.value.isNew ? "角色已建立" : "角色已更新");
};

// ── 刪除自訂角色 ──
const handleDelete = async (role) => {
  const ok = await confirm({
    title: "刪除角色",
    message: `確定刪除「${role.name}」？此操作無法復原。`,
    confirmText: "刪除",
    variant: "danger",
  });
  if (!ok) return;
  try {
    await api.delete(`/api/admin/rbac/roles/${role.id}`);
    await fetchRoles();
    toastSuccess("角色已刪除");
  } catch (error) {
    if (error.response) setAnnouncement(error.response.data?.message ?? "刪除失敗，請稍後再試。", "danger");
  }
};

// ── 權限編輯 / 檢視（共用 PermissionPicker） ──
const showPicker = ref(false);
const pickerRole = ref(null);
const pickerSaving = ref(false);

const openPermissions = (role) => {
  pickerRole.value = role;
  showPicker.value = true;
};
const savePermissions = async (ids) => {
  if (!pickerRole.value) return;
  pickerSaving.value = true;
  try {
    await api.put(`/api/admin/rbac/roles/${pickerRole.value.id}/permissions`, { permissions: ids });
    await fetchRoles();
  } catch (error) {
    pickerSaving.value = false;
    if (error.response) setAnnouncement(error.response.data?.message ?? "更新權限失敗，請稍後再試。", "danger");
    return;
  }
  pickerSaving.value = false;
  showPicker.value = false;
  toastSuccess("角色權限已更新");
};

const ROLE_ICONS = {
  SUPER_ADMIN: "bi-shield-fill-check",
  ADMIN: "bi-person-fill-gear",
  CUSTOMER_SERVICE: "bi-headset",
  BUYER: "bi-person",
  ORGANIZER: "bi-shop",
};
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-shield-lock me-2" style="color: var(--tap-primary)"></i>權限管理</h4>
    </div>

    <RbacTabs />

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else>
      <!-- 內部人員角色 -->
      <div class="d-flex align-items-center justify-content-between mb-2">
        <div class="fw-bold">TAP 角色</div>
        <button type="button" class="btn btn-sm btn-primary fw-semibold" @click="openCreate">
          <i class="bi bi-plus-lg me-1"></i>新增內部角色
        </button>
      </div>
      <div class="row g-3 mb-4">
        <div v-for="role in internalRoles" :key="role.id" class="col-12 col-md-4">
          <div class="card border shadow-sm rounded-4 h-100">
            <div class="card-body d-flex flex-column">
              <div class="d-flex align-items-center gap-2 mb-2">
                <i class="bi fs-4" :class="ROLE_ICONS[role.code] ?? 'bi-person-badge'" style="color: var(--tap-primary)"></i>
                <div>
                  <div class="fw-bold">{{ role.name }}</div>
                  <code class="small">{{ role.code }}</code>
                </div>
              </div>
              <p class="small text-tap-secondary flex-grow-1 mb-3">{{ role.description }}</p>
              <div class="d-flex align-items-center justify-content-between">
                <span class="small text-tap-secondary">
                  <i class="bi bi-people me-1"></i>{{ role.memberCount }} 人 ·
                  <i class="bi bi-key ms-1 me-1"></i>{{ role.permissionCount }} 項權限
                </span>
                <div class="d-flex gap-2">
                  <!-- 編輯名稱說明按鈕 -->
                  <button
                    type="button"
                    class="btn btn-sm btn-icon"
                    :class="role.editable ? 'btn-outline-primary' : 'btn-locked'"
                    :disabled="!role.editable"
                    :title="role.editable ? '編輯名稱/說明' : '系統內建，不可編輯'"
                    @click="openEdit(role)"
                  >
                    <i class="bi bi-pencil-square"></i>
                  </button>
                  <!-- 權限設定按鈕（齒輪） -->
                  <button
                    type="button"
                    class="btn btn-sm btn-icon btn-outline-primary"
                    :title="role.editable ? '權限設定' : '檢視權限'"
                    @click="openPermissions(role)"
                  >
                    <i class="bi bi-gear"></i>
                  </button>
                  <!-- 刪除角色按鈕 -->
                  <button
                    type="button"
                    class="btn btn-sm btn-icon"
                    :class="role.deletable ? 'btn-outline-danger' : 'btn-locked'"
                    :disabled="!role.deletable"
                    :title="role.deletable ? '刪除角色' : '內建角色或已有成員，不可刪除'"
                    @click="handleDelete(role)"
                  >
                    <i class="bi bi-trash3"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      <!-- 系統身分角色（唯讀） -->
      <div class="fw-bold mb-2">
        系統角色
      </div>
      <div class="row g-3">
        <div v-for="role in systemRoles" :key="role.id" class="col-12 col-md-4">
          <div class="card border shadow-sm rounded-4 h-100">
            <div class="card-body d-flex flex-column">
              <div class="d-flex align-items-center gap-2 mb-2">
                <i class="bi fs-4" :class="ROLE_ICONS[role.code] ?? 'bi-person-badge'" style="color: var(--tap-text-secondary)"></i>
                <div>
                  <div class="fw-bold">{{ role.name }} <i class="bi bi-lock-fill small text-tap-secondary"></i></div>
                  <code class="small">{{ role.code }}</code>
                </div>
              </div>
              <p class="small text-tap-secondary flex-grow-1 mb-3">{{ role.description }}</p>
              <div class="d-flex align-items-center justify-content-between">
                <span class="small text-tap-secondary">
                  <i class="bi bi-people me-1"></i>{{ role.memberCount }} 人 ·
                  <i class="bi bi-key ms-1 me-1"></i>{{ role.permissionCount }} 項權限
                </span>
                <div class="d-flex gap-2">
                  <!-- 編輯名稱說明按鈕（系統角色鎖定） -->
                  <button
                    type="button"
                    class="btn btn-sm btn-icon btn-locked"
                    disabled
                    title="系統角色，不可編輯"
                  >
                    <i class="bi bi-pencil-square"></i>
                  </button>
                  <!-- 權限檢視按鈕（齒輪） -->
                  <button
                    type="button"
                    class="btn btn-sm btn-icon btn-outline-primary"
                    title="檢視權限"
                    @click="openPermissions(role)"
                  >
                    <i class="bi bi-gear"></i>
                  </button>
                  <!-- 刪除按鈕（系統角色鎖定） -->
                  <button
                    type="button"
                    class="btn btn-sm btn-icon btn-locked"
                    disabled
                    title="系統角色，不可刪除"
                  >
                    <i class="bi bi-trash3"></i>
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 新增 / 編輯名稱說明 -->
    <BaseModal v-model:show="showEdit" :title="editing?.isNew ? '新增內部角色' : `編輯角色：${editing?.id ?? ''}`">
      <template v-if="editing">
        <div class="mb-3">
          <label class="form-label small fw-semibold">角色名稱</label>
          <input v-model.trim="editing.name" type="text" class="form-control" placeholder="例如：退款專員" />
        </div>
        <div>
          <label class="form-label small fw-semibold">角色說明</label>
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

    <!-- 權限編輯 / 檢視 -->
    <PermissionPicker
      v-model:show="showPicker"
      :title="`${pickerRole?.editable ? '權限設定' : '檢視權限'}：${pickerRole?.name ?? ''}`"
      :permissions="permissions"
      :model-value="pickerRole?.permissionIds ?? []"
      :readonly="!pickerRole?.editable"
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
