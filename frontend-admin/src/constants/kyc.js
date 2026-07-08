/**
 * KYC 狀態常數 — 與 SQL Schema Organizers.kyc_status 一致
 */
export const KYC_STATUS = {
  DRAFT: 0, // 草稿（尚未提交）
  PENDING: 1, // 待審核
  APPROVED: 2, // 已通過
  REJECTED: 3, // 已退件
};

/** 對應 StatusBadge 的顯示設定 */
export const KYC_STATUS_META = {
  [KYC_STATUS.DRAFT]: { label: "草稿", variant: "secondary" },
  [KYC_STATUS.PENDING]: { label: "待審核", variant: "warning" },
  [KYC_STATUS.APPROVED]: { label: "已通過", variant: "success" },
  [KYC_STATUS.REJECTED]: { label: "已退件", variant: "danger" },
};
