<template>
  <div class="container-xl py-4">
    <div class="w-100">
      <header class="d-flex justify-content-between align-items-center mb-4">
        <div>
          <h3 class="fw-bold mb-1">商品退貨與售後</h3>
          <p class="text-muted mb-0 small">
            快速辦理售後退貨與退款作業
          </p>
        </div>
        <div
          class="bg-white px-4 py-2 rounded-pill shadow-sm border d-flex align-items-center gap-2"
        >
          <span class="text-muted small">當前統計營收:</span>
          <span class="fw-bold text-success"
            >NT$ {{ totalRevenue }}</span
          >
        </div>
      </header>

      <!-- 快速退貨辦理面板 -->
      <section class="card border-0 shadow-sm rounded-4 mb-4 p-4">
        <h5 class="fw-bold mb-3 text-danger">
          <i class="bi bi-arrow-left-right me-2"></i>快速退貨辦理
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
                placeholder="請輸入商品明細 ID..."
                required
              />
            </div>
          </div>
          <div class="col-md-4">
            <button
              type="submit"
              class="btn btn-danger w-100 rounded-3 fw-bold py-2 shadow-sm"
            >
              <i class="bi bi-arrow-left-right me-1"></i>辦理退貨
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

      <!-- 商品明細表格 -->
      <main class="card border-0 shadow-sm rounded-4 p-3">
        <div
          class="d-flex justify-content-between align-items-center mb-3 flex-wrap gap-2"
        >
          <h5 class="fw-bold mb-0 text-nowrap">所有商品訂單明細</h5>
          <div class="d-flex gap-2 flex-wrap align-items-center ms-auto">
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
                placeholder="搜尋商品明細"
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
            <!-- 關聯活動篩選下拉選單 -->
            <div class="dropdown">
              <button
                class="btn btn-outline-secondary dropdown-toggle btn-sm"
                data-bs-toggle="dropdown"
              >
                關聯活動: {{ selectedSessionTitle }}
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
                  <a class="dropdown-item" @click="selectedStatus = '退貨'"
                    >退貨</a
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
                <th scope="col" class="py-3">關聯活動</th>
                <th scope="col" class="py-3">購買者</th>
                <th scope="col" class="py-3">單價</th>
                <th scope="col" class="py-3">數量</th>
                <th scope="col" class="py-3">小計</th>
                <th scope="col" class="py-3">訂單狀態</th>
                <th scope="col" class="py-3">操作</th>
              </tr>
            </thead>
            <tbody class="text-center">
              <tr
                v-for="m in filteredMerches"
                :key="m.m_order_id + '-' + m.m_detail_id"
              >
                <td class="text-start ps-3">
                  <div class="d-flex flex-column gap-1">
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-secondary border font-monospace px-2 py-1"
                        >訂單</span
                      >
                      <span class="fw-bold text-dark font-monospace"
                        >#{{ m.m_order_id || "無資料" }}</span
                      >
                    </div>
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-muted border font-monospace px-2 py-1"
                        >明細</span
                      >
                      <span class="text-muted small font-monospace">{{
                        m.m_detail_id || "無資料"
                      }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ getOrgName(m.organizer_id) }}</td>
                <td>{{ getSessionTitleByTheme(m.theme_id) }}</td>
                <td>{{ m.user_id }}</td>
                <td>NT$ {{ m.unit_price }}</td>
                <td>{{ m.quantity }}</td>
                <td>NT$ {{ m.unit_price * m.quantity }}</td>
                <td>
                  <span
                    class="badge rounded-pill px-3 py-2 d-inline-flex align-items-center"
                    :class="
                      m.item_status === '正常'
                        ? 'badge-soft-success'
                        : m.item_status === '退貨'
                          ? 'badge-soft-danger'
                          : m.item_status === '取消'
                            ? 'badge-soft-secondary'
                            : 'badge-soft-warning'
                    "
                  >
                    <span
                      class="status-dot me-2"
                      :class="
                        m.item_status === '正常'
                          ? 'bg-success'
                          : m.item_status === '退貨'
                            ? 'bg-danger'
                            : m.item_status === '取消'
                              ? 'bg-secondary'
                              : 'bg-warning'
                      "
                    ></span>
                    {{ m.item_status }}
                  </span>
                </td>
                <td>
                  <button
                    v-if="m.item_status === '正常'"
                    class="btn btn-sm btn-outline-danger px-3 rounded-pill fw-bold"
                    @click="handleRefund(m)"
                  >
                    退貨
                  </button>
                  <span v-else-if="m.item_status === '退貨'" class="text-muted small">已完成退貨</span>
                  <span v-else-if="m.item_status === '取消'" class="text-muted small">已取消</span>
                  <span v-else-if="m.item_status === '待付'" class="text-muted small">未付款</span>
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
</style>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import { useToast } from "@/composables/useToast.js";
import { useConfirm } from "@/composables/useConfirm.js";
import axios from "@/plugins/axios.js";
import sessionData from "@/data/sessionData.json";

const toast = useToast();
const { confirm } = useConfirm();

const merchList = ref([]);
const totalRevenue = ref(0);
const refundInput = ref("");
const selectedStatus = ref("全部");
const selectedOrg = ref("全部");
const selectedSession = ref("全部");
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

  // 後備機制：如果靜態映射找不到，試著從訂單明細資料中匹配
  const matchedDetail = merchList.value.find((d) => d.theme_id === themeId);
  if (matchedDetail && matchedDetail.organizer_id) {
    return matchedDetail.organizer_id;
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

const fetchMerches = async () => {
  try {
    const res = await axios.get("/api/merch");
    merchList.value = res.data;
  } catch (err) {
    console.error("無法取得商品明細資料:", err);
  }
};

const fetchRevenue = async () => {
  try {
    const res = await axios.get("/api/revenue/merch");
    totalRevenue.value = res.data.totalRevenue;
  } catch (err) {
    totalRevenue.value = merchList.value
      .filter((m) => m.item_status === "正常")
      .reduce((sum, m) => sum + Number(m.unit_price) * Number(m.quantity), 0);
  }
};

onMounted(async () => {
  await fetchMerches();

  // 載入主辦方清單
  const orgs = localStorage.getItem("user_orgs");
  orgList.value = orgs
    ? JSON.parse(orgs)
    : [
        { id: "ORG0000001", name: "晴天音樂祭有限公司" },
        { id: "ORG0000002", name: "藍海展覽股份有限公司" },
        { id: "ORG0000005", name: "星光娛樂有限公司" },
      ];

  fetchRevenue();
});

const selectedOrgName = computed(() => {
  if (selectedOrg.value === "全部") return "全部";
  const org = orgList.value.find((o) => o.id === selectedOrg.value);
  return org ? org.name : selectedOrg.value;
});

const getOrgName = (orgId) => {
  const id = orgId || "ORG0000001";
  const org = orgList.value.find((o) => o.id === id);
  return org ? org.name : id;
};

const selectedSessionTitle = computed(() => {
  if (selectedSession.value === "全部") return "全部";
  const session = sessionList.value.find(
    (s) => s.session_id === selectedSession.value,
  );
  return session ? session.title : selectedSession.value;
});

const getSessionTitleByTheme = (themeId) => {
  const id = themeId || 1;
  const session = sessionList.value.find((s) => s.theme_id === id);
  return session ? session.title : `活動主題 #${id}`;
};

// 狀態卡片數據
const stats = computed(() => {
  const list = merchList.value;
  const total = list.length;
  const refundable = list.filter((m) => m.item_status === "正常").length;
  const refunded = list.filter((m) => m.item_status === "退貨").length;

  return [
    {
      title: "總商品明細數量",
      value: `${total} 筆`,
      icon: "bi-box-seam",
      bgClass: "bg-primary text-primary",
    },
    {
      title: "可退貨數量",
      value: `${refundable} 筆`,
      icon: "bi-check-circle",
      bgClass: "bg-success text-success",
    },
    {
      title: "退貨辦理數",
      value: `${refunded} 筆`,
      icon: "bi-arrow-counterclockwise",
      bgClass: "bg-danger text-danger",
    },
  ];
});

// 篩選與分頁
const filteredMerches = computed(() => {
  return merchList.value
    .filter((m) => {
      const matchStatus =
        selectedStatus.value === "全部" ||
        m.item_status === selectedStatus.value;
      const matchOrg =
        selectedOrg.value === "全部" ||
        (m.organizer_id || "ORG0000001") === selectedOrg.value;

      // 關聯活動場次篩選 (商品 theme_id 與場次 theme_id 比對)
      let matchSession = true;
      if (selectedSession.value !== "全部") {
        const session = sessionList.value.find(
          (s) => s.session_id === selectedSession.value,
        );
        if (session) {
          matchSession = m.theme_id === session.theme_id;
        } else {
          matchSession = false;
        }
      }

      // 全域搜尋明細 ID 篩選
      const matchSearch =
        !searchDetailId.value.trim() ||
        (m.m_detail_id &&
          m.m_detail_id
            .toLowerCase()
            .includes(searchDetailId.value.trim().toLowerCase()));

      return matchStatus && matchOrg && matchSession && matchSearch;
    })
    .slice(0, pageSize.value);
});

// 處理快速退貨
const handleQuickRefund = async () => {
  const code = refundInput.value.trim();
  if (!code) return;

  const item = merchList.value.find(
    (m) => m.m_detail_id && m.m_detail_id.toLowerCase() === code.toLowerCase()
  );

  if (!item) {
    toast.error("找不到符合該明細 ID 的商品，請重新輸入。");
    return;
  }

  if (item.item_status === "退貨") {
    toast.info("該商品先前已完成退貨程序。");
    return;
  }

  if (item.item_status === "取消") {
    toast.info("該商品所屬訂單已被取消，無法進行退貨辦理。");
    return;
  }

  if (item.item_status === "待付") {
    toast.info("該商品尚未付款，無法進行退貨辦理。");
    return;
  }

  await handleRefund(item);
  refundInput.value = "";
};

// 處理退貨
const handleRefund = async (item) => {
  const ok = await confirm({
    title: "確定要進行售後退貨？",
    message: `將辦理訂單 #${item.m_order_id} 明細 ${item.m_detail_id} 的退貨手續並退款。`,
    confirmText: "確定退貨",
    variant: "danger"
  });

  if (ok) {
    try {
      const res = await axios.post(
        `/api/merch/${item.m_order_id}/refund/${item.m_detail_id}`
      );

      await fetchMerches();

      toast.success(res.data.message || "退貨成功");
      fetchRevenue();
    } catch (err) {
      toast.error(err.response?.data?.message || "退貨失敗，找不到該筆明細！");
    }
  }
};
</script>
