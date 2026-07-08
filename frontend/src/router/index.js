// 匯入 vue-router 所需函式
import { useWorkspaceStore } from "@/stores/workspace.js";
import { createRouter, createWebHistory } from "vue-router";
// [Jason] RBAC #4b M7：以該組織角色過濾後的選單樹做逐頁 gating
import { useToast } from "@/composables/useToast.js";
import { useMenuStore } from "@/stores/menu.js";
import { updateFavicon } from "@/utils/favicon.js";

// ==========================================
// 【合併 Main 分支建議移除之舊版 Layout 檔案】
// 當此分支合併至 main 時，請確保一併刪除以下不再使用的 Layout 檔案：
// 1. src/layouts/OrganizerLayout.vue (已由 OrganizerPortalLayout.vue 取代)
// 2. src/layouts/OrgLoginLayout.vue (已由 OrganizerDashboardLayout.vue 取代)
// ==========================================

//匯入組件
import OrganizerDashboardLayout from "@/layouts/OrganizerDashboardLayout.vue";
import OrganizerPortalLayout from "@/layouts/OrganizerPortalLayout.vue";
import UserLayout from "@/layouts/UserLayout.vue";
import NotFound from "@/views/NotFoundView.vue";
import OrderMerch from "@/views/OrderMerch.vue";
import OrderTicket from "@/views/OrderTicket.vue";
import OrderTicketCheckIn from "@/views/OrderTicketCheckIn.vue";
// [Jason] 現場掃碼驗票（獨立掃碼頁，與組員的 /checkin 管理頁職責分離）
import SettingsLayout from "@/layouts/nested/SettingsLayout.vue";
import AccountSettings from "@/views/AccountSettings.vue";
import BookTicket from "@/views/BookTicket.vue";
import DocumentView from "@/views/DocumentView.vue";
import ForbiddenView from "@/views/ForbiddenView.vue";
import Login from "@/views/Login.vue";
import OrderMerchReturned from "@/views/OrderMerchReturned.vue";
import ScanCheckIn from "@/views/org/ScanCheckIn.vue";
import OrganizerLogin from "@/views/OrganizerLogin.vue";
import OrganizerSelect from "@/views/OrganizerSelect.vue";
import OrganizerSettlement from "@/views/OrganizerSettlement.vue";
import Profile from "@/views/Profile.vue";
import Theme from "@/views/Theme.vue";
import ThemeList from "@/views/ThemeList.vue";
import AuctionDetail from "@/views/AuctionDetail.vue";
import ThemeManage from "@/views/ThemeManage.vue";
import TicketTypeEdit from "@/views/TicketTypeEdit.vue";

import PaymentMerch from "@/views/PaymentMerch.vue";
import PaymentSuccess from "@/views/PaymentSuccess.vue";
import PaymentTicket from "@/views/PaymentTicket.vue";

import AdminLayout from "@/views/products/AdminLayout.vue";
import CartPage from "@/views/products/CartPage.vue";
import ProductList from "@/views/products/ProductList.vue";
import ShopDetail from "@/views/products/ShopDetail.vue";
import ShopHome from "@/views/products/ShopHome.vue";
import ShopLayout from "@/views/products/ShopLayout.vue";
import ShopTheme from "@/views/products/ShopTheme.vue";
// 定義路由
const routes = [
    // 一般使用者介面
    {
        path: "/",
        component: UserLayout,
        children: [
            {
                path: "/",
                redirect: "/themes",
            },
            {
                path: "/themes",
                component: ThemeList,
                name: "ThemeList",
            },
            {
                path: "/paymentTicket",
                component: PaymentTicket,
                name: "PaymentTicket",
            },
            {
                path: "/paymentMerch",
                component: PaymentMerch,
                name: "PaymentMerch",
            },
            {
                path: "/paymentSuccess",
                component: PaymentSuccess,
                name: "PaymentSuccess",
            },
            {
                path: "/member/orders",
                component: () => import("@/views/member/MemberOrders.vue"),
                name: "MemberOrders",
            },
            {
                path: "themes/:themeId",
                component: Theme,
                name: "Theme",
            },
            {
                path: "themes/:themeId/auctions/:auctionId",
                component: AuctionDetail,
                name: "AuctionDetail",
            },
            {
                path: "themes/:themeId/session/:sessionId/book-ticket",
                component: BookTicket,
                name: "BookTicket",
            },
            {
                path: "login", // 一般使用者登入
                component: Login,
                name: "Login",
            },
            {
                path: "profile",
                redirect: "/settings/profile",
            },
            {
                path: "account",
                redirect: "/settings/account",
            },
            {
                path: "settings",
                component: SettingsLayout,
                children: [
                    {
                        path: "",
                        redirect: "/settings/profile",
                    },
                    {
                        path: "profile",
                        component: Profile,
                        name: "Profile",
                    },
                    {
                        path: "account",
                        component: AccountSettings,
                        name: "AccountSettings",
                    },
                ],
            },
            {
                path: "privacy",
                redirect: "/docs/privacy",
            },
            {
                path: "terms",
                redirect: "/docs/terms",
            },
            {
                path: "docs/:docType(privacy|terms)",
                component: DocumentView,
                name: "UserDocument",
            },
            {
                path: "/shop",
                component: ShopLayout,
                redirect: "/shop/home",
                children: [
                    {
                        path: "home",
                        component: ShopHome, // 前台商品選購大廳
                        name: "ShopHome",
                    },
                    {
                        path: "theme/:id",
                        component: ShopTheme, // 主題商品選購大廳
                        name: "ShopTheme",
                    },
                    {
                        path: "product/:id",
                        component: ShopDetail, // 商品詳情頁（選顏色尺寸）
                        name: "ShopDetail",
                    },
                    {
                        path: "cart",
                        component: CartPage, // 購物車清單與結帳準備頁
                        name: "CartPage",
                    },
                ],
            },
        ],
    },
    // 廠商 門戶與登入 (套用門戶外殼 OrganizerPortalLayout)
    {
        path: "/org",
        component: OrganizerPortalLayout,
        children: [
            {
                path: "",
                redirect: "/org/login",
            },
            {
                path: "login", // 廠商登入 (實際網址為 /org/login)
                component: OrganizerLogin,
                name: "OrganizerLogin",
            },
            {
                path: "select", // 組織選擇啟動台 (實際網址為 /org/select)
                component: OrganizerSelect,
                name: "OrganizerSelect",
            },
            {
                path: "guide",
                redirect: "/org/docs/guide",
            },
            {
                path: "docs/:docType(privacy|terms|guide)",
                component: DocumentView,
                name: "OrgDocument",
            },
            {
                path: "settings",
                component: SettingsLayout,
                children: [
                    {
                        path: "",
                        redirect: "/org/settings/profile",
                    },
                    {
                        path: "profile",
                        component: Profile,
                        name: "OrgProfile",
                    },
                    {
                        path: "account",
                        component: AccountSettings,
                        name: "OrgAccountSettings",
                    },
                ],
            },
        ],
    },
    // 廠商 後台管理控制台 (套用後台外殼 OrganizerDashboardLayout)
    // 限制匹配 ORG 開頭 + 7 位數字格式的 ID
    {
        path: "/org/:organizerId(ORG\\d{7})",
        component: OrganizerDashboardLayout,
        beforeEnter: async (to) => {
            console.log("[beforeEnter Parent] Entering B2B Org Parent path:", to.path);
            const targetOrgId = to.params.organizerId;
            const menuStore = useMenuStore();
            console.log("[beforeEnter Parent] Fetching menu for org:", targetOrgId);
            await menuStore.fetchMenu(targetOrgId);
            console.log("[beforeEnter Parent] Menu loaded. Tree size:", menuStore.menuTree.length);

            const stripped = menuStore._stripOrg(to.path);
            if (stripped === "/") {
                const landing = menuStore.firstAccessiblePath;
                console.log("[beforeEnter Parent] firstAccessiblePath is:", landing);
                if (landing) {
                    const resolved = menuStore.resolvePath(landing, targetOrgId);
                    console.log("[beforeEnter Parent] Redirecting to resolved path:", resolved);
                    if (menuStore._stripOrg(resolved) !== "/") {
                        return resolved;
                    }
                }
                console.log("[beforeEnter Parent] Fallback redirecting to ThemeManage with param:", targetOrgId);
                return { name: "ThemeManage", params: { organizerId: targetOrgId } };
            }
            console.log("[beforeEnter Parent] Subpath detected, proceeding to page.");
        },
        children: [
            {
                path: "themes", // 實際網址為 /org/ORGxxxxxxx/themes
                component: ThemeManage,
                name: "ThemeManage",
            },
            {
                path: "themes/sessions/:sessionId/ticket-types", // 實際網址為 /org/:organizerId/themes/sessions/:sessionId/ticket-types
                component: TicketTypeEdit,
                name: "TicketTypeEdit",
            },
            {
                path: "ticket", // 實際網址為 /org/ORGxxxxxxx/ticket
                component: OrderTicket,
                name: "OrderTicket",
            },
            {
                path: "checkin", // 實際網址為 /org/ORGxxxxxxx/checkin
                component: OrderTicketCheckIn,
                name: "OrderTicketCheckIn",
            },
            {
                // [Jason] 現場掃碼驗票（相機 + /api/checkin）：實際網址為 /org/ORGxxxxxxx/scan
                path: "scan",
                component: ScanCheckIn,
                name: "OrgScanCheckIn",
            },
            {
                path: "merch", // 實際網址為 /org/ORGxxxxxxx/merch
                component: OrderMerch,
                name: "OrderMerch",
            },
            {
                path: "merch-returned", // 實際網址為 /org/ORGxxxxxxx/merch-returned
                component: OrderMerchReturned,
                name: "OrderMerchReturned",
            },
            {
                // [Jason] 退款申請審核（退票/退貨審核制）：實際網址為 /org/ORGxxxxxxx/refund-requests
                path: "refund-requests",
                component: () => import("@/views/RefundRequestReview.vue"),
                name: "RefundRequestReview",
            },
            {
                path: "settlement", // 實際網址為 /org/ORGxxxxxxx/settlement
                component: OrganizerSettlement,
                name: "OrganizerSettlement",
            },
            {
                path: "admin",
                component: AdminLayout,
                redirect: (to) => `/org/${to.params.organizerId}/admin/products`,
                children: [
                    {
                        path: "products",
                        component: ProductList,
                        name: "ProductList",
                    },
                    {
                        path: "product-add",
                        component: () => import('@/views/products/ProductForm.vue'),
                        name: "ProductAdd",
                    },
                    {
                        path: "products/:productId/edit",
                        component: () => import('@/views/products/ProductForm.vue'),
                        name: "ProductEdit",
                    },
                ],
            },

            // 團隊管理 (成員、角色與權限)
            {
                path: "team",
                component: () => import("@/layouts/nested/TeamLayout.vue"),
                children: [
                    {
                        path: "",
                        redirect: { name: "OrgMembers" },
                    },
                    {
                        path: "members",
                        component: () => import("@/views/org/team/Members.vue"),
                        name: "OrgMembers",
                    },
                    {
                        path: "roles",
                        component: () => import("@/views/org/team/Roles.vue"),
                        name: "OrgRoles",
                    },
                ],
            },
            // 組織設定 (組織資料與 KYC、合約、訂閱方案)
            {
                path: "settings",
                component: () => import("@/layouts/nested/OrgSettingsLayout.vue"),
                children: [
                    {
                        path: "",
                        redirect: { name: "OrgSettingsProfile" },
                    },
                    {
                        path: "profile",
                        component: () => import("@/views/org/settings/OrgProfile.vue"),
                        name: "OrgSettingsProfile",
                    },
                    {
                        path: "contracts",
                        component: () => import("@/views/org/settings/Contracts.vue"),
                        name: "OrgContracts",
                    },
                    {
                        path: "subscription",
                        component: () => import("@/views/org/settings/Subscription.vue"),
                        name: "OrgSubscription",
                    },
                ],
            },
        ],
    },

    // 廠商 登入後介面

    // 403
    {
        path: "/403",
        component: ForbiddenView,
        name: "Forbidden",
    },
    // 404
    {
        path: "/:pathmatch(.*)*",
        component: NotFound,
        name: NotFound,
    },
];

/*
=========================================================================
【舊版 B2B 路由對比與重構原因備忘】

舊版路由結構：
{
    path: "/org",
    component: OrganizerLayout,
    children: [
        {
            path: "organizerLogin",
            component: OrgLoginLayout,
            children: [
                { path: "organizers/:organizerId/themes", component: ThemeManage },
                { path: "ticket", component: Ticket },
                { path: "merch", component: Merch },
                { path: "settlement", component: Settlement }
            ]
        },
        { path: "login", component: OrganizerLogin },
        { path: "guide", component: OrganizerGuide }
    ]
}

重構原因：
1. 解決佈局巢狀套疊衝突：舊版將後台 (OrgLoginLayout) 套在門戶 (OrganizerLayout) 內，
   導致頁面上同時渲染兩個 Footer，且迫使側邊欄使用 fixed 定位與 z-index 補丁去避開外殼 Navbar。
2. 補上網址 Context (防呆與防止多開分頁狀態污染)：舊版的票務 (ticket)、商品 (merch) 與結算 (settlement) 
   未包含 :organizerId，多開分頁或重新整理 (F5) 會因為狀態清除而丟失商家 context。
   新版將 ID 移到根路由 "/org/:organizerId" 並加入正則驗證 (ORG\d{7}) 以對齊資料庫。
3. 語意優化：移除容易讓人誤解為登入頁的 "organizerLogin" 控制台路徑。
=========================================================================
*/

// 建立 router
const router = createRouter({
    history: createWebHistory(),
    routes,
    // 返回上一頁時保留原本位置
    scrollBehavior(to, from, savedPosition) {
        if (savedPosition) {
            return savedPosition
        }

        // 其他情況一律回到最上面
        return {
            top: 0,
            left: 0,
            behavior: 'smooth' // 可改成 'auto'
        }
    }
});

// 全域導航守衛
router.beforeEach(async (to, from, next) => {
    console.log(
        "[Global beforeEach] Navigating to:",
        to.path,
        "params:",
        to.params,
        "matched:",
        to.matched.map((r) => r.path),
    );
    const isLoggedIn = localStorage.getItem("is_logged_in") === "true";

    // 已登入者點選主辦方登入頁，直接進到選擇組織頁
    if (to.path === "/org/login" && isLoggedIn) {
        return next({ name: "OrganizerSelect" });
    }

    // 已登入者點選一般登入頁，直接導向首頁活動列表
    if (to.path === "/login" && isLoggedIn) {
        return next({ name: "ThemeList" });
    }

    // 檢查是否為 B2B 管理路由 (/org/ORGxxxxxxx 開頭)
    if (to.path.startsWith("/org/ORG")) {
        if (!isLoggedIn) {
            return next({ name: "OrganizerLogin" }); // 未登入引導至主辦方登入頁
        }

        // 以 workspace store 的組織清單（唯一來源）做權限檢查；尚未載入時先向後端拉取
        const workspaceStore = useWorkspaceStore();
        if (!workspaceStore.myOrgsLoaded) {
            await workspaceStore.fetchMyOrgs();
        }

        const targetOrgId = to.params.organizerId;
        const hasAccess = workspaceStore.myOrgs.some((org) => org.id === targetOrgId);
        if (!hasAccess) {
            // [Jason] 帶 b2b 標記，讓 403 頁的「返回首頁/切換帳號」按鈕導向 B2B 情境（返回上一頁則走瀏覽器原生上一頁）
            return next({ name: "Forbidden", query: { b2b: "1" } }); // 非該組織成員導向 403
        }

        // [Jason] RBAC #4b M7：逐頁權限 gating（在「組織成員」檢查之上再加一層）。
        // 以該組織角色過濾後的選單樹判斷此頁是否可見。採 fetch-then-guard + fail-open：
        // 僅在「選單已成功載入且確為當前組織」時才據以阻擋，否則放行，避免後端異常把成員鎖在門外。
        // 組織根路徑（會 redirect 到實際頁）不在此攔，交由 redirect 後的目標頁再判斷。
        const menuStore = useMenuStore();
        await menuStore.fetchMenu(targetOrgId);
        const stripped = menuStore._stripOrg(to.path);
        if (stripped !== "/" && menuStore.menuLoaded && menuStore.currentOrgId === targetOrgId && !menuStore.isPathAllowed(to.path)) {
            // [Dev Mode] 開發環境且未開啟嚴格模式下僅警告不阻擋，方便組員快速開發
            // 若要測試正式 403 阻擋，可在瀏覽器 Console 執行: localStorage.setItem('strict_rbac', 'true')
            const isStrict = localStorage.getItem("strict_rbac") === "true";
            if (import.meta.env.DEV && !isStrict) {
                console.warn(`[Dev Bypass] 偵測到未註冊路徑: ${to.path}，開發模式已自動放行。`);
                useToast().error("403", 3000);
                return next();
            }
            // [Jason] 落點救援：目標頁無權限時，先導向該角色「第一個有權限的頁」，而非直接 403。
            // 解決會計等受限角色進組織被 fallback 到 /themes 再被踢 403，而不是落在可用頁的問題。
            const landing = menuStore.firstAccessiblePath;
            if (landing) {
                const resolved = menuStore.resolvePath(landing, targetOrgId);
                if (resolved && menuStore._stripOrg(resolved) !== stripped && menuStore.isPathAllowed(resolved)) {
                    return next(resolved);
                }
            }
            // 真的沒有任何可用頁時才導 403（帶 b2b 標記供 403 頁情境導向）
            return next({ name: "Forbidden", query: { b2b: "1" } }); // 該組織角色無此頁權限導向 403
        }
    }
    next();
});

// 依情境動態切換分頁 favicon：/org 開頭視為 B2B 廠商情境，其餘為 B2C 前台
router.afterEach((to) => {
    updateFavicon(to.path.startsWith("/org"));
});

// 匯出 router
export default router;
