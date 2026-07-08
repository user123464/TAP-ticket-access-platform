<script setup>
/**
 * B2bSidebarNode.vue — B2B 側欄選單樹的遞迴節點（視覺分層版）
 *
 * 遞迴渲染規則：
 * - 有 children 的節點＝群組（MENU）→ 靜態小標題 (Subheader) + 巢狀子節點（遞迴自身，不折疊）。
 * - 無 children 但有 urlPath 的節點＝頁面（PAGE）→ RouterLink，
 *   並將 urlPath 的 :organizerId 佔位符換成實際組織 ID。
 */
import { computed } from "vue";

const props = defineProps({
  node: { type: Object, required: true },
  organizerId: { type: String, default: "" },
});

const emit = defineEmits(["navigate"]);

const isGroup = computed(
  () => Array.isArray(props.node.children) && props.node.children.length > 0
);

// 將選單 urlPath 的 :organizerId 佔位符換成實際組織 ID
const resolvedPath = computed(() =>
  props.node.urlPath ? props.node.urlPath.replace(":organizerId", props.organizerId) : null
);
</script>

<template>
  <li class="nav-item">
    <!-- 群組（MENU）：非互動的靜態小標題 + 遞迴子節點（恆顯、不折疊） -->
    <template v-if="isGroup">
      <div class="sidebar-group-header">
        <span class="text-truncate">{{ node.name }}</span>
      </div>
      <ul class="nav flex-column sidebar-submenu">
        <B2bSidebarNode
          v-for="child in node.children"
          :key="child.resourceId"
          :node="child"
          :organizer-id="organizerId"
          @navigate="emit('navigate')"
        />
      </ul>
    </template>

    <!-- 頁面（PAGE）：連結 -->
    <RouterLink
      v-else-if="resolvedPath"
      :to="resolvedPath"
      class="nav-link sidebar-link d-flex align-items-center"
      @click="emit('navigate')"
    >
      <i class="bi me-2" :class="node.icon"></i>{{ node.name }}
    </RouterLink>
  </li>
</template>

<style scoped>
.sidebar-link {
  color: var(--tap-light-gray, #e2e8f0);
  font-weight: 500;
  /* 僅過渡顏色與背景，避開寬度排版過渡防 resize 抖動 */
  transition: background-color 0.2s ease-in-out, color 0.2s ease-in-out;
  border-radius: 6px;
  padding: 0.75rem 1rem;
}
.sidebar-link:hover {
  background-color: rgba(255, 255, 255, 0.1);
  color: #fff;
}
.sidebar-link.router-link-active {
  background-color: var(--tap-primary, #e57346);
  color: #fff;
  font-weight: bold;
}

/* 群組標題：非互動的靜態小標題（subheader），不可點、不折疊 */
.sidebar-group-header {
  display: flex;
  align-items: center;
  padding: 0.5rem 1rem 0.35rem;
  margin-top: 1.5rem;
  font-size: 0.68rem;
  font-weight: 700;
  letter-spacing: 0.1em;
  text-transform: uppercase;
  color: var(--tap-light-gray, #e2e8f0);
  opacity: 0.4; /* 半透明灰色 */
  border-top: 1px solid rgba(255, 255, 255, 0.1);
  user-select: none;
}
/* 第一個群組標題不需上分隔線（緊接側欄頂部） */
.nav-item:first-child .sidebar-group-header {
  margin-top: 0;
  border-top: none;
}

/* 子選單：群組下的入口平鋪、不縮排（小標題已足夠分隔層級） */
.sidebar-submenu {
  gap: 0.25rem;
  margin-top: 0.25rem;
}
</style>
