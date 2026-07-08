/**
 * useActiveRoute.js — 側欄選單高亮的路徑匹配邏輯
 *
 * 背景：多個子頁籤／詳情頁（如 /admin/organizers/kyc）在路由設定中與其入口頁
 *       (/admin/organizers) 為「平級兄弟」而非父子巢狀，導致 Vue Router 原生的
 *       router-link-active 無法向父級入口傳遞高亮。
 *
 * 解法：以「功能主體前綴」判斷高亮——只要目前路由落在某入口所屬的模組前綴下，
 *       該入口側欄連結就保持啟用狀態。
 *
 * 匹配優先序：
 *   1. 精確相等
 *   2. 直屬子路徑（path + '/'）前綴
 *   3. 模組前綴對照表（集中設定，子頁籤/詳情頁皆保父級高亮）
 */
import { useRoute } from "vue-router";

// 模組前綴對照表：key＝側欄「可見入口」的 urlPath，value＝視為同模組的路由前綴清單。
// 目前路由若命中清單中任一前綴，該入口即保持高亮。
//
// 僅在「子頁路徑不延伸自入口路徑」時才需登記（否則 rule 2 的直屬子路徑前綴已涵蓋）。
// 注意：/admin/system 底下有兩個可見入口（通知與公告群、系統維護群），
//       不可用 /admin/system 做籠統前綴，否則兩個入口會同時高亮。
const MODULE_PREFIX_MAP = {
  // 客服與查詢：入口 submissions，含 events / orders 同模組
  "/admin/operations/submissions": ["/admin/operations"],
  // 訂閱與合約：入口 subscriptions，含 plans / contracts（路徑不延伸自入口）
  "/admin/billing/subscriptions": ["/admin/billing"],
  // 通知與公告群：入口 announcements，含 templates / notifications
  "/admin/system/announcements": [
    "/admin/system/announcements",
    "/admin/system/templates",
    "/admin/system/notifications",
  ],
  // 系統維護群：入口 jobs，含 dictionaries / config / media
  "/admin/system/jobs": [
    "/admin/system/jobs",
    "/admin/system/dictionaries",
    "/admin/system/config",
    "/admin/system/media",
  ],
};

export function useActiveRoute() {
  const route = useRoute();

  const isLinkActive = (path) => {
    if (!path) return false;
    const current = route.path;

    // 1. 精確相等
    if (current === path) return true;

    // 2. 直屬子路徑前綴
    if (current.startsWith(path + "/")) return true;

    // 3. 模組前綴對照表
    const prefixes = MODULE_PREFIX_MAP[path];
    if (prefixes && prefixes.some((p) => current === p || current.startsWith(p + "/"))) {
      return true;
    }

    return false;
  };

  return { isLinkActive };
}
