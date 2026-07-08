<script setup>
/**
 * ConfirmDialog.vue — 確認對話框（Admin 深色版，雙模式）
 *
 * 為了同時支援：
 *   1.【全域模式】搭配 useConfirm()（App.vue 掛載一次，props 傳 global）。
 *      對應通知規格 d：禁止 / 危險操作（彈窗確認）。
 *   2.【受控模式】既有頁面以 v-model:show + props/slot 局部使用（KycReview、ContractEdit…）。
 * 本元件用 `global` prop 切換來源，避免破壞既有頁面的使用方式。
 * 由 frontend 全域版移植，色系改為 Admin 深色主題。
 */
import { computed } from "vue";
import BaseModal from "@/components/common/BaseModal.vue";
import { confirmState, settleConfirm } from "@/composables/useConfirm";

const props = defineProps({
  // 全域模式：讀 useConfirm 的 confirmState；不傳則為受控模式
  global: { type: Boolean, default: false },
  // ── 以下為受控模式 props（既有頁面使用） ──
  show: { type: Boolean, default: false },
  title: { type: String, default: "確認操作" },
  message: { type: String, default: "" },
  confirmText: { type: String, default: "確認" },
  cancelText: { type: String, default: "取消" },
  variant: { type: String, default: "primary" },
  loading: { type: Boolean, default: false },
});

const emit = defineEmits(["update:show", "confirm", "cancel"]);

// 依模式決定實際顯示資料來源
const visible = computed(() => (props.global ? confirmState.show : props.show));
const titleText = computed(() => (props.global ? confirmState.title : props.title));
const messageText = computed(() => (props.global ? confirmState.message : props.message));
const confirmLabel = computed(() => (props.global ? confirmState.confirmText : props.confirmText));
const cancelLabel = computed(() => (props.global ? confirmState.cancelText : props.cancelText));
const variantClass = computed(() => {
  const v = props.global ? confirmState.variant : props.variant;
  if (v === "danger") return "btn-danger";
  if (v === "success") return "btn-success";
  return "btn-primary";
});

const onUpdateShow = (val) => {
  if (props.global) {
    if (!val) settleConfirm(false); // X / 背景 / Esc 關閉視為取消
  } else {
    emit("update:show", val);
  }
};

const onConfirm = () => {
  if (props.global) {
    settleConfirm(true);
  } else {
    emit("confirm");
  }
};

const onCancel = () => {
  if (props.global) {
    settleConfirm(false);
  } else {
    emit("update:show", false);
    emit("cancel");
  }
};
</script>

<template>
  <BaseModal :show="visible" :title="titleText" size="modal-sm" @update:show="onUpdateShow">
    <!-- 受控模式可用 default slot 放自訂內容（如退件原因輸入框）；全域模式顯示 message -->
    <slot>
      <p class="mb-0 confirm-msg">{{ messageText }}</p>
    </slot>

    <template #footer>
      <div class="confirm-actions">
        <button
          type="button"
          class="btn btn-outline-secondary py-2 rounded-3 fw-semibold text-nowrap m-0"
          :disabled="loading"
          @click="onCancel"
        >
          {{ cancelLabel }}
        </button>
        <button
          type="button"
          class="btn py-2 rounded-3 fw-bold shadow-sm text-white d-inline-flex align-items-center justify-content-center text-nowrap m-0"
          :class="variantClass"
          :disabled="loading"
          @click="onConfirm"
        >
          <span v-if="loading" class="spinner-border spinner-border-sm" role="status" aria-label="處理中"></span>
          <span v-else>{{ confirmLabel }}</span>
        </button>
      </div>
    </template>
  </BaseModal>
</template>

<style scoped>
.confirm-msg {
  line-height: 1.65;
  white-space: pre-line; /* 保留訊息中的換行符 */
  color: var(--tap-text-secondary);
}

/* footer 只放這一個容器，撐滿寬度；不再依賴 Bootstrap 的 flex 對齊 */
.confirm-actions {
  display: grid;
  grid-template-columns: 1fr 1fr; /* 純比例切版，無視按鈕內容 → 永遠等寬 */
  gap: 0.5rem;
  width: 100%;
}

/* 按鈕固定高度，loading 文字↔spinner 切換時高度也不跳動 */
.confirm-actions .btn {
  min-width: 0; /* 允許 grid 欄收縮，內容過長不撐破版面 */
  min-height: 44px;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>
