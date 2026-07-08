/**
 * 使用者相關常數 — 與 SQL Schema Users 一致
 */

/** 登入方式 */
export const AUTH_PROVIDER_META = {
  LOCAL: { label: "帳密註冊", variant: "secondary" },
  GOOGLE: { label: "Google", variant: "info" },
};

/** 帳號狀態（由 is_active / locked_until / is_deleted 綜合判斷） */
export function resolveUserStatus(user) {
  if (user.isDeleted) return { label: "已刪除", variant: "secondary" };
  if (user.lockedUntil && new Date(user.lockedUntil) > new Date()) {
    return { label: "鎖定中", variant: "warning" };
  }
  if (!user.isActive) return { label: "已停用", variant: "danger" };
  return { label: "正常", variant: "success" };
}
