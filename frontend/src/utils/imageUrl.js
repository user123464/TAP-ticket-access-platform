/**
 * 圖片 URL 解析工具
 * 將相對路徑 /api/... 轉換成完整網址
 */
import axios from '@/plugins/axios'

const BASE_URL = axios.defaults.baseURL || window.location.origin

export function resolveImageUrl(url) {
  if (!url) return ''

  // 已經是完整網址
  if (url.startsWith('http')) {
    return url
  }

  // 避免重複 /api
  if (url.startsWith('/api')) {
    return BASE_URL.replace(/\/$/, '') + url
  }

  // 一般相對路徑
  return `${BASE_URL}${url.startsWith('/') ? '' : '/'}${url}`
}