<script setup>
/**
 * NotificationTabs.vue — 通知與公告頁籤導覽（全站公告 / 通知範本 / 發送日誌）
 * 三者路徑互不為前綴，無需 exact。
 */
const tabs = [
  { path: "/admin/system/announcements", label: "系統文件", icon: "bi-file-earmark-text" },
  { path: "/admin/system/templates", label: "通知範本", icon: "bi-envelope-paper" },
  { path: "/admin/system/notifications", label: "發送日誌", icon: "bi-send-check" },
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
