<template>
  <div class="modal fade" id="merchDetail" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered"> 
      <div class="modal-content shadow-lg border-0">
        
        <div class="modal-header px-4 py-3 bg-white">
          <h5 class="modal-title fw-bold d-flex align-items-center">
            <i class="bi bi-receipt-cutoff me-2 text-primary fs-4"></i>商城訂單明細
          </h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        
        <div class="modal-body p-4 bg-light-subtle" v-if="order">
          <!-- 頂部訂單編號與狀態 -->
          <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
              <span class="text-muted small fw-bold d-block mb-1">商城訂單編號</span>
              <div class="fw-bold fs-4 text-dark font-monospace">#{{ order.m_order_id }}</div>
            </div>
            <div class="d-flex flex-column align-items-end gap-2">
              <span class="text-muted small fw-bold d-block">狀態標籤</span>
              <div class="d-flex gap-2">
                <span class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center" :class="itemStatusClass(order.item_status)">
                  訂單: {{ order.item_status }}
                </span>
              </div>
            </div>
          </div>

          <hr class="my-3 opacity-25" />

          <!-- 金額統計卡片 -->
          <div class="row g-3 mb-4">
            <div class="col-6">
              <div class="p-3 border rounded bg-white shadow-sm d-flex flex-column h-100">
                <span class="text-muted small mb-2"><i class="bi bi-tag-fill text-primary me-1"></i>單筆消費金額</span>
                <span class="price-tag mt-auto">NT$ {{ (order.unit_price * order.quantity) || 0 }}</span>
              </div>
            </div>
            <div class="col-6">
              <div class="p-3 border rounded bg-white shadow-sm d-flex flex-column h-100">
                <span class="text-muted small mb-2"><i class="bi bi-wallet2 text-success me-1"></i>訂單總額</span>
                <span class="total-price-tag mt-auto">NT$ {{ order.total_amount || 0 }}</span>
              </div>
            </div>
          </div>

          <!-- 詳細明細資訊 -->
          <div class="mb-4">
            <h6 class="section-title"><i class="bi bi-info-circle-fill me-1 text-primary"></i>商品明細</h6>
            <div class="p-3 border rounded bg-white shadow-sm">
              <div class="row g-3">
                <div class="col-6">
                  <div class="detail-label">明細 ID</div>
                  <div class="detail-value text-secondary font-monospace">{{ order.m_detail_id || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">購買者</div>
                  <div class="detail-value">{{ order.user_id || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">商品識別碼</div>
                  <div class="detail-value text-secondary font-monospace">{{ order.product_id || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">款式 ID</div>
                  <div class="detail-value text-secondary font-monospace">{{ order.variant_id || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">購買數量</div>
                  <div class="detail-value">{{ order.quantity || '0' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">商品單價</div>
                  <div class="detail-value">NT$ {{ order.unit_price || '0' }}</div>
                </div>
                <div class="col-12">
                  <div class="detail-label">訂單建立時間</div>
                  <div class="detail-value text-secondary">{{ order.created_at || '-' }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 收件與配送資訊 -->
          <div>
            <h6 class="section-title"><i class="bi bi-truck me-1 text-primary"></i>收件與配送資訊</h6>
            <div class="p-3 border rounded bg-white shadow-sm">
              <div class="row g-3">
                <div class="col-6">
                  <div class="detail-label">收件人姓名</div>
                  <div class="detail-value">{{ order.recipient_name || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">聯絡電話</div>
                  <div class="detail-value">{{ order.recipient_phone || '-' }}</div>
                </div>
                <div class="col-12">
                  <div class="detail-label">電子信箱</div>
                  <div class="detail-value text-secondary">{{ order.recipient_email || '-' }}</div>
                </div>
                <div class="col-12">
                  <div class="detail-label">配送地址</div>
                  <div class="detail-value">{{ order.recipient_address || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">身分證字號</div>
                  <div class="detail-value text-secondary font-monospace">{{ order.identity_number || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">付款時間</div>
                  <div class="detail-value text-secondary">{{ order.paid_at || '尚未付款' }}</div>
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <div class="modal-footer bg-light-subtle border-top-0 px-4 py-3">
          <button type="button" class="btn btn-secondary px-4 fw-bold rounded-pill" data-bs-dismiss="modal">關閉</button>
        </div>

      </div>
    </div>
  </div>
</template>

<script setup>
defineProps({ order: Object });

// 訂單狀態樣式
const itemStatusClass = (status) => {
  if (status === '正常') return 'badge-soft-success';
  if (status === '取消') return 'badge-soft-secondary';
  if (status === '待付') return 'badge-soft-warning';
  return 'badge-soft-danger'; // 退貨
};
</script>

<style scoped>
.modal-content {
  border-radius: 16px;
  overflow: hidden;
}
.section-title {
  font-size: 0.8rem;
  font-weight: 700;
  text-transform: uppercase;
  color: #495057;
  letter-spacing: 0.5px;
  margin-bottom: 0.6rem;
}
.detail-label {
  font-size: 0.75rem;
  color: #6c757d;
  margin-bottom: 0.2rem;
}
.detail-value {
  font-size: 0.9rem;
  font-weight: 600;
  color: #212529;
}
.price-tag {
  font-size: 1.2rem;
  font-weight: 700;
  color: #0d6efd;
}
.total-price-tag {
  font-size: 1.2rem;
  font-weight: 700;
  color: #198754;
}

/* 狀態軟色調樣式 */
.badge-soft-success {
  background-color: #d1e7dd;
  color: #0f5132;
  border: 1px solid rgba(15, 81, 50, 0.2);
}
.badge-soft-danger {
  background-color: #f8d7da;
  color: #842029;
  border: 1px solid rgba(132, 32, 41, 0.2);
}
.badge-soft-secondary {
  background-color: #e2e3e5;
  color: #41464b;
  border: 1px solid rgba(65, 70, 75, 0.2);
}
.badge-soft-warning {
  background-color: #fff0e6;
  color: #e65100;
  border: 1px solid rgba(230, 81, 0, 0.2);
}
</style>
