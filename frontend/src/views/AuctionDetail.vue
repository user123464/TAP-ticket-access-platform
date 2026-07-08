<template>
    <div class="auction-detail-page">

        <!-- 載入中 -->
        <div v-if="loading" class="text-center py-5 text-muted">載入競標資訊中…</div>
        <template v-else>
            <div class="row container auction-shell px-3 px-sm-4 px-lg-5 g-3 align-items-stretch">
                <!-- 主資訊卡 -->
                <div class="col-12 col-lg-6 auction-info-card shadow-sm mb-3">
                    <h1 ref="titleEl" class="auction-title mb-2" :style="{ fontSize: titleFontSize + 'px' }">{{ auction.title }}</h1>
                    <p class="auction-detail text-muted mb-4">{{ auction.detail }}</p>
                    <!-- 商品圖片 -->
                    <div v-if="auction.image" class="auction-hero-img">
                        <img :src="resolveImageUrl(auction.image)" :alt="auction.title" />
                    </div>
                    <div class="d-flex align-items-center gap-2 pt-3 mb-3">
                        <span class="auction-badge">限定競標</span>
                        <span class="status-badge" :class="statusClass">{{ statusLabel }}</span>
                    </div>
                    <div class="d-flex flex-wrap gap-4">
                        <!-- 目前價格 -->
                        <div>
                            <div class="info-label">目前價格</div>
                            <div class="info-price">
                                $ {{ currentPrice?.toLocaleString() ?? auction.startPrice?.toLocaleString() ?? '—' }}
                            </div>
                        </div>
                        <!-- 剩餘時間 -->
                        <div>
                            <div class="info-label">{{ timeLabel }}</div>
                            <div class="info-time fw-bold" :class="{ 'text-danger': isUrgent }">
                                {{ timeValue }}
                            </div>
                        </div>
                        <!-- 直接買斷 -->
                        <div v-if="auction.buyoutPrice">
                            <div class="info-label">直接買斷</div>
                            <div class="info-buyout">$ {{ auction.buyoutPrice?.toLocaleString() }}</div>
                        </div>
                    </div>
                </div>

                <div class="col-12 col-lg-6 d-flex flex-column gap-3">
                    <!-- 出價區 & 競標紀錄 -->
                    <div class="bid-card shadow-sm">
                        <!-- 出價區 -->
                        <div v-if="isActive" class="pb-3 mb-3 border-bottom">
                            <h5 class="fw-bold mb-3"><i class="bi bi-cash-stack me-1"></i>出價</h5>
                            <div class="d-flex flex-column flex-sm-row gap-2 align-items-stretch align-items-sm-start">
                                <div class="flex-grow-1">
                                    <input v-model.number="bidAmount" type="number" class="form-control"
                                        :placeholder="`最低出價：$ ${minBid?.toLocaleString()}`" :min="minBid"
                                        :max="auction.buyoutPrice ?? undefined"
                                        :disabled="submitting" />
                                    <div v-if="bidError" class="text-danger small mt-1">{{ bidError }}</div>
                                </div>
                                <button class="btn btn-primary fw-bold rounded-pill px-4 flex-shrink-0 bid-submit-btn"
                                    :disabled="submitting || isMyLatestBid || awaitingBidConfirm"
                                    @click="submitBid">
                                    {{ submitting ? '送出中…' : awaitingBidConfirm ? '等待確認…' : isMyLatestBid ? '已是最高出價' : '立即出價' }}
                                </button>
                            </div>
                            <div v-if="isMyLatestBid" class="text-muted small mt-2">
                                目前最高出價已是你的出價，暫時無法再次出價。
                            </div>
                            <div v-if="outbidNotice" class="alert alert-danger border-0 shadow-sm rounded-4 mt-3 mb-0 py-3 outbid-alert" role="alert">
                                <div class="fw-bold mb-1">{{ outbidNotice.title }}</div>
                                <div class="small">{{ outbidNotice.message }}</div>
                            </div>
                            <div v-if="bidSuccess" class="alert alert-success border-0 shadow-sm rounded-4 mt-3 mb-0 py-3 success-alert" role="alert">
                                <div class="fw-bold mb-1">{{ bidSuccessTitle }}</div>
                                <div class="small">{{ bidSuccessMessage }}</div>
                            </div>
                        </div>
                        <div v-else-if="isEnded" class="pb-3 mb-3 border-bottom text-center text-muted">
                            競標已結束
                        </div>
                        <div v-else class="pb-3 mb-3 border-bottom text-center text-muted">
                            競標尚未開始
                        </div>
                        <!-- 競標紀錄 -->
                        <div>
                            <h5 class="fw-bold mb-3"><i class="bi bi-list-ul me-1"></i>競標紀錄</h5>

                        <div v-if="bidHistory.length === 0" class="text-muted text-center py-3 small">
                            目前尚無出價紀錄
                        </div>

                            <ul v-else class="list-unstyled mb-0">
                                <li v-for="(bid, i) in bidHistory" :key="bid.bidId ?? i"
                                    class="d-flex justify-content-between align-items-center py-2 border-bottom">
                                    <span class="text-muted small">{{ bid.userName ?? '匿名用戶' }}</span>
                                    <span class="fw-bold">$ {{ bid.bidPrice?.toLocaleString() }}</span>
                                    <span class="text-muted small">{{ formatDateTime(bid.createdAt) }}</span>
                                </li>
                            </ul>
                        </div>
                    </div>
                </div>


            </div>
        </template>

    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, nextTick } from 'vue'
import { useRoute } from 'vue-router'
import { useAuthStore } from '@/stores/auth.js'
import { useConfirm } from "@/composables/useConfirm.js"

const { confirm } = useConfirm()
import { getAuctionCurrentPrice, getBidHistory, placeBid, getAuctionById, createBidEventSource } from '@/api/auctionApi'
import { resolveImageUrl } from '@/utils/imageUrl'

const route = useRoute()
const auctionId = route.params.auctionId
const authStore = useAuthStore()

// ── 資料狀態 ──────────────────────────────
// 優先從路由 state 取（AuctionCard 導航時夾帶），否則頁面載入後再打 API
const auction = ref(window.history.state?.auction ?? null)
const loading = ref(!auction.value?.auctionId)
const currentPrice = ref(null)
const bidHistory = ref([])

// 標題自適應為單行（縮小字體直到可以一行顯示）
const titleEl = ref(null)
const titleFontSize = ref(32) // 初始值（px）
const TITLE_MIN_FONT = 14
const TITLE_MAX_FONT = 36
let titleResizeObserver = null

// ── 出價表單 ──────────────────────────────
const bidAmount = ref(null)
const bidError = ref('')
const bidSuccess = ref(false)
const bidSuccessTitle = ref('')
const bidSuccessMessage = ref('')
const awaitingBidConfirm = ref(false)
const submitting = ref(false)
const outbidNotice = ref(null)

// ── 倒數計時 ──────────────────────────────
const tick = ref(0)
let clockTimer = null

// ── SSE 即時連線 ──────────────────────────
let eventSource = null
let historyRefreshTimer = null

// ── 計算屬性 ──────────────────────────────
const isActive = computed(() => auction.value?.status === 'ACTIVE')
const isEnded = computed(() => auction.value?.status === 'ARCHIVED')

const statusLabel = computed(() => {
    switch (auction.value?.status) {
        case 'ACTIVE': return '競標進行中'
        case 'ARCHIVED': return '已結束'
        default: return '尚未開始'
    }
})

const statusClass = computed(() => ({
    'badge-active': isActive.value,
    'badge-ended': isEnded.value,
    'badge-pending': !isActive.value && !isEnded.value,
}))

// 每秒由 tick 觸發重新計算
const remainingMs = computed(() => {
    void tick.value
    if (!auction.value?.endTime) return 0
    return new Date(auction.value.endTime) - Date.now()
})

const isUrgent = computed(() => remainingMs.value > 0 && remainingMs.value < 10 * 60 * 1000)

const isMoreThanOneDay = computed(() => remainingMs.value > 24 * 60 * 60 * 1000)

const isBuyoutReached = computed(() => {
    if (auction.value?.buyoutPrice == null || currentPrice.value == null) return false
    return Number(currentPrice.value) >= Number(auction.value.buyoutPrice)
})

const timeLabel = computed(() => (isMoreThanOneDay.value ? '截標日期' : '剩餘時間'))

const remainingLabel = computed(() => {
    const ms = remainingMs.value
    if (ms <= 0) return '已結束'
    const h = Math.floor(ms / 3600000)
    const m = Math.floor((ms % 3600000) / 60000)
    const s = Math.floor((ms % 60000) / 1000)
    if (h > 0) return `${h}:${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
    return `${String(m).padStart(2, '0')}:${String(s).padStart(2, '0')}`
})

const timeValue = computed(() => {
    if (remainingMs.value <= 0) return '已結束'
    return isMoreThanOneDay.value ? formatShortDateTime(auction.value?.endTime) : remainingLabel.value
})

// 最低出價 = 目前最高價 + 1
const minBid = computed(() => Number(currentPrice.value ?? auction.value?.startPrice ?? 0) + 1)
const maxBid = computed(() => auction.value?.buyoutPrice ?? null)
const latestBid = computed(() => bidHistory.value[0] ?? null)
const isMyLatestBid = computed(() => {
    const latest = latestBid.value
    if (!latest?.userId || !authStore.userId) return false
    return latest.userId === authStore.userId && Number(latest.bidPrice) === Number(currentPrice.value)
})

function syncAuctionLifecycle() {
    if (!auction.value) return

    if (remainingMs.value <= 0 || isBuyoutReached.value) {
        auction.value.status = 'ARCHIVED'
        stopSse()
    }
}

// ── 生命週期 ──────────────────────────────
onMounted(async () => {
    if (!auction.value?.auctionId) {
        auction.value = await getAuctionById(auctionId).catch(() => null)
    }
    loading.value = false

    // 並行取得初始價格與出價紀錄
    const [price, history] = await Promise.all([
        getAuctionCurrentPrice(auctionId).catch(() => null),
        getBidHistory(auctionId).catch(() => []),
    ])
    currentPrice.value = price
    bidHistory.value = history
    syncAuctionLifecycle()

    clockTimer = setInterval(() => {
        tick.value++
        syncAuctionLifecycle()
    }, 1000)
    startSse()

    // 等待 DOM 更新後啟動標題自適應
    await nextTick()
    adjustTitleFont()
    try {
        const parent = titleEl.value?.parentElement
        if (parent) {
            titleResizeObserver = new ResizeObserver(adjustTitleFont)
            titleResizeObserver.observe(parent)
        }
        window.addEventListener('resize', adjustTitleFont)
    } catch {}
})

onUnmounted(() => {
    clearInterval(clockTimer)
    stopSse()

    // 清理標題自適應的 observer 與事件
    try {
        titleResizeObserver?.disconnect()
        titleResizeObserver = null
        window.removeEventListener('resize', adjustTitleFont)
    } catch {}
})


// 調整標題字體：保持單行顯示，透過不停調小字體直到不換行
function adjustTitleFont() {
    try {
        const el = titleEl.value
        if (!el) return

        const container = el.parentElement
        if (!container) return

        // 強制單行測量
        el.style.whiteSpace = 'nowrap'
        el.style.lineHeight = '1'

        // 以目前 fontSize 為起點，向下或向上微調至最接近的可容納大小
        let font = titleFontSize.value || TITLE_MAX_FONT
        font = Math.max(Math.min(font, TITLE_MAX_FONT), TITLE_MIN_FONT)
        el.style.fontSize = font + 'px'

        // 若內容超出容器寬度就逐步縮小（每次 1px）
        while (el.scrollWidth > container.clientWidth && font > TITLE_MIN_FONT) {
            font -= 1
            el.style.fontSize = font + 'px'
        }

        // 若有空間，嘗試逐步放大到最大值（避免過小顯示）
        while (el.scrollWidth <= container.clientWidth && font < TITLE_MAX_FONT) {
            // 先嘗試放大 1px，若放大後溢位則回退並停止
            const next = font + 1
            el.style.fontSize = next + 'px'
            if (el.scrollWidth > container.clientWidth) {
                el.style.fontSize = font + 'px'
                break
            }
            font = next
        }

        titleFontSize.value = font
    } catch {}
}

// ── 出價 ──────────────────────────────────
async function submitBid() {
    bidError.value = ''
    bidSuccess.value = false
    bidSuccessTitle.value = ''
    bidSuccessMessage.value = ''
    outbidNotice.value = null

    if (!bidAmount.value || bidAmount.value < minBid.value) {
        bidError.value = `出價金額需大於目前最高價（$ ${(minBid.value - 1).toLocaleString()}）`
        return
    }

    if (maxBid.value != null && bidAmount.value > maxBid.value) {
        bidError.value = `出價金額高於直購價，請改用直購價（$ ${maxBid.value.toLocaleString()}）`
        return
    }

    const confirmMessage = maxBid.value != null && bidAmount.value === maxBid.value
        ? '確認要以直購價出價嗎？'
        : `確認是否以輸入金額 $ ${bidAmount.value.toLocaleString()} 出價？`

    const ok = await confirm({
        title: '確認出價',
        message: confirmMessage,
        confirmText: '確定',
        variant: 'primary'
    })
    if (!ok) {
        return
    }

    submitting.value = true
    try {
        await placeBid(auctionId, bidAmount.value)
        bidAmount.value = null
        bidSuccess.value = true
        bidSuccessTitle.value = '出價已送出'
        bidSuccessMessage.value = '出價已受理，等待即時確認…'
        awaitingBidConfirm.value = true
        scheduleBidHistoryRefresh(500)
        setTimeout(() => {
            if (awaitingBidConfirm.value) {
                bidSuccess.value = false
                bidSuccessTitle.value = ''
                bidSuccessMessage.value = ''
            }
        }, 5000)
    } catch (err) {
        bidError.value = err.response?.data?.message ?? '出價失敗，請稍後再試'
    } finally {
        submitting.value = false
    }
}

// ── SSE 即時更新 ──────────────────────────
// bid-update 事件 payload 格式：{ currentPrice, latestBid: { bidId, userName, bidPrice, createdAt } }
function startSse() {
    stopSse()
    try {
        eventSource = createBidEventSource(auctionId)

        eventSource.addEventListener('error', () => {
            stopSse()
            setTimeout(startSse, 3000) // 3 秒後重連
        })

        eventSource.addEventListener('bid-update', (e) => {
            try {
                const payload = JSON.parse(e.data)
                const wasMyLatestBid = isMyLatestBid.value
                const previousPrice = Number(currentPrice.value ?? auction.value?.startPrice ?? 0)
                const isMyBidEvent = payload.latestBid?.userId === authStore.userId
                    && Number(payload.latestBid?.bidPrice ?? payload.currentPrice) === Number(payload.currentPrice)

                // 更新目前最高價
                if (payload.currentPrice != null) {
                    currentPrice.value = payload.currentPrice
                }

                if (payload.currentPrice != null && auction.value?.buyoutPrice != null
                    && Number(payload.currentPrice) >= Number(auction.value.buyoutPrice)) {
                    auction.value.status = 'ARCHIVED'
                }

                // 後端此版本會先推 SSE，再背景寫 DB。
                // 因為 payload 可能還沒有 bidId，所以等一下再重新抓歷史紀錄。
                awaitingBidConfirm.value = false
                bidSuccess.value = true

                if (wasMyLatestBid && payload.currentPrice != null && Number(payload.currentPrice) > previousPrice) {
                    bidSuccess.value = false
                    bidSuccessTitle.value = ''
                    bidSuccessMessage.value = ''
                    outbidNotice.value = {
                        title: '你的出價已被超過',
                        message: `目前最高價已更新為 $ ${Number(payload.currentPrice).toLocaleString()}，請立即調整出價。`,
                    }
                } else if (isMyBidEvent) {
                    outbidNotice.value = null
                    bidSuccess.value = true
                    bidSuccessTitle.value = '你現在是最高出價'
                    bidSuccessMessage.value = `目前最高價為 $ ${Number(payload.currentPrice ?? currentPrice.value ?? auction.value?.startPrice ?? 0).toLocaleString()}，請留意是否有人再次追價。`
                } 

                scheduleBidHistoryRefresh(300)
                syncAuctionLifecycle()
            } catch { }
        })
    } catch { }
}

function stopSse() {
    eventSource?.close()
    eventSource = null
    clearTimeout(historyRefreshTimer)
    historyRefreshTimer = null
}

function scheduleBidHistoryRefresh(delay = 400) {
    clearTimeout(historyRefreshTimer)
    historyRefreshTimer = setTimeout(async () => {
        const history = await getBidHistory(auctionId).catch(() => null)
        if (history) {
            bidHistory.value = history

            if (isMyLatestBid.value) {
                outbidNotice.value = null
            }
        }
    }, delay)
}

function formatDateTime(dt) {
    if (!dt) return ''
    return dt.replace('T', ' ').slice(0, 16)
}

function formatShortDateTime(dt) {
    if (!dt) return ''
    const value = dt.replace('T', ' ')
    return value.slice(5, 16)
}
</script>

<style scoped>
.auction-detail-page {
    background:
        radial-gradient(circle at top, rgba(229, 115, 70, 0.12), transparent 30%),
        linear-gradient(180deg, var(--tap-light-white) 0%, #ffffff 60%);
    min-height: auto;
    padding: 0.75rem 0 0;
}

.auction-hero-img {
    width: 100%;
    height: 300px;
    min-height: 200px;
    overflow: hidden;
    background: var(--tap-light-white);
}

.auction-hero-img img {
    width: 100%;
    height: 100%;
    object-fit: contain;
    background: var(--tap-light-white);
}

.auction-shell {
    max-width: 1000px;
    margin: 0 auto;
    padding-top: 1rem;
}

.auction-shell > [class*='col-'] {
    min-width: 0;
}

.auction-info-card,
.bid-card,
.bid-history-card {
    background: var(--tap-light-white);
    border: 1px solid var(--tap-light-gray);
    border-radius: 1.5rem;
    padding: 1.25rem;
}

.auction-badge {
    background: var(--tap-primary);
    color: #fff;
    font-size: 1rem;
    letter-spacing: 0.06em;
    padding: 0.2em 0.75em;
    border-radius: 999px;
}

.status-badge {
    font-size: 1rem;
    padding: 0.2em 0.75em;
    border-radius: 999px;
    font-weight: 600;
}

.badge-active {
    background: #d1fae5;
    color: #065f46;
}

.badge-ended {
    background: #fee2e2;
    color: #991b1b;
}

.badge-pending {
    background: #fef3c7;
    color: #92400e;
}

.auction-title {
    font-size: clamp(1.5rem, 4vw, 2.2rem);
    font-weight: 800;
    color: var(--tap-dark-blue);
}

.auction-detail {
    font-size: 1rem;
    line-height: 1.75;
}

.info-label {
    font-size: 0.78rem;
    color: var(--tap-light-blue);
    font-weight: 700;
    letter-spacing: 0.1em;
    text-transform: uppercase;
    margin-bottom: 0.2rem;
}

.info-price {
    font-size: 2rem;
    font-weight: 800;
    color: var(--tap-dark-blue);
    line-height: 1.1;
}

.info-time {
    font-size: 1rem;
    color: var(--tap-light-blue);
    font-variant-numeric: tabular-nums;
    line-height: 1.2;
    white-space: nowrap;
}

.info-buyout {
    font-size: 1rem;
    color: var(--tap-light-blue);
    font-weight: 600;
    white-space: nowrap;
}

.bid-card ul {
    max-height: 260px;
    overflow-y: auto;
    padding-right: 0.25rem;
}

.outbid-alert {
    background: linear-gradient(135deg, #dc3545 0%, #b91c1c 100%);
    color: #fff;
}

.outbid-alert :deep(.small) {
    color: rgba(255, 255, 255, 0.92);
}

.success-alert {
    background: linear-gradient(135deg, #16a34a 0%, #15803d 100%);
    color: #fff;
}

.success-alert :deep(.small) {
    color: rgba(255, 255, 255, 0.92);
}

.bid-submit-btn {
    width: 100%;
}

@media (max-width: 575.98px) {
    .auction-detail-page {
        padding-top: 0.5rem;
    }

    .auction-shell {
        padding-top: 0.25rem;
    }

    .auction-info-card,
    .bid-card,
    .bid-history-card {
        padding: 1rem;
        border-radius: 1.1rem;
    }

    .auction-hero-img {
        height: 180px;
        min-height: 180px;
    }

    .auction-title {
        font-size: 1.45rem;
    }

    .auction-detail {
        font-size: 0.95rem;
        line-height: 1.65;
    }

    .info-price {
        font-size: 1.7rem;
    }

    .info-label,
    .info-time,
    .info-buyout,
    .status-badge,
    .auction-badge {
        font-size: 0.85rem;
    }

    .d-flex.flex-wrap.gap-4 > div {
        min-width: calc(50% - 1rem);
    }

    .bid-card ul {
        max-height: 220px;
    }
}

@media (min-width: 576px) {
    .bid-submit-btn {
        width: auto;
    }
}
</style>
