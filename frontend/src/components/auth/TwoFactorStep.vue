<script setup>
import { ref, onMounted, onUnmounted } from 'vue';

const props = defineProps({
    email: {
        type: String,
        required: true
    },
    errorMsg: {
        type: String,
        default: ''
    }
});

const emit = defineEmits(['back', 'submit', 'resend', 'clear-error']);
const code = ref('');

// 重新發送倒數（秒）
const countdown = ref(60);
let timer = null;

const startCountdown = (seconds = 60) => {
    if (timer) clearInterval(timer);
    countdown.value = seconds;
    timer = setInterval(() => {
        if (countdown.value > 0) {
            countdown.value--;
        } else {
            clearInterval(timer);
            timer = null;
        }
    }, 1000);
};

const handleSubmit = () => {
    if (code.value.length !== 6) return;
    emit('submit', { code: code.value });
};

const handleResend = () => {
    if (countdown.value > 0) return;
    emit('resend');
    startCountdown(60);
};

onMounted(() => startCountdown(60));
onUnmounted(() => { if (timer) clearInterval(timer); });
</script>

<template>
    <div class="w-100 animation-fade-in">
        <form @submit.prevent="handleSubmit">
            <!-- 提示文案 -->
            <div class="mb-3 text-center privacy-text" style="font-size: 0.85rem;">
                <span class="d-block mb-1">
                    <i class="bi bi-shield-lock-fill text-primary"></i> 此帳號已啟用登入雙重驗證
                </span>
                <span class="d-block">我們已將 6 位數驗證碼寄至</span>
                <span class="d-block fw-bold text-dark">{{ email }}</span>
            </div>

            <!-- 驗證碼輸入框 -->
            <div class="mb-2 text-start">
                <div class="form-floating position-relative">
                    <input
                        type="text"
                        class="form-control login-input focus-ring"
                        :class="{ 'is-invalid': errorMsg }"
                        id="twoFactorCode"
                        placeholder=""
                        v-model="code"
                        maxlength="6"
                        required
                        autofocus
                        @input="emit('clear-error')"
                    />
                    <label for="twoFactorCode">輸入驗證碼</label>

                    <div class="position-absolute top-50 translate-middle-y end-0 pe-3" style="z-index: 10;">
                        <span v-if="errorMsg" class="text-danger small" style="font-size: 13px !important; color: #ef4444 !important; font-weight: 400; letter-spacing: 0.5px;">
                            {{ errorMsg }}
                        </span>
                    </div>
                </div>
            </div>

            <!-- 登入按鈕 -->
            <button
                type="submit"
                class="btn btn-primary login-btn email-btn w-100 mb-2"
                :disabled="code.length !== 6"
            >
                驗證並登入
            </button>

            <!-- 重新發送與切換 Email 步驟 -->
            <div class="text-center privacy-text mb-0" style="font-size: 0.85rem;">
                沒收到驗證信嗎？
                <span v-if="countdown > 0" class="text-muted">重新發送 ({{ countdown }} 秒)</span>
                <a v-else href="#" class="privacy-link" @click.prevent="handleResend">重新發送</a>
            </div>
            <div class="text-center privacy-text mb-0" style="font-size: 0.85rem;">
                <a href="#" class="privacy-link" @click.prevent="emit('back')">返回重新登入</a>
            </div>
        </form>
    </div>
</template>

<style scoped>
.login-input {
    padding-right: 45px !important;
    transition: padding-right 0.2s ease;
}
.login-input.is-invalid {
    padding-right: 140px !important;
    background-image: none !important;
    border-color: #fca5a5 !important;
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12) !important;
}
</style>
