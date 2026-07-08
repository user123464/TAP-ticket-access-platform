import { defineStore } from 'pinia';
import axios from '@/plugins/axios.js';

export const useAuthStore = defineStore('auth', {
  state: () => ({
    isLoggedIn: localStorage.getItem('is_logged_in') === 'true',
    accessToken: localStorage.getItem('access_token') || '',
    userEmail: localStorage.getItem('user_email') || '',
    userId: localStorage.getItem('user_id') || '',
    userName: localStorage.getItem('user_name') || '使用者',
    userAvatar: localStorage.getItem('user_avatar') || 'boring:default'
  }),

  actions: {
    /**
     * 處理登入成功狀態變更與寫入快取
     */
    login(data) {
      this.isLoggedIn = true;
      this.accessToken = data.accessToken;
      this.userEmail = data.email;
      this.userId = data.userId;
      this.userName = data.name;
      this.userAvatar = data.avatarUrl || `boring:${data.name}`;

      localStorage.setItem('is_logged_in', 'true');
      localStorage.setItem('access_token', data.accessToken);
      localStorage.setItem('user_email', data.email);
      localStorage.setItem('user_id', data.userId);
      localStorage.setItem('user_name', data.name);
      localStorage.setItem('user_avatar', this.userAvatar);

      // 派發自訂事件，相容部分未重構元件的 navbar 狀態同步
      window.dispatchEvent(new Event('profile-updated'));
    },

    /**
     * 處理登出：整合後端登出 API 並清空本地快取 (預留將來轉移至 Redis 黑名單機制)
     */
    async logout(redirectTo = '/') {
      try {
        if (this.accessToken) {
          logInfoToConsole('開始調用後端登出接口...');
          await axios.post('/api/auth/logout');
        }
      } catch (err) {
        console.error('後端登出 API 調用失敗，執行本地強制登出清除：', err);
      } finally {
        // 1. 清空 Store 中的響應式狀態
        this.isLoggedIn = false;
        this.accessToken = '';
        this.userEmail = '';
        this.userId = '';
        this.userName = '使用者';
        this.userAvatar = 'boring:default';

        // 2. 清空 Local Storage 中的狀態資料
        localStorage.removeItem('is_logged_in');
        localStorage.removeItem('access_token');
        localStorage.removeItem('user_email');
        localStorage.removeItem('user_id');
        localStorage.removeItem('user_name');
        localStorage.removeItem('user_avatar');
        localStorage.removeItem('user_profile');
        // 清除組織工作區持久化資料，避免換帳號登入時殘留前一位使用者的組織清單/名稱/頭像
        localStorage.removeItem('workspace');
        localStorage.removeItem('user_orgs');

        // 清除所有 useCachedResource 的資源快取（cache: 前綴），避免換帳號殘留前一位使用者資料
        Object.keys(localStorage)
          .filter(k => k.startsWith('cache:'))
          .forEach(k => localStorage.removeItem(k));

        // 3. 通知其他 Layout 與導覽列
        window.dispatchEvent(new Event('profile-updated'));

        // 4. 強制將頁面重導向至指定路徑，清理殘留頁面狀態
        window.location.href = redirectTo;
      }
    },

    /**
     * 更新個人基本資料（名字、頭像）
     */
    updateProfile(profileData) {
      if (profileData.name) {
        this.userName = profileData.name;
        localStorage.setItem('user_name', profileData.name);
      }
      if (profileData.avatarUrl !== undefined) {
        this.userAvatar = profileData.avatarUrl || `boring:${this.userName}`;
        localStorage.setItem('user_avatar', this.userAvatar);
      } else if (this.userAvatar && this.userAvatar.startsWith('boring:')) {
        this.userAvatar = `boring:${this.userName}`;
        localStorage.setItem('user_avatar', this.userAvatar);
      }
      // 觸發 UI 更新事件
      window.dispatchEvent(new Event('profile-updated'));
    }
  }
});

// 輔助調試方法
function logInfoToConsole(msg) {
  console.log(`[AuthStore] ${msg}`);
}
