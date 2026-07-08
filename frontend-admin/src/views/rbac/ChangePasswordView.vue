<script setup>
import { ref, computed } from "vue";
import { useRouter } from "vue-router";
import api from "@/plugins/axios.js";
import { useAuthStore } from "@/stores/auth.js";
import { useCapsLock } from "@/composables/useCapsLock.js";

const router = useRouter();
const authStore = useAuthStore();

const currentPassword = ref("");
const newPassword = ref("");
const confirmPassword = ref("");

const showCurrentPassword = ref(false);
const showNewPassword = ref(false);
const showConfirmPassword = ref(false);

const currentFocused = ref(false);
const newFocused = ref(false);
const confirmFocused = ref(false);

const isSubmitting = ref(false);
const errorMessage = ref("");
const successMessage = ref("");

const { isCapsLockOn } = useCapsLock();

// 密碼強度檢測
const passwordStrength = computed(() => {
  const pwd = newPassword.value;
  if (!pwd) return { score: 0, text: "", color: "" };
  
  let score = 1; // 預設為最低分 1 (弱)

  const hasUpperCase = /[A-Z]/.test(pwd);
  const hasLowerCase = /[a-z]/.test(pwd);
  const hasNumber = /[0-9]/.test(pwd);
  const hasLength8 = pwd.length >= 8;

  if (hasUpperCase && hasLowerCase && hasNumber && hasLength8) {
    score = 2; // 符合中級
    if (pwd.length >= 12) {
      score = 3; // 符合強級
    }
  }
  
  if (score === 2) return { score, text: "中 (密碼符合標準，增加長度可提升安全性)", color: "text-success" };
  if (score === 3) return { score, text: "強 (密碼安全性高)", color: "text-success" };
  
  return { score, text: "弱 (需滿 8 碼且包含英文大小寫與數字)", color: "text-warning" };
});

const isForced = computed(() => !!authStore.user?.mustChangePassword);

const isFormValid = computed(() => {
  const STD_PWD_REGEX = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
  const newPwdValid = STD_PWD_REGEX.test(newPassword.value) &&
                      newPassword.value === confirmPassword.value;
  if (isForced.value) {
    return newPwdValid;
  }
  return (
    currentPassword.value.length > 0 &&
    newPwdValid &&
    currentPassword.value !== newPassword.value
  );
});

const handleSubmit = async () => {
  if (!isFormValid.value || isSubmitting.value) return;

  isSubmitting.value = true;
  errorMessage.value = "";
  successMessage.value = "";

  try {
    await api.put("/api/user/password", {
      currentPassword: isForced.value ? "" : currentPassword.value,
      newPassword: newPassword.value,
    });

    // 成功修改
    successMessage.value = "密碼修改成功！正在引導您...";
    
    // 更新本地 auth store 狀態以解除路由鎖定
    if (authStore.user) {
      authStore.user.mustChangePassword = false;
    }

    currentPassword.value = "";
    newPassword.value = "";
    confirmPassword.value = "";

    setTimeout(() => {
      router.push("/admin/dashboard");
    }, 1500);
  } catch (error) {
    if (error.response) {
      errorMessage.value = error.response.data?.message ?? "密碼變更失敗，請檢查輸入的舊密碼";
    } else {
      errorMessage.value = "連線失敗，請檢查網路連線";
    }
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <div class="container py-5 d-flex justify-content-center">
    <div class="card shadow border-0 p-3 w-100" style="max-width: 500px; background-color: var(--tap-bg-surface);">
      <div class="card-body">
        
        <!-- 頂部標題 -->
        <h4 class="fw-bold mb-1 text-tap-primary">
          <i class="bi bi-shield-lock me-2 text-primary"></i>修改登入密碼
        </h4>
        <p class="text-tap-secondary small mb-4">
          為了維護您的帳號安全，請使用大小寫英文字母與數字之組合設定新密碼。
        </p>

        <!-- 偵測到預設密碼時的強制提示 Banner -->
        <div v-if="authStore.user?.mustChangePassword" class="alert alert-warning py-3 small mb-4">
          <div class="d-flex gap-2 align-items-start">
            <i class="bi bi-exclamation-triangle-fill fs-5 text-warning"></i>
            <div>
              <div class="fw-bold text-warning-emphasis mb-1">首次登入 / 密碼已被重設</div>
              <div class="text-tap-secondary">偵測到您目前使用的是預設密碼，請立即變更您的密碼以繼續存取管理後台系統。</div>
            </div>
          </div>
        </div>

        <!-- 成功 / 失敗訊息 -->
        <div v-if="errorMessage" class="alert alert-danger py-2 small d-flex align-items-center gap-2 mb-3">
          <i class="bi bi-exclamation-triangle-fill"></i>{{ errorMessage }}
        </div>
        <div v-if="successMessage" class="alert alert-success py-2 small d-flex align-items-center gap-2 mb-3">
          <i class="bi bi-check-circle-fill"></i>{{ successMessage }}
        </div>

        <form @submit.prevent="handleSubmit" novalidate>
          
          <!-- 目前密碼 (舊密碼) - 強制更換時免輸入，優化體驗 -->
          <div v-if="!isForced" class="mb-3 text-start">
            <div class="form-floating position-relative">
              <input
                :type="showCurrentPassword ? 'text' : 'password'"
                class="form-control focus-ring"
                id="currentPassword"
                placeholder="目前密碼"
                v-model="currentPassword"
                required
                @focus="currentFocused = true"
                @blur="currentFocused = false"
              />
              <label for="currentPassword">
                目前密碼
                <span v-if="isCapsLockOn && (currentFocused || currentPassword)" class="ms-3 text-warning small">
                  Caps Lock 已開啟
                </span>
              </label>
              
              <button
                type="button"
                class="btn eye-toggle-btn position-absolute top-50 translate-middle-y end-0 me-2"
                @click="showCurrentPassword = !showCurrentPassword"
                tabindex="-1"
              >
                <i class="bi" :class="showCurrentPassword ? 'bi-eye-slash-fill' : 'bi-eye-fill'"></i>
              </button>
            </div>
          </div>

          <!-- 新密碼 -->
          <div class="mb-3 text-start">
            <div class="form-floating position-relative">
              <input
                :type="showNewPassword ? 'text' : 'password'"
                class="form-control focus-ring"
                id="newPassword"
                placeholder="新密碼"
                v-model="newPassword"
                required
                @focus="newFocused = true"
                @blur="newFocused = false"
              />
              <label for="newPassword">
                新密碼
                <span v-if="isCapsLockOn && (newFocused || newPassword)" class="ms-3 text-warning small">
                  Caps Lock 已開啟
                </span>
              </label>
              
              <button
                type="button"
                class="btn eye-toggle-btn position-absolute top-50 translate-middle-y end-0 me-2"
                @click="showNewPassword = !showNewPassword"
                tabindex="-1"
              >
                <i class="bi" :class="showNewPassword ? 'bi-eye-slash-fill' : 'bi-eye-fill'"></i>
              </button>
            </div>
            
            <!-- 密碼強度提示 -->
            <div class="form-text mt-2 ps-1">
              <span v-if="newPassword" :class="passwordStrength.color" class="fw-semibold">
                密碼強度：{{ passwordStrength.text }}
              </span>
              <span v-else class="text-tap-secondary">密碼須達 8 碼以上，且必須包含英文大小寫字母與數字。</span>
            </div>
          </div>

          <!-- 確認新密碼 -->
          <div class="mb-4 text-start">
            <div class="form-floating position-relative">
              <input
                :type="showConfirmPassword ? 'text' : 'password'"
                class="form-control focus-ring"
                id="confirmPassword"
                placeholder="確認新密碼"
                v-model="confirmPassword"
                required
                @focus="confirmFocused = true"
                @blur="confirmFocused = false"
              />
              <label for="confirmPassword">
                確認新密碼
                <span v-if="isCapsLockOn && (confirmFocused || confirmPassword)" class="ms-3 text-warning small">
                  Caps Lock 已開啟
                </span>
              </label>
              
              <button
                type="button"
                class="btn eye-toggle-btn position-absolute top-50 translate-middle-y end-0 me-2"
                @click="showConfirmPassword = !showConfirmPassword"
                tabindex="-1"
              >
                <i class="bi" :class="showConfirmPassword ? 'bi-eye-slash-fill' : 'bi-eye-fill'"></i>
              </button>
            </div>
            
            <div v-if="confirmPassword && newPassword !== confirmPassword" class="form-text text-danger ps-1 mt-1">
              兩次輸入的新密碼不相符
            </div>
            <div v-if="confirmPassword && newPassword === confirmPassword" class="form-text text-success ps-1 mt-1">
              新密碼一致
            </div>
          </div>

          <!-- 新舊密碼相同防呆 -->
          <div v-if="!isForced && newPassword && currentPassword === newPassword" class="alert alert-danger py-2 small mb-3">
            新密碼不可與目前密碼相同
          </div>

          <!-- 提交按鈕 -->
          <button
            type="submit"
            class="btn btn-primary w-100 fw-bold py-2 shadow-sm"
            :disabled="!isFormValid || isSubmitting"
          >
            <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2" role="status"></span>
            儲存變更並套用
          </button>

        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.eye-toggle-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: var(--tap-text-secondary);
  opacity: 0.5;
  transition: opacity 0.2s ease, color 0.2s ease;
  cursor: pointer;
  outline: none;
  box-shadow: none !important;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
  width: 40px;
  height: 40px;
}
.eye-toggle-btn:hover {
  opacity: 0.9;
  color: var(--tap-text-primary);
}
</style>
