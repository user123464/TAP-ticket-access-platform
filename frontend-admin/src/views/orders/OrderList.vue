<script setup>
/**
 * OrderList.vue — 全平台訂單查詢列表（模組 7 ★ P2，唯讀）
 *
 * 供客服查詢偵錯，Admin 不修改訂單。搜尋（訂單編號/買家 Email）+ 篩選（狀態）。
 * API：GET /api/admin/orders
 */
import { ref, computed, watch, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import OperationsTabs from "@/components/operations/OperationsTabs.vue";
import { ORDER_STATUS_META } from "@/constants/order.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();

// SWR 快取：建立時立刻帶回上次列表（不閃白），背景再 refresh 取最新
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:orders",
  () => api.get("/api/admin/orders").then((r) => r.data.data ?? []),
  { initial: [] }
);
const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

const keyword = ref(""); // 訂單編號 / 買家 Email / 活動名稱
const filterStatus = ref("");
const fromDate = ref(""); // 下單時間起
const toDate = ref("");   // 下單時間迄

// 金額 / 張數雙向拉桿（上限預設為資料最大值，0 視為尚未初始化＝不設上限）
const amountMin = ref(0);
const amountMax = ref(0);
const qtyMin = ref(0);
const qtyMax = ref(0);

// 依資料動態算出拉桿上限（向上對齊好看的刻度）
const maxAmount = computed(() => {
  if (rows.value.length === 0) return 10000;
  const v = Math.max(1000, ...rows.value.map((o) => o.amount ?? 0));
  return Math.ceil(v / 1000) * 1000;
});
const maxQty = computed(() => {
  if (rows.value.length === 0) return 10;
  return Math.max(1, ...rows.value.map((o) => o.qty ?? 0));
});

const amountStep = computed(() => {
  const m = maxAmount.value;
  if (m <= 5000) return 100;
  if (m <= 20000) return 250;
  if (m <= 50000) return 500;
  return 1000;
});

const amountProgress = computed(() => {
  const lo = (amountMin.value / maxAmount.value) * 100;
  const hi = (amountMax.value / maxAmount.value) * 100;
  return { left: `${lo}%`, width: `${Math.max(0, hi - lo)}%` };
});
const qtyProgress = computed(() => {
  const lo = (qtyMin.value / maxQty.value) * 100;
  const hi = (qtyMax.value / maxQty.value) * 100;
  return { left: `${lo}%`, width: `${Math.max(0, hi - lo)}%` };
});

// 防止兩端拉桿交叉
const onAmountMin = () => { if (amountMin.value > amountMax.value) amountMin.value = amountMax.value; };
const onAmountMax = () => { if (amountMax.value < amountMin.value) amountMax.value = amountMin.value; };
const onQtyMin = () => { if (qtyMin.value > qtyMax.value) qtyMin.value = qtyMax.value; };
const onQtyMax = () => { if (qtyMax.value < qtyMin.value) qtyMax.value = qtyMin.value; };

// 資料載入後，將上限初始化為資料最大值（amountMax 仍為 0 代表尚未初始化）
watch(maxAmount, (m) => { if (amountMax.value === 0) amountMax.value = m; }, { immediate: true });
watch(maxQty, (m) => { if (qtyMax.value === 0) qtyMax.value = m; }, { immediate: true });

const fetchOrders = async () => {
  try {
    await refresh();
  } catch (error) {
    // 後端回錯(4xx/5xx)：仍走原本錯誤提示，不吞錯；保留快取舊資料避免閃白
    if (error.response) {
      setAnnouncement("載入訂單清單失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  }
};

onMounted(fetchOrders);

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value.filter((o) => {
    if (kw &&
        !String(o.id ?? "").toLowerCase().includes(kw) &&
        !String(o.buyerEmail ?? "").toLowerCase().includes(kw) &&
        !String(o.eventTitle ?? "").toLowerCase().includes(kw)) return false;
    if (filterStatus.value !== "" && o.status !== Number(filterStatus.value)) return false;
    if (fromDate.value && dayjs(o.createdAt).isBefore(dayjs(fromDate.value).startOf("day"))) return false;
    if (toDate.value && dayjs(o.createdAt).isAfter(dayjs(toDate.value).endOf("day"))) return false;
    if ((o.amount ?? 0) < amountMin.value || (amountMax.value > 0 && (o.amount ?? 0) > amountMax.value)) return false;
    if ((o.qty ?? 0) < qtyMin.value || (qtyMax.value > 0 && (o.qty ?? 0) > qtyMax.value)) return false;
    return true;
  });
});

const columns = [
  { key: "id", label: "訂單編號", sortable: true, width: "120px" },
  { key: "buyerName", label: "買家", sortable: true },
  { key: "eventTitle", label: "活動", sortable: true },
  { key: "qty", label: "張數", sortable: true, width: "60px" },
  { key: "amount", label: "金額", sortable: true, width: "100px" },
  { key: "createdAt", label: "下單時間", sortable: true, width: "150px" },
  { key: "status", label: "狀態", sortable: true, width: "100px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
const formatCurrency = (n) => `NT$ ${Number(n ?? 0).toLocaleString("zh-Hant-TW")}`;

// 匯出：對應目前篩選後的清單，狀態/時間轉成可讀文字
const EXPORT_COLUMNS = {
  id: "訂單編號",
  buyerName: "買家姓名",
  buyerEmail: "買家 Email",
  eventTitle: "活動",
  qty: "張數",
  amount: "金額",
  createdAt: "下單時間",
  statusText: "狀態",
};
const exportRows = computed(() =>
  filteredRows.value.map((o) => ({
    id: o.id,
    buyerName: o.buyerName,
    buyerEmail: o.buyerEmail,
    eventTitle: o.eventTitle,
    qty: o.qty,
    amount: o.amount,
    createdAt: formatTime(o.createdAt),
    statusText: ORDER_STATUS_META[o.status]?.label ?? "未知",
  }))
);
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-headset me-2" style="color: var(--tap-primary)"></i>客服與查詢</h4>
      <span class="badge rounded-pill" style="background-color: var(--tap-bg-hover)"><i class="bi bi-eye me-1"></i>唯讀</span>
    </div>

    <div class="d-flex justify-content-between align-items-start gap-2 flex-wrap">
      <OperationsTabs />
      <ExportButtons file-name="訂單清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="訂單清單" :disabled="loading" />
    </div>

    <!-- 搜尋 + 篩選：分兩行。第一行＝搜尋框＋下單時間＋狀態；第二行＝金額／張數雙向拉桿 -->
    <div class="card border shadow-sm mb-3">
      <div class="card-body py-3 px-3 d-flex flex-column gap-2">
        <!-- 第一行 -->
        <div class="row gx-3 gy-2 align-items-center">
          <div class="col-12 col-md flex-grow-1 min-w-0" style="min-width: 220px">
            <div class="input-group input-group-sm">
              <span class="input-group-text bg-transparent text-tap-secondary"><i class="bi bi-search"></i></span>
              <input v-model="keyword" type="text" class="form-control" placeholder="搜尋訂單編號 / 買家 Email / 活動名稱" />
            </div>
          </div>
          <div class="col-12 col-md-auto">
            <div class="input-group input-group-sm">
              <span class="input-group-text bg-transparent text-tap-secondary"><i class="bi bi-calendar3 me-1"></i>下單時間</span>
              <input v-model="fromDate" type="date" class="form-control text-tap-secondary" title="開始日期" />
              <span class="input-group-text bg-transparent text-tap-secondary px-2">至</span>
              <input v-model="toDate" type="date" class="form-control text-tap-secondary" title="結束日期" />
            </div>
          </div>
        </div>

        <!-- 第二行：狀態 + 金額 / 張數 雙向拉桿 -->
        <div class="row gx-3 gy-2 align-items-center">
          <!-- 狀態 -->
          <div class="col-6 col-md-auto">
            <select v-model="filterStatus" class="form-select form-select-sm">
              <option value="">全部狀態</option>
              <option value="0">待付款</option>
              <option value="1">已付款</option>
              <option value="2">已完成</option>
              <option value="3">已取消</option>
              <option value="4">已退款</option>
            </select>
          </div>

          <!-- 金額 -->
          <div class="col-12 col-md-auto">
            <div class="form-control form-control-sm d-flex align-items-center gap-2 px-2" style="width: auto; height: 31px; background-color: var(--bs-body-bg); border-color: var(--bs-border-color);">
              <span class="text-nowrap"><i class="bi bi-cash-coin me-1"></i>金額：</span>
              <div class="dual-range-container position-relative mx-2" style="width: 140px; height: 20px;">
                <div class="slider-track position-absolute top-50 start-0 end-0 rounded bg-secondary-subtle" style="height: 4px; transform: translateY(-50%);"></div>
                <div class="slider-progress position-absolute top-50 rounded" :style="amountProgress" style="height: 4px; transform: translateY(-50%); background-color: var(--tap-primary);"></div>
                <input type="range" v-model.number="amountMin" :min="0" :max="maxAmount" :step="amountStep" @input="onAmountMin" title="最低金額"
                  style="pointer-events: none; -webkit-appearance: none; appearance: none; background: none; width: 100%; height: 100%; position: absolute; top: 0; left: 0; margin: 0;" />
                <input type="range" v-model.number="amountMax" :min="0" :max="maxAmount" :step="amountStep" @input="onAmountMax" title="最高金額"
                  style="pointer-events: none; -webkit-appearance: none; appearance: none; background: none; width: 100%; height: 100%; position: absolute; top: 0; left: 0; margin: 0;" />
              </div>
              <span class="text-nowrap text-primary fw-bold" style="min-width: 150px; display: inline-block; text-align: right;">
                NT$ {{ amountMin.toLocaleString() }} ~ {{ amountMax.toLocaleString() }}
              </span>
            </div>
          </div>

          <!-- 張數 -->
          <div class="col-12 col-md-auto">
            <div class="form-control form-control-sm d-flex align-items-center gap-2 px-2" style="width: auto; height: 31px; background-color: var(--bs-body-bg); border-color: var(--bs-border-color);">
              <span class="text-nowrap"><i class="bi bi-ticket-perforated me-1"></i>張數：</span>
              <div class="dual-range-container position-relative mx-2" style="width: 120px; height: 20px;">
                <div class="slider-track position-absolute top-50 start-0 end-0 rounded bg-secondary-subtle" style="height: 4px; transform: translateY(-50%);"></div>
                <div class="slider-progress position-absolute top-50 rounded" :style="qtyProgress" style="height: 4px; transform: translateY(-50%); background-color: var(--tap-primary);"></div>
                <input type="range" v-model.number="qtyMin" :min="0" :max="maxQty" :step="1" @input="onQtyMin" title="最少張數"
                  style="pointer-events: none; -webkit-appearance: none; appearance: none; background: none; width: 100%; height: 100%; position: absolute; top: 0; left: 0; margin: 0;" />
                <input type="range" v-model.number="qtyMax" :min="0" :max="maxQty" :step="1" @input="onQtyMax" title="最多張數"
                  style="pointer-events: none; -webkit-appearance: none; appearance: none; background: none; width: 100%; height: 100%; position: absolute; top: 0; left: 0; margin: 0;" />
              </div>
              <span class="text-nowrap text-primary fw-bold" style="min-width: 70px; display: inline-block; text-align: right;">
                {{ qtyMin }} ~ {{ qtyMax }} 張
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="沒有符合條件的訂單" actions-width="70px">
      <template #cell-id="{ value }"><code class="small">{{ value }}</code></template>
      <template #cell-buyerName="{ row }">
        <div class="fw-semibold small">{{ row.buyerName }}</div>
        <div class="small text-tap-secondary">{{ row.buyerEmail }}</div>
      </template>
      <template #cell-amount="{ value }">{{ formatCurrency(value) }}</template>
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-status="{ value }">
        <StatusBadge :variant="ORDER_STATUS_META[value]?.variant" :label="ORDER_STATUS_META[value]?.label ?? '未知'" />
      </template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/operations/orders/${row.id}`" class="btn btn-sm btn-icon btn-outline-primary" title="查看">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
/* 僅縮小本頁表格欄位的左右間距（預設主題為 1rem，全站共用；此處只調訂單頁） */
:deep(.table > thead > tr > th),
:deep(.table > tbody > tr > td) {
  padding-left: 0.5rem;
  padding-right: 0.5rem;
}

/* 雙向拉桿樣式（與 /admin/venues 場地列表一致） */
.dual-range-container input[type="range"]::-webkit-slider-runnable-track {
  background: transparent;
  border: none;
}
.dual-range-container input[type="range"]::-moz-range-track {
  background: transparent;
  border: none;
}
/* Chrome/Safari/Opera/Edge */
.dual-range-container input[type="range"]::-webkit-slider-thumb {
  pointer-events: auto;
  -webkit-appearance: none;
  appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--tap-primary);
  border: 1.5px solid #fff;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  transition: transform 0.1s;
}
.dual-range-container input[type="range"]::-webkit-slider-thumb:active {
  transform: scale(1.3);
}
/* Firefox */
.dual-range-container input[type="range"]::-moz-range-thumb {
  pointer-events: auto;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--tap-primary);
  border: 1.5px solid #fff;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  transition: transform 0.1s;
}
.dual-range-container input[type="range"]::-moz-range-thumb:active {
  transform: scale(1.3);
}
</style>
