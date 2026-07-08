<template>
  <div class="checkout-page bg-light min-vh-100 py-5">
    <div class="container">
      <div class="row g-4">
        <!-- 左側：收件資訊與確認 (50%) -->
        <div class="col-lg-6">
          <form
            @submit.prevent="submitCheckout"
            class="d-flex flex-column gap-4"
            novalidate
          >
            <!-- 收件人與聯絡資訊 -->
            <div class="card border-0 shadow-sm rounded-4 p-4">
              <div
                class="d-flex justify-content-between align-items-center mb-4"
              >
                <h5 class="card-title fw-bold mb-0 text-dark">
                  <i class="bi bi-person-fill text-primary me-2"></i>收件人資訊
                </h5>
                <div class="form-check form-switch mb-0">
                  <input
                    class="form-check-input cursor-pointer"
                    type="checkbox"
                    id="autoFillCheck"
                    v-model="isAutoFilled"
                    @change="toggleAutoFill"
                  />
                  <label
                    class="form-check-label small text-muted cursor-pointer"
                    for="autoFillCheck"
                    >帶入會員資料</label
                  >
                </div>
              </div>

              <div class="row g-3">
                <div class="col-md-6">
                  <label class="form-label small fw-bold text-secondary"
                    >收件人姓名</label
                  >
                  <input
                    type="text"
                    class="form-control animate-transition"
                    placeholder="請輸入真實姓名"
                    v-model="form.name"
                    required
                  />
                  <div
                    v-if="validationErrors.name"
                    class="text-danger small mt-1"
                  >
                    {{ validationErrors.name }}
                  </div>
                </div>
                <div class="col-md-6">
                  <label class="form-label small fw-bold text-secondary"
                    >收件人電話</label
                  >
                  <input
                    type="tel"
                    class="form-control animate-transition"
                    placeholder="例：0912345678"
                    v-model="form.phone"
                    required
                  />
                  <div
                    v-if="validationErrors.phone"
                    class="text-danger small mt-1"
                  >
                    {{ validationErrors.phone }}
                  </div>
                </div>
                <div class="col-12">
                  <label class="form-label small fw-bold text-secondary" for="recipientAddressInput"
                    >收件人地址</label
                  >
                  <input
                    id="recipientAddressInput"
                    type="text"
                    class="form-control animate-transition"
                    placeholder="請輸入收件人地址"
                    v-model="form.address"
                    required
                  />
                  <div
                    v-if="validationErrors.address"
                    class="text-danger small mt-1"
                  >
                    {{ validationErrors.address }}
                  </div>
                </div>
                <div class="col-12">
                  <label class="form-label small fw-bold text-secondary"
                    >電子信箱 (寄送發票與出貨通知)</label
                  >
                  <input
                    type="email"
                    class="form-control animate-transition"
                    placeholder="example@mail.com"
                    v-model="form.email"
                    required
                  />
                  <div
                    v-if="validationErrors.email"
                    class="text-danger small mt-1"
                  >
                    {{ validationErrors.email }}
                  </div>
                </div>
                <div class="col-12">
                  <label class="form-label small fw-bold text-secondary"
                    >身分證字號 / 護照號碼 (實名制驗證)</label
                  >
                  <input
                    type="text"
                    class="form-control animate-transition"
                    placeholder="請輸入身分證字號或護照號碼"
                    v-model="form.identityNumber"
                    required
                  />
                  <div
                    v-if="validationErrors.identityNumber"
                    class="text-danger small mt-1"
                  >
                    {{ validationErrors.identityNumber }}
                  </div>
                </div>
              </div>
            </div>

            <!-- 同意條款與確認按鈕 -->
            <div class="card border-0 shadow-sm rounded-4 p-4">
              <div class="mb-3">
                <div class="form-check">
                  <input
                    class="form-check-input cursor-pointer"
                    type="checkbox"
                    id="agreeTerms"
                    v-model="form.agree"
                  />
                  <label
                    class="form-check-label small text-muted cursor-pointer"
                    for="agreeTerms"
                  >
                    我已閱讀並同意
                    <a
                      href="#"
                      @click.prevent
                      class="text-primary text-decoration-none"
                      >服務條款</a
                    >
                    與
                    <a
                      href="#"
                      @click.prevent
                      class="text-primary text-decoration-none"
                      >購物退換貨規定</a
                    >。
                  </label>
                </div>
                <div
                  v-if="validationErrors.agree"
                  class="text-danger small mt-1"
                >
                  {{ validationErrors.agree }}
                </div>
              </div>

              <button
                class="btn btn-primary w-100 py-3 rounded-3 fw-bold fs-5 shadow-sm d-flex align-items-center justify-content-center gap-2 btn-submit-hover"
                type="submit"
                :disabled="isSubmitting"
              >
                <span
                  v-if="isSubmitting"
                  class="spinner-border spinner-border-sm"
                ></span>
                <span>{{
                  isSubmitting
                    ? "處理訂單中..."
                    : `確認付款 NT$ ${totalPrice.toLocaleString()}`
                }}</span>
              </button>
            </div>
          </form>
        </div>

        <!-- 右側：訂單明細 (50%) -->
        <div class="col-lg-6">
          <div class="sticky-top-card d-flex flex-column gap-4">
            <!-- 訂單明細卡 -->
            <div class="card border-0 shadow-sm rounded-4 p-4">
              <h5 class="card-title fw-bold mb-3 pb-2 border-bottom text-dark">
                訂單明細
              </h5>

              <div class="event-summary mb-4">
                <span
                  class="badge bg-primary-subtle text-primary mb-2 fw-semibold px-2.5 py-1"
                  >TAP 官方商城</span
                >
                <h6 class="fw-bold mb-1 text-dark">{{ eventDetails.title }}</h6>
                <div class="small text-secondary">
                  <i class="bi bi-truck me-2"></i>{{ eventDetails.location }}
                </div>
              </div>

              <div class="ticket-seats-list mb-4">
                <div
                  class="d-flex justify-content-between align-items-center small text-secondary fw-semibold mb-2"
                >
                  <span>商品明細</span>
                  <span>單價 / 數量</span>
                </div>
                <div class="d-flex flex-column gap-2">
                  <div
                    v-for="(item, index) in items"
                    :key="index"
                    class="d-flex justify-content-between align-items-center small text-dark p-2 bg-light rounded-3"
                  >
                    <span class="fw-semibold">{{ item.name }}</span>
                    <div class="d-flex align-items-center gap-3">
                      <span class="text-secondary small"
                        >x{{ item.qty || 1 }}</span
                      >
                      <span class="font-monospace"
                        >NT$
                        {{
                          (item.price * (item.qty || 1)).toLocaleString()
                        }}</span
                      >
                    </div>
                  </div>
                </div>
              </div>

              <div
                class="price-breakdown d-flex flex-column gap-2 pt-3 border-top"
              >
                <div
                  class="d-flex justify-content-between align-items-center text-secondary small"
                >
                  <span>商品小計 (共 {{ totalQuantity }} 件)</span>
                  <span class="font-monospace"
                    >NT$ {{ subtotalPrice.toLocaleString() }}</span
                  >
                </div>
                <div
                  class="d-flex justify-content-between align-items-center text-secondary small"
                >
                  <span>運費</span>
                  <span class="font-monospace">NT$ {{ shippingFee }}</span>
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
import { ref, reactive, computed, onMounted } from "vue";
import { useRouter, useRoute } from "vue-router";
import Swal from "sweetalert2";
import axiosApi from "@/plugins/axios";
import { useCartStore } from "@/stores/cart";

const router = useRouter();
const route = useRoute();
const cartStore = useCartStore();

// ==========================================
// 1. 核心狀態與模擬資料 (Core States & Mock Data)
// ==========================================
const form = reactive({
  name: "",
  phone: "",
  email: "",
  address: "",
  identityNumber: "",
  agree: false,
});
const isAutoFilled = ref(false);
const isSubmitting = ref(false);
const validationErrors = reactive({});

// 動態接收場次/訂單資訊，若無則顯示預設模擬資料
const eventDetails = reactive({
  title: route.query.title || history.state?.title || "2026 TAP 狂歡限定周邊",
  location:
    route.query.location ||
    history.state?.location ||
    "出貨方式: 超商取貨 / 宅配",
});

const shippingFee = ref(history.state?.items ? 0 : 80);

// 動態接收商品資訊，若無則顯示預設模擬商品
const items = ref(history.state?.items || []);

if (items.value.length === 0) {
  if (cartStore.cartItems.length > 0) {
    items.value = cartStore.cartItems.map((item) => ({
      productId: item.productId,
      variantId: item.variantId,
      name: `${item.productName} (${item.productColor} - ${item.productSize})`,
      price: item.unitPrice,
      qty: item.quantity,
    }));
    shippingFee.value = 0;
  } else {
    items.value = [
      { name: "2026 TAP 紀念連帽 T-shirt (曜石黑 - L)", price: 1580, qty: 1 },
      { name: "2026 TAP 狂歡限定運動毛巾 (搖滾黃)", price: 550, qty: 2 },
    ];
    shippingFee.value = 80;
  }
}

const subtotalPrice = computed(() =>
  items.value.reduce((sum, item) => sum + item.price * (item.qty || 1), 0),
);
const totalQuantity = computed(() =>
  items.value.reduce((sum, item) => sum + (item.qty || 1), 0),
);
const totalPrice = computed(() => subtotalPrice.value + shippingFee.value);



// 帶入會員真實資料 (GET /api/user/profile)
const toggleAutoFill = async () => {
  if (isAutoFilled.value) {
    try {
      const { data: profile } = await axiosApi.get("/api/user/profile");
      Object.assign(form, {
        name: profile.name || "",
        phone: profile.phone || "",
        email: profile.email || "",
        address: profile.address || "",
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
    Object.assign(form, { name: "", phone: "", email: "", address: "", identityNumber: "" });
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
  if (!form.address.trim()) {
    validationErrors.address = "收件人地址為必填欄位";
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

// ==========================================
// 2. 隨機 Token 產生與後端傳送邏輯 (防重複下單)
// ==========================================
const generateIdempotencyToken = () => {
  // 非 https (例如區網 IP demo) 環境下 crypto.randomUUID 可能不存在，改用 fallback 產生亂數字串，
  // 避免在 try 區塊外拋出 TypeError 導致 finally 沒被執行、按鈕卡在讀取中狀態
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return Date.now().toString(36) + Math.random().toString(36).slice(2);
};

const submitCheckout = async () => {
  if (isSubmitting.value) return;
  if (!validateForm()) {
    Swal.fire({
      title: "欄位未完整",
      text: "請檢查紅字標示的欄位。",
      icon: "error",
    });
    return;
  }

  isSubmitting.value = true;

  try {
    const idempotencyToken = generateIdempotencyToken();
    console.log("Generated Idempotency Token:", idempotencyToken);

    let mockApiResponse = null;
    let isSuccess = false;
    try {
      // 轉換購物車項目格式以符合後端 OrderCreateRequestDTO
      const orderMerches = items.value.map((item) => ({
        productId: item.productId || item.product_id || 1, // 預設降級商品ID為1 (模擬沙盒)
        variantId: item.variantId || item.variant_id || 1, // 預設降級款式ID為1
        quantity: item.qty || item.quantity || 1,
      }));
      console.log(orderMerches);

      const response = await axiosApi.post("/api/checkout/merch", {
        submitToken: idempotencyToken,
        orderMerches: orderMerches,
        recipientName: form.name,
        recipientPhone: form.phone,
        recipientEmail: form.email,
        recipientAddress: form.address,
        identityNumber: form.identityNumber,
      });

      if (response.data) {
        mockApiResponse = response.data;
        isSuccess = true;
      } else {
        throw new Error("後端處理失敗");
      }
    } catch (err) {
      console.error("連線後端或處理失敗", err);
      Swal.fire({
        title: "結帳失敗",
        text:
          err.response?.data?.message ||
          "無法連線至伺服器或後端處理失敗，請稍後再試。",
        icon: "error",
      });
      isSuccess = false;
    }

    await new Promise((resolve) => setTimeout(resolve, 1500));

    if (isSuccess) {
      const mOrderId =
        mockApiResponse?.data?.mOrderId ??
        mockApiResponse?.data?.m_order_id ??
        mockApiResponse?.mOrderId ??
        mockApiResponse?.m_order_id ??
        "";

      const orderData = {
        mOrderId: mOrderId,
        amount: totalPrice.value,
        email: form.email,
        name: form.name,
        phone: form.phone,
        address: form.address,
        identityNumber: form.identityNumber,
        items: items.value,
        type: "merch",
        paymentMethod: "綠界科技 (ECPay)",
        time: new Date().toLocaleString(),
      };
      sessionStorage.setItem(
        "payment_success_order",
        JSON.stringify(orderData),
      );

      // 檢查是否有綠界科技的 HTML 表單，若有則動態寫入並自動提交以跳轉
      const ecpayForm =
        mockApiResponse?.data?.ecpayForm ??
        mockApiResponse?.ecpayForm ??
        mockApiResponse;

      if (ecpayForm && typeof ecpayForm === "string" && ecpayForm.includes("<form")) {
        const div = document.createElement("div");
        div.innerHTML = ecpayForm;
        document.body.appendChild(div);
        const formElement = div.querySelector("form");
        if (formElement) {
          formElement.submit();
          return;
        }
      }

      router.push({
        name: "PaymentSuccess",
        state: orderData,
      });
    }
  } catch (error) {
    console.error(error);
    Swal.fire({
      title: "結帳失敗",
      text: "系統異常，請稍後再試。",
      icon: "error",
    });
  } finally {
    isSubmitting.value = false;
  }
};

onMounted(async () => {
  await cartStore.fetchRemoteCart();
  if (
    items.value.length === 0 ||
    (history.state?.items == null && cartStore.cartItems.length > 0)
  ) {
    items.value = cartStore.cartItems.map((item) => ({
      productId: item.productId,
      variantId: item.variantId,
      name: `${item.productName} (${item.productColor} - ${item.productSize})`,
      price: item.unitPrice,
      qty: item.quantity,
    }));
    shippingFee.value = 0;
  }
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
