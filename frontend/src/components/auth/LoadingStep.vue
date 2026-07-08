<script setup>
// 單純顯示載入中動畫的組件
</script>

<template>
    <div class="w-100 d-flex flex-column justify-content-center align-items-center animation-fade-in">
        <!-- Bootstrap 內建的轉圈圈組件 -->
        <div class="spinner-border text-primary mb-3" role="status" style="width: 2.5rem; height: 2.5rem; border-width: 0.2rem;">
            <span class="visually-hidden">Loading...</span>
        </div>
        
        <!-- 提示文字 -->
        <div class="text-primary fw-medium" style="font-size: 0.95rem; letter-spacing: 0.5px;">
            處理中，請稍候...
        </div>
    </div>
</template>
