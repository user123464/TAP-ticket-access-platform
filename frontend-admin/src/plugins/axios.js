import axios from "axios";
import { useSystemBanner } from "@/composables/useSystemBanner";

const { showNetworkError, clearNetworkError } = useSystemBanner();

/**
 * Admin 後台 axios 實體 — JWT 認證版
 *
 * - request 攔截器自 localStorage 取 tap-admin-token 掛 Bearer header
 * - 401（Token 過期/未登入）統一導回登入頁
 * - 斷線（無 error.response）顯示系統錯誤橫幅；成功回應時清除斷線橫幅
 */
const instance = axios.create({
  baseURL: import.meta.env.VITE_BACKEND_API,
});

// Request Interceptor: 自動夾帶 JWT Token
instance.interceptors.request.use(
  function (config) {
    const token = localStorage.getItem("tap-admin-token");
    if (token) {
      config.headers["Authorization"] = `Bearer ${token}`;
    }
    return config;
  },
  function (error) {
    return Promise.reject(error);
  }
);

instance.interceptors.response.use(
  function (response) {
    // 連線恢復：收起先前的斷線橫幅
    clearNetworkError();
    return response;
  },
  function (error) {
    // 完全無回應（斷線 / 伺服器無回應）：顯示系統錯誤橫幅，不誤判成業務錯誤
    if (!error.response) {
      showNetworkError();
      return Promise.reject(error);
    }

    const status = error.response?.status;

    // Session 過期或未登入：清除本地登入狀態並導回登入頁
    if (status === 401 && window.location.pathname !== "/login") {
      localStorage.removeItem("tap-admin-auth");
      localStorage.removeItem("tap-admin-token");
      const redirect = encodeURIComponent(window.location.pathname + window.location.search);
      window.location.href = `/login?redirect=${redirect}`;
    }

    // [Dev Mode] 403 開發放行補丁 — 與 B2B frontend 同步
    // 開發環境且未開啟嚴格模式：僅 console 警告，不阻擋頁面
    // 若要測試正式 403 阻擋，可在瀏覽器 Console 執行: localStorage.setItem('strict_rbac', 'true')
    if (status === 403) {
      const isStrict = localStorage.getItem('strict_rbac') === 'true';
      if (import.meta.env.DEV && !isStrict) {
        console.warn(`[403 Dev Bypass] API "${error.config?.url}" 回傳了 403，開發模式已自動忽略跳轉。`);
      } else {
        window.location.href = "/login";
      }
    }

    return Promise.reject(error);
  }
);

export default instance;
