import { useRouter, useRoute } from 'vue-router';

// 智慧返回：有 SPA 站內歷史就回上一頁；若為直接開啟連結/新分頁(無歷史)則退回語境首頁。
// 取代各頁面寫死的 backPath，並統一原本 ForbiddenView 手刻的來源偵測邏輯。
export function useSmartBack() {
  const router = useRouter();
  const route = useRoute();

  const goBack = () => {
    // window.history.state.back 有值代表存在站內上一頁
    if (window.history.state && window.history.state.back) {
      router.back();
    } else {
      // 無歷史的 fallback：B2B → 組織選擇；B2C → 首頁
      router.push(route.path.startsWith('/org') ? '/org/select' : '/');
    }
  };

  return { goBack };
}
