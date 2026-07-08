import * as XLSX from "xlsx";
import dayjs from "dayjs";

/**
 * useExport.js — 報表匯出（xlsx / SheetJS）
 *
 * 用法：
 *   const { exportExcel, exportCsv } = useExport();
 *   exportExcel("手續費明細", rows, { orgName: "廠商", fee: "手續費" });
 *
 * columns 為 { 資料key: 標題 } 對照，匯出時依此順序取欄並換成中文標題。
 */
export function useExport() {
  /** 依 columns 對照把 rows 轉成標題列 + 資料列的二維陣列 */
  const toSheetData = (rows, columns) => {
    const keys = Object.keys(columns);
    const header = keys.map((k) => columns[k]);
    const body = rows.map((row) => keys.map((k) => row[k] ?? ""));
    return [header, ...body];
  };

  const buildSheet = (rows, columns) => {
    const sheet = XLSX.utils.aoa_to_sheet(toSheetData(rows, columns));
    // 依標題長度估算欄寬，避免中文標題被截斷
    sheet["!cols"] = Object.values(columns).map((title) => ({ wch: Math.max(12, title.length * 2 + 4) }));
    return sheet;
  };

  const stampedName = (filename) => `${filename}_${dayjs().format("YYYYMMDD_HHmmss")}`;

  /** 匯出 .xlsx */
  const exportExcel = (filename, rows, columns, sheetName = "Sheet1") => {
    const book = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(book, buildSheet(rows, columns), sheetName);
    XLSX.writeFile(book, `${stampedName(filename)}.xlsx`);
  };

  /** 匯出 .csv（帶 BOM，Excel 開啟中文不亂碼） */
  const exportCsv = (filename, rows, columns) => {
    const book = XLSX.utils.book_new();
    XLSX.utils.book_append_sheet(book, buildSheet(rows, columns), "data");
    XLSX.writeFile(book, `${stampedName(filename)}.csv`, { bookType: "csv", FS: ",", forceUtf8: true });
  };

  return { exportExcel, exportCsv };
}
