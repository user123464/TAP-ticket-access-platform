<script setup>
/**
 * PlanManage.vue — 訂閱方案目錄
 *
 * 系統固定三個預設方案（FREE / ANNUAL / CUSTOM），不可新增/刪除/上下架，
 * 僅能「編輯」既有方案的說明文字：方案名稱、價格（限年繳，僅 ANNUAL 可改金額）、
 * 方案簡介、特色功能簡介。功能權限不在此編輯（由種子資料決定，CUSTOM 走最大權限）。
 * 保留 B2B 預覽模式切換。
 *
 * API：GET /api/admin/subscription/plans、PUT .../plans/{id}
 */
import { ref, computed, onMounted } from "vue";
import api from "@/plugins/axios.js";
import BaseModal from "@/components/common/BaseModal.vue";
import BillingTabs from "@/components/billing/BillingTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

const isPreviewMode = ref(false);

// SWR 快取
const { data: plans, isLoading, refresh } = useCachedResource(
  "admin:subscription:plans",
  () => api.get("/api/admin/subscription/plans").then((r) => r.data.data ?? []),
  { initial: [] }
);

const loading = computed(() => isLoading.value && plans.value.length === 0);

const fetchPlans = async () => {
  try {
    await refresh();
  } catch (error) {
    if (error.response) {
      setAnnouncement("載入訂閱方案失敗，請稍後再試。", "danger");
    }
  }
};

const BILLING_LABELS = { FREE: "免費", YEARLY: "年繳", CUSTOM: "客製報價" };

onMounted(fetchPlans);

// 方案卡片排序：免費 → 年繳 → 客製報價。
const BILLING_ORDER = { FREE: 0, YEARLY: 1, CUSTOM: 2 };

const sortedPlans = computed(() => {
  return [...plans.value].sort((a, b) => {
    const oa = BILLING_ORDER[a.billingCycle] ?? 99;
    const ob = BILLING_ORDER[b.billingCycle] ?? 99;
    if (oa !== ob) return oa - ob;
    return (a.price ?? 0) - (b.price ?? 0);
  });
});

// 預覽模式只顯示啟用中的方案
const displayPlans = computed(() =>
  isPreviewMode.value ? sortedPlans.value.filter((p) => p.isActive) : sortedPlans.value
);

const formatPrice = (plan) => {
  if (plan.billingCycle === "CUSTOM") return "客製報價";
  if (!plan.price) return "免費";
  return `NT$ ${plan.price.toLocaleString()}`;
};

const TARGET_FEATURE_COUNT = 8;
const getPaddedFeaturesList = (plan) => {
  const padded = [...(plan.marketingHighlights ?? [])];
  while (padded.length < TARGET_FEATURE_COUNT) padded.push("");
  return padded;
};

// --- 編輯（僅編輯既有方案說明，不可新增/刪除）---
const editing = ref(null);
const showEdit = ref(false);
const saving = ref(false);
const marketingHighlightsText = ref("");

// 僅組織年費方案（ANNUAL）可編輯金額
const canEditPrice = computed(() => editing.value?.id === "ANNUAL");

const openEdit = (plan) => {
  editing.value = {
    id: plan.id,
    name: plan.name,
    billingCycle: plan.billingCycle,
    price: plan.price,
    description: plan.description,
    // 功能權限不在 UI 編輯，原樣保留帶回後端，避免清空 CUSTOM 的最大權限
    systemFeatures: [...(plan.systemFeatures ?? [])],
  };
  marketingHighlightsText.value = (plan.marketingHighlights ?? []).join("\n");
  showEdit.value = true;
};

const handleSave = async () => {
  saving.value = true;
  const payload = {
    id: editing.value.id,
    name: editing.value.name,
    description: editing.value.description,
    systemFeatures: editing.value.systemFeatures, // pass-through，保留既有功能權限
    marketingHighlights: marketingHighlightsText.value
      .split("\n")
      .map((s) => s.trim())
      .filter(Boolean),
  };
  // 僅年費方案帶價格，免費/客製方案金額固定不送
  if (canEditPrice.value) payload.price = editing.value.price;

  try {
    await api.put(`/api/admin/subscription/plans/${payload.id}`, payload);
    await fetchPlans();
    toast.success("方案已更新");
    showEdit.value = false;
  } catch (error) {
    if (error.response) {
      toast.error(error.response.data?.message ?? "儲存失敗");
    }
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-box-seam me-2" style="color: var(--tap-primary)"></i>訂閱與合約</h4>
      <button type="button" class="btn btn-sm btn-outline-secondary" @click="isPreviewMode = !isPreviewMode">
        <i class="bi" :class="isPreviewMode ? 'bi-eye-slash' : 'bi-eye'"></i>
        {{ isPreviewMode ? '切換管理模式' : '切換預覽模式' }}
      </button>
    </div>

    <BillingTabs />

    <div v-if="loading" class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>

    <div v-else class="row g-3">
      <div v-for="plan in displayPlans" :key="plan.id" class="col-12 col-md-6 col-lg-4">
        <div class="card border h-100 transition-all"
             :class="[
               isPreviewMode ? 'b2b-preview-card border-light shadow rounded-4 p-2' : 'admin-card border-secondary shadow-sm rounded-2'
             ]">
          <div class="card-body d-flex flex-column">
            <!-- Admin Mode Card Content -->
            <template v-if="!isPreviewMode">
              <div class="d-flex align-items-start justify-content-between mb-2">
                <div class="fw-bold">{{ plan.name }}</div>
              </div>
              <div class="mb-1">
                <span class="fs-4 fw-bold" style="color: var(--tap-primary)">{{ formatPrice(plan) }}</span>
                <span v-if="plan.price" class="small text-tap-secondary"> / {{ BILLING_LABELS[plan.billingCycle] }}</span>
              </div>
              <div class="small text-tap-secondary mb-3"><i class="bi bi-people me-1"></i>{{ plan.subscriberCount }} 個組織訂閱中</div>
            </template>

            <!-- B2B Preview Mode Card Content -->
            <template v-else>
              <div class="mb-1">
                <h5 class="fw-bold text-dark mb-0">{{ plan.name }}</h5>
              </div>
              <div class="fs-2 fw-bold text-primary mb-3">
                <template v-if="plan.billingCycle === 'CUSTOM'">
                  客製化計費 <span class="fs-6 fw-normal text-secondary">/ 來信洽詢</span>
                </template>
                <template v-else-if="plan.price === 0 || !plan.price">
                  TWD 0 <span class="fs-6 fw-normal text-secondary">/ 永久</span>
                </template>
                <template v-else>
                  TWD {{ plan.price.toLocaleString() }} <span class="fs-6 fw-normal text-secondary">/ 年</span>
                </template>
              </div>
              <p class="small text-secondary mb-4">{{ plan.description || '（尚無方案描述）' }}</p>
            </template>

            <!-- Feature highlights -->
            <ul class="list-unstyled small flex-grow-1 mb-3 d-flex flex-column gap-1">
              <li v-for="(f, idx) in getPaddedFeaturesList(plan)" :key="idx"
                  class="d-flex align-items-center"
                  :class="[
                    isPreviewMode ? 'text-secondary mb-2.5 gap-2' : 'text-tap-secondary gap-1',
                    { 'opacity-0': !f }
                  ]">
                <i class="bi" :class="isPreviewMode ? 'bi-check-circle-fill text-success' : 'bi-check-circle text-success'"></i>
                <span>{{ f || '&nbsp;' }}</span>
              </li>
            </ul>

            <!-- Simulated B2B Action Button (Only in Preview Mode) -->
            <div v-if="isPreviewMode" class="mb-3">
              <button v-if="plan.billingCycle === 'FREE'" class="btn btn-outline-secondary w-100 rounded-3 fw-bold text-secondary disabled bg-light text-muted" disabled>
                啟用中
              </button>
              <button v-else-if="plan.billingCycle === 'CUSTOM'" class="btn btn-outline-primary w-100 rounded-3 fw-bold">
                聯絡我們
              </button>
              <button v-else class="btn btn-primary text-white w-100 rounded-3 fw-bold shadow-sm">
                立即升級
              </button>
            </div>

            <!-- Admin Actions (Only in Admin Mode) — 僅編輯 -->
            <div v-if="!isPreviewMode" class="mt-auto">
              <button type="button" class="btn btn-sm btn-outline-primary w-100" @click="openEdit(plan)">
                <i class="bi bi-pencil-square me-1"></i>編輯說明
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 編輯 Modal（僅編輯說明文字）-->
    <BaseModal v-model:show="showEdit" size="modal-lg">
      <template #header>
        <h5 v-if="editing" class="modal-title fw-bold mb-0">編輯方案</h5>
      </template>
      <template #default>
        <div v-if="editing" class="row g-3">
          <!-- 方案名稱 -->
          <div class="col-12 col-md-7">
            <label class="form-label small fw-semibold mb-1">方案名稱</label>
            <input v-model.trim="editing.name" type="text" class="form-control form-control-sm" placeholder="例：組織年費方案" />
          </div>
          <!-- 價格（限年繳，僅 ANNUAL 可編輯）-->
          <div class="col-12 col-md-5">
            <label class="form-label small fw-semibold mb-1">價格（年繳）</label>
            <input
              v-if="canEditPrice"
              v-model.number="editing.price"
              type="number"
              min="0"
              class="form-control form-control-sm"
            />
            <input
              v-else
              :value="editing.billingCycle === 'CUSTOM' ? '客製報價' : '免費'"
              type="text"
              class="form-control form-control-sm"
              disabled
            />
          </div>

          <!-- 方案簡介 -->
          <div class="col-12">
            <label class="form-label small fw-semibold mb-1">方案簡介</label>
            <input v-model.trim="editing.description" type="text" class="form-control form-control-sm" placeholder="例如：適合中大型活動與常態辦展廠商。" />
          </div>

          <!-- 特色功能簡介 -->
          <div class="col-12">
            <label class="form-label small fw-semibold mb-1">前台展示特色功能簡介（每行一項，純文字展示）</label>
            <textarea v-model="marketingHighlightsText" class="form-control form-control-sm" rows="8" placeholder="可同時發布 5 場活動&#10;單場參與人數上限 500 人&#10;可靈活自訂的報名表單&#10;電子票券 QR Code 驗票&#10;全站 SSL 安全加密"></textarea>
          </div>
        </div>
      </template>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary modal-btn" :disabled="saving" @click="showEdit = false">取消</button>
        <button type="button" class="btn btn-primary modal-btn d-inline-flex align-items-center justify-content-center" :disabled="saving" @click="handleSave">
          <span v-if="saving" class="spinner-border spinner-border-sm" role="status"></span>
          <span v-else>儲存</span>
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<style scoped>
/* B2B Preview Styles */
.b2b-preview-card {
  background-color: #ffffff !important;
  color: #212529 !important;
  border-radius: 1rem !important; /* rounded-4 */
  border: 1px solid #dee2e6 !important;
  box-shadow: 0 0.5rem 1rem rgba(0, 0, 0, 0.05) !important;
}

.b2b-preview-card .text-secondary {
  color: #6c757d !important;
}
.b2b-preview-card .text-primary {
  color: var(--tap-primary, #e57346) !important;
}
.b2b-preview-card .btn-primary {
  background-color: var(--tap-primary, #e57346) !important;
  border-color: var(--tap-primary, #e57346) !important;
}
.b2b-preview-card .btn-outline-primary {
  color: var(--tap-primary, #e57346) !important;
  border-color: var(--tap-primary, #e57346) !important;
}
.b2b-preview-card .btn-outline-primary:hover {
  background-color: var(--tap-primary, #e57346) !important;
  color: #ffffff !important;
}

.admin-card {
  border-radius: 0.25rem !important;
}

.modal-btn {
  min-width: 85px;
}
</style>
