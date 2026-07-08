import http from "k6/http";
import { check, sleep } from "k6";

// 記錄腳本初始化的時間
const startTime = Date.now();

export const options = {
    stages: [
        // ♨️ 暖身期 (蓋高速公路)
        { duration: "10s", target: 50 },
        { duration: "10s", target: 50 },

        // 💥 爬升期 (擴建高速公路)
        { duration: "3s", target: 400 },

        // ⚔️ 決戰期 (400 人滿載，此時剛好經過 23 秒)
        { duration: "15s", target: 400 },

        // 📉 退場期
        { duration: "5s", target: 0 },
    ],

    thresholds: {
        http_req_duration: ["p(95)<800"],
        http_req_failed: ["rate<0.05"],
    },
};

export default function () {
    const url = "http://localhost:8080/api/ticket/lock";

    // 計算目前經過了幾秒鐘
    const elapsedSec = (Date.now() - startTime) / 1000;

    let targetTicketIds = [];

    // ==========================================
    // 🎯 核心魔法：目標切換邏輯
    // ==========================================
    // 前面 23 秒的階段，使用暖身票 (1072~1075) 作為祭品
    if (elapsedSec < 23) {
        // 從 4 張暖身票中隨機挑選 1 張來搶，分散一點壓力，主要目的是維持連線活躍
        const warmupTickets = [1072, 1073, 1074, 1075];
        const randomTarget = warmupTickets[Math.floor(Math.random() * warmupTickets.length)];
        targetTicketIds = [randomTarget];
    }
    // 時間一超過 23 秒 (此時 400 人已全數上線)，所有 VU 瞬間將砲火對準 1071！
    else {
        targetTicketIds = [1071];
    }

    const payload = JSON.stringify({
        sessionId: 3,
        ticketIds: targetTicketIds,
    });

    const params = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    const res = http.post(url, payload, params);

    let isSuccess = false;
    let isTooLate = false;

    try {
        const body = res.json();
        if (body.success === true) {
            isSuccess = true;
        } else if (body.success === false) {
            isTooLate = true;
        }
    } catch (e) {}

    check(res, {
        "HTTP Status = 200": (r) => r.status === 200,
        "Response < 800ms": (r) => r.timings.duration < 800,
        "🎉 Lock Success": () => isSuccess,
        "❌ Lock Failed": () => isTooLate,
    });

    // 依然保留微小的隨機停頓，避免單機 Port 耗盡
    sleep(Math.random() * 0.2 + 0.1);
}
