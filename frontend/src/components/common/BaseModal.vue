<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue';
import { Modal } from 'bootstrap';

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    default: ''
  },
  size: {
    type: String,
    default: '' // '', 'modal-sm', 'modal-lg', 'modal-xl'
  },
  closeOnBackdrop: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['update:show', 'close', 'open']);

const modalRef = ref(null);
let modalInstance = null;

const initModal = () => {
  if (modalRef.value) {
    modalInstance = new Modal(modalRef.value, {
      backdrop: props.closeOnBackdrop ? true : 'static',
      keyboard: props.closeOnBackdrop
    });

    // 同步狀態回外部 v-model:show
    modalRef.value.addEventListener('hidden.bs.modal', () => {
      emit('update:show', false);
      emit('close');
    });

    modalRef.value.addEventListener('shown.bs.modal', () => {
      emit('open');
    });
  }
};

watch(() => props.show, (newVal) => {
  if (modalInstance) {
    if (newVal) {
      modalInstance.show();
    } else {
      modalInstance.hide();
    }
  }
});

onMounted(async () => {
  await nextTick();
  initModal();
  if (props.show && modalInstance) {
    modalInstance.show();
  }
});

onUnmounted(() => {
  if (modalInstance) {
    modalInstance.hide();
    modalInstance.dispose();
  }
});
</script>

<template>
  <div class="modal fade" ref="modalRef" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered" :class="size">
      <div class="modal-content rounded-4 border-0 shadow-lg">
        <!-- Header -->
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <slot name="header">
            <h5 class="modal-title fw-bold text-dark">{{ title }}</h5>
          </slot>
          <button type="button" class="btn-close" @click="emit('update:show', false)" aria-label="Close"></button>
        </div>
        
        <!-- Body -->
        <div class="modal-body p-4">
          <slot></slot>
        </div>
        
        <!-- Footer -->
        <div v-if="$slots.footer" class="modal-footer border-0 pt-0 pb-4 px-4 gap-2">
          <slot name="footer"></slot>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 避免 Bootstrap 預設的黑色背景與我們自訂的樣式衝突 */
.modal-backdrop {
  --bs-backdrop-bg: rgba(0, 0, 0, 0.5);
}
</style>
