import { defineStore } from 'pinia';
import axios from '@/plugins/axios.js';

// B2B 後台動態選單唯一來源。
// 來源：GET /api/resources/menu?portalType=B2B_PORTAL
//   回傳已被後端依「當前使用者權限」過濾後的選單樹，
//   節點形狀：{ resourceId, resourceType(MENU/PAGE/BUTTON), name, urlPath, permissionId, sortOrder, children }
//
// 用途：
//   1. 側欄 (B2bSidebar) 依此樹渲染可見頁面（取代寫死的選單）。
//   2. 路由守衛 (router/index.js) 以此樹作為「可見頁面」唯一真相做 gating。
//
// 設計：採 fetch-then-guard，尚未載入完成前不得誤判為無權限。
export const useMenuStore = defineStore('menu', {
  state: () => ({
    menuTree: [],       // 後端回傳的選單樹（已依當前組織角色過濾）
    menuLoaded: false,  // 是否已成功載入過一次（守衛據此避免誤判）
    currentOrgId: null  // [Jason] RBAC #4b：選單已是 per-org，記住已載入的組織以便換組織時重新拉取
  }),

  getters: {
    // 攤平整棵樹所有節點的 urlPath（用於路徑比對 / gating）
    allPaths: (state) => {
      const paths = [];
      const walk = (nodes) => {
        for (const n of nodes || []) {
          if (n.urlPath) paths.push(n.urlPath);
          if (n.children?.length) walk(n.children);
        }
      };
      walk(state.menuTree);
      return paths;
    },

    // [Jason] 組織根落點用：依顯示順序（sortOrder）走訪可見選單樹，回傳第一個有 urlPath
    // 的頁（PAGE 葉節點）原始 urlPath（含 :organizerId 佔位符）。無可見頁時回 null。
    // 選單樹已由後端依「當前組織角色」過濾，故第一個有權限的頁即為此角色最合理的落點
    // （如驗票員只剩 /scan → 自然落在掃碼頁，不再被寫死 redirect 丟到無權限的 /themes）。
    firstAccessiblePath: (state) => {
      const sortNodes = (nodes) =>
        [...(nodes || [])].sort((a, b) => (a.sortOrder ?? 0) - (b.sortOrder ?? 0));
      const walk = (nodes) => {
        for (const n of sortNodes(nodes)) {
          if (n.urlPath) return n.urlPath;
          const child = walk(n.children);
          if (child) return child;
        }
        return null;
      };
      return walk(state.menuTree);
    }
  },

  actions: {
    // 載入 B2B 後台選單樹（依該組織內角色過濾）。force=true 強制重新拉取。
    // [Jason] RBAC #4b：選單已 per-org，需帶 organizerId；換組織（與已載入的不同）時自動重新拉取。
    async fetchMenu(organizerId, force = false) {
      console.log('[menuStore fetchMenu] called with organizerId:', organizerId, 'force:', force);
      console.log('[menuStore fetchMenu] current state - menuLoaded:', this.menuLoaded, 'currentOrgId:', this.currentOrgId, 'menuTree size:', this.menuTree?.length);
      if (this.menuLoaded && !force && this.currentOrgId === organizerId) {
        console.log('[menuStore fetchMenu] returning cached menuTree:', this.menuTree);
        return this.menuTree;
      }
      try {
        console.log('[menuStore fetchMenu] sending API request to /api/resources/menu...');
        const res = await axios.get('/api/resources/menu', {
          params: { portalType: 'B2B_PORTAL', organizerId }
        });
        // 後端可能回傳 { data: [...] } 或直接陣列，兩者皆容錯
        const tree = Array.isArray(res.data) ? res.data : (res.data?.data || []);
        console.log('[menuStore fetchMenu] API response received, items:', tree.length);
        this.menuTree = tree;
        this.menuLoaded = true;
        this.currentOrgId = organizerId;
        console.log('[RBAC Menu Debug] 載入選單路徑成功，可用路徑清單：', this.allPaths);
      } catch (e) {
        console.error('載入 B2B 選單失敗', e);
        // 後端無回應時沿用已持久化的選單，避免誤判無權限；不注入假資料
      }
      return this.menuTree;
    },

    // [Jason] 去掉 /org/{ORGxxxxxxx 或 :organizerId} 前綴，取得「組織內相對路徑」，
    // 使選單 urlPath(/org/:organizerId/...) 與實際網址(/org/ORG0000001/...) 能一致比對。
    _stripOrg(p) {
      return (p || '').replace(/^\/org\/[^/]+/, '') || '/';
    },

    // [Jason] 將選單 urlPath 的 :organizerId 佔位符換成實際組織 ID，供側欄產生可點連結。
    resolvePath(urlPath, organizerId) {
      if (!urlPath) return urlPath;
      return organizerId ? urlPath.replace(':organizerId', organizerId) : urlPath;
    },

    // [Jason] 路由守衛用：目標路徑是否落在使用者可見選單樹內。
    // 去組織前綴後做前綴相符（比舊版「比對最末段」精準，避免不同頁面末段同名而誤判）。
    isPathAllowed(targetPath) {
      if (!targetPath) return false;
      const target = this._stripOrg(targetPath);
      return this.allPaths.some((menuPath) => {
        const menu = this._stripOrg(menuPath);
        return target === menu || target.startsWith(menu + '/') || menu.startsWith(target + '/');
      });
    },

    // [Jason] 側欄用：某導覽項（可能是含子頁的前綴，如 /org/ID/team）是否有任一可見頁面。
    // fail-open：選單尚未載入完成時回 true，沿用既有「先顯示」行為，避免閃爍或誤藏可用頁面。
    hasMenuAccess(fullPath) {
      if (!this.menuLoaded) return true;
      const target = this._stripOrg(fullPath);
      return this.allPaths.some((menuPath) => {
        const menu = this._stripOrg(menuPath);
        return menu === target || menu.startsWith(target + '/') || target.startsWith(menu + '/');
      });
    }
  },

  // 持久化選單樹，重整 / 換頁時可同步還原，避免側欄閃爍與守衛誤判。
  // 刻意不保存 menuLoaded，讓每個 session 首次進入仍會向後端刷新一次。
  persist: {
    pick: ['menuTree']
  }
});
