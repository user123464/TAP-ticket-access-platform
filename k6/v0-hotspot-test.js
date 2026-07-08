import http from "k6/http";
import { check, sleep } from "k6";

// ==========================================
// 1. 測試策略設定 (極端突發流量)
// ==========================================
export const options = {
    stages: [
        { duration: "2s", target: 500 }, // 瞬間湧入！2 秒內立刻拉高到 500 人
        { duration: "8s", target: 500 }, // 500 人同時點擊，持續 8 秒
        { duration: "2s", target: 0 }, // 快速退場
    ],
};

// ==========================================
// 2. 虛擬使用者 (VU) 的執行邏輯
// ==========================================
export default function () {
    const url = "http://localhost:8080/api/ticket/lock";

    // 🌟 核心關鍵：刻意「寫死」目標座位！
    // 讓 500 個人全部撞在同一組座位上，製造嚴重的資源爭奪 (Hotspot)
    const payload = JSON.stringify({
        sessionId: 3,
        ticketIds: [1061, 1062, 1063, 1064], // 鎖定這 4 張票
    });

    const params = {
        headers: {
            "Content-Type": "application/json",
        },
    };

    // 發射 POST 請求
    const res = http.post(url, payload, params);

    // ==========================================
    // 3. 斷言與檢查 (動態捕捉成功與失敗比例)
    // ==========================================
    let isSuccess = false;
    let isTooLate = false;

    // 嘗試解析後端回傳的 JSON
    try {
        const body = res.json();
        if (body.success === true) {
            isSuccess = true;
        } else if (body.success === false) {
            isTooLate = true;
        }
    } catch (e) {
        // 忽略解析錯誤，交給下方的 check 統一處理
    }

    check(res, {
        "API 存活確認 (Status 200)": (r) => r.status === 200,
        "處理極速 (< 500ms)": (r) => r.timings.duration < 500,
        // 🌟 追蹤這兩種情境的發生次數
        "🎉 搶票成功 (僅限 1 人)": () => isSuccess,
        "❌ 晚了一步 (預期 499 人)": () => isTooLate,
    });

    // 為了製造最大的瞬間碰撞，這裡的 sleep 調得非常短
    sleep(Math.random() * 0.3 + 0.1);
}
