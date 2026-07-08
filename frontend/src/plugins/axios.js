import axios from "axios";
import { useSystemBanner } from "@/composables/useSystemBanner";

const instance = axios.create({
    baseURL: import.meta.env.VITE_BACKEND_API,
    timeout: 10000,
});

const systemBanner = useSystemBanner();

// 1. 請求攔截器 (Request Interceptor)
instance.interceptors.request.use(
    function (config) {
        const token = localStorage.getItem("access_token");
        if (token) {
            config.headers["Authorization"] = `Bearer ${token}`;
        }
        return config;
    },
    function (error) {
        return Promise.reject(error);
    },
);

// 2. 回應攔截器 (Response Interceptor)
instance.interceptors.response.use(
    function (response) {
        // 請求成功 = 連線正常，自動收起先前的斷線公告
        systemBanner.clearNetworkError();
        return response;
    },
    function (error) {
        // 無 response 代表斷線 / 伺服器無回應 / CORS，於最頂端顯示全站斷線公告
        if (!error.response) {
            systemBanner.showNetworkError();
            return Promise.reject(error);
        }

        if (error.response && error.response.status) {
            const status = error.response.status;

            if (status === 401) {
                // 檢查請求配置中是否要求跳過自動登出跳轉處理
                if (error.config?._skip401Redirect) {
                    return Promise.reject(error);
                }

                // Token 無效或已過期，清空前端登入狀態並重新載入
                localStorage.removeItem("is_logged_in");
                localStorage.removeItem("access_token");
                localStorage.removeItem("user_email");
                localStorage.removeItem("user_name");
                localStorage.removeItem("user_avatar");
                localStorage.removeItem("user_profile");

                // 派發自訂事件通知 Navbar / Header 更新 UI 狀態
                window.dispatchEvent(new Event("profile-updated"));

                // 導回登入頁
                window.location.href = "/login";
             } else if (status === 403) {
                 // 個別請求可帶 _skip403Redirect 自行處理 403（例如付款成功頁的訂單明細降級顯示），
                 // 不做全頁跳轉，直接把錯誤丟回呼叫端的 catch（與上方 _skip401Redirect 同一機制）
                 // 💡 所有 /api/auth/ 相關的 API（登入、註冊、驗證碼）皆不應觸發 403 全頁跳轉，直接將錯誤交由呼叫端處理。
                 const isAuthApi = error.config?.url && error.config.url.includes("/api/auth/");
                 if (error.config?._skip403Redirect || isAuthApi) {
                     return Promise.reject(error);
                 }
                 const isStrict = localStorage.getItem('strict_rbac') === 'true';
                 if (import.meta.env.DEV && !isStrict) {
                     console.warn(`[403 Dev Bypass] API "${error.config?.url}" 回傳了 403，已忽略跳轉。`);
                     import("@/composables/useToast.js").then(({ useToast }) => {
                         useToast().error("403", 3000);
                     });
                 } else {
                     window.location.href = "/403";
                 }
             }
        }
        return Promise.reject(error);
    },
);

export default instance;
