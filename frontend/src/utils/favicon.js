// 動態 favicon 切換
// B2C 前台與 B2B 廠商後台共用同一個 Vite app（同一份 index.html），
// 無法用靜態 <link> 給兩者不同的圖示，因此改由路由在執行期抽換 <link rel="icon">。
// B2C → logo.png、B2B → logo-light.png（Admin 為獨立 node，走各自的靜態 favicon）。
import b2cIcon from "@/assets/images/logo.png";
import b2bIcon from "@/assets/images/logo-light.png";

/**
 * 依情境更新瀏覽器分頁圖示。
 * @param {boolean} isB2B 是否為 B2B 廠商情境（/org 開頭路由）
 */
export function updateFavicon(isB2B) {
  const href = isB2B ? b2bIcon : b2cIcon;
  let link = document.querySelector("link[rel~='icon']");
  if (!link) {
    link = document.createElement("link");
    link.rel = "icon";
    document.head.appendChild(link);
  }
  link.type = "image/png";
  link.href = href;
}
