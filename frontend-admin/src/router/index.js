import { createRouter, createWebHistory } from "vue-router";
import { useAuthStore } from "@/stores/auth.js";

import AdminLayout from "@/layouts/AdminLayout.vue";
import LoginLayout from "@/layouts/LoginLayout.vue";

/**
 * 路由策略：
 * - 扁平巢狀兩層：/login（公開）與 /admin/*（需 Session）
 * - 子頁面採動態 import 做 code-splitting
 * - P1/P2 模組先指向 PlaceholderView 佔位，路徑與選單（stores/menu.js）對齊
 */
const routes = [
  {
    path: "/",
    redirect: "/admin/dashboard",
  },
  {
    path: "/login",
    component: LoginLayout,
    meta: { public: true },
    children: [
      { path: "", name: "Login", component: () => import("@/views/LoginView.vue") },
    ],
  },
  {
    path: "/admin",
    component: AdminLayout,
    redirect: "/admin/dashboard",
    children: [
      // ============================================================
      // 營運管理 Operations
      // ============================================================
      // 1 平台總覽
      { path: "dashboard", name: "Dashboard", component: () => import("@/views/DashboardView.vue") },

      // 2 使用者管理
      { path: "users", name: "UserList", component: () => import("@/views/users/UserList.vue") },
      { path: "users/:userId", name: "UserDetail", component: () => import("@/views/users/UserDetail.vue") },

      // 3 商戶管理（頁籤：商戶列表 / KYC 審核）
      //   KYC 收入 organizers 命名空間；organizers/kyc 為靜態段，rank 高於
      //   organizers/:orgId（dynamic），不會被誤匹配。
      { path: "organizers", name: "OrganizerList", component: () => import("@/views/organizers/OrganizerList.vue") },
      { path: "organizers/kyc", name: "KycList", component: () => import("@/views/kyc/KycList.vue") },
      { path: "organizers/kyc/:orgId", name: "KycReview", component: () => import("@/views/kyc/KycReview.vue") },
      { path: "organizers/:orgId", name: "OrganizerDetail", component: () => import("@/views/organizers/OrganizerDetail.vue") },

      // 4 客服與營運查詢（頁籤：客訴回饋 / 活動查詢 / 訂單查詢）
      { path: "operations/submissions", name: "Submissions", component: () => import("@/views/system/Submissions.vue") },
      { path: "operations/events", name: "EventList", component: () => import("@/views/events/EventList.vue") },
      { path: "operations/events/:eventId", name: "EventDetail", component: () => import("@/views/events/EventDetail.vue") },
      { path: "operations/orders", name: "OrderList", component: () => import("@/views/orders/OrderList.vue") },
      { path: "operations/orders/:orderId", name: "OrderDetail", component: () => import("@/views/orders/OrderDetail.vue") },

      // 5 場地公版
      { path: "venues", name: "VenueList", component: () => import("@/views/LocationList.vue") },
      { path: "venues/new", name: "VenueTemplateCreate", component: () => import("@/views/LocationTemplate.vue") },
      { path: "venues/:id/edit", name: "VenueTemplateEdit", component: () => import("@/views/LocationTemplate.vue") },

      // ============================================================
      // 商務財務 Business & Finance
      // ============================================================
      // 6 財務管理（頁籤：財務總覽 / 費用結算）
      { path: "finance", name: "FinanceDashboard", component: () => import("@/views/finance/FinanceDashboard.vue") },
      { path: "finance/settlement", name: "FeeSettlement", component: () => import("@/views/finance/FeeSettlement.vue") },

      // 7 訂閱與合約（頁籤：客戶訂閱 / 方案目錄 / 合約與範本）
      { path: "billing/subscriptions", name: "SubscriptionList", component: () => import("@/views/subscriptions/SubscriptionList.vue") },
      { path: "billing/plans", name: "PlanManage", component: () => import("@/views/subscriptions/PlanManage.vue") },
      { path: "billing/contracts", name: "ContractList", component: () => import("@/views/contracts/ContractList.vue") },
      { path: "billing/contracts/template", name: "ContractTemplateEdit", component: () => import("@/views/contracts/ContractEdit.vue"), meta: { mode: "template" } },
      { path: "billing/contracts/new", name: "ContractCustomNew", component: () => import("@/views/contracts/ContractEdit.vue"), meta: { mode: "custom" } },
      // 合約 id 為字串（CONxxxxxxx），不是數字。用 CON 前綴正則精準匹配，
      // 既能解析字串 id，又不會與上方更具體的 contracts/template、contracts/new 衝突。
      { path: "billing/contracts/:contractId(CON\\w+)", name: "ContractDetail", component: () => import("@/views/contracts/ContractDetail.vue") },

      // ============================================================
      // 系統權限 System
      // ============================================================
      // 8 權限管理（RbacTabs：內部人員 / 平台角色 / 組織角色模板 / 選單資源）
      { path: "change-password", name: "ChangePassword", component: () => import("@/views/rbac/ChangePasswordView.vue") },
      { path: "rbac", redirect: "/admin/rbac/staff" },
      { path: "rbac/staff", name: "RbacStaffList", component: () => import("@/views/rbac/StaffList.vue") },
      { path: "rbac/staff/:userId", name: "RbacStaffDetail", component: () => import("@/views/users/UserDetail.vue") },
      { path: "rbac/roles", name: "RbacRoleList", component: () => import("@/views/rbac/RoleList.vue") },
      { path: "rbac/templates", name: "RbacTemplateList", component: () => import("@/views/rbac/TemplateList.vue") },
      { path: "rbac/resources", name: "RbacResourceList", component: () => import("@/views/rbac/ResourceList.vue") },

      // 9 通知與公告 / 10 系統維護 / 11 稽核日誌（皆在 system 命名空間）
      //   bare /admin/system 落「通知與公告」群首頁（公告）。客訴已移至 operations。
      { path: "system", redirect: "/admin/system/announcements" },
      { path: "system/announcements", name: "Announcements", component: () => import("@/views/system/Announcements.vue") },
      { path: "system/templates", name: "NotificationTemplates", component: () => import("@/views/system/NotificationTemplates.vue") },
      { path: "system/notifications", name: "NotificationLogs", component: () => import("@/views/system/NotificationLogs.vue") },
      { path: "system/jobs", name: "ScheduledJobs", component: () => import("@/views/system/ScheduledJobs.vue") },
      { path: "system/dictionaries", name: "Dictionaries", component: () => import("@/views/system/Dictionaries.vue") },
      { path: "system/config", name: "SystemConfig", component: () => import("@/views/system/SystemConfig.vue") },
      { path: "system/media", name: "MediaFiles", component: () => import("@/views/system/MediaFiles.vue") },
      { path: "system/audit", name: "AuditLog", component: () => import("@/views/system/AuditLog.vue") },


      // ============================================================
      // 舊路徑 redirect 兜底（命名空間搬移前的連結不致 404）
      // ============================================================
      { path: "rbac/matrix", redirect: "/admin/rbac/roles" },
      { path: "kyc", redirect: "/admin/organizers/kyc" },
      { path: "kyc/:orgId", redirect: (to) => `/admin/organizers/kyc/${to.params.orgId}` },
      { path: "system/submissions", redirect: "/admin/operations/submissions" },
      { path: "events", redirect: "/admin/operations/events" },
      { path: "events/:eventId", redirect: (to) => `/admin/operations/events/${to.params.eventId}` },
      { path: "orders", redirect: "/admin/operations/orders" },
      { path: "orders/:orderId", redirect: (to) => `/admin/operations/orders/${to.params.orderId}` },
      { path: "subscriptions", redirect: "/admin/billing/subscriptions" },
      { path: "subscriptions/plans", redirect: "/admin/billing/plans" },
      { path: "contracts", redirect: "/admin/billing/contracts" },
      { path: "contracts/template", redirect: "/admin/billing/contracts/template" },
      { path: "contracts/new", redirect: "/admin/billing/contracts/new" },
      { path: "contracts/:contractId(CON\\w+)", redirect: (to) => `/admin/billing/contracts/${to.params.contractId}` },
    ],
  },
  {
    path: "/:pathMatch(.*)*",
    name: "NotFound",
    component: () => import("@/views/NotFoundView.vue"),
    meta: { public: true },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
});

// 每次 App 載入只向後端驗證一次 Session，之後信任 store 狀態（401 攔截器兜底）
let sessionVerified = false;

router.beforeEach(async (to) => {
  if (to.meta.public) return true;

  const auth = useAuthStore();

  if (!sessionVerified) {
    sessionVerified = true;
    await auth.fetchMe();
  }

  if (!auth.isLoggedIn) {
    // 記住原目的地，登入成功後導回
    return { path: "/login", query: { redirect: to.fullPath } };
  }

  // 強制修改密碼檢查
  if (auth.user?.mustChangePassword && to.name !== "ChangePassword") {
    return { path: "/admin/change-password", query: { force: "true" } };
  }

  return true;
});

export default router;
