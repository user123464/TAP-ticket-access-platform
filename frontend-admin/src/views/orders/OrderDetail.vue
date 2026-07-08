<script setup>
/**
 * OrderDetail.vue — 訂單詳情（模組 7 ★ P2，唯讀）
 *
 * 供客服查詢：買家資訊 + 活動 + 票券明細 + 金流時間軸。Admin 不修改訂單。
 * API：GET /api/admin/orders/{id}
 */
import { ref, computed, onMounted } from "vue";
import { useRoute } from "vue-router";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import StatusBadge from "@/components/common/StatusBadge.vue";
import DataTable from "@/components/common/DataTable.vue";
import { ORDER_STATUS_META } from "@/constants/order.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useGoBack } from "@/composables/useGoBack.js";

const { setAnnouncement } = useSystemBanner();
const { goBack } = useGoBack();

const route = useRoute();
const orderId = computed(() => route.params.orderId);

const loading = ref(true);
const detail = ref(null);

const fetchDetail = async () => {
  loading.value = true;
  try {
    const { data } = await api.get(`/api/admin/orders/${orderId.value}`);
    detail.value = data.data ?? null;
  } catch (error) {
    detail.value = null;
    if (error.response) {
      setAnnouncement("載入訂單詳情失敗，請稍後再試。", "danger");
    }
    // 斷線情境由 axios 攔截器顯示橫幅
  } finally {
    loading.value = false;
  }
};

onMounted(fetchDetail);

const statusMeta = computed(() => ORDER_STATUS_META[detail.value?.status] ?? { label: "未知", variant: "secondary" });

const ticketColumns = [
  { key: "ticketNo", label: "票券編號", width: "150px" },
  { key: "typeName", label: "票種", width: "140px" },
  { key: "seat", label: "座位" },
  { key: "price", label: "票價", width: "110px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm:ss") : "—");
const formatCurrency = (n) => `NT$ ${Number(n ?? 0).toLocaleString("zh-Hant-TW")}`;
</script>

<template>
  <div>
    <div class="detail-header-row">
      <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="goBack('/admin/operations/orders')">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h4 class="fw-bold">訂單詳情</h4>
      <StatusBadge v-if="detail" :variant="statusMeta.variant" :label="statusMeta.label" />
      <span class="badge rounded-pill ms-auto" style="background-color: var(--tap-bg-hover)"><i class="bi bi-eye me-1"></i>唯讀</span>
    </div>

    <div v-if="loading" class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>

    <template v-else-if="detail">
      <div class="row g-3">
        <!-- 左：訂單與票券 -->
        <div class="col-12 col-xl-8">
          <div class="card border shadow-sm rounded-4 mb-3">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-receipt me-2" style="color: var(--tap-primary)"></i>訂單資訊</div>
              <div class="row g-3 small">
                <div class="col-12 col-md-6">
                  <div class="text-tap-secondary mb-1">訂單編號</div>
                  <code>{{ detail.id }}</code>
                </div>
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">訂單金額</div>
                  <div class="fw-semibold text-success">{{ formatCurrency(detail.amount) }}</div>
                </div>
                <div class="col-6 col-md-3">
                  <div class="text-tap-secondary mb-1">付款方式</div>
                  <div class="fw-semibold">{{ detail.paymentMethod || "—" }}</div>
                </div>
                <div class="col-12">
                  <div class="text-tap-secondary mb-1">活動</div>
                  <RouterLink :to="`/admin/operations/events/${detail.eventId}`" class="fw-semibold text-decoration-none">{{ detail.eventTitle }}</RouterLink>
                  <span class="text-tap-secondary"> · {{ detail.orgName }}</span>
                </div>
              </div>
            </div>
          </div>

          <div class="fw-bold mb-2"><i class="bi bi-ticket-detailed me-2" style="color: var(--tap-primary)"></i>票券明細</div>
          <DataTable :columns="ticketColumns" :rows="detail.tickets ?? []" row-key="ticketNo" :page-size="10" emptyText="無票券">
            <template #cell-ticketNo="{ value }"><code class="small">{{ value }}</code></template>
            <template #cell-price="{ value }">{{ formatCurrency(value) }}</template>
          </DataTable>
        </div>

        <!-- 右：買家 + 金流時間軸 -->
        <div class="col-12 col-xl-4">
          <div class="card border shadow-sm rounded-4 mb-3">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-person me-2" style="color: var(--tap-primary)"></i>買家資訊</div>
              <div class="small d-flex flex-column gap-2">
                <div><span class="text-tap-secondary">姓名：</span><span class="fw-semibold">{{ detail.buyerName }}</span></div>
                <div><span class="text-tap-secondary">Email：</span><span class="fw-semibold">{{ detail.buyerEmail }}</span></div>
                <div><span class="text-tap-secondary">電話：</span><span class="fw-semibold">{{ detail.buyerPhone || "—" }}</span></div>
              </div>
            </div>
          </div>

          <div class="card border shadow-sm rounded-4">
            <div class="card-body">
              <div class="fw-bold mb-3"><i class="bi bi-clock-history me-2" style="color: var(--tap-primary)"></i>金流時間軸</div>
              <ul class="list-unstyled mb-0 small d-flex flex-column gap-3">
                <li v-for="(item, idx) in detail.timeline" :key="idx" class="d-flex gap-2">
                  <i class="bi bi-circle-fill mt-1" style="font-size: 0.5em; color: var(--tap-primary)"></i>
                  <div>
                    <div class="fw-semibold">{{ item.event }}</div>
                    <div class="text-tap-secondary">{{ formatTime(item.time) }}</div>
                  </div>
                </li>
              </ul>
            </div>
          </div>
        </div>
      </div>
    </template>
  </div>
</template>
