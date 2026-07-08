<script setup>
/**
 * EventDetail.vue — 活動詳情（模組 7 ★ P2，唯讀）
 *
 * 純偵錯檢視：活動基本資料 + 場次 + 票種售況。Admin 不修改活動。
 * API：GET /api/admin/events/{id}
 */
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import StatusBadge from "@/components/common/StatusBadge.vue";
import DataTable from "@/components/common/DataTable.vue";
import { EVENT_STATUS_META } from "@/constants/event.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useGoBack } from "@/composables/useGoBack.js";

const { setAnnouncement } = useSystemBanner();
const { goBack } = useGoBack();

const route = useRoute();
const eventId = computed(() => route.params.eventId);

const loading = ref(true);
const detail = ref(null);

const fetchDetail = async () => {
  loading.value = true;
  try {
    const { data } = await api.get(`/api/admin/events/${eventId.value}`);
    detail.value = data.data ?? null;
  } catch (error) {
    detail.value = null;
    if (error.response) {
      setAnnouncement("載入活動詳情失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    loading.value = false;
  }
};

onMounted(fetchDetail);

const statusMeta = computed(() => EVENT_STATUS_META[detail.value?.status] ?? { label: "未知", variant: "secondary" });
const totalSold = computed(() => (detail.value?.ticketTypes ?? []).reduce((s, t) => s + t.sold, 0));
const totalRevenue = computed(() => (detail.value?.ticketTypes ?? []).reduce((s, t) => s + t.sold * t.price, 0));

const ticketColumns = [
  { key: "name", label: "票種" },
  { key: "price", label: "票價", width: "120px" },
  { key: "progress", label: "售出 / 總量", width: "180px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");
const formatCurrency = (n) => `NT$ ${Number(n ?? 0).toLocaleString("zh-Hant-TW")}`;
</script>

<template>
  <div>
    <div class="detail-header-row">
      <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="goBack('/admin/operations/events')">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h4 class="fw-bold">活動詳情</h4>
      <StatusBadge v-if="detail" :variant="statusMeta.variant" :label="statusMeta.label" />
      <span class="badge rounded-pill ms-auto" style="background-color: var(--tap-bg-hover)"><i class="bi bi-eye me-1"></i>唯讀</span>
    </div>

    <div v-if="loading" class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>

    <template v-else-if="detail">
      <!-- 基本資料 -->
      <div class="card border shadow-sm rounded-4 mb-3">
        <div class="card-body">
          <h5 class="fw-bold mb-3">{{ detail.title }}</h5>
          <div class="row g-3 small">
            <div class="col-6 col-md-3">
              <div class="text-tap-secondary mb-1">活動編號</div>
              <div class="fw-semibold">{{ detail.id }}</div>
            </div>
            <div class="col-6 col-md-3">
              <div class="text-tap-secondary mb-1">主辦組織</div>
              <RouterLink :to="`/admin/organizers/${detail.orgId}`" class="fw-semibold text-decoration-none">{{ detail.orgName }}</RouterLink>
            </div>
            <div class="col-6 col-md-3">
              <div class="text-tap-secondary mb-1">場地</div>
              <div class="fw-semibold">{{ detail.venue || "—" }}</div>
            </div>
            <div class="col-6 col-md-3">
              <div class="text-tap-secondary mb-1">活動時間</div>
              <div class="fw-semibold">{{ formatTime(detail.startAt) }}</div>
            </div>
            <div class="col-12">
              <div class="text-tap-secondary mb-1">活動說明</div>
              <div>{{ detail.description || "—" }}</div>
            </div>
          </div>
        </div>
      </div>

      <!-- 售況統計 -->
      <div class="row g-3 mb-3">
        <div class="col-6">
          <div class="card border shadow-sm rounded-4">
            <div class="card-body py-3 d-flex align-items-center justify-content-between">
              <span class="small text-tap-secondary">總售票數</span>
              <span class="fw-bold fs-5">{{ totalSold.toLocaleString() }}</span>
            </div>
          </div>
        </div>
        <div class="col-6">
          <div class="card border shadow-sm rounded-4">
            <div class="card-body py-3 d-flex align-items-center justify-content-between">
              <span class="small text-tap-secondary">票房收入（估）</span>
              <span class="fw-bold fs-5 text-success">{{ formatCurrency(totalRevenue) }}</span>
            </div>
          </div>
        </div>
      </div>

      <!-- 票種售況 -->
      <div class="fw-bold mb-2"><i class="bi bi-ticket-perforated me-2" style="color: var(--tap-primary)"></i>票種售況</div>
      <DataTable :columns="ticketColumns" :rows="detail.ticketTypes ?? []" row-key="name" :page-size="10" emptyText="尚無票種">
        <template #cell-name="{ value }"><span class="fw-semibold">{{ value }}</span></template>
        <template #cell-price="{ value }">{{ formatCurrency(value) }}</template>
        <template #cell-progress="{ row }">
          <div class="small mb-1">{{ row.sold.toLocaleString() }} / {{ row.total.toLocaleString() }}</div>
          <div class="progress" style="height: 4px">
            <div class="progress-bar bg-primary" :style="{ width: (row.total ? Math.round((row.sold / row.total) * 100) : 0) + '%' }"></div>
          </div>
        </template>
      </DataTable>
    </template>
  </div>
</template>
