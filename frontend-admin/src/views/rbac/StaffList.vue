<script setup>
/**
 * StaffList.vue — 內部人員（員工帳號）管理（權限管理 ▸ 內部人員）
 *
 * 由「使用者管理」搬入：建立員工帳號（僅 SUPER_ADMIN）與內部人員清單。
 * 角色下拉資料驅動：取 GET /api/admin/rbac/roles 的 category=INTERNAL 角色。
 * API：GET /api/admin/users/staff、POST /api/admin/users、GET /api/admin/rbac/roles
 */
import { ref, reactive, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import RbacTabs from "@/components/rbac/RbacTabs.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import BaseAvatar from "@/components/common/BaseAvatar.vue";
import BaseModal from "@/components/common/BaseModal.vue";
import { resolveUserStatus } from "@/constants/user.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";
import { useToast } from "@/composables/useToast";
import { useAuthStore } from "@/stores/auth.js";
import { usePermission } from "@/composables/usePermission";
import { useConfirm } from "@/composables/useConfirm";

const { setAnnouncement } = useSystemBanner();
const { success: toastSuccess, error: toastError } = useToast();
const authStore = useAuthStore();
const { can } = usePermission();
const { confirm } = useConfirm();

const { data: staff, isLoading, refresh } = useCachedResource(
  "admin:staff",
  () => api.get("/api/admin/users/staff").then((r) => r.data.data ?? []),
  { initial: [] }
);
const { data: rolesData, refresh: refreshRoles } = useCachedResource(
  "admin:rbac:roles",
  () => api.get("/api/admin/rbac/roles").then((r) => r.data.data ?? {}),
  { initial: { platformRoles: [], roleTemplates: [] } }
);
const internalRoles = computed(() =>
  (rolesData.value?.platformRoles ?? []).filter((r) => r.category === "INTERNAL")
);
const loading = computed(() => isLoading.value && (staff.value?.length ?? 0) === 0);

// --- 搜尋與篩選邏輯 ---
const keyword = ref("");
const filterRole = ref("");
const filterStatus = ref("");

const hasExtraFilters = computed(() => !!filterRole.value || !!filterStatus.value);
const clearExtraFilters = () => {
  filterRole.value = "";
  filterStatus.value = "";
};

const statusOf = (user) => {
  if (user.isDeleted) return "deleted";
  if (user.lockedUntil && new Date(user.lockedUntil) > new Date()) return "locked";
  if (!user.isActive) return "disabled";
  return "active";
};

// 前端過濾與資料預處理（確保 status 與 roleNames 支援排序）
const filteredStaff = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  const targetRole = internalRoles.value.find((r) => r.id === filterRole.value);
  const targetRoleName = targetRole?.name;

  return staff.value
    .map((u) => {
      const statusMeta = resolveUserStatus(u);
      return {
        ...u,
        statusLabel: statusMeta.label,
        roleNamesStr: (u.roleNames ?? []).join(", "),
      };
    })
    .filter((u) => {
      // 關鍵字搜尋：Email 或姓名
      if (kw && !u.email.toLowerCase().includes(kw) && !(u.name ?? "").toLowerCase().includes(kw)) {
        return false;
      }
      // 角色篩選
      if (filterRole.value) {
        if (!targetRoleName || !u.roleNames || !u.roleNames.includes(targetRoleName)) {
          return false;
        }
      }
      // 狀態篩選
      if (filterStatus.value && statusOf(u) !== filterStatus.value) {
        return false;
      }
      return true;
    });
});

const fetchStaff = async () => {
  try {
    await Promise.all([
      refresh(),
      refreshRoles()
    ]);
  } catch (error) {
    if (error.response) setAnnouncement("載入內部人員清單與角色失敗，請稍後再試。", "danger");
  }
};
onMounted(fetchStaff);

// --- 建立員工帳號（僅 SUPER_ADMIN）---
const canCreateStaff = computed(() => authStore.isSuperAdmin);
const showCreate = ref(false);
const creating = ref(false);
const createError = ref("");
const showPwd = ref(false);
const createForm = reactive({ email: "", name: "", password: "", roleId: "" });

const openCreate = () => {
  createForm.email = "";
  createForm.name = "";
  createForm.password = "";
  createForm.roleId = internalRoles.value[0]?.id ?? "";
  createError.value = "";
  showPwd.value = false;
  showCreate.value = true;
};

const handleCreate = async () => {
  createError.value = "";
  
  // 防呆：過濾並只取 @ 符號前的部分作為帳號字首
  const prefix = createForm.email.trim().split('@')[0];
  if (!prefix || !createForm.name.trim()) {
    createError.value = "請填寫帳號與姓名";
    return;
  }

  // 強制初始密碼也必須符合大小寫及數字規則，以防登入直接報錯
  const STD_PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
  if (!createForm.password) {
    createError.value = "請輸入初始密碼";
    return;
  }
  if (!STD_PWD_REGEX.test(createForm.password)) {
    createError.value = "初始密碼格式不符，須包含大小寫英文與數字，且至少 8 字元";
    return;
  }

  if (!createForm.roleId) {
    createError.value = "請選擇平台角色";
    return;
  }
  creating.value = true;
  try {
    await api.post("/api/admin/users", {
      email: `${prefix}@tap.com`,
      name: createForm.name.trim(),
      password: createForm.password,
      roleId: createForm.roleId,
    });
    await refresh();
  } catch (error) {
    creating.value = false;
    if (error.response) createError.value = error.response.data?.message ?? "建立失敗，請稍後再試";
    return;
  }
  creating.value = false;
  showCreate.value = false;
  toastSuccess(`員工帳號「${createForm.name.trim()}」已建立`);
};

// --- 平台角色加掛 Modal 相關狀態與邏輯 ---
const showAssign = ref(false);
const loadingDetail = ref(false);
const submittingRole = ref(false);
const selectedStaff = ref(null);
const currentStaffDetail = ref(null);
const modalRoleToAdd = ref("");

// 計算已持有和可加掛角色
const heldRoleIds = computed(() => {
  return new Set((currentStaffDetail.value?.platformRoles ?? []).map((r) => r.id));
});

const modalAssignableRoles = computed(() => {
  return internalRoles.value.filter((r) => !heldRoleIds.value.has(r.id));
});

// 開啟加掛 Modal 並載入使用者詳情
const openAssignModal = async (row) => {
  selectedStaff.value = row;
  currentStaffDetail.value = null;
  modalRoleToAdd.value = "";
  showAssign.value = true;
  loadingDetail.value = true;
  try {
    const { data } = await api.get(`/api/admin/users/${row.id}`);
    currentStaffDetail.value = data.data ?? null;
  } catch (error) {
    if (error.response) {
      toastError(error.response.data?.message ?? "載入平台角色失敗");
    }
  } finally {
    loadingDetail.value = false;
  }
};

// 執行加掛角色
const handleModalAssignRole = async () => {
  if (!modalRoleToAdd.value || !selectedStaff.value) return;
  submittingRole.value = true;
  try {
    const { data } = await api.post(`/api/admin/users/${selectedStaff.value.id}/roles`, {
      roleId: modalRoleToAdd.value
    });
    currentStaffDetail.value = data.data ?? currentStaffDetail.value;
    modalRoleToAdd.value = "";
    toastSuccess("角色已加掛");
    await refresh(); // 刷新人員列表
  } catch (error) {
    if (error.response) {
      toastError(error.response.data?.message ?? "加掛失敗，請稍後再試");
    }
  } finally {
    submittingRole.value = false;
  }
};

// 執行取下角色
const handleModalRemoveRole = async (role) => {
  if (!selectedStaff.value) return;
  const ok = await confirm({
    title: "取下角色",
    message: `確定取下「${role.name}」？此操作只影響此帳號。`,
    confirmText: "取下",
    variant: "danger",
  });
  if (!ok) return;
  
  submittingRole.value = true;
  try {
    const { data } = await api.delete(`/api/admin/users/${selectedStaff.value.id}/roles/${role.id}`);
    currentStaffDetail.value = data.data ?? currentStaffDetail.value;
    toastSuccess("角色已取下");
    await refresh(); // 刷新人員列表
  } catch (error) {
    if (error.response) {
      toastError(error.response.data?.message ?? "取下失敗，請稍後再試");
    }
  } finally {
    submittingRole.value = false;
  }
};

const resettingPassword = ref(false);
const handleResetPassword = async () => {
  if (!selectedStaff.value) return;
  const ok = await confirm({
    title: "重設員工密碼",
    message: `確定要將員工「${selectedStaff.value.name}」的密碼重設為預設密碼「Aa123456」？此操作會強制該員工於下次登入時修改密碼。`,
    confirmText: "重設",
    variant: "danger",
  });
  if (!ok) return;

  resettingPassword.value = true;
  try {
    await api.post(`/api/admin/users/${selectedStaff.value.id}/reset-password`);
    toastSuccess("密碼重設成功");
  } catch (error) {
    if (error.response) {
      toastError(error.response.data?.message ?? "重設失敗，請稍後再試");
    }
  } finally {
    resettingPassword.value = false;
  }
};

const columns = [
  { key: "name", label: "員工", sortable: true },
  { key: "roleNamesStr", label: "內部角色", sortable: true },
  { key: "statusLabel", label: "帳號狀態", sortable: true, width: "110px" },
  { key: "lastLoginAt", label: "最後登入", sortable: true, width: "170px" },
  { key: "createdAt", label: "建立時間", sortable: true, width: "170px" },
];
const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-shield-lock me-2" style="color: var(--tap-primary)"></i>權限管理</h4>
    </div>

    <RbacTabs />

    <div class="d-flex align-items-center justify-content-between mb-3">
      <div class="fw-bold">TAP 管理帳號列表</div>
      <button v-if="canCreateStaff" type="button" class="btn btn-sm btn-primary fw-semibold" @click="openCreate">
        <i class="bi bi-person-plus me-1"></i>建立員工帳號
      </button>
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      keyword-placeholder="搜尋 Email / 姓名"
      :show-date-range="false"
      :has-extra-filters="hasExtraFilters"
      @clear="clearExtraFilters"
    >
      <div class="col-6 col-md-auto">
        <select v-model="filterRole" class="form-select form-select-sm" title="內部角色">
          <option value="">全部角色</option>
          <option v-for="role in internalRoles" :key="role.id" :value="role.id">{{ role.name }}</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm" title="帳號狀態">
          <option value="">全部狀態</option>
          <option value="active">正常</option>
          <option value="locked">鎖定中</option>
          <option value="disabled">已停用</option>
          <option value="deleted">已刪除</option>
        </select>
      </div>
    </SearchBar>

    <DataTable :columns="columns" :rows="filteredStaff" :loading="loading" emptyText="尚無內部人員帳號" actions-width="120px">
      <template #cell-name="{ row }">
        <div class="d-flex align-items-center gap-2">
          <BaseAvatar :name="row.name" :size="32" />
          <div class="min-w-0">
            <div class="fw-semibold text-truncate">{{ row.name }}</div>
            <div class="small text-tap-secondary text-truncate">{{ row.email }}</div>
          </div>
        </div>
      </template>

      <template #cell-roleNamesStr="{ row }">
        <span v-for="r in row.roleNames" :key="r" class="badge bg-primary-subtle text-primary-emphasis me-1">{{ r }}</span>
        <span v-if="!row.roleNames || row.roleNames.length === 0" class="text-tap-secondary small">—</span>
      </template>

      <template #cell-statusLabel="{ row }">
        <StatusBadge :variant="resolveUserStatus(row).variant" :label="row.statusLabel" />
      </template>

      <template #cell-lastLoginAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>

      <template #actions="{ row }">
        <button
          v-if="can('USER_EDIT')"
          type="button"
          class="btn btn-sm btn-icon btn-outline-primary me-1"
          title="加掛角色"
          @click="openAssignModal(row)"
        >
          <i class="bi bi-person-badge"></i>
        </button>
        <RouterLink :to="`/admin/rbac/staff/${row.id}`" class="btn btn-sm btn-icon btn-outline-primary" title="管理角色與帳號">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>

    <!-- 建立員工帳號 Modal（僅 SUPER_ADMIN）-->
    <BaseModal v-model:show="showCreate" title="建立員工帳號">
      <div v-if="createError" class="alert alert-danger py-2 small d-flex align-items-center gap-2 mb-3">
        <i class="bi bi-exclamation-triangle-fill"></i>{{ createError }}
      </div>
      <div class="mb-3">
        <label class="form-label small fw-semibold">帳號 (Email)</label>
        <div class="input-group">
          <input v-model.trim="createForm.email" type="text" class="form-control" placeholder="員工帳號" autocomplete="off" />
          <span class="input-group-text">@tap.com</span>
        </div>
      </div>
      <div class="mb-3">
        <label class="form-label small fw-semibold">姓名</label>
        <input v-model.trim="createForm.name" type="text" class="form-control" placeholder="員工姓名" />
      </div>
      <div class="mb-3">
        <label class="form-label small fw-semibold">初始密碼</label>
        <div class="input-group">
          <input v-model="createForm.password" :type="showPwd ? 'text' : 'password'" class="form-control" placeholder="至少 8 碼" autocomplete="new-password" />
          <button type="button" class="btn btn-outline-secondary" @click="showPwd = !showPwd" :title="showPwd ? '隱藏' : '顯示'">
            <i class="bi" :class="showPwd ? 'bi-eye-slash' : 'bi-eye'"></i>
          </button>
        </div>
        <div class="form-text">員工首次登入後請自行於個人設定變更密碼。</div>
      </div>
      <div>
        <label class="form-label small fw-semibold">平台角色</label>
        <select v-model="createForm.roleId" class="form-select">
          <option v-for="role in internalRoles" :key="role.id" :value="role.id">{{ role.name }}（{{ role.code }}）</option>
        </select>
        <div v-if="internalRoles.length === 0" class="form-text text-danger">尚無可指派的內部角色，請先於「平台角色」新增。</div>
      </div>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary" :disabled="creating" @click="showCreate = false">取消</button>
        <button type="button" class="btn btn-primary position-relative" :disabled="creating" @click="handleCreate">
          <span :class="{ 'invisible': creating }">建立帳號</span>
          <span v-if="creating" class="spinner-border spinner-border-sm position-absolute top-50 start-50 translate-middle" role="status"></span>
        </button>
      </template>
    </BaseModal>

    <!-- 編輯加掛角色 Modal -->
    <BaseModal v-model:show="showAssign" :title="`帳號與權限編輯 — ${selectedStaff?.name || ''}`">
      <div v-if="loadingDetail" class="text-center py-4">
        <div class="spinner-border text-primary" role="status"></div>
      </div>
      <template v-else-if="currentStaffDetail">
        <!-- 已加掛角色 -->
        <div class="mb-4">
          <label class="form-label small fw-semibold d-block">已加掛的平台角色</label>
          <div v-if="(currentStaffDetail.platformRoles ?? []).length === 0" class="small text-tap-secondary">尚無平台角色</div>
          <div v-else class="d-flex flex-wrap gap-2">
            <span
              v-for="role in currentStaffDetail.platformRoles"
              :key="role.id"
              class="badge rounded-pill d-inline-flex align-items-center gap-1"
              :class="role.editable ? 'bg-primary-subtle text-primary-emphasis' : 'bg-light text-tap-secondary border'"
            >
              {{ role.name }}
              <button
                v-if="role.editable"
                type="button"
                class="btn btn-sm p-0 border-0 bg-transparent lh-1 text-danger"
                style="font-size: 1rem"
                :disabled="submittingRole"
                :title="`取下 ${role.name}`"
                @click="handleModalRemoveRole(role)"
              >&times;</button>
            </span>
          </div>
        </div>

        <!-- 加掛新角色 -->
        <div class="mb-3">
          <label class="form-label small fw-semibold">新增加掛角色</label>
          <div class="input-group input-group-sm">
            <select v-model="modalRoleToAdd" class="form-select">
              <option value="">＋ 選擇角色…</option>
              <option v-for="r in modalAssignableRoles" :key="r.id" :value="r.id">{{ r.name }}</option>
            </select>
            <button type="button" class="btn btn-primary" :disabled="!modalRoleToAdd || submittingRole" @click="handleModalAssignRole">加掛</button>
          </div>
          <div class="form-text mt-2">臨時加權：需要時加掛、事後取下。系統基本身分角色不可在此調整。</div>
        </div>

        <!-- 重設密碼 (僅限 SUPER_ADMIN) -->
        <div v-if="authStore.isSuperAdmin" class="border-top pt-3 mt-3">
          <label class="form-label small fw-semibold d-block mb-2 text-danger">重設員工密碼</label>
          <div class="d-flex align-items-center gap-3">
            <button
              type="button"
              class="btn btn-sm btn-outline-danger fw-semibold"
              :disabled="resettingPassword"
              @click="handleResetPassword"
            >
              <span v-if="resettingPassword" class="spinner-border spinner-border-sm me-1" role="status"></span>
              重設
            </button>
            <span class="form-text text-tap-secondary m-0">將強制該員工下次登入時變更密碼，預設密碼「Aa123456。」</span>
          </div>
        </div>
      </template>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary" @click="showAssign = false">關閉</button>
      </template>
    </BaseModal>
  </div>
</template>
