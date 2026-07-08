<script setup>
/**
 * ExportButtons.vue — 通用「匯出 Excel」按鈕
 *
 * 封裝 useExport，任何有資料的列表頁皆可直接套用：
 *   <ExportButtons :file-name="fileName" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="手續費明細" />
 *
 * - rows：已整理成可匯出格式的資料列（欄位值請先轉成字串/數字，避免匯出物件）
 * - columns：{ 資料key: 中文標題 } 對照，決定匯出欄位順序與標題
 * - 無資料或 disabled 時自動禁用，避免匯出空檔
 * - 資料量保護：超過 maxRows（預設 10,000）時中止並提示，避免瀏覽器在前端組大型
 *   xlsx 時凍結／當掉。資料量更大時應改走後端串流匯出。
 */
import { computed } from "vue";
import { useExport } from "@/composables/useExport.js";
import { useToast } from "@/composables/useToast.js";

const props = defineProps({
  fileName: { type: String, required: true },
  rows: { type: Array, default: () => [] },
  columns: { type: Object, required: true },
  sheetName: { type: String, default: "資料" },
  disabled: { type: Boolean, default: false },
  // 單次匯出上限：超過則中止並提示使用者縮小範圍（防止前端組大型 xlsx 凍結）
  maxRows: { type: Number, default: 10000 },
});

const { exportExcel } = useExport();
const toast = useToast();

const isDisabled = computed(() => props.disabled || props.rows.length === 0);

const onExcel = () => {
  if (props.rows.length > props.maxRows) {
    toast.info(
      `資料共 ${props.rows.length.toLocaleString()} 筆，超過單次匯出上限 ${props.maxRows.toLocaleString()} 筆，請用日期或狀態縮小範圍後再匯出。`
    );
    return;
  }
  exportExcel(props.fileName, props.rows, props.columns, props.sheetName);
};
</script>

<template>
  <button type="button" class="btn btn-sm btn-outline-success" :disabled="isDisabled" @click="onExcel">
    <i class="bi bi-file-earmark-excel me-1"></i>匯出 Excel
  </button>
</template>
