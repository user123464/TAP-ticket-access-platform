import { ref } from 'vue';

// 快取優先資料載入（SWR：stale-while-revalidate）
// 用途：解決「切換頁面 / 首次載入時，畫面先空白再被 API 資料覆蓋」造成的閃爍。
//
// 運作方式：
//   1. 建立當下立刻回傳 localStorage 裡的「上次資料」(data 一開始就有值，不是空的)。
//   2. 呼叫 refresh() 時背景打 API，回來後靜默更新 data 並寫回快取。
//   3. 因為一開始畫的就是上次的真實資料，使用者幾乎感覺不到切換 → 不閃。
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
 * @param {string} key       此資源的唯一快取鍵（會自動加上 cache: 前綴）
 * @param {Function} fetcher 回傳 Promise 的取資料函式（通常包一層 axios）
 * @param {Object} [options]
 * @param {*} [options.initial=null] 連快取都沒有時的初始值
 */
export function useCachedResource(key, fetcher, options = {}) {
  const storageKey = PREFIX + key;
  const initial = options.initial ?? null;

  // 一開始就帶入快取值，畫面立即有東西可渲染
  const data = ref(readCache(storageKey, initial));
  const isLoading = ref(false);
  const error = ref(null);

  // 背景向後端取最新資料，成功後更新 data 並寫回快取
  async function refresh() {
    isLoading.value = true;
    error.value = null;
    try {
      const result = await fetcher();
      data.value = result;
      writeCache(storageKey, result);
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
  }

  return { data, isLoading, error, refresh, mutate };
}
