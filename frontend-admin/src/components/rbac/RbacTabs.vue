<script setup>
/**
 * RbacTabs.vue — RBAC 模組三頁共用頁籤導覽
 */
const tabs = [
  { path: "/admin/rbac/staff", label: "內部人員", icon: "bi-person-vcard" },
  { path: "/admin/rbac/roles", label: "角色管理", icon: "bi-person-badge" },
  { path: "/admin/rbac/templates", label: "組織角色模板", icon: "bi-diagram-2" },
  { path: "/admin/rbac/resources", label: "選單資源", icon: "bi-list-nested" },
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
