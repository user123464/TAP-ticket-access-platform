<script setup>
/**
 * LoginView.vue — Admin 登入頁（單步驟帳密 + Session）
 *
 * 內網部署：無 Turnstile 人機驗證、無註冊/忘記密碼流程（帳號由 SUPER_ADMIN 開立）。
 */
import { ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "@/stores/auth.js";

const route = useRoute();
const router = useRouter();
const authStore = useAuthStore();

const username = ref("");
const password = ref("");
const showPassword = ref(false);
const isSubmitting = ref(false);
const errorMessage = ref("");

const handleSubmit = async () => {
  if (!username.value || !password.value) {
    errorMessage.value = "請輸入帳號與密碼";
    return;
  }

  isSubmitting.value = true;
  errorMessage.value = "";

  const loginEmail = username.value.includes("@") ? username.value : `${username.value}@tap.com`;
  const result = await authStore.login(loginEmail, password.value);

  isSubmitting.value = false;

  if (result.success) {
    // 登入成功：導回原目的地（守衛記錄的 redirect）或 Dashboard
    router.push(route.query.redirect || "/admin/dashboard");
  } else {
    errorMessage.value = result.message;
  }
};
</script>

<template>
  <div class="login-card card border-0 shadow-lg p-2">
    <div class="card-body p-4">
      <!-- 品牌標題 -->
      <div class="text-center mb-4">
        <img src="@/assets/images/logo-light.png" alt="TAP Logo" style="height: 56px" />
        <h4 class="fw-bold mt-3 mb-1">TAP Admin</h4>
        <div class="small text-tap-secondary">總管理後台 · 內網限定</div>
      </div>

      <!-- 錯誤提示 -->
      <div v-if="errorMessage" class="alert alert-danger py-2 small d-flex align-items-center gap-2">
        <i class="bi bi-exclamation-triangle-fill"></i>{{ errorMessage }}
      </div>

      <form @submit.prevent="handleSubmit" novalidate>
        <div class="mb-3">
          <label for="username" class="form-label small fw-semibold">帳號</label>
          <input
            id="username"
            v-model.trim="username"
            type="text"
            class="form-control"
            placeholder="請輸入管理員帳號"
            autocomplete="username"
            autofocus
          />
        </div>

        <div class="mb-4">
          <label for="password" class="form-label small fw-semibold">密碼</label>
          <div class="input-group">
            <input
              id="password"
              v-model="password"
              :type="showPassword ? 'text' : 'password'"
              class="form-control"
              placeholder="請輸入密碼"
              autocomplete="current-password"
            />
            <button
              type="button"
              class="btn btn-outline-secondary"
              @click="showPassword = !showPassword"
              :aria-label="showPassword ? '隱藏密碼' : '顯示密碼'"
            >
              <i class="bi" :class="showPassword ? 'bi-eye-slash' : 'bi-eye'"></i>
            </button>
          </div>
        </div>

        <button type="submit" class="btn btn-primary w-100 fw-bold py-2" :disabled="isSubmitting">
          <span v-if="isSubmitting" class="spinner-border spinner-border-sm me-2"></span>
          {{ isSubmitting ? "登入中..." : "登入" }}
        </button>
      </form>

<br>
    </div>
  </div>
</template>

<style scoped>
.login-card {
  width: 100%;
  max-width: 400px;
}
</style>
