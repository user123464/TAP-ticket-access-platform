<script setup>
import { ref, computed } from 'vue';
import { useCapsLock } from '@/composables/useCapsLock.js';

const props = defineProps({
    email: {
        type: String,
        required: true
    },
    mode: {
        type: String,
        default: 'set' // 'set' | 'reset'
    },
    errorMsg: {
        type: String,
        default: ''
    }
});

const emit = defineEmits(['back', 'submit', 'clear-error']);

const password = ref('');
const confirmPassword = ref('');
const passwordError = ref('');
const confirmPasswordError = ref('');

const showPassword = ref(false);
const showConfirmPassword = ref(false);
const passwordFocused = ref(false);
const confirmPasswordFocused = ref(false);

const { isCapsLockOn } = useCapsLock();

const isModeReset = computed(() => props.mode === 'reset');

const titleText = computed(() => isModeReset.value ? '重設您的密碼' : '設定您的密碼');
const descText = computed(() => isModeReset.value ? '請為您的帳號設定一個新的密碼' : '這是您第一次登入，請設定初始密碼');
const buttonText = computed(() => isModeReset.value ? '重設密碼並登入' : '設定密碼並登入');

const validatePasswords = () => {
    // 1. 檢查長度與規則
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
    if (password.value) {
        if (password.value.length < 8) {
            passwordError.value = '最少 8 字元';
        } else if (!regex.test(password.value)) {
            passwordError.value = '須包含大小寫英文與數字';
        } else {
            passwordError.value = '';
        }
    } else {
        passwordError.value = '';
    }

    // 2. 檢查兩次密碼一致性 (當確認密碼不為空，且兩者不同時才提示)
    if (confirmPassword.value) {
        if (password.value !== confirmPassword.value) {
            confirmPasswordError.value = '密碼不一致';
        } else {
            confirmPasswordError.value = '';
        }
    } else {
        confirmPasswordError.value = '';
    }
};

const handlePasswordInput = () => {
    emit('clear-error');
    validatePasswords();
};

const handleConfirmPasswordInput = () => {
    emit('clear-error');
    validatePasswords();
};

const handleSubmit = () => {
    validatePasswords();
    
    const regex = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
    if (!password.value || !regex.test(password.value)) {
        passwordError.value = '須包含大小寫英文與數字，且至少8字元';
        return;
    }
    if (password.value !== confirmPassword.value) {
        confirmPasswordError.value = '密碼不一致';
        return;
    }
    
    console.log(`${props.mode === 'reset' ? 'Reset' : 'Set'} password for:`, props.email);
    emit('submit', { email: props.email, password: password.value });
};
</script>

<template>
    <div class="w-100 animation-fade-in">
        <form @submit.prevent="handleSubmit">
            
            <!-- 標題與說明 -->
            <div class="mb-2 text-center">
                <h5 class="fw-bold mb-1 text-dark" style="font-size: 1.15rem;">{{ titleText }}</h5>
                <p class="text-muted mb-0" style="font-size: 0.8rem;">
                    {{ descText }}<br>
                    <span class="fw-semibold text-secondary">{{ email }}</span>
                </p>
            </div>

            <!-- 新密碼輸入框 -->
            <div class="mb-2 text-start">
                <div class="form-floating position-relative">
                    <input 
                        :type="showPassword ? 'text' : 'password'" 
                        class="form-control login-input focus-ring"
                        :class="{ 'is-invalid': passwordError }"
                        id="newPassword" 
                        placeholder="" 
                        v-model="password"
                        required
                        autofocus
                        @input="handlePasswordInput"
                        @focus="passwordFocused = true"
                        @blur="passwordFocused = false"
                    />
                    <label for="newPassword">
                        新密碼
                        <span v-if="isCapsLockOn && (passwordFocused || password)" class="ms-3 text-muted" style="opacity: 0.5;">
                            Caps Lock
                        </span>
                    </label>

                    <!-- 右側極簡整合元件區：提示文字 + 眼睛按鈕 (移除紅叉) -->
                    <div class="position-absolute top-50 translate-middle-y end-0 d-flex align-items-center gap-2 pe-3" style="z-index: 10;">
                        <span v-if="passwordError" class="text-danger small" style="font-size: 13px !important; font-weight: 400 !important; color: #ef4444 !important; letter-spacing: 0.5px;">
                            {{ passwordError }}
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

            <!-- 確認密碼輸入框 -->
            <div class="mb-2 text-start">
                <div class="form-floating position-relative">
                    <input 
                        :type="showConfirmPassword ? 'text' : 'password'" 
                        class="form-control login-input focus-ring"
                        :class="{ 'is-invalid': confirmPasswordError }"
                        id="confirmPassword" 
                        placeholder="" 
                        v-model="confirmPassword"
                        required
                        @input="handleConfirmPasswordInput"
                        @focus="confirmPasswordFocused = true"
                        @blur="confirmPasswordFocused = false"
                    />
                    <label for="confirmPassword">
                        確認密碼
                        <span v-if="isCapsLockOn && (confirmPasswordFocused || confirmPassword)" class="ms-3 text-muted" style="opacity: 0.5;">
                            Caps Lock
                        </span>
                    </label>

                    <!-- 右側極簡整合元件區：提示文字 + 眼睛按鈕 (移除紅叉) -->
                    <div class="position-absolute top-50 translate-middle-y end-0 d-flex align-items-center gap-2 pe-3" style="z-index: 10;">
                        <span v-if="confirmPasswordError" class="text-danger small" style="font-size: 13px !important; font-weight: 400 !important; color: #ef4444 !important; letter-spacing: 0.5px;">
                            {{ confirmPasswordError }}
                        </span>
                        <button 
                            type="button" 
                            class="eye-toggle-btn"
                            @click="showConfirmPassword = !showConfirmPassword"
                            tabindex="-1"
                        >
                            <i :class="showConfirmPassword ? 'bi bi-eye-slash-fill' : 'bi bi-eye-fill'"></i>
                        </button>
                    </div>
                </div>
            </div>

            <!-- API 錯誤提示 (極簡行內，防版面抖動) -->
            <div v-if="errorMsg" class="text-danger text-center small mb-2" style="font-size: 13px !important; font-weight: 400 !important; color: #ef4444 !important; letter-spacing: 0.5px;">
                {{ errorMsg }}
            </div>

            <!-- 提交按鈕 -->
            <button 
                type="submit" 
                class="btn btn-primary login-btn email-btn w-100 mb-2"
            >
                {{ buttonText }}
            </button>

            <!-- 動態返回連結 -->
            <p class="privacy-text text-center mb-0" style="font-size: 0.85rem;">
                <span v-if="isModeReset">
                    想起來了嗎？ 
                    <a href="#" class="privacy-link" @click.prevent="emit('back')">返回登入</a>
                </span>
                <span v-else>
                    不想註冊了？ 
                    <a href="#" class="privacy-link" @click.prevent="emit('back')">返回輸入信箱</a>
                </span>
            </p>
        </form>
    </div>
</template>

<style scoped>
.login-input {
    /* 保護輸入文字，防範輸入字元太長時與右側提示或眼睛重疊 */
    padding-right: 45px !important;
    transition: padding-right 0.2s ease;
}
.login-input.is-invalid {
    padding-right: 125px !important; /* 錯誤字樣較長，維持較大保護間距 */
}
.login-input.is-invalid {
    background-image: none !important; /* 阻斷 Bootstrap 預設驚嘆號 */
    border-color: #fca5a5 !important; /* 柔和淡紅框 */
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
