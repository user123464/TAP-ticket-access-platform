<template>
  <div class="container-xl py-4">
    <div class="w-100">
      <header class="d-flex justify-content-between align-items-center mb-3 flex-nowrap">
        <h3 class="fw-bold mb-0 text-nowrap">票務訂單</h3>
        <div class="d-flex gap-2 flex-nowrap align-items-center">
          <!-- 全域搜尋文字框 -->
          <div class="input-group search-box" style="width: 180px">
            <span class="input-group-text bg-white border-end-0 text-muted">
              <i class="bi bi-search"></i>
            </span>
            <input
              v-model="searchOrderId"
              type="text"
              class="form-control border-start-0 ps-0 text-secondary"
              placeholder="搜尋票務訂單編號"
            />
          </div>

          <!-- 主辦方篩選下拉選單 -->
          <div class="dropdown">
            <button
              class="btn btn-outline-secondary dropdown-toggle"
              data-bs-toggle="dropdown"
            >
              主辦方: {{ selectedOrgName }}
            </button>
            <ul
              class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
            >
              <li>
                <a class="dropdown-item" @click="selectedOrg = '全部'">全部</a>
              </li>
              <li v-for="org in orgList" :key="org.id">
                <a class="dropdown-item" @click="selectedOrg = org.id">{{
                  org.name
                }}</a>
              </li>
            </ul>
          </div>
          <!-- 場次篩選下拉選單 -->
          <div class="dropdown">
            <button
              class="btn btn-outline-secondary dropdown-toggle"
              data-bs-toggle="dropdown"
            >
              場次: {{ selectedSessionTitle }}
            </button>
            <ul
              class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
            >
              <li>
                <a class="dropdown-item" @click="selectedSession = '全部'"
                  >全部</a
                >
              </li>
              <li
                v-for="session in filteredSessionList"
                :key="session.session_id"
              >
                <a
                  class="dropdown-item"
                  @click="selectedSession = session.session_id"
                  >{{ session.title }}</a
                >
              </li>
            </ul>
          </div>

          <!-- 狀態篩選下拉選單 -->
          <div class="dropdown">
            <button
              class="btn btn-outline-secondary dropdown-toggle"
              data-bs-toggle="dropdown"
            >
              狀態: {{ selectedStatus }}
            </button>
            <ul
              class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
            >
              <li>
                <a class="dropdown-item" @click="selectedStatus = '全部'"
                  >全部</a
                >
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '正常'"
                  >正常</a
                >
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '待付'"
                  >待付</a
                >
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '退票'"
                  >退票</a
                >
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '取消'"
                  >取消</a
                >
              </li>
            </ul>
          </div>

          <!-- 每頁筆數下拉選單 -->
          <div class="dropdown">
            <button
              class="btn btn-outline-secondary dropdown-toggle"
              data-bs-toggle="dropdown"
            >
              每頁 {{ pageSize }} 筆
            </button>
            <ul
              class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
            >
              <li><a class="dropdown-item" @click="pageSize = 10">10筆</a></li>
              <li><a class="dropdown-item" @click="pageSize = 20">20筆</a></li>
              <li><a class="dropdown-item" @click="pageSize = 30">30筆</a></li>
            </ul>
          </div>
        </div>
      </header>

      <main>
        <div class="table-responsive bg-white rounded shadow-sm border">
          <table class="table table-hover table-bordered align-middle mb-0">
            <thead class="table-light">
              <tr class="text-center">
                <th scope="col" class="py-3">訂單編號 / 明細 ID</th>
                <th scope="col" class="py-3">主辦方</th>
                <th scope="col" class="py-3">購買者</th>
                <th scope="col" class="py-3">單筆消費金額</th>
                <th scope="col" class="py-3">訂單建立時間</th>
                <th scope="col" class="py-3">訂單狀態</th>
                <th scope="col" class="py-3">詳細資料</th>
              </tr>
            </thead>
            <tbody class="text-center">
              <tr
                v-for="data in tableData"
                :key="data.t_order_id + '-' + data.t_detail_id"
              >
                <td class="text-start ps-3">
                  <div class="d-flex flex-column gap-1">
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-secondary border font-monospace px-2 py-1"
                        >訂單</span
                      >
                      <span class="fw-bold text-dark font-monospace"
                        >#{{ data.t_order_id || "無資料" }}</span
                      >
                    </div>
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-muted border font-monospace px-2 py-1"
                        >明細</span
                      >
                      <span class="text-muted small font-monospace">{{
                        data.t_detail_id || "無資料"
                      }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ getOrgName(data.organizer_id) }}</td>
                <td>{{ data.user_id }}</td>
                <td>NT$ {{ data.unit_price || 0 }}</td>
                <td>{{ data.created_at }}</td>
                <td>
                  <span
                    class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center"
                    :class="
                      data.item_status === '正常'
                        ? 'badge-soft-success'
                        : data.item_status === '取消'
                        ? 'badge-soft-secondary'
                        : data.item_status === '待付'
                        ? 'badge-soft-warning'
                        : 'badge-soft-danger'
                    "
                  >
                    <span
                      class="status-dot me-2"
                      :class="
                        data.item_status === '正常'
                          ? 'bg-success'
                          : data.item_status === '取消'
                          ? 'bg-secondary'
                          : data.item_status === '待付'
                          ? 'bg-warning'
                          : 'bg-danger'
                      "
                    ></span>
                    {{ data.item_status }}
                  </span>
                </td>
                <td>
                  <button
                    type="button"
                    class="btn btn-sm btn-outline-primary px-3 rounded-pill"
                    data-bs-toggle="modal"
                    data-bs-target="#ticketDetail"
                    @click="selectedOrder = data"
                  >
                    詳情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <TicketDetail :order="selectedOrder" />
      </main>
    </div>
  </div>
</template>

<style scoped>
/* 下拉選單柔和動畫 */
.custom-dropdown {
  border-radius: 8px;
  padding: 0.5rem;
}
.dropdown-item {
  border-radius: 6px;
  transition: all 0.2s ease-in-out;
  cursor: pointer;
  margin-bottom: 2px;
}
.dropdown-item:hover {
  background-color: #f8f9fa;
  color: #0d6efd;
  transform: translateX(4px);
}

/* 狀態徽章視覺化 */
.badge-soft-success {
  background-color: #d1e7dd;
  color: #0f5132;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px solid rgba(15, 81, 50, 0.25);
  box-shadow: 0 2px 4px rgba(15, 81, 50, 0.08);
  font-size: 0.85rem;
}
.badge-soft-danger {
  background-color: #f8d7da;
  color: #842029;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px solid rgba(132, 32, 41, 0.25);
  box-shadow: 0 2px 4px rgba(132, 32, 41, 0.08);
  font-size: 0.85rem;
}
.badge-soft-secondary {
  background-color: #e2e3e5;
  color: #41464b;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px solid rgba(65, 70, 75, 0.25);
  box-shadow: 0 2px 4px rgba(65, 70, 75, 0.08);
  font-size: 0.85rem;
}
.badge-soft-warning {
  background-color: #fff0e6;
  color: #e65100;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px solid rgba(230, 81, 0, 0.25);
  box-shadow: 0 2px 4px rgba(230, 81, 0, 0.08);
  font-size: 0.85rem;
}
.bg-warning {
  background-color: #ff6f00 !important;
}
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  box-shadow: 0 0 4px currentColor;
}

/* 表格欄位文字縮小與防折行 */
.table th {
  font-size: 0.8rem !important;
  white-space: nowrap;
  vertical-align: middle;
  padding: 0.5rem 0.4rem !important;
}
.table td {
  font-size: 0.75rem !important;
  white-space: nowrap;
  vertical-align: middle;
  padding: 0.5rem 0.4rem !important;
}
</style>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useTicketStore } from "@/stores/ticketStore";
import TicketDetail from "@/components/TicketDetail.vue";
import sessionData from "@/data/sessionData.json";

// 1. 初始化 Pinia Store，用來管理與呼叫後端 API 的資料狀態
const ticketStore = useTicketStore();

// 2. 宣告分頁與篩選的狀態變數 (響應式 ref 變數)
const pageSize = ref(10); // 每頁顯示幾筆
const selectedStatus = ref("全部"); // 篩選狀態 (全部/正常/退票)
const selectedOrg = ref("全部"); // 篩選主辦方
const selectedSession = ref("全部"); // 篩選場次
const searchOrderId = ref(""); // 全域搜尋訂單編號

const selectedOrder = ref(null); // 點擊「詳情」時被選中的訂單資料
const sessionList = ref(sessionData); // 場次選單資料來源
const orgList = ref([]); // 主辦方選單資料來源

// 活動主題與主辦方的關聯映射 (Theme ID -> Organizer ID)
const themeOrgMap = {
  1: "ORG0000001",
  2: "ORG0000001",
  7: "ORG0000001",
  12: "ORG0000001",
  17: "ORG0000001",
  3: "ORG0000002",
  8: "ORG0000002",
  13: "ORG0000002",
  18: "ORG0000002",
  4: "ORG0000003",
  9: "ORG0000003",
  14: "ORG0000003",
  19: "ORG0000003",
  5: "ORG0000004",
  10: "ORG0000004",
  15: "ORG0000004",
  20: "ORG0000004",
  6: "ORG0000005",
  11: "ORG0000005",
  16: "ORG0000005",
};

// 取得特定場次所對應的主辦方編號 (包含靜態映射與從門票明細動態解析的後備機制)
const getSessionOrgId = (session) => {
  const themeId = session.theme_id;
  const staticOrgId = themeOrgMap[themeId];
  if (staticOrgId) return staticOrgId;

  const matchedDetail = ticketStore.tickets
    .flatMap((o) => o.orderDetailTickets || [])
    .find((d) => d.sessionId === session.session_id);
  if (matchedDetail && matchedDetail.organizerId) {
    return matchedDetail.organizerId;
  }
  return null;
};

// 根據目前選擇的主辦方過濾場次選單
const filteredSessionList = computed(() => {
  if (selectedOrg.value === "全部") return sessionList.value;
  return sessionList.value.filter((session) => {
    return getSessionOrgId(session) === selectedOrg.value;
  });
});

// 當主辦方變更時，若目前選取的場次不屬於此主辦方，重設場次篩選為「全部」
watch(selectedOrg, (newOrg) => {
  if (newOrg === "全部") return;
  if (selectedSession.value === "全部") return;
  const currentSession = sessionList.value.find(
    (s) => s.session_id === selectedSession.value,
  );
  if (currentSession && getSessionOrgId(currentSession) !== newOrg) {
    selectedSession.value = "全部";
  }
});

// 3. 當網頁元件掛載完成 (onMounted) 時，自動執行串接邏輯
onMounted(async () => {
  // 呼叫 Store 的動作去跟後端拿資料 (對應後端 /api/tickets，底層資料來自 data.sql)
  await ticketStore.fetchTickets();

  // 讀取本地快取的主辦方列表，如果沒有就使用預設的測試資料
  const orgs = localStorage.getItem("user_orgs");
  orgList.value = orgs
    ? JSON.parse(orgs)
    : [
        { id: "ORG0000001", name: "晴天音樂祭有限公司" },
        { id: "ORG0000002", name: "藍海展覽股份有限公司" },
        { id: "ORG0000005", name: "星光娛樂有限公司" },
      ];
});

// 4. 計算屬性：計算目前選擇的主辦方名稱
const selectedOrgName = computed(() => {
  if (selectedOrg.value === "全部") return "全部";
  const org = orgList.value.find((o) => o.id === selectedOrg.value);
  return org ? org.name : selectedOrg.value;
});

// 取得主辦方名稱
const getOrgName = (orgId) => {
  const org = orgList.value.find((o) => o.id === orgId);
  return org ? org.name : orgId;
};

// 5. 計算屬性：計算目前選擇的場次標題
const selectedSessionTitle = computed(() => {
  if (selectedSession.value === "全部") return "全部";
  const session = sessionList.value.find(
    (s) => s.session_id === selectedSession.value,
  );
  return session ? session.title : selectedSession.value;
});

// 6. 核心串接與資料攤平邏輯：把後端巢狀 JSON 扁平化，讓 HTML 表格 (v-for) 最好懂、最容易呈現
const tableData = computed(() => {
  const flatTickets = [];

  // 步驟 A：遍歷後端傳過來的每一筆「訂單主檔」 (來自 ticketStore.tickets)
  ticketStore.tickets.forEach((order) => {
    // 檢查這筆訂單底下有沒有「明細檔」列表
    if (order.orderDetailTickets) {
      // 步驟 B：遍歷這筆訂單底下的每一張「門票明細」
      order.orderDetailTickets.forEach((detail) => {
        // 步驟 C：把主檔與明細的欄位整理成一個「簡單的平面物件」，推入陣列中
        flatTickets.push({
          // --- 訂單主檔資訊 ---
          // 支援 camelCase 的 tOrderId 與 Jackson 預設產生的 torderId，確保不管後端如何命名皆可順利解析
          t_order_id: order.tOrderId || order.torderId,
          total_amount: order.totalAmount,
          created_at: order.createAt ? order.createAt.substring(0, 10) : "",
          user_id: detail.realName || order.contactName || "陳大明",

          // --- 門票明細資訊 ---
          t_detail_id: detail.tDetailId,
          unit_price: detail.unitPrice,
          // 狀態轉換：若後端是 REFUNDED 代表退票，CANCELLED 代表取消，UNPAID 代表待付，否則為正常
          item_status:
            detail.itemStatus === "REFUNDED"
              ? "退票"
              : detail.itemStatus === "CANCELLED"
                ? "取消"
                : detail.itemStatus === "UNPAID"
                  ? "待付"
                  : "正常",
          qr_code_hash: detail.qrCodeHash,
          is_used:
            detail.isUsed === "Redeemed"
              ? "已核銷"
              : detail.isUsed === "Canceled"
                ? "已取消"
                : "未核銷",
          used_at: detail.usedAt || null,

          // --- 關聯的座位、場次與主辦方資訊 ---
          seat_id: detail.seatInfo || "A1",
          price_category_id: detail.ticketTypeId?.toString() || "1",
          session_id: detail.sessionId || 1,
          organizer_id: detail.organizerId || "ORG0000001",
        });
      });
    }
  });

  // 步驟 D：套用下拉選單的「篩選邏輯」並進行「分頁切片」，回傳給 Vue 畫面渲染
  return flatTickets
    .filter((data) => {
      // 狀態篩選：如果是"全部"就通過，否則必須跟選擇的狀態一樣
      const matchStatus =
        selectedStatus.value === "全部" ||
        data.item_status === selectedStatus.value;

      // 主辦方篩選
      const matchOrg =
        selectedOrg.value === "全部" || data.organizer_id === selectedOrg.value;

      // 場次篩選
      const matchSession =
        selectedSession.value === "全部" ||
        data.session_id === selectedSession.value;

      // 搜尋訂單編號篩選
      const matchSearch =
        !searchOrderId.value.trim() ||
        (data.t_order_id &&
          data.t_order_id
            .toLowerCase()
            .includes(searchOrderId.value.trim().toLowerCase()));

      return matchStatus && matchOrg && matchSession && matchSearch;
    })
    .slice(0, pageSize.value);
});
</script>
