<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue';
import { useRouter } from 'vue-router';
import { Modal } from 'bootstrap';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';
import { useCachedResource } from '@/composables/useCachedResource';
import { useAuthStore } from '@/stores/auth.js';
import PasswordField from '@/components/common/PasswordField.vue';

const router = useRouter();
const toast = useToast();
const { confirm } = useConfirm();
const authStore = useAuthStore();

// 帳號資訊（以後端 authProvider 為唯一真實來源，不再依賴 localStorage login_method）
const userEmail = ref('');
const loginMethod = ref('email'); // 'email' | 'google'
const isTwoFactorEnabled = ref(false);
const createdAt = ref('');

// 快取優先載入個人資料（與 Profile.vue 共用同一份 'user-profile' 快取，避免切頁閃爍）
const { data: profile, refresh: refreshProfile } = useCachedResource(
  'user-profile',
  () => axios.get('/api/user/profile').then(r => r.data)
);

// 將後端 profile 攤平成本頁所需狀態
const applyProfile = (p) => {
  if (!p) return;
  userEmail.value = p.email || '';
  loginMethod.value = p.authProvider === 'GOOGLE' ? 'google' : 'email';
  isTwoFactorEnabled.value = !!p.isTwoFactorEnabled;
  createdAt.value = p.createdAt ? String(p.createdAt).slice(0, 10) : '';
};

// 密碼變更欄位
const currentPassword = ref('');
const newPassword = ref('');
const confirmPassword = ref('');

// 驗證碼 Modal 相關
const mockVerificationCode = ref('');
const modal2faRef = ref(null);
let modal2faInstance = null;

// 刪除帳號與折疊/檢查相關狀態
const isDeleteOpen = ref(false);
const isCheckingOrg = ref(false);
const isOrgMember = ref(false);
const deletionReason = ref('');
const deletionBlockingOrgs = ref([]);
const confirmEmailInput = ref('');
const modalDeleteRef = ref(null);
let modalDeleteInstance = null;

// 訊息回饋
const successMessage = ref('');
const errorMessage = ref('');

// 登入裝置會話狀態
const activeSessions = ref([]);
const isLoadingSessions = ref(false);

const fetchSessions = async () => {
  isLoadingSessions.value = true;
  try {
    const res = await axios.get('/api/user/sessions');
    activeSessions.value = res.data || [];
  } catch (err) {
    if (err.response) {
      toast.error(err.response.data?.message || '無法取得登入裝置列表，請稍後再試');
    }
  } finally {
    isLoadingSessions.value = false;
  }
};

const parseUserAgent = (ua) => {
  if (!ua) return { os: '未知作業系統', browser: '未知瀏覽器', icon: 'bi-question-circle text-secondary' };
  let os = '未知作業系統';
  let browser = '未知瀏覽器';
  let icon = 'bi-laptop text-primary';

  const uaLower = ua.toLowerCase();
  
  if (uaLower.includes('windows')) {
    os = 'Windows';
    icon = 'bi-laptop text-primary';
  } else if (uaLower.includes('macintosh') || uaLower.includes('mac os x')) {
    os = 'macOS';
    icon = 'bi-laptop text-info';
  } else if (uaLower.includes('iphone')) {
    os = 'iPhone';
    icon = 'bi-phone text-success';
  } else if (uaLower.includes('ipad')) {
    os = 'iPad';
    icon = 'bi-tablet text-success';
  } else if (uaLower.includes('android')) {
    os = 'Android';
    icon = 'bi-phone text-warning';
  } else if (uaLower.includes('linux')) {
    os = 'Linux';
    icon = 'bi-terminal text-dark';
  }

  if (uaLower.includes('edg/')) {
    browser = 'Microsoft Edge';
  } else if (uaLower.includes('chrome') || uaLower.includes('crios')) {
    browser = 'Google Chrome';
  } else if (uaLower.includes('safari') && !uaLower.includes('chrome')) {
    browser = 'Apple Safari';
  } else if (uaLower.includes('firefox')) {
    browser = 'Mozilla Firefox';
  } else if (uaLower.includes('opera') || uaLower.includes('opr')) {
    browser = 'Opera';
  }
  
  return { os, browser, icon };
};

const formatDateTime = (dateTimeStr) => {
  if (!dateTimeStr) return '';
  const d = new Date(dateTimeStr);
  return d.toLocaleString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit',
    second: '2-digit',
    hour12: false
  });
};

const handleLogoutSession = async (session) => {
  if (session.isCurrent) {
    const ok = await confirm({
      title: '登出此裝置',
      message: '確定要登出目前使用的裝置嗎？這將會讓您立即被登出並返回登入頁面。',
      confirmText: '登出',
      variant: 'danger'
    });
    if (ok) {
      await authStore.logout('/login');
    }
  } else {
    const ok = await confirm({
      title: '登出其他裝置',
      message: '確定要登出此裝置嗎？該裝置在進行下一次操作時將需要重新登入。',
      confirmText: '登出',
      variant: 'danger'
    });
    if (ok) {
      try {
        await axios.delete(`/api/user/sessions/${session.sessionId}`);
        toast.success('已成功將該裝置登出');
        fetchSessions();
      } catch (err) {
        if (err.response) {
          toast.error(err.response.data?.message || '登出裝置失敗，請稍後再試');
        }
      }
    }
  }
};

const handleLogoutAllSessions = async () => {
  const ok = await confirm({
    title: '登出所有裝置',
    message: '確定要登出所有裝置嗎？這將會使包含目前裝置在內的所有登入狀態失效。',
    confirmText: '登出所有裝置',
    variant: 'danger'
  });
  if (ok) {
    try {
      await axios.delete('/api/user/sessions');
      toast.success('已成功登出所有裝置');
      await authStore.logout('/login');
    } catch (err) {
      if (err.response) {
        toast.error(err.response.data?.message || '登出所有裝置失敗，請稍後再試');
      }
    }
  }
};

onMounted(async () => {
  // 先以快取值立即渲染，再背景取最新資料（SWR）
  applyProfile(profile.value);
  try {
    const latest = await refreshProfile();
    applyProfile(latest);
  } catch {
    // 取資料失敗（斷線等）由 axios 攔截器於全站公告條統一提示，此處沿用快取值即可
  }

  // 獲取目前登入裝置列表
  fetchSessions();

  // 初始化 Bootstrap Modal 實例
  if (modal2faRef.value) {
    modal2faInstance = new Modal(modal2faRef.value);
  }
  if (modalDeleteRef.value) {
    modalDeleteInstance = new Modal(modalDeleteRef.value);
  }
});


// 密碼強度檢測2.0 >>newPwdField
const passwordStrength = computed(() => {
  const pwd = newPassword.value;
  if (!pwd) return { score: 0, text: '', color: '' };
  
  let score = 1; // 預設為最低分 1 (弱)

  // 1. 檢查是否符合「中」的條件：包含大小寫、數字 且 滿 8 碼
  const hasUpperCase = /[A-Z]/.test(pwd);
  const hasLowerCase = /[a-z]/.test(pwd);
  const hasNumber = /[0-9]/.test(pwd); // 新增數字判斷
  const hasLength8 = pwd.length >= 8;

  // 條件必須同時包含大小寫、數字與 8 碼
  if (hasUpperCase && hasLowerCase && hasNumber && hasLength8) {
    score = 2; // 符合中級，分數變 2
    
    // 2. 在符合中級的前提下，進一步檢查是否符合「強」：滿 12 碼
    if (pwd.length >= 12) {
      score = 3; // 符合強級，分數變 3
    }
  }
  
  // 3. 回傳對應狀態 (同步更新「弱」的提示文字)
  if (score === 2) return { score, text: '中 (密碼符合標準，增加長度可提升安全性)', color: 'text-success' };
  if (score === 3) return { score, text: '強 (密碼安全性高)', color: 'text-success' };
  
  return { score, text: '弱 (需滿 8 碼且包含英文大小寫與數字)', color: 'text-warning' };
});


// === 目前密碼即時驗證 ===
// 狀態：'default'(空白) | 'format'(格式不符，本地提示不打 API) | 'valid' | 'invalid'
// 刻意不顯示「檢查中」，避免太快回應造成閃爍。
const currentPwdState = ref('default');
let verifyTimer = null;

// 標準密碼格式：8 碼以上且含大小寫英文與數字。未通過前只給本地提示、不打 API。
const STD_PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;

const runVerifyCurrentPassword = async () => {
  const pwd = currentPassword.value;
  if (!pwd) { currentPwdState.value = 'default'; return; }
  // 格式不符：本地提示，不打 API
  if (!STD_PWD_REGEX.test(pwd)) { currentPwdState.value = 'format'; return; }
  try {
    const r = await axios.post('/api/user/password/verify', { currentPassword: pwd });
    // 防競態：回來時若輸入已變更則丟棄此結果
    if (currentPassword.value !== pwd) return;
    currentPwdState.value = r.data.valid ? 'valid' : 'invalid';
  } catch {
    currentPwdState.value = 'default'; // 驗證請求失敗不誤導使用者，保留送出時的後端把關
  }
};

const onCurrentPasswordInput = () => {
  if (verifyTimer) clearTimeout(verifyTimer);
  const pwd = currentPassword.value;
  if (!pwd) { currentPwdState.value = 'default'; return; }
  // 格式不符：即時本地提示（不打 API）
  if (!STD_PWD_REGEX.test(pwd)) { currentPwdState.value = 'format'; return; }
  // 格式 OK：先清空提示（避免閃爍），debounce 後才打 API 驗證
  currentPwdState.value = 'default';
  verifyTimer = setTimeout(runVerifyCurrentPassword, 500);
};

const onCurrentPasswordBlur = () => {
  if (verifyTimer) clearTimeout(verifyTimer);
  runVerifyCurrentPassword();
};

// 三個欄位的「狀態 + 底部提示文字」對應
const currentPwdField = computed(() => {
  switch (currentPwdState.value) {
    case 'format': return { state: 'warning', hint: '密碼需 8 碼以上，且含大小寫英文與數字' };
    case 'valid': return { state: 'success', hint: '密碼正確' };
    case 'invalid': return { state: 'error', hint: '密碼不正確' };
    default: return { state: 'default', hint: '' };
  }
});

const newPwdField = computed(() => {
  if (!newPassword.value) return { state: 'default', hint: '' };
  // 新密碼不可與目前密碼相同：即時提示（不等到送出才報錯）
  if (currentPassword.value && newPassword.value === currentPassword.value) {
    return { state: 'error', hint: '新密碼不可與目前密碼相同' };
  }
  if (passwordStrength.value.score >= 2) return { state: 'success', hint: `強度：${passwordStrength.value.text}` };
  return { state: 'warning', hint: `強度：${passwordStrength.value.text}` };
});

const confirmPwdField = computed(() => {
  if (!confirmPassword.value) return { state: 'default', hint: '' };
  if (newPassword.value === confirmPassword.value) return { state: 'success', hint: '新密碼一致' };
  return { state: 'error', hint: '兩次輸入的新密碼不相符' };
});

// 密碼變更表單驗證（目前密碼即時驗證為輔，最終仍由後端把關；已知不正確則擋下送出）
const isChangingPassword = ref(false);
const isPasswordFormValid = computed(() => {
  return (
    currentPassword.value.length > 0 &&
    currentPwdState.value !== 'invalid' &&
    currentPwdState.value !== 'format' &&
    newPassword.value.length >= 8 &&
    passwordStrength.value.score >= 2 &&
    newPassword.value === confirmPassword.value &&
    newPassword.value !== currentPassword.value
  );
});

// 密碼變更送出（呼叫真實後端 API）
const handleChangePassword = async () => {
  if (!isPasswordFormValid.value || isChangingPassword.value) return;

  isChangingPassword.value = true;
  try {
    await axios.put('/api/user/password', {
      currentPassword: currentPassword.value,
      newPassword: newPassword.value
    });

    // 清空輸入欄位
    currentPassword.value = '';
    newPassword.value = '';
    confirmPassword.value = '';

    toast.success('密碼變更成功');
  } catch (err) {
    // 斷線（無 response）由 axios 攔截器統一提示；其餘顯示後端訊息（如目前密碼不正確）
    if (err.response) {
      toast.error(err.response.data?.message || '密碼變更失敗，請稍後再試');
    }
  } finally {
    isChangingPassword.value = false;
  }
};

// 安全等級計算
const securityLevel = computed(() => {
  if (loginMethod.value === 'google') {
    return { text: '高', color: 'bg-success', description: '由 Google 兩步驟驗證保護。' };
  }
  if (isTwoFactorEnabled.value) {
    return { text: '高', color: 'bg-success', description: '您的帳號已受到雙重防護。' };
  }
  return { text: '中', color: 'bg-warning', description: '建議啟用電子郵件雙重驗證。' };
});

const isSending2faCode = ref(false);
const isConfirming2fa = ref(false);

// 處理 2FA 切換開關點擊
const handle2faToggle = async (event) => {
  const checked = event.target.checked;

  if (checked) {
    // 開啟流程：先請後端寄出設定驗證碼，成功才開啟輸入 Modal
    event.target.checked = false; // 先還原，待驗證成功才真正開啟
    mockVerificationCode.value = '';
    isSending2faCode.value = true;
    try {
      await axios.post('/api/user/2fa/send-code');
      modal2faInstance.show();
    } catch (err) {
      if (err.response) toast.error(err.response.data?.message || '驗證碼寄送失敗，請稍後再試');
    } finally {
      isSending2faCode.value = false;
    }
  } else {
    const ok = await confirm({
      title: '關閉雙重驗證',
      message: '確定要關閉登入雙重驗證嗎？這將會降低您的帳號安全性。',
      confirmText: '關閉',
      variant: 'danger'
    });
    if (ok) {
      try {
        await axios.put('/api/user/2fa', { enable: false });
        isTwoFactorEnabled.value = false;
        refreshProfile().catch(() => {});
        toast.success('雙重驗證已關閉');
      } catch (err) {
        event.target.checked = true; // 失敗還原開關
        if (err.response) toast.error(err.response.data?.message || '關閉失敗，請稍後再試');
      }
    } else {
      event.target.checked = true;
    }
  }
};

// 確認啟用 2FA (Modal 提交)：送出信箱驗證碼
const confirmEnable2fa = async () => {
  if (mockVerificationCode.value.length !== 6) {
    toast.error('請輸入 6 位數驗證碼');
    return;
  }
  if (isConfirming2fa.value) return;

  isConfirming2fa.value = true;
  try {
    await axios.put('/api/user/2fa', { enable: true, code: mockVerificationCode.value });
    isTwoFactorEnabled.value = true;
    refreshProfile().catch(() => {});
    modal2faInstance.hide();
    toast.success('雙重驗證已啟用');
  } catch (err) {
    if (err.response) toast.error(err.response.data?.message || '驗證碼不正確或已過期');
  } finally {
    isConfirming2fa.value = false;
  }
};

// 查詢刪除資格：展開折疊卡片時預先檢查，若為運行中組織的所有權人則鎖定刪除按鈕並顯示原因與組織清單
const checkDeletionEligibility = async () => {
  isCheckingOrg.value = true;
  try {
    const res = await axios.get('/api/user/profile/deletion-eligibility');
    const data = res.data?.data || res.data;
    isOrgMember.value = !data.allowed;
    deletionReason.value = data.reason || '';
    deletionBlockingOrgs.value = data.blockingOrgs || [];
  } catch (err) {
    // 查詢失敗不預先擋下，維持原行為：交由實際送出刪除時由後端把關（409 blocked）
    if (err.response) toast.error(err.response.data?.message || '無法查詢刪除資格，請稍後再試');
  } finally {
    isCheckingOrg.value = false;
  }
};

// 處理刪除帳號折疊：展開時查詢一次刪除資格（是否為運行中組織的所有權人）
const toggleDeleteCollapse = () => {
  isDeleteOpen.value = !isDeleteOpen.value;
  if (isDeleteOpen.value) {
    checkDeletionEligibility();
  }
};

// 喚起刪除帳號確認 Modal
const openDeleteModal = () => {
  confirmEmailInput.value = '';
  modalDeleteInstance.show();
};

// 執行刪除帳號 (軟刪除，串接真實後端)
const isDeleting = ref(false);
const handleDeleteAccount = async () => {
  if (confirmEmailInput.value !== userEmail.value) {
    toast.error('輸入的 Email 與目前登入的帳號不符');
    return;
  }
  if (isDeleting.value) return;

  isDeleting.value = true;
  try {
    await axios.delete('/api/user/account');
    modalDeleteInstance.hide();
    // 後端已軟刪除並撤銷所有會話；用 authStore.logout 統一清空狀態/快取並導回首頁（轉跳即回饋）
    await authStore.logout('/');
  } catch (err) {
    modalDeleteInstance.hide();
    // 409：仍綁定主辦方組織 → 切換顯示組織綁定阻擋區塊，引導前往主辦方管理
    if (err.response?.status === 409 || err.response?.data?.blocked) {
      isOrgMember.value = true;
      toast.error(err.response.data?.message || '您仍綁定主辦方組織，無法刪除帳號');
    } else if (err.response) {
      toast.error(err.response.data?.message || '帳號刪除失敗，請稍後再試');
    }
  } finally {
    isDeleting.value = false;
  }
};

onUnmounted(() => {
  if (modal2faInstance) {
    modal2faInstance.dispose();
  }
  if (modalDeleteInstance) {
    modalDeleteInstance.dispose();
  }
});
</script>

<template>
  <!-- 訊息提示回饋 -->
  <div v-if="successMessage" class="alert alert-success alert-dismissible fade show border-0 shadow-sm rounded-3 mb-4" role="alert">
    <i class="bi bi-check-circle-fill me-2"></i> {{ successMessage }}
    <button type="button" class="btn-close" @click="successMessage = ''" aria-label="Close"></button>
  </div>
  
  <div v-if="errorMessage" class="alert alert-danger alert-dismissible fade show border-0 shadow-sm rounded-3 mb-4" role="alert">
    <i class="bi bi-exclamation-triangle-fill me-2"></i> {{ errorMessage }}
    <button type="button" class="btn-close" @click="errorMessage = ''" aria-label="Close"></button>
  </div>

  <!-- 區塊 1: 帳號基本狀態卡片 -->
  <div class="card border border-light-gray rounded-4 shadow-sm mb-4" style="overflow: hidden;">
    <div class="card-body p-4">
      <h5 class="fw-bold mb-3 d-flex align-items-center gap-2">
        <i class="bi bi-info-circle text-primary"></i> 帳號狀態
      </h5>
      <div class="row g-3">
        <div class="col-md-6">
          <label class="form-label text-secondary small mb-1">登入電子郵件</label>
          <div class="fw-bold text-dark">{{ userEmail }}</div>
        </div>
        <div class="col-md-6">
          <label class="form-label text-secondary small mb-1">帳號安全等級</label>
          <div class="d-flex align-items-center gap-2">
            <span class="badge rounded-pill px-3 py-2" :class="securityLevel.color">
              {{ securityLevel.text }}
            </span>
            <span class="text-secondary small">{{ securityLevel.description }}</span>
          </div>
        </div>
        <div class="col-md-6">
          <label class="form-label text-secondary small mb-1">登入方式</label>
          <div class="d-flex align-items-center gap-2 fw-bold text-dark">
            <i v-if="loginMethod === 'google'" class="bi bi-google text-danger"></i>
            <i v-else class="bi bi-envelope text-primary"></i>
            {{ loginMethod === 'google' ? 'Google 快速登入' : '一般電子郵件' }}
          </div>
        </div>
        <div class="col-md-6">
          <label class="form-label text-secondary small mb-1">帳號註冊日期</label>
          <div class="text-secondary fw-semibold">{{ createdAt }}</div>
        </div>
      </div>
    </div>
  </div>

  <!-- 區塊 2: 雙重驗證 Email 2FA -->
  <div class="card border border-light-gray rounded-4 shadow-sm mb-4" style="overflow: hidden;">
    <div class="card-body p-4">
      <div class="d-flex align-items-start justify-content-between">
        <div class="pe-3">
          <h5 class="fw-bold mb-3 d-flex align-items-center gap-2">
            <i class="bi bi-shield-check text-primary"></i> 電子郵件雙重驗證
          </h5>
          <p class="text-secondary small mb-0" style="line-height: 1.6;">
            啟用後，每次在新的瀏覽器或裝置登入您的 TAP 帳號時，除了輸入密碼外，系統會發送一組 6 位數的一次性安全認證碼至您的信箱，輸入後才能成功登入。
          </p>
        </div>
        <!-- 一般帳號才提供本系統 2FA 開關 -->
        <div v-if="loginMethod === 'email'" class="form-check form-switch fs-4 pt-1 flex-shrink-0">
          <input
            class="form-check-input"
            type="checkbox"
            role="switch"
            id="twoFactorSwitch"
            :checked="isTwoFactorEnabled"
            :disabled="isSending2faCode"
            @change="handle2faToggle"
          />
        </div>
      </div>

      <!-- Google 帳號：安全性由 Google 兩步驟驗證保護，不提供本系統 2FA -->
      <div v-if="loginMethod === 'google'" class="mt-3 p-3 bg-light rounded-3 d-flex align-items-start gap-3">
        <i class="bi bi-google fs-5 text-danger mt-1"></i>
        <div class="small text-secondary" style="line-height: 1.6;">
          您使用 <strong class="text-dark">Google 登入</strong>，帳號安全已由 Google 帳號的兩步驟驗證保護，無須在本系統另外設定。
          建議您至 <a href="https://myaccount.google.com/security" target="_blank" rel="noopener" class="fw-semibold">Google 帳號安全設定</a> 確認已啟用兩步驟驗證。
        </div>
      </div>
    </div>
  </div>

  <!-- 區塊: 登入裝置管理 -->
  <div class="card border border-light-gray rounded-4 shadow-sm mb-4" style="overflow: hidden;">
    <div class="card-body p-4">
      <div class="d-flex align-items-center justify-content-between mb-3">
        <h5 class="fw-bold mb-0 d-flex align-items-center gap-2">
          <i class="bi bi-display text-primary"></i> 登入裝置管理
        </h5>
        <button 
          v-if="activeSessions.length > 0"
          type="button" 
          class="btn btn-outline-danger btn-sm rounded-3 fw-bold px-3"
          @click="handleLogoutAllSessions"
        >
          登出所有裝置
        </button>
      </div>
      
      <p class="text-secondary small mb-4" style="line-height: 1.6;">
        此處顯示目前正登入您 TAP 帳號的裝置列表。如果您發現有不尋常的裝置登入，可以隨時將其強制登出。
      </p>

      <!-- 載入中骨架屏 -->
      <div v-if="isLoadingSessions" class="text-center py-4">
        <div class="spinner-border spinner-border-sm text-primary me-2" role="status"></div>
        <span class="text-secondary small">正在載入登入裝置...</span>
      </div>

      <!-- 裝置列表 -->
      <div v-else-if="activeSessions.length > 0" class="d-flex flex-column gap-3">
        <div 
          v-for="session in activeSessions" 
          :key="session.sessionId" 
          class="d-flex align-items-center justify-content-between p-3 rounded-3 border border-light-gray bg-light-hover transition-all"
        >
          <div class="d-flex align-items-center gap-3">
            <div class="fs-2 px-1">
              <i :class="parseUserAgent(session.userAgent).icon"></i>
            </div>
            <div class="flex-grow-1">
              <div class="d-flex align-items-center gap-2 flex-wrap">
                <span class="fw-bold text-dark">
                  {{ parseUserAgent(session.userAgent).os }} - {{ parseUserAgent(session.userAgent).browser }}
                </span>
                <span v-if="session.isCurrent" class="badge bg-primary text-white rounded-pill px-2.5 py-1 small">
                  此裝置
                </span>
              </div>
              <div class="text-secondary small mt-1">
                <span>IP 地址：{{ session.ipAddress }}</span>
                <span class="mx-2">•</span>
                <span>首次登入：{{ formatDateTime(session.createdAt) }}</span>
              </div>
            </div>
          </div>
          <div class="flex-shrink-0 ms-3">
            <button 
              type="button" 
              class="btn btn-light btn-sm rounded-3 text-danger fw-semibold border border-light-gray bg-white shadow-sm text-nowrap"
              @click="handleLogoutSession(session)"
            >
              {{ session.isCurrent ? '登出此裝置' : '強制登出' }}
            </button>
          </div>
        </div>
      </div>

      <!-- 無裝置 -->
      <div v-else class="text-center py-4 text-secondary small">
        目前無有效的登入裝置紀錄。
      </div>
    </div>
  </div>

  <!-- 區塊 3: 變更密碼 (調整回直式排版，與個人資料卡片對齊一致) -->
  <div class="card border border-light-gray rounded-4 shadow-sm mb-4" style="overflow: hidden;">
    <div class="card-body p-4">
      <h5 class="fw-bold mb-3 d-flex align-items-center gap-2">
        <i class="bi bi-key text-primary"></i> 變更密碼
      </h5>

      <!-- 一般 Email 登入使用者：顯示變更密碼表單 -->
      <form v-if="loginMethod === 'email'" @submit.prevent="handleChangePassword">

        <!-- 目前密碼（即時驗證） -->
        <PasswordField
          id="currentPassword"
          label="目前密碼"
          placeholder="請輸入您目前的密碼"
          autocomplete="current-password"
          v-model="currentPassword"
          :state="currentPwdField.state"
          :hint="currentPwdField.hint"
          @update:modelValue="onCurrentPasswordInput"
          @blur="onCurrentPasswordBlur"
        />

        <!-- 新密碼 -->
        <PasswordField
          id="newPassword"
          label="新密碼"
          placeholder="請輸入新密碼"
          autocomplete="new-password"
          v-model="newPassword"
          :state="newPwdField.state"
          :hint="newPwdField.hint"
        />

        <!-- 確認新密碼 -->
        <PasswordField
          id="confirmPassword"
          label="確認新密碼"
          placeholder="請再次輸入新密碼"
          autocomplete="new-password"
          v-model="confirmPassword"
          :state="confirmPwdField.state"
          :hint="confirmPwdField.hint"
        />

        <!-- 密碼設定規則提示 -->
        <div class="p-3 bg-light rounded-3 mb-4">
          <div class="fw-semibold text-dark small mb-2">
            <i class="bi bi-info-circle text-primary"></i> 密碼設定規則：
          </div>
          <ul class="text-secondary small mb-0 ps-3">
            <li :class="newPassword.length >= 8 ? 'text-success fw-semibold' : ''" class="mb-1">密碼長度達 8 個字元以上</li>
            <li :class="/[a-z]/.test(newPassword) && /[A-Z]/.test(newPassword) && /[0-9]/.test(newPassword) ? 'text-success fw-semibold' : ''">需同時包含英文大小寫與數字</li>
          </ul>
        </div>

        <div class="text-end">
          <button
            type="submit"
            class="btn btn-primary px-4 py-2 rounded-3 text-white fw-bold shadow-sm w-100 w-sm-auto btn-loading-keep-width"
            :class="{ 'is-loading': isChangingPassword }"
            :disabled="!isPasswordFormValid || isChangingPassword"
          >
            <span v-if="isChangingPassword" class="spinner-border spinner-border-sm btn-loading-keep-width__spinner" role="status" aria-hidden="true"></span>
            <span class="btn-loading-keep-width__label">更新密碼</span>
          </button>
        </div>
      </form>

      <!-- Google 快速登入使用者：顯示第三方代管提示 -->
      <div v-else class="p-4 bg-light rounded-4 border-0 d-flex align-items-start gap-3">
        <div class="bg-white p-2.5 rounded-circle shadow-sm text-danger d-flex align-items-center justify-content-center flex-shrink-0">
          <i class="bi bi-google fs-4"></i>
        </div>
        <div>
          <h6 class="fw-bold text-dark mb-1">您目前使用 Google 快速登入</h6>
          <p class="text-secondary small mb-0" style="line-height: 1.6;">
            此帳號與 Google 平台安全綁定，無須在 TAP 系統建立本地密碼。
            如需變更密碼，請至 Google 帳號設定頁面處理。
          </p>
        </div>
      </div>

    </div>
  </div>

  <!-- 區塊 4: 刪除帳號 (折疊面板) -->
  <div class="card border rounded-4 shadow-sm mb-4 transition-all" 
       :class="isDeleteOpen ? 'border-danger' : 'border-light-gray'"
       style="overflow: hidden;">
    
    <!-- 點擊折疊頭部 (一行的寬高) -->
    <div class="card-header bg-white border-0 py-3 px-4 d-flex align-items-center justify-content-between cursor-pointer" 
         @click="toggleDeleteCollapse" 
         style="user-select: none;">
      <div class="d-flex align-items-center gap-2">
        <i class="bi bi-exclamation-triangle-fill text-primary fs-5"></i>
        <span class="fw-bold text-dark mb-0">刪除帳號</span>
      </div>
      <i class="bi bi-chevron-down fs-5 text-secondary transition-transform" :class="{ 'rotate-180': isDeleteOpen }"></i>
    </div>

    <!-- 展開內容區 -->
    <div v-show="isDeleteOpen" class="card-body px-4 pb-4 pt-0">
      <hr class="mt-0 mb-3 border-light-gray" />

      <!-- 情況一: 檢查 Org 身分中 (轉圈一下) -->
      <div v-if="isCheckingOrg" class="d-flex align-items-center justify-content-center py-4 gap-2">
        <span class="spinner-border spinner-border-sm text-danger" role="status" aria-hidden="true"></span>
        <span class="text-secondary small">正在檢查帳號狀態與組織綁定...</span>
      </div>

      <!-- 情況二: 檢查完畢 -->
      <div v-else>
        <!-- 情況 2A: 用戶具有 B2B 主辦方 (Org) 身分 -->
        <div v-if="isOrgMember">
          <div class="alert alert-warning border-0 rounded-3 small mb-3">
            <i class="bi bi-exclamation-triangle-fill text-warning me-1"></i>
            <strong>無法刪除此帳號</strong>：{{ deletionReason || '偵測到您目前仍是主辦方組織的所有權人，請先轉移所有權或註銷組織後方可刪除此帳號。' }}
            <ul v-if="deletionBlockingOrgs.length" class="mb-0 mt-2 ps-3">
              <li v-for="org in deletionBlockingOrgs" :key="org.orgId">{{ org.orgName }}</li>
            </ul>
          </div>
          <div class="text-end">
            <RouterLink to="/org/select" class="btn btn-primary text-white fw-bold btn-sm rounded-3 shadow-sm">
              前往主辦方帳號管理
            </RouterLink>
          </div>
        </div>

        <!-- 情況 2B: 一般用戶，允許刪除 -->
        <div v-else>
          <p class="text-secondary small mb-3" style="line-height: 1.6;">
            這將會永久註銷您的帳號。一旦帳號刪除，您在 TAP 系統中購買的所有活動票券、周邊商品訂單、會員點數及個人設定，皆會一併刪除且**無法復原**。
          </p>
          <div class="text-end">
            <button 
              type="button" 
              class="btn btn-danger px-4 py-2 rounded-3 fw-bold shadow-sm"
              @click="openDeleteModal"
            >
              刪除此帳號
            </button>
          </div>
        </div>
      </div>

    </div>
  </div>

  <!-- 彈窗 A: 2FA 模擬驗證碼確認 Modal -->
  <div class="modal fade" id="modal2fa" ref="modal2faRef" tabindex="-1" aria-labelledby="modal2faLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4 border-0 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold" id="modal2faLabel">啟用登入雙重驗證</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body p-4">
          <p class="text-secondary small mb-3">
            我們已發送一組 6 位數的安全認證碼至您的信箱：<strong>{{ userEmail }}</strong>。請於下方輸入此認證碼以啟用防護。
          </p>
          <div class="mb-3">
            <label for="verificationCode" class="form-label fw-semibold text-dark">輸入驗證碼</label>
            <input 
              v-model="mockVerificationCode"
              type="text" 
              class="form-control text-center rounded-3 fs-4 tracking-widest fw-bold" 
              id="verificationCode" 
              placeholder="------"
              maxlength="6"
              required
            />
            <div class="form-text text-center text-muted small mt-2">
              請輸入信箱收到的 6 位數驗證碼（5 分鐘內有效）
            </div>
          </div>
        </div>
        <div class="modal-footer border-0 pt-0 pb-4 px-4 gap-2">
          <button type="button" class="btn btn-secondary px-3 py-2 rounded-3 text-secondary bg-light border-0 fw-semibold" data-bs-dismiss="modal">取消</button>
          <button
            type="button"
            class="btn btn-primary px-4 py-2 rounded-3 text-white fw-bold btn-loading-keep-width"
            :class="{ 'is-loading': isConfirming2fa }"
            :disabled="mockVerificationCode.length !== 6 || isConfirming2fa"
            @click="confirmEnable2fa"
          >
            <span v-if="isConfirming2fa" class="spinner-border spinner-border-sm btn-loading-keep-width__spinner" role="status" aria-hidden="true"></span>
            <span class="btn-loading-keep-width__label">確認啟用</span>
          </button>
        </div>
      </div>
    </div>
  </div>

  <!-- 彈窗 B: 刪除帳號二次確認 Modal -->
  <div class="modal fade" id="modalDelete" ref="modalDeleteRef" tabindex="-1" aria-labelledby="modalDeleteLabel" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4 border-0 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold text-danger" id="modalDeleteLabel">警告：即將永久刪除帳號</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body p-4">
          <div class="alert alert-danger border-0 rounded-3 small mb-3">
            <i class="bi bi-exclamation-triangle-fill"></i> 請注意，這是一個不可逆的操作。刪除後您的帳號將再也無法登入，所有交易紀錄皆會註銷。
          </div>
          <p class="text-secondary small mb-3">
            請在下方輸入您的登入帳號信箱 <strong>{{ userEmail }}</strong> 來確認此刪除操作：
          </p>
          <div class="mb-3">
            <input 
              v-model="confirmEmailInput"
              type="email" 
              class="form-control rounded-3" 
              placeholder="請輸入您的 Email 信箱"
              required
            />
          </div>
        </div>
        <div class="modal-footer border-0 pt-0 pb-4 px-4 gap-2">
          <button type="button" class="btn btn-secondary px-3 py-2 rounded-3 text-secondary bg-light border-0 fw-semibold" data-bs-dismiss="modal">取消</button>
          <button
            type="button"
            class="btn btn-danger px-4 py-2 rounded-3 fw-bold btn-loading-keep-width"
            :class="{ 'is-loading': isDeleting }"
            :disabled="confirmEmailInput !== userEmail || isDeleting"
            @click="handleDeleteAccount"
          >
            <span v-if="isDeleting" class="spinner-border spinner-border-sm btn-loading-keep-width__spinner" role="status" aria-hidden="true"></span>
            <span class="btn-loading-keep-width__label">確認永久刪除</span>
          </button>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>

/* 驗證碼間距與對齊 */
.tracking-widest {
  letter-spacing: 0.35em;
}

/* 邊框顏色設定，對齊 custom.scss 中的 tap 色調 */
.border-light-gray {
  border-color: var(--tap-light-gray) !important;
}

/* 確保卡片按鈕有高級感 */
.card {
  background-color: #ffffff;
}

/* 游標效果 */
.cursor-pointer {
  cursor: pointer;
}

/* 邊框與陰影漸變效果 */
.transition-all {
  transition: border-color 0.25s ease-in-out, box-shadow 0.25s ease-in-out;
}

/* 箭頭旋轉效果 */
.transition-transform {
  transition: transform 0.2s ease-in-out;
}

.rotate-180 {
  transform: rotate(180deg);
}

.bg-light-hover {
  background-color: #f8f9fa;
  transition: background-color 0.2s ease;
}

.bg-light-hover:hover {
  background-color: #f1f3f5;
}
</style>
