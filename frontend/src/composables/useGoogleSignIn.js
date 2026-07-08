// Google Identity Services（GIS）載入與初始化輔助
// index.html 已以 <script src="https://accounts.google.com/gsi/client" async defer> 載入 SDK，
// 但載入是非同步的，這裡提供一個 Promise 等待 window.google 就緒。

const GOOGLE_CLIENT_ID = import.meta.env.VITE_GOOGLE_CLIENT_ID;

/**
 * 等待 GIS SDK 載入完成，回傳 window.google.accounts.id。
 * 逾時（預設 8 秒）會 reject，呼叫端可據此顯示「Google 登入暫時無法使用」。
 */
export function loadGoogleIdentity(timeoutMs = 8000) {
  return new Promise((resolve, reject) => {
    if (window.google?.accounts?.id) {
      resolve(window.google.accounts.id);
      return;
    }
    const start = Date.now();
    const timer = setInterval(() => {
      if (window.google?.accounts?.id) {
        clearInterval(timer);
        resolve(window.google.accounts.id);
      } else if (Date.now() - start > timeoutMs) {
        clearInterval(timer);
        reject(new Error('Google 登入服務載入逾時'));
      }
    }, 100);
  });
}

export { GOOGLE_CLIENT_ID };
