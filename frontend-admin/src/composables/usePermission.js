import { useAuthStore } from "@/stores/auth.js";

/**
 * usePermission — 前端按鈕級 RBAC 判斷（Phase 4 示範）
 *
 * 權限來源：登入後路由守衛呼叫 authStore.fetchMe() → GET /api/auth/me
 *   回傳 `permissions`（扁平的權限碼陣列，如 ["USER_VIEW","USER_EDIT",...]，
 *   即該帳號各角色展開後的 GrantedAuthority，去掉 ROLE_ 前綴）。
 *
 * 用途：在頁面內以 v-if="can('USER_EDIT')" 控制動作鈕顯隱。
 *   後端本就以 @PreAuthorize 在方法層 403 enforce；前端先藏 = 沒權限的人
 *   連按鈕都看不到，避免「點了才被打回」的挫折感，視覺與後端一致。
 *
 * reactive：can() 於 render 期間讀取 authStore.user（reactive），
 *   權限變動（重新 fetchMe / 切換帳號）時依賴它的模板會自動重算。
 */
export function usePermission() {
  const auth = useAuthStore();

  /**
   * 是否具備某權限碼。純讀 auth.user.permissions（後端展開各角色後的權限清單）。
   * 不再對 SUPER_ADMIN 開捷徑——授權一律以權限碼為準，與後端 @PreAuthorize 同一真相源；
   * SUPER_ADMIN 的全權應由其角色實際持有完整權限碼體現，而非前端硬編。
   */
  const can = (code) => {
    if (!code) return false;
    if (auth.isSuperAdmin) return true;
    return (auth.user?.permissions ?? []).includes(code);
  };

  return { can };
}
