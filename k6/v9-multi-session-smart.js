import http from "k6/http";
import { check, sleep } from "k6";

// ==========================================
// 🎯 場次配置設定
// ==========================================
const SESSIONS = {
    2: { min: 951, max: 10950 },
    3: { min: 10951, max: 20950 }, // 假設 Session 3 接在 Session 2 後面
};

export const options = {
    scenarios: {
        step_load: {
            executor: "ramping-arrival-rate",
            startRate: 0,
            timeUnit: "1s",
            preAllocatedVUs: 500,
            maxVUs: 4000,

            // 階梯式爬升：每階層維持 2 分鐘，觀察系統穩定度
            stages: [
                { target: 200, duration: "10s" }, // 暖身
                { target: 200, duration: "2m" },
                { target: 600, duration: "10s" }, // 中階壓力
                { target: 600, duration: "2m" },
                { target: 1000, duration: "10s" }, // 極限目標
                { target: 1000, duration: "2m" },
                { target: 0, duration: "10s" }, // 退場
            ],
        },
    },
};

export default function () {
    const url = "http://localhost:8080/api/ticket/lock";

    // 1. 隨機選擇場次 2 或 3
    const sessionId = Math.random() > 0.5 ? 2 : 3;

    // 2. 根據場次取得對應的區間
    const config = SESSIONS[sessionId];

    // 3. 在正確區間內隨機選票
    const randomTicketId = Math.floor(Math.random() * (config.max - config.min + 1)) + config.min;

    const payload = JSON.stringify({
        sessionId: sessionId,
        ticketIds: [randomTicketId],
    });

    const params = {
        headers: { "Content-Type": "application/json" },
    };

    const res = http.post(url, payload, params);

    check(res, {
        "Status 200": (r) => r.status === 200,
    });
    // 依然保留隨機睡眠，保護本地 TCP Port
    sleep(Math.random() * 0.1 + 0.05);
}
