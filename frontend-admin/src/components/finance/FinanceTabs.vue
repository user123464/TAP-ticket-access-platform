<script setup>
/**
 * FinanceTabs.vue — 財務管理頁籤導覽（財務總覽 / 費用結算）
 * 群首頁（財務總覽）為 /admin/finance/settlement 前綴，標 exact 僅完全相符時高亮。
 */
const tabs = [
  { path: "/admin/finance", label: "財務總覽", icon: "bi-cash-stack", exact: true },
  { path: "/admin/finance/settlement", label: "費用結算", icon: "bi-calculator" },
];
</script>

<template>
  <ul class="nav nav-pills gap-2 mb-0">
    <li v-for="tab in tabs" :key="tab.path" class="nav-item">
      <RouterLink :to="tab.path" class="nav-link py-1 px-3" :class="{ 'exact-only': tab.exact }" active-class="active">
        <i class="bi me-1" :class="tab.icon"></i>{{ tab.label }}
      </RouterLink>
    </li>
  </ul>
</template>

<style scoped>
/* /admin/finance 是 /admin/finance/* 的前綴，「財務總覽」僅在完全相符時高亮 */
.exact-only.active:not(.router-link-exact-active) {
  background-color: transparent !important;
  color: var(--tap-text-secondary) !important;
}
</style>
