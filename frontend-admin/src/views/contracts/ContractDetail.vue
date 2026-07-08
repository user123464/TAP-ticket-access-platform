<script setup>
/**
 * ContractDetail.vue — 合約詳情（模組 5 ★ P1）
 *
 * 公版：顯示簽署當下的版本快照 .md 內容（MdPreview 渲染）。
 * 客製：顯示「線下簽約」說明 + .md 內容 + PDF 預覽/下載（Media_Files）。
 *
 * API：GET /api/admin/contracts/{id}
 */
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import dayjs from "dayjs";
import { MdPreview } from "md-editor-v3";
import "md-editor-v3/lib/preview.css";
import api from "@/plugins/axios.js";
import StatusBadge from "@/components/common/StatusBadge.vue";
import { CONTRACT_TYPE, CONTRACT_TYPE_META, CONTRACT_STATUS_META, FEE_TYPE_META, formatFee } from "@/constants/contract.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useGoBack } from "@/composables/useGoBack.js";

const { setAnnouncement } = useSystemBanner();
const { goBack } = useGoBack();

const route = useRoute();
const contractId = computed(() => route.params.contractId);

const loading = ref(true);
const detail = ref(null);

const fetchDetail = async () => {
  loading.value = true;
  try {
    const { data } = await api.get(`/api/admin/contracts/${contractId.value}`);
    // 後端統一回傳 { status:"success", data:{...} }
    detail.value = data.data ?? null;
  } catch (error) {
    detail.value = null;
    if (error.response) {
      setAnnouncement("載入合約詳情失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    loading.value = false;
  }
};

onMounted(fetchDetail);

const isCustom = computed(() => detail.value?.contractType === CONTRACT_TYPE.CUSTOM);

// 客製合約 PDF 以帶 Bearer token 的 axios 取回 blob 後在新分頁開啟（直接 <a href> 無法夾帶 JWT）
const pdfLoading = ref(false);
const openPdf = async () => {
  if (!detail.value?.pdfUrl) return;
  pdfLoading.value = true;
  try {
    const { data } = await api.get(detail.value.pdfUrl, { responseType: "blob" });
    const url = URL.createObjectURL(data);
    window.open(url, "_blank");
    setTimeout(() => URL.revokeObjectURL(url), 60000);
  } catch (error) {
    if (error.response) setAnnouncement("PDF 下載失敗，請稍後再試。", "danger");
  } finally {
    pdfLoading.value = false;
  }
};

const formatDate = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
</script>

<template>
  <div>
    <!-- 標題列 -->
    <div class="detail-header-row">
      <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="goBack('/admin/billing/contracts')">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h4 class="fw-bold">合約詳情 #{{ contractId }}</h4>
      <template v-if="detail">
        <StatusBadge :dot="false" :variant="CONTRACT_TYPE_META[detail.contractType]?.variant" :label="CONTRACT_TYPE_META[detail.contractType]?.label" />
        <StatusBadge :variant="CONTRACT_STATUS_META[detail.contractStatus]?.variant" :label="CONTRACT_STATUS_META[detail.contractStatus]?.label" />
      </template>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else-if="detail">
      <!-- 客製合約提示 -->
      <div v-if="isCustom" class="alert border-0 shadow-sm rounded-4 mb-3 d-flex align-items-center gap-2 custom-banner">
        <i class="bi bi-info-circle-fill" style="color: var(--tap-primary)"></i>
        <span>本合約為<strong>線下紙本簽約</strong>之客製合約，線上內容僅供存查，法律效力依實體合約為主。</span>
      </div>

      <div class="row g-3">
        <!-- 左：合約內容（版本快照） -->
        <div class="col-12 col-xl-8">
          <div class="card border shadow-sm rounded-4">
            <div class="card-body">
              <div class="d-flex align-items-center justify-content-between mb-3">
                <div class="fw-bold"><i class="bi bi-file-earmark-richtext me-2" style="color: var(--tap-primary)"></i>合約內容（{{ detail.version }} 版本快照）</div>
                <button v-if="isCustom && detail.pdfUrl" type="button" class="btn btn-sm btn-outline-primary" :disabled="pdfLoading" @click="openPdf">
                  <i class="bi bi-file-earmark-pdf me-1"></i>{{ pdfLoading ? "開啟中…" : "下載 PDF" }}
                </button>
              </div>
              <MdPreview :model-value="detail.contentMd" theme="dark" preview-theme="github" />
            </div>
          </div>
        </div>

        <!-- 右：簽署資訊 -->
        <div class="col-12 col-xl-4">
          <div class="card border shadow-sm rounded-4">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-vector-pen me-2" style="color: var(--tap-primary)"></i>簽署資訊</div>
              <div class="small d-flex flex-column gap-3">
                <div>
                  <div class="text-tap-secondary mb-1">簽約組織</div>
                  <RouterLink :to="`/admin/organizers/${detail.orgId}`" class="fw-semibold text-decoration-none">
                    {{ detail.orgName }} <i class="bi bi-box-arrow-up-right small"></i>
                  </RouterLink>
                </div>
                <div>
                  <div class="text-tap-secondary mb-1">費率（{{ FEE_TYPE_META[detail.feeType]?.label }}）</div>
                  <div class="fw-semibold">{{ formatFee(detail.feeType, detail.feeValue) }}</div>
                </div>
                <div>
                  <div class="text-tap-secondary mb-1">簽署時間</div>
                  <div class="fw-semibold">{{ formatDate(detail.signedAt) }}</div>
                </div>
                <div>
                  <div class="text-tap-secondary mb-1">簽署人</div>
                  <div class="fw-semibold">{{ detail.signedBy || "—" }}</div>
                </div>
                <div>
                  <div class="text-tap-secondary mb-1">到期日</div>
                  <div class="fw-semibold">{{ formatDate(detail.expiresAt) }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>

<style scoped>
.custom-banner {
  background-color: var(--tap-bg-surface);
  color: var(--tap-text-primary);
}

/* MdPreview 深色底融入卡片 */
:deep(.md-editor-preview-wrapper) {
  background-color: transparent;
}
:deep(.md-editor) {
  background-color: transparent;
}
</style>
