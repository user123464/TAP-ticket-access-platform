<script setup>
/**
 * OrganizerDetail.vue — 組織詳情（模組 3 ★ P1）
 *
 * 區塊：組織資料 / KYC 狀態（連到審核頁）/ 成員 / 合約 / 訂閱
 * 狀態操作：暫停（status=1）/ 恢復（status=0）/ 封存（status=2）
 *
 * API：
 *   GET  /api/admin/organizers/{orgId}
 *   POST /api/admin/organizers/{orgId}/status  { status }
 */
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import StatusBadge from "@/components/common/StatusBadge.vue";
import DataTable from "@/components/common/DataTable.vue";
import BaseAvatar from "@/components/common/BaseAvatar.vue";
import { ORG_STATUS, ORG_STATUS_META } from "@/constants/organizer.js";
import { KYC_STATUS_META } from "@/constants/kyc.js";
import { CONTRACT_TYPE_META, CONTRACT_STATUS_META, formatFee } from "@/constants/contract.js";
import { useToast } from "@/composables/useToast";
import { useConfirm } from "@/composables/useConfirm";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useGoBack } from "@/composables/useGoBack.js";

const route = useRoute();
const orgId = computed(() => route.params.orgId);

const toast = useToast();
const { confirm } = useConfirm();
const { setAnnouncement } = useSystemBanner();
const { goBack } = useGoBack();

const loading = ref(true);
const detail = ref(null);

const fetchDetail = async (silent = false) => {
  if (!silent) loading.value = true;
  try {
    const { data } = await api.get(`/api/admin/organizers/${orgId.value}`);
    detail.value = data.data ?? null;
  } catch (error) {
    if (error.response) {
      if (!silent) detail.value = null;
      setAnnouncement("載入組織詳情失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    if (!silent) loading.value = false;
  }
};

onMounted(fetchDetail);

const statusMeta = computed(() => ORG_STATUS_META[detail.value?.status] ?? { label: "未知", variant: "secondary" });

// --- 狀態操作（統一走全域 useConfirm + useToast） ---
const submitting = ref(false);

const ACTIONS = {
  [ORG_STATUS.SUSPENDED]: { title: "暫停組織", variant: "danger", confirmText: "暫停", message: "暫停後該組織 B2B 後台將鎖定發布售票與提現功能，可隨時恢復。", success: "已暫停組織" },
  [ORG_STATUS.ACTIVE]: { title: "恢復組織", variant: "success", confirmText: "恢復", message: "確定恢復此組織的正常營運狀態？", success: "已恢復組織" },
  [ORG_STATUS.ARCHIVED]: { title: "封存組織", variant: "danger", confirmText: "確認封存", message: "封存代表停業/解約，組織轉為唯讀保留，不可再進行任何營運操作。此操作影響重大，請確認。", success: "已封存組織" },
};

const handleStatusChange = async (targetStatus) => {
  const cfg = ACTIONS[targetStatus];
  const ok = await confirm({
    title: cfg.title,
    message: cfg.message,
    confirmText: cfg.confirmText,
    variant: cfg.variant,
  });
  if (!ok) return;

  submitting.value = true;
  try {
    await api.post(`/api/admin/organizers/${orgId.value}/status`, { status: targetStatus });
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

// 成員 / 合約表格
const memberColumns = [
  { key: "name", label: "成員" },
  { key: "roleName", label: "角色" },
  { key: "joinedAt", label: "加入時間"},
];

const contractColumns = [
  { key: "id", label: "編號", width: "150px" },
  { key: "contractType", label: "類型"},
  { key: "fee", label: "費率"},
  { key: "expiresAt", label: "到期日" },
  { key: "contractStatus", label: "狀態"},
];

const formatDate = (t) => (t ? dayjs(t).format("YYYY-MM-DD") : "—");
</script>

<template>
  <div>
    <!-- 標題列 -->
    <div class="detail-header-row">
      <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="goBack('/admin/organizers')">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h4 class="fw-bold">組織詳情</h4>
      <StatusBadge v-if="detail" :variant="statusMeta.variant" :label="statusMeta.label" />
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="detail">
      <div class="d-flex flex-column gap-4">
        <!-- 組織資料 (1/1 滿版) -->
        <div class="card border shadow-sm rounded-4 detail-card">
          <div class="card-body">
            <div class="fw-bold mb-3"><i class="bi bi-building me-2" style="color: var(--tap-primary)"></i>組織資料</div>
            <div class="row g-3 small">
              <div class="col-6 col-md-3">
                <div class="text-tap-secondary mb-1">組織編號</div>
                <div class="fw-semibold">{{ detail.orgId }}</div>
              </div>
              <div class="col-6 col-md-5">
                <div class="text-tap-secondary mb-1">組織名稱</div>
                <div class="fw-semibold">{{ detail.name }}</div>
              </div>
              <div class="col-6 col-md-2">
                <div class="text-tap-secondary mb-1">統一編號</div>
                <div class="fw-semibold">{{ detail.taxId || "—" }}</div>
              </div>
              <div class="col-6 col-md-2">
                <div class="text-tap-secondary mb-1">建立時間</div>
                <div class="fw-semibold">{{ formatDate(detail.createdAt) }}</div>
              </div>
              <div class="col-6 col-md-3">
                <div class="text-tap-secondary mb-1">聯絡電話</div>
                <div class="fw-semibold">{{ detail.phone || "—" }}</div>
              </div>
              <div class="col-6 col-md-9">
                <div class="text-tap-secondary mb-1">通訊地址</div>
                <div class="fw-semibold">{{ detail.address || "—" }}</div>
              </div>
            </div>
          </div>
        </div>

        <!-- 功能小卡 (並列一行，各佔 1/3) -->
        <div class="row g-3">
          <!-- KYC -->
          <div class="col-12 col-md-4">
            <div class="card border shadow-sm rounded-4 h-100 detail-card">
              <div class="card-body">
                <div class="fw-bold mb-3"><i class="bi bi-patch-check me-2" style="color: var(--tap-primary)"></i>KYC 實名驗證</div>
                <div class="d-flex align-items-center justify-content-between">
                  <StatusBadge :variant="KYC_STATUS_META[detail.kycStatus]?.variant" :label="KYC_STATUS_META[detail.kycStatus]?.label ?? '未知'" />
                  <RouterLink :to="`/admin/organizers/kyc/${detail.orgId}`" class="btn btn-sm btn-outline-primary">
                    {{ detail.kycStatus === 1 ? "前往審核" : "查看詳情" }}
                  </RouterLink>
                </div>
              </div>
            </div>
          </div>

          <!-- 訂閱 -->
          <div class="col-12 col-md-4">
            <div class="card border shadow-sm rounded-4 h-100 detail-card">
              <div class="card-body">
                <div class="fw-bold mb-3"><i class="bi bi-box-seam me-2" style="color: var(--tap-primary)"></i>SaaS 訂閱</div>
                <template v-if="detail.subscription">
                  <div class="d-flex align-items-center justify-content-between mb-2">
                    <span class="fw-semibold">{{ detail.subscription.planName }}</span>
                    <StatusBadge :variant="detail.subscription.status === 'ACTIVE' ? 'success' : 'secondary'" :label="detail.subscription.status === 'ACTIVE' ? '訂閱中' : '已到期'" />
                  </div>
                  <div class="small text-tap-secondary">
                    {{ formatDate(detail.subscription.startedAt) }} ～ {{ formatDate(detail.subscription.expiresAt) }}
                  </div>
                </template>
                <div v-else class="small text-tap-secondary">未訂閱任何方案</div>
              </div>
            </div>
          </div>

          <!-- 狀態操作 -->
          <div class="col-12 col-md-4">
            <div class="card border shadow-sm rounded-4 h-100 detail-card">
              <div class="card-body">
                <div class="fw-bold mb-3"><i class="bi bi-gear me-2" style="color: var(--tap-primary)"></i>組織操作</div>
                <div class="d-grid gap-2">
                  <template v-if="detail.status === ORG_STATUS.ACTIVE">
                    <button type="button" class="btn btn-outline-warning" :disabled="submitting" @click="handleStatusChange(ORG_STATUS.SUSPENDED)">
                      <i class="bi bi-pause-circle me-2"></i>暫停組織
                    </button>
                    <button type="button" class="btn btn-outline-danger" :disabled="submitting" @click="handleStatusChange(ORG_STATUS.ARCHIVED)">
                      <i class="bi bi-archive me-2"></i>封存組織
                    </button>
                  </template>
                  <template v-else-if="detail.status === ORG_STATUS.SUSPENDED">
                    <button type="button" class="btn btn-outline-success" :disabled="submitting" @click="handleStatusChange(ORG_STATUS.ACTIVE)">
                      <i class="bi bi-play-circle me-2"></i>恢復組織
                    </button>
                    <button type="button" class="btn btn-outline-danger" :disabled="submitting" @click="handleStatusChange(ORG_STATUS.ARCHIVED)">
                      <i class="bi bi-archive me-2"></i>封存組織
                    </button>
                  </template>
                  <div v-else class="small text-tap-secondary text-center">已封存（唯讀保留）</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 成員 (1/1 滿版) -->
        <div>
          <div class="fw-bold mb-2"><i class="bi bi-people me-2" style="color: var(--tap-primary)"></i>成員</div>
          <DataTable :columns="memberColumns" :rows="detail.members ?? []" row-key="userId" :page-size="5" emptyText="尚無成員" actions-width="90px">
            <template #cell-name="{ row }">
              <div class="d-flex align-items-center gap-2">
                <BaseAvatar :name="row.name" :size="28" />
                <div class="min-w-0">
                  <div class="fw-semibold small text-truncate">{{ row.name }}</div>
                  <div class="small text-tap-secondary text-truncate">{{ row.email }}</div>
                </div>
              </div>
            </template>
            <template #cell-roleName="{ value }">
              <span class="badge rounded-pill" style="background-color: var(--tap-bg-hover)">{{ value }}</span>
            </template>
            <template #cell-joinedAt="{ value }"><span class="small">{{ formatDate(value) }}</span></template>
            <template #actions="{ row }">
              <RouterLink :to="`/admin/users/${row.userId}`" class="btn btn-sm btn-icon btn-outline-primary" title="詳細資料">
                <i class="bi bi-eye"></i>
              </RouterLink>
            </template>
          </DataTable>
        </div>

        <!-- 合約紀錄 (1/1 滿版) -->
        <div>
          <div class="fw-bold mb-2"><i class="bi bi-file-earmark-text me-2" style="color: var(--tap-primary)"></i>合約紀錄</div>
          <DataTable :columns="contractColumns" :rows="detail.contracts ?? []" :page-size="5" emptyText="尚無合約" actions-width="90px">
            <template #cell-contractType="{ value }">
              <StatusBadge :dot="false" :variant="CONTRACT_TYPE_META[value]?.variant" :label="CONTRACT_TYPE_META[value]?.label ?? '未知'" />
            </template>
            <template #cell-fee="{ row }"><span class="small">{{ formatFee(row.feeType, row.feeValue) }}</span></template>
            <template #cell-expiresAt="{ value }"><span class="small">{{ formatDate(value) }}</span></template>
            <template #cell-contractStatus="{ value }">
              <StatusBadge :variant="CONTRACT_STATUS_META[value]?.variant" :label="CONTRACT_STATUS_META[value]?.label ?? '未知'" />
            </template>
            <template #actions="{ row }">
              <RouterLink :to="`/admin/billing/contracts/${row.id}`" class="btn btn-sm btn-icon btn-outline-primary" title="詳細資料">
                <i class="bi bi-eye"></i>
              </RouterLink>
            </template>
          </DataTable>
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
</style>
