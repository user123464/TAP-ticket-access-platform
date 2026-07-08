import http from "k6/http";
import { check, sleep } from "k6";

// =====================================================================
// 融合「熱機暖身」與「極端碰撞」的單機最佳化壓測劇本
// =====================================================================
export const options = {
    // 捨棄瞬間併發的 per-vu-iterations，改用階段式的 stages
    stages: [
        // --------------------------------------------------
        // ♨️ 階段一：暖身期 (Warm-up)
        // 目的：喚醒 Tomcat 執行緒、建立 HikariCP 資料庫連線、觸發 JVM 的 JIT 編譯
        // --------------------------------------------------
        { duration: "10s", target: 50 }, // 前 10 秒：溫和地將流量拉升到 50 人
        { duration: "10s", target: 50 }, // 接著 10 秒：維持 50 人。讓系統在低負載下完成所有底層資源的初始化 (熱機完畢)

        // --------------------------------------------------
        // 💥 階段二：極限碰撞期 (Spike / Collision)
        // 目的：考驗 Redis 單點防超賣能力，以及 Tomcat 解封印後的高併發吞吐量
        // --------------------------------------------------
        // 🌟 關鍵魔法：不給 0 秒，而是給 2~3 秒的極速爬升期！
        // 這能將幾百個 TCP 握手 (SYN) 分散在兩千毫秒內，完美騙過 Windows 網卡防禦機制
        { duration: "3s", target: 400 },
        { duration: "15s", target: 400 }, // 維持 400 人瘋狂搶同一張票，製造嚴重的資源爭奪 (Hotspot)

        // --------------------------------------------------
        // 📉 階段三：冷卻退場期
        // 目的：觀察系統資源是否能平穩釋放，不引發 Memory Leak
        // --------------------------------------------------
        { duration: "5s", target: 0 },
    ],

    thresholds: {
        // 放寬一點點標準，畢竟單機會有 CPU 上下文切換 (Context Switch) 的物理極限
        http_req_duration: ["p(95)<800"],
        http_req_failed: ["rate<0.05"], // 容許極少量的失敗 (因單機 port 耗盡)
    },
};

export default function () {
    const url = "http://localhost:8080/api/ticket/lock";

    // 故意讓所有人搶同一批票 (驗證 Redis 單點鎖)
    const payload = JSON.stringify({
        sessionId: 3,
        ticketIds: [1071, 1072, 1073, 1074],
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
    } catch (e) {
        // 若不是 JSON，交由下面 check 判斷
    }

    check(res, {
        "HTTP Status = 200": (r) => r.status === 200,
        "Response < 800ms": (r) => r.timings.duration < 800,
        "🎉 唯一幸運兒 (Lock Success)": () => isSuccess,
        "❌ 晚了一步 (Lock Failed)": () => isTooLate,
        "✅ 業務邏輯正確": () => isSuccess || isTooLate,
    });

    // --------------------------------------------------
    // 🌟 核心防禦：絕對不能拿掉 sleep！
    // --------------------------------------------------
    // 在單機環境中，如果 400 個人以 0 毫秒的間隔「無限迴圈」打 API
    // Windows 會在 5 秒內把 65535 個可用 Port 全部耗盡 (TIME_WAIT 狀態)
    // 加上 100~300 毫秒的隨機停頓，既能保證瞬間碰撞的猛烈度，又能給 OS 喘息空間回收 Port
    sleep(Math.random() * 0.2 + 0.1);
}
