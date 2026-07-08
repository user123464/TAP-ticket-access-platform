<script setup>
/**
 * BillingTabs.vue — 訂閱與合約頁籤導覽（客戶訂閱 / 方案目錄 / 合約與範本）
 * 由原 SubscriptionTabs 改造而來，補入合約。三者路徑互不為前綴，
 * 合約 tab 前綴匹配合約詳情/範本/新建頁，無需 exact。
 */
const tabs = [
  { path: "/admin/billing/subscriptions", label: "客戶訂閱", icon: "bi-list-check" },
  { path: "/admin/billing/plans", label: "方案目錄", icon: "bi-grid" },
  { path: "/admin/billing/contracts", label: "合約與範本", icon: "bi-file-earmark-text" },
];
</script>

<template>
  <ul class="nav nav-pills gap-2 mb-3">
    <li v-for="tab in tabs" :key="tab.path" class="nav-item">
      <RouterLink :to="tab.path" class="nav-link py-1 px-3" active-class="active">
        <i class="bi me-1" :class="tab.icon"></i>{{ tab.label }}
      </RouterLink>
    </li>
  </ul>
</template>
