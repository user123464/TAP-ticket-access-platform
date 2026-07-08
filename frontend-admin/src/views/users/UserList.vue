<script setup>
/**
 * UserList.vue — 使用者列表（模組 2 ★ P1）
 *
 * 搜尋（Email/姓名）+ 篩選（帳號狀態 / Email 驗證 / auth_provider）+ 分頁（DataTable）。
 * API：GET /api/admin/users（後端未就緒時 fallback mock）
 */
import { ref, computed, onMounted } from "vue";
import dayjs from "dayjs";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import BaseAvatar from "@/components/common/BaseAvatar.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import { AUTH_PROVIDER_META, resolveUserStatus } from "@/constants/user.js";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";

const { setAnnouncement } = useSystemBanner();
const loadError = ref(false);

// SWR 快取：建立時立刻帶回上次列表（不閃白），背景再 refresh 取最新
// （Email 屬營運必要欄位、可快取；不含身分證/銀行等敏感詳情）
const { data: rows, isLoading, refresh } = useCachedResource(
  "admin:users",
  () =>
    api.get("/api/admin/users").then((r) => {
      const list = r.data.data ?? [];
      return list.map((u) => ({
        ...u,
        status: resolveUserStatus(u).label,
      }));
    }),
  { initial: [] }
);
// spinner 只在「完全沒有快取可顯示」時出現
const loading = computed(() => isLoading.value && (rows.value?.length ?? 0) === 0);

// 篩選條件
const keyword = ref(""); // Email / 姓名
const filterStatus = ref(""); // '' 全部 / active / locked / disabled / deleted
const filterVerified = ref(""); // '' 全部 / true / false
const filterProvider = ref(""); // '' 全部 / LOCAL / GOOGLE
const fromDate = ref(""); // 註冊時間起
const toDate = ref("");   // 註冊時間迄
const lastLoginFrom = ref(""); // 最後登入起
const lastLoginTo = ref("");   // 最後登入迄

const hasExtraFilters = computed(() => {
  return !!filterStatus.value ||
         !!filterVerified.value ||
         !!filterProvider.value ||
         !!fromDate.value ||
         !!toDate.value ||
         !!lastLoginFrom.value ||
         !!lastLoginTo.value;
});

const clearExtraFilters = () => {
  filterStatus.value = "";
  filterVerified.value = "";
  filterProvider.value = "";
  fromDate.value = "";
  toDate.value = "";
  lastLoginFrom.value = "";
  lastLoginTo.value = "";
};

const fetchUsers = async () => {
  loadError.value = false;
  try {
    await refresh();
  } catch (error) {
    // 有回應(4xx/5xx)：顯示錯誤狀態，不假裝成功；斷線由 axios 攔截器處理
    // 保留快取舊資料避免閃白，但仍標示錯誤、不吞錯
    if (error.response) {
      loadError.value = true;
      setAnnouncement("載入使用者清單失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchUsers);

// 帳號狀態歸類（供下拉篩選）
const statusOf = (u) => {
  if (u.isDeleted) return "deleted";
  if (u.lockedUntil && new Date(u.lockedUntil) > new Date()) return "locked";
  if (!u.isActive) return "disabled";
  return "active";
};

const filteredRows = computed(() => {
  const kw = keyword.value.trim().toLowerCase();
  return rows.value.filter((u) => {
    if (kw && !u.email.toLowerCase().includes(kw) && !(u.name ?? "").toLowerCase().includes(kw)) return false;
    if (filterStatus.value && statusOf(u) !== filterStatus.value) return false;
    if (filterVerified.value && String(u.isVerified) !== filterVerified.value) return false;
    if (filterProvider.value && u.authProvider !== filterProvider.value) return false;
    
    if (fromDate.value) {
      if (dayjs(u.createdAt).isBefore(dayjs(fromDate.value).startOf('day'))) return false;
    }
    if (toDate.value) {
      if (dayjs(u.createdAt).isAfter(dayjs(toDate.value).endOf('day'))) return false;
    }

    if (lastLoginFrom.value) {
      if (!u.lastLoginAt || dayjs(u.lastLoginAt).isBefore(dayjs(lastLoginFrom.value).startOf('day'))) return false;
    }
    if (lastLoginTo.value) {
      if (!u.lastLoginAt || dayjs(u.lastLoginAt).isAfter(dayjs(lastLoginTo.value).endOf('day'))) return false;
    }

    return true;
  });
});

const columns = [
  { key: "name", label: "使用者", sortable: true },
  { key: "authProvider", label: "登入方式", sortable: true, width: "110px" },
  { key: "isVerified", label: "Email 驗證", sortable: true, width: "110px" },
  { key: "status", label: "帳號狀態", sortable: true, width: "110px" },
  { key: "lastLoginAt", label: "最後登入", sortable: true, width: "170px" },
  { key: "createdAt", label: "註冊時間", sortable: true, width: "170px" },
];

const formatTime = (t) => (t ? dayjs(t).format("YYYY-MM-DD HH:mm") : "—");

// 匯出：對應目前篩選後的清單，登入方式/驗證/時間轉成可讀文字（status 已是中文標籤）
const EXPORT_COLUMNS = {
  name: "使用者",
  email: "Email",
  authProviderText: "登入方式",
  verifiedText: "Email 驗證",
  status: "帳號狀態",
  lastLoginAt: "最後登入",
  createdAt: "註冊時間",
};
const exportRows = computed(() =>
  filteredRows.value.map((u) => ({
    name: u.name,
    email: u.email,
    authProviderText: AUTH_PROVIDER_META[u.authProvider]?.label ?? u.authProvider,
    verifiedText: u.isVerified ? "已驗證" : "未驗證",
    status: u.status,
    lastLoginAt: formatTime(u.lastLoginAt),
    createdAt: formatTime(u.createdAt),
  }))
);
</script>

<template>
  <div>
    <!-- 頁面標題 -->
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-people me-2" style="color: var(--tap-primary)"></i>使用者管理</h4>
      <div class="d-flex align-items-center gap-2">
        <ExportButtons file-name="使用者清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="使用者清單" :disabled="loading" />
      </div>
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar
      v-model:keyword="keyword"
      keyword-placeholder="搜尋 Email / 姓名"
      :show-date-range="false"
      :has-extra-filters="hasExtraFilters"
      @clear="clearExtraFilters"
    >
      <div class="col-4 col-md-auto">
        <select v-model="filterStatus" class="form-select form-select-sm" title="帳號狀態">
          <option value="">全部狀態</option>
          <option value="active">正常</option>
          <option value="locked">鎖定中</option>
          <option value="disabled">已停用</option>
          <option value="deleted">已刪除</option>
        </select>
      </div>
      <div class="col-4 col-md-auto">
        <select v-model="filterVerified" class="form-select form-select-sm" title="Email 驗證">
          <option value="">全部驗證</option>
          <option value="true">已驗證</option>
          <option value="false">未驗證</option>
        </select>
      </div>
      <div class="col-4 col-md-auto">
        <select v-model="filterProvider" class="form-select form-select-sm" title="登入方式">
          <option value="">全部來源</option>
          <option value="LOCAL">帳密註冊</option>
          <option value="GOOGLE">Google</option>
        </select>
      </div>
      <template #date>
        <div class="col-12 col-xl-auto d-flex align-items-center gap-3 flex-wrap">
          <!-- 註冊時間 -->
          <div class="input-group input-group-sm" style="width: auto; min-width: 280px;">
            <span class="input-group-text bg-transparent text-tap-secondary">
              <i class="bi bi-calendar3 me-1"></i>註冊時間
            </span>
            <input 
              type="date" 
              class="form-control text-tap-secondary" 
              v-model="fromDate"
              title="註冊時間開始日期" 
            />
            <span class="input-group-text bg-transparent text-tap-secondary border-start-0 border-end-0 px-2">至</span>
            <input 
              type="date" 
              class="form-control text-tap-secondary" 
              v-model="toDate"
              title="註冊時間結束日期" 
            />
          </div>

          <!-- 最後登入 -->
          <div class="input-group input-group-sm" style="width: auto; min-width: 280px;">
            <span class="input-group-text bg-transparent text-tap-secondary">
              <i class="bi bi-calendar3 me-1"></i>最後登入
            </span>
            <input 
              type="date" 
              class="form-control text-tap-secondary" 
              v-model="lastLoginFrom"
              title="最後登入開始日期" 
            />
            <span class="input-group-text bg-transparent text-tap-secondary border-start-0 border-end-0 px-2">至</span>
            <input 
              type="date" 
              class="form-control text-tap-secondary" 
              v-model="lastLoginTo"
              title="最後登入結束日期" 
            />
          </div>
        </div>
      </template>
    </SearchBar>

    <!-- 載入失敗提示（有後端回應的錯誤；斷線另由系統橫幅顯示） -->
    <div v-if="loadError && !loading" class="alert alert-danger d-flex align-items-center justify-content-between">
      <span><i class="bi bi-exclamation-triangle-fill me-2"></i>載入使用者清單失敗。</span>
      <button type="button" class="btn btn-sm btn-outline-light" @click="fetchUsers">重試</button>
    </div>

    <!-- 列表 -->
    <DataTable :columns="columns" :rows="filteredRows" :loading="loading" emptyText="沒有符合條件的使用者" actions-width="90px">
      <template #cell-name="{ row }">
        <div class="d-flex align-items-center gap-2">
          <BaseAvatar :name="row.name" :size="32" />
          <div class="min-w-0">
            <div class="fw-semibold text-truncate">{{ row.name }}</div>
            <div class="small text-tap-secondary text-truncate">{{ row.email }}</div>
          </div>
        </div>
      </template>

      <template #cell-authProvider="{ value }">
        <StatusBadge :dot="false" :variant="AUTH_PROVIDER_META[value]?.variant ?? 'secondary'" :label="AUTH_PROVIDER_META[value]?.label ?? value" />
      </template>

      <template #cell-isVerified="{ value }">
        <i v-if="value" class="bi bi-patch-check-fill text-success"></i>
        <i v-else class="bi bi-dash-circle text-tap-secondary"></i>
      </template>

      <template #cell-status="{ row }">
        <StatusBadge :variant="resolveUserStatus(row).variant" :label="resolveUserStatus(row).label" />
      </template>

      <template #cell-lastLoginAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>
      <template #cell-createdAt="{ value }"><span class="small">{{ formatTime(value) }}</span></template>

      <template #actions="{ row }">
        <RouterLink :to="`/admin/users/${row.id}`" class="btn btn-sm btn-icon btn-outline-primary" title="詳細資料">
          <i class="bi bi-eye"></i>
        </RouterLink>
      </template>
    </DataTable>
  </div>
</template>
