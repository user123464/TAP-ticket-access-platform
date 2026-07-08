<script setup>
/**
 * OrganizerTabs.vue — 商戶管理頁籤導覽（商戶列表 / KYC 審核）
 * 沿用 RbacTabs 模式：active-class 前綴高亮；群首頁（商戶列表）為其他子路徑前綴，標 exact 僅完全相符時高亮。
 */
const tabs = [
  { path: "/admin/organizers", label: "組織列表", icon: "bi-building", exact: true },
  { path: "/admin/organizers/kyc", label: "KYC 審核", icon: "bi-patch-check" },
];
</script>

<template>
  <ul class="nav nav-pills gap-2 mb-3">
    <li v-for="tab in tabs" :key="tab.path" class="nav-item">
      <RouterLink :to="tab.path" class="nav-link py-1 px-3" :class="{ 'exact-only': tab.exact }" active-class="active">
        <i class="bi me-1" :class="tab.icon"></i>{{ tab.label }}
      </RouterLink>
    </li>
  </ul>
</template>

<style scoped>
/* /admin/organizers 是 /admin/organizers/* 的前綴，「商戶列表」僅在完全相符時高亮 */
.exact-only.active:not(.router-link-exact-active) {
  background-color: transparent !important;
  color: var(--tap-text-secondary) !important;
}
</style>
