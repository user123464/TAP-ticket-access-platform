<script setup>
import { ref, computed } from 'vue';
import { useCapsLock } from '@/composables/useCapsLock.js';

// 共用密碼輸入欄位：內建 show/hide、Caps Lock 右側提示、底部固定一行 helper（提示出現不撐版）。
// 視覺語彙對齊登入頁，但提示採「下方一行」型態（設定頁提示較多）。登入頁維持原樣不共用此元件。
const props = defineProps({
  modelValue: { type: String, default: '' },
  id: { type: String, required: true },
  label: { type: String, default: '' },
  placeholder: { type: String, default: '' },
  autocomplete: { type: String, default: 'off' },
  // 'default' | 'error' | 'success' | 'warning'
  state: { type: String, default: 'default' },
  // 底部提示文字（空字串時仍保留一行高度，避免版面跳動）
  hint: { type: String, default: '' },
});

const emit = defineEmits(['update:modelValue', 'blur']);

const { isCapsLockOn } = useCapsLock();
const showPassword = ref(false);
const isFocused = ref(false);

// Caps Lock 僅在開啟且使用者正在此欄位（聚焦或已有輸入）時提示
const showCaps = computed(() => isCapsLockOn.value && (isFocused.value || !!props.modelValue));

const hintClass = computed(() => ({
  error: 'text-danger',
  success: 'text-success',
  warning: 'text-warning',
  default: 'text-secondary',
}[props.state] || 'text-secondary'));

const hintIcon = computed(() => ({
  error: 'bi-x-circle',
  success: 'bi-check-circle',
  warning: 'bi-exclamation-circle',
  default: '',
}[props.state] || ''));

const onInput = (e) => emit('update:modelValue', e.target.value);
const onBlur = () => { isFocused.value = false; emit('blur'); };
</script>

<template>
  <div class="password-field mb-1">
    <label v-if="label" :for="id" class="form-label fw-semibold text-dark mb-1">{{ label }}</label>

    <div class="position-relative">
      <input
        :id="id"
        :type="showPassword ? 'text' : 'password'"
        class="form-control rounded-3 pwd-input"
        :class="{ 'is-error': state === 'error', 'is-success': state === 'success', 'pwd-input--caps': showCaps }"
        :value="modelValue"
        :placeholder="placeholder"
        :autocomplete="autocomplete"
        @input="onInput"
        @focus="isFocused = true"
        @blur="onBlur"
      />

      <!-- 右側固定：Caps Lock 指示（條件顯示）＋ 顯示/隱藏密碼 -->
      <div class="position-absolute top-50 translate-middle-y end-0 d-flex align-items-center gap-2 pe-3" style="z-index: 5;">
        <span v-if="showCaps" class="caps-hint text-muted" title="Caps Lock 已開啟">Caps Lock</span>
        <button type="button" class="eye-toggle-btn" tabindex="-1" @click="showPassword = !showPassword">
          <i :class="showPassword ? 'bi bi-eye-slash-fill' : 'bi bi-eye-fill'"></i>
        </button>
      </div>
    </div>

    <!-- 底部固定一行 helper text：永遠占位，提示出現也不撐大卡片 -->
    <div class="pwd-hint small mt-1" :class="hintClass">
      <template v-if="hint">
        <i v-if="hintIcon" class="bi me-1" :class="hintIcon"></i>{{ hint }}
      </template>
    </div>
  </div>
</template>

<style scoped>
.pwd-input {
  padding-right: 3rem !important; /* 預留右側眼睛圖示空間 */
}
.pwd-input.pwd-input--caps {
  padding-right: 7rem !important; /* Caps Lock 文字顯示時，加大右側空間避免蓋住輸入內容 */
}

/* Caps Lock 提示：灰色淺字，對齊登入頁的設計語彙（text-muted + 半透明） */
.caps-hint {
  font-size: 0.8rem;
  font-weight: 400;
  opacity: 0.5;
  white-space: nowrap;
  user-select: none;
}
.pwd-input.is-error {
  background-image: none !important;
  border-color: #fca5a5 !important;
  box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12) !important;
}
.pwd-input.is-success {
  border-color: #86efac !important;
}

/* 固定一行高度，提示有無都占位 → 不跳動 */
.pwd-hint {
  min-height: 1.25rem;
  line-height: 1.25rem;
}

.eye-toggle-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: #6c757d;
  opacity: 0.55;
  transition: opacity 0.2s ease, color 0.2s ease;
  cursor: pointer;
  outline: none;
  box-shadow: none !important;
  display: flex;
  align-items: center;
  justify-content: center;
}
.eye-toggle-btn:hover {
  opacity: 0.9;
  color: #212529;
}
</style>
