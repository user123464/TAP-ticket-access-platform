/**
 * 活動相關常數 — 與 SQL Schema Themes/Events 一致
 *
 * Admin 對活動為「唯讀查詢」，僅需狀態顯示對照，不含操作。
 */
export const EVENT_STATUS = {
  DRAFT: 0, // 草稿
  PENDING: 1, // 審核中
  PUBLISHED: 2, // 已發布（售票中）
  ENDED: 3, // 已結束
  CANCELLED: 4, // 已取消
};

export const EVENT_STATUS_META = {
  [EVENT_STATUS.DRAFT]: { label: "草稿", variant: "secondary" },
  [EVENT_STATUS.PENDING]: { label: "審核中", variant: "warning" },
  [EVENT_STATUS.PUBLISHED]: { label: "售票中", variant: "success" },
  [EVENT_STATUS.ENDED]: { label: "已結束", variant: "secondary" },
  [EVENT_STATUS.CANCELLED]: { label: "已取消", variant: "danger" },
};
