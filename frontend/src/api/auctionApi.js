import axios from '@/plugins/axios'

// 前台: 取得某活動的所有競標商品
export async function getAuctions(themeId) {
    const res = await axios.get(`/api/themes/${themeId}/auctions`)
    return res.data?.data
}

// 取得單一競標商品資料
export async function getAuctionById(auctionId) {
    const res = await axios.get(`/api/auctions/${auctionId}`)
    return res.data?.data ?? null
}

// 取得目前競標價格
export async function getAuctionCurrentPrice(auctionId) {
    const res = await axios.get(`/api/auctions/${auctionId}/current-price`)
    return res.data?.data?.currentPrice ?? null
}

// 取得競標紀錄（出價列表）
export async function getBidHistory(auctionId) {
    const res = await axios.get(`/api/auctions/${auctionId}/bids`)
    return res.data?.data ?? []
}

// 出價
// 注意：userId 應由後端從 JWT 取得，若後端尚未支援，需在此補上
export async function placeBid(auctionId, bidPrice) {
    const res = await axios.post(`/api/auctions/${auctionId}/bids`, { bidPrice })
    return res.data?.data
}

// ==================== 後台管理 API ====================

// 取得廠商的所有競標
export async function getOrgAuctions(themeId) {
    const res = await axios.get(`/api/org/themes/${themeId}/auctions`)
    return res.data?.data ?? []
}

// 新增競標（DRAFT 狀態）
export async function createAuction(themeId, payload) {
    const res = await axios.post(`/api/org/themes/${themeId}/auctions`, payload)
    return res.data?.data
}

// 編輯競標（僅限 DRAFT 狀態）
export async function updateAuction(auctionId, payload) {
    const res = await axios.put(`/api/org/auctions/${auctionId}`, payload)
    return res.data?.data
}

// 發佈競標（DRAFT → ACTIVE）
export async function publishAuction(auctionId) {
    const res = await axios.put(`/api/org/auctions/${auctionId}/publish`)
    return res.data?.data
}

// 結束競標（ACTIVE → ARCHIVED）
export async function endAuction(auctionId) {
    const res = await axios.put(`/api/org/auctions/${auctionId}/end`)
    return res.data?.data
}

// 刪除競標（改狀態為 DELETED）
export async function deleteAuction(auctionId) {
    const res = await axios.delete(`/api/org/auctions/${auctionId}`)
    return res.data?.data
}

export async function uploadAuctionImage(auctionId, file) {
    const formData = new FormData();
    formData.append("file", file);
    formData.append("auctionId", auctionId);

    const res = await axios.post("/api/org/auctions/upload-image", formData, {
        headers: {
            "Content-Type": "multipart/form-data"
        }
    });

    return res.data;
}

// 建立競標出價事件串流（SSE）
export function createBidEventSource(auctionId) {
    const baseUrl = import.meta.env.VITE_BACKEND_API
    const url = `${baseUrl}/api/auctions/${auctionId}/bids/stream`
    return new EventSource(url)
}
