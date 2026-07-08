<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue';
import MarkdownIt from 'markdown-it';
import api from '@/plugins/axios';

const props = defineProps({
  modelValue: {
    type: Boolean,
    default: false
  },
  docType: {
    type: String,
    required: true
  },
  title: {
    type: String,
    default: '線上合約簽署'
  },
  requireScrollToBottom: {
    type: Boolean,
    default: true
  }
});

const emit = defineEmits(['update:modelValue', 'agreed', 'cancelled']);

// 初始化 markdown-it
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
});

const currentRawContent = ref('');
const isLoading = ref(false);
const fetchError = ref('');
const hasScrolledToBottom = ref(false);
const modalContentRef = ref(null);

const htmlContent = computed(() => {
  if (currentRawContent.value) {
    return md.render(currentRawContent.value);
  }
  return '';
});

// 檢查是否捲動到底部
const checkScroll = () => {
  if (!props.requireScrollToBottom || hasScrolledToBottom.value) return;
  
  const el = modalContentRef.value;
  if (!el) return;
  
  // 如果內容高度小於或等於容器高度，直接解鎖
  if (el.scrollHeight <= el.clientHeight) {
    hasScrolledToBottom.value = true;
    return;
  }
  
  // 判斷是否捲動到底部（允許 20px 誤差）
  if (el.scrollTop + el.clientHeight >= el.scrollHeight - 20) {
    hasScrolledToBottom.value = true;
  }
};

const fetchDocument = async () => {
  isLoading.value = true;
  fetchError.value = '';
  currentRawContent.value = '';
  
  try {
    const response = await api.get(`/api/documents/${props.docType}.md`);
    if (response.data && response.data.success) {
      currentRawContent.value = response.data.content;
      
      // 內容載入後重新檢查捲動狀態
      nextTick(() => {
        if (!props.requireScrollToBottom) {
          hasScrolledToBottom.value = true;
        } else {
          // 重置狀態
          hasScrolledToBottom.value = false;
          // 使用 setTimeout 延遲 150ms 執行，確保瀏覽器已完成渲染與排版
          setTimeout(() => {
            checkScroll();
          }, 150);
        }
      });
    } else {
      fetchError.value = response.data?.message || '無法載入合約內容';
    }
  } catch (error) {
    console.error('Fetch document failed', error);
    fetchError.value = '載入合約時發生錯誤，請稍後再試。';
  } finally {
    isLoading.value = false;
  }
};

// 監聽 Modal 開關狀態
watch(() => props.modelValue, (newVal) => {
  if (newVal) {
    document.body.style.overflow = 'hidden';
    fetchDocument();
  } else {
    document.body.style.overflow = '';
  }
});

const handleAgree = () => {
  emit('agreed');
  closeModal();
};

const handleCancel = () => {
  emit('cancelled');
  closeModal();
};

const closeModal = () => {
  emit('update:modelValue', false);
};

// 處理點擊背景關閉
const handleBackdropClick = (e) => {
  if (e.target.classList.contains('modal')) {
    handleCancel();
  }
};

onMounted(() => {
  if (props.modelValue) {
    document.body.style.overflow = 'hidden';
    fetchDocument();
  }
});

onUnmounted(() => {
  document.body.style.overflow = '';
});
</script>

<template>
  <Transition name="backdrop-fade">
    <div v-if="modelValue" class="modal-backdrop"></div>
  </Transition>
  
  <Transition name="modal-zoom">
    <div 
      v-if="modelValue" 
      class="modal d-block" 
      tabindex="-1" 
      @click="handleBackdropClick"
      style="z-index: 1055;"
    >
      <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable modal-lg">
      <div class="modal-content rounded-4 border-0 shadow-lg modal-premium">
        <div class="modal-header border-bottom-0 pb-0 pt-2 px-4">
          <h5 class="modal-title fw-bold text-dark-blue d-flex align-items-center gap-2">
            <div class="header-icon-wrapper">
              <i class="bi bi-file-earmark-text-fill text-primary"></i>
            </div>
            {{ title }}
          </h5>
          <button type="button" class="btn-close-modern" @click="handleCancel" aria-label="Close">
            <i class="bi bi-x"></i>
          </button>
        </div>
        
        <div 
          class="modal-body p-4 position-relative" 
          ref="modalContentRef"
          @scroll="checkScroll"
        >
          <!-- 載入中狀態 (在一般流中展開，並延遲 200ms 顯示，防止極速載入時的閃爍) -->
          <div v-if="isLoading" class="d-flex flex-column align-items-center justify-content-center text-secondary flex-grow-1 spinner-delayed" style="min-height: 380px;">
            <div class="spinner-border text-primary mb-3" role="status">
              <span class="visually-hidden">Loading...</span>
            </div>
            <div class="fw-semibold">正在載入合約條款...</div>
          </div>
          
          <!-- 錯誤狀態 (Google/Apple 風格 Empty State) -->
          <div v-else-if="fetchError" class="d-flex flex-column align-items-center justify-content-center text-center px-4 flex-grow-1" style="min-height: 380px;">
            <div class="error-illustration-wrapper mb-4 mx-auto">
              <div class="error-glow-bg"></div>
              <i class="bi bi-wifi-off text-muted fs-2"></i>
            </div>
            <h6 class="fw-bold text-dark-blue mb-2" style="font-size: 1.1rem; letter-spacing: -0.01em;">連線服務暫時無法使用</h6>
            <p class="text-secondary small mb-4 mx-auto" style="line-height: 1.6; max-width: 320px;">
              {{ fetchError }}
            </p>
            <button class="btn btn-modern-outline d-flex align-items-center gap-2 mx-auto" @click="fetchDocument">
              <i class="bi bi-arrow-clockwise"></i> 重新載入條款
            </button>
          </div>
          
          <!-- 合約內容 -->
          <template v-else>
            <div class="markdown-body contract-content" v-html="htmlContent"></div>
          </template>
        </div>

        <!-- 漂浮的小橫幅圖塊：僅在需要滾動且未滾動到底部時顯示，絕對定位在底部中央 -->
        <transition name="fade">
          <div 
            v-if="requireScrollToBottom && !hasScrolledToBottom && !isLoading && !fetchError" 
            class="scroll-floating-banner shadow-sm"
          >
            <i class="bi bi-arrow-down-short text-primary animate-bounce-subtle" style="font-size: 0.95rem; line-height: 1;"></i>
            <span>向下捲動以解鎖同意按鈕</span>
          </div>
        </transition>
        
        <div class="modal-footer border-top-0 py-2 px-4 bg-light-premium rounded-bottom-4 d-flex justify-content-center">
          <div class="d-flex justify-content-center gap-3 w-100" style="max-width: 280px;">
            <button type="button" class="btn btn-cancel-modern w-50 py-1.5 rounded-3 fw-semibold border" @click="handleCancel">
              取消
            </button>
            <button 
              type="button" 
              class="btn w-50 py-1.5 rounded-3 fw-bold d-flex align-items-center justify-content-center gap-2 transition-premium" 
              :class="requireScrollToBottom && !hasScrolledToBottom ? 'btn-premium-locked' : 'btn-premium-unlocked'"
              :disabled="requireScrollToBottom && !hasScrolledToBottom"
              @click="handleAgree"
            >
              同意
            </button>
          </div>
        </div>
      </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.modal-backdrop {
  position: fixed;
  top: 0;
  left: 0;
  z-index: 1050;
  width: 100vw;
  height: 100vh;
  background-color: rgba(0, 0, 0, 0.55);
}

/* Backdrop fade transition */
.backdrop-fade-enter-active,
.backdrop-fade-leave-active {
  transition: opacity 0.2s ease;
}
.backdrop-fade-enter-from,
.backdrop-fade-leave-to {
  opacity: 0;
}

/* Modal zoom transition */
.modal-zoom-enter-active,
.modal-zoom-leave-active {
  transition: opacity 0.2s ease;
}
.modal-zoom-enter-active .modal-dialog,
.modal-zoom-leave-active .modal-dialog {
  transition: transform 0.25s cubic-bezier(0.34, 1.56, 0.64, 1);
}
.modal-zoom-enter-from,
.modal-zoom-leave-to {
  opacity: 0;
}
.modal-zoom-enter-from .modal-dialog,
.modal-zoom-leave-to .modal-dialog {
  transform: scale(0.95);
}

/* 漂浮滾動提示條 */
.scroll-floating-banner {
  position: absolute;
  bottom: 70px; /* 與下方頁尾保持適度間距 */
  left: 50%;
  transform: translateX(-50%);
  background-color: rgba(255, 255, 255, 0.95);
  border: 1px solid #e2e8f0;
  border-radius: 50px;
  padding: 4px 12px;
  font-size: 0.75rem;
  font-weight: 600;
  color: #64748b; /* Slate-500, 降低存在感 */
  display: flex;
  align-items: center;
  gap: 4px;
  z-index: 20;
  pointer-events: none; /* 防止遮擋鼠標點擊事件 */
  box-shadow: 0 4px 10px rgba(15, 23, 42, 0.06) !important;
}

/* Vue Transition fade 動畫 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s ease, transform 0.3s ease;
}
.fade-enter-from,
.fade-leave-to {
  opacity: 0;
  transform: translate(-50%, 10px);
}

.modal-premium {
  position: relative;
  background: #ffffff;
  border: 1px solid rgba(226, 232, 240, 0.8);
  box-shadow: 0 20px 25px -5px rgba(0, 0, 0, 0.05), 0 10px 10px -5px rgba(0, 0, 0, 0.02) !important;
}

.modal-body {
  display: flex;
  flex-direction: column;
}

@media (min-height: 680px) {
  .modal-body {
    min-height: 450px;
  }
}

.text-dark-blue {
  color: #1e293b;
}

.bg-light-premium {
  background-color: #f8fafc;
  border-top: 1px solid #f1f5f9;
}

/* 頂部圖示與關閉按鈕 */
.header-icon-wrapper {
  background: rgba(229, 115, 70, 0.08);
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 1.15rem;
}

.btn-close-modern {
  position: absolute;
  top: 12px; /* 完美水平對齊標題中心點 (Header pt-2 8px + 36px icon wrapper的一半 = 26px) */
  right: 24px; /* 對齊內容右側邊界 (px-4 = 24px) */
  z-index: 10;
  background: transparent;
  border: none;
  width: 28px; /* 固定寬度，使其與標題高度比例和諧 */
  height: 28px; /* 固定高度 */
  font-size: 1.2rem; /* 減小叉叉尺寸，與標題字體大小更協調 */
  color: #94a3b8;
  padding: 0;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
}
.btn-close-modern:hover {
  background-color: #f1f5f9;
  color: #475569;
}

/* 錯誤提示 (Apple 風格) */
.error-illustration-wrapper {
  position: relative;
  width: 64px;
  height: 64px;
  background: #f8fafc;
  border: 1px solid #e2e8f0;
  border-radius: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #64748b;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.02);
}
.error-glow-bg {
  position: absolute;
  width: 100%;
  height: 100%;
  background: radial-gradient(circle, rgba(239, 68, 68, 0.05) 0%, rgba(255, 255, 255, 0) 70%);
  z-index: 0;
  pointer-events: none;
}
.btn-modern-outline {
  background: #ffffff;
  border: 1px solid #cbd5e1;
  color: #475569;
  font-size: 0.875rem;
  font-weight: 600;
  padding: 8px 18px;
  border-radius: 10px;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
}
.btn-modern-outline:hover {
  background: #f8fafc;
  border-color: #94a3b8;
  color: #1e293b;
  transform: translateY(-1px);
}
.btn-modern-outline:active {
  transform: translateY(0);
}

/* 現代捲動提示條 */
.modern-scroll-prompt {
  background: rgba(255, 255, 255, 0.85);
  border: 1px solid rgba(226, 232, 240, 0.9);
  backdrop-filter: blur(8px);
  -webkit-backdrop-filter: blur(8px);
  padding: 6px 16px;
  border-radius: 50px;
  color: #475569;
  font-size: 0.825rem;
  font-weight: 500;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  box-shadow: 0 4px 12px rgba(15, 23, 42, 0.03);
}

/* 現代按鈕設計與解鎖轉場 */
.transition-premium {
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);
}

.btn-cancel-modern {
  background: #ffffff;
  border: 1px solid #e2e8f0 !important;
  color: #64748b;
  font-size: 0.925rem;
  font-weight: 600;
  transition: all 0.2s;
  cursor: pointer;
}
.btn-cancel-modern:hover {
  background: #f8fafc;
  color: #334155;
  border-color: #cbd5e1 !important;
}

.btn-premium-locked {
  background: #f1f5f9;
  color: #94a3b8;
  border: 1px solid #e2e8f0;
  font-size: 0.925rem;
  cursor: not-allowed !important;
}

.btn-premium-unlocked {
  background: var(--tap-primary, #e57346);
  color: #ffffff;
  border: 1px solid transparent;
  font-size: 0.925rem;
  box-shadow: 0 4px 14px rgba(229, 115, 70, 0.25);
  cursor: pointer;
}
.btn-premium-unlocked:hover {
  background: #d45f32;
  box-shadow: 0 6px 20px rgba(229, 115, 70, 0.35);
  transform: translateY(-1px);
}
.btn-premium-unlocked:active {
  transform: translateY(0);
}

/* 內容樣式 */
.contract-content {
  color: #334155;
  font-size: 0.95rem;
  line-height: 1.85;
}

.contract-content :deep(h1) {
  font-size: 1.4rem;
  font-weight: 800;
  margin-bottom: 1.25rem;
  color: #0f172a;
  border-bottom: 1px solid #e2e8f0;
  padding-bottom: 0.75rem;
}

.contract-content :deep(h2) {
  font-size: 1.15rem;
  font-weight: 700;
  margin-top: 1.75rem;
  margin-bottom: 0.75rem;
  color: #0f172a;
}

.contract-content :deep(p), .contract-content :deep(ul), .contract-content :deep(ol) {
  margin-bottom: 1.25rem;
}

.contract-content :deep(li) {
  margin-bottom: 0.5rem;
}

/* 微小動畫 */
.animate-bounce-subtle {
  animation: bounceSubtle 1.5s infinite;
}

@keyframes bounceSubtle {
  0%, 100% {
    transform: translateY(0);
  }
  50% {
    transform: translateY(3px);
  }
}

/* 自訂捲軸美化 */
.modal-body::-webkit-scrollbar {
  width: 6px;
}
.modal-body::-webkit-scrollbar-track {
  background: transparent;
}
.modal-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.modal-body::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
.animate-fade-in {
  animation: fadeIn 0.3s ease-in-out forwards;
}
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

/* 延遲顯示載入轉圈，優化感知效能 (Perceived Performance) */
.spinner-delayed {
  opacity: 0;
  animation: fadeInSpinner 0.2s ease-in-out 200ms forwards;
}
@keyframes fadeInSpinner {
  to {
    opacity: 1;
  }
}
</style>
