<script setup>
// 全域確認對話框：搭配 useConfirm() 使用，於 App.vue 掛載一次即可全站共用。
import BaseModal from '@/components/common/BaseModal.vue';
import { confirmState, settleConfirm } from '@/composables/useConfirm';

// 由 X / 背景 / Esc 關閉時，視為「取消」
const onUpdateShow = (val) => {
  if (!val) settleConfirm(false);
};
</script>

<template>
  <BaseModal
    :show="confirmState.show"
    :title="confirmState.title"
    size="modal-sm"
    @update:show="onUpdateShow"
  >
    <p class="mb-0 text-secondary confirm-msg">{{ confirmState.message }}</p>

    <template #footer>
      <button
        type="button"
        class="btn btn-light px-3 py-2 rounded-3 border fw-semibold"
        @click="settleConfirm(false)"
      >
        {{ confirmState.cancelText }}
      </button>
      <button
        type="button"
        class="btn px-4 py-2 rounded-3 fw-bold shadow-sm"
        :class="confirmState.variant === 'danger' ? 'btn-danger' : 'btn-primary text-white'"
        @click="settleConfirm(true)"
      >
        {{ confirmState.confirmText }}
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
.confirm-msg {
  line-height: 1.65;
  white-space: pre-line; /* 保留訊息中的換行符 */
}
</style>
