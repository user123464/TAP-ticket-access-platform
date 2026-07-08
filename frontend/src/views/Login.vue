<script setup>
import AuthCard from '@/components/auth/AuthCard.vue';
</script>

<template>
    <div class="login-wrapper d-flex justify-content-center align-items-center min-vh-100">
        <!-- 恢復置中，不往上推 -->
        <div class="w-100 text-center px-4" style="max-width: 420px; margin-top: -5vh;">

            <!-- 標題與副標 (改用襯線體 Georgia) -->
            <h1 class="login-title mb-2">Tap easy,<br>rock louder</h1>
            <p class="login-subtitle mb-4">Ticket Access Platform</p>

            <!-- 卡片 (寫死高度 320px) -->
            <div class="login-card p-4 mx-auto rounded-4 shadow-sm d-flex flex-column justify-content-center" style="height: 320px;">

                <!-- 登入組件核心 (整合了 Email/Password/Verify 等步驟) -->
                <AuthCard />

            </div>
        </div>
    </div>
</template>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600&display=swap');

/* ==========================================
   🎨 登入頁專屬樣式調整區
========================================== */

.login-wrapper {
    /* 全局預設改為無襯線體 Inter */
    font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
}

.login-card {
    border: 1px solid #dee2e6; /* 增加灰色細線 */
}

/* --- 標題區 (襯線體) --- */
.login-title {
    color: #212529; 
    font-family: 'Georgia', serif; /* 襯線體 */
    font-size: clamp(3.5rem, 5vw, 4rem); /* 放大至約 56px-64px */
    font-weight: 400; /* 改為常規體，讓襯線體顯得纖細優雅 */
    line-height: 1.1;
    letter-spacing: -0.02em; 
}

.login-subtitle {
    color: #212529; 
    font-family: 'Georgia', serif; /* 襯線體 */
    font-size: clamp(1.25rem, 2vw, 1.5rem); /* 約 20px-24px */
    font-weight: 300; 
}


</style>