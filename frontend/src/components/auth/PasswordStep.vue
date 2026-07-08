<script setup>
import { ref } from 'vue';
import TurnstileWidget from '@/components/common/TurnstileWidget.vue';
import { useCapsLock } from '@/composables/useCapsLock.js';

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

const emit = defineEmits(['back', 'submit', 'forgot-password', 'clear-error']);
const password = ref('');
const turnstileToken = ref('');
const showPassword = ref(false);
const isFocused = ref(false);

const { isCapsLockOn } = useCapsLock();

const handleVerify = (token) => {
    turnstileToken.value = token;
};

const handleExpireOrError = () => {
    turnstileToken.value = '';
};

const handleInput = () => {
    emit('clear-error');
};

// 【暫時性模擬功能】待後端 API 串接後，此處應發送密碼登入驗證 API，而非使用模擬的彈窗提示
const handleSubmit = () => {
    console.log('Login attempt:', props.email, password.value, 'Turnstile Token:', turnstileToken.value);
    emit('submit', { email: props.email, password: password.value, turnstileToken: turnstileToken.value });
};
</script>

<template>
    <div class="w-100 animation-fade-in">
        <form @submit.prevent="handleSubmit">
            
            <!-- 顯示剛剛輸入的 Email (固定間距 mb-3，絕不跳動) -->
            <div class="mb-3">
                <div class="w-100 bg-light border text-dark d-flex align-items-center justify-content-center" 
                     style="height: 42px; border-radius: 0.5rem; font-size: 0.9rem; font-weight: 500;">
                    {{ email }}
                </div>
            </div>

            <!-- 密碼輸入框 (固定間距 mb-3，絕不跳動) -->
            <div class="mb-3 text-start">
                <div class="form-floating position-relative">
                    <input 
                        :type="showPassword ? 'text' : 'password'" 
                        class="form-control login-input focus-ring"
                        :class="{ 'is-invalid': errorMsg }"
                        id="password" 
                        placeholder="" 
                        v-model="password"
                        required
                        autofocus
                        @input="handleInput"
                        @focus="isFocused = true"
                        @blur="isFocused = false"
                    />
                    <label for="password">
                        密碼
                        <span v-if="isCapsLockOn && (isFocused || password)" class="ms-3 text-muted" style="opacity: 0.5;">
                            Caps Lock
                        </span>
                    </label>

                    <!-- 右側極簡整合元件區：錯誤提示 + 眼睛按鈕 (統一 13px, 400 細字體) -->
                    <div class="position-absolute top-50 translate-middle-y end-0 d-flex align-items-center gap-2 pe-3" style="z-index: 10;">
                        <span v-if="errorMsg" class="text-danger small text-truncate" style="font-size: 13px !important; font-weight: 400 !important; color: #ef4444 !important; letter-spacing: 0.5px; max-width: 150px;">
                            {{ errorMsg }}
                        </span>
                        <button 
                            type="button" 
                            class="eye-toggle-btn"
                            @click="showPassword = !showPassword"
                            tabindex="-1"
                        >
                            <i :class="showPassword ? 'bi bi-eye-slash-fill' : 'bi bi-eye-fill'"></i>
                        </button>
                    </div>
                </div>
            </div>

            <!-- 登入按鈕 -->
            <button 
                type="submit" 
                class="btn btn-primary login-btn email-btn w-100 mb-2"
                :disabled="!turnstileToken"
            >
                使用電子郵件登入
            </button>

            <!-- 人機驗證碼 -->
            <TurnstileWidget 
                @verify="handleVerify"
                @expire="handleExpireOrError"
                @error="handleExpireOrError"
            />

            <!-- 退回 Email 步驟 -->
            <p class="privacy-text text-center mb-0" style="font-size: 0.85rem;">
                不是這個電子郵件嗎？ 
                <a href="#" class="privacy-link" @click.prevent="emit('back')">切換電子郵件</a>
                <span> | </span>
                <a href="#" class="privacy-link" @click.prevent="emit('forgot-password')">忘記密碼</a>
            </p>
        </form>
    </div>
</template>

<style scoped>
.login-input {
    padding-right: 45px !important;
    transition: padding-right 0.2s ease;
}
.login-input.is-invalid {
    padding-right: 125px !important; /* 錯誤字樣較長，維持較大保護間距 */
}
.login-input.is-invalid {
    background-image: none !important; /* 阻斷 Bootstrap 內建紅色驚嘆號 */
    border-color: #fca5a5 !important; /* 更柔和的淡紅框 */
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12) !important; /* 柔和紅暈，與一般焦點 3px 尺寸一致 */
}
.eye-toggle-btn {
    border: none;
    background: transparent;
    padding: 0;
    color: #6c757d;
    opacity: 0.5;
    transition: opacity 0.2s ease, color 0.2s ease;
    cursor: pointer;
    outline: none;
    box-shadow: none !important;
    display: flex;
    align-items: center;
    justify-content: center;
    z-index: 5;
}
.eye-toggle-btn:hover {
    opacity: 0.9;
    color: #212529;
}
</style>

