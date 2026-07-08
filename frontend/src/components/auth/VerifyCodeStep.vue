<script setup>
import { ref, onMounted, onUnmounted } from 'vue';
import TurnstileWidget from '@/components/common/TurnstileWidget.vue';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast';

const toast = useToast();

const props = defineProps({
    email: {
        type: String,
        required: true
    }
});

const emit = defineEmits(['back', 'submit']);
const verifyCode = ref('');
const turnstileToken = ref('');
const errorMsg = ref('');

// 倒數計時狀態（秒）與計時器變數
const countdown = ref(60);
let timer = null;

const handleVerify = (token) => {
    turnstileToken.value = token;
};

const handleExpireOrError = () => {
    turnstileToken.value = '';
};

// 啟動倒數計時
const startCountdown = (seconds = 60) => {
    if (timer) clearInterval(timer);
    countdown.value = seconds;
    timer = setInterval(() => {
        if (countdown.value > 0) {
            countdown.value--;
        } else {
            clearInterval(timer);
            timer = null;
            localStorage.removeItem('resend_cooldown_timestamp');
        }
    }, 1000);
};

// 寫入時間戳記並啟動 60 秒倒數
const triggerCountdown = () => {
    localStorage.setItem('resend_cooldown_timestamp', Date.now().toString());
    startCountdown(60);
};

// 呼叫驗證碼驗證 API
const handleSubmit = async () => {
    errorMsg.value = '';
    console.log('Verify attempt:', props.email, verifyCode.value, 'Turnstile Token:', turnstileToken.value);
    
    try {
        await axios.post('/api/auth/verify-code', {
            email: props.email,
            code: verifyCode.value
        });

        localStorage.removeItem('resend_cooldown_timestamp'); // 驗證成功後清除冷卻時間
        emit('submit', { email: props.email, code: verifyCode.value });
    } catch (err) {
        console.error('Verify code failed:', err);
        errorMsg.value = err.response?.data?.message || '驗證碼錯誤或已過期';
    }
};

// 呼叫重新發送驗證信 API
const handleResend = async () => {
    if (countdown.value > 0) return;
    errorMsg.value = '';

    try {
        await axios.post(`/api/auth/send-code?email=${props.email}`);
        toast.success('驗證信已重新發送');
        triggerCountdown();
    } catch (err) {
        console.error('Resend code failed:', err);
        errorMsg.value = err.response?.data?.message || '發送失敗，請稍後再試';
    }
};

onMounted(() => {
    const cooldownTime = localStorage.getItem('resend_cooldown_timestamp');
    if (cooldownTime) {
        const timePassed = Math.floor((Date.now() - parseInt(cooldownTime, 10)) / 1000);
        if (timePassed < 60) {
            startCountdown(60 - timePassed);
        } else {
            localStorage.removeItem('resend_cooldown_timestamp');
            countdown.value = 0;
        }
    } else {
        // 首次進入此步驟，啟動 60 秒倒數 (後端會由上一個步驟的 check-email 觸發寄信，所以這裡直接開始計時)
        triggerCountdown();
    }
});

onUnmounted(() => {
    if (timer) clearInterval(timer);
});
</script>

<template>
    <div class="w-100 animation-fade-in">
        <form @submit.prevent="handleSubmit">
            
            <!-- 提示文案 -->
            <div class="mb-2 text-center privacy-text" style="font-size: 0.85rem;">
                <span class="d-block">請輸入發送至以下信箱的驗證碼</span>
                <span class="d-block fw-bold text-dark">{{ email }}</span>
            </div>

            <!-- 驗證碼輸入框 -->
            <div class="mb-2 text-start">
                <div class="form-floating position-relative">
                    <input 
                        type="text" 
                        class="form-control login-input focus-ring"
                        :class="{ 'is-invalid': errorMsg }"
                        id="verifyCode" 
                        placeholder="" 
                        v-model="verifyCode"
                        required
                        autofocus
                        @input="errorMsg = ''"
                    />
                    <label for="verifyCode">輸入驗證碼</label>
                    
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
                :disabled="!turnstileToken"
            >
                驗證並登入
            </button>

            <!-- 人機驗證碼 -->
            <TurnstileWidget
                @verify="handleVerify"
                @expire="handleExpireOrError"
                @error="handleExpireOrError"
            />

            <!-- 重新發送與切換 Email 步驟 -->
            <div class="text-center privacy-text mb-0" style="font-size: 0.85rem;">
                沒收到驗證信嗎？ 
                <span v-if="countdown > 0" class="text-muted">重新發送 ({{ countdown }} 秒)</span>
                <a v-else href="#" class="privacy-link" @click.prevent="handleResend">重新發送</a>
            </div>
            <div class="text-center privacy-text mb-0" style="font-size: 0.85rem;">
                不是這個電子郵件嗎？ 
                <a href="#" class="privacy-link" @click.prevent="emit('back')">切換電子郵件</a>
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
    padding-right: 140px !important; /* 錯誤字樣較長，維持較大保護間距 */
}
.login-input.is-invalid {
    background-image: none !important; /* 阻斷 Bootstrap 內建紅色驚嘆號 */
    border-color: #fca5a5 !important; /* 更柔和的淡紅框 */
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12) !important; /* 柔和紅暈 */
}
</style>


