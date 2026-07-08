import { defineStore } from "pinia";
import api from "@/plugins/axios.js";
import { clearCachedResources } from "@/composables/useCachedResource.js";

/**
 * auth.js — Admin 登入狀態 Store（JWT）
 *
 * JWT 認證流程：
 * 1. login() 成功後後端回傳 accessToken，寫入 localStorage 的 tap-admin-token
 * 2. 之後所有 API 請求由 axios request 攔截器自動掛 Bearer header
 * 3. 頁面重新整理時以 persist 還原 UI 狀態，路由守衛再以 fetchMe() 向後端驗證 Token 是否仍有效
 *
 * 地基批次：已移除 DEV mock fallback，登入 / 驗證一律以後端真實回應為準。
 */

// 角色代碼對照（與 SQL Schema Roles 一致）
export const ROLE_LABELS = {
  SUPER_ADMIN: "超級管理員",
  ADMIN: "一般管理員",
  CUSTOMER_SERVICE: "客服人員",
};

export const useAuthStore = defineStore("auth", {
  state: () => ({
    isLoggedIn: false,
    user: null, // { id, username, name, email, roleCode, avatarUrl }
  }),

  getters: {
    roleCode: (state) => state.user?.roleCode ?? null,
    roleLabel: (state) => ROLE_LABELS[state.user?.roleCode] ?? "未知角色",
    isSuperAdmin: (state) => state.user?.roleCode === "SUPER_ADMIN",
  },

  actions: {
    /** 帳密登入（單步驟，內網不需 Turnstile） */
    async login(username, password) {
      try {
        const { data } = await api.post("/api/auth/login", { email: username, password });

        // 儲存 JWT Token 到 LocalStorage
        localStorage.setItem("tap-admin-token", data.accessToken);

        // 立即呼叫 fetchMe() 載入包含權限在內的完整使用者資訊，以解決登入後未重新載入權限導致按鈕隱藏的 Bug
        await this.fetchMe();

        return { success: true };
      } catch (error) {
        // 斷線（無回應）由 axios 攔截器顯示橫幅；此處給通用訊息
        if (!error.response) {
          return { success: false, message: "無法連線至伺服器，請稍後再試" };
        }
        const message =
          error.response?.data?.message ??
          (error.response?.status === 401 ? "帳號或密碼錯誤" : "登入失敗，請稍後再試");
        return { success: false, message };
      }
    },

    /** 向後端確認 Token 是否仍有效（路由守衛、App 啟動時呼叫） */
    async fetchMe() {
      try {
        const { data } = await api.get("/api/auth/me");
        this.user = data;
        this.isLoggedIn = true;
        return true;
      } catch {
        // Token 失效 / 無效一律登出（斷線情境橫幅另由攔截器處理）
        localStorage.removeItem("tap-admin-token");
        clearCachedResources(); // Token 失效＝session 結束，一併清快取
        this.$reset();
        return false;
      }
    },

    /** 登出：後端撤銷 Token，前端清除狀態 */
    async logout() {
      try {
        await api.post("/api/auth/logout");
      } catch {
        // 後端未就緒或 Token 已失效都不影響前端登出
      }
      localStorage.removeItem("tap-admin-token");
      clearCachedResources(); // 清除 SWR 快取，避免上一位使用者資料殘留（共用電腦隱私）
      this.$reset();
    },
  },

  // 持久化 UI 狀態（Token 在 localStorage，這裡只存顯示用資料）
  persist: {
    key: "tap-admin-auth",
  },
});
