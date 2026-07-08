/**
 * usePdfExport.js — 將 Markdown 內容輸出為 PDF（透過瀏覽器列印）
 *
 * 作法：以 markdown-it 將 .md 渲染成 HTML，置入一個隱藏 iframe，
 *       在 iframe 內呼叫 window.print()，使用者即可於列印對話框
 *       選擇「另存為 PDF」。向量排版、無外部依賴、跨瀏覽器可用。
 *
 * 列印樣式（無邊框/背景、純黑字、自動分頁）內嵌於 iframe 文件，
 * 與主畫面深色主題隔離，確保輸出為適合文件的淺色版面。
 *
 * 用法：
 *   const { printMarkdownToPdf } = usePdfExport();
 *   printMarkdownToPdf("公版合約範本 v3", contentMd.value);
 */
import MarkdownIt from "markdown-it";

const md = new MarkdownIt({ html: false, linkify: true, breaks: false });

// iframe 內文件的列印樣式：A4 邊距、純黑字、表格框線、避免標題孤行。
const PRINT_STYLES = `
  @page { margin: 20mm; }
  * { box-sizing: border-box; }
  body {
    font-family: "Noto Sans TC", "Microsoft JhengHei", system-ui, sans-serif;
    color: #000;
    background: #fff;
    line-height: 1.7;
    font-size: 12pt;
  }
  h1, h2, h3, h4 { color: #000; margin: 1.2em 0 0.6em; page-break-after: avoid; }
  h1 { font-size: 20pt; }
  h2 { font-size: 16pt; }
  h3 { font-size: 14pt; }
  p, li { orphans: 3; widows: 3; }
  table { width: 100%; border-collapse: collapse; margin: 1em 0; page-break-inside: avoid; }
  th, td { border: 1px solid #444; padding: 6px 10px; text-align: left; }
  blockquote { border-left: 3px solid #999; margin: 1em 0; padding-left: 1em; color: #333; }
  code { background: #f2f2f2; padding: 1px 4px; border-radius: 3px; }
  hr { border: none; border-top: 1px solid #ccc; margin: 1.5em 0; }
  .doc-title { font-size: 22pt; font-weight: 700; margin-bottom: 1.5em; text-align: center; }
`;

const escapeHtml = (s) =>
  String(s ?? "")
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;");

export function usePdfExport() {
  /**
   * 將 Markdown 內容輸出為可列印 / 另存 PDF 的文件。
   * @param {string} title    文件標題（顯示於頂部與瀏覽器列印標頭）
   * @param {string} mdContent Markdown 原始內容
   */
  const printMarkdownToPdf = (title, mdContent) => {
    const bodyHtml = md.render(mdContent ?? "");

    const iframe = document.createElement("iframe");
    iframe.setAttribute("aria-hidden", "true");
    iframe.style.position = "fixed";
    iframe.style.right = "0";
    iframe.style.bottom = "0";
    iframe.style.width = "0";
    iframe.style.height = "0";
    iframe.style.border = "0";
    document.body.appendChild(iframe);

    const doc = iframe.contentWindow.document;
    doc.open();
    doc.write(`<!DOCTYPE html>
<html lang="zh-Hant">
<head>
  <meta charset="utf-8" />
  <title>${escapeHtml(title)}</title>
  <style>${PRINT_STYLES}</style>
</head>
<body>
  ${title ? `<div class="doc-title">${escapeHtml(title)}</div>` : ""}
  ${bodyHtml}
</body>
</html>`);
    doc.close();

    // 等待 iframe 內容渲染完成再列印，並於列印對話框關閉後移除 iframe
    const win = iframe.contentWindow;
    const cleanup = () => {
      // 延遲移除，避免部分瀏覽器在列印尚未送出前就銷毀文件
      setTimeout(() => iframe.remove(), 500);
    };
    win.addEventListener("afterprint", cleanup);

    iframe.onload = () => {
      win.focus();
      win.print();
      // 部分瀏覽器不觸發 afterprint，加上保底清理
      setTimeout(cleanup, 60000);
    };
  };

  return { printMarkdownToPdf };
}
