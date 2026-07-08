<script setup>
import { ref, watch, computed } from 'vue';

const props = defineProps({
  keyword: {
    type: String,
    default: "",
  },
  keywordPlaceholder: {
    type: String,
    default: "搜尋...",
  },
  fromDate: {
    type: String,
    default: "",
  },
  toDate: {
    type: String,
    default: "",
  },
  showDateRange: {
    type: Boolean,
    default: false,
  },
  // 日期範圍篩選的對象欄位標籤（如「建立時間」「提交時間」），讓使用者明確知道篩的是哪個時間。
  dateLabel: {
    type: String,
    default: "",
  },
  // 是否有額外的篩選條件處於啟用狀態，用來決定清除按鈕是否顯示
  hasExtraFilters: {
    type: Boolean,
    default: false,
  }
});

const emit = defineEmits(["update:keyword", "update:fromDate", "update:toDate", "clear"]);

const internalKeyword = ref(props.keyword);
const internalFromDate = ref(props.fromDate);
const internalToDate = ref(props.toDate);

// 記錄初始值以供重設與髒狀態判定
const initialKeyword = props.keyword;
const initialFromDate = props.fromDate;
const initialToDate = props.toDate;

watch(() => props.keyword, (val) => internalKeyword.value = val);
watch(() => props.fromDate, (val) => internalFromDate.value = val);
watch(() => props.toDate, (val) => internalToDate.value = val);

const onKeywordInput = (e) => {
  internalKeyword.value = e.target.value;
  emit("update:keyword", e.target.value);
};

const onFromDateInput = (e) => {
  internalFromDate.value = e.target.value;
  emit("update:fromDate", e.target.value);
};

const onToDateInput = (e) => {
  internalToDate.value = e.target.value;
  emit("update:toDate", e.target.value);
};

const isDirty = computed(() => {
  return internalKeyword.value !== initialKeyword ||
         internalFromDate.value !== initialFromDate ||
         internalToDate.value !== initialToDate ||
         props.hasExtraFilters;
});

const clear = () => {
  internalKeyword.value = initialKeyword;
  internalFromDate.value = initialFromDate;
  internalToDate.value = initialToDate;
  emit("update:keyword", initialKeyword);
  emit("update:fromDate", initialFromDate);
  emit("update:toDate", initialToDate);
  emit("clear");
};
</script>

<template>
  <div class="card border shadow-sm mb-3">
    <div class="card-body py-3 px-3">
      <div class="row gx-3 gy-3 align-items-center">
        <!-- 外部自訂篩選項目 (例如 select 下拉選單) -->
        <slot></slot>

        <!-- 關鍵字搜尋 -->
        <div class="col-12 col-md-auto flex-grow-1 min-w-0" style="min-width: 200px;">
          <div class="input-group input-group-sm">
            <span class="input-group-text bg-transparent text-tap-secondary"><i class="bi bi-search"></i></span>
            <input 
              :value="internalKeyword"
              @input="onKeywordInput"
              type="text" 
              class="form-control" 
              :placeholder="keywordPlaceholder" 
            />
            <button 
              v-if="isDirty" 
              class="btn btn-outline-secondary border-start-0" 
              type="button" 
              @click="clear" 
              title="清除所有篩選條件"
            >
              <i class="bi bi-x-lg"></i>
            </button>
          </div>
        </div>

        <!-- 自定義日期/額外插槽 -->
        <slot name="date"></slot>

        <!-- 日期範圍 -->
        <div v-if="showDateRange" class="col-12 col-md-auto">
          <div class="input-group input-group-sm">
            <span class="input-group-text bg-transparent text-tap-secondary">
              <i class="bi bi-calendar3 me-1"></i>{{ dateLabel || "日期" }}
            </span>
            <input 
              type="date" 
              class="form-control text-tap-secondary" 
              :value="internalFromDate" 
              @input="onFromDateInput" 
              title="開始日期" 
            />
            <span class="input-group-text bg-transparent text-tap-secondary border-start-0 border-end-0 px-2">至</span>
            <input 
              type="date" 
              class="form-control text-tap-secondary" 
              :value="internalToDate" 
              @input="onToDateInput" 
              title="結束日期" 
            />
          </div>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
/* 讓 input-group 中間的邊框能順暢顯示 */
.input-group > .form-control:focus {
  z-index: 3;
}
/* 讓日期 input 原生的 placeholder 顏色也是 secondary */
input[type="date"]::-webkit-datetime-edit-year-field,
input[type="date"]::-webkit-datetime-edit-month-field,
input[type="date"]::-webkit-datetime-edit-day-field,
input[type="date"]::-webkit-datetime-edit-text {
  color: inherit;
}
input[type="date"]::-webkit-calendar-picker-indicator {
  filter: invert(0.6);
}
</style>
