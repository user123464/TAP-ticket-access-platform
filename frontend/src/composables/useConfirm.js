import { reactive } from 'vue';

// 全域共享的確認對話框狀態（同時間僅一個），由 <ConfirmDialog> 渲染。
// 取代原生 confirm()：呼叫 confirm({...}) 回傳 Promise<boolean>，
// 讓 `if (confirm(...))` 可直接改寫為 `if (await confirm(...))`。
const confirmState = reactive({
  show: false,
  title: '請確認',
  message: '',
  confirmText: '確認',
  cancelText: '取消',
  variant: 'primary' // 'primary' | 'danger'
});

let resolver = null;

const open = (opts = {}) => {
  confirmState.title = opts.title || '請確認';
  confirmState.message = opts.message || '';
  confirmState.confirmText = opts.confirmText || '確認';
  confirmState.cancelText = opts.cancelText || '取消';
  confirmState.variant = opts.variant || 'primary';
  confirmState.show = true;
  return new Promise((resolve) => {
    resolver = resolve;
  });
};

// 結算並回傳結果（重複呼叫安全：resolver 用後即清空）
const settleConfirm = (result) => {
  confirmState.show = false;
  if (resolver) {
    resolver(result);
    resolver = null;
  }
};

export function useConfirm() {
  return { confirm: open };
}

export { confirmState, settleConfirm };
