<script setup>
// 【開發備註 / Note for future refactoring】
// 此文件的排版未來還需要進一步修改調整。

import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import MarkdownIt from 'markdown-it';
import api from '@/plugins/axios';
import { useSmartBack } from '@/composables/useSmartBack';

const route = useRoute();
const router = useRouter();

// 智慧返回：回到使用者真正的上一頁（無站內歷史時退回語境首頁）
const { goBack } = useSmartBack();

// 將標題文字轉為 URL-safe 的 slug（支援中文）
const slugify = (text) => {
  return text
    .trim()
    .toLowerCase()
    .replace(/\s+/g, '-')
    .replace(/[^\w\u4e00-\u9fff-]/g, '')
    .replace(/--+/g, '-');
};

// 初始化 markdown-it，並自訂 heading_open 規則以自動加入 id 屬性
const md = new MarkdownIt({
  html: true,
  linkify: true,
  typographer: true
});

// 覆寫 heading_open 渲染器，讓每個 h1/h2/h3 自動帶上 id
const defaultHeadingOpen = md.renderer.rules.heading_open ||
  function (tokens, idx, options, env, self) {
    return self.renderToken(tokens, idx, options);
  };

md.renderer.rules.heading_open = function (tokens, idx, options, env, self) {
  const token = tokens[idx];
  const level = parseInt(token.tag.substring(1)); // 取得標題層級數字

  // 只處理 h1, h2, h3
  if (level <= 3) {
    // 下一個 token 是 inline 文字內容
    const contentToken = tokens[idx + 1];
    if (contentToken && contentToken.children) {
      const text = contentToken.children
        .filter(t => t.type === 'text' || t.type === 'code_inline')
        .map(t => t.content)
        .join('');
      const slug = slugify(text);
      token.attrSet('id', slug);
    }
  }

  return defaultHeadingOpen(tokens, idx, options, env, self);
};

// 判斷是否為 B2B 語境
const isB2b = computed(() => route.path.startsWith('/org'));

// 從路由參數中取得當前的文件類型 (privacy, terms, guide)
const activeDocType = computed(() => route.params.docType || 'privacy');

// 文件庫配置 (動態語境標題)
const docs = computed(() => ({
  privacy: {
    title: isB2b.value ? '個人資料保護聲明' : '隱私權政策',
    icon: 'bi-shield-lock-fill'
  },
  terms: {
    title: isB2b.value ? '主辦方服務條款' : '服務條款',
    icon: 'bi-file-earmark-text-fill'
  },
  guide: {
    title: '主辦方使用說明',
    icon: 'bi-journal-bookmark-fill'
  }
}));

const currentRawContent = ref('');
const isLoading = ref(false);
const showLoadingState = ref(false);
const fetchError = ref('');
let loadingTimeout = null;

// 追蹤最後一次請求的文件類型，防止競態問題
let latestRequestedDoc = '';

// 抓取 Markdown 內容
const fetchDocument = async (docType) => {
  latestRequestedDoc = docType;
  isLoading.value = true;
  fetchError.value = '';
  
  if (loadingTimeout) {
    clearTimeout(loadingTimeout);
  }
  
  loadingTimeout = setTimeout(() => {
    if (isLoading.value && latestRequestedDoc === docType) {
      showLoadingState.value = true;
    }
  }, 200);

  try {
    const response = await api.get(`/api/documents/${docType}.md`);
    if (latestRequestedDoc !== docType) return;
    
    if (response.data && response.data.success) {
      currentRawContent.value = response.data.content;
    } else {
      fetchError.value = response.data?.message || '無法載入文件內容';
    }
  } catch (error) {
    if (latestRequestedDoc !== docType) return;
    console.error('Fetch document failed', error);
    fetchError.value = '載入文件時發生錯誤，請稍後再試。';
  } finally {
    if (latestRequestedDoc === docType) {
      isLoading.value = false;
      if (loadingTimeout) {
        clearTimeout(loadingTimeout);
        loadingTimeout = null;
      }
      showLoadingState.value = false;
      nextTick(() => handleScroll());
    }
  }
};

// 依語境動態過濾側邊欄選單
const menuItems = computed(() => {
  const baseItems = [
    { type: 'privacy', ...docs.value.privacy },
    { type: 'terms', ...docs.value.terms }
  ];
  if (isB2b.value) {
    // B2B 額外顯示使用說明，並排在最前面
    baseItems.unshift({ type: 'guide', ...docs.value.guide });
  }
  return baseItems;
});

// 取得已處理語境名稱的 Markdown 內容
const processedContent = computed(() => {
  if (currentRawContent.value) {
    let content = currentRawContent.value;
    if (isB2b.value) {
      content = content.replace(/^# 隱私權政策/, '# 個人資料保護聲明');
      content = content.replace(/^# 服務條款/, '# 主辦方服務條款');
    }
    return content;
  }
  return '';
});

// Markdown 渲染後的 HTML 內容
const htmlContent = computed(() => {
  if (processedContent.value) {
    return md.render(processedContent.value);
  }
  return '';
});

// 使用 markdown-it 的 token 解析出標題清單，用於右側目錄（保證與渲染出的 HTML 標題 ID 100% 一致）
const tocItems = computed(() => {
  if (!processedContent.value) return [];

  const tokens = md.parse(processedContent.value, {});
  const items = [];

  for (let i = 0; i < tokens.length; i++) {
    const token = tokens[i];
    if (token.type === 'heading_open') {
      const level = parseInt(token.tag.substring(1)); // h1, h2, h3 的數字
      if (level <= 3) {
        const inlineToken = tokens[i + 1];
        if (inlineToken && inlineToken.children) {
          const text = inlineToken.children
            .filter(t => t.type === 'text' || t.type === 'code_inline')
            .map(t => t.content)
            .join('');
          const slug = slugify(text);
          items.push({ level, text, slug });
        }
      }
    }
  }

  return items;
});

// 當前高亮的目錄項目 slug (scroll spy)
const activeSlug = ref('');

// 滾動監聽：偵測當前可視區域的標題，高亮對應的目錄項
const handleScroll = () => {
  const headings = document.querySelectorAll('.markdown-body h1[id], .markdown-body h2[id], .markdown-body h3[id]');
  if (!headings.length) return;

  let current = '';
  for (const heading of headings) {
    const rect = heading.getBoundingClientRect();
    // 當標題頂部進入視窗上方 120px 以內時，認為此標題為當前段落
    if (rect.top <= 120) {
      current = heading.id;
    }
  }
  activeSlug.value = current;
};

// 點擊目錄項目，平滑滾動到對應標題
const scrollToHeading = (slug) => {
  const el = document.getElementById(slug);
  if (el) {
    el.scrollIntoView({ behavior: 'smooth', block: 'start' });
    // 手動設定高亮
    activeSlug.value = slug;
  }
};

// 切換選單的路由處理
const selectDoc = (type) => {
  if (isB2b.value) {
    router.replace(`/org/docs/${type}`);
  } else {
    router.replace(`/docs/${type}`);
  }
};

onMounted(() => {
  window.addEventListener('scroll', handleScroll, { passive: true });
  // 局部選取器設定，僅在此頁面掛載時啟用滾動條穩定
  document.documentElement.classList.add('stable-scrollbar');
  fetchDocument(activeDocType.value);
});

onUnmounted(() => {
  window.removeEventListener('scroll', handleScroll);
  // 離開頁面時，清除類別，還原全域網頁的正常設定
  document.documentElement.classList.remove('stable-scrollbar');
  if (loadingTimeout) {
    clearTimeout(loadingTimeout);
  }
});

// 切換文件時，重新獲取內容並重置目錄高亮
watch(activeDocType, (newType) => {
  activeSlug.value = '';
  fetchDocument(newType);
});
</script>

<template>
  <div class="container-fluid py-4 px-3 px-md-4">

    <div class="row g-4">
      <!-- 左側導覽選單 (col-md-3) -->
      <div class="col-12 col-md-3 col-xl-2">
        <!-- 行動版返回按鈕 + 下拉式選單 -->
        <div class="d-md-none mb-3">
          <button type="button" @click="goBack" class="btn btn-sm btn-back rounded-3 fw-semibold d-inline-flex align-items-center justify-content-center gap-2 mb-3 w-100">
            <i class="bi bi-arrow-left"></i> 返回
          </button>
          <label class="form-label fw-bold text-secondary small">選擇閱讀文件</label>
          <select 
            class="form-select focus-ring" 
            :value="activeDocType" 
            @change="e => selectDoc(e.target.value)"
          >
            <option v-for="item in menuItems" :key="item.type" :value="item.type">
              {{ item.title }}
            </option>
          </select>
        </div>

        <!-- 桌面版左側導覽 (統一包裹在 sticky 容器中，確保滾動時不分離) -->
        <div class="d-none d-md-block sidebar-sticky">
          <!-- 返回按鈕 -->
          <div class="mb-3">
            <button
              type="button"
              @click="goBack"
              class="btn btn-back btn-sm w-100 rounded-3 fw-semibold d-flex align-items-center justify-content-center gap-2 py-2 shadow-sm"
            >
              <i class="bi bi-arrow-left"></i>
              返回
            </button>
          </div>

          <!-- 側邊選單卡片 -->
          <div class="card border rounded-4 shadow-sm p-3 bg-white">
            <!-- 頂部標題 -->
            <div class="fw-bold text-dark fs-6 pb-2 mb-2" style="border-bottom: 1px solid var(--tap-light-gray, #e2e8f0);">
              文件導覽
            </div>
            <!-- 文件選單清單 (使用 RouterLink 以利新分頁開啟等原生行為) -->
            <div class="list-group list-group-flush border-0">
              <RouterLink
                v-for="item in menuItems"
                :key="item.type"
                :to="isB2b ? `/org/docs/${item.type}` : `/docs/${item.type}`"
                replace
                :title="item.title"
                class="list-group-item list-group-item-action d-flex align-items-center gap-3 border-0 py-2.5 px-3 rounded-3 my-1 transition-all"
                :class="activeDocType === item.type ? 'active-menu' : 'text-secondary'"
              >
                <i class="bi" :class="item.icon"></i>
                <span class="fw-semibold">{{ item.title }}</span>
              </RouterLink>
            </div>
          </div>
        </div>
      </div>

      <!-- 中間文件內容區 (col-md-7) -->
      <div class="col-12 col-md-7 col-xl-8">
        <div class="card border rounded-4 shadow-sm p-4 p-md-5 bg-white position-relative" style="min-height: 400px;">
          <!-- 加載中遮罩 (絕對定位覆蓋) -->
          <div v-if="showLoadingState" class="loading-overlay d-flex flex-column align-items-center justify-content-center">
            <div class="spinner-border text-primary mb-3" role="status">
              <span class="visually-hidden">Loading...</span>
            </div>
            <div class="text-secondary small fw-semibold">正在載入文件...</div>
          </div>
          
          <!-- 文件內容包裝器 (在載入時變透明、模糊，但不卸載以維持高度) -->
          <div class="document-content-wrapper" :class="{ 'content-loading': showLoadingState }">
            <!-- 錯誤狀態 -->
            <div v-if="fetchError" class="text-center text-danger py-5">
              <i class="bi bi-exclamation-circle" style="font-size: 2rem;"></i>
              <div class="mt-2">{{ fetchError }}</div>
              <button class="btn btn-sm btn-outline-primary mt-3" @click="fetchDocument(activeDocType)">重試</button>
            </div>

            <!-- 文件渲染區 -->
            <template v-else>
              <div class="markdown-body" v-html="htmlContent"></div>
              
              <div class="border-top pt-4 mt-5 d-flex justify-content-between align-items-center text-muted small">
                <span>系統自動更新</span>
                <span>TAP 售票系統</span>
              </div>
            </template>
          </div>
        </div>
      </div>

      <!-- 右側快速檢索目錄 (col-md-2, 桌面版才顯示) -->
      <div class="col-md-2 d-none d-md-block">
        <div class="toc-sidebar sidebar-sticky">
          <div class="toc-header">
            <i class="bi bi-list-nested me-1"></i>快速檢索
          </div>
          <nav class="toc-nav" v-if="tocItems.length">
            <a
              v-for="item in tocItems"
              :key="item.slug"
              :href="'#' + item.slug"
              class="toc-link"
              :class="[
                `toc-level-${item.level}`,
                { 'toc-active': activeSlug === item.slug }
              ]"
              @click.prevent="scrollToHeading(item.slug)"
              :title="item.text"
            >
              {{ item.text }}
            </a>
          </nav>
          <div v-else class="small text-muted px-3 py-2">
            此文件無標題項目
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped>
/* 側邊選單高亮樣式 */
.active-menu {
  background-color: var(--tap-primary, #e57346) !important;
  color: #ffffff !important;
}

.list-group-item {
  transition: all 0.2s ease;
  overflow: hidden;
}

.list-group-item:hover:not(.active-menu) {
  background-color: #f8fafc;
  color: var(--tap-primary, #e57346) !important;
}

.list-group-item span {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex-grow: 1;
  min-width: 0;
}

/* 返回按鈕樣式 */
.btn-back {
  background-color: #ffffff;
  border: 1px solid var(--tap-light-gray, #e2e8f0);
  color: var(--tap-light-blue, #64748b);
  transition: all 0.2s ease-in-out;
}

.btn-back:hover {
  background-color: #f8fafc;
  border-color: var(--tap-primary, #e57346);
  color: var(--tap-primary, #e57346);
  transform: translateX(-2px);
}

/* 左側選單與右側 TOC 的 sticky 定位 */
.sidebar-sticky {
  position: sticky;
  top: 90px; /* 鎖定在接近初始渲染時的視窗高度，避免捲動時與 Navbar 擠壓，維持視覺位置恆定 */
}

/* ─── 右側目錄 (TOC) 樣式 ─── */
.toc-sidebar {
  max-height: calc(100vh - 130px); /* 配合 top 90px 調整，保留適當的底部安全邊距與完整滾動範圍 */
  overflow-y: auto;
}

.toc-header {
  font-size: 1rem; /* 與左側文件導覽一致，改為標準 fs-6 大小 */
  font-weight: 700;
  color: var(--tap-dark-blue, #1e293b);
  padding: 0 0.25rem 0.5rem;
  border-bottom: 1px solid var(--tap-light-gray, #e2e8f0);
  margin-bottom: 0.5rem;
}

.toc-nav {
  display: flex;
  flex-direction: column;
  gap: 1px;
}

.toc-link {
  display: block;
  font-size: 0.875rem; /* 使用本站標準 small 尺寸，增強易讀性 */
  line-height: 1.5;
  color: var(--tap-light-blue, #64748b);
  text-decoration: none;
  padding: 6px 12px;
  border-left: 2px solid transparent;
  border-radius: 0 4px 4px 0;
  transition: all 0.15s ease;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.toc-link:hover {
  color: var(--tap-primary, #e57346);
  background-color: rgba(229, 115, 70, 0.05);
}

.toc-link.toc-active {
  color: var(--tap-primary, #e57346);
  border-left-color: var(--tap-primary, #e57346);
  font-weight: 600;
  background-color: rgba(229, 115, 70, 0.06);
}

/* 目錄層級縮排與視覺層次 */
.toc-level-1 {
  font-weight: 600;
  color: var(--tap-dark-blue, #1e293b);
  padding-left: 8px;
}
.toc-level-2 {
  padding-left: 20px;
}
.toc-level-3 {
  padding-left: 32px;
  color: #94a3b8;
}

/* 美化 TOC 滾動條 */
.toc-sidebar::-webkit-scrollbar {
  width: 3px;
}
.toc-sidebar::-webkit-scrollbar-track {
  background: transparent;
}
.toc-sidebar::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.15);
  border-radius: 3px;
}

/* ─── Markdown 排版美化 ─── */
.markdown-body :deep(h1) {
  font-size: 1.8rem;
  font-weight: 700;
  margin-top: 0;
  margin-bottom: 1.5rem;
  padding-bottom: 0.5rem;
  border-bottom: 2px solid #f0f0f0;
  color: #1e293b;
}

.markdown-body :deep(h2) {
  font-size: 1.4rem;
  font-weight: 700;
  margin-top: 2rem;
  margin-bottom: 1rem;
  padding-bottom: 0.3rem;
  border-bottom: 1px solid #eaecef;
  color: #1e293b;
}

.markdown-body :deep(h3) {
  font-size: 1.15rem;
  font-weight: 700;
  margin-top: 1.5rem;
  margin-bottom: 0.8rem;
  color: #334155;
}

.markdown-body :deep(p) {
  line-height: 1.7;
  margin-bottom: 1.2rem;
  color: #475569;
}

.markdown-body :deep(ul), .markdown-body :deep(ol) {
  padding-left: 2rem;
  margin-bottom: 1.2rem;
  line-height: 1.7;
  color: #475569;
}

.markdown-body :deep(li) {
  margin-bottom: 0.4rem;
}

.markdown-body :deep(strong) {
  font-weight: 700;
  color: #0f172a;
}

/* 僅在 html 帶有該 class 時套用此樣式 */
:global(html.stable-scrollbar) {
  scrollbar-gutter: stable;
}

/* 加載中半透明遮罩 */
.loading-overlay {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.7);
  z-index: 10;
  border-radius: 1rem;
  transition: opacity 0.3s ease;
}

/* 內容區過渡動畫：變透明、些微模糊，維持原本高度防塌陷 */
.document-content-wrapper {
  transition: opacity 0.25s ease-in-out, filter 0.25s ease-in-out;
  opacity: 1;
  filter: blur(0);
}

.document-content-wrapper.content-loading {
  opacity: 0.15;
  filter: blur(1px);
}
</style>
