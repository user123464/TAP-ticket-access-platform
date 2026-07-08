-- =========================================
-- 新增開發測試資料 (Seed Data)
-- 順序：依賴層級由上往下 (父表 -> 子表)
-- =========================================
/* -----------------------------------------
    1. 會員與主辦方模塊 (最頂層依賴)
   ----------------------------------------- */
INSERT INTO [permission] (permission_id, resource_code, action_code, description) VALUES
-- ── 平台管理類（ADMIN + SUPER_ADMIN 共享，★ 標記者僅 SUPER_ADMIN）──
('DASHBOARD_VIEW',      'DASHBOARD',       'VIEW',       N'※後台與平台總覽儀表板(基本管理員門檻)※'),
('USER_VIEW',           'USER',            'VIEW',       N'查看使用者列表/詳情'),
('USER_EDIT',           'USER',            'EDIT',       N'編輯使用者資料（停權、解鎖）'),
('USER_SESSION_MANAGE', 'USER_SESSION',    'MANAGE',     N'管理使用者裝置 Session（強制登出）'),
('ORGANIZER_VIEW',      'ORGANIZER',       'VIEW',       N'查看主辦方列表'),
('ORGANIZER_KYC_REVIEW','ORGANIZER',       'KYC_REVIEW', N'審核 KYC'),
('CONTRACT_VIEW',       'CONTRACT',        'VIEW',       N'查看合約列表'),
('CONTRACT_CREATE',     'CONTRACT',        'CREATE',     N'建立客製合約'),
('CONTRACT_EDIT',       'CONTRACT',        'EDIT',       N'編輯合約狀態'),
('SETTLEMENT_VIEW',     'SETTLEMENT',      'VIEW',       N'查看結算單'),
('SETTLEMENT_MANAGE',   'SETTLEMENT',      'MANAGE',     N'結算單狀態流轉及上傳匯款證明'),
('INVOICE_MANAGE',      'INVOICE',         'MANAGE',     N'管理發票'),
('ROLE_VIEW',           'ROLE',            'VIEW',       N'查看角色列表'),
('ROLE_MANAGE',         'ROLE',            'MANAGE',     N'設定角色權限 ★ SUPER_ADMIN'),
('RESOURCE_MANAGE',     'SYSTEM_RESOURCE', 'MANAGE',     N'UI 資源與選單管理 ★ SUPER_ADMIN'),
('SYSTEM_CONFIG',       'SYSTEM',          'CONFIG',     N'系統參數設定 ★ SUPER_ADMIN'),
('DICT_MANAGE',         'DICTIONARY',      'MANAGE',     N'字典管理'),
('FEATURE_MANAGE',      'SAAS_FEATURE',    'MANAGE',     N'功能開關管理'),
('PLAN_MANAGE',         'MEMBERSHIP_PLAN', 'MANAGE',     N'會員方案管理'),
('ANNOUNCEMENT_MANAGE', 'ANNOUNCEMENT',    'MANAGE',     N'系統公告管理'),
('MEDIA_MANAGE',        'MEDIA',           'MANAGE',     N'媒體檔案管理'),
('TEMPLATE_MANAGE',     'NOTIFICATION',    'MANAGE',     N'通知範本管理'),
('NOTIF_LOG_VIEW',      'NOTIFICATION_LOG','VIEW',       N'通知發送紀錄查看'),
('JOB_MANAGE',          'SCHEDULED_JOB',   'MANAGE',     N'排程任務管理'),
('SUBMISSION_VIEW',     'SUBMISSION',      'VIEW',       N'表單回覆查看'),
('REFUND_REVIEW',       'REFUND',          'REVIEW',     N'退票審核'),
('AUDIT_VIEW',          'AUDIT_LOG',       'VIEW',       N'稽核日誌查看'),
-- ── Admin 監看區──
('ORDER_VIEW',          'ORDER',           'VIEW',       N'檢視訂單列表與購票紀錄'),
('LOCATION_VIEW',       'LOCATION',        'VIEW',       N'檢視活動場地列表與詳情'),
('PROMOTION_VIEW',      'PROMOTION',       'VIEW',       N'檢視促銷活動與優惠代碼'),
-- ── B2C 用戶（BUYER 角色）──
('PROFILE_VIEW',        'PROFILE',         'VIEW',       N'查看自己的個人資料'),
('PROFILE_EDIT',        'PROFILE',         'EDIT',       N'編輯自己的個人資料'),
-- ── B2B 廠商主（ORGANIZER 角色）──
('ORG_PROFILE_VIEW',    'ORG_PROFILE',     'VIEW',       N'檢視組織基本資料'),
('ORG_PROFILE_EDIT',    'ORG_PROFILE',     'EDIT',       N'編輯組織基本資料'),
('ORG_CONTRACT_VIEW',   'ORG_CONTRACT',    'VIEW',       N'檢視組織合約與協議'),
('ORG_SUBSCRIPTION_VIEW','ORG_SUBSCRIPTION','VIEW',      N'檢視訂閱方案與額度'),
('ORG_MEMBER_MANAGE',   'ORG_MEMBER',      'MANAGE',     N'管理組織團隊成員'),
('ORG_SETTLEMENT_VIEW', 'ORG_SETTLEMENT',  'VIEW',       N'檢視帳務結算紀錄'),
('ORG_DATA_VIEW',       'ORG_DATA',        'VIEW',       N'檢視活動營運數據與報表'),
('ORG_SCAN_TICKET',     'ORG_TICKET',      'SCAN',       N'執行現場驗票與登錄'),
-- ── 組員功能（由組員實作，permission_id 已預先定義供對接）──
('EVENT_VIEW',          'EVENT',           'VIEW',       N'檢視活動列表與詳情'),
('EVENT_CREATE',        'EVENT',           'CREATE',     N'新增與發起新活動'),
('EVENT_EDIT',          'EVENT',           'EDIT',       N'編輯與修改活動資訊'),
('EVENT_PUBLISH',       'EVENT',           'PUBLISH',    N'發布、上架或下架活動'),
('TICKET_TYPE_VIEW',    'TICKET_TYPE',     'VIEW',       N'檢視票券種類與設定'),
('TICKET_TYPE_CREATE',  'TICKET_TYPE',     'CREATE',     N'建立與配置活動票種'),
('TICKET_TYPE_EDIT',    'TICKET_TYPE',     'EDIT',       N'修改與調整活動票種'),
('LOCATION_CREATE',     'LOCATION',        'CREATE',     N'建立與配置活動場地'),
('LOCATION_EDIT',       'LOCATION',        'EDIT',       N'修改與編輯活動場地'),
('PROMOTION_CREATE',    'PROMOTION',       'CREATE',     N'設定促銷方案與優惠券'),
('PROMOTION_EDIT',      'PROMOTION',       'EDIT',       N'修改與編輯促銷優惠方案'),
('MERCH_VIEW',          'MERCH',           'VIEW',       N'檢視週邊商城商品列表'),
('MERCH_CREATE',        'MERCH',           'CREATE',     N'上架與建立週邊商品'),
('MERCH_EDIT',          'MERCH',           'EDIT',       N'修改與編輯週邊商品資訊'),
-- ── 補強（OWN_ORDER_VIEW）──
('OWN_ORDER_VIEW',      'OWN_ORDER',       'VIEW',       N'檢視個人訂單紀錄');

-- 平台角色（organizer_id IS NULL）。
--   系統身分角色（BUYER/ORGANIZER/SUPER_ADMIN）is_editable=0：Admin 後台唯讀，不可改名/刪除/改權限。
--   內部人員角色（ADMIN/CUSTOMER_SERVICE）is_editable=1：可就地編輯權限、可指派給員工。
INSERT INTO [role] (role_id, role_name, description, organizer_id, is_editable) VALUES
('BUYER',            N'一般買家',   N'',                NULL, 0),
('ORGANIZER',        N'廠商主',     N'',            NULL, 0),
('ADMIN',            N'管理員',     N'',  NULL, 1),
('CUSTOMER_SERVICE', N'客服人員',   N'',        NULL, 1),
('SUPER_ADMIN',      N'超級管理員', N'',      NULL, 0);

-- 組織自訂角色（範例示範，預設註解 — 為孤兒 FK，需先有對應 Organizer 才能執行）
--   role_id 由 App 層生成，採「ROL + 7 碼流水號」(如 ROL0000001)，與平台角色語意字串共存於 VARCHAR(30)
--   歸屬哪個組織由 organizer_id 欄位表示，不再塞進 role_id
-- INSERT INTO [role] (role_id, role_name, description, organizer_id, is_editable) VALUES
-- ('ROL0000001', N'Admin',      N'', 'ORG0000001', 0),
-- ('ROL0000002', N'Accountant', N'',     'ORG0000001', 1);

INSERT INTO [role_permission] (role_id, permission_id) VALUES
('BUYER', 'PROFILE_VIEW'),
('BUYER', 'PROFILE_EDIT'),
('BUYER', 'OWN_ORDER_VIEW');

INSERT INTO [role_permission] (role_id, permission_id) VALUES
('ORGANIZER', 'ORG_PROFILE_VIEW'),
('ORGANIZER', 'ORG_PROFILE_EDIT'),
('ORGANIZER', 'ORG_CONTRACT_VIEW'),
('ORGANIZER', 'ORG_SUBSCRIPTION_VIEW'),
('ORGANIZER', 'ORG_MEMBER_MANAGE'),
('ORGANIZER', 'ORG_SETTLEMENT_VIEW'),
('ORGANIZER', 'ORG_DATA_VIEW'),
('ORGANIZER', 'ORG_SCAN_TICKET'),
('ORGANIZER', 'EVENT_VIEW'),
('ORGANIZER', 'EVENT_CREATE'),
('ORGANIZER', 'EVENT_EDIT'),
('ORGANIZER', 'EVENT_PUBLISH'),
('ORGANIZER', 'TICKET_TYPE_VIEW'),
('ORGANIZER', 'TICKET_TYPE_CREATE'),
('ORGANIZER', 'TICKET_TYPE_EDIT'),
('ORGANIZER', 'LOCATION_VIEW'),
('ORGANIZER', 'LOCATION_CREATE'),
('ORGANIZER', 'LOCATION_EDIT'),
('ORGANIZER', 'PROMOTION_VIEW'),
('ORGANIZER', 'PROMOTION_CREATE'),
('ORGANIZER', 'PROMOTION_EDIT'),
('ORGANIZER', 'ORDER_VIEW'),
('ORGANIZER', 'MERCH_VIEW'),
('ORGANIZER', 'MERCH_CREATE'),
('ORGANIZER', 'MERCH_EDIT');

INSERT INTO [role_permission] (role_id, permission_id) VALUES
('ADMIN', 'DASHBOARD_VIEW'),
('ADMIN', 'USER_VIEW'),
('ADMIN', 'USER_EDIT'),
('ADMIN', 'USER_SESSION_MANAGE'),
('ADMIN', 'ORGANIZER_VIEW'),
('ADMIN', 'ORGANIZER_KYC_REVIEW'),
('ADMIN', 'CONTRACT_VIEW'),
('ADMIN', 'CONTRACT_CREATE'),
('ADMIN', 'CONTRACT_EDIT'),
('ADMIN', 'SETTLEMENT_VIEW'),
('ADMIN', 'SETTLEMENT_MANAGE'),
('ADMIN', 'INVOICE_MANAGE'),
('ADMIN', 'ROLE_VIEW'),
('ADMIN', 'DICT_MANAGE'),
('ADMIN', 'FEATURE_MANAGE'),
('ADMIN', 'PLAN_MANAGE'),
('ADMIN', 'ANNOUNCEMENT_MANAGE'),
('ADMIN', 'MEDIA_MANAGE'),
('ADMIN', 'TEMPLATE_MANAGE'),
('ADMIN', 'NOTIF_LOG_VIEW'),
('ADMIN', 'JOB_MANAGE'),
('ADMIN', 'SUBMISSION_VIEW'),
('ADMIN', 'REFUND_REVIEW'),
('ADMIN', 'AUDIT_VIEW'),
('ADMIN', 'ORDER_VIEW'),
('ADMIN', 'LOCATION_VIEW'),
('ADMIN', 'PROMOTION_VIEW'),
('ADMIN', 'EVENT_VIEW');

INSERT INTO [role_permission] (role_id, permission_id) VALUES
('SUPER_ADMIN', 'DASHBOARD_VIEW'),
('SUPER_ADMIN', 'USER_VIEW'),
('SUPER_ADMIN', 'USER_EDIT'),
('SUPER_ADMIN', 'USER_SESSION_MANAGE'),
('SUPER_ADMIN', 'ORGANIZER_VIEW'),
('SUPER_ADMIN', 'ORGANIZER_KYC_REVIEW'),
('SUPER_ADMIN', 'CONTRACT_VIEW'),
('SUPER_ADMIN', 'CONTRACT_CREATE'),
('SUPER_ADMIN', 'CONTRACT_EDIT'),
('SUPER_ADMIN', 'SETTLEMENT_VIEW'),
('SUPER_ADMIN', 'SETTLEMENT_MANAGE'),
('SUPER_ADMIN', 'INVOICE_MANAGE'),
('SUPER_ADMIN', 'ROLE_VIEW'),
('SUPER_ADMIN', 'DICT_MANAGE'),
('SUPER_ADMIN', 'FEATURE_MANAGE'),
('SUPER_ADMIN', 'PLAN_MANAGE'),
('SUPER_ADMIN', 'ANNOUNCEMENT_MANAGE'),
('SUPER_ADMIN', 'MEDIA_MANAGE'),
('SUPER_ADMIN', 'TEMPLATE_MANAGE'),
('SUPER_ADMIN', 'NOTIF_LOG_VIEW'),
('SUPER_ADMIN', 'JOB_MANAGE'),
('SUPER_ADMIN', 'SUBMISSION_VIEW'),
('SUPER_ADMIN', 'REFUND_REVIEW'),
('SUPER_ADMIN', 'AUDIT_VIEW'),
('SUPER_ADMIN', 'ORDER_VIEW'),
('SUPER_ADMIN', 'LOCATION_VIEW'),
('SUPER_ADMIN', 'PROMOTION_VIEW'),
('SUPER_ADMIN', 'ROLE_MANAGE'),
('SUPER_ADMIN', 'RESOURCE_MANAGE'),
('SUPER_ADMIN', 'SYSTEM_CONFIG'),
('SUPER_ADMIN', 'EVENT_VIEW');

-- CUSTOMER_SERVICE：客服日常（訂單/客訴/使用者檢視 + 退款審核）
INSERT INTO [role_permission] (role_id, permission_id) VALUES
('CUSTOMER_SERVICE', 'DASHBOARD_VIEW'),
('CUSTOMER_SERVICE', 'USER_VIEW'),
('CUSTOMER_SERVICE', 'ORDER_VIEW'),
('CUSTOMER_SERVICE', 'SUBMISSION_VIEW'),
('CUSTOMER_SERVICE', 'REFUND_REVIEW');

-- 組織角色模板主表（藍圖：新組織建立時依此生成組織角色；複製當下、之後互不影響）
INSERT INTO [role_template] (template_id, template_name, description) VALUES
('DEFAULT_ORG_ADMIN',      N'組織管理員', N'組織管理員預設權限：活動/票務/商品/財務檢視與操作'),
('DEFAULT_ORG_ACCOUNTANT', N'組織會計',   N'財務相關唯讀：結算紀錄、營運數據、訂單檢視'),
('DEFAULT_ORG_SCANNER',    N'組織驗票員', N'現場驗票預設權限：執行現場驗票與登錄'),
('DEFAULT_ORG_CS',         N'組織客服',   N'客服日常預設權限：活動與訂單檢視、執行現場驗票與售後處理');

-- DEFAULT_ORG_ADMIN：組織管理員預設權限（25 筆）
INSERT INTO [role_permission_template] (template_id, permission_id) VALUES
('DEFAULT_ORG_ADMIN', 'ORG_PROFILE_VIEW'),
('DEFAULT_ORG_ADMIN', 'ORG_PROFILE_EDIT'),
('DEFAULT_ORG_ADMIN', 'ORG_CONTRACT_VIEW'),
('DEFAULT_ORG_ADMIN', 'ORG_SUBSCRIPTION_VIEW'),
('DEFAULT_ORG_ADMIN', 'ORG_MEMBER_MANAGE'),
('DEFAULT_ORG_ADMIN', 'ORG_SETTLEMENT_VIEW'),
('DEFAULT_ORG_ADMIN', 'ORG_DATA_VIEW'),
('DEFAULT_ORG_ADMIN', 'ORG_SCAN_TICKET'),
('DEFAULT_ORG_ADMIN', 'EVENT_VIEW'),
('DEFAULT_ORG_ADMIN', 'EVENT_CREATE'),
('DEFAULT_ORG_ADMIN', 'EVENT_EDIT'),
('DEFAULT_ORG_ADMIN', 'EVENT_PUBLISH'),
('DEFAULT_ORG_ADMIN', 'TICKET_TYPE_VIEW'),
('DEFAULT_ORG_ADMIN', 'TICKET_TYPE_CREATE'),
('DEFAULT_ORG_ADMIN', 'TICKET_TYPE_EDIT'),
('DEFAULT_ORG_ADMIN', 'LOCATION_VIEW'),
('DEFAULT_ORG_ADMIN', 'LOCATION_CREATE'),
('DEFAULT_ORG_ADMIN', 'LOCATION_EDIT'),
('DEFAULT_ORG_ADMIN', 'PROMOTION_VIEW'),
('DEFAULT_ORG_ADMIN', 'PROMOTION_CREATE'),
('DEFAULT_ORG_ADMIN', 'PROMOTION_EDIT'),
('DEFAULT_ORG_ADMIN', 'ORDER_VIEW'),
('DEFAULT_ORG_ADMIN', 'MERCH_VIEW'),
('DEFAULT_ORG_ADMIN', 'MERCH_CREATE'),
('DEFAULT_ORG_ADMIN', 'MERCH_EDIT');

-- DEFAULT_ORG_ACCOUNTANT：財務相關唯讀（3 筆）
INSERT INTO [role_permission_template] (template_id, permission_id) VALUES
('DEFAULT_ORG_ACCOUNTANT', 'ORG_SETTLEMENT_VIEW'),
('DEFAULT_ORG_ACCOUNTANT', 'ORG_DATA_VIEW'),
('DEFAULT_ORG_ACCOUNTANT', 'ORDER_VIEW');

-- DEFAULT_ORG_SCANNER：現場驗票預設權限（1 筆）
INSERT INTO [role_permission_template] (template_id, permission_id) VALUES
('DEFAULT_ORG_SCANNER', 'ORG_SCAN_TICKET');

-- DEFAULT_ORG_CS：客服日常預設權限（5 筆）
INSERT INTO [role_permission_template] (template_id, permission_id) VALUES
('DEFAULT_ORG_CS', 'EVENT_VIEW'),
('DEFAULT_ORG_CS', 'TICKET_TYPE_VIEW'),
('DEFAULT_ORG_CS', 'ORDER_VIEW'),
('DEFAULT_ORG_CS', 'MERCH_VIEW'),
('DEFAULT_ORG_CS', 'ORG_SCAN_TICKET');

-- [Jason] Phase 1 種子歸正：
--   1) 修正原欄位清單壞損（c8debd2 誤把 url_path, permission_id 寫成 u n_id，導致 7 欄對 8 值、DB 初始化失敗）。
--   2) 加 icon / is_visible 兩欄（配合 DB 驅動選單；BUTTON 級 is_visible=0 不進側欄）。
--   3) ADMIN_LOCAL 重寫為對齊 frontend-admin 實際 12 頁路由的 2 層樹。
--   4) 封存 B2C_FRONT（公開賣場、買家無角色分野，會員頁靠 isLoggedIn 控制而非 RBAC，不套選單樹）。
-- B2C_FRONT 封存（保留供查考）：
-- ('B2C_PROFILE', 'B2C_FRONT', NULL, 1, N'個人資料', '/profile', 'PROFILE_VIEW', 1, NULL, 1)
-- ('B2C_PROFILE_EDIT', 'B2C_FRONT', 'B2C_PROFILE', 2, N'編輯個人資料', NULL, 'PROFILE_EDIT', 1, NULL, 0)
-- ('B2C_MY_ORDERS', 'B2C_FRONT', NULL, 1, N'我的訂單', '/orders', 'OWN_ORDER_VIEW', 2, NULL, 1)
INSERT INTO [system_resource]
    (resource_id, portal_type, parent_id, resource_type, name, url_path, permission_id, sort_order, icon, is_visible)
VALUES
-- B2B_PORTAL（畫面層級側欄；is_visible=0 的細項節點＝RBAC 登記用、不進側欄，由畫面內頁籤呈現；路徑對齊 frontend router）
-- 🎫 活動管理
('B2B_OPERATION',      'B2B_PORTAL', NULL,                0, N'活動管理',     NULL,                                       NULL,                    0, 'bi-easel',                  1),
('B2B_EVENT',          'B2B_PORTAL', 'B2B_OPERATION',     1, N'活動管理',     '/org/:organizerId/themes',                 'EVENT_VIEW',            1, 'bi-calendar-event',         1),
('B2B_TICKET',         'B2B_PORTAL', 'B2B_OPERATION',     1, N'票務訂單',     '/org/:organizerId/ticket',                 'ORDER_VIEW',            2, 'bi-ticket-perforated',      1),
('B2B_MERCH',          'B2B_PORTAL', 'B2B_OPERATION',     1, N'商城訂單',     '/org/:organizerId/merch',                  'MERCH_VIEW',            3, 'bi-bag',                    1),
-- [整合 2026-07-04] 對齊 Mary 的商品頁路由：/admin/products=商品列表(檢視權)、/admin/product-add=上架表單(建立權)
('B2B_PRODUCTS',       'B2B_PORTAL', 'B2B_OPERATION',     1, N'周邊商城',     '/org/:organizerId/admin/products',         'MERCH_VIEW',            4, 'bi-shop',                   1),
('B2B_PRODUCT_ADD',    'B2B_PORTAL', 'B2B_OPERATION',     1, N'商品上架',     '/org/:organizerId/admin/product-add',      'MERCH_CREATE',          5, 'bi-box-seam',               1),
-- 💰 財務與售後
('B2B_FINANCE',        'B2B_PORTAL', NULL,                0, N'財務與售後',   NULL,                                       NULL,                    1, 'bi-wallet2',                1),
-- [Jason] /checkin 為「票務核銷與售後」管理頁，改掛管理權 ORDER_VIEW（原 ORG_SCAN_TICKET 與 /scan 同權，
--          會讓只有掃碼權的 Scanner 同時看到此管理頁）。Owner/Admin 本就有 ORDER_VIEW（DEFAULT_ORG_ADMIN）。
('B2B_CHECKIN',        'B2B_PORTAL', 'B2B_FINANCE',       1, N'票務售後', '/org/:organizerId/checkin',              'ORDER_VIEW',            1, 'bi-clipboard-check',        1),
('B2B_SCAN',           'B2B_PORTAL', 'B2B_FINANCE',       1, N'票務核銷', '/org/:organizerId/scan',                   'ORG_SCAN_TICKET',       2, 'bi-qr-code-scan',           1),
('B2B_MERCH_RETURNED', 'B2B_PORTAL', 'B2B_FINANCE',       1, N'商品退貨與售後', '/org/:organizerId/merch-returned',       'MERCH_VIEW',            3, 'bi-arrow-counterclockwise', 1),
('B2B_SETTLEMENT',     'B2B_PORTAL', 'B2B_FINANCE',       1, N'帳務結算',     '/org/:organizerId/settlement',             'ORG_SETTLEMENT_VIEW',   4, 'bi-cash-stack',             1),
-- [Jason] 退款申請審核（退票/退貨審核制）：會員申請附理由，主辦方在此核准/駁回；權限沿用 ORDER_VIEW 不新增權限碼
('B2B_REFUND_REQUESTS','B2B_PORTAL', 'B2B_FINANCE',       1, N'退款申請審核', '/org/:organizerId/refund-requests',        'ORDER_VIEW',            5, 'bi-inbox',                  1),
-- 🏢 組織管理（團隊管理/組織設定為畫面層級入口，進去用頁籤切子頁；子頁節點 is_visible=0 僅供 RBAC 登記）
('B2B_ORG',            'B2B_PORTAL', NULL,                0, N'組織管理',     NULL,                                       NULL,                    2, 'bi-building',               1),
('B2B_TEAM',           'B2B_PORTAL', 'B2B_ORG',           1, N'團隊管理',     '/org/:organizerId/team',                   'ORG_MEMBER_MANAGE',     1, 'bi-people',                 1),
('B2B_MEMBERS',        'B2B_PORTAL', 'B2B_TEAM',          1, N'團隊成員',     '/org/:organizerId/team/members',           'ORG_MEMBER_MANAGE',     1, 'bi-person-lines-fill',      0),
('B2B_ROLES',          'B2B_PORTAL', 'B2B_TEAM',          1, N'角色權限',     '/org/:organizerId/team/roles',             'ORG_MEMBER_MANAGE',     2, 'bi-shield-lock',            0),
('B2B_SETTINGS_HOME',  'B2B_PORTAL', 'B2B_ORG',           1, N'組織設定',     '/org/:organizerId/settings',               'ORG_PROFILE_VIEW',      2, 'bi-gear',                   1),
('B2B_ORG_PROFILE',    'B2B_PORTAL', 'B2B_SETTINGS_HOME', 1, N'廠商資料',     '/org/:organizerId/settings/profile',       'ORG_PROFILE_VIEW',      1, 'bi-building',               0),
('B2B_ORG_EDIT',       'B2B_PORTAL', 'B2B_ORG_PROFILE',   2, N'編輯廠商資料', NULL,                                       'ORG_PROFILE_EDIT',      1, NULL,                        0),
('B2B_CONTRACT',       'B2B_PORTAL', 'B2B_SETTINGS_HOME', 1, N'我的合約',     '/org/:organizerId/settings/contracts',     'ORG_CONTRACT_VIEW',     2, 'bi-file-earmark-text',      0),
('B2B_SUBSCRIPTION',   'B2B_PORTAL', 'B2B_SETTINGS_HOME', 1, N'訂閱方案',     '/org/:organizerId/settings/subscription',  'ORG_SUBSCRIPTION_VIEW', 3, 'bi-box-seam',               0),
-- ADMIN_LOCAL（定案 IA：3 靜態群組 / 11 PAGE 入口 / 頁籤化；type 0=MENU 1=PAGE。
--   群組 MENU(url=NULL,permission=NULL)：靠後端 isNotEmptyGroup 過濾，子項全無權則整群隱藏。
--   11 入口 is_visible=1 進側欄＝各領域群首頁；其下「頁籤子頁」is_visible=0 不進側欄、僅供 RBAC 登記，
--   側欄渲染時 PAGE 入口因可見子項被過濾而 children 為空→渲染成連結。對齊 frontend-admin/src/router/index.js。)
-- ── 群組 1 營運管理 Operations ──
('GRP_ADM_OPS',         'ADMIN_LOCAL', NULL,           0, N'營運管理',     NULL,                          NULL,                   1, 'bi-clipboard-data',    1),
('ADM_DASHBOARD',       'ADMIN_LOCAL', 'GRP_ADM_OPS',  1, N'平台總覽',     '/admin/dashboard',            NULL,                   1, 'bi-speedometer2',      1),
('ADM_USERS',           'ADMIN_LOCAL', 'GRP_ADM_OPS',  1, N'使用者管理',   '/admin/users',                'USER_VIEW',            2, 'bi-people',            1),
('ADM_ORGANIZERS',      'ADMIN_LOCAL', 'GRP_ADM_OPS',  1, N'組織管理',     '/admin/organizers',           'ORGANIZER_VIEW',       3, 'bi-building',          1),
('ADM_KYC',             'ADMIN_LOCAL', 'ADM_ORGANIZERS', 1, N'KYC 審核',   '/admin/organizers/kyc',       'ORGANIZER_KYC_REVIEW', 1, 'bi-patch-check',       0),
('ADM_OPERATIONS',      'ADMIN_LOCAL', 'GRP_ADM_OPS',  1, N'客服與查詢', '/admin/operations/submissions', 'SUBMISSION_VIEW',    4, 'bi-headset',           1),
('ADM_EVENTS',          'ADMIN_LOCAL', 'ADM_OPERATIONS', 1, N'活動查詢',   '/admin/operations/events',    NULL,                   1, 'bi-calendar-event',    0),
('ADM_ORDERS',          'ADMIN_LOCAL', 'ADM_OPERATIONS', 1, N'訂單查詢',   '/admin/operations/orders',    'ORDER_VIEW',           2, 'bi-receipt',           0),
('ADM_VENUES',          'ADMIN_LOCAL', 'GRP_ADM_OPS',  1, N'場地模板',     '/admin/venues',               'LOCATION_VIEW',        5, 'bi-grid-3x3',          1),
-- ── 群組 2 商務財務 Business & Finance ──
('GRP_ADM_BIZ',         'ADMIN_LOCAL', NULL,           0, N'商務財務',     NULL,                          NULL,                   2, 'bi-cash-coin',         1),
('ADM_FINANCE',         'ADMIN_LOCAL', 'GRP_ADM_BIZ',  1, N'財務管理',     '/admin/finance',              'SETTLEMENT_VIEW',      1, 'bi-cash-stack',        1),
('ADM_FIN_SETTLE',      'ADMIN_LOCAL', 'ADM_FINANCE',  1, N'費用結算',     '/admin/finance/settlement',   'SETTLEMENT_VIEW',      1, 'bi-calculator',        0),
('ADM_SUBSCRIPTIONS',   'ADMIN_LOCAL', 'GRP_ADM_BIZ',  1, N'訂閱與合約',   '/admin/billing/subscriptions','PLAN_MANAGE',          2, 'bi-box-seam',          1),
('ADM_PLANS',           'ADMIN_LOCAL', 'ADM_SUBSCRIPTIONS', 1, N'方案目錄', '/admin/billing/plans',       'PLAN_MANAGE',          1, 'bi-grid',              0),
('ADM_CONTRACTS',       'ADMIN_LOCAL', 'ADM_SUBSCRIPTIONS', 1, N'合約與範本', '/admin/billing/contracts', 'CONTRACT_VIEW',        2, 'bi-file-earmark-text', 0),
-- ── 群組 3 系統權限 System ──
('GRP_ADM_SYS',         'ADMIN_LOCAL', NULL,           0, N'系統權限',     NULL,                          NULL,                   3, 'bi-shield-lock',       1),
('ADM_RBAC',            'ADMIN_LOCAL', 'GRP_ADM_SYS',  1, N'權限管理',     '/admin/rbac',                 'ROLE_VIEW',            1, 'bi-person-badge',      1),
('ADM_RBAC_STAFF',      'ADMIN_LOCAL', 'ADM_RBAC',     1, N'內部人員',     '/admin/rbac/staff',           'ROLE_VIEW',            1, 'bi-person-vcard',      0),
('ADM_RBAC_TMPL',       'ADMIN_LOCAL', 'ADM_RBAC',     1, N'組織角色模板', '/admin/rbac/templates',       'ROLE_MANAGE',          2, 'bi-diagram-2',         0),
('ADM_RBAC_RES',        'ADMIN_LOCAL', 'ADM_RBAC',     1, N'資源與選單',   '/admin/rbac/resources',       'RESOURCE_MANAGE',      3, 'bi-diagram-3',         0),
('ADM_SYS_ANNOUNCE',    'ADMIN_LOCAL', 'GRP_ADM_SYS',  1, N'通知與公告',   '/admin/system/announcements', 'ANNOUNCEMENT_MANAGE',  2, 'bi-megaphone',         1),
('ADM_SYS_TMPL',        'ADMIN_LOCAL', 'ADM_SYS_ANNOUNCE', 1, N'通知範本', '/admin/system/templates',     'TEMPLATE_MANAGE',      1, 'bi-envelope-paper',    0),
('ADM_SYS_NOTIF',       'ADMIN_LOCAL', 'ADM_SYS_ANNOUNCE', 1, N'發送日誌', '/admin/system/notifications', 'NOTIF_LOG_VIEW',       2, 'bi-bell',              0),
('ADM_SYS_JOBS',        'ADMIN_LOCAL', 'GRP_ADM_SYS',  1, N'系統維護',     '/admin/system/jobs',          'JOB_MANAGE',           3, 'bi-tools',             1),
('ADM_SYS_DICT',        'ADMIN_LOCAL', 'ADM_SYS_JOBS', 1, N'資料字典',     '/admin/system/dictionaries',  'DICT_MANAGE',          1, 'bi-book',              0),
('ADM_SYS_CONFIG',      'ADMIN_LOCAL', 'ADM_SYS_JOBS', 1, N'系統參數',     '/admin/system/config',        'SYSTEM_CONFIG',        2, 'bi-sliders',           0),
('ADM_SYS_MEDIA',       'ADMIN_LOCAL', 'ADM_SYS_JOBS', 1, N'檔案管理',     '/admin/system/media',         'MEDIA_MANAGE',         3, 'bi-images',            0),
('ADM_SYS_AUDIT',       'ADMIN_LOCAL', 'GRP_ADM_SYS',  1, N'稽核日誌',     '/admin/system/audit',         'AUDIT_VIEW',           4, 'bi-clipboard-check',   1);

INSERT INTO [saas_feature] (feature_id, description) VALUES
('EVENT_PUBLISH',      N'活動上架'),
('BASIC_ANALYTICS',    N'基本數據報表'),
('MERCH_STORE',        N'週邊商城'),
('PROMO_CODE',         N'促銷代碼'),
('EVENT_TOP',          N'活動置頂'),
('ADVANCED_ANALYTICS', N'進階數據分析'),
('DEDICATED_SUPPORT',  N'專員客服'),
('CUSTOM_CONTRACT',    N'客製合約簽訂');

INSERT INTO [notification_template] (template_id, template_code, channel, subject, body_template, is_active) VALUES
('TMPL_EMAIL_VERIFY',   'EMAIL_VERIFICATION', 0,
    N'[平台] 請驗證您的信箱',
    N'親愛的 {{name}}，請點擊以下連結驗證您的信箱：{{verify_url}}（連結 24 小時內有效）', 1),
('TMPL_PWD_RESET',      'PASSWORD_RESET',     0,
    N'[平台] 密碼重設請求',
    N'親愛的 {{name}}，請點擊以下連結重設您的密碼：{{reset_url}}（連結 30 分鐘內有效）', 1),
('TMPL_KYC_APPROVED',   'KYC_APPROVED',       0,
    N'[平台] 您的主辦方身份審核已通過',
    N'親愛的 {{name}}，恭喜您的主辦方身份審核已通過，您現在可以發布活動。', 1),
('TMPL_KYC_REJECTED',   'KYC_REJECTED',       0,
    N'[平台] 您的主辦方身份審核未通過',
    N'親愛的 {{name}}，您的審核未通過，原因：{{rejection_reason}}。請修正後重新提交。', 1),
('TMPL_MEMBER_INVITE',  'MEMBER_INVITE',      0,
    N'[平台] 您被邀請加入 {{org_name}}',
    N'親愛的 {{name}}，{{inviter_name}} 邀請您加入 {{org_name}}。請點擊連結確認邀請：{{invite_url}}（3 天內有效）', 1),
('TMPL_CONTRACT_READY', 'CONTRACT_READY',     0,
    N'[平台] 您有一份合約待簽署',
    N'親愛的 {{name}}，您有一份新合約待簽署，請登入後台查看。', 1);

INSERT INTO [scheduled_job] (job_id, job_code, description, status_code) VALUES
('JOB_EXPIRE_SUBS',      'EXPIRE_SUBSCRIPTIONS', N'定期將 end_date 已過期的 organizer_subscription 標記為 EXPIRED', 0),
('JOB_EXPIRE_CONTRACTS', 'EXPIRE_CONTRACTS',     N'定期將 valid_to 已到期的 contract 標記為 EXPIRED',             0),
('JOB_CUMULATIVE_UPG',   'CUMULATIVE_UPGRADE',   N'定期計算廠商累積銷售金額，達門檻自動升級訂閱方案',               0),
('JOB_PURGE_ATTENDEE',   'PURGE_ATTENDEE_DATA',  N'個資定期銷毀：清除 Ticket_Attendees 的敏感個資欄位',             0);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('GENDER', 'M', N'男',   1),
('GENDER', 'F', N'女',   2),
('GENDER', 'O', N'其他', 3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('KYC_STATUS', 'PENDING',  N'待審核', 1),
('KYC_STATUS', 'APPROVED', N'已通過', 2),
('KYC_STATUS', 'REJECTED', N'已退件', 3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('AUTH_PROVIDER',        '0', N'本地帳號',     1),
('AUTH_PROVIDER',        '1', N'Google',      2);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('ORG_STATUS',           '0', N'營運中',       1),
('ORG_STATUS',           '1', N'已停權',       2),
('ORG_STATUS',           '2', N'已封存',       3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('MEMBER_STATUS',        '0', N'待確認',       1),
('MEMBER_STATUS',        '1', N'已加入',       2),
('MEMBER_STATUS',        '2', N'已拒絕',       3),
('MEMBER_STATUS',        '3', N'已撤銷',       4);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('CONTRACT_STATUS',      '0', N'草稿',         1),
('CONTRACT_STATUS',      '1', N'生效中',       2),
('CONTRACT_STATUS',      '2', N'已終止',       3),
('CONTRACT_STATUS',      '3', N'已到期',       4);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('CONTRACT_TYPE',        '0', N'免費標準',     1),
('CONTRACT_TYPE',        '1', N'年費制',       2),
('CONTRACT_TYPE',        '2', N'客製合約',     3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('FEE_TYPE',             '0', N'抽成百分比',   1),
('FEE_TYPE',             '1', N'每票固定金額', 2);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('SUBSCRIPTION_STATUS',  '0', N'生效中',       1),
('SUBSCRIPTION_STATUS',  '1', N'已到期',       2);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('UPGRADE_TYPE',         '0', N'年費制',       1),
('UPGRADE_TYPE',         '1', N'累積銷售',     2),
('UPGRADE_TYPE',         '2', N'人工指定',     3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('NOTIFICATION_CHANNEL', '0', N'Email',        1),
('NOTIFICATION_CHANNEL', '1', N'簡訊',         2);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('NOTIFICATION_STATUS',  '0', N'待發送',       1),
('NOTIFICATION_STATUS',  '1', N'已發送',       2),
('NOTIFICATION_STATUS',  '2', N'發送失敗',     3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('JOB_STATUS',           '0', N'閒置',         1),
('JOB_STATUS',           '1', N'執行中',       2),
('JOB_STATUS',           '2', N'成功',         3),
('JOB_STATUS',           '3', N'失敗',         4);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('RESOURCE_TYPE',        '0', N'選單',         1),
('RESOURCE_TYPE',        '1', N'頁面',         2),
('RESOURCE_TYPE',        '2', N'按鈕',         3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('FILE_TYPE',            '0', N'圖片',         1),
('FILE_TYPE',            '1', N'文件',         2);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('FORM_TYPE',            '0', N'聯絡我們',     1),
('FORM_TYPE',            '1', N'意見回饋',     2);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('SUBMISSION_STATUS',    '0', N'未讀',         1),
('SUBMISSION_STATUS',    '1', N'處理中',       2),
('SUBMISSION_STATUS',    '2', N'已解決',       3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('LOGIN_FAILURE_REASON', '0', N'密碼錯誤',     1),
('LOGIN_FAILURE_REASON', '1', N'帳號不存在',   2),
('LOGIN_FAILURE_REASON', '2', N'帳號已鎖定',   3),
('LOGIN_FAILURE_REASON', '3', N'帳號未啟用',   4);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('SESSION_REVOKED_BY',   '0', N'使用者自行',   1),
('SESSION_REVOKED_BY',   '1', N'管理員',       2),
('SESSION_REVOKED_BY',   '2', N'系統自動',     3);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('AUDIT_ACTION_TYPE',    '0', N'認證',         1),
('AUDIT_ACTION_TYPE',    '1', N'身份與權限',   2),
('AUDIT_ACTION_TYPE',    '2', N'組織管理',     3),
('AUDIT_ACTION_TYPE',    '3', N'財務',         4),
('AUDIT_ACTION_TYPE',    '4', N'系統',         5),
('AUDIT_ACTION_TYPE',    '5', N'內容',         6);

INSERT INTO [system_dictionary] (dict_type, dict_code, dict_label, sort_order) VALUES
('PORTAL_TYPE',          'B2C_FRONT',   N'前台（買家）',  1),
('PORTAL_TYPE',          'B2B_PORTAL',  N'廠商後台',      2),
('PORTAL_TYPE',          'ADMIN_LOCAL', N'管理後台',      3);

INSERT INTO [system_config] (config_id, config_key, config_value, description) VALUES
('CFG_LOGIN_MAX_FAIL',  'LOGIN_FAIL_MAX_ATTEMPTS',     '5',  N'連續登入失敗達此次數時鎖定帳號'),
('CFG_LOGIN_LOCK_MIN',  'LOGIN_LOCK_DURATION_MINUTES', '15', N'帳號鎖定持續分鐘數'),
('CFG_EMAIL_VERIFY_HR', 'EMAIL_VERIFY_EXPIRE_HOURS',   '24', N'Email 驗證信有效時間（小時）'),
('CFG_PWD_RESET_MIN',   'PWD_RESET_EXPIRE_MINUTES',    '30', N'密碼重設連結有效時間（分鐘）'),
('CFG_INVITE_EXPIRE_D', 'INVITE_TOKEN_EXPIRE_DAYS',    '3',  N'組織成員邀請連結有效天數'),
('CFG_CONTRACT_DEF_TYPE',  'CONTRACT_DEFAULT_FEE_TYPE',  '0',    N'公版合約預設費率類型 (0: 百分比抽成, 1: 每筆固定)'),
('CFG_CONTRACT_DEF_VALUE', 'CONTRACT_DEFAULT_FEE_VALUE', '5.00', N'公版合約預設費率值 (百分比存 0-100; 每筆固定存金額)');


-- ================================================================
-- ████  示範 / 測試假資料（DEMO DATA）  ████
-- ----------------------------------------------------------------
-- 用途：開發與測試用的假資料，涵蓋 17 張業務/異動表。
-- 注意：本區段為「示範資料」，正式上線部署前可整段刪除（上方為系統必要種子）。
-- 載入順序已依 FK 依賴排序；BIGINT IDENTITY 表不指定 PK，交 DB 自動產生。
-- 反向清除：請依與下方相反的順序 DELETE，或先停用 FK 檢查。
-- ================================================================

-- ── [1] [user]（20 筆）── auth_provider 0=LOCAL(需密碼)/1=GOOGLE(無密碼)；gender M/F/O
-- pwd_hash 為示範用假 bcrypt 字串
INSERT INTO [user] (user_id, email, password_hash, name, phone, gender, birth_date, auth_provider, google_oauth_id, email_verified_at, is_active) VALUES
('USR0000001', 'super.admin@tap.com', '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'系統超管',  '0911000001', 'M', '1985-03-12', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000002', 'admin@tap.com',       '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'管理員',    '0911000002', 'F', '1990-07-25', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000003', 'ownerA@demo.com',      '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'陳志明',    '0911000003', 'M', '1988-11-03', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000004', 'ownerB@demo.com',      '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'林雅婷',    '0911000004', 'F', '1992-01-18', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000005', 'ownerC@demo.com',      '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'黃建國',    '0911000005', 'M', '1979-09-30', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000006', 'ownerD@demo.com',      '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'吳美玲',    '0911000006', 'O', '1995-05-05', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000007', 'staff1@demo.com',      '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'張家豪',    '0911000007', 'M', '1998-02-14', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000008', 'staff2@demo.com',      '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'李佳蓉',    '0911000008', 'F', '1997-12-22', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000009', 'buyer1@demo.com',      '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'林小美',    '0911000009', 'F', '2000-06-08', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000010', 'buyer.google@demo.com', NULL,                            N'黃明宏',    '0911000010', 'M', '2001-04-17', 1, 'google-oauth-uid-1001', SYSUTCDATETIME(), 1),
('USR0000011', 'buyer.locked@demo.com','$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'許雅琪',    '0911000011', 'F', '1999-08-19', 0, NULL, NULL, 1),
('USR0000012', 'buyer.inactive@demo.com','$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'劉冠廷',  '0911000012', 'M', '1996-10-11', 0, NULL, SYSUTCDATETIME(), 0);

-- 帳號鎖定示範：USR0000011 鎖定 15 分鐘
UPDATE [user] SET locked_until = DATEADD(MINUTE, 15, SYSUTCDATETIME()) WHERE user_id = 'USR0000011';

INSERT INTO [user] (user_id, email, password_hash, name, phone, gender, birth_date, auth_provider, google_oauth_id, email_verified_at, is_active) VALUES
('USR0000013', 'ownerE@demo.com',        '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'趙文瑄',   '0911000013', 'M', '1987-04-22', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000014', 'ownerF@demo.com',        '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'周怡君',   '0911000014', 'F', '1991-08-15', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000015', 'ownerG@demo.com',         NULL,                            N'鄭凱文',   '0911000015', 'M', '1993-12-01', 1, 'google-oauth-uid-1002', SYSUTCDATETIME(), 1),
('USR0000016', 'staff3@demo.com',        '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'劉佩珊',   '0911000016', 'F', '1999-03-28', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000017', 'staff4@demo.com',        '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'許志豪',   '0911000017', 'M', '2000-11-09', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000018', 'buyer2@demo.com',        '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'蔡宜蓁',   '0911000018', 'F', '2002-07-03', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000019', 'buyer3@demo.com',        '$2a$10$s3nTD1d2A449JXd6c/IdGe2iMtRsI7yqb7JvuQqPvMPRDJ77eSf1e', N'吳宗翰',   '0911000019', 'M', '1998-01-25', 0, NULL, SYSUTCDATETIME(), 1),
('USR0000020', 'buyer.google2@demo.com',  NULL,                            N'楊雅雯',   '0911000020', 'F', '2003-09-14', 1, 'google-oauth-uid-1003', SYSUTCDATETIME(), 1);

-- 部分使用者已上傳大頭照
UPDATE [user] SET avatar_url = 'https://cdn.demo.com/avatar/USR0000001.png' WHERE user_id = 'USR0000001';
UPDATE [user] SET avatar_url = 'https://cdn.demo.com/avatar/USR0000003.png' WHERE user_id = 'USR0000003';
UPDATE [user] SET avatar_url = 'https://cdn.demo.com/avatar/USR0000004.png' WHERE user_id = 'USR0000004';
UPDATE [user] SET avatar_url = 'https://cdn.demo.com/avatar/USR0000009.png' WHERE user_id = 'USR0000009';
UPDATE [user] SET avatar_url = 'https://cdn.demo.com/avatar/USR0000013.png' WHERE user_id = 'USR0000013';
UPDATE [user] SET avatar_url = 'https://cdn.demo.com/avatar/USR0000014.png' WHERE user_id = 'USR0000014';
UPDATE [user] SET avatar_url = 'https://cdn.demo.com/avatar/USR0000018.png' WHERE user_id = 'USR0000018';

-- ── [2] [organizer]（7 筆）── status 0=ACTIVE/1=SUSPENDED；kyc 0=DRAFT/1=PENDING/2=APPROVED/3=REJECTED
INSERT INTO [organizer] (organizer_id, owner_user_id, name, tax_id, status, kyc_status, kyc_rejection_reason, kyc_reviewed_at, kyc_reviewed_by) VALUES
('ORG0000001', 'USR0000003', N'晴天音樂祭有限公司', '12345678', 0, 2, NULL,                SYSUTCDATETIME(), 'USR0000002'),
('ORG0000002', 'USR0000004', N'藍海展覽股份有限公司', '23456789', 0, 2, NULL,              SYSUTCDATETIME(), 'USR0000002'),
('ORG0000003', 'USR0000005', N'青山體育活動工作室', '34567890', 0, 1, NULL,                NULL,      NULL),
('ORG0000004', 'USR0000006', N'未過審測試主辦',     '45678901', 1, 3, N'營業登記文件不符', SYSUTCDATETIME(), 'USR0000002'),
('ORG0000005', 'USR0000013', N'星光娛樂有限公司',   '56789012', 0, 2, NULL,                SYSUTCDATETIME(), 'USR0000002'),
('ORG0000006', 'USR0000014', N'綠野戶外探索工作室', '67890123', 0, 1, NULL,                NULL,      NULL),
('ORG0000007', 'USR0000015', N'紅磚藝文空間',       '78901234', 0, 0, NULL,                NULL,      NULL);

-- ── [3] [role] 組織自訂角色（6 筆）── organizer_id NOT NULL；role_id 用 ROL+流水號
INSERT INTO [role] (role_id, role_name, description, organizer_id, is_editable) VALUES
('ROL0000001', N'Admin',      N'', 'ORG0000001', 0),
('ROL0000002', N'財務專員',   N'',                    'ORG0000001', 1),
('ROL0000003', N'Admin',      N'',                  'ORG0000002', 0),
('ROL0000004', N'Admin',      N'',                  'ORG0000005', 0),
('ROL0000005', N'Scanner',    N'',                      'ORG0000005', 1),
('ROL0000006', N'Admin',      N'',                  'ORG0000006', 0);

-- 組織自訂角色（ROL*）的權限對應。須置於 [role] 與 [permission] 種子之後（role_permission 兩端 FK）。
-- 內建 Admin 角色（is_editable=0）權限＝DEFAULT_ORG_ADMIN 範本 25 項（與 createOrganization runtime 依範本
-- 複製的結果一致）；Accountant＝DEFAULT_ORG_ACCOUNTANT 範本 3 項；Scanner＝僅現場驗票。
-- 非 Owner 成員的權限來自此表（OrganizerService.getOrgPermissionIds 讀 role.getPermissions()）。
INSERT INTO [role_permission] (role_id, permission_id) VALUES
-- ROL0000001：ORG1 Admin（25）
('ROL0000001', 'ORG_PROFILE_VIEW'),
('ROL0000001', 'ORG_PROFILE_EDIT'),
('ROL0000001', 'ORG_CONTRACT_VIEW'),
('ROL0000001', 'ORG_SUBSCRIPTION_VIEW'),
('ROL0000001', 'ORG_MEMBER_MANAGE'),
('ROL0000001', 'ORG_SETTLEMENT_VIEW'),
('ROL0000001', 'ORG_DATA_VIEW'),
('ROL0000001', 'ORG_SCAN_TICKET'),
('ROL0000001', 'EVENT_VIEW'),
('ROL0000001', 'EVENT_CREATE'),
('ROL0000001', 'EVENT_EDIT'),
('ROL0000001', 'EVENT_PUBLISH'),
('ROL0000001', 'TICKET_TYPE_VIEW'),
('ROL0000001', 'TICKET_TYPE_CREATE'),
('ROL0000001', 'TICKET_TYPE_EDIT'),
('ROL0000001', 'LOCATION_VIEW'),
('ROL0000001', 'LOCATION_CREATE'),
('ROL0000001', 'LOCATION_EDIT'),
('ROL0000001', 'PROMOTION_VIEW'),
('ROL0000001', 'PROMOTION_CREATE'),
('ROL0000001', 'PROMOTION_EDIT'),
('ROL0000001', 'ORDER_VIEW'),
('ROL0000001', 'MERCH_VIEW'),
('ROL0000001', 'MERCH_CREATE'),
('ROL0000001', 'MERCH_EDIT'),
-- ROL0000002：ORG1 Accountant（3）
('ROL0000002', 'ORG_SETTLEMENT_VIEW'),
('ROL0000002', 'ORG_DATA_VIEW'),
('ROL0000002', 'ORDER_VIEW'),
-- ROL0000003：ORG2 Admin（25）
('ROL0000003', 'ORG_PROFILE_VIEW'),
('ROL0000003', 'ORG_PROFILE_EDIT'),
('ROL0000003', 'ORG_CONTRACT_VIEW'),
('ROL0000003', 'ORG_SUBSCRIPTION_VIEW'),
('ROL0000003', 'ORG_MEMBER_MANAGE'),
('ROL0000003', 'ORG_SETTLEMENT_VIEW'),
('ROL0000003', 'ORG_DATA_VIEW'),
('ROL0000003', 'ORG_SCAN_TICKET'),
('ROL0000003', 'EVENT_VIEW'),
('ROL0000003', 'EVENT_CREATE'),
('ROL0000003', 'EVENT_EDIT'),
('ROL0000003', 'EVENT_PUBLISH'),
('ROL0000003', 'TICKET_TYPE_VIEW'),
('ROL0000003', 'TICKET_TYPE_CREATE'),
('ROL0000003', 'TICKET_TYPE_EDIT'),
('ROL0000003', 'LOCATION_VIEW'),
('ROL0000003', 'LOCATION_CREATE'),
('ROL0000003', 'LOCATION_EDIT'),
('ROL0000003', 'PROMOTION_VIEW'),
('ROL0000003', 'PROMOTION_CREATE'),
('ROL0000003', 'PROMOTION_EDIT'),
('ROL0000003', 'ORDER_VIEW'),
('ROL0000003', 'MERCH_VIEW'),
('ROL0000003', 'MERCH_CREATE'),
('ROL0000003', 'MERCH_EDIT'),
-- ROL0000004：ORG5 Admin（25）
('ROL0000004', 'ORG_PROFILE_VIEW'),
('ROL0000004', 'ORG_PROFILE_EDIT'),
('ROL0000004', 'ORG_CONTRACT_VIEW'),
('ROL0000004', 'ORG_SUBSCRIPTION_VIEW'),
('ROL0000004', 'ORG_MEMBER_MANAGE'),
('ROL0000004', 'ORG_SETTLEMENT_VIEW'),
('ROL0000004', 'ORG_DATA_VIEW'),
('ROL0000004', 'ORG_SCAN_TICKET'),
('ROL0000004', 'EVENT_VIEW'),
('ROL0000004', 'EVENT_CREATE'),
('ROL0000004', 'EVENT_EDIT'),
('ROL0000004', 'EVENT_PUBLISH'),
('ROL0000004', 'TICKET_TYPE_VIEW'),
('ROL0000004', 'TICKET_TYPE_CREATE'),
('ROL0000004', 'TICKET_TYPE_EDIT'),
('ROL0000004', 'LOCATION_VIEW'),
('ROL0000004', 'LOCATION_CREATE'),
('ROL0000004', 'LOCATION_EDIT'),
('ROL0000004', 'PROMOTION_VIEW'),
('ROL0000004', 'PROMOTION_CREATE'),
('ROL0000004', 'PROMOTION_EDIT'),
('ROL0000004', 'ORDER_VIEW'),
('ROL0000004', 'MERCH_VIEW'),
('ROL0000004', 'MERCH_CREATE'),
('ROL0000004', 'MERCH_EDIT'),
-- ROL0000005：ORG5 Scanner（1，僅現場驗票）
('ROL0000005', 'ORG_SCAN_TICKET'),
-- ROL0000006：ORG6 Admin（25）
('ROL0000006', 'ORG_PROFILE_VIEW'),
('ROL0000006', 'ORG_PROFILE_EDIT'),
('ROL0000006', 'ORG_CONTRACT_VIEW'),
('ROL0000006', 'ORG_SUBSCRIPTION_VIEW'),
('ROL0000006', 'ORG_MEMBER_MANAGE'),
('ROL0000006', 'ORG_SETTLEMENT_VIEW'),
('ROL0000006', 'ORG_DATA_VIEW'),
('ROL0000006', 'ORG_SCAN_TICKET'),
('ROL0000006', 'EVENT_VIEW'),
('ROL0000006', 'EVENT_CREATE'),
('ROL0000006', 'EVENT_EDIT'),
('ROL0000006', 'EVENT_PUBLISH'),
('ROL0000006', 'TICKET_TYPE_VIEW'),
('ROL0000006', 'TICKET_TYPE_CREATE'),
('ROL0000006', 'TICKET_TYPE_EDIT'),
('ROL0000006', 'LOCATION_VIEW'),
('ROL0000006', 'LOCATION_CREATE'),
('ROL0000006', 'LOCATION_EDIT'),
('ROL0000006', 'PROMOTION_VIEW'),
('ROL0000006', 'PROMOTION_CREATE'),
('ROL0000006', 'PROMOTION_EDIT'),
('ROL0000006', 'ORDER_VIEW'),
('ROL0000006', 'MERCH_VIEW'),
('ROL0000006', 'MERCH_CREATE'),
('ROL0000006', 'MERCH_EDIT');

-- ── [4] [user_role] 平台角色指派（29 筆）── BIGINT IDENTITY，不填 PK；所有 demo 帳號皆有 BUYER；含一人多角色
INSERT INTO [user_role] (user_id, role_id) VALUES
('USR0000001', 'SUPER_ADMIN'),
('USR0000001', 'BUYER'),
('USR0000002', 'ADMIN'),
('USR0000002', 'BUYER'),
('USR0000003', 'ORGANIZER'),
('USR0000003', 'BUYER'),
('USR0000004', 'ORGANIZER'),
('USR0000004', 'BUYER'),
('USR0000005', 'ORGANIZER'),
('USR0000005', 'BUYER'),
('USR0000006', 'ORGANIZER'),
('USR0000006', 'BUYER'),
('USR0000007', 'BUYER'),
('USR0000008', 'BUYER'),
('USR0000009', 'BUYER'),
('USR0000010', 'BUYER'),
('USR0000011', 'BUYER'),
('USR0000012', 'BUYER'),
('USR0000013', 'ORGANIZER'),
('USR0000013', 'BUYER'),
('USR0000014', 'ORGANIZER'),
('USR0000014', 'BUYER'),
('USR0000015', 'ORGANIZER'),
('USR0000015', 'BUYER'),
('USR0000016', 'BUYER'),
('USR0000017', 'BUYER'),
('USR0000018', 'BUYER'),
('USR0000019', 'BUYER'),
('USR0000020', 'BUYER');

-- ── [5] [organizer_member]（15 筆）── status 0=PENDING/1=ACCEPTED/2=REJECTED/3=REVOKED；Owner 一併寫入(status=1)
-- role_id：指派組織角色（同組織 FK）。owner 可留 NULL（getOrgPermissionIds 對 owner 走全權限分支）；
--   非 owner 成員需有角色才有 B2B 選單權限，否則進組織會被導 403。
--   ⭐ USR0000007@ORG1=財務專員(會計 demo)、USR0000016@ORG5=Scanner(驗票員 demo)。
INSERT INTO [organizer_member] (member_id, organizer_id, user_id, role_id, invited_by, invite_token, invite_token_expires, status, invited_at, joined_at) VALUES
('MBR0000001', 'ORG0000001', 'USR0000003', 'ROL0000001', 'USR0000003', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000002', 'ORG0000001', 'USR0000007', 'ROL0000002', 'USR0000003', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000003', 'ORG0000001', 'USR0000008', 'ROL0000002', 'USR0000003', 'invite-tok-01', DATEADD(DAY, 3, SYSUTCDATETIME()), 0, SYSUTCDATETIME(), NULL),
('MBR0000004', 'ORG0000002', 'USR0000004', 'ROL0000003', 'USR0000004', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000005', 'ORG0000002', 'USR0000007', 'ROL0000003', 'USR0000004', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000006', 'ORG0000003', 'USR0000005', NULL, 'USR0000005', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000007', 'ORG0000002', 'USR0000008', 'ROL0000003', 'USR0000004', NULL,            NULL,                              3, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000008', 'ORG0000005', 'USR0000013', 'ROL0000004', 'USR0000013', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000009', 'ORG0000005', 'USR0000016', 'ROL0000005', 'USR0000013', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000010', 'ORG0000005', 'USR0000017', 'ROL0000005', 'USR0000013', 'invite-tok-02', DATEADD(DAY, 3, SYSUTCDATETIME()), 0, SYSUTCDATETIME(), NULL),
('MBR0000011', 'ORG0000006', 'USR0000014', 'ROL0000006', 'USR0000014', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000012', 'ORG0000006', 'USR0000007', 'ROL0000006', 'USR0000014', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000013', 'ORG0000007', 'USR0000015', NULL, 'USR0000015', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME()),
('MBR0000014', 'ORG0000001', 'USR0000019', 'ROL0000002', 'USR0000003', NULL,            NULL,                              2, SYSUTCDATETIME(), NULL),
('MBR0000015', 'ORG0000004', 'USR0000006', NULL, 'USR0000006', NULL,            NULL,                              1, SYSUTCDATETIME(), SYSUTCDATETIME());

-- ── [6] 組織角色指派（V9.2 R8）── 原 Organizer_Member_Roles 橋接表已取消，
--      改直接寫入 [organizer_member].role_id（1 成員 = 1 組織角色）。
--      未列出的成員 role_id 維持 NULL：Owner 權限來自平台 ORGANIZER 角色，
--      PENDING/REJECTED/REVOKED 成員尚未或已不具組織角色。
--      （所有指派皆滿足「role 的 organizer_id = 成員的 organizer_id」）
UPDATE [organizer_member] SET role_id = 'ROL0000001' WHERE member_id = 'MBR0000001';  -- ORG1 USR3
UPDATE [organizer_member] SET role_id = 'ROL0000002' WHERE member_id = 'MBR0000002';  -- ORG1 USR7
UPDATE [organizer_member] SET role_id = 'ROL0000003' WHERE member_id = 'MBR0000004';  -- ORG2 USR4
UPDATE [organizer_member] SET role_id = 'ROL0000003' WHERE member_id = 'MBR0000005';  -- ORG2 USR7
UPDATE [organizer_member] SET role_id = 'ROL0000004' WHERE member_id = 'MBR0000008';  -- ORG5 USR13
UPDATE [organizer_member] SET role_id = 'ROL0000005' WHERE member_id = 'MBR0000009';  -- ORG5 USR16
UPDATE [organizer_member] SET role_id = 'ROL0000006' WHERE member_id = 'MBR0000011';  -- ORG6 USR14
UPDATE [organizer_member] SET role_id = 'ROL0000006' WHERE member_id = 'MBR0000012';  -- ORG6 USR7

-- ── [7] [user_session]（16 筆）── BIGINT IDENTITY；portal_type∈(B2C_FRONT,B2B_PORTAL,ADMIN_LOCAL)；revoked_by 0=SELF/1=ADMIN/2=SYSTEM
INSERT INTO [user_session] (user_id, token_jti, portal_type, ip_address, user_agent, expires_at, revoked_at, revoked_by) VALUES
('USR0000001', 'jti-admin-0001', 'ADMIN_LOCAL', '10.0.0.1',   'Chrome/AdminConsole', DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000003', 'jti-b2b-0001',   'B2B_PORTAL',  '10.0.0.3',   'Chrome/Mac',          DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000003', 'jti-b2c-0001',   'B2C_FRONT',   '10.0.0.3',   'Safari/iPhone',       DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000009', 'jti-b2c-0002',   'B2C_FRONT',   '10.0.0.9',   'Chrome/Android',      DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000010', 'jti-b2c-0003',   'B2C_FRONT',   '10.0.0.10',  'Edge/Windows',        DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000004', 'jti-b2b-0002',   'B2B_PORTAL',  '10.0.0.4',   'Firefox/Linux',       DATEADD(MINUTE, 15, SYSUTCDATETIME()), SYSUTCDATETIME(), 0),
('USR0000002', 'jti-admin-0002', 'ADMIN_LOCAL', '10.0.0.2',   'Chrome/AdminConsole', DATEADD(MINUTE, 15, SYSUTCDATETIME()), SYSUTCDATETIME(), 2),
('USR0000011', 'jti-b2c-0004',   'B2C_FRONT',   '10.0.0.11',  'Chrome/Android',      DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000013', 'jti-b2b-0003',   'B2B_PORTAL',  '10.0.0.13',  'Chrome/Mac',          DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000013', 'jti-b2c-0005',   'B2C_FRONT',   '10.0.0.13',  'Safari/iPhone',       DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000014', 'jti-b2b-0004',   'B2B_PORTAL',  '10.0.0.14',  'Firefox/Windows',     DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000016', 'jti-b2b-0005',   'B2B_PORTAL',  '10.0.0.16',  'Chrome/Windows',      DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000018', 'jti-b2c-0006',   'B2C_FRONT',   '10.0.0.18',  'Safari/Mac',          DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000019', 'jti-b2c-0007',   'B2C_FRONT',   '10.0.0.19',  'Chrome/Android',      DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL),
('USR0000020', 'jti-b2c-0008',   'B2C_FRONT',   '10.0.0.20',  'Edge/Windows',        DATEADD(MINUTE, 15, SYSUTCDATETIME()), NULL, NULL);

-- ── [8] [login_attempt]（18 筆）── BIGINT IDENTITY；success=1→reason NULL；success=0→reason 0=WRONG_PWD/1=NOT_FOUND/2=LOCKED/3=INACTIVE
INSERT INTO [login_attempt] (email, ip_address, success, failure_reason) VALUES
('super.admin@tap.com',  '10.0.0.1',  1, NULL),
('admin@tap.com',        '10.0.0.2',  1, NULL),
('ownerA@demo.com',       '10.0.0.3',  1, NULL),
('ownerA@demo.com',       '10.0.0.3',  0, 0),
('buyer1@demo.com',       '10.0.0.9',  1, NULL),
('notexist@demo.com',     '203.0.0.1', 0, 1),
('buyer.locked@demo.com', '10.0.0.11', 0, 2),
('buyer.inactive@demo.com','10.0.0.12',0, 3),
('buyer1@demo.com',       '203.0.0.2', 0, 0),
('ownerB@demo.com',       '10.0.0.4',  1, NULL),
('ownerE@demo.com',       '10.0.0.13', 1, NULL),
('ownerF@demo.com',       '10.0.0.14', 1, NULL),
('buyer2@demo.com',       '10.0.0.18', 1, NULL),
('buyer3@demo.com',       '10.0.0.19', 0, 0),
('buyer3@demo.com',       '10.0.0.19', 1, NULL),
('buyer.google2@demo.com','10.0.0.20', 1, NULL),
('ownerF@demo.com',       '203.0.0.5', 0, 0),
('ownerF@demo.com',       '203.0.0.5', 0, 0);

-- ── [9] [contract]（8 筆）── type 0=FREE/1=ANNUAL/2=CUSTOM；fee_type 0=%/1=FIXED；status 0=DRAFT/1=ACTIVE/2=TERMINATED/3=EXPIRED
--   每 organizer 僅一筆 status=1(ACTIVE)；DRAFT 可無簽署紀錄
INSERT INTO [contract] (contract_id, organizer_id, contract_type, fee_type, fee_value, valid_from, valid_to, contract_status, signed_at, signed_by_user_id, created_by) VALUES
('CON0000001', 'ORG0000001', 1, 0, 5.0000,  '2026-01-01', '2026-12-31', 1, '2025-12-20', 'USR0000003', 'USR0000002'),
('CON0000002', 'ORG0000002', 2, 1, 15.0000, '2026-02-01', '2027-01-31', 1, '2026-01-15', 'USR0000004', 'USR0000002'),
('CON0000003', 'ORG0000003', 0, 0, 5.0000,  '2026-06-01', NULL,         0, NULL,         NULL,         'USR0000002'),
('CON0000004', 'ORG0000001', 1, 0, 6.0000,  '2025-01-01', '2025-12-31', 3, '2024-12-20', 'USR0000003', 'USR0000002'),
('CON0000005', 'ORG0000004', 2, 1, 12.0000, '2025-03-01', '2026-02-28', 2, '2025-02-20', 'USR0000006', 'USR0000002'),
('CON0000006', 'ORG0000005', 1, 0, 4.5000,  '2026-01-01', '2026-12-31', 1, '2025-12-28', 'USR0000013', 'USR0000002'),
('CON0000007', 'ORG0000006', 0, 0, 5.0000,  '2026-06-01', NULL,         0, NULL,         NULL,         'USR0000002'),
('CON0000008', 'ORG0000005', 0, 0, 5.0000,  '2025-01-01', '2025-12-31', 3, '2024-12-15', 'USR0000013', 'USR0000002');

-- ── [10] [system_audit_log]（12 筆）── BIGINT IDENTITY；action_type 0=AUTH/1=IAM/2=ORGANIZER/3=FINANCIAL/4=SYSTEM/5=CONTENT
INSERT INTO [system_audit_log] (action_user_id, tenant_id, action_type, action_detail, target_table, target_id, ip_address) VALUES
('USR0000001', NULL,         4, N'更新系統參數 LOGIN_FAIL_MAX_ATTEMPTS', 'system_config',        'CFG_LOGIN_MAX_FAIL', '10.0.0.1'),
('USR0000002', 'ORG0000001', 2, N'核准 KYC',                             'organizer',            'ORG0000001',         '10.0.0.2'),
('USR0000002', 'ORG0000004', 2, N'退回 KYC',                             'organizer',            'ORG0000004',         '10.0.0.2'),
('USR0000002', 'ORG0000001', 3, N'建立合約',                             'contract',             'CON0000001',         '10.0.0.2'),
('USR0000003', 'ORG0000001', 1, N'指派組織角色 role_id',                 'organizer_member',     'MBR0000001',  '10.0.0.3'),
('USR0000003', 'ORG0000001', 0, N'登入成功',                             'user_session',         'jti-b2b-0001',       '10.0.0.3'),
('USR0000002', NULL,         0, N'強制登出使用者 Session',               'user_session',         'jti-admin-0002',     '10.0.0.2'),
('USR0000004', 'ORG0000002', 5, N'發布系統公告',                         'system_announcement',  'ANN0000001',    '10.0.0.4'),
('USR0000002', 'ORG0000005', 2, N'核准 KYC',                             'organizer',            'ORG0000005',    '10.0.0.2'),
('USR0000002', 'ORG0000005', 3, N'建立合約',                             'contract',             'CON0000006',    '10.0.0.2'),
('USR0000013', 'ORG0000005', 1, N'邀請成員',                             'organizer_member',     'MBR0000010',    '10.0.0.13'),
('USR0000013', 'ORG0000005', 0, N'登入成功',                             'user_session',         'jti-b2b-0003',  '10.0.0.13');

-- ── [11] [membership_plan]（3 筆）── VARCHAR(30) 語意 PK
INSERT INTO [membership_plan] (plan_id, plan_name, annual_fee, cumulative_threshold, default_fee_rate, description) VALUES
('FREE',   N'免費方案', 0,    NULL, NULL, N'發布上限 5 場，單場 500 人'),
('ANNUAL', N'組織年費方案', 7950, NULL, NULL, N'發布上限 20 場，單場 30000 人'),
('CUSTOM', N'特別方案',     NULL, NULL, NULL, N'客製化需求，來信洽詢');

-- ── [12] [plan_feature]（20 筆）── BIGINT IDENTITY；方案↔功能([saas_feature]) 對應
INSERT INTO [plan_feature] (plan_id, feature_id) VALUES
('FREE','EVENT_PUBLISH'),('FREE','BASIC_ANALYTICS'),
('ANNUAL','EVENT_PUBLISH'),('ANNUAL','BASIC_ANALYTICS'),('ANNUAL','MERCH_STORE'),('ANNUAL','PROMO_CODE'),
('CUSTOM','EVENT_PUBLISH'),('CUSTOM','BASIC_ANALYTICS'),('CUSTOM','MERCH_STORE'),('CUSTOM','PROMO_CODE'),
('CUSTOM','EVENT_TOP'),('CUSTOM','ADVANCED_ANALYTICS'),('CUSTOM','DEDICATED_SUPPORT'),('CUSTOM','CUSTOM_CONTRACT');

-- ── [13] [organizer_subscription]（6 筆）── status 0=ACTIVE/1=EXPIRED；upgrade_type 0=ANNUAL/1=CUMULATIVE/2=MANUAL；每 org 僅一筆 active
INSERT INTO [organizer_subscription] (subscription_id, organizer_id, plan_id, status_code, upgrade_type, start_date, end_date) VALUES
('SUB0000001', 'ORG0000001', 'ANNUAL', 0, 0, '2026-01-01', '2026-12-31'),
('SUB0000002', 'ORG0000002', 'ANNUAL', 0, 2, '2026-02-01', '2027-01-31'),
('SUB0000003', 'ORG0000003', 'FREE',   0, 2, '2026-06-01', NULL),
('SUB0000004', 'ORG0000001', 'ANNUAL', 1, 0, '2025-01-01', '2025-12-31'),
('SUB0000005', 'ORG0000005', 'ANNUAL', 0, 0, '2026-01-01', '2026-12-31'),
('SUB0000006', 'ORG0000006', 'FREE',   0, 2, '2026-06-01', NULL);

-- ── [14] [notification_log]（11 筆）── BIGINT IDENTITY；channel 0=EMAIL/1=SMS；status 0=PENDING/1=SENT/2=FAILED；recipient_id 可 NULL
INSERT INTO [notification_log] (template_id, recipient_id, channel, recipient, status_code, sent_at, error_msg) VALUES
('TMPL_EMAIL_VERIFY',   'USR0000009', 0, 'buyer1@demo.com',       1, SYSUTCDATETIME(), NULL),
('TMPL_EMAIL_VERIFY',   'USR0000010', 0, 'buyer.google@demo.com', 1, SYSUTCDATETIME(), NULL),
('TMPL_PWD_RESET',      'USR0000011', 0, 'buyer.locked@demo.com', 1, SYSUTCDATETIME(), NULL),
('TMPL_KYC_APPROVED',   'USR0000003', 0, 'ownerA@demo.com',       1, SYSUTCDATETIME(), NULL),
('TMPL_KYC_REJECTED',   'USR0000006', 0, 'ownerD@demo.com',       1, SYSUTCDATETIME(), NULL),
('TMPL_MEMBER_INVITE',  'USR0000008', 0, 'staff2@demo.com',       0, NULL,      NULL),
('TMPL_CONTRACT_READY', 'USR0000004', 0, 'ownerB@demo.com',       2, NULL,      N'SMTP 連線逾時'),
('TMPL_EMAIL_VERIFY',   'USR0000018', 0, 'buyer2@demo.com',       1, SYSUTCDATETIME(), NULL),
('TMPL_EMAIL_VERIFY',   'USR0000019', 0, 'buyer3@demo.com',       1, SYSUTCDATETIME(), NULL),
('TMPL_KYC_APPROVED',   'USR0000013', 0, 'ownerE@demo.com',       1, SYSUTCDATETIME(), NULL),
('TMPL_MEMBER_INVITE',  'USR0000017', 0, 'staff4@demo.com',       0, NULL,      NULL);

-- ── [15] [system_announcement]（4 筆）── target_portal∈(B2C_FRONT,B2B_PORTAL,ADMIN_LOCAL,ALL)；is_published=1 須有 published_at
INSERT INTO [system_announcement] (announcement_id, title, content, target_portal, is_published, published_at, expires_at, created_by) VALUES
('ANN0000001', N'系統維護通知',     N'本平台將於 6/15 02:00-04:00 進行維護。', 'ALL',        1, SYSUTCDATETIME(), DATEADD(DAY, 30, SYSUTCDATETIME()), 'USR0000001'),
('ANN0000002', N'新增週邊商城功能', N'Silver 以上方案開放週邊商城。',         'B2B_PORTAL', 1, SYSUTCDATETIME(), NULL,                              'USR0000002'),
('ANN0000003', N'暑期購票優惠',     N'指定活動享 9 折優惠。',                 'B2C_FRONT',  1, SYSUTCDATETIME(), DATEADD(DAY, 60, SYSUTCDATETIME()), 'USR0000002'),
('ANN0000004', N'後台改版預告',     N'管理後台介面即將更新。',                'ADMIN_LOCAL',0, NULL,             NULL,                              'USR0000001');

-- ── [16] [media_file]（9 筆）── BIGINT IDENTITY；file_type 0=IMAGE/1=DOCUMENT；uploader_id 可 NULL
--   related_table/related_id 為多型參照，無 FK（USM0000002 見 [17]）
INSERT INTO [media_file] (uploader_id, related_table, related_id, file_type, file_url, file_size_kb) VALUES
('USR0000003', 'organizer',       'ORG0000001', 0, '/api/themes/images/theme_1.jpg',     128),
('USR0000003', 'organizer',       'ORG0000001', 1, 'https://cdn.demo.com/org1-license.pdf',  512),
('USR0000004', 'organizer',       'ORG0000002', 0, '/api/themes/images/theme_2.jpg',      96),
('USR0000006', 'organizer',       'ORG0000004', 1, 'https://cdn.demo.com/org4-license.pdf',  430),
('USR0000009', 'user_submission', 'USM0000002', 0, '/api/auctions/images/auction_1.png',  64),
(NULL,         NULL,              NULL,         0, '/api/auctions/images/auction_2.jpg',    40),
('USR0000013', 'organizer',       'ORG0000005', 0, '/api/themes/images/theme_3.jpg',     156),
('USR0000013', 'organizer',       'ORG0000005', 1, 'https://cdn.demo.com/org5-license.pdf',  480),
('USR0000014', 'organizer',       'ORG0000006', 0, '/api/auctions/images/auction_4.png',     140);

-- ── [17] [user_submission]（4 筆）── form_type 0=CONTACT/1=FEEDBACK；status 0=UNREAD/1=IN_PROGRESS/2=RESOLVED
--   CK_UserSubmissions_Resolved：status=2(RESOLVED) 必須有 handled_by + handled_at；user_id 可 NULL（訪客）
INSERT INTO [user_submission] (submission_id, user_id, form_type, content, status_code, handled_by, handled_at) VALUES
('USM0000001', 'USR0000009', 1, N'活動頁面在手機上排版跑掉，希望盡快修正。',     2, 'USR0000002', SYSUTCDATETIME()),
('USM0000002', 'USR0000009', 1, N'建議增加電子票券加入 Apple Wallet 的功能。',   1, 'USR0000002', NULL),
('USM0000003', NULL,         0, N'請問企業包場合作要聯絡哪個窗口？（訪客留言）', 0, NULL,         NULL),
('USM0000004', 'USR0000018', 0, N'付款後沒有收到確認信，請協助查詢訂單。',       2, 'USR0000002', SYSUTCDATETIME());



/* -----------------------------------------
    2. 場地與活動模塊
   ----------------------------------------- */
-- 新增 Location (IDENTITY 欄位會自動產生 location_id = 1)
INSERT INTO location (name, total_capacity, grid_max_x, grid_max_y, address)
VALUES (N'台北小巨蛋', 10000, 100, 100, N'臺北市松山區南京東路四段2號'),
(N'高雄巨蛋', 15000, 120, 120, N'高雄市左營區博愛二路757號'),
(N'台北流行音樂中心', 5000, 80, 60, N'台北市南港區市民大道八段99號'),
(N'高雄流行音樂中心', 4000, 70, 60, N'高雄市鹽埕區真愛路1號'),
(N'Zepp New Taipei', 2245, 50, 40, N'新北市新莊區新北大道四段3號8樓'),
(N'台北南港展覽館1館', 18000, 150, 120, N'台北市南港區經貿二路1號'),
(N'高雄國家體育場 (世運主場館)', 50000, 250, 200, N'高雄市左營區世運大道100號'),
(N'台北大巨蛋', 40000, 200, 180, N'台北市信義區忠孝東路四段515號'),
(N'TICC 台北國際會議中心', 3100, 60, 50, N'台北市信義區信義路五段1號'),
(N'台大綜合體育館 (小巨蛋)', 4200, 70, 60, N'台北市大安區羅斯福路四段1號'),
(N'台中洲際棒球場', 20000, 160, 140, N'台中市北屯區崇德路三段835號'),
(N'樂天桃園棒球場', 20000, 160, 140, N'桃園市中壢區領航北路一段1號'),
(N'桃園陽光劇場', 15000, 120, 100, N'桃園市大園區領航北路四段216號'),
(N'新莊體育館', 7100, 90, 80, N'新北市新莊區中華路一段75號'),
(N'Legacy Taipei 傳 音樂展演空間', 1200, 40, 30, N'台北市中正區八德路一段1號'),
(N'Legacy Taichung', 1000, 35, 30, N'台中市西屯區安和路117號'),
(N'駁二 LIVE WAREHOUSE', 1400, 45, 35, N'高雄市鹽埕區大義街2之5號'),
(N'THE WALL 音樂展演空間', 600, 30, 20, N'台北市文山區羅斯福路四段200號B1'),
(N'國家音樂廳', 2000, 50, 40, N'台北市中正區中山南路21-1號'),
(N'衛武營國家藝術文化中心', 2200, 50, 45, N'高雄市鳳山區三多一路1號'),
(N'屏東演藝廳', 1050, 35, 30, N'屏東縣屏東市民生路4-17號');

-- 新增新場地 (IDENTITY 22~28)
-- =========================================
INSERT INTO location (name, total_capacity, grid_max_x, grid_max_y, address, raw_svg, bound_svg) VALUES
(N'台北小巨蛋 (20260706_1046)', 90, 10, 10, N'臺北市松山區南京東路四段2號', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="101"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="102"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="103"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="104"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="105"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="106"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="107"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="3"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="4"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(16, 185, 129); transition: fill 0.3s;" data-zone-id="5"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(6, 182, 212); transition: fill 0.3s;" data-zone-id="6"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(59, 130, 246); transition: fill 0.3s;" data-zone-id="7"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(99, 102, 241); transition: fill 0.3s;" data-zone-id="8"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(139, 92, 246); transition: fill 0.3s;" data-zone-id="9"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>'),
(N'test', 90, 10, 10, N'123', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="101"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="102"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="103"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="104"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="105"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="106"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="107"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="10"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="11"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(16, 185, 129); transition: fill 0.3s;" data-zone-id="12"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(6, 182, 212); transition: fill 0.3s;" data-zone-id="13"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(59, 130, 246); transition: fill 0.3s;" data-zone-id="14"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(99, 102, 241); transition: fill 0.3s;" data-zone-id="15"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(139, 92, 246); transition: fill 0.3s;" data-zone-id="16"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>'),
(N'台北小巨蛋-new', 90, 10, 10, N'臺北市松山區南京東路四段2號', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="101"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="102"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="103"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="104"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="105"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="106"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="107"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="17"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="17"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="17"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="17"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="17"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="17"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="17"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>'),
(N'台北小巨蛋new', 1304, 50, 50, N'臺北市松山區南京東路四段2號', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
  
  <!-- ================= STAGE 舞台區 ================= -->
  <rect x="250" y="20" width="300" height="50" fill="#334155" rx="8"></rect>
  <text x="400" y="53" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE</text>

  <!-- ================= 側邊看台 紫區 (左側) ================= -->
  <rect x="100" y="20" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="18"></rect>
  <text x="145" y="46" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2A</text>

  <rect x="100" y="70" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="19"></rect>
  <text x="145" y="96" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2B</text>

  <rect x="100" y="120" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="20"></rect>
  <text x="145" y="146" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2C</text>

  <rect x="100" y="170" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="21"></rect>
  <text x="145" y="196" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2D</text>

  <path d="M 100,220 L 190,220 L 210,290 L 120,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="22"></path>
  <text x="155" y="260" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2E</text>


  <!-- ================= 側邊看台 紅區 (右側) ================= -->
  <rect x="610" y="20" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="23"></rect>
  <text x="655" y="46" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2A</text>

  <rect x="610" y="70" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="24"></rect>
  <text x="655" y="96" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2B</text>

  <rect x="610" y="120" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="25"></rect>
  <text x="655" y="146" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2C</text>

  <rect x="610" y="170" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="26"></rect>
  <text x="655" y="196" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2D</text>

  <path d="M 610,220 L 700,220 L 680,290 L 590,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="27"></path>
  <text x="645" y="260" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2E</text>


  <!-- ================= 中央平面 特區 ================= -->
  <rect x="220" y="85" width="110" height="90" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="28"></rect>
  <text x="275" y="135" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特A區</text>

  <rect x="345" y="85" width="110" height="90" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="29"></rect>
  <text x="400" y="135" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特B區</text>

  <rect x="470" y="85" width="110" height="90" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="30"></rect>
  <text x="525" y="135" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特C區</text>

  <rect x="220" y="190" width="110" height="70" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="31"></rect>
  <text x="275" y="230" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特D區</text>

  <rect x="345" y="190" width="110" height="70" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="32"></rect>
  <text x="400" y="230" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特E區</text>

  <rect x="470" y="190" width="110" height="70" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="33"></rect>
  <text x="525" y="230" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特F區</text>

  <!-- 控台 -->
  <rect x="330" y="275" width="140" height="30" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="34"></rect>
  <text x="400" y="296" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">控台</text>


  <!-- ================= 內圈環繞 黃2區 ================= -->
  <path d="M 140,310 L 210,340 L 170,400 L 100,360 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="35"></path>
  <text x="155" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2E</text>

  <path d="M 230,350 L 320,370 L 300,430 L 200,410 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="36"></path>
  <text x="260" y="395" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2D</text>

  <path d="M 340,380 L 460,380 L 480,440 L 320,440 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="37"></path>
  <text x="400" y="415" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2C</text>

  <path d="M 480,370 L 570,350 L 600,410 L 500,430 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="38"></path>
  <text x="540" y="395" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2B</text>

  <path d="M 590,340 L 660,310 L 700,360 L 630,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="39"></path>
  <text x="645" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2A</text>


  <!-- ================= 外圈環繞 黃3區 ================= -->
  <path d="M 50,320 L 80,360 L 40,410 L 10,360 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="40"></path>
  <text x="45" y="370" font-family="sans-serif" font-size="14" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3J</text>

  <path d="M 95,375 L 145,415 L 105,475 L 50,425 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="41"></path>
  <text x="100" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3I</text>

  <path d="M 160,430 L 230,455 L 195,525 L 120,490 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="42"></path>
  <text x="175" y="485" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3H</text>

  <path d="M 245,465 L 310,480 L 295,555 L 215,535 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="43"></path>
  <text x="265" y="515" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3G</text>

  <path d="M 330,490 L 395,495 L 395,570 L 320,565 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="44"></path>
  <text x="360" y="535" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3F</text>

  <path d="M 405,495 L 470,490 L 480,565 L 405,570 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="45"></path>
  <text x="440" y="535" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3E</text>

  <path d="M 490,480 L 555,465 L 585,535 L 505,555 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="46"></path>
  <text x="535" y="515" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3D</text>

  <path d="M 570,455 L 640,430 L 680,490 L 605,525 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="47"></path>
  <text x="625" y="485" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3C</text>

  <path d="M 655,415 L 705,375 L 750,425 L 695,475 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="48"></path>
  <text x="700" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3B</text>

  <path d="M 720,360 L 750,320 L 790,360 L 760,410 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="49"></path>
  <text x="755" y="370" font-family="sans-serif" font-size="14" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3A</text>

</svg>
', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
  
  <!-- ================= STAGE 舞台區 ================= -->
  <rect x="250" y="20" width="300" height="50" fill="#334155" rx="8"></rect>
  <text x="400" y="53" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE</text>

  <!-- ================= 側邊看台 紫區 (左側) ================= -->
  <rect x="100" y="20" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(146, 50, 210); transition: fill 0.3s;" data-zone-id="18"></rect>
  <text x="145" y="46" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2A</text>

  <rect x="100" y="70" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(146, 50, 210); transition: fill 0.3s;" data-zone-id="19"></rect>
  <text x="145" y="96" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2B</text>

  <rect x="100" y="120" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(146, 50, 210); transition: fill 0.3s;" data-zone-id="20"></rect>
  <text x="145" y="146" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2C</text>

  <rect x="100" y="170" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(146, 50, 210); transition: fill 0.3s;" data-zone-id="21"></rect>
  <text x="145" y="196" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2D</text>

  <path d="M 100,220 L 190,220 L 210,290 L 120,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(146, 50, 210); transition: fill 0.3s;" data-zone-id="22"></path>
  <text x="155" y="260" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紫2E</text>


  <!-- ================= 側邊看台 紅區 (右側) ================= -->
  <rect x="610" y="20" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(218, 16, 16); transition: fill 0.3s;" data-zone-id="23"></rect>
  <text x="655" y="46" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2A</text>

  <rect x="610" y="70" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(218, 16, 16); transition: fill 0.3s;" data-zone-id="24"></rect>
  <text x="655" y="96" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2B</text>

  <rect x="610" y="120" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(218, 16, 16); transition: fill 0.3s;" data-zone-id="25"></rect>
  <text x="655" y="146" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2C</text>

  <rect x="610" y="170" width="90" height="40" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(218, 16, 16); transition: fill 0.3s;" data-zone-id="26"></rect>
  <text x="655" y="196" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2D</text>

  <path d="M 610,220 L 700,220 L 680,290 L 590,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(218, 16, 16); transition: fill 0.3s;" data-zone-id="27"></path>
  <text x="645" y="260" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">紅2E</text>


  <!-- ================= 中央平面 特區 ================= -->
  <rect x="220" y="85" width="110" height="90" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="28"></rect>
  <text x="275" y="135" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特A區</text>

  <rect x="345" y="85" width="110" height="90" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(16, 185, 129); transition: fill 0.3s;" data-zone-id="29"></rect>
  <text x="400" y="135" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特B區</text>

  <rect x="470" y="85" width="110" height="90" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(121, 215, 184); transition: fill 0.3s;" data-zone-id="30"></rect>
  <text x="525" y="135" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特C區</text>

  <rect x="220" y="190" width="110" height="70" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(6, 182, 212); transition: fill 0.3s;" data-zone-id="31"></rect>
  <text x="275" y="230" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特D區</text>

  <rect x="345" y="190" width="110" height="70" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(59, 130, 246); transition: fill 0.3s;" data-zone-id="32"></rect>
  <text x="400" y="230" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特E區</text>

  <rect x="470" y="190" width="110" height="70" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(99, 102, 241); transition: fill 0.3s;" data-zone-id="33"></rect>
  <text x="525" y="230" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特F區</text>

  <!-- 控台 -->
  <rect x="330" y="275" width="140" height="30" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="34"></rect>
  <text x="400" y="296" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">控台</text>


  <!-- ================= 內圈環繞 黃2區 ================= -->
  <path d="M 140,310 L 210,340 L 170,400 L 100,360 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="35"></path>
  <text x="155" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2E</text>

  <path d="M 230,350 L 320,370 L 300,430 L 200,410 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(251, 211, 9); transition: fill 0.3s;" data-zone-id="36"></path>
  <text x="260" y="395" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2D</text>

  <path d="M 340,380 L 460,380 L 480,440 L 320,440 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(251, 211, 9); transition: fill 0.3s;" data-zone-id="37"></path>
  <text x="400" y="415" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2C</text>

  <path d="M 480,370 L 570,350 L 600,410 L 500,430 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(251, 211, 9); transition: fill 0.3s;" data-zone-id="38"></path>
  <text x="540" y="395" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2B</text>

  <path d="M 590,340 L 660,310 L 700,360 L 630,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(251, 211, 9); transition: fill 0.3s;" data-zone-id="39"></path>
  <text x="645" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2A</text>


  <!-- ================= 外圈環繞 黃3區 ================= -->
  <path d="M 50,320 L 80,360 L 40,410 L 10,360 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="40"></path>
  <text x="45" y="370" font-family="sans-serif" font-size="14" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3J</text>

  <path d="M 95,375 L 145,415 L 105,475 L 50,425 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="41"></path>
  <text x="100" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3I</text>

  <path d="M 160,430 L 230,455 L 195,525 L 120,490 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(239, 68, 68); transition: fill 0.3s;" data-zone-id="42"></path>
  <text x="175" y="485" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3H</text>

  <path d="M 245,465 L 310,480 L 295,555 L 215,535 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(236, 72, 153); transition: fill 0.3s;" data-zone-id="43"></path>
  <text x="265" y="515" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3G</text>

  <path d="M 330,490 L 395,495 L 395,570 L 320,565 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(217, 70, 239); transition: fill 0.3s;" data-zone-id="44"></path>
  <text x="360" y="535" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3F</text>

  <path d="M 405,495 L 470,490 L 480,565 L 405,570 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(139, 92, 246); transition: fill 0.3s;" data-zone-id="45"></path>
  <text x="440" y="535" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3E</text>

  <path d="M 490,480 L 555,465 L 585,535 L 505,555 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(99, 102, 241); transition: fill 0.3s;" data-zone-id="46"></path>
  <text x="535" y="515" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3D</text>

  <path d="M 570,455 L 640,430 L 680,490 L 605,525 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(38, 227, 101); transition: fill 0.3s;" data-zone-id="47"></path>
  <text x="625" y="485" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3C</text>

  <path d="M 655,415 L 705,375 L 750,425 L 695,475 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(112, 222, 23); transition: fill 0.3s;" data-zone-id="48"></path>
  <text x="700" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3B</text>

  <path d="M 720,360 L 750,320 L 790,360 L 760,410 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(112, 222, 23); transition: fill 0.3s;" data-zone-id="49"></path>
  <text x="755" y="370" font-family="sans-serif" font-size="14" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃3A</text>

</svg>
'),
(N'測試場地-大型', 910, 50, 50, N'ISPAN', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
  <!-- STAGE 舞台區 -->
  <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
  <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>

  <!-- 西北特區 A1 -->
  <path d="M 150,120 L 390,120 L 390,220 L 150,220 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="101"></path>
  <text x="270" y="180" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西北特區 A1</text>

  <!-- 東北特區 A2 -->
  <path d="M 410,120 L 650,120 L 650,220 L 410,220 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="102"></path>
  <text x="530" y="180" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東北特區 A2</text>

  <!-- 西中搖滾 B1 -->
  <path d="M 100,240 L 390,240 L 390,340 L 100,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="103"></path>
  <text x="245" y="300" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西中搖滾 B1</text>

  <!-- 東中搖滾 B2 -->
  <path d="M 410,240 L 700,240 L 700,340 L 410,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="104"></path>
  <text x="555" y="300" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東中搖滾 B2</text>

  <!-- 西側看台區 -->
  <path d="M 50,120 L 140,120 L 140,340 L 50,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="105"></path>
  <text x="95" y="235" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 西區</text>

  <!-- 東側看台區 -->
  <path d="M 660,120 L 750,120 L 750,340 L 660,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="106"></path>
  <text x="705" y="235" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 東區</text>

  <!-- 西南看台 C1 -->
  <path d="M 50,360 L 300,360 L 300,480 L 50,480 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="107"></path>
  <text x="175" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西南看台 C1</text>

  <!-- 中央南看台 C2 (控台後) -->
  <path d="M 310,360 L 530,360 L 530,480 L 310,480 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="108"></path>
  <text x="420" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">中央南看台 C2 (控台後)</text>

  <!-- 東南看台 C3 -->
  <path d="M 550,360 L 750,360 L 750,480 L 550,480 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="109"></path>
  <text x="650" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東南看台 C3</text>
</svg>
', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
  <!-- STAGE 舞台區 -->
  <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
  <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>

  <!-- 西北特區 A1 -->
  <path d="M 150,120 L 390,120 L 390,220 L 150,220 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="50"></path>
  <text x="270" y="180" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西北特區 A1</text>

  <!-- 東北特區 A2 -->
  <path d="M 410,120 L 650,120 L 650,220 L 410,220 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="51"></path>
  <text x="530" y="180" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東北特區 A2</text>

  <!-- 西中搖滾 B1 -->
  <path d="M 100,240 L 390,240 L 390,340 L 100,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(16, 185, 129); transition: fill 0.3s;" data-zone-id="52"></path>
  <text x="245" y="300" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西中搖滾 B1</text>

  <!-- 東中搖滾 B2 -->
  <path d="M 410,240 L 700,240 L 700,340 L 410,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(6, 182, 212); transition: fill 0.3s;" data-zone-id="53"></path>
  <text x="555" y="300" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東中搖滾 B2</text>

  <!-- 西側看台區 -->
  <path d="M 50,120 L 140,120 L 140,340 L 50,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(217, 70, 239); transition: fill 0.3s;" data-zone-id="57"></path>
  <text x="95" y="235" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 西區</text>

  <!-- 東側看台區 -->
  <path d="M 660,120 L 750,120 L 750,340 L 660,340 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(236, 72, 153); transition: fill 0.3s;" data-zone-id="58"></path>
  <text x="705" y="235" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 東區</text>

  <!-- 西南看台 C1 -->
  <path d="M 50,360 L 300,360 L 300,480 L 50,480 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(59, 130, 246); transition: fill 0.3s;" data-zone-id="54"></path>
  <text x="175" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西南看台 C1</text>

  <!-- 中央南看台 C2 (控台後) -->
  <path d="M 310,360 L 530,360 L 530,480 L 310,480 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(99, 102, 241); transition: fill 0.3s;" data-zone-id="55"></path>
  <text x="420" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">中央南看台 C2 (控台後)</text>

  <!-- 東南看台 C3 -->
  <path d="M 550,360 L 750,360 L 750,480 L 550,480 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(139, 92, 246); transition: fill 0.3s;" data-zone-id="56"></path>
  <text x="650" y="430" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東南看台 C3</text>
</svg>
'),
(N'測試場地-環繞', 1040, 50, 50, N'ISPAN', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
  
  <path d="M 50,40 L 210,40 L 210,180 L 50,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="201"></path>
  <text x="130" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西北特區 N1</text>

  <path d="M 230,40 L 390,40 L 390,180 L 230,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="202"></path>
  <text x="310" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正北特區 N2</text>

  <path d="M 410,40 L 570,40 L 570,180 L 410,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="203"></path>
  <text x="490" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正北特區 N3</text>

  <path d="M 590,40 L 750,40 L 750,180 L 590,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="204"></path>
  <text x="670" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東北特區 N4</text>


  <path d="M 50,200 L 190,200 L 190,290 L 50,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="205"></path>
  <text x="120" y="250" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西看台 W1</text>

  <path d="M 50,310 L 190,310 L 190,400 L 50,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="206"></path>
  <text x="120" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西看台 W2</text>

  <rect x="220" y="200" width="360" height="200" fill="#334155" rx="8"></rect>
  <text x="400" y="308" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 中央舞台區</text>

  <path d="M 610,200 L 750,200 L 750,290 L 610,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="207"></path>
  <text x="680" y="250" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東看台 E1</text>

  <path d="M 610,310 L 750,310 L 750,400 L 610,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="208"></path>
  <text x="680" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東看台 E2</text>


  <path d="M 50,420 L 210,420 L 210,560 L 50,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="209"></path>
  <text x="130" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西南特區 S1</text>

  <path d="M 230,420 L 390,420 L 390,560 L 230,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="210"></path>
  <text x="310" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正南特區 S2</text>

  <path d="M 410,420 L 570,420 L 570,560 L 410,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="211"></path>
  <text x="490" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正南特區 S3</text>

  <path d="M 590,420 L 750,420 L 750,560 L 590,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="212"></path>
  <text x="670" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東南特區 S4</text>

</svg>
', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
  
  <path d="M 50,40 L 210,40 L 210,180 L 50,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="59"></path>
  <text x="130" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西北特區 N1</text>

  <path d="M 230,40 L 390,40 L 390,180 L 230,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="60"></path>
  <text x="310" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正北特區 N2</text>

  <path d="M 410,40 L 570,40 L 570,180 L 410,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(16, 185, 129); transition: fill 0.3s;" data-zone-id="61"></path>
  <text x="490" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正北特區 N3</text>

  <path d="M 590,40 L 750,40 L 750,180 L 590,180 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(6, 182, 212); transition: fill 0.3s;" data-zone-id="62"></path>
  <text x="670" y="115" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東北特區 N4</text>


  <path d="M 50,200 L 190,200 L 190,290 L 50,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(59, 130, 246); transition: fill 0.3s;" data-zone-id="63"></path>
  <text x="120" y="250" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西看台 W1</text>

  <path d="M 50,310 L 190,310 L 190,400 L 50,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(99, 102, 241); transition: fill 0.3s;" data-zone-id="64"></path>
  <text x="120" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西看台 W2</text>

  <rect x="220" y="200" width="360" height="200" fill="#334155" rx="8"></rect>
  <text x="400" y="308" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 中央舞台區</text>

  <path d="M 610,200 L 750,200 L 750,290 L 610,290 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(139, 92, 246); transition: fill 0.3s;" data-zone-id="65"></path>
  <text x="680" y="250" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東看台 E1</text>

  <path d="M 610,310 L 750,310 L 750,400 L 610,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(217, 70, 239); transition: fill 0.3s;" data-zone-id="66"></path>
  <text x="680" y="360" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東看台 E2</text>


  <path d="M 50,420 L 210,420 L 210,560 L 50,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(236, 72, 153); transition: fill 0.3s;" data-zone-id="67"></path>
  <text x="130" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">西南特區 S1</text>

  <path d="M 230,420 L 390,420 L 390,560 L 230,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(239, 68, 68); transition: fill 0.3s;" data-zone-id="68"></path>
  <text x="310" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正南特區 S2</text>

  <path d="M 410,420 L 570,420 L 570,560 L 410,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="69"></path>
  <text x="490" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">正南特區 S3</text>

  <path d="M 590,420 L 750,420 L 750,560 L 590,560 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="70"></path>
  <text x="670" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">東南特區 S4</text>

</svg>
'),
(N'台北小巨蛋 (20260706_1153)', 90, 10, 10, N'臺北市松山區南京東路四段2號', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="101"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="102"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="103"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="104"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="105"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="106"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="107"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>', N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>
      
      <path d="M 150,130 L 380,130 L 380,250 L 150,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(249, 115, 22); transition: fill 0.3s;" data-zone-id="71"></path>
      <text x="265" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A1</text>

      <path d="M 420,130 L 650,130 L 650,250 L 420,250 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(234, 179, 8); transition: fill 0.3s;" data-zone-id="72"></path>
      <text x="535" y="195" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 A2</text>

      <path d="M 100,280 L 380,280 L 380,400 L 100,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(16, 185, 129); transition: fill 0.3s;" data-zone-id="73"></path>
      <text x="240" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B1</text>

      <path d="M 420,280 L 700,280 L 700,400 L 420,400 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(6, 182, 212); transition: fill 0.3s;" data-zone-id="74"></path>
      <text x="560" y="345" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">搖滾區 B2</text>

      <path d="M 50,430 L 250,430 L 250,550 L 50,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(59, 130, 246); transition: fill 0.3s;" data-zone-id="75"></path>
      <text x="150" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C1</text>

      <path d="M 280,430 L 520,430 L 520,550 L 280,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(99, 102, 241); transition: fill 0.3s;" data-zone-id="76"></path>
      <text x="400" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C2 (控台後方)</text>

      <path d="M 550,430 L 750,430 L 750,550 L 550,550 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(139, 92, 246); transition: fill 0.3s;" data-zone-id="77"></path>
      <text x="650" y="495" font-family="sans-serif" font-size="16" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">看台區 C3</text>
    </svg>');

-- 新增 Zone (關聯 location_id = 1)
INSERT INTO zone (location_id, name, color)
VALUES 
(1, N'特區 (搖滾區)', '#ec4899'),
(1, N'黃2B區 (看台)', '#10b981');

-- 新增新分區 (IDENTITY 3~77)
-- =========================================
INSERT INTO zone (location_id, name, color, seat_count) VALUES
(22, N'新分區', '#f97316', 20),
(22, N'新分區', '#eab308', 20),
(22, N'新分區', '#10b981', 15),
(22, N'新分區', '#06b6d4', 15),
(22, N'新分區', '#3b82f6', 4),
(22, N'新分區', '#6366f1', 10),
(22, N'新分區', '#8b5cf6', 6),
(23, N'A1', '#f97316', 20),
(23, N'新分區', '#eab308', 20),
(23, N'新分區', '#10b981', 15),
(23, N'新分區', '#06b6d4', 15),
(23, N'新分區', '#3b82f6', 6),
(23, N'新分區', '#6366f1', 8),
(23, N'新分區', '#8b5cf6', 6),
(24, N'新分區', '#f97316', 90),
(25, N'紫2A', '#9232d2', 48),
(25, N'紫2B', '#9232d2', 48),
(25, N'紫2C', '#9232d2', 48),
(25, N'紫2D', '#9232d2', 48),
(25, N'紫2E', '#9232d2', 48),
(25, N'R2A', '#da1010', 48),
(25, N'R2B', '#da1010', 48),
(25, N'R2C', '#da1010', 48),
(25, N'R2D', '#da1010', 48),
(25, N'R2E', '#da1010', 48),
(25, N'SA', '#eab308', 30),
(25, N'SB', '#10b981', 48),
(25, N'SC', '#79d7b8', 30),
(25, N'SD', '#06b6d4', 35),
(25, N'SE', '#3b82f6', 56),
(25, N'SF', '#6366f1', 35),
(25, N'no', '#ffffff', 48),
(25, N'Y1A', '#fbd309', 40),
(25, N'Y1B', '#fbd309', 48),
(25, N'Y1C', '#fbd309', 96),
(25, N'Y1D', '#fbd309', 48),
(25, N'Y1E', '#eab308', 40),
(25, N'Y2A', '#70de17', 27),
(25, N'Y2B', '#70de17', 27),
(25, N'Y2C', '#26e365', 27),
(25, N'Y2D', '#6366f1', 27),
(25, N'Y2E', '#8b5cf6', 27),
(25, N'Y2F', '#d946ef', 27),
(25, N'Y2G', '#ec4899', 27),
(25, N'Y2H', '#ef4444', 27),
(25, N'Y2I', '#f97316', 27),
(25, N'Y2J', '#eab308', 27),
(26, N'A1', '#f97316', 98),
(26, N'A2', '#eab308', 91),
(26, N'B1', '#10b981', 98),
(26, N'B2', '#06b6d4', 91),
(26, N'C1', '#3b82f6', 84),
(26, N'C2', '#6366f1', 140),
(26, N'C3', '#8b5cf6', 84),
(26, N'西看台', '#d946ef', 112),
(26, N'東看台', '#ec4899', 112),
(27, N'N1', '#f97316', 100),
(27, N'N2', '#eab308', 110),
(27, N'N3', '#10b981', 100),
(27, N'N4', '#06b6d4', 100),
(27, N'W1', '#3b82f6', 50),
(27, N'W2', '#6366f1', 60),
(27, N'E1', '#8b5cf6', 50),
(27, N'E2', '#d946ef', 60),
(27, N'S1', '#ec4899', 100),
(27, N'S2', '#ef4444', 110),
(27, N'S3', '#f97316', 100),
(27, N'S4', '#eab308', 100),
(28, N'新分區', '#f97316', 20),
(28, N'新分區', '#eab308', 20),
(28, N'新分區', '#10b981', 15),
(28, N'新分區', '#06b6d4', 15),
(28, N'新分區', '#3b82f6', 6),
(28, N'新分區', '#6366f1', 8),
(28, N'新分區', '#8b5cf6', 6);

-- 新增 Seat (關聯 zone_id)(補齊 x_index 與 y_index 讓前端畫得出來)
INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index)
VALUES 
(1, 1, 1, 1, 1, 1), -- 搖滾區 1排1號 (網格 X=1, Y=1)
(2, 1, 1, 2, 2, 1), -- 搖滾區 1排2號 (網格 X=2, Y=1)
(3, 2, 10, 5, 5, 10); -- 看台區 10排5號 (網格 X=5, Y=10)

-- 新增新座位 (seat_id 951~3898)
-- =========================================
INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(951, 3, 1, 1, 1, 2),
(952, 3, 1, 2, 2, 2),
(953, 3, 1, 3, 3, 2),
(954, 3, 1, 4, 4, 2),
(955, 3, 1, 5, 5, 2),
(956, 3, 2, 1, 1, 3),
(957, 3, 2, 2, 2, 3),
(958, 3, 2, 3, 3, 3),
(959, 3, 2, 4, 4, 3),
(960, 3, 2, 5, 5, 3),
(961, 3, 3, 1, 1, 4),
(962, 3, 3, 2, 2, 4),
(963, 3, 3, 3, 3, 4),
(964, 3, 3, 4, 4, 4),
(965, 3, 3, 5, 5, 4),
(966, 3, 4, 1, 1, 5),
(967, 3, 4, 2, 2, 5),
(968, 3, 4, 3, 3, 5),
(969, 3, 4, 4, 4, 5),
(970, 3, 4, 5, 5, 5),
(971, 4, 1, 6, 6, 2),
(972, 4, 1, 7, 7, 2),
(973, 4, 1, 8, 8, 2),
(974, 4, 1, 9, 9, 2),
(975, 4, 1, 10, 10, 2),
(976, 4, 2, 6, 6, 3),
(977, 4, 2, 7, 7, 3),
(978, 4, 2, 8, 8, 3),
(979, 4, 2, 9, 9, 3),
(980, 4, 2, 10, 10, 3),
(981, 4, 3, 6, 6, 4),
(982, 4, 3, 7, 7, 4),
(983, 4, 3, 8, 8, 4),
(984, 4, 3, 9, 9, 4),
(985, 4, 3, 10, 10, 4),
(986, 4, 4, 6, 6, 5),
(987, 4, 4, 7, 7, 5),
(988, 4, 4, 8, 8, 5),
(989, 4, 4, 9, 9, 5),
(990, 4, 4, 10, 10, 5),
(991, 5, 5, 1, 1, 6),
(992, 5, 5, 2, 2, 6),
(993, 5, 5, 3, 3, 6),
(994, 5, 5, 4, 4, 6),
(995, 5, 5, 5, 5, 6),
(996, 5, 6, 1, 1, 7),
(997, 5, 6, 2, 2, 7),
(998, 5, 6, 3, 3, 7),
(999, 5, 6, 4, 4, 7),
(1000, 5, 6, 5, 5, 7),
(1001, 5, 7, 1, 1, 8),
(1002, 5, 7, 2, 2, 8),
(1003, 5, 7, 3, 3, 8),
(1004, 5, 7, 4, 4, 8),
(1005, 5, 7, 5, 5, 8),
(1006, 6, 5, 6, 6, 6),
(1007, 6, 5, 7, 7, 6),
(1008, 6, 5, 8, 8, 6),
(1009, 6, 5, 9, 9, 6),
(1010, 6, 5, 10, 10, 6),
(1011, 6, 6, 6, 6, 7),
(1012, 6, 6, 7, 7, 7),
(1013, 6, 6, 8, 8, 7),
(1014, 6, 6, 9, 9, 7),
(1015, 6, 6, 10, 10, 7),
(1016, 6, 7, 6, 6, 8),
(1017, 6, 7, 7, 7, 8),
(1018, 6, 7, 8, 8, 8),
(1019, 6, 7, 9, 9, 8),
(1020, 6, 7, 10, 10, 8),
(1021, 7, 8, 1, 1, 9),
(1022, 7, 8, 2, 2, 9),
(1023, 7, 9, 1, 1, 10),
(1024, 7, 9, 2, 2, 10),
(1025, 8, 8, 3, 3, 9),
(1026, 8, 8, 4, 4, 9),
(1027, 8, 8, 5, 5, 9),
(1028, 8, 8, 6, 6, 9),
(1029, 8, 8, 7, 7, 9),
(1030, 8, 9, 3, 3, 10),
(1031, 8, 9, 4, 4, 10),
(1032, 8, 9, 5, 5, 10),
(1033, 8, 9, 6, 6, 10),
(1034, 8, 9, 7, 7, 10),
(1035, 9, 8, 8, 8, 9),
(1036, 9, 8, 9, 9, 9),
(1037, 9, 8, 10, 10, 9),
(1038, 9, 9, 8, 8, 10),
(1039, 9, 9, 9, 9, 10),
(1040, 9, 9, 10, 10, 10),
(1041, 10, 1, 1, 1, 2),
(1042, 10, 1, 2, 2, 2),
(1043, 10, 1, 3, 3, 2),
(1044, 10, 1, 4, 4, 2),
(1045, 10, 1, 5, 5, 2),
(1046, 10, 2, 1, 1, 3),
(1047, 10, 2, 2, 2, 3),
(1048, 10, 2, 3, 3, 3),
(1049, 10, 2, 4, 4, 3),
(1050, 10, 2, 5, 5, 3),
(1051, 10, 3, 1, 1, 4),
(1052, 10, 3, 2, 2, 4),
(1053, 10, 3, 3, 3, 4),
(1054, 10, 3, 4, 4, 4),
(1055, 10, 3, 5, 5, 4),
(1056, 10, 4, 1, 1, 5),
(1057, 10, 4, 2, 2, 5),
(1058, 10, 4, 3, 3, 5),
(1059, 10, 4, 4, 4, 5),
(1060, 10, 4, 5, 5, 5),
(1061, 11, 1, 6, 6, 2),
(1062, 11, 1, 7, 7, 2),
(1063, 11, 1, 8, 8, 2),
(1064, 11, 1, 9, 9, 2),
(1065, 11, 1, 10, 10, 2),
(1066, 11, 2, 6, 6, 3),
(1067, 11, 2, 7, 7, 3),
(1068, 11, 2, 8, 8, 3),
(1069, 11, 2, 9, 9, 3),
(1070, 11, 2, 10, 10, 3),
(1071, 11, 3, 6, 6, 4),
(1072, 11, 3, 7, 7, 4),
(1073, 11, 3, 8, 8, 4),
(1074, 11, 3, 9, 9, 4),
(1075, 11, 3, 10, 10, 4),
(1076, 11, 4, 6, 6, 5),
(1077, 11, 4, 7, 7, 5),
(1078, 11, 4, 8, 8, 5),
(1079, 11, 4, 9, 9, 5),
(1080, 11, 4, 10, 10, 5),
(1081, 12, 5, 1, 1, 6),
(1082, 12, 5, 2, 2, 6),
(1083, 12, 5, 3, 3, 6),
(1084, 12, 5, 4, 4, 6),
(1085, 12, 5, 5, 5, 6),
(1086, 12, 6, 1, 1, 7),
(1087, 12, 6, 2, 2, 7),
(1088, 12, 6, 3, 3, 7),
(1089, 12, 6, 4, 4, 7),
(1090, 12, 6, 5, 5, 7),
(1091, 12, 7, 1, 1, 8),
(1092, 12, 7, 2, 2, 8),
(1093, 12, 7, 3, 3, 8),
(1094, 12, 7, 4, 4, 8),
(1095, 12, 7, 5, 5, 8),
(1096, 13, 5, 6, 6, 6),
(1097, 13, 5, 7, 7, 6),
(1098, 13, 5, 8, 8, 6),
(1099, 13, 5, 9, 9, 6),
(1100, 13, 5, 10, 10, 6),
(1101, 13, 6, 6, 6, 7),
(1102, 13, 6, 7, 7, 7),
(1103, 13, 6, 8, 8, 7),
(1104, 13, 6, 9, 9, 7),
(1105, 13, 6, 10, 10, 7),
(1106, 13, 7, 6, 6, 8),
(1107, 13, 7, 7, 7, 8),
(1108, 13, 7, 8, 8, 8),
(1109, 13, 7, 9, 9, 8),
(1110, 13, 7, 10, 10, 8),
(1111, 14, 8, 1, 1, 9),
(1112, 14, 8, 2, 2, 9),
(1113, 14, 8, 3, 3, 9),
(1114, 14, 9, 1, 1, 10),
(1115, 14, 9, 2, 2, 10),
(1116, 14, 9, 3, 3, 10),
(1117, 15, 8, 4, 4, 9),
(1118, 15, 8, 5, 5, 9),
(1119, 15, 8, 6, 6, 9),
(1120, 15, 8, 7, 7, 9),
(1121, 15, 9, 4, 4, 10),
(1122, 15, 9, 5, 5, 10),
(1123, 15, 9, 6, 6, 10),
(1124, 15, 9, 7, 7, 10),
(1125, 16, 8, 8, 8, 9),
(1126, 16, 8, 9, 9, 9),
(1127, 16, 8, 10, 10, 9),
(1128, 16, 9, 8, 8, 10),
(1129, 16, 9, 9, 9, 10),
(1130, 16, 9, 10, 10, 10),
(1131, 17, 1, 1, 1, 2),
(1132, 17, 1, 2, 2, 2),
(1133, 17, 1, 3, 3, 2),
(1134, 17, 1, 4, 4, 2),
(1135, 17, 1, 5, 5, 2),
(1136, 17, 1, 6, 6, 2),
(1137, 17, 1, 7, 7, 2),
(1138, 17, 1, 8, 8, 2),
(1139, 17, 1, 9, 9, 2),
(1140, 17, 1, 10, 10, 2),
(1141, 17, 2, 1, 1, 3),
(1142, 17, 2, 2, 2, 3),
(1143, 17, 2, 3, 3, 3),
(1144, 17, 2, 4, 4, 3),
(1145, 17, 2, 5, 5, 3),
(1146, 17, 2, 6, 6, 3),
(1147, 17, 2, 7, 7, 3),
(1148, 17, 2, 8, 8, 3),
(1149, 17, 2, 9, 9, 3),
(1150, 17, 2, 10, 10, 3),
(1151, 17, 3, 1, 1, 4),
(1152, 17, 3, 2, 2, 4),
(1153, 17, 3, 3, 3, 4),
(1154, 17, 3, 4, 4, 4),
(1155, 17, 3, 5, 5, 4),
(1156, 17, 3, 6, 6, 4),
(1157, 17, 3, 7, 7, 4),
(1158, 17, 3, 8, 8, 4),
(1159, 17, 3, 9, 9, 4),
(1160, 17, 3, 10, 10, 4),
(1161, 17, 4, 1, 1, 5),
(1162, 17, 4, 2, 2, 5),
(1163, 17, 4, 3, 3, 5),
(1164, 17, 4, 4, 4, 5),
(1165, 17, 4, 5, 5, 5),
(1166, 17, 4, 6, 6, 5),
(1167, 17, 4, 7, 7, 5),
(1168, 17, 4, 8, 8, 5),
(1169, 17, 4, 9, 9, 5),
(1170, 17, 4, 10, 10, 5),
(1171, 17, 5, 1, 1, 6),
(1172, 17, 5, 2, 2, 6),
(1173, 17, 5, 3, 3, 6),
(1174, 17, 5, 4, 4, 6),
(1175, 17, 5, 5, 5, 6),
(1176, 17, 5, 6, 6, 6),
(1177, 17, 5, 7, 7, 6),
(1178, 17, 5, 8, 8, 6),
(1179, 17, 5, 9, 9, 6),
(1180, 17, 5, 10, 10, 6),
(1181, 17, 6, 1, 1, 7),
(1182, 17, 6, 2, 2, 7),
(1183, 17, 6, 3, 3, 7),
(1184, 17, 6, 4, 4, 7),
(1185, 17, 6, 5, 5, 7),
(1186, 17, 6, 6, 6, 7),
(1187, 17, 6, 7, 7, 7),
(1188, 17, 6, 8, 8, 7),
(1189, 17, 6, 9, 9, 7),
(1190, 17, 6, 10, 10, 7),
(1191, 17, 7, 1, 1, 8),
(1192, 17, 7, 2, 2, 8),
(1193, 17, 7, 3, 3, 8),
(1194, 17, 7, 4, 4, 8),
(1195, 17, 7, 5, 5, 8),
(1196, 17, 7, 6, 6, 8),
(1197, 17, 7, 7, 7, 8),
(1198, 17, 7, 8, 8, 8),
(1199, 17, 7, 9, 9, 8),
(1200, 17, 7, 10, 10, 8),
(1201, 17, 8, 1, 1, 9),
(1202, 17, 8, 2, 2, 9),
(1203, 17, 8, 3, 3, 9),
(1204, 17, 8, 4, 4, 9),
(1205, 17, 8, 5, 5, 9),
(1206, 17, 8, 6, 6, 9),
(1207, 17, 8, 7, 7, 9),
(1208, 17, 8, 8, 8, 9),
(1209, 17, 8, 9, 9, 9),
(1210, 17, 8, 10, 10, 9),
(1211, 17, 9, 1, 1, 10),
(1212, 17, 9, 2, 2, 10),
(1213, 17, 9, 3, 3, 10),
(1214, 17, 9, 4, 4, 10),
(1215, 17, 9, 5, 5, 10),
(1216, 17, 9, 6, 6, 10),
(1217, 17, 9, 7, 7, 10),
(1218, 17, 9, 8, 8, 10),
(1219, 17, 9, 9, 9, 10),
(1220, 17, 9, 10, 10, 10),
(1221, 18, 12, 1, 1, 1),
(1222, 18, 11, 1, 2, 1),
(1223, 18, 10, 1, 3, 1),
(1224, 18, 9, 1, 4, 1),
(1225, 18, 8, 1, 5, 1),
(1226, 18, 7, 1, 6, 1),
(1227, 18, 6, 1, 7, 1),
(1228, 18, 5, 1, 8, 1),
(1229, 18, 4, 1, 9, 1),
(1230, 18, 3, 1, 10, 1),
(1231, 18, 2, 1, 11, 1),
(1232, 18, 1, 1, 12, 1),
(1233, 18, 12, 2, 1, 2),
(1234, 18, 11, 2, 2, 2),
(1235, 18, 10, 2, 3, 2),
(1236, 18, 9, 2, 4, 2),
(1237, 18, 8, 2, 5, 2),
(1238, 18, 7, 2, 6, 2),
(1239, 18, 6, 2, 7, 2),
(1240, 18, 5, 2, 8, 2),
(1241, 18, 4, 2, 9, 2),
(1242, 18, 3, 2, 10, 2),
(1243, 18, 2, 2, 11, 2),
(1244, 18, 1, 2, 12, 2),
(1245, 18, 12, 3, 1, 3),
(1246, 18, 11, 3, 2, 3),
(1247, 18, 10, 3, 3, 3),
(1248, 18, 9, 3, 4, 3),
(1249, 18, 8, 3, 5, 3),
(1250, 18, 7, 3, 6, 3),
(1251, 18, 6, 3, 7, 3),
(1252, 18, 5, 3, 8, 3),
(1253, 18, 4, 3, 9, 3),
(1254, 18, 3, 3, 10, 3),
(1255, 18, 2, 3, 11, 3),
(1256, 18, 1, 3, 12, 3),
(1257, 18, 12, 4, 1, 4),
(1258, 18, 11, 4, 2, 4),
(1259, 18, 10, 4, 3, 4),
(1260, 18, 9, 4, 4, 4),
(1261, 18, 8, 4, 5, 4),
(1262, 18, 7, 4, 6, 4),
(1263, 18, 6, 4, 7, 4),
(1264, 18, 5, 4, 8, 4),
(1265, 18, 4, 4, 9, 4),
(1266, 18, 3, 4, 10, 4),
(1267, 18, 2, 4, 11, 4),
(1268, 18, 1, 4, 12, 4),
(1269, 19, 1, 1, 1, 6),
(1270, 19, 1, 2, 2, 6),
(1271, 19, 1, 3, 3, 6),
(1272, 19, 1, 4, 4, 6),
(1273, 19, 1, 5, 5, 6),
(1274, 19, 1, 6, 6, 6),
(1275, 19, 1, 7, 7, 6),
(1276, 19, 1, 8, 8, 6),
(1277, 19, 1, 9, 9, 6),
(1278, 19, 1, 10, 10, 6),
(1279, 19, 1, 11, 11, 6),
(1280, 19, 1, 12, 12, 6),
(1281, 19, 2, 1, 1, 7),
(1282, 19, 2, 2, 2, 7),
(1283, 19, 2, 3, 3, 7),
(1284, 19, 2, 4, 4, 7),
(1285, 19, 2, 5, 5, 7),
(1286, 19, 2, 6, 6, 7),
(1287, 19, 2, 7, 7, 7),
(1288, 19, 2, 8, 8, 7),
(1289, 19, 2, 9, 9, 7),
(1290, 19, 2, 10, 10, 7),
(1291, 19, 2, 11, 11, 7),
(1292, 19, 2, 12, 12, 7),
(1293, 19, 3, 1, 1, 8),
(1294, 19, 3, 2, 2, 8),
(1295, 19, 3, 3, 3, 8),
(1296, 19, 3, 4, 4, 8),
(1297, 19, 3, 5, 5, 8),
(1298, 19, 3, 6, 6, 8),
(1299, 19, 3, 7, 7, 8),
(1300, 19, 3, 8, 8, 8),
(1301, 19, 3, 9, 9, 8),
(1302, 19, 3, 10, 10, 8),
(1303, 19, 3, 11, 11, 8),
(1304, 19, 3, 12, 12, 8),
(1305, 19, 4, 1, 1, 9),
(1306, 19, 4, 2, 2, 9),
(1307, 19, 4, 3, 3, 9),
(1308, 19, 4, 4, 4, 9),
(1309, 19, 4, 5, 5, 9),
(1310, 19, 4, 6, 6, 9),
(1311, 19, 4, 7, 7, 9),
(1312, 19, 4, 8, 8, 9),
(1313, 19, 4, 9, 9, 9),
(1314, 19, 4, 10, 10, 9),
(1315, 19, 4, 11, 11, 9),
(1316, 19, 4, 12, 12, 9),
(1317, 20, 6, 1, 1, 11),
(1318, 20, 6, 2, 2, 11),
(1319, 20, 6, 3, 3, 11),
(1320, 20, 6, 4, 4, 11),
(1321, 20, 6, 5, 5, 11),
(1322, 20, 6, 6, 6, 11),
(1323, 20, 6, 7, 7, 11),
(1324, 20, 6, 8, 8, 11),
(1325, 20, 6, 9, 9, 11),
(1326, 20, 6, 10, 10, 11),
(1327, 20, 6, 11, 11, 11),
(1328, 20, 6, 12, 12, 11),
(1329, 20, 7, 1, 1, 12),
(1330, 20, 7, 2, 2, 12),
(1331, 20, 7, 3, 3, 12),
(1332, 20, 7, 4, 4, 12),
(1333, 20, 7, 5, 5, 12),
(1334, 20, 7, 6, 6, 12),
(1335, 20, 7, 7, 7, 12),
(1336, 20, 7, 8, 8, 12),
(1337, 20, 7, 9, 9, 12),
(1338, 20, 7, 10, 10, 12),
(1339, 20, 7, 11, 11, 12),
(1340, 20, 7, 12, 12, 12),
(1341, 20, 8, 1, 1, 13),
(1342, 20, 8, 2, 2, 13),
(1343, 20, 8, 3, 3, 13),
(1344, 20, 8, 4, 4, 13),
(1345, 20, 8, 5, 5, 13),
(1346, 20, 8, 6, 6, 13),
(1347, 20, 8, 7, 7, 13),
(1348, 20, 8, 8, 8, 13),
(1349, 20, 8, 9, 9, 13),
(1350, 20, 8, 10, 10, 13),
(1351, 20, 8, 11, 11, 13),
(1352, 20, 8, 12, 12, 13),
(1353, 20, 9, 1, 1, 14),
(1354, 20, 9, 2, 2, 14),
(1355, 20, 9, 3, 3, 14),
(1356, 20, 9, 4, 4, 14),
(1357, 20, 9, 5, 5, 14),
(1358, 20, 9, 6, 6, 14),
(1359, 20, 9, 7, 7, 14),
(1360, 20, 9, 8, 8, 14),
(1361, 20, 9, 9, 9, 14),
(1362, 20, 9, 10, 10, 14),
(1363, 20, 9, 11, 11, 14),
(1364, 20, 9, 12, 12, 14),
(1365, 21, 11, 1, 1, 16),
(1366, 21, 11, 2, 2, 16),
(1367, 21, 11, 3, 3, 16),
(1368, 21, 11, 4, 4, 16),
(1369, 21, 11, 5, 5, 16),
(1370, 21, 11, 6, 6, 16),
(1371, 21, 11, 7, 7, 16),
(1372, 21, 11, 8, 8, 16),
(1373, 21, 11, 9, 9, 16),
(1374, 21, 11, 10, 10, 16),
(1375, 21, 11, 11, 11, 16),
(1376, 21, 11, 12, 12, 16),
(1377, 21, 12, 1, 1, 17),
(1378, 21, 12, 2, 2, 17),
(1379, 21, 12, 3, 3, 17),
(1380, 21, 12, 4, 4, 17),
(1381, 21, 12, 5, 5, 17),
(1382, 21, 12, 6, 6, 17),
(1383, 21, 12, 7, 7, 17),
(1384, 21, 12, 8, 8, 17),
(1385, 21, 12, 9, 9, 17),
(1386, 21, 12, 10, 10, 17),
(1387, 21, 12, 11, 11, 17),
(1388, 21, 12, 12, 12, 17),
(1389, 21, 13, 1, 1, 18),
(1390, 21, 13, 2, 2, 18),
(1391, 21, 13, 3, 3, 18),
(1392, 21, 13, 4, 4, 18),
(1393, 21, 13, 5, 5, 18),
(1394, 21, 13, 6, 6, 18),
(1395, 21, 13, 7, 7, 18),
(1396, 21, 13, 8, 8, 18),
(1397, 21, 13, 9, 9, 18),
(1398, 21, 13, 10, 10, 18),
(1399, 21, 13, 11, 11, 18),
(1400, 21, 13, 12, 12, 18),
(1401, 21, 14, 1, 1, 19),
(1402, 21, 14, 2, 2, 19),
(1403, 21, 14, 3, 3, 19),
(1404, 21, 14, 4, 4, 19),
(1405, 21, 14, 5, 5, 19),
(1406, 21, 14, 6, 6, 19),
(1407, 21, 14, 7, 7, 19),
(1408, 21, 14, 8, 8, 19),
(1409, 21, 14, 9, 9, 19),
(1410, 21, 14, 10, 10, 19),
(1411, 21, 14, 11, 11, 19),
(1412, 21, 14, 12, 12, 19),
(1413, 22, 15, 1, 1, 21),
(1414, 22, 15, 2, 2, 21),
(1415, 22, 15, 3, 3, 21),
(1416, 22, 15, 4, 4, 21),
(1417, 22, 15, 5, 5, 21),
(1418, 22, 15, 6, 6, 21),
(1419, 22, 15, 7, 7, 21),
(1420, 22, 15, 8, 8, 21),
(1421, 22, 15, 9, 9, 21),
(1422, 22, 15, 10, 10, 21),
(1423, 22, 15, 11, 11, 21),
(1424, 22, 15, 12, 12, 21),
(1425, 22, 16, 1, 1, 22),
(1426, 22, 16, 2, 2, 22),
(1427, 22, 16, 3, 3, 22),
(1428, 22, 16, 4, 4, 22),
(1429, 22, 16, 5, 5, 22),
(1430, 22, 16, 6, 6, 22),
(1431, 22, 16, 7, 7, 22),
(1432, 22, 16, 8, 8, 22),
(1433, 22, 16, 9, 9, 22),
(1434, 22, 16, 10, 10, 22),
(1435, 22, 16, 11, 11, 22),
(1436, 22, 16, 12, 12, 22),
(1437, 22, 17, 1, 1, 23),
(1438, 22, 17, 2, 2, 23),
(1439, 22, 17, 3, 3, 23),
(1440, 22, 17, 4, 4, 23),
(1441, 22, 17, 5, 5, 23),
(1442, 22, 17, 6, 6, 23),
(1443, 22, 17, 7, 7, 23),
(1444, 22, 17, 8, 8, 23),
(1445, 22, 17, 9, 9, 23),
(1446, 22, 17, 10, 10, 23),
(1447, 22, 17, 11, 11, 23),
(1448, 22, 17, 12, 12, 23),
(1449, 22, 18, 1, 1, 24),
(1450, 22, 18, 2, 2, 24);

INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(1451, 22, 18, 3, 3, 24),
(1452, 22, 18, 4, 4, 24),
(1453, 22, 18, 5, 5, 24),
(1454, 22, 18, 6, 6, 24),
(1455, 22, 18, 7, 7, 24),
(1456, 22, 18, 8, 8, 24),
(1457, 22, 18, 9, 9, 24),
(1458, 22, 18, 10, 10, 24),
(1459, 22, 18, 11, 11, 24),
(1460, 22, 18, 12, 12, 24),
(1461, 23, 1, 1, 39, 1),
(1462, 23, 2, 1, 40, 1),
(1463, 23, 3, 1, 41, 1),
(1464, 23, 4, 1, 42, 1),
(1465, 23, 5, 1, 43, 1),
(1466, 23, 6, 1, 44, 1),
(1467, 23, 7, 1, 45, 1),
(1468, 23, 8, 1, 46, 1),
(1469, 23, 9, 1, 47, 1),
(1470, 23, 10, 1, 48, 1),
(1471, 23, 11, 1, 49, 1),
(1472, 23, 12, 1, 50, 1),
(1473, 23, 1, 2, 39, 2),
(1474, 23, 2, 2, 40, 2),
(1475, 23, 3, 2, 41, 2),
(1476, 23, 4, 2, 42, 2),
(1477, 23, 5, 2, 43, 2),
(1478, 23, 6, 2, 44, 2),
(1479, 23, 7, 2, 45, 2),
(1480, 23, 8, 2, 46, 2),
(1481, 23, 9, 2, 47, 2),
(1482, 23, 10, 2, 48, 2),
(1483, 23, 11, 2, 49, 2),
(1484, 23, 12, 2, 50, 2),
(1485, 23, 1, 3, 39, 3),
(1486, 23, 2, 3, 40, 3),
(1487, 23, 3, 3, 41, 3),
(1488, 23, 4, 3, 42, 3),
(1489, 23, 5, 3, 43, 3),
(1490, 23, 6, 3, 44, 3),
(1491, 23, 7, 3, 45, 3),
(1492, 23, 8, 3, 46, 3),
(1493, 23, 9, 3, 47, 3),
(1494, 23, 10, 3, 48, 3),
(1495, 23, 11, 3, 49, 3),
(1496, 23, 12, 3, 50, 3),
(1497, 23, 1, 4, 39, 4),
(1498, 23, 2, 4, 40, 4),
(1499, 23, 3, 4, 41, 4),
(1500, 23, 4, 4, 42, 4),
(1501, 23, 5, 4, 43, 4),
(1502, 23, 6, 4, 44, 4),
(1503, 23, 7, 4, 45, 4),
(1504, 23, 8, 4, 46, 4),
(1505, 23, 9, 4, 47, 4),
(1506, 23, 10, 4, 48, 4),
(1507, 23, 11, 4, 49, 4),
(1508, 23, 12, 4, 50, 4),
(1509, 24, 1, 31, 39, 6),
(1510, 24, 1, 32, 40, 6),
(1511, 24, 1, 33, 41, 6),
(1512, 24, 1, 34, 42, 6),
(1513, 24, 1, 35, 43, 6),
(1514, 24, 1, 36, 44, 6),
(1515, 24, 1, 37, 45, 6),
(1516, 24, 1, 38, 46, 6),
(1517, 24, 1, 39, 47, 6),
(1518, 24, 1, 40, 48, 6),
(1519, 24, 1, 41, 49, 6),
(1520, 24, 1, 42, 50, 6),
(1521, 24, 2, 31, 39, 7),
(1522, 24, 2, 32, 40, 7),
(1523, 24, 2, 33, 41, 7),
(1524, 24, 2, 34, 42, 7),
(1525, 24, 2, 35, 43, 7),
(1526, 24, 2, 36, 44, 7),
(1527, 24, 2, 37, 45, 7),
(1528, 24, 2, 38, 46, 7),
(1529, 24, 2, 39, 47, 7),
(1530, 24, 2, 40, 48, 7),
(1531, 24, 2, 41, 49, 7),
(1532, 24, 2, 42, 50, 7),
(1533, 24, 3, 31, 39, 8),
(1534, 24, 3, 32, 40, 8),
(1535, 24, 3, 33, 41, 8),
(1536, 24, 3, 34, 42, 8),
(1537, 24, 3, 35, 43, 8),
(1538, 24, 3, 36, 44, 8),
(1539, 24, 3, 37, 45, 8),
(1540, 24, 3, 38, 46, 8),
(1541, 24, 3, 39, 47, 8),
(1542, 24, 3, 40, 48, 8),
(1543, 24, 3, 41, 49, 8),
(1544, 24, 3, 42, 50, 8),
(1545, 24, 4, 31, 39, 9),
(1546, 24, 4, 32, 40, 9),
(1547, 24, 4, 33, 41, 9),
(1548, 24, 4, 34, 42, 9),
(1549, 24, 4, 35, 43, 9),
(1550, 24, 4, 36, 44, 9),
(1551, 24, 4, 37, 45, 9),
(1552, 24, 4, 38, 46, 9),
(1553, 24, 4, 39, 47, 9),
(1554, 24, 4, 40, 48, 9),
(1555, 24, 4, 41, 49, 9),
(1556, 24, 4, 42, 50, 9),
(1557, 25, 6, 31, 39, 11),
(1558, 25, 6, 32, 40, 11),
(1559, 25, 6, 33, 41, 11),
(1560, 25, 6, 34, 42, 11),
(1561, 25, 6, 35, 43, 11),
(1562, 25, 6, 36, 44, 11),
(1563, 25, 6, 37, 45, 11),
(1564, 25, 6, 38, 46, 11),
(1565, 25, 6, 39, 47, 11),
(1566, 25, 6, 40, 48, 11),
(1567, 25, 6, 41, 49, 11),
(1568, 25, 6, 42, 50, 11),
(1569, 25, 7, 13, 39, 12),
(1570, 25, 7, 14, 40, 12),
(1571, 25, 7, 15, 41, 12),
(1572, 25, 7, 16, 42, 12),
(1573, 25, 7, 17, 43, 12),
(1574, 25, 7, 18, 44, 12),
(1575, 25, 7, 19, 45, 12),
(1576, 25, 7, 20, 46, 12),
(1577, 25, 7, 21, 47, 12),
(1578, 25, 7, 22, 48, 12),
(1579, 25, 7, 23, 49, 12),
(1580, 25, 7, 24, 50, 12),
(1581, 25, 8, 31, 39, 13),
(1582, 25, 8, 32, 40, 13),
(1583, 25, 8, 33, 41, 13),
(1584, 25, 8, 34, 42, 13),
(1585, 25, 8, 35, 43, 13),
(1586, 25, 8, 36, 44, 13),
(1587, 25, 8, 37, 45, 13),
(1588, 25, 8, 38, 46, 13),
(1589, 25, 8, 39, 47, 13),
(1590, 25, 8, 40, 48, 13),
(1591, 25, 8, 41, 49, 13),
(1592, 25, 8, 42, 50, 13),
(1593, 25, 9, 31, 39, 14),
(1594, 25, 9, 32, 40, 14),
(1595, 25, 9, 33, 41, 14),
(1596, 25, 9, 34, 42, 14),
(1597, 25, 9, 35, 43, 14),
(1598, 25, 9, 36, 44, 14),
(1599, 25, 9, 37, 45, 14),
(1600, 25, 9, 38, 46, 14),
(1601, 25, 9, 39, 47, 14),
(1602, 25, 9, 40, 48, 14),
(1603, 25, 9, 41, 49, 14),
(1604, 25, 9, 42, 50, 14),
(1605, 26, 11, 31, 39, 16),
(1606, 26, 11, 32, 40, 16),
(1607, 26, 11, 33, 41, 16),
(1608, 26, 11, 34, 42, 16),
(1609, 26, 11, 35, 43, 16),
(1610, 26, 11, 36, 44, 16),
(1611, 26, 11, 37, 45, 16),
(1612, 26, 11, 38, 46, 16),
(1613, 26, 11, 39, 47, 16),
(1614, 26, 11, 40, 48, 16),
(1615, 26, 11, 41, 49, 16),
(1616, 26, 11, 42, 50, 16),
(1617, 26, 12, 31, 39, 17),
(1618, 26, 12, 32, 40, 17),
(1619, 26, 12, 33, 41, 17),
(1620, 26, 12, 34, 42, 17),
(1621, 26, 12, 35, 43, 17),
(1622, 26, 12, 36, 44, 17),
(1623, 26, 12, 37, 45, 17),
(1624, 26, 12, 38, 46, 17),
(1625, 26, 12, 39, 47, 17),
(1626, 26, 12, 40, 48, 17),
(1627, 26, 12, 41, 49, 17),
(1628, 26, 12, 42, 50, 17),
(1629, 26, 13, 31, 39, 18),
(1630, 26, 13, 32, 40, 18),
(1631, 26, 13, 33, 41, 18),
(1632, 26, 13, 34, 42, 18),
(1633, 26, 13, 35, 43, 18),
(1634, 26, 13, 36, 44, 18),
(1635, 26, 13, 37, 45, 18),
(1636, 26, 13, 38, 46, 18),
(1637, 26, 13, 39, 47, 18),
(1638, 26, 13, 40, 48, 18),
(1639, 26, 13, 41, 49, 18),
(1640, 26, 13, 42, 50, 18),
(1641, 26, 14, 31, 39, 19),
(1642, 26, 14, 32, 40, 19),
(1643, 26, 14, 33, 41, 19),
(1644, 26, 14, 34, 42, 19),
(1645, 26, 14, 35, 43, 19),
(1646, 26, 14, 36, 44, 19),
(1647, 26, 14, 37, 45, 19),
(1648, 26, 14, 38, 46, 19),
(1649, 26, 14, 39, 47, 19),
(1650, 26, 14, 40, 48, 19),
(1651, 26, 14, 41, 49, 19),
(1652, 26, 14, 42, 50, 19),
(1653, 27, 15, 25, 39, 21),
(1654, 27, 15, 26, 40, 21),
(1655, 27, 15, 27, 41, 21),
(1656, 27, 15, 28, 42, 21),
(1657, 27, 15, 29, 43, 21),
(1658, 27, 15, 30, 44, 21),
(1659, 27, 15, 31, 45, 21),
(1660, 27, 15, 32, 46, 21),
(1661, 27, 15, 33, 47, 21),
(1662, 27, 15, 34, 48, 21),
(1663, 27, 15, 35, 49, 21),
(1664, 27, 15, 36, 50, 21),
(1665, 27, 16, 25, 39, 22),
(1666, 27, 16, 26, 40, 22),
(1667, 27, 16, 27, 41, 22),
(1668, 27, 16, 28, 42, 22),
(1669, 27, 16, 29, 43, 22),
(1670, 27, 16, 30, 44, 22),
(1671, 27, 16, 31, 45, 22),
(1672, 27, 16, 32, 46, 22),
(1673, 27, 16, 33, 47, 22),
(1674, 27, 16, 34, 48, 22),
(1675, 27, 16, 35, 49, 22),
(1676, 27, 16, 36, 50, 22),
(1677, 27, 17, 25, 39, 23),
(1678, 27, 17, 26, 40, 23),
(1679, 27, 17, 27, 41, 23),
(1680, 27, 17, 28, 42, 23),
(1681, 27, 17, 29, 43, 23),
(1682, 27, 17, 30, 44, 23),
(1683, 27, 17, 31, 45, 23),
(1684, 27, 17, 32, 46, 23),
(1685, 27, 17, 33, 47, 23),
(1686, 27, 17, 34, 48, 23),
(1687, 27, 17, 35, 49, 23),
(1688, 27, 17, 36, 50, 23),
(1689, 27, 18, 25, 39, 24),
(1690, 27, 18, 26, 40, 24),
(1691, 27, 18, 27, 41, 24),
(1692, 27, 18, 28, 42, 24),
(1693, 27, 18, 29, 43, 24),
(1694, 27, 18, 30, 44, 24),
(1695, 27, 18, 31, 45, 24),
(1696, 27, 18, 32, 46, 24),
(1697, 27, 18, 33, 47, 24),
(1698, 27, 18, 34, 48, 24),
(1699, 27, 18, 35, 49, 24),
(1700, 27, 18, 36, 50, 24),
(1701, 28, 1, 13, 16, 6),
(1702, 28, 1, 14, 17, 6),
(1703, 28, 1, 15, 18, 6),
(1704, 28, 1, 16, 19, 6),
(1705, 28, 1, 17, 20, 6),
(1706, 28, 2, 13, 16, 7),
(1707, 28, 2, 14, 17, 7),
(1708, 28, 2, 15, 18, 7),
(1709, 28, 2, 16, 19, 7),
(1710, 28, 2, 17, 20, 7),
(1711, 28, 3, 13, 16, 8),
(1712, 28, 3, 14, 17, 8),
(1713, 28, 3, 15, 18, 8),
(1714, 28, 3, 16, 19, 8),
(1715, 28, 3, 17, 20, 8),
(1716, 28, 4, 13, 16, 9),
(1717, 28, 4, 14, 17, 9),
(1718, 28, 4, 15, 18, 9),
(1719, 28, 4, 16, 19, 9),
(1720, 28, 4, 17, 20, 9),
(1721, 28, 5, 1, 16, 10),
(1722, 28, 5, 2, 17, 10),
(1723, 28, 5, 3, 18, 10),
(1724, 28, 5, 4, 19, 10),
(1725, 28, 5, 5, 20, 10),
(1726, 28, 6, 13, 16, 11),
(1727, 28, 6, 14, 17, 11),
(1728, 28, 6, 15, 18, 11),
(1729, 28, 6, 16, 19, 11),
(1730, 28, 6, 17, 20, 11),
(1731, 29, 1, 18, 22, 6),
(1732, 29, 1, 19, 23, 6),
(1733, 29, 1, 20, 24, 6),
(1734, 29, 1, 21, 25, 6),
(1735, 29, 1, 22, 26, 6),
(1736, 29, 1, 23, 27, 6),
(1737, 29, 1, 24, 28, 6),
(1738, 29, 1, 25, 29, 6),
(1739, 29, 2, 18, 22, 7),
(1740, 29, 2, 19, 23, 7),
(1741, 29, 2, 20, 24, 7),
(1742, 29, 2, 21, 25, 7),
(1743, 29, 2, 22, 26, 7),
(1744, 29, 2, 23, 27, 7),
(1745, 29, 2, 24, 28, 7),
(1746, 29, 2, 25, 29, 7),
(1747, 29, 3, 18, 22, 8),
(1748, 29, 3, 19, 23, 8),
(1749, 29, 3, 20, 24, 8),
(1750, 29, 3, 21, 25, 8),
(1751, 29, 3, 22, 26, 8),
(1752, 29, 3, 23, 27, 8),
(1753, 29, 3, 24, 28, 8),
(1754, 29, 3, 25, 29, 8),
(1755, 29, 4, 18, 22, 9),
(1756, 29, 4, 19, 23, 9),
(1757, 29, 4, 20, 24, 9),
(1758, 29, 4, 21, 25, 9),
(1759, 29, 4, 22, 26, 9),
(1760, 29, 4, 23, 27, 9),
(1761, 29, 4, 24, 28, 9),
(1762, 29, 4, 25, 29, 9),
(1763, 29, 5, 6, 22, 10),
(1764, 29, 5, 7, 23, 10),
(1765, 29, 5, 8, 24, 10),
(1766, 29, 5, 9, 25, 10),
(1767, 29, 5, 10, 26, 10),
(1768, 29, 5, 11, 27, 10),
(1769, 29, 5, 12, 28, 10),
(1770, 29, 5, 13, 29, 10),
(1771, 29, 6, 18, 22, 11),
(1772, 29, 6, 19, 23, 11),
(1773, 29, 6, 20, 24, 11),
(1774, 29, 6, 21, 25, 11),
(1775, 29, 6, 22, 26, 11),
(1776, 29, 6, 23, 27, 11),
(1777, 29, 6, 24, 28, 11),
(1778, 29, 6, 25, 29, 11),
(1779, 30, 1, 26, 31, 6),
(1780, 30, 1, 27, 32, 6),
(1781, 30, 1, 28, 33, 6),
(1782, 30, 1, 29, 34, 6),
(1783, 30, 1, 30, 35, 6),
(1784, 30, 2, 26, 31, 7),
(1785, 30, 2, 27, 32, 7),
(1786, 30, 2, 28, 33, 7),
(1787, 30, 2, 29, 34, 7),
(1788, 30, 2, 30, 35, 7),
(1789, 30, 3, 26, 31, 8),
(1790, 30, 3, 27, 32, 8),
(1791, 30, 3, 28, 33, 8),
(1792, 30, 3, 29, 34, 8),
(1793, 30, 3, 30, 35, 8),
(1794, 30, 4, 26, 31, 9),
(1795, 30, 4, 27, 32, 9),
(1796, 30, 4, 28, 33, 9),
(1797, 30, 4, 29, 34, 9),
(1798, 30, 4, 30, 35, 9),
(1799, 30, 5, 14, 31, 10),
(1800, 30, 5, 15, 32, 10),
(1801, 30, 5, 16, 33, 10),
(1802, 30, 5, 17, 34, 10),
(1803, 30, 5, 18, 35, 10),
(1804, 30, 6, 26, 31, 11),
(1805, 30, 6, 27, 32, 11),
(1806, 30, 6, 28, 33, 11),
(1807, 30, 6, 29, 34, 11),
(1808, 30, 6, 30, 35, 11),
(1809, 31, 8, 13, 16, 13),
(1810, 31, 8, 14, 17, 13),
(1811, 31, 8, 15, 18, 13),
(1812, 31, 8, 16, 19, 13),
(1813, 31, 8, 17, 20, 13),
(1814, 31, 9, 13, 16, 14),
(1815, 31, 9, 14, 17, 14),
(1816, 31, 9, 15, 18, 14),
(1817, 31, 9, 16, 19, 14),
(1818, 31, 9, 17, 20, 14),
(1819, 31, 10, 1, 16, 15),
(1820, 31, 10, 2, 17, 15),
(1821, 31, 10, 3, 18, 15),
(1822, 31, 10, 4, 19, 15),
(1823, 31, 10, 5, 20, 15),
(1824, 31, 11, 13, 16, 16),
(1825, 31, 11, 14, 17, 16),
(1826, 31, 11, 15, 18, 16),
(1827, 31, 11, 16, 19, 16),
(1828, 31, 11, 17, 20, 16),
(1829, 31, 12, 13, 16, 17),
(1830, 31, 12, 14, 17, 17),
(1831, 31, 12, 15, 18, 17),
(1832, 31, 12, 16, 19, 17),
(1833, 31, 12, 17, 20, 17),
(1834, 31, 13, 13, 16, 18),
(1835, 31, 13, 14, 17, 18),
(1836, 31, 13, 15, 18, 18),
(1837, 31, 13, 16, 19, 18),
(1838, 31, 13, 17, 20, 18),
(1839, 31, 14, 13, 16, 19),
(1840, 31, 14, 14, 17, 19),
(1841, 31, 14, 15, 18, 19),
(1842, 31, 14, 16, 19, 19),
(1843, 31, 14, 17, 20, 19),
(1844, 32, 8, 18, 22, 13),
(1845, 32, 8, 19, 23, 13),
(1846, 32, 8, 20, 24, 13),
(1847, 32, 8, 21, 25, 13),
(1848, 32, 8, 22, 26, 13),
(1849, 32, 8, 23, 27, 13),
(1850, 32, 8, 24, 28, 13),
(1851, 32, 8, 25, 29, 13),
(1852, 32, 9, 18, 22, 14),
(1853, 32, 9, 19, 23, 14),
(1854, 32, 9, 20, 24, 14),
(1855, 32, 9, 21, 25, 14),
(1856, 32, 9, 22, 26, 14),
(1857, 32, 9, 23, 27, 14),
(1858, 32, 9, 24, 28, 14),
(1859, 32, 9, 25, 29, 14),
(1860, 32, 10, 6, 22, 15),
(1861, 32, 10, 7, 23, 15),
(1862, 32, 10, 8, 24, 15),
(1863, 32, 10, 9, 25, 15),
(1864, 32, 10, 10, 26, 15),
(1865, 32, 10, 11, 27, 15),
(1866, 32, 10, 12, 28, 15),
(1867, 32, 10, 13, 29, 15),
(1868, 32, 11, 18, 22, 16),
(1869, 32, 11, 19, 23, 16),
(1870, 32, 11, 20, 24, 16),
(1871, 32, 11, 21, 25, 16),
(1872, 32, 11, 22, 26, 16),
(1873, 32, 11, 23, 27, 16),
(1874, 32, 11, 24, 28, 16),
(1875, 32, 11, 25, 29, 16),
(1876, 32, 12, 18, 22, 17),
(1877, 32, 12, 19, 23, 17),
(1878, 32, 12, 20, 24, 17),
(1879, 32, 12, 21, 25, 17),
(1880, 32, 12, 22, 26, 17),
(1881, 32, 12, 23, 27, 17),
(1882, 32, 12, 24, 28, 17),
(1883, 32, 12, 25, 29, 17),
(1884, 32, 13, 18, 22, 18),
(1885, 32, 13, 19, 23, 18),
(1886, 32, 13, 20, 24, 18),
(1887, 32, 13, 21, 25, 18),
(1888, 32, 13, 22, 26, 18),
(1889, 32, 13, 23, 27, 18),
(1890, 32, 13, 24, 28, 18),
(1891, 32, 13, 25, 29, 18),
(1892, 32, 14, 18, 22, 19),
(1893, 32, 14, 19, 23, 19),
(1894, 32, 14, 20, 24, 19),
(1895, 32, 14, 21, 25, 19),
(1896, 32, 14, 22, 26, 19),
(1897, 32, 14, 23, 27, 19),
(1898, 32, 14, 24, 28, 19),
(1899, 32, 14, 25, 29, 19),
(1900, 33, 8, 26, 31, 13),
(1901, 33, 8, 27, 32, 13),
(1902, 33, 8, 28, 33, 13),
(1903, 33, 8, 29, 34, 13),
(1904, 33, 8, 30, 35, 13),
(1905, 33, 9, 26, 31, 14),
(1906, 33, 9, 27, 32, 14),
(1907, 33, 9, 28, 33, 14),
(1908, 33, 9, 29, 34, 14),
(1909, 33, 9, 30, 35, 14),
(1910, 33, 10, 14, 31, 15),
(1911, 33, 10, 15, 32, 15),
(1912, 33, 10, 16, 33, 15),
(1913, 33, 10, 17, 34, 15),
(1914, 33, 10, 18, 35, 15),
(1915, 33, 11, 26, 31, 16),
(1916, 33, 11, 27, 32, 16),
(1917, 33, 11, 28, 33, 16),
(1918, 33, 11, 29, 34, 16),
(1919, 33, 11, 30, 35, 16),
(1920, 33, 12, 26, 31, 17),
(1921, 33, 12, 27, 32, 17),
(1922, 33, 12, 28, 33, 17),
(1923, 33, 12, 29, 34, 17),
(1924, 33, 12, 30, 35, 17),
(1925, 33, 13, 26, 31, 18),
(1926, 33, 13, 27, 32, 18),
(1927, 33, 13, 28, 33, 18),
(1928, 33, 13, 29, 34, 18),
(1929, 33, 13, 30, 35, 18),
(1930, 33, 14, 26, 31, 19),
(1931, 33, 14, 27, 32, 19),
(1932, 33, 14, 28, 33, 19),
(1933, 33, 14, 29, 34, 19),
(1934, 33, 14, 30, 35, 19),
(1935, 34, 15, 13, 20, 21),
(1936, 34, 15, 14, 21, 21),
(1937, 34, 15, 15, 22, 21),
(1938, 34, 15, 16, 23, 21),
(1939, 34, 15, 17, 24, 21),
(1940, 34, 15, 18, 25, 21),
(1941, 34, 15, 19, 26, 21),
(1942, 34, 15, 20, 27, 21),
(1943, 34, 15, 21, 28, 21),
(1944, 34, 15, 22, 29, 21),
(1945, 34, 15, 23, 30, 21),
(1946, 34, 15, 24, 31, 21),
(1947, 34, 16, 13, 20, 22),
(1948, 34, 16, 14, 21, 22),
(1949, 34, 16, 15, 22, 22),
(1950, 34, 16, 16, 23, 22);

INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(1951, 34, 16, 17, 24, 22),
(1952, 34, 16, 18, 25, 22),
(1953, 34, 16, 19, 26, 22),
(1954, 34, 16, 20, 27, 22),
(1955, 34, 16, 21, 28, 22),
(1956, 34, 16, 22, 29, 22),
(1957, 34, 16, 23, 30, 22),
(1958, 34, 16, 24, 31, 22),
(1959, 34, 17, 13, 20, 23),
(1960, 34, 17, 14, 21, 23),
(1961, 34, 17, 15, 22, 23),
(1962, 34, 17, 16, 23, 23),
(1963, 34, 17, 17, 24, 23),
(1964, 34, 17, 18, 25, 23),
(1965, 34, 17, 19, 26, 23),
(1966, 34, 17, 20, 27, 23),
(1967, 34, 17, 21, 28, 23),
(1968, 34, 17, 22, 29, 23),
(1969, 34, 17, 23, 30, 23),
(1970, 34, 17, 24, 31, 23),
(1971, 34, 18, 13, 20, 24),
(1972, 34, 18, 14, 21, 24),
(1973, 34, 18, 15, 22, 24),
(1974, 34, 18, 16, 23, 24),
(1975, 34, 18, 17, 24, 24),
(1976, 34, 18, 18, 25, 24),
(1977, 34, 18, 19, 26, 24),
(1978, 34, 18, 20, 27, 24),
(1979, 34, 18, 21, 28, 24),
(1980, 34, 18, 22, 29, 24),
(1981, 34, 18, 23, 30, 24),
(1982, 34, 18, 24, 31, 24),
(1983, 35, 19, 30, 44, 27),
(1984, 35, 19, 31, 45, 27),
(1985, 35, 19, 32, 46, 27),
(1986, 35, 19, 33, 47, 27),
(1987, 35, 19, 34, 48, 27),
(1988, 35, 20, 30, 44, 28),
(1989, 35, 20, 31, 45, 28),
(1990, 35, 20, 32, 46, 28),
(1991, 35, 20, 33, 47, 28),
(1992, 35, 20, 34, 48, 28),
(1993, 35, 21, 30, 44, 29),
(1994, 35, 21, 31, 45, 29),
(1995, 35, 21, 32, 46, 29),
(1996, 35, 21, 33, 47, 29),
(1997, 35, 21, 34, 48, 29),
(1998, 35, 22, 30, 44, 30),
(1999, 35, 22, 31, 45, 30),
(2000, 35, 22, 32, 46, 30),
(2001, 35, 22, 33, 47, 30),
(2002, 35, 22, 34, 48, 30),
(2003, 35, 23, 30, 44, 31),
(2004, 35, 23, 31, 45, 31),
(2005, 35, 23, 32, 46, 31),
(2006, 35, 23, 33, 47, 31),
(2007, 35, 23, 34, 48, 31),
(2008, 35, 24, 30, 44, 32),
(2009, 35, 24, 31, 45, 32),
(2010, 35, 24, 32, 46, 32),
(2011, 35, 24, 33, 47, 32),
(2012, 35, 24, 34, 48, 32),
(2013, 35, 25, 30, 44, 33),
(2014, 35, 25, 31, 45, 33),
(2015, 35, 25, 32, 46, 33),
(2016, 35, 25, 33, 47, 33),
(2017, 35, 25, 34, 48, 33),
(2018, 35, 26, 30, 44, 34),
(2019, 35, 26, 31, 45, 34),
(2020, 35, 26, 32, 46, 34),
(2021, 35, 26, 33, 47, 34),
(2022, 35, 26, 34, 48, 34),
(2023, 36, 19, 24, 35, 27),
(2024, 36, 19, 25, 36, 27),
(2025, 36, 19, 26, 37, 27),
(2026, 36, 19, 27, 38, 27),
(2027, 36, 19, 28, 39, 27),
(2028, 36, 19, 29, 40, 27),
(2029, 36, 20, 24, 35, 28),
(2030, 36, 20, 25, 36, 28),
(2031, 36, 20, 26, 37, 28),
(2032, 36, 20, 27, 38, 28),
(2033, 36, 20, 28, 39, 28),
(2034, 36, 20, 29, 40, 28),
(2035, 36, 21, 24, 35, 29),
(2036, 36, 21, 25, 36, 29),
(2037, 36, 21, 26, 37, 29),
(2038, 36, 21, 27, 38, 29),
(2039, 36, 21, 28, 39, 29),
(2040, 36, 21, 29, 40, 29),
(2041, 36, 22, 24, 35, 30),
(2042, 36, 22, 25, 36, 30),
(2043, 36, 22, 26, 37, 30),
(2044, 36, 22, 27, 38, 30),
(2045, 36, 22, 28, 39, 30),
(2046, 36, 22, 29, 40, 30),
(2047, 36, 23, 24, 35, 31),
(2048, 36, 23, 25, 36, 31),
(2049, 36, 23, 26, 37, 31),
(2050, 36, 23, 27, 38, 31),
(2051, 36, 23, 28, 39, 31),
(2052, 36, 23, 29, 40, 31),
(2053, 36, 24, 24, 35, 32),
(2054, 36, 24, 25, 36, 32),
(2055, 36, 24, 26, 37, 32),
(2056, 36, 24, 27, 38, 32),
(2057, 36, 24, 28, 39, 32),
(2058, 36, 24, 29, 40, 32),
(2059, 36, 25, 24, 35, 33),
(2060, 36, 25, 25, 36, 33),
(2061, 36, 25, 26, 37, 33),
(2062, 36, 25, 27, 38, 33),
(2063, 36, 25, 28, 39, 33),
(2064, 36, 25, 29, 40, 33),
(2065, 36, 26, 24, 35, 34),
(2066, 36, 26, 25, 36, 34),
(2067, 36, 26, 26, 37, 34),
(2068, 36, 26, 27, 38, 34),
(2069, 36, 26, 28, 39, 34),
(2070, 36, 26, 29, 40, 34),
(2071, 37, 19, 12, 20, 27),
(2072, 37, 19, 13, 21, 27),
(2073, 37, 19, 14, 22, 27),
(2074, 37, 19, 15, 23, 27),
(2075, 37, 19, 16, 24, 27),
(2076, 37, 19, 17, 25, 27),
(2077, 37, 19, 18, 26, 27),
(2078, 37, 19, 19, 27, 27),
(2079, 37, 19, 20, 28, 27),
(2080, 37, 19, 21, 29, 27),
(2081, 37, 19, 22, 30, 27),
(2082, 37, 19, 23, 31, 27),
(2083, 37, 20, 12, 20, 28),
(2084, 37, 20, 13, 21, 28),
(2085, 37, 20, 14, 22, 28),
(2086, 37, 20, 15, 23, 28),
(2087, 37, 20, 16, 24, 28),
(2088, 37, 20, 17, 25, 28),
(2089, 37, 20, 18, 26, 28),
(2090, 37, 20, 19, 27, 28),
(2091, 37, 20, 20, 28, 28),
(2092, 37, 20, 21, 29, 28),
(2093, 37, 20, 22, 30, 28),
(2094, 37, 20, 23, 31, 28),
(2095, 37, 21, 12, 20, 29),
(2096, 37, 21, 13, 21, 29),
(2097, 37, 21, 14, 22, 29),
(2098, 37, 21, 15, 23, 29),
(2099, 37, 21, 16, 24, 29),
(2100, 37, 21, 17, 25, 29),
(2101, 37, 21, 18, 26, 29),
(2102, 37, 21, 19, 27, 29),
(2103, 37, 21, 20, 28, 29),
(2104, 37, 21, 21, 29, 29),
(2105, 37, 21, 22, 30, 29),
(2106, 37, 21, 23, 31, 29),
(2107, 37, 22, 12, 20, 30),
(2108, 37, 22, 13, 21, 30),
(2109, 37, 22, 14, 22, 30),
(2110, 37, 22, 15, 23, 30),
(2111, 37, 22, 16, 24, 30),
(2112, 37, 22, 17, 25, 30),
(2113, 37, 22, 18, 26, 30),
(2114, 37, 22, 19, 27, 30),
(2115, 37, 22, 20, 28, 30),
(2116, 37, 22, 21, 29, 30),
(2117, 37, 22, 22, 30, 30),
(2118, 37, 22, 23, 31, 30),
(2119, 37, 23, 12, 20, 31),
(2120, 37, 23, 13, 21, 31),
(2121, 37, 23, 14, 22, 31),
(2122, 37, 23, 15, 23, 31),
(2123, 37, 23, 16, 24, 31),
(2124, 37, 23, 17, 25, 31),
(2125, 37, 23, 18, 26, 31),
(2126, 37, 23, 19, 27, 31),
(2127, 37, 23, 20, 28, 31),
(2128, 37, 23, 21, 29, 31),
(2129, 37, 23, 22, 30, 31),
(2130, 37, 23, 23, 31, 31),
(2131, 37, 24, 12, 20, 32),
(2132, 37, 24, 13, 21, 32),
(2133, 37, 24, 14, 22, 32),
(2134, 37, 24, 15, 23, 32),
(2135, 37, 24, 16, 24, 32),
(2136, 37, 24, 17, 25, 32),
(2137, 37, 24, 18, 26, 32),
(2138, 37, 24, 19, 27, 32),
(2139, 37, 24, 20, 28, 32),
(2140, 37, 24, 21, 29, 32),
(2141, 37, 24, 22, 30, 32),
(2142, 37, 24, 23, 31, 32),
(2143, 37, 25, 12, 20, 33),
(2144, 37, 25, 13, 21, 33),
(2145, 37, 25, 14, 22, 33),
(2146, 37, 25, 15, 23, 33),
(2147, 37, 25, 16, 24, 33),
(2148, 37, 25, 17, 25, 33),
(2149, 37, 25, 18, 26, 33),
(2150, 37, 25, 19, 27, 33),
(2151, 37, 25, 20, 28, 33),
(2152, 37, 25, 21, 29, 33),
(2153, 37, 25, 22, 30, 33),
(2154, 37, 25, 23, 31, 33),
(2155, 37, 26, 12, 20, 34),
(2156, 37, 26, 13, 21, 34),
(2157, 37, 26, 14, 22, 34),
(2158, 37, 26, 15, 23, 34),
(2159, 37, 26, 16, 24, 34),
(2160, 37, 26, 17, 25, 34),
(2161, 37, 26, 18, 26, 34),
(2162, 37, 26, 19, 27, 34),
(2163, 37, 26, 20, 28, 34),
(2164, 37, 26, 21, 29, 34),
(2165, 37, 26, 22, 30, 34),
(2166, 37, 26, 23, 31, 34),
(2167, 38, 19, 6, 11, 27),
(2168, 38, 19, 7, 12, 27),
(2169, 38, 19, 8, 13, 27),
(2170, 38, 19, 9, 14, 27),
(2171, 38, 19, 10, 15, 27),
(2172, 38, 19, 11, 16, 27),
(2173, 38, 20, 6, 11, 28),
(2174, 38, 20, 7, 12, 28),
(2175, 38, 20, 8, 13, 28),
(2176, 38, 20, 9, 14, 28),
(2177, 38, 20, 10, 15, 28),
(2178, 38, 20, 11, 16, 28),
(2179, 38, 21, 6, 11, 29),
(2180, 38, 21, 7, 12, 29),
(2181, 38, 21, 8, 13, 29),
(2182, 38, 21, 9, 14, 29),
(2183, 38, 21, 10, 15, 29),
(2184, 38, 21, 11, 16, 29),
(2185, 38, 22, 6, 11, 30),
(2186, 38, 22, 7, 12, 30),
(2187, 38, 22, 8, 13, 30),
(2188, 38, 22, 9, 14, 30),
(2189, 38, 22, 10, 15, 30),
(2190, 38, 22, 11, 16, 30),
(2191, 38, 23, 6, 11, 31),
(2192, 38, 23, 7, 12, 31),
(2193, 38, 23, 8, 13, 31),
(2194, 38, 23, 9, 14, 31),
(2195, 38, 23, 10, 15, 31),
(2196, 38, 23, 11, 16, 31),
(2197, 38, 24, 6, 11, 32),
(2198, 38, 24, 7, 12, 32),
(2199, 38, 24, 8, 13, 32),
(2200, 38, 24, 9, 14, 32),
(2201, 38, 24, 10, 15, 32),
(2202, 38, 24, 11, 16, 32),
(2203, 38, 25, 6, 11, 33),
(2204, 38, 25, 7, 12, 33),
(2205, 38, 25, 8, 13, 33),
(2206, 38, 25, 9, 14, 33),
(2207, 38, 25, 10, 15, 33),
(2208, 38, 25, 11, 16, 33),
(2209, 38, 26, 6, 11, 34),
(2210, 38, 26, 7, 12, 34),
(2211, 38, 26, 8, 13, 34),
(2212, 38, 26, 9, 14, 34),
(2213, 38, 26, 10, 15, 34),
(2214, 38, 26, 11, 16, 34),
(2215, 39, 19, 1, 3, 27),
(2216, 39, 19, 2, 4, 27),
(2217, 39, 19, 3, 5, 27),
(2218, 39, 19, 4, 6, 27),
(2219, 39, 19, 5, 7, 27),
(2220, 39, 20, 1, 3, 28),
(2221, 39, 20, 2, 4, 28),
(2222, 39, 20, 3, 5, 28),
(2223, 39, 20, 4, 6, 28),
(2224, 39, 20, 5, 7, 28),
(2225, 39, 21, 1, 3, 29),
(2226, 39, 21, 2, 4, 29),
(2227, 39, 21, 3, 5, 29),
(2228, 39, 21, 4, 6, 29),
(2229, 39, 21, 5, 7, 29),
(2230, 39, 22, 1, 3, 30),
(2231, 39, 22, 2, 4, 30),
(2232, 39, 22, 3, 5, 30),
(2233, 39, 22, 4, 6, 30),
(2234, 39, 22, 5, 7, 30),
(2235, 39, 23, 1, 3, 31),
(2236, 39, 23, 2, 4, 31),
(2237, 39, 23, 3, 5, 31),
(2238, 39, 23, 4, 6, 31),
(2239, 39, 23, 5, 7, 31),
(2240, 39, 24, 1, 3, 32),
(2241, 39, 24, 2, 4, 32),
(2242, 39, 24, 3, 5, 32),
(2243, 39, 24, 4, 6, 32),
(2244, 39, 24, 5, 7, 32),
(2245, 39, 25, 1, 3, 33),
(2246, 39, 25, 2, 4, 33),
(2247, 39, 25, 3, 5, 33),
(2248, 39, 25, 4, 6, 33),
(2249, 39, 25, 5, 7, 33),
(2250, 39, 26, 1, 3, 34),
(2251, 39, 26, 2, 4, 34),
(2252, 39, 26, 3, 5, 34),
(2253, 39, 26, 4, 6, 34),
(2254, 39, 26, 5, 7, 34),
(2255, 40, 27, 28, 47, 38),
(2256, 40, 27, 29, 48, 38),
(2257, 40, 27, 30, 49, 38),
(2258, 40, 28, 28, 47, 39),
(2259, 40, 28, 29, 48, 39),
(2260, 40, 28, 30, 49, 39),
(2261, 40, 29, 28, 47, 40),
(2262, 40, 29, 29, 48, 40),
(2263, 40, 29, 30, 49, 40),
(2264, 40, 30, 28, 47, 41),
(2265, 40, 30, 29, 48, 41),
(2266, 40, 30, 30, 49, 41),
(2267, 40, 31, 28, 47, 42),
(2268, 40, 31, 29, 48, 42),
(2269, 40, 31, 30, 49, 42),
(2270, 40, 32, 28, 47, 43),
(2271, 40, 32, 29, 48, 43),
(2272, 40, 32, 30, 49, 43),
(2273, 40, 33, 28, 47, 44),
(2274, 40, 33, 29, 48, 44),
(2275, 40, 33, 30, 49, 44),
(2276, 40, 34, 28, 47, 45),
(2277, 40, 34, 29, 48, 45),
(2278, 40, 34, 30, 49, 45),
(2279, 40, 35, 28, 47, 46),
(2280, 40, 35, 29, 48, 46),
(2281, 40, 35, 30, 49, 46),
(2282, 41, 27, 25, 42, 38),
(2283, 41, 27, 26, 43, 38),
(2284, 41, 27, 27, 44, 38),
(2285, 41, 28, 25, 42, 39),
(2286, 41, 28, 26, 43, 39),
(2287, 41, 28, 27, 44, 39),
(2288, 41, 29, 25, 42, 40),
(2289, 41, 29, 26, 43, 40),
(2290, 41, 29, 27, 44, 40),
(2291, 41, 30, 25, 42, 41),
(2292, 41, 30, 26, 43, 41),
(2293, 41, 30, 27, 44, 41),
(2294, 41, 31, 25, 42, 42),
(2295, 41, 31, 26, 43, 42),
(2296, 41, 31, 27, 44, 42),
(2297, 41, 32, 25, 42, 43),
(2298, 41, 32, 26, 43, 43),
(2299, 41, 32, 27, 44, 43),
(2300, 41, 33, 25, 42, 44),
(2301, 41, 33, 26, 43, 44),
(2302, 41, 33, 27, 44, 44),
(2303, 41, 34, 25, 42, 45),
(2304, 41, 34, 26, 43, 45),
(2305, 41, 34, 27, 44, 45),
(2306, 41, 35, 25, 42, 46),
(2307, 41, 35, 26, 43, 46),
(2308, 41, 35, 27, 44, 46),
(2309, 42, 27, 22, 37, 38),
(2310, 42, 27, 23, 38, 38),
(2311, 42, 27, 24, 39, 38),
(2312, 42, 28, 22, 37, 39),
(2313, 42, 28, 23, 38, 39),
(2314, 42, 28, 24, 39, 39),
(2315, 42, 29, 22, 37, 40),
(2316, 42, 29, 23, 38, 40),
(2317, 42, 29, 24, 39, 40),
(2318, 42, 30, 22, 37, 41),
(2319, 42, 30, 23, 38, 41),
(2320, 42, 30, 24, 39, 41),
(2321, 42, 31, 22, 37, 42),
(2322, 42, 31, 23, 38, 42),
(2323, 42, 31, 24, 39, 42),
(2324, 42, 32, 22, 37, 43),
(2325, 42, 32, 23, 38, 43),
(2326, 42, 32, 24, 39, 43),
(2327, 42, 33, 22, 37, 44),
(2328, 42, 33, 23, 38, 44),
(2329, 42, 33, 24, 39, 44),
(2330, 42, 34, 22, 37, 45),
(2331, 42, 34, 23, 38, 45),
(2332, 42, 34, 24, 39, 45),
(2333, 42, 35, 22, 37, 46),
(2334, 42, 35, 23, 38, 46),
(2335, 42, 35, 24, 39, 46),
(2336, 43, 27, 19, 32, 38),
(2337, 43, 27, 20, 33, 38),
(2338, 43, 27, 21, 34, 38),
(2339, 43, 28, 19, 32, 39),
(2340, 43, 28, 20, 33, 39),
(2341, 43, 28, 21, 34, 39),
(2342, 43, 29, 19, 32, 40),
(2343, 43, 29, 20, 33, 40),
(2344, 43, 29, 21, 34, 40),
(2345, 43, 30, 19, 32, 41),
(2346, 43, 30, 20, 33, 41),
(2347, 43, 30, 21, 34, 41),
(2348, 43, 31, 19, 32, 42),
(2349, 43, 31, 20, 33, 42),
(2350, 43, 31, 21, 34, 42),
(2351, 43, 32, 19, 32, 43),
(2352, 43, 32, 20, 33, 43),
(2353, 43, 32, 21, 34, 43),
(2354, 43, 33, 19, 32, 44),
(2355, 43, 33, 20, 33, 44),
(2356, 43, 33, 21, 34, 44),
(2357, 43, 34, 19, 32, 45),
(2358, 43, 34, 20, 33, 45),
(2359, 43, 34, 21, 34, 45),
(2360, 43, 35, 19, 32, 46),
(2361, 43, 35, 20, 33, 46),
(2362, 43, 35, 21, 34, 46),
(2363, 44, 27, 16, 27, 38),
(2364, 44, 27, 17, 28, 38),
(2365, 44, 27, 18, 29, 38),
(2366, 44, 28, 16, 27, 39),
(2367, 44, 28, 17, 28, 39),
(2368, 44, 28, 18, 29, 39),
(2369, 44, 29, 16, 27, 40),
(2370, 44, 29, 17, 28, 40),
(2371, 44, 29, 18, 29, 40),
(2372, 44, 30, 16, 27, 41),
(2373, 44, 30, 17, 28, 41),
(2374, 44, 30, 18, 29, 41),
(2375, 44, 31, 16, 27, 42),
(2376, 44, 31, 17, 28, 42),
(2377, 44, 31, 18, 29, 42),
(2378, 44, 32, 16, 27, 43),
(2379, 44, 32, 17, 28, 43),
(2380, 44, 32, 18, 29, 43),
(2381, 44, 33, 16, 27, 44),
(2382, 44, 33, 17, 28, 44),
(2383, 44, 33, 18, 29, 44),
(2384, 44, 34, 16, 27, 45),
(2385, 44, 34, 17, 28, 45),
(2386, 44, 34, 18, 29, 45),
(2387, 44, 35, 16, 27, 46),
(2388, 44, 35, 17, 28, 46),
(2389, 44, 35, 18, 29, 46),
(2390, 45, 27, 13, 22, 38),
(2391, 45, 27, 14, 23, 38),
(2392, 45, 27, 15, 24, 38),
(2393, 45, 28, 13, 22, 39),
(2394, 45, 28, 14, 23, 39),
(2395, 45, 28, 15, 24, 39),
(2396, 45, 29, 13, 22, 40),
(2397, 45, 29, 14, 23, 40),
(2398, 45, 29, 15, 24, 40),
(2399, 45, 30, 13, 22, 41),
(2400, 45, 30, 14, 23, 41),
(2401, 45, 30, 15, 24, 41),
(2402, 45, 31, 13, 22, 42),
(2403, 45, 31, 14, 23, 42),
(2404, 45, 31, 15, 24, 42),
(2405, 45, 32, 13, 22, 43),
(2406, 45, 32, 14, 23, 43),
(2407, 45, 32, 15, 24, 43),
(2408, 45, 33, 13, 22, 44),
(2409, 45, 33, 14, 23, 44),
(2410, 45, 33, 15, 24, 44),
(2411, 45, 34, 13, 22, 45),
(2412, 45, 34, 14, 23, 45),
(2413, 45, 34, 15, 24, 45),
(2414, 45, 35, 13, 22, 46),
(2415, 45, 35, 14, 23, 46),
(2416, 45, 35, 15, 24, 46),
(2417, 46, 27, 10, 17, 38),
(2418, 46, 27, 11, 18, 38),
(2419, 46, 27, 12, 19, 38),
(2420, 46, 28, 10, 17, 39),
(2421, 46, 28, 11, 18, 39),
(2422, 46, 28, 12, 19, 39),
(2423, 46, 29, 10, 17, 40),
(2424, 46, 29, 11, 18, 40),
(2425, 46, 29, 12, 19, 40),
(2426, 46, 30, 10, 17, 41),
(2427, 46, 30, 11, 18, 41),
(2428, 46, 30, 12, 19, 41),
(2429, 46, 31, 10, 17, 42),
(2430, 46, 31, 11, 18, 42),
(2431, 46, 31, 12, 19, 42),
(2432, 46, 32, 10, 17, 43),
(2433, 46, 32, 11, 18, 43),
(2434, 46, 32, 12, 19, 43),
(2435, 46, 33, 10, 17, 44),
(2436, 46, 33, 11, 18, 44),
(2437, 46, 33, 12, 19, 44),
(2438, 46, 34, 10, 17, 45),
(2439, 46, 34, 11, 18, 45),
(2440, 46, 34, 12, 19, 45),
(2441, 46, 35, 10, 17, 46),
(2442, 46, 35, 11, 18, 46),
(2443, 46, 35, 12, 19, 46),
(2444, 47, 27, 7, 12, 38),
(2445, 47, 27, 8, 13, 38),
(2446, 47, 27, 9, 14, 38),
(2447, 47, 28, 7, 12, 39),
(2448, 47, 28, 8, 13, 39),
(2449, 47, 28, 9, 14, 39),
(2450, 47, 29, 7, 12, 40);

INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(2451, 47, 29, 8, 13, 40),
(2452, 47, 29, 9, 14, 40),
(2453, 47, 30, 7, 12, 41),
(2454, 47, 30, 8, 13, 41),
(2455, 47, 30, 9, 14, 41),
(2456, 47, 31, 7, 12, 42),
(2457, 47, 31, 8, 13, 42),
(2458, 47, 31, 9, 14, 42),
(2459, 47, 32, 7, 12, 43),
(2460, 47, 32, 8, 13, 43),
(2461, 47, 32, 9, 14, 43),
(2462, 47, 33, 7, 12, 44),
(2463, 47, 33, 8, 13, 44),
(2464, 47, 33, 9, 14, 44),
(2465, 47, 34, 7, 12, 45),
(2466, 47, 34, 8, 13, 45),
(2467, 47, 34, 9, 14, 45),
(2468, 47, 35, 7, 12, 46),
(2469, 47, 35, 8, 13, 46),
(2470, 47, 35, 9, 14, 46),
(2471, 48, 27, 4, 7, 38),
(2472, 48, 27, 5, 8, 38),
(2473, 48, 27, 6, 9, 38),
(2474, 48, 28, 4, 7, 39),
(2475, 48, 28, 5, 8, 39),
(2476, 48, 28, 6, 9, 39),
(2477, 48, 29, 4, 7, 40),
(2478, 48, 29, 5, 8, 40),
(2479, 48, 29, 6, 9, 40),
(2480, 48, 30, 4, 7, 41),
(2481, 48, 30, 5, 8, 41),
(2482, 48, 30, 6, 9, 41),
(2483, 48, 31, 4, 7, 42),
(2484, 48, 31, 5, 8, 42),
(2485, 48, 31, 6, 9, 42),
(2486, 48, 32, 4, 7, 43),
(2487, 48, 32, 5, 8, 43),
(2488, 48, 32, 6, 9, 43),
(2489, 48, 33, 4, 7, 44),
(2490, 48, 33, 5, 8, 44),
(2491, 48, 33, 6, 9, 44),
(2492, 48, 34, 4, 7, 45),
(2493, 48, 34, 5, 8, 45),
(2494, 48, 34, 6, 9, 45),
(2495, 48, 35, 4, 7, 46),
(2496, 48, 35, 5, 8, 46),
(2497, 48, 35, 6, 9, 46),
(2498, 49, 27, 1, 2, 38),
(2499, 49, 27, 2, 3, 38),
(2500, 49, 27, 3, 4, 38),
(2501, 49, 28, 1, 2, 39),
(2502, 49, 28, 2, 3, 39),
(2503, 49, 28, 3, 4, 39),
(2504, 49, 29, 1, 2, 40),
(2505, 49, 29, 2, 3, 40),
(2506, 49, 29, 3, 4, 40),
(2507, 49, 30, 1, 2, 41),
(2508, 49, 30, 2, 3, 41),
(2509, 49, 30, 3, 4, 41),
(2510, 49, 31, 1, 2, 42),
(2511, 49, 31, 2, 3, 42),
(2512, 49, 31, 3, 4, 42),
(2513, 49, 32, 1, 2, 43),
(2514, 49, 32, 2, 3, 43),
(2515, 49, 32, 3, 4, 43),
(2516, 49, 33, 1, 2, 44),
(2517, 49, 33, 2, 3, 44),
(2518, 49, 33, 3, 4, 44),
(2519, 49, 34, 1, 2, 45),
(2520, 49, 34, 2, 3, 45),
(2521, 49, 34, 3, 4, 45),
(2522, 49, 35, 1, 2, 46),
(2523, 49, 35, 2, 3, 46),
(2524, 49, 35, 3, 4, 46),
(2525, 50, 1, 1, 11, 6),
(2526, 50, 1, 2, 12, 6),
(2527, 50, 1, 3, 13, 6),
(2528, 50, 1, 4, 14, 6),
(2529, 50, 1, 5, 15, 6),
(2530, 50, 1, 6, 16, 6),
(2531, 50, 1, 7, 17, 6),
(2532, 50, 1, 8, 18, 6),
(2533, 50, 1, 9, 19, 6),
(2534, 50, 1, 10, 20, 6),
(2535, 50, 1, 11, 21, 6),
(2536, 50, 1, 12, 22, 6),
(2537, 50, 1, 13, 23, 6),
(2538, 50, 1, 14, 24, 6),
(2539, 50, 2, 1, 11, 7),
(2540, 50, 2, 2, 12, 7),
(2541, 50, 2, 3, 13, 7),
(2542, 50, 2, 4, 14, 7),
(2543, 50, 2, 5, 15, 7),
(2544, 50, 2, 6, 16, 7),
(2545, 50, 2, 7, 17, 7),
(2546, 50, 2, 8, 18, 7),
(2547, 50, 2, 9, 19, 7),
(2548, 50, 2, 10, 20, 7),
(2549, 50, 2, 11, 21, 7),
(2550, 50, 2, 12, 22, 7),
(2551, 50, 2, 13, 23, 7),
(2552, 50, 2, 14, 24, 7),
(2553, 50, 3, 1, 11, 8),
(2554, 50, 3, 2, 12, 8),
(2555, 50, 3, 3, 13, 8),
(2556, 50, 3, 4, 14, 8),
(2557, 50, 3, 5, 15, 8),
(2558, 50, 3, 6, 16, 8),
(2559, 50, 3, 7, 17, 8),
(2560, 50, 3, 8, 18, 8),
(2561, 50, 3, 9, 19, 8),
(2562, 50, 3, 10, 20, 8),
(2563, 50, 3, 11, 21, 8),
(2564, 50, 3, 12, 22, 8),
(2565, 50, 3, 13, 23, 8),
(2566, 50, 3, 14, 24, 8),
(2567, 50, 4, 1, 11, 9),
(2568, 50, 4, 2, 12, 9),
(2569, 50, 4, 3, 13, 9),
(2570, 50, 4, 4, 14, 9),
(2571, 50, 4, 5, 15, 9),
(2572, 50, 4, 6, 16, 9),
(2573, 50, 4, 7, 17, 9),
(2574, 50, 4, 8, 18, 9),
(2575, 50, 4, 9, 19, 9),
(2576, 50, 4, 10, 20, 9),
(2577, 50, 4, 11, 21, 9),
(2578, 50, 4, 12, 22, 9),
(2579, 50, 4, 13, 23, 9),
(2580, 50, 4, 14, 24, 9),
(2581, 50, 5, 1, 11, 10),
(2582, 50, 5, 2, 12, 10),
(2583, 50, 5, 3, 13, 10),
(2584, 50, 5, 4, 14, 10),
(2585, 50, 5, 5, 15, 10),
(2586, 50, 5, 6, 16, 10),
(2587, 50, 5, 7, 17, 10),
(2588, 50, 5, 8, 18, 10),
(2589, 50, 5, 9, 19, 10),
(2590, 50, 5, 10, 20, 10),
(2591, 50, 5, 11, 21, 10),
(2592, 50, 5, 12, 22, 10),
(2593, 50, 5, 13, 23, 10),
(2594, 50, 5, 14, 24, 10),
(2595, 50, 6, 1, 11, 11),
(2596, 50, 6, 2, 12, 11),
(2597, 50, 6, 3, 13, 11),
(2598, 50, 6, 4, 14, 11),
(2599, 50, 6, 5, 15, 11),
(2600, 50, 6, 6, 16, 11),
(2601, 50, 6, 7, 17, 11),
(2602, 50, 6, 8, 18, 11),
(2603, 50, 6, 9, 19, 11),
(2604, 50, 6, 10, 20, 11),
(2605, 50, 6, 11, 21, 11),
(2606, 50, 6, 12, 22, 11),
(2607, 50, 6, 13, 23, 11),
(2608, 50, 6, 14, 24, 11),
(2609, 50, 7, 1, 11, 12),
(2610, 50, 7, 2, 12, 12),
(2611, 50, 7, 3, 13, 12),
(2612, 50, 7, 4, 14, 12),
(2613, 50, 7, 5, 15, 12),
(2614, 50, 7, 6, 16, 12),
(2615, 50, 7, 7, 17, 12),
(2616, 50, 7, 8, 18, 12),
(2617, 50, 7, 9, 19, 12),
(2618, 50, 7, 10, 20, 12),
(2619, 50, 7, 11, 21, 12),
(2620, 50, 7, 12, 22, 12),
(2621, 50, 7, 13, 23, 12),
(2622, 50, 7, 14, 24, 12),
(2623, 51, 1, 15, 28, 6),
(2624, 51, 1, 16, 29, 6),
(2625, 51, 1, 17, 30, 6),
(2626, 51, 1, 18, 31, 6),
(2627, 51, 1, 19, 32, 6),
(2628, 51, 1, 20, 33, 6),
(2629, 51, 1, 21, 34, 6),
(2630, 51, 1, 22, 35, 6),
(2631, 51, 1, 23, 36, 6),
(2632, 51, 1, 24, 37, 6),
(2633, 51, 1, 25, 38, 6),
(2634, 51, 1, 26, 39, 6),
(2635, 51, 1, 27, 40, 6),
(2636, 51, 2, 15, 28, 7),
(2637, 51, 2, 16, 29, 7),
(2638, 51, 2, 17, 30, 7),
(2639, 51, 2, 18, 31, 7),
(2640, 51, 2, 19, 32, 7),
(2641, 51, 2, 20, 33, 7),
(2642, 51, 2, 21, 34, 7),
(2643, 51, 2, 22, 35, 7),
(2644, 51, 2, 23, 36, 7),
(2645, 51, 2, 24, 37, 7),
(2646, 51, 2, 25, 38, 7),
(2647, 51, 2, 26, 39, 7),
(2648, 51, 2, 27, 40, 7),
(2649, 51, 3, 15, 28, 8),
(2650, 51, 3, 16, 29, 8),
(2651, 51, 3, 17, 30, 8),
(2652, 51, 3, 18, 31, 8),
(2653, 51, 3, 19, 32, 8),
(2654, 51, 3, 20, 33, 8),
(2655, 51, 3, 21, 34, 8),
(2656, 51, 3, 22, 35, 8),
(2657, 51, 3, 23, 36, 8),
(2658, 51, 3, 24, 37, 8),
(2659, 51, 3, 25, 38, 8),
(2660, 51, 3, 26, 39, 8),
(2661, 51, 3, 27, 40, 8),
(2662, 51, 4, 15, 28, 9),
(2663, 51, 4, 16, 29, 9),
(2664, 51, 4, 17, 30, 9),
(2665, 51, 4, 18, 31, 9),
(2666, 51, 4, 19, 32, 9),
(2667, 51, 4, 20, 33, 9),
(2668, 51, 4, 21, 34, 9),
(2669, 51, 4, 22, 35, 9),
(2670, 51, 4, 23, 36, 9),
(2671, 51, 4, 24, 37, 9),
(2672, 51, 4, 25, 38, 9),
(2673, 51, 4, 26, 39, 9),
(2674, 51, 4, 27, 40, 9),
(2675, 51, 5, 15, 28, 10),
(2676, 51, 5, 16, 29, 10),
(2677, 51, 5, 17, 30, 10),
(2678, 51, 5, 18, 31, 10),
(2679, 51, 5, 19, 32, 10),
(2680, 51, 5, 20, 33, 10),
(2681, 51, 5, 21, 34, 10),
(2682, 51, 5, 22, 35, 10),
(2683, 51, 5, 23, 36, 10),
(2684, 51, 5, 24, 37, 10),
(2685, 51, 5, 25, 38, 10),
(2686, 51, 5, 26, 39, 10),
(2687, 51, 5, 27, 40, 10),
(2688, 51, 6, 15, 28, 11),
(2689, 51, 6, 16, 29, 11),
(2690, 51, 6, 17, 30, 11),
(2691, 51, 6, 18, 31, 11),
(2692, 51, 6, 19, 32, 11),
(2693, 51, 6, 20, 33, 11),
(2694, 51, 6, 21, 34, 11),
(2695, 51, 6, 22, 35, 11),
(2696, 51, 6, 23, 36, 11),
(2697, 51, 6, 24, 37, 11),
(2698, 51, 6, 25, 38, 11),
(2699, 51, 6, 26, 39, 11),
(2700, 51, 6, 27, 40, 11),
(2701, 51, 7, 15, 28, 12),
(2702, 51, 7, 16, 29, 12),
(2703, 51, 7, 17, 30, 12),
(2704, 51, 7, 18, 31, 12),
(2705, 51, 7, 19, 32, 12),
(2706, 51, 7, 20, 33, 12),
(2707, 51, 7, 21, 34, 12),
(2708, 51, 7, 22, 35, 12),
(2709, 51, 7, 23, 36, 12),
(2710, 51, 7, 24, 37, 12),
(2711, 51, 7, 25, 38, 12),
(2712, 51, 7, 26, 39, 12),
(2713, 51, 7, 27, 40, 12),
(2714, 52, 8, 1, 11, 15),
(2715, 52, 8, 2, 12, 15),
(2716, 52, 8, 3, 13, 15),
(2717, 52, 8, 4, 14, 15),
(2718, 52, 8, 5, 15, 15),
(2719, 52, 8, 6, 16, 15),
(2720, 52, 8, 7, 17, 15),
(2721, 52, 8, 8, 18, 15),
(2722, 52, 8, 9, 19, 15),
(2723, 52, 8, 10, 20, 15),
(2724, 52, 8, 11, 21, 15),
(2725, 52, 8, 12, 22, 15),
(2726, 52, 8, 13, 23, 15),
(2727, 52, 8, 14, 24, 15),
(2728, 52, 9, 1, 11, 16),
(2729, 52, 9, 2, 12, 16),
(2730, 52, 9, 3, 13, 16),
(2731, 52, 9, 4, 14, 16),
(2732, 52, 9, 5, 15, 16),
(2733, 52, 9, 6, 16, 16),
(2734, 52, 9, 7, 17, 16),
(2735, 52, 9, 8, 18, 16),
(2736, 52, 9, 9, 19, 16),
(2737, 52, 9, 10, 20, 16),
(2738, 52, 9, 11, 21, 16),
(2739, 52, 9, 12, 22, 16),
(2740, 52, 9, 13, 23, 16),
(2741, 52, 9, 14, 24, 16),
(2742, 52, 10, 1, 11, 17),
(2743, 52, 10, 2, 12, 17),
(2744, 52, 10, 3, 13, 17),
(2745, 52, 10, 4, 14, 17),
(2746, 52, 10, 5, 15, 17),
(2747, 52, 10, 6, 16, 17),
(2748, 52, 10, 7, 17, 17),
(2749, 52, 10, 8, 18, 17),
(2750, 52, 10, 9, 19, 17),
(2751, 52, 10, 10, 20, 17),
(2752, 52, 10, 11, 21, 17),
(2753, 52, 10, 12, 22, 17),
(2754, 52, 10, 13, 23, 17),
(2755, 52, 10, 14, 24, 17),
(2756, 52, 11, 1, 11, 18),
(2757, 52, 11, 2, 12, 18),
(2758, 52, 11, 3, 13, 18),
(2759, 52, 11, 4, 14, 18),
(2760, 52, 11, 5, 15, 18),
(2761, 52, 11, 6, 16, 18),
(2762, 52, 11, 7, 17, 18),
(2763, 52, 11, 8, 18, 18),
(2764, 52, 11, 9, 19, 18),
(2765, 52, 11, 10, 20, 18),
(2766, 52, 11, 11, 21, 18),
(2767, 52, 11, 12, 22, 18),
(2768, 52, 11, 13, 23, 18),
(2769, 52, 11, 14, 24, 18),
(2770, 52, 12, 1, 11, 19),
(2771, 52, 12, 2, 12, 19),
(2772, 52, 12, 3, 13, 19),
(2773, 52, 12, 4, 14, 19),
(2774, 52, 12, 5, 15, 19),
(2775, 52, 12, 6, 16, 19),
(2776, 52, 12, 7, 17, 19),
(2777, 52, 12, 8, 18, 19),
(2778, 52, 12, 9, 19, 19),
(2779, 52, 12, 10, 20, 19),
(2780, 52, 12, 11, 21, 19),
(2781, 52, 12, 12, 22, 19),
(2782, 52, 12, 13, 23, 19),
(2783, 52, 12, 14, 24, 19),
(2784, 52, 13, 1, 11, 20),
(2785, 52, 13, 2, 12, 20),
(2786, 52, 13, 3, 13, 20),
(2787, 52, 13, 4, 14, 20),
(2788, 52, 13, 5, 15, 20),
(2789, 52, 13, 6, 16, 20),
(2790, 52, 13, 7, 17, 20),
(2791, 52, 13, 8, 18, 20),
(2792, 52, 13, 9, 19, 20),
(2793, 52, 13, 10, 20, 20),
(2794, 52, 13, 11, 21, 20),
(2795, 52, 13, 12, 22, 20),
(2796, 52, 13, 13, 23, 20),
(2797, 52, 13, 14, 24, 20),
(2798, 52, 14, 1, 11, 21),
(2799, 52, 14, 2, 12, 21),
(2800, 52, 14, 3, 13, 21),
(2801, 52, 14, 4, 14, 21),
(2802, 52, 14, 5, 15, 21),
(2803, 52, 14, 6, 16, 21),
(2804, 52, 14, 7, 17, 21),
(2805, 52, 14, 8, 18, 21),
(2806, 52, 14, 9, 19, 21),
(2807, 52, 14, 10, 20, 21),
(2808, 52, 14, 11, 21, 21),
(2809, 52, 14, 12, 22, 21),
(2810, 52, 14, 13, 23, 21),
(2811, 52, 14, 14, 24, 21),
(2812, 53, 8, 15, 28, 15),
(2813, 53, 8, 16, 29, 15),
(2814, 53, 8, 17, 30, 15),
(2815, 53, 8, 18, 31, 15),
(2816, 53, 8, 19, 32, 15),
(2817, 53, 8, 20, 33, 15),
(2818, 53, 8, 21, 34, 15),
(2819, 53, 8, 22, 35, 15),
(2820, 53, 8, 23, 36, 15),
(2821, 53, 8, 24, 37, 15),
(2822, 53, 8, 25, 38, 15),
(2823, 53, 8, 26, 39, 15),
(2824, 53, 8, 27, 40, 15),
(2825, 53, 9, 15, 28, 16),
(2826, 53, 9, 16, 29, 16),
(2827, 53, 9, 17, 30, 16),
(2828, 53, 9, 18, 31, 16),
(2829, 53, 9, 19, 32, 16),
(2830, 53, 9, 20, 33, 16),
(2831, 53, 9, 21, 34, 16),
(2832, 53, 9, 22, 35, 16),
(2833, 53, 9, 23, 36, 16),
(2834, 53, 9, 24, 37, 16),
(2835, 53, 9, 25, 38, 16),
(2836, 53, 9, 26, 39, 16),
(2837, 53, 9, 27, 40, 16),
(2838, 53, 10, 15, 28, 17),
(2839, 53, 10, 16, 29, 17),
(2840, 53, 10, 17, 30, 17),
(2841, 53, 10, 18, 31, 17),
(2842, 53, 10, 19, 32, 17),
(2843, 53, 10, 20, 33, 17),
(2844, 53, 10, 21, 34, 17),
(2845, 53, 10, 22, 35, 17),
(2846, 53, 10, 23, 36, 17),
(2847, 53, 10, 24, 37, 17),
(2848, 53, 10, 25, 38, 17),
(2849, 53, 10, 26, 39, 17),
(2850, 53, 10, 27, 40, 17),
(2851, 53, 11, 15, 28, 18),
(2852, 53, 11, 16, 29, 18),
(2853, 53, 11, 17, 30, 18),
(2854, 53, 11, 18, 31, 18),
(2855, 53, 11, 19, 32, 18),
(2856, 53, 11, 20, 33, 18),
(2857, 53, 11, 21, 34, 18),
(2858, 53, 11, 22, 35, 18),
(2859, 53, 11, 23, 36, 18),
(2860, 53, 11, 24, 37, 18),
(2861, 53, 11, 25, 38, 18),
(2862, 53, 11, 26, 39, 18),
(2863, 53, 11, 27, 40, 18),
(2864, 53, 12, 15, 28, 19),
(2865, 53, 12, 16, 29, 19),
(2866, 53, 12, 17, 30, 19),
(2867, 53, 12, 18, 31, 19),
(2868, 53, 12, 19, 32, 19),
(2869, 53, 12, 20, 33, 19),
(2870, 53, 12, 21, 34, 19),
(2871, 53, 12, 22, 35, 19),
(2872, 53, 12, 23, 36, 19),
(2873, 53, 12, 24, 37, 19),
(2874, 53, 12, 25, 38, 19),
(2875, 53, 12, 26, 39, 19),
(2876, 53, 12, 27, 40, 19),
(2877, 53, 13, 15, 28, 20),
(2878, 53, 13, 16, 29, 20),
(2879, 53, 13, 17, 30, 20),
(2880, 53, 13, 18, 31, 20),
(2881, 53, 13, 19, 32, 20),
(2882, 53, 13, 20, 33, 20),
(2883, 53, 13, 21, 34, 20),
(2884, 53, 13, 22, 35, 20),
(2885, 53, 13, 23, 36, 20),
(2886, 53, 13, 24, 37, 20),
(2887, 53, 13, 25, 38, 20),
(2888, 53, 13, 26, 39, 20),
(2889, 53, 13, 27, 40, 20),
(2890, 53, 14, 15, 28, 21),
(2891, 53, 14, 16, 29, 21),
(2892, 53, 14, 17, 30, 21),
(2893, 53, 14, 18, 31, 21),
(2894, 53, 14, 19, 32, 21),
(2895, 53, 14, 20, 33, 21),
(2896, 53, 14, 21, 34, 21),
(2897, 53, 14, 22, 35, 21),
(2898, 53, 14, 23, 36, 21),
(2899, 53, 14, 24, 37, 21),
(2900, 53, 14, 25, 38, 21),
(2901, 53, 14, 26, 39, 21),
(2902, 53, 14, 27, 40, 21),
(2903, 54, 15, 1, 1, 25),
(2904, 54, 15, 2, 2, 25),
(2905, 54, 15, 3, 3, 25),
(2906, 54, 15, 4, 4, 25),
(2907, 54, 15, 5, 5, 25),
(2908, 54, 15, 6, 6, 25),
(2909, 54, 15, 7, 7, 25),
(2910, 54, 15, 8, 8, 25),
(2911, 54, 15, 9, 9, 25),
(2912, 54, 15, 10, 10, 25),
(2913, 54, 15, 11, 11, 25),
(2914, 54, 15, 12, 12, 25),
(2915, 54, 16, 1, 1, 26),
(2916, 54, 16, 2, 2, 26),
(2917, 54, 16, 3, 3, 26),
(2918, 54, 16, 4, 4, 26),
(2919, 54, 16, 5, 5, 26),
(2920, 54, 16, 6, 6, 26),
(2921, 54, 16, 7, 7, 26),
(2922, 54, 16, 8, 8, 26),
(2923, 54, 16, 9, 9, 26),
(2924, 54, 16, 10, 10, 26),
(2925, 54, 16, 11, 11, 26),
(2926, 54, 16, 12, 12, 26),
(2927, 54, 17, 1, 1, 27),
(2928, 54, 17, 2, 2, 27),
(2929, 54, 17, 3, 3, 27),
(2930, 54, 17, 4, 4, 27),
(2931, 54, 17, 5, 5, 27),
(2932, 54, 17, 6, 6, 27),
(2933, 54, 17, 7, 7, 27),
(2934, 54, 17, 8, 8, 27),
(2935, 54, 17, 9, 9, 27),
(2936, 54, 17, 10, 10, 27),
(2937, 54, 17, 11, 11, 27),
(2938, 54, 17, 12, 12, 27),
(2939, 54, 18, 1, 1, 28),
(2940, 54, 18, 2, 2, 28),
(2941, 54, 18, 3, 3, 28),
(2942, 54, 18, 4, 4, 28),
(2943, 54, 18, 5, 5, 28),
(2944, 54, 18, 6, 6, 28),
(2945, 54, 18, 7, 7, 28),
(2946, 54, 18, 8, 8, 28),
(2947, 54, 18, 9, 9, 28),
(2948, 54, 18, 10, 10, 28),
(2949, 54, 18, 11, 11, 28),
(2950, 54, 18, 12, 12, 28);

INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(2951, 54, 19, 1, 1, 29),
(2952, 54, 19, 2, 2, 29),
(2953, 54, 19, 3, 3, 29),
(2954, 54, 19, 4, 4, 29),
(2955, 54, 19, 5, 5, 29),
(2956, 54, 19, 6, 6, 29),
(2957, 54, 19, 7, 7, 29),
(2958, 54, 19, 8, 8, 29),
(2959, 54, 19, 9, 9, 29),
(2960, 54, 19, 10, 10, 29),
(2961, 54, 19, 11, 11, 29),
(2962, 54, 19, 12, 12, 29),
(2963, 54, 20, 1, 1, 30),
(2964, 54, 20, 2, 2, 30),
(2965, 54, 20, 3, 3, 30),
(2966, 54, 20, 4, 4, 30),
(2967, 54, 20, 5, 5, 30),
(2968, 54, 20, 6, 6, 30),
(2969, 54, 20, 7, 7, 30),
(2970, 54, 20, 8, 8, 30),
(2971, 54, 20, 9, 9, 30),
(2972, 54, 20, 10, 10, 30),
(2973, 54, 20, 11, 11, 30),
(2974, 54, 20, 12, 12, 30),
(2975, 54, 21, 1, 1, 31),
(2976, 54, 21, 2, 2, 31),
(2977, 54, 21, 3, 3, 31),
(2978, 54, 21, 4, 4, 31),
(2979, 54, 21, 5, 5, 31),
(2980, 54, 21, 6, 6, 31),
(2981, 54, 21, 7, 7, 31),
(2982, 54, 21, 8, 8, 31),
(2983, 54, 21, 9, 9, 31),
(2984, 54, 21, 10, 10, 31),
(2985, 54, 21, 11, 11, 31),
(2986, 54, 21, 12, 12, 31),
(2987, 55, 15, 13, 16, 25),
(2988, 55, 15, 14, 17, 25),
(2989, 55, 15, 15, 18, 25),
(2990, 55, 15, 16, 19, 25),
(2991, 55, 15, 17, 20, 25),
(2992, 55, 15, 18, 21, 25),
(2993, 55, 15, 19, 22, 25),
(2994, 55, 15, 20, 23, 25),
(2995, 55, 15, 21, 24, 25),
(2996, 55, 15, 22, 25, 25),
(2997, 55, 15, 23, 26, 25),
(2998, 55, 15, 24, 27, 25),
(2999, 55, 15, 25, 28, 25),
(3000, 55, 15, 26, 29, 25),
(3001, 55, 15, 27, 30, 25),
(3002, 55, 15, 28, 31, 25),
(3003, 55, 15, 29, 32, 25),
(3004, 55, 15, 30, 33, 25),
(3005, 55, 15, 31, 34, 25),
(3006, 55, 15, 32, 35, 25),
(3007, 55, 16, 13, 16, 26),
(3008, 55, 16, 14, 17, 26),
(3009, 55, 16, 15, 18, 26),
(3010, 55, 16, 16, 19, 26),
(3011, 55, 16, 17, 20, 26),
(3012, 55, 16, 18, 21, 26),
(3013, 55, 16, 19, 22, 26),
(3014, 55, 16, 20, 23, 26),
(3015, 55, 16, 21, 24, 26),
(3016, 55, 16, 22, 25, 26),
(3017, 55, 16, 23, 26, 26),
(3018, 55, 16, 24, 27, 26),
(3019, 55, 16, 25, 28, 26),
(3020, 55, 16, 26, 29, 26),
(3021, 55, 16, 27, 30, 26),
(3022, 55, 16, 28, 31, 26),
(3023, 55, 16, 29, 32, 26),
(3024, 55, 16, 30, 33, 26),
(3025, 55, 16, 31, 34, 26),
(3026, 55, 16, 32, 35, 26),
(3027, 55, 17, 13, 16, 27),
(3028, 55, 17, 14, 17, 27),
(3029, 55, 17, 15, 18, 27),
(3030, 55, 17, 16, 19, 27),
(3031, 55, 17, 17, 20, 27),
(3032, 55, 17, 18, 21, 27),
(3033, 55, 17, 19, 22, 27),
(3034, 55, 17, 20, 23, 27),
(3035, 55, 17, 21, 24, 27),
(3036, 55, 17, 22, 25, 27),
(3037, 55, 17, 23, 26, 27),
(3038, 55, 17, 24, 27, 27),
(3039, 55, 17, 25, 28, 27),
(3040, 55, 17, 26, 29, 27),
(3041, 55, 17, 27, 30, 27),
(3042, 55, 17, 28, 31, 27),
(3043, 55, 17, 29, 32, 27),
(3044, 55, 17, 30, 33, 27),
(3045, 55, 17, 31, 34, 27),
(3046, 55, 17, 32, 35, 27),
(3047, 55, 18, 13, 16, 28),
(3048, 55, 18, 14, 17, 28),
(3049, 55, 18, 15, 18, 28),
(3050, 55, 18, 16, 19, 28),
(3051, 55, 18, 17, 20, 28),
(3052, 55, 18, 18, 21, 28),
(3053, 55, 18, 19, 22, 28),
(3054, 55, 18, 20, 23, 28),
(3055, 55, 18, 21, 24, 28),
(3056, 55, 18, 22, 25, 28),
(3057, 55, 18, 23, 26, 28),
(3058, 55, 18, 24, 27, 28),
(3059, 55, 18, 25, 28, 28),
(3060, 55, 18, 26, 29, 28),
(3061, 55, 18, 27, 30, 28),
(3062, 55, 18, 28, 31, 28),
(3063, 55, 18, 29, 32, 28),
(3064, 55, 18, 30, 33, 28),
(3065, 55, 18, 31, 34, 28),
(3066, 55, 18, 32, 35, 28),
(3067, 55, 19, 13, 16, 29),
(3068, 55, 19, 14, 17, 29),
(3069, 55, 19, 15, 18, 29),
(3070, 55, 19, 16, 19, 29),
(3071, 55, 19, 17, 20, 29),
(3072, 55, 19, 18, 21, 29),
(3073, 55, 19, 19, 22, 29),
(3074, 55, 19, 20, 23, 29),
(3075, 55, 19, 21, 24, 29),
(3076, 55, 19, 22, 25, 29),
(3077, 55, 19, 23, 26, 29),
(3078, 55, 19, 24, 27, 29),
(3079, 55, 19, 25, 28, 29),
(3080, 55, 19, 26, 29, 29),
(3081, 55, 19, 27, 30, 29),
(3082, 55, 19, 28, 31, 29),
(3083, 55, 19, 29, 32, 29),
(3084, 55, 19, 30, 33, 29),
(3085, 55, 19, 31, 34, 29),
(3086, 55, 19, 32, 35, 29),
(3087, 55, 20, 13, 16, 30),
(3088, 55, 20, 14, 17, 30),
(3089, 55, 20, 15, 18, 30),
(3090, 55, 20, 16, 19, 30),
(3091, 55, 20, 17, 20, 30),
(3092, 55, 20, 18, 21, 30),
(3093, 55, 20, 19, 22, 30),
(3094, 55, 20, 20, 23, 30),
(3095, 55, 20, 21, 24, 30),
(3096, 55, 20, 22, 25, 30),
(3097, 55, 20, 23, 26, 30),
(3098, 55, 20, 24, 27, 30),
(3099, 55, 20, 25, 28, 30),
(3100, 55, 20, 26, 29, 30),
(3101, 55, 20, 27, 30, 30),
(3102, 55, 20, 28, 31, 30),
(3103, 55, 20, 29, 32, 30),
(3104, 55, 20, 30, 33, 30),
(3105, 55, 20, 31, 34, 30),
(3106, 55, 20, 32, 35, 30),
(3107, 55, 21, 13, 16, 31),
(3108, 55, 21, 14, 17, 31),
(3109, 55, 21, 15, 18, 31),
(3110, 55, 21, 16, 19, 31),
(3111, 55, 21, 17, 20, 31),
(3112, 55, 21, 18, 21, 31),
(3113, 55, 21, 19, 22, 31),
(3114, 55, 21, 20, 23, 31),
(3115, 55, 21, 21, 24, 31),
(3116, 55, 21, 22, 25, 31),
(3117, 55, 21, 23, 26, 31),
(3118, 55, 21, 24, 27, 31),
(3119, 55, 21, 25, 28, 31),
(3120, 55, 21, 26, 29, 31),
(3121, 55, 21, 27, 30, 31),
(3122, 55, 21, 28, 31, 31),
(3123, 55, 21, 29, 32, 31),
(3124, 55, 21, 30, 33, 31),
(3125, 55, 21, 31, 34, 31),
(3126, 55, 21, 32, 35, 31),
(3127, 56, 15, 33, 39, 25),
(3128, 56, 15, 34, 40, 25),
(3129, 56, 15, 35, 41, 25),
(3130, 56, 15, 36, 42, 25),
(3131, 56, 15, 37, 43, 25),
(3132, 56, 15, 38, 44, 25),
(3133, 56, 15, 39, 45, 25),
(3134, 56, 15, 40, 46, 25),
(3135, 56, 15, 41, 47, 25),
(3136, 56, 15, 42, 48, 25),
(3137, 56, 15, 43, 49, 25),
(3138, 56, 15, 44, 50, 25),
(3139, 56, 16, 33, 39, 26),
(3140, 56, 16, 34, 40, 26),
(3141, 56, 16, 35, 41, 26),
(3142, 56, 16, 36, 42, 26),
(3143, 56, 16, 37, 43, 26),
(3144, 56, 16, 38, 44, 26),
(3145, 56, 16, 39, 45, 26),
(3146, 56, 16, 40, 46, 26),
(3147, 56, 16, 41, 47, 26),
(3148, 56, 16, 42, 48, 26),
(3149, 56, 16, 43, 49, 26),
(3150, 56, 16, 44, 50, 26),
(3151, 56, 17, 33, 39, 27),
(3152, 56, 17, 34, 40, 27),
(3153, 56, 17, 35, 41, 27),
(3154, 56, 17, 36, 42, 27),
(3155, 56, 17, 37, 43, 27),
(3156, 56, 17, 38, 44, 27),
(3157, 56, 17, 39, 45, 27),
(3158, 56, 17, 40, 46, 27),
(3159, 56, 17, 41, 47, 27),
(3160, 56, 17, 42, 48, 27),
(3161, 56, 17, 43, 49, 27),
(3162, 56, 17, 44, 50, 27),
(3163, 56, 18, 33, 39, 28),
(3164, 56, 18, 34, 40, 28),
(3165, 56, 18, 35, 41, 28),
(3166, 56, 18, 36, 42, 28),
(3167, 56, 18, 37, 43, 28),
(3168, 56, 18, 38, 44, 28),
(3169, 56, 18, 39, 45, 28),
(3170, 56, 18, 40, 46, 28),
(3171, 56, 18, 41, 47, 28),
(3172, 56, 18, 42, 48, 28),
(3173, 56, 18, 43, 49, 28),
(3174, 56, 18, 44, 50, 28),
(3175, 56, 19, 33, 39, 29),
(3176, 56, 19, 34, 40, 29),
(3177, 56, 19, 35, 41, 29),
(3178, 56, 19, 36, 42, 29),
(3179, 56, 19, 37, 43, 29),
(3180, 56, 19, 38, 44, 29),
(3181, 56, 19, 39, 45, 29),
(3182, 56, 19, 40, 46, 29),
(3183, 56, 19, 41, 47, 29),
(3184, 56, 19, 42, 48, 29),
(3185, 56, 19, 43, 49, 29),
(3186, 56, 19, 44, 50, 29),
(3187, 56, 20, 33, 39, 30),
(3188, 56, 20, 34, 40, 30),
(3189, 56, 20, 35, 41, 30),
(3190, 56, 20, 36, 42, 30),
(3191, 56, 20, 37, 43, 30),
(3192, 56, 20, 38, 44, 30),
(3193, 56, 20, 39, 45, 30),
(3194, 56, 20, 40, 46, 30),
(3195, 56, 20, 41, 47, 30),
(3196, 56, 20, 42, 48, 30),
(3197, 56, 20, 43, 49, 30),
(3198, 56, 20, 44, 50, 30),
(3199, 56, 21, 33, 39, 31),
(3200, 56, 21, 34, 40, 31),
(3201, 56, 21, 35, 41, 31),
(3202, 56, 21, 36, 42, 31),
(3203, 56, 21, 37, 43, 31),
(3204, 56, 21, 38, 44, 31),
(3205, 56, 21, 39, 45, 31),
(3206, 56, 21, 40, 46, 31),
(3207, 56, 21, 41, 47, 31),
(3208, 56, 21, 42, 48, 31),
(3209, 56, 21, 43, 49, 31),
(3210, 56, 21, 44, 50, 31),
(3211, 57, 7, 1, 1, 6),
(3212, 57, 6, 1, 2, 6),
(3213, 57, 5, 1, 3, 6),
(3214, 57, 4, 1, 4, 6),
(3215, 57, 3, 1, 5, 6),
(3216, 57, 2, 1, 6, 6),
(3217, 57, 1, 1, 7, 6),
(3218, 57, 7, 2, 1, 7),
(3219, 57, 6, 2, 2, 7),
(3220, 57, 5, 2, 3, 7),
(3221, 57, 4, 2, 4, 7),
(3222, 57, 3, 2, 5, 7),
(3223, 57, 2, 2, 6, 7),
(3224, 57, 1, 2, 7, 7),
(3225, 57, 7, 3, 1, 8),
(3226, 57, 6, 3, 2, 8),
(3227, 57, 5, 3, 3, 8),
(3228, 57, 4, 3, 4, 8),
(3229, 57, 3, 3, 5, 8),
(3230, 57, 2, 3, 6, 8),
(3231, 57, 1, 3, 7, 8),
(3232, 57, 7, 4, 1, 9),
(3233, 57, 6, 4, 2, 9),
(3234, 57, 5, 4, 3, 9),
(3235, 57, 4, 4, 4, 9),
(3236, 57, 3, 4, 5, 9),
(3237, 57, 2, 4, 6, 9),
(3238, 57, 1, 4, 7, 9),
(3239, 57, 7, 5, 1, 10),
(3240, 57, 6, 5, 2, 10),
(3241, 57, 5, 5, 3, 10),
(3242, 57, 4, 5, 4, 10),
(3243, 57, 3, 5, 5, 10),
(3244, 57, 2, 5, 6, 10),
(3245, 57, 1, 5, 7, 10),
(3246, 57, 7, 6, 1, 11),
(3247, 57, 6, 6, 2, 11),
(3248, 57, 5, 6, 3, 11),
(3249, 57, 4, 6, 4, 11),
(3250, 57, 3, 6, 5, 11),
(3251, 57, 2, 6, 6, 11),
(3252, 57, 1, 6, 7, 11),
(3253, 57, 7, 7, 1, 12),
(3254, 57, 6, 7, 2, 12),
(3255, 57, 5, 7, 3, 12),
(3256, 57, 4, 7, 4, 12),
(3257, 57, 3, 7, 5, 12),
(3258, 57, 2, 7, 6, 12),
(3259, 57, 1, 7, 7, 12),
(3260, 57, 7, 8, 1, 13),
(3261, 57, 6, 8, 2, 13),
(3262, 57, 5, 8, 3, 13),
(3263, 57, 4, 8, 4, 13),
(3264, 57, 3, 8, 5, 13),
(3265, 57, 2, 8, 6, 13),
(3266, 57, 1, 8, 7, 13),
(3267, 57, 7, 9, 1, 14),
(3268, 57, 6, 9, 2, 14),
(3269, 57, 5, 9, 3, 14),
(3270, 57, 4, 9, 4, 14),
(3271, 57, 3, 9, 5, 14),
(3272, 57, 2, 9, 6, 14),
(3273, 57, 1, 9, 7, 14),
(3274, 57, 7, 10, 1, 15),
(3275, 57, 6, 10, 2, 15),
(3276, 57, 5, 10, 3, 15),
(3277, 57, 4, 10, 4, 15),
(3278, 57, 3, 10, 5, 15),
(3279, 57, 2, 10, 6, 15),
(3280, 57, 1, 10, 7, 15),
(3281, 57, 7, 11, 1, 16),
(3282, 57, 6, 11, 2, 16),
(3283, 57, 5, 11, 3, 16),
(3284, 57, 4, 11, 4, 16),
(3285, 57, 3, 11, 5, 16),
(3286, 57, 2, 11, 6, 16),
(3287, 57, 1, 11, 7, 16),
(3288, 57, 7, 12, 1, 17),
(3289, 57, 6, 12, 2, 17),
(3290, 57, 5, 12, 3, 17),
(3291, 57, 4, 12, 4, 17),
(3292, 57, 3, 12, 5, 17),
(3293, 57, 2, 12, 6, 17),
(3294, 57, 1, 12, 7, 17),
(3295, 57, 7, 13, 1, 18),
(3296, 57, 6, 13, 2, 18),
(3297, 57, 5, 13, 3, 18),
(3298, 57, 4, 13, 4, 18),
(3299, 57, 3, 13, 5, 18),
(3300, 57, 2, 13, 6, 18),
(3301, 57, 1, 13, 7, 18),
(3302, 57, 7, 14, 1, 19),
(3303, 57, 6, 14, 2, 19),
(3304, 57, 5, 14, 3, 19),
(3305, 57, 4, 14, 4, 19),
(3306, 57, 3, 14, 5, 19),
(3307, 57, 2, 14, 6, 19),
(3308, 57, 1, 14, 7, 19),
(3309, 57, 7, 15, 1, 20),
(3310, 57, 6, 15, 2, 20),
(3311, 57, 5, 15, 3, 20),
(3312, 57, 4, 15, 4, 20),
(3313, 57, 3, 15, 5, 20),
(3314, 57, 2, 15, 6, 20),
(3315, 57, 1, 15, 7, 20),
(3316, 57, 7, 16, 1, 21),
(3317, 57, 6, 16, 2, 21),
(3318, 57, 5, 16, 3, 21),
(3319, 57, 4, 16, 4, 21),
(3320, 57, 3, 16, 5, 21),
(3321, 57, 2, 16, 6, 21),
(3322, 57, 1, 16, 7, 21),
(3323, 58, 1, 1, 44, 6),
(3324, 58, 2, 1, 45, 6),
(3325, 58, 3, 1, 46, 6),
(3326, 58, 4, 1, 47, 6),
(3327, 58, 5, 1, 48, 6),
(3328, 58, 6, 1, 49, 6),
(3329, 58, 7, 1, 50, 6),
(3330, 58, 1, 2, 44, 7),
(3331, 58, 2, 2, 45, 7),
(3332, 58, 3, 2, 46, 7),
(3333, 58, 4, 2, 47, 7),
(3334, 58, 5, 2, 48, 7),
(3335, 58, 6, 2, 49, 7),
(3336, 58, 7, 2, 50, 7),
(3337, 58, 1, 3, 44, 8),
(3338, 58, 2, 3, 45, 8),
(3339, 58, 3, 3, 46, 8),
(3340, 58, 4, 3, 47, 8),
(3341, 58, 5, 3, 48, 8),
(3342, 58, 6, 3, 49, 8),
(3343, 58, 7, 3, 50, 8),
(3344, 58, 1, 4, 44, 9),
(3345, 58, 2, 4, 45, 9),
(3346, 58, 3, 4, 46, 9),
(3347, 58, 4, 4, 47, 9),
(3348, 58, 5, 4, 48, 9),
(3349, 58, 6, 4, 49, 9),
(3350, 58, 7, 4, 50, 9),
(3351, 58, 1, 5, 44, 10),
(3352, 58, 2, 5, 45, 10),
(3353, 58, 3, 5, 46, 10),
(3354, 58, 4, 5, 47, 10),
(3355, 58, 5, 5, 48, 10),
(3356, 58, 6, 5, 49, 10),
(3357, 58, 7, 5, 50, 10),
(3358, 58, 1, 6, 44, 11),
(3359, 58, 2, 6, 45, 11),
(3360, 58, 3, 6, 46, 11),
(3361, 58, 4, 6, 47, 11),
(3362, 58, 5, 6, 48, 11),
(3363, 58, 6, 6, 49, 11),
(3364, 58, 7, 6, 50, 11),
(3365, 58, 1, 7, 44, 12),
(3366, 58, 2, 7, 45, 12),
(3367, 58, 3, 7, 46, 12),
(3368, 58, 4, 7, 47, 12),
(3369, 58, 5, 7, 48, 12),
(3370, 58, 6, 7, 49, 12),
(3371, 58, 7, 7, 50, 12),
(3372, 58, 1, 8, 44, 13),
(3373, 58, 2, 8, 45, 13),
(3374, 58, 3, 8, 46, 13),
(3375, 58, 4, 8, 47, 13),
(3376, 58, 5, 8, 48, 13),
(3377, 58, 6, 8, 49, 13),
(3378, 58, 7, 8, 50, 13),
(3379, 58, 1, 9, 44, 14),
(3380, 58, 2, 9, 45, 14),
(3381, 58, 3, 9, 46, 14),
(3382, 58, 4, 9, 47, 14),
(3383, 58, 5, 9, 48, 14),
(3384, 58, 6, 9, 49, 14),
(3385, 58, 7, 9, 50, 14),
(3386, 58, 1, 10, 44, 15),
(3387, 58, 2, 10, 45, 15),
(3388, 58, 3, 10, 46, 15),
(3389, 58, 4, 10, 47, 15),
(3390, 58, 5, 10, 48, 15),
(3391, 58, 6, 10, 49, 15),
(3392, 58, 7, 10, 50, 15),
(3393, 58, 1, 11, 44, 16),
(3394, 58, 2, 11, 45, 16),
(3395, 58, 3, 11, 46, 16),
(3396, 58, 4, 11, 47, 16),
(3397, 58, 5, 11, 48, 16),
(3398, 58, 6, 11, 49, 16),
(3399, 58, 7, 11, 50, 16),
(3400, 58, 1, 12, 44, 17),
(3401, 58, 2, 12, 45, 17),
(3402, 58, 3, 12, 46, 17),
(3403, 58, 4, 12, 47, 17),
(3404, 58, 5, 12, 48, 17),
(3405, 58, 6, 12, 49, 17),
(3406, 58, 7, 12, 50, 17),
(3407, 58, 1, 13, 44, 18),
(3408, 58, 2, 13, 45, 18),
(3409, 58, 3, 13, 46, 18),
(3410, 58, 4, 13, 47, 18),
(3411, 58, 5, 13, 48, 18),
(3412, 58, 6, 13, 49, 18),
(3413, 58, 7, 13, 50, 18),
(3414, 58, 1, 14, 44, 19),
(3415, 58, 2, 14, 45, 19),
(3416, 58, 3, 14, 46, 19),
(3417, 58, 4, 14, 47, 19),
(3418, 58, 5, 14, 48, 19),
(3419, 58, 6, 14, 49, 19),
(3420, 58, 7, 14, 50, 19),
(3421, 58, 1, 15, 44, 20),
(3422, 58, 2, 15, 45, 20),
(3423, 58, 3, 15, 46, 20),
(3424, 58, 4, 15, 47, 20),
(3425, 58, 5, 15, 48, 20),
(3426, 58, 6, 15, 49, 20),
(3427, 58, 7, 15, 50, 20),
(3428, 58, 1, 16, 44, 21),
(3429, 58, 2, 16, 45, 21),
(3430, 58, 3, 16, 46, 21),
(3431, 58, 4, 16, 47, 21),
(3432, 58, 5, 16, 48, 21),
(3433, 58, 6, 16, 49, 21),
(3434, 58, 7, 16, 50, 21),
(3435, 59, 10, 1, 1, 1),
(3436, 59, 10, 2, 2, 1),
(3437, 59, 10, 3, 3, 1),
(3438, 59, 10, 4, 4, 1),
(3439, 59, 10, 5, 5, 1),
(3440, 59, 10, 6, 6, 1),
(3441, 59, 10, 7, 7, 1),
(3442, 59, 10, 8, 8, 1),
(3443, 59, 10, 9, 9, 1),
(3444, 59, 10, 10, 10, 1),
(3445, 59, 9, 1, 1, 2),
(3446, 59, 9, 2, 2, 2),
(3447, 59, 9, 3, 3, 2),
(3448, 59, 9, 4, 4, 2),
(3449, 59, 9, 5, 5, 2),
(3450, 59, 9, 6, 6, 2);

INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(3451, 59, 9, 7, 7, 2),
(3452, 59, 9, 8, 8, 2),
(3453, 59, 9, 9, 9, 2),
(3454, 59, 9, 10, 10, 2),
(3455, 59, 8, 1, 1, 3),
(3456, 59, 8, 2, 2, 3),
(3457, 59, 8, 3, 3, 3),
(3458, 59, 8, 4, 4, 3),
(3459, 59, 8, 5, 5, 3),
(3460, 59, 8, 6, 6, 3),
(3461, 59, 8, 7, 7, 3),
(3462, 59, 8, 8, 8, 3),
(3463, 59, 8, 9, 9, 3),
(3464, 59, 8, 10, 10, 3),
(3465, 59, 7, 1, 1, 4),
(3466, 59, 7, 2, 2, 4),
(3467, 59, 7, 3, 3, 4),
(3468, 59, 7, 4, 4, 4),
(3469, 59, 7, 5, 5, 4),
(3470, 59, 7, 6, 6, 4),
(3471, 59, 7, 7, 7, 4),
(3472, 59, 7, 8, 8, 4),
(3473, 59, 7, 9, 9, 4),
(3474, 59, 7, 10, 10, 4),
(3475, 59, 6, 1, 1, 5),
(3476, 59, 6, 2, 2, 5),
(3477, 59, 6, 3, 3, 5),
(3478, 59, 6, 4, 4, 5),
(3479, 59, 6, 5, 5, 5),
(3480, 59, 6, 6, 6, 5),
(3481, 59, 6, 7, 7, 5),
(3482, 59, 6, 8, 8, 5),
(3483, 59, 6, 9, 9, 5),
(3484, 59, 6, 10, 10, 5),
(3485, 59, 5, 1, 1, 6),
(3486, 59, 5, 2, 2, 6),
(3487, 59, 5, 3, 3, 6),
(3488, 59, 5, 4, 4, 6),
(3489, 59, 5, 5, 5, 6),
(3490, 59, 5, 6, 6, 6),
(3491, 59, 5, 7, 7, 6),
(3492, 59, 5, 8, 8, 6),
(3493, 59, 5, 9, 9, 6),
(3494, 59, 5, 10, 10, 6),
(3495, 59, 4, 1, 1, 7),
(3496, 59, 4, 2, 2, 7),
(3497, 59, 4, 3, 3, 7),
(3498, 59, 4, 4, 4, 7),
(3499, 59, 4, 5, 5, 7),
(3500, 59, 4, 6, 6, 7),
(3501, 59, 4, 7, 7, 7),
(3502, 59, 4, 8, 8, 7),
(3503, 59, 4, 9, 9, 7),
(3504, 59, 4, 10, 10, 7),
(3505, 59, 3, 1, 1, 8),
(3506, 59, 3, 2, 2, 8),
(3507, 59, 3, 3, 3, 8),
(3508, 59, 3, 4, 4, 8),
(3509, 59, 3, 5, 5, 8),
(3510, 59, 3, 6, 6, 8),
(3511, 59, 3, 7, 7, 8),
(3512, 59, 3, 8, 8, 8),
(3513, 59, 3, 9, 9, 8),
(3514, 59, 3, 10, 10, 8),
(3515, 59, 2, 1, 1, 9),
(3516, 59, 2, 2, 2, 9),
(3517, 59, 2, 3, 3, 9),
(3518, 59, 2, 4, 4, 9),
(3519, 59, 2, 5, 5, 9),
(3520, 59, 2, 6, 6, 9),
(3521, 59, 2, 7, 7, 9),
(3522, 59, 2, 8, 8, 9),
(3523, 59, 2, 9, 9, 9),
(3524, 59, 2, 10, 10, 9),
(3525, 59, 1, 1, 1, 10),
(3526, 59, 1, 2, 2, 10),
(3527, 59, 1, 3, 3, 10),
(3528, 59, 1, 4, 4, 10),
(3529, 59, 1, 5, 5, 10),
(3530, 59, 1, 6, 6, 10),
(3531, 59, 1, 7, 7, 10),
(3532, 59, 1, 8, 8, 10),
(3533, 59, 1, 9, 9, 10),
(3534, 59, 1, 10, 10, 10),
(3535, 60, 10, 11, 14, 1),
(3536, 60, 10, 12, 15, 1),
(3537, 60, 10, 13, 16, 1),
(3538, 60, 10, 14, 17, 1),
(3539, 60, 10, 15, 18, 1),
(3540, 60, 10, 16, 19, 1),
(3541, 60, 10, 17, 20, 1),
(3542, 60, 10, 18, 21, 1),
(3543, 60, 10, 19, 22, 1),
(3544, 60, 10, 20, 23, 1),
(3545, 60, 10, 21, 24, 1),
(3546, 60, 9, 11, 14, 2),
(3547, 60, 9, 12, 15, 2),
(3548, 60, 9, 13, 16, 2),
(3549, 60, 9, 14, 17, 2),
(3550, 60, 9, 15, 18, 2),
(3551, 60, 9, 16, 19, 2),
(3552, 60, 9, 17, 20, 2),
(3553, 60, 9, 18, 21, 2),
(3554, 60, 9, 19, 22, 2),
(3555, 60, 9, 20, 23, 2),
(3556, 60, 9, 21, 24, 2),
(3557, 60, 8, 11, 14, 3),
(3558, 60, 8, 12, 15, 3),
(3559, 60, 8, 13, 16, 3),
(3560, 60, 8, 14, 17, 3),
(3561, 60, 8, 15, 18, 3),
(3562, 60, 8, 16, 19, 3),
(3563, 60, 8, 17, 20, 3),
(3564, 60, 8, 18, 21, 3),
(3565, 60, 8, 19, 22, 3),
(3566, 60, 8, 20, 23, 3),
(3567, 60, 8, 21, 24, 3),
(3568, 60, 7, 11, 14, 4),
(3569, 60, 7, 12, 15, 4),
(3570, 60, 7, 13, 16, 4),
(3571, 60, 7, 14, 17, 4),
(3572, 60, 7, 15, 18, 4),
(3573, 60, 7, 16, 19, 4),
(3574, 60, 7, 17, 20, 4),
(3575, 60, 7, 18, 21, 4),
(3576, 60, 7, 19, 22, 4),
(3577, 60, 7, 20, 23, 4),
(3578, 60, 7, 21, 24, 4),
(3579, 60, 6, 11, 14, 5),
(3580, 60, 6, 12, 15, 5),
(3581, 60, 6, 13, 16, 5),
(3582, 60, 6, 14, 17, 5),
(3583, 60, 6, 15, 18, 5),
(3584, 60, 6, 16, 19, 5),
(3585, 60, 6, 17, 20, 5),
(3586, 60, 6, 18, 21, 5),
(3587, 60, 6, 19, 22, 5),
(3588, 60, 6, 20, 23, 5),
(3589, 60, 6, 21, 24, 5),
(3590, 60, 5, 11, 14, 6),
(3591, 60, 5, 12, 15, 6),
(3592, 60, 5, 13, 16, 6),
(3593, 60, 5, 14, 17, 6),
(3594, 60, 5, 15, 18, 6),
(3595, 60, 5, 16, 19, 6),
(3596, 60, 5, 17, 20, 6),
(3597, 60, 5, 18, 21, 6),
(3598, 60, 5, 19, 22, 6),
(3599, 60, 5, 20, 23, 6),
(3600, 60, 5, 21, 24, 6),
(3601, 60, 4, 11, 14, 7),
(3602, 60, 4, 12, 15, 7),
(3603, 60, 4, 13, 16, 7),
(3604, 60, 4, 14, 17, 7),
(3605, 60, 4, 15, 18, 7),
(3606, 60, 4, 16, 19, 7),
(3607, 60, 4, 17, 20, 7),
(3608, 60, 4, 18, 21, 7),
(3609, 60, 4, 19, 22, 7),
(3610, 60, 4, 20, 23, 7),
(3611, 60, 4, 21, 24, 7),
(3612, 60, 3, 11, 14, 8),
(3613, 60, 3, 12, 15, 8),
(3614, 60, 3, 13, 16, 8),
(3615, 60, 3, 14, 17, 8),
(3616, 60, 3, 15, 18, 8),
(3617, 60, 3, 16, 19, 8),
(3618, 60, 3, 17, 20, 8),
(3619, 60, 3, 18, 21, 8),
(3620, 60, 3, 19, 22, 8),
(3621, 60, 3, 20, 23, 8),
(3622, 60, 3, 21, 24, 8),
(3623, 60, 2, 11, 14, 9),
(3624, 60, 2, 12, 15, 9),
(3625, 60, 2, 13, 16, 9),
(3626, 60, 2, 14, 17, 9),
(3627, 60, 2, 15, 18, 9),
(3628, 60, 2, 16, 19, 9),
(3629, 60, 2, 17, 20, 9),
(3630, 60, 2, 18, 21, 9),
(3631, 60, 2, 19, 22, 9),
(3632, 60, 2, 20, 23, 9),
(3633, 60, 2, 21, 24, 9),
(3634, 60, 1, 11, 14, 10),
(3635, 60, 1, 12, 15, 10),
(3636, 60, 1, 13, 16, 10),
(3637, 60, 1, 14, 17, 10),
(3638, 60, 1, 15, 18, 10),
(3639, 60, 1, 16, 19, 10),
(3640, 60, 1, 17, 20, 10),
(3641, 60, 1, 18, 21, 10),
(3642, 60, 1, 19, 22, 10),
(3643, 60, 1, 20, 23, 10),
(3644, 60, 1, 21, 24, 10),
(3645, 61, 10, 22, 28, 1),
(3646, 61, 10, 23, 29, 1),
(3647, 61, 10, 24, 30, 1),
(3648, 61, 10, 25, 31, 1),
(3649, 61, 10, 26, 32, 1),
(3650, 61, 10, 27, 33, 1),
(3651, 61, 10, 28, 34, 1),
(3652, 61, 10, 29, 35, 1),
(3653, 61, 10, 30, 36, 1),
(3654, 61, 10, 31, 37, 1),
(3655, 61, 9, 22, 28, 2),
(3656, 61, 9, 23, 29, 2),
(3657, 61, 9, 24, 30, 2),
(3658, 61, 9, 25, 31, 2),
(3659, 61, 9, 26, 32, 2),
(3660, 61, 9, 27, 33, 2),
(3661, 61, 9, 28, 34, 2),
(3662, 61, 9, 29, 35, 2),
(3663, 61, 9, 30, 36, 2),
(3664, 61, 9, 31, 37, 2),
(3665, 61, 8, 22, 28, 3),
(3666, 61, 8, 23, 29, 3),
(3667, 61, 8, 24, 30, 3),
(3668, 61, 8, 25, 31, 3),
(3669, 61, 8, 26, 32, 3),
(3670, 61, 8, 27, 33, 3),
(3671, 61, 8, 28, 34, 3),
(3672, 61, 8, 29, 35, 3),
(3673, 61, 8, 30, 36, 3),
(3674, 61, 8, 31, 37, 3),
(3675, 61, 7, 22, 28, 4),
(3676, 61, 7, 23, 29, 4),
(3677, 61, 7, 24, 30, 4),
(3678, 61, 7, 25, 31, 4),
(3679, 61, 7, 26, 32, 4),
(3680, 61, 7, 27, 33, 4),
(3681, 61, 7, 28, 34, 4),
(3682, 61, 7, 29, 35, 4),
(3683, 61, 7, 30, 36, 4),
(3684, 61, 7, 31, 37, 4),
(3685, 61, 6, 22, 28, 5),
(3686, 61, 6, 23, 29, 5),
(3687, 61, 6, 24, 30, 5),
(3688, 61, 6, 25, 31, 5),
(3689, 61, 6, 26, 32, 5),
(3690, 61, 6, 27, 33, 5),
(3691, 61, 6, 28, 34, 5),
(3692, 61, 6, 29, 35, 5),
(3693, 61, 6, 30, 36, 5),
(3694, 61, 6, 31, 37, 5),
(3695, 61, 5, 22, 28, 6),
(3696, 61, 5, 23, 29, 6),
(3697, 61, 5, 24, 30, 6),
(3698, 61, 5, 25, 31, 6),
(3699, 61, 5, 26, 32, 6),
(3700, 61, 5, 27, 33, 6),
(3701, 61, 5, 28, 34, 6),
(3702, 61, 5, 29, 35, 6),
(3703, 61, 5, 30, 36, 6),
(3704, 61, 5, 31, 37, 6),
(3705, 61, 4, 22, 28, 7),
(3706, 61, 4, 23, 29, 7),
(3707, 61, 4, 24, 30, 7),
(3708, 61, 4, 25, 31, 7),
(3709, 61, 4, 26, 32, 7),
(3710, 61, 4, 27, 33, 7),
(3711, 61, 4, 28, 34, 7),
(3712, 61, 4, 29, 35, 7),
(3713, 61, 4, 30, 36, 7),
(3714, 61, 4, 31, 37, 7),
(3715, 61, 3, 22, 28, 8),
(3716, 61, 3, 23, 29, 8),
(3717, 61, 3, 24, 30, 8),
(3718, 61, 3, 25, 31, 8),
(3719, 61, 3, 26, 32, 8),
(3720, 61, 3, 27, 33, 8),
(3721, 61, 3, 28, 34, 8),
(3722, 61, 3, 29, 35, 8),
(3723, 61, 3, 30, 36, 8),
(3724, 61, 3, 31, 37, 8),
(3725, 61, 2, 22, 28, 9),
(3726, 61, 2, 23, 29, 9),
(3727, 61, 2, 24, 30, 9),
(3728, 61, 2, 25, 31, 9),
(3729, 61, 2, 26, 32, 9),
(3730, 61, 2, 27, 33, 9),
(3731, 61, 2, 28, 34, 9),
(3732, 61, 2, 29, 35, 9),
(3733, 61, 2, 30, 36, 9),
(3734, 61, 2, 31, 37, 9),
(3735, 61, 1, 22, 28, 10),
(3736, 61, 1, 23, 29, 10),
(3737, 61, 1, 24, 30, 10),
(3738, 61, 1, 25, 31, 10),
(3739, 61, 1, 26, 32, 10),
(3740, 61, 1, 27, 33, 10),
(3741, 61, 1, 28, 34, 10),
(3742, 61, 1, 29, 35, 10),
(3743, 61, 1, 30, 36, 10),
(3744, 61, 1, 31, 37, 10),
(3745, 62, 10, 32, 41, 1),
(3746, 62, 10, 33, 42, 1),
(3747, 62, 10, 34, 43, 1),
(3748, 62, 10, 35, 44, 1),
(3749, 62, 10, 36, 45, 1),
(3750, 62, 10, 37, 46, 1),
(3751, 62, 10, 38, 47, 1),
(3752, 62, 10, 39, 48, 1),
(3753, 62, 10, 40, 49, 1),
(3754, 62, 10, 41, 50, 1),
(3755, 62, 9, 32, 41, 2),
(3756, 62, 9, 33, 42, 2),
(3757, 62, 9, 34, 43, 2),
(3758, 62, 9, 35, 44, 2),
(3759, 62, 9, 36, 45, 2),
(3760, 62, 9, 37, 46, 2),
(3761, 62, 9, 38, 47, 2),
(3762, 62, 9, 39, 48, 2),
(3763, 62, 9, 40, 49, 2),
(3764, 62, 9, 41, 50, 2),
(3765, 62, 8, 32, 41, 3),
(3766, 62, 8, 33, 42, 3),
(3767, 62, 8, 34, 43, 3),
(3768, 62, 8, 35, 44, 3),
(3769, 62, 8, 36, 45, 3),
(3770, 62, 8, 37, 46, 3),
(3771, 62, 8, 38, 47, 3),
(3772, 62, 8, 39, 48, 3),
(3773, 62, 8, 40, 49, 3),
(3774, 62, 8, 41, 50, 3),
(3775, 62, 7, 32, 41, 4),
(3776, 62, 7, 33, 42, 4),
(3777, 62, 7, 34, 43, 4),
(3778, 62, 7, 35, 44, 4),
(3779, 62, 7, 36, 45, 4),
(3780, 62, 7, 37, 46, 4),
(3781, 62, 7, 38, 47, 4),
(3782, 62, 7, 39, 48, 4),
(3783, 62, 7, 40, 49, 4),
(3784, 62, 7, 41, 50, 4),
(3785, 62, 6, 32, 41, 5),
(3786, 62, 6, 33, 42, 5),
(3787, 62, 6, 34, 43, 5),
(3788, 62, 6, 35, 44, 5),
(3789, 62, 6, 36, 45, 5),
(3790, 62, 6, 37, 46, 5),
(3791, 62, 6, 38, 47, 5),
(3792, 62, 6, 39, 48, 5),
(3793, 62, 6, 40, 49, 5),
(3794, 62, 6, 41, 50, 5),
(3795, 62, 5, 32, 41, 6),
(3796, 62, 5, 33, 42, 6),
(3797, 62, 5, 34, 43, 6),
(3798, 62, 5, 35, 44, 6),
(3799, 62, 5, 36, 45, 6),
(3800, 62, 5, 37, 46, 6),
(3801, 62, 5, 38, 47, 6),
(3802, 62, 5, 39, 48, 6),
(3803, 62, 5, 40, 49, 6),
(3804, 62, 5, 41, 50, 6),
(3805, 62, 4, 32, 41, 7),
(3806, 62, 4, 33, 42, 7),
(3807, 62, 4, 34, 43, 7),
(3808, 62, 4, 35, 44, 7),
(3809, 62, 4, 36, 45, 7),
(3810, 62, 4, 37, 46, 7),
(3811, 62, 4, 38, 47, 7),
(3812, 62, 4, 39, 48, 7),
(3813, 62, 4, 40, 49, 7),
(3814, 62, 4, 41, 50, 7),
(3815, 62, 3, 32, 41, 8),
(3816, 62, 3, 33, 42, 8),
(3817, 62, 3, 34, 43, 8),
(3818, 62, 3, 35, 44, 8),
(3819, 62, 3, 36, 45, 8),
(3820, 62, 3, 37, 46, 8),
(3821, 62, 3, 38, 47, 8),
(3822, 62, 3, 39, 48, 8),
(3823, 62, 3, 40, 49, 8),
(3824, 62, 3, 41, 50, 8),
(3825, 62, 2, 32, 41, 9),
(3826, 62, 2, 33, 42, 9),
(3827, 62, 2, 34, 43, 9),
(3828, 62, 2, 35, 44, 9),
(3829, 62, 2, 36, 45, 9),
(3830, 62, 2, 37, 46, 9),
(3831, 62, 2, 38, 47, 9),
(3832, 62, 2, 39, 48, 9),
(3833, 62, 2, 40, 49, 9),
(3834, 62, 2, 41, 50, 9),
(3835, 62, 1, 32, 41, 10),
(3836, 62, 1, 33, 42, 10),
(3837, 62, 1, 34, 43, 10),
(3838, 62, 1, 35, 44, 10),
(3839, 62, 1, 36, 45, 10),
(3840, 62, 1, 37, 46, 10),
(3841, 62, 1, 38, 47, 10),
(3842, 62, 1, 39, 48, 10),
(3843, 62, 1, 40, 49, 10),
(3844, 62, 1, 41, 50, 10),
(3845, 63, 10, 1, 1, 13),
(3846, 63, 9, 1, 2, 13),
(3847, 63, 8, 1, 3, 13),
(3848, 63, 7, 1, 4, 13),
(3849, 63, 6, 1, 5, 13),
(3850, 63, 5, 1, 6, 13),
(3851, 63, 4, 1, 7, 13),
(3852, 63, 3, 1, 8, 13),
(3853, 63, 2, 1, 9, 13),
(3854, 63, 1, 1, 10, 13),
(3855, 63, 10, 2, 1, 14),
(3856, 63, 9, 2, 2, 14),
(3857, 63, 8, 2, 3, 14),
(3858, 63, 7, 2, 4, 14),
(3859, 63, 6, 2, 5, 14),
(3860, 63, 5, 2, 6, 14),
(3861, 63, 4, 2, 7, 14),
(3862, 63, 3, 2, 8, 14),
(3863, 63, 2, 2, 9, 14),
(3864, 63, 1, 2, 10, 14),
(3865, 63, 10, 3, 1, 15),
(3866, 63, 9, 3, 2, 15),
(3867, 63, 8, 3, 3, 15),
(3868, 63, 7, 3, 4, 15),
(3869, 63, 6, 3, 5, 15),
(3870, 63, 5, 3, 6, 15),
(3871, 63, 4, 3, 7, 15),
(3872, 63, 3, 3, 8, 15),
(3873, 63, 2, 3, 9, 15),
(3874, 63, 1, 3, 10, 15),
(3875, 63, 10, 4, 1, 16),
(3876, 63, 9, 4, 2, 16),
(3877, 63, 8, 4, 3, 16),
(3878, 63, 7, 4, 4, 16),
(3879, 63, 6, 4, 5, 16),
(3880, 63, 5, 4, 6, 16),
(3881, 63, 4, 4, 7, 16),
(3882, 63, 3, 4, 8, 16),
(3883, 63, 2, 4, 9, 16),
(3884, 63, 1, 4, 10, 16),
(3885, 63, 10, 5, 1, 17),
(3886, 63, 9, 5, 2, 17),
(3887, 63, 8, 5, 3, 17),
(3888, 63, 7, 5, 4, 17),
(3889, 63, 6, 5, 5, 17),
(3890, 63, 5, 5, 6, 17),
(3891, 63, 4, 5, 7, 17),
(3892, 63, 3, 5, 8, 17),
(3893, 63, 2, 5, 9, 17),
(3894, 63, 1, 5, 10, 17),
(3895, 64, 10, 6, 1, 20),
(3896, 64, 9, 6, 2, 20),
(3897, 64, 8, 6, 3, 20),
(3898, 64, 7, 6, 4, 20),
(3899, 64, 6, 6, 5, 20),
(3900, 64, 5, 6, 6, 20),
(3901, 64, 4, 6, 7, 20),
(3902, 64, 3, 6, 8, 20),
(3903, 64, 2, 6, 9, 20),
(3904, 64, 1, 6, 10, 20),
(3905, 64, 10, 7, 1, 21),
(3906, 64, 9, 7, 2, 21),
(3907, 64, 8, 7, 3, 21),
(3908, 64, 7, 7, 4, 21),
(3909, 64, 6, 7, 5, 21),
(3910, 64, 5, 7, 6, 21),
(3911, 64, 4, 7, 7, 21),
(3912, 64, 3, 7, 8, 21),
(3913, 64, 2, 7, 9, 21),
(3914, 64, 1, 7, 10, 21),
(3915, 64, 10, 8, 1, 22),
(3916, 64, 9, 8, 2, 22),
(3917, 64, 8, 8, 3, 22),
(3918, 64, 7, 8, 4, 22),
(3919, 64, 6, 8, 5, 22),
(3920, 64, 5, 8, 6, 22),
(3921, 64, 4, 8, 7, 22),
(3922, 64, 3, 8, 8, 22),
(3923, 64, 2, 8, 9, 22),
(3924, 64, 1, 8, 10, 22),
(3925, 64, 10, 9, 1, 23),
(3926, 64, 9, 9, 2, 23),
(3927, 64, 8, 9, 3, 23),
(3928, 64, 7, 9, 4, 23),
(3929, 64, 6, 9, 5, 23),
(3930, 64, 5, 9, 6, 23),
(3931, 64, 4, 9, 7, 23),
(3932, 64, 3, 9, 8, 23),
(3933, 64, 2, 9, 9, 23),
(3934, 64, 1, 9, 10, 23),
(3935, 64, 10, 10, 1, 24),
(3936, 64, 9, 10, 2, 24),
(3937, 64, 8, 10, 3, 24),
(3938, 64, 7, 10, 4, 24),
(3939, 64, 6, 10, 5, 24),
(3940, 64, 5, 10, 6, 24),
(3941, 64, 4, 10, 7, 24),
(3942, 64, 3, 10, 8, 24),
(3943, 64, 2, 10, 9, 24),
(3944, 64, 1, 10, 10, 24),
(3945, 64, 10, 11, 1, 25),
(3946, 64, 9, 11, 2, 25),
(3947, 64, 8, 11, 3, 25),
(3948, 64, 7, 11, 4, 25),
(3949, 64, 6, 11, 5, 25),
(3950, 64, 5, 11, 6, 25);

INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(3951, 64, 4, 11, 7, 25),
(3952, 64, 3, 11, 8, 25),
(3953, 64, 2, 11, 9, 25),
(3954, 64, 1, 11, 10, 25),
(3955, 65, 1, 1, 41, 13),
(3956, 65, 2, 1, 42, 13),
(3957, 65, 3, 1, 43, 13),
(3958, 65, 4, 1, 44, 13),
(3959, 65, 5, 1, 45, 13),
(3960, 65, 6, 1, 46, 13),
(3961, 65, 7, 1, 47, 13),
(3962, 65, 8, 1, 48, 13),
(3963, 65, 9, 1, 49, 13),
(3964, 65, 10, 1, 50, 13),
(3965, 65, 1, 2, 41, 14),
(3966, 65, 2, 2, 42, 14),
(3967, 65, 3, 2, 43, 14),
(3968, 65, 4, 2, 44, 14),
(3969, 65, 5, 2, 45, 14),
(3970, 65, 6, 2, 46, 14),
(3971, 65, 7, 2, 47, 14),
(3972, 65, 8, 2, 48, 14),
(3973, 65, 9, 2, 49, 14),
(3974, 65, 10, 2, 50, 14),
(3975, 65, 1, 3, 41, 15),
(3976, 65, 2, 3, 42, 15),
(3977, 65, 3, 3, 43, 15),
(3978, 65, 4, 3, 44, 15),
(3979, 65, 5, 3, 45, 15),
(3980, 65, 6, 3, 46, 15),
(3981, 65, 7, 3, 47, 15),
(3982, 65, 8, 3, 48, 15),
(3983, 65, 9, 3, 49, 15),
(3984, 65, 10, 3, 50, 15),
(3985, 65, 1, 4, 41, 16),
(3986, 65, 2, 4, 42, 16),
(3987, 65, 3, 4, 43, 16),
(3988, 65, 4, 4, 44, 16),
(3989, 65, 5, 4, 45, 16),
(3990, 65, 6, 4, 46, 16),
(3991, 65, 7, 4, 47, 16),
(3992, 65, 8, 4, 48, 16),
(3993, 65, 9, 4, 49, 16),
(3994, 65, 10, 4, 50, 16),
(3995, 65, 1, 5, 41, 17),
(3996, 65, 2, 5, 42, 17),
(3997, 65, 3, 5, 43, 17),
(3998, 65, 4, 5, 44, 17),
(3999, 65, 5, 5, 45, 17),
(4000, 65, 6, 5, 46, 17),
(4001, 65, 7, 5, 47, 17),
(4002, 65, 8, 5, 48, 17),
(4003, 65, 9, 5, 49, 17),
(4004, 65, 10, 5, 50, 17),
(4005, 66, 1, 6, 41, 20),
(4006, 66, 2, 6, 42, 20),
(4007, 66, 3, 6, 43, 20),
(4008, 66, 4, 6, 44, 20),
(4009, 66, 5, 6, 45, 20),
(4010, 66, 6, 6, 46, 20),
(4011, 66, 7, 6, 47, 20),
(4012, 66, 8, 6, 48, 20),
(4013, 66, 9, 6, 49, 20),
(4014, 66, 10, 6, 50, 20),
(4015, 66, 1, 7, 41, 21),
(4016, 66, 2, 7, 42, 21),
(4017, 66, 3, 7, 43, 21),
(4018, 66, 4, 7, 44, 21),
(4019, 66, 5, 7, 45, 21),
(4020, 66, 6, 7, 46, 21),
(4021, 66, 7, 7, 47, 21),
(4022, 66, 8, 7, 48, 21),
(4023, 66, 9, 7, 49, 21),
(4024, 66, 10, 7, 50, 21),
(4025, 66, 1, 8, 41, 22),
(4026, 66, 2, 8, 42, 22),
(4027, 66, 3, 8, 43, 22),
(4028, 66, 4, 8, 44, 22),
(4029, 66, 5, 8, 45, 22),
(4030, 66, 6, 8, 46, 22),
(4031, 66, 7, 8, 47, 22),
(4032, 66, 8, 8, 48, 22),
(4033, 66, 9, 8, 49, 22),
(4034, 66, 10, 8, 50, 22),
(4035, 66, 1, 9, 41, 23),
(4036, 66, 2, 9, 42, 23),
(4037, 66, 3, 9, 43, 23),
(4038, 66, 4, 9, 44, 23),
(4039, 66, 5, 9, 45, 23),
(4040, 66, 6, 9, 46, 23),
(4041, 66, 7, 9, 47, 23),
(4042, 66, 8, 9, 48, 23),
(4043, 66, 9, 9, 49, 23),
(4044, 66, 10, 9, 50, 23),
(4045, 66, 1, 10, 41, 24),
(4046, 66, 2, 10, 42, 24),
(4047, 66, 3, 10, 43, 24),
(4048, 66, 4, 10, 44, 24),
(4049, 66, 5, 10, 45, 24),
(4050, 66, 6, 10, 46, 24),
(4051, 66, 7, 10, 47, 24),
(4052, 66, 8, 10, 48, 24),
(4053, 66, 9, 10, 49, 24),
(4054, 66, 10, 10, 50, 24),
(4055, 66, 1, 11, 41, 25),
(4056, 66, 2, 11, 42, 25),
(4057, 66, 3, 11, 43, 25),
(4058, 66, 4, 11, 44, 25),
(4059, 66, 5, 11, 45, 25),
(4060, 66, 6, 11, 46, 25),
(4061, 66, 7, 11, 47, 25),
(4062, 66, 8, 11, 48, 25),
(4063, 66, 9, 11, 49, 25),
(4064, 66, 10, 11, 50, 25),
(4065, 67, 1, 1, 1, 28),
(4066, 67, 1, 2, 2, 28),
(4067, 67, 1, 3, 3, 28),
(4068, 67, 1, 4, 4, 28),
(4069, 67, 1, 5, 5, 28),
(4070, 67, 1, 6, 6, 28),
(4071, 67, 1, 7, 7, 28),
(4072, 67, 1, 8, 8, 28),
(4073, 67, 1, 9, 9, 28),
(4074, 67, 1, 10, 10, 28),
(4075, 67, 2, 1, 1, 29),
(4076, 67, 2, 2, 2, 29),
(4077, 67, 2, 3, 3, 29),
(4078, 67, 2, 4, 4, 29),
(4079, 67, 2, 5, 5, 29),
(4080, 67, 2, 6, 6, 29),
(4081, 67, 2, 7, 7, 29),
(4082, 67, 2, 8, 8, 29),
(4083, 67, 2, 9, 9, 29),
(4084, 67, 2, 10, 10, 29),
(4085, 67, 3, 1, 1, 30),
(4086, 67, 3, 2, 2, 30),
(4087, 67, 3, 3, 3, 30),
(4088, 67, 3, 4, 4, 30),
(4089, 67, 3, 5, 5, 30),
(4090, 67, 3, 6, 6, 30),
(4091, 67, 3, 7, 7, 30),
(4092, 67, 3, 8, 8, 30),
(4093, 67, 3, 9, 9, 30),
(4094, 67, 3, 10, 10, 30),
(4095, 67, 4, 1, 1, 31),
(4096, 67, 4, 2, 2, 31),
(4097, 67, 4, 3, 3, 31),
(4098, 67, 4, 4, 4, 31),
(4099, 67, 4, 5, 5, 31),
(4100, 67, 4, 6, 6, 31),
(4101, 67, 4, 7, 7, 31),
(4102, 67, 4, 8, 8, 31),
(4103, 67, 4, 9, 9, 31),
(4104, 67, 4, 10, 10, 31),
(4105, 67, 5, 1, 1, 32),
(4106, 67, 5, 2, 2, 32),
(4107, 67, 5, 3, 3, 32),
(4108, 67, 5, 4, 4, 32),
(4109, 67, 5, 5, 5, 32),
(4110, 67, 5, 6, 6, 32),
(4111, 67, 5, 7, 7, 32),
(4112, 67, 5, 8, 8, 32),
(4113, 67, 5, 9, 9, 32),
(4114, 67, 5, 10, 10, 32),
(4115, 67, 6, 1, 1, 33),
(4116, 67, 6, 2, 2, 33),
(4117, 67, 6, 3, 3, 33),
(4118, 67, 6, 4, 4, 33),
(4119, 67, 6, 5, 5, 33),
(4120, 67, 6, 6, 6, 33),
(4121, 67, 6, 7, 7, 33),
(4122, 67, 6, 8, 8, 33),
(4123, 67, 6, 9, 9, 33),
(4124, 67, 6, 10, 10, 33),
(4125, 67, 7, 1, 1, 34),
(4126, 67, 7, 2, 2, 34),
(4127, 67, 7, 3, 3, 34),
(4128, 67, 7, 4, 4, 34),
(4129, 67, 7, 5, 5, 34),
(4130, 67, 7, 6, 6, 34),
(4131, 67, 7, 7, 7, 34),
(4132, 67, 7, 8, 8, 34),
(4133, 67, 7, 9, 9, 34),
(4134, 67, 7, 10, 10, 34),
(4135, 67, 8, 1, 1, 35),
(4136, 67, 8, 2, 2, 35),
(4137, 67, 8, 3, 3, 35),
(4138, 67, 8, 4, 4, 35),
(4139, 67, 8, 5, 5, 35),
(4140, 67, 8, 6, 6, 35),
(4141, 67, 8, 7, 7, 35),
(4142, 67, 8, 8, 8, 35),
(4143, 67, 8, 9, 9, 35),
(4144, 67, 8, 10, 10, 35),
(4145, 67, 9, 1, 1, 36),
(4146, 67, 9, 2, 2, 36),
(4147, 67, 9, 3, 3, 36),
(4148, 67, 9, 4, 4, 36),
(4149, 67, 9, 5, 5, 36),
(4150, 67, 9, 6, 6, 36),
(4151, 67, 9, 7, 7, 36),
(4152, 67, 9, 8, 8, 36),
(4153, 67, 9, 9, 9, 36),
(4154, 67, 9, 10, 10, 36),
(4155, 67, 10, 1, 1, 37),
(4156, 67, 10, 2, 2, 37),
(4157, 67, 10, 3, 3, 37),
(4158, 67, 10, 4, 4, 37),
(4159, 67, 10, 5, 5, 37),
(4160, 67, 10, 6, 6, 37),
(4161, 67, 10, 7, 7, 37),
(4162, 67, 10, 8, 8, 37),
(4163, 67, 10, 9, 9, 37),
(4164, 67, 10, 10, 10, 37),
(4165, 68, 1, 11, 14, 28),
(4166, 68, 1, 12, 15, 28),
(4167, 68, 1, 13, 16, 28),
(4168, 68, 1, 14, 17, 28),
(4169, 68, 1, 15, 18, 28),
(4170, 68, 1, 16, 19, 28),
(4171, 68, 1, 17, 20, 28),
(4172, 68, 1, 18, 21, 28),
(4173, 68, 1, 19, 22, 28),
(4174, 68, 1, 20, 23, 28),
(4175, 68, 1, 21, 24, 28),
(4176, 68, 2, 11, 14, 29),
(4177, 68, 2, 12, 15, 29),
(4178, 68, 2, 13, 16, 29),
(4179, 68, 2, 14, 17, 29),
(4180, 68, 2, 15, 18, 29),
(4181, 68, 2, 16, 19, 29),
(4182, 68, 2, 17, 20, 29),
(4183, 68, 2, 18, 21, 29),
(4184, 68, 2, 19, 22, 29),
(4185, 68, 2, 20, 23, 29),
(4186, 68, 2, 21, 24, 29),
(4187, 68, 3, 11, 14, 30),
(4188, 68, 3, 12, 15, 30),
(4189, 68, 3, 13, 16, 30),
(4190, 68, 3, 14, 17, 30),
(4191, 68, 3, 15, 18, 30),
(4192, 68, 3, 16, 19, 30),
(4193, 68, 3, 17, 20, 30),
(4194, 68, 3, 18, 21, 30),
(4195, 68, 3, 19, 22, 30),
(4196, 68, 3, 20, 23, 30),
(4197, 68, 3, 21, 24, 30),
(4198, 68, 4, 11, 14, 31),
(4199, 68, 4, 12, 15, 31),
(4200, 68, 4, 13, 16, 31),
(4201, 68, 4, 14, 17, 31),
(4202, 68, 4, 15, 18, 31),
(4203, 68, 4, 16, 19, 31),
(4204, 68, 4, 17, 20, 31),
(4205, 68, 4, 18, 21, 31),
(4206, 68, 4, 19, 22, 31),
(4207, 68, 4, 20, 23, 31),
(4208, 68, 4, 21, 24, 31),
(4209, 68, 5, 11, 14, 32),
(4210, 68, 5, 12, 15, 32),
(4211, 68, 5, 13, 16, 32),
(4212, 68, 5, 14, 17, 32),
(4213, 68, 5, 15, 18, 32),
(4214, 68, 5, 16, 19, 32),
(4215, 68, 5, 17, 20, 32),
(4216, 68, 5, 18, 21, 32),
(4217, 68, 5, 19, 22, 32),
(4218, 68, 5, 20, 23, 32),
(4219, 68, 5, 21, 24, 32),
(4220, 68, 6, 11, 14, 33),
(4221, 68, 6, 12, 15, 33),
(4222, 68, 6, 13, 16, 33),
(4223, 68, 6, 14, 17, 33),
(4224, 68, 6, 15, 18, 33),
(4225, 68, 6, 16, 19, 33),
(4226, 68, 6, 17, 20, 33),
(4227, 68, 6, 18, 21, 33),
(4228, 68, 6, 19, 22, 33),
(4229, 68, 6, 20, 23, 33),
(4230, 68, 6, 21, 24, 33),
(4231, 68, 7, 11, 14, 34),
(4232, 68, 7, 12, 15, 34),
(4233, 68, 7, 13, 16, 34),
(4234, 68, 7, 14, 17, 34),
(4235, 68, 7, 15, 18, 34),
(4236, 68, 7, 16, 19, 34),
(4237, 68, 7, 17, 20, 34),
(4238, 68, 7, 18, 21, 34),
(4239, 68, 7, 19, 22, 34),
(4240, 68, 7, 20, 23, 34),
(4241, 68, 7, 21, 24, 34),
(4242, 68, 8, 11, 14, 35),
(4243, 68, 8, 12, 15, 35),
(4244, 68, 8, 13, 16, 35),
(4245, 68, 8, 14, 17, 35),
(4246, 68, 8, 15, 18, 35),
(4247, 68, 8, 16, 19, 35),
(4248, 68, 8, 17, 20, 35),
(4249, 68, 8, 18, 21, 35),
(4250, 68, 8, 19, 22, 35),
(4251, 68, 8, 20, 23, 35),
(4252, 68, 8, 21, 24, 35),
(4253, 68, 9, 11, 14, 36),
(4254, 68, 9, 12, 15, 36),
(4255, 68, 9, 13, 16, 36),
(4256, 68, 9, 14, 17, 36),
(4257, 68, 9, 15, 18, 36),
(4258, 68, 9, 16, 19, 36),
(4259, 68, 9, 17, 20, 36),
(4260, 68, 9, 18, 21, 36),
(4261, 68, 9, 19, 22, 36),
(4262, 68, 9, 20, 23, 36),
(4263, 68, 9, 21, 24, 36),
(4264, 68, 10, 11, 14, 37),
(4265, 68, 10, 12, 15, 37),
(4266, 68, 10, 13, 16, 37),
(4267, 68, 10, 14, 17, 37),
(4268, 68, 10, 15, 18, 37),
(4269, 68, 10, 16, 19, 37),
(4270, 68, 10, 17, 20, 37),
(4271, 68, 10, 18, 21, 37),
(4272, 68, 10, 19, 22, 37),
(4273, 68, 10, 20, 23, 37),
(4274, 68, 10, 21, 24, 37),
(4275, 69, 1, 22, 28, 28),
(4276, 69, 1, 23, 29, 28),
(4277, 69, 1, 24, 30, 28),
(4278, 69, 1, 25, 31, 28),
(4279, 69, 1, 26, 32, 28),
(4280, 69, 1, 27, 33, 28),
(4281, 69, 1, 28, 34, 28),
(4282, 69, 1, 29, 35, 28),
(4283, 69, 1, 30, 36, 28),
(4284, 69, 1, 31, 37, 28),
(4285, 69, 2, 22, 28, 29),
(4286, 69, 2, 23, 29, 29),
(4287, 69, 2, 24, 30, 29),
(4288, 69, 2, 25, 31, 29),
(4289, 69, 2, 26, 32, 29),
(4290, 69, 2, 27, 33, 29),
(4291, 69, 2, 28, 34, 29),
(4292, 69, 2, 29, 35, 29),
(4293, 69, 2, 30, 36, 29),
(4294, 69, 2, 31, 37, 29),
(4295, 69, 3, 22, 28, 30),
(4296, 69, 3, 23, 29, 30),
(4297, 69, 3, 24, 30, 30),
(4298, 69, 3, 25, 31, 30),
(4299, 69, 3, 26, 32, 30),
(4300, 69, 3, 27, 33, 30),
(4301, 69, 3, 28, 34, 30),
(4302, 69, 3, 29, 35, 30),
(4303, 69, 3, 30, 36, 30),
(4304, 69, 3, 31, 37, 30),
(4305, 69, 4, 22, 28, 31),
(4306, 69, 4, 23, 29, 31),
(4307, 69, 4, 24, 30, 31),
(4308, 69, 4, 25, 31, 31),
(4309, 69, 4, 26, 32, 31),
(4310, 69, 4, 27, 33, 31),
(4311, 69, 4, 28, 34, 31),
(4312, 69, 4, 29, 35, 31),
(4313, 69, 4, 30, 36, 31),
(4314, 69, 4, 31, 37, 31),
(4315, 69, 5, 22, 28, 32),
(4316, 69, 5, 23, 29, 32),
(4317, 69, 5, 24, 30, 32),
(4318, 69, 5, 25, 31, 32),
(4319, 69, 5, 26, 32, 32),
(4320, 69, 5, 27, 33, 32),
(4321, 69, 5, 28, 34, 32),
(4322, 69, 5, 29, 35, 32),
(4323, 69, 5, 30, 36, 32),
(4324, 69, 5, 31, 37, 32),
(4325, 69, 6, 22, 28, 33),
(4326, 69, 6, 23, 29, 33),
(4327, 69, 6, 24, 30, 33),
(4328, 69, 6, 25, 31, 33),
(4329, 69, 6, 26, 32, 33),
(4330, 69, 6, 27, 33, 33),
(4331, 69, 6, 28, 34, 33),
(4332, 69, 6, 29, 35, 33),
(4333, 69, 6, 30, 36, 33),
(4334, 69, 6, 31, 37, 33),
(4335, 69, 7, 22, 28, 34),
(4336, 69, 7, 23, 29, 34),
(4337, 69, 7, 24, 30, 34),
(4338, 69, 7, 25, 31, 34),
(4339, 69, 7, 26, 32, 34),
(4340, 69, 7, 27, 33, 34),
(4341, 69, 7, 28, 34, 34),
(4342, 69, 7, 29, 35, 34),
(4343, 69, 7, 30, 36, 34),
(4344, 69, 7, 31, 37, 34),
(4345, 69, 8, 22, 28, 35),
(4346, 69, 8, 23, 29, 35),
(4347, 69, 8, 24, 30, 35),
(4348, 69, 8, 25, 31, 35),
(4349, 69, 8, 26, 32, 35),
(4350, 69, 8, 27, 33, 35),
(4351, 69, 8, 28, 34, 35),
(4352, 69, 8, 29, 35, 35),
(4353, 69, 8, 30, 36, 35),
(4354, 69, 8, 31, 37, 35),
(4355, 69, 9, 22, 28, 36),
(4356, 69, 9, 23, 29, 36),
(4357, 69, 9, 24, 30, 36),
(4358, 69, 9, 25, 31, 36),
(4359, 69, 9, 26, 32, 36),
(4360, 69, 9, 27, 33, 36),
(4361, 69, 9, 28, 34, 36),
(4362, 69, 9, 29, 35, 36),
(4363, 69, 9, 30, 36, 36),
(4364, 69, 9, 31, 37, 36),
(4365, 69, 10, 22, 28, 37),
(4366, 69, 10, 23, 29, 37),
(4367, 69, 10, 24, 30, 37),
(4368, 69, 10, 25, 31, 37),
(4369, 69, 10, 26, 32, 37),
(4370, 69, 10, 27, 33, 37),
(4371, 69, 10, 28, 34, 37),
(4372, 69, 10, 29, 35, 37),
(4373, 69, 10, 30, 36, 37),
(4374, 69, 10, 31, 37, 37),
(4375, 70, 1, 32, 41, 28),
(4376, 70, 1, 33, 42, 28),
(4377, 70, 1, 34, 43, 28),
(4378, 70, 1, 35, 44, 28),
(4379, 70, 1, 36, 45, 28),
(4380, 70, 1, 37, 46, 28),
(4381, 70, 1, 38, 47, 28),
(4382, 70, 1, 39, 48, 28),
(4383, 70, 1, 40, 49, 28),
(4384, 70, 1, 41, 50, 28),
(4385, 70, 2, 32, 41, 29),
(4386, 70, 2, 33, 42, 29),
(4387, 70, 2, 34, 43, 29),
(4388, 70, 2, 35, 44, 29),
(4389, 70, 2, 36, 45, 29),
(4390, 70, 2, 37, 46, 29),
(4391, 70, 2, 38, 47, 29),
(4392, 70, 2, 39, 48, 29),
(4393, 70, 2, 40, 49, 29),
(4394, 70, 2, 41, 50, 29),
(4395, 70, 3, 32, 41, 30),
(4396, 70, 3, 33, 42, 30),
(4397, 70, 3, 34, 43, 30),
(4398, 70, 3, 35, 44, 30),
(4399, 70, 3, 36, 45, 30),
(4400, 70, 3, 37, 46, 30),
(4401, 70, 3, 38, 47, 30),
(4402, 70, 3, 39, 48, 30),
(4403, 70, 3, 40, 49, 30),
(4404, 70, 3, 41, 50, 30),
(4405, 70, 4, 32, 41, 31),
(4406, 70, 4, 33, 42, 31),
(4407, 70, 4, 34, 43, 31),
(4408, 70, 4, 35, 44, 31),
(4409, 70, 4, 36, 45, 31),
(4410, 70, 4, 37, 46, 31),
(4411, 70, 4, 38, 47, 31),
(4412, 70, 4, 39, 48, 31),
(4413, 70, 4, 40, 49, 31),
(4414, 70, 4, 41, 50, 31),
(4415, 70, 5, 32, 41, 32),
(4416, 70, 5, 33, 42, 32),
(4417, 70, 5, 34, 43, 32),
(4418, 70, 5, 35, 44, 32),
(4419, 70, 5, 36, 45, 32),
(4420, 70, 5, 37, 46, 32),
(4421, 70, 5, 38, 47, 32),
(4422, 70, 5, 39, 48, 32),
(4423, 70, 5, 40, 49, 32),
(4424, 70, 5, 41, 50, 32),
(4425, 70, 6, 32, 41, 33),
(4426, 70, 6, 33, 42, 33),
(4427, 70, 6, 34, 43, 33),
(4428, 70, 6, 35, 44, 33),
(4429, 70, 6, 36, 45, 33),
(4430, 70, 6, 37, 46, 33),
(4431, 70, 6, 38, 47, 33),
(4432, 70, 6, 39, 48, 33),
(4433, 70, 6, 40, 49, 33),
(4434, 70, 6, 41, 50, 33),
(4435, 70, 7, 32, 41, 34),
(4436, 70, 7, 33, 42, 34),
(4437, 70, 7, 34, 43, 34),
(4438, 70, 7, 35, 44, 34),
(4439, 70, 7, 36, 45, 34),
(4440, 70, 7, 37, 46, 34),
(4441, 70, 7, 38, 47, 34),
(4442, 70, 7, 39, 48, 34),
(4443, 70, 7, 40, 49, 34),
(4444, 70, 7, 41, 50, 34),
(4445, 70, 8, 32, 41, 35),
(4446, 70, 8, 33, 42, 35),
(4447, 70, 8, 34, 43, 35),
(4448, 70, 8, 35, 44, 35),
(4449, 70, 8, 36, 45, 35),
(4450, 70, 8, 37, 46, 35);

INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(4451, 70, 8, 38, 47, 35),
(4452, 70, 8, 39, 48, 35),
(4453, 70, 8, 40, 49, 35),
(4454, 70, 8, 41, 50, 35),
(4455, 70, 9, 32, 41, 36),
(4456, 70, 9, 33, 42, 36),
(4457, 70, 9, 34, 43, 36),
(4458, 70, 9, 35, 44, 36),
(4459, 70, 9, 36, 45, 36),
(4460, 70, 9, 37, 46, 36),
(4461, 70, 9, 38, 47, 36),
(4462, 70, 9, 39, 48, 36),
(4463, 70, 9, 40, 49, 36),
(4464, 70, 9, 41, 50, 36),
(4465, 70, 10, 32, 41, 37),
(4466, 70, 10, 33, 42, 37),
(4467, 70, 10, 34, 43, 37),
(4468, 70, 10, 35, 44, 37),
(4469, 70, 10, 36, 45, 37),
(4470, 70, 10, 37, 46, 37),
(4471, 70, 10, 38, 47, 37),
(4472, 70, 10, 39, 48, 37),
(4473, 70, 10, 40, 49, 37),
(4474, 70, 10, 41, 50, 37),
(4475, 71, 1, 1, 1, 2),
(4476, 71, 1, 2, 2, 2),
(4477, 71, 1, 3, 3, 2),
(4478, 71, 1, 4, 4, 2),
(4479, 71, 1, 5, 5, 2),
(4480, 71, 2, 1, 1, 3),
(4481, 71, 2, 2, 2, 3),
(4482, 71, 2, 3, 3, 3),
(4483, 71, 2, 4, 4, 3),
(4484, 71, 2, 5, 5, 3),
(4485, 71, 3, 1, 1, 4),
(4486, 71, 3, 2, 2, 4),
(4487, 71, 3, 3, 3, 4),
(4488, 71, 3, 4, 4, 4),
(4489, 71, 3, 5, 5, 4),
(4490, 71, 4, 1, 1, 5),
(4491, 71, 4, 2, 2, 5),
(4492, 71, 4, 3, 3, 5),
(4493, 71, 4, 4, 4, 5),
(4494, 71, 4, 5, 5, 5),
(4495, 72, 1, 6, 6, 2),
(4496, 72, 1, 7, 7, 2),
(4497, 72, 1, 8, 8, 2),
(4498, 72, 1, 9, 9, 2),
(4499, 72, 1, 10, 10, 2),
(4500, 72, 2, 6, 6, 3),
(4501, 72, 2, 7, 7, 3),
(4502, 72, 2, 8, 8, 3),
(4503, 72, 2, 9, 9, 3),
(4504, 72, 2, 10, 10, 3),
(4505, 72, 3, 6, 6, 4),
(4506, 72, 3, 7, 7, 4),
(4507, 72, 3, 8, 8, 4),
(4508, 72, 3, 9, 9, 4),
(4509, 72, 3, 10, 10, 4),
(4510, 72, 4, 6, 6, 5),
(4511, 72, 4, 7, 7, 5),
(4512, 72, 4, 8, 8, 5),
(4513, 72, 4, 9, 9, 5),
(4514, 72, 4, 10, 10, 5),
(4515, 73, 5, 1, 1, 6),
(4516, 73, 5, 2, 2, 6),
(4517, 73, 5, 3, 3, 6),
(4518, 73, 5, 4, 4, 6),
(4519, 73, 5, 5, 5, 6),
(4520, 73, 6, 1, 1, 7),
(4521, 73, 6, 2, 2, 7),
(4522, 73, 6, 3, 3, 7),
(4523, 73, 6, 4, 4, 7),
(4524, 73, 6, 5, 5, 7),
(4525, 73, 7, 1, 1, 8),
(4526, 73, 7, 2, 2, 8),
(4527, 73, 7, 3, 3, 8),
(4528, 73, 7, 4, 4, 8),
(4529, 73, 7, 5, 5, 8),
(4530, 74, 5, 6, 6, 6),
(4531, 74, 5, 7, 7, 6),
(4532, 74, 5, 8, 8, 6),
(4533, 74, 5, 9, 9, 6),
(4534, 74, 5, 10, 10, 6),
(4535, 74, 6, 6, 6, 7),
(4536, 74, 6, 7, 7, 7),
(4537, 74, 6, 8, 8, 7),
(4538, 74, 6, 9, 9, 7),
(4539, 74, 6, 10, 10, 7),
(4540, 74, 7, 6, 6, 8),
(4541, 74, 7, 7, 7, 8),
(4542, 74, 7, 8, 8, 8),
(4543, 74, 7, 9, 9, 8),
(4544, 74, 7, 10, 10, 8),
(4545, 75, 8, 1, 1, 9),
(4546, 75, 8, 2, 2, 9),
(4547, 75, 8, 3, 3, 9),
(4548, 75, 9, 1, 1, 10),
(4549, 75, 9, 2, 2, 10),
(4550, 75, 9, 3, 3, 10),
(4551, 76, 8, 4, 4, 9),
(4552, 76, 8, 5, 5, 9),
(4553, 76, 8, 6, 6, 9),
(4554, 76, 8, 7, 7, 9),
(4555, 76, 9, 4, 4, 10),
(4556, 76, 9, 5, 5, 10),
(4557, 76, 9, 6, 6, 10),
(4558, 76, 9, 7, 7, 10),
(4559, 77, 8, 8, 8, 9),
(4560, 77, 8, 9, 9, 9),
(4561, 77, 8, 10, 10, 9),
(4562, 77, 9, 8, 8, 10),
(4563, 77, 9, 9, 9, 10),
(4564, 77, 9, 10, 10, 10);

-- 新增 Theme 活動主題 (關聯 organizer_id)
INSERT INTO theme (organizer_id, status, title, detail, image)
VALUES 
-- ACTIVE (1~20) 
('ORG0000001','ACTIVE',N'2026 馬佬獅巡迴演唱會',N'聽完課程，來聽聽演唱會吧！一起沉浸在音樂與熱情的現場氛圍中，感受最震撼的舞台魅力。上課是知識輸入，演唱會是情緒輸出，這次沒有考試，但全場都要跟著嗨！課堂上教你人生大道理，舞台上直接教你什麼叫做 High 到最高點！平常佬獅用麥克風講課，今天用麥克風征服全場。請注意：本次課程不點名，但會點燃全場氣氛！這次沒有作業、沒有考試，只有尖叫、掌聲和一場讓耳朵升級的現場體驗！','/api/themes/images/theme_1.jpg'),
('ORG0000001','ACTIVE',N'2026 五月天回到那一天',N'25週年巡迴演唱會台北站正式啟動，帶領歌迷穿梭時光隧道重溫經典歌曲。無論您是從哪一年開始加入五月天，今天讓我們回到相遇的那一天，唱響共同的青春回憶。','/api/themes/images/theme_2.jpg'),
('ORG0000002','ACTIVE',N'2026 台北國際動漫展',N'全台動漫迷不容錯過的年度盛會！現場展出各類人氣作品周邊、限量珍藏模型，並邀請多位知名聲優與作家舉辦見面簽名會，帶您進入熱血的二次元世界。','/api/themes/images/theme_3.jpg'),
('ORG0000003','ACTIVE',N'2026 AI 科技論壇',N'人工智慧趨勢分享，集結頂尖學者與產業領袖，共同探討生成式人工智慧與深度學習的最新趨勢。論壇將深入分析 AI 在各行業的創新應用，為未來的科技轉型勾勒出清晰藍圖。','/api/themes/images/theme_4.jpg'),
('ORG0000004','ACTIVE',N'2026 夏日音樂祭',N'多組藝人輪番演出，夏日必備的戶外音樂派對，多組知名藝人與樂團輪番登台演出。在蔚藍晴空下享受最純粹的節奏律動，讓熱情高亢的旋律陪伴您度過一個難忘的狂歡週末。','/api/themes/images/theme_5.jpg'),
('ORG0000005','ACTIVE',N'2026 寵物博覽會',N'超過百家品牌參展，匯聚百家頂尖寵物食品與用品品牌，提供毛孩食衣住行全方位服務。現場設有健康諮詢與趣味競賽，歡迎帶著心愛的寵物一起來同樂踩點。','/api/themes/images/theme_6.jpg'),
('ORG0000001','ACTIVE',N'2026 電競嘉年華',N'電競明星交流賽，熱血沸騰的電競盛宴！不僅有電競明星對抗賽與人氣戰隊見面會，現場更開放最新硬體設備體驗，邀請所有熱愛遊戲的玩家一同見證極速對決的魅力。','/api/themes/images/theme_7.jpg'),
('ORG0000002','ACTIVE',N'2026 台北美食節',N'超過100家美食攤位，集結上百家經典台灣小吃與異國精緻料理，帶給您一場最極致的味覺饗宴。不論是米其林推薦餐廳還是排隊名店，所有美味佳餚一次滿足您的老饕味蕾。','/api/themes/images/theme_8.jpg'),
('ORG0000003','ACTIVE',N'2026 國際旅展',N'旅遊優惠與行程規劃，集結各大旅行社、航空公司與五星級飯店，推出年度最優惠的行程方案。不論您計劃國內深度旅遊還是出國度假，現場專人諮詢與超值促銷幫您輕鬆實現旅行夢。','/api/themes/images/theme_9.jpg'),
('ORG0000004','ACTIVE',N'2026 創業交流論壇',N'創業家經驗分享，專為夢想與創新者打造的交流平台，邀請成功創業家分享寶貴的心路歷程與實戰經驗。現場將深度剖析市場趨勢，協助您擴展商業人脈並啟發無限的創業靈感。','/api/themes/images/theme_10.jpg'),
('ORG0000005','ACTIVE',N'2026 電影首映會',N'年度大片上映，年度史詩大片的全球首映盛宴，邀請多位巨星蒞臨共襄盛舉。最震撼的視聽特效與高張力的劇情，將在寬螢幕上完美呈現，帶給您絕無僅有的觀影享受。','/api/themes/images/theme_11.jpg'),
('ORG0000001','ACTIVE',N'2026 籃球明星賽',N'明星球員同場競技，頂尖職業籃球員齊聚一堂，展現高超球技與絕妙團隊默契。熱血噴張的灌籃大賽與神準的三分球對決，將點燃球場上的每分每秒，帶來最頂級的運動盛宴。','https://picsum.photos/seed/theme12/1920/1080'),
('ORG0000002','ACTIVE',N'2026 遊戲展',N'最新遊戲搶先體驗，年度最受期待的遊戲新品發表會，提供多款未上市大作的現場搶先試玩。現場還有知名實況主與玩家近距離互動，邀請您一同探索數位娛樂的未來世界。','https://picsum.photos/seed/theme13/1920/1080'),
('ORG0000003','ACTIVE',N'2026 手作市集',N'文創商品與手作品，支持本土原創設計，市集網羅各式精緻的手工皮革、金工飾品與質感生活器物。每一件作品都凝聚了職人的熱情與溫度，等待您來發掘屬於自己的生活美學。','https://picsum.photos/seed/theme14/1920/1080'),
('ORG0000004','ACTIVE',N'2026 台北書展',N'知名作者現場簽書，這是一場充滿墨香與思維碰撞的文化閱讀盛宴，邀請多位重量級作家現場與讀者面對面簽書分享。現場規劃豐富的主題展區與講座，與您一同漫遊在文字的無垠宇宙中。','https://picsum.photos/seed/theme15/1920/1080'),
('ORG0000005','ACTIVE',N'2026 健身博覽會',N'健身器材與體驗課程，匯集最新穎的重訓器材、高機能運動服飾與專業營養品，並由明星教練現場指導體驗課程。無論是健身愛好者還是剛入門的新手，都能在這裡找到最適合的運動方案。','https://picsum.photos/seed/theme16/1920/1080'),
('ORG0000001','ACTIVE',N'2026 國際車展',N'最新車款展示，各大車廠年度旗艦車款與未來概念車首度亮相，展示最前沿的自動駕駛與新能源科技。現場融合了極致工藝美學與豪華內裝，為愛車人士呈現一場科技與速度的視覺饗宴。','https://picsum.photos/seed/theme17/1920/1080'),
('ORG0000002','ACTIVE',N'2026 電影動漫節',N'動畫與電影交流盛會，動漫迷與影迷的跨界狂歡！現場集結經典動畫周邊商品、限量電影海報，並特別企劃 Cosplay 大賽與名家簽名會，帶您身歷其境體驗二次元的無限創意。','https://picsum.photos/seed/theme18/1920/1080'),
('ORG0000003','ACTIVE',N'2026 夏日路跑',N'全民運動活動，在陽光灑落的夏日清晨，揮灑汗水與千名跑友共同奔跑在綠意盎然的賽道上。活動不限年齡與程度，現場更設有歡樂補給站，讓大家享受健康與活力交織的快樂旅程。','https://picsum.photos/seed/theme19/1920/1080'),
('ORG0000004','ACTIVE',N'2026 VR體驗展',N'沉浸式互動展示，運用最先進的虛擬實境技術，帶領您穿梭於超現實的奇幻世界中進行沉浸式互動。突破空間與感官極限的視聽科技，將帶給您前所未有的震撼娛樂體驗。','https://picsum.photos/seed/theme20/1920/1080'),

-- DRAFT
('ORG0000001','DRAFT',N'2026 米老鼠全星會',N'邀請米妮同台演出...','https://picsum.photos/seed/theme21/1920/1080'),
('ORG0000002','DRAFT',N'2026 夏季旅遊節',N'活動規劃中...','https://picsum.photos/seed/theme22/1920/1080'),
('ORG0000003','DRAFT',N'2026 程式論壇',N'內容製作中...','https://picsum.photos/seed/theme23/1920/1080'),
('ORG0000004','DRAFT',N'2026 世界甜點展',N'招商中...','https://picsum.photos/seed/theme24/1920/1080'),
('ORG0000005','DRAFT',N'2026 VR體驗營',N'內容規劃中...','https://picsum.photos/seed/theme25/1920/1080'),
('ORG0000001','DRAFT',N'2026 攝影展',N'活動規劃中...','https://picsum.photos/seed/theme26/1920/1080'),
('ORG0000002','DRAFT',N'2026 桌遊展',N'邀請創作者參展...','https://picsum.photos/seed/theme27/1920/1080'),
('ORG0000003','DRAFT',N'2026 AI高峰會',N'尚未公開...','https://picsum.photos/seed/theme28/1920/1080'),
('ORG0000004','DRAFT',N'2026 美妝展',N'招商中...','https://picsum.photos/seed/theme29/1920/1080'),
('ORG0000005','DRAFT',N'2026 科技體驗營',N'內容規劃中...','https://picsum.photos/seed/theme30/1920/1080'),

-- ARCHIVED
('ORG0000001','ARCHIVED',N'2025 夏日音樂節',N'活動已結束...','https://picsum.photos/seed/theme36/1920/1080'),
('ORG0000002','ARCHIVED',N'2025 書展',N'活動已結束...','https://picsum.photos/seed/theme37/1920/1080'),
('ORG0000003','ARCHIVED',N'2025 國際旅展',N'活動已結束...','https://picsum.photos/seed/theme38/1920/1080'),
('ORG0000004','ARCHIVED',N'2025 遊戲博覽會',N'活動已結束...','https://picsum.photos/seed/theme39/1920/1080'),
('ORG0000005','ARCHIVED',N'2025 電競決賽',N'活動已結束...','https://picsum.photos/seed/theme40/1920/1080'),

-- DELETED
('ORG0000001','DELETED',N'測試資料1',N'邏輯刪除資料...','https://picsum.photos/seed/theme51/1920/1080'),
('ORG0000002','DELETED',N'測試資料2',N'邏輯刪除資料...','https://picsum.photos/seed/theme52/1920/1080'),
('ORG0000003','DELETED',N'測試資料3',N'邏輯刪除資料...','https://picsum.photos/seed/theme53/1920/1080'),
('ORG0000004','DELETED',N'測試資料4',N'邏輯刪除資料...','https://picsum.photos/seed/theme54/1920/1080'),
('ORG0000005','DELETED',N'測試資料5',N'邏輯刪除資料...','https://picsum.photos/seed/theme55/1920/1080');





-- 新增 Session 活動場次 (關聯 theme_id = 1, location_id = 1)
INSERT INTO session (theme_id, location_id, status, title, detail, publish_time, selling_start_time, selling_end_time, start_time, end_time)
VALUES 
(1, 1, 'ACTIVE', N'台北夏季首場', N'馬佬獅帶您燃燒盛夏之夜', '2026-06-01 12:00:00', '2026-06-15 12:00:00', '2026-07-09 18:00:00', '2026-07-10 19:30:00', '2026-07-10 22:30:00'),
(1, 1, 'ACTIVE', N'台北夏季第二場', N'馬佬獅帶您燃燒盛夏之夜', '2026-06-01 12:00:00', '2026-06-15 12:00:00', '2026-07-10 18:00:00', '2026-07-11 19:30:00', '2026-07-11 22:30:00'),
(1, 1, 'ACTIVE', N'台北夏季第三場', N'馬佬獅帶您燃燒盛夏之夜', '2026-06-01 12:00:00', '2026-06-15 12:00:00', '2026-07-11 18:00:00', '2026-07-12 19:30:00', '2026-07-12 22:30:00'),
(1, 1, 'DRAFT', N'台北夏季加演場', N'馬佬獅加碼狂歡', '2026-05-01 12:00:00', '2026-05-15 12:00:00', '2026-06-28 18:00:00', '2026-06-29 19:30:00', '2026-06-29 22:30:00'),
-- 五月天巡迴演唱會 (theme_id = 2, location_id = 2 高雄巨蛋)
(2, 2, 'ACTIVE', N'五月天高雄首場', N'回到那一天', '2026-10-01 12:00:00', '2026-11-15 12:00:00', '2026-12-25 18:00:00', '2026-12-25 19:00:00', '2026-12-25 23:00:00'),
(2, 2, 'ACTIVE', N'五月天高雄第二場', N'回到那一天', '2026-10-01 12:00:00', '2026-11-15 12:00:00', '2026-12-26 18:00:00', '2026-12-26 19:00:00', '2026-12-26 23:00:00'),
(2, 2, 'ACTIVE', N'五月天高雄第三場', N'回到那一天', '2026-10-01 12:00:00', '2026-11-15 12:00:00', '2026-12-27 18:00:00', '2026-12-27 19:00:00', '2026-12-27 23:00:00'),
-- 六月花巡迴演唱會 (theme_id = 3, location_id = 3 台北流行音樂中心)
(3, 3, 'ACTIVE', N'六月花台北首場', N'六月花開了', '2026-09-01 12:00:00', '2026-09-15 12:00:00', '2026-11-20 18:30:00', '2026-11-20 19:30:00', '2026-11-20 22:30:00'),
(3, 3, 'ACTIVE', N'六月花台北第二場', N'六月花開了', '2026-09-01 12:00:00', '2026-09-15 12:00:00', '2026-11-21 18:30:00', '2026-11-21 19:30:00', '2026-11-21 22:30:00'),
(3, 3, 'ACTIVE', N'六月花台北第三場', N'六月花開了', '2026-09-01 12:00:00', '2026-09-15 12:00:00', '2026-11-22 18:30:00', '2026-11-22 19:30:00', '2026-11-22 22:30:00'),
(4, 1, 'ACTIVE', N'AI 科技論壇主場', N'人工智慧趨勢分享','2026-06-01 12:00:00','2026-06-10 12:00:00','2026-07-10 18:00:00','2026-07-11 09:00:00','2026-07-11 17:00:00'),
(5, 1, 'ACTIVE', N'夏日音樂祭首日', N'戶外音樂舞台','2026-06-05 12:00:00','2026-06-15 12:00:00','2026-07-20 18:00:00','2026-07-21 18:00:00','2026-07-21 23:00:00'),
(6, 1, 'ACTIVE', N'寵物博覽會開幕', N'萌寵互動展','2026-06-10 12:00:00','2026-06-20 12:00:00','2026-08-01 18:00:00','2026-08-02 10:00:00','2026-08-02 18:00:00'),
(7, 1, 'ACTIVE', N'電競嘉年華主舞台', N'LOL & FPS 表演賽','2026-06-12 12:00:00','2026-06-22 12:00:00','2026-08-10 18:00:00','2026-08-11 13:00:00','2026-08-11 20:00:00'),
(8, 1, 'ACTIVE', N'台北美食節開幕日', N'百家攤位進駐','2026-06-15 12:00:00','2026-06-25 12:00:00','2026-08-15 18:00:00','2026-08-16 11:00:00','2026-08-16 21:00:00'),
(9, 2, 'ACTIVE', N'國際旅展主場', N'旅遊優惠展售','2026-06-18 12:00:00','2026-06-28 12:00:00','2026-08-20 18:00:00','2026-08-21 10:00:00','2026-08-21 18:00:00'),
(10, 2, 'ACTIVE', N'創業論壇主題日', N'創業家分享會','2026-06-20 12:00:00','2026-06-30 12:00:00','2026-08-25 18:00:00','2026-08-26 09:00:00','2026-08-26 17:00:00'),
(11, 2, 'ACTIVE', N'電影首映會', N'年度大片首映','2026-06-22 12:00:00','2026-07-01 12:00:00','2026-09-01 18:00:00','2026-09-02 19:00:00','2026-09-02 22:00:00'),
(12, 1, 'ACTIVE', N'籃球明星賽', N'明星對抗賽','2026-06-25 12:00:00','2026-07-05 12:00:00','2026-09-10 18:00:00','2026-09-11 18:00:00','2026-09-11 21:00:00'),
(13, 1, 'ACTIVE', N'遊戲展主場', N'最新遊戲體驗','2026-06-28 12:00:00','2026-07-08 12:00:00','2026-09-15 18:00:00','2026-09-16 10:00:00','2026-09-16 18:00:00'),
(14, 1, 'ACTIVE', N'手作市集主日', N'文創展售','2026-07-01 12:00:00','2026-07-10 12:00:00','2026-09-20 18:00:00','2026-09-21 11:00:00','2026-09-21 19:00:00'),
(15, 1, 'ACTIVE', N'台北書展主場', N'作者簽書會','2026-07-03 12:00:00','2026-07-12 12:00:00','2026-09-25 18:00:00','2026-09-26 10:00:00','2026-09-26 18:00:00'),
(16, 1, 'ACTIVE', N'健身博覽會主日', N'健身體驗課','2026-07-05 12:00:00','2026-07-15 12:00:00','2026-10-01 18:00:00','2026-10-02 10:00:00','2026-10-02 18:00:00'),
(17, 2, 'ACTIVE', N'國際車展首日', N'新車展示','2026-07-07 12:00:00','2026-07-17 12:00:00','2026-10-05 18:00:00','2026-10-06 10:00:00','2026-10-06 18:00:00'),
(18, 2, 'ACTIVE', N'動漫電影節', N'動畫展演','2026-07-10 12:00:00','2026-07-20 12:00:00','2026-10-10 18:00:00','2026-10-11 10:00:00','2026-10-11 20:00:00'),
(19, 2, 'ACTIVE', N'夏日路跑', N'全民運動活動','2026-07-12 12:00:00','2026-07-22 12:00:00','2026-10-15 18:00:00','2026-10-16 07:00:00','2026-10-16 12:00:00'),
(20, 2, 'ACTIVE', N'VR體驗展', N'沉浸式科技體驗','2026-07-15 12:00:00','2026-07-25 12:00:00','2026-10-20 18:00:00','2026-10-21 10:00:00','2026-10-21 18:00:00');

/* -----------------------------------------
    3. 票務模塊
   ----------------------------------------- */
-- 新增 Ticket_Type 票種
INSERT INTO ticket_type (theme_id, name, price, color)
VALUES 
(1, N'搖滾特區', 4500.00, '#ec4899'),
(1, N'看台區', 2800.00, '#10b981');

-- 新增 Session_Zone_Mapping 中介表資料 (定義商業配對規則)
-- ✨ 新增 Session_Zone_Mapping 中介表資料 (定義商業配對規則)
INSERT INTO session_zone_mapping (session_id, zone_id, ticket_type_id, is_enabled, created_by, updated_by)
VALUES 
-- 規則 1：跨年首場 (Session 1) 的 特區 (Zone 1) 賣 搖滾特區(Type 1)
(1, 1, 1, 1, 'USR0000003', 'USR0000003'), 
-- 規則 2：跨年首場 (Session 1) 的 黃2B區 (Zone 2) 賣 看台區 (Type 2)
(1, 2, 2, 1, 'USR0000003', 'USR0000003');


-- 新增 Ticket 實體票券庫存 (將 場次、座位、票種 綁定在一起)
-- ⚠️ 架構提示：有了中介表後，未來實體票是靠「讀取 Mapping 規則」批次產生的
-- 但為了目前前端畫面測試，我們先手動塞入這張對應的實體票：
INSERT INTO ticket (ticket_id, session_id, seat_id, ticket_type_id, status)
VALUES
(1, 1, 1, 1, 1), -- 庫存1: 跨年首場 + 搖滾區1排1號 + 4500元 (可售)
(2, 1, 2, 1, 1), -- 庫存2: 跨年場 + 搖滾區1排2號 + 4500元 (可售)
(3, 1, 3, 2, 1); -- 庫存3: 跨年場 + 看台區10排5號 + 2800元 (可售)

-- =========================================
-- [Jason 2026-07-06] 訂票流程 seed 補完
-- 問題：所有場次都綁 location 1/2/3，但這三個場館沒有 bound_svg（座位預覽一片空白），
--       且除 session 1 (僅 3 席) 外沒有任何 mapping/庫存可賣。
-- 本區塊讓 session 1、3、11 可完整走「選區 → 選位 → 結帳」流程：
--   1) 補 location 1 的 raw_svg / bound_svg（data-zone-id 對應 zone 1、2）
--   2) 補 zone 1 (特區) 至 20 席、zone 2 (看台) 至 24 席（seat_id 11~53，避開 951+ 與 seq 1000+）
--   3) 補 theme 4 (AI 科技論壇) 票種（IDENTITY 接續為 3、4）
--   4) 補 session 3、11 的 session_zone_mapping
--   5) 補 session 1/3/11 全席次 ticket 庫存（ticket_id 4~132，低於 ticket_seq 起始值 1000）
-- =========================================

-- 1) location 1 (台北小巨蛋) 場地 SVG：兩個分區，data-zone-id 對應 zone 1 (特區)、zone 2 (黃2B區看台)
UPDATE location SET
raw_svg = N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>

      <path d="M 200,150 L 600,150 L 600,320 L 200,320 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="1"></path>
      <text x="400" y="245" font-family="sans-serif" font-size="18" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 (搖滾區)</text>

      <path d="M 120,370 L 680,370 L 680,540 L 120,540 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer;" data-zone-id="2"></path>
      <text x="400" y="465" font-family="sans-serif" font-size="18" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2B區 (看台)</text>
    </svg>',
bound_svg = N'<svg viewBox="0 0 800 600" xmlns="http://www.w3.org/2000/svg" style="width: 100%; height: auto;">
      <rect x="250" y="30" width="300" height="60" fill="#334155" rx="8"></rect>
      <text x="400" y="68" font-family="sans-serif" font-size="24" font-weight="bold" fill="#fff" text-anchor="middle">STAGE 舞台區</text>

      <path d="M 200,150 L 600,150 L 600,320 L 200,320 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(236, 72, 153); transition: fill 0.3s;" data-zone-id="1"></path>
      <text x="400" y="245" font-family="sans-serif" font-size="18" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">特區 (搖滾區)</text>

      <path d="M 120,370 L 680,370 L 680,540 L 120,540 Z" fill="#f1f5f9" stroke="#cbd5e1" stroke-width="2" style="cursor: pointer; fill: rgb(16, 185, 129); transition: fill 0.3s;" data-zone-id="2"></path>
      <text x="400" y="465" font-family="sans-serif" font-size="18" font-weight="bold" fill="#64748b" text-anchor="middle" pointer-events="none">黃2B區 (看台)</text>
    </svg>'
WHERE location_id = 1;

-- 2) 補座位：zone 1 (特區) 原有 seat 1、2，補至 4 排 x 5 席 = 20 席
INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(11, 1, 1, 3, 3, 1), (12, 1, 1, 4, 4, 1), (13, 1, 1, 5, 5, 1),
(14, 1, 2, 1, 1, 2), (15, 1, 2, 2, 2, 2), (16, 1, 2, 3, 3, 2), (17, 1, 2, 4, 4, 2), (18, 1, 2, 5, 5, 2),
(19, 1, 3, 1, 1, 3), (20, 1, 3, 2, 2, 3), (21, 1, 3, 3, 3, 3), (22, 1, 3, 4, 4, 3), (23, 1, 3, 5, 5, 3),
(24, 1, 4, 1, 1, 4), (25, 1, 4, 2, 2, 4), (26, 1, 4, 3, 3, 4), (27, 1, 4, 4, 4, 4), (28, 1, 4, 5, 5, 4);

-- zone 2 (黃2B區看台) 原有 seat 3 (10排5號)，補至 3 排 x 8 席 = 24 席
INSERT INTO seat (seat_id, zone_id, row_num, seat_num, x_index, y_index) VALUES
(31, 2, 10, 1, 1, 10), (32, 2, 10, 2, 2, 10), (33, 2, 10, 3, 3, 10), (34, 2, 10, 4, 4, 10),
(35, 2, 10, 6, 6, 10), (36, 2, 10, 7, 7, 10), (37, 2, 10, 8, 8, 10),
(38, 2, 11, 1, 1, 11), (39, 2, 11, 2, 2, 11), (40, 2, 11, 3, 3, 11), (41, 2, 11, 4, 4, 11),
(42, 2, 11, 5, 5, 11), (43, 2, 11, 6, 6, 11), (44, 2, 11, 7, 7, 11), (45, 2, 11, 8, 8, 11),
(46, 2, 12, 1, 1, 12), (47, 2, 12, 2, 2, 12), (48, 2, 12, 3, 3, 12), (49, 2, 12, 4, 4, 12),
(50, 2, 12, 5, 5, 12), (51, 2, 12, 6, 6, 12), (52, 2, 12, 7, 7, 12), (53, 2, 12, 8, 8, 12);

-- 3) theme 4 (AI 科技論壇) 票種（IDENTITY 接續 → ticket_type_id = 3、4）
INSERT INTO ticket_type (theme_id, name, price, color)
VALUES
(4, N'論壇 VIP 席', 1200.00, '#6366f1'),
(4, N'論壇一般席', 600.00, '#f59e0b');

-- 4) session 3 (台北夏季第三場, theme 1) 與 session 11 (AI 科技論壇主場, theme 4) 的配對規則
INSERT INTO session_zone_mapping (session_id, zone_id, ticket_type_id, is_enabled, created_by, updated_by)
VALUES
(3, 1, 1, 1, 'USR0000003', 'USR0000003'),
(3, 2, 2, 1, 'USR0000003', 'USR0000003'),
(11, 1, 3, 1, 'USR0000003', 'USR0000003'),
(11, 2, 4, 1, 'USR0000003', 'USR0000003');

-- 5) 票券庫存 (status 1 = 可售)
-- session 1 補齊新座位 (原已有 ticket 1~3 對 seat 1~3)
INSERT INTO ticket (ticket_id, session_id, seat_id, ticket_type_id, status) VALUES
(4, 1, 11, 1, 1), (5, 1, 12, 1, 1), (6, 1, 13, 1, 1), (7, 1, 14, 1, 1), (8, 1, 15, 1, 1),
(9, 1, 16, 1, 1), (10, 1, 17, 1, 1), (11, 1, 18, 1, 1), (12, 1, 19, 1, 1), (13, 1, 20, 1, 1),
(14, 1, 21, 1, 1), (15, 1, 22, 1, 1), (16, 1, 23, 1, 1), (17, 1, 24, 1, 1), (18, 1, 25, 1, 1),
(19, 1, 26, 1, 1), (20, 1, 27, 1, 1), (21, 1, 28, 1, 1),
(22, 1, 31, 2, 1), (23, 1, 32, 2, 1), (24, 1, 33, 2, 1), (25, 1, 34, 2, 1), (26, 1, 35, 2, 1),
(27, 1, 36, 2, 1), (28, 1, 37, 2, 1), (29, 1, 38, 2, 1), (30, 1, 39, 2, 1), (31, 1, 40, 2, 1),
(32, 1, 41, 2, 1), (33, 1, 42, 2, 1), (34, 1, 43, 2, 1), (35, 1, 44, 2, 1), (36, 1, 45, 2, 1),
(37, 1, 46, 2, 1), (38, 1, 47, 2, 1), (39, 1, 48, 2, 1), (40, 1, 49, 2, 1), (41, 1, 50, 2, 1),
(42, 1, 51, 2, 1), (43, 1, 52, 2, 1), (44, 1, 53, 2, 1);

-- session 3 全席次 (特區 → 搖滾特區 4500, 看台 → 看台區 2800)
INSERT INTO ticket (ticket_id, session_id, seat_id, ticket_type_id, status) VALUES
(45, 3, 1, 1, 1), (46, 3, 2, 1, 1), (47, 3, 11, 1, 1), (48, 3, 12, 1, 1), (49, 3, 13, 1, 1),
(50, 3, 14, 1, 1), (51, 3, 15, 1, 1), (52, 3, 16, 1, 1), (53, 3, 17, 1, 1), (54, 3, 18, 1, 1),
(55, 3, 19, 1, 1), (56, 3, 20, 1, 1), (57, 3, 21, 1, 1), (58, 3, 22, 1, 1), (59, 3, 23, 1, 1),
(60, 3, 24, 1, 1), (61, 3, 25, 1, 1), (62, 3, 26, 1, 1), (63, 3, 27, 1, 1), (64, 3, 28, 1, 1),
(65, 3, 3, 2, 1), (66, 3, 31, 2, 1), (67, 3, 32, 2, 1), (68, 3, 33, 2, 1), (69, 3, 34, 2, 1),
(70, 3, 35, 2, 1), (71, 3, 36, 2, 1), (72, 3, 37, 2, 1), (73, 3, 38, 2, 1), (74, 3, 39, 2, 1),
(75, 3, 40, 2, 1), (76, 3, 41, 2, 1), (77, 3, 42, 2, 1), (78, 3, 43, 2, 1), (79, 3, 44, 2, 1),
(80, 3, 45, 2, 1), (81, 3, 46, 2, 1), (82, 3, 47, 2, 1), (83, 3, 48, 2, 1), (84, 3, 49, 2, 1),
(85, 3, 50, 2, 1), (86, 3, 51, 2, 1), (87, 3, 52, 2, 1), (88, 3, 53, 2, 1);

-- session 11 全席次 (特區 → 論壇 VIP 1200, 看台 → 論壇一般 600)
INSERT INTO ticket (ticket_id, session_id, seat_id, ticket_type_id, status) VALUES
(89, 11, 1, 3, 1), (90, 11, 2, 3, 1), (91, 11, 11, 3, 1), (92, 11, 12, 3, 1), (93, 11, 13, 3, 1),
(94, 11, 14, 3, 1), (95, 11, 15, 3, 1), (96, 11, 16, 3, 1), (97, 11, 17, 3, 1), (98, 11, 18, 3, 1),
(99, 11, 19, 3, 1), (100, 11, 20, 3, 1), (101, 11, 21, 3, 1), (102, 11, 22, 3, 1), (103, 11, 23, 3, 1),
(104, 11, 24, 3, 1), (105, 11, 25, 3, 1), (106, 11, 26, 3, 1), (107, 11, 27, 3, 1), (108, 11, 28, 3, 1),
(109, 11, 3, 4, 1), (110, 11, 31, 4, 1), (111, 11, 32, 4, 1), (112, 11, 33, 4, 1), (113, 11, 34, 4, 1),
(114, 11, 35, 4, 1), (115, 11, 36, 4, 1), (116, 11, 37, 4, 1), (117, 11, 38, 4, 1), (118, 11, 39, 4, 1),
(119, 11, 40, 4, 1), (120, 11, 41, 4, 1), (121, 11, 42, 4, 1), (122, 11, 43, 4, 1), (123, 11, 44, 4, 1),
(124, 11, 45, 4, 1), (125, 11, 46, 4, 1), (126, 11, 47, 4, 1), (127, 11, 48, 4, 1), (128, 11, 49, 4, 1),
(129, 11, 50, 4, 1), (130, 11, 51, 4, 1), (131, 11, 52, 4, 1), (132, 11, 53, 4, 1);


/* -----------------------------------------
    4. 商城周邊模塊
   ----------------------------------------- */
-- 新增 product_categories 商品分類 (先建父類，再建子類)
INSERT INTO product_categories (category_name, parent_id) VALUES (N'官方周邊', NULL); -- ID: 1
INSERT INTO product_categories (category_name, parent_id) VALUES (N'上衣', 1);       -- ID: 2
INSERT INTO product_categories (category_name, parent_id) VALUES (N'外套', 1);       -- ID: 3
INSERT INTO product_categories (category_name, parent_id) VALUES (N'下身', 1);       -- ID: 4
INSERT INTO product_categories (category_name, parent_id) VALUES (N'帽子', 1);       -- ID: 5
INSERT INTO product_categories (category_name, parent_id) VALUES (N'襪子', 1);       -- ID: 6
INSERT INTO product_categories (category_name, parent_id) VALUES (N'毛巾', 1);       -- ID: 7
INSERT INTO product_categories (category_name, parent_id) VALUES (N'其他周邊', 1);    -- ID: 8


-- 新增 products 商品主檔
INSERT INTO products (theme_id, category_id, product_name, product_sim_description, product_description, status)
VALUES 
(1, 2, N'巡迴紀念 T-shirt', N'100%純棉，演唱會必備', N'詳細尺寸表請參考官網說明...', 1),
(1, 8, N'互動式中控螢光棒', N'雲端中控變色', N'請下載官方APP綁定座位...', 1);

-- 新增 product_variants 商品款式
INSERT INTO product_variants (product_id, org_skuNo, product_size, product_color, unit_price, stock_qty, barcode, status)
VALUES 
(1, 'JO-CT-BLK-M', 'M', N'黑色', 890.00, 100, 'TSHIRT-M-BLK', 1),
(1, 'JO-CT-BLK-L', 'L', N'黑色', 890.00, 100, 'TSHIRT-L-BLK', 1),
(2, 'JO-OT-WH', 'FREE', N'黑色', 149.00, 500, 'LIGHTSTICK-01', 1);

-- 新增 product_images 商品假圖
INSERT INTO product_images (product_id, image_url, sort_order, is_main)
VALUES 
(1, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1783235967/T-Shirt_Ma_fz72ah.png', 1, 1),
(2, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1783235968/Stick_Ma_fli7ze.png', 1, 1);

-- -- 新增 merch_cart 購物車 (模擬會員阿明把商品加進購物車)
-- INSERT INTO merch_cart (user_id, variant_id, quantity)
-- VALUES 
-- ('USR0000020', 1, 1), -- 買1件M號衣服
-- ('USR0000020', 3, 2); -- 買2支螢光棒


/* -----------------------------------------
    5. 訂單與結算模塊
   ----------------------------------------- */
-- 新增 ticket_orders 票券訂單主檔 (模擬阿明買了一張票)
-- [整合決策 2026-07-04] 訂單/結算種子改採訂單組「即時結算制度」：維持空表，由真實下單流程產生資料。
-- 若 demo 需要預載資料，取消下列註解即可還原（值為 Jason 版種子）。
-- INSERT INTO ticket_orders (t_order_id, user_id, total_amount, contact_name, contact_email, contact_phone)
-- VALUES ('TO26060401', 'USR0000020', 4500.00, N'王阿明', 'ming@example.com', '0912345678');

-- 新增 ticket_order_details 票券訂單明細 (買走了庫存 ticket_id = 1 的票)
-- INSERT INTO ticket_order_details (t_detail_id, t_order_id, ticket_id, unit_price, real_name, identity_number, item_status, is_used)
-- VALUES ('TOD-0001', 'TO26060401', 1, 4500.00, N'王阿明', 'A123456789', 'NORMAL', 'Unredeemed');

-- 新增 merch_orders 商城訂單主檔 (模擬阿明結帳周邊)
-- INSERT INTO merch_orders (m_order_id, user_id, total_amount, payment_status, paid_at)
-- VALUES ('MO26060401', 'USR0000020', 1188.00, 'PAID', GETDATE());

-- 新增 merch_order_details 商城訂單明細
-- INSERT INTO merch_order_details (m_detail_id, m_order_id, product_id, variant_id, quantity, unit_price, item_status)
-- VALUES
-- ('MOD-0001', 'MO26060401', 1, 1, 1, 890.00, 'NORMAL'),
-- ('MOD-0002', 'MO26060401', 2, 3, 2, 149.00, 'NORMAL');

-- 新增 merchant_settlements 廠商結算 (模擬主辦方月底結算帳務)
-- INSERT INTO merchant_settlements (settlement_id, organizer_id, period_start, period_end, total_orders_amount, final_payout_amount, item_status)
-- VALUES ('SETTLE-2605', 'ORG0000001', '2026-05-01', '2026-05-31', 500000.00, 475000.00, 'PENDING');


-- 新增 auction 競標商品（少量測試資料）
INSERT INTO auction (theme_id, status, title, detail, image, start_price, buyout_price, current_price, start_time, end_time)
VALUES                                                                  
(1, 'ACTIVE',   N'馬佬獅簽名海報', N'官方親簽海報，附活動紀念章。', '/api/auctions/images/auction_1.png', 1000.00, 5000.00, 1800.00, '2026-06-20 10:00:00', '2026-07-10 22:00:00'),
(2, 'DRAFT',    N'五月天後台體驗券',N'含彩排參觀與合照資格。','/api/auctions/images/auction_2.jpg', 3000.00, 12000.00, 3000.00, '2026-07-01 10:00:00', '2026-07-20 22:00:00'),
(3, 'ARCHIVED', N'動漫展限定福袋',N'內含限定模型與周邊。','https://picsum.photos/seed/auction3/1000/1000', 800.00,  3000.00, 2200.00, '2026-05-01 10:00:00', '2026-05-15 22:00:00'),
(1, 'ACTIVE', N'傳奇圖騰紀念幣組',N'純銀打造的馬（Apollo）蹄鐵圖騰， 24K 鍍金的獅子（Leo）獸爪圖騰。','/api/auctions/images/auction_4.png', 800.00,  9000.00, 1000.00, '2026-07-05 10:00:00', '2026-07-09 22:00:00'),
(1, 'ACTIVE', N'專屬鍍金黑膠唱片相框組',N'經典曲目刻錄在 12 吋的「鍍金黑膠唱片」',        '/api/auctions/images/auction_5.png', 800.00,  8000.00, 1100.00, '2026-07-05 10:00:00', '2026-07-09 10:00:00');
-- 新增 bid 出價紀錄（對應 auction_id 1、3）
INSERT INTO bid (auction_id, user_id, bid_price)
VALUES
(1, 'USR0000018', 1200.00),
(1, 'USR0000019', 1500.00),
(1, 'USR0000020', 1800.00),
(3, 'USR0000016', 1000.00),
(3, 'USR0000017', 1500.00),
(3, 'USR0000018', 2200.00),
(4, 'USR0000018', 1000.00),
(5, 'USR0000018', 1100.00);

-- ================================================================
-- 重新設定序列起始值，避免與硬編碼種子資料主鍵衝突
-- 載入後請對齊各 SEQUENCE：ALTER SEQUENCE seq_XXX RESTART WITH <現有最大號+1>
-- ================================================================
ALTER SEQUENCE seq_USR RESTART WITH 21;
ALTER SEQUENCE seq_ORG RESTART WITH 8;
ALTER SEQUENCE seq_CON RESTART WITH 9;
ALTER SEQUENCE seq_MBR RESTART WITH 16;
ALTER SEQUENCE seq_SUB RESTART WITH 7;
ALTER SEQUENCE seq_ANN RESTART WITH 5;
ALTER SEQUENCE seq_USM RESTART WITH 5;
ALTER SEQUENCE seq_ROL RESTART WITH 7;


-- 商品新增(Ma)
INSERT INTO products (theme_id, category_id, product_name, product_sim_description, product_description, status)
SELECT     
    T.theme_id,
    T.category_id,
    T.p_name,
    T.p_sim_desc,
    T.p_desc,
    1
FROM (
    SELECT 1 AS theme_id, 8 AS category_id, N'小馬玩偶' AS p_name, N'小馬玩偶' AS p_sim_desc, N'澳洲天然羊毛材質' AS p_desc UNION ALL
    SELECT 1, 6, N'小馬中筒襪', N'小馬中筒襪', N'100%棉' UNION ALL
    SELECT 1, 7, N'小馬毛巾', N'小馬毛巾', N'100%棉'
) AS T;

INSERT INTO product_variants(product_id, org_skuno, product_color, product_size, unit_price, stock_qty, barcode, status)
SELECT P.product_id, V.sku, V.color, V.size, V.price, V.qty, V.bar, 1
FROM products P
CROSS APPLY (
    -- 在這裡為不同名稱的商品，對應綁定其尺寸、顏色與庫存
    SELECT N'小馬玩偶' AS name, 'finaraW' AS sku, N'質感白' AS color, 'FREE' AS size, 5980 AS price, 5000 AS qty, '4810000000021' AS bar UNION ALL
    SELECT N'小馬玩偶', 'finaraM', N'奶茶色', 'FREE', 5980, 2500, '4810000000022' UNION ALL
    SELECT N'小馬中筒襪', 'finaraSocks', N'紅色', 'FREE', 280, 1500, '4810000000023' UNION ALL
    SELECT N'小馬毛巾', 'finaraTowelY', N'沙灘黃', 'FREE', 350, 2000, '4810000000024' UNION ALL
    SELECT N'小馬毛巾', 'finaraTowelR', N'梅紅', 'FREE', 350, 2000, '4810000000025' UNION ALL
    SELECT N'小馬毛巾', 'finaraTowelB', N'湖水藍', 'FREE', 350, 2000, '4810000000026'
) AS V
WHERE P.product_name = V.name AND P.theme_id = 1;

INSERT INTO product_images
(
    product_id,
    image_url,
    sort_order,
    is_main
)
VALUES
(3, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782101697/Finara%E8%B2%BB%E7%B4%8D%E6%8B%89-%E6%BE%B3%E6%B4%B2%E7%BE%8E%E9%BA%97%E8%AB%BE%E7%BE%8A%E6%AF%9B-%E8%90%8C%E5%AF%B5%E5%B0%8F%E9%A6%AC_qwigvn.avif', 1, 1),

(3, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782101697/%E5%A5%B6%E8%8C%B6%E9%A6%AC_zwh4hf.avif', 2, 0),

(3, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782101676/%E7%99%BD%E9%A6%AC_efjy6b.avif', 3, 0),

(4, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782106060/HorseSocks_vauq84.webp', 1, 1),

(5, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782105931/Horse_Towel_wi6avt.webp', 1, 1),

(5, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782105904/13734672_02_002_R_xeisqt.webp', 2, 0),

(5, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782105904/Horse_Towel-b_ywccp7.webp', 3, 0),

(5, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782105904/13734672_02_003_y_fy4mng.webp', 4, 0);


-- 商品新增(June)
INSERT INTO products (theme_id, category_id, product_name, product_sim_description, product_description, status)
SELECT     
    T.theme_id,
    T.category_id,
    T.p_name,
    T.p_sim_desc,
    T.p_desc,
    1
FROM (
    SELECT 3 AS theme_id, 8 AS category_id, N'3D立體彩色繡球花花束卡片' AS p_name, N'3D立體彩色繡球花花束卡片' AS p_sim_desc, N'特種紙' AS p_desc UNION ALL
    SELECT 3, 8, N'繡球花系列香氛花藝-藍繡球', N'繡球花系列香氛花藝-藍繡球', N'材質：乳膠、塑膠複合媒材、人造絲綢布料' UNION ALL
    SELECT 3, 8, N'香氛蠟燭-繡球花', N'香氛蠟燭-繡球花', N'香味：清新'
) AS T;

INSERT INTO product_variants(product_id, org_skuno, product_color, product_size, unit_price, stock_qty, barcode, status)
SELECT P.product_id, V.sku, V.color, V.size, V.price, V.qty, V.bar, 1
FROM products P
CROSS APPLY (
    -- 在這裡為不同名稱的商品，對應綁定其尺寸、顏色與庫存
    SELECT N'3D立體彩色繡球花花束卡片' AS name, 'hydrangeasCd' AS sku, N'彩色' AS color, 'FREE' AS size, 324 AS price, 5000 AS qty, '4810000000027' AS bar UNION ALL
    SELECT N'繡球花系列香氛花藝-藍繡球', 'hydrangeasB', N'藍色', 'FREE', 1212, 2500, '4810000000028' UNION ALL
    SELECT N'香氛蠟燭-繡球花', 'hydrangeasCdl-PK', N'粉', 'FREE', 323, 1500, '4810000000029' UNION ALL
    SELECT N'香氛蠟燭-繡球花', 'hydrangeasCdl-PL', N'紫', 'FREE', 323, 1500, '4810000000030' UNION ALL
    SELECT N'香氛蠟燭-繡球花', 'hydrangeasCdl-GR', N'綠', 'FREE', 323, 1500, '4810000000031' UNION ALL
    SELECT N'香氛蠟燭-繡球花', 'hydrangeasCdl-BL', N'藍', 'FREE', 323, 1500, '4810000000032'
) AS V
WHERE P.product_name = V.name AND P.theme_id = 3;

INSERT INTO product_images
(
    product_id,
    image_url,
    sort_order,
    is_main
)
VALUES
(6, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782122503/hydrangeas_Card_oh4omt.jpg', 1, 1),

(7, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782122503/hydrangeasB_fa31bd.webp', 1, 1),

(8, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782122502/hydrangeasCdl-All_v5ta9m.webp', 1, 1),

(8, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782122503/hydrangeasCdl-PK_dka2wl.webp', 2, 0),

(8, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782122503/hydrangeasCdl-BL_d8cxd2.webp', 5, 0),

(8, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782122503/hydrangeasCdl-PL_mwwwl4.webp', 3, 0),

(8, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782122745/hydrangeasCdl-GR_vorhuf.webp', 4, 0);

-- 商品新增(MayDay)
INSERT INTO products (theme_id, category_id, product_name, product_sim_description, product_description, status)
SELECT     
    T.theme_id,
    T.category_id,
    T.p_name,
    T.p_sim_desc,
    T.p_desc,
    1
FROM (
    SELECT 2 AS theme_id, 5 AS category_id, N'〔五月天 #5525〕因為你所以五球變變變頭套｜知足球款' AS p_name, N'吊飾頭套1入' AS p_sim_desc, N'材質：100%聚酯纖維。商品圖因螢幕顯示、拍攝光線等因素會產生色差，以實際商品為準。' AS p_desc UNION ALL
    SELECT 2, 5, N'〔五月天 #5525〕因為你所以五球變變變頭套｜藍恐龍款', N'吊飾頭套1入', N'材質：100%聚酯纖維。商品圖因螢幕顯示、拍攝光線等因素會產生色差，以實際商品為準。' UNION ALL
    SELECT 2, 2, N'〔五月天 #5525〕回到侏儸紀那一天 五球怪恐龍T', N'五球怪恐龍T', N'材質：100%棉' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕5555 活到那一天 雲端互動版 LED 2.0', N'80歲螢光拐杖套組', N'材質：丙烯腈-丁二烯-苯乙烯共聚物 (ABS)｜聚碳酸酯 (PC)｜矽膠 (SILICONE)｜聚甲醛 (POM)｜熱塑性彈性體 (TPE)｜聚酯纖維 (POLYESTER)｜發光二極體 (LED)。內容物：螢光棒1支 五球座1座 掛繩1條 Type-C充電線1條 罩蓋2個。' UNION ALL
    SELECT 2, 5, N'〔五月天 #5525〕因為你所以五球變變變頭套｜馬不停蹄款', N'吊飾頭套1入', N'材質：100%聚酯纖維。商品圖因螢幕顯示、拍攝光線等因素會產生色差，以實際商品為準。' UNION ALL
    SELECT 2, 5, N'〔五月天 #5525〕因為你所以五球變變變頭套｜白馬王子款', N'吊飾頭套1入', N'材質：100%聚酯纖維。商品圖因螢幕顯示、拍攝光線等因素會產生色差，以實際商品為準。' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕晚安喵星人 哄你入睡保暖披風', N'毛毯１入', N'材質：聚酯纖維' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕當我們混掛在一起 多功能掛扣長織帶', N'織帶1入', N'材質：聚酯纖維織帶/聚丙烯扣件/鋅合金五金扣。' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕五公仔雪花音樂盒｜還你自由款', N'古銅色音樂盒1入', N'材質：高密度樹脂、純水、PVC塑膠、玻璃、金屬。' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕五公仔雪花音樂盒｜這是我的溫柔款', N'銀色音樂盒1入', N'材質：高密度樹脂、純水、PVC塑膠、玻璃、金屬。' UNION ALL
    SELECT 2, 5, N'〔五月天 #5525〕因為你所以五球變變變頭套｜聖誕夜驚魂款', N'吊飾頭套1入', N'材質：100%聚酯纖維。商品圖因螢幕顯示、拍攝光線等因素會產生色差，以實際商品為準。' UNION ALL
    SELECT 2, 3, N'〔五月天 #5525〕HOME RUN MD25棒球外套', N'外套1入', N'材質：羊毛＋聚酯纖維；裡布材料：聚酯纖維，填充物材料：聚酯纖維。' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜冠佑款', N'抱枕1入', N'材質：聚酯纖維100%' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜瑪莎款', N'抱枕1入', N'材質：聚酯纖維100%' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜石頭款', N'抱枕1入', N'材質：聚酯纖維100%' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜怪獸款', N'抱枕1入', N'材質：聚酯纖維100%' UNION ALL
    SELECT 2, 8, N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜阿信款', N'抱枕1入', N'材質：聚酯纖維100%' UNION ALL
    SELECT 2, 2, N'〔五月天 #5525〕活到那一天 5555八十歲五迷T', N'八十歲五迷T', N'材質：100%棉' UNION ALL
    SELECT 2, 2, N'〔五月天 #5525〕嘿我要走了 步步喵星人 黑T', N'步步喵星人 黑T', N'材質：100%棉' UNION ALL
    SELECT 2, 2, N'〔五月天 #5525〕嘿我要走了 步步喵星人 米白T', N'步步喵星人 米白T', N'材質：100%棉' UNION ALL
    SELECT 2, 6, N'〔五月天 #5525〕因為你所以五 我們白襪', N'因為你所以五白襪', N'材質：60%棉、35%聚酯纖維、5%氨綸' UNION ALL
    SELECT 2, 7, N'〔五月天 #5525〕我要我瘋我要我愛 盛夏涼感毛巾', N'盛夏涼感毛巾', N'材質：100%聚酯纖維' 
) AS T;

INSERT INTO product_variants(product_id, org_skuno, product_color, product_size, unit_price, stock_qty, barcode, status)
SELECT P.product_id, V.sku, V.color, V.size, V.price, V.qty, V.bar, 1
FROM products P
CROSS APPLY (
    -- 在這裡為不同名稱的商品，對應綁定其尺寸、顏色與庫存
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜知足球款' AS name, 'MD11769226R' AS sku, N'紅色' AS color, 'FREE' AS size, 325 AS price, 5000 AS qty, '4810000000033' AS bar UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜知足球款', 'MD11769226GR', N'綠色', 'FREE', 325, 5000, '4810000000034' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜知足球款', 'MD11769226PK', N'粉色', 'FREE', 325, 5000, '4810000000035' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜知足球款', 'MD11769226Y', N'黃色', 'FREE', 325, 5000, '4810000000036' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜知足球款', 'MD11769226BL', N'藍色', 'FREE', 325, 5000, '4810000000037' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜藍恐龍款', '11766520GR', N'綠色', 'FREE', 375, 5000, '4810000000038' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜藍恐龍款', '11766520R', N'紅色', 'FREE', 375, 5000, '4810000000039' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜藍恐龍款', '11766520Y', N'黃色', 'FREE', 375, 5000, '4810000000040' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜藍恐龍款', '11766520PK', N'粉色', 'FREE', 375, 5000, '4810000000041' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜藍恐龍款', '11766520BL', N'藍色', 'FREE', 375, 5000, '4810000000042' UNION ALL
    SELECT N'〔五月天 #5525〕回到侏儸紀那一天 五球怪恐龍T', '11766519XS', N'黑色', 'XS', 1225, 5000, '4810000000043' UNION ALL
    SELECT N'〔五月天 #5525〕回到侏儸紀那一天 五球怪恐龍T', '11766519S', N'黑色', 'S', 1225, 5000, '4810000000044' UNION ALL
    SELECT N'〔五月天 #5525〕回到侏儸紀那一天 五球怪恐龍T', '11766519M', N'黑色', 'M', 1225, 5000, '4810000000045' UNION ALL
    SELECT N'〔五月天 #5525〕回到侏儸紀那一天 五球怪恐龍T', '11766519L', N'黑色', 'L', 1225, 5000, '4810000000046' UNION ALL
    SELECT N'〔五月天 #5525〕回到侏儸紀那一天 五球怪恐龍T', '11766519XL', N'黑色', 'XL', 1225, 5000, '4810000000047' UNION ALL
    SELECT N'〔五月天 #5525〕回到侏儸紀那一天 五球怪恐龍T', '11766519XXL', N'黑色', 'XXL', 1225, 5000, '4810000000048' UNION ALL
    SELECT N'〔五月天 #5525〕5555 活到那一天 雲端互動版 LED 2.0 80歲螢光拐杖套組', '11428090R', N'紅色', 'FREE', 1525, 5000, '4810000000049' UNION ALL
    SELECT N'〔五月天 #5525〕5555 活到那一天 雲端互動版 LED 2.0 80歲螢光拐杖套組', '11428090GR', N'綠色', 'FREE', 1525, 5000, '4810000000050' UNION ALL
    SELECT N'〔五月天 #5525〕5555 活到那一天 雲端互動版 LED 2.0 80歲螢光拐杖套組', '11428090PK', N'粉色', 'FREE', 1525, 5000, '4810000000051' UNION ALL
    SELECT N'〔五月天 #5525〕5555 活到那一天 雲端互動版 LED 2.0 80歲螢光拐杖套組', '11428090Y', N'黃色', 'FREE', 1525, 5000, '4810000000052' UNION ALL
    SELECT N'〔五月天 #5525〕5555 活到那一天 雲端互動版 LED 2.0 80歲螢光拐杖套組', '11428090BL', N'藍色', 'FREE', 1525, 5000, '4810000000053' UNION ALL
    SELECT N'〔五月天 #5525〕5555 活到那一天 雲端互動版 LED 2.0 80歲螢光拐杖套組', '11428090WH', N'白色', 'FREE', 1525, 5000, '4810000000054' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜馬不停蹄款', '11428084GR', N'綠色', 'FREE', 325, 5000, '4810000000055' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜白馬王子款', '11428078GR', N'綠色', 'FREE', 325, 5000, '4810000000056' UNION ALL
    SELECT N'〔五月天 #5525〕晚安喵星人 哄你入睡保暖披風', '11428065', N'咖啡色', 'FREE', 1055, 5000, '4810000000057' UNION ALL
    SELECT N'〔五月天 #5525〕當我們混掛在一起 多功能掛扣長織帶', '11428058', N'黑色', 'FREE', 525, 5000, '4810000000058' UNION ALL
    SELECT N'〔五月天 #5525〕五公仔雪花音樂盒｜還你自由款', '11361764', N'古銅色', 'FREE', 2525, 5000, '4810000000059' UNION ALL
    SELECT N'〔五月天 #5525〕五公仔雪花音樂盒｜這是我的溫柔款', '11361762', N'銀色', 'FREE', 2525, 5000, '4810000000060' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五球變變變頭套｜聖誕夜驚魂款', '11361758GR', N'綠色', 'FREE', 255, 5000, '4810000000061' UNION ALL
    SELECT N'〔五月天 #5525〕HOME RUN MD25棒球外套', '11361752XS', N'黑色', 'XS', 4525, 5000, '4810000000062' UNION ALL
    SELECT N'〔五月天 #5525〕HOME RUN MD25棒球外套', '11361752S', N'黑色', 'S', 4525, 5000, '4810000000063' UNION ALL
    SELECT N'〔五月天 #5525〕HOME RUN MD25棒球外套', '11361752M', N'黑色', 'M', 4525, 5000, '4810000000064' UNION ALL
    SELECT N'〔五月天 #5525〕HOME RUN MD25棒球外套', '11361752L', N'黑色', 'L', 4525, 5000, '4810000000065' UNION ALL
    SELECT N'〔五月天 #5525〕HOME RUN MD25棒球外套', '11361752XXL', N'黑色', 'XXL', 4525, 5000, '4810000000066' UNION ALL
    SELECT N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜冠佑款', '11249164', N'藍色', 'FREE', 1055, 5000, '4810000000067' UNION ALL
    SELECT N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜瑪莎款', '11249113', N'黃色', 'FREE', 1055, 5000, '4810000000068' UNION ALL
    SELECT N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜石頭款', '11249066', N'綠色', 'FREE', 1055, 5000, '4810000000069' UNION ALL
    SELECT N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜怪獸款', '11249027', N'紅色', 'FREE', 1055, 5000, '4810000000070' UNION ALL
    SELECT N'〔五月天 #5525〕我不願讓你一個人擁抱枕｜阿信款', '11249164', N'粉色', 'FREE', 1055, 5000, '4810000000071' UNION ALL
    SELECT N'〔五月天 #5525〕活到那一天 5555八十歲五迷T', '11071024XS', N'黑色', 'XS', 1225, 5000, '4810000000072' UNION ALL
    SELECT N'〔五月天 #5525〕活到那一天 5555八十歲五迷T', '11071024S', N'黑色', 'S', 1225, 5000, '4810000000073' UNION ALL
    SELECT N'〔五月天 #5525〕活到那一天 5555八十歲五迷T', '11071024M', N'黑色', 'M', 1225, 5000, '4810000000074' UNION ALL
    SELECT N'〔五月天 #5525〕活到那一天 5555八十歲五迷T', '11071024L', N'黑色', 'L', 1225, 5000, '4810000000075' UNION ALL
    SELECT N'〔五月天 #5525〕活到那一天 5555八十歲五迷T', '11071024XL', N'黑色', 'XL', 1225, 5000, '4810000000076' UNION ALL
    SELECT N'〔五月天 #5525〕活到那一天 5555八十歲五迷T', '11071024XXL', N'黑色', 'XXL', 1225, 5000, '4810000000077' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 黑T', '11069744XS', N'黑色', 'XS', 1225, 5000, '4810000000078' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 黑T', '11069744S', N'黑色', 'S', 1225, 5000, '4810000000079' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 黑T', '11069744M', N'黑色', 'M', 1225, 5000, '4810000000080' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 黑T', '11069744L', N'黑色', 'L', 1225, 5000, '4810000000081' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 黑T', '11069744XL', N'黑色', 'XL', 1225, 5000, '4810000000082' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 黑T', '11069744XXL', N'黑色', 'XXL', 1225, 5000, '4810000000083' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 米白T', '11069868XS', N'米白色', 'XS', 1225, 5000, '4810000000084' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 米白T', '11069868S', N'米白色', 'S', 1225, 5000, '4810000000085' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 米白T', '11069868M', N'米白色', 'M', 1225, 5000, '4810000000086' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 米白T', '11069868L', N'米白色', 'L', 1225, 5000, '4810000000087' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 米白T', '11069868XL', N'米白色', 'XL', 1225, 5000, '4810000000088' UNION ALL
    SELECT N'〔五月天 #5525〕嘿我要走了 步步喵星人 米白T', '11069868XXL', N'米白色', 'XXL', 1225, 5000, '4810000000089' UNION ALL
    SELECT N'〔五月天 #5525〕因為你所以五 我們白襪', '9779446', N'白色', 'FREE', 325, 5000, '4810000000090' UNION ALL
    SELECT N'〔五月天 #5525〕我要我瘋我要我愛 盛夏涼感毛巾', '11070131', N'白色', 'FREE', 375, 5000, '4810000000091'
) AS V
WHERE P.product_name = V.name AND P.theme_id = 2;

INSERT INTO product_images
(
    product_id,
    image_url,
    sort_order,
    is_main
)
VALUES
(9, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E7%9F%A5%E8%B6%B3%E7%90%83%E6%AC%BE_ui9zwf.jpg', 1, 1),
(9, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E7%9F%A5%E8%B6%B3%E7%90%83%E6%AC%BE-2_wfghmq.jpg', 2, 0),
(9, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E7%9F%A5%E8%B6%B3%E7%90%83%E6%AC%BE-3_sdavhm.jpg', 3, 0),
(9, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E7%9F%A5%E8%B6%B3%E7%90%83%E6%AC%BE-4_wprdvs.jpg', 4, 0),
(9, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279440/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E7%9F%A5%E8%B6%B3%E7%90%83%E6%AC%BE-5_ughybk.jpg', 5, 0),
(9, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279440/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E7%9F%A5%E8%B6%B3%E7%90%83%E6%AC%BE-6_afa1g3.jpg', 6, 0),
(10, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%97%8D%E6%81%90%E9%BE%8D%E6%AC%BE-3_cd6x6o.jpg', 1, 1),
(10, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279441/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%97%8D%E6%81%90%E9%BE%8D%E6%AC%BE_nibhzs.jpg', 2, 0),
(10, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%97%8D%E6%81%90%E9%BE%8D%E6%AC%BE-5_es0x7n.jpg', 3, 0),
(10, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%97%8D%E6%81%90%E9%BE%8D%E6%AC%BE-4_ykwshu.jpg', 4, 0),
(10, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279932/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%97%8D%E6%81%90%E9%BE%8D%E6%AC%BEPK_r0e6kl.jpg', 5, 0),
(10, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279441/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%97%8D%E6%81%90%E9%BE%8D%E6%AC%BE-2_syjciy.jpg', 6, 0),
(11, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279441/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%9E%E5%88%B0%E4%BE%8F%E5%84%B8%E7%B4%80%E9%82%A3%E4%B8%80%E5%A4%A9_%E4%BA%94%E7%90%83%E6%80%AA%E6%81%90%E9%BE%8DT_jeimps.jpg', 1, 1),
(11, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279441/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%9E%E5%88%B0%E4%BE%8F%E5%84%B8%E7%B4%80%E9%82%A3%E4%B8%80%E5%A4%A9_%E4%BA%94%E7%90%83%E6%80%AA%E6%81%90%E9%BE%8DT-2_xjbx6v.jpg', 2, 0),
(11, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279438/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%9E%E5%88%B0%E4%BE%8F%E5%84%B8%E7%B4%80%E9%82%A3%E4%B8%80%E5%A4%A9_%E4%BA%94%E7%90%83%E6%80%AA%E6%81%90%E9%BE%8DT-3_modqqe.jpg', 3, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279438/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-1_jni4o1.jpg', 1, 1),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-8_gv0rim.jpg', 2, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-2_yewdkq.jpg', 3, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-7_h0a5zb.jpg', 4, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279440/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-9_kifgsg.jpg', 5, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279439/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-6_ucoglz.jpg', 6, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279441/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-10_tlshdk.jpg', 7, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279440/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-11_obeiha.jpg', 8, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279440/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-12_hgohky.jpg', 9, 0),
(12, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279441/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_5555_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_%E9%9B%B2%E7%AB%AF%E4%BA%92%E5%8B%95%E7%89%88_LED_2.0_80%E6%AD%B2%E8%9E%A2%E5%85%89%E6%8B%90%E6%9D%96%E5%A5%97%E7%B5%84-13_gzypll.jpg', 10, 0),
(13, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279441/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E9%A6%AC%E4%B8%8D%E5%81%9C%E8%B9%84%E6%AC%BEGR_v1ikia.jpg', 1, 1),
(14, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279438/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E7%99%BD%E9%A6%AC%E7%8E%8B%E5%AD%90%E6%AC%BEGR_iqesrr.jpg', 1, 1),
(15, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279443/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%99%9A%E5%AE%89%E5%96%B5%E6%98%9F%E4%BA%BA_%E5%93%84%E4%BD%A0%E5%85%A5%E7%9D%A1%E4%BF%9D%E6%9A%96%E6%8A%AB%E9%A2%A8_l3e58j.jpg', 1, 1),
(16, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279444/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E7%95%B6%E6%88%91%E5%80%91%E6%B7%B7%E6%8E%9B%E5%9C%A8%E4%B8%80%E8%B5%B7_%E5%A4%9A%E5%8A%9F%E8%83%BD%E6%8E%9B%E6%89%A3%E9%95%B7%E7%B9%94%E5%B8%B6_dguzks.jpg', 1, 1),
(17, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279438/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E4%BA%94%E5%85%AC%E4%BB%94%E9%9B%AA%E8%8A%B1%E9%9F%B3%E6%A8%82%E7%9B%92_%E9%82%84%E4%BD%A0%E8%87%AA%E7%94%B1%E6%AC%BE-1_grzzn5.jpg', 1, 1),
(18, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782280467/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E4%BA%94%E5%85%AC%E4%BB%94%E9%9B%AA%E8%8A%B1%E9%9F%B3%E6%A8%82%E7%9B%92_%E9%80%99%E6%98%AF%E6%88%91%E7%9A%84%E6%BA%AB%E6%9F%94%E6%AC%BE_a4hhl2.jpg', 1, 1),
(19, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279437/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%81%96%E8%AA%95%E5%A4%9C%E9%A9%9A%E9%AD%82%E6%AC%BE-1_zi7mgg.jpg', 1, 1),
(19, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279437/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94%E7%90%83%E8%AE%8A%E8%AE%8A%E8%AE%8A%E9%A0%AD%E5%A5%97_%E8%81%96%E8%AA%95%E5%A4%9C%E9%A9%9A%E9%AD%82%E6%AC%BE-2_y1lpgt.jpg', 2, 0),
(20, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279437/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_HOME_RUN_MD25%E6%A3%92%E7%90%83%E5%A4%96%E5%A5%97_wpnoxy.jpg', 1, 1),
(21, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%88%91%E4%B8%8D%E9%A1%98%E8%AE%93%E4%BD%A0%E4%B8%80%E5%80%8B%E4%BA%BA%E6%93%81%E6%8A%B1%E6%9E%95_%E5%86%A0%E4%BD%91%E6%AC%BE_vlsdsf.jpg', 1, 1),
(22, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279444/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%88%91%E4%B8%8D%E9%A1%98%E8%AE%93%E4%BD%A0%E4%B8%80%E5%80%8B%E4%BA%BA%E6%93%81%E6%8A%B1%E6%9E%95_%E7%91%AA%E8%8E%8E%E6%AC%BE_jmfhw8.jpg', 1, 1),
(23, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%88%91%E4%B8%8D%E9%A1%98%E8%AE%93%E4%BD%A0%E4%B8%80%E5%80%8B%E4%BA%BA%E6%93%81%E6%8A%B1%E6%9E%95_%E7%9F%B3%E9%A0%AD%E6%AC%BE_rgrqa7.jpg', 1, 1),
(24, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%88%91%E4%B8%8D%E9%A1%98%E8%AE%93%E4%BD%A0%E4%B8%80%E5%80%8B%E4%BA%BA%E6%93%81%E6%8A%B1%E6%9E%95_%E6%80%AA%E7%8D%B8%E6%AC%BE_njyo7g.jpg', 1, 1),
(25, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%88%91%E4%B8%8D%E9%A1%98%E8%AE%93%E4%BD%A0%E4%B8%80%E5%80%8B%E4%BA%BA%E6%93%81%E6%8A%B1%E6%9E%95_%E9%98%BF%E4%BF%A1%E6%AC%BE_umqztw.jpg', 1, 1),
(26, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279443/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%B4%BB%E5%88%B0%E9%82%A3%E4%B8%80%E5%A4%A9_5555%E5%85%AB%E5%8D%81%E6%AD%B2%E4%BA%94%E8%BF%B7T_wrm20l.jpg', 1, 1),
(27, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279444/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%98%BF%E6%88%91%E8%A6%81%E8%B5%B0%E4%BA%86_%E6%AD%A5%E6%AD%A5%E5%96%B5%E6%98%9F%E4%BA%BA_%E9%BB%91T-1_lq8hbi.jpg', 1, 1),
(28, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782280791/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%98%BF%E6%88%91%E8%A6%81%E8%B5%B0%E4%BA%86_%E6%AD%A5%E6%AD%A5%E5%96%B5%E6%98%9F%E4%BA%BA_%E7%B1%B3%E7%99%BDT_qyk9o5.jpg', 1, 1),
(29, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279438/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E5%9B%A0%E7%82%BA%E4%BD%A0%E6%89%80%E4%BB%A5%E4%BA%94_%E6%88%91%E5%80%91%E7%99%BD%E8%A5%AA_l4i68h.jpg', 1, 1),
(30, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782279442/%E4%BA%94%E6%9C%88%E5%A4%A9_5525_%E6%88%91%E8%A6%81%E6%88%91%E7%98%8B%E6%88%91%E8%A6%81%E6%88%91%E6%84%9B_%E7%9B%9B%E5%A4%8F%E6%B6%BC%E6%84%9F%E6%AF%9B%E5%B7%BE_nxpes6.jpg', 1, 1);


-- 商品新增(Mickey)
INSERT INTO products (theme_id, category_id, product_name, product_sim_description, product_description, status)
SELECT     
    T.theme_id,
    T.category_id,
    T.p_name,
    T.p_sim_desc,
    T.p_desc,
    1
FROM (
    SELECT 4 AS theme_id, 8 AS category_id, N'迪士尼米奇米妮絨毛娃娃玩偶黑金廚師款' AS p_name, N'米奇米妮絨毛娃娃' AS p_sim_desc, N'材質：絨毛、聚酯纖維' AS p_desc UNION ALL
    SELECT 4, 8, N'迪士尼米奇米妮絨毛娃娃玩偶抱枕38公分', N'米奇米妮絨毛娃娃', N'材質：絨毛、聚酯纖維' UNION ALL
    SELECT 4, 8, N'迪士尼米妮鑰匙圈掛飾', N'鑰匙圈掛飾', N'材質：金屬、塑膠' UNION ALL
    SELECT 4, 2, N'Disney聯名款 星夜米奇 中性長袖衛衣', N'衛衣', N'材質：棉布、聚酯纖維' UNION ALL
    SELECT 4, 3, N'Disney迪士尼聯名 Logo印花米奇造型連帽外套', N'米奇連帽外套', N'材質：棉布、聚酯纖維' UNION ALL
    SELECT 4, 5, N'Disney 聯名米奇五片帽', N'米奇五片帽', N'材質：棉 100%'
) AS T;

INSERT INTO product_variants(product_id, org_skuno, product_color, product_size, unit_price, stock_qty, barcode, status)
SELECT P.product_id, V.sku, V.color, V.size, V.price, V.qty, V.bar, 1
FROM products P
CROSS APPLY (
    -- 在這裡為不同名稱的商品，對應綁定其尺寸、顏色與庫存
    SELECT N'迪士尼米奇米妮絨毛娃娃玩偶黑金廚師款' AS name, '192123M' AS sku, N'黑金' AS color, 'FREE' AS size, 424 AS price, 5000 AS qty, '4810000000092' AS bar UNION ALL
    SELECT N'迪士尼米奇米妮絨毛娃娃玩偶黑金廚師款' AS name, '192124F' AS sku, N'黑金' AS color, 'FREE' AS size, 424 AS price, 5000 AS qty, '4810000000093' UNION ALL
    SELECT N'迪士尼米奇米妮絨毛娃娃玩偶抱枕38公分', '890590', N'如圖', 'FREE', 483, 1500, '4810000000094' UNION ALL
    SELECT N'迪士尼米奇米妮絨毛娃娃玩偶抱枕38公分', '890591', N'如圖', 'FREE', 483, 1500, '4810000000095' UNION ALL
    SELECT N'迪士尼米妮鑰匙圈掛飾', '44-00032-2', N'如圖', 'FREE', 237, 2000, '4810000000096' UNION ALL
    SELECT N'Disney聯名款 星夜米奇 中性長袖衛衣', '11053152', N'暖駝', 'Ｓ', 1299, 2000, '4810000000097' UNION ALL
    SELECT N'Disney迪士尼聯名 Logo印花米奇造型連帽外套', '524996S', N'象牙白', 'S', 899, 2000, '4810000000098' UNION ALL
    SELECT N'Disney迪士尼聯名 Logo印花米奇造型連帽外套', '524996M', N'象牙白', 'M', 899, 2000, '4810000000099' UNION ALL
    SELECT N'Disney迪士尼聯名 Logo印花米奇造型連帽外套', '524996L', N'象牙白', 'L', 899, 5000, '4810000000100' UNION ALL
    SELECT N'Disney 聯名米奇五片帽', 'TP00013730004855', N'淺米色', 'FREE', 2480, 5000, '4810000000101'
) AS V
WHERE P.product_name = V.name AND P.theme_id = 4;

INSERT INTO product_images
(
    product_id,
    image_url,
    sort_order,
    is_main
)
VALUES
(31, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782285392/%E8%BF%AA%E5%A3%AB%E5%B0%BC%E7%B1%B3%E5%A5%87%E7%B1%B3%E5%A6%AE%E7%B5%A8%E6%AF%9B%E5%A8%83%E5%A8%83%E7%8E%A9%E5%81%B6%E9%BB%91%E9%87%91%E5%BB%9A%E5%B8%AB%E6%AC%BE35%E5%85%AC%E5%88%86_cpiydf.webp', 1, 1),
(32, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782285391/%E8%BF%AA%E5%A3%AB%E5%B0%BC%E7%B1%B3%E5%A5%87%E7%B1%B3%E5%A6%AE%E7%B5%A8%E6%AF%9B%E5%A8%83%E5%A8%83%E7%8E%A9%E5%81%B6%E6%8A%B1%E6%9E%9538%E5%85%AC%E5%88%86_ucm2e5.webp', 1, 1),
(33, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782285392/%E8%BF%AA%E5%A3%AB%E5%B0%BC%E7%B1%B3%E5%A6%AE%E9%91%B0%E5%8C%99%E5%9C%88_lsxy5e.webp', 1, 1),
(34, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782285391/Disney%E8%81%AF%E5%90%8D%E6%AC%BE_%E6%98%9F%E5%A4%9C%E7%B1%B3%E5%A5%87_%E4%B8%AD%E6%80%A7%E9%95%B7%E8%A2%96%E8%A1%9B%E8%A1%A3_op97ca.webp', 1, 1),
(35, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782285390/Disney%E8%BF%AA%E5%A3%AB%E5%B0%BC%E8%81%AF%E5%90%8D_Logo%E5%8D%B0%E8%8A%B1%E7%B1%B3%E5%A5%87%E9%80%A0%E5%9E%8B%E9%80%A3%E5%B8%BD%E5%A4%96%E5%A5%97_d62ocy.webp', 1, 1),
(36, 'https://res.cloudinary.com/dw6mnxj0a/image/upload/v1782285391/MICKEY_GRAPHIC_5_PANEL_CAP_h5qein.jpg', 1, 1);
