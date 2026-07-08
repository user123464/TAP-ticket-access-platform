/**
 * 組織相關常數 — 與 SQL Schema Organizers.status 一致
 */
export const ORG_STATUS = {
  ACTIVE: 0, // 正常營運
  SUSPENDED: 1, // 暫停（違規/欠費等，B2B 端功能鎖定）
  ARCHIVED: 2, // 封存（停業/解約，唯讀保留）
};

export const ORG_STATUS_META = {
  [ORG_STATUS.ACTIVE]: { label: "正常", variant: "success" },
  [ORG_STATUS.SUSPENDED]: { label: "暫停", variant: "warning" },
  [ORG_STATUS.ARCHIVED]: { label: "封存", variant: "secondary" },
};
