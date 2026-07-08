<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import MarkdownIt from 'markdown-it';
import axios from '@/plugins/axios.js';
import { useCachedResource } from '@/composables/useCachedResource';

const route = useRoute();
const orgId = computed(() => route.params.organizerId);

const md = new MarkdownIt({ html: true, linkify: true, typographer: true });

// 快取優先載入合約清單（依組織分開快取），切換頁籤／重整時先畫上次資料、背景再刷新，避免閃爍
const { data: contractsCache, refresh: refreshContracts } = useCachedResource(
  `org-contracts:${orgId.value}`,
  () => axios.get(`/api/organizer/${orgId.value}/contracts`).then(r => (Array.isArray(r.data) ? r.data : [])),
  { initial: [] }
);

const contracts = computed(() => contractsCache.value || []);
const isFirstLoading = ref(false); // 僅在「連快取都沒有」時顯示整頁轉圈
const loadError = ref(false);

// 當前生效中合約（contractStatus === 1 ACTIVE）
const activeContract = computed(() => contracts.value.find(c => c.contractStatus === 1) || null);

// 畫面主要呈現的合約：優先生效中，否則取後端預設提供的免費標準方案（皆來自後端，不造假資料）
const displayContract = computed(() =>
  activeContract.value || contracts.value.find(c => c.contractType === 0) || contracts.value[0] || null
);

// 歷史紀錄：排除「目前合約」那筆，只列真正的過往合約（已終止/已過期等）
const historyContracts = computed(() =>
  contracts.value.filter(c => c.contractId !== displayContract.value?.contractId)
);

// 合約三態（皆由後端資料推導）：
//   active   生效中（ACTIVE）
//   pending  已線上簽署、待平台核准（DRAFT 且有簽署時間）
//   unsigned 尚未簽署（DRAFT 且無簽署時間，即 KYC 尚未提交）
const contractState = computed(() => {
  const c = displayContract.value;
  if (!c) return 'none';
  if (c.contractStatus === 1) return 'active';
  return c.signedAt ? 'pending' : 'unsigned';
});

// 是否已完成簽署（active 或 pending 皆已有簽署資訊可顯示）
const hasSignInfo = computed(() => !!displayContract.value?.signedAt);

// 三態對應的徽章 / 圖示 / 卡片邊框樣式
const stateMeta = computed(() => {
  switch (contractState.value) {
    case 'active':
      return {
        badgeText: '合約生效中',
        badgeClass: 'bg-success-subtle text-success border border-success-subtle',
        orbClass: 'orb-active', orbIcon: 'bi-file-earmark-check-fill',
        cardBorder: 'border-success-subtle',
      };
    case 'pending':
      return {
        badgeText: '已簽署',
        badgeClass: 'bg-info-subtle text-info-emphasis border border-info-subtle',
        orbClass: 'orb-signed', orbIcon: 'bi-pen-fill',
        cardBorder: 'border-light-gray',
      };
    default: // unsigned
      return {
        badgeText: '尚未簽署',
        badgeClass: 'bg-light text-secondary border',
        orbClass: 'orb-pending', orbIcon: 'bi-file-earmark-text',
        cardBorder: 'border-light-gray',
      };
  }
});

const loadContracts = async () => {
  // 沒有任何快取時才顯示整頁轉圈；有快取則背景靜默刷新（SWR）
  if (contracts.value.length === 0) isFirstLoading.value = true;
  loadError.value = false;
  try {
    await refreshContracts();
  } catch (error) {
    console.error('載入合約清單失敗', error);
    // 已有快取資料就維持顯示（不打斷），只有完全沒資料時才顯示錯誤畫面
    if (contracts.value.length === 0) loadError.value = true;
  } finally {
    isFirstLoading.value = false;
  }
};

// ---- 合約條款就地展開閱讀（延遲載入：首次展開才向後端抓取 markdown 全文）----
const showTerms = ref(false);
const termsLoading = ref(false);
const termsError = ref('');
const termsRaw = ref('');
const termsLoadedType = ref(null); // 已載入的合約類型，避免重複抓取
const termsHtml = computed(() => (termsRaw.value ? md.render(termsRaw.value) : ''));

// 合約類型對應的條款文件（目前僅免費標準方案有文件，其餘 fallback 提示）
const docTypeForContract = (type) => {
  switch (type) {
    case 1: return 'contract_annual';
    case 2: return 'contract_custom';
    default: return 'contract_free';
  }
};

const fetchTerms = async () => {
  const c = displayContract.value;
  if (!c) return;
  termsLoading.value = true;
  termsError.value = '';
  try {
    const docType = docTypeForContract(c.contractType);
    const res = await axios.get(`/api/documents/${docType}.md`);
    if (res.data && res.data.success) {
      termsRaw.value = res.data.content;
      termsLoadedType.value = c.contractType;
    } else {
      termsError.value = res.data?.message || '此合約條款文件尚未提供，請聯繫平台客服。';
    }
  } catch (error) {
    console.error('載入合約條款失敗', error);
    termsError.value = '載入合約條款時發生錯誤，請稍後再試。';
  } finally {
    termsLoading.value = false;
  }
};

const toggleTerms = () => {
  if (!displayContract.value) return;
  showTerms.value = !showTerms.value;
  // 首次展開、或合約類型改變時才抓取
  if (showTerms.value && termsLoadedType.value !== displayContract.value.contractType && !termsLoading.value) {
    fetchTerms();
  }
};

// 輔助格式化方法
const getContractTypeName = (type) => {
  switch (type) {
    case 0: return '免費標準方案';
    case 1: return '年費推廣方案';
    case 2: return '專屬客製合約';
    default: return '未知合約';
  }
};

const getFeeTypeName = (type, value) => {
  if (type === 0) {
    return `票價比例抽成 (${value}%)`;
  } else {
    return `每張票券固定扣款 ($${value} TWD)`;
  }
};

// 合約徽章（與主卡三態一致；DRAFT 依 signedAt 區分未簽/已簽，不再用「待平台核准」）
const getContractBadge = (c) => {
  switch (c.contractStatus) {
    case 1: return { text: '合約生效中', class: 'bg-success text-white border-success' };
    case 2: return { text: '已終止', class: 'bg-secondary text-white border-secondary' };
    case 3: return { text: '已過期', class: 'bg-light text-secondary border' };
    case 0: return c.signedAt
      ? { text: '已簽署', class: 'bg-info-subtle text-info-emphasis border border-info-subtle' }
      : { text: '尚未簽署', class: 'bg-light text-secondary border' };
    default: return { text: '未知狀態', class: 'bg-light text-secondary border' };
  }
};

const formatDate = (isoStr) => {
  if (!isoStr) return '-';
  return new Date(isoStr).toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  });
};

onMounted(() => {
  loadContracts();
});
</script>

<template>
  <div class="row g-4">
    <!-- 左側：服務合約方案 -->
    <div class="col-lg-8">

      <!-- 首次載入中（無快取時才顯示） -->
      <div v-if="isFirstLoading && !displayContract" class="card border rounded-4 shadow-sm p-5 mb-4 d-flex flex-column align-items-center justify-content-center text-secondary" style="min-height: 280px;">
        <div class="spinner-border text-primary mb-3" role="status"><span class="visually-hidden">Loading...</span></div>
        <div class="fw-semibold">正在載入合約資料...</div>
      </div>

      <!-- 載入失敗（且完全無資料時） -->
      <div v-else-if="loadError && !displayContract" class="card border border-danger-subtle rounded-4 shadow-sm p-4 mb-4 bg-danger-subtle text-danger-emphasis d-flex align-items-center justify-content-between flex-row gap-3">
        <div class="d-flex align-items-center gap-3">
          <i class="bi bi-wifi-off fs-2"></i>
          <div>
            <h5 class="fw-bold mb-1">無法載入合約資料</h5>
            <p class="small mb-0 opacity-75">連線或伺服器暫時無法回應，請稍後再試。</p>
          </div>
        </div>
        <button class="btn btn-outline-danger rounded-3 fw-semibold text-nowrap" @click="loadContracts">
          <i class="bi bi-arrow-clockwise me-1"></i>重新載入
        </button>
      </div>

      <!-- 服務合約卡（生效中 / 待核准 / 尚未簽署 共用同一骨架） -->
      <div v-else-if="displayContract" class="card border rounded-4 shadow-sm mb-4 contract-card" :class="stateMeta.cardBorder">

        <!-- 卡頭：狀態圖示 + 標題 + 狀態徽章 -->
        <div class="p-4 d-flex align-items-center gap-3">
          <div class="status-orb d-flex align-items-center justify-content-center rounded-circle flex-shrink-0" :class="stateMeta.orbClass">
            <i class="bi" :class="stateMeta.orbIcon"></i>
          </div>
          <div class="flex-grow-1">
            <h5 class="fw-bold mb-1 text-dark">目前服務合約</h5>
            <span class="small text-secondary fw-semibold">
              {{ contractState === 'unsigned' ? '平台預設方案，尚未簽署' : `合約代碼：${displayContract.contractId}` }}
            </span>
          </div>
          <span class="badge rounded-pill px-3 py-2 fw-semibold" :class="stateMeta.badgeClass">
            {{ stateMeta.badgeText }}
          </span>
        </div>

        <!-- 卡身：合約內容（兩種狀態皆顯示方案與抽成，差別在簽署資訊） -->
        <div class="px-4 pb-1">
          <div class="p-4 bg-light border border-light-gray rounded-4">
            <div class="row g-4">
              <div class="col-sm-6">
                <span class="text-secondary small fw-semibold d-block mb-1"><i class="bi bi-tags-fill me-1"></i>合約方案名稱</span>
                <strong class="text-dark fs-5">{{ getContractTypeName(displayContract.contractType) }}</strong>
              </div>
              <div class="col-sm-6">
                <span class="text-secondary small fw-semibold d-block mb-1"><i class="bi bi-cash-coin me-1"></i>票券銷售抽成</span>
                <strong class="text-primary fs-5">{{ getFeeTypeName(displayContract.feeType, displayContract.feeValue) }}</strong>
              </div>
              <div class="col-12">
                <hr class="text-muted opacity-25 my-1" />
              </div>
              <div class="col-sm-6">
                <span class="text-secondary small fw-semibold d-block mb-1"><i class="bi bi-calendar-check me-1"></i>生效日期</span>
                <template v-if="contractState === 'active'">
                  <strong class="text-dark">{{ formatDate(displayContract.validFrom) }} 起</strong>
                  <span v-if="displayContract.validTo" class="ms-1">至 {{ formatDate(displayContract.validTo) }}</span>
                  <span v-else class="badge bg-success-subtle text-success border border-success-subtle ms-2">無限期</span>
                </template>
                <span v-else-if="contractState === 'pending'" class="text-secondary">待平台核准後生效</span>
                <span v-else class="text-secondary">簽署後生效</span>
              </div>
              <div class="col-sm-6">
                <span class="text-secondary small fw-semibold d-block mb-1"><i class="bi bi-pen-fill me-1"></i>簽署代表</span>
                <template v-if="hasSignInfo">
                  <span class="badge bg-white text-secondary border px-2 py-1">{{ displayContract.signedByName || '—' }}</span>
                  <span class="small text-muted ms-2">{{ formatDate(displayContract.signedAt) }}</span>
                </template>
                <span v-else class="text-secondary">尚未簽署</span>
              </div>
            </div>
          </div>

          <!-- 尚未簽署：引導提交 KYC（中性語氣，非警告） -->
          <div v-if="contractState === 'unsigned'" class="d-flex align-items-start gap-2 small text-secondary px-1 py-3">
            <i class="bi bi-info-circle mt-1"></i>
            <span>請先至「組織設定 → 實名驗證 (KYC)」提交申請，平台審核通過後將自動為您完成簽署並使本合約生效。您可先展開下方條款內容詳閱。</span>
          </div>
          <!-- 待核准：已簽署、等待平台審核 -->
          <div v-else-if="contractState === 'pending'" class="d-flex align-items-start gap-2 small text-secondary px-1 py-3">
            <i class="bi bi-info-circle mt-1"></i>
            <span>您已完成線上簽署，平台審核通過後本合約即正式生效。審核期間方案內容與抽成費率維持不變。</span>
          </div>
        </div>

        <!-- 條款展開閱讀區（兩種狀態皆可點開閱讀） -->
        <div class="border-top border-light-gray mt-2">
          <button type="button" class="disclosure-toggle w-100 d-flex align-items-center justify-content-between px-4 py-3" @click="toggleTerms" :aria-expanded="showTerms">
            <span class="fw-semibold text-dark d-flex align-items-center gap-2">
              <i class="bi bi-file-text text-primary"></i>閱讀合約條款全文
            </span>
            <i class="bi bi-chevron-down chevron text-secondary" :class="{ 'rotate-180': showTerms }"></i>
          </button>

          <div class="terms-collapse" :class="{ open: showTerms }">
            <div class="terms-inner px-4 pb-4">
              <!-- 載入中 -->
              <div v-if="termsLoading" class="d-flex align-items-center justify-content-center text-secondary py-5">
                <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
                <span class="small">正在載入合約條款...</span>
              </div>
              <!-- 載入失敗 / 文件未提供 -->
              <div v-else-if="termsError" class="text-center text-secondary py-5 px-3">
                <p class="small mb-3">{{ termsError }}</p>
                <button class="btn btn-sm btn-outline-secondary rounded-3" @click="fetchTerms">
                  <i class="bi bi-arrow-clockwise me-1"></i>重新載入
                </button>
              </div>
              <!-- 條款內容（可捲動） -->
              <div v-else class="terms-scroll markdown-body" v-html="termsHtml"></div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右側：歷史合約清單 -->
    <div class="col-lg-4">
      <div class="card border rounded-4 shadow-sm p-4 history-card d-flex flex-column">
        <h5 class="fw-bold mb-4"><i class="bi bi-clock-history me-2 text-primary"></i>合約歷史紀錄</h5>

        <!-- 空狀態（純文字、不放壞掉感的圖示）：撐滿標題以下空間並垂直置中 -->
        <div v-if="!isFirstLoading && historyContracts.length === 0" class="text-center text-secondary flex-grow-1 d-flex align-items-center justify-content-center">
          <span class="small">尚無其他合約紀錄</span>
        </div>

        <div v-else class="list-group list-group-flush">
          <div v-for="c in historyContracts" :key="c.contractId" class="list-group-item px-0 py-3 border-bottom border-light-gray bg-transparent">
            <div class="d-flex justify-content-between align-items-start mb-2">
              <div>
                <span class="fw-bold text-dark fs-6 d-block">{{ getContractTypeName(c.contractType) }}</span>
                <span class="text-muted small" style="font-size: 0.75rem;">代碼: {{ c.contractId }}</span>
              </div>
              <span class="badge border py-1 px-2 rounded-pill fw-semibold shadow-sm" :class="getContractBadge(c).class">
                {{ getContractBadge(c).text }}
              </span>
            </div>
            <div class="small text-secondary mt-3">
              <div class="mb-1"><i class="bi bi-cash me-2"></i>{{ getFeeTypeName(c.feeType, c.feeValue) }}</div>
              <div><i class="bi bi-calendar-range me-2"></i>{{ formatDate(c.validFrom) }} 生效</div>
              <div v-if="c.signedAt" class="mt-2 text-muted" style="font-size: 0.75rem; background: #f8f9fa; padding: 4px 8px; border-radius: 4px;">
                <i class="bi bi-pen-fill me-1"></i>簽署人：{{ c.signedByName || '—' }} ({{ formatDate(c.signedAt) }})
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.border-light-gray {
  border-color: var(--tap-light-gray, #e2e8f0) !important;
}

.card {
  background-color: #ffffff;
}

/* 歷史紀錄卡：依自身內容高度顯示、不隨左欄展開而拉長 */
/* 僅寬版（≥992px 左右分欄時）保留最小高度，讓空/少資料時版面有預期感；窄版讓內容自然撐開 */
@media (min-width: 992px) {
  .history-card {
    min-height: 360px;
  }
}

.badge {
  letter-spacing: 0.5px;
}

/* 狀態圖示圓盤 */
.status-orb {
  width: 48px;
  height: 48px;
  font-size: 1.25rem;
}
.orb-active {
  background: var(--bs-success, #198754);
  color: #fff;
  box-shadow: 0 4px 10px rgba(25, 135, 84, 0.2);
}
.orb-pending {
  background: #f1f5f9;
  color: #94a3b8;
}
.orb-signed {
  background: rgba(13, 110, 253, 0.1);
  color: #0d6efd;
}

/* 條款展開按鈕 */
.disclosure-toggle {
  background: transparent;
  border: none;
  cursor: pointer;
  transition: background-color 0.15s ease-in-out;
}
.disclosure-toggle:hover {
  background-color: #f8fafc;
}
.chevron {
  transition: transform 0.25s ease-in-out;
}
.rotate-180 {
  transform: rotate(180deg);
}

/* 展開收合動畫：max-height + overflow hidden，關閉時完全裁切收乾淨（不殘留可捲動的縫隙） */
.terms-collapse {
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease-in-out;
}
.terms-collapse.open {
  max-height: 500px; /* 需 >= 內距 + 捲動區(400) + 載入/錯誤狀態高度 */
}

/* 條款內容可捲動區 */
.terms-scroll {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 0.5rem;
  color: #334155;
  font-size: 0.9rem;
  line-height: 1.8;
}
.terms-scroll::-webkit-scrollbar {
  width: 6px;
}
.terms-scroll::-webkit-scrollbar-track {
  background: transparent;
}
.terms-scroll::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.terms-scroll::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
.terms-scroll :deep(h1) {
  font-size: 1.25rem;
  font-weight: 800;
  margin-bottom: 1rem;
  color: #0f172a;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0.6rem;
}
.terms-scroll :deep(h2) {
  font-size: 1.05rem;
  font-weight: 700;
  margin-top: 1.4rem;
  margin-bottom: 0.6rem;
  color: #0f172a;
}
.terms-scroll :deep(p),
.terms-scroll :deep(ul),
.terms-scroll :deep(ol) {
  margin-bottom: 1rem;
}
.terms-scroll :deep(li) {
  margin-bottom: 0.4rem;
}
</style>
