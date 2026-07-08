import http from "k6/http";
import { check } from "k6";

export const options = {
    // 這次我們模擬更巨大的流量
    scenarios: {
        spike: {
            executor: "constant-arrival-rate", // 固定每秒進入的人數，比 VU 更能模擬真實場景
            rate: 200, // 每秒湧入 200 人
            timeUnit: "1s",
            duration: "30s", // 持續轟炸 30 秒
            preAllocatedVUs: 200,
            maxVUs: 1000,
        },
    },

    thresholds: {
        http_req_duration: ["p(95)<1000"], // 放寬到 1 秒，因為這是在壓力下測試 MQ 吞吐
    },
};

export default function () {
    const url = "http://localhost:8080/api/ticket/lock";

    // 隨機選票：每人買 1~2 張，從 1061~1170 中隨機抽
    const randomTicketId = Math.floor(Math.random() * 110) + 1061;

    const payload = JSON.stringify({
        sessionId: 3,
        ticketIds: [randomTicketId], // 這裡搶 1 張票
        userId: "USR-" + Math.floor(Math.random() * 10000), // 模擬不同使用者
    });

    const params = {
        headers: { "Content-Type": "application/json" },
    };

    const res = http.post(url, payload, params);

    // 驗證 API 是否給予「排隊中」或「成功/失敗」的回應
    check(res, {
        "status 200": (r) => r.status === 200,
    });
}
