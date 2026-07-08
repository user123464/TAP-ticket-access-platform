<template>
  <div class="checkout-page bg-light min-vh-100 py-5">
    <div class="container">
      <div class="row g-4">
        <!-- 左側：聯絡資訊與確認 (50%) -->
        <div class="col-lg-6">
          <form @submit.prevent="submitCheckout" class="d-flex flex-column gap-4" novalidate>
            <!-- 聯絡人與取票資訊 -->
            <div class="card border-0 shadow-sm rounded-4 p-4">
              <div class="d-flex justify-content-between align-items-center mb-4">
                <h5 class="card-title fw-bold mb-0 text-dark">
                  <i class="bi bi-person-fill text-primary me-2"></i>聯絡資訊
                </h5>
                <div class="form-check form-switch mb-0">
                  <input class="form-check-input cursor-pointer" type="checkbox" id="autoFillCheck"
                    v-model="isAutoFilled" @change="toggleAutoFill" />
                  <label class="form-check-label small text-muted cursor-pointer" for="autoFillCheck">帶入會員資料</label>
                </div>
              </div>

              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label small fw-bold text-secondary">姓名</label>
                  <input type="text" class="form-control animate-transition" placeholder="請輸入真實姓名" v-model="form.name"
                    required />
                  <div v-if="validationErrors.name" class="text-danger small mt-1">
                    {{ validationErrors.name }}
                  </div>
                </div>
                <div class="col-md-6">
                  <label class="form-label small fw-bold text-secondary">手機號碼</label>
                  <input type="tel" class="form-control animate-transition" placeholder="例：0912345678"
                    v-model="form.phone" required />
                  <div v-if="validationErrors.phone" class="text-danger small mt-1">
                    {{ validationErrors.phone }}
                  </div>
                </div>
                <div class="col-12">
                  <label class="form-label small fw-bold text-secondary">電子信箱 (發送電子票券)</label>
                  <input type="email" class="form-control animate-transition" placeholder="example@mail.com"
                    v-model="form.email" required />
                  <div v-if="validationErrors.email" class="text-danger small mt-1">
                    {{ validationErrors.email }}
                  </div>
                </div>
                <div class="col-12">
                  <label class="form-label small fw-bold text-secondary">身分證字號 / 護照號碼 (實名制驗證)</label>
                  <input type="text" class="form-control animate-transition" placeholder="請輸入身分證字號或護照號碼"
                    v-model="form.identityNumber" required />
                  <div v-if="validationErrors.identityNumber" class="text-danger small mt-1">
                    {{ validationErrors.identityNumber }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 同意條款與確認按鈕 -->
            <div class="card border-0 shadow-sm rounded-4 p-4">
              <div class="mb-3">
                <div class="form-check">
                  <input class="form-check-input cursor-pointer" type="checkbox" id="agreeTerms" v-model="form.agree" />
                  <label class="form-check-label small text-muted cursor-pointer" for="agreeTerms">
                    我已閱讀並同意
                    <a href="#" @click.prevent class="text-primary text-decoration-none">服務條款</a>
                    與
                    <a href="#" @click.prevent class="text-primary text-decoration-none">退票規定</a>。
                  </label>
                </div>
                <div v-if="validationErrors.agree" class="text-danger small mt-1">
                  {{ validationErrors.agree }}
                </div>
              </div>

              <button
                class="btn btn-primary w-100 py-3 rounded-3 fw-bold fs-5 shadow-sm d-flex align-items-center justify-content-center gap-2 btn-submit-hover"
                type="submit" :disabled="isSubmitting">
                <span v-if="isSubmitting" class="spinner-border spinner-border-sm"></span>
                <span>{{
                  isSubmitting
                    ? "處理交易中..."
                    : `確認付款 NT$ ${totalPrice.toLocaleString()}`
                }}</span>
              </button>
            </div>
          </form>
        </div>

        <!-- 右側：訂單明細 (50%) -->
        <div class="col-lg-6">
          <div class="sticky-top-card d-flex flex-column gap-4">
            <!-- 訂單倒數保留卡 (精簡美化版) -->
            <div
              class="card border-0 shadow-sm rounded-4 bg-dark text-white p-3 d-flex flex-row align-items-center justify-content-between countdown-card">
              <div class="d-flex align-items-center gap-3">
                <i class="bi bi-clock-history text-warning fs-3 animate-pulse"></i>
                <div>
                  <span class="d-block small text-white-50 fw-semibold">訂單座位保留中</span>
                  <span class="d-block small text-white-50 text-xs mt-0.5">請於時限內完成付款，逾時座位將自動釋出</span>
                </div>
              </div>
              <h3 class="fw-bold text-warning mb-0 font-monospace tracking-wider timer-display">
                {{ timerString }}
              </h3>
            </div>

            <!-- 訂單明細卡 -->
            <div class="card border-0 shadow-sm rounded-4 p-4">
              <h5 class="card-title fw-bold mb-3 pb-2 border-bottom text-dark">
                訂單明細
              </h5>

              <div class="event-summary mb-4">
                <span class="badge bg-primary-subtle text-primary mb-2 fw-semibold px-2.5 py-1">TAP 票務系統</span>
                <h6 class="fw-bold mb-1 text-dark">{{ eventDetails.title }}</h6>
                <div class="small text-secondary mb-1">
                  <i class="bi bi-calendar3 me-2"></i>{{ eventDetails.time }}
                </div>
                <div class="small text-secondary">
                  <i class="bi bi-geo-alt me-2"></i>{{ eventDetails.location }}
                </div>
              </div>

              <div class="ticket-seats-list mb-4">
                <div class="d-flex justify-content-between align-items-center small text-secondary fw-semibold mb-2">
                  <span>已選座位</span>
                  <span>單價</span>
                </div>
                <div class="d-flex flex-column gap-2">
                  <div v-for="(seat, index) in seats" :key="index"
                    class="d-flex justify-content-between align-items-center small text-dark p-2 bg-light rounded-3">
                    <span class="fw-semibold">{{ seat.zone_name ? seat.zone_name + ' ' : '' }}{{ seat.physical_row }} 排 - {{ seat.physical_seat }} 號</span>
                    <span class="font-monospace">NT$ {{ seat.price.toLocaleString() }}</span>
                  </div>
                </div>
              </div>

              <div class="price-breakdown d-flex flex-column gap-2 pt-3 border-top">
                <div class="d-flex justify-content-between align-items-center text-secondary small">
                  <span>票券小計 ({{ seats.length }} 張)</span>
                  <span class="font-monospace">NT$ {{ subtotalPrice.toLocaleString() }}</span>
                </div>
                <div class="d-flex justify-content-between align-items-center text-secondary small">
                  <span>系統服務費</span>
                  <span class="font-monospace">NT$ {{ serviceFee }}</span>
                </div>
                <hr class="my-2 border-light-subtle" />
                <div class="d-flex justify-content-between align-items-center">
                  <span class="fw-bold text-dark">應付總額</span>
                  <h3 class="fw-bold text-danger mb-0 font-monospace">
                    NT$ {{ totalPrice.toLocaleString() }}
                  </h3>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onUnmounted } from "vue";
import { useRouter, useRoute } from "vue-router";

import Swal from "sweetalert2";
import { useTicketCartStore } from "@/stores/ticketCartStore";
import axiosApi from "@/plugins/axios";
import { nextTick } from "vue";
import Swiper from "swiper";
import "swiper/css";

const router = useRouter();
const route = useRoute();
const cartStore = useTicketCartStore();

// ==========================================
// 1. 核心狀態 (Core States)
// ==========================================
const form = reactive({
  name: "",
  phone: "",
  email: "",
  identityNumber: "",
  agree: false,
});
const isAutoFilled = ref(false);
const isSubmitting = ref(false);
const validationErrors = reactive({});

// 動態接收場次資訊，若無則顯示預設模擬資料
const eventDetails = reactive({
  title:
    route.query.title ||
    history.state?.title ||
    "2026 TAP 狂歡音樂祭 - 搖滾之夜",
  time: route.query.time || history.state?.time || "2026-08-15 (六) 19:00 開演",
  location:
    route.query.location ||
    history.state?.location ||
    "台北流行音樂中心 表演廳",
});

// 直接綁定 Pinia Store 中的鎖定座位
const seats = computed(() => cartStore.tickets);

const serviceFee = ref(100);
const subtotalPrice = computed(() => cartStore.cartTotalPrice);
const totalPrice = computed(() => subtotalPrice.value + serviceFee.value);

// 倒數計時器設定 (15分鐘 = 900秒，基於鎖定時間戳記計算)
const timeLeftSeconds = ref(900);
let timerInterval = null;

const timerString = computed(() => {
  const mins = Math.floor(timeLeftSeconds.value / 60);
  const secs = timeLeftSeconds.value % 60;
  return `${mins.toString().padStart(2, "0")}:${secs.toString().padStart(2, "0")}`;
});

const handleTimeout = () => {
  clearInterval(timerInterval);
  cartStore.clearCart(); // 清空購物車與鎖定狀態
  Swal.fire({
    title: "訂單保留已過期",
    text: "很抱歉，您的訂單保留時間已過。請重新選擇座位購票。",
    icon: "warning",
    confirmButtonText: "確定",
    allowOutsideClick: false,
  }).then(() => router.push("/"));
};

const updateTimeLeft = () => {
  // 1. 如果購物車空了，直接清空計時器並結束
  if (!cartStore.tickets || cartStore.tickets.length === 0) {
    clearInterval(timerInterval);
    return;
  }

  // 2. 如果沒有時間戳記，顯示警告但不執行任何跳轉邏輯
  if (!cartStore.lockTimestamp) {
    console.warn("Timestamp 還沒準備好（Pinia hydrate delay）");
    return;
  }

  const now = Date.now();
  const elapsedSeconds = Math.floor((now - cartStore.lockTimestamp) / 1000);
  const remaining = 900 - elapsedSeconds;

  if (remaining <= 0) {
    timeLeftSeconds.value = 0;
    // 加入狀態鎖，如果正在結帳中，絕對不執行 handleTimeout
    if (!isSubmitting.value) {
      handleTimeout();
    }
  } else {
    timeLeftSeconds.value = remaining;
  }
};

const startTimer = () => {
  if (timerInterval) clearInterval(timerInterval);
  updateTimeLeft();
  timerInterval = setInterval(updateTimeLeft, 1000);
};

// 手機瀏覽器背景/鎖屏時 setInterval 會被節流或暫停，回到前景時立即重新校正一次，
// 避免畫面卡在舊數字、看起來像「沒有在倒數」
const handleVisibilityResync = () => {
  if (document.visibilityState === "visible") {
    updateTimeLeft();
  }
};

// 帶入會員真實資料 (GET /api/user/profile)
const toggleAutoFill = async () => {
  if (isAutoFilled.value) {
    try {
      const { data: profile } = await axiosApi.get("/api/user/profile");
      Object.assign(form, {
        name: profile.name || "",
        phone: profile.phone || "",
        email: profile.email || "",
        // 後端 UserProfileResponse 目前沒有身分證字號欄位，保留原值讓使用者自行輸入
      });
    } catch (err) {
      console.error("帶入會員資料失敗", err);
      isAutoFilled.value = false;
      Swal.fire({
        toast: true,
        position: "top-end",
        showConfirmButton: false,
        timer: 2500,
        icon: "warning",
        title: "無法帶入會員資料",
      });
      return;
    }
  } else {
    Object.assign(form, { name: "", phone: "", email: "", identityNumber: "" });
  }
  Object.keys(validationErrors).forEach((key) => (validationErrors[key] = ""));
};

// 表單驗證
const validateForm = () => {
  Object.keys(validationErrors).forEach((k) => delete validationErrors[k]);
  if (!form.name.trim()) validationErrors.name = "姓名為必填欄位";
  if (!form.phone.trim()) {
    validationErrors.phone = "手機號碼為必填欄位";
  } else if (!/^09\d{8}$/.test(form.phone.trim())) {
    validationErrors.phone = "格式應為 09 開頭的 10 位數字";
  }
  if (!form.email.trim()) {
    validationErrors.email = "電子信箱為必填欄位";
  } else if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(form.email.trim())) {
    validationErrors.email = "信箱格式有誤";
  }
  if (!form.identityNumber.trim()) {
    validationErrors.identityNumber = "身分證字號 / 護照號碼為必填欄位";
  } else if (!/^[A-Z0-9]{8,15}$/i.test(form.identityNumber.trim())) {
    validationErrors.identityNumber = "身分證字號或護照格式不正確";
  }
  if (!form.agree) validationErrors.agree = "您必須同意條款才能進行結帳";
  return Object.keys(validationErrors).length === 0;
};

// 後端鎖定狀態前置驗證
// const validateLockedSeats = async () => {
//   if (cartStore.isClientSideExpired || cartStore.ticketIds.length === 0) {
//     handleTimeout();
//     return false;
//   }
// console.log(cartStore.currentSessionId);
// console.log(cartStore.ticketIds);
// try {
//   const response = await axiosApi.post("/api/ticket/validate", {
//     sessionId: parseInt(cartStore.currentSessionId),
//     ticketIds: cartStore.ticketIds,
//   });
//   // 🌟 修正點：使用 Axios 的標準寫法，讀取 response.data.success
//   if (!response.data || !response.data.success) {
//     handleTimeout();
//     return false;
//   }
//   return true;
// } catch (err) {
//   // console.error("結帳前置驗證失敗", err);
//   // 連線失敗或異常亦先退回，防止幽靈下單
//   handleTimeout();
//   return false;
// }
// };

// ==========================================
// 2. 隨機 Token 產生與後端傳送邏輯 (防重複下單)
// ==========================================
const generateIdempotencyToken = () => {
  return crypto.randomUUID();
};

const submitCheckout = async () => {
  if (isSubmitting.value) return;

  // 1. 基本驗證
  if (!validateForm()) return;
  isSubmitting.value = true;

  try {
    const requestBody = {
      submitToken: crypto.randomUUID(),
      contactName: form.name,
      contactEmail: form.email,
      contactPhone: form.phone,
      orderTickets: cartStore.tickets.map((seat) => ({
        ticketId: seat.ticket_id || seat.ticketId || 1,
        realName: form.name,
        identityNumber: form.identityNumber,
      })),
    };

    // 2. 發送請求給後端
    const response = await axiosApi.post("/api/checkout/ticket", requestBody);

    console.log("========== response ==========");
    console.log(JSON.stringify(response.data, null, 2));
    console.log("==============================");

    const result = response.data;

    console.log("success =", result.success);
    console.log("data =", result.data);
    console.log("ecpayForm =", result.ecpayForm);
    console.log("result =", result);

    // 如果沒有 success 欄位
    if (!result) {
      throw new Error("後端沒有回傳資料");
    }

    // 後端可能回 HTTP 200 但 success:false（例如「座位未鎖定」等真正失敗原因），
    // 若不先判斷 success，會被下面的「找不到 ecpayForm」通用錯誤蓋掉，使用者看不到真因
    if (result.success === false) {
      console.log(result);
      throw new Error(result.message || "結帳失敗，請重新嘗試");
    }

    const ecpayForm = result.data?.ecpayForm ?? result.ecpayForm ?? result.data;

    if (!ecpayForm) {
      console.log(result);
      throw new Error("後端沒有回傳綠界 HTML");
    }

    clearInterval(timerInterval);

    const div = document.createElement("div");
    div.innerHTML = ecpayForm;
    document.body.appendChild(div);

    const formElement = div.querySelector("form");

    if (!formElement) {
      throw new Error("HTML 裡沒有 form");
    }

    formElement.submit();
    return;
  } catch (error) {
    console.error(error);
    console.error(error.response);
    console.error(error.response?.data);
    console.error("結帳錯誤:", error);

    Swal.fire({
      title: "結帳失敗",
      text: error.response?.data?.message || error.message,
      icon: "error",
    });
    isSubmitting.value = false;
  }
};

onMounted(async () => {
  console.log("checkout mounted");
  console.log("🔥 checkout page 已載入");
  console.log("tickets =", cartStore.tickets);

  if (!cartStore.tickets?.length) {
    // 購物車為空時不能導向 BookTicket：該路由需要 themeId/sessionId 參數，
    // 這裡沒有這些參數會丟出 "Missing required param themeId" 並使頁面卡住。
    // 改導向不需參數的活動列表，讓使用者重新選購。
    router.replace({ name: "ThemeList" });
    return;
  }

  // 【徹底清除殘留】避免上一個週期留下的計時器導致彈窗
  if (timerInterval) clearInterval(timerInterval);

  console.log("偵錯資訊 - 購物車長度:", cartStore.tickets?.length);

  console.log(
    "tickets detail =",
    JSON.stringify(cartStore.tickets, null, 2)
  );

  // 狀態判斷：如果購物車為空
  if (!cartStore.tickets || cartStore.tickets.length === 0) {
    console.log(
      "購物車為空，這是正常的（可能是已結帳回退），停止所有計時邏輯。",
    );
    return; // 這裡不執行任何 router.push 或 handleTimeout
  }

  await nextTick(); // 🔥 等 pinia hydrate

  if (cartStore.tickets.length > 0 && cartStore.lockTimestamp) {
    startTimer();
  }

  document.addEventListener("visibilitychange", handleVisibilityResync);
  window.addEventListener("pageshow", handleVisibilityResync);

  // 檢查 URL 是否有付款失敗或取消的狀態
  if (route.query.status === "canceled") {
    cartStore.clearCart();
    Swal.fire({
      title: "付款已取消",
      text: "您已取消付款，訂單已釋放，請重新選購座位。",
      icon: "info",
    }).then(() => {
      router.push("/");
    });
  } else if (route.query.status === "failed") {
    cartStore.clearCart();
    Swal.fire({
      title: "付款失敗",
      text: "交易未成功，訂單已失效，請重新選購座位。",
      icon: "error",
    }).then(() => {
      router.push("/");
    });
  }

  if (cartStore.tickets.length === 0 && !route.query.status) {
    // 使用者是透過非正常流程回到此頁面
    console.log("偵測到異常回退，導向首頁...");
    router.push("/");
  }
});

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval);
    timerInterval = null; // 顯式清除
  }
  document.removeEventListener("visibilitychange", handleVisibilityResync);
  window.removeEventListener("pageshow", handleVisibilityResync);
});
</script>

<style scoped>
.checkout-page {
  font-family:
    "Inter",
    -apple-system,
    sans-serif;
  color: #334155;
}

.cursor-pointer {
  cursor: pointer;
}

.break-all {
  word-break: break-all;
}

.sticky-top-card {
  position: sticky;
  top: 24px;
}

.countdown-card {
  background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%) !important;
}

.timer-display {
  font-size: 1.75rem;
}

.text-xs {
  font-size: 0.75rem;
}

.animate-pulse {
  animation: pulse 2s cubic-bezier(0.4, 0, 0.6, 1) infinite;
}

.animate-transition {
  transition: all 0.3s ease;
}

.animate-transition:focus {
  border-color: #3b82f6;
  box-shadow: 0 0 0 4px rgba(59, 130, 246, 0.15);
}

.btn-submit-hover {
  transition: all 0.3s ease;
}

.btn-submit-hover:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 20px rgba(59, 130, 246, 0.3) !important;
}

@keyframes pulse {

  0%,
  100% {
    opacity: 1;
  }

  50% {
    opacity: 0.6;
    transform: scale(1.05);
  }
}

@media (max-width: 991.98px) {
  .sticky-top-card {
    position: static;
  }
}
</style>
