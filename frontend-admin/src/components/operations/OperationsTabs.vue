<script setup>
/**
 * OperationsTabs.vue — 客服與營運查詢頁籤導覽（客訴回饋 / 活動查詢 / 訂單查詢）
 * 三者路徑互不為前綴，各自前綴匹配其詳情頁（events/:id、orders/:id），無需 exact。
 */
const tabs = [
  { path: "/admin/operations/submissions", label: "客訴回饋", icon: "bi-chat-left-dots" },
  { path: "/admin/operations/events", label: "活動查詢", icon: "bi-calendar-event" },
  { path: "/admin/operations/orders", label: "訂單查詢", icon: "bi-receipt" },
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
