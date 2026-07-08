import { defineStore } from 'pinia';
import axios from '@/plugins/axios.js';
import { getOrgDefaultLogo } from '@/constants/avatar.js';

export const useWorkspaceStore = defineStore('workspace', {
  state: () => ({
    currentOrgId: null,
    kycStatus: 2, // 預設 2 = APPROVED，避免初次載入前頁面按鈕閃爍
    orgName: '',
    orgLogo: '',
    myOrgs: [],          // 組織清單唯一來源（來自 GET /api/organizer/my-organizations）
    myOrgsLoaded: false  // 是否已成功（或自快取）載入過清單
  }),

  getters: {
    isKycApproved: (state) => state.kycStatus === 2,
    isKycPending: (state) => state.kycStatus === 1,
    isKycDraft: (state) => state.kycStatus === 0,
    isKycRejected: (state) => state.kycStatus === 3,
  },

  actions: {
    // 載入當前登入者所屬組織清單（唯一來源）。force=true 強制重新向後端拉取。
    async fetchMyOrgs(force = false) {
      if (this.myOrgsLoaded && !force) return this.myOrgs;
      try {
        const res = await axios.get('/api/organizer/my-organizations');
        const list = Array.isArray(res.data) ? res.data : [];
        this.myOrgs = list.map(org => ({
          id: org.id,
          name: org.name,
          logo: org.logo || getOrgDefaultLogo(org.name),
          role: org.role,
          kycStatus: org.kycStatus
        }));
        this.myOrgsLoaded = true;
      } catch (e) {
        console.error('載入組織清單失敗', e);
        // 後端無回應時沿用已持久化的清單（persist 自 localStorage 還原），避免誤判無權限；不注入假資料
      }
      return this.myOrgs;
    },

    // 建立 / 名稱或 Logo 變更後，就地更新清單中的單一組織（只覆寫有提供的欄位）
    upsertOrg(patch) {
      if (!patch || !patch.id) return;
      const idx = this.myOrgs.findIndex(o => o.id === patch.id);
      if (idx === -1) {
        this.myOrgs.push({
          id: patch.id,
          name: patch.name || '',
          logo: patch.logo || getOrgDefaultLogo(patch.name),
          role: patch.role,
          kycStatus: patch.kycStatus
        });
      } else {
        const cur = this.myOrgs[idx];
        this.myOrgs[idx] = {
          ...cur,
          ...(patch.name !== undefined ? { name: patch.name } : {}),
          ...(patch.logo !== undefined ? { logo: patch.logo || getOrgDefaultLogo(patch.name || cur.name) } : {}),
          ...(patch.role !== undefined ? { role: patch.role } : {}),
          ...(patch.kycStatus !== undefined ? { kycStatus: patch.kycStatus } : {})
        };
      }
    },

    // 自清單移除組織（註銷 / 退出後）
    removeOrg(orgId) {
      this.myOrgs = this.myOrgs.filter(o => o.id !== orgId);
    },

    // 切換組織時，設定 ID 並向後端載入該組織最新狀態
    setOrgId(orgId) {
      this.currentOrgId = orgId;
      this.loadWorkspaceData();
    },

    // 自後端載入當前組織詳細狀態（kyc_status / name / logo）
    async loadWorkspaceData() {
      if (!this.currentOrgId) return;
      const targetId = this.currentOrgId;
      try {
        const res = await axios.get(`/api/organizer/${targetId}`);
        // 避免在請求往返期間使用者已切換組織，導致狀態錯置
        if (this.currentOrgId !== targetId) return;

        const data = res.data || {};
        this.kycStatus = typeof data.kyc_status === 'number' ? data.kyc_status : 0;
        this.orgName = data.name || '';
        try {
          const kycData = JSON.parse(data.kyc_data_json || '{}');
          this.orgLogo = kycData.logo_url || '';
        } catch (e) {
          console.error('解析 kyc_data_json 失敗', e);
          this.orgLogo = '';
        }
      } catch (e) {
        console.error('載入組織詳細狀態失敗', e);
        // 後端無回應時，沿用清單中已知的名稱 / Logo / KYC 狀態，避免畫面跳動
        const known = this.myOrgs.find(o => o.id === targetId);
        if (known) {
          this.orgName = known.name || this.orgName;
          this.orgLogo = known.logo || this.orgLogo;
          if (typeof known.kycStatus === 'number') this.kycStatus = known.kycStatus;
        }
      }
    },

    // 提交審核時即時更新狀態（後端提交成功後呼叫，讓頂部 Banner 立即反映 PENDING）
    submitForVerification() {
      this.kycStatus = 1;
    }
  },

  // 持久化組織清單與當前組織狀態，重整 / 換頁時可同步還原，避免頭像名稱閃爍出預設值。
  // 刻意不保存 myOrgsLoaded，讓每個 session 首次進入仍會向後端刷新一次清單。
  persist: {
    pick: ['myOrgs', 'currentOrgId', 'orgName', 'orgLogo', 'kycStatus']
  }
});
