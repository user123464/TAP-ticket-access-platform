<script setup>
/**
 * MaintenanceTabs.vue — 系統維護頁籤導覽（排程任務 / 資料字典 / 系統參數 / 檔案管理）
 * 四者路徑互不為前綴，無需 exact。
 */
const tabs = [
  { path: "/admin/system/jobs", label: "排程任務", icon: "bi-clock" },
  { path: "/admin/system/dictionaries", label: "資料字典", icon: "bi-card-list" },
  { path: "/admin/system/config", label: "系統參數", icon: "bi-sliders" },
  { path: "/admin/system/media", label: "檔案管理", icon: "bi-folder" },
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
