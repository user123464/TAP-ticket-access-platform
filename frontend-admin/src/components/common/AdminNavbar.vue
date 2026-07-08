<script setup>
/**
 * AdminNavbar.vue — Admin 後台頂部導覽列（深色版）
 *
 * 與 B2B Navbar 的差異：
 * - 無組織切換 / 聯絡支援，僅保留使用者下拉選單與登出
 * - 使用者資訊來自 auth store（Session），登出呼叫後端銷毀 Session
 */
import { computed, ref, onMounted, onUnmounted, nextTick } from "vue";
import { useRouter } from "vue-router";
import { storeToRefs } from "pinia";
import { Dropdown } from "bootstrap";
import { useAuthStore } from "@/stores/auth.js";
import { useMenuStore } from "@/stores/menu.js";
import BaseAvatar from "@/components/common/BaseAvatar.vue";

const router = useRouter();
const authStore = useAuthStore();
const menuStore = useMenuStore();
const { user } = storeToRefs(authStore);

const displayName = computed(() => user.value?.name || user.value?.username || "管理員");

// 頭像下拉：比照 B2bNavbar 程式化建立 Dropdown 實例。
// 原本依賴 data-bs-toggle data-api，但本後台未全域初始化 Bootstrap data-api，
// 下拉點不開；改用 new Dropdown(ref) 明確掛載、unmount 時 dispose 釋放。
const dropdownRef = ref(null);
let dropdownInstance = null;

onMounted(async () => {
  await nextTick();
  if (dropdownRef.value) {
    dropdownInstance = new Dropdown(dropdownRef.value);
  }
});

onUnmounted(() => {
  if (dropdownInstance) {
    dropdownInstance.dispose();
    dropdownInstance = null;
  }
});

// 本後台未啟用 Bootstrap data-api，點擊不會自動觸發；需手動呼叫實例的 toggle()。
const toggleDropdown = () => dropdownInstance?.toggle();

const handleLogout = async () => {
  await authStore.logout();
  menuStore.reset();
  router.push("/login");
};
</script>

<template>
  <header>
    <nav class="navbar navbar-expand admin-navbar border-top border-primary border-5 shadow-sm py-2">
      <div class="container-fluid px-4">
        <!-- Logo 與品牌名稱 -->
        <RouterLink class="navbar-brand d-flex align-items-center gap-2" to="/admin/dashboard">
          <img src="@/assets/images/logo-light.png" alt="TAP Logo" class="brand-logo" />
          <span class="brand-name">TAP</span>
          <span class="brand-subtitle fs-6 fw-normal ms-1 d-none d-sm-inline">Admin 總管理後台</span>
        </RouterLink>

        <!-- 右側：使用者下拉選單 -->
        <div class="d-flex align-items-center ms-auto">
          <div class="nav-item dropdown">
            <a
              ref="dropdownRef"
              class="nav-link dropdown-toggle d-flex align-items-center gap-2 text-white"
              href="#"
              role="button"
              aria-expanded="false"
              @click.prevent="toggleDropdown"
            >
              <BaseAvatar :src="user?.avatarUrl" :name="displayName" :size="28" />
              <span class="fw-semibold d-none d-sm-inline">{{ displayName }}</span>
            </a>

            <ul class="dropdown-menu dropdown-menu-end shadow border-0 mt-2 p-2" style="min-width: 220px">
              <!-- 使用者資訊區 -->
              <li class="px-3 py-2 rounded-3 mb-2 user-info-block">
                <div class="fw-bold text-truncate">{{ displayName }}</div>
                <div class="small text-tap-secondary text-truncate">{{ user?.email }}</div>
                <span class="badge mt-2 role-badge">{{ authStore.roleLabel }}</span>
              </li>

              <li><hr class="dropdown-divider" /></li>

              <li>
                <RouterLink
                  class="dropdown-item d-flex align-items-center gap-2 py-2 rounded-2"
                  to="/admin/change-password"
                >
                  <i class="bi bi-shield-lock"></i>修改密碼
                </RouterLink>
              </li>

              <li>
                <a
                  class="dropdown-item text-danger d-flex align-items-center gap-2 py-2 rounded-2"
                  href="#"
                  @click.prevent="handleLogout"
                >
                  <i class="bi bi-box-arrow-right"></i>登出管理後台
                </a>
              </li>
            </ul>
          </div>
        </div>
      </div>
    </nav>
  </header>
</template>

<style scoped>
@import url("https://fonts.googleapis.com/css2?family=Syne:wght@400..800&display=swap");

.admin-navbar {
  background-color: var(--tap-bg-surface);
}

.brand-name {
  font-family: "Syne", sans-serif;
  font-weight: 700;
  font-size: 1.4rem;
  letter-spacing: -0.5px;
  color: var(--tap-text-primary) !important;
}

.brand-subtitle {
  color: var(--tap-text-secondary);
}

.brand-logo {
  height: 32px;
  width: auto;
  object-fit: contain;
}

.user-info-block {
  background-color: var(--tap-bg-hover);
}

.role-badge {
  background-color: var(--tap-primary);
}
</style>
