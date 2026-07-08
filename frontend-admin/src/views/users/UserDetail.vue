<script setup>
/**
 * UserDetail.vue — 使用者詳情（模組 2 ★ P1）
 *
 * 區塊：個資 / 所屬組織與角色 / 登入嘗試紀錄（Session 歷史與失敗原因）
 * 帳號操作：停用/啟用（is_active）、解除鎖定（locked_until）、軟刪除（is_deleted）
 *
 * API：
 *   GET  /api/admin/users/{id}
 *   POST /api/admin/users/{id}/activate | deactivate | unlock | delete
 */
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import StatusBadge from "@/components/common/StatusBadge.vue";
import BaseAvatar from "@/components/common/BaseAvatar.vue";
import DataTable from "@/components/common/DataTable.vue";
import { AUTH_PROVIDER_META, resolveUserStatus } from "@/constants/user.js";
import { useToast } from "@/composables/useToast";
import { useConfirm } from "@/composables/useConfirm";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { usePermission } from "@/composables/usePermission";
import { useGoBack } from "@/composables/useGoBack.js";

const route = useRoute();
const userId = computed(() => route.params.userId);
// 返回目的地依來源路由：從「內部人員」進來則回 staff 頁，否則回使用者管理
const backTo = computed(() =>
  route.path.startsWith("/admin/rbac/staff") ? "/admin/rbac/staff" : "/admin/users"
);

const { goBack } = useGoBack();
const { can } = usePermission();
const toast = useToast();
const { confirm } = useConfirm();
const { setAnnouncement } = useSystemBanner();

const loading = ref(true);
const detail = ref(null);

const fetchDetail = async (silent = false) => {
  if (!silent) loading.value = true;
  try {
    const { data } = await api.get(`/api/admin/users/${userId.value}`);
    detail.value = data.data ?? null;
  } catch (error) {
    if (error.response) {
      if (!silent) detail.value = null;
      setAnnouncement("載入使用者詳情失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    if (!silent) loading.value = false;
  }
};

onMounted(fetchDetail);

const status = computed(() => (detail.value ? resolveUserStatus(detail.value) : null));
const isLocked = computed(
  () => detail.value?.lockedUntil && new Date(detail.value.lockedUntil) > new Date()
);

// --- 帳號操作（統一走全域 useConfirm + useToast） ---
const submitting = ref(false);

const ACTIONS = {
  deactivate: { title: "停用", variant: "warning", confirmText: "停用", message: "停用後該使用者將無法登入，系統將同時強制撤銷其目前所有登入狀態。", success: "已停用" },
  activate: { title: "啟用", variant: "success", confirmText: "啟用", message: "確定重新啟用此帳號？", success: "已啟用" },
  unlock: { title: "解除鎖定", variant: "primary", confirmText: "解除鎖定", message: "清除 locked_until 鎖定時間，使用者可立即重新嘗試登入。", success: "已解除鎖定" },
  delete: { title: "軟刪除", variant: "danger", confirmText: "確認刪除", message: "標記 is_deleted 軟刪除：帳號將無法登入且自列表隱藏，請確認。", success: "帳號已標記刪除" },
  restore: { title: "恢復", variant: "success", confirmText: "確認恢復", message: "解除軟刪除狀態，帳號將重新出現於列表，請留意 Email 衝突風險。", success: "已恢復" },
  "force-logout": { title: "強制登出", variant: "warning", confirmText: "強制登出", message: "撤銷該帳號所有目前登入的 JWT 會話，對方需重新登入。", success: "已強制登出" }
};

const handleAction = async (type) => {
  const cfg = ACTIONS[type];
  const ok = await confirm({
    title: cfg.title,
    message: cfg.message,
    confirmText: cfg.confirmText,
    variant: cfg.variant,
  });
  if (!ok) return;

  submitting.value = true;
  try {
    await api.post(`/api/admin/users/${userId.value}/${type}`);
    await fetchDetail(true);
    toast.success(cfg.success);
  } catch (error) {
    if (error.response) {
      toast.error(error.response.data?.message ?? "操作失敗，請稍後再試");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    submitting.value = false;
  }
};


// 登入紀錄表格
const loginColumns = [
  { key: "time", label: "時間", width: "180px" },
  { key: "ip", label: "IP 位址", width: "150px" },
  { key: "success", label: "結果", width: "100px" },
  { key: "failReason", label: "失敗原因" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
</script>

<template>
  <div>
    <!-- 標題列 -->
    <div class="detail-header-row">
      <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="goBack(backTo)">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h4 class="fw-bold">使用者詳情</h4>
      <StatusBadge v-if="status" :variant="status.variant" :label="status.label" />
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="detail">
      <div class="row g-3">
        <!-- 左：個資 + 登入紀錄 -->
        <div class="col-12 col-xl-8">
          <!-- 個資 -->
          <div class="card border shadow-sm rounded-4 mb-3 detail-card">
            <div class="card-body">
              <div class="d-flex align-items-center gap-3 mb-4">
                <BaseAvatar :name="detail.name" :size="56" />
                <div>
                  <div class="fs-5 fw-bold">{{ detail.name }}</div>
                  <div class="small text-tap-secondary">#{{ detail.id }} · {{ detail.email }}</div>
                </div>
              </div>
              <div class="row g-3 small">
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">登入方式</div>
                  <StatusBadge :dot="false" :variant="AUTH_PROVIDER_META[detail.authProvider]?.variant" :label="AUTH_PROVIDER_META[detail.authProvider]?.label ?? detail.authProvider" />
                </div>
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">Email 驗證</div>
                  <div class="fw-semibold">{{ detail.isVerified ? "已驗證" : "未驗證" }}</div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">聯絡電話</div>
                  <div class="fw-semibold">{{ detail.phone || "—" }}</div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">註冊時間</div>
                  <div class="fw-semibold">{{ formatTime(detail.createdAt) }}</div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">最後登入</div>
                  <div class="fw-semibold">{{ formatTime(detail.lastLoginAt) }}</div>
                </div>
                <div v-if="isLocked" class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">鎖定至</div>
                  <div class="fw-semibold text-warning">{{ formatTime(detail.lockedUntil) }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 登入嘗試紀錄 -->
          <div class="mb-3">
            <div class="fw-bold mb-2"><i class="bi bi-clock-history me-2" style="color: var(--tap-primary)"></i>登入嘗試紀錄</div>
            <DataTable :columns="loginColumns" :rows="detail.loginAttempts ?? []" :page-size="5" emptyText="尚無登入紀錄">
              <template #cell-time="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
              <template #cell-success="{ value }">
                <StatusBadge :variant="value ? 'success' : 'danger'" :label="value ? '成功' : '失敗'" />
              </template>
              <template #cell-failReason="{ value }"><span class="small">{{ value || "—" }}</span></template>
            </DataTable>
          </div>
        </div>

        <!-- 右：所屬組織 + 帳號操作 -->
        <div class="col-12 col-xl-4">
          <!-- 所屬組織與角色 -->
          <div class="card border shadow-sm rounded-4 mb-3 detail-card">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-building me-2" style="color: var(--tap-primary)"></i>所屬組織與角色</div>
              <div v-if="(detail.organizations ?? []).length === 0" class="small text-tap-secondary">未加入任何組織</div>
              <div v-else class="d-flex flex-column gap-2">
                <RouterLink
                  v-for="org in detail.organizations"
                  :key="org.orgId"
                  :to="`/admin/organizers/${org.orgId}`"
                  class="d-flex align-items-center justify-content-between border rounded-3 p-2 text-decoration-none org-link"
                  style="border-color: var(--tap-border) !important"
                >
                  <div class="min-w-0">
                    <div class="fw-semibold small text-truncate">{{ org.orgName }}</div>
                    <div class="small text-tap-secondary">{{ org.orgId }}</div>
                  </div>
                  <span class="badge rounded-pill" style="background-color: var(--tap-bg-hover)">{{ org.roleName }}</span>
                </RouterLink>
              </div>
            </div>
          </div>



          <!-- 帳號操作（Phase 4 按鈕級 RBAC：無 USER_EDIT 則整張卡隱藏，避免空殼） -->
          <div v-if="can('USER_EDIT')" class="card border shadow-sm rounded-4 detail-card">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-gear me-2" style="color: var(--tap-primary)"></i>帳號操作</div>
              <div class="d-grid gap-2">
                <button v-if="isLocked" type="button" class="btn btn-outline-primary" :disabled="submitting" @click="handleAction('unlock')">
                  <i class="bi bi-unlock me-2"></i>解除鎖定
                </button>
                <button v-if="detail.isActive" type="button" class="btn btn-outline-warning" :disabled="detail.isDeleted || submitting" @click="handleAction('deactivate')">
                  <i class="bi bi-pause-circle me-2"></i>停用
                </button>
                <button v-else type="button" class="btn btn-outline-success" :disabled="detail.isDeleted || submitting" @click="handleAction('activate')">
                  <i class="bi bi-play-circle me-2"></i>啟用
                </button>
                <button v-if="!detail.isDeleted" type="button" class="btn btn-outline-danger" :disabled="submitting" @click="handleAction('delete')">
                  <i class="bi bi-trash3 me-2"></i>軟刪除
                </button>
                <button v-else type="button" class="btn btn-outline-success" :disabled="submitting" @click="handleAction('restore')">
                  <i class="bi bi-arrow-counterclockwise me-2"></i>恢復
                </button>
                <button type="button" class="btn btn-outline-secondary" :disabled="submitting" @click="handleAction('force-logout')">
                  <i class="bi bi-box-arrow-right me-2"></i>強制登出
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
    <!-- 操作確認改由全域 ConfirmDialog（App.vue 掛載）+ useConfirm 處理 -->
  </div>
</template>

<style scoped>
.detail-card {
  margin-bottom: 1rem !important;
}
.admin-compact .row .card.detail-card {
  margin-bottom: 1rem !important;
}
.org-link {
  transition: background-color 0.15s ease-in-out, border-color 0.15s ease-in-out;
}
.org-link:hover {
  background-color: var(--tap-bg-hover) !important;
  border-color: var(--tap-text-secondary) !important;
}
</style>
