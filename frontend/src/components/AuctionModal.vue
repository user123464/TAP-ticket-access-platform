<template>
  <div v-if="show">
    <div class="modal show d-block" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <!-- header -->
          <div class="modal-header">
            <h5 class="modal-title">{{ data ? '修改競標' : '新增競標' }}</h5>
            <button type="button" class="btn-close" @click="close"></button>
          </div>
          <!-- body -->
          <div class="modal-body">

            <!-- 標題 -->
            <div class="mb-3">
              <label class="form-label">標題</label>
              <input
                type="text"
                class="form-control"
                v-model="form.title"
                placeholder="輸入競標商品標題"
              />
            </div>

            <!-- 詳細描述 -->
            <div class="mb-3">
              <label class="form-label">詳細描述</label>
              <textarea
                class="form-control"
                v-model="form.detail"
                rows="3"
                placeholder="輸入競標商品詳細描述"
              ></textarea>
            </div>

            <!-- 商品圖片 -->
            <div class="mb-3">
              <label class="form-label">商品圖片</label>
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

            <!-- 起標價 -->
            <div class="mb-3">
              <label class="form-label">起標價 (元)</label>
              <input
                type="number"
                class="form-control"
                v-model="form.startPrice"
                placeholder="0"
                step="0.01"
                min="0"
              />
            </div>

            <!-- 直購價 -->
            <div class="mb-3">
              <label class="form-label">直購價 (元)</label>
              <input
                type="number"
                class="form-control"
                v-model="form.buyoutPrice"
                placeholder="0"
                step="0.01"
                min="0"
              />
            </div>

            <!-- 競標開始時間 -->
            <div class="mb-3">
              <label class="form-label">競標開始</label>
              <FlatPickr v-model="form.startTime" :config="getPickerConfig('startTime')" class="form-control" />
            </div>

            <!-- 競標結束時間 -->
            <div class="mb-3">
              <label class="form-label">競標結束</label>
              <FlatPickr v-model="form.endTime" :config="getPickerConfig('endTime')" class="form-control" />
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
import { reactive, watch, computed, ref } from 'vue'
import FlatPickr from 'vue-flatpickr-component'
import 'flatpickr/dist/flatpickr.css'
import { useThemeStore } from '@/stores/themeStore'
import { resolveImageUrl } from '@/utils/imageUrl'

const props = defineProps({ data: Object })
const show = defineModel('show')
const emit = defineEmits(['submit'])

const isEdit = computed(() => !!props.data)

const form = reactive({
  auctionId: null,
  title: "",
  detail: "",
  image: "",
  startPrice: "",
  buyoutPrice: "",
  startTime: "",
  endTime: ""
})

const store = useThemeStore()

const fileInput = ref(null)
const isUploading = ref(false)
const uploadError = ref('')
const pendingImageFile = ref(null)
const pendingImageName = ref('')

const pickerConfig = {
  enableTime: true,
  time_24hr: true,
  minuteIncrement: 15,
  dateFormat: 'Y-m-d H:i',
  altInput: true,
  altFormat: 'Y年m月d日 H:i',
  allowInput: false,
  disableMobile: true
}

function normalizeDatetime(value) {
  if (!value) return ''

  const date = new Date(value)
  if (Number.isNaN(date.getTime())) {
    return String(value).replace('T', ' ').slice(0, 16)
  }

  const pad = (num) => String(num).padStart(2, '0')
  return [
    date.getFullYear(),
    pad(date.getMonth() + 1),
    pad(date.getDate())
  ].join('-') + ` ${pad(date.getHours())}:${pad(date.getMinutes())}`
}

function convertToApiFormat(dateTimeString) {
  if (!dateTimeString) return ''
  return dateTimeString.replace(' ', 'T').slice(0, 16)
}

function getPickerConfig(fieldKey) {
  const config = { ...pickerConfig }

  if (fieldKey === 'endTime') {
    config.minDate = form.startTime || null
  }

  return config
}

function populateForm(data) {
  form.auctionId = data.auctionId
  form.title = data.title
  form.detail = data.detail
  form.image = data.image || ""
  form.startPrice = data.startPrice
  form.buyoutPrice = data.buyoutPrice
  form.startTime = normalizeDatetime(data.startTime)
  form.endTime = normalizeDatetime(data.endTime)
}

function triggerFileInput() {
  fileInput.value?.click()
}

async function uploadImage(auctionId, file) {
  const data = await store.uploadAuctionImage(auctionId, file)
  form.image = data?.image || ''
}

async function handleUpload(event) {
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 2 * 1024 * 1024) {
    uploadError.value = '圖片大小不能超過 2MB'
    event.target.value = ''
    return
  }

  if (!form.auctionId) {
    pendingImageFile.value = file
    pendingImageName.value = file.name
    uploadError.value = ''
    event.target.value = ''
    return
  }

  isUploading.value = true
  uploadError.value = ''

  try {
    await uploadImage(form.auctionId, file)
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

  if (!form.auctionId) {
    form.image = ''
    pendingImageFile.value = null
    pendingImageName.value = ''
    return
  }

  try {
    const payload = { image: '' }
    await store.updateAuction(payload, form.auctionId)
    form.image = ''
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

  if (!form.detail) {
    alert('請輸入詳細描述')
    return
  }

  const startPrice = parseFloat(form.startPrice)
  const buyoutPrice = parseFloat(form.buyoutPrice)

  if (isNaN(startPrice) || startPrice <= 0) {
    alert('起標價必須大於 0')
    return
  }

  if (isNaN(buyoutPrice) || buyoutPrice <= 0) {
    alert('直購價必須大於 0')
    return
  }

  if (buyoutPrice < startPrice) {
    alert('直購價必須大於等於起標價')
    return
  }

  const startDateTime = convertToApiFormat(form.startTime)
  const endDateTime = convertToApiFormat(form.endTime)

  if (!startDateTime || !endDateTime) {
    alert('請輸入競標開始/結束時間')
    return
  }

  if (startDateTime >= endDateTime) {
    alert('競標結束時間必須晚於開始時間')
    return
  }

  if (isEdit.value && props.data?.status === 'ACTIVE') {
    alert('已發佈的競標無法修改')
    return
  }

  emit('submit', {
    auctionId: form.auctionId,
    title: form.title,
    detail: form.detail,
    image: form.image,
    startPrice: startPrice,
    buyoutPrice: buyoutPrice,
    startTime: startDateTime,
    endTime: endDateTime,
    pendingImageFile: pendingImageFile.value
  })
  close()
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
      populateForm(data)
    } else {
      reset()
    }
  },
  { immediate: true, deep: true }
)

function reset() {
  form.auctionId = null
  form.title = ""
  form.detail = ""
  form.image = ""
  form.startPrice = ""
  form.buyoutPrice = ""
  form.startTime = ""
  form.endTime = ""
}
</script>
