import { ref, onMounted, onUnmounted } from 'vue';

// 快取優先資料載入（SWR：stale-while-revalidate）
// 用途：解決「切換頁面 / 首次載入時，畫面先空白再被 API 資料覆蓋」造成的閃爍。
//
// 運作方式：
//   1. 建立當下立刻回傳 localStorage 裡的「上次資料」(data 一開始就有值，不是空的)。
//   2. 呼叫 refresh() 時背景打 API，回來後靜默更新 data 並寫回快取。
//   3. 因為一開始畫的就是上次的真實資料，使用者幾乎感覺不到切換 → 不閃。
//   4. 「切回視窗自動刷新」(revalidateOnFocus)：使用者從別的視窗/分頁切回後台時，
//      自動在背景重抓最新資料（含 dedupe 去抖，短時間內重複切換不會狂打 API）。
//
// 用法：
//   const { data, isLoading, error, refresh } = useCachedResource(
//     'user-profile',
//     () => axios.get('/api/user/profile').then(r => r.data)
//   );
//   // data.value 立刻可用（快取值），onMounted 時呼叫 refresh() 取最新

const PREFIX = 'cache:';

function readCache(storageKey, fallback) {
  try {
    const raw = localStorage.getItem(storageKey);
    return raw ? JSON.parse(raw) : fallback;
  } catch {
    return fallback;
  }
}

function writeCache(storageKey, value) {
  try {
    localStorage.setItem(storageKey, JSON.stringify(value));
  } catch {
    // localStorage 寫入失敗（如隱私模式 / 容量滿）時靜默忽略，不影響功能
  }
}

/**
 * 清除所有 cache: 前綴的 SWR 快取。
 * 用途：登出時呼叫，避免上一位使用者的後台資料殘留在 localStorage（共用電腦隱私）。
 */
export function clearCachedResources() {
  try {
    const keys = [];
    for (let i = 0; i < localStorage.length; i++) {
      const k = localStorage.key(i);
      if (k && k.startsWith(PREFIX)) keys.push(k);
    }
    keys.forEach((k) => localStorage.removeItem(k));
  } catch {
    // 存取 localStorage 失敗（如隱私模式）時靜默忽略
  }
}

/**
 * @param {string} key       此資源的唯一快取鍵（會自動加上 cache: 前綴）
 * @param {Function} fetcher 回傳 Promise 的取資料函式（通常包一層 axios）
 * @param {Object} [options]
 * @param {*} [options.initial=null] 連快取都沒有時的初始值
 * @param {boolean} [options.revalidateOnFocus=true] 切回視窗時自動背景刷新
 * @param {number} [options.dedupeMs=3000] 兩次自動刷新之間的最小間隔（去抖，避免狂打 API）
 */
export function useCachedResource(key, fetcher, options = {}) {
  const storageKey = PREFIX + key;
  const initial = options.initial ?? null;
  const revalidateOnFocus = options.revalidateOnFocus ?? true;
  const dedupeMs = options.dedupeMs ?? 3000;

  // 一開始就帶入快取值，畫面立即有東西可渲染
  const data = ref(readCache(storageKey, initial));
  const isLoading = ref(false);
  const error = ref(null);
  let lastFetchedAt = 0;

  // 背景向後端取最新資料，成功後更新 data 並寫回快取
  async function refresh() {
    isLoading.value = true;
    error.value = null;
    try {
      const result = await fetcher();
      data.value = result;
      writeCache(storageKey, result);
      lastFetchedAt = Date.now();
      return result;
    } catch (e) {
      error.value = e;
      throw e;
    } finally {
      isLoading.value = false;
    }
  }

  // 手動覆寫快取（例如表單儲存成功後，直接把最新值同步進快取，免得下次又閃舊值）
  function mutate(value) {
    data.value = value;
    writeCache(storageKey, value);
    lastFetchedAt = Date.now();
  }

  // ── 切回視窗自動刷新 ──
  // 分頁從背景切回前景（visibilitychange → visible）或視窗重新取得焦點時，
  // 若距離上次抓取已超過 dedupeMs 就在背景重抓。錯誤靜默忽略（避免焦點事件丟出
  // 未捕捉的 rejection；頁面自己的 onMounted refresh 仍會走原本的錯誤提示）。
  function onFocusRevalidate() {
    if (document.visibilityState && document.visibilityState !== 'visible') return;
    if (isLoading.value) return;
    if (Date.now() - lastFetchedAt < dedupeMs) return;
    refresh().catch(() => {});
  }

  if (revalidateOnFocus) {
    onMounted(() => {
      window.addEventListener('focus', onFocusRevalidate);
      document.addEventListener('visibilitychange', onFocusRevalidate);
    });
    onUnmounted(() => {
      window.removeEventListener('focus', onFocusRevalidate);
      document.removeEventListener('visibilitychange', onFocusRevalidate);
    });
  }

  return { data, isLoading, error, refresh, mutate };
}
