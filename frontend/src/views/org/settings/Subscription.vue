<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';
import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';
import axios from '@/plugins/axios.js';

const route = useRoute();
const router = useRouter();
const orgId = computed(() => route.params.organizerId);
const toast = useToast();
const { confirm } = useConfirm();
const authStore = useAuthStore();
const workspaceStore = useWorkspaceStore();

// 真實後端資料
const plans = ref([]);
const activeSub = ref(null);
const activePlan = ref('FREE'); // 'FREE' | 'ANNUAL' | 'CUSTOM' 等
const subStartDate = ref('');
const subEndDate = ref('');
const isLoading = ref(true);

// 綠界付款跳轉中狀態（避免重複點擊）
const isRedirectingToPayment = ref(false);

// 聯絡我們 Modal 狀態與欄位
const showContactModal = ref(false);
const isSubmittingContact = ref(false);
const contactForm = ref({
  name: '',
  phone: '',
  email: '',
  eventScale: '1000-5000',
  description: ''
});

// 活動規模代碼對應的可讀文字（併入 description 一起送後端，不漏資訊）
const eventScaleLabel = {
  'under-1000': '1,000 人以下',
  '1000-5000': '1,000 - 5,000 人',
  '5000-30000': '5,000 - 30,000 人',
  'above-30000': '30,000 人以上'
};

// 開啟洽詢 Modal：登入狀態下自動帶入聯絡人姓名與 Email（比照 SupportModal）
const openContactModal = () => {
  if (authStore.isLoggedIn) {
    // 排除預設佔位字「使用者」，避免帶入無意義的名稱
    if (!contactForm.value.name && authStore.userName && authStore.userName !== '使用者') {
      contactForm.value.name = authStore.userName;
    }
    if (!contactForm.value.email) contactForm.value.email = authStore.userEmail || '';
  }
  showContactModal.value = true;
};

// 處理聯絡表單送出：串接後端 /api/submissions（與技術支援共用同一收件管道）
const handleContactSubmit = async () => {
  isSubmittingContact.value = true;
  const f = contactForm.value;
  const scaleLabel = eventScaleLabel[f.eventScale] || f.eventScale;
  // submissions 端點無電話/規模欄位，folding 進需求說明，確保資訊完整
  const composedDescription =
    `【客製化計費 / 特別方案洽詢】\n` +
    `預估活動年參與人次：${scaleLabel}\n` +
    `聯絡電話：${f.phone}\n\n` +
    `${f.description || '（未填寫需求說明）'}`;

  try {
    await axios.post('/api/submissions', {
      name: f.name,
      email: f.email,
      company: workspaceStore.orgName || '',
      category: 'custom_plan',
      description: composedDescription
    });
    showContactModal.value = false;
    toast.success('諮詢需求已送出，專屬顧問將於 24 小時內與您聯繫');
    // 重置表單
    contactForm.value = {
      name: '',
      phone: '',
      email: '',
      eventScale: '1000-5000',
      description: ''
    };
  } catch (err) {
    console.error('Submit custom plan contact error:', err);
    toast.error(err.response?.data?.message || '送出失敗，請稍後再試。');
  } finally {
    isSubmittingContact.value = false;
  }
};

// 加值服務清單
const valueAddedServices = [
  { id: 'invoice', title: '代開雲端發票', icon: 'bi-receipt', desc: '平台自動代開電子發票給購票人，免除手動開立繁瑣程序，符合報稅規範。' },
  { id: 'cvs', title: '超商取票', icon: 'bi-shop', desc: '串接全台四大超商（7-11、全家等）取票系統，提供實體取票管道。' },
  { id: 'seating', title: '劃位活動票券銷售', icon: 'bi-grid-3x3-gap', desc: '支援場地視覺化劃位設定，購票人可線上選位，適合演唱會、劇院活動。' },
  { id: 'print', title: '實體票印製', icon: 'bi-printer', desc: '高品質防偽實體票券設計與印製服務，支援條碼/QR code 掃描。' },
  { id: 'devices', title: '驗票設備租借', icon: 'bi-qr-code-scan', desc: '提供現場專業 QR Code 掃描槍、出票機租借與現場技術團隊支援。' },
  { id: 'live', title: '線上 Live 直播', icon: 'bi-broadcast', desc: '整合低延遲線上串流直播技術，支援線上票專屬付費觀看管道。' }
];

// 載入訂閱與方案資料 (對接後端)
const fetchData = async () => {
  isLoading.value = true;
  try {
    // 1. 取得所有有效訂閱方案
    const plansRes = await axios.get('/api/saas/plans');
    plans.value = plansRes.data ?? [];
    console.log('[Subscription Debug] 所有載入的方案列表 (Plans):', JSON.parse(JSON.stringify(plans.value)));

    // 2. 取得目前生效的訂閱
    const subRes = await axios.get('/api/saas/subscription/active', {
      params: { organizerId: orgId.value }
    });
    activeSub.value = subRes.data;
    console.log('[Subscription Debug] 目前生效的訂閱狀態 (ActiveSub):', JSON.parse(JSON.stringify(activeSub.value)));
    if (activeSub.value) {
      activePlan.value = activeSub.value.planId;
      subStartDate.value = activeSub.value.startDate;
      subEndDate.value = activeSub.value.endDate;
    }
  } catch (err) {
    console.error('[Subscription Debug] 載入訂閱資訊失敗 error:', err);
    toast.error('無法載入訂閱資訊，請稍後再試。');
  } finally {
    isLoading.value = false;
  }
};

// 方案排序：免費 -> 年費 -> 特別方案 -> 其它自訂方案 (按年費排序)
const sortedPlans = computed(() => {
  const orderMap = { FREE: 0, ANNUAL: 1, CUSTOM: 2 };
  return [...plans.value].sort((a, b) => {
    const oa = orderMap[a.planId] !== undefined ? orderMap[a.planId] : 99;
    const ob = orderMap[b.planId] !== undefined ? orderMap[b.planId] : 99;
    if (oa !== ob) return oa - ob;
    const feeA = a.annualFee ?? 0;
    const feeB = b.annualFee ?? 0;
    return feeA - feeB;
  });
});

const getPlanFeaturesList = (plan) => {
  return plan.marketingHighlights ?? [];
};

const TARGET_FEATURE_COUNT = 8;

const getPaddedFeaturesList = (plan) => {
  const list = getPlanFeaturesList(plan);
  const padded = [...list];
  while (padded.length < TARGET_FEATURE_COUNT) {
    padded.push('');
  }
  return padded;
};


// 發起升級：向後端建立綠界付款，取回自動提交表單後跳轉綠界收銀台。
// 實際開通在綠界付款成功的 S2S callback 後才發生（付款成功才開通）。
const triggerUpgrade = async (plan) => {
  if (isRedirectingToPayment.value) return;
  isRedirectingToPayment.value = true;

  try {
    const res = await axios.post('/api/saas/subscription/checkout', {
      organizerId: orgId.value,
      planId: plan.planId
    });

    const ecpayForm = res.data?.ecpayForm ?? res.data;
    if (!ecpayForm || typeof ecpayForm !== 'string') {
      throw new Error('後端未回傳有效的綠界付款表單');
    }

    // 比照 PaymentMerch.vue：注入後端產生的自動提交表單並送出，整頁導向綠界
    const div = document.createElement('div');
    div.innerHTML = ecpayForm;
    document.body.appendChild(div);
    const formElement = div.querySelector('form');
    if (!formElement) {
      throw new Error('綠界付款表單解析失敗');
    }
    formElement.submit();
  } catch (err) {
    console.error('Create subscription checkout error:', err);
    toast.error(err.response?.data?.message || '無法建立付款，請稍後再試。');
    isRedirectingToPayment.value = false;
  }
};

// 降級為免費 (對接後端)
const handleDowngrade = async () => {
  const ok = await confirm({
    title: '變更為免費方案',
    message: '確定要將組織方案變更為免費方案嗎？\n降級後，您的活動發布額度將會受限。',
    confirmText: '確認降級',
    variant: 'danger'
  });

  if (ok) {
    try {
      const res = await axios.post('/api/saas/subscription/subscribe', {
        organizerId: orgId.value,
        planId: 'FREE',
        upgradeType: 'MANUAL'
      });

      activeSub.value = res.data;
      activePlan.value = res.data.planId;
      subStartDate.value = res.data.startDate;
      subEndDate.value = res.data.endDate;

      toast.success('已成功降級為免費方案');
    } catch (err) {
      console.error('Downgrade subscription error:', err);
      toast.error(err.response?.data?.message || '變更失敗，請稍後再試。');
    }
  }
};

const formatDate = (isoStr) => {
  if (!isoStr) return '-';
  return new Date(isoStr).toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
};

// 綠界付款完成後從後端重定向回本頁時，依 status 顯示結果並清掉 query
const handlePaymentReturn = () => {
  const status = route.query.status;
  if (!status) return;

  if (status === 'success') {
    // callback 開通有極短時序落差，fetchData 會抓到最新方案；提示以實際資料為準
    toast.success('付款成功，您的組織年費方案已開通');
  } else if (status === 'failed') {
    toast.error('付款失敗，方案未變更，請稍後再試。');
  } else if (status === 'canceled') {
    toast.info?.('已取消付款，方案未變更。') ?? toast.error('已取消付款，方案未變更。');
  }

  // 清掉 query，避免重新整理重複提示
  router.replace({ query: {} });
};

onMounted(async () => {
  await fetchData();
  handlePaymentReturn();
});
</script>

<template>
  <div class="row g-4">
    <!-- 上半部：方案比較卡片 -->
    <div class="col-12">
      <div class="text-center mb-4">
        <h4 class="fw-bold mb-2">選擇適合您活動規模的訂閱方案</h4>
        <p class="text-secondary small">所有方案皆提供全站 SSL 加密，提供最安全的售票體驗。</p>
      </div>

      <!-- 載入中 -->
      <div v-if="isLoading" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
      </div>

      <!-- 方案列表 -->
      <div v-else class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4 mb-5">
        <div v-for="plan in sortedPlans" :key="plan.planId" class="col">
          <div class="card h-100 border rounded-4 shadow-sm position-relative overflow-hidden transition-all"
               :class="{ 'border-primary border-2 shadow': activePlan === plan.planId }">
            <div v-if="activePlan === plan.planId" class="active-badge position-absolute bg-primary text-white text-center py-1 px-4 small fw-bold">
              目前方案
            </div>
            
            <div class="card-body p-4 d-flex flex-column">
              <h5 class="fw-bold text-dark mb-1">{{ plan.planName }}</h5>
              <div class="fs-2 fw-bold text-primary mb-3">
                <template v-if="plan.annualFee === null">
                  客製化計費 <span class="fs-6 fw-normal text-secondary">/ 來信洽詢</span>
                </template>
                <template v-else-if="plan.annualFee === 0">
                  TWD 0 <span class="fs-6 fw-normal text-secondary">/ 永久</span>
                </template>
                <template v-else>
                  TWD {{ plan.annualFee.toLocaleString() }} <span class="fs-6 fw-normal text-secondary">/ 年</span>
                </template>
              </div>
              <p class="small text-secondary mb-4">{{ plan.description }}</p>
              
              <ul class="list-unstyled mb-4 flex-grow-1">
                <li v-for="(feat, idx) in getPaddedFeaturesList(plan)" :key="idx" 
                    class="mb-2.5 small text-secondary d-flex align-items-center gap-2"
                    :class="{ 'opacity-0': !feat }">
                  <i class="bi bi-check-circle-fill text-success"></i>
                  <span>{{ feat || '&nbsp;' }}</span>
                </li>
              </ul>
              
              <!-- 狀態與操作按鈕 -->
              <template v-if="activePlan === plan.planId">
                <button class="btn btn-outline-secondary w-100 rounded-3 fw-bold disabled bg-light text-muted" disabled>
                  啟用中 <span v-if="plan.planId === 'ANNUAL' && subEndDate">(至 {{ formatDate(subEndDate) }})</span>
                </button>
              </template>
              <template v-else>
                <!-- 免費方案按鈕 -->
                <button v-if="plan.planId === 'FREE'" @click="handleDowngrade" class="btn btn-outline-secondary w-100 rounded-3 fw-bold text-secondary">
                  切換為免費方案
                </button>
                <!-- 特別方案 / 客製化方案按鈕 -->
                <button v-else-if="plan.planId === 'CUSTOM' || plan.annualFee === null" @click="openContactModal" class="btn btn-outline-primary w-100 rounded-3 fw-bold">
                  聯絡我們
                </button>
                <!-- 其它升級/付費方案按鈕 -->
                <button v-else @click="triggerUpgrade(plan)" :disabled="isRedirectingToPayment" class="btn btn-primary text-white w-100 rounded-3 fw-bold shadow-sm">
                  <span v-if="isRedirectingToPayment" class="spinner-border spinner-border-sm me-2" role="status"></span>
                  立即升級
                </button>
              </template>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 下半部：加值服務介紹 -->
    <div class="col-12">
      <hr class="my-5 text-muted opacity-25" />
      
      <div class="text-center mb-4">
        <h4 class="fw-bold mb-2">主辦單位專屬加值服務</h4>
        <p class="text-secondary small">我們提供全方位活動配套服務，助您辦活動更省時省力，歡迎隨時聯絡提出加值需求。</p>
      </div>

      <div class="row row-cols-1 row-cols-md-2 row-cols-lg-3 g-4">
        <div v-for="s in valueAddedServices" :key="s.id" class="col">
          <div class="card h-100 bg-light rounded-4 p-4 transition-all hover-service-card">
            <div class="icon-box text-primary d-flex align-items-center justify-content-center rounded-3 bg-white shadow-sm mb-3" style="width: 46px; height: 46px;">
              <i class="bi" :class="s.icon" style="font-size: 1.4rem;"></i>
            </div>
            <h6 class="fw-bold text-dark mb-2">{{ s.title }}</h6>
            <p class="small text-secondary mb-0" style="line-height: 1.6;">{{ s.desc }}</p>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 模擬聯絡我們 Modal（Bootstrap modal-dialog-scrollable：標頭/按鈕固定、欄位區可捲動，矮螢幕自動貼合） -->
  <div v-if="showContactModal" class="modal-backdrop fade show"></div>
  <div v-if="showContactModal" class="modal fade show d-block" tabindex="-1" @click.self="showContactModal = false">
    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable">
      <div class="modal-content border-0 rounded-4 shadow-lg">
        <div class="modal-header border-0 pb-2 pt-4 px-4">
          <h5 class="modal-title fw-bold"><i class="bi bi-chat-left-text-fill me-2 text-primary"></i>洽詢特別方案 / 客製化需求</h5>
          <button type="button" class="btn-close" @click="showContactModal = false" :disabled="isSubmittingContact"></button>
        </div>

        <form id="contactForm" @submit.prevent="handleContactSubmit" class="modal-body p-4">
            <p class="text-secondary small mb-4">請填寫以下資訊，我們將指派專屬顧問為您量身打造售票與現場支援方案。</p>

            <div class="mb-3">
              <label class="form-label text-secondary small fw-semibold">聯絡人姓名</label>
              <input type="text" class="form-control" v-model="contactForm.name" required placeholder="例如：陳大同" :disabled="isSubmittingContact" />
            </div>

            <div class="row g-3 mb-3">
              <div class="col-6">
                <label class="form-label text-secondary small fw-semibold">聯絡電話</label>
                <input type="tel" class="form-control" v-model="contactForm.phone" required placeholder="0912-345-678" :disabled="isSubmittingContact" />
              </div>
              <div class="col-6">
                <label class="form-label text-secondary small fw-semibold">電子信箱</label>
                <input type="email" class="form-control" v-model="contactForm.email" required placeholder="partner@example.com" :disabled="isSubmittingContact" />
              </div>
            </div>

            <div class="mb-3">
              <label class="form-label text-secondary small fw-semibold">預估活動年參與人次</label>
              <select class="form-select" v-model="contactForm.eventScale" :disabled="isSubmittingContact">
                <option value="under-1000">1,000 人以下</option>
                <option value="1000-5000">1,000 - 5,000 人</option>
                <option value="5000-30000">5,000 - 30,000 人</option>
                <option value="above-30000">30,000 人以上</option>
              </select>
            </div>

            <div class="mb-0">
              <label class="form-label text-secondary small fw-semibold">客製化需求說明</label>
              <textarea class="form-control" v-model="contactForm.description" rows="3" placeholder="例如：我們需要專屬的售票獨立伺服器、現場 20 台驗票掃描槍租借與現場技術支援..." :disabled="isSubmittingContact"></textarea>
            </div>
        </form>

        <div class="modal-footer border-0 pt-2 pb-4 px-4 gap-2 flex-nowrap">
          <button type="button" class="btn btn-light border px-3 py-2 rounded-3 fw-semibold" @click="showContactModal = false" :disabled="isSubmittingContact">取消</button>
          <button type="submit" form="contactForm" class="btn btn-primary px-4 py-2 rounded-3 text-white fw-bold shadow-sm" :disabled="isSubmittingContact">
            <span v-if="isSubmittingContact" class="spinner-border spinner-border-sm me-2" role="status"></span>
            送出洽詢需求
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-label {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.form-control, .form-select {
  border-radius: 0.5rem;
  font-size: 0.95rem;
  border-color: #ced4da;
}

.form-control:focus, .form-select:focus {
  border-color: var(--tap-primary, #e57346);
  box-shadow: 0 0 0 0.25rem rgba(229, 115, 70, 0.15);
}

.card {
  background-color: #ffffff;
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
}

.active-badge {
  top: 15px;
  right: -30px;
  transform: rotate(45deg);
  width: 140px;
  font-size: 0.75rem;
}

.hover-service-card {
  transition: transform 0.25s ease-in-out, box-shadow 0.25s ease-in-out, background-color 0.25s ease-in-out, border-color 0.25s ease-in-out;
  border: 1px solid transparent !important;
  backface-visibility: hidden;
  transform: translate3d(0, 0, 0);
  will-change: transform, box-shadow;
}

.hover-service-card:hover {
  transform: translate3d(0, -5px, 0);
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.05);
  background-color: #ffffff !important;
  border-color: var(--tap-light-gray, #e2e8f0) !important;
}

.hover-service-card:hover .icon-box {
  background-color: rgba(229, 115, 70, 0.08) !important;
}

.icon-box {
  transition: background-color 0.25s ease-in-out;
}

.modal-backdrop {
  background-color: rgba(0, 0, 0, 0.5);
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1040;
}
</style>
