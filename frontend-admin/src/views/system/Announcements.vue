<script setup>
/**
 * Announcements.vue — 系統文件編輯器（原系統公告管理改版）
 *
 * 提供條列式資料表展示 privacy.md, terms.md, guide.md 等系統條約文件，
 * 並整合 md-editor-v3 提供即時的 Markdown 編輯與預覽功能，儲存後直接更新伺服器檔案。
 */
import { ref } from "vue";
import api from "@/plugins/axios.js";
import DataTable from "@/components/common/DataTable.vue";
import NotificationTabs from "@/components/system/NotificationTabs.vue";
import ContractEditor from "@/components/contracts/ContractEditor.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";

const toast = useToast();
const { setAnnouncement } = useSystemBanner();

// 系統管理文件清單 (不含 contract_free.md)
const rows = ref([
  { code: "privacy.md", name: "隱私權政策與個人資料保護聲明", description: "使用者與主辦方註冊及交易時適用的個人隱私及資料安全保護條款" },
  { code: "terms.md", name: "服務條款", description: "平台使用規範、買賣票券服務協定及退換票條款" },
  { code: "guide.md", name: "主辦方使用說明", description: "提供給 B2B 廠商/主辦方的使用手冊與售票後台操作指引" }
]);

const loadingList = ref(false);
const editingDoc = ref(null);
const contentMd = ref("");
const loadingContent = ref(false);
const saving = ref(false);
const showSaveConfirm = ref(false);

const columns = [
  { key: "name", label: "文件名稱", sortable: false },
  { key: "code", label: "檔案名稱", width: "180px", sortable: false },
  { key: "description", label: "說明描述", sortable: false },
];

// 載入文件內容並進入編輯模式
const startEdit = async (row) => {
  editingDoc.value = row;
  loadingContent.value = true;
  contentMd.value = "";
  try {
    const { data } = await api.get(`/api/documents/${row.code}`);
    if (data && data.success) {
      contentMd.value = data.content ?? "";
    } else {
      setAnnouncement(`載入文件 ${row.code} 失敗。`, "danger");
      editingDoc.value = null;
    }
  } catch (error) {
    console.error("載入文件失敗", error);
    setAnnouncement(`載入文件 ${row.code} 失敗，請稍後再試。`, "danger");
    editingDoc.value = null;
  } finally {
    loadingContent.value = false;
  }
};

// 返回列表模式
const cancelEdit = () => {
  editingDoc.value = null;
  contentMd.value = "";
};

// 儲存變更寫入檔案
const handleSave = async () => {
  saving.value = true;
  try {
    const response = await api.put(`/api/documents/${editingDoc.value.code}`, {
      content: contentMd.value
    });
    if (response.data && response.data.success) {
      toast.success("文件已成功更新！");
      showSaveConfirm.value = false;
      editingDoc.value = null;
      contentMd.value = "";
    } else {
      toast.error(response.data?.message || "儲存失敗");
    }
  } catch (error) {
    console.error("儲存文件失敗", error);
    toast.error("儲存文件時發生錯誤，請稍後再試。");
  } finally {
    saving.value = false;
  }
};
</script>

<template>
  <div>
    <!-- 列表模式 -->
    <template v-if="!editingDoc">
      <div class="d-flex align-items-center justify-content-between mb-4 flex-wrap gap-2">
        <h4 class="fw-bold mb-0">
          <i class="bi bi-file-earmark-text me-2" style="color: var(--tap-primary)"></i>系統文件
        </h4>
      </div>

      <NotificationTabs />

      <DataTable :columns="columns" :rows="rows" :loading="loadingList" emptyText="尚無系統文件" actions-width="80px">
        <template #cell-name="{ value }">
          <span class="fw-semibold">{{ value }}</span>
        </template>
        <template #cell-code="{ value }">
          <code class="small text-secondary">{{ value }}</code>
        </template>
        <template #cell-description="{ value }">
          <span class="text-tap-secondary small">{{ value }}</span>
        </template>

        <template #actions="{ row }">
          <button type="button" class="btn btn-sm btn-icon btn-outline-primary" @click="startEdit(row)" title="編輯文件">
            <i class="bi bi-pencil-square"></i>
          </button>
        </template>
      </DataTable>
    </template>

    <!-- 編輯模式 -->
    <template v-else>
      <div class="d-flex align-items-center gap-3 mb-4">
        <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="cancelEdit" title="返回列表">
          <i class="bi bi-arrow-left"></i>
        </button>
        <h4 class="fw-bold mb-0">編輯系統文件：{{ editingDoc.name }}</h4>
        <code class="px-2 py-1 rounded bg-dark text-tap-secondary small">{{ editingDoc.code }}</code>
      </div>

      <div v-if="loadingContent" class="text-center py-5">
        <div class="spinner-border text-primary" role="status"></div>
      </div>

      <template v-else>


        <!-- 編輯器卡片 -->
        <div class="card border shadow-sm rounded-4 mb-4">
          <div class="card-body">
            <div class="fw-bold mb-3">
              <i class="bi bi-markdown me-2" style="color: var(--tap-primary)"></i>Markdown 內容編輯
            </div>
            <ContractEditor v-model="contentMd" height="600px" />
          </div>
        </div>

        <!-- 儲存 / 取消 -->
        <div class="d-flex justify-content-end gap-2 mb-4">
          <button type="button" class="btn btn-outline-secondary" @click="cancelEdit">取消</button>
          <button type="button" class="btn btn-primary" @click="showSaveConfirm = true">儲存</button>
        </div>
      </template>
    </template>

    <!-- 儲存確認對話框 -->
    <ConfirmDialog
      v-if="showSaveConfirm"
      :show="showSaveConfirm"
      title="確認儲存"
      message="確定要將修改內容寫入系統文件嗎？這將即時影響所有前台與主辦方看到的條款內容。"
      confirmText="儲存"
      :loading="saving"
      @update:show="showSaveConfirm = false"
      @confirm="handleSave"
    />
  </div>
</template>

<style scoped>
:deep(.table th),
:deep(.table td) {
  padding-left: 8px !important;
  padding-right: 8px !important;
}
:deep(.table th:first-child),
:deep(.table td:first-child) {
  padding-left: 16px !important;
}
:deep(.table th:last-child),
:deep(.table td:last-child) {
  padding-right: 16px !important;
}
:deep(.table td .d-flex) {
  gap: 12px !important;
}

.mode-banner {
  background-color: var(--tap-bg-hover);
  border-radius: 12px;
}
</style>
