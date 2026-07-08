<script setup>
import { ref, nextTick, computed, onUnmounted } from 'vue';
import { Modal } from 'bootstrap';
import Cropper from 'cropperjs';
import BaseAvatar from '@/components/common/BaseAvatar.vue';
import 'cropperjs/dist/cropper.css';
import { useToast } from '@/composables/useToast';
import axios from '@/plugins/axios.js';

const toast = useToast();
const isSubmittingCrop = ref(false);

const props = defineProps({
  modelValue: {
    type: String,
    default: ''
  },
  mode: {
    type: String,
    default: 'personal', // 'personal' | 'org'
    validator: (val) => ['personal', 'org'].includes(val)
  },
  title: {
    type: String,
    default: '變更個人頭像'
  },
  orgName: {
    type: String,
    default: ''
  },
  organizerId: {
    type: String,
    default: ''
  },
  // true：裁切後「不」立即上傳，僅在本地預覽，等父層按儲存時呼叫 commitUpload 才真正上傳
  // false：維持「裁切即上傳」行為（組織 Logo 等流程）
  deferUpload: {
    type: Boolean,
    default: false
  }
});

const emit = defineEmits(['update:modelValue']);

// Modal 與 Cropper 實例
const modalRef = ref(null);
let modalInstance = null;
let cropperInstance = null;

// 裁剪與上傳狀態
const isCropping = ref(false);
const imageToCropSrc = ref('');
const cropImageRef = ref(null);
const fileInput = ref(null);

const isUploading = ref(false);
const uploadProgress = ref(0);
const dragOver = ref(false);

// 延後上傳模式：暫存裁切結果，待父層儲存時才上傳
const pendingBlob = ref(null);
const pendingPreviewUrl = ref('');

// 釋放本地預覽並清除待上傳檔案（切換成插圖／配色時呼叫）
const clearPending = () => {
  if (pendingPreviewUrl.value) {
    URL.revokeObjectURL(pendingPreviewUrl.value);
    pendingPreviewUrl.value = '';
  }
  pendingBlob.value = null;
};

// 個人模式：隨機插圖 Seed
const illustrationSeeds = ref([]);
const regenerateSeeds = () => {
  illustrationSeeds.value = Array.from({ length: 5 }, () => Math.random().toString(36).substring(7));
};

const selectIllustration = (seed) => {
  clearPending();
  const url = `boring:${seed}`;
  emit('update:modelValue', url);
};

// 組織模式：調色盤 5 色與首字元 SVG 生成
const orgColors = [
  { value: '#e57346', textColor: '#ffffff' }, // TAP Primary (暖橘)
  { value: '#1e293b', textColor: '#ffffff' }, // Dark Blue (深藍灰)
  { value: '#64748b', textColor: '#ffffff' }, // Light Blue (中度藍灰)
  { value: '#f8fafc', textColor: '#1e293b' }, // Light White (暖白)
  { value: '#e2e8f0', textColor: '#1e293b' }  // Light Gray (淺灰)
];

const orgInitials = computed(() => {
  if (!props.orgName) return 'O';
  const trimmed = props.orgName.trim();
  if (!trimmed) return 'O';
  return trimmed.charAt(0).toUpperCase();
});

const generateInitialsSvg = (bgColor, textColor) => {
  const char = orgInitials.value;
  const svg = `
    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 100 100" width="100" height="100">
      <rect width="100" height="100" fill="${bgColor}"/>
      <text x="50" y="50" dominant-baseline="central" text-anchor="middle" font-size="50" font-family="system-ui, -apple-system, sans-serif" font-weight="bold" fill="${textColor}">${char}</text>
    </svg>
  `.trim();
  return `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
};

const selectOrgColor = (bgColor, textColor) => {
  clearPending();
  const svgUrl = generateInitialsSvg(bgColor, textColor);
  emit('update:modelValue', svgUrl);
};

const isCurrentColorActive = (bgColor) => {
  if (!props.modelValue) return false;
  return props.modelValue.includes(encodeURIComponent(bgColor));
};

// 檔案處理
const processFile = (file) => {
  if (!file) return;

  const allowedTypes = ['image/jpeg', 'image/png', 'image/jpg'];
  if (!allowedTypes.includes(file.type)) {
    toast.error('只支援上傳 JPG 或 PNG 格式的圖片');
    return;
  }

  if (file.size > 2 * 1024 * 1024) {
    toast.error('上傳的圖片大小不能超過 2MB');
    return;
  }

  const reader = new FileReader();
  reader.onload = (e) => {
    imageToCropSrc.value = e.target.result;
    isUploading.value = false;
    isCropping.value = true;

    nextTick(() => {
      initCropper();
    });
  };
  reader.readAsDataURL(file);
};

const initCropper = () => {
  if (cropperInstance) {
    cropperInstance.destroy();
  }
  if (cropImageRef.value) {
    cropperInstance = new Cropper(cropImageRef.value, {
      aspectRatio: 1,
      viewMode: 1,
      dragMode: 'move',
      background: false,
      autoCropArea: 0.8,
      responsive: true,
      restore: true,
      checkCrossOrigin: false,
      cropBoxMovable: false,
      cropBoxResizable: false,
      toggleDragModeOnDblclick: false
    });
  }
};

const applyCrop = async () => {
  if (!cropperInstance) return;

  const croppedCanvas = cropperInstance.getCroppedCanvas({
    width: 250,
    height: 250,
    imageSmoothingEnabled: true,
    imageSmoothingQuality: 'high'
  });

  if (!croppedCanvas) {
    toast.error('裁剪失敗，請重新選擇圖片');
    return;
  }

  // Canvas 轉 Blob，準備上傳
  croppedCanvas.toBlob(async (blob) => {
    if (!blob) {
      toast.error('圖片處理失敗，請重新嘗試');
      return;
    }

    // 延後上傳模式：只在本地產生預覽（每次都是全新 blob URL，可避免同檔名 URL 不變導致畫面不更新），
    // 真正上傳延到父層按「儲存」時才透過 commitUpload 執行。
    if (props.deferUpload) {
      if (pendingPreviewUrl.value) URL.revokeObjectURL(pendingPreviewUrl.value);
      pendingBlob.value = blob;
      pendingPreviewUrl.value = URL.createObjectURL(blob);
      emit('update:modelValue', pendingPreviewUrl.value);

      // 收起裁剪畫面並關閉 Modal（尚未上傳，等使用者儲存）
      isCropping.value = false;
      isUploading.value = false;
      if (cropperInstance) {
        cropperInstance.destroy();
        cropperInstance = null;
      }
      modalInstance?.hide();
      return;
    }

    // 立即上傳模式（組織 Logo 等）：維持「裁切即上傳」行為
    // 記住上傳前的舊值，失敗時用來還原
    const originalUrl = props.modelValue;

    // 立即顯示本地裁切預覽（不等上傳完成），讓使用者看到即時效果
    const localPreviewUrl = URL.createObjectURL(blob);
    emit('update:modelValue', localPreviewUrl);

    isSubmittingCrop.value = true;

    try {
      // 以後端正式 URL 取代本地預覽（blob → api URL，Vue 偵測到變化並重渲染）
      const avatarUrl = await uploadBlob(blob);
      emit('update:modelValue', avatarUrl);
      URL.revokeObjectURL(localPreviewUrl);

      toast.success('頭像上傳成功');

      // 清理並關閉 Modal
      isCropping.value = false;
      isUploading.value = false;
      if (cropperInstance) {
        cropperInstance.destroy();
        cropperInstance = null;
      }
      modalInstance?.hide();

    } catch (err) {
      // 上傳失敗：釋放 blob 記憶體並還原舊頭像
      URL.revokeObjectURL(localPreviewUrl);
      emit('update:modelValue', originalUrl);
      console.error('頭像上傳失敗', err);
      toast.error(err.response?.data?.message || '上傳失敗，請稍後再試');
    } finally {
      isSubmittingCrop.value = false;
    }
  }, 'image/jpeg', 0.9);
};

// 將裁切後的 blob 上傳至後端，回傳後端正式 URL
const uploadBlob = async (blob) => {
  const formData = new FormData();
  formData.append('file', blob, 'avatar.jpg');

  // 根據模式選擇上傳端點
  const uploadUrl = props.mode === 'org' && props.organizerId
    ? `/api/organizer/${props.organizerId}/avatar`
    : '/api/user/avatar';

  const response = await axios.post(uploadUrl, formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  });
  return response.data.avatarUrl;
};

// 對外暴露：父層儲存時呼叫，上傳尚未送出的裁切頭像。
// 有待上傳檔案 → 回傳後端正式 URL；沒有（選了插圖／配色或未變更）→ 回傳 null。
const commitUpload = async () => {
  if (!pendingBlob.value) return null;
  const avatarUrl = await uploadBlob(pendingBlob.value);
  clearPending();
  return avatarUrl;
};

const cancelCrop = () => {
  isCropping.value = false;
  isUploading.value = false;
  uploadProgress.value = 0;
  imageToCropSrc.value = '';
  if (cropperInstance) {
    cropperInstance.destroy();
    cropperInstance = null;
  }
};

const handleFileUpload = (event) => {
  const file = event.target.files[0];
  processFile(file);
  event.target.value = '';
};

const handleDragOver = (e) => {
  e.preventDefault();
  dragOver.value = true;
};

const handleDragLeave = () => {
  dragOver.value = false;
};

const handleDrop = (e) => {
  e.preventDefault();
  dragOver.value = false;
  const files = e.dataTransfer.files;
  if (files.length > 0) {
    processFile(files[0]);
  }
};

// 對外暴露的 Modal 控制方法
const show = () => {
  if (props.mode === 'personal') {
    regenerateSeeds();
  }
  isUploading.value = false;
  uploadProgress.value = 0;
  isCropping.value = false;
  imageToCropSrc.value = '';
  if (cropperInstance) {
    cropperInstance.destroy();
    cropperInstance = null;
  }
  if (!modalInstance && modalRef.value) {
    modalInstance = new Modal(modalRef.value);
  }
  modalInstance?.show();
};

const hide = () => {
  modalInstance?.hide();
};

defineExpose({
  show,
  hide,
  commitUpload
});

onUnmounted(() => {
  if (pendingPreviewUrl.value) {
    URL.revokeObjectURL(pendingPreviewUrl.value);
  }
  if (cropperInstance) {
    cropperInstance.destroy();
  }
  if (modalInstance) {
    modalInstance.dispose();
  }
});
</script>

<template>
  <div class="modal fade" tabindex="-1" aria-hidden="true" ref="modalRef">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border-0 shadow-lg rounded-4">
        <div class="modal-header border-0 pb-0">
          <h5 class="modal-title fw-bold">{{ title }}</h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        <div class="modal-body py-3">
          <!-- 當不在裁剪模式時，顯示插圖與上傳 -->
          <div v-if="!isCropping">
            <!-- 情況 1：個人模式 (隨機插圖) -->
            <div v-if="mode === 'personal'" class="mb-4">
              <div class="d-flex justify-content-between align-items-center mb-2">
                <label class="form-label fw-semibold text-secondary mb-0">快速選擇推薦插圖</label>
                <button type="button" class="btn btn-link btn-sm text-decoration-none p-0 text-primary d-flex align-items-center" @click="regenerateSeeds">
                  <i class="bi bi-arrow-clockwise me-1"></i> 換一組
                </button>
              </div>
              <div class="d-flex justify-content-between align-items-center gap-2 px-1">
                <div 
                  v-for="(seed, index) in illustrationSeeds" 
                  :key="index"
                  class="illustration-item cursor-pointer border rounded-circle p-1"
                  :class="{ 'active-seed': modelValue.includes(seed) }"
                  @click="selectIllustration(seed)"
                >
                  <BaseAvatar :seed="seed" size="60" />
                </div>
              </div>
            </div>

            <!-- 情況 2：組織模式 (5種 SCSS 設計調色盤背景 + 首字元) -->
            <div v-else-if="mode === 'org'" class="mb-4">
              <label class="form-label fw-semibold text-secondary mb-2">預設品牌配色</label>
              <div class="d-flex justify-content-between align-items-center gap-2 px-1">
                <div 
                  v-for="(color, index) in orgColors" 
                  :key="index"
                  class="org-color-item cursor-pointer border rounded-circle p-1 d-flex align-items-center justify-content-center transition-all"
                  :class="{ 'active-color': isCurrentColorActive(color.value) }"
                  @click="selectOrgColor(color.value, color.textColor)"
                  :style="{ width: '60px', height: '60px', backgroundColor: color.value, borderColor: '#dee2e6' }"
                >
                  <span :style="{ color: color.textColor }" class="fw-bold fs-3">{{ orgInitials }}</span>
                </div>
              </div>
            </div>

            <!-- 分隔線 OR -->
            <div class="d-flex align-items-center my-4">
              <hr class="flex-grow-1 text-muted opacity-25 m-0" />
              <span class="px-3 text-secondary small fw-semibold">OR</span>
              <hr class="flex-grow-1 text-muted opacity-25 m-0" />
            </div>

            <!-- 下半部：上傳本地圖片 (共用) -->
            <div>
              <label class="form-label fw-semibold text-secondary mb-2">上傳自訂照片</label>
              
              <!-- 拖曳與點擊上傳區 -->
              <div 
                class="upload-dropzone border border-dashed rounded-3 p-4 text-center cursor-pointer position-relative transition-all"
                :class="{ 'drag-over': dragOver }"
                @dragover="handleDragOver"
                @dragleave="handleDragLeave"
                @drop="handleDrop"
                @click="fileInput.click()"
              >
                <input type="file" ref="fileInput" accept="image/jpeg, image/png" class="d-none" @change="handleFileUpload" />
                
                <div>
                  <i class="bi bi-cloud-arrow-up fs-2 text-muted mb-2 d-block"></i>
                  <span class="d-block small text-secondary">將檔案拖曳至此，或 <span class="text-primary fw-semibold">點擊瀏覽</span></span>
                  <span class="d-block text-muted" style="font-size: 0.75rem; margin-top: 4px;">支援 JPG / PNG，大小不超過 2MB</span>
                </div>
              </div>
            </div>
          </div>

          <!-- 當在裁剪模式時，顯示裁剪畫布 -->
          <div v-else class="cropper-container-wrapper py-2 fade-in">
            <label class="form-label fw-semibold text-secondary mb-2 d-block text-center">調整裁剪範圍</label>
            <p class="text-muted small text-center mb-3">您可以拖曳圖片移動位置，或使用滑鼠滾輪進行縮放</p>
            
            <!-- 裁剪畫布 -->
            <div class="cropper-canvas-box rounded-3 overflow-hidden border shadow-sm bg-light mx-auto position-relative" style="max-width: 100%; height: 320px;">
              <img ref="cropImageRef" :src="imageToCropSrc" alt="Image to crop" style="display: block; max-width: 100%;" />
            </div>
          </div>
        </div>
        <div class="modal-footer border-0 pt-0">
          <!-- 當不在裁剪模式時，顯示原本的一鍵確定按鈕 -->
          <button v-if="!isCropping" type="button" class="btn btn-primary text-white rounded-3 px-4 w-100 py-2" data-bs-dismiss="modal">
            確定
          </button>
          <!-- 當在裁剪模式時，顯示返回與套用裁剪按鈕 -->
          <div v-else class="d-flex gap-2 w-100">
            <button type="button" class="btn btn-outline-secondary w-50 py-2 rounded-3 fw-semibold" :disabled="isSubmittingCrop" @click="cancelCrop">
              返回上傳
            </button>
            <button type="button" class="btn btn-primary text-white w-50 py-2 rounded-3 fw-semibold" :disabled="isSubmittingCrop" @click="applyCrop">
              <span v-if="isSubmittingCrop" class="spinner-border spinner-border-sm me-2" role="status"></span>
              {{ isSubmittingCrop ? '上傳中...' : '確定裁剪' }}
            </button>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
.cropper-container-wrapper {
  text-align: center;
}

:deep(.cropper-view-box),
:deep(.cropper-face) {
  border-radius: 50%;
  outline: initial;
  border: 2px solid var(--tap-primary, #e57346);
}

:deep(.cropper-modal) {
  background-color: rgba(0, 0, 0, 0.65);
}

.cursor-pointer {
  cursor: pointer;
}

.illustration-item {
  transition: all 0.2s ease-in-out;
}

.illustration-item:hover {
  transform: scale(1.1);
  border-color: var(--tap-primary, #e57346) !important;
}

.illustration-item.active-seed {
  border-color: var(--tap-primary, #e57346) !important;
  border-width: 2px !important;
  box-shadow: 0 0 0 0.2rem rgba(229, 115, 70, 0.15);
}

.org-color-item {
  border-width: 2px !important;
  transition: all 0.2s ease-in-out;
}

.org-color-item:hover {
  transform: scale(1.1);
}

.org-color-item.active-color {
  border-color: var(--tap-primary, #e57346) !important;
  box-shadow: 0 0 0 0.25rem rgba(229, 115, 70, 0.2);
}

.border-dashed {
  border-style: dashed !important;
}

.upload-dropzone {
  border-width: 2px !important;
  border-color: #dee2e6 !important;
  background-color: #f8f9fa;
  transition: all 0.2s ease-in-out;
}

.upload-dropzone:hover, .upload-dropzone.drag-over {
  border-color: var(--tap-primary, #e57346) !important;
  background-color: rgba(229, 115, 70, 0.02);
}

.transition-all {
  transition: all 0.2s ease-in-out;
}

.fade-in {
  animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>
