<script setup>
import { onMounted, onUnmounted, ref } from 'vue';

const props = defineProps({
    sitekey: {
        type: String,
        default: () => import.meta.env.VITE_TURNSTILE_SITE_KEY || '0x4AAAAAADgr2len0duLrJsz'
    },
    theme: {
        type: String,
        default: 'light' // 'light', 'dark', 'auto'
    },
    size: {
        type: String,
        default: 'normal' // 'normal', 'compact', 'flexible'
    }
});

const emit = defineEmits(['verify', 'expire', 'error']);

const container = ref(null);
let widgetId = null;

const loadScript = () => {
    return new Promise((resolve, reject) => {
        if (window.turnstile) {
            resolve();
            return;
        }

        const scriptId = 'cloudflare-turnstile-script';
        let script = document.getElementById(scriptId);

        if (script) {
            // 腳本已存在但 window.turnstile 還沒準備好，則輪詢等待
            const interval = setInterval(() => {
                if (window.turnstile) {
                    clearInterval(interval);
                    resolve();
                }
            }, 100);
            return;
        }

        script = document.createElement('script');
        script.id = scriptId;
        script.src = 'https://challenges.cloudflare.com/turnstile/v0/api.js?render=explicit';
        script.async = true;
        script.defer = true;
        script.onload = () => {
            const check = setInterval(() => {
                if (window.turnstile) {
                    clearInterval(check);
                    resolve();
                }
            }, 50);
        };
        script.onerror = (err) => reject(err);
        document.head.appendChild(script);
    });
};

onMounted(async () => {
    try {
        await loadScript();
        if (container.value && window.turnstile) {
            widgetId = window.turnstile.render(container.value, {
                sitekey: props.sitekey,
                theme: props.theme,
                size: props.size,
                callback: (token) => {
                    emit('verify', token);
                },
                'expired-callback': () => {
                    emit('expire');
                },
                'error-callback': (err) => {
                    emit('error', err);
                }
            });
        }
    } catch (err) {
        console.error('Failed to load Cloudflare Turnstile script:', err);
        emit('error', err);
    }
});

onUnmounted(() => {
    if (widgetId && window.turnstile) {
        window.turnstile.remove(widgetId);
    }
});

const reset = () => {
    if (widgetId && window.turnstile) {
        window.turnstile.reset(widgetId);
    }
};

defineExpose({
    reset
});
</script>

<template>
    <div class="turnstile-wrapper">
        <div ref="container" class="cf-turnstile"></div>
    </div>
</template>

<style scoped>
.turnstile-wrapper {
    display: flex;
    justify-content: center;
    align-items: center;
    width: 100%;
    height: 56px; /* 縮放後 (65 * 0.85 = 55.25) 的視覺高度 */
    margin: 8px 0;
    overflow: hidden;
}

.cf-turnstile {
    transform: scale(0.85);
    transform-origin: center;
    width: 300px;
    height: 65px;
    display: flex;
    justify-content: center;
}

/* 支援透過 CSS 進行縮放的輔助 Class */
:deep(iframe) {
    border: none;
}
</style>
