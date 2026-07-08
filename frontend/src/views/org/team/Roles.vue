<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';

const route = useRoute();
const orgId = computed(() => route.params.organizerId);
const toast = useToast();
const { confirm } = useConfirm();

// 可勾選的真實權限清單 (後端動態載入)
const availablePermissions = ref([]);

// 模組分組定義 (對應 resourceCode)
const MODULE_MAPPING = {
  'EVENT': { name: '活動管理', icon: 'bi-calendar-event-fill text-secondary' },
  'LOCATION': { name: '活動管理', icon: 'bi-calendar-event-fill text-secondary' },
  'TICKET_TYPE': { name: '票券管理', icon: 'bi-ticket-perforated-fill text-secondary' },
  'ORG_TICKET': { name: '票券管理', icon: 'bi-ticket-perforated-fill text-secondary' },
  'ORDER': { name: '票券管理', icon: 'bi-ticket-perforated-fill text-secondary' },
  'MERCH': { name: '商品管理', icon: 'bi-bag-fill text-secondary' },
  'ORG_SETTLEMENT': { name: '財務管理', icon: 'bi-cash-coin text-secondary' },
  'ORG_MEMBER': { name: '團隊管理', icon: 'bi-people-fill text-secondary' },
  'ORG_PROFILE': { name: '組織管理', icon: 'bi-building-fill text-secondary' },
  'ORG_CONTRACT': { name: '組織管理', icon: 'bi-building-fill text-secondary' },
  'ORG_SUBSCRIPTION': { name: '組織管理', icon: 'bi-building-fill text-secondary' },
  'ORG_DATA': { name: '組織管理', icon: 'bi-building-fill text-secondary' }
};

// 將真實權限分組
const permissionGroups = computed(() => {
  const groups = {
    '活動管理': { name: '活動管理', icon: 'bi-calendar-event-fill text-secondary', permissions: [] },
    '票券管理': { name: '票券管理', icon: 'bi-ticket-perforated-fill text-secondary', permissions: [] },
    '商品管理': { name: '商品管理', icon: 'bi-bag-fill text-secondary', permissions: [] },
    '財務管理': { name: '財務管理', icon: 'bi-cash-coin text-secondary', permissions: [] },
    '團隊管理': { name: '團隊管理', icon: 'bi-people-fill text-secondary', permissions: [] },
    '組織管理': { name: '組織管理', icon: 'bi-building-fill text-secondary', permissions: [] },
    '其他': { name: '其他', icon: 'bi-shield-slash-fill text-secondary', permissions: [] }
  };
  
  for (const p of availablePermissions.value) {
    const map = MODULE_MAPPING[p.resourceCode];
    const groupName = map ? map.name : '其他';
    groups[groupName].permissions.push(p);
  }
  
  return Object.values(groups).filter(g => g.permissions.length > 0);
});

// 角色清單狀態
const roles = ref([]);
// 權限對照 Map (key: roleId, value: Array of permissionIds)
const rolePermissionsMap = ref({});

// 載入可勾選的真實權限清單
const loadAvailablePermissions = async () => {
  try {
    const res = await axios.get(`/api/organizer/${orgId.value}/permissions`);
    availablePermissions.value = res.data.data || [];
  } catch (error) {
    console.error('Failed to load permissions', error);
    toast.error('無法載入權限清單');
  }
};

// 載入角色與權限資料
const loadRolesAndPermissions = async () => {
  try {
    const res = await axios.get(`/api/organizer/${orgId.value}/roles`);
    const backendRoles = res.data.data;
    
    const mappedRoles = backendRoles.map(r => ({
      role_id: r.roleId,
      role_name: r.roleName,
      description: r.description,
      is_editable: r.isEditable
    }));

    // 依企業組織角色階級排序：公司負責人 (ORGANIZER) -> 管理員 (Admin) -> 財務專員 (Accountant) -> 客服專員 (CS/組織客服) -> 驗票員 (Scanner/組織驗票員) -> 自訂角色 (字母順序)
    const roleOrder = { 'ORGANIZER': 1, 'Admin': 2, 'Accountant': 3, 'CS': 4, '組織客服': 4, 'Scanner': 5, '組織驗票員': 5 };
    mappedRoles.sort((a, b) => {
      const orderA = roleOrder[a.role_id] || roleOrder[a.role_name] || 99;
      const orderB = roleOrder[b.role_id] || roleOrder[b.role_name] || 99;
      if (orderA !== orderB) {
        return orderA - orderB;
      }
      return a.role_name.localeCompare(b.role_name, 'zh-hant');
    });
    roles.value = mappedRoles;

    const newMap = {};
    backendRoles.forEach(r => {
      newMap[r.roleId] = r.permissions || [];
    });
    rolePermissionsMap.value = newMap;
  } catch (error) {
    console.error('Failed to load roles', error);
    toast.error('無法載入角色資料');
  }
};

// 角色是否為系統內建（不可編輯）
const isRoleLocked = (roleId) => {
  const r = roles.value.find(role => role.role_id === roleId);
  return r ? r.is_editable === false : false;
};

// 中文顯示名稱對照
const getRoleDisplayName = (role) => {
  if (!role) return '';
  if (role.role_id === 'ORGANIZER') return '公司負責人';
  if (role.role_name === 'Admin') return '管理員';
  if (role.role_name === 'Accountant') return '財務專員';
  if (role.role_name === 'CS' || role.role_name === '組織客服') return '客服專員';
  if (role.role_name === 'Scanner' || role.role_name === '組織驗票員') return '驗票員';
  return role.role_name;
};

const formatTemplateName = (name) => {
  if (name === '組織管理員') return '管理員';
  if (name === '組織會計') return '財務專員';
  if (name === '組織驗票員') return '驗票員';
  if (name === '組織客服') return '客服專員';
  return name;
};

// 重新載入所有資料
const reloadAll = async () => {
  // 嘗試從 localStorage 提取快取資料，用於加速畫面初始化渲染
  const cacheKey = `b2b_roles_cache_${orgId.value}`;
  try {
    const cached = localStorage.getItem(cacheKey);
    if (cached) {
      const parsed = JSON.parse(cached);
      if (parsed.availablePermissions) availablePermissions.value = parsed.availablePermissions;
      if (parsed.roles) roles.value = parsed.roles;
      if (parsed.rolePermissionsMap) rolePermissionsMap.value = parsed.rolePermissionsMap;
      if (parsed.roleTemplates) roleTemplates.value = parsed.roleTemplates;
    }
  } catch (e) {
    console.warn('Failed to parse cached roles data', e);
  }

  await Promise.all([loadAvailablePermissions(), loadRolesAndPermissions(), loadRoleTemplates()]);

  // 寫入最新快取
  try {
    localStorage.setItem(cacheKey, JSON.stringify({
      availablePermissions: availablePermissions.value,
      roles: roles.value,
      rolePermissionsMap: rolePermissionsMap.value,
      roleTemplates: roleTemplates.value
    }));
  } catch (e) {
    console.warn('Failed to write cached roles data', e);
  }
};

// ==================== Drawer 編輯權限相關狀態與方法 ====================
const showDrawer = ref(false);
const activeRole = ref(null);
const drawerPermissions = ref([]);
const activeCollapseNames = ref([]); // 預設展開的折疊面板
const drawerTemplateId = ref('');
const originalDrawerPermissions = ref([]);
const originalDrawerTemplateId = ref('');
const editableRoleName = ref('');
const isEditingName = ref(false);
const nameInputRef = ref(null);

const startEditingName = () => {
  if (activeRole.value && isRoleLocked(activeRole.value.role_id)) return;
  isEditingName.value = true;
  nextTick(() => {
    nameInputRef.value?.focus();
  });
};

// 開啟權限編輯 Drawer
const handleOpenPermissions = (role) => {
  activeRole.value = role;
  // 複製一份目前的權限陣列到臨時變數
  drawerPermissions.value = [...(rolePermissionsMap.value[role.role_id] || [])];
  originalDrawerPermissions.value = [...drawerPermissions.value];
  
  // 初始化角色名稱與編輯狀態 (若為預設英文代碼，利用對照表顯示中文以求一致)
  editableRoleName.value = getRoleDisplayName(role);
  isEditingName.value = false;

  // 判斷如果是系統角色就固定在該角色模板
  if (isRoleLocked(role.role_id)) {
    if (role.role_name === 'Admin' || role.role_id === 'DEFAULT_ORG_ADMIN') {
      drawerTemplateId.value = 'DEFAULT_ORG_ADMIN';
    } else if (role.role_name === 'Accountant' || role.role_name === '財務專員' || role.role_id === 'DEFAULT_ORG_ACCOUNTANT') {
      drawerTemplateId.value = 'DEFAULT_ORG_ACCOUNTANT';
    } else {
      drawerTemplateId.value = '';
    }
  } else {
    drawerTemplateId.value = '';
  }
  originalDrawerTemplateId.value = drawerTemplateId.value;

  // 預設折疊所有分組
  activeCollapseNames.value = [];
  showDrawer.value = true;
};

// 抽屜內套用模板
const handleApplyDrawerTemplate = () => {
  if (activeRole.value && isRoleLocked(activeRole.value.role_id)) {
    return; // 系統角色固定，不可切換
  }
  if (!drawerTemplateId.value || drawerTemplateId.value === 'custom') {
    return; // 選擇自訂或未選擇，不套用模板權限
  }
  const selected = roleTemplates.value.find(t => t.templateId === drawerTemplateId.value);
  if (selected) {
    drawerPermissions.value = [...selected.permissions];
    toast.success(`已套用【${formatTemplateName(selected.templateName)}】的預設權限`);
  }
};

// 抽屜內重設權限與名稱
const handleResetDrawer = () => {
  drawerPermissions.value = [...originalDrawerPermissions.value];
  drawerTemplateId.value = originalDrawerTemplateId.value;
  editableRoleName.value = getRoleDisplayName(activeRole.value);
  isEditingName.value = false;
  toast.success('已重設為編輯前的權限狀態');
};

// 儲存 Drawer 的權限變更與角色名稱
const handleSaveRolePermissions = async () => {
  if (!activeRole.value) return;
  const roleId = activeRole.value.role_id;
  
  if (isRoleLocked(roleId)) {
    toast.error('系統預設角色不可修改');
    return;
  }

  if (!editableRoleName.value || !editableRoleName.value.trim()) {
    toast.error('角色名稱不能為空');
    return;
  }

  try {
    // 1. 如果名稱有改變，先更新名稱
    if (editableRoleName.value.trim() !== activeRole.value.role_name) {
      await axios.put(`/api/organizer/${orgId.value}/roles/${roleId}/name`, {
        name: editableRoleName.value.trim()
      });
    }

    // 2. 呼叫更新單一角色權限 API
    await axios.put(`/api/organizer/${orgId.value}/roles/${roleId}/permissions`, {
      permissions: drawerPermissions.value
    });
    
    // 同步更新本地 state
    rolePermissionsMap.value[roleId] = [...drawerPermissions.value];
    toast.success(`角色【${editableRoleName.value.trim()}】的設定已更新`);
    showDrawer.value = false;
    reloadAll();
  } catch (error) {
    console.error('Save role settings failed', error);
    toast.error('更新失敗：' + (error.response?.data?.message || error.message));
  }
};

// 計算分組全選/半選狀態
const getGroupCheckedState = (group) => {
  const groupPermIds = group.permissions.map(p => p.permissionId);
  const checkedCount = groupPermIds.filter(pId => drawerPermissions.value.includes(pId)).length;
  
  return {
    isAllChecked: checkedCount === groupPermIds.length,
    isIndeterminate: checkedCount > 0 && checkedCount < groupPermIds.length,
    checkedCount,
    totalCount: groupPermIds.length
  };
};

// 根據勾選狀態，動態回傳分組 Badge 顏色樣式
const getGroupBadgeClass = (checkedState) => {
  if (checkedState.isAllChecked) {
    return 'bg-success text-success bg-opacity-10 border border-success border-opacity-25';
  }
  if (checkedState.isIndeterminate) {
    return 'bg-warning text-warning bg-opacity-10 border border-warning border-opacity-25';
  }
  return 'bg-light text-secondary border';
};

// 分組全選 Checkbox 點擊 Handler
const handleGroupAllCheckChange = (group, checked) => {
  const groupPermIds = group.permissions.map(p => p.permissionId);
  const current = new Set(drawerPermissions.value);
  
  if (checked) {
    // 全選：將該組所有 ID 加入 Set
    groupPermIds.forEach(pId => current.add(pId));
  } else {
    // 全取消：將該組所有 ID 自 Set 移除
    groupPermIds.forEach(pId => current.delete(pId));
  }
  
  drawerPermissions.value = Array.from(current);
};

// ==================== 新建自訂角色相關狀態與方法 ====================
const showCreateModal = ref(false);
const newRoleName = ref('');
const newRoleDescription = ref('');
const newRolePermissions = ref([]);
const createActiveCollapseNames = ref([]);
const selectedTemplateId = ref('');

const roleTemplates = ref([]);

// 載入預設角色模板資料
const loadRoleTemplates = async () => {
  try {
    const res = await axios.get(`/api/organizer/${orgId.value}/roles/templates`);
    roleTemplates.value = res.data.data || [];
  } catch (error) {
    console.error('Failed to load role templates', error);
  }
};

const handleApplyTemplate = () => {
  if (!selectedTemplateId.value || selectedTemplateId.value === 'custom') {
    newRoleName.value = '';
    newRolePermissions.value = [];
    return;
  }
  const selected = roleTemplates.value.find(t => t.templateId === selectedTemplateId.value);
  if (selected) {
    newRoleName.value = selected.templateName;
    newRolePermissions.value = [...selected.permissions];
  }
};

const handleOpenCreateModal = () => {
  newRoleName.value = '';
  newRoleDescription.value = '';
  newRolePermissions.value = [];
  createActiveCollapseNames.value = [];
  selectedTemplateId.value = '';
  showCreateModal.value = true;
};

// 建立新角色
const handleCreateRole = async () => {
  if (!newRoleName.value.trim()) {
    toast.error('請輸入角色名稱');
    return;
  }

  try {
    const payload = {
      roleName: newRoleName.value.trim(),
      description: newRoleDescription.value.trim(),
      permissions: [...newRolePermissions.value]
    };
    
    await axios.post(`/api/organizer/${orgId.value}/roles`, payload);
    
    showCreateModal.value = false;
    toast.success('自訂角色建立成功');
    loadRolesAndPermissions();
  } catch (error) {
    console.error('Create failed', error);
    toast.error('建立失敗：' + (error.response?.data?.message || error.message));
  }
};

// 新建角色中的分組全選控制
const getCreateGroupCheckedState = (group) => {
  const groupPermIds = group.permissions.map(p => p.permissionId);
  const checkedCount = groupPermIds.filter(pId => newRolePermissions.value.includes(pId)).length;
  
  return {
    isAllChecked: checkedCount === groupPermIds.length,
    isIndeterminate: checkedCount > 0 && checkedCount < groupPermIds.length,
    checkedCount,
    totalCount: groupPermIds.length
  };
};

const handleCreateGroupAllCheckChange = (group, checked) => {
  const groupPermIds = group.permissions.map(p => p.permissionId);
  const current = new Set(newRolePermissions.value);
  
  if (checked) {
    groupPermIds.forEach(pId => current.add(pId));
  } else {
    groupPermIds.forEach(pId => current.delete(pId));
  }
  
  newRolePermissions.value = Array.from(current);
};

// ==================== 刪除角色相關方法 ====================
const handleDeleteRole = async (roleId) => {
  if (isRoleLocked(roleId)) return;
  const targetRole = roles.value.find(r => r.role_id === roleId);

  const ok = await confirm({
    title: '刪除自訂角色',
    message: `確定要刪除自訂角色【${targetRole ? getRoleDisplayName(targetRole) : ''}】嗎？`,
    confirmText: '刪除',
    confirmButtonColor: '#d33',
    variant: 'danger'
  });
  if (ok) {
    try {
      await axios.delete(`/api/organizer/${orgId.value}/roles/${roleId}`);
      toast.success('自訂角色已成功刪除');
      loadRolesAndPermissions();
    } catch (error) {
      console.error('Delete failed', error);
      toast.error('刪除失敗：' + (error.response?.data?.message || error.message));
    }
  }
};

onMounted(() => {
  reloadAll();
});
</script>

<template>
  <div class="card border rounded-4 shadow-sm p-4">
    <!-- 頂部區塊 -->
    <div class="d-flex align-items-center justify-content-between mb-4">
      <div>
        <h5 class="fw-bold mb-1"><i class="bi bi-shield-check me-2 text-primary"></i>角色與權限設定</h5>
        <p class="small text-secondary mb-0">配置與管理組織內各角色的功能權限範圍。</p>
      </div>
      <div>
        <button @click="handleOpenCreateModal" class="btn btn-primary text-white rounded-3 fw-bold px-3 py-2">
          <i class="bi bi-plus-lg me-1"></i>新建自訂角色
        </button>
      </div>
    </div>

    <!-- 桌機版角色列表表格 (簡化為 3 欄式) -->
    <div class="d-none d-md-block table-responsive border rounded-3 mb-4">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light text-nowrap">
          <tr>
            <th class="py-3 text-center" style="width: 70px; min-width: 70px; padding-left: 0; padding-right: 0;">類型</th>
            <th class="py-3 px-3">角色名稱</th>
            <th class="py-3 text-center" style="width: 140px; min-width: 140px;">已啟用權限</th>
            <th class="py-3 text-center" style="width: 120px; min-width: 120px;">操作</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="r in roles" :key="r.role_id">
            <!-- 類型 -->
            <td class="py-3 text-center" style="padding-left: 0; padding-right: 0;">
              <!-- 微型簡潔標籤 -->
              <span v-if="!r.is_editable" class="badge-role-tag bg-light text-secondary border">系統</span>
              <span v-else class="badge-role-tag badge-role-tag-custom">自訂</span>
            </td>
            
            <!-- 角色名稱 -->
            <td class="py-3 px-3 text-dark fw-bold">
              {{ getRoleDisplayName(r) }}
            </td>
            
            <!-- 已啟用權限 (Monospace 純文字) -->
            <td class="py-3 text-center text-secondary font-monospace fw-semibold" style="font-size: 0.9rem;">
              {{ rolePermissionsMap[r.role_id]?.length || 0 }} / {{ availablePermissions.length }}
            </td>
            
            <!-- 操作 (設定齒輪與刪除) -->
            <td class="py-3 text-center text-nowrap">
              <div class="d-flex gap-2 justify-content-center align-items-center">
                <!-- 統一使用灰色齒輪，無 tooltip -->
                <button
                  class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center"
                  style="width: 32px; height: 32px;"
                  @click="handleOpenPermissions(r)"
                >
                  <i class="bi bi-gear-fill"></i>
                </button>
                
                <!-- 系統角色：虛線置灰垃圾桶，無 tooltip -->
                <button
                  v-if="!r.is_editable"
                  class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center disabled border-dashed bg-light"
                  style="border-style: dashed !important; width: 32px; height: 32px; opacity: 0.4; cursor: not-allowed;"
                  disabled
                >
                  <i class="bi bi-trash-fill text-muted"></i>
                </button>
                
                <!-- 自訂角色：主題橘色垃圾桶，無 tooltip -->
                <button
                  v-else
                  class="btn btn-outline-orange btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center"
                  style="width: 32px; height: 32px;"
                  @click="handleDeleteRole(r.role_id)"
                >
                  <i class="bi bi-trash-fill"></i>
                </button>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 手機版自適應角色卡片列表 (Card List) -->
    <div class="d-block d-md-none mb-4">
      <div v-if="roles.length === 0" class="text-center py-5 text-secondary border rounded-3 bg-light">
        目前無任何角色資料
      </div>
      <div v-else class="d-flex flex-column gap-3">
        <div 
          v-for="r in roles" 
          :key="r.role_id" 
          class="card border rounded-3 p-3 shadow-sm"
        >
          <div class="d-flex align-items-start justify-content-between">
            <!-- 角色名稱與標籤 -->
            <div class="d-flex align-items-center gap-2">
              <span v-if="!r.is_editable" class="badge-role-tag bg-light text-secondary border">系統</span>
              <span v-else class="badge-role-tag badge-role-tag-custom">自訂</span>
              <span class="fw-bold text-dark">{{ getRoleDisplayName(r) }}</span>
            </div>
            
            <!-- 操作按鈕 -->
            <div class="d-flex gap-2">
              <!-- 統一使用灰色齒輪，無 tooltip -->
              <button
                class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center"
                style="width: 32px; height: 32px;"
                @click="handleOpenPermissions(r)"
              >
                <i class="bi bi-gear-fill"></i>
              </button>
              
              <!-- 系統角色：虛線置灰垃圾桶，無 tooltip -->
              <button
                v-if="!r.is_editable"
                class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center disabled border-dashed bg-light"
                style="border-style: dashed !important; width: 32px; height: 32px; opacity: 0.4; cursor: not-allowed;"
                disabled
              >
                <i class="bi bi-trash-fill text-muted"></i>
              </button>
              
              <!-- 自訂角色：主題橘色垃圾桶，無 tooltip -->
              <button
                v-else
                class="btn btn-outline-orange btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center"
                style="width: 32px; height: 32px;"
                @click="handleDeleteRole(r.role_id)"
              >
                <i class="bi bi-trash-fill"></i>
              </button>
            </div>
          </div>
          
          <hr class="my-2 text-muted" style="opacity: 0.15;" />
          
          <!-- 權限計數 -->
          <div class="d-flex align-items-center justify-content-between mt-1">
            <span class="small text-secondary">已啟用功能權限:</span>
            <span class="font-monospace text-dark fw-semibold" style="font-size: 0.85rem;">
              {{ rolePermissionsMap[r.role_id]?.length || 0 }} / {{ availablePermissions.length }} 個權限
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 權限設定 Drawer -->
  <el-drawer
    v-model="showDrawer"
    size="550px"
    direction="rtl"
    destroy-on-close
    class="roles-permission-drawer"
  >
    <template #header>
      <div class="d-flex align-items-center gap-2 w-100" style="height: 38px;">
        <template v-if="activeRole && !isRoleLocked(activeRole.role_id)">
          <!-- 編輯狀態 (Enter 或 Blur 即自動保存，寬度填滿並保留關閉按鈕空間) -->
          <div v-if="isEditingName" class="d-flex align-items-center flex-grow-1 me-4">
            <input 
              type="text" 
              class="form-control form-control-sm py-1 fw-bold fs-4 text-dark w-100" 
              style="height: 36px;"
              v-model="editableRoleName" 
              @keyup.enter="isEditingName = false"
              @blur="isEditingName = false"
              ref="nameInputRef"
            />
          </div>
          <!-- 唯讀狀態 (點擊名字或 icon 修改，附斜鉛筆) -->
          <div 
            v-else 
            class="d-flex align-items-center cursor-pointer role-name-title-container" 
            @click="startEditingName"
            title="點擊編輯角色名稱"
          >
            <span class="fs-4 fw-bold text-dark">{{ editableRoleName }}</span>
            <i class="bi bi-pencil-fill ms-2 text-secondary role-name-pencil-icon" style="font-size: 0.85rem;"></i>
          </div>
        </template>
        
        <!-- 系統鎖定角色 (公司負責人/管理員，唯讀) -->
        <template v-else>
          <span class="fs-4 fw-bold text-dark">{{ activeRole ? getRoleDisplayName(activeRole) : '' }}</span>
        </template>
      </div>
    </template>
    
    <!-- 預設角色模板 (拿掉外層淺灰色區塊與邊框) -->
    <div class="mb-3">
      <!-- 系統鎖定角色顯示文字 -->
      <div v-if="activeRole && isRoleLocked(activeRole.role_id)" class="text-secondary fw-normal py-1" style="font-size: 0.9rem;">
        系統預設角色
      </div>
      
      <!-- 自訂角色顯示選單 -->
      <select 
        v-else
        class="form-select rounded-3" 
        v-model="drawerTemplateId"
        @change="handleApplyDrawerTemplate"
      >
        <option value="" disabled selected hidden>從預設角色挑選</option>
        <option value="custom">自訂</option>
        <option v-for="t in roleTemplates" :key="t.templateId" :value="t.templateId">
          {{ formatTemplateName(t.templateName) }}
        </option>
      </select>
    </div>

    <!-- 摺疊面板 (Collapse) -->
    <el-collapse v-model="activeCollapseNames" class="permissions-collapse border-0">
      <el-collapse-item
        v-for="group in permissionGroups"
        :key="group.name"
        :name="group.name"
        class="border-bottom"
      >
        <!-- 自訂 Collapse Header -->
        <template #title>
          <div class="d-flex align-items-center justify-content-between w-100 pe-3">
            <div class="d-flex align-items-center gap-2">
              <i :class="group.icon" class="fs-6"></i>
              <span class="text-dark fw-bold" style="font-size: 0.95rem;">{{ group.name }}</span>
              <span class="text-secondary font-monospace ms-1" style="font-size: 0.8rem;">
                ({{ getGroupCheckedState(group).checkedCount }}/{{ getGroupCheckedState(group).totalCount }})
              </span>
            </div>
            <!-- 全選 Checkbox -->
            <div @click.stop>
              <el-checkbox
                :model-value="getGroupCheckedState(group).isAllChecked"
                :indeterminate="getGroupCheckedState(group).isIndeterminate"
                :disabled="activeRole && isRoleLocked(activeRole.role_id)"
                @change="(checked) => handleGroupAllCheckChange(group, checked)"
                class="mb-0"
              >
                全選
              </el-checkbox>
            </div>
          </div>
        </template>

        <!-- 面板內容：Checkbox Grid (col-sm-6 雙欄網格) -->
        <div class="p-3 bg-light-subtle rounded-3 border">
          <div class="row g-2">
            <div v-for="p in group.permissions" :key="p.permissionId" class="col-12 col-sm-6">
              <div 
                class="permission-item-card border rounded-3 bg-white h-100"
                :class="{ 'is-checked': drawerPermissions.includes(p.permissionId) }"
              >
                <el-checkbox
                  v-model="drawerPermissions"
                  :label="p.permissionId"
                  :disabled="activeRole && isRoleLocked(activeRole.role_id)"
                  class="w-100 h-100 d-flex align-items-center px-3 py-3 mb-0 me-0"
                >
                  <span class="text-dark fw-medium" style="font-size: 0.95rem;">{{ p.description }}</span>
                </el-checkbox>
              </div>
            </div>
          </div>
        </div>
      </el-collapse-item>
    </el-collapse>

    <!-- Footer -->
    <template #footer>
      <div class="d-flex align-items-center justify-content-end gap-2 p-3 border-top bg-white">
        <template v-if="activeRole && isRoleLocked(activeRole.role_id)">
          <button
            type="button"
            class="btn btn-secondary text-white px-3.5 py-2 rounded-3 fw-bold"
            @click="showDrawer = false"
          >
            關閉
          </button>
        </template>
        <template v-else>
          <button
            type="button"
            class="btn btn-secondary text-white px-3.5 py-2 rounded-3 fw-bold"
            @click="handleResetDrawer"
          >
            重設
          </button>
          <button
            type="button"
            class="btn btn-light border px-3.5 py-2 rounded-3 fw-semibold"
            @click="showDrawer = false"
          >
            取消
          </button>
          <button
            type="button"
            class="btn btn-primary text-white px-3.5 py-2 rounded-3 fw-bold"
            @click="handleSaveRolePermissions"
          >
            儲存
          </button>
        </template>
      </div>
    </template>
  </el-drawer>

  <!-- 新建自訂角色 Modal -->
  <div v-if="showCreateModal" class="modal-backdrop fade show"></div>
  <div v-if="showCreateModal" class="modal fade show d-block" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered modal-lg">
      <div class="modal-content border-0 rounded-4 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold">
            <i class="bi bi-shield-plus me-2 text-primary"></i>建立自訂角色
          </h5>
          <button type="button" class="btn-close" @click="showCreateModal = false"></button>
        </div>

        <form @submit.prevent="handleCreateRole">
          <div class="modal-body p-4">
            <div class="row g-3">
              <!-- 從預設角色挑選 -->
              <div class="col-12">
                <select class="form-select rounded-3" v-model="selectedTemplateId" @change="handleApplyTemplate">
                  <option value="" disabled selected hidden>從預設角色挑選</option>
                  <option value="custom">自訂</option>
                  <option v-for="t in roleTemplates" :key="t.templateId" :value="t.templateId">
                    {{ formatTemplateName(t.templateName) }}
                  </option>
                </select>
              </div>

              <!-- 名稱 -->
              <div class="col-12">
                <label class="form-label fw-semibold text-secondary">角色顯示名稱 <span class="text-danger">*</span></label>
                <input type="text" class="form-control" v-model="newRoleName" required placeholder="例如：行銷推廣" />
              </div>

              <!-- 初始權限勾選 -->
              <div class="col-12">
                <hr class="my-3 text-muted opacity-25" />
                <label class="form-label fw-semibold text-secondary d-block mb-3">配置初始功能權限</label>
                
                <el-collapse v-model="createActiveCollapseNames" class="permissions-collapse border-0">
                  <el-collapse-item
                    v-for="group in permissionGroups"
                    :key="group.name"
                    :name="group.name"
                    class="border-bottom"
                  >
                    <!-- Collapse Header -->
                    <template #title>
                      <div class="d-flex align-items-center justify-content-between w-100 pe-3">
                        <div class="d-flex align-items-center gap-2">
                          <i :class="group.icon" class="fs-6"></i>
                          <span class="text-dark fw-bold" style="font-size: 0.95rem;">{{ group.name }}</span>
                          <span class="text-secondary font-monospace ms-1" style="font-size: 0.8rem;">
                            ({{ getCreateGroupCheckedState(group).checkedCount }}/{{ getCreateGroupCheckedState(group).totalCount }})
                          </span>
                        </div>
                        <div @click.stop>
                          <el-checkbox
                            :model-value="getCreateGroupCheckedState(group).isAllChecked"
                            :indeterminate="getCreateGroupCheckedState(group).isIndeterminate"
                            @change="(checked) => handleCreateGroupAllCheckChange(group, checked)"
                            class="mb-0"
                          >
                            全選
                          </el-checkbox>
                        </div>
                      </div>
                    </template>

                    <!-- 面板內容 (雙欄網格) -->
                    <div class="p-3 bg-light-subtle rounded-3 border">
                      <div class="row g-2">
                        <div v-for="p in group.permissions" :key="p.permissionId" class="col-12 col-sm-6">
                          <div 
                            class="permission-item-card border rounded-3 bg-white h-100"
                            :class="{ 'is-checked': newRolePermissions.includes(p.permissionId) }"
                          >
                            <el-checkbox
                              v-model="newRolePermissions"
                              :label="p.permissionId"
                              class="w-100 h-100 d-flex align-items-center px-3 py-3 mb-0 me-0"
                            >
                              <span class="text-dark fw-medium" style="font-size: 0.95rem;">{{ p.description }}</span>
                            </el-checkbox>
                          </div>
                        </div>
                      </div>
                    </div>
                  </el-collapse-item>
                </el-collapse>
              </div>
            </div>
          </div>

          <div class="modal-footer border-0 pt-0 pb-4 px-4 gap-2">
            <button type="button" class="btn btn-light border px-3.5 py-2 rounded-3 fw-semibold" @click="showCreateModal = false">取消</button>
            <button type="submit" class="btn btn-primary px-3.5 py-2 rounded-3 text-white fw-bold">建立角色</button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<style scoped>
.form-label {
  font-size: 0.875rem;
  margin-bottom: 0.25rem;
}

.form-control, .form-select {
  border-radius: 0.5rem;
  font-size: 0.95rem;
  border-color: #ced4da;
}

.form-control:focus, .form-select:focus {
  border-color: var(--tap-primary, #e57346);
  box-shadow: 0 0 0 0.25rem rgba(229, 115, 70, 0.15);
}

.card {
  background-color: #ffffff;
}

.table th {
  font-size: 0.85rem;
  color: #64748b;
  font-weight: 600;
  vertical-align: middle;
}

/* Element Plus Checkbox 主題橘色客製化 (僅限可編輯角色，系統鎖定角色保持預設灰底唯讀樣式) */
:deep(.el-checkbox__input.is-checked:not(.is-disabled) .el-checkbox__inner) {
  background-color: var(--tap-primary, #e57346) !important;
  border-color: var(--tap-primary, #e57346) !important;
}

:deep(.el-checkbox__input.is-indeterminate:not(.is-disabled) .el-checkbox__inner) {
  background-color: var(--tap-primary, #e57346) !important;
  border-color: var(--tap-primary, #e57346) !important;
}

:deep(.el-checkbox__input.is-focus:not(.is-disabled) .el-checkbox__inner) {
  border-color: var(--tap-primary, #e57346) !important;
}

:deep(.el-checkbox__input:not(.is-disabled) .el-checkbox__inner:hover) {
  border-color: var(--tap-primary, #e57346) !important;
}



/* 摺疊面板自訂樣式 */
.permissions-collapse {
  --el-collapse-header-height: 52px;
  --el-collapse-header-bg-color: #ffffff;
  --el-collapse-content-bg-color: transparent;
  --el-collapse-content-padding: 0.75rem 0.5rem;
}

:deep(.el-collapse-item__header) {
  border-bottom: 1px solid #f1f5f9;
  padding-left: 0.5rem;
}

:deep(.el-collapse-item__wrap) {
  border-bottom: none;
}

.permission-item-card {
  transition: all 0.22s cubic-bezier(0.4, 0, 0.2, 1);
  border: 1px solid #e2e8f0;
}

.permission-item-card:hover {
  border-color: var(--tap-primary, #e57346) !important;
  background-color: #fffaf8 !important;
  transform: translateY(-1px);
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05), 0 2px 4px -1px rgba(0, 0, 0, 0.03);
}

.permission-item-card.is-checked {
  border-color: var(--tap-primary, #e57346) !important;
  background-color: rgba(229, 115, 70, 0.04) !important;
}

/* 確保 checkbox 佔滿整個 card 並優化文字折行與對齊 */
:deep(.permission-item-card .el-checkbox) {
  height: 100%;
  display: inline-flex;
}

:deep(.permission-item-card .el-checkbox__label) {
  white-space: normal;
  word-break: break-all;
  line-height: 1.4;
  padding-left: 8px;
}

.text-purple {
  color: #8b5cf6 !important;
}

.bg-light-subtle {
  background-color: #f8fafc !important;
}

.modal-backdrop {
  background-color: rgba(0, 0, 0, 0.5);
  position: fixed;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  z-index: 1040;
}

.modal {
  z-index: 1050;
}

/* 讓 Modal 中的 collapse 高度有限制並可滾動 */
.modal-body .permissions-collapse {
  max-height: 400px;
  overflow-y: auto;
  padding-right: 0.25rem;
}

.badge-role-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 0.7rem;
  font-weight: 600;
  padding: 0.15rem 0.4rem;
  border-radius: 0.375rem;
  line-height: 1;
}

.badge-role-tag-custom {
  background-color: rgba(229, 115, 70, 0.1) !important;
  color: var(--tap-primary, #e57346) !important;
  border: 1px solid rgba(229, 115, 70, 0.2) !important;
}

.btn-outline-orange {
  color: var(--tap-primary, #e57346);
  border-color: var(--tap-primary, #e57346);
  background-color: transparent;
  transition: all 0.2s ease;
}

.btn-outline-orange:hover,
.btn-outline-orange:focus,
.btn-outline-orange:active {
  color: #fff !important;
  background-color: var(--tap-primary, #e57346) !important;
  border-color: var(--tap-primary, #e57346) !important;
}
</style>

<style>
/* 全域覆寫：因為 el-drawer 會被 Teleport 到 body 下方，scoped style 無法成功觸及。
   此處使用特定 class 限制範圍，避免污染其他地方的 drawer 樣式。 */
.roles-permission-drawer .el-drawer__body {
  padding: 0.75rem 1.25rem 1.25rem 1.25rem !important; /* 保留頂部 0.75rem 的舒適間距 */
  display: flex;
  flex-direction: column;
  gap: 1rem;
  overflow-y: auto;
}

.roles-permission-drawer .el-drawer__header {
  margin-bottom: 0 !important;
  padding: 1.25rem 1.25rem 0.75rem 1.25rem !important; /* 保持底部 0.75rem 的對稱間距 */
  border-bottom: 1px solid #f1f5f9;
  font-weight: 700;
}

/* 角色名稱編輯微互動 */
.role-name-title-container {
  display: inline-flex;
  align-items: center;
  padding: 2px 6px;
  margin-left: -6px;
  border-radius: 4px;
  transition: all 0.15s ease;
}

.role-name-title-container:hover {
  background-color: #f1f5f9;
}

.role-name-title-container:hover .role-name-pencil-icon {
  color: var(--tap-primary, #e57346) !important;
}

.role-name-pencil-icon {
  transition: all 0.15s ease;
}

/* 行動版/窄螢幕下，調整權限設定側欄寬度，防止左側文字被截斷 */
@media (max-width: 576px) {
  .roles-permission-drawer {
    width: 100% !important;
  }
  .roles-permission-drawer.el-drawer {
    width: 100% !important;
  }
  .roles-permission-drawer .el-drawer {
    width: 100% !important;
  }
}
@media (max-width: 400px) {
  .roles-permission-drawer .el-drawer__body {
    padding: 0.5rem 0.75rem 1rem 0.75rem !important;
  }
  .roles-permission-drawer .el-drawer__header {
    padding: 1rem 0.75rem 0.5rem 0.75rem !important;
  }
}
</style>
