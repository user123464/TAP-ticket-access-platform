<template>
  <div v-if="show">
    <div class="modal show d-block" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <!-- header -->
          <div class="modal-header">
            <h5 class="modal-title">{{ data ? '修改活動' : '新增活動' }}</h5>
            <button type="button" class="btn-close" @click="close"></button>
          </div>
          <!-- body -->
          <div class="modal-body">

            <div class="mb-3">
              <label class="form-label">標題</label>
              <input class="form-control" v-model="form.title"/>
            </div>

            <div class="mb-3">
              <label class="form-label">描述</label>
              <textarea class="form-control" v-model="form.detail"></textarea>
            </div>

            <div class="mb-3">
              <label class="form-label">活動圖片</label>
              <div v-if="form.image" class="mb-2 position-relative d-inline-block border rounded overflow-hidden" style="max-width: 100%; max-height: 200px;">
                <img :src="resolveImageUrl(form.image)" class="img-fluid d-block mx-auto" style="max-height: 200px; object-fit: contain;" />
                <button type="button" class="btn btn-danger btn-sm position-absolute top-0 end-0 m-2 rounded-circle p-0 d-flex align-items-center justify-content-center" style="width: 24px; height: 24px; line-height: 1;" @click="clearImage" title="清除圖片">
                  <i class="bi bi-x-lg"></i>
                </button>
              </div>
              <div class="d-flex align-items-center gap-2">
                <input type="file" ref="fileInput" accept="image/*" class="d-none" @change="handleUpload" />
                <button type="button" class="btn btn-outline-primary btn-sm d-flex align-items-center gap-1" :disabled="isUploading" @click="triggerFileInput">
                  <span v-if="isUploading" class="spinner-border spinner-border-sm" role="status"></span>
                  <i v-else class="bi bi-cloud-arrow-up"></i>
                  {{ isUploading ? '上傳中...' : (form.image ? '變更圖片' : '上傳圖片') }}
                </button>
                <span v-if="pendingImageName" class="text-muted small ms-2">已選擇：{{ pendingImageName }}（送出後上傳）</span>
                <span v-if="uploadError" class="text-danger small ms-2">{{ uploadError }}</span>
              </div>
            </div>

          </div>
          <!-- footer -->
          <div class="modal-footer">
            <button class="btn btn-primary" @click="submit">{{ data ? '修改' : '新增' }}</button>
            <button class="btn btn-secondary" @click="close">關閉</button>            
          </div>
        </div>
      </div>
    </div>
    <!-- 背景遮罩 -->
    <div class="modal-backdrop fade show"></div>
  </div>
</template>

<script setup>
import { reactive, watch, ref } from 'vue'
import { useThemeStore } from '@/stores/themeStore'
import { resolveImageUrl } from '@/utils/imageUrl'

const store = useThemeStore()

const props = defineProps({data: Object})
const show = defineModel('show')
const emit = defineEmits(['submit'])

const form = reactive({
  themeId: null,
  title: '',
  detail: '',
  image: ''
})

const fileInput = ref(null)
const isUploading = ref(false)
const uploadError = ref('')
const pendingImageFile = ref(null)
const pendingImageName = ref('')

function triggerFileInput() {
  fileInput.value?.click()
}

async function uploadImage(themeId, file) {
  const data = await store.uploadThemeImage(themeId, file)
  form.image = data?.image || ''
}

// 處理圖片上傳
async function handleUpload(event) {
  // 確保有選擇檔案
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    uploadError.value = '圖片大小不能超過 2MB'
    // 清空檔案輸入的值，讓使用者可以重新選擇
    event.target.value = ''
    return
  }

  // 新增模式先暫存檔案，建立成功後再由外層流程上傳
  if (!form.themeId) {
    pendingImageFile.value = file
    pendingImageName.value = file.name
    uploadError.value = ''
    event.target.value = ''
    return
  }

  isUploading.value = true
  uploadError.value = ''

  try {
    await uploadImage(form.themeId, file)
    pendingImageFile.value = null
    pendingImageName.value = ''
  } catch (err) {
    console.error('Upload error:', err)
    uploadError.value = err.response?.data?.message || '上傳失敗，請稍後再試'
  } finally {
    isUploading.value = false
    event.target.value = ''
  }
}

async function clearImage() {
  uploadError.value = ''

  // 新增模式只清除暫存檔案
  if (!form.themeId) {
    form.image = ''
    pendingImageFile.value = null
    pendingImageName.value = ''
    return
  }

  try {
    const payload = {
      title: form.title,
      detail: form.detail,
      image: ''
    }
    const data = await store.clearThemeImage(form.themeId, payload)
    form.image = data?.image || ''
    pendingImageFile.value = null
    pendingImageName.value = ''
  } catch (err) {
    console.error('Clear image error:', err)
    uploadError.value = err.response?.data?.message || '清除圖片失敗，請稍後再試'
  }
}

function submit() {
  if (!form.title) {
    alert('請輸入標題')
    return
  }
  emit('submit', { ...form, pendingImageFile: pendingImageFile.value })
}

function close() {
  show.value = false
  reset()
}

watch(
  () => [show.value, props.data],
  ([visible, data]) => {
    if (!visible) return

    if (data) {
      form.themeId = data.themeId
      form.title = data.title
      form.detail = data.detail
      form.image = data.image
      pendingImageFile.value = null
      pendingImageName.value = ''
    } else {
      reset()
    }
  },
  { immediate: true, deep: true }
)

watch(
  show,
  (visible) => {
    // 新增模式下，重新開啟 modal 時強制重置，避免殘留上次輸入
    if (visible && !props.data) {
      reset()
    }
  }
)

function reset() {
  form.themeId = null
  form.title = ''
  form.detail = ''
  form.image = ''
  pendingImageFile.value = null
  pendingImageName.value = ''
  uploadError.value = ''
}
</script>