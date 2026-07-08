<template>
  <div class="modal fade" id="ticketDetail" tabindex="-1" aria-hidden="true">
    <div class="modal-dialog modal-dialog-centered"> 
      <div class="modal-content shadow-lg border-0">
        
        <div class="modal-header px-4 py-3 bg-white">
          <h5 class="modal-title fw-bold d-flex align-items-center">
            <i class="bi bi-ticket-detailed-fill me-2 text-primary fs-4"></i>票務訂單明細
          </h5>
          <button type="button" class="btn-close" data-bs-dismiss="modal" aria-label="Close"></button>
        </div>
        
        <div class="modal-body p-4 bg-light-subtle" v-if="order">
          <!-- 頂部訂單編號與狀態 -->
          <div class="d-flex justify-content-between align-items-start mb-3">
            <div>
              <span class="text-muted small fw-bold d-block mb-1">票務訂單編號</span>
              <div class="fw-bold fs-4 text-dark font-monospace">#{{ order.t_order_id }}</div>
            </div>
            <div class="d-flex flex-column align-items-end gap-2">
              <span class="text-muted small fw-bold d-block">狀態標籤</span>
              <div class="d-flex gap-2">
                <span class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center" :class="itemStatusClass(order.item_status)">
                  訂單: {{ order.item_status }}
                </span>
                <span class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center" :class="checkInStatusClass(order.is_used)">
                  核銷: {{ order.is_used || '未核銷' }}
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
                <span class="price-tag mt-auto">NT$ {{ order.unit_price || 0 }}</span>
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
            <h6 class="section-title"><i class="bi bi-info-circle-fill me-1 text-primary"></i>票券明細</h6>
            <div class="p-3 border rounded bg-white shadow-sm">
              <div class="row g-3">
                <div class="col-6">
                  <div class="detail-label">明細 ID</div>
                  <div class="detail-value text-secondary font-monospace">{{ order.t_detail_id || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">購買者</div>
                  <div class="detail-value">{{ order.user_id || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">票種 ID</div>
                  <div class="detail-value">{{ order.price_category_id || '-' }}</div>
                </div>
                <div class="col-6">
                  <div class="detail-label">座位 ID</div>
                  <div class="detail-value"><span class="badge bg-light text-dark border px-2 py-1 font-monospace">{{ order.seat_id || '-' }}</span></div>
                </div>
                <div class="col-12">
                  <div class="detail-label">訂單建立時間</div>
                  <div class="detail-value text-secondary">{{ order.created_at || '-' }}</div>
                </div>
              </div>
            </div>
          </div>

          <!-- 安全與核銷 -->
          <div>
            <h6 class="section-title"><i class="bi bi-shield-lock-fill me-1 text-primary"></i>安全與核銷資訊</h6>
            <div class="p-3 border rounded bg-white shadow-sm">
              <div class="mb-3">
                <div class="detail-label">核銷時間</div>
                <div class="detail-value">
                  <span v-if="checkIsUsed(order.is_used)" class="text-danger fw-bold">
                    <i class="bi bi-clock-history me-1"></i>{{ formatTime(order.used_at) }}
                  </span>
                  <span v-else class="text-muted small">-</span>
                </div>
              </div>
              <div>
                <div class="detail-label mb-2">核銷 QR Code</div>
                <div v-if="order.qr_code_hash" class="d-flex flex-column align-items-center gap-2 mb-2">
                  <img
                    :src="`https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=${encodeURIComponent(order.qr_code_hash)}`"
                    :alt="order.qr_code_hash"
                    class="img-fluid border rounded p-1 bg-white"
                    style="width: 150px; height: 150px"
                  />
                </div>
                <div class="detail-label">QR Code Hash</div>
                <div class="qr-code-box text-break">{{ order.qr_code_hash || '無資料' }}</div>
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

// 判斷是否已核銷 (避免 "未核銷" 字串被誤判為 true)
const checkIsUsed = (status) => status === true || status === 1 || status === '已核銷';

// 格式化時間，程式碼大幅簡化
const formatTime = (time) => {
  if (!time) return '無';
  const d = new Date(time);
  return isNaN(d.getTime()) ? time : d.toLocaleString('zh-TW');
};

// 訂單狀態樣式
const itemStatusClass = (status) => {
  if (status === '正常') return 'badge-soft-success';
  if (status === '取消') return 'badge-soft-secondary';
  if (status === '待付') return 'badge-soft-warning';
  return 'badge-soft-danger'; // 退票
};

// 核銷狀態樣式
const checkInStatusClass = (status) => {
  if (status === '已核銷' || status === '已取消') return 'badge-soft-secondary';
  return 'badge-soft-success'; // 未核銷
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
.qr-code-box {
  background-color: #f8f9fa;
  border: 1px dashed #dee2e6;
  border-radius: 6px;
  padding: 0.6rem;
  font-family: var(--bs-font-monospace);
  font-size: 0.75rem;
  color: #495057;
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


