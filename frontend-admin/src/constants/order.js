/**
 * 訂單相關常數 — 與 SQL Schema Orders 一致
 *
 * Admin 對訂單為「唯讀查詢」，供客服查詢，不含操作。
 */
export const ORDER_STATUS = {
  PENDING: 0, // 待付款
  PAID: 1, // 已付款
  COMPLETED: 2, // 已完成（已使用/已入場）
  CANCELLED: 3, // 已取消
  REFUNDED: 4, // 已退款
};

export const ORDER_STATUS_META = {
  [ORDER_STATUS.PENDING]: { label: "待付款", variant: "warning" },
  [ORDER_STATUS.PAID]: { label: "已付款", variant: "success" },
  [ORDER_STATUS.COMPLETED]: { label: "已完成", variant: "info" },
  [ORDER_STATUS.CANCELLED]: { label: "已取消", variant: "secondary" },
  [ORDER_STATUS.REFUNDED]: { label: "已退款", variant: "danger" },
};
