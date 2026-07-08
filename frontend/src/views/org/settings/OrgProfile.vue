<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { useWorkspaceStore } from '@/stores/workspace.js';
import { validateTaxId, validateTwId, validatePhone } from '@/utils/validators.js';
import { BANKS, getBankName } from '@/constants/banks.js';
import AvatarEditModal from '@/components/common/AvatarEditModal.vue';
import BaseAvatar from '@/components/common/BaseAvatar.vue';
import ContractModal from '@/components/common/ContractModal.vue';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';
import { useCachedResource } from '@/composables/useCachedResource';
import { getOrgDefaultLogo } from '@/constants/avatar.js';

const BACKEND_URL = import.meta.env.VITE_BACKEND_API || 'http://localhost:8080';

const route = useRoute();
const router = useRouter();
const orgId = computed(() => route.params.organizerId);
const workspaceStore = useWorkspaceStore();
const toast = useToast();
const { confirm } = useConfirm();

// 自持久化的組織清單取初始名稱 / Logo，讓頭像在 loadOrgData 回來前先顯示正確值，避免閃爍出預設 O 字
const cachedOrg = workspaceStore.myOrgs.find(o => o.id === orgId.value);

// 組織詳細資料狀態
const name = ref(cachedOrg?.name || '');
const taxId = ref('');
const bankCode = ref('');
const bankName = ref('');
const accountNo = ref('');
const accountName = ref('');

// kyc_data_json 內部的欄位
const logoUrl = ref(cachedOrg?.logo || '');
const phone = ref('');
const fax = ref('');
const ownerName = ref('');       // 負責人姓名
const ownerIdNumber = ref('');   // 負責人身分證字號
const registrationDocName = ref('');
const registrationDocUrl = ref('');
const identityCardName = ref('');
const identityCardUrl = ref('');

// 台灣縣市清單與通訊地址狀態
const cities = [
  '基隆市', '臺北市', '新北市', '桃園市', '新竹市', 
  '新竹縣', '苗栗縣', '臺中市', '彰化縣', '南投縣', 
  '雲林縣', '嘉義市', '嘉義縣', '臺南市', '高雄市', 
  '屏東縣', '宜蘭縣', '花蓮縣', '臺東縣', '澎湖縣', 
  '金門縣', '連江縣'
];
const selectedCity = ref('');
const detailAddress = ref('');

// 狀態欄位
const status = ref(0); // 0=ACTIVE, 1=SUSPENDED, 2=ARCHIVED
const kycStatus = ref(0); // 0=DRAFT, 1=PENDING, 2=APPROVED, 3=REJECTED
const kycRejectionReason = ref('');
const ownerUserId = ref('USR0000001'); // 組織擁有者 ID（由後端 owner_user_id 帶入）

// 品牌 Logo 彈窗與真實檔案上傳 Ref
const logoModalRef = ref(null);
const docFileInput = ref(null);
const idFileInput = ref(null);

const isUploadingDoc = ref(false);
const uploadDocProgress = ref(0);
const isUploadingId = ref(false);
const uploadIdProgress = ref(0);

// 延後上傳：KYC 證明文件選取後僅暫存於本地，待「提交審核」通過驗證才真正上傳，避免產生垃圾檔
const pendingDocFile = ref(null);
const pendingIdFile = ref(null);

const isSavingProfile = ref(false);
const isSubmittingKyc = ref(false);

// ---- 表單即時驗證（純前端防呆與提示，回傳 { valid, message }）----
const taxIdCheck = computed(() => validateTaxId(taxId.value));
const ownerIdCheck = computed(() => validateTwId(ownerIdNumber.value));
const phoneCheck = computed(() => validatePhone(phone.value));
const faxCheck = computed(() => validatePhone(fax.value));

// 撥款帳號僅允許數字（不含 -），給予簡單防呆
const accountNoCheck = computed(() => {
  const v = (accountNo.value || '').trim();
  if (!v) return { valid: true, message: '' };
  return /^\d{8,16}$/.test(v)
    ? { valid: true, message: '' }
    : { valid: false, message: '撥款帳號應為 8-16 位數字' };
});

// 使用者輸入／選擇銀行代碼後，若銀行名稱欄尚空白則自動帶入查得的銀行名稱（保留使用者後續補上分行）
watch(bankCode, (code) => {
  const name = getBankName(code);
  if (name && !bankName.value.trim()) {
    bankName.value = name;
  }
});

// 銀行代碼自動完成下拉：輸入框照常打字，下方用 Bootstrap dropdown 樣式顯示可點選的建議清單
const showBankMenu = ref(false);
const filteredBanks = computed(() => {
  const q = (bankCode.value || '').trim();
  if (!q) return BANKS;
  return BANKS.filter(b => b.code.startsWith(q) || b.name.includes(q));
});
const selectBank = (bank) => {
  bankCode.value = bank.code;
  bankName.value = bank.name; // 直接帶入銀行名稱，使用者再於後方補上分行
  showBankMenu.value = false;
};
const onBankBlur = () => {
  // 延遲關閉，讓選單項目的點擊事件先觸發
  setTimeout(() => { showBankMenu.value = false; }, 150);
};

// 身分證字號統一存為大寫，確保送出後端的值與畫面檢驗一致
watch(ownerIdNumber, (v) => {
  const upper = (v || '').toUpperCase();
  if (upper !== v) ownerIdNumber.value = upper;
});

// 依實際檔名副檔名決定預覽圖示（PDF 顯示 PDF 圖示，圖片顯示圖片圖示），避免固定圖示與實際檔案不符
const fileIconClass = (fileName) => {
  return /\.pdf$/i.test(fileName || '')
    ? 'bi-file-earmark-pdf-fill text-danger'
    : 'bi-image-fill text-primary';
};

const handleOpenLogoModal = () => {
  logoModalRef.value?.show();
};

// 所有權轉移相關狀態
const selectedTransferee = ref('');
const confirmTransferInput = ref('');
const showTransferModal = ref(false);
const pendingTransferUser = ref(null);
const transferExpiresAt = ref(null);

// 模擬組織成員列表（用於轉移所有權下拉選單）
const orgMembers = ref([]);

// 將後端組織詳細資料 DTO 攤平填入各表單欄位
const applyOrgData = (data) => {
  if (!data) return;

  name.value = data.name || '';
  taxId.value = data.tax_id || '';
  status.value = data.status || 0;
  kycStatus.value = data.kyc_status || 0;
  kycRejectionReason.value = data.kyc_rejection_reason || '';
  ownerUserId.value = data.owner_user_id || 'USR0000001';

  // 解析銀行帳戶 JSON
  try {
    const bank = JSON.parse(data.bank_account_info || '{}');
    bankCode.value = bank.bank_code || '';
    bankName.value = bank.bank_name || '';
    accountNo.value = bank.account_no || '';
    accountName.value = bank.account_name || '';
  } catch (e) {
    console.error('Failed to parse bank account info', e);
  }

  // 解析 kyc_data_json
  try {
    const kycData = JSON.parse(data.kyc_data_json || '{}');
    logoUrl.value = kycData.logo_url || '';

    // 解析通訊地址
    const savedAddress = kycData.address || '';
    let matchedCity = '';
    for (const city of cities) {
      if (savedAddress.startsWith(city)) {
        matchedCity = city;
        break;
      }
    }
    if (matchedCity) {
      selectedCity.value = matchedCity;
      detailAddress.value = savedAddress.substring(matchedCity.length);
    } else {
      selectedCity.value = '';
      detailAddress.value = savedAddress;
    }

    phone.value = kycData.phone || '';
    fax.value = kycData.fax || '';
    ownerName.value = kycData.owner_name || '';
    ownerIdNumber.value = kycData.owner_id_number || '';
    registrationDocName.value = kycData.registration_doc_name || '';
    registrationDocUrl.value = kycData.registration_doc_url || '';
    identityCardName.value = kycData.identity_card_name || '';
    identityCardUrl.value = kycData.identity_card_url || '';
  } catch (e) {
    console.error('Failed to parse kyc data json', e);
  }
};

// 快取優先載入組織詳細資料（依組織 ID 分開快取），避免切換頁面時欄位與 Logo 閃爍
const { data: cachedOrgDetail, refresh: refreshOrg } =
  useCachedResource(`org-detail:${orgId.value}`, () => axios.get(`/api/organizer/${orgId.value}`).then(r => r.data));

// 進畫面立即用快取的完整資料填表（比僅有名稱/Logo 的 workspace 快取更完整）
applyOrgData(cachedOrgDetail.value);

// 載入組織與成員資料
const loadOrgData = async () => {
  // 1. 從後端載入真實成員清單（供轉移所有權下拉選單使用；MemberResponse: userId/name/email/status...）
  try {
    const { data } = await axios.get(`/api/organizer/${orgId.value}/members`);
    orgMembers.value = data.data || [];
  } catch (error) {
    console.error('Failed to load organizer members', error);
    orgMembers.value = [];
  }

  // 2. 從後端載入組織詳細資料（快取優先：先用上次資料畫面，背景再刷新，避免閃爍）
  try {
    applyOrgData(await refreshOrg());
  } catch (error) {
    console.error('Failed to load org data from backend', error);
  }
};

// 儲存組織基本資料 (不提交審核)
const handleSaveProfile = async () => {
  // 格式防呆：統編／電話／傳真有填則須通過檢驗才放行儲存
  const profileErrors = [taxIdCheck.value, phoneCheck.value, faxCheck.value]
    .filter(c => !c.valid)
    .map(c => c.message);
  if (profileErrors.length) {
    toast.error(profileErrors[0]);
    return;
  }

  isSavingProfile.value = true;
  const minDelay = new Promise(r => setTimeout(r, 300));

  // 1. 延後上傳：先把尚未上傳的裁切 Logo 送出（裁切時僅本地預覽，按儲存才真正上傳）
  //    上傳失敗就中止，避免把 blob: 預覽 URL 當成正式網址存下去
  try {
    const uploadedUrlPromise = logoModalRef.value?.commitUpload();
    const [uploadedUrl] = await Promise.all([uploadedUrlPromise, minDelay]);
    if (uploadedUrl) {
      logoUrl.value = uploadedUrl;
    }
  } catch (error) {
    toast.error('Logo 上傳失敗：' + (error.response?.data?.message || error.message));
    isSavingProfile.value = false;
    return;
  }

  // 2. 將 Logo 持久化到後端：寫入 ORG table 的 kyc_data_json.logo_url，並刪除被取代的舊檔
  //    （重整／重新登入後 loadOrgData 從後端重抓才不會走樣）
  //    best-effort：mock fallback 模式下若組織尚未存在於 DB，仍以 localStorage 為準
  try {
    await axios.put(`/api/organizer/${orgId.value}/logo`, { logoUrl: logoUrl.value });
  } catch (error) {
    console.warn('Logo 後端持久化失敗，改以本地暫存為準', error);
  }

  // 3. 持久化基本資料到後端（正式儲存 API）；失敗即回報，不再靜默假裝成功
  try {
    const fullAddress = (selectedCity.value || '') + detailAddress.value.trim();
    await axios.put(`/api/organizer/${orgId.value}`, {
      name: name.value,
      taxId: taxId.value,
      bankCode: bankCode.value,
      bankName: bankName.value,
      accountNo: accountNo.value,
      accountName: accountName.value,
      phone: phone.value,
      fax: fax.value,
      address: fullAddress,
      ownerName: ownerName.value,
      ownerIdNumber: ownerIdNumber.value
    });
  } catch (error) {
    toast.error('儲存失敗：' + (error.response?.data?.message || error.message));
    isSavingProfile.value = false;
    return;
  }

  // 4. 同步 Pinia（供 Navbar 下拉選單、Sidebar 等即時更新）
  syncWorkspace(kycStatus.value);
  // 靜默刷新資源快取，下次進頁面直接顯示最新值、不會閃舊資料
  refreshOrg().catch(() => {});
  toast.success('基本資料已儲存');
  isSavingProfile.value = false;
};

// Base64 轉 File 輔助函數
const dataURLtoFile = (dataurl, filename) => {
  let arr = dataurl.split(','), mime = arr[0].match(/:(.*?);/)[1],
      bstr = atob(arr[1]), n = bstr.length, u8arr = new Uint8Array(n);
  while(n--){
      u8arr[n] = bstr.charCodeAt(n);
  }
  return new File([u8arr], filename, {type:mime});
};

// 同意合約狀態
// 用於 KYC 表單提交狀態
const agreeGeneralTerms = ref(false);
const agreeKycTerms = ref(false);
const showContractModal = ref(false);

const handleKycCheckboxClick = (e) => {
  if (!agreeKycTerms.value) {
    e.preventDefault(); // 阻止直接勾選
    showContractModal.value = true; // 強制彈出合約審閱
  } else {
    // 允許取消勾選
    agreeKycTerms.value = false;
  }
};

const handleContractAgreed = () => {
  agreeKycTerms.value = true;
};

// 提交 KYC 審核
const handleSubmitKyc = async () => {
  if (!agreeGeneralTerms.value || !agreeKycTerms.value) {
    toast.error('您必須勾選並同意主辦方服務條款、個人資料保護聲明與免費託售合約，方能提交驗證申請');
    return;
  }
  // 文件可能尚未上傳（延後上傳）：已選取待上傳的檔案也算通過
  const hasDoc = registrationDocUrl.value || pendingDocFile.value;
  const hasId = identityCardUrl.value || pendingIdFile.value;
  if (!name.value.trim() || !taxId.value.trim() || !hasDoc || !hasId) {
    toast.error('請填寫完整公司名稱、統一編號，並上傳設立登記文件及負責人身份證影本');
    return;
  }
  if (!ownerName.value.trim() || !ownerIdNumber.value.trim()) {
    toast.error('請填寫負責人姓名與身分證字號');
    return;
  }
  if (!bankCode.value.trim() || !bankName.value.trim() || !accountNo.value.trim() || !accountName.value.trim()) {
    toast.error('請填寫完整撥款指定金融帳戶資訊');
    return;
  }
  // 格式防呆：統編、身分證、電話、傳真、撥款帳號須通過檢驗
  const kycErrors = [taxIdCheck.value, ownerIdCheck.value, phoneCheck.value, faxCheck.value, accountNoCheck.value]
    .filter(c => !c.valid)
    .map(c => c.message);
  if (kycErrors.length) {
    toast.error(kycErrors[0]);
    return;
  }

  isSubmittingKyc.value = true;
  const minDelay = new Promise(r => setTimeout(r, 300));

  try {
    // 延後上傳：先把尚未上傳的裁切 Logo 送出，再以最終 URL 提交審核
    const uploadedUrlPromise = logoModalRef.value?.commitUpload();
    const [uploadedUrl] = await Promise.all([uploadedUrlPromise, minDelay]);
    if (uploadedUrl) {
      logoUrl.value = uploadedUrl;
    }
    const finalLogoUrl = logoUrl.value;

    // 驗證已通過、確定要送出，這時才把暫存的 KYC 文件實際上傳（避免半途放棄留下垃圾檔）
    await uploadPendingKycFiles();

    const fullAddress = (selectedCity.value || '') + detailAddress.value.trim();
    
    // 呼叫後端提交 KYC API
    await axios.post(`/api/organizer/${orgId.value}/kyc/submit`, {
      taxId: taxId.value,
      bankCode: bankCode.value,
      bankName: bankName.value,
      accountNo: accountNo.value,
      accountName: accountName.value,
      phone: phone.value,
      fax: fax.value,
      address: fullAddress,
      ownerName: ownerName.value,
      ownerIdNumber: ownerIdNumber.value,
      logoUrl: finalLogoUrl,
      registrationDocName: registrationDocName.value,
      registrationDocUrl: registrationDocUrl.value,
      identityCardName: identityCardName.value,
      identityCardUrl: identityCardUrl.value
    });

    kycStatus.value = 1; // PENDING
    
    // 透過儲存至 LocalStorage 同步 Sidebar 等狀態 (保留相容)
    syncWorkspace(1);
    // 靜默刷新資源快取，下次進頁面直接顯示最新審核狀態與資料
    refreshOrg().catch(() => {});

    toast.success('已提交實名驗證申請，管理團隊將於 1-3 個工作天內完成審核');
  } catch (error) {
    toast.error('提交失敗：' + (error.response?.data?.message || error.message));
  } finally {
    isSubmittingKyc.value = false;
  }
};

// 將編輯後的名稱 / Logo / KYC 狀態即時同步至 workspace store（組織清單唯一來源）
const syncWorkspace = (targetKycStatus) => {
  // 同步頂部警告 Banner 狀態
  workspaceStore.kycStatus = targetKycStatus;
  if (workspaceStore.currentOrgId === orgId.value) {
    workspaceStore.orgName = name.value;
    workspaceStore.orgLogo = logoUrl.value;
  }

  // 就地更新組織清單中的名稱與 Logo，側欄 / 導覽列即時反映
  workspaceStore.upsertOrg({
    id: orgId.value,
    name: name.value,
    logo: logoUrl.value,
    kycStatus: targetKycStatus
  });
};

// 檔案選取（延後上傳）：僅做格式／大小驗證並暫存於本地，顯示檔名預覽，
// 真正上傳延到「提交審核」通過驗證時才執行（見 uploadPendingKycFiles）
const selectKycFile = (file, type) => {
  if (!file) return;

  const allowedTypes = ['application/pdf', 'image/jpeg', 'image/jpg', 'image/png'];
  if (!allowedTypes.includes(file.type)) {
    toast.error('只支援上傳 PDF、JPG 或 PNG 格式的檔案');
    return;
  }
  if (file.size > 15 * 1024 * 1024) {
    toast.error('上傳的檔案大小不能超過 15MB');
    return;
  }

  if (type === 'doc') {
    pendingDocFile.value = file;
    registrationDocName.value = file.name;
    registrationDocUrl.value = ''; // 清掉舊路徑，標記為待上傳（送出審核時才上傳）
  } else {
    pendingIdFile.value = file;
    identityCardName.value = file.name;
    identityCardUrl.value = '';
  }
};

// 將單一暫存檔案上傳至後端，回傳後端儲存路徑
const uploadKycFileNow = async (file, type) => {
  const formData = new FormData();
  formData.append('file', file);
  formData.append('fileType', type.toUpperCase()); // DOC 或 ID
  const res = await axios.post(`/api/organizer/${orgId.value}/kyc/upload`, formData);
  if (!res.data.success) throw new Error(res.data.message || '檔案上傳失敗');
  return res.data.filePath;
};

// 提交審核時呼叫：把尚未上傳的 KYC 檔案實際送至後端，並寫回對應 URL
// （只有在表單驗證通過、確定要送出時才執行，避免半途放棄留下垃圾檔）
const uploadPendingKycFiles = async () => {
  if (pendingDocFile.value) {
    isUploadingDoc.value = true;
    try {
      registrationDocUrl.value = await uploadKycFileNow(pendingDocFile.value, 'doc');
      pendingDocFile.value = null;
    } finally {
      isUploadingDoc.value = false;
    }
  }
  if (pendingIdFile.value) {
    isUploadingId.value = true;
    try {
      identityCardUrl.value = await uploadKycFileNow(pendingIdFile.value, 'id');
      pendingIdFile.value = null;
    } finally {
      isUploadingId.value = false;
    }
  }
};

const handleDocFileUpload = (event) => {
  const file = event.target.files[0];
  selectKycFile(file, 'doc');
  event.target.value = '';
};

const handleIdFileUpload = (event) => {
  const file = event.target.files[0];
  selectKycFile(file, 'id');
  event.target.value = '';
};

// 所有權轉移處理：發起 → 後端建立待確認紀錄並寄出認證信給接手人
const handleStartTransfer = async () => {
  const targetMember = orgMembers.value.find(m => m.userId === selectedTransferee.value);
  if (!targetMember) return;

  // 檢查防呆字串輸入（前端先擋一次，後端不依賴此值）
  if (confirmTransferInput.value !== name.value) {
    toast.error('輸入的組織名稱不符，請重新輸入以示確認');
    return;
  }

  isTransferring.value = true;
  try {
    await axios.post(`/api/organizer/${orgId.value}/transfer-owner`, {
      targetUserId: targetMember.userId
    });
    showTransferModal.value = false;
    confirmTransferInput.value = '';
    selectedTransferee.value = '';
    toast.success(`已向 ${targetMember.name} 寄出所有權轉移確認信，待對方於 24 小時內接受後即完成轉移`);
    // 重新讀取危險區域狀態，讓「轉移發起中」橫幅與按鈕禁用即時反映
    await refreshDangerZone(false);
  } catch (e) {
    toast.error(e.response?.data?.message || '發起所有權轉移失敗，請稍後再試');
  } finally {
    isTransferring.value = false;
  }
};

// 撤銷轉移
const handleCancelTransfer = async () => {
  const ok = await confirm({
    title: '取消所有權轉移',
    message: '確定要取消本次的所有權轉移申請嗎？',
    confirmText: '取消轉移',
    variant: 'danger'
  });
  if (!ok) return;
  try {
    await axios.post(`/api/organizer/${orgId.value}/transfer-owner/cancel`);
    pendingTransferUser.value = null;
    transferExpiresAt.value = null;
    toast.success('轉移申請已撤銷');
    await refreshDangerZone(false);
  } catch (e) {
    toast.error(e.response?.data?.message || '取消轉移失敗，請稍後再試');
  }
};

// 【模擬輔助功能】模擬後端審核動作
const simulateKycApproval = (approve) => {
  if (approve) {
    kycStatus.value = 2; // APPROVED
    kycRejectionReason.value = '';
    syncWorkspace(2);
    toast.success('【測試模擬】KYC 審核通過，組織已開通');
  } else {
    kycStatus.value = 3; // REJECTED
    kycRejectionReason.value = '上傳的負責人身分證影像模糊不清，且公司名稱與登記表不符。';
    syncWorkspace(3);
    toast.info('【測試模擬】KYC 審核退件，原因已顯示在畫面上');
  }
};

// 危險區域折疊與狀態（皆改吃後端 /danger-zone 真實 SQL 狀態，不再前端模擬）
const isDangerOpen = ref(false);
const isCheckingDanger = ref(false);
const showDangerSpinner = ref(false);
let spinnerTimer = null;
const isTransferring = ref(false);
const dangerRole = ref('MEMBER');          // 'OWNER' | 'MEMBER'，由後端依 owner_user_id 判定
const deleteBlockReasons = ref([]);        // ['ACTIVE_EVENTS', 'UNSETTLED']
const canDeleteOrg = computed(() => dangerRole.value === 'OWNER' && deleteBlockReasons.value.length === 0);

// 內建 Modal 控制狀態與變數
const showDeleteOrgModal = ref(false);
const showDeleteOrgErrorModal = ref(false);
const showLeaveOrgModal = ref(false);
const confirmDeleteOrgInput = ref('');
const confirmLeaveOrgInput = ref('');      // 退出組織同樣需 key in 組織名稱二次確認

// 向後端讀取危險區域狀態：角色、是否可刪除＋阻擋原因、是否有待處理的所有權轉移
const refreshDangerZone = async (showSpinner = true) => {
  if (showSpinner) {
    isCheckingDanger.value = true;
    showDangerSpinner.value = false;
    if (spinnerTimer) clearTimeout(spinnerTimer);
    spinnerTimer = setTimeout(() => {
      if (isCheckingDanger.value) {
        showDangerSpinner.value = true;
      }
    }, 300);
  }
  try {
    const { data } = await axios.get(`/api/organizer/${orgId.value}/danger-zone`);
    const d = data.data || {};
    dangerRole.value = d.role || 'MEMBER';
    deleteBlockReasons.value = d.blockReasons || [];
    if (d.pendingTransfer) {
      pendingTransferUser.value = {
        userId: d.pendingTransfer.targetUserId,
        name: d.pendingTransfer.targetName
      };
      transferExpiresAt.value = d.pendingTransfer.expiresAt
        ? new Date(d.pendingTransfer.expiresAt).toLocaleString()
        : null;
    } else {
      pendingTransferUser.value = null;
      transferExpiresAt.value = null;
    }
  } catch (e) {
    if (showSpinner) {
      toast.error(e.response?.data?.message || '無法載入組織狀態，請稍後再試');
      isDangerOpen.value = false;
    }
  } finally {
    if (showSpinner) {
      isCheckingDanger.value = false;
      showDangerSpinner.value = false;
      if (spinnerTimer) clearTimeout(spinnerTimer);
    }
  }
};

const toggleDangerCollapse = () => {
  isDangerOpen.value = !isDangerOpen.value;
  if (isDangerOpen.value) {
    refreshDangerZone(true);
  }
};

const handleDeleteOrg = () => {
  if (!canDeleteOrg.value) {
    // 有進行中活動 / 未結算款項 → 顯示無法刪除原因
    showDeleteOrgErrorModal.value = true;
    return;
  }
  confirmDeleteOrgInput.value = '';
  showDeleteOrgModal.value = true;
};

const confirmDeleteOrg = async () => {
  if (confirmDeleteOrgInput.value !== name.value) return;
  try {
    await axios.delete(`/api/organizer/${orgId.value}`);
    workspaceStore.removeOrg(orgId.value);
    showDeleteOrgModal.value = false;
    toast.success('組織已成功封存註銷');
    router.push('/org/select');
  } catch (e) {
    toast.error(e.response?.data?.message || '刪除組織失敗，請稍後再試');
  }
};

const handleLeaveOrg = () => {
  confirmLeaveOrgInput.value = '';
  showLeaveOrgModal.value = true;
};

const confirmLeaveOrg = async () => {
  if (confirmLeaveOrgInput.value !== name.value) return;
  try {
    await axios.post(`/api/organizer/${orgId.value}/leave`);
    workspaceStore.removeOrg(orgId.value);
    showLeaveOrgModal.value = false;
    toast.success('您已成功退出該組織');
    router.push('/org/select');
  } catch (e) {
    toast.error(e.response?.data?.message || '退出組織失敗，請稍後再試');
  }
};

onMounted(() => {
  loadOrgData();
  workspaceStore.setOrgId(orgId.value);
  // 進畫面即靜默讀取一次（不顯示 spinner），讓「轉移發起中」橫幅可立即呈現
  refreshDangerZone(false);
});
</script>

<template>
  <div class="row g-4">
    <!-- 左側主要表單區 -->
    <div class="col-lg-8">
      <!-- 驗證狀態橫幅提示 -->
      <div v-if="kycStatus === 0" class="alert-premium alert-premium-secondary mb-4">
        <div class="alert-premium-icon">
          <i class="bi bi-info-circle"></i>
        </div>
        <div>
          <h6 class="alert-title-premium">尚未提交實名驗證 (KYC)</h6>
          <div class="small opacity-75" style="line-height: 1.6;">您的組織帳戶目前處於草稿階段。請填妥下方資訊並上傳證明文件提交驗證。通過驗證前，系統暫時無法開通售票與結算提現功能。</div>
        </div>
      </div>
      
      <div v-else-if="kycStatus === 1" class="alert-premium alert-premium-warning mb-4">
        <div class="alert-premium-icon">
          <i class="bi bi-clock-history"></i>
        </div>
        <div>
          <h6 class="alert-title-premium">實名驗證等待審查中</h6>
          <div class="small opacity-75" style="line-height: 1.6;">已提交審核。目前處於等待審查中狀態，系統管理員正在審查您的資料與證明文件，請耐心等候。在審核完成前，資料欄位將鎖定唯讀。</div>
        </div>
      </div>

      <div v-else-if="kycStatus === 2" class="alert-premium alert-premium-success mb-4">
        <div class="alert-premium-icon">
          <i class="bi bi-patch-check-fill"></i>
        </div>
        <div>
          <h6 class="alert-title-premium">組織實名驗證已通過</h6>
          <div class="small opacity-75" style="line-height: 1.6;">恭喜！您的組織已完成 KYC 實名認證，售票及提現結算功能已全面開通。</div>
        </div>
      </div>

      <div v-else-if="kycStatus === 3" class="alert-premium alert-premium-danger mb-4">
        <div class="alert-premium-icon">
          <i class="bi bi-exclamation-octagon-fill"></i>
        </div>
        <div>
          <h6 class="alert-title-premium">實名驗證退件</h6>
          <div class="small fw-semibold mb-1">退件原因：{{ kycRejectionReason }}</div>
          <div class="small opacity-90" style="line-height: 1.6;">請修正下方有問題的欄位，並重新上傳正確的檔案後，再次提交審核。</div>
        </div>
      </div>

      <!-- 轉移所有權進行中提示 -->
      <div v-if="pendingTransferUser" class="alert alert-info border-0 rounded-4 p-4 mb-4">
        <div class="d-flex justify-content-between align-items-start">
          <div>
            <h5 class="fw-bold mb-1 text-info-emphasis"><i class="bi bi-envelope-exclamation-fill me-2"></i>組織所有權轉移發起中</h5>
            <p class="small mb-0 text-info-emphasis">
              已發出轉移確認信給：<strong>{{ pendingTransferUser.name }}</strong>。<br />
              對方必須在 <strong>{{ transferExpiresAt }}</strong> 之前點擊信中連結確認接受。
            </p>
          </div>
          <div class="d-flex gap-2">
            <button @click="handleCancelTransfer" class="btn btn-outline-danger btn-sm rounded-3 fw-semibold">取消轉移</button>
          </div>
        </div>
      </div>

      <!-- 基本設定卡片 -->
      <div class="card border rounded-4 shadow-sm p-4 mb-4">
        <h5 class="fw-bold mb-4"><i class="bi bi-file-earmark-text me-2 text-primary"></i>組織基本資料</h5>
        
        <!-- Logo 上傳預覽與相機按鈕 -->
        <div class="text-center mb-4">
          <div class="logo-wrapper d-inline-block position-relative cursor-pointer" @click="kycStatus !== 1 && handleOpenLogoModal()" :title="kycStatus === 1 ? '審查中鎖定' : '更換組織 Logo'">
            <img
              :src="logoUrl && logoUrl.startsWith('/api/') ? (BACKEND_URL + logoUrl) : (logoUrl || getOrgDefaultLogo(name))"
              alt="Org Logo"
              class="logo-preview rounded-circle border shadow-sm"
            />
            <!-- 相機徽章 -->
            <div v-if="kycStatus !== 1" class="camera-badge d-flex align-items-center justify-content-center rounded-circle border shadow-sm">
              <i class="bi bi-camera-fill"></i>
            </div>
          </div>
        </div>

        <form @submit.prevent="handleSaveProfile">
          <div class="row g-3">
            <div class="col-md-6">
              <label class="form-label fw-semibold text-secondary">組織名稱</label>
              <input type="text" class="form-control form-control-lg" v-model="name" :disabled="kycStatus === 1" required placeholder="請輸入公司或品牌名稱" />
            </div>
            
            <div class="col-md-6">
              <label class="form-label fw-semibold text-secondary">統一編號</label>
              <input type="text" class="form-control form-control-lg" :class="{ 'is-invalid': !taxIdCheck.valid }" v-model="taxId" :disabled="kycStatus === 1" maxlength="8" inputmode="numeric" placeholder="請輸入 8 位數統一編號" />
              <div class="field-hint text-danger">{{ taxIdCheck.valid ? '' : taxIdCheck.message }}</div>
            </div>

            <div class="col-md-6">
              <label class="form-label fw-semibold text-secondary">聯絡電話</label>
              <input type="tel" class="form-control form-control-lg" :class="{ 'is-invalid': !phoneCheck.valid }" v-model="phone" :disabled="kycStatus === 1" placeholder="市話或手機號碼" />
              <div class="field-hint text-danger">{{ phoneCheck.valid ? '' : phoneCheck.message }}</div>
            </div>

            <div class="col-md-6">
              <label class="form-label fw-semibold text-secondary">傳真號碼</label>
              <input type="tel" class="form-control form-control-lg" :class="{ 'is-invalid': !faxCheck.valid }" v-model="fax" :disabled="kycStatus === 1" placeholder="例如：02-12345678" />
              <div class="field-hint text-danger">{{ faxCheck.valid ? '' : faxCheck.message }}</div>
            </div>

            <div class="col-12">
              <label class="form-label fw-semibold text-secondary">通訊地址</label>
              <div class="row g-2">
                <div class="col-md-4">
                  <select class="form-select form-select-lg" v-model="selectedCity" :disabled="kycStatus === 1">
                    <option value="">請選擇縣市</option>
                    <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
                  </select>
                </div>
                <div class="col-md-8">
                  <input type="text" class="form-control form-control-lg" v-model="detailAddress" :disabled="kycStatus === 1" placeholder="請輸入詳細地址" />
                </div>
              </div>
            </div>
          </div>
          
          <div class="text-end mt-4" v-if="kycStatus !== 1">
            <button type="submit" class="btn btn-outline-primary px-4 py-2 rounded-3 fw-bold" :disabled="isSavingProfile" style="min-width: 160px;">
              <span v-if="isSavingProfile" class="spinner-border spinner-border-sm me-2" role="status"></span>
              {{ isSavingProfile ? '儲存中...' : '儲存基本資料' }}
            </button>
          </div>
        </form>
      </div>

      <!-- KYC 審核文件與撥款資料卡片 -->
      <div class="card border rounded-4 shadow-sm p-4 mb-4">
        <h5 class="fw-bold mb-4"><i class="bi bi-shield-check me-2 text-primary"></i>實名驗證 (KYC) 證明文件</h5>
        
        <!-- 隱藏的真實檔案選擇器 -->
        <input type="file" ref="docFileInput" accept=".pdf, .jpg, .jpeg, .png" class="d-none" @change="handleDocFileUpload" />
        <input type="file" ref="idFileInput" accept=".pdf, .jpg, .jpeg, .png" class="d-none" @change="handleIdFileUpload" />

        <div class="row g-4">
          <!-- 設立登記表上傳 -->
          <div class="col-md-6">
            <label class="form-label fw-semibold text-secondary">公司設立登記證明文件 <span class="text-danger">*</span></label>
            <div class="border rounded-3 p-3 bg-light text-center kyc-upload-box d-flex flex-column align-items-center justify-content-center">
              <div v-if="registrationDocName">
                <i class="bi fs-2" :class="fileIconClass(registrationDocName)"></i>
                <div class="text-truncate fw-semibold small mt-2 px-2" :title="registrationDocName">{{ registrationDocName }}</div>
                <button v-if="kycStatus !== 1" @click="registrationDocUrl = ''; registrationDocName = ''; pendingDocFile = null" class="btn btn-link btn-sm text-danger text-decoration-none mt-1">重新上傳</button>
              </div>
              <div v-else-if="isUploadingDoc">
                <div class="spinner-border spinner-border-sm text-primary mb-2" role="status"></div>
                <div class="small text-secondary">上傳中...</div>
              </div>
              <div v-else>
                <i class="bi bi-cloud-arrow-up fs-2 text-muted mb-2 d-block"></i>
                <button type="button" class="btn btn-sm btn-primary text-white px-3 py-1.5 rounded-3" :disabled="kycStatus === 1" @click="docFileInput.click()">點擊上傳檔案</button>
                <div class="text-muted mt-2" style="font-size: 0.75rem;">支援 PDF / JPG / PNG，最大 15MB</div>
              </div>
            </div>
          </div>

          <!-- 負責人身分證影本 -->
          <div class="col-md-6">
            <label class="form-label fw-semibold text-secondary">負責人身分證影本 (正反面) <span class="text-danger">*</span></label>
            <div class="border rounded-3 p-3 bg-light text-center kyc-upload-box d-flex flex-column align-items-center justify-content-center">
              <div v-if="identityCardName">
                <i class="bi fs-2" :class="fileIconClass(identityCardName)"></i>
                <div class="text-truncate fw-semibold small mt-2 px-2" :title="identityCardName">{{ identityCardName }}</div>
                <button v-if="kycStatus !== 1" @click="identityCardUrl = ''; identityCardName = ''; pendingIdFile = null" class="btn btn-link btn-sm text-danger text-decoration-none mt-1">重新上傳</button>
              </div>
              <div v-else-if="isUploadingId">
                <div class="spinner-border spinner-border-sm text-primary mb-2" role="status"></div>
                <div class="small text-secondary">上傳中...</div>
              </div>
              <div v-else>
                <i class="bi bi-cloud-arrow-up fs-2 text-muted mb-2 d-block"></i>
                <button type="button" class="btn btn-sm btn-primary text-white px-3 py-1.5 rounded-3" :disabled="kycStatus === 1" @click="idFileInput.click()">點擊上傳檔案</button>
                <div class="text-muted mt-2" style="font-size: 0.75rem;">支援 PDF / JPG / PNG，最大 15MB</div>
              </div>
            </div>
          </div>

          <!-- 負責人身分資料 -->
          <div class="col-12">
            <hr class="my-3 text-muted opacity-25" />
            <h6 class="fw-bold mb-3"><i class="bi bi-person-badge me-2 text-secondary"></i>負責人身分資料</h6>
            <div class="row g-3">
              <div class="col-md-6">
                <label class="form-label text-secondary small">負責人姓名 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" v-model="ownerName" :disabled="kycStatus === 1" placeholder="請輸入與身分證相符之姓名" />
              </div>
              <div class="col-md-6">
                <label class="form-label text-secondary small">負責人身分證字號 <span class="text-danger">*</span></label>
                <input type="text" class="form-control text-uppercase" :class="{ 'is-invalid': !ownerIdCheck.valid }" v-model="ownerIdNumber" :disabled="kycStatus === 1" placeholder="例如：A123456789" maxlength="10" />
                <div class="field-hint text-danger">{{ ownerIdCheck.valid ? '' : ownerIdCheck.message }}</div>
              </div>
            </div>
          </div>

          <!-- 撥款銀行資料 -->
          <div class="col-12">
            <hr class="my-3 text-muted opacity-25" />
            <h6 class="fw-bold mb-3"><i class="bi bi-bank me-2 text-secondary"></i>撥款指定金融帳戶</h6>
            
            <div class="row g-3">
              <div class="col-md-4">
                <label class="form-label text-secondary small">銀行代碼 <span class="text-danger">*</span></label>
                <div class="position-relative">
                  <input type="text" class="form-control" v-model="bankCode" :disabled="kycStatus === 1" maxlength="3" inputmode="numeric" placeholder="例如：007" autocomplete="off" @focus="showBankMenu = true" @blur="onBankBlur" />
                  <ul v-if="showBankMenu && filteredBanks.length" class="dropdown-menu show w-100 shadow-sm border bank-menu">
                    <li v-for="b in filteredBanks" :key="b.code">
                      <button type="button" class="dropdown-item d-flex align-items-center" @mousedown.prevent="selectBank(b)">
                        <span class="fw-semibold me-2">{{ b.code }}</span>
                        <span class="text-secondary small text-truncate">{{ b.name }}</span>
                      </button>
                    </li>
                  </ul>
                </div>
              </div>
              <div class="col-md-8">
                <label class="form-label text-secondary small">銀行名稱與分行 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" v-model="bankName" :disabled="kycStatus === 1" placeholder="選擇銀行代碼後自動帶入，請補上分行名稱" />
              </div>
              <div class="col-md-6">
                <label class="form-label text-secondary small">撥款帳號 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" :class="{ 'is-invalid': !accountNoCheck.valid }" v-model="accountNo" :disabled="kycStatus === 1" inputmode="numeric" placeholder="請輸入完整匯款帳號" />
                <div class="field-hint text-danger">{{ accountNoCheck.valid ? '' : accountNoCheck.message }}</div>
              </div>
              <div class="col-md-6">
                <label class="form-label text-secondary small">戶名 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" v-model="accountName" :disabled="kycStatus === 1" placeholder="必須與公司登記名稱或負責人姓名相符" />
              </div>
            </div>
          </div>
        </div>

        <div class="mt-4 mb-4" v-if="kycStatus !== 1">
          <!-- 勾選 1：主辦方服務條款 & 個人資料保護聲明 -->
          <div class="form-check d-inline-flex align-items-start text-start gap-2 mb-2 w-100">
            <input class="form-check-input mt-1" type="checkbox" id="generalTermsCheckbox" v-model="agreeGeneralTerms" required />
            <label class="form-check-label small text-secondary cursor-pointer" for="generalTermsCheckbox">
              我已確認上述資料填寫無誤，並代表本組織同意本平台之
              <RouterLink to="/org/docs/terms" target="_blank" @click.stop class="text-primary text-decoration-none fw-semibold">《主辦方服務條款》</RouterLink> 與
              <RouterLink to="/org/docs/privacy" target="_blank" @click.stop class="text-primary text-decoration-none fw-semibold">《個人資料保護聲明》</RouterLink>。
            </label>
          </div>
          <!-- 勾選 2：免費標準方案託售合約 -->
          <div class="form-check d-inline-flex align-items-start text-start gap-2 w-100">
            <input class="form-check-input mt-1" type="checkbox" id="kycTermsCheckbox" :checked="agreeKycTerms" @click="handleKycCheckboxClick" required />
            <label class="form-check-label small text-secondary cursor-pointer" for="kycTermsCheckbox">
              同意並簽署
              <a href="#" @click.prevent.stop="showContractModal = true" class="text-primary text-decoration-none fw-semibold">《免費標準方案託售合約》</a>
              （每場活動人數上限 500 人；平台票券交易手續費抽成 5.0%）。
            </label>
          </div>
        </div>

        <div class="text-end mt-4" v-if="kycStatus !== 1">
          <button type="button" @click="handleSubmitKyc" class="btn btn-primary px-4 py-2.5 rounded-3 text-white fw-bold shadow-sm" :disabled="!agreeGeneralTerms || !agreeKycTerms || isSubmittingKyc" style="min-width: 230px;">
            <span v-if="isSubmittingKyc" class="spinner-border spinner-border-sm me-2" role="status"></span>
            <i v-else class="bi bi-send-fill me-1"></i>{{ isSubmittingKyc ? '提交中...' : '提交實名驗證審核' }}
          </button>
        </div>
      </div>

      <!-- 危險區域 (折疊面板) -->
      <div class="card border rounded-4 shadow-sm mb-4 transition-all" 
           :class="isDangerOpen ? 'border-danger' : 'border-light-gray'"
           style="overflow: hidden;">
        
        <!-- 點擊折疊頭部 -->
        <div class="card-header bg-white border-0 py-3 px-4 d-flex align-items-center justify-content-between cursor-pointer" 
             @click="toggleDangerCollapse" 
             style="user-select: none;">
          <div class="d-flex align-items-center gap-2">
            <i class="bi bi-exclamation-triangle-fill text-primary fs-5"></i>
            <span class="fw-bold text-dark mb-0">
              退出、轉移或刪除組織
            </span>
          </div>
          <i class="bi bi-chevron-down fs-5 text-secondary transition-transform" :class="{ 'rotate-180': isDangerOpen }"></i>
        </div>

        <!-- 展開內容區 -->
        <div v-show="isDangerOpen" class="card-body px-4 pb-4 pt-0">
          <hr class="mt-0 mb-3 text-muted opacity-25" />
          
          <!-- 情況一: 檢查狀態與身分中 (轉圈動畫) -->
          <div v-if="isCheckingDanger && showDangerSpinner" class="d-flex align-items-center justify-content-center py-4">
            <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true" style="color: var(--tap-primary, #e57346);"></span>
          </div>

          <!-- 情況二: 檢查完畢 -->
          <div v-else>
            <!-- 2A. 負責人專用按鈕 (轉移所有權 & 刪除組織) -->
            <div v-if="dangerRole === 'OWNER'">
              <!-- 項目 1: 轉移所有權 -->
              <div class="p-3 bg-light rounded-3 mb-3">
                <div class="row align-items-center g-3">
                  <div class="col-sm-8 col-md-9">
                    <div class="fw-bold text-dark mb-1">轉移組織所有權</div>
                    <div class="text-muted small">將主辦方的最高管理者權限轉移給其他成員，轉移後您將降為普通成員。</div>
                  </div>
                  <div class="col-sm-4 col-md-3 text-sm-end">
                    <button @click="showTransferModal = true" :disabled="pendingTransferUser !== null" class="btn btn-orange w-100 px-3 py-2 rounded-3 fw-bold">轉移所有權</button>
                  </div>
                </div>
              </div>

              <!-- 項目 2: 刪除組織 -->
              <div class="p-3 bg-light rounded-3">
                <div class="row align-items-center g-3">
                  <div class="col-sm-8 col-md-9">
                    <div class="fw-bold text-dark mb-1">刪除組織</div>
                    <div class="text-muted small">註銷並封存此主辦單位，名下其他成員將一併被撤銷管理權限（無須先逐一移除）。刪除前請先確認，組織不可有進行中活動或尚未結算之款項。</div>
                    <div v-if="deleteBlockReasons.length" class="text-danger small fw-semibold mt-2">
                      <i class="bi bi-exclamation-circle-fill me-1"></i>
                      <span v-if="deleteBlockReasons.includes('ACTIVE_EVENTS')">偵測到名下仍有「進行中」活動。</span>
                      <span v-if="deleteBlockReasons.includes('UNSETTLED')">偵測到名下仍有「尚未結算」之款項。</span>
                      請處理後再行刪除。
                    </div>
                  </div>
                  <div class="col-sm-4 col-md-3 text-sm-end">
                    <button @click="handleDeleteOrg" :disabled="!canDeleteOrg" class="btn btn-danger w-100 px-3 py-2 rounded-3 fw-bold">刪除組織</button>
                  </div>
                </div>
              </div>
            </div>

            <!-- 2B. 一般成員專用按鈕 (僅顯示退出組織) -->
            <div v-else>
              <!-- 項目 1: 退出組織 -->
              <div class="p-3 bg-light rounded-3">
                <div class="row align-items-center g-3">
                  <div class="col-sm-8 col-md-9">
                    <div class="fw-bold text-dark mb-1">退出主辦方組織</div>
                    <div class="text-muted small">退出此主辦方組織，您將失去此組織的所有協作與管理權限。</div>
                  </div>
                  <div class="col-sm-4 col-md-3 text-sm-end">
                    <button @click="handleLeaveOrg" class="btn btn-danger w-100 px-3 py-2 rounded-3 fw-bold">退出組織</button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 右側資訊面板（驗證指引與測試輔助） -->
    <div class="col-lg-4">
      <div class="card border rounded-4 shadow-sm p-4 mb-4 bg-light">
        <h6 class="fw-bold mb-3"><i class="bi bi-lightbulb-fill text-warning me-1"></i>實名驗證須知</h6>
        <ul class="small text-secondary ps-3 mb-0" style="line-height: 1.7;">
          <li class="mb-2"><strong>為什麼需要驗證？</strong> 根據我國洗錢防制法與消費者保護法規，銷售票券與代收票款平台，必須確實核實主辦方身分方能進行撥款。</li>
          <li class="mb-2"><strong>資料保密承諾</strong>：您所上傳的證明文件及銀行帳戶資訊，將透過加密機制儲存，僅供管理方進行合規性審核使用，絕不外洩。</li>
          <li class="mb-0"><strong>審核時間</strong>：送出後平均 1-3 個工作天內完成審核，您將收到 Email 與系統通知。</li>
        </ul>
      </div>

      <!-- 完成驗證開通功能說明 -->
      <div class="card border rounded-4 shadow-sm p-4 mb-4 bg-light">
        <h6 class="fw-bold mb-3"><i class="bi bi-patch-check-fill text-success me-1"></i>完成實名認證享有權限</h6>
        <p class="small text-secondary mb-3">通過實名驗證 (KYC) 後，您的主辦方帳戶將自動升級並全面開通以下功能：</p>
        <ul class="small text-secondary ps-3 mb-0" style="line-height: 1.7;">
          <li class="mb-2"><strong>發佈付費活動售票</strong>：自由建立付費票券、公開發佈活動，接受線上信用卡與 ATM 轉帳等多元購票管道。</li>
          <li class="mb-2"><strong>活動票款結算提現</strong>：活動結束後，代收之票款即可快速結算，並安全撥款至您所綁定的指定金融帳戶。</li>
          <li class="mb-0"><strong>簽署方案與額度解鎖</strong>：啟用標準方案或與平台洽談專屬客製化抽成費率，且單場活動報名人數無 500 人上限。</li>
        </ul>
      </div>

    </div>
  </div>

  <!-- 所有權轉移 Confirmation Modal -->
  <div v-if="showTransferModal" class="modal-backdrop fade show"></div>
  <div v-if="showTransferModal" class="modal fade show d-block" tabindex="-1" style="z-index: 1055;">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4 border-0 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold text-danger"><i class="bi bi-exclamation-triangle-fill me-2"></i>警告：即將轉移組織所有權</h5>
          <button type="button" class="btn-close" @click="showTransferModal = false"></button>
        </div>
        
        <form @submit.prevent="handleStartTransfer">
          <div class="modal-body p-4">
            <p class="text-secondary small mb-3">
              您即將發起轉移流程。請選擇組織內的一位<strong>已加入 (ACCEPTED)</strong> 的成員作為新的擁有者。
              轉移發起後，對方會收到通知，並有 <strong>24 小時</strong> 的時間接受。若對方同意，您將自動降級，且無法還原。
            </p>
            
            <!-- 選擇受移轉人 -->
            <div class="mb-3">
              <label class="form-label fw-semibold text-dark">選擇接管成員</label>
              <select class="form-select" v-model="selectedTransferee" required>
                <option value="">-- 請選擇組織成員 --</option>
                <!-- 過濾掉擁有者本人且僅列出狀態為 1=ACCEPTED 的成員 -->
                <option v-for="m in orgMembers.filter(m => m.status === 1 && m.userId !== ownerUserId)" :key="m.memberId" :value="m.userId">
                  {{ m.name }} ({{ m.email }})
                </option>
              </select>
            </div>

            <!-- 防呆輸入確認 -->
            <div class="mb-3">
              <label class="form-label fw-semibold text-dark">請輸入本組織名稱以確認：<strong style="color: var(--tap-primary, #e57346);">{{ name }}</strong></label>
              <input type="text" class="form-control" v-model="confirmTransferInput" required placeholder="請輸入完整的組織名稱" />
            </div>
          </div>
          
          <div class="modal-footer border-0 pt-0 pb-4 px-4 gap-2">
            <button type="button" class="btn btn-light rounded-3 fw-semibold border d-inline-flex align-items-center justify-content-center" @click="showTransferModal = false" style="width: 90px; height: 38px; padding: 0;">取消</button>
            <button type="submit" class="btn btn-orange py-2 rounded-3 fw-bold d-inline-flex align-items-center justify-content-center" :disabled="!selectedTransferee || confirmTransferInput !== name || isTransferring" style="width: 90px; height: 38px; padding: 0;">
              <span v-if="isTransferring" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
              <span v-else>轉移</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <!-- 專業合約審閱彈窗 -->
  <ContractModal 
    v-model="showContractModal" 
    docType="contract_free" 
    title="免費標準方案託售合約"
    @agreed="handleContractAgreed"
  />

  <!-- 刪除組織 - 無法刪除警告 Modal (ORG0000001) -->
  <div v-if="showDeleteOrgErrorModal" class="modal-backdrop fade show"></div>
  <div v-if="showDeleteOrgErrorModal" class="modal fade show d-block" tabindex="-1" style="z-index: 1055;">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4 border-0 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold text-danger"><i class="bi bi-exclamation-octagon-fill me-2"></i>無法刪除組織</h5>
          <button type="button" class="btn-close" @click="showDeleteOrgErrorModal = false"></button>
        </div>
        <div class="modal-body p-4">
          <p class="text-secondary small mb-0" style="line-height: 1.6;">
            偵測到本組織名下仍有
            <strong v-if="deleteBlockReasons.includes('ACTIVE_EVENTS')" class="text-danger">「進行中」的活動</strong><span v-if="deleteBlockReasons.includes('ACTIVE_EVENTS') && deleteBlockReasons.includes('UNSETTLED')"> 與 </span><strong v-if="deleteBlockReasons.includes('UNSETTLED')" class="text-danger">「尚未結算」之款項</strong>，
            因此暫時無法刪除。<br /><br />
            請先將相關活動結束並完成財務結算後，再行刪除組織；或考慮改用「轉移所有權」交接給其他成員。
          </p>
        </div>
        <div class="modal-footer border-0 pt-0 pb-4 px-4">
          <button type="button" class="btn btn-primary text-white w-100 py-2 rounded-3 fw-bold" @click="showDeleteOrgErrorModal = false">我知道了</button>
        </div>
      </div>
    </div>
  </div>

  <!-- 刪除組織 - 二次確認 Modal -->
  <div v-if="showDeleteOrgModal" class="modal-backdrop fade show"></div>
  <div v-if="showDeleteOrgModal" class="modal fade show d-block" tabindex="-1" style="z-index: 1055;">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4 border-0 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold text-danger"><i class="bi bi-exclamation-triangle-fill me-2"></i>警告：即將永久刪除組織</h5>
          <button type="button" class="btn-close" @click="showDeleteOrgModal = false"></button>
        </div>
        <form @submit.prevent="confirmDeleteOrg">
          <div class="modal-body p-4">
            <p class="text-secondary small mb-3" style="line-height: 1.6;">
              您確定要註銷並刪除此主辦單位嗎？此操作將會封存組織，您名下的其他團隊成員也將失去此組織的存取權（此操作無法復原，但會保留歷史紀錄供日後追溯）。
            </p>
            <div class="mb-3">
              <label class="form-label fw-semibold text-dark">請輸入本組織名稱以確認：<strong style="color: var(--tap-primary, #e57346);">{{ name }}</strong></label>
              <input type="text" class="form-control" v-model="confirmDeleteOrgInput" required placeholder="請輸入完整的組織名稱" />
            </div>
          </div>
          <div class="modal-footer border-0 pt-0 pb-4 px-4 gap-2">
            <button type="button" class="btn btn-light px-3 py-2 rounded-3 fw-semibold border" @click="showDeleteOrgModal = false">取消</button>
            <button type="submit" class="btn btn-danger px-4 py-2 rounded-3 fw-bold" :disabled="confirmDeleteOrgInput !== name">
              確認永久刪除
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <!-- 退出組織 - 確認 Modal -->
  <div v-if="showLeaveOrgModal" class="modal-backdrop fade show"></div>
  <div v-if="showLeaveOrgModal" class="modal fade show d-block" tabindex="-1" style="z-index: 1055;">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content rounded-4 border-0 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold text-danger"><i class="bi bi-exclamation-triangle-fill me-2"></i>退出組織確認</h5>
          <button type="button" class="btn-close" @click="showLeaveOrgModal = false"></button>
        </div>
        <form @submit.prevent="confirmLeaveOrg">
          <div class="modal-body p-4">
            <p class="text-secondary small mb-3" style="line-height: 1.6;">
              確定要退出此主辦方組織嗎？退出後您將失去此組織的所有管理與協作權限。
            </p>
            <div class="mb-3">
              <label class="form-label fw-semibold text-dark">請輸入本組織名稱以確認：<strong style="color: var(--tap-primary, #e57346);">{{ name }}</strong></label>
              <input type="text" class="form-control" v-model="confirmLeaveOrgInput" required placeholder="請輸入完整的組織名稱" />
            </div>
          </div>
          <div class="modal-footer border-0 pt-0 pb-4 px-4 gap-2">
            <button type="button" class="btn btn-light px-3 py-2 rounded-3 fw-semibold border" @click="showLeaveOrgModal = false">取消</button>
            <button type="submit" class="btn btn-danger px-4 py-2 rounded-3 fw-bold" :disabled="confirmLeaveOrgInput !== name">
              確認退出組織
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>

  <!-- 組織 Logo 編輯 Modal -->
  <AvatarEditModal
    ref="logoModalRef"
    v-model="logoUrl"
    mode="org"
    :orgName="name"
    :organizerId="orgId"
    title="更換組織 Logo"
    :defer-upload="true"
  />
</template>

<style scoped>
.form-label {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

/* 銀行代碼自動完成下拉：緊貼輸入框下方、可捲動 */
.bank-menu {
  top: 100%;
  left: 0;
  min-width: 0; /* 覆寫 Bootstrap 預設 10rem，讓選單寬度嚴格跟隨輸入框 */
  margin-top: 0.25rem;
  max-height: 260px;
  overflow-y: auto;
  z-index: 1060;
}

/* 欄位提示／錯誤訊息固定高度，內容切換時不撐開版面、避免抖動 */
.field-hint {
  min-height: 1.25rem;
  margin-top: 0.25rem;
  font-size: 0.8rem;
  line-height: 1.25rem;
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

/* Logo 編輯與預覽樣式 */
.logo-wrapper {
  position: relative;
}

.logo-wrapper:hover .logo-preview {
  filter: brightness(0.85);
}

.logo-preview {
  width: 100px;
  height: 100px;
  object-fit: cover;
  background-color: #f8f9fa;
  transition: filter 0.2s ease-in-out;
}

.camera-badge {
  position: absolute;
  bottom: 2px;
  right: 2px;
  width: 30px;
  height: 30px;
  background-color: #ffffff;
  color: var(--tap-primary, #e57346);
  border: 1px solid #dee2e6 !important;
  font-size: 1rem;
  transition: background-color 0.2s;
}

.logo-wrapper:hover .camera-badge {
  background-color: #f8f9fa;
}

.kyc-upload-box {
  height: 160px;
}

/* 邊框顏色與危險區域動畫 */
.border-light-gray {
  border-color: var(--tap-light-gray) !important;
}

.transition-all {
  transition: border-color 0.25s ease-in-out, box-shadow 0.25s ease-in-out;
}

.transition-transform {
  transition: transform 0.2s ease-in-out;
}

.rotate-180 {
  transform: rotate(180deg);
}

/* 現代化 Alert 卡片 */
.alert-premium {
  border-radius: 16px;
  padding: 1.25rem 1.5rem;
  display: flex;
  gap: 1rem;
  align-items: start;
  font-size: 0.9rem;
}
.alert-premium-icon {
  font-size: 1.25rem;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 8px;
}
.alert-premium-secondary {
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  color: #475569;
}
.alert-premium-secondary .alert-premium-icon {
  background: #cbd5e1;
  color: #475569;
}
.alert-premium-warning {
  background: rgba(245, 158, 11, 0.04);
  border: 1px solid rgba(245, 158, 11, 0.18);
  color: #b45309;
}
.alert-premium-warning .alert-premium-icon {
  background: rgba(245, 158, 11, 0.1);
  color: #d97706;
}
.alert-premium-success {
  background: rgba(16, 185, 129, 0.04);
  border: 1px solid rgba(16, 185, 129, 0.18);
  color: #047857;
}
.alert-premium-success .alert-premium-icon {
  background: rgba(16, 185, 129, 0.1);
  color: #059669;
}
.alert-premium-danger {
  background: rgba(239, 68, 68, 0.04);
  border: 1px solid rgba(239, 68, 68, 0.18);
  color: #b91c1c;
}
.alert-premium-danger .alert-premium-icon {
  background: rgba(239, 68, 68, 0.1);
  color: #dc2626;
}
.alert-title-premium {
  font-weight: 700;
  margin-bottom: 0.25rem;
  color: #0f172a;
}
.alert-premium-warning .alert-title-premium {
  color: #92400e;
}
.alert-premium-success .alert-title-premium {
  color: #065f46;
}
.alert-premium-danger .alert-title-premium {
  color: #991b1b;
}

.btn-orange {
  color: #fff !important;
  background-color: var(--tap-primary, #e57346) !important;
  border-color: var(--tap-primary, #e57346) !important;
}

.btn-orange:hover,
.btn-orange:focus,
.btn-orange:active {
  background-color: #d66437 !important;
  border-color: #d66437 !important;
}

.btn-orange:disabled {
  background-color: #f7a887 !important;
  border-color: #f7a887 !important;
  opacity: 0.65;
}
</style>


