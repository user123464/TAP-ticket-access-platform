<script setup>
import { ref, onMounted, computed, watch } from 'vue';
import { useRouter } from 'vue-router';
import AvatarEditModal from '@/components/common/AvatarEditModal.vue';
import BaseAvatar from '@/components/common/BaseAvatar.vue';
import axios from '@/plugins/axios.js';
import { useAuthStore } from '@/stores/auth.js';
import { useToast } from '@/composables/useToast';
import { useCachedResource } from '@/composables/useCachedResource';

const router = useRouter();
const authStore = useAuthStore();
const toast = useToast();

// 表單資料
const email = ref('');
const name = ref('');
const phone = ref('');
const gender = ref(''); // 'M' | 'F' | 'O' | ''
const avatarUrl = ref('');



// 生日下拉選單相關變數與計算
const birthYear = ref('');
const birthMonth = ref('');
const birthDay = ref('');

const currentYear = new Date().getFullYear();
const years = Array.from({ length: 100 }, (_, i) => currentYear - i); // [2026, 2025, ..., 1927]
const months = Array.from({ length: 12 }, (_, i) => i + 1); // [1, 2, ..., 12]

const daysInMonth = computed(() => {
    if (!birthYear.value || !birthMonth.value) return 31;
    return new Date(parseInt(birthYear.value), parseInt(birthMonth.value), 0).getDate();
});

const days = computed(() => {
    return Array.from({ length: daysInMonth.value }, (_, i) => i + 1);
});

// 當月份天數上限縮小時，若原本選取的天數大於最大天數，則自動縮回最大天數
watch(daysInMonth, (newMax) => {
    if (birthDay.value && parseInt(birthDay.value) > newMax) {
        birthDay.value = String(newMax);
    }
});

// 台灣縣市清單
const cities = [
    '基隆市', '臺北市', '新北市', '桃園市', '新竹市', 
    '新竹縣', '苗栗縣', '臺中市', '彰化縣', '南投縣', 
    '雲林縣', '嘉義市', '嘉義縣', '臺南市', '高雄市', 
    '屏東縣', '宜蘭縣', '花蓮縣', '臺東縣', '澎湖縣', 
    '金門縣', '連江縣'
];
const selectedCity = ref('');
const detailAddress = ref('');

// 將後端 UserProfileResponse DTO 攤平填入各表單欄位
const applyProfile = (profile) => {
    if (!profile) return;

    email.value = profile.email || '';
    name.value = profile.name || '';
    phone.value = profile.phone || '';
    gender.value = profile.gender || '';
    avatarUrl.value = profile.avatarUrl || '';

    // 解析生日為 年、月、日
    const savedBirthDate = profile.birthDate || '';
    if (savedBirthDate && savedBirthDate.includes('-')) {
        const parts = savedBirthDate.split('-');
        if (parts.length === 3) {
            birthYear.value = parseInt(parts[0]).toString();
            birthMonth.value = parseInt(parts[1]).toString();
            birthDay.value = parseInt(parts[2]).toString();
        }
    } else {
        birthYear.value = '';
        birthMonth.value = '';
        birthDay.value = '';
    }

    // 解析地址為 縣市 與 詳細地址
    const savedAddress = profile.address || '';
    let matchedCity = '';
    for (const city of cities) {
        if (savedAddress.startsWith(city)) {
            matchedCity = city;
            break;
        }
    }
    if (matchedCity) {
        selectedCity.value = matchedCity;
        detailAddress.value = savedAddress.substring(matchedCity.length);
    } else {
        selectedCity.value = '';
        detailAddress.value = savedAddress;
    }
};

// 快取優先載入：data 一開始就是上次的個資快取，避免頭像 / 欄位先空白再被覆蓋造成閃爍
const { data: cachedProfile, refresh: refreshProfile, mutate: mutateProfile } =
    useCachedResource('user-profile', () => axios.get('/api/user/profile').then(r => r.data));

// 1. 立即用快取值把畫面畫出來（不閃）
applyProfile(cachedProfile.value);

// 2. 背景向後端取最新資料，回來後靜默更新
onMounted(async () => {
    try {
        const profile = await refreshProfile();
        applyProfile(profile);

        // 同步更新 Pinia Store，確保 Navbar 頭像與後端資料庫最新資料一致
        authStore.updateProfile({
            name: profile.name,
            avatarUrl: profile.avatarUrl
        });
    } catch (e) {
        console.error('Failed to load user profile from backend', e);
        // 若已有快取可顯示就不打擾使用者，僅在完全沒資料時提示
        if (!cachedProfile.value) {
            toast.error('無法載入個人資料，請重新整理頁面');
        }
    }
});

// 變更頭像彈窗
const avatarModalRef = ref(null);
const isSaving = ref(false);

const handleOpenAvatarModal = () => {
    avatarModalRef.value?.show();
};

// 儲存資料
const handleSave = async () => {
    if (!name.value.trim()) {
        toast.error('請填寫姓名');
        return;
    }

    if (isSaving.value) return;
    isSaving.value = true;

    // 拼接縣市與詳細地址
    const fullAddress = (selectedCity.value || '') + detailAddress.value.trim();

    // 拼接生日為 YYYY-MM-DD
    let fullBirthDate = null;
    if (birthYear.value && birthMonth.value && birthDay.value) {
        const y = birthYear.value;
        const m = birthMonth.value.padStart(2, '0');
        const d = birthDay.value.padStart(2, '0');
        fullBirthDate = `${y}-${m}-${d}`;
    }

    try {
        // 先送出尚未上傳的裁切頭像（裁切時僅本地預覽，按下儲存才真正上傳）
        let finalAvatarUrl = avatarUrl.value;
        const uploadedUrl = await avatarModalRef.value?.commitUpload();
        if (uploadedUrl) {
            finalAvatarUrl = uploadedUrl;
        }

        const response = await axios.put('/api/user/profile', {
            name: name.value,
            phone: phone.value,
            gender: gender.value,
            birthDate: fullBirthDate,
            address: fullAddress,
            avatarUrl: finalAvatarUrl
        });

        const profile = response.data; // 更新後的 UserProfileResponse DTO

        // 後端頭像採唯一檔名，URL 每次都不同，直接以回傳值為準即可觸發本頁預覽與 Navbar 更新
        avatarUrl.value = profile.avatarUrl || '';

        // 把最新 DTO 寫回資源快取，下次進頁面直接顯示最新值、不會閃舊資料
        mutateProfile(profile);

        // 同步更新 Pinia Store 狀態與 Local Storage 快取
        authStore.updateProfile({
            name: profile.name,
            avatarUrl: profile.avatarUrl
        });

        toast.success('個人資料已儲存');
    } catch (err) {
        console.error('Failed to save user profile', err);
        toast.error(err.response?.data?.message || '儲存個人資料失敗，請稍後再試');
    } finally {
        isSaving.value = false;
    }
};

</script>

<template>
  <div class="card profile-card shadow-sm border rounded-4 p-4">
    <!-- 頭像區塊 -->
    <div class="text-center mb-4">
      <div class="avatar-wrapper d-inline-block position-relative cursor-pointer" @click="handleOpenAvatarModal" title="更換頭像">
        <BaseAvatar :src="avatarUrl" :seed="name || email || 'default'" size="120" alt="User Avatar" class="avatar-preview" />
        <!-- 相機徽章 -->
        <div class="camera-badge d-flex align-items-center justify-content-center rounded-circle border shadow-sm" title="更換頭像">
          <i class="bi bi-camera-fill"></i>
        </div>
      </div>
    </div>

    <form @submit.prevent="handleSave">
      <div class="row g-4">
        <!-- 電子郵件 (唯讀) -->
        <div class="col-12">
          <label for="email" class="form-label fw-semibold text-secondary">電子郵件 (帳號)</label>
          <input type="email" id="email" class="form-control form-control-lg bg-light" :value="email" readonly disabled />
        </div>

        <!-- 姓名 -->
        <div class="col-md-6">
          <label for="name" class="form-label fw-semibold text-secondary">姓名 <span class="text-danger">*</span></label>
          <input type="text" id="name" class="form-control form-control-lg" v-model="name" required placeholder="請輸入姓名" />
        </div>

        <!-- 電話 -->
        <div class="col-md-6">
          <label for="phone" class="form-label fw-semibold text-secondary">聯絡電話</label>
          <input type="tel" id="phone" class="form-control form-control-lg" v-model="phone" placeholder="例如：0912345678" />
        </div>

        <!-- 性別 -->
        <div class="col-12">
          <label class="form-label fw-semibold text-secondary d-block">性別</label>
          <div class="row g-2">
            <div class="col-3">
              <label class="gender-card-label text-center p-2 border rounded-3 cursor-pointer d-flex flex-column align-items-center gap-1 w-100 h-100"
                :class="{ 'active': gender === 'M' }">
                <input type="radio" name="gender" value="M" v-model="gender" class="d-none">
                <i class="bi bi-gender-male fs-4 text-primary-icon"></i>
                <span class="text-label">男</span>
              </label>
            </div>
            <div class="col-3">
              <label class="gender-card-label text-center p-2 border rounded-3 cursor-pointer d-flex flex-column align-items-center gap-1 w-100 h-100"
                :class="{ 'active': gender === 'F' }">
                <input type="radio" name="gender" value="F" v-model="gender" class="d-none">
                <i class="bi bi-gender-female fs-4 text-primary-icon"></i>
                <span class="text-label">女</span>
              </label>
            </div>
            <div class="col-3">
              <label class="gender-card-label text-center p-2 border rounded-3 cursor-pointer d-flex flex-column align-items-center gap-1 w-100 h-100"
                :class="{ 'active': gender === 'O' }">
                <input type="radio" name="gender" value="O" v-model="gender" class="d-none">
                <i class="bi bi-gender-ambiguous fs-4 text-primary-icon"></i>
                <span class="text-label">其他</span>
              </label>
            </div>
            <div class="col-3">
              <label class="gender-card-label text-center p-2 border rounded-3 cursor-pointer d-flex flex-column align-items-center gap-1 w-100 h-100"
                :class="{ 'active': gender === '' }">
                <input type="radio" name="gender" value="" v-model="gender" class="d-none">
                <i class="bi bi-eye-slash fs-4 text-primary-icon"></i>
                <span class="text-label">不透露</span>
              </label>
            </div>
          </div>
        </div>

        <!-- 生日 -->
        <div class="col-12">
          <label class="form-label fw-semibold text-secondary">出生日期</label>
          <div class="row g-2">
            <div class="col-4">
              <select class="form-select form-select-lg" v-model="birthYear">
                <option value="">年</option>
                <option v-for="y in years" :key="y" :value="String(y)">{{ y }} 年</option>
              </select>
            </div>
            <div class="col-4">
              <select class="form-select form-select-lg" v-model="birthMonth">
                <option value="">月</option>
                <option v-for="m in months" :key="m" :value="String(m)">{{ m }} 月</option>
              </select>
            </div>
            <div class="col-4">
              <select class="form-select form-select-lg" v-model="birthDay">
                <option value="">日</option>
                <option v-for="d in days" :key="d" :value="String(d)">{{ d }} 日</option>
              </select>
            </div>
          </div>
        </div>

        <!-- 地址 -->
        <div class="col-12">
          <label class="form-label fw-semibold text-secondary">通訊地址</label>
          <div class="row g-2">
            <div class="col-md-4">
              <select class="form-select form-select-lg" v-model="selectedCity">
                <option value="">請選擇縣市</option>
                <option v-for="city in cities" :key="city" :value="city">{{ city }}</option>
              </select>
            </div>
            <div class="col-md-8">
              <input type="text" class="form-control form-control-lg" v-model="detailAddress" placeholder="請輸入詳細地址" />
            </div>
          </div>
        </div>
      </div>

      <!-- 動作按鈕 -->
      <div class="text-end mt-5">
        <button type="submit" class="btn btn-primary px-4 py-2 text-white rounded-3 fw-bold shadow-sm w-100 w-sm-auto" :disabled="isSaving">
          <span v-if="isSaving" class="spinner-border spinner-border-sm me-2" role="status"></span>
          {{ isSaving ? '儲存中...' : '儲存修改' }}
        </button>
      </div>
    </form>
  </div>

  <!-- 變更頭像 Modal -->
  <AvatarEditModal
    ref="avatarModalRef"
    v-model="avatarUrl"
    mode="personal"
    title="變更個人頭像"
    :defer-upload="true"
  />
</template>

<style scoped>
.profile-card {
  background-color: #ffffff;
  border-color: #dee2e6 !important;
}

.avatar-wrapper {
  position: relative;
}

.avatar-wrapper:hover .avatar-preview {
  filter: brightness(0.85);
}

.avatar-preview {
  width: 120px;
  height: 120px;
  object-fit: cover;
  background-color: #f8f9fa;
  transition: filter 0.2s ease-in-out;
}

.camera-badge {
  position: absolute;
  bottom: 5px;
  right: 5px;
  width: 34px;
  height: 34px;
  background-color: #ffffff;
  color: var(--tap-primary, #e57346);
  border: 1px solid #dee2e6 !important;
  font-size: 1.1rem;
  transition: background-color 0.2s;
}

.avatar-wrapper:hover .camera-badge {
  background-color: #f8f9fa;
}

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

.cursor-pointer {
  cursor: pointer;
}

/* 性別卡片樣式 */
.gender-card-label {
  background-color: #ffffff;
  transition: all 0.2s ease-in-out;
  border: 2px solid #ced4da !important;
}

.gender-card-label:hover {
  border-color: var(--tap-primary, #e57346) !important;
  background-color: rgba(229, 115, 70, 0.02);
}

.gender-card-label.active {
  border-color: var(--tap-primary, #e57346) !important;
  background-color: rgba(229, 115, 70, 0.05);
}

.gender-card-label .text-primary-icon {
  color: #6c757d;
  transition: color 0.2s;
}

.gender-card-label.active .text-primary-icon {
  color: var(--tap-primary, #e57346);
}

.gender-card-label .text-label {
  color: #495057;
  font-size: 0.85rem;
}

.gender-card-label.active .text-label {
  color: var(--tap-primary, #e57346);
  font-weight: 600;
}

.transition-all {
  transition: all 0.2s ease-in-out;
}
</style>
