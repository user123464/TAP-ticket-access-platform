import { reactive } from 'vue';

// 全站系統公告條狀態（顯示於 navbar 上方、會把整頁往下推），由 <SystemBanner> 渲染。
// 兩種來源：
//   1. 手動維護公告 setAnnouncement() —— 平時關閉，需要時開啟。
//   2. 斷線/伺服器無回應 showNetworkError() —— 由 axios 攔截器自動觸發。
const bannerState = reactive({
  show: false,
  type: 'warning', // 'warning' | 'danger' | 'info'
  message: '',
  dismissible: true
});

// 手動維護公告（例如：2026/06/06 06:00 系統更新停機維護）
const setAnnouncement = (message, type = 'warning') => {
  bannerState.message = message;
  bannerState.type = type;
  bannerState.dismissible = true;
  bannerState.show = true;
};

// 斷線 / 伺服器無回應（由 axios 攔截器呼叫）；同型別已顯示則不重複洗版
const showNetworkError = (message = '網站目前無法連線，請檢查您的網路或稍後再試。') => {
  if (bannerState.show && bannerState.type === 'danger') return;
  bannerState.message = message;
  bannerState.type = 'danger';
  bannerState.dismissible = true;
  bannerState.show = true;
};

// 連線恢復時自動收起斷線公告（由 axios 成功攔截器呼叫）
const clearNetworkError = () => {
  if (bannerState.show && bannerState.type === 'danger') {
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
