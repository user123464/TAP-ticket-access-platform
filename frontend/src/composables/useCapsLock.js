import { ref, onMounted, onUnmounted } from 'vue';

// 全域共享狀態 (Single Source of Truth)
const globalCapsLock = ref(false);
let listenerCount = 0;

// 檢查事件中的 Caps Lock 狀態
const checkCapsLock = (event) => {
    if (event && typeof event.getModifierState === 'function') {
        globalCapsLock.value = event.getModifierState('CapsLock');
    }
};

export function useCapsLock() {
    onMounted(() => {
        if (listenerCount === 0) {
            window.addEventListener('keydown', checkCapsLock);
            window.addEventListener('keyup', checkCapsLock);
            window.addEventListener('mousedown', checkCapsLock);
        }
        listenerCount++;
    });

    onUnmounted(() => {
        listenerCount--;
        if (listenerCount === 0) {
            window.removeEventListener('keydown', checkCapsLock);
            window.removeEventListener('keyup', checkCapsLock);
            window.removeEventListener('mousedown', checkCapsLock);
        }
    });

    return {
        isCapsLockOn: globalCapsLock
    };
}
