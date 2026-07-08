<script setup>
import { ref, watch } from 'vue';
import { useAuthStore } from '@/stores/auth.js';
import { useWorkspaceStore } from '@/stores/workspace.js';
import axios from '@/plugins/axios.js';
import { useToast } from '@/composables/useToast';

const props = defineProps({
  show: {
    type: Boolean,
    required: true
  }
});

const emit = defineEmits(['update:show']);

const authStore = useAuthStore();
const workspaceStore = useWorkspaceStore();
const toast = useToast();

const name = ref('');
const email = ref('');
const company = ref('');
const category = ref('system');
const description = ref('');
const isSubmitting = ref(false);

// 當 Modal 開啟時，若使用者為登入狀態則自動帶入相關資料
watch(() => props.show, (newVal) => {
  if (newVal) {
    if (authStore.isLoggedIn) {
      if (!name.value || name.value === '使用者') {
        name.value = authStore.userName || '';
      }
      if (!email.value) {
        email.value = authStore.userEmail || '';
      }
      if (!company.value && workspaceStore.orgName) {
        company.value = workspaceStore.orgName;
      }
    }
  }
});

const closeModal = () => {
  if (isSubmitting.value) return;
  emit('update:show', false);
};

const handleSubmit = async () => {
  isSubmitting.value = true;

  try {
    await axios.post('/api/submissions', {
      name: name.value,
      email: email.value,
      company: company.value,
      category: category.value,
      description: description.value
    });

    // 成功送出後，立刻關閉 Modal 並且顯示右下角 Toast 提示
    emit('update:show', false);
    toast.success('您的請求已成功送出！技術支援團隊將儘快與您聯繫。');

    // 清空表單欄位
    name.value = '';
    email.value = '';
    company.value = '';
    category.value = 'system';
    description.value = '';
  } catch (err) {
    console.error('Submit contact form error:', err);
    toast.error(err.response?.data?.message || '傳送失敗，請稍後再試。');
  } finally {
    isSubmitting.value = false;
  }
};
</script>

<template>
  <!-- 採 Bootstrap 標準 modal：modal-dialog-scrollable 讓標頭/按鈕固定、僅中間欄位捲動，
       且高度自動受限於視窗，大螢幕不變、矮螢幕自動貼合不溢出（按鈕不再被裁切） -->
  <div v-if="show" class="modal-backdrop fade show"></div>
  <div v-if="show" class="modal fade show d-block" tabindex="-1" style="z-index: 1055;" @click.self="closeModal">
    <div class="modal-dialog modal-dialog-centered modal-dialog-scrollable" style="max-width: 500px;">
      <div class="modal-content border-0 rounded-4 shadow-lg">

        <!-- 標頭 -->
        <div class="modal-header border-0 pb-2 pt-4 px-4">
          <h5 class="modal-title fw-bold text-dark d-flex align-items-center gap-2">
            <i class="bi bi-headset text-primary"></i> 聯絡技術支援
          </h5>
          <button type="button" class="btn-close shadow-none" @click="closeModal" :disabled="isSubmitting"></button>
        </div>

        <!-- 表單欄位（可捲動區） -->
        <div class="modal-body px-4 py-2">
          <form id="supportForm" @submit.prevent="handleSubmit">
            <!-- 聯絡姓名 -->
            <div class="mb-3 text-start">
              <label for="supportName" class="form-label small fw-bold text-dark">聯絡姓名 <span class="text-danger">*</span></label>
              <input
                type="text"
                class="form-control support-input focus-ring"
                id="supportName"
                placeholder="請輸入您的真實姓名"
                v-model="name"
                required
                :disabled="isSubmitting"
              />
            </div>

            <!-- 電子信箱 -->
            <div class="mb-3 text-start">
              <label for="supportEmail" class="form-label small fw-bold text-dark">電子信箱 <span class="text-danger">*</span></label>
              <input
                type="email"
                class="form-control support-input focus-ring"
                id="supportEmail"
                placeholder="請輸入可聯絡的電子信箱"
                v-model="email"
                required
                :disabled="isSubmitting"
              />
            </div>

            <!-- 主辦單位 -->
            <div class="mb-3 text-start">
              <label for="supportCompany" class="form-label small fw-bold text-dark">主辦單位 / 廠商名稱</label>
              <input
                type="text"
                class="form-control support-input focus-ring"
                id="supportCompany"
                placeholder="請輸入您的主辦單位名稱"
                v-model="company"
                :disabled="isSubmitting"
              />
            </div>

            <!-- 問題分類 -->
            <div class="mb-3 text-start">
              <label for="supportCategory" class="form-label small fw-bold text-dark">問題分類 <span class="text-danger">*</span></label>
              <select
                id="supportCategory"
                class="form-select support-input focus-ring"
                v-model="category"
                required
                :disabled="isSubmitting"
              >
                <option value="system">系統錯誤 / 無法操作</option>
                <option value="auth">帳號權限 / 登入問題</option>
                <option value="payment">金流結算 / 退款帳務</option>
                <option value="activity">活動上架 / 票券管理</option>
                <option value="suggestion">功能建議與合作</option>
                <option value="other">其他問題</option>
              </select>
            </div>

            <!-- 問題描述 -->
            <div class="mb-0 text-start">
              <label for="supportDesc" class="form-label small fw-bold text-dark">問題描述 <span class="text-danger">*</span></label>
              <textarea
                id="supportDesc"
                class="form-control support-input focus-ring"
                rows="4"
                placeholder="請詳述您遇到的問題，這能幫助我們更快為您解決。"
                v-model="description"
                required
                :disabled="isSubmitting"
              ></textarea>
            </div>
          </form>
        </div>

        <!-- 按鈕（固定於底部） -->
        <div class="modal-footer border-0 pt-2 pb-4 px-4 gap-2 flex-nowrap">
          <button
            type="button"
            class="btn btn-light w-50 py-2 rounded-3 text-dark fw-bold border"
            @click="closeModal"
            :disabled="isSubmitting"
          >
            取消
          </button>
          <button
            type="submit"
            form="supportForm"
            class="btn btn-primary w-50 py-2 rounded-3 text-white fw-bold d-flex align-items-center justify-content-center gap-2 m-0"
            :disabled="isSubmitting"
          >
            <span v-if="isSubmitting" class="spinner-border spinner-border-sm" role="status" aria-hidden="true"></span>
            <span>{{ isSubmitting ? '傳送中...' : '確認送出' }}</span>
          </button>
        </div>

      </div>
    </div>
  </div>
</template>

<style scoped>
.modal-backdrop {
  background-color: rgba(0, 0, 0, 0.5);
}

/* 輸入框聚焦時的主題色樣式 */
.support-input {
  border-radius: 0.5rem;
  font-size: 0.95rem;
  border: 1px solid #dee2e6;
  padding: 0.6rem 0.8rem;
}

.support-input:focus {
  border-color: var(--bs-primary);
  box-shadow: 0 0 0 0.25rem rgba(229, 115, 70, 0.25); /* 橘色淡影，配合品牌色 */
}

/* 捲動區捲軸美化 */
.modal-body::-webkit-scrollbar {
  width: 6px;
}
.modal-body::-webkit-scrollbar-track {
  background: transparent;
}
.modal-body::-webkit-scrollbar-thumb {
  background: #cbd5e1;
  border-radius: 10px;
}
.modal-body::-webkit-scrollbar-thumb:hover {
  background: #94a3b8;
}
</style>
