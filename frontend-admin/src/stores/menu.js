import { defineStore } from "pinia";
import api from "@/plugins/axios.js";

/**
 * menu.js — 側邊欄動態選單 Store（DB 驅動）
 *
 * 選單唯一真相 = 後端 system_resource（GET /api/admin/menus，ADMIN_LOCAL）。
 * - 後端 getMenuTree 已依「登入者實際權限」過濾並回傳整棵樹（含 icon / 群組 children），
 *   前端只負責儲存與遞迴渲染（見 AdminSidebar / AdminSidebarNode），不再於前端寫死選單。
 * - 節點形狀：{ resourceId, resourceType(MENU/PAGE/BUTTON), name, urlPath, permissionId,
 *   sortOrder, icon, visible, children[] }。
 * - SUPER_ADMIN 在種子已被授予全部權限，故其選單樹自然完整，無需前端特例。
 *
 * 在 RBAC 資源頁改 name / icon / 排序 / 隱藏並重整後，側欄會即時反映（名實相符，非 no-op）。
 */
export const useMenuStore = defineStore("menu", {
  state: () => ({
    menus: [], // 後端回傳的選單樹（已依權限過濾）
    loaded: false,
  }),

  actions: {
    /** 載入 Admin 側欄選單樹（後端依權限過濾）。進入後台時呼叫。 */
    async fetchMenus() {
      try {
        const { data } = await api.get("/api/admin/menus");
        // controller 直接回傳陣列；亦容錯 { data: [...] } 包裝
        this.menus = Array.isArray(data) ? data : data?.data ?? [];
      } catch {
        // 後端未就緒：保留空樹，不注入假資料（頁面仍可由網址直達，路由守衛獨立把關）
        this.menus = [];
      } finally {
        this.loaded = true;
      }
    },

    /** 登出時重置，避免下一位登入者短暫看到上一位的選單 */
    reset() {
      this.menus = [];
      this.loaded = false;
    },
  },
});
