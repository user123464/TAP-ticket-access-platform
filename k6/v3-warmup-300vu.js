import http from "k6/http";
import { check, sleep } from "k6";

// ======================================================
// 100 VU 暖身測試
//
// 目的：
// 1. 驗證 API 是否正常
// 2. 驗證 Redis / RabbitMQ / SQL Server 是否正常
// 3. 驗證 Grafana Dashboard 是否正常收集資料
// 4. 不追求極限，只確認系統沒有明顯問題
// ======================================================

export const options = {
    // --------------------------------------------------
    // 使用 stages 讓流量慢慢增加
    // 避免一開始就瞬間打爆系統
    // --------------------------------------------------
    stages: [
        { duration: "5s", target: 300 }, // 5 秒升到 300 VU
        { duration: "30s", target: 300 }, // 維持 30 秒，方便觀察 Grafana
        { duration: "5s", target: 0 }, // 降回 0
    ],

    // --------------------------------------------------
    // 壓測門檻
    // 如果沒達標，K6 會直接標示 FAILED
    // --------------------------------------------------
    thresholds: {
        // 95% Request 必須小於 500ms
        http_req_duration: ["p(95)<500"],

        // HTTP 失敗率小於 1%
        http_req_failed: ["rate<0.01"],
    },
};

export default function () {
    const url = "http://localhost:8080/api/ticket/lock";

    // --------------------------------------------------
    // 故意讓所有人搶同一批票
    // 驗證 Redis 鎖是否真的有效
    // --------------------------------------------------
    const payload = JSON.stringify({
        sessionId: 3,

        ticketIds: [1071, 1072, 1073, 1074],
    });

    const params = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    // --------------------------------------------------
    // 發送 POST Request
    // --------------------------------------------------
    const res = http.post(url, payload, params);

    // --------------------------------------------------
    // 判斷後端回傳內容
    // --------------------------------------------------
    let isSuccess = false;
    let isTooLate = false;

    try {
        const body = res.json();

        if (body.success === true) {
            isSuccess = true;
        } else if (body.success === false) {
            isTooLate = true;
        }
    } catch (e) {
        // 若不是 JSON，交由下面 check 判斷
    }

    // --------------------------------------------------
    // 驗證 API 行為
    // --------------------------------------------------
    check(res, {
        // API 是否正常存活
        "HTTP Status = 200": (r) => r.status === 200,

        // Response Time 是否小於 500ms
        "Response < 500ms": (r) => r.timings.duration < 500,

        // 只要符合其中一種預期結果即可
        "Expected Business Result": () => isSuccess || isTooLate,
    });

    // --------------------------------------------------
    // 暖身測試保留少量思考時間
    //
    // 模擬使用者：
    // 點擊
    // ↓
    // 等待畫面回應
    //
    // 正式 Hotspot 測試會移除 sleep()
    // --------------------------------------------------
    sleep(0.5); // v3 加壓 縮短至 0.5 s
}
