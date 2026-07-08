import http from "k6/http";
import { check, sleep } from "k6";

// ==========================================
// 1. 測試策略設定 (Options)
// ==========================================
export const options = {
    // 這裡我們模擬一個「突發流量 (Spike)」的場景
    stages: [
        { duration: "5s", target: 50 }, // 前 5 秒，快速湧入 50 個虛擬使用者 (VUs)
        { duration: "10s", target: 50 }, // 維持 50 個人同時不斷點擊，持續 10 秒
        { duration: "5s", target: 0 }, // 最後 5 秒，人潮散去
    ],
};

// ==========================================
// 2. 虛擬使用者 (VU) 的執行邏輯
// ==========================================
export default function () {
    // 目標 API 網址 (請確認 port 號與實際相符)
    const url = "http://localhost:8080/api/ticket/lock";

    // 隨機決定這位使用者要買幾張票 (1 到 4 張)
    const numTickets = Math.floor(Math.random() * 4) + 1;
    const ticketIds = [];

    // 根據目標票數，隨機從 1061~1170 中挑選不重複的座位
    for (let i = 0; i < numTickets; i++) {
        let isDuplicate = true;
        let randomTicketId;

        while (isDuplicate) {
            // 產生 1061 到 1170 之間的整數 (總共 110 個號碼)
            randomTicketId = Math.floor(Math.random() * 110) + 1351;
            if (!ticketIds.includes(randomTicketId)) {
                isDuplicate = false;
            }
        }
        ticketIds.push(randomTicketId);
    }
    // 準備 JSON 籌碼
    const payload = JSON.stringify({
        sessionId: 3,
        ticketIds: ticketIds,
    });
    // 設定 Request Header
    const params = {
        headers: {
            "Content-Type": "application/json",
        },
    };
    // 發射 POST 請求！
    const res = http.post(url, payload, params);

    // ==========================================
    // 3. 斷言與檢查 (Checks)
    // ==========================================
    check(res, {
        // 確認 API 是否正常回傳 HTTP 200 (就算晚了一步，你的系統也是回 200 OK 加上自訂 Error Message，這很棒)
        "status is 200": (r) => r.status === 200,
        // 確認這趟請求是不是在 500 毫秒內極速處理完畢
        "response time < 500ms": (r) => r.timings.duration < 500,
    });

    // 模擬人類的停頓時間 (Think time)，每次點擊後休息 0.5 到 1.5 秒
    sleep(Math.random() + 0.5);
}
