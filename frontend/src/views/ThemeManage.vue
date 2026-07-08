<template>
  <div class="container-fluid">
    <div class="row align-items-start">

      <!--活動列表 -->
      <div class="col-12 col-md-4 border-end overflow-auto bg-light-subtle theme-list-sidebar">
        <!-- align-items-center 垂直置中 -->
        <div class="d-flex gap-2 align-items-center mb-3 mt-3">
          <h5 class="fw-bold mb-0">活動管理</h5>
          <!-- ThemeModal新增活動按鈕 -->
          <button class="btn btn-primary btn-sm rounded-pill px-3 ms-auto" @click="openThemeInsert">
            <i class="bi bi-plus-lg me-1"></i>建立活動
          </button>
        </div>

        <!-- 搜尋與篩選 -->
        <div class="mb-3">
          <div class="input-group input-group-sm mb-2">
            <span class="input-group-text bg-white border-end-0 text-muted">
              <i class="bi bi-search"></i>
            </span>
            <input type="text" class="form-control border-start-0 ps-0" placeholder="搜尋活動名稱..."
              v-model="themeSearchQuery">
          </div>
          <ThemeFilterGroup class="col-12" :options="statusOptions" v-model="store.themeFilter" />
        </div>

        <!-- 活動列表 -->
        <ul class="list-group list-group-flush rounded-3 border">
          <li v-for="theme in searchedThemes" :key="theme.themeId"
            class="list-group-item list-group-item-action d-flex justify-content-between align-items-start py-3 border-start-indicator"
            :class="{ active: store.selectedTheme?.themeId === theme.themeId }" @click="store.selectTheme(theme)">
            <!-- 左側資訊 -->
            <div class="overflow-hidden me-2">
              <div class="fw-bold text-truncate"
                :class="{ 'text-primary': store.selectedTheme?.themeId === theme.themeId }">
                {{ theme.title }}
              </div>
              <div>
                <span class="badge" :class="statusBadgeConfig[theme.status] || 'bg-secondary'"
                  style="font-size: 0.7rem;">
                  {{ theme.statusDesc }}
                </span>
              </div>
            </div>

            <!-- 右側操作 -->
            <div v-if="theme.status === 'DRAFT'" class="ms-2 d-flex gap-1 flex-shrink-0">
              <button class="btn btn-sm btn-outline-success fw-bolder" @click.stop="handlePublishTheme(theme.themeId)">公開</button>
              <button class="btn btn-sm btn-outline-warning fw-bolder text-dark" @click.stop="openThemeEdit(theme)">修改</button>
              <button class="btn btn-sm btn-outline-danger  fw-bolder" @click.stop="handleConfirmDeleteTheme(theme.themeId)">刪除</button>
            </div>
          </li>
        </ul>
      </div>

      <!-- 活動詳情-->
      <div class="col-12 col-md-8 py-3">
        <!-- 未選擇活動時的空狀態 -->
        <div v-if="!store.selectedTheme"
          class="h-100 d-flex flex-column align-items-center justify-content-center text-muted border rounded-4 bg-light shadow-sm"
          style="min-height: 400px;">
          <i class="bi bi-card-checklist fs-1 mb-3"></i>
          <p class="fw-bold">請從左側列表選擇一個活動以查看詳情</p>
        </div>

        <!-- 選中活動後顯示詳情卡片 -->
        <div v-else class="card border rounded-4 shadow-sm h-100">
          <!-- 活動標題區 -->
          <div class="card-header bg-white border-bottom-0 pt-4 px-4">
            <div class="d-flex flex-column flex-sm-row gap-3 align-items-start">
              <div v-if="store.selectedTheme.image" class="flex-shrink-0" style="width: 120px; height: 90px;">
                <img :src="resolveImageUrl(store.selectedTheme.image)" class="img-fluid rounded border shadow-sm"
                  style="width: 100%; height: 100%; object-fit: cover;" />
              </div>
              <div class="flex-grow-1" style="min-width: 0;">
                <div class="d-flex align-items-center gap-2 mb-2 flex-wrap">
                  <h3 class="fw-bold mb-0">{{ store.selectedTheme.title }}</h3>
                  <span class="badge" :class="statusBadgeConfig[store.selectedTheme.status] || 'bg-secondary'">
                    {{ store.selectedTheme.statusDesc }}
                  </span>
                </div>
                <p class="text-secondary mb-0">{{ store.selectedTheme.detail }}</p>
              </div>
            </div>
          </div>

          <div class="card-body px-4 pb-4">
            <hr class="my-4 opacity-25">

            <div ref="sessionSectionRef" class="session-section">
              <!-- 場次管理標題區 -->
              <div class="d-flex flex-column flex-sm-row justify-content-between align-items-start align-items-sm-center gap-2 mb-3">
                <h5 class="fw-bold mb-0"><i class="bi bi-calendar-event me-2 text-primary"></i>場次管理</h5>
                <div class="d-flex gap-2 w-100 w-sm-auto justify-content-between justify-content-sm-start">
                  <button class="btn btn-primary btn-sm rounded-pill px-3 flex-shrink-0" @click="openSessionInsert">
                    <i class="bi bi-plus-lg me-1"></i>建立場次
                  </button>
                  <button class="btn btn-sm btn-outline-secondary rounded-pill collapse-control flex-shrink-0"
                    @click="toggleSessionSection">
                    <i :class="['bi', sessionCollapsed ? 'bi-chevron-down' : 'bi-chevron-up']"></i>
                    {{ sessionCollapsed ? '展開' : '縮小' }}
                  </button>
                </div>
              </div>

              <div v-show="!sessionCollapsed">
                <!-- 篩選場次 -->
                <div class="mb-3">
                  <ThemeFilterGroup :options="statusOptions" v-model="store.sessionFilter" />
                </div>

                <!-- 場次列表 -->
                <div class="list-group list-group-flush border rounded-3 overflow-hidden">
                  <div v-for="session in store.filteredSessions" :key="session.sessionId"
                    class="list-group-item list-group-item-action p-3"
                    :class="{ 'bg-light border-start border-primary border-4': store.selectedSession?.sessionId === session.sessionId }"
                    @click="store.selectSession(session)">

                    <div class="d-flex flex-column flex-sm-row justify-content-between align-items-stretch align-items-sm-start gap-2">
                      <!-- 左側場次資訊 -->
                      <div class="flex-grow-1">
                        <div class="row g-2">
                          <div class="col-12">
                            <div class="d-flex align-items-center gap-2 mb-0">
                              <span class="fw-bold fs-5 text-dark">{{ session.title }}</span>
                              <span class="badge" :class="statusBadgeConfig[session.status] || 'bg-secondary'">{{ session.statusDesc }}</span>
                            </div>
                          </div>
                          <div class="col-12 text-muted small">
                            <i class="bi bi-clock me-1"></i> 販售時間:{{ formatDateRange(session.sellingStartTime, session.sellingEndTime) }}
                          </div>
                          <div class="col-12 text-muted small">
                            <i class="bi bi-clock me-1"></i> 表演時間:{{ formatDateRange(session.startTime, session.endTime) }}
                          </div>
                          <div class="col-12 text-muted small">
                            <i class="bi bi-geo-alt me-1"></i> 地點：{{ session.locationName }}
                          </div>
                        </div>
                      </div>
                      <!-- 右側操作按鈕 -->
                      <div v-if="session.status === 'DRAFT'" class="ms-0 ms-sm-3 d-flex flex-row flex-sm-column gap-2 align-items-center align-items-sm-stretch mt-2 mt-sm-0 flex-wrap flex-shrink-0">
                        <div class="d-flex gap-1 flex-wrap">
                          <span v-if="store.selectedTheme?.status !== 'ACTIVE'" class="d-inline-block" title="活動未公開">
                            <button
                              class="btn btn-sm btn-outline-success fw-bolder"
                              disabled
                              @click.stop="handlePublishSession(session.sessionId)"
                            >公開</button>
                          </span>
                          <button
                            v-else
                            class="btn btn-sm btn-outline-success fw-bolder"
                            title="公開場次"
                            @click.stop="handlePublishSession(session.sessionId)"
                          >公開</button>
                          <button class="btn btn-sm btn-outline-warning  fw-bolder text-dark" @click.stop="openSessionEdit(session)">修改</button>
                          <button class="btn btn-sm btn-outline-danger  fw-bolder" @click.stop="handleConfirmDeleteSession(session.sessionId)">刪除</button>
                        </div>
                        <button class="btn btn-sm btn-outline-warning w-100 fw-bolder text-dark text-nowrap" @click.stop="goTicketTypeEdit(session.sessionId)">管理座位</button>
                      </div>
                    </div>
                  </div>
                  <!-- 無資料狀態 -->
                  <div v-if="store.filteredSessions.length === 0" class="p-5 text-center text-muted small">
                    目前沒有符合篩選條件的場次資訊
                  </div>
                </div>
              </div>

              <hr class="my-4 opacity-25">

              <div class="auction-section">
                <!-- 競標管理標題區 -->
                <div class="d-flex flex-column flex-sm-row justify-content-between align-items-start align-items-sm-center gap-2 mb-3">
                  <h5 class="fw-bold mb-0"><i class="bi bi-hammer me-2 text-success"></i>競標管理</h5>
                  <div class="d-flex gap-2 w-100 w-sm-auto justify-content-between justify-content-sm-start">
                    <button class="btn btn-primary btn-sm rounded-pill px-3 flex-shrink-0" @click="openAuctionInsert">
                      <i class="bi bi-plus-lg me-1"></i>建立競標
                    </button>
                    <button class="btn btn-sm btn-outline-secondary rounded-pill collapse-control flex-shrink-0"
                      @click="toggleAuctionSection">
                      <i :class="['bi', auctionCollapsed ? 'bi-chevron-down' : 'bi-chevron-up']"></i>
                      {{ auctionCollapsed ? '展開' : '縮小' }}
                    </button>
                  </div>
                </div>

                <div v-show="!auctionCollapsed">
                  <!-- 篩選競標 -->
                  <div class="mb-3">
                    <ThemeFilterGroup :options="statusOptions" v-model="store.auctionFilter" />
                  </div>

                  <!-- 競標列表 -->
                  <div class="list-group list-group-flush border rounded-3 overflow-hidden">
                    <div v-for="auction in store.filteredAuctions" :key="auction.auctionId"
                      class="list-group-item list-group-item-action p-3">

                      <div class="d-flex flex-column flex-sm-row justify-content-between align-items-stretch align-items-sm-start gap-2">
                        <!-- 左側競標資訊 -->
                        <div class="flex-grow-1">
                          <div class="row g-2">
                            <div class="col-12">
                              <div class="d-flex align-items-center gap-2 mb-0">
                                <span class="fw-bold fs-5 text-dark">{{ auction.title }}</span>
                                <span class="badge" :class="statusBadgeConfig[auction.status] || 'bg-secondary'">{{ auction.statusDesc }}</span>
                              </div>
                            </div>
                            <div class="col-12 text-muted small">
                              <i class="bi bi-clock me-1"></i> 競標時間：{{ formatDateRange(auction.startTime, auction.endTime) }}
                            </div>
                            <div class="col-12 text-muted small">
                              <i class="bi bi-tag me-1"></i> 起標價：{{ formatPrice(auction.startPrice) }}
                            </div>
                          </div>
                        </div>

                        <!-- 右側操作按鈕 -->
                        <div v-if="auction.status === 'DRAFT'" class="ms-0 ms-sm-3 d-flex flex-row flex-sm-column gap-2 align-items-center align-items-sm-stretch mt-2 mt-sm-0 flex-wrap flex-shrink-0">
                          <div class="d-flex gap-1 flex-wrap">
                            <span v-if="store.selectedTheme?.status !== 'ACTIVE'" class="d-inline-block" title="活動未公開">
                              <button
                                class="btn btn-sm btn-outline-success fw-bolder"
                                disabled
                                @click.stop="handlePublishAuction(auction.auctionId)"
                              >公開</button>
                            </span>
                            <button
                              v-else
                              class="btn btn-sm btn-outline-success fw-bolder"
                              title="公開競標"
                              @click.stop="handlePublishAuction(auction.auctionId)"
                            >公開</button>
                            <button class="btn btn-sm btn-outline-warning fw-bolder text-dark" @click.stop="openAuctionEdit(auction)">修改</button>
                            <button class="btn btn-sm btn-outline-danger  fw-bolder"  @click.stop="handleConfirmDeleteAuction(auction.auctionId)">刪除</button>
                          </div>
                        </div>
                      </div>
                    </div>
                    <!-- 無資料狀態 -->
                    <div v-if="store.filteredAuctions.length === 0" class="p-5 text-center text-muted small">
                      目前沒有符合篩選條件的競標資訊
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <ThemeModal v-model:show="showThemeModal" :data="themeModalData" @submit="themeHandleSubmit" />
  <SessionModal v-model:show="showSessionModal" :data="sessionModalData" @submit="sessionHandleSubmit" />
  <AuctionModal v-model:show="showAuctionModal" :data="auctionModalData" @submit="auctionHandleSubmit" />

</template>

<script setup>
import { ref, onMounted, watch, nextTick } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useThemeStore } from '@/stores/themeStore'
import { resolveImageUrl } from '@/utils/imageUrl'
import ThemeModal from '@/components/ThemeModal.vue'
import SessionModal from '@/components/SessionModal.vue'
import AuctionModal from '@/components/AuctionModal.vue'
import ThemeFilterGroup from '@/components/ThemeFilterGroup.vue'
import { useConfirm } from '@/composables/useConfirm.js'

// 搜尋邏輯
import { computed } from 'vue'
const themeSearchQuery = ref('')
const searchedThemes = computed(() => {
  if (!themeSearchQuery.value) return store.filteredThemes
  return store.filteredThemes.filter(t =>
    t.title.toLowerCase().includes(themeSearchQuery.value.toLowerCase())
  )
})

const route = useRoute()
const store = useThemeStore()
const router = useRouter()
const { confirm } = useConfirm()
const organizerId = route.params.organizerId
const sessionSectionRef = ref(null)
const sessionCollapsed = ref(false)
const auctionCollapsed = ref(false)

//session modal===========================
const showSessionModal = ref(false)
const sessionModalData = ref(null)

function openSessionInsert() {
  sessionModalData.value = null
  showSessionModal.value = true
}

function openSessionEdit(session) {
  sessionModalData.value = session
  showSessionModal.value = true
}

function goTicketTypeEdit(sessionId) {
  const currentOrgId = route.params.organizerId;
  console.log("goTicketTypeEdit clicked. sessionId:", sessionId, "organizerId:", currentOrgId);
  if (!currentOrgId) {
    alert("錯誤：路由參數中缺少 organizerId！");
    return;
  }
  router.push({
    name: "TicketTypeEdit",
    params: {
      sessionId: sessionId,
      organizerId: currentOrgId
    }
  }).catch(err => {
    console.error("Vue Router push rejected:", err);
    alert("路由跳轉失敗 (Promise 被拒絕)：\n" + err.message + "\n" + JSON.stringify(err));
  });
}

async function handlePublishTheme(themeId) {
  try {
    const ok = await confirm({
      title: '確認公開',
      message: '確定要公開這個活動嗎？',
      confirmText: '公開',
      variant: 'primary'
    })
    if (!ok) return

    await store.publishTheme(themeId, organizerId)
  } catch (e) {
    alert(e?.message)
  }
}

async function handleConfirmDeleteTheme(themeId) {
  try {
    await store.confirmDeleteTheme(themeId, organizerId)
  } catch (e) {
    alert(e?.message)
  }
}

async function handlePublishSession(sessionId) {
  try {
    const ok = await confirm({
      title: '確認公開',
      message: '確定要公開這個場次嗎？',
      confirmText: '公開',
      variant: 'primary'
    })
    if (!ok) return

    await store.publishSession(sessionId)
  } catch (e) {
    alert(e?.message)
  }
}

async function handleConfirmDeleteSession(sessionId) {
  try {
    await store.confirmDeleteSession(sessionId)
  } catch (e) {
    alert(e?.message)
  }
}

function toggleSessionSection() {
  sessionCollapsed.value = !sessionCollapsed.value
}

async function sessionHandleSubmit(payload) {
  try {
    if (payload.sessionId) {
      // 修改
      await store.updateSession(payload, payload.sessionId)
    } else {
      // 新增
      await store.createSession(payload, store.selectedTheme.themeId)
    }
    showSessionModal.value = false
  } catch (e) {
    alert(e?.message)
  }
}

//auction modal===========================
const showAuctionModal = ref(false)
const auctionModalData = ref(null)

function openAuctionInsert() {
  auctionModalData.value = null
  showAuctionModal.value = true
}

function openAuctionEdit(auction) {
  auctionModalData.value = auction
  showAuctionModal.value = true
}

async function handlePublishAuction(auctionId) {
  try {
    const ok = await confirm({
      title: '確認公開',
      message: '確定要公開這個競標嗎？',
      confirmText: '公開',
      variant: 'primary'
    })
    if (!ok) return

    await store.publishAuction(auctionId)
  } catch (e) {
    alert(e?.message)
  }
}

function toggleAuctionSection() {
  auctionCollapsed.value = !auctionCollapsed.value
}

async function handleConfirmDeleteAuction(auctionId) {
  try {
    await store.confirmDeleteAuction(auctionId)
  } catch (e) {
    alert(e?.message)
  }
}

async function auctionHandleSubmit(payload) {
  try {
    const { pendingImageFile, ...auctionPayload } = payload

    let savedAuction = null

    if (auctionPayload.auctionId) {
      // 修改
      await store.updateAuction(auctionPayload, auctionPayload.auctionId)
      savedAuction = auctionPayload
    } else {
      // 新增
      savedAuction = await store.createAuction(auctionPayload, store.selectedTheme.themeId)
    }

    const auctionId = auctionPayload.auctionId || savedAuction?.auctionId

    if (pendingImageFile && auctionId) {
      await store.uploadAuctionImage(auctionId, pendingImageFile)
      await store.fetchAuctions(store.selectedTheme.themeId)
    }

    showAuctionModal.value = false
  } catch (e) {
    alert(e?.message)
  }
}


//theme modal===========================
const showThemeModal = ref(false)
const themeModalData = ref(null)

function openThemeInsert() {
  themeModalData.value = null
  showThemeModal.value = true
}

function openThemeEdit(theme) {
  themeModalData.value = theme
  showThemeModal.value = true
}

async function themeHandleSubmit(payload) {
  const { pendingImageFile, ...themePayload } = payload

  try {
    let savedTheme = null

    if (themePayload.themeId) {
      // 修改
      savedTheme = await store.updateTheme(themePayload, organizerId)
    } else {
      // 新增
      savedTheme = await store.createTheme(themePayload, organizerId)
    }

    const themeId = themePayload.themeId || savedTheme?.themeId

    if (pendingImageFile && themeId) {
      await store.uploadThemeImage(themeId, pendingImageFile)
      await store.fetchThemes(organizerId)
    }

    showThemeModal.value = false
  } catch (e) {
    console.error('themeHandleSubmit error', e)
    alert(e?.message)
  }
}

const statusBadgeConfig = {
  ACTIVE: 'bg-success',
  DRAFT: 'bg-warning text-dark',
  DELETED: 'bg-danger',
  ARCHIVED: 'bg-info text-dark'
}

//轉換時間格式===========================
function formatDateTime(dt) {
  if (!dt) return ''
  return dt.replace('T', ' ').slice(0, 16)
}

function formatDateRange(startDt, endDt) {
  if (!startDt || !endDt) return formatDateTime(startDt || endDt)

  const startDate = startDt.slice(0, 10)
  const endDate = endDt.slice(0, 10)
  const startTime = formatDateTime(startDt)
  const endTime = formatDateTime(endDt)

  if (startDate === endDate) {
    return `${startTime} ~ ${endTime.slice(-5)}`
  }

  return `${startTime} ~ ${endTime}`
}

function formatPrice(price) {
  if (!price && price !== 0) return ''
  return `NT$${Number(price).toLocaleString('zh-TW')}`
}

//======================================

const statusOptions = [
  { label: '全部', value: 'ALL' },
  { label: '草稿', value: 'DRAFT' },
  { label: '公開', value: 'ACTIVE' },
  { label: '已結束', value: 'ARCHIVED' },
  { label: '已刪除', value: 'DELETED' }
]

watch(
  () => store.selectedTheme?.themeId,
  async (newThemeId, oldThemeId) => {
    if (!newThemeId || newThemeId === oldThemeId) return
    if (window.innerWidth >= 768) return

    await nextTick()
    // 滾動到場次管理區塊
    // behavior: 'smooth'：平滑捲動，不是瞬間跳過去
    // block: 'start'：捲動到元素的頂部對齊視窗頂部
    sessionSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
)

onMounted(async () => {
  try {
    await store.fetchThemes(organizerId)
    // // 如果有主題，自動選擇第一個
    // if (store.themes && store.themes.length > 0) {
    //   await store.selectTheme(store.themes[0])
    // }
  } catch (e) {
    alert(e?.message)
  }
})

</script>
<style scoped>
@media (min-width: 768px) {
  .theme-list-sidebar {
    position: sticky;
    top: 1rem; 
    max-height: calc(87vh);
  }
}

.border-start-indicator {
  border-left: 4px solid transparent;
  transition: all 0.2s ease;
}

.list-group-item.active {
  background-color: #f1f5f9;
  border-left-color: #0d6efd;
  color: #212529;
  border-bottom-color: rgba(0, 0, 0, 0.125);
}

.session-section {
  scroll-margin-top: 1rem;
}
</style>