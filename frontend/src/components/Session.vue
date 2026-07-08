<template>
  <div class="card shadow-sm border border-2 rounded-4 mb-3 position-relative">
    <div class="card-body">

      <div class="row align-items-center">
        <!-- 左側資訊 -->
        <div class="col-12 session-info-col">

          <!-- 標題 -->
          <div class="fs-5 fw-bold text-dark mb-2">
            {{ session.title }}
          </div>

          <!-- 描述 -->
          <div class="text-muted small lh-lg mb-3 p-2 bg-light rounded">
            {{ session.detail }}
          </div>

          <!-- 時間區塊 -->
          <div class="small text-secondary d-flex flex-column gap-1">

            <div>
              場地：{{session.locationName}}
            </div>

            <div>
              <i class="bi bi-clock me-1"></i>表演時間：{{ formatDateTime(session.startTime) }}
              <span class="mx-1">~</span>
              {{ formatDateTime(session.endTime) }}
            </div>

            <div>
              <i class="bi bi-ticket-detailed me-1"></i>販售時間：
              {{ formatDateTime(session.sellingStartTime) }}
              <span class="mx-1">~</span>
              {{ formatDateTime(session.sellingEndTime) }}
            </div>

          </div>
        </div>
      </div>

      <!-- 前往購票按鈕 (絕對定位於右上角) -->
      <button
        type="button"
        class="btn btn-primary rounded-pill text-white fixed-btn position-absolute session-book-btn"
        @click="goBookTicket"
      >
        <i class="bi bi-ticket me-1"></i>前往購票
      </button>

    </div>
  </div>
</template>

<script setup>
import { useRouter, useRoute } from 'vue-router'

const props = defineProps({
    session: {
        type: Object,
        required: true
    }
})

//轉換時間格式===========================
function formatDateTime(dt) {
    if (!dt) return ''
    return dt.replace('T', ' ').slice(0, 16)
}

const router = useRouter()
const route = useRoute()

function goBookTicket() {
    const currentThemeId = route.params.themeId

    router.push({
        name: 'BookTicket',
        params: {
            themeId: currentThemeId,
            sessionId: props.session.sessionId
        }
    })
}
</script>

<style scoped>
.fixed-btn {
  width: 140px;
}

.session-book-btn {
  top: 1.5rem;
  right: 1.5rem;
  height: 38px; /* 比原按鈕高度約43px減少了5px */
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: normal !important; /* 不要粗體 */
}

.session-info-col {
  padding-right: 160px;
}

.card {
  background: linear-gradient(135deg, #ffffff 0%, #f8f9fa 100%);
  border: 1px solid rgba(0, 0, 0, 0.05);
  border-left: 5px solid #0d6efd;
  transition: all 0.2s ease;
}

.card:hover {
  transform: translateY(-3px);
  box-shadow: 0 10px 25px rgba(0, 0, 0, 0.12);
}

@media (max-width: 576px) {
  .session-book-btn {
    position: static !important;
    margin-top: 15px;
    width: 100%;
  }
  .session-info-col {
    padding-right: 0px;
  }
}
</style>
