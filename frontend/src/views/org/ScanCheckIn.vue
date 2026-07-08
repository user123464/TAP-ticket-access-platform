<template>
  <div class="container-xl py-4">
    <header class="mb-4">
      <h3 class="fw-bold mb-1"><i class="bi bi-qr-code-scan me-2 text-primary"></i>現場掃碼驗票</h3>
      <p class="text-muted mb-0 small">以手機相機掃描票券 QR Code 即時核銷；相機不可用時可改手動輸入 HASH。</p>
    </header>

    <section class="card border-0 shadow-sm rounded-4 p-4 mb-4" style="max-width: 480px;">
      <!-- 開啟/關閉相機按鈕 -->
      <button
        type="button"
        class="btn btn-primary rounded-3 fw-bold py-2 px-4 shadow-sm camera-toggle-btn"
        @click="toggleCamera"
      >
        <i class="bi me-2" :class="isCameraOpen ? 'bi-camera-video-off' : 'bi-camera-video'"></i>
        {{ isCameraOpen ? '關閉相機' : '開啟相機掃描 QR' }}
      </button>

      <!-- 相機預覽區域（僅在開啟時顯示） -->
      <div v-if="isCameraOpen" class="mt-3 qr-scanner-wrapper">
        <div class="qr-scanner-container rounded-4 overflow-hidden shadow-sm border position-relative">
          <div id="qr-reader"></div>
          <div class="qr-hint-overlay">
            <span class="qr-hint-text">將 QR Code 對準框內</span>
          </div>
        </div>
        <div v-if="cameraError" class="alert alert-warning mt-2 small mb-0 rounded-3">
          <i class="bi bi-exclamation-triangle me-1"></i>{{ cameraError }}
        </div>
      </div>

      <!-- 手動輸入備援 -->
      <hr class="my-4" />
      <label class="form-label small text-muted">手動輸入備援</label>
      <form @submit.prevent="handleManualSubmit" class="d-flex gap-2">
        <div class="input-group">
          <span class="input-group-text bg-light border-end-0"><i class="bi bi-hash"></i></span>
          <input
            v-model="qrInput"
            type="text"
            class="form-control bg-light border-start-0"
            placeholder="輸入票券 QR Code HASH 代碼..."
          />
        </div>
        <button type="submit" class="btn btn-outline-primary rounded-3 fw-bold px-3 text-nowrap">
          <i class="bi bi-check2-circle me-1"></i>核銷
        </button>
      </form>

      <!-- 最近結果 -->
      <div
        v-if="lastResult"
        class="alert mt-3 mb-0 small rounded-3"
        :class="lastResult.ok ? 'alert-success' : 'alert-danger'"
      >
        <i class="bi me-1" :class="lastResult.ok ? 'bi-check-circle' : 'bi-x-circle'"></i>{{ lastResult.text }}
      </div>
    </section>
  </div>
</template>

<script setup>
/**
 * ScanCheckIn.vue — [Jason] 現場掃碼驗票（獨立掃碼頁）
 *
 * 設計：
 * - 這頁專供「現場掃 QR 核銷」用（相機 + 後鏡頭 + /api/checkin），與組員的
 *   /checkin「票務核銷與售後管理」頁（手打 hash／清單／售後）職責分離。
 * - 相機邏輯（html5-qrcode 動態 import、解碼成功自動核銷、卸載時停相機）原本被
 *   塞進組員的 OrderTicketCheckIn.vue，本輪搬遷到此 Jason 領土頁，還原組員檔。
 * - 通知一律走 toast（與其他頁一致），不使用原生 alert。
 * - 提供手動輸入備援：相機不可用時仍能手打 hash 核銷。
 */
import { ref, onBeforeUnmount } from 'vue';
import { useToast } from "@/composables/useToast.js";
import axios from '@/plugins/axios';

const toast = useToast();

const qrInput = ref('');        // 手動輸入備援的 hash
const isCameraOpen = ref(false); // 相機是否已開啟
const cameraError = ref('');     // 相機錯誤訊息（顯示於 UI，禁用原生 alert）
const lastResult = ref(null);    // 最近一次核銷結果 { ok, text }
let html5QrScanner = null;       // html5-qrcode 實例，元件卸載時需停止

// 元件卸載前停止相機，避免記憶體洩漏
onBeforeUnmount(async () => {
  await stopScanner();
});

async function stopScanner() {
  if (html5QrScanner) {
    try {
      await html5QrScanner.stop();
    } catch (_) {
      // 忽略停止時的錯誤（例如相機未啟動）
    }
    html5QrScanner = null;
  }
}

// 核銷單張票（掃碼成功或手動送出皆走此）
async function checkIn(hash) {
  const code = (hash || '').trim();
  if (!code) return;
  try {
    const res = await axios.post(`/api/checkin?qrCodeHash=${code}`);
    lastResult.value = { ok: true, text: res.data?.message || `核銷成功：${code}` };
    toast.success(res.data?.message || '核銷成功');
    qrInput.value = '';
  } catch (err) {
    lastResult.value = { ok: false, text: err.response?.data?.message || '核銷失敗' };
    toast.error(err.response?.data?.message || '核銷失敗');
  }
}

// 手動輸入送出
const handleManualSubmit = () => checkIn(qrInput.value);

// 開啟/關閉手機相機掃 QR Code
// 解碼成功後自動呼叫 checkIn，不重寫驗證邏輯
const toggleCamera = async () => {
  cameraError.value = '';

  if (isCameraOpen.value) {
    // --- 關閉相機 ---
    await stopScanner();
    isCameraOpen.value = false;
    return;
  }

  // --- 開啟相機 ---
  // 先確認瀏覽器支援 getUserMedia（不支援則顯示 cameraError，勿用原生 alert）
  if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
    cameraError.value = '您的瀏覽器不支援相機功能，請改用 Chrome/Safari 手機版或改手動輸入。';
    isCameraOpen.value = true; // 顯示錯誤提示區塊
    return;
  }

  isCameraOpen.value = true;

  // 等 v-if DOM 渲染後再掛載 html5-qrcode
  await new Promise(resolve => setTimeout(resolve, 80));

  try {
    // 動態 import，避免 SSR 或 Tree-shaking 問題
    const { Html5Qrcode } = await import('html5-qrcode');

    html5QrScanner = new Html5Qrcode('qr-reader');

    await html5QrScanner.start(
      { facingMode: 'environment' }, // 優先使用後鏡頭（掃票情境）
      {
        fps: 10,      // 每秒掃描幀數，10 fps 省電且夠用
        qrbox: { width: 240, height: 240 },
        aspectRatio: 1.0
      },
      // 解碼成功回呼：核銷後自動關閉相機，避免重複掃同一張票
      (decodedText) => {
        checkIn(decodedText);
        toggleCamera();
      },
      // 解碼失敗回呼（每幀都會觸發，靜默不打擾）
      (_errorMessage) => {}
    );
  } catch (err) {
    // 相機權限被拒或其他錯誤：顯示於 cameraError，勿用原生 alert
    cameraError.value =
      err?.message?.includes('Permission')
        ? '相機權限遭拒，請在瀏覽器允許此頁面使用相機後重試。'
        : `相機啟動失敗：${err?.message || '未知錯誤'}，請改手動輸入。`;
    isCameraOpen.value = true; // 保持顯示錯誤提示
    html5QrScanner = null;
  }
};
</script>

<style scoped>
.camera-toggle-btn {
  width: 100%;
}
@media (min-width: 768px) {
  .camera-toggle-btn {
    width: auto;
  }
}

.qr-scanner-wrapper {
  max-width: 400px;
}
@media (max-width: 576px) {
  .qr-scanner-wrapper {
    max-width: 100%;
  }
}

/* html5-qrcode 內建 UI 微調：讓掃描框填滿容器 */
.qr-scanner-container #qr-reader {
  width: 100% !important;
  border: none !important;
}
.qr-scanner-container #qr-reader video {
  width: 100% !important;
  border-radius: 0.75rem;
}
/* 隱藏 html5-qrcode 預設的檔案上傳 UI，手機只需相機 */
.qr-scanner-container #qr-reader__dashboard_section_csr span,
.qr-scanner-container #qr-reader__filescan_input,
.qr-scanner-container #qr-reader__dashboard_section_swaplink {
  display: none !important;
}

/* 掃描提示文字浮層 */
.qr-hint-overlay {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  background: linear-gradient(transparent, rgba(0, 0, 0, 0.55));
  padding: 0.5rem 0.75rem;
  border-radius: 0 0 0.75rem 0.75rem;
  pointer-events: none;
}
.qr-hint-text {
  color: #fff;
  font-size: 0.8rem;
  font-weight: 500;
  letter-spacing: 0.02em;
}
</style>
