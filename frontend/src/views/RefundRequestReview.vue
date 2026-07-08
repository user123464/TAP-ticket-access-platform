<template>
  <div class="refund-request-page container-fluid py-4">
    <!-- 頁面標題 -->
    <div class="d-flex align-items-center justify-content-between mb-4">
      <h4 class="fw-bold text-dark mb-0">
        <i class="bi bi-inbox text-primary me-2"></i>退款申請審核
      </h4>
      <button type="button" class="btn btn-sm btn-outline-secondary rounded-3" @click="loadRequests">
        <i class="bi bi-arrow-clockwise me-1"></i>重新整理
      </button>
    </div>

    <!-- 狀態統計卡片 -->
    <div class="row g-3 mb-4">
      <div class="col-6 col-md-3">
        <div class="card border-0 shadow-sm rounded-4 p-3">
          <span class="text-secondary small">待審核</span>
          <span class="fs-4 fw-bold text-warning">{{ countByStatus('PENDING') }}</span>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="card border-0 shadow-sm rounded-4 p-3">
          <span class="text-secondary small">已核准</span>
          <span class="fs-4 fw-bold text-success">{{ countByStatus('APPROVED') }}</span>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="card border-0 shadow-sm rounded-4 p-3">
          <span class="text-secondary small">已駁回</span>
          <span class="fs-4 fw-bold text-danger">{{ countByStatus('REJECTED') }}</span>
        </div>
      </div>
      <div class="col-6 col-md-3">
        <div class="card border-0 shadow-sm rounded-4 p-3">
          <span class="text-secondary small">全部申請</span>
          <span class="fs-4 fw-bold text-dark">{{ requests.length }}</span>
        </div>
      </div>
    </div>

    <!-- 篩選列 -->
    <div class="card border-0 shadow-sm rounded-4 p-3 mb-4">
      <div class="row g-2 align-items-center">
        <div class="col-12 col-md-4">
          <div class="input-group input-group-sm">
            <span class="input-group-text bg-white"><i class="bi bi-search"></i></span>
            <input
              v-model="keyword"
              type="text"
              class="form-control"
              placeholder="搜尋申請人 / 訂單編號 / 品項 / 理由"
            />
          </div>
        </div>
        <div class="col-6 col-md-3">
          <select v-model="typeFilter" class="form-select form-select-sm">
            <option value="">全部類型</option>
            <option value="TICKET">退票</option>
            <option value="MERCH">退貨</option>
          </select>
        </div>
        <div class="col-6 col-md-3">
          <select v-model="statusFilter" class="form-select form-select-sm">
            <option value="">全部狀態</option>
            <option value="PENDING">待審核</option>
            <option value="APPROVED">已核准</option>
            <option value="REJECTED">已駁回</option>
          </select>
        </div>
      </div>
    </div>

    <!-- 申請清單 -->
    <div class="card border-0 shadow-sm rounded-4">
      <div v-if="loading" class="text-center py-5 text-secondary">
        <div class="spinner-border text-primary mb-3" role="status"></div>
        <div>申請資料載入中...</div>
      </div>
      <div v-else-if="filteredRequests.length === 0" class="text-center py-5 text-secondary">
        <i class="bi bi-inbox display-4 d-block mb-3"></i>
        目前沒有符合條件的退款申請
      </div>
      <div v-else class="table-responsive">
        <table class="table table-hover align-middle mb-0">
          <thead class="table-light">
            <tr>
              <th>類型</th>
              <th>申請人 / 聯絡資訊</th>
              <th>品項</th>
              <th class="text-end">金額</th>
              <th>購買時間</th>
              <th>申請時間</th>
              <th style="min-width: 180px">申退理由</th>
              <th>狀態</th>
              <th class="text-end">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="req in filteredRequests" :key="req.requestId">
              <td>
                <span class="badge rounded-pill" :class="req.requestType === 'TICKET' ? 'bg-primary' : 'bg-info text-dark'">
                  {{ req.requestType === 'TICKET' ? '退票' : '退貨' }}
                </span>
              </td>
              <td>
                <div class="fw-bold text-dark">{{ req.applicantName || '—' }}</div>
                <div class="small text-muted">{{ req.applicantPhone || '' }}</div>
                <div class="small text-muted">{{ req.applicantEmail || '' }}</div>
                <div class="small text-muted font-monospace">訂單：{{ req.orderId }}</div>
              </td>
              <td>
                <div class="text-dark small" style="max-width: 220px">{{ req.itemSummary || '—' }}</div>
              </td>
              <td class="text-end fw-bold font-monospace">
                {{ req.amount != null ? 'NT$ ' + Number(req.amount).toLocaleString() : '—' }}
              </td>
              <td class="small text-muted">{{ formatTime(req.purchasedAt) }}</td>
              <td class="small text-muted">{{ formatTime(req.createdAt) }}</td>
              <td>
                <div class="small text-dark reason-cell" :title="req.reason">{{ req.reason }}</div>
                <div v-if="req.status === 'REJECTED' && req.rejectNote" class="small text-danger mt-1">
                  駁回原因：{{ req.rejectNote }}
                </div>
              </td>
              <td>
                <span class="badge rounded-pill" :class="statusBadgeClass(req.status)">
                  {{ statusText(req.status) }}
                </span>
                <div v-if="req.processedAt" class="small text-muted mt-1">{{ formatTime(req.processedAt) }}</div>
              </td>
              <td class="text-end">
                <template v-if="req.status === 'PENDING'">
                  <button
                    type="button"
                    class="btn btn-sm btn-success rounded-3 me-1"
                    :disabled="processingId === req.requestId"
                    @click="approveRequest(req)"
                  >
                    <span v-if="processingId === req.requestId" class="spinner-border spinner-border-sm me-1" role="status"></span>
                    <i v-else class="bi bi-check-lg me-1"></i>核准
                  </button>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-danger rounded-3"
                    :disabled="processingId === req.requestId"
                    @click="openRejectModal(req)"
                  >
                    <i class="bi bi-x-lg me-1"></i>駁回
                  </button>
                </template>
                <span v-else class="text-muted small">已處理</span>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
    </div>

    <!-- 駁回原因彈窗 -->
    <BaseModal v-model:show="showRejectModal" title="駁回退款申請">
      <div v-if="rejectTarget">
        <p class="text-secondary small mb-3">
          <i class="bi bi-info-circle me-1"></i>
          駁回後申請人可再次送出申請。駁回原因將顯示給申請人（選填）。
        </p>
        <div class="card bg-light border-0 rounded-3 p-3 mb-3 small">
          <div><strong>{{ rejectTarget.requestType === 'TICKET' ? '退票' : '退貨' }}</strong>｜{{ rejectTarget.itemSummary || rejectTarget.orderId }}</div>
          <div class="text-muted mt-1">申退理由：{{ rejectTarget.reason }}</div>
        </div>
        <label class="form-label fw-bold">駁回原因</label>
        <textarea
          v-model="rejectNote"
          class="form-control"
          rows="3"
          maxlength="500"
          placeholder="例如：已超過退換貨期限（選填）"
        ></textarea>
      </div>
      <template #footer>
        <button type="button" class="btn btn-outline-secondary rounded-3" @click="showRejectModal = false">
          取消
        </button>
        <button
          type="button"
          class="btn btn-danger rounded-3"
          :disabled="processingId != null"
          @click="rejectRequest"
        >
          <span v-if="processingId != null" class="spinner-border spinner-border-sm me-1" role="status"></span>
          確認駁回
        </button>
      </template>
    </BaseModal>
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast.js';
import { useConfirm } from '@/composables/useConfirm.js';
import BaseModal from '@/components/common/BaseModal.vue';

const route = useRoute();
const toast = useToast();
const { confirm } = useConfirm();

const loading = ref(true);
const requests = ref([]);
const keyword = ref('');
const typeFilter = ref('');
const statusFilter = ref('');
const processingId = ref(null);

const showRejectModal = ref(false);
const rejectTarget = ref(null);
const rejectNote = ref('');

const organizerId = computed(() => route.params.organizerId);

const formatTime = (value) => {
  if (!value) return '—';
  const d = new Date(value);
  if (Number.isNaN(d.getTime())) return '—';
  return d.toLocaleString();
};

const statusText = (status) => {
  switch (status) {
    case 'PENDING': return '待審核';
    case 'APPROVED': return '已核准';
    case 'REJECTED': return '已駁回';
    default: return status || '未知';
  }
};

const statusBadgeClass = (status) => {
  switch (status) {
    case 'PENDING': return 'bg-warning text-dark';
    case 'APPROVED': return 'bg-success';
    case 'REJECTED': return 'bg-danger';
    default: return 'bg-secondary';
  }
};

const countByStatus = (status) => requests.value.filter((r) => r.status === status).length;

const filteredRequests = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return requests.value.filter((r) => {
    if (typeFilter.value && r.requestType !== typeFilter.value) return false;
    if (statusFilter.value && r.status !== statusFilter.value) return false;
    if (kw) {
      const haystack = [r.applicantName, r.applicantEmail, r.applicantPhone, r.orderId, r.detailId, r.itemSummary, r.reason]
        .filter(Boolean)
        .join(' ')
        .toLowerCase();
      if (!haystack.includes(kw)) return false;
    }
    return true;
  });
});

const loadRequests = async () => {
  loading.value = true;
  try {
    const res = await axios.get(`/api/org/${organizerId.value}/refund-requests`);
    requests.value = res.data?.data || [];
  } catch (err) {
    console.error('讀取退款申請清單失敗:', err);
    toast.error('退款申請載入失敗，請稍後再試');
  } finally {
    loading.value = false;
  }
};

const approveRequest = async (req) => {
  const isConfirmed = await confirm({
    title: '確定核准此退款申請？',
    message: `核准後將立即執行${req.requestType === 'TICKET' ? '退票（還原座位庫存並扣減訂單金額）' : '退貨（回補商品庫存並扣減訂單金額）'}，此操作無法復原。`,
    confirmText: '確定核准',
    cancelText: '取消',
    variant: 'danger',
  });
  if (!isConfirmed) return;

  processingId.value = req.requestId;
  try {
    const res = await axios.post(`/api/org/${organizerId.value}/refund-requests/${req.requestId}/approve`);
    toast.success(res.data?.message || '已核准退款申請');
    await loadRequests();
  } catch (err) {
    console.error('核准退款申請失敗:', err);
    toast.error(err.response?.data?.message || '核准失敗，請稍後再試');
  } finally {
    processingId.value = null;
  }
};

const openRejectModal = (req) => {
  rejectTarget.value = req;
  rejectNote.value = '';
  showRejectModal.value = true;
};

const rejectRequest = async () => {
  if (!rejectTarget.value) return;
  processingId.value = rejectTarget.value.requestId;
  try {
    const res = await axios.post(
      `/api/org/${organizerId.value}/refund-requests/${rejectTarget.value.requestId}/reject`,
      { note: rejectNote.value }
    );
    toast.success(res.data?.message || '已駁回退款申請');
    showRejectModal.value = false;
    await loadRequests();
  } catch (err) {
    console.error('駁回退款申請失敗:', err);
    toast.error(err.response?.data?.message || '駁回失敗，請稍後再試');
  } finally {
    processingId.value = null;
  }
};

onMounted(loadRequests);
</script>

<style scoped>
.reason-cell {
  max-width: 220px;
  white-space: pre-wrap;
  word-break: break-word;
}
</style>
