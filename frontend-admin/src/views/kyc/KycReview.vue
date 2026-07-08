<script setup>
/**
 * KycReview.vue — KYC 審核頁（模組 4 ★ P0）
 *
 * 流程：查看 kyc_data_json 認證文件 → 通過（kyc_status→2）或退件（→3 + 退件原因）。
 * 審核歷史：kyc_reviewed_by + kyc_reviewed_at。
 *
 * API：
 *   GET  /api/admin/organizers/{orgId}            （AdminOrganizerDetail）
 *   POST /api/admin/organizers/{orgId}/kyc/approve
 *   POST /api/admin/organizers/{orgId}/kyc/reject  { reason }
 *
 * 後端 AdminOrganizerDetail 已從 kyc_data_json / bank_account_info / kyc_* 欄位萃取：
 * phone/address、負責人(ownerName/ownerIdNumber)、認證文件(名稱 + 受保護下載 path)、
 * 收款帳戶(bankCode/bankName/accountNo/accountName)、審核歷史(reviewedBy/reviewedAt/rejectReason)。
 * 文件下載走受 ORGANIZER_VIEW 保護的 GET /api/admin/organizers/{orgId}/kyc/file?type=DOC|ID，
 * 以帶 JWT 的 axios 取 blob 後開新分頁（比照 ContractDetail.vue；<a href> 無法夾帶 token）。
 */
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import StatusBadge from "@/components/common/StatusBadge.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
import { KYC_STATUS, KYC_STATUS_META } from "@/constants/kyc.js";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useGoBack } from "@/composables/useGoBack.js";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();
const { goBack } = useGoBack();

const route = useRoute();
const router = useRouter();
const orgId = computed(() => route.params.orgId);

const loading = ref(true);
const detail = ref(null);

// 後端 AdminOrganizerDetail 已萃取為平面欄位（缺則顯示「—」）。
const kycData = computed(() => ({
  phone: detail.value?.phone,
  address: detail.value?.address,
  owner_name: detail.value?.ownerName,
  owner_id_number: detail.value?.ownerIdNumber,
  registration_doc_name: detail.value?.registrationDocName,
  registration_doc_url: detail.value?.registrationDocUrl,
  identity_card_name: detail.value?.identityCardName,
  identity_card_url: detail.value?.identityCardUrl,
}));

const isPending = computed(() => detail.value?.kycStatus === KYC_STATUS.PENDING);
const isApproved = computed(() => detail.value?.kycStatus === KYC_STATUS.APPROVED);
const statusMeta = computed(() => KYC_STATUS_META[detail.value?.kycStatus] ?? { label: "未知", variant: "secondary" });

// 撤銷認證防禦性鎖定：目前 runtime 未對組織狀態做售票 gating，
// 若仍有進行中(ACTIVE)活動就撤銷會造成「後台已撤銷、前台仍售票」漏洞，故鎖定撤銷鈕。
const activeEventCount = computed(() => detail.value?.activeEventCount ?? 0);
const hasActiveEvents = computed(() => activeEventCount.value > 0);

const checks = ref([false, false, false, false]);
const allChecked = computed(() => checks.value.every((c) => c));

const fetchDetail = async () => {
  loading.value = true;
  try {
    const { data } = await api.get(`/api/admin/organizers/${orgId.value}`);
    // 後端無 submittedAt，沿用 createdAt
    const d = data.data ?? null;
    detail.value = d ? { ...d, submittedAt: d.submittedAt ?? d.createdAt } : null;
  } catch (error) {
    detail.value = null;
    if (error.response) {
      setAnnouncement("載入 KYC 詳情失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    loading.value = false;
  }
};

onMounted(fetchDetail);

// --- 通過 ---
const showApproveConfirm = ref(false);
const submitting = ref(false);

const handleApprove = async () => {
  submitting.value = true;
  try {
    await api.post(`/api/admin/organizers/${orgId.value}/kyc/approve`);
    toast.success("KYC 審核已通過");
    showApproveConfirm.value = false;
    router.push("/admin/organizers/kyc");
  } catch (error) {
    if (error.response) {
      toast.error(error.response.data?.message ?? "操作失敗，請稍後再試");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    submitting.value = false;
  }
};

// --- 退件 ---
const showRejectModal = ref(false);
const rejectReason = ref("");
const rejectError = ref("");

const handleReject = async () => {
  if (!rejectReason.value.trim()) {
    rejectError.value = "請填寫退件原因（將顯示給組織方）";
    return;
  }
  submitting.value = true;
  try {
    await api.post(`/api/admin/organizers/${orgId.value}/kyc/reject`, { reason: rejectReason.value.trim() });
    toast.success("KYC 已退件");
    showRejectModal.value = false;
    router.push("/admin/organizers/kyc");
  } catch (error) {
    if (error.response) {
      rejectError.value = error.response.data?.message ?? "操作失敗，請稍後再試";
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    submitting.value = false;
  }
};

// --- 文件預覽 ---
// 文件以帶 Bearer token 的 axios 取回 blob 後在新分頁開啟（<a href> 無法夾帶 JWT，比照 ContractDetail.vue）。
const previewingUrl = ref(null); // 正在載入中的 url（用於按鈕 loading 狀態）

const openPreview = async (name, url) => {
  if (!url) return;
  previewingUrl.value = url;
  try {
    const { data } = await api.get(url, { responseType: "blob" });
    const objectUrl = URL.createObjectURL(data);
    window.open(objectUrl, "_blank");
    setTimeout(() => URL.revokeObjectURL(objectUrl), 60000);
  } catch (error) {
    if (error.response) setAnnouncement("文件載入失敗，請稍後再試。", "danger");
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    previewingUrl.value = null;
  }
};

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
</script>

<template>
  <div>
    <!-- 頁面標題 + 返回 -->
    <div class="detail-header-row">
      <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="goBack('/admin/organizers/kyc')">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h4 class="fw-bold">KYC 審核詳情</h4>
      <StatusBadge v-if="detail" :variant="statusMeta.variant" :label="statusMeta.label" />
    </div>

    <!-- Loading -->
    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="detail">
      <div class="row g-3">
        <!-- 左：組織與認證資料 -->
        <div class="col-12 col-xl-8">
          <!-- 組織基本資料 -->
          <div class="card border shadow-sm rounded-4 mb-3">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-building me-2" style="color: var(--tap-primary)"></i>組織基本資料</div>
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
                  <div class="text-tap-secondary mb-1">提交時間</div>
                  <div class="fw-semibold">{{ formatTime(detail.submittedAt) }}</div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">聯絡電話</div>
                  <div class="fw-semibold">{{ kycData.phone || "—" }}</div>
                </div>
                <div class="col-6 col-md-9">
                  <div class="text-tap-secondary mb-1">通訊地址</div>
                  <div class="fw-semibold">{{ kycData.address || "—" }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 負責人資料 -->
          <div class="card border shadow-sm rounded-4 mb-3">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-person-vcard me-2" style="color: var(--tap-primary)"></i>負責人資料</div>
              <div class="row g-3 small">
                <div class="col-6 col-md-4">
                  <div class="text-tap-secondary mb-1">負責人姓名</div>
                  <div class="fw-semibold">{{ kycData.owner_name || "—" }}</div>
                </div>
                <div class="col-6 col-md-4">
                  <div class="text-tap-secondary mb-1">身分證字號</div>
                  <div class="fw-semibold">{{ kycData.owner_id_number || "—" }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 認證文件 -->
          <div class="card border shadow-sm rounded-4 mb-3">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-file-earmark-check me-2" style="color: var(--tap-primary)"></i>認證文件</div>
              <div class="d-flex flex-column gap-2">
                <div
                  v-for="doc in [
                    { label: '商業登記文件', name: kycData.registration_doc_name, url: kycData.registration_doc_url },
                    { label: '負責人身分證件', name: kycData.identity_card_name, url: kycData.identity_card_url },
                  ]"
                  :key="doc.label"
                  class="d-flex align-items-center justify-content-between border rounded-3 p-3"
                  style="border-color: var(--tap-border) !important"
                >
                  <div class="d-flex align-items-center gap-3 min-w-0">
                    <i class="bi bi-file-earmark-richtext fs-4 text-tap-secondary"></i>
                    <div class="min-w-0">
                      <div class="small text-tap-secondary">{{ doc.label }}</div>
                      <div class="fw-semibold text-truncate">{{ doc.name || "未提供" }}</div>
                    </div>
                  </div>
                  <button
                    v-if="doc.name"
                    type="button"
                    class="btn btn-sm btn-outline-secondary text-nowrap"
                    :disabled="!doc.url || previewingUrl === doc.url"
                    @click="openPreview(doc.name, doc.url)"
                  >
                    <span v-if="previewingUrl === doc.url" class="spinner-border spinner-border-sm me-1" role="status"></span>
                    <i v-else class="bi bi-eye me-1"></i>{{ previewingUrl === doc.url ? "開啟中…" : "預覽" }}
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 收款帳戶 -->
          <div class="card border shadow-sm rounded-4">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-bank me-2" style="color: var(--tap-primary)"></i>收款帳戶</div>
              <div class="row g-3 small">
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">銀行</div>
                  <div class="fw-semibold">{{ detail.bankCode }} {{ detail.bankName }}</div>
                </div>
                <div class="col-6 col-md-4">
                  <div class="text-tap-secondary mb-1">帳號</div>
                  <div class="fw-semibold">{{ detail.accountNo || "—" }}</div>
                </div>
                <div class="col-6 col-md-5">
                  <div class="text-tap-secondary mb-1">戶名</div>
                  <div class="fw-semibold">{{ detail.accountName || "—" }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 右：審核操作 / 審核歷史 -->
        <div class="col-12 col-xl-4">
          <!-- 審核操作 -->
          <div v-if="isPending || isApproved" class="card border shadow-sm rounded-4 mb-3">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-clipboard-check me-2" style="color: var(--tap-primary)"></i>審核操作</div>
              
              <template v-if="isPending">
                <div class="mb-3">
                  <div class="fw-semibold small mb-2 text-tap-secondary">通過前請確認以下事項：</div>
                  <div class="form-check small mb-1">
                    <input class="form-check-input" type="checkbox" id="chk1" v-model="checks[0]">
                    <label class="form-check-label" for="chk1">確認「商業登記文件」統編與填寫相符</label>
                  </div>
                  <div class="form-check small mb-1">
                    <input class="form-check-input" type="checkbox" id="chk2" v-model="checks[1]">
                    <label class="form-check-label" for="chk2">確認「負責人身分證件」姓名與填寫相符</label>
                  </div>
                  <div class="form-check small mb-1">
                    <input class="form-check-input" type="checkbox" id="chk3" v-model="checks[2]">
                    <label class="form-check-label" for="chk3">確認「收款銀行」戶名與公司或負責人相符</label>
                  </div>
                  <div class="form-check small">
                    <input class="form-check-input" type="checkbox" id="chk4" v-model="checks[3]">
                    <label class="form-check-label" for="chk4">確認文件清晰且無偽造變造疑慮</label>
                  </div>
                </div>
                <div class="d-grid gap-2">
                  <button type="button" class="btn btn-success fw-bold" :disabled="!allChecked" @click="showApproveConfirm = true">
                    <i class="bi bi-check-circle me-2"></i>通過審核
                  </button>
                  <button type="button" class="btn btn-outline-danger fw-bold" @click="showRejectModal = true; rejectReason = ''; rejectError = ''">
                    <i class="bi bi-x-circle me-2"></i>退件
                  </button>
                </div>
                <div class="small text-tap-secondary mt-3">
                  通過後組織即可發布售票與提現結算；退件需填寫原因，組織方將收到通知並可重新提交。
                </div>
              </template>

              <template v-else-if="isApproved">
                <div class="d-grid gap-2">
                  <button
                    type="button"
                    class="btn btn-outline-danger fw-bold"
                    :disabled="hasActiveEvents"
                    :title="hasActiveEvents ? '請先請該組織下架所有進行中活動並確認無未結算款項後，方可撤銷' : ''"
                    @click="showRejectModal = true; rejectReason = ''; rejectError = ''"
                  >
                    <i class="bi bi-arrow-counterclockwise me-2"></i>撤銷認證 / 退回重審
                  </button>
                </div>
                <div v-if="hasActiveEvents" class="small text-danger mt-3">
                  <i class="bi bi-exclamation-triangle me-1"></i>
                  此組織尚有 {{ activeEventCount }} 個進行中活動，無法撤銷。請先請組織下架所有活動並確認無未結算款項。
                </div>
                <div v-else class="small text-tap-secondary mt-3">
                  撤銷後該組織將被標記為「暫停(SUSPENDED)」並需重新審核。請確實填寫撤銷原因。
                </div>
              </template>
            </div>
          </div>

          <!-- 審核歷史 -->
          <div class="card border shadow-sm rounded-4">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-clock-history me-2" style="color: var(--tap-primary)"></i>審核歷史</div>
              <div class="small d-flex flex-column gap-3">
                <div>
                  <div class="text-tap-secondary mb-1">提交時間</div>
                  <div class="fw-semibold">{{ formatTime(detail.submittedAt) }}</div>
                </div>
                <div>
                  <div class="text-tap-secondary mb-1">審核人員（kyc_reviewed_by）</div>
                  <div class="fw-semibold">{{ detail.reviewedBy || "尚未審核" }}</div>
                </div>
                <div>
                  <div class="text-tap-secondary mb-1">審核時間（kyc_reviewed_at）</div>
                  <div class="fw-semibold">{{ formatTime(detail.reviewedAt) }}</div>
                </div>
                <div v-if="detail.kycStatus === KYC_STATUS.REJECTED">
                  <div class="text-tap-secondary mb-1">退件原因</div>
                  <div class="fw-semibold text-danger">{{ detail.rejectReason || "—" }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>

    <!-- 通過確認 -->
    <ConfirmDialog
      v-model:show="showApproveConfirm"
      title="確認通過審核"
      :message="`確定通過「${detail?.name}」的實名驗證？通過後該組織將可發布售票與提現結算。`"
      confirmText="通過審核"
      variant="success"
      :loading="submitting"
      @confirm="handleApprove"
    />

    <!-- 退件（含原因輸入） -->
    <ConfirmDialog
      v-model:show="showRejectModal"
      title="退件"
      confirmText="確認退件"
      variant="danger"
      :loading="submitting"
      @confirm="handleReject"
    >
      <p class="small text-tap-secondary">退件原因將顯示於組織方的 B2B 後台，請具體說明需補正的項目。</p>
      <textarea
        v-model="rejectReason"
        class="form-control"
        rows="4"
        placeholder="例如：商業登記文件影像模糊，請重新上傳清晰版本"
      ></textarea>
      <div v-if="rejectError" class="small text-danger mt-2">
        <i class="bi bi-exclamation-triangle-fill me-1"></i>{{ rejectError }}
      </div>
    </ConfirmDialog>
  </div>
</template>
