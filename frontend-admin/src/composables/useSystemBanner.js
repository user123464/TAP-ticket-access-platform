import { reactive } from "vue";

/**
 * useSystemBanner — 全站系統公告 / 斷線橫幅（從 frontend 移植）
 *
 * 顯示於版面最上方的橫幅，由 <SystemBanner> 渲染。
 * 兩種來源：
 *   1. 手動維護公告 setAnnouncement() —— 平時關閉，需要時開啟。
 *   2. 斷線 / 伺服器無回應 showNetworkError() —— 由 axios 攔截器自動觸發。
 * 對應通知規格 a：系統錯誤（danger 橫幅）；b：狀態提示（warning / info 橫幅）。
 */
const bannerState = reactive({
  show: false,
  type: "warning", // 'warning' | 'danger' | 'info'
  message: "",
  dismissible: true,
});

// 手動維護公告 / 狀態提示
const setAnnouncement = (message, type = "warning") => {
  bannerState.message = message;
  bannerState.type = type;
  bannerState.dismissible = true;
  bannerState.show = true;
};

// 斷線 / 伺服器無回應（由 axios 攔截器呼叫）；同型別已顯示則不重複洗版
const showNetworkError = (message = "後台目前無法連線，請檢查您的網路或稍後再試。") => {
  if (bannerState.show && bannerState.type === "danger") return;
  bannerState.message = message;
  bannerState.type = "danger";
  bannerState.dismissible = true;
  bannerState.show = true;
};

// 連線恢復時自動收起斷線公告（由 axios 成功攔截器呼叫）
const clearNetworkError = () => {
  if (bannerState.show && bannerState.type === "danger") {
    bannerState.show = false;
  }
};

const dismiss = () => {
  bannerState.show = false;
};

export function useSystemBanner() {
  return { setAnnouncement, showNetworkError, clearNetworkError, dismiss };
}

export { bannerState };
