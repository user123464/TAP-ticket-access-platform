import http from "k6/http";
import { check } from "k6";

// =====================================================================
// 🌊 5 分鐘浸泡測試 (Soak Test) - MQ 削峰填谷極限驗證
// =====================================================================
export const options = {
    scenarios: {
        // 使用 constant-arrival-rate 能最精準控制「每秒發送量 (RPS)」
        mq_soak_test: {
            executor: "constant-arrival-rate",

            // 🎯 核心目標：每秒穩定送出 400 個 Request
            rate: 400,
            timeUnit: "1s",

            // ⏳ 持續時間：5 分鐘 (300 秒)
            // 總計會產生 1,000 * 300 = 300,000 次 Request
            duration: "5m",

            // 預先分配 500 個虛擬使用者在後台待命
            preAllocatedVUs: 500,
            // 如果系統回應變慢，最多允許喚醒 3000 個人來維持 1000 RPS 的發送率
            maxVUs: 3000,
        },
    },

    thresholds: {
        // 在長時間高壓下，標準可以稍微放寬，確保 95% 的請求在 2 秒內得到「排隊中」的回應
        http_req_duration: ["p(95)<2000"],
        // 容許極少量的網路波動失敗 (例如單機 Port 偶爾耗盡)
        http_req_failed: ["rate<0.05"],
    },
};

export default function () {
    const url = "http://localhost:8080/api/ticket/lock";

    // ==========================================
    // 🎫 隨機產生目標票券與使用者 ID
    // ==========================================
    // 假設你的 10,000 張票 ID 是從 1 到 10000
    // (⚠️ 請依照你資料庫實際產生的 ID 區間進行修改，例如 1001 ~ 11000)
    const minTicketId = 951;
    const maxTicketId = 10950;
    const randomTicketId = Math.floor(Math.random() * (maxTicketId - minTicketId + 1)) + minTicketId;

    // 模擬海量不同的使用者 (使用 1 到 50 萬的隨機數，確保每個人身份盡量不同)
    //不送 userId API 已經開放

    const payload = JSON.stringify({
        sessionId: 2,
        ticketIds: [randomTicketId], // 每人隨機搶 1 張票
    });

    const params = {
        headers: { "Content-Type": "application/json" },
    };

    // 發送搶票請求
    const res = http.post(url, payload, params);

    // 斷言：只要伺服器沒有崩潰 (回傳 500)，有正常回傳 JSON 就算成功扛住壓力
    check(res, {
        "API 存活 (Status 200)": (r) => r.status === 200,
        "Response < 1000ms": (r) => r.timings.duration < 1000,
    });
}
