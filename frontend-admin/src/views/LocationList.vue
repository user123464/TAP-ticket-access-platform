<script setup>
/**
 * LocationList.vue — 場地模板列表
 *
 * 搜尋（場館名稱/地址）+ 篩選（狀態）。
 * API：GET /api/locations, PATCH /api/locations/{id}/toggle-status
 */
import { ref, computed, onMounted, watch } from "vue";
import { useRouter } from "vue-router";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import StatusBadge from "@/components/common/StatusBadge.vue";
import SearchBar from "@/components/common/SearchBar.vue";
import ExportButtons from "@/components/common/ExportButtons.vue";
import { useToast } from "@/composables/useToast.js";
import { useConfirm } from "@/composables/useConfirm.js";
import { useCachedResource } from "@/composables/useCachedResource.js";

const router = useRouter();
const toast = useToast();
const { confirm } = useConfirm();

const searchQuery = ref("");
const statusFilter = ref("all");
const cityFilter = ref("");
const capacityMinFilter = ref(0);
const capacityMaxFilter = ref(50000);

// SWR 快取
const { data: locations, isLoading, refresh, mutate } = useCachedResource(
  "admin:locations",
  () => api.get("/api/locations").then((r) => {
    if (r.data.success) {
      return r.data.data ?? [];
    }
    throw new Error(r.data.message || "讀取失敗");
  }),
  { initial: [] }
);

const loading = computed(() => isLoading.value && locations.value.length === 0);

const cities = computed(() => {
  const set = new Set();
  locations.value.forEach((loc) => {
    if (loc.address) {
      const cleanAddress = loc.address.trim().replace(/^\d+/, '').trim();
      const city = cleanAddress.substring(0, 3);
      if (city.endsWith("市") || city.endsWith("縣")) {
        set.add(city);
      }
    }
  });
  return [...set].sort();
});

const maxCapacity = computed(() => {
  if (locations.value.length === 0) return 50000;
  const maxVal = Math.max(5000, ...locations.value.map((l) => l.totalCapacity ?? 0));
  // 四捨五入向上對齊 5000 進位，讓拉桿刻度好看
  return Math.ceil(maxVal / 5000) * 5000;
});

const sliderStep = computed(() => {
  const max = maxCapacity.value;
  if (max <= 5000) return 100;
  if (max <= 20000) return 250;
  if (max <= 50000) return 500;
  return 1000;
});

const progressStyle = computed(() => {
  const minPercent = (capacityMinFilter.value / maxCapacity.value) * 100;
  const maxPercent = (capacityMaxFilter.value / maxCapacity.value) * 100;
  return {
    left: `${minPercent}%`,
    width: `${maxPercent - minPercent}%`,
  };
});

const onMinInput = () => {
  if (capacityMinFilter.value > capacityMaxFilter.value) {
    capacityMinFilter.value = capacityMaxFilter.value;
  }
};

const onMaxInput = () => {
  if (capacityMaxFilter.value < capacityMinFilter.value) {
    capacityMaxFilter.value = capacityMinFilter.value;
  }
};

const hasExtraFilters = computed(() => {
  return (
    statusFilter.value !== "all" ||
    !!cityFilter.value ||
    capacityMinFilter.value > 0 ||
    capacityMaxFilter.value < maxCapacity.value
  );
});

const clearExtraFilters = () => {
  statusFilter.value = "all";
  cityFilter.value = "";
  capacityMinFilter.value = 0;
  capacityMaxFilter.value = maxCapacity.value;
};

// 固定欄寬比例：避免因內容多寡造成版面抖動（搭配 DataTable table-layout: fixed）
const columns = [
  { key: "id", label: "場地編號", sortable: true, width: "130px" },
  { key: "name", label: "場館名稱", sortable: true, width: "25%" },
  { key: "address", label: "地址", sortable: true, width: "45%" },
  { key: "totalCapacity", label: "最大容量", sortable: true, width: "110px" },
  { key: "isDeleted", label: "狀態", sortable: true, width: "90px" },
];

const fetchLocations = async () => {
  try {
    await refresh();
  } catch (error) {
    console.error("取得場地列表失敗:", error);
    toast.error("無法連線到伺服器，請稍後再試");
  }
};

onMounted(fetchLocations);

// 當場地資料改變時，重設容量篩選上限
watch(locations, (newVal) => {
  if (newVal && newVal.length > 0) {
    capacityMaxFilter.value = maxCapacity.value;
  }
});

const filteredLocations = computed(() => {
  return locations.value.filter((loc) => {
    const matchKeyword =
      !searchQuery.value ||
      loc.name.toLowerCase().includes(searchQuery.value.toLowerCase()) ||
      (loc.address && loc.address.toLowerCase().includes(searchQuery.value.toLowerCase()));

    const matchStatus =
      statusFilter.value === "all" ||
      (statusFilter.value === "published" && !loc.isDeleted) ||
      (statusFilter.value === "hidden" && loc.isDeleted);

    const cleanAddress = loc.address ? loc.address.trim().replace(/^\d+/, '').trim() : '';
    const matchCity = !cityFilter.value || (cleanAddress && cleanAddress.startsWith(cityFilter.value));

    const matchCapacity =
      (loc.totalCapacity ?? 0) >= capacityMinFilter.value &&
      (loc.totalCapacity ?? 0) <= capacityMaxFilter.value;

    return matchKeyword && matchStatus && matchCity && matchCapacity;
  });
});

const toggleStatus = async (location) => {
  const actionText = location.isDeleted ? "發布" : "隱藏";

  const ok = await confirm({
    title: `確定要${actionText}這個場地嗎？`,
    message: location.isDeleted
      ? "發布後，廠商將可以在建立活動時選擇此場地。"
      : "隱藏後，新的活動將無法選擇此場地 (但不影響已建立的活動)。",
    confirmText: "確定",
    cancelText: "取消",
    variant: "primary",
  });

  if (ok) {
    try {
      const response = await api.patch(`/api/locations/${location.id}/toggle-status`);
      if (response.data.success) {
        location.isDeleted = !location.isDeleted;
        mutate([...locations.value]);
        toast.success("狀態已更新！");
      } else {
        toast.error(response.data.message || "操作失敗");
      }
    } catch (error) {
      console.error("切換狀態失敗:", error);
      toast.error("無法連線到伺服器");
    }
  }
};

// 匯出：對應目前篩選後的清單，狀態轉成可讀文字
const EXPORT_COLUMNS = {
  id: "場地編號",
  name: "場館名稱",
  address: "地址",
  totalCapacity: "最大容量",
  statusText: "狀態",
};
const exportRows = computed(() =>
  filteredLocations.value.map((loc) => ({
    id: loc.id,
    name: loc.name,
    address: loc.address,
    totalCapacity: loc.totalCapacity,
    statusText: loc.isDeleted ? "已隱藏" : "已發布",
  }))
);

const goToCreate = () => {
  router.push("/admin/venues/new");
};

const editLocation = (id) => {
  router.push(`/admin/venues/${id}/edit`);
};
</script>

<template>
  <div>
    <!-- 頁面標題 -->
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0">
        <i class="bi bi-grid-3x3 me-2" style="color: var(--tap-primary)"></i>場地模板
      </h4>
      <div class="d-flex align-items-center gap-2">
        <ExportButtons file-name="場地模板清單" :rows="exportRows" :columns="EXPORT_COLUMNS" sheet-name="場地模板"
          :disabled="loading" />
        <button class="btn btn-primary fw-bold" @click="goToCreate">
          + 新增模板
        </button>
      </div>
    </div>

    <!-- 搜尋 + 篩選 -->
    <SearchBar v-model:keyword="searchQuery" keywordPlaceholder="搜尋場館名稱 / 地址..." :has-extra-filters="hasExtraFilters"
      @clear="clearExtraFilters">
      <div class="col-6 col-md-auto">
        <select v-model="statusFilter" class="form-select form-select-sm" title="狀態篩選">
          <option value="all">全部狀態</option>
          <option value="published">公開</option>
          <option value="hidden">隱藏</option>
        </select>
      </div>
      <div class="col-6 col-md-auto">
        <select v-model="cityFilter" class="form-select form-select-sm" title="縣市篩選">
          <option value="">全部縣市</option>
          <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
        </select>
      </div>

      <template #date>
        <div class="col-12 col-xl-auto">
          <div class="form-control form-control-sm d-flex align-items-center gap-2 px-2"
            style="width: auto; height: 31px; background-color: var(--bs-body-bg); border-color: var(--bs-border-color);">
            <span class="text-nowrap">
              <i class="bi bi-people me-1"></i>容量：
            </span>
            <div class="dual-range-container position-relative mx-2" style="width: 140px; height: 20px;">
              <!-- Background track -->
              <div
                class="slider-track position-absolute top-50 start-0 end-0 translate-y-middle rounded bg-secondary-subtle"
                style="height: 4px; transform: translateY(-50%);"></div>
              <!-- Highlighted progress range -->
              <div class="slider-progress position-absolute top-50 translate-y-middle rounded" :style="progressStyle"
                style="height: 4px; transform: translateY(-50%); background-color: var(--tap-primary);"></div>
              <!-- Min input -->
              <input type="range" v-model.number="capacityMinFilter" :min="0" :max="maxCapacity" :step="sliderStep"
                style="pointer-events: none; -webkit-appearance: none; appearance: none; background: none; width: 100%; height: 100%; position: absolute; top: 0; left: 0; margin: 0;"
                @input="onMinInput" title="最小容量" />
              <!-- Max input -->
              <input type="range" v-model.number="capacityMaxFilter" :min="0" :max="maxCapacity" :step="sliderStep"
                style="pointer-events: none; -webkit-appearance: none; appearance: none; background: none; width: 100%; height: 100%; position: absolute; top: 0; left: 0; margin: 0;"
                @input="onMaxInput" title="最大容量" />
            </div>
            <span class="text-nowrap text-primary fw-bold"
              style="min-width: 110px; display: inline-block; text-align: right;">
              {{ capacityMinFilter.toLocaleString() }} ~ {{ capacityMaxFilter.toLocaleString() }} 席
            </span>
          </div>
        </div>
      </template>
    </SearchBar>

    <!-- 數據表格 -->
    <DataTable :columns="columns" :rows="filteredLocations" :loading="loading" emptyText="找不到符合條件的場地資料"
      actions-width="105px">
      <template #cell-id="{ value }">
        <span class="fw-bold text-primary">LOC-{{ String(value).padStart(4, "0") }}</span>
      </template>
      <template #cell-name="{ value }">
        <span class="fw-semibold d-inline-block text-truncate w-100" :title="value">{{ value }}</span>
      </template>
      <template #cell-address="{ value }">
        <span class="d-inline-block text-truncate w-100" :title="value">{{ value }}</span>
      </template>
      <template #cell-totalCapacity="{ value }">
        <span>{{ value.toLocaleString() }} 席</span>
      </template>
      <template #cell-isDeleted="{ value }">
        <StatusBadge :variant="value ? 'secondary' : 'success'" :label="value ? '隱藏' : '公開'" />
      </template>
      <template #actions="{ row }">
        <div class="d-flex gap-3 justify-content-center">
          <button v-if="!row.isDeleted" class="btn btn-sm btn-icon btn-outline-success" title="隱藏場地"
            @click="toggleStatus(row)">
            <i class="bi bi-eye"></i>
          </button>
          <button v-else class="btn btn-sm btn-icon btn-outline-secondary" title="公開場地" @click="toggleStatus(row)">
            <i class="bi bi-eye-slash"></i>
          </button>
          <button class="btn btn-sm btn-icon btn-outline-primary" title="編輯" @click="editLocation(row.id)">
            <i class="bi bi-pencil-square"></i>
          </button>
        </div>
      </template>
    </DataTable>
  </div>
</template>

<style scoped>
/* 雙向拉桿樣式 */
.dual-range-container input[type="range"]::-webkit-slider-runnable-track {
  background: transparent;
  border: none;
}

.dual-range-container input[type="range"]::-moz-range-track {
  background: transparent;
  border: none;
}

/* Chrome/Safari/Opera/Edge */
.dual-range-container input[type="range"]::-webkit-slider-thumb {
  pointer-events: auto;
  -webkit-appearance: none;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--tap-primary);
  border: 1.5px solid #fff;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  transition: transform 0.1s;
}

.dual-range-container input[type="range"]::-webkit-slider-thumb:active {
  transform: scale(1.3);
}

/* Firefox */
.dual-range-container input[type="range"]::-moz-range-thumb {
  pointer-events: auto;
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: var(--tap-primary);
  border: 1.5px solid #fff;
  cursor: pointer;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.35);
  transition: transform 0.1s;
}

.dual-range-container input[type="range"]::-moz-range-thumb:active {
  transform: scale(1.3);
}
</style>