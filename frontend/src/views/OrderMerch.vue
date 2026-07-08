<template>
  <div class="container-xl py-4">
    <div class="w-100">
      <header class="d-flex justify-content-between align-items-center mb-3 flex-nowrap">
        <h3 class="fw-bold mb-0 text-nowrap">商城訂單</h3>
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
              placeholder="搜尋商城訂單編號"
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
          <!-- 關聯活動篩選下拉選單 -->
          <div class="dropdown">
            <button
              class="btn btn-outline-secondary dropdown-toggle"
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
              class="btn btn-outline-secondary dropdown-toggle"
              data-bs-toggle="dropdown"
            >
              狀態: {{ selectedStatus }}
            </button>
            <ul
              class="dropdown-menu dropdown-menu-end shadow-sm border-0 custom-dropdown"
            >
              <li>
                <a class="dropdown-item" @click="selectedStatus = '全部'">全部</a>
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '正常'">正常</a>
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '待付'">待付</a>
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '退貨'">退貨</a>
              </li>
              <li>
                <a class="dropdown-item" @click="selectedStatus = '取消'">取消</a>
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
                <th scope="col" class="py-3">關聯活動</th>
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
                :key="data.m_order_id + '-' + data.m_detail_id"
              >
                <td class="text-start ps-3">
                  <div class="d-flex flex-column gap-1">
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-secondary border font-monospace px-2 py-1"
                        >訂單</span
                      >
                      <span class="fw-bold text-dark font-monospace"
                        >#{{ data.m_order_id || "無資料" }}</span
                      >
                    </div>
                    <div class="d-flex align-items-center gap-2">
                      <span
                        class="badge bg-light text-muted border font-monospace px-2 py-1"
                        >明細</span
                      >
                      <span class="text-muted small font-monospace">{{
                        data.m_detail_id || "無資料"
                      }}</span>
                    </div>
                  </div>
                </td>
                <td>{{ getOrgName(data.organizer_id) }}</td>
                <td>{{ getSessionTitleByTheme(data.theme_id) }}</td>
                <td>{{ data.user_id }}</td>
                <td>NT$ {{ data.unit_price }} (x{{ data.quantity }})</td>
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
                    data-bs-target="#merchDetail"
                    @click="selectedOrder = data"
                  >
                    詳情
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <MerchDetail :order="selectedOrder" />
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
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  box-shadow: 0 0 4px currentColor;
}
</style>

<script setup>
import { ref, computed, onMounted, watch } from "vue";
import MerchDetail from "@/components/MerchDetail.vue";
import axios from "@/plugins/axios.js";
import sessionData from "@/data/sessionData.json";

// 每頁顯示筆數與篩選變數
const pageSize = ref(10);
const selectedStatus = ref("全部"); // 篩選狀態 (全部/正常/待付/退貨/取消)
const selectedOrg = ref("全部"); // 篩選主辦方
const selectedSession = ref("全部"); // 篩選場次
const searchOrderId = ref(""); // 全域搜尋商城訂單編號

const selectedOrder = ref(null); // 點擊「詳情」時選中的訂單明細
const merchList = ref([]); // 商品訂單明細資料
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

// 元件掛載完成時獲取資料
const fetchMerches = async () => {
  try {
    const response = await axios.get("/api/merch");
    merchList.value = response.data;
  } catch (error) {
    console.error("無法取得商品訂單資料:", error);
  }
};

onMounted(() => {
  fetchMerches();
  const orgs = localStorage.getItem("user_orgs");
  orgList.value = orgs
    ? JSON.parse(orgs)
    : [
        { id: "ORG0000001", name: "晴天音樂祭有限公司" },
        { id: "ORG0000002", name: "藍海展覽股份有限公司" },
        { id: "ORG0000005", name: "星光娛樂有限公司" },
      ];
});

// 計算目前選擇的主辦方名稱
const selectedOrgName = computed(() => {
  if (selectedOrg.value === "全部") return "全部";
  const org = orgList.value.find((o) => o.id === selectedOrg.value);
  return org ? org.name : selectedOrg.value;
});

// 取得主辦方名稱
const getOrgName = (orgId) => {
  const id = orgId || "ORG0000001";
  const org = orgList.value.find((o) => o.id === id);
  return org ? org.name : id;
};

// 計算目前選擇的場次標題
const selectedSessionTitle = computed(() => {
  if (selectedSession.value === "全部") return "全部";
  const session = sessionList.value.find(
    (s) => s.session_id === selectedSession.value,
  );
  return session ? session.title : selectedSession.value;
});

// 根據商品訂單對應的 theme_id 反查場次標題 (用於表格顯示)
const getSessionTitleByTheme = (themeId) => {
  const id = themeId || 1;
  const session = sessionList.value.find((s) => s.theme_id === id);
  return session ? session.title : `活動主題 #${id}`;
};

// 訂單表格資料來源 (過濾後並套用分頁)
const tableData = computed(() => {
  return merchList.value
    .filter((data) => {
      // 1. 狀態篩選
      const matchStatus =
        selectedStatus.value === "全部" ||
        data.item_status === selectedStatus.value;

      // 2. 主辦方篩選
      const matchOrg =
        selectedOrg.value === "全部" ||
        (data.organizer_id || "ORG0000001") === selectedOrg.value;

      // 3. 關聯活動場次篩選 (將商品的 theme_id 與場次對應的 theme_id 比對)
      let matchSession = true;
      if (selectedSession.value !== "全部") {
        const session = sessionList.value.find(
          (s) => s.session_id === selectedSession.value,
        );
        if (session) {
          matchSession = data.theme_id === session.theme_id;
        } else {
          matchSession = false;
        }
      }

      // 4. 搜尋訂單編號篩選
      const matchSearch =
        !searchOrderId.value.trim() ||
        (data.m_order_id &&
          data.m_order_id
            .toLowerCase()
            .includes(searchOrderId.value.trim().toLowerCase()));

      return matchStatus && matchOrg && matchSession && matchSearch;
    })
    .slice(0, pageSize.value);
});
</script>
