import { defineStore } from "pinia";
import { ref, computed } from "vue";

export const useTicketCartStore = defineStore(
    "ticketCart",
    () => {
        // ==========================================
        // 1. 狀態 (State)
        // ==========================================
        const tickets = ref([]); // 存放已鎖定的票券詳細資訊
        const currentSessionId = ref(null); // 紀錄這些票券屬於哪個場次
        const lockTimestamp = ref(null); // 紀錄鎖定當下的時間戳記 (前端輔助驗證用)

        // ==========================================
        // 2. 動作 (Actions)
        // ==========================================
        /**
         * 將選好的票券存入購物車
         * @param {Array} selectedSeats - 剛剛在畫面選中的座位陣列
         * @param {Number|String} sessionId - 場次 ID
         */

        function saveTickets(selectedSeats, sessionId) {
            tickets.value = selectedSeats;
            currentSessionId.value = sessionId;
            lockTimestamp.value = Date.now(); // 壓上當前時間
            console.log("selectedSeats =", selectedSeats);
        }

        /**
         * 清空購物車
         * 進入選位頁面時，或超過 15 分鐘結帳失敗時呼叫
         */
        function clearCart() {
            tickets.value = [];
            currentSessionId.value = null;
            lockTimestamp.value = null;
        }

        // ==========================================
        // 3. 衍生狀態 (Getters / Computed)
        // ==========================================

        // 讓結帳頁面的隊友可以直接呼叫這個屬性，拿到純粹的 seat_id 陣列發給後端
        const ticketIds = computed(() => {
            return tickets.value.map((seat) => seat.ticket_id);
        });

        // 計算總金額，方便結帳頁面直接顯示，不用重寫一次邏輯
        const cartTotalPrice = computed(() => {
            return tickets.value.reduce((sum, seat) => sum + seat.price, 0);
        });

        // 檢查票券是否可能已經在前端過期 (15分鐘 = 900,000 毫秒)
        // 結帳頁面載入時可以先拿這個判斷，提早擋下無效結帳
        // const isClientSideExpired = computed(() => {
        //     if (!lockTimestamp.value) return true;
        //     const now = Date.now();
        //     const diffInMinutes = (now - lockTimestamp.value) / 1000 / 60;
        //     return diffInMinutes >= 15;
        // });

        return {
            tickets,
            currentSessionId,
            lockTimestamp,
            saveTickets,
            clearCart,
            ticketIds,
            cartTotalPrice,
            // isClientSideExpired,
        };
    },
    {
        // ==========================================
        // 🛡️ 核心防護：持久化設定 (Persisted State Configuration)
        // ==========================================
        persist: {
            // 業界無痛隔離大法：強制使用 sessionStorage，確保多個分頁不會互相覆蓋資料！
            storage: window.sessionStorage,

            // （可選）你可以指定只把哪些狀態存進去。這裡我們全存。
            paths: ["tickets", "currentSessionId", "lockTimestamp"],
        },
    },
);
