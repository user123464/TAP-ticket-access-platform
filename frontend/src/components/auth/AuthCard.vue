<script setup>
import { ref } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import EmailStep from '@/components/auth/EmailStep.vue';
import PasswordStep from '@/components/auth/PasswordStep.vue';
import VerifyCodeStep from '@/components/auth/VerifyCodeStep.vue';
import SetPasswordStep from '@/components/auth/SetPasswordStep.vue';
import TwoFactorStep from '@/components/auth/TwoFactorStep.vue';
import LoadingStep from '@/components/auth/LoadingStep.vue';
import axios from '@/plugins/axios.js';
import { useAuthStore } from '@/stores/auth.js';

const router = useRouter();
const route = useRoute();
const authStore = useAuthStore();

// 狀態管理：'email' | 'password' | 'verifyCode' | 'setPassword' | 'login2fa' | 'loading'
const step = ref('email');
const email = ref('');

// 暫存登入密碼，供 2FA 流程「重新發送驗證碼」時重打登入 API 使用（僅存於記憶體，不寫入快取）
const pendingPassword = ref('');

// 錯誤訊息狀態 (傳遞給密碼輸入卡片，防止版面擠壓)
const errorMsg = ref('');

// Google 帳號引導提示（輸入到 Google 帳號 Email 時顯示於 Email 步驟）
const googleHint = ref('');

// 暫存已被驗證成功的 OTP 驗證碼，供後續註冊/重設密碼 API 使用
const verifiedOtpCode = ref('');

// 追蹤流程類型：'register' (註冊流程) | 'forgotPassword' (忘記密碼流程)
const flowType = ref('register');

// 處理 Email 下一步：發送真實 API 檢查 Email 註冊狀態
const handleNext = async (data) => {
    email.value = data.email;
    errorMsg.value = '';
    googleHint.value = '';
    step.value = 'loading'; // 立即進入轉場動畫，等待後端 API 回傳

    try {
        const response = await axios.get(`/api/auth/check-email?email=${data.email}`, {
            _skip401Redirect: true
        });
        const { exists, authProvider } = response.data;

        if (exists) {
            if (authProvider === 'GOOGLE') {
                // Google 帳號沒有本地密碼，不可進密碼步驟，退回 Email 步驟並引導改按 Google 按鈕
                googleHint.value = '此帳號使用 Google 登入，請點下方按鈕。';
                step.value = 'email';
            } else {
                // 一般帳號，進入密碼登入頁面
                step.value = 'password';
            }
        } else {
            // 未註冊，先呼叫後端發送註冊驗證信
            await axios.post(`/api/auth/send-code?email=${data.email}`);
            flowType.value = 'register';
            step.value = 'verifyCode';
        }
    } catch (err) {
        console.error('Check email error:', err);
        // 斷線（無 response）由 axios 攔截器於全站公告條統一提示
        if (err.response) {
            errorMsg.value = err.response.data?.message || '發生未知錯誤';
        }
        step.value = 'email'; // 失敗退回原輸入頁
    }
};

// 處理「忘記密碼」事件：發送重設驗證信並導向驗證碼流程
const handleForgotPassword = async () => {
    errorMsg.value = '';
    flowType.value = 'forgotPassword';
    step.value = 'loading';
    try {
        await axios.post(`/api/auth/send-code?email=${email.value}`);
        step.value = 'verifyCode';
    } catch (err) {
        console.error('Forgot password send code error:', err);
        if (err.response) {
            errorMsg.value = err.response.data?.message || '發送驗證碼失敗，請稍後再試';
        }
        step.value = 'password';
    }
};

// 處理驗證信完成事件：暫存驗證碼並進入密碼設定頁面
const handleVerificationComplete = (data) => {
    verifiedOtpCode.value = data.code;
    step.value = 'loading';
    setTimeout(() => {
        step.value = 'setPassword';
    }, 500);
};

// 處理設定或重設密碼完成事件：呼叫後端註冊/重設 API
const handlePasswordSetSubmit = async (data) => {
    step.value = 'loading';
    try {
        if (flowType.value === 'register') {
            // 發送註冊請求
            await axios.post('/api/auth/register', {
                email: email.value,
                password: data.password,
                name: email.value.split('@')[0] || '使用者'
            });
            
            // 註冊成功，直接自動幫使用者執行登入與跳轉
            await handleSubmit({ email: email.value, password: data.password });
        } else {
            // 忘記密碼與重設密碼流程：先呼叫重設密碼 API，更新資料庫，再登入
            await axios.post('/api/auth/reset-password', {
                email: email.value,
                code: verifiedOtpCode.value,
                password: data.password
            });
            await handleSubmit({ email: email.value, password: data.password });
        }
    } catch (err) {
        console.error('Password submission error:', err);
        if (err.response) {
            errorMsg.value = err.response.data?.message || '密碼設定失敗，請稍後再試';
        }
        step.value = 'setPassword';
    }
};

// 呼叫登入 API 並儲存伺服器回傳的 JWT Token
const handleSubmit = async (loginData) => {
    console.log('AuthCard submitting login to backend:', loginData.email);
    step.value = 'loading';
    errorMsg.value = '';
    
    try {
        const response = await axios.post('/api/auth/login', {
            email: loginData.email,
            password: loginData.password
        }, {
            _skip401Redirect: true // 告訴攔截器這是一個本地需要處理的 401 請求
        });
        
        const data = response.data; // 一般登入含 accessToken；2FA 帳號則回傳 twoFactorRequired

        // 帳號已啟用 2FA：密碼正確但尚未發 Token，後端已寄出驗證碼，進入第二步
        if (data.twoFactorRequired) {
            email.value = loginData.email;
            pendingPassword.value = loginData.password;
            step.value = 'login2fa';
            return;
        }

        // 一般登入：使用 Pinia Store 統一處理登入狀態寫入
        authStore.login(data);

        // 跳轉
        if (route.path.startsWith('/org')) {
            router.push('/org/select');
        } else {
            router.push('/');
        }
    } catch (err) {
        console.error('Login error:', err);
        // 斷線（無 response）由 axios 攔截器於全站公告條統一提示
        if (err.response) {
            // 將錯誤訊息回傳給密碼組件，以小紅字安全呈現
            errorMsg.value = err.response.data?.message || '帳號或密碼錯誤，請重新輸入';
        }
        step.value = 'password';
    }
};

// 2FA 第二步：送出信箱驗證碼換取 JWT
const handleTwoFactorSubmit = async (data) => {
    errorMsg.value = '';
    try {
        const response = await axios.post('/api/auth/login/2fa', {
            email: email.value,
            code: data.code
        }, {
            _skip401Redirect: true
        });

        pendingPassword.value = '';
        authStore.login(response.data);

        if (route.path.startsWith('/org')) {
            router.push('/org/select');
        } else {
            router.push('/');
        }
    } catch (err) {
        console.error('2FA login error:', err);
        if (err.response) {
            errorMsg.value = err.response.data?.message || '驗證碼不正確或已過期';
        }
    }
};

// 2FA 重新發送：以暫存的帳密重打登入 API，後端會重發驗證碼
const handleTwoFactorResend = async () => {
    if (!pendingPassword.value) return;
    try {
        await axios.post('/api/auth/login', {
            email: email.value,
            password: pendingPassword.value
        }, { _skip401Redirect: true });
    } catch (err) {
        console.error('2FA resend error:', err);
        if (err.response) {
            errorMsg.value = err.response.data?.message || '重新發送失敗，請稍後再試';
        }
    }
};

// 真實 Google 登入：拿 EmailStep 上拋的 ID Token 送後端驗證，換取本系統 JWT
const handleGoogleCredential = async (credential) => {
    if (!credential) return;
    errorMsg.value = '';
    step.value = 'loading';

    try {
        const response = await axios.post('/api/auth/google', {
            idToken: credential
        }, {
            _skip401Redirect: true
        });

        // 後端回傳與一般登入相同結構 (accessToken, email, name, userId, avatarUrl)
        authStore.login(response.data);

        // 跳轉回首頁或 B2B 選擇組織頁
        if (route.path.startsWith('/org')) {
            router.push('/org/select');
        } else {
            router.push('/');
        }
    } catch (err) {
        console.error('Google login error:', err);
        // 斷線（無 response）由 axios 攔截器於全站公告條統一提示
        if (err.response) {
            errorMsg.value = err.response.data?.message || 'Google 登入失敗，請稍後再試';
        }
        step.value = 'email'; // 失敗退回 Email 輸入頁
    }
};
</script>

<template>
    <div class="w-100 h-100 d-flex flex-column justify-content-center">
        <EmailStep
            v-if="step === 'email'"
            :error-msg="errorMsg"
            :google-hint="googleHint"
            @next="handleNext"
            @google-credential="handleGoogleCredential"
            @clear-error="errorMsg = ''; googleHint = '';"
        />
        <PasswordStep 
            v-else-if="step === 'password'" 
            :email="email" 
            :error-msg="errorMsg"
            @back="step = 'email'; errorMsg = '';" 
            @submit="handleSubmit"
            @forgot-password="handleForgotPassword"
            @clear-error="errorMsg = ''"
        />
        <VerifyCodeStep 
            v-else-if="step === 'verifyCode'" 
            :email="email" 
            @back="step = 'email'" 
            @submit="handleVerificationComplete"
        />
        <SetPasswordStep
            v-else-if="step === 'setPassword'"
            :email="email"
            :mode="flowType === 'forgotPassword' ? 'reset' : 'set'"
            :error-msg="errorMsg"
            @back="step = (flowType === 'forgotPassword' ? 'password' : 'email'); errorMsg = '';"
            @submit="handlePasswordSetSubmit"
            @clear-error="errorMsg = ''"
        />
        <TwoFactorStep
            v-else-if="step === 'login2fa'"
            :email="email"
            :error-msg="errorMsg"
            @submit="handleTwoFactorSubmit"
            @resend="handleTwoFactorResend"
            @back="step = 'password'; errorMsg = '';"
            @clear-error="errorMsg = ''"
        />
        <LoadingStep
            v-else-if="step === 'loading'"
        />
    </div>
</template>

<style scoped>
/* 將原本寫在 Login.vue 的 :deep 樣式集中在此，確保無論用在哪裡樣式都一致 */
:deep(.login-btn) {
    height: 42px; 
    font-size: 1rem; 
    font-weight: 300; 
    border-radius: 0.5rem; 
}

:deep(.email-btn) {
    color: #ffffff !important;
}

:deep(.login-input) {
    font-size: 1rem;
    font-weight: 500;
    border-radius: 0.5rem;
}

:deep(.login-input::placeholder) {
    color: #6c757d;
}

:deep(.divider-text) {
    color: #6c757d; 
    font-size: 0.75rem; 
    font-weight: 400; 
    letter-spacing: 1px; 
}

:deep(.privacy-text) {
    color: #6c757d; 
    font-size: 0.75rem; 
    font-weight: 300;
}

:deep(.privacy-link) {
    color: #212529;
    text-decoration: underline;
    opacity: 0.75;
}

:deep(.animation-fade-in) {
    animation: fadeIn 0.3s ease-in-out;
}

@keyframes fadeIn {
    from { opacity: 0; transform: translateY(5px); }
    to { opacity: 1; transform: translateY(0); }
}
</style>
