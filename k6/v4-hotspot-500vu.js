import http from "k6/http";
import { check } from "k6";

// Burst Test（突發流量）500 人開賣瞬間同時按
export const options = {
    scenarios: {
        hotspot: {
            executor: "per-vu-iterations",
            vus: 500, //500 個 VU
            iterations: 1, //每人只送 1 次 Request全部結束
            maxDuration: "30s", //持續 30 s
        },
    },

    thresholds: {
        http_req_duration: ["p(95)<500"],

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

        // 是否真的有人成功鎖票
        "Lock Success": () => isSuccess,

        // 是否有人因票已被鎖定而失敗
        "Lock Failed (Expected)": () => isTooLate,

        // 只要符合其中一種預期結果即可
        "Expected Business Result": () => isSuccess || isTooLate,
    });

    //突發流量 拿掉 sleep()
}
