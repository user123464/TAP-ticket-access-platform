<script setup>
import { ref, onMounted, computed } from 'vue';
import { useRoute } from 'vue-router';
import axios from '@/plugins/axios.js';
import BaseAvatar from '@/components/common/BaseAvatar.vue';
import { useToast } from '@/composables/useToast';
import { useConfirm } from '@/composables/useConfirm';

const route = useRoute();
const orgId = computed(() => route.params.organizerId);
const toast = useToast();
const { confirm } = useConfirm();

// 成員資料列表
const members = ref([]);

// 組織角色清單
const orgRoles = ref([]);

// 邀請 Modal 狀態
const showInviteModal = ref(false);
const inviteEmail = ref('');
const inviteRole = ref('');
const inviteError = ref('');
const editingPendingMember = ref(null); // 正在編輯的待定成員
const isSubmittingInvite = ref(false); // 正在發送邀請中

// 編輯成員角色狀態
const showRoleModal = ref(false);
const editingMember = ref(null);
const selectedNewRole = ref('');

const ownerUserId = ref('');

// 載入成員資料與角色
const loadMembersAndRoles = async () => {
  // 嘗試從 localStorage 提取快取資料，用於加速畫面初始化渲染
  const cacheKey = `b2b_members_cache_${orgId.value}`;
  try {
    const cached = localStorage.getItem(cacheKey);
    if (cached) {
      const parsed = JSON.parse(cached);
      if (parsed.ownerUserId) ownerUserId.value = parsed.ownerUserId;
      if (parsed.orgRoles) orgRoles.value = parsed.orgRoles;
      if (parsed.members) members.value = parsed.members;
    }
  } catch (e) {
    console.warn('Failed to parse cached members', e);
  }

  try {
    // 0. 載入組織詳情以獲取負責人 ID
    const orgRes = await axios.get(`/api/organizer/${orgId.value}`);
    ownerUserId.value = orgRes.data.owner_user_id;

    // 1. 載入 Roles
    const rolesRes = await axios.get(`/api/organizer/${orgId.value}/roles`);
    orgRoles.value = rolesRes.data.data;

    // 2. 載入 Members
    const membersRes = await axios.get(`/api/organizer/${orgId.value}/members`);
    const newMembers = membersRes.data.data
      .map(m => ({
        id: m.memberId,
        userId: m.userId,
        name: m.name,
        email: m.email,
        roleId: m.roleId,
        status: m.status,
        joinedAt: m.joinedAt,
        invitedAt: m.invitedAt
      }))
      .filter(m => m.status !== 3); // 排除已撤銷 (REVOKED) 的成員/邀請
    
    // 更新狀態與快取
    members.value = newMembers;
    localStorage.setItem(cacheKey, JSON.stringify({
      ownerUserId: ownerUserId.value,
      orgRoles: orgRoles.value,
      members: newMembers
    }));
  } catch (error) {
    console.error('Failed to load members or roles', error);
  }
};

// 發送成員邀請
const handleInviteMember = async () => {
  inviteError.value = '';
  const emailInput = inviteEmail.value.trim().toLowerCase();

  if (!emailInput || !inviteRole.value) {
    inviteError.value = '請填寫電子郵件並選擇角色！';
    return;
  }

  isSubmittingInvite.value = true;
  try {
    await axios.post(`/api/organizer/${orgId.value}/invite`, {
      email: emailInput,
      roleId: inviteRole.value
    });
    
    closeInviteModal();
    toast.success(`邀請信已成功發送至 ${emailInput}`);
    loadMembersAndRoles();
  } catch (error) {
    console.error('Invite failed', error);
    inviteError.value = error.response?.data?.message || '邀請發送失敗';
  } finally {
    isSubmittingInvite.value = false;
  }
};

// 撤銷邀請 / 移除成員 (呼叫同一個 API，由後端設為 REVOKED)
const handleRemoveOrRevokeMember = async (memberId, memberName, isRevoke = false) => {
  const actionText = isRevoke ? '撤銷此成員的邀請' : `將成員 ${memberName} 移出組織`;
  const ok = await confirm({
    title: isRevoke ? '撤銷邀請' : '移出組織',
    message: `確定要${actionText}嗎？`,
    confirmText: isRevoke ? '撤銷邀請' : '移出組織',
    variant: 'danger'
  });
  if (ok) {
    try {
      await axios.delete(`/api/organizer/${orgId.value}/members/${memberId}`);
      toast.success('操作成功');

      if (showInviteModal.value) closeInviteModal();
      if (showRoleModal.value) {
        showRoleModal.value = false;
        editingMember.value = null;
      }

      loadMembersAndRoles();
    } catch (error) {
      console.error('Remove failed', error);
      toast.error('操作失敗：' + (error.response?.data?.message || error.message));
    }
  }
};

const handleRevokeInvite = (memberId) => handleRemoveOrRevokeMember(memberId, '', true);
const handleRemoveMember = (memberId) => handleRemoveOrRevokeMember(memberId, '', false);

// 重新發送邀請
const handleResendInvite = async (memberId) => {
  const target = members.value.find(m => m.id === memberId);
  if (target) {
    try {
      // 若後端不支援重複邀請，建議前端先撤銷後再邀請，或呼叫特定 resend API
      // 這裡簡單模擬呼叫 invite
      await axios.post(`/api/organizer/${orgId.value}/invite`, {
        email: target.email,
        roleId: target.roleId
      });
      toast.success(`已重新發送邀請通知給 ${target.name}`);
      loadMembersAndRoles();
    } catch (error) {
      toast.error('重新發送失敗：' + (error.response?.data?.message || '目前此功能受後端限制'));
    }
  }
};

// 開啟新建邀請 Modal
const openInviteModalForCreate = () => {
  editingPendingMember.value = null;
  inviteEmail.value = '';
  inviteRole.value = '';
  inviteError.value = '';
  showInviteModal.value = true;
};

// 開啟待定邀請編輯 Modal
const openInviteModalForEdit = (member) => {
  editingPendingMember.value = member;
  inviteEmail.value = member.email;
  inviteRole.value = member.roleId || '';
  showInviteModal.value = true;
};

// 關閉/重設邀請 Modal
const closeInviteModal = () => {
  showInviteModal.value = false;
  inviteEmail.value = '';
  inviteRole.value = '';
  inviteError.value = '';
  editingPendingMember.value = null;
};

// 從 Modal 撤銷邀請
const handleRevokeInviteFromModal = () => {
  if (editingPendingMember.value) {
    handleRevokeInvite(editingPendingMember.value.id);
  }
};

// 從 Modal 移出成員
const handleRemoveMemberFromModal = () => {
  if (editingMember.value) {
    handleRemoveOrRevokeMember(editingMember.value.id, editingMember.value.name, false);
  }
};

// 變更角色 Modal 喚起
const openRoleEdit = (member) => {
  editingMember.value = member;
  selectedNewRole.value = member.roleId;
  showRoleModal.value = true;
};

const handleUpdateMemberRole = async () => {
  if (editingMember.value) {
    try {
      await axios.put(`/api/organizer/${orgId.value}/members/${editingMember.value.id}`, {
        role_id: selectedNewRole.value
      });
      toast.success('已成功更新角色');
      showRoleModal.value = false;
      editingMember.value = null;
      loadMembersAndRoles();
    } catch (error) {
      console.error('Update role failed', error);
      toast.error('更新失敗：' + (error.response?.data?.message || error.message));
    }
  }
};

const getRoleName = (roleId) => {
  if (roleId === 'ORGANIZER') return '公司負責人';
  const role = orgRoles.value.find(r => r.roleId === roleId);
  if (role) {
    if (role.roleName === 'Admin') return '管理員';
    if (role.roleName === 'Accountant') return '財務專員';
    return role.roleName;
  }
  return roleId;
};

const getMemberStatusBadge = (status) => {
  switch (status) {
    case 0: return { text: '邀請中', class: 'bg-warning text-dark border-warning' };
    case 1: return { text: '已加入', class: 'bg-success text-white border-success' };
    case 2: return { text: '已拒絕', class: 'bg-danger text-white border-danger' };
    case 3: return { text: '已撤銷', class: 'bg-secondary text-white border-secondary' };
    default: return { text: '未知', class: 'bg-light text-secondary border' };
  }
};

const formatDate = (isoStr) => {
  if (!isoStr) return '-';
  return new Date(isoStr).toLocaleDateString('zh-TW', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit'
  });
};

onMounted(() => {
  loadMembersAndRoles();
});
</script>

<template>
  <div class="card border rounded-4 shadow-sm p-4">
    <!-- 標題與邀請按鈕 -->
    <div class="d-flex align-items-center justify-content-between flex-wrap gap-2 mb-4">
      <h5 class="fw-bold mb-0"><i class="bi bi-people me-2 text-primary"></i>成員名單與加入狀態</h5>
      <button @click="openInviteModalForCreate" class="btn btn-primary text-white rounded-3 fw-bold px-3 py-2 flex-shrink-0 text-nowrap">
        <i class="bi bi-plus-lg me-1"></i>邀請新成員
      </button>
    </div>

    <!-- 桌機版成員列表 Table (減至 4 欄，防折行與左右拉動) -->
    <div class="d-none d-md-block table-responsive border rounded-3 mb-3">
      <table class="table table-hover align-middle mb-0">
        <thead class="table-light text-nowrap">
          <tr>
            <th scope="col" class="py-3 px-4" style="width: 40%; min-width: 220px;">成員資訊</th>
            <th scope="col" class="py-3" style="width: 25%; min-width: 130px;">目前角色</th>
            <th scope="col" class="py-3" style="width: 25%; min-width: 150px;">加入狀態與時間</th>
            <th scope="col" class="py-3 text-start px-4" style="width: 10%; min-width: 80px;">編輯</th>
          </tr>
        </thead>
        <tbody>
          <tr v-for="m in members" :key="m.id" :class="{ 'text-muted-row': m.status !== 1 }">
            <!-- 成員資訊 (合併頭像、姓名與 Email 垂直堆疊) -->
            <td class="py-3 px-4">
              <div class="d-flex align-items-center gap-3">
                <BaseAvatar :seed="m.name" size="38" alt="Avatar" />
                <div class="d-flex flex-column min-w-0">
                  <span class="fw-bold text-dark text-truncate d-block" :title="m.name" style="max-width: 180px;">{{ m.name }}</span>
                  <span class="text-secondary small font-monospace text-truncate d-block" :title="m.email" style="max-width: 200px;">{{ m.email }}</span>
                </div>
              </div>
            </td>

            <!-- 目前角色 -->
            <td class="text-nowrap">
              <span class="badge bg-light text-dark border small fw-semibold">
                {{ m.userId === ownerUserId ? '公司負責人' : getRoleName(m.roleId) }}
              </span>
            </td>

            <!-- 加入狀態與時間 (垂直堆疊) -->
            <td>
              <div class="d-flex flex-column align-items-start gap-1">
                <span class="badge border py-1 px-2.5 rounded-pill" :class="getMemberStatusBadge(m.status).class">
                  {{ getMemberStatusBadge(m.status).text }}
                </span>
                <span class="text-secondary small font-monospace" style="font-size: 0.8rem;">
                  <span v-if="m.status === 1">{{ formatDate(m.joinedAt) }} 加入</span>
                  <span v-else-if="m.status === 0">{{ formatDate(m.invitedAt) }} 邀請</span>
                  <span v-else-if="m.status === 2">{{ formatDate(m.invitedAt) }} 拒絕</span>
                  <span v-else-if="m.status === 3">{{ formatDate(m.invitedAt) }} 撤銷</span>
                </span>
              </div>
            </td>

            <!-- 編輯欄位 -->
            <td class="py-3 px-4 text-start text-nowrap">
              <div class="d-flex gap-2 justify-content-start align-items-center">
                <!-- 狀態 1：已加入 -->
                <template v-if="m.status === 1">
                  <button 
                    v-if="m.userId === ownerUserId"
                    class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center disabled border-dashed bg-light" 
                    title="公司負責人，不可變更角色與移出組織"
                    style="border-style: dashed !important; width: 32px; height: 32px;"
                    disabled
                  >
                    <i class="bi bi-gear-fill text-muted"></i>
                  </button>
                  <button 
                    v-else
                    @click="openRoleEdit(m)" 
                    class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center" 
                    title="角色設定與移出"
                    style="width: 32px; height: 32px;"
                  >
                    <i class="bi bi-gear-fill"></i>
                  </button>
                </template>

                <!-- 狀態 0：邀請中 -->
                <template v-else-if="m.status === 0">
                  <button 
                    @click="openInviteModalForEdit(m)" 
                    class="btn btn-outline-primary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center" 
                    title="更換身份、重新邀請或撤銷邀請"
                    style="width: 32px; height: 32px;"
                  >
                    <i class="bi bi-envelope-fill"></i>
                  </button>
                </template>

                <!-- 其他狀態 -->
                <template v-else>
                  <button 
                    @click="openInviteModalForEdit(m)" 
                    class="btn btn-outline-primary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center"
                    title="重新邀請"
                    style="width: 32px; height: 32px;"
                  >
                    <i class="bi bi-envelope-fill"></i>
                  </button>
                </template>
              </div>
            </td>
          </tr>
        </tbody>
      </table>
    </div>

    <!-- 手機版響應式卡片清單 (Card List) -->
    <div class="d-block d-md-none mb-3">
      <div v-if="members.length === 0" class="text-center py-5 text-secondary border rounded-3 bg-light">
        <i class="bi bi-people text-muted fs-2 d-block mb-2"></i>
        <span>目前無任何成員資料</span>
      </div>
      <div v-else class="d-flex flex-column gap-3">
        <div 
          v-for="m in members" 
          :key="m.id" 
          class="card border rounded-3 p-3 shadow-sm"
          :class="{ 'bg-light text-muted-row': m.status !== 1 }"
        >
          <div class="d-flex align-items-start justify-content-between">
            <!-- 頭像與姓名/Email -->
            <div class="d-flex align-items-center gap-3 min-w-0">
              <BaseAvatar :seed="m.name" size="40" alt="Avatar" />
              <div class="d-flex flex-column min-w-0">
                <span class="fw-bold text-dark text-truncate d-block" :title="m.name" style="max-width: 160px;">{{ m.name }}</span>
                <span class="text-secondary small font-monospace text-truncate d-block" :title="m.email" style="max-width: 180px;">{{ m.email }}</span>
              </div>
            </div>
            
            <!-- 操作按鈕 -->
            <div class="d-flex align-items-center">
              <template v-if="m.status === 1">
                <button 
                  v-if="m.userId === ownerUserId"
                  class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center disabled border-dashed bg-light" 
                  title="公司負責人"
                  style="border-style: dashed !important; width: 32px; height: 32px;"
                  disabled
                >
                  <i class="bi bi-gear-fill text-muted"></i>
                </button>
                <button 
                  v-else
                  @click="openRoleEdit(m)" 
                  class="btn btn-outline-secondary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center" 
                  title="角色設定與移出"
                  style="width: 32px; height: 32px;"
                >
                  <i class="bi bi-gear-fill"></i>
                </button>
              </template>
              <template v-else-if="m.status === 0">
                <button 
                  @click="openInviteModalForEdit(m)" 
                  class="btn btn-outline-primary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center" 
                  title="重新邀請或撤銷邀請"
                  style="width: 32px; height: 32px;"
                >
                  <i class="bi bi-envelope-fill"></i>
                </button>
              </template>
              <template v-else>
                <button 
                  @click="openInviteModalForEdit(m)" 
                  class="btn btn-outline-primary btn-sm rounded-circle p-2 d-inline-flex align-items-center justify-content-center"
                  title="重新邀請"
                  style="width: 32px; height: 32px;"
                >
                  <i class="bi bi-envelope-fill"></i>
                </button>
              </template>
            </div>
          </div>
          
          <hr class="my-2 text-muted" style="opacity: 0.15;" />
          
          <!-- 角色與狀態/時間 -->
          <div class="d-flex align-items-center justify-content-between mt-1">
            <div class="d-flex align-items-center">
              <span class="small text-secondary me-2">角色:</span>
              <span class="badge bg-light text-dark border small fw-semibold">
                {{ m.userId === ownerUserId ? '公司負責人' : getRoleName(m.roleId) }}
              </span>
            </div>
            <div class="d-flex align-items-center gap-2">
              <span class="badge border py-1 px-2 rounded-pill small" :class="getMemberStatusBadge(m.status).class">
                {{ getMemberStatusBadge(m.status).text }}
              </span>
              <span class="text-secondary small font-monospace" style="font-size: 0.75rem;">
                <span v-if="m.status === 1">{{ formatDate(m.joinedAt) }}</span>
                <span v-else>{{ formatDate(m.invitedAt) }}</span>
              </span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>

  <!-- 邀請成員 Modal -->
  <div v-if="showInviteModal" class="modal-backdrop fade show"></div>
  <div v-if="showInviteModal" class="modal fade show d-block" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border-0 rounded-4 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold"><i class="bi bi-person-plus-fill me-2 text-primary"></i>{{ editingPendingMember ? '修改與重送邀請' : '發送團隊加入邀請' }}</h5>
          <button type="button" class="btn-close" @click="closeInviteModal"></button>
        </div>

        <form @submit.prevent="handleInviteMember">
          <div class="modal-body p-4">
            
            <!-- 錯誤回饋提示 -->
            <div v-if="inviteError" class="alert alert-danger border-0 small rounded-3 mb-3">
              <i class="bi bi-exclamation-triangle-fill me-1"></i> {{ inviteError }}
            </div>

            <!-- Email -->
            <div class="mb-3">
              <label class="form-label fw-semibold text-secondary">受邀人電子郵件 (Email) <span class="text-danger">*</span></label>
              <input type="email" class="form-control" v-model="inviteEmail" required :disabled="editingPendingMember !== null" placeholder="請輸入受邀人已註冊的 Email" />
            </div>

            <!-- Role -->
            <div class="mb-3">
              <label class="form-label fw-semibold text-secondary">指派角色權限 <span class="text-danger">*</span></label>
              <select class="form-select" v-model="inviteRole" required>
                <option value="" disabled selected>-- 請指派角色 --</option>
                <option v-for="r in orgRoles.filter(role => role.roleId !== 'OWNER' && role.roleId !== 'ORGANIZER')" :key="r.roleId" :value="r.roleId">
                  {{ r.roleName === 'Admin' ? '管理員' : (r.roleName === 'Accountant' ? '財務專員' : r.roleName) }}
                </option>
              </select>
            </div>
          </div>

          <div class="modal-footer border-0 pt-0 pb-4 px-4">
            <template v-if="editingPendingMember">
              <div class="row g-2 w-100 m-0">
                <div class="col-3">
                  <button 
                    type="button" 
                    class="btn btn-outline-danger w-100 py-2 rounded-3 fw-semibold" 
                    @click="handleRevokeInviteFromModal"
                    :disabled="isSubmittingInvite"
                  >
                    撤銷邀請
                  </button>
                </div>
                <div class="col-3 offset-3">
                  <button 
                    type="button" 
                    class="btn btn-light border w-100 py-2 rounded-3 fw-semibold" 
                    @click="closeInviteModal"
                    :disabled="isSubmittingInvite"
                  >
                    取消
                  </button>
                </div>
                <div class="col-3">
                  <button 
                    type="submit" 
                    class="btn btn-primary w-100 py-2 rounded-3 text-white fw-bold position-relative"
                    :disabled="isSubmittingInvite"
                  >
                    <span :class="{ 'invisible': isSubmittingInvite }">重新發送</span>
                    <span v-if="isSubmittingInvite" class="position-absolute start-50 top-50 translate-middle">
                      <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                    </span>
                  </button>
                </div>
              </div>
            </template>
            <template v-else>
              <div class="d-flex justify-content-end gap-2 w-100">
                <button 
                  type="button" 
                  class="btn btn-light border px-3 py-2 rounded-3 fw-semibold" 
                  @click="closeInviteModal"
                  :disabled="isSubmittingInvite"
                >
                  取消
                </button>
                <button 
                  type="submit" 
                  class="btn btn-primary px-4 py-2 rounded-3 text-white fw-bold position-relative"
                  :disabled="isSubmittingInvite"
                >
                  <span :class="{ 'invisible': isSubmittingInvite }">發送邀請</span>
                  <span v-if="isSubmittingInvite" class="position-absolute start-50 top-50 translate-middle">
                    <span class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
                  </span>
                </button>
              </div>
            </template>
          </div>
        </form>
      </div>
    </div>
  </div>

  <!-- 變更成員角色 Modal -->
  <div v-if="showRoleModal" class="modal-backdrop fade show"></div>
  <div v-if="showRoleModal" class="modal fade show d-block" tabindex="-1">
    <div class="modal-dialog modal-dialog-centered">
      <div class="modal-content border-0 rounded-4 shadow-lg">
        <div class="modal-header border-0 pb-0 pt-4 px-4">
          <h5 class="modal-title fw-bold"><i class="bi bi-shield-check me-2 text-primary"></i>變更成員角色</h5>
          <button type="button" class="btn-close" @click="showRoleModal = false; editingMember = null"></button>
        </div>

        <form @submit.prevent="handleUpdateMemberRole">
          <div class="modal-body p-4" v-if="editingMember">
            <p class="small text-secondary mb-3">
              正在調整成員 <strong>{{ editingMember.name }}</strong> ({{ editingMember.email }}) 的角色。變更後該成員將立即繼承新角色的所有權限。
            </p>
            
            <div class="mb-3">
              <label class="form-label fw-semibold text-secondary">選擇新角色</label>
              <select class="form-select" v-model="selectedNewRole" required>
                <option v-for="r in orgRoles.filter(role => role.roleId !== 'OWNER' && role.roleId !== 'ORGANIZER')" :key="r.roleId" :value="r.roleId">
                  {{ r.roleName === 'Admin' ? '管理員' : (r.roleName === 'Accountant' ? '財務專員' : r.roleName) }}
                </option>
              </select>
            </div>
          </div>

          <div class="modal-footer border-0 pt-0 pb-4 px-4">
            <div class="row g-2 w-100 m-0">
              <div class="col-3">
                <button 
                  type="button" 
                  class="btn btn-outline-danger w-100 py-2 rounded-3 fw-semibold" 
                  @click="handleRemoveMemberFromModal"
                >
                  移出組織
                </button>
              </div>
              <div class="col-3 offset-3">
                <button 
                  type="button" 
                  class="btn btn-light border w-100 py-2 rounded-3 fw-semibold" 
                  @click="showRoleModal = false; editingMember = null"
                >
                  取消
                </button>
              </div>
              <div class="col-3">
                <button 
                  type="submit" 
                  class="btn btn-primary w-100 py-2 rounded-3 text-white fw-bold"
                >
                  保存變更
                </button>
              </div>
            </div>
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

.table td {
  vertical-align: middle;
}

.border-dashed {
  border-style: dashed !important;
}

.cursor-help {
  cursor: help;
}

.text-muted-row {
  color: var(--tap-light-blue, #64748b);
}

.text-muted-row td {
  color: var(--tap-light-blue, #64748b) !important;
}

.text-muted-row .text-dark {
  color: var(--tap-light-blue, #64748b) !important;
}

.text-muted-row img {
  filter: grayscale(80%) opacity(70%);
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

.min-w-0 {
  min-width: 0;
}
</style>
