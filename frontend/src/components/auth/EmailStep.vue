<script setup>
import { ref, computed, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import { loadGoogleIdentity, GOOGLE_CLIENT_ID } from '@/composables/useGoogleSignIn';

const props = defineProps({
    errorMsg: {
        type: String,
        default: ''
    },
    // 當使用者輸入的是 Google 帳號 Email 時，顯示引導改按 Google 按鈕的提示
    googleHint: {
        type: String,
        default: ''
    }
});

const route = useRoute();
const emit = defineEmits(['next', 'google-credential', 'clear-error']);
const email = ref('');

const googleBtnContainer = ref(null);
const googleError = ref('');

const privacyLink = computed(() => {
    return route.path.startsWith('/org') ? '/org/docs/privacy' : '/docs/privacy';
});

const handleInput = () => {
    emit('clear-error');
};

const handleSubmit = () => {
    emit('next', { email: email.value });
};

// 載入 GIS 並渲染官方 Google 登入按鈕；取得 ID Token 後上拋給 AuthCard 串接後端
onMounted(async () => {
    if (!GOOGLE_CLIENT_ID) {
        googleError.value = 'Google 登入未設定（缺少 Client ID）';
        return;
    }
    try {
        const gid = await loadGoogleIdentity();
        gid.initialize({
            client_id: GOOGLE_CLIENT_ID,
            callback: (response) => emit('google-credential', response.credential),
        });
        if (googleBtnContainer.value) {
            gid.renderButton(googleBtnContainer.value, {
                type: 'standard',
                theme: 'outline',     // 白底外框，貼近原本 btn-outline 樣式
                size: 'large',        // 約 40px 高，與電子郵件按鈕相近
                text: 'signin_with',  // 「透過 Google 登入」
                shape: 'rectangular', // 圓角矩形，對齊其他登入按鈕
                logo_alignment: 'center',
                locale: 'zh_TW',
                width: googleBtnContainer.value.offsetWidth || 360,
            });
        }
    } catch (e) {
        console.error('Google 登入初始化失敗:', e);
        googleError.value = 'Google 登入暫時無法使用，請改用電子郵件登入';
    }
});
</script>

<template>
    <div class="animation-fade-in">
        <!-- Google 帳號引導提示：偵測到此 Email 為 Google 帳號時顯示，引導使用者改按下方按鈕 -->
        <div v-if="googleHint" class="alert alert-info border-0 rounded-3 py-2 px-3 mb-2 d-flex align-items-center gap-2" style="font-size: 0.85rem;">
            <i class="bi bi-google text-danger"></i>
            <span>{{ googleHint }}</span>
        </div>

        <!-- Google 登入：由 Google Identity Services 官方按鈕渲染於此容器 -->
        <div ref="googleBtnContainer" class="d-flex justify-content-center mb-2"
             :class="{ 'google-btn-highlight rounded-3': googleHint }"></div>
        <p v-if="googleError" class="text-danger small text-center mb-2">{{ googleError }}</p>

        <!-- 分隔線 -->
        <div class="d-flex align-items-center my-3">
            <hr class="flex-grow-1 my-0 opacity-25">
            <span class="divider-text px-3 text-uppercase">or</span>
            <hr class="flex-grow-1 my-0 opacity-25">
        </div>

        <form @submit.prevent="handleSubmit">
            <!-- 使用 form-floating -->
            <div class="mb-3 text-start">
                <div class="form-floating position-relative">
                    <input 
                        type="email" 
                        class="form-control login-input focus-ring"
                        :class="{ 'is-invalid': errorMsg }"
                        id="email" 
                        placeholder="" 
                        v-model="email"
                        required
                        @input="handleInput"
                    />
                    <label for="email">Email address</label>
                    
                    <!-- 右側極簡整合元件區：錯誤提示 (統一 13px, 400 細字體) -->
                    <div class="position-absolute top-50 translate-middle-y end-0 d-flex align-items-center pe-3" style="z-index: 10;">
                        <span v-if="errorMsg" class="text-danger small text-truncate" style="font-size: 13px !important; font-weight: 400 !important; color: #ef4444 !important; letter-spacing: 0.5px; max-width: 150px;">
                            {{ errorMsg }}
                        </span>
                    </div>
                </div>
            </div>

            <button type="submit" class="btn btn-primary login-btn email-btn w-100 mb-2">
                使用電子郵件登入
            </button>
        </form>

        <!-- 底部隱私權 -->
        <p class="privacy-text lh-sm mb-0 px-2">
            輸入電子郵件完成註冊，即表示您同意TAP的 
            <RouterLink :to="privacyLink" class="privacy-link">隱私權政策</RouterLink>.
        </p>
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
    box-shadow: 0 0 0 3px rgba(239, 68, 68, 0.12) !important; /* 柔和紅暈，與一般焦點 3px 尺寸一致 */
}

/* 偵測到 Google 帳號時，對 Google 按鈕做一次柔和脈動，引導使用者點擊 */
.google-btn-highlight {
    animation: googlePulse 1.6s ease-in-out 2;
}
@keyframes googlePulse {
    0%, 100% { box-shadow: 0 0 0 0 rgba(66, 133, 244, 0); }
    50% { box-shadow: 0 0 0 4px rgba(66, 133, 244, 0.25); }
}
</style>

