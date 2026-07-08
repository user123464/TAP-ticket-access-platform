/**
 * 合約相關常數 — 與 SQL Schema Contracts 一致
 */

/** 合約類型 */
export const CONTRACT_TYPE = {
  STANDARD: 1, // 公版（線上簽署，.md 範本 + 版本快照）
  CUSTOM: 2, // 客製（線下紙本簽約，上傳 .md + PDF）
};

export const CONTRACT_TYPE_META = {
  [CONTRACT_TYPE.STANDARD]: { label: "公版合約", variant: "info" },
  [CONTRACT_TYPE.CUSTOM]: { label: "客製合約", variant: "primary" },
};

/** 合約狀態 */
export const CONTRACT_STATUS = {
  DRAFT: 0, // 草稿（未生效）
  ACTIVE: 1, // 生效中
  EXPIRED: 2, // 已到期
  TERMINATED: 3, // 已終止
};

export const CONTRACT_STATUS_META = {
  [CONTRACT_STATUS.DRAFT]: { label: "草稿", variant: "secondary" },
  [CONTRACT_STATUS.ACTIVE]: { label: "生效中", variant: "success" },
  [CONTRACT_STATUS.EXPIRED]: { label: "已到期", variant: "warning" },
  [CONTRACT_STATUS.TERMINATED]: { label: "已終止", variant: "danger" },
};

/** 抽成費率類型 */
export const FEE_TYPE = {
  PERCENTAGE: 1, // 交易額百分比抽成
  FIXED: 2, // 每筆固定金額
};

export const FEE_TYPE_META = {
  [FEE_TYPE.PERCENTAGE]: { label: "百分比抽成", unit: "%" },
  [FEE_TYPE.FIXED]: { label: "每筆固定", unit: "元/筆" },
};

/** 費率顯示文字 */
export function formatFee(feeType, feeValue) {
  const meta = FEE_TYPE_META[feeType];
  if (!meta) return "—";
  return `${feeValue}${meta.unit}`;
}
