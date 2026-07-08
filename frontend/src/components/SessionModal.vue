<template>
  <div v-if="show">
    <div class="modal show d-block" tabindex="-1">
      <div class="modal-dialog modal-dialog-centered">
        <div class="modal-content">
          <!-- header -->
          <div class="modal-header">
            <h5 class="modal-title">{{ data ? '修改場次' : '新增場次' }}</h5>
            <button type="button" class="btn-close" @click="close"></button>
          </div>
          <!-- body -->
          <div class="modal-body">

            <div v-for="field in formConfig" :key="field.key" class="mb-3">
              <label class="form-label">{{ field.label }}</label>
              <select
                v-if="field.type === 'select'"
                class="form-select"
                v-model="form[field.key]"
              >
                <option :value="null" disabled>請選擇地點</option>
                <option
                  v-for="location in locationOptions"
                  :key="location.locationId ?? location.id"
                  :value="location.locationId ?? location.id"
                >
                  {{ location.locationName ?? location.name }}
                </option>
              </select>

              <input
                v-else-if="field.type !== 'textarea'"
                :type="field.type"
                class="form-control"
                v-model="form[field.key]"
              />

              <textarea
                v-else
                class="form-control"
                v-model="form[field.key]"
              ></textarea>
            </div>
            
            <div class="mb-3">
              <label class="form-label">販售開始</label>
              <FlatPickr v-model="form.sellingStartTime" :config="getPickerConfig('sellingStartTime')" class="form-control" />
            </div>

            <div class="mb-3">
              <label class="form-label">販售結束</label>
              <FlatPickr v-model="form.sellingEndTime" :config="getPickerConfig('sellingEndTime')" class="form-control" />
            </div>

            <div class="mb-3">
              <label class="form-label">表演開始</label>
              <FlatPickr v-model="form.startTime" :config="getPickerConfig('startTime')" class="form-control" />
            </div>

            <div class="mb-3">
              <label class="form-label">表演結束</label>
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
import { reactive, watch, computed } from 'vue'
import { storeToRefs } from 'pinia'
import FlatPickr from 'vue-flatpickr-component'
import 'flatpickr/dist/flatpickr.css'
import { useThemeStore } from '@/stores/themeStore'

const props = defineProps({data: Object})
const show = defineModel('show')
const emit = defineEmits(['submit'])
const themeStore = useThemeStore()
const { locations: locationOptions } = storeToRefs(themeStore)

const isEdit = computed(() => !!props.data)

const formConfig = computed(() => {
  const base = [
    { key: "title", label: "標題", type: "text" },
    { key: "detail", label: "描述", type: "textarea" },
  ]
  if (!isEdit.value) {
    base.unshift({
      key: "locationId", label: "地點", type: "select" }
    )}
  return base
})

const form = reactive({
  sessionId: null,
  locationId: null,
  title: "",
  detail: "",
  startTime: "",
  endTime: "",
  sellingStartTime: "",
  sellingEndTime: ""
})

watch(
  () => show.value,
  async (visible) => {
    if (!visible || isEdit.value) return
    await themeStore.fetchLocations()
  }
)

watch(
  () => form.locationId,
  (value) => {
    if (value === null || value === '') return
    form.locationId = Number(value)
  }
)

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

  if (fieldKey === 'sellingStartTime') {
    config.maxDate = form.startTime || null
  }

  if (fieldKey === 'sellingEndTime') {
    config.minDate = form.sellingStartTime || null
    config.maxDate = form.startTime || null
  }

  return config
}

function populateForm(data) {
  form.sessionId = data.sessionId
  form.locationId = data.locationId
  form.title = data.title
  form.detail = data.detail

  form.startTime = normalizeDatetime(data.startTime)
  form.endTime = normalizeDatetime(data.endTime)
  form.sellingStartTime = normalizeDatetime(data.sellingStartTime)
  form.sellingEndTime = normalizeDatetime(data.sellingEndTime)
}

function submit() {
  if (!form.title) {
    alert('請輸入標題')
    return
  }

  if (!isEdit.value && !form.locationId) {
    alert('請選擇地點')
    return
  }

  const startDateTime = convertToApiFormat(form.startTime)
  const endDateTime = convertToApiFormat(form.endTime)
  const sellingStartDateTime = convertToApiFormat(form.sellingStartTime)
  const sellingEndDateTime = convertToApiFormat(form.sellingEndTime)

  if (!startDateTime || !endDateTime) {
    alert('請輸入 表演開始/表演結束 時間')
    return
  }

  if (startDateTime >= endDateTime) {
    alert('調整 表演結束>表演開始 時間')
    return
  }

  if (sellingStartDateTime && sellingEndDateTime && sellingStartDateTime > sellingEndDateTime) {
    alert('調整 販售結束>販售開始 時間')
    return
  }

  if (sellingEndDateTime && startDateTime && sellingEndDateTime > startDateTime) {
    alert('調整 販售結束<表演開始 時間')
    return
  }

  emit('submit', {
    sessionId: form.sessionId,
    locationId: form.locationId,
    title: form.title,
    detail: form.detail,
    startTime: startDateTime,
    endTime: endDateTime,
    sellingStartTime: sellingStartDateTime,
    sellingEndTime: sellingEndDateTime
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
  form.sessionId = null
  form.locationId = null
  form.title = ""
  form.detail = ""
  form.startTime = ""
  form.endTime = ""
  form.sellingStartTime = ""
  form.sellingEndTime = ""
}
</script>