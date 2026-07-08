<script setup>
/**
 * AdminSidebarNode.vue — 側欄選單樹的遞迴節點
 *
 * 渲染規則：
 * - 有 children 的節點＝群組（MENU）→ 非互動的靜態小標題（subheader）+ 巢狀子節點（恆顯、不折疊）。
 * - 無 children 但有 urlPath 的節點＝頁面（PAGE）→ RouterLink。
 *   （頁面入口的頁籤子頁在後端以 is_visible=0 過濾，故 PAGE 入口 children 為空、渲染成連結。）
 * 資料來自後端 system_resource 樹（icon/name/urlPath/children 皆來自 DB）。
 * 定案 IA：3 群組靜態小標題、不折疊（使用者否決折疊互動）。
 */
import { computed } from "vue";
import { useActiveRoute } from "@/composables/useActiveRoute";

const props = defineProps({
  node: { type: Object, required: true },
});

const emit = defineEmits(["navigate"]); // 點擊葉節點時冒泡，供外層關閉手機抽屜

const isGroup = computed(
  () => Array.isArray(props.node.children) && props.node.children.length > 0
);

// 自訂高亮：子頁籤/詳情頁與入口為平級路由，原生 router-link-active 無法傳遞，
// 改以模組前綴匹配，使入口在整個功能主體下保持高亮。
const { isLinkActive } = useActiveRoute();
const isActive = computed(() => isLinkActive(props.node.urlPath));
</script>

<template>
  <li class="nav-item">
    <!-- 群組（MENU）：非互動的靜態小標題 + 遞迴子節點（恆顯、不折疊） -->
    <template v-if="isGroup">
      <div class="sidebar-group-header">
        <span class="text-truncate">{{ node.name }}</span>
      </div>
      <ul class="nav flex-column sidebar-submenu">
        <AdminSidebarNode
          v-for="child in node.children"
          :key="child.resourceId"
          :node="child"
          @navigate="emit('navigate')"
        />
      </ul>
    </template>

    <!-- 頁面（PAGE）：連結 -->
    <RouterLink
      v-else-if="node.urlPath"
      :to="node.urlPath"
      class="nav-link sidebar-link d-flex align-items-center"
      :class="{ active: isActive }"
      @click="emit('navigate')"
    >
      <i class="bi me-2" :class="node.icon"></i>{{ node.name }}
    </RouterLink>
  </li>
</template>

<style scoped>
/* 連結樣式定義於此（Vue scoped 樣式不會作用到子元件內部，故各節點自帶樣式） */
.sidebar-link {
  color: var(--tap-text-secondary);
  font-weight: 500;
  transition: background-color 0.2s ease-in-out, color 0.2s ease-in-out;
  border-radius: 6px;
  padding: 0.75rem 1rem;
}
.sidebar-link:hover {
  background-color: var(--tap-bg-hover);
  color: var(--tap-text-primary);
}
/* 高亮：原生 exact active 與自訂模組前綴 active 共用同一視覺 */
.sidebar-link.router-link-active,
.sidebar-link.active {
  background-color: var(--tap-primary);
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
  color: var(--tap-text-secondary);
  opacity: 0.85;
  border-top: 1px solid rgba(255, 255, 255, 0.15);
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
