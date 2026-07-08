<template>
  <div class="member-orders-page bg-light min-vh-100 py-5">
    <div class="container">
      <div class="row justify-content-center">
        <div class="col-12 col-lg-9">
          <h2 class="fw-bold text-dark mb-4">
            <i class="bi bi-receipt text-primary me-2"></i>我的訂單
          </h2>

          <!-- Tab 切換 -->
          <ul class="nav nav-pills mb-4 gap-2">
            <li class="nav-item">
              <button
                type="button"
                class="nav-link"
                :class="{ active: activeTab === 'ticket' }"
                @click="activeTab = 'ticket'"
              >
                <i class="bi bi-ticket-perforated me-1"></i>票務訂單
              </button>
            </li>
            <li class="nav-item">
              <button
                type="button"
                class="nav-link"
                :class="{ active: activeTab === 'merch' }"
                @click="activeTab = 'merch'"
              >
                <i class="bi bi-bag me-1"></i>商城訂單
              </button>
            </li>
          </ul>

          <!-- 載入中 -->
          <div v-if="loading" class="text-center py-5 text-secondary">
            <div class="spinner-border text-primary mb-3" role="status"></div>
            <div>訂單載入中...</div>
          </div>

          <!-- 票務訂單 -->
          <div v-else-if="activeTab === 'ticket'">
            <div v-if="ticketOrders.length === 0" class="empty-state">
              <i class="bi bi-ticket-perforated display-4 text-secondary mb-3 d-block"></i>
              <p class="text-secondary mb-3">尚無票務訂單，去逛逛精彩活動吧</p>
              <router-link to="/themes" class="btn btn-primary rounded-3 px-4">
                <i class="bi bi-search me-1"></i>去逛逛
              </router-link>
            </div>
            <div v-else class="d-flex flex-column gap-3">
              <div
                v-for="order in ticketOrders"
                :key="order.tOrderId"
                class="card border-0 shadow-sm rounded-4 p-3 p-sm-4 order-card"
              >
                <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2">
                  <div>
                    <div class="fw-bold text-dark mb-1">
                      {{ order.activityTitle || '活動門票' }}
                      <span v-if="order.sessionTitle" class="text-secondary fw-normal small ms-1">
                        / {{ order.sessionTitle }}
                      </span>
                    </div>
                    <div class="small text-muted font-monospace">訂單編號：{{ order.tOrderId }}</div>
                    <div class="small text-muted">
                      建立時間：{{ formatTime(order.createAt) }}
                      <span v-if="order.ticketCount"> ・ {{ order.ticketCount }} 張票</span>
                    </div>
                  </div>
                  <div class="text-sm-end">
                    <span class="badge rounded-pill mb-2" :class="statusBadgeClass(order.paymentStatus)">
                      {{ statusText(order.paymentStatus) }}
                    </span>
                    <div class="fw-bold text-dark font-monospace">
                      NT$ {{ Number(order.totalAmount || 0).toLocaleString() }}
                    </div>
                  </div>
                </div>
                
                <div class="mt-3 pt-3 border-top d-flex justify-content-end">
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-primary rounded-3 px-3"
                    @click="openTicketDetail(order.tOrderId)"
                  >
                    <i class="bi bi-file-earmark-text me-1"></i>查看明細
                  </button>
                </div>
              </div>
            </div>
          </div>

          <!-- 商城訂單 -->
          <div v-else>
            <div v-if="merchOrders.length === 0" class="empty-state">
              <template v-if="cartStore.totalCount > 0">
                <i class="bi bi-cart-check display-4 text-primary mb-3 d-block"></i>
                <p class="text-secondary mb-1 fw-bold">尚無商城訂單</p>
                <p class="text-muted small mb-3">但您的購物車內有 <strong class="text-danger">{{ cartStore.totalCount }}</strong> 件商品尚未結帳！</p>
                <div class="d-flex justify-content-center gap-3">
                  <router-link to="/shop/cart" class="btn btn-primary rounded-3 px-4">
                    <i class="bi bi-cart4 me-1"></i>前往購物車結帳
                  </router-link>
                  <router-link to="/shop/home" class="btn btn-outline-secondary rounded-3 px-4">
                    繼續逛逛
                  </router-link>
                </div>
              </template>
              <template v-else>
                <i class="bi bi-bag display-4 text-secondary mb-3 d-block"></i>
                <p class="text-secondary mb-3">尚無商城訂單，去逛逛周邊商品吧</p>
                <router-link to="/shop/home" class="btn btn-primary rounded-3 px-4">
                  <i class="bi bi-search me-1"></i>去逛逛
                </router-link>
              </template>
            </div>
            <div v-else class="d-flex flex-column gap-3">
              <div
                v-for="order in merchOrders"
                :key="order.mOrderId"
                class="card border-0 shadow-sm rounded-4 p-3 p-sm-4 order-card"
              >
                <div class="d-flex flex-column flex-sm-row justify-content-between align-items-sm-center gap-2">
                  <div>
                    <div class="fw-bold text-dark mb-1">
                      {{ order.itemSummary || '商城商品' }}
                    </div>
                    <div class="small text-muted font-monospace">訂單編號：{{ order.mOrderId }}</div>
                    <div class="small text-muted">
                      建立時間：{{ formatTime(order.createdAt) }}
                      <span v-if="order.totalQuantity"> ・ 共 {{ order.totalQuantity }} 件</span>
                    </div>
                  </div>
                  <div class="text-sm-end">
                    <span class="badge rounded-pill mb-2" :class="statusBadgeClass(order.paymentStatus)">
                      {{ statusText(order.paymentStatus) }}
                    </span>
                    <div class="fw-bold text-dark font-monospace">
                      NT$ {{ Number(order.totalAmount || 0).toLocaleString() }}
                    </div>
                  </div>
                </div>
                
                <div class="mt-3 pt-3 border-top d-flex justify-content-end">
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-primary rounded-3 px-3"
                    @click="openMerchDetail(order.mOrderId)"
                  >
                    <i class="bi bi-file-earmark-text me-1"></i>查看明細
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 票務訂單明細彈窗 -->
    <BaseModal
      v-model:show="showTicketModal"
      title="票務訂單詳情"
      size="modal-lg"
    >
      <div v-if="selectedTicketOrder" class="ticket-modal-content">
        <!-- 訂單基本資訊 -->
        <div class="card bg-light border-0 rounded-3 p-3 mb-4">
          <div class="row g-3">
            <div class="col-md-4">
              <span class="text-secondary small d-block">訂單編號</span>
              <strong class="text-dark font-monospace">{{ selectedTicketOrder.tOrderId }}</strong>
            </div>
            <div class="col-md-4">
              <span class="text-secondary small d-block">成立時間</span>
              <strong class="text-dark">{{ formatTime(selectedTicketOrder.createAt) }}</strong>
            </div>
            <div class="col-md-4">
              <span class="text-secondary small d-block">聯絡人</span>
              <strong class="text-dark">{{ selectedTicketOrder.contactName || "無" }}</strong>
            </div>
          </div>
        </div>

        <!-- 票券明細清單 -->
        <h5 class="fw-bold text-dark mb-3">
          <i class="bi bi-info-circle me-2 text-primary"></i>票務明細內容
        </h5>

        <div class="row g-3">
          <div
            class="col-12"
            v-for="detail in selectedTicketOrder.orderDetailTickets"
            :key="detail.tDetailId"
          >
            <div class="card detail-item-card border shadow-none rounded-3 p-3 bg-white">
              <div class="row align-items-center g-3">
                <div class="col-md-4">
                  <h6 class="fw-bold text-dark mb-1 text-truncate" :title="detail.activityTitle">
                    {{ detail.activityTitle }}
                  </h6>
                  <p class="text-muted small mb-0 text-truncate">
                    票種:
                    <span class="text-dark fw-semibold">{{ detail.ticketTypeName }}</span>
                    <span v-if="detail.seatInfo" class="ms-2">({{ detail.seatInfo }})</span>
                  </p>
                </div>
                <div class="col-md-2">
                  <span class="text-secondary small d-block">入場姓名</span>
                  <div class="fw-bold text-dark text-truncate" :title="detail.realName || '未填寫'">
                    {{ detail.realName || "未填寫" }}
                  </div>
                </div>
                <div class="col-md-2">
                  <span class="text-secondary small d-block">單價</span>
                  <div class="fw-bold text-primary">
                    {{ formatCurrency(detail.unitPrice) }}
                  </div>
                </div>
                <div class="col-md-2">
                  <span class="text-secondary small d-block">核銷狀態</span>
                  <div>
                    <span :class="getTicketUseClass(detail.isUsed)">
                      {{ getTicketUseText(detail.isUsed) }}
                    </span>
                  </div>
                </div>
                <div class="col-md-2">
                  <span class="text-secondary small d-block">明細狀態</span>
                  <div :class="getTicketItemStatusClass(detail.itemStatus)">
                    {{ getTicketItemStatusText(detail.itemStatus) }}
                  </div>
                </div>
              </div>

              <!-- QR Code 相關區塊 -->
              <div class="mt-3 pt-3 border-top">
                <!-- 1. 已付款且正常狀態的票券 -->
                <div
                  v-if="selectedTicketOrder.paymentStatus === 'PAID' && detail.itemStatus === 'NORMAL'"
                  class="d-flex flex-column flex-sm-row align-items-sm-center justify-content-between gap-3"
                >
                  <!-- 1.1 未使用 (顯示正常 QR Code) -->
                  <div v-if="detail.isUsed === 'Unredeemed'" class="d-flex align-items-center gap-3">
                    <div
                      class="border p-1 bg-white rounded shadow-sm d-flex align-items-center justify-content-center"
                      style="width: 110px; height: 110px; flex-shrink: 0"
                    >
                      <img
                        :src="`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${detail.qrCodeHash}`"
                        :alt="detail.qrCodeHash"
                        class="img-fluid"
                        style="width: 100px; height: 100px"
                      />
                    </div>
                    <div>
                      <div class="d-flex flex-wrap gap-3 gap-sm-4 align-items-center">
                        <div>
                          <span class="text-secondary small d-block">入場驗票電子憑證</span>
                          <code class="text-primary fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.qrCodeHash }}</code>
                        </div>
                        <div class="border-start ps-3 ps-sm-4" style="border-color: #dee2e6 !important;">
                          <span class="text-secondary small d-block">票券卡號</span>
                          <code class="text-dark fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.tDetailId }}</code>
                        </div>
                      </div>
                      <p class="text-muted small mb-0 mt-2">
                        <i class="bi bi-exclamation-circle me-1"></i>入場時請出示此憑證代碼或讓現場工作人員掃描
                      </p>
                    </div>
                  </div>

                  <!-- 1.2 已使用 (顯示已核銷 QR Code) -->
                  <div v-else-if="detail.isUsed === 'Redeemed'" class="d-flex align-items-center gap-3 opacity-75">
                    <div
                      class="border p-1 bg-white rounded shadow-sm d-flex align-items-center justify-content-center position-relative"
                      style="width: 110px; height: 110px; flex-shrink: 0"
                    >
                      <img
                        :src="`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${detail.qrCodeHash}`"
                        :alt="detail.qrCodeHash"
                        class="img-fluid"
                        style="width: 100px; height: 100px; filter: grayscale(1) opacity(0.3);"
                      />
                      <div class="position-absolute top-50 start-50 translate-middle badge bg-success text-white px-2 py-1 rounded shadow">
                        已核銷
                      </div>
                    </div>
                    <div>
                      <div class="d-flex flex-wrap gap-3 gap-sm-4 align-items-center">
                        <div>
                          <span class="text-secondary small d-block">憑證已核銷</span>
                          <code class="text-secondary fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.qrCodeHash }}</code>
                        </div>
                        <div class="border-start ps-3 ps-sm-4" style="border-color: #dee2e6 !important;">
                          <span class="text-secondary small d-block">票券卡號</span>
                          <code class="text-dark fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.tDetailId }}</code>
                        </div>
                      </div>
                      <p class="text-success small mb-0 mt-2">
                        <i class="bi bi-check-circle-fill me-1"></i>此票券已於現場成功驗收核銷
                      </p>
                    </div>
                  </div>

                  <!-- 1.3 其他使用狀態 (例如 Canceled 已取消) -->
                  <div v-else class="d-flex justify-content-between align-items-center w-100 py-1">
                    <div class="text-muted small">
                      <i class="bi bi-x-circle me-1"></i>此票券使用狀態：{{ getTicketUseText(detail.isUsed) }}
                    </div>
                    <div>
                      <span class="text-secondary small">票券卡號：</span>
                      <code class="text-dark fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.tDetailId }}</code>
                    </div>
                  </div>

                  <!-- 退票申請 (僅限未核銷時顯示；送出後由主辦方審核) -->
                  <div v-if="detail.isUsed === 'Unredeemed'" class="text-sm-end align-self-end align-self-sm-center">
                    <span
                      v-if="pendingRequest(detail.tDetailId)"
                      class="badge bg-warning text-dark px-3 py-2 rounded-3 text-nowrap"
                    >
                      <i class="bi bi-hourglass-split me-1"></i>退票審核中
                    </span>
                    <button
                      v-else
                      type="button"
                      class="btn btn-sm btn-outline-danger px-3 py-2 rounded-3 text-nowrap"
                      @click="openRefundModal('TICKET', selectedTicketOrder.tOrderId, detail.tDetailId)"
                    >
                      <i class="bi bi-x-circle me-1"></i>申請退票
                    </button>
                    <div
                      v-if="!pendingRequest(detail.tDetailId) && latestRejected(detail.tDetailId)"
                      class="small text-danger mt-1"
                    >
                      前次申請被駁回{{ latestRejected(detail.tDetailId).rejectNote ? '：' + latestRejected(detail.tDetailId).rejectNote : '' }}
                    </div>
                  </div>
                </div>

                <!-- 2. 已退票狀態 -->
                <div v-else-if="detail.itemStatus === 'REFUNDED'" class="d-flex justify-content-between align-items-center py-1">
                  <div class="text-danger small fw-bold">
                    <i class="bi bi-cash-stack me-1"></i>此票券已退票完成，退款流程已發起
                  </div>
                  <div>
                    <span class="text-secondary small">票券卡號：</span>
                    <code class="text-dark fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.tDetailId }}</code>
                  </div>
                </div>

                <!-- 3. 未付款狀態 -->
                <div v-else-if="selectedTicketOrder.paymentStatus === 'UNPAID'" class="d-flex justify-content-between align-items-center py-1">
                  <div class="text-warning small fw-bold">
                    <i class="bi bi-exclamation-triangle-fill me-1"></i>訂單尚未付款，付款完成後即可使用電子憑證 QR Code
                  </div>
                  <div>
                    <span class="text-secondary small">票券卡號：</span>
                    <code class="text-dark fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.tDetailId }}</code>
                  </div>
                </div>

                <!-- 4. 其他狀態 (如付款失敗、過期等) -->
                <div v-else class="d-flex justify-content-between align-items-center py-1">
                  <div class="text-secondary small">
                    <i class="bi bi-exclamation-circle-fill me-1"></i>訂單狀態為「{{ statusText(selectedTicketOrder.paymentStatus) }}」，無法提供電子憑證。
                  </div>
                  <div>
                    <span class="text-secondary small">票券卡號：</span>
                    <code class="text-dark fw-bold font-monospace bg-light px-2 py-1 rounded">{{ detail.tDetailId }}</code>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
          <span class="fw-bold text-dark">訂單總金額</span>
          <span class="fw-bold text-danger fs-4">
            NT$ {{ Number(selectedTicketOrder.totalAmount || 0).toLocaleString() }}
          </span>
        </div>
      </div>
    </BaseModal>

    <!-- 商品訂單明細彈窗 -->
    <BaseModal
      v-model:show="showMerchModal"
      title="商品訂單詳情"
      size="modal-lg"
    >
      <div v-if="selectedMerchOrder" class="merch-modal-content">
        <!-- 訂單基本資訊 -->
        <div class="card bg-light border-0 rounded-3 p-3 mb-4">
          <div class="row g-3">
            <div class="col-md-4">
              <span class="text-secondary small d-block">訂單編號</span>
              <strong class="text-dark font-monospace">{{ selectedMerchOrder.mOrderId }}</strong>
            </div>
            <div class="col-md-4">
              <span class="text-secondary small d-block">成立時間</span>
              <strong class="text-dark">{{ formatTime(selectedMerchOrder.createdAt) }}</strong>
            </div>
            <div class="col-md-4">
              <span class="text-secondary small d-block">收件人姓名</span>
              <strong class="text-dark">{{ selectedMerchOrder.recipientName || "無" }}</strong>
            </div>
            <div class="col-md-4">
              <span class="text-secondary small d-block">收件人電話</span>
              <strong class="text-dark">{{ selectedMerchOrder.recipientPhone || "無" }}</strong>
            </div>
            <div class="col-md-8">
              <span class="text-secondary small d-block">收件地址</span>
              <strong class="text-dark">{{ selectedMerchOrder.recipientAddress || "無" }}</strong>
            </div>
          </div>
        </div>

        <!-- 商品明細清單 -->
        <h5 class="fw-bold text-dark mb-3 mt-4">
          <i class="bi bi-info-circle me-2 text-primary"></i>商品明細內容
        </h5>

        <div class="row g-3">
          <div
            class="col-12"
            v-for="detail in selectedMerchOrder.orderDetailMerches"
            :key="detail.mDetailId"
          >
            <div class="card detail-item-card border shadow-none rounded-3 p-3 bg-white">
              <div class="row align-items-center g-3">
                <div class="col-md-3">
                  <div class="d-flex align-items-center gap-2">
                    <div
                      class="product-icon-avatar bg-primary-subtle text-primary rounded d-flex align-items-center justify-content-center"
                      style="width: 40px; height: 40px; flex-shrink: 0"
                    >
                      <i class="bi bi-gift"></i>
                    </div>
                    <div>
                      <h6 class="fw-bold text-dark mb-1">
                        {{ detail.productProduct?.productName || "商品" }}
                      </h6>
                      <p class="text-muted small mb-0">
                        規格: {{ getMerchSpec(detail) }}
                      </p>
                    </div>
                  </div>
                </div>
                <div class="col-md-2">
                  <span class="text-secondary small d-block">訂單明細</span>
                  <div class="font-monospace text-dark small text-nowrap overflow-hidden text-truncate" :title="detail.mDetailId">
                    {{ detail.mDetailId }}
                  </div>
                </div>
                <div class="col-md-2">
                  <span class="text-secondary small d-block">單價</span>
                  <div class="fw-bold text-primary">
                    {{ formatCurrency(detail.unitPrice) }}
                  </div>
                </div>
                <div class="col-md-1">
                  <span class="text-secondary small d-block">數量</span>
                  <div class="fw-bold text-dark">
                    {{ detail.quantity }}
                  </div>
                </div>
                <div class="col-md-2">
                  <span class="text-secondary small d-block">狀態</span>
                  <div class="text-nowrap" :class="getMerchItemStatusClass(detail.itemStatus)">
                    {{ getMerchItemStatusText(detail.itemStatus) }}
                  </div>
                </div>
                <!-- 退貨申請 (送出後由主辦方審核) -->
                <div class="col-md-2 text-md-end">
                  <template v-if="selectedMerchOrder.paymentStatus === 'PAID' && detail.itemStatus === 'NORMAL'">
                    <span
                      v-if="pendingRequest(detail.mDetailId)"
                      class="badge bg-warning text-dark px-3 py-2 rounded-3 text-nowrap"
                    >
                      <i class="bi bi-hourglass-split me-1"></i>退貨審核中
                    </span>
                    <button
                      v-else
                      type="button"
                      class="btn btn-sm btn-outline-danger rounded-3 text-nowrap"
                      @click="openRefundModal('MERCH', selectedMerchOrder.mOrderId, detail.mDetailId)"
                    >
                      <i class="bi bi-arrow-counterclockwise me-1"></i>申請退貨
                    </button>
                    <div
                      v-if="!pendingRequest(detail.mDetailId) && latestRejected(detail.mDetailId)"
                      class="small text-danger mt-1"
                    >
                      前次申請被駁回{{ latestRejected(detail.mDetailId).rejectNote ? '：' + latestRejected(detail.mDetailId).rejectNote : '' }}
                    </div>
                  </template>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div class="d-flex justify-content-between align-items-center mt-4 pt-3 border-top">
          <span class="fw-bold text-dark">訂單總金額</span>
          <span class="fw-bold text-danger fs-4">
            NT$ {{ Number(selectedMerchOrder.totalAmount || 0).toLocaleString() }}
          </span>
        </div>
      </div>
    </BaseModal>

    <!-- 退款申請 (退票/退貨) 理由彈窗 -->
    <BaseModal
      v-model:show="showRefundModal"
      :title="refundForm.type === 'TICKET' ? '申請退票' : '申請退貨'"
    >
      <div>
        <p class="text-secondary small mb-3">
          <i class="bi bi-info-circle me-1"></i>
          送出申請後將由主辦方審核，審核通過後才會執行退款{{ refundForm.type === 'MERCH' ? '，並請依指示寄回商品' : '' }}。
        </p>
        <label class="form-label fw-bold">申退理由 <span class="text-danger">*</span></label>
        <textarea
          v-model="refundForm.reason"
          class="form-control"
          rows="4"
          maxlength="500"
          placeholder="請說明您申請退款的原因（必填，最多 500 字）"
        ></textarea>
        <div class="text-end small text-muted mt-1">{{ refundForm.reason.length }}/500</div>
      </div>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary rounded-3" @click="showRefundModal = false">
          取消
        </button>
        <button
          type="button"
          class="btn btn-danger rounded-3"
          :disabled="submittingRefund || !refundForm.reason.trim()"
          @click="submitRefundRequest"
        >
          <span v-if="submittingRefund" class="spinner-border spinner-border-sm me-1" role="status"></span>
          送出申請
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast.js';
import BaseModal from '@/components/common/BaseModal.vue';
import { useCartStore } from '@/stores/cart.js';

const route = useRoute();
const toast = useToast();
const cartStore = useCartStore();

const activeTab = ref('ticket');
const loading = ref(true);
const ticketOrders = ref([]);
const merchOrders = ref([]);

const showTicketModal = ref(false);
const selectedTicketOrder = ref(null);
const showMerchModal = ref(false);
const selectedMerchOrder = ref(null);

// 退款申請 (審核制) 狀態
const myRefundRequests = ref([]);
const showRefundModal = ref(false);
const refundForm = ref({ type: 'TICKET', orderId: '', detailId: '', reason: '' });
const submittingRefund = ref(false);

const formatTime = (value) => {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString();
};

const formatCurrency = (val) => {
  if (val === null || val === undefined) return 'NT$ 0';
  return `NT$ ${Number(val).toLocaleString()}`;
};

const statusText = (status) => {
  switch (status) {
    case 'PAID':
      return '已付款';
    case 'UNPAID':
      return '待付款';
    case 'FAILED':
      return '付款失敗';
    case 'EXPIRED':
      return '已逾期';
    default:
      return status || '未知';
  }
};

const statusBadgeClass = (status) => {
  switch (status) {
    case 'PAID':
      return 'bg-success';
    case 'UNPAID':
      return 'bg-warning text-dark';
    case 'FAILED':
    case 'EXPIRED':
      return 'bg-danger';
    default:
      return 'bg-secondary';
  }
};

const getTicketUseText = (useStatus) => {
  switch (useStatus) {
    case 'Unredeemed': return '未核銷';
    case 'Redeemed': return '已核銷';
    case 'Canceled': return '已取消';
    case 'UNESTABLISHED': return '未建立';
    default: return useStatus || '未核銷';
  }
};

const getTicketUseClass = (useStatus) => {
  switch (useStatus) {
    case 'Unredeemed': return 'badge bg-primary text-white';
    case 'Redeemed': return 'badge bg-success text-white';
    case 'Canceled': return 'badge bg-danger text-white';
    case 'UNESTABLISHED': return 'badge bg-secondary text-white';
    default: return 'badge bg-secondary text-white';
  }
};

const getTicketItemStatusText = (status) => {
  switch (status) {
    case 'NORMAL': return '正常';
    case 'REFUNDED': return '已退票';
    default: return status || '正常';
  }
};

const getTicketItemStatusClass = (status) => {
  switch (status) {
    case 'NORMAL': return 'text-success fw-bold';
    case 'REFUNDED': return 'text-danger fw-bold';
    default: return 'text-dark';
  }
};

const getMerchItemStatusText = (status) => {
  switch (status) {
    case 'NORMAL': return '正常';
    case 'RETURNED': return '已退貨';
    case 'CANCELLED': return '已取消';
    case 'UNPAID': return '待付款';
    default: return status || '正常';
  }
};

const getMerchItemStatusClass = (status) => {
  switch (status) {
    case 'NORMAL': return 'text-success fw-bold';
    case 'RETURNED': return 'text-danger fw-bold';
    case 'CANCELLED': return 'text-muted';
    case 'UNPAID': return 'text-warning fw-bold';
    default: return 'text-dark';
  }
};

const getMerchSpec = (item) => {
  if (!item.productProduct || !item.productProduct.variants) return '預設規格';
  const variant = item.productProduct.variants.find(v => v.variantId === item.variantId);
  if (!variant) return `規格 ID: ${item.variantId}`;
  const color = variant.productColor ? `顏色: ${variant.productColor}` : '';
  const size = variant.productSize ? `尺寸: ${variant.productSize}` : '';
  return [color, size].filter(Boolean).join(' | ') || '預設規格';
};

const loadOrders = async () => {
  loading.value = true;
  try {
    const [ticketRes, merchRes] = await Promise.all([
      axios.get('/api/member/ticket-orders'),
      axios.get('/api/member/merch-orders'),
    ]);
    ticketOrders.value = ticketRes.data?.data || [];
    merchOrders.value = merchRes.data?.data || [];
  } catch (err) {
    console.error('讀取會員訂單失敗:', err);
    toast.error('訂單載入失敗，請稍後再試');
  } finally {
    loading.value = false;
  }
};

const openTicketDetail = async (tOrderId) => {
  try {
    const res = await axios.get(`/api/member/ticket-orders/${tOrderId}`);
    selectedTicketOrder.value = res.data?.data;
    showTicketModal.value = true;
  } catch (err) {
    console.error('取得門票訂單明細失敗:', err);
    toast.error('無法載入訂單明細');
  }
};

const openMerchDetail = async (mOrderId) => {
  try {
    const res = await axios.get(`/api/member/merch-orders/${mOrderId}`);
    selectedMerchOrder.value = res.data?.data;
    showMerchModal.value = true;
  } catch (err) {
    console.error('取得商品訂單明細失敗:', err);
    toast.error('無法載入訂單明細');
  }
};

// ── 退款申請 (審核制)：送出附理由的申請，待主辦方於 B2B 後台核准後才執行退款 ──

const loadRefundRequests = async () => {
  try {
    const res = await axios.get('/api/member/refund-requests');
    myRefundRequests.value = res.data?.data || [];
  } catch (err) {
    console.error('讀取退款申請紀錄失敗:', err);
  }
};

// 該明細是否有待審核申請 (清單為新到舊)
const pendingRequest = (detailId) =>
  myRefundRequests.value.find((r) => r.detailId === detailId && r.status === 'PENDING');

// 該明細最近一筆被駁回的申請 (顯示駁回原因，並允許再次申請)
const latestRejected = (detailId) =>
  myRefundRequests.value.find((r) => r.detailId === detailId && r.status === 'REJECTED');

const openRefundModal = (type, orderId, detailId) => {
  refundForm.value = { type, orderId, detailId, reason: '' };
  showRefundModal.value = true;
};

const submitRefundRequest = async () => {
  if (!refundForm.value.reason.trim()) {
    toast.error('請填寫申退理由');
    return;
  }
  submittingRefund.value = true;
  try {
    const res = await axios.post('/api/member/refund-requests', refundForm.value);
    toast.success(res.data?.message || '已送出退款申請，請等候主辦方審核');
    showRefundModal.value = false;
    await loadRefundRequests();
  } catch (err) {
    console.error('送出退款申請失敗:', err);
    toast.error(err.response?.data?.message || '申請送出失敗，請稍後再試');
  } finally {
    submittingRefund.value = false;
  }
};

onMounted(() => {
  const tab = route.query.tab;
  if (tab === 'merch' || tab === 'ticket') {
    activeTab.value = tab;
  }
  loadOrders();
  loadRefundRequests();
  cartStore.fetchRemoteCart();
});
</script>

<style scoped>
.nav-pills .nav-link {
  color: var(--tap-light-blue);
  font-weight: 600;
  border-radius: 999px;
  padding: 0.5rem 1.25rem;
}

.nav-pills .nav-link.active {
  background-color: var(--tap-primary);
  color: #fff;
}

.order-card {
  transition: box-shadow 0.2s ease-in-out;
}

.order-card:hover {
  box-shadow: 0 8px 20px rgba(0, 0, 0, 0.08) !important;
}

.empty-state {
  text-align: center;
  padding: 4rem 1rem;
  background-color: #fff;
  border-radius: 1rem;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.05);
}

.detail-item-card {
  transition: border-color 0.2s;
}

.detail-item-card:hover {
  border-color: var(--tap-primary) !important;
}
</style>
