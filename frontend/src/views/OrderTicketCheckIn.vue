<template>
  <div class="container-xl py-4">
    <div class="w-100">
      <header class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h3 class="fw-bold mb-1">票務售後</h3>
          <p class="text-muted mb-0 small">
            快速辦理售後退票與退款作業
          </p>
        </div>
        <div
          class="bg-white px-4 py-2 rounded-pill shadow-sm border d-flex align-items-center gap-2"
        >
          <span class="text-muted small">當前統計營收:</span>
          <span class="fw-bold text-success"
            >NT$ {{ ticketStore.totalRevenue }}</span
          >
        </div>
      </header>

      <!-- 快速退票辦理面板 -->
      <section class="card border-0 shadow-sm rounded-4 mb-4 p-4">
        <h5 class="fw-bold mb-3 text-danger">
          <i class="bi bi-arrow-left-right me-2"></i>快速退票辦理
        </h5>
        <form
          @submit.prevent="handleQuickRefund"
          class="row g-3 align-items-center"
        >
          <div class="col-md-8">
            <div class="input-group">
              <span class="input-group-text bg-light border-end-0"
                ><i class="bi bi-hash"></i
              ></span>
              <input
                v-model="refundInput"
                type="text"
                class="form-control bg-light border-start-0"
                placeholder="請輸入票券明細 ID 或 QR Code HASH 代碼..."
                required
              />
            </div>
          </div>
          <div class="col-md-4">
            <button
              type="submit"
              class="btn btn-danger w-100 rounded-3 fw-bold py-2 shadow-sm"
            >
              <i class="bi bi-arrow-left-right me-1"></i>辦理退票
            </button>
          </div>
        </form>
      </section>

      <!-- 狀態卡片 -->
      <div class="row g-3 mb-4">
        <div class="col-md-4" v-for="stat in stats" :key="stat.title">
          <div
            class="card border-0 shadow-sm rounded-4 p-3 d-flex flex-row align-items-center gap-3"
          >
            <div
              class="rounded-circle bg-opacity-10 flex-shrink-0 d-flex align-items-center justify-content-center"
              :class="stat.bgClass"
              style="width: 56px; height: 56px"
            >
              <i class="bi fs-4" :class="stat.icon"></i>
            </div>
            <div>
              <span class="text-muted small d-block">{{ stat.title }}</span>
              <strong class="fs-4">{{ stat.value }}</strong>
            </div>
          </div>
        </div>
      </div>

      <!-- 票券管理表格 -->
      <main class="card border-0 shadow-sm rounded-4 p-3">
        <div
          class="d-flex flex-column flex-md-row justify-content-between align-items-stretch align-items-md-center gap-3 mb-3"
        >
          <h5 class="fw-bold mb-0 text-nowrap">所有票券明細</h5>
          <div class="d-flex gap-2 flex-wrap align-items-center">
            <!-- 全域搜尋文字框 -->
            <div
              class="input-group input-group-sm search-box"
              style="width: 180px"
            >
              <span class="input-group-text bg-white border-end-0 text-muted">
                <i class="bi bi-search"></i>
              </span>
              <input
                v-model="searchDetailId"
                type="text"
                class="form-control border-start-0 ps-0 text-secondary"
                placeholder="搜尋票務明細"
              />
            </div>

            <!-- 主辦方篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-outline-secondary dropdown-toggle btn-sm"
                data-bs-toggle="dropdown"
              >
                主辦方: {{ selectedOrgName }}
              </button>
              <ul
                class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
              >
                <li>
                  <a class="dropdown-item" @click="selectedOrg = '全部'"
                    >全部</a
                  >
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
                class="btn btn-outline-secondary dropdown-toggle btn-sm"
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
                class="btn btn-outline-secondary dropdown-toggle btn-sm"
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
                  <a class="dropdown-item" @click="selectedStatus = '退票'"
                    >退票</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedStatus = '取消'"
                    >取消</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedStatus = '待付'"
                    >待付</a
                  >
                </li>
              </ul>
            </div>
            <!-- 核銷篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-outline-secondary dropdown-toggle btn-sm"
                data-bs-toggle="dropdown"
              >
                核銷: {{ selectedUsed }}
              </button>
              <ul
                class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
              >
                <li>
                  <a class="dropdown-item" @click="selectedUsed = '全部'"
                    >全部</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedUsed = '已核銷'"
                    >已核銷</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedUsed = '未核銷'"
                    >未核銷</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedUsed = '已取消'"
                    >已取消</a
                  >
                </li>
                <li>
                  <a class="dropdown-item" @click="selectedUsed = '未成立'"
                    >未成立</a
                  >
                </li>
              </ul>
            </div>
            <!-- 每頁筆數下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-outline-secondary dropdown-toggle btn-sm"
                data-bs-toggle="dropdown"
              >
                每頁 {{ pageSize }} 筆
              </button>
              <ul
                class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
              >
                <li>
                  <a class="dropdown-item" @click="pageSize = 10">10筆</a>
                </li>
                <li>
                  <a class="dropdown-item" @click="pageSize = 20">20筆</a>
                </li>
                <li>
                  <a class="dropdown-item" @click="pageSize = 30">30筆</a>
                </li>
              </ul>
            </div>
          </div>
        </div>

        <div class="table-responsive bg-white rounded shadow-sm border">
          <table class="table table-hover table-bordered align-middle mb-0">
            <thead class="table-light">
              <tr class="text-center">
                <th scope="col" class="py-3">訂單編號 / 明細ID</th>
                <th scope="col" class="py-3">主辦方</th>
                <th scope="col" class="py-3">購買者</th>
                <th scope="col" class="py-3">票價</th>
                <th scope="col" class="py-3">座位</th>
                <th scope="col" class="py-3">核銷狀態</th>
                <th scope="col" class="py-3">訂單狀態</th>
                <th scope="col" class="py-3">操作</th>
              </tr>
            </thead>
            <tbody class="text-center">
              <tr
                v-for="t in filteredTickets"
                :key="t.t_order_id + '-' + t.t_detail_id"
              >
                <td class="text-start ps-3">
                  <div class="d-flex flex-column gap-1">
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-secondary border font-monospace px-2 py-1"
                        >訂單</span
                      >
                      <span class="fw-bold text-dark font-monospace"
                        >#{{ t.t_order_id || "無資料" }}</span
                      >
                    </div>
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-muted border font-monospace px-2 py-1"
                        >明細</span
                      >
                      <span class="text-muted small font-monospace">{{
                        t.t_detail_id || "無資料"
                      }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ getOrgName(t.organizer_id) }}</td>
                <td>{{ t.user_id }}</td>
                <td>NT$ {{ t.unit_price }}</td>
                <td>
                  <span class="badge bg-light text-dark px-2.5 py-1.5">{{
                    t.seat_id
                  }}</span>
                </td>
                <td>
                  <span
                    class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center"
                    :class="
                      t.is_used === '已核銷'
                        ? 'badge-use-redeemed'
                        : t.is_used === '未核銷'
                          ? 'badge-use-unredeemed'
                          : t.is_used === '已取消'
                            ? 'badge-use-canceled'
                            : 'badge-use-unestablished'
                    "
                  >
                    <span
                      class="status-dot me-2"
                      :class="
                        t.is_used === '已核銷'
                          ? 'bg-use-redeemed-dot'
                          : t.is_used === '未核銷'
                            ? 'bg-use-unredeemed-dot'
                            : t.is_used === '已取消'
                              ? 'bg-use-canceled-dot'
                              : 'bg-use-unestablished-dot'
                      "
                    ></span>
                    {{ t.is_used }}
                  </span>
                </td>
                <td>
                  <span
                    class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center"
                    :class="
                      t.item_status === '正常'
                        ? 'badge-soft-success'
                        : t.item_status === '退票'
                          ? 'badge-soft-danger'
                          : t.item_status === '取消'
                            ? 'badge-soft-secondary'
                            : 'badge-soft-warning'
                    "
                  >
                    <span
                      class="status-dot me-2"
                      :class="
                        t.item_status === '正常'
                          ? 'bg-success'
                          : t.item_status === '退票'
                            ? 'bg-danger'
                            : t.item_status === '取消'
                              ? 'bg-secondary'
                              : 'bg-warning'
                      "
                    ></span>
                    {{ t.item_status }}
                  </span>
                </td>
                <td>
                  <button
                    v-if="t.item_status === '正常'"
                    class="btn btn-sm btn-outline-danger px-3 rounded-pill fw-bold"
                    :disabled="t.is_used === '已核銷'"
                    @click="handleRefund(t)"
                  >
                    退票
                  </button>
                  <span v-else-if="t.item_status === '退票'" class="text-muted small">已完成退票</span>
                  <span v-else-if="t.item_status === '取消'" class="text-muted small">已取消</span>
                  <span v-else-if="t.item_status === '待付'" class="text-muted small">未付款</span>
                  <span v-else class="text-muted small">--</span>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </div>
  </div>
</template>

<style scoped>
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

/* 核銷狀態專用徽章視覺化 (採用獨立配色體系，加強與訂單狀態的區隔及易讀性) */
.badge-use-redeemed {
  background-color: #e6fffa;
  color: #0f766e;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px solid rgba(13, 148, 136, 0.25);
  box-shadow: 0 2px 4px rgba(13, 148, 136, 0.08);
  font-size: 0.85rem;
}
.bg-use-redeemed-dot {
  background-color: #0d9488 !important;
}

.badge-use-unredeemed {
  background-color: #f3e8ff;
  color: #6b21a8;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px solid rgba(147, 51, 234, 0.25);
  box-shadow: 0 2px 4px rgba(147, 51, 234, 0.08);
  font-size: 0.85rem;
}
.bg-use-unredeemed-dot {
  background-color: #9333ea !important;
}

.badge-use-canceled {
  background-color: #f5f5f4;
  color: #57534e;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px solid rgba(120, 113, 110, 0.25);
  box-shadow: 0 2px 4px rgba(120, 113, 110, 0.08);
  font-size: 0.85rem;
}
.bg-use-canceled-dot {
  background-color: #78716c !important;
}

.badge-use-unestablished {
  background-color: #fafaf9;
  color: #78716c;
  font-weight: 600;
  letter-spacing: 1px;
  border: 1px dashed rgba(168, 162, 158, 0.6);
  box-shadow: none;
  font-size: 0.85rem;
}
.bg-use-unestablished-dot {
  background-color: #a8a29e !important;
}
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  box-shadow: 0 0 4px currentColor;
}

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

/* 票券已核銷時將退票按鈕自定義為暗灰色 */
.btn-outline-danger:disabled {
  color: #555555 !important;
  border-color: #777777 !important;
  background-color: #e9ecef !important;
  opacity: 0.7;
  cursor: not-allowed;
}
</style>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useTicketStore } from "@/stores/ticketStore";
import { useToast } from "@/composables/useToast.js";
import { useConfirm } from "@/composables/useConfirm.js";
import sessionData from "@/data/sessionData.json";

const ticketStore = useTicketStore();
const toast = useToast();
const { confirm } = useConfirm();
const refundInput = ref("");
const selectedStatus = ref("全部");
const selectedOrg = ref("全部");
const selectedSession = ref("全部");
const selectedUsed = ref("全部");
const pageSize = ref(10);
const searchDetailId = ref(""); // 全域搜尋明細 ID

const sessionList = ref(sessionData);
const orgList = ref([]);

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
  21: "ORG0000005",
  22: "ORG0000005",
};

// 取得特定場次所對應的主辦方編號
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

onMounted(() => {
  ticketStore.fetchTickets();
  ticketStore.fetchRevenue();
  const orgs = localStorage.getItem("user_orgs");
  orgList.value = orgs
    ? JSON.parse(orgs)
    : [
        { id: "ORG0000001", name: "晴天音樂祭有限公司" },
        { id: "ORG0000002", name: "藍海展覽股份有限公司" },
        { id: "ORG0000005", name: "星光娛樂有限公司" },
      ];
});

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

// 計算屬性：計算目前選擇的場次標題
const selectedSessionTitle = computed(() => {
  if (selectedSession.value === "全部") return "全部";
  const session = sessionList.value.find(
    (s) => s.session_id === selectedSession.value,
  );
  return session ? session.title : selectedSession.value;
});

// 核心資料攤平邏輯：將巢狀的訂單與明細轉為扁平陣列，讓前端能顯示各別門票資訊
const flatTickets = computed(() => {
  const result = [];
  ticketStore.tickets.forEach((order) => {
    if (order.orderDetailTickets) {
      order.orderDetailTickets.forEach((detail) => {
        result.push({
          t_order_id: order.tOrderId || order.torderId,
          t_detail_id: detail.tDetailId,
          user_id: detail.realName || order.contactName || "陳大明",
          unit_price: detail.unitPrice,
          seat_id: detail.seatInfo || "A1",
          is_used:
            detail.itemStatus === "CANCELLED" || detail.isUsed === "UNESTABLISHED"
              ? "未成立"
              : detail.isUsed === "Redeemed"
                ? "已核銷"
                : detail.isUsed === "Canceled"
                  ? "已取消"
                  : "未核銷",
          item_status:
            detail.itemStatus === "REFUNDED"
              ? "退票"
              : detail.itemStatus === "CANCELLED"
                ? "取消"
                : detail.itemStatus === "UNPAID"
                  ? "待付"
                  : "正常",
          organizer_id: detail.organizerId || "ORG0000001",
          session_id: detail.sessionId || 1,
          qr_code_hash: detail.qrCodeHash || "",
        });
      });
    }
  });
  return result;
});

// 計算狀態數據卡片 (基於攤平後的資料)
const stats = computed(() => {
  const list = flatTickets.value;
  const total = list.length;
  const refundable = list.filter((t) => t.item_status === "正常" && t.is_used !== "已核銷").length;
  const refunded = list.filter((t) => t.item_status === "退票").length;

  return [
    {
      title: "總票券數量",
      value: `${total} 張`,
      icon: "bi-ticket-detailed",
      bgClass: "bg-primary text-primary",
    },
    {
      title: "可退票數量",
      value: `${refundable} 張`,
      icon: "bi-check-circle",
      bgClass: "bg-success text-success",
    },
    {
      title: "退票辦理數",
      value: `${refunded} 張`,
      icon: "bi-arrow-left-right",
      bgClass: "bg-danger text-danger",
    },
  ];
});

// 篩選與分頁票券列表 (基於攤平後的資料)
const filteredTickets = computed(() => {
  return flatTickets.value
    .filter((t) => {
      const matchStatus =
        selectedStatus.value === "全部" ||
        t.item_status === selectedStatus.value;
      const matchUsed =
        selectedUsed.value === "全部" || t.is_used === selectedUsed.value;
      const matchOrg =
        selectedOrg.value === "全部" || t.organizer_id === selectedOrg.value;
      const matchSession =
        selectedSession.value === "全部" ||
        t.session_id === selectedSession.value;

      // 全域搜尋明細 ID 篩選
      const matchSearch =
        !searchDetailId.value.trim() ||
        (t.t_detail_id &&
          t.t_detail_id
            .toLowerCase()
            .includes(searchDetailId.value.trim().toLowerCase()));

      return (
        matchStatus && matchUsed && matchOrg && matchSession && matchSearch
      );
    })
    .slice(0, pageSize.value);
});

// 處理快速退票搜尋與辦理
const handleQuickRefund = async () => {
  const code = refundInput.value.trim();
  if (!code) return;

  const ticket = flatTickets.value.find(
    (t) =>
      (t.t_detail_id && t.t_detail_id.toLowerCase() === code.toLowerCase()) ||
      (t.qr_code_hash && t.qr_code_hash === code)
  );

  if (!ticket) {
    toast.error("找不到符合該明細 ID 或 HASH 的票券，請重新輸入。");
    return;
  }

  if (ticket.item_status === "退票") {
    toast.info("該票券先前已完成退票程序。");
    return;
  }

  if (ticket.item_status === "取消") {
    toast.info("該票券所屬訂單已被取消，無法進行退票辦理。");
    return;
  }

  if (ticket.item_status === "待付") {
    toast.info("該票券尚未付款，無法進行退票辦理。");
    return;
  }

  if (ticket.is_used === "已核銷") {
    toast.error("該票券已核銷使用，無法進行退票辦理。");
    return;
  }

  await handleRefund(ticket);
  refundInput.value = "";
};

// 處理退款
const handleRefund = async (ticket) => {
  const ok = await confirm({
    title: "確定要進行售後退票？",
    message: `將辦理訂單 #${ticket.t_order_id} 明細 ${ticket.t_detail_id} 的退票手續並退款。`,
    confirmText: "確定退票",
    variant: "danger"
  });

  if (ok) {
    const result = await ticketStore.refundTicket(
      ticket.t_order_id,
      ticket.t_detail_id
    );
    if (result.success) {
      toast.success(result.message);
      ticketStore.fetchRevenue();
    } else {
      toast.error("退票失敗: " + result.message);
    }
  }
};
</script>
