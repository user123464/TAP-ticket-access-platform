<template>
  <div class="success-page bg-light min-vh-100 py-5 d-flex align-items-center">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-12 col-md-10 col-lg-7">
          <div
            class="card border-0 shadow-sm rounded-4 overflow-hidden success-card"
          >
            <!-- 頂部炫光漸層裝飾條 -->
            <div class="gradient-header"></div>

            <div class="p-4 p-sm-5 text-center">
              <!-- 精美 SVG 綠色打勾微動效 -->
              <div class="success-animation d-flex justify-content-center mb-4">
                <svg
                  class="checkmark"
                  xmlns="http://www.w3.org/2000/svg"
                  viewBox="0 0 52 52"
                >
                  <circle
                    class="checkmark__circle"
                    cx="26"
                    cy="26"
                    r="25"
                    fill="none"
                  />
                  <path
                    class="checkmark__check"
                    fill="none"
                    d="M14.1 27.2l7.1 7.2 16.7-16.8"
                  />
                </svg>
              </div>

              <h2 class="fw-bold text-dark mb-2">付款成功！</h2>
              <p class="text-secondary small mb-5">
                系統已確認收到您的款項，並成功為您建立該筆交易訂單。
              </p>

              <!-- 訂單摘要資訊卡 -->
              <div
                class="card border-0 bg-light rounded-4 p-4 text-start mb-4 shadow-xs"
              >
                <h5 class="fw-bold mb-3 border-bottom pb-2 text-dark">
                  訂單基本資訊
                </h5>

                <div class="row g-3 small">
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">訂單編號</span>
                    <strong class="text-dark font-monospace">{{
                      displayOrderId
                    }}</strong>
                  </div>
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">交易時間</span>
                    <strong class="text-dark">{{ orderInfo.time }}</strong>
                  </div>
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">付款方式</span>
                    <strong class="text-dark"
                      ><i
                        class="bi bi-credit-card-2-front me-1 text-primary"
                      ></i
                      >{{ orderInfo.paymentMethod }}</strong
                    >
                  </div>
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">聯絡人</span>
                    <strong class="text-dark"
                      >{{ orderInfo.name }} </strong
                    >
                  </div>
                </div>
              </div>

              <!-- 明細細項清單卡 -->
              <div
                class="card border-0 bg-light rounded-4 p-4 text-start mb-4 shadow-xs"
              >
                <div
                  class="d-flex justify-content-between align-items-center mb-3 border-bottom pb-2"
                >
                  <h5 class="fw-bold mb-0 text-dark">購買品項明細</h5>
                  <span class="badge bg-primary rounded-pill">{{
                    orderInfo.type === "ticket" ? "票券訂單" : "商品訂單"
                  }}</span>
                </div>

                <!-- 明細撈取降級提示：401/403 等錯誤時不顯示滿版錯誤，僅提示改至會員訂單查詢 -->
                <div
                  v-if="detailFetchDegraded"
                  class="alert alert-warning d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3 rounded-3 mb-3"
                  role="alert"
                >
                  <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-info-circle-fill"></i>
                    <span>付款已完成，訂單明細請至會員訂單查詢。</span>
                  </div>
                  <button
                    type="button"
                    class="btn btn-sm btn-warning fw-bold flex-shrink-0"
                    @click="viewOrders"
                  >
                    前往會員訂單
                  </button>
                </div>

                <!-- 商品/座位列表 -->
                <div v-else-if="orderInfo.type === 'ticket' && ticketDetails.length > 0" class="d-flex flex-column gap-3 mb-3">
                  <div
                    v-for="ticket in ticketDetails"
                    :key="ticket.tDetailId"
                    class="p-3 bg-white rounded-3 border border-light-subtle d-flex flex-column flex-sm-row justify-content-between align-items-center gap-3"
                  >
                    <div class="text-start flex-grow-1">
                      <div class="fw-bold text-dark mb-1">{{ ticket.activityTitle || '活動門票' }}</div>
                      <div class="small text-secondary mb-1">
                        票種：{{ ticket.ticketTypeName || '一般票' }}
                      </div>
                      <div class="small text-secondary mb-1" v-if="ticket.seatInfo">
                        座位：{{ ticket.seatInfo }}
                      </div>
                      <div class="small text-muted mb-1">
                        持票人姓名：{{ ticket.realName }}
                      </div>
                      <div class="small text-muted mb-1" v-if="ticket.tDetailId || ticket.tdetailId || ticket.t_detail_id">
                        明細編號：<span class="font-monospace">{{ ticket.tDetailId || ticket.tdetailId || ticket.t_detail_id }}</span>
                      </div>
                      <div class="small text-muted mb-1">
                        票價：<span class="font-monospace fw-bold text-danger">NT$ {{ Number(ticket.unitPrice || 0).toLocaleString() }}</span>
                      </div>
                      <div class="small text-muted">
                        核銷狀態：
                        <span :class="ticket.isUsed === 'Redeemed' ? 'badge bg-success' : ticket.isUsed === 'Canceled' ? 'badge bg-danger' : 'badge bg-warning text-dark'">
                          {{ ticket.isUsed === 'Redeemed' ? '已核銷' : ticket.isUsed === 'Canceled' ? '已取消' : '未核銷' }}
                        </span>
                      </div>
                    </div>
                    <!-- QR Code 驗票區 -->
                    <div class="text-center bg-light p-2 rounded-3 border border-light d-flex flex-column align-items-center justify-content-center" style="width: 120px; min-height: 120px;">
                      <img
                        v-if="ticket.qrCodeHash"
                        :src="`https://api.qrserver.com/v1/create-qr-code/?size=100x100&data=${ticket.qrCodeHash}`"
                        :alt="ticket.qrCodeHash"
                        title="請出示此二維碼核銷進場"
                        style="width: 100px; height: 100px; object-fit: contain;"
                      />
                      <div v-if="ticket.qrCodeHash" class="small text-muted font-monospace mt-1" style="font-size: 0.65rem; max-width: 100px; word-break: break-all; line-height: 1.2;">
                        {{ ticket.qrCodeHash }}
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 商品列表 -->
                <div v-else class="d-flex flex-column gap-2 mb-3">
                  <div
                    v-for="(item, index) in items"
                    :key="item.tDetailId || index"
                    class="d-flex justify-content-between align-items-center small p-2 bg-white rounded-3 border-light"
                  >
                    <div class="d-flex flex-column text-start">
                      <span class="fw-semibold text-dark">{{ item.name }}</span>
                      <span v-if="item.tDetailId" class="text-muted small" style="font-size: 0.75rem;">
                        明細編號: {{ item.tDetailId }}
                      </span>
                    </div>
                    <div class="d-flex align-items-center gap-3">
                      <span class="text-secondary small"
                        >x{{ item.qty || 1 }}</span
                      >
                      <span class="font-monospace fw-bold text-dark"
                        >NT$
                        {{
                          (item.price * (item.qty || 1)).toLocaleString()
                        }}</span
                      >
                    </div>
                  </div>
                </div>

                <div
                  class="d-flex justify-content-between align-items-center pt-2 border-top border-light"
                >
                  <span class="fw-bold text-dark">總計付款金額</span>
                  <h4 class="fw-bold text-danger mb-0 font-monospace">
                    NT$ {{ orderInfo.amount.toLocaleString() }}
                  </h4>
                </div>
              </div>

              <!-- 收件資訊卡 (僅限商品訂單) -->
              <div
                v-if="orderInfo.type === 'merch'"
                class="card border-0 bg-light rounded-4 p-4 text-start mb-4 shadow-xs"
              >
                <h5 class="fw-bold mb-3 border-bottom pb-2 text-dark">
                  收件資訊
                </h5>
                <div class="row g-3 small">
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">收件人姓名</span>
                    <strong class="text-dark">{{ orderInfo.name || '無' }}</strong>
                  </div>
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">聯絡電話</span>
                    <strong class="text-dark">{{ orderInfo.phone || '無' }}</strong>
                  </div>
                  <div class="col-12">
                    <span class="text-muted d-block mb-1">收件地址</span>
                    <strong class="text-dark">{{ orderInfo.address || '無' }}</strong>
                  </div>
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">電子信箱</span>
                    <strong class="text-dark">{{ orderInfo.email || '無' }}</strong>
                  </div>
                  <div class="col-sm-6">
                    <span class="text-muted d-block mb-1">身分證字號 / 護照號碼</span>
                    <strong class="text-dark">{{ orderInfo.identityNumber || '無' }}</strong>
                  </div>
                </div>
              </div>

              <!-- 按鈕引導區 -->
              <div
                class="d-flex flex-column flex-sm-row gap-3 justify-content-center"
              >
                <!-- 不管是門票或商品訂單，皆提供查看訂單按鈕導向會員訂單頁 -->
                <button
                  @click="viewOrders"
                  class="btn btn-primary py-3 px-4 rounded-3 fw-bold btn-submit-hover flex-fill d-flex align-items-center justify-content-center gap-2"
                >
                  <i class="bi bi-receipt"></i>
                  <span>查看我的訂單</span>
                </button>
                <button
                  @click="goHome"
                  class="btn btn-outline-secondary py-3 px-4 rounded-3 fw-bold flex-fill d-flex align-items-center justify-content-center gap-2 btn-home-hover"
                >
                  <i class="bi bi-house-door-fill"></i>
                  <span>回到首頁</span>
                </button>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import axios from "@/plugins/axios";

const router = useRouter();
const route = useRoute();

// ==========================================
// 1. 動態接收交易訂單資料 (從 sessionStorage 讀取以防止重新整理遺失，並相容路由 query/state)
// ==========================================
let storedOrder = null;
try {
  const sessionData = sessionStorage.getItem("payment_success_order");
  if (sessionData) {
    storedOrder = JSON.parse(sessionData);
  }
} catch (e) {
  console.error("解析 sessionStorage 訂單資料失敗:", e);
}

const orderInfo = reactive({
  tOrderId: route.query.tOrderId || route.query.t_order_id || history.state?.tOrderId || history.state?.t_order_id || storedOrder?.tOrderId || storedOrder?.t_order_id || '',
  mOrderId: route.query.mOrderId || route.query.m_order_id || history.state?.mOrderId || history.state?.m_order_id || storedOrder?.mOrderId || storedOrder?.m_order_id || '',
  orderId: route.query.orderId || route.query.order_id || history.state?.orderId || history.state?.order_id || storedOrder?.orderId || storedOrder?.order_id || '',
  amount: Number(route.query.amount || history.state?.amount || storedOrder?.amount || 0),
  email: route.query.email || history.state?.email || storedOrder?.email || '',
  name: route.query.name || history.state?.name || storedOrder?.name || '',
  phone: route.query.phone || history.state?.phone || storedOrder?.phone || '',
  address: route.query.address || history.state?.address || storedOrder?.address || '',
  identityNumber: route.query.identityNumber || history.state?.identityNumber || storedOrder?.identityNumber || '',
  paymentMethod: route.query.paymentMethod || history.state?.paymentMethod || storedOrder?.paymentMethod || '信用卡 (SSL 安全加密交易)',
  time: route.query.time || history.state?.time || storedOrder?.time || new Date().toLocaleString(),
  type: route.query.type || history.state?.type || storedOrder?.type || 'ticket'
});

const displayOrderId = computed(() => {
  if (orderInfo.type === 'ticket') {
    return orderInfo.tOrderId || orderInfo.orderId;
  } else {
    return orderInfo.mOrderId || orderInfo.orderId;
  }
});

// 商品/座位明細清單
const items = ref([]);
const ticketDetails = ref([]);

// 付款回跳撈明細時遇到 401/403（例如登入狀態尚未就緒）的降級旗標：
// 畫面仍維持「付款成功」狀態，僅將明細區塊替換為提示 + 前往會員訂單按鈕
const detailFetchDegraded = ref(false);

const rawItems = history.state?.items || storedOrder?.items || [];
const rawSeats = history.state?.seats || storedOrder?.seats || [];

if (orderInfo.type === 'merch') {
  items.value = rawItems;
} else {
  items.value = (rawSeats.length > 0 ? rawSeats : rawItems).map(seat => ({
    name: seat.name,
    price: seat.price,
    qty: seat.qty || 1
  }));
}

onMounted(async () => {
  const targetId = orderInfo.type === 'merch'
    ? (orderInfo.mOrderId || orderInfo.orderId)
    : (orderInfo.tOrderId || orderInfo.orderId);

  if (orderInfo.type === 'ticket' && targetId) {
    try {
      const res = await axios.get(`/api/tickets/${targetId}`, { _skip401Redirect: true, _skip403Redirect: true });
      if (res.data) {
        const orderData = res.data;
        orderInfo.amount = orderData.totalAmount || orderInfo.amount;
        orderInfo.name = orderData.contactName || orderInfo.name;
        if (orderData.createAt) {
          orderInfo.time = new Date(orderData.createAt).toLocaleString();
        }
        ticketDetails.value = orderData.orderDetailTickets || [];
      }
    } catch (err) {
      console.error("無法取得門票訂單明細:", err);
      if (err?.response?.status === 401 || err?.response?.status === 403) {
        detailFetchDegraded.value = true;
      }
    }
  } else if (orderInfo.type === 'merch' && targetId) {
    try {
      const res = await axios.get(`/api/merch/${targetId}`, { _skip401Redirect: true, _skip403Redirect: true });
      if (res.data) {
        const orderData = res.data;
        orderInfo.amount = orderData.totalAmount || orderInfo.amount;
        orderInfo.name = orderData.recipientName || orderInfo.name;
        orderInfo.phone = orderData.recipientPhone || orderInfo.phone;
        orderInfo.email = orderData.recipientEmail || orderInfo.email;
        orderInfo.address = orderData.recipientAddress || orderInfo.address;
        orderInfo.identityNumber = orderData.identityNumber || orderInfo.identityNumber;
        
        if (orderData.createdAt) {
          orderInfo.time = new Date(orderData.createdAt).toLocaleString();
        }
        if (orderData.orderDetailMerches && orderData.orderDetailMerches.length > 0) {
          items.value = orderData.orderDetailMerches.map(detail => {
            const matchedVariant = detail.productProduct?.variants?.find(v => v.variantId === detail.variantId);
            let spec = '';
            if (matchedVariant) {
              const parts = [];
              if (matchedVariant.productColor) parts.push(matchedVariant.productColor);
              if (matchedVariant.productSize) parts.push(matchedVariant.productSize);
              if (parts.length > 0) spec = ` (${parts.join(' - ')})`;
            }
            return {
              name: (detail.productProduct?.productName || '商品') + spec,
              price: detail.unitPrice,
              qty: detail.quantity,
              tDetailId: detail.mDetailId
            };
          });
        }
      }
    } catch (err) {
      console.error("無法取得商品訂單明細:", err);
      if (err?.response?.status === 401 || err?.response?.status === 403) {
        detailFetchDegraded.value = true;
      }
    }
  }
});

// ==========================================
// 2. 引導按鈕導頁
// ==========================================
const viewOrders = () => {
  // 統一導向會員訂單頁，依訂單類型帶 query 切換分頁
  const tab = orderInfo.type === "ticket" ? "ticket" : "merch";
  router.push({ path: "/member/orders", query: { tab } });
};

const goHome = () => {
  router.push("/");
};
</script>

<style scoped>
.success-page {
  font-family:
    "Inter",
    -apple-system,
    sans-serif;
  color: #334155;
}

.success-card {
  position: relative;
  border-radius: 20px;
}

.gradient-header {
  height: 6px;
  background: linear-gradient(90deg, #22c55e 0%, #10b981 50%, #3b82f6 100%);
  width: 100%;
}

.shadow-xs {
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

/* ==========================================
 * SVG 綠色打勾動畫樣式
 * ========================================== */
.success-animation {
  height: 80px;
}

.checkmark {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  display: block;
  stroke-width: 3;
  stroke: #fff;
  stroke-miterlimit: 10;
  box-shadow: inset 0px 0px 0px #22c55e;
  animation:
    fill 0.4s ease-in-out 0.4s forwards,
    scale 0.3s ease-in-out 0.9s forwards;
}

.checkmark__circle {
  stroke-dasharray: 166;
  stroke-dashoffset: 166;
  stroke-width: 2;
  stroke-miterlimit: 10;
  stroke: #22c55e;
  fill: none;
  animation: stroke 0.6s cubic-bezier(0.65, 0, 0.45, 1) forwards;
}

.checkmark__check {
  transform-origin: 50% 50%;
  stroke-dasharray: 48;
  stroke-dashoffset: 48;
  animation: stroke 0.3s cubic-bezier(0.65, 0, 0.45, 1) 0.8s forwards;
}

@keyframes stroke {
  100% {
    stroke-dashoffset: 0;
  }
}

@keyframes fill {
  100% {
    box-shadow: inset 0px 0px 0px 40px #22c55e;
  }
}

@keyframes scale {
  0%,
  100% {
    transform: none;
  }
  50% {
    transform: scale3d(1.1, 1.1, 1);
  }
}

/* 按鈕懸停效果 */
.btn-submit-hover {
  transition: all 0.3s ease;
}
.btn-submit-hover:hover {
  transform: translateY(-2px);
  box-shadow: 0 6px 15px rgba(59, 130, 246, 0.3) !important;
}

.btn-home-hover {
  transition: all 0.3s ease;
}
.btn-home-hover:hover {
  background-color: #f1f5f9;
  transform: translateY(-2px);
}
</style>
