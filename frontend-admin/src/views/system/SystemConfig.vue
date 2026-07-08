<script setup>
/**
 * SystemConfig.vue — 系統設定（模組 9 ★ P2）
 *
 * 管理 config_key / config_value 全域參數，依分組瀏覽，就地編輯。
 * API：GET /api/admin/system/configs、PUT .../{key}
 */
import { ref, computed, onMounted } from "vue";
import api from "@/plugins/axios.js";
import MaintenanceTabs from "@/components/system/MaintenanceTabs.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { useCachedResource } from "@/composables/useCachedResource";
import { usePermission } from "@/composables/usePermission";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();
const { can } = usePermission();

// 系統參數為 SUPER_ADMIN 專屬權限（後端 @PreAuthorize('SYSTEM_CONFIG')）。
// 一般 ADMIN 無此權限：前端先以權限碼判斷顯示友善說明，避免直接撞 403 紅錯。
const hasConfigPermission = computed(() => can("SYSTEM_CONFIG"));
const forbidden = ref(false); // 後端回 403 時的保底（即使前端權限判斷遺漏也優雅處理）

// SWR 快取
const { data: configs, isLoading, refresh } = useCachedResource(
  "admin:system:configs",
  () => api.get("/api/admin/system/configs").then((r) => (r.data.data ?? []).map((c) => ({ ...c, editValue: c.value, dirty: false }))),
  { initial: [] }
);

const loading = computed(() => isLoading.value && configs.value.length === 0);

const fetchConfigs = async () => {
  // 無權限者不發送請求，直接顯示友善說明
  if (!hasConfigPermission.value) {
    forbidden.value = true;
    return;
  }
  try {
    await refresh();
  } catch (error) {
    if (error.response?.status === 403) {
      forbidden.value = true; // 保底：後端攔截時也優雅處理，不丟紅錯
    } else if (error.response) {
      setAnnouncement("載入系統設定失敗，請稍後再試。", "danger");
    }
  }
};

onMounted(fetchConfigs);

const sortedConfigs = computed(() => {
  return [...configs.value].sort((a, b) => {
    if (a.group !== b.group) return String(a.group ?? "").localeCompare(String(b.group ?? ""), "zh-Hant");
    return String(a.key ?? "").localeCompare(String(b.key ?? ""), "zh-Hant");
  });
});

const markDirty = (cfg) => { cfg.dirty = cfg.editValue !== cfg.value; };

const savingKey = ref(null);

const handleSave = async (cfg) => {
  savingKey.value = cfg.key;
  try {
    await api.put(`/api/admin/system/configs/${encodeURIComponent(cfg.key)}`, { value: String(cfg.editValue) });
    cfg.value = cfg.editValue;
    cfg.dirty = false;
    await refresh();
    toast.success("設定已更新");
  } catch (error) {
    if (error.response) toast.error(error.response.data?.message ?? "儲存失敗");
  } finally {
    savingKey.value = null;
  }
};
</script>

<template>
  <div>
    <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
      <h4 class="fw-bold mb-0"><i class="bi bi-tools me-2" style="color: var(--tap-primary)"></i>系統維護</h4>
    </div>

    <MaintenanceTabs />

    <!-- 無權限：友善說明（系統參數僅限超級管理員） -->
    <div v-if="forbidden" class="card border shadow-sm rounded-4">
      <div class="card-body text-center py-5">
        <i class="bi bi-shield-lock fs-1 d-block mb-3" style="color: var(--tap-primary)"></i>
        <div class="fw-bold mb-1">此功能僅限超級管理員</div>
        <div class="small text-tap-secondary">
          「系統參數」需要 <code>SYSTEM_CONFIG</code> 權限，請改用具超級管理員權限的帳號操作。
        </div>
      </div>
    </div>

    <div v-else-if="loading" class="text-center py-5"><div class="spinner-border text-primary" role="status"></div></div>

    <div v-else class="row g-3">
      <div v-for="cfg in sortedConfigs" :key="cfg.key" class="col-12 col-md-6">
        <div class="card border shadow-sm rounded-4 h-100">
          <div class="card-body d-flex flex-column justify-content-between gap-3">
            <div>
              <div class="d-flex align-items-start justify-content-between gap-2 mb-1">
                <div class="fw-semibold small">{{ cfg.description }}</div>
                <span class="badge rounded-pill flex-shrink-0 text-tap-secondary" style="background-color: var(--tap-bg-hover)">
                  {{ cfg.group }}
                </span>
              </div>
              <code class="small text-tap-secondary">{{ cfg.key }}</code>
            </div>
            <div class="d-flex align-items-center justify-content-between gap-3 mt-auto pt-2 border-top" style="border-color: var(--tap-border) !important">
              <div class="flex-grow-1">
                <!-- boolean 用開關 -->
                <div v-if="cfg.type === 'boolean'" class="form-check form-switch mb-0">
                  <input
                    class="form-check-input"
                    type="checkbox"
                    :checked="cfg.editValue === 'true'"
                    @change="cfg.editValue = $event.target.checked ? 'true' : 'false'; markDirty(cfg)"
                  />
                </div>
                <input
                  v-else
                  v-model="cfg.editValue"
                  :type="cfg.type === 'number' ? 'number' : 'text'"
                  class="form-control form-control-sm"
                  @input="markDirty(cfg)"
                />
              </div>
              <button type="button" class="btn btn-sm btn-icon btn-primary flex-shrink-0" :disabled="!cfg.dirty || savingKey === cfg.key" @click="handleSave(cfg)" title="儲存">
                <span v-if="savingKey === cfg.key" class="spinner-border spinner-border-sm"></span>
                <i v-else class="bi bi-check2"></i>
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
