<script setup>
/**
 * ContractEdit.vue — 公版範本編輯 / 客製合約建立（模組 5 ★ P1，單檔雙模式）
 *
 * 模式由路由 meta.mode 決定：
 * - template：編輯公版合約 .md 範本（md-editor-v3），儲存即產生新版本；
 *             用戶簽署時後端擷取當下版本快照存入合約記錄。
 * - custom：建立客製合約（contract_type=2）：選擇組織、填費率、
 *           .md 內容（可上傳檔案帶入）+ PDF 上傳（Media_Files）。
 *
 * API：
 *   GET  /api/admin/contracts/template          目前公版範本 + 版本
 *   POST /api/admin/contracts/template          儲存新版本
 *   POST /api/admin/contracts/custom            建立客製合約（multipart：pdf）
 */
import { ref, computed, onMounted } from "vue";
import { useRoute, useRouter } from "vue-router";
import api from "@/plugins/axios.js";
import ContractEditor from "@/components/contracts/ContractEditor.vue";
import ConfirmDialog from "@/components/common/ConfirmDialog.vue";
import { FEE_TYPE } from "@/constants/contract.js";
import { useToast } from "@/composables/useToast";
import { useSystemBanner } from "@/composables/useSystemBanner";
import { usePdfExport } from "@/composables/usePdfExport";
import { useGoBack } from "@/composables/useGoBack.js";

const { success: toastSuccess } = useToast();
const { setAnnouncement } = useSystemBanner();
const { printMarkdownToPdf } = usePdfExport();
const { goBack } = useGoBack();

const route = useRoute();
const router = useRouter();

const isTemplateMode = computed(() => route.meta.mode === "template");

const loading = ref(true);
const submitting = ref(false);
const showSaveConfirm = ref(false);
const errorMessage = ref("");

// --- 公版範本 ---
const templateVersion = ref("");
const contentMd = ref("");
const defaultFeeType = ref(1); // 1 = PERCENTAGE, 2 = FIXED
const defaultFeeValue = ref(null);

// --- 客製合約表單 ---
const customForm = ref({
  orgId: "",
  feeType: FEE_TYPE.PERCENTAGE,
  feeValue: null,
  expiresAt: "",
  pdfFile: null,
});

// --- 可選組織（正常營運中）與搜尋下拉邏輯 ---
const orgOptions = ref([]);
const orgSearch = ref("");
const showOrgDropdown = ref(false);

const filteredOrgOptions = computed(() => {
  const query = orgSearch.value.trim().toLowerCase();
  if (!query) return orgOptions.value;
  return orgOptions.value.filter(o => 
    String(o.name ?? "").toLowerCase().includes(query) || 
    String(o.orgId ?? "").toLowerCase().includes(query)
  );
});

const selectOrg = (org) => {
  customForm.value.orgId = org.orgId;
  orgSearch.value = `${org.name}（${org.orgId}）`;
  showOrgDropdown.value = false;
};

const handleOrgBlur = () => {
  setTimeout(() => {
    showOrgDropdown.value = false;
    const selected = orgOptions.value.find(o => o.orgId === customForm.value.orgId);
    orgSearch.value = selected ? `${selected.name}（${selected.orgId}）` : "";
  }, 200);
};

const handleOrgFocus = () => {
  orgSearch.value = "";
  showOrgDropdown.value = true;
};

onMounted(async () => {
  if (isTemplateMode.value) {
    try {
      const { data } = await api.get("/api/admin/contracts/template");
      const tpl = data.data ?? data;
      templateVersion.value = tpl.version;
      contentMd.value = tpl.contentMd;
      defaultFeeType.value = tpl.defaultFeeType ?? 1;
      defaultFeeValue.value = tpl.defaultFeeValue ?? 5.0;
    } catch (error) {
      if (error.response) {
        setAnnouncement("載入公版範本失敗，請稍後再試。", "danger");
      }
    }
  } else {
    try {
      // 預先載入最新公版範本內容作為起點，方便管理員在此基礎上微調客製內容
      const { data: tplRes } = await api.get("/api/admin/contracts/template");
      const tpl = tplRes.data ?? tplRes;
      contentMd.value = tpl.contentMd ?? "";
      defaultFeeType.value = tpl.defaultFeeType ?? 1;
      defaultFeeValue.value = tpl.defaultFeeValue ?? 5.0;
    } catch (error) {
      contentMd.value = ""; // 失敗時降級為空白編輯器
    }
    try {
      const { data } = await api.get("/api/admin/organizers");
      const list = data.data ?? [];
      // status 0 = ACTIVE（正常營運中）
      orgOptions.value = list.filter((o) => o.status === 0);
    } catch (error) {
      orgOptions.value = [];
      if (error.response) {
        setAnnouncement("載入組織清單失敗，請稍後再試。", "danger");
      }
    }
  }
  loading.value = false;
});

// 上傳 .md 檔帶入編輯器
const handleMdUpload = (event) => {
  const file = event.target.files?.[0];
  if (!file) return;
  const reader = new FileReader();
  reader.onload = () => {
    contentMd.value = String(reader.result ?? "");
  };
  reader.readAsText(file, "utf-8");
  event.target.value = ""; // 允許重複選同一檔
};

const handlePdfChange = (event) => {
  customForm.value.pdfFile = event.target.files?.[0] ?? null;
};

const varTag = (key) => `{{${key}}}`;

const resolvedVariables = computed(() => {
  const selectedOrg = orgOptions.value.find(o => o.orgId === customForm.value.orgId);
  const orgName = selectedOrg ? selectedOrg.name : "";
  const feeUnit = customForm.value.feeType === 1 ? "%" : "元/筆";
  
  return [
    {
      variable: "{{org_name}}",
      description: "簽約組織名稱",
      rawValue: orgName,
      displayValue: orgName ? orgName : "(請選取上方組織)",
    },
    {
      variable: "{{org_id}}",
      description: "簽約組織代碼",
      rawValue: customForm.value.orgId,
      displayValue: customForm.value.orgId ? customForm.value.orgId : "(請選取上方組織)",
    },
    {
      variable: "{{fee_value}}",
      description: "費率值",
      rawValue: customForm.value.feeValue,
      displayValue: customForm.value.feeValue != null ? `${customForm.value.feeValue} ${feeUnit}` : "(請填寫上方費率)",
    },
    {
      variable: "{{expires_at}}",
      description: "合約到期日",
      rawValue: customForm.value.expiresAt,
      displayValue: customForm.value.expiresAt ? customForm.value.expiresAt : "無期限 (未填則預設為無期限)",
    }
  ];
});

const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text);
  toastSuccess(`已複製變數：${text}`);
};

// 下載 PDF：以編輯器當前 Markdown 內容渲染後送瀏覽器列印（可另存 PDF）
const handleDownloadPdf = () => {
  if (!contentMd.value.trim()) {
    setAnnouncement("合約內容為空，無法輸出 PDF。", "warning");
    return;
  }
  const title = isTemplateMode.value
    ? `公版合約範本${templateVersion.value ? ` ${templateVersion.value}` : ""}`
    : "客製合約";
  printMarkdownToPdf(title, contentMd.value);
};

// --- 儲存 ---
const validate = () => {
  errorMessage.value = "";
  if (!contentMd.value.trim()) {
    errorMessage.value = "合約內容不可為空";
    return false;
  }
  if (!isTemplateMode.value) {
    if (!customForm.value.orgId) {
      errorMessage.value = "請選擇簽約組織";
      return false;
    }
    if (customForm.value.feeValue == null || customForm.value.feeValue < 0) {
      errorMessage.value = "請填寫有效費率";
      return false;
    }
    if (!customForm.value.pdfFile) {
      errorMessage.value = "客製合約須上傳已簽署的紙本掃描 PDF";
      return false;
    }
  }
  return true;
};

const openSaveConfirm = () => {
  if (validate()) showSaveConfirm.value = true;
};

const handleSave = async () => {
  submitting.value = true;
  try {
    if (isTemplateMode.value) {
      await api.post("/api/admin/contracts/template", { contentMd: contentMd.value });
    } else {
      // 建立客製合約時，自動將內容中的變數佔位符替換為表單中所填寫的實際值
      const selectedOrg = orgOptions.value.find(o => o.orgId === customForm.value.orgId);
      const orgName = selectedOrg ? selectedOrg.name : "";
      
      const finalContentMd = contentMd.value
        .replace(/\{\{\s*fee_value\s*\}\}/g, customForm.value.feeValue)
        .replace(/\{\{\s*org_name\s*\}\}/g, orgName)
        .replace(/\{\{\s*org_id\s*\}\}/g, customForm.value.orgId)
        .replace(/\{\{\s*expires_at\s*\}\}/g, customForm.value.expiresAt || "—");

      const formData = new FormData();
      formData.append("orgId", customForm.value.orgId);
      formData.append("feeType", customForm.value.feeType);
      formData.append("feeValue", customForm.value.feeValue);
      formData.append("expiresAt", customForm.value.expiresAt);
      formData.append("contentMd", finalContentMd);
      formData.append("pdf", customForm.value.pdfFile);
      await api.post("/api/admin/contracts/custom", formData);
    }
  } catch (error) {
    submitting.value = false;
    showSaveConfirm.value = false;
    if (error.response) {
      errorMessage.value = error.response.data?.message ?? "儲存失敗，請稍後再試";
    }
    // 斷線情境由 axios 攔截器顯示橫幅
    return;
  }
  submitting.value = false;
  showSaveConfirm.value = false;
  toastSuccess(isTemplateMode.value ? "公版範本已儲存新版本" : "客製合約建立成功");
  router.push("/admin/billing/contracts");
};
</script>

<template>
  <div>
    <!-- 標題列 -->
    <div class="detail-header-row">
      <button type="button" class="btn btn-sm btn-icon btn-outline-secondary" @click="goBack('/admin/billing/contracts')">
        <i class="bi bi-arrow-left"></i>
      </button>
      <h4 class="fw-bold">{{ isTemplateMode ? "編輯公版合約範本" : "建立客製合約" }}</h4>
      <span v-if="isTemplateMode && templateVersion" class="badge rounded-pill" style="background-color: var(--tap-bg-hover)">
        目前版本 {{ templateVersion }}
      </span>
    </div>

    <div v-if="loading" class="text-center py-5">
      <div class="spinner-border text-primary" role="status"></div>
    </div>

    <template v-else>
      <!-- 模式說明 -->
      <div class="alert border-0 shadow-sm rounded-4 mb-3 d-flex align-items-center gap-2 mode-banner">
        <i class="bi bi-info-circle-fill" style="color: var(--tap-primary)"></i>
        <span v-if="isTemplateMode">
          儲存後產生<strong>新版本</strong>；既有合約不受影響——用戶簽署時系統擷取當下範本的<strong>版本快照</strong>存入合約記錄。
        </span>
        <span v-else>
          客製合約流程：線下紙本簽約完成後，在此建立客製化合約記錄，並上傳合約 Markdown 內容與已簽署的 PDF 掃描檔。
        </span>
      </div>

      <!-- 錯誤提示 -->
      <div v-if="errorMessage" class="alert alert-danger py-2 small d-flex align-items-center gap-2">
        <i class="bi bi-exclamation-triangle-fill"></i>{{ errorMessage }}
      </div>

      <!-- 客製合約表單 -->
      <div v-if="!isTemplateMode" class="card border shadow-sm rounded-4 mb-3">
        <div class="card-body">
          <div class="row g-3">
            <div class="col-12 col-md-3">
              <label class="form-label small fw-semibold">簽約組織</label>
              <div class="position-relative">
                <input
                  type="text"
                  class="form-control form-control-sm"
                  placeholder="請輸入組織名稱或代碼搜尋..."
                  v-model="orgSearch"
                  @focus="handleOrgFocus"
                  @blur="handleOrgBlur"
                />
                <div
                  v-if="showOrgDropdown && filteredOrgOptions.length > 0"
                  class="position-absolute w-100 mt-1 shadow-lg rounded-3 border overflow-auto"
                  style="max-height: 200px; z-index: 1050; background-color: var(--tap-bg-surface); border-color: var(--tap-border) !important"
                >
                  <div
                    v-for="org in filteredOrgOptions"
                    :key="org.orgId"
                    class="px-3 py-2 cursor-pointer small org-dropdown-item"
                    style="transition: background-color 0.15s"
                    @mousedown="selectOrg(org)"
                  >
                    {{ org.name }}（{{ org.orgId }}）
                  </div>
                </div>
                <div
                  v-else-if="showOrgDropdown"
                  class="position-absolute w-100 mt-1 shadow-lg rounded-3 border p-2 text-center small text-tap-secondary"
                  style="z-index: 1050; background-color: var(--tap-bg-surface); border-color: var(--tap-border) !important"
                >
                  無匹配的組織
                </div>
              </div>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small fw-semibold">費率類型</label>
              <select v-model.number="customForm.feeType" class="form-select form-select-sm">
                <option :value="1">百分比抽成</option>
                <option :value="2">每筆固定</option>
              </select>
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small fw-semibold">費率值 {{ customForm.feeType === 1 ? "(%)" : "(元/筆)" }}</label>
              <input v-model.number="customForm.feeValue" type="number" min="0" step="0.1" class="form-control form-control-sm" placeholder="例：3.5" />
            </div>
            <div class="col-6 col-md-2">
              <label class="form-label small fw-semibold">合約到期日</label>
              <input v-model="customForm.expiresAt" type="date" class="form-control form-control-sm" />
            </div>
            <div class="col-6 col-md-3">
              <label class="form-label small fw-semibold">已簽署 PDF</label>
              <input type="file" accept=".pdf" class="form-control form-control-sm" @change="handlePdfChange" />
            </div>
          </div>
        </div>
      </div>

      <!-- Markdown 編輯器 -->
      <div class="card border shadow-sm rounded-4 mb-3">
        <div class="card-body">
          <div class="d-flex align-items-center justify-content-between mb-3 flex-wrap gap-2">
            <div class="fw-bold"><i class="bi bi-markdown me-2" style="color: var(--tap-primary)"></i>合約內容（Markdown）</div>
            <div class="d-flex align-items-center gap-2">
              <label class="btn btn-sm btn-outline-secondary mb-0">
                <i class="bi bi-upload me-1"></i>上傳 .md 帶入
                <input type="file" accept=".md,.markdown,.txt" hidden @change="handleMdUpload" />
              </label>
              <button type="button" class="btn btn-sm btn-outline-secondary" @click="handleDownloadPdf">
                <i class="bi bi-file-earmark-pdf me-1"></i>下載 PDF
              </button>
            </div>
          </div>
          <!-- 變數即時替換與對照表 -->
          <div v-if="!isTemplateMode" class="card border-0 mb-2" style="background-color: var(--tap-bg-hover); border-radius: 12px">
            <div class="card-body px-3 py-2">
              <div class="fw-bold mb-2 small">
                變數對照表
              </div>
              <div class="table-responsive">
                <table class="table table-borderless mb-0 align-middle var-table" style="font-size: 0.85rem">
                  <thead>
                    <tr class="text-tap-secondary" style="border-bottom: 1px solid var(--tap-border)">
                      <th style="width: 25%">變數名稱</th>
                      <th style="width: 25%">對應欄位</th>
                      <th style="width: 50%">當前實際套用值</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in resolvedVariables" :key="item.variable" style="border-bottom: 1px dashed rgba(0, 0, 0, 0.05)">
                      <td>
                        <code 
                          class="px-2 py-1 rounded fw-semibold cursor-pointer var-code-badge" 
                          @click="copyToClipboard(item.variable)" 
                          title="點擊複製變數"
                        >
                          {{ item.variable }}
                        </code>
                      </td>
                      <td class="text-tap-secondary">{{ item.description }}</td>
                      <td>
                        <span :class="item.rawValue ? 'text-success fw-semibold' : 'text-muted fst-italic'">
                          {{ item.displayValue }}
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>

          <div v-else class="card border-0 mb-2" style="background-color: var(--tap-bg-hover); border-radius: 12px">
            <div class="card-body px-3 py-2">
              <div class="fw-bold mb-2 small">
                可用範本變數
              </div>
              <div class="table-responsive">
                <table class="table table-borderless mb-0 align-middle var-table" style="font-size: 0.85rem">
                  <thead>
                    <tr class="text-tap-secondary" style="border-bottom: 1px solid var(--tap-border)">
                      <th style="width: 30%">變數名稱</th>
                      <th style="width: 70%">欄位說明</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-for="item in resolvedVariables" :key="item.variable" style="border-bottom: 1px dashed rgba(0, 0, 0, 0.05)">
                      <td>
                        <code 
                          class="px-2 py-1 rounded fw-semibold cursor-pointer var-code-badge" 
                          @click="copyToClipboard(item.variable)" 
                          title="點擊複製變數"
                        >
                          {{ item.variable }}
                        </code>
                      </td>
                      <td class="text-tap-secondary">
                        <template v-if="item.variable === '{{fee_value}}'">
                          <span class="text-success fw-semibold">{{ defaultFeeValue ?? '5' }} {{ defaultFeeType === 1 ? '%' : '元/筆' }}</span> <span class="text-muted small">(*由系統參數設定)</span>
                        </template>
                        <template v-else>
                          {{ item.description }}
                        </template>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </div>
          <ContractEditor v-model="contentMd" />
        </div>
      </div>

      <!-- 儲存 -->
      <div class="d-flex justify-content-end gap-2">
        <RouterLink to="/admin/billing/contracts" class="btn btn-outline-secondary">取消</RouterLink>
        <button type="button" class="btn btn-primary fw-bold px-4" @click="openSaveConfirm">
          <i class="bi bi-check-lg me-1"></i>{{ isTemplateMode ? "儲存新版本" : "建立客製合約" }}
        </button>
      </div>
    </template>

    <!-- 儲存確認 -->
    <ConfirmDialog
      v-model:show="showSaveConfirm"
      :title="isTemplateMode ? '儲存公版範本新版本' : '建立客製合約'"
      :message="isTemplateMode
        ? '確定儲存？新版本將即刻成為新簽約用戶的範本，既有合約維持各自簽署時的版本快照。'
        : '確定建立此客製合約？建立後 B2B 端即顯示線下簽約資訊。'"
      :confirmText="isTemplateMode ? '儲存新版本' : '建立合約'"
      variant="primary"
      :loading="submitting"
      @confirm="handleSave"
    />
  </div>
</template>

<style scoped>
.mode-banner {
  background-color: var(--tap-bg-surface);
  color: var(--tap-text-primary);
}
.org-dropdown-item {
  color: var(--tap-text-primary);
}
.org-dropdown-item:hover {
  background-color: var(--tap-bg-hover);
  color: var(--tap-primary) !important;
}
.var-code-badge {
  background-color: var(--tap-bg-surface) !important;
  color: var(--tap-primary) !important;
  border: 1px solid var(--tap-border) !important;
  transition: all 0.2s ease;
  display: inline-block;
}
.var-code-badge:hover {
  background-color: var(--tap-primary) !important;
  color: #fff !important;
  border-color: var(--tap-primary) !important;
}
.var-table th,
.var-table td {
  padding: 6px 12px !important;
}
.var-table th {
  font-weight: 600;
}
</style>
