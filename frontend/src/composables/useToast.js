import { ref } from 'vue';

// 全域共享的 toast 佇列 (Single Source of Truth)，供 <ToastContainer> 渲染。
// 任何元件呼叫 useToast() 取得 success / error / info 方法即可推播短暫的動作回饋。
const toasts = ref([]);
let seq = 0;

// 各型別預設停留時間：錯誤停久一點讓使用者讀完，成功/資訊較短。
const DEFAULT_DURATION = {
  success: 3000,
  info: 3500,
  error: 5000
};

const remove = (id) => {
  const idx = toasts.value.findIndex(t => t.id === id);
  if (idx !== -1) toasts.value.splice(idx, 1);
};

const push = (type, message, duration) => {
  const id = ++seq;
  const ttl = duration ?? DEFAULT_DURATION[type] ?? 3500;
  toasts.value.push({ id, type, message });
  if (ttl > 0) {
    setTimeout(() => remove(id), ttl);
  }
  return id;
};

export function useToast() {
  return {
    toasts,
    remove,
    success: (message, duration) => push('success', message, duration),
    error: (message, duration) => push('error', message, duration),
    info: (message, duration) => push('info', message, duration)
  };
}
