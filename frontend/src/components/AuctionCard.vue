<template>
    <div class="auction-card rounded-4 border border-2 shadow-sm overflow-hidden">
        <!-- 商品圖片 -->
        <div v-if="auction.image" class="auction-card-img-box">
            <img :src="resolveImageUrl(auction.image)" :alt="auction.title" />
        </div>

        <div class="p-3 d-flex flex-column gap-2">
            <!-- 狀態徽章 + 標題 -->
            <div class="d-flex align-items-center gap-2">
                <span class="badge auction-badge">限定競標</span>
                <span class="fw-bold text-dark text-truncate">{{ auction.title }}</span>
            </div>

            <!-- 價格 & 剩餘時間 -->
            <div class="d-flex justify-content-between align-items-end">
                <div>
                    <div class="text-muted small">目前出價</div>
                    <div class="auction-price fw-bold">
                        $ {{ currentPrice?.toLocaleString() ?? auction.startPrice?.toLocaleString() ?? '—' }}
                    </div>
                </div>
                <div class="text-end">
                    <div class="text-muted small">剩餘時間</div>
                    <div class="auction-time fw-semibold" :class="{ 'text-danger': isUrgent }">
                        {{ remainingLabel }}
                    </div>
                </div>
            </div>

            <button
                class="btn btn-outline-primary rounded-pill fw-bold w-100 mt-1"
                @click="goDetail"
            >
                <i class="bi bi-tag me-1"></i>查看詳情
            </button>
        </div>
    </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getAuctionCurrentPrice } from '@/api/auctionApi'
import { resolveImageUrl } from '@/utils/imageUrl'

const props = defineProps({
    auction: {
        type: Object,
        required: true
    }
})

const router = useRouter()
const route = useRoute()

const currentPrice = ref(null)

// 計算剩餘時間
const remainingMs = computed(() => {
    tick.value
    if (!props.auction.endTime) return 0
    return new Date(props.auction.endTime) - Date.now()
})

const isUrgent = computed(() => remainingMs.value > 0 && remainingMs.value < 10 * 60 * 1000)

const remainingLabel = computed(() => {
    const ms = remainingMs.value
    if (ms <= 0) return '已結束'
    const h = Math.floor(ms / 3600000)
    const m = Math.floor((ms % 3600000) / 60000)
    if (h > 0) return `${h} 小時 ${m} 分`
    const s = Math.floor((ms % 60000) / 1000)
    if (m > 0) return `${m} 分 ${s} 秒`
    return `${s} 秒`
})

// 強制重新計算 remainingMs（每秒更新顯示）
const tick = ref(0)
let clockTimer = null

onMounted(async () => {
    // 取得目前出價
    try {
        currentPrice.value = await getAuctionCurrentPrice(props.auction.auctionId)
    } catch {
        // 靜默失敗，顯示起標價
    }
    // 每秒更新剩餘時間顯示
    clockTimer = setInterval(() => tick.value++, 1000)
})

onUnmounted(() => {
    clearInterval(clockTimer)
})

function goDetail() {
    router.push({
        name: 'AuctionDetail',
        params: {
            themeId: route.params.themeId,
            auctionId: props.auction.auctionId
        },
        state: { auction: props.auction }
    })
}
</script>

<style scoped>
.auction-card {
    background: var(--tap-light-white);
    border-color: var(--tap-light-gray) !important;
    transition: box-shadow 0.2s;
}

.auction-card:hover {
    box-shadow: 0 4px 18px rgba(229, 115, 70, 0.18) !important;
}

.auction-card-img-box {
    width: 100%;
    height: 250px;
    overflow: hidden;
}

.auction-card-img-box img {
    width: 100%;
    height: 100%;
    object-fit: cover;
}

.auction-badge {
    background: var(--tap-primary);
    color: #fff;
    font-size: 0.7rem;
    letter-spacing: 0.06em;
    padding: 0.2em 0.65em;
    border-radius: 999px;
    white-space: nowrap;
}

.auction-price {
    font-size: 1.25rem;
    color: var(--tap-dark-blue);
}

.auction-time {
    font-size: 0.95rem;
    color: var(--tap-light-blue);
}
</style>
