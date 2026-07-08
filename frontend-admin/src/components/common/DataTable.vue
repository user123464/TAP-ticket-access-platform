<script setup>
/**
 * DataTable.vue — 通用資料表格（深色版）
 *
 * 功能：欄位定義渲染、客製儲存格 slot、客戶端排序、分頁（vuejs-paginate-next）、
 *       loading / 空資料狀態。
 *
 * 用法：
 *   <DataTable :columns="columns" :rows="rows" :loading="loading" :page-size="10">
 *     <template #cell-status="{ row }"><StatusBadge ... /></template>
 *     <template #actions="{ row }"><button>...</button></template>
 *   </DataTable>
 *
 * columns: [{ key, label, sortable?, width?, class? }]
 * 搜尋/篩選屬於各列表頁的業務邏輯，由父層過濾 rows 後傳入。
 * P1 對接後端分頁時，可改傳 server-mode 相關 props 擴充（預留）。
 */
import { ref, computed, watch } from "vue";
import Paginate from "vuejs-paginate-next";

const props = defineProps({
  columns: {
    type: Array,
    required: true,
  },
  rows: {
    type: Array,
    default: () => [],
  },
  rowKey: {
    type: String,
    default: "id",
  },
  loading: {
    type: Boolean,
    default: false,
  },
  pageSize: {
    type: Number,
    default: 25,
  },
  // 每頁筆數可選項（分頁列內建切換器）
  pageSizeOptions: {
    type: Array,
    default: () => [10, 25, 50, 100],
  },
  emptyText: {
    type: String,
    default: "目前沒有資料",
  },
  // 有傳入 #actions slot 時顯示的操作欄標題
  actionsLabel: {
    type: String,
    default: "操作",
  },
  // 操作欄固定寬度（置中對齊），避免按鈕擠壓換行
  actionsWidth: {
    type: String,
    default: "120px",
  },
  // table-layout: fixed 讓欄寬依 column.width 固定，避免內容多寡造成抖動。
  // 個別頁面若需自適應寬度，可傳 :fixed-layout="false"。
  fixedLayout: {
    type: Boolean,
    default: true,
  },
});

const emit = defineEmits(["update:pageSize"]);

// 每頁筆數：以 prop 為初值，內部可被切換器調整並對外同步
const internalPageSize = ref(props.pageSize);
watch(
  () => props.pageSize,
  (val) => {
    internalPageSize.value = val;
  }
);
const onPageSizeChange = (e) => {
  const val = Number(e.target.value);
  internalPageSize.value = val;
  currentPage.value = 1;
  emit("update:pageSize", val);
};

// 分頁選項健壯化：若父層傳入的 pageSize 不在預設選項內（如 15），
// 動態併入，避免 <select> 找不到對應 option 而顯示空白。
const pageSizeChoices = computed(() => {
  const opts = [...props.pageSizeOptions];
  if (!opts.includes(internalPageSize.value)) opts.push(internalPageSize.value);
  return opts.sort((a, b) => a - b);
});

// --- 排序 ---
const sortKey = ref("");
const sortOrder = ref(1); // 1 升冪 / -1 降冪

const toggleSort = (col) => {
  if (!col.sortable) return;
  if (sortKey.value === col.key) {
    sortOrder.value = -sortOrder.value;
  } else {
    sortKey.value = col.key;
    sortOrder.value = 1;
  }
};

const sortedRows = computed(() => {
  if (!sortKey.value) return props.rows;
  return [...props.rows].sort((a, b) => {
    const va = a[sortKey.value];
    const vb = b[sortKey.value];
    if (va === vb) return 0;
    if (va == null) return 1;
    if (vb == null) return -1;
    const result = typeof va === "number" && typeof vb === "number"
      ? va - vb
      : String(va).localeCompare(String(vb), "zh-Hant");
    return result * sortOrder.value;
  });
});

// --- 分頁 ---
const currentPage = ref(1);

const pageCount = computed(() => Math.max(1, Math.ceil(sortedRows.value.length / internalPageSize.value)));

const pagedRows = computed(() => {
  const start = (currentPage.value - 1) * internalPageSize.value;
  return sortedRows.value.slice(start, start + internalPageSize.value);
});

// 資料變動（搜尋/篩選）導致頁數縮減時，自動拉回有效頁
watch(
  () => props.rows,
  () => {
    if (currentPage.value > pageCount.value) currentPage.value = 1;
  }
);

const handlePageClick = (page) => {
  currentPage.value = page;
};
</script>

<template>
  <div class="card border shadow-sm rounded-4 overflow-hidden position-relative">
    <!-- 頂部 Loading 進度條（Material-style 跑馬燈） -->
    <div v-if="loading" class="datatable-progress" role="progressbar" aria-label="載入中">
      <div class="datatable-progress-bar"></div>
    </div>

    <!-- 頂部分頁列：左側每頁筆數切換 + 總筆數，右側頁碼 -->
    <div
      v-if="!loading && sortedRows.length > 0"
      class="d-flex justify-content-between align-items-center gap-3 px-3 py-2 border-bottom flex-wrap"
      style="border-color: var(--tap-border) !important"
    >
      <div class="d-flex align-items-center gap-2 small text-tap-secondary">
        <label class="d-flex align-items-center gap-1 mb-0">
          每頁顯示
          <select
            class="form-select form-select-sm w-auto"
            :value="internalPageSize"
            @change="onPageSizeChange"
          >
            <option v-for="opt in pageSizeChoices" :key="opt" :value="opt">{{ opt }}</option>
          </select>
          筆
        </label>
        <span class="ms-2">共 {{ sortedRows.length }} 筆</span>
      </div>
      <Paginate
        v-if="pageCount > 1"
        v-model="currentPage"
        :page-count="pageCount"
        :click-handler="handlePageClick"
        :page-range="3"
        :margin-pages="1"
        prev-text="&lsaquo;"
        next-text="&rsaquo;"
        container-class="pagination pagination-sm mb-0"
        page-class="page-item"
        page-link-class="page-link"
        prev-class="page-item"
        prev-link-class="page-link"
        next-class="page-item"
        next-link-class="page-link"
        active-class="active"
        disabled-class="disabled"
      />
    </div>

    <!-- 表格 -->
    <div class="table-responsive">
      <table
        class="table table-striped table-hover align-middle mb-0"
        :class="{ 'table-fixed-layout': fixedLayout }"
      >
        <thead>
          <tr>
            <th
              v-for="col in columns"
              :key="col.key"
              :style="col.width ? { width: col.width } : {}"
              :class="[col.class, { 'cursor-pointer user-select-none': col.sortable }]"
              class="text-nowrap small text-tap-secondary fw-semibold py-3"
              @click="toggleSort(col)"
            >
              {{ col.label }}
              <i
                v-if="col.sortable"
                class="bi ms-1"
                :class="
                  sortKey === col.key
                    ? sortOrder === 1
                      ? 'bi-sort-up'
                      : 'bi-sort-down'
                    : 'bi-arrow-down-up opacity-50'
                "
              ></i>
            </th>
            <th
              v-if="$slots.actions"
              :style="{ width: actionsWidth }"
              class="text-nowrap small text-tap-secondary fw-semibold py-3 text-center"
            >
              {{ actionsLabel }}
            </th>
          </tr>
        </thead>

        <tbody>
          <!-- Loading -->
          <tr v-if="loading">
            <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="text-center py-5">
              <div class="spinner-border text-primary" role="status"></div>
              <div class="small text-tap-secondary mt-2">資料載入中...</div>
            </td>
          </tr>

          <!-- 空資料 -->
          <tr v-else-if="pagedRows.length === 0">
            <td :colspan="columns.length + ($slots.actions ? 1 : 0)" class="text-center py-5">
              <i class="bi bi-inbox fs-1 text-tap-secondary d-block mb-2"></i>
              <span class="text-tap-secondary">{{ emptyText }}</span>
            </td>
          </tr>

          <tr v-else v-for="row in pagedRows" :key="row[rowKey]">
            <td
              v-for="col in columns"
              :key="col.key"
              :class="col.class"
              :title="
                col.key === 'actions' || typeof row[col.key] === 'object'
                  ? undefined
                  : row[col.key] != null
                  ? String(row[col.key])
                  : undefined
              "
            >
              <!-- 自訂儲存格：#cell-欄位key -->
              <slot :name="`cell-${col.key}`" :row="row" :value="row[col.key]">
                {{ row[col.key] }}
              </slot>
            </td>
            <td v-if="$slots.actions" :style="{ width: actionsWidth }">
              <div class="d-flex justify-content-center align-items-center gap-1 flex-nowrap">
                <slot name="actions" :row="row"></slot>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 分頁列：左側每頁筆數切換 + 總筆數，右側頁碼 -->
    <div
      v-if="!loading && sortedRows.length > 0"
      class="d-flex justify-content-between align-items-center gap-3 px-3 py-2 border-top flex-wrap"
      style="border-color: var(--tap-border) !important"
    >
      <div class="d-flex align-items-center gap-2 small text-tap-secondary">
        <label class="d-flex align-items-center gap-1 mb-0">
          每頁顯示
          <select
            class="form-select form-select-sm w-auto"
            :value="internalPageSize"
            @change="onPageSizeChange"
          >
            <option v-for="opt in pageSizeChoices" :key="opt" :value="opt">{{ opt }}</option>
          </select>
          筆
        </label>
        <span class="ms-2">共 {{ sortedRows.length }} 筆</span>
      </div>
      <Paginate
        v-if="pageCount > 1"
        v-model="currentPage"
        :page-count="pageCount"
        :click-handler="handlePageClick"
        :page-range="3"
        :margin-pages="1"
        prev-text="&lsaquo;"
        next-text="&rsaquo;"
        container-class="pagination pagination-sm mb-0"
        page-class="page-item"
        page-link-class="page-link"
        prev-class="page-item"
        prev-link-class="page-link"
        next-class="page-item"
        next-link-class="page-link"
        active-class="active"
        disabled-class="disabled"
      />
    </div>
  </div>
</template>

<style scoped>
/* table-layout: fixed — 欄寬依 column.width 固定，避免內容多寡造成版面抖動 */
.table-fixed-layout {
  table-layout: fixed;
}
/* 固定佈局下，過長文字一律不換行並以省略號收斂（搭配 column 的 width / title）。
   需要多行的儲存格（如雙日期區間），由該頁自訂 slot 內以區塊元素堆疊呈現。 */
.table-fixed-layout td {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 窄螢幕（<lg）：固定佈局會把欄位壓縮重疊、資料被切且無法左右滑。
   改為自動佈局並讓表格依內容撐寬（width:max-content），交由外層 .table-responsive
   觸發橫向捲動；同時解除省略號截斷，讓完整資料可滑動檢視。 */
@media (max-width: 991.98px) {
  .table-fixed-layout {
    table-layout: auto;
    width: max-content;
    min-width: 100%;
  }
  .table-fixed-layout td {
    overflow: visible;
    text-overflow: clip;
    white-space: nowrap;
  }
}

/* 頂部 Material-style 跑馬燈進度條 */
.datatable-progress {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  overflow: hidden;
  background-color: color-mix(in srgb, var(--tap-primary) 22%, transparent);
  z-index: 5;
}
.datatable-progress-bar {
  position: absolute;
  top: 0;
  height: 100%;
  width: 40%;
  background-color: var(--tap-primary);
  border-radius: 2px;
  animation: datatable-indeterminate 1.1s ease-in-out infinite;
}
@keyframes datatable-indeterminate {
  0% {
    left: -40%;
  }
  100% {
    left: 100%;
  }
}
</style>
