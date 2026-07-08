<script setup>
/**
 * PermissionPicker.vue — RBAC 權限勾選面板（平台角色 / 組織角色模板共用）
 *
 * 移植 B2B frontend/src/views/org/team/Roles.vue 的「依 resourceCode 分組 + 三態全選」邏輯，
 * 改以 Bootstrap + BaseModal 呈現。父層負責 API 呼叫：監聽 @save 取得勾選後的權限 id 陣列。
 *
 * Props:
 *   show          v-model:show 控制開關
 *   title         標題
 *   permissions   全部權限 [{ id, description, resourceCode }]
 *   modelValue    已選權限 id 陣列（開啟時複製為草稿）
 *   readonly      唯讀（系統身分角色）：停用勾選，只能關閉
 *   saving        儲存中（禁用按鈕）
 */
import { ref, computed, watch } from "vue";
import BaseModal from "@/components/common/BaseModal.vue";

const props = defineProps({
  show: { type: Boolean, default: false },
  title: { type: String, default: "權限設定" },
  permissions: { type: Array, default: () => [] },
  modelValue: { type: Array, default: () => [] },
  readonly: { type: Boolean, default: false },
  saving: { type: Boolean, default: false },
});

const emit = defineEmits(["update:show", "save"]);

// resourceCode → 群組（高凝聚分桶；未知碼落「其他」）
const RESOURCE_GROUP = {
  USER: "使用者與帳號", OWN_ORDER: "使用者與帳號", PROFILE: "使用者與帳號",
  ORGANIZER: "商戶與組織", ORG_PROFILE: "商戶與組織", ORG_CONTRACT: "商戶與組織",
  ORG_SUBSCRIPTION: "商戶與組織", ORG_MEMBER: "商戶與組織", ORG_DATA: "商戶與組織",
  ORG_SETTLEMENT: "商戶與組織", ORG_TICKET: "商戶與組織",
  CONTRACT: "合約與財務", SETTLEMENT: "合約與財務", INVOICE: "合約與財務", PLAN: "合約與財務",
  EVENT: "活動與票務", TICKET_TYPE: "活動與票務", LOCATION: "活動與票務",
  PROMOTION: "活動與票務", ORDER: "活動與票務", MERCH: "活動與票務",
  ANNOUNCEMENT: "內容與通知", MEDIA: "內容與通知", TEMPLATE: "內容與通知",
  NOTIF: "內容與通知", NOTIF_LOG: "內容與通知",
  ROLE: "系統與權限", RESOURCE: "系統與權限", DICT: "系統與權限", FEATURE: "系統與權限",
  JOB: "系統與權限", SYSTEM: "系統與權限", SYSTEM_CONFIG: "系統與權限",
  AUDIT: "稽核與審查", SUBMISSION: "稽核與審查", REFUND: "稽核與審查",
};
const GROUP_ORDER = [
  "使用者與帳號", "商戶與組織", "合約與財務", "活動與票務",
  "內容與通知", "稽核與審查", "系統與權限", "其他",
];

// 勾選草稿（開啟時複製 modelValue，避免直接改父層）
const selected = ref([]);
// 群組開合：預設全展開；以「!== false」判定，故晚到的群組也預設展開
const openGroups = ref({});
const isGroupOpen = (name) => openGroups.value[name] !== false;
const toggleOpen = (name) => {
  openGroups.value = { ...openGroups.value, [name]: !isGroupOpen(name) };
};
watch(
  () => props.show,
  (isOpen) => {
    if (isOpen) {
      selected.value = [...(props.modelValue ?? [])];
      openGroups.value = {}; // 每次開窗重置為全展開
    }
  },
  { immediate: true }
);

const groups = computed(() => {
  const buckets = {};
  for (const p of props.permissions) {
    const name = RESOURCE_GROUP[p.resourceCode] ?? "其他";
    (buckets[name] ??= []).push(p);
  }
  return GROUP_ORDER
    .filter((name) => buckets[name]?.length)
    .map((name) => ({ name, permissions: buckets[name] }));
});

const isChecked = (id) => selected.value.includes(id);

const togglePerm = (id) => {
  if (props.readonly) return;
  const set = new Set(selected.value);
  set.has(id) ? set.delete(id) : set.add(id);
  selected.value = [...set];
};

const groupState = (group) => {
  const ids = group.permissions.map((p) => p.id);
  const checked = ids.filter((id) => selected.value.includes(id)).length;
  return { checked, total: ids.length, all: checked === ids.length, some: checked > 0 && checked < ids.length };
};

const toggleGroup = (group, checked) => {
  if (props.readonly) return;
  const ids = group.permissions.map((p) => p.id);
  const set = new Set(selected.value);
  ids.forEach((id) => (checked ? set.add(id) : set.delete(id)));
  selected.value = [...set];
};

const badgeClass = (st) => {
  if (st.all) return "bg-success-subtle text-success-emphasis border border-success-subtle";
  if (st.some) return "bg-warning-subtle text-warning-emphasis border border-warning-subtle";
  return "bg-secondary-subtle text-secondary-emphasis";
};

const close = () => emit("update:show", false);
const save = () => emit("save", [...selected.value]);
</script>

<template>
  <BaseModal :show="show" :title="title" size="modal-lg" @update:show="(v) => emit('update:show', v)">
    <p v-if="readonly" class="small text-tap-secondary mb-3">
      <i class="bi bi-lock-fill me-1"></i>系統身分角色為唯讀，僅供檢視。
    </p>

    <div v-if="groups.length === 0" class="text-center text-tap-secondary py-4">無可用權限</div>

    <div v-for="group in groups" :key="group.name" class="mb-2 border rounded-3">
      <div
        class="d-flex align-items-center justify-content-between px-3 py-2 rounded-top-3"
        style="cursor: pointer; background-color: var(--tap-bg-hover)"
        @click="toggleOpen(group.name)"
      >
        <div class="form-check mb-0" @click.stop>
          <input
            class="form-check-input"
            type="checkbox"
            :id="`grp-${group.name}`"
            :checked="groupState(group).all"
            :indeterminate.prop="groupState(group).some"
            :disabled="readonly"
            @change="toggleGroup(group, $event.target.checked)"
          />
          <label class="form-check-label fw-semibold" :for="`grp-${group.name}`">{{ group.name }}</label>
        </div>
        <div class="d-flex align-items-center gap-2">
          <span class="badge rounded-pill small" :class="badgeClass(groupState(group))">
            {{ groupState(group).checked }} / {{ groupState(group).total }}
          </span>
          <i class="bi" :class="isGroupOpen(group.name) ? 'bi-chevron-up' : 'bi-chevron-down'"></i>
        </div>
      </div>
      <div v-show="isGroupOpen(group.name)" class="row g-2 px-3 py-2">
        <div v-for="p in group.permissions" :key="p.id" class="col-12 col-md-6">
          <div class="form-check">
            <input
              class="form-check-input"
              type="checkbox"
              :id="`perm-${p.id}`"
              :checked="isChecked(p.id)"
              :disabled="readonly"
              @change="togglePerm(p.id)"
            />
            <label class="form-check-label" :for="`perm-${p.id}`">
              {{ p.description }} <code class="small text-tap-secondary">{{ p.id }}</code>
            </label>
          </div>
        </div>
      </div>
    </div>

    <template #footer>
      <button type="button" class="btn btn-outline-secondary picker-btn" :disabled="saving" @click="close">
        {{ readonly ? "關閉" : "取消" }}
      </button>
      <button v-if="!readonly" type="button" class="btn btn-primary picker-btn" :disabled="saving" @click="save">
        <span v-if="saving" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
        <span v-else>儲存</span>
      </button>
    </template>
  </BaseModal>
</template>

<style scoped>
/* 取消／儲存固定相同寬度：儲存時 spinner 取代文字，寬度不變、不抖動 */
.picker-btn {
  min-width: 96px;
}
</style>
