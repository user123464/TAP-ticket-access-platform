-- =========================================
-- 刪除資料表 (Drop Tables)
-- 順序：依賴層級由下往上刪除
-- =========================================
DROP TABLE IF EXISTS refund_request;         -- 退款申請單 (退票/退貨審核制)
DROP TABLE IF EXISTS merchant_settlements;   -- 廠商結算
DROP TABLE IF EXISTS merch_order_details;    -- 商城訂單明細
DROP TABLE IF EXISTS merch_orders;           -- 商城訂單主檔
DROP TABLE IF EXISTS ticket_order_details;   -- 票券訂單明細
DROP TABLE IF EXISTS ticket_orders;          -- 票券訂單主檔

DROP TABLE IF EXISTS merch_cart;          -- 購物車
DROP TABLE IF EXISTS product_images;      -- 商品圖片
DROP TABLE IF EXISTS product_variants;    -- 商品款式
DROP TABLE IF EXISTS products;            -- 商品主檔
DROP TABLE IF EXISTS product_categories;  -- 商品分類

DROP TABLE IF EXISTS session_zone_mapping; --場次-分區 中介表
DROP TABLE IF EXISTS bid;           -- 出價紀錄
DROP TABLE IF EXISTS auction;       -- 競標商品

DROP TABLE IF EXISTS ticket;        -- 票券庫存
DROP TABLE IF EXISTS ticket_type;   -- 票券種類

DROP TABLE IF EXISTS session        --場次
DROP TABLE IF EXISTS theme          --活動主題


DROP TABLE IF EXISTS seat;          --座位
DROP TABLE IF EXISTS zone;          --分區
DROP TABLE IF EXISTS location;      --場館



-- Phase 5：系統支撐
DROP TABLE IF EXISTS [user_submission];          -- 使用者表單/回饋
DROP TABLE IF EXISTS [media_file];               -- 媒體檔案
DROP TABLE IF EXISTS [system_config];            -- 系統設定
DROP TABLE IF EXISTS [system_dictionary];        -- 系統字典

-- Phase 4：通知與排程
DROP TABLE IF EXISTS [system_announcement];      -- 系統公告
DROP TABLE IF EXISTS [scheduled_job];            -- 排程任務
DROP TABLE IF EXISTS [notification_log];         -- 通知發送紀錄
DROP TABLE IF EXISTS [notification_template];    -- 通知樣板

-- Phase 3：訂閱方案
DROP TABLE IF EXISTS [organizer_subscription];   -- 廠商訂閱紀錄
DROP TABLE IF EXISTS [plan_feature];             -- 方案功能橋接表
DROP TABLE IF EXISTS [membership_plan];          -- 訂閱會員方案
DROP TABLE IF EXISTS [saas_feature];             -- SaaS 功能清單

-- Phase 2：財務與稽核
DROP TABLE IF EXISTS [system_audit_log];         -- 系統稽核日誌
DROP TABLE IF EXISTS [contract];                 -- 廠商合約

-- Phase 1：登入模組
DROP TABLE IF EXISTS [login_attempt];            -- 登入嘗試紀錄
DROP TABLE IF EXISTS [user_session];             -- 使用者登入 Session

-- Phase 0：RBAC + 組織架構 (依賴關係較深，需嚴格依序)
DROP TABLE IF EXISTS [organizer_invitation];
DROP TABLE IF EXISTS [organizer_ownership_transfer]; -- 組織所有權轉移 (依賴 user, organizer)
DROP TABLE IF EXISTS [organizer_member];         -- 組織成員 (依賴 user, organizer, role)
DROP TABLE IF EXISTS [system_resource];          -- 系統資源 (依賴 permission, system_resource)
DROP TABLE IF EXISTS [role_permission_template]; -- 角色權限模板橋接 (依賴 permission, role_template)
DROP TABLE IF EXISTS [role_template];            -- 組織角色模板主表 (被 role_permission_template 參照)
DROP TABLE IF EXISTS [role_permission];          -- 角色權限橋接表 (依賴 role, permission)
DROP TABLE IF EXISTS [user_role];                -- 使用者角色橋接表 (依賴 user, role)
DROP TABLE IF EXISTS [role];                     -- 角色 (依賴 organizer)
DROP TABLE IF EXISTS [organizer];                -- 廠商/組織 (依賴 user)
DROP TABLE IF EXISTS [permission];               -- 權限 (獨立表)
DROP TABLE IF EXISTS [user];                     -- 使用者 (最底層核心表)

-- ================================================================
-- 刪除序列 (Sequences) - 流水號不受單複數影響，維持原樣
-- ================================================================
DROP SEQUENCE IF EXISTS [seq_ROL];                -- 組織自訂角色流水號
DROP SEQUENCE IF EXISTS [seq_USM];                -- 表單回饋流水號
DROP SEQUENCE IF EXISTS [seq_ANN];                -- 系統公告流水號
DROP SEQUENCE IF EXISTS [seq_SUB];                -- 廠商訂閱流水號
DROP SEQUENCE IF EXISTS [seq_TRF];                -- 組織所有權轉移流水號
DROP SEQUENCE IF EXISTS [seq_MBR];                -- 組織成員流水號
DROP SEQUENCE IF EXISTS [seq_CON];                -- 廠商合約流水號
DROP SEQUENCE IF EXISTS [seq_ORG];                -- 廠商組織流水號
DROP SEQUENCE IF EXISTS [seq_USR];                -- 使用者流水號

DROP SEQUENCE IF EXISTS [seat_seq];
DROP SEQUENCE IF EXISTS [ticket_seq];

-- =========================================
-- 新增資料表 (Create Tables)
-- 順序：依賴層級由上往下
-- =========================================

-- =========================================
-- 會員
-- =========================================

-- ================================================================
-- 序列
-- ================================================================
CREATE SEQUENCE seq_USR AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_ORG AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_CON AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_MBR AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_TRF AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_SUB AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_ANN AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_USM AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;
CREATE SEQUENCE seq_ROL AS BIGINT START WITH 1 INCREMENT BY 1 MINVALUE 1 MAXVALUE 9999999 NO CYCLE;


CREATE SEQUENCE seat_seq AS BIGINT START WITH 10000 INCREMENT BY 50 NO CYCLE; --座位ID
CREATE SEQUENCE ticket_seq AS BIGINT START WITH 10000 INCREMENT BY 50 NO CYCLE; --票券ID
-- ================================================================
-- Phase 0：RBAC + 組織架構
-- ================================================================

-- ── user ───────────────────────────────────────────────────
CREATE TABLE [user] (
    user_id               CHAR(10)      PRIMARY KEY,               -- [R1] prefix USR，seq_USR
    email                 NVARCHAR(100) NOT NULL,                  -- [S1] 唯一性改 filtered unique index（見表後）
    password_hash         NVARCHAR(255) NULL,                      -- LOCAL 帳號必填；GOOGLE 帳號為 NULL
    name                  NVARCHAR(50)  NOT NULL,
    phone                 NVARCHAR(20)  NULL,
    gender                CHAR(1)       NULL,                      -- [R6] M=男, F=女, O=其他
    birth_date            DATE          NULL,
    address               NVARCHAR(255) NULL,
    avatar_url            NVARCHAR(255) NULL,
    -- 登入方式
    auth_provider         TINYINT       NOT NULL DEFAULT 0,        -- [R6] 0=LOCAL, 1=GOOGLE
    -- Google OAuth 綁定
    google_oauth_id       NVARCHAR(100) NULL,                      -- [C1] 唯一性改 filtered unique index（見表後）
    -- Email 驗證
    email_verified_at     DATETIME2     NULL,
    email_verify_token    NVARCHAR(255) NULL,
    email_verify_expires  DATETIME2     NULL,
    -- 忘記密碼
    pwd_reset_token       NVARCHAR(255) NULL,
    pwd_reset_expires     DATETIME2     NULL,
    -- 帳號鎖定
    locked_until          DATETIME2     NULL,
    -- 2FA 預留
    is_two_factor_enabled BIT           NOT NULL DEFAULT 0,
    -- 帳號狀態
    is_active             BIT           NOT NULL DEFAULT 1,
    is_deleted            BIT           NOT NULL DEFAULT 0,
    must_change_password  BIT           NOT NULL DEFAULT 0,
    created_at            DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3] UTC
    updated_at            DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3] UTC（App 於 UPDATE 時主動寫入）
    row_version           ROWVERSION    NOT NULL,                  -- [H5] 樂觀鎖，JPA @Version
    CONSTRAINT CK_Users_AuthProvider CHECK (
        (auth_provider = 0 AND password_hash IS NOT NULL) OR      -- 0=LOCAL
        (auth_provider = 1 AND password_hash IS NULL)             -- 1=GOOGLE
    ),
    CONSTRAINT CK_Users_Gender CHECK (gender IS NULL OR gender IN ('M', 'F', 'O'))  -- [M6]
);

-- [S1] email 僅對未刪除帳號唯一
CREATE UNIQUE INDEX UX_Users_EmailActive
    ON [user](email)
    WHERE is_deleted = 0;

-- [C1] google_oauth_id 僅對非 NULL 強制唯一
CREATE UNIQUE INDEX UX_Users_GoogleOAuth
    ON [user](google_oauth_id)
    WHERE google_oauth_id IS NOT NULL;

-- ── permission ─────────────────────────────────────────────
CREATE TABLE [permission] (
    permission_id VARCHAR(30)   PRIMARY KEY,                       -- [R1] 語意代碼如 USER_VIEW
    resource_code NVARCHAR(50)  NOT NULL,
    action_code   NVARCHAR(50)  NOT NULL,
    description   NVARCHAR(255) NULL,
    UNIQUE (resource_code, action_code)
);

-- ── organizer ──────────────────────────────────────────────
CREATE TABLE [organizer] (
    organizer_id         CHAR(10)      PRIMARY KEY,                -- [R1] prefix ORG，seq_ORG
    owner_user_id        CHAR(10)      NOT NULL,
    name                 NVARCHAR(100) NOT NULL,
    tax_id               NVARCHAR(20)  NULL,
    status               TINYINT       NOT NULL DEFAULT 0,         -- [R6] 0=ACTIVE, 1=SUSPENDED, 2=ARCHIVED
    kyc_status           TINYINT       NOT NULL DEFAULT 0,         -- [R6] 0=DRAFT, 1=PENDING, 2=APPROVED, 3=REJECTED
    kyc_data_json        NVARCHAR(MAX) NULL,
    bank_account_info    NVARCHAR(MAX) NULL,                       -- 撥款帳戶（App 層加密儲存）
    kyc_rejection_reason NVARCHAR(500) NULL,
    kyc_reviewed_at      DATETIME2     NULL,
    kyc_reviewed_by      CHAR(10)      NULL,
    created_at           DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    updated_at           DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    row_version          ROWVERSION    NOT NULL,                   -- [H5]
    CONSTRAINT CK_Organizers_Status
        CHECK (status     BETWEEN 0 AND 2),
    CONSTRAINT CK_Organizers_KycStatus
        CHECK (kyc_status BETWEEN 0 AND 3),
    CONSTRAINT CK_Organizers_KycRejected
        CHECK (kyc_status != 3 OR kyc_rejection_reason IS NOT NULL),
    CONSTRAINT CK_Organizers_KycReviewed
        CHECK (kyc_status NOT IN (2, 3) OR (kyc_reviewed_by IS NOT NULL AND kyc_reviewed_at IS NOT NULL)),
    FOREIGN KEY (owner_user_id)   REFERENCES [user](user_id),
    FOREIGN KEY (kyc_reviewed_by) REFERENCES [user](user_id)
);

CREATE NONCLUSTERED INDEX IX_Organizers_Owner       ON [organizer](owner_user_id);    -- [H4]
CREATE NONCLUSTERED INDEX IX_Organizers_KycReviewer ON [organizer](kyc_reviewed_by);  -- [H4]

-- ── role ───────────────────────────────────────────────────
CREATE TABLE [role] (
    role_id      VARCHAR(30)   PRIMARY KEY,                        -- [R1] 平台角色語意字串(BUYER…) / 組織角色 ROL+流水號(seq_ROL)
    role_name    NVARCHAR(50)  NOT NULL,
    description  NVARCHAR(255) NULL,
    organizer_id CHAR(10)      NULL,         -- NULL = 平台角色；NOT NULL = 組織自訂角色
    is_editable  BIT           NOT NULL DEFAULT 1,
    FOREIGN KEY (organizer_id) REFERENCES [organizer](organizer_id)
);

CREATE UNIQUE INDEX UX_Roles_PlatformName
    ON [role](role_name)
    WHERE organizer_id IS NULL;

CREATE NONCLUSTERED INDEX IX_Roles_Organizer ON [role](organizer_id);  -- [H4]

-- ── user_role ──────────────────────────────────────────────
CREATE TABLE [user_role] (
    user_role_id BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- [R1] 橋接表代理鍵
    user_id      CHAR(10)     NOT NULL,
    role_id      VARCHAR(30)  NOT NULL,                            -- App 層限平台角色（organizer_id IS NULL）
    UNIQUE (user_id, role_id),
    FOREIGN KEY (user_id) REFERENCES [user](user_id),
    FOREIGN KEY (role_id) REFERENCES [role](role_id)
);

CREATE NONCLUSTERED INDEX IX_UserRoles_Role ON [user_role](role_id);  -- [H4]

-- ── role_permission ────────────────────────────────────────
CREATE TABLE [role_permission] (
    rp_id         BIGINT IDENTITY(1,1) PRIMARY KEY,                -- [R1] 橋接表代理鍵
    role_id       VARCHAR(30)  NOT NULL,
    permission_id VARCHAR(30)  NOT NULL,
    UNIQUE (role_id, permission_id),
    FOREIGN KEY (role_id)       REFERENCES [role](role_id),
    FOREIGN KEY (permission_id) REFERENCES [permission](permission_id)
);

CREATE NONCLUSTERED INDEX IX_RolePerms_Permission ON [role_permission](permission_id);  -- [H4]

-- ── role_template ──────────────────────────────────────────
-- 組織預設角色模板主表（DEFAULT_ORG_*）。新組織建立時依此生成組織角色（藍圖：複製當下、之後互不影響）。
CREATE TABLE [role_template] (
    template_id   VARCHAR(50)   PRIMARY KEY,
    template_name NVARCHAR(50)  NOT NULL,
    description   NVARCHAR(255) NULL
);

-- ── role_permission_template ───────────────────────────────
CREATE TABLE [role_permission_template] (
    rpt_id        BIGINT IDENTITY(1,1) PRIMARY KEY,                -- [R1] 橋接表代理鍵
    template_id   VARCHAR(50)  NOT NULL,
    permission_id VARCHAR(30)  NOT NULL,
    UNIQUE (template_id, permission_id),
    FOREIGN KEY (template_id)   REFERENCES [role_template](template_id) ON DELETE CASCADE,
    FOREIGN KEY (permission_id) REFERENCES [permission](permission_id)
);

CREATE NONCLUSTERED INDEX IX_RPT_Permission ON [role_permission_template](permission_id);  -- [H4]

-- ── system_resource ────────────────────────────────────────
CREATE TABLE [system_resource] (
    resource_id   VARCHAR(30)   PRIMARY KEY,                       -- [R1] 語意代碼如 ADM_USER
    portal_type   NVARCHAR(20)  NOT NULL,
    parent_id     VARCHAR(30)   NULL,
    resource_type TINYINT       NOT NULL,                          -- [R6] 0=MENU, 1=PAGE, 2=BUTTON
    name          NVARCHAR(100) NOT NULL,
    url_path      NVARCHAR(255) NULL,
    permission_id VARCHAR(30)   NULL,
    sort_order    INT           NOT NULL DEFAULT 0,
    icon          NVARCHAR(50)  NULL,                            -- [Jason] 選單圖示（bootstrap-icons class，如 bi-people）
    is_visible    BIT           NOT NULL DEFAULT 1,              -- [Jason] 是否顯示於選單（0=隱藏，例如 BUTTON 級資源不進側欄）
    CONSTRAINT CK_SysRes_Portal
        CHECK (portal_type IN ('B2C_FRONT', 'B2B_PORTAL', 'ADMIN_LOCAL')),
    CONSTRAINT CK_SysRes_ResourceType
        CHECK (resource_type BETWEEN 0 AND 2),
    FOREIGN KEY (parent_id)     REFERENCES [system_resource](resource_id),
    FOREIGN KEY (permission_id) REFERENCES [permission](permission_id)
);

CREATE NONCLUSTERED INDEX IX_SysRes_Parent     ON [system_resource](parent_id);      -- [H4]
CREATE NONCLUSTERED INDEX IX_SysRes_Permission ON [system_resource](permission_id);  -- [H4]

-- ── organizer_member ───────────────────────────────────────
CREATE TABLE [organizer_member] (
    member_id            CHAR(10)      PRIMARY KEY,                -- [R1] prefix MBR，seq_MBR
    organizer_id         CHAR(10)      NOT NULL,
    user_id              CHAR(10)      NOT NULL,
    invited_by           CHAR(10)      NOT NULL,
    role_id              VARCHAR(30)   NULL,                       -- [R8]
    invite_token         NVARCHAR(255) NULL,
    invite_token_expires DATETIME2     NULL,
    status               TINYINT       NOT NULL DEFAULT 0,         -- [R6] 0=PENDING, 1=ACCEPTED, 2=REJECTED, 3=REVOKED
    invited_at           DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    joined_at            DATETIME2     NULL,
    CONSTRAINT CK_OrgMembers_Status
        CHECK (status BETWEEN 0 AND 3),
    CONSTRAINT CK_OrgMembers_JoinedAt
        CHECK (status != 1 OR joined_at IS NOT NULL),
    UNIQUE (organizer_id, user_id),
    FOREIGN KEY (organizer_id) REFERENCES [organizer](organizer_id),
    FOREIGN KEY (user_id)      REFERENCES [user](user_id),
    FOREIGN KEY (invited_by)   REFERENCES [user](user_id),
    FOREIGN KEY (role_id)      REFERENCES [role](role_id)          -- [R8]
);

CREATE NONCLUSTERED INDEX IX_OrgMembers_User      ON [organizer_member](user_id);     -- [H4]
CREATE NONCLUSTERED INDEX IX_OrgMembers_InvitedBy ON [organizer_member](invited_by);  -- [H4]
CREATE NONCLUSTERED INDEX IX_OrgMembers_Role      ON [organizer_member](role_id);     -- [H4][R8]


-- 組織成員邀請暫存表（供未註冊 Email 邀請入組用，於註冊時自動綁定）
CREATE TABLE [organizer_invitation] (
    invitation_id        BIGINT IDENTITY(1,1) PRIMARY KEY,
    email                NVARCHAR(255) NOT NULL,
    organizer_id         CHAR(10)      NOT NULL,
    role_id              VARCHAR(30)   NULL,
    invited_by           CHAR(10)      NOT NULL,
    invite_token         NVARCHAR(255) NOT NULL,
    invite_token_expires DATETIME2     NOT NULL,
    status               INT           NOT NULL DEFAULT 0, -- 0=PENDING, 1=ACCEPTED, 2=EXPIRED, 3=REVOKED
    invited_at           DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    FOREIGN KEY (organizer_id) REFERENCES [organizer](organizer_id),
    FOREIGN KEY (invited_by)   REFERENCES [user](user_id),
    FOREIGN KEY (role_id)      REFERENCES [role](role_id)
);

CREATE NONCLUSTERED INDEX IX_OrgInvite_Email ON [organizer_invitation](email);
CREATE NONCLUSTERED INDEX IX_OrgInvite_Token ON [organizer_invitation](invite_token);


-- 組織所有權轉移：owner 發起 → 被轉移人 email 認證接受才生效
CREATE TABLE [organizer_ownership_transfer] (
    transfer_id   CHAR(10)      PRIMARY KEY,                       -- prefix TRF，seq_TRF
    organizer_id  CHAR(10)      NOT NULL,
    from_user_id  CHAR(10)      NOT NULL,                          -- 發起轉移的現任 owner
    to_user_id    CHAR(10)      NOT NULL,                          -- 被轉移的目標成員
    token         NVARCHAR(255) NOT NULL,                          -- email 認證 Token
    token_expires DATETIME2     NOT NULL,
    status        TINYINT       NOT NULL DEFAULT 0,                -- 0=PENDING, 1=ACCEPTED, 2=CANCELLED, 3=EXPIRED
    created_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),
    CONSTRAINT CK_OrgTransfer_Status
        CHECK (status BETWEEN 0 AND 3),
    FOREIGN KEY (organizer_id) REFERENCES [organizer](organizer_id),
    FOREIGN KEY (from_user_id) REFERENCES [user](user_id),
    FOREIGN KEY (to_user_id)   REFERENCES [user](user_id)
);

CREATE NONCLUSTERED INDEX IX_OrgTransfer_Organizer ON [organizer_ownership_transfer](organizer_id);
CREATE NONCLUSTERED INDEX IX_OrgTransfer_Token     ON [organizer_ownership_transfer](token);


-- ================================================================
-- Phase 1：登入模組
-- ================================================================

-- ── user_session ───────────────────────────────────────────
CREATE TABLE [user_session] (
    session_id  BIGINT IDENTITY(1,1) PRIMARY KEY,                  -- [R1] 高頻 Log 表
    user_id     CHAR(10)      NOT NULL,
    token_jti   NVARCHAR(100) NOT NULL UNIQUE,
    portal_type NVARCHAR(20)  NOT NULL,                            -- 保留字串：B2C_FRONT, B2B_PORTAL, ADMIN_LOCAL
    ip_address  NVARCHAR(45)  NULL,
    user_agent  NVARCHAR(255) NULL,
    created_at  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),   -- [H3]
    expires_at  DATETIME2     NOT NULL,
    revoked_at  DATETIME2     NULL,
    revoked_by  TINYINT       NULL,                                -- [R6] 0=SELF, 1=ADMIN, 2=SYSTEM（NULL = 尚未撤銷）
    CONSTRAINT CK_UserSessions_Portal
        CHECK (portal_type IN ('B2C_FRONT', 'B2B_PORTAL', 'ADMIN_LOCAL')),
    CONSTRAINT CK_UserSessions_RevokedBy
        CHECK (revoked_by IS NULL OR revoked_by IN (0, 1, 2)),
    FOREIGN KEY (user_id) REFERENCES [user](user_id)
);

CREATE NONCLUSTERED INDEX IX_UserSessions_UserId   ON [user_session](user_id, revoked_at);

-- ── login_attempt ──────────────────────────────────────────
CREATE TABLE [login_attempt] (
    attempt_id     BIGINT IDENTITY(1,1) PRIMARY KEY,               -- [R1] 高頻 Log 表
    email          NVARCHAR(100) NOT NULL,
    ip_address     NVARCHAR(45)  NOT NULL,
    success        BIT           NOT NULL,
    failure_reason TINYINT       NULL,                             -- [R6] 0=WRONG_PASSWORD...
    attempted_at   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),-- [H3]
    CONSTRAINT CK_LoginAttempts_FailureReason
        CHECK (
            (success = 1 AND failure_reason IS NULL) OR
            (success = 0 AND failure_reason IN (0, 1, 2, 3))
        )
);

CREATE NONCLUSTERED INDEX IX_LoginAttempts_Email ON [login_attempt](email, attempted_at);
CREATE NONCLUSTERED INDEX IX_LoginAttempts_IP    ON [login_attempt](ip_address, attempted_at);


-- ================================================================
-- Phase 2：財務與稽核
-- ================================================================

-- ── contract ───────────────────────────────────────────────
CREATE TABLE [contract] (
    contract_id       CHAR(10)       PRIMARY KEY,                  -- [R1] prefix CON，seq_CON
    organizer_id      CHAR(10)       NOT NULL,
    contract_type     TINYINT        NOT NULL,                     -- [R6] 0=FREE_STANDARD, 1=ANNUAL_FEE, 2=CUSTOM
    fee_type          TINYINT        NOT NULL,                     -- [R6] 0=PERCENTAGE, 1=FIXED_PER_TICKET
    fee_value         DECIMAL(10, 4) NOT NULL,                     -- PERCENTAGE 存 0–100；FIXED 存每票金額
    valid_from        DATETIME2      NOT NULL,
    valid_to          DATETIME2      NULL,
    contract_status   TINYINT        NOT NULL DEFAULT 0,           -- [R6] 0=DRAFT, 1=ACTIVE, 2=TERMINATED, 3=EXPIRED
    signed_at         DATETIME2      NULL,
    signed_by_user_id CHAR(10)       NULL,
    created_by        CHAR(10)       NULL,
    created_at        DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    updated_at        DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    row_version       ROWVERSION     NOT NULL,                     -- [H5]
    CONSTRAINT CK_Contracts_Type
        CHECK (contract_type   BETWEEN 0 AND 2),
    CONSTRAINT CK_Contracts_FeeType
        CHECK (fee_type        BETWEEN 0 AND 1),
    CONSTRAINT CK_Contracts_Status
        CHECK (contract_status BETWEEN 0 AND 3),
    CONSTRAINT CK_Contracts_FeeValue
        CHECK (fee_value >= 0),                                    -- [S2]
    CONSTRAINT CK_Contracts_ValidPeriod
        CHECK (valid_to IS NULL OR valid_from <= valid_to),
    CONSTRAINT CK_Contracts_SignedConsistency
        CHECK (
            contract_status = 0 OR
            (signed_at IS NOT NULL AND signed_by_user_id IS NOT NULL)
        ),
    FOREIGN KEY (organizer_id)      REFERENCES [organizer](organizer_id),
    FOREIGN KEY (signed_by_user_id) REFERENCES [user](user_id),
    FOREIGN KEY (created_by)        REFERENCES [user](user_id)
);

CREATE UNIQUE INDEX UX_Contracts_ActiveOrganizer
    ON [contract](organizer_id)
    WHERE contract_status = 1;

CREATE NONCLUSTERED INDEX IX_Contracts_Organizer ON [contract](organizer_id);       -- [H4]
CREATE NONCLUSTERED INDEX IX_Contracts_SignedBy  ON [contract](signed_by_user_id);  -- [H4]
CREATE NONCLUSTERED INDEX IX_Contracts_CreatedBy ON [contract](created_by);         -- [H4]

-- ── system_audit_log ───────────────────────────────────────
CREATE TABLE [system_audit_log] (
    audit_id       BIGINT IDENTITY(1,1) PRIMARY KEY,               -- [R1] 高頻 Log 表
    action_user_id CHAR(10)      NULL,                             -- 操作者 user_id；刻意無 FK
    tenant_id      CHAR(10)      NULL,                             -- 受影響的 organizer_id；刻意無 FK
    action_type    TINYINT       NOT NULL,                         -- [R6] 0=AUTH...
    action_detail  NVARCHAR(100) NOT NULL,
    target_table   NVARCHAR(50)  NOT NULL,
    target_id      NVARCHAR(50)  NOT NULL,
    old_value      NVARCHAR(MAX) NULL,
    new_value      NVARCHAR(MAX) NULL,
    ip_address     NVARCHAR(45)  NULL,
    created_at     DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    CONSTRAINT CK_AuditLogs_ActionType
        CHECK (action_type BETWEEN 0 AND 5)
);

CREATE NONCLUSTERED INDEX IX_AuditLogs_Tenant     ON [system_audit_log](tenant_id, created_at);
CREATE NONCLUSTERED INDEX IX_AuditLogs_ActionUser ON [system_audit_log](action_user_id, created_at);
CREATE NONCLUSTERED INDEX IX_AuditLogs_Target     ON [system_audit_log](target_table, target_id, created_at);
CREATE NONCLUSTERED INDEX IX_AuditLogs_CreatedAt  ON [system_audit_log](created_at);  -- [S3]


-- ================================================================
-- Phase 3：訂閱方案
-- ================================================================

-- ── saas_feature ───────────────────────────────────────────
CREATE TABLE [saas_feature] (
    feature_id  VARCHAR(30)   PRIMARY KEY,                         -- [R1] 語意代碼如 MERCH_STORE
    description NVARCHAR(255) NULL,
    is_active   BIT           NOT NULL DEFAULT 1,
    created_at  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()    -- [H3]
);

-- ── membership_plan ────────────────────────────────────────
CREATE TABLE [membership_plan] (
    plan_id              VARCHAR(30)    PRIMARY KEY,               -- [R1] 語意代碼如 BRONZE, SILVER, GOLD
    plan_name            NVARCHAR(50)   NOT NULL,
    annual_fee           DECIMAL(10, 2) NULL,
    cumulative_threshold DECIMAL(15, 2) NULL,
    default_fee_rate     DECIMAL(5, 2)  NULL,
    description          NVARCHAR(500)  NULL,
    is_active            BIT            NOT NULL DEFAULT 1,
    created_at           DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    updated_at           DATETIME2      NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    row_version          ROWVERSION     NOT NULL                   -- [H5]
);

-- ── plan_feature ───────────────────────────────────────────
CREATE TABLE [plan_feature] (
    pf_id      BIGINT IDENTITY(1,1) PRIMARY KEY,                   -- [R1] 橋接表代理鍵
    plan_id    VARCHAR(30)  NOT NULL,
    feature_id VARCHAR(30)  NOT NULL,
    created_at DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),     -- [H3]
    UNIQUE (plan_id, feature_id),
    FOREIGN KEY (plan_id)    REFERENCES [membership_plan](plan_id),
    FOREIGN KEY (feature_id) REFERENCES [saas_feature](feature_id)
);

CREATE NONCLUSTERED INDEX IX_PlanFeatures_Feature ON [plan_feature](feature_id);  -- [H4]

-- ── organizer_subscription ─────────────────────────────────
CREATE TABLE [organizer_subscription] (
    subscription_id CHAR(10)     PRIMARY KEY,                      -- [R1] prefix SUB，seq_SUB
    organizer_id    CHAR(10)     NOT NULL,
    plan_id         VARCHAR(30)  NOT NULL,
    status_code     TINYINT      NOT NULL DEFAULT 0,               -- [R6] 0=ACTIVE, 1=EXPIRED
    upgrade_type    TINYINT      NOT NULL,                         -- [R6] 0=ANNUAL_FEE, 1=CUMULATIVE, 2=MANUAL
    start_date      DATETIME2    NOT NULL,
    end_date        DATETIME2    NULL,
    created_at      DATETIME2    NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    row_version     ROWVERSION   NOT NULL,                         -- [H5]
    CONSTRAINT CK_OrgSubs_Status
        CHECK (status_code  BETWEEN 0 AND 1),
    CONSTRAINT CK_OrgSubs_UpgradeType
        CHECK (upgrade_type BETWEEN 0 AND 2),
    CONSTRAINT CK_OrgSubs_Period
        CHECK (end_date IS NULL OR start_date <= end_date),       -- [M9]
    FOREIGN KEY (organizer_id) REFERENCES [organizer](organizer_id),
    FOREIGN KEY (plan_id)      REFERENCES [membership_plan](plan_id)
);

CREATE UNIQUE INDEX UX_OrgSubs_ActiveOrganizer
    ON [organizer_subscription](organizer_id)
    WHERE status_code = 0;

CREATE NONCLUSTERED INDEX IX_OrgSubs_Organizer ON [organizer_subscription](organizer_id);  -- [H4]
CREATE NONCLUSTERED INDEX IX_OrgSubs_Plan      ON [organizer_subscription](plan_id);       -- [H4]


-- ================================================================
-- Phase 4：通知與排程
-- ================================================================

-- ── notification_template ──────────────────────────────────
CREATE TABLE [notification_template] (
    template_id   VARCHAR(30)   PRIMARY KEY,                       -- [R1] 語意代碼如 KYC_APPROVED
    template_code NVARCHAR(50)  NOT NULL UNIQUE,
    channel       TINYINT       NOT NULL,                          -- [R6] 0=EMAIL, 1=SMS
    subject       NVARCHAR(150) NULL,
    body_template NVARCHAR(MAX) NOT NULL,
    is_active     BIT           NOT NULL DEFAULT 1,
    created_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    updated_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    row_version   ROWVERSION    NOT NULL,                          -- [H5]
    CONSTRAINT CK_NotifTemplate_Channel
        CHECK (channel BETWEEN 0 AND 1),
    CONSTRAINT CK_NotifTemplate_Subject
        CHECK (channel != 0 OR subject IS NOT NULL)                -- 0=EMAIL 必須有主旨
);

-- ── notification_log ───────────────────────────────────────
CREATE TABLE [notification_log] (
    log_id       BIGINT IDENTITY(1,1) PRIMARY KEY,                 -- [R1] 高頻 Log 表
    template_id  VARCHAR(30)   NOT NULL,
    recipient_id CHAR(10)      NULL,                               -- 收件者 user_id；NULL = 未註冊收件者
    channel      TINYINT       NOT NULL,                           -- [R6] 0=EMAIL, 1=SMS
    recipient    NVARCHAR(100) NOT NULL,
    status_code  TINYINT       NOT NULL DEFAULT 0,                 -- [R6] 0=PENDING, 1=SENT, 2=FAILED
    sent_at      DATETIME2     NULL,
    error_msg    NVARCHAR(255) NULL,
    created_at   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    CONSTRAINT CK_NotifLogs_Channel
        CHECK (channel     BETWEEN 0 AND 1),
    CONSTRAINT CK_NotifLogs_Status
        CHECK (status_code BETWEEN 0 AND 2),
    CONSTRAINT CK_NotifLogs_SentAt
        CHECK (status_code != 1 OR sent_at IS NOT NULL),
    CONSTRAINT CK_NotifLogs_ErrorMsg
        CHECK (status_code != 2 OR error_msg IS NOT NULL),
    FOREIGN KEY (template_id)  REFERENCES [notification_template](template_id),
    FOREIGN KEY (recipient_id) REFERENCES [user](user_id)
);

CREATE NONCLUSTERED INDEX IX_NotifLogs_Status    ON [notification_log](status_code, created_at);
CREATE NONCLUSTERED INDEX IX_NotifLogs_Template  ON [notification_log](template_id);   -- [H4]
CREATE NONCLUSTERED INDEX IX_NotifLogs_Recipient ON [notification_log](recipient_id);  -- [H4]

-- ── scheduled_job ──────────────────────────────────────────
CREATE TABLE [scheduled_job] (
    job_id      VARCHAR(30)   PRIMARY KEY,                         -- [R1] 語意代碼如 EXPIRE_SUBSCRIPTIONS
    job_code    NVARCHAR(50)  NOT NULL UNIQUE,
    description NVARCHAR(255) NULL,
    status_code TINYINT       NOT NULL DEFAULT 0,                  -- [R6] 0=IDLE, 1=RUNNING, 2=SUCCESS, 3=FAILED
    last_run_at DATETIME2     NULL,
    next_run_at DATETIME2     NULL,
    error_msg   NVARCHAR(255) NULL,
    created_at  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),   -- [H3]
    updated_at  DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),   -- [H3]
    CONSTRAINT CK_ScheduledJobs_Status
        CHECK (status_code BETWEEN 0 AND 3)
);

-- ── system_announcement ────────────────────────────────────
CREATE TABLE [system_announcement] (
    announcement_id CHAR(10)      PRIMARY KEY,                     -- [R1] prefix ANN，seq_ANN
    title           NVARCHAR(150) NOT NULL,
    content         NVARCHAR(MAX) NOT NULL,
    target_portal   NVARCHAR(20)  NOT NULL,                        -- 保留字串：B2C_FRONT, B2B_PORTAL, ADMIN_LOCAL, ALL
    is_published    BIT           NOT NULL DEFAULT 0,
    published_at    DATETIME2     NULL,
    expires_at      DATETIME2     NULL,
    created_by      CHAR(10)      NULL,
    created_at      DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    updated_at      DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    CONSTRAINT CK_Announcements_Portal
        CHECK (target_portal IN ('B2C_FRONT', 'B2B_PORTAL', 'ADMIN_LOCAL', 'ALL')),
    CONSTRAINT CK_Announcements_Published
        CHECK (is_published = 0 OR published_at IS NOT NULL),
    FOREIGN KEY (created_by) REFERENCES [user](user_id)
);

CREATE NONCLUSTERED INDEX IX_Announcements_CreatedBy ON [system_announcement](created_by);  -- [H4]


-- ================================================================
-- Phase 5：系統支撐
-- ================================================================

-- ── system_dictionary ─────────────────────────────────────
CREATE TABLE [system_dictionary] (
    dict_id    BIGINT IDENTITY(1,1) PRIMARY KEY,                   -- [R1] PK 無任何 FK 引用
    dict_type  NVARCHAR(50)  NOT NULL,
    dict_code  NVARCHAR(50)  NOT NULL,
    dict_label NVARCHAR(100) NOT NULL,
    sort_order INT           NOT NULL DEFAULT 0,
    is_active  BIT           NOT NULL DEFAULT 1,
    created_at DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),    -- [H3]
    UNIQUE (dict_type, dict_code)
);

-- ── system_config ──────────────────────────────────────────
CREATE TABLE [system_config] (
    config_id    VARCHAR(30)   PRIMARY KEY,                        -- [R1] 語意代碼如 CFG_LOGIN_MAX_FAIL
    config_key   NVARCHAR(100) NOT NULL UNIQUE,
    config_value NVARCHAR(255) NOT NULL,
    description  NVARCHAR(255) NULL,
    created_at   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    updated_at   DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME()   -- [H3]
);

-- ── media_file ─────────────────────────────────────────────
CREATE TABLE [media_file] (
    file_id       BIGINT IDENTITY(1,1) PRIMARY KEY,                -- [R1] 高頻 Append 表
    uploader_id   CHAR(10)      NULL,
    related_table NVARCHAR(50)  NULL,
    related_id    NVARCHAR(50)  NULL,
    file_type     TINYINT       NOT NULL,                          -- [R6] 0=IMAGE, 1=DOCUMENT
    file_url      NVARCHAR(500) NOT NULL,
    file_size_kb  INT           NULL,
    created_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    CONSTRAINT CK_MediaFiles_FileType
        CHECK (file_type BETWEEN 0 AND 1),
    FOREIGN KEY (uploader_id) REFERENCES [user](user_id)
);

CREATE NONCLUSTERED INDEX IX_MediaFiles_Related  ON [media_file](related_table, related_id);
CREATE NONCLUSTERED INDEX IX_MediaFiles_Uploader ON [media_file](uploader_id);  -- [H4]

-- ── user_submission ────────────────────────────────────────
CREATE TABLE [user_submission] (
    submission_id CHAR(10)      PRIMARY KEY,                       -- [R1] prefix USM，seq_USM
    user_id       CHAR(10)      NULL,
    form_type     TINYINT       NOT NULL,                          -- [R6] 0=CONTACT, 1=FEEDBACK
    content       NVARCHAR(MAX) NOT NULL,
    status_code   TINYINT       NOT NULL DEFAULT 0,                -- [R6] 0=UNREAD, 1=IN_PROGRESS, 2=RESOLVED
    handled_by    CHAR(10)      NULL,
    handled_at    DATETIME2     NULL,
    created_at    DATETIME2     NOT NULL DEFAULT SYSUTCDATETIME(),  -- [H3]
    CONSTRAINT CK_UserSubmissions_FormType
        CHECK (form_type   BETWEEN 0 AND 1),
    CONSTRAINT CK_UserSubmissions_Status
        CHECK (status_code BETWEEN 0 AND 2),
    CONSTRAINT CK_UserSubmissions_Resolved
        CHECK (status_code != 2 OR (handled_by IS NOT NULL AND handled_at IS NOT NULL)),
    FOREIGN KEY (user_id)    REFERENCES [user](user_id),
    FOREIGN KEY (handled_by) REFERENCES [user](user_id)
);

CREATE NONCLUSTERED INDEX IX_UserSubmissions_User      ON [user_submission](user_id);     -- [H4]
CREATE NONCLUSTERED INDEX IX_UserSubmissions_HandledBy ON [user_submission](handled_by);  -- [H4]

-- ================================================================
-- 腳本結束
-- ================================================================

-- =========================================
-- 場地
-- =========================================
-- 建立「Location (場地/場館)」資料表
CREATE TABLE location (
    location_id INT IDENTITY(1,1) PRIMARY KEY,      -- [PK] 場館 ID
    name NVARCHAR(100) NOT NULL,                    -- 場館名稱，如：台北小巨蛋
    total_capacity INT NOT NULL DEFAULT 0,          -- 最大容納人數或單位總數
    address  NVARCHAR(200),                         -- 地址
    grid_max_x INT NOT NULL DEFAULT 0,              -- 網格畫布最大寬(maxX)
    grid_max_y INT NOT NULL DEFAULT 0,              -- 網格畫布最大高(maxY)
    raw_svg NVARCHAR(max) NULL,                     -- SVG原始檔(物理基礎)
    bound_svg NVARCHAR(max) NULL,                   -- SVG工作檔(已帶有 data-zone-id)
    is_deleted BIT NOT NULL DEFAULT 0               -- 軟刪除標記 (0: 正常, 1: 已刪除)
);

-- 建立「Zone (分區)」資料表
CREATE TABLE zone (
    zone_id INT IDENTITY(1,1) PRIMARY KEY,          -- [PK] 分區 ID
    location_id INT NOT NULL,                       -- [FK] 關聯場館 ID
    name NVARCHAR(50) NOT NULL,                     -- 物理區域名稱，如：黃3B區
    color char(7),                                  -- 分區顏色  
    seat_count INT                                  -- 分區座位數量
);



-- 建立「Seat (座位)」資料表
CREATE TABLE seat (
    -- ❌ 原本：seat_id INT IDENTITY(1,1) PRIMARY KEY
    -- ✅ 修改為：型態改 BIGINT，並設定 DEFAULT 呼叫 SEQUENCE
    seat_id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR seat_seq, -- [PK] 座位 ID
    zone_id INT NOT NULL,                                       -- [FK] 關聯物理分區 ID
    row_num INT NOT NULL,                                       -- 排數，如：15
    seat_num INT NOT NULL,                                      -- 座位號碼，如：30
    x_index INT NOT NULL,                                       -- 座位網格CSS Grid X 座標
    y_index INT NOT NULL,                                       -- 座位網格CSS Grid Y 座標

    -- [防護] 確保同分區沒有重複的排號，且網格位置不重疊
    CONSTRAINT UQ_Zone_Seat_Num UNIQUE (zone_id, row_num, seat_num),
    CONSTRAINT UQ_Zone_Grid_Index UNIQUE (zone_id, x_index, y_index)
);


-- =========================================
-- 活動
-- =========================================

--活動主題
CREATE TABLE theme (
    theme_id     INT IDENTITY(1,1) PRIMARY KEY,-- PK 
    organizer_id CHAR(10) NOT NULL,        -- FK organizer(organizer_id)
    status       VARCHAR(20)  NOT NULL DEFAULT 'DRAFT',--DRAFT("草稿"),ACTIVE("啟用"),ARCHIVED("已結束"),DELETED("軟刪");
    title        NVARCHAR(150)  NOT NULL, -- 標題
    detail       NVARCHAR(MAX)          , -- 詳情
    image        NVARCHAR(255)          , -- 圖
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(), --建立時間
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()  --更新時間
    -- CONSTRAINT fk_theme_organizer
    --     FOREIGN KEY (organizer_id)  REFERENCES organizer(organizer_id)
);

--活動場次
CREATE TABLE session (
    session_id  INT IDENTITY(1,1) PRIMARY KEY,-- PK
    theme_id    INT           NOT NULL,       -- FK theme(theme_id)
    location_id INT           NOT NULL,       -- FK location(location_id)
    status VARCHAR(20) NOT NULL DEFAULT 'DRAFT', --DRAFT("草稿"),ACTIVE("公開"),ARCHIVED("已結束"),DELETED("軟刪");
    title       NVARCHAR(150) NOT NULL,   -- 標題    
    detail      NVARCHAR(MAX),            -- 詳情
    publish_time       DATETIME2,         -- 公開時間
    selling_start_time DATETIME2,         -- 販售開始時間
    selling_end_time   DATETIME2,         -- 販售結束時間
    start_time         DATETIME2,         -- 場次開始時間
    end_time           DATETIME2,         -- 場次結束時間
    created_at DATETIME2 NOT NULL DEFAULT GETDATE(), --建立時間
    updated_at DATETIME2 NOT NULL DEFAULT GETDATE()  --更新時間
    -- CONSTRAINT fk_session_theme
    --     FOREIGN KEY (theme_id) REFERENCES theme(theme_id),
        
    -- CONSTRAINT fk_session_location
    --     FOREIGN KEY (location_id) REFERENCES location(location_id)
);

-- =========================================
-- 競標
-- =========================================

--競標商品
CREATE TABLE auction (
    auction_id INT IDENTITY(1,1) PRIMARY KEY,-- PK
    theme_id        INT      NOT NULL,        -- FK theme(theme_id)
    status          VARCHAR(20)   NOT NULL DEFAULT 'DRAFT', --DRAFT("草稿"),ACTIVE("啟用"),ARCHIVED("已結束"),DELETED("軟刪");
    title           NVARCHAR(150) NOT NULL,       -- 標題    
    detail          NVARCHAR(MAX),                -- 詳情
    image           NVARCHAR(255),                -- 圖
    start_price     DECIMAL(10, 2)NOT NULL,             -- 起標價
    buyout_price    DECIMAL(10, 2)        ,             -- 直購價
    current_price   DECIMAL(10, 2),                     -- 目前價格
    start_time      DATETIME2     NOT NULL,                 -- 開始時間
    end_time        DATETIME2     NOT NULL,                 -- 結束時間
    created_at      DATETIME2     NOT NULL DEFAULT GETDATE(), --建立時間
    updated_at      DATETIME2     NOT NULL DEFAULT GETDATE(), --更新時間
    CONSTRAINT fk_auction_theme
        FOREIGN KEY (theme_id) REFERENCES theme(theme_id)
);

--bid出價紀錄
CREATE TABLE bid (
    bid_id      INT IDENTITY(1,1) PRIMARY KEY,-- PK
    auction_id  INT           NOT NULL,       -- FK auction(auction_id)
    user_id     CHAR(10)      NOT NULL,       -- FK user(user_id)
    bid_price   DECIMAL(10, 2)NOT NULL,       -- 出價金額
    created_at  DATETIME2     NOT NULL DEFAULT GETDATE(), --建立時間
    CONSTRAINT fk_bid_auction
        FOREIGN KEY (auction_id) REFERENCES auction(auction_id),
    CONSTRAINT fk_bid_user
        FOREIGN KEY (user_id) REFERENCES [user](user_id)
);

CREATE INDEX idx_bid_auction_price
ON bid (auction_id, bid_price DESC);

-- =========================================
-- 票務
-- =========================================

-- 建立「Ticket_Type (票種)」資料表
CREATE TABLE ticket_type (
    ticket_type_id INT IDENTITY(1,1) PRIMARY KEY,   -- [PK] 票種 ID（自增主鍵）
    theme_id INT NOT NULL,                          -- [FK] 關聯活動主題 ID
    name NVARCHAR(50) NOT NULL,                      -- 廠商自訂名稱，如：搖滾A區、3800看台
    price DECIMAL(10, 2) NOT NULL,                  -- 票價金額
    color CHAR(7),                                  -- 票種顏色
    is_deleted BIT NOT NULL DEFAULT 0              -- 軟刪除標記 (0: 正常, 1: 已刪除)
);

-- 分區-票種 中介表
CREATE TABLE session_zone_mapping(
    mapping_id BIGINT IDENTITY(1,1) PRIMARY KEY,    --唯一識別碼
    session_id INT NOT NULL,                        --[FK]對應演唱會場次ID
    zone_id INT NOT NULL,                           --[FK]對應實體分區ID
    ticket_type_id INT NOT NULL,                    --[FK]對應票種ID
    is_enabled BIT NOT NULL DEFAULT 1,              -- 票種是否上架 (0: 前端完全不顯示, 1: 上架)
    is_deleted BIT NOT NULL DEFAULT 0,              -- 軟刪除標記 (0: 正常, 1: 已刪除)
    --審計欄位  是誰/何時改了票價綁定
    created_at DATETIME2,                           -- 建立時間
    created_by CHAR(10),                            -- [FK] 建立人
    updated_at DATETIME2,                           -- 最後更新時間
    updated_by CHAR(10)                             -- [FK] 最後修改人

    -- 防呆：唯一約束 (Unique Constraint)
    -- 確保「同一個場次」的「同一個實體分區」，只能綁定「一種票種」
    -- 如果沒有這行，系統可能會讓「特區」同時綁定 8000元 跟 3000元，後續生票會大亂。
    CONSTRAINT UQ_Session_Zone UNIQUE (session_id, zone_id),
);

-- 建立「Ticket (票券庫存)」資料表 (場次 + 座位 + 票種)
CREATE TABLE ticket (
    ticket_id BIGINT PRIMARY KEY DEFAULT NEXT VALUE FOR ticket_seq,        -- [PK] 票券 ID（自增主鍵）
    session_id INT NOT NULL,                        -- [FK] 關聯場次 ID
    -- 這裡必須跟著 Seat 表升級成 BIGINT
    seat_id BIGINT NOT NULL,                           -- [FK] 關聯物理座位 ID
    ticket_type_id INT,                             -- [FK] 關聯廠商票種 ID
    status TINYINT DEFAULT 1,                       -- 狀態 (1: 可售, 2: 鎖定中, 3: 已售出, 0: 硬體保留/不可見)
    version INT DEFAULT 0,                          -- 樂觀鎖版號（搭配 JPA @Version），防超賣機制
    locked_by_user CHAR(10),                        -- [FK] 鎖定者 ID（防惡意劫持，記錄被誰鎖定）
    locked_until DATETIME2 NULL                     -- 鎖定到期時間（寫入當下 +10 分鐘，超時未轉 3: 已售出 由排程還原庫存）

    CONSTRAINT UQ_Session_Seat UNIQUE (session_id, seat_id)
);

-- 為 ticket 表的併發查詢欄位建立高效索引 (加載 SeatMap 選位狀態時的速度優化)
CREATE NONCLUSTERED INDEX IX_Ticket_Session_Status 
ON ticket (session_id, status) 
INCLUDE (seat_id, ticket_type_id);

-- =========================================
-- 商城
-- =========================================

--商品分類
CREATE TABLE product_categories (
    category_id INT IDENTITY(1,1) PRIMARY KEY,
    category_name NVARCHAR(100) NOT NULL,
    parent_id INT NULL
);

-- 商品主檔
CREATE TABLE products (
    product_id INT IDENTITY(1,1) PRIMARY KEY,
    theme_id INT NOT NULL,
    category_id INT NULL,
    product_name NVARCHAR(100) NOT NULL,
    product_sim_description NVARCHAR(100) NOT NULL,
    product_description NVARCHAR(MAX) NULL,
    status TINYINT NOT NULL DEFAULT 0,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 NULL,
    released_at DATETIME2 NULL,
    deleted_at DATETIME2 NULL
);

-- 商品款式
CREATE TABLE product_variants (
    variant_id INT IDENTITY(1,1) PRIMARY KEY,
    product_id INT NOT NULL,
    org_skuno NVARCHAR(20) NULL,
    product_size NVARCHAR(50) NULL,
    product_color NVARCHAR(50) NULL,
    unit_price DECIMAL(10,2) NOT NULL,
    stock_qty INT NOT NULL DEFAULT 0,
    barcode NVARCHAR(50) NULL,
    status TINYINT NOT NULL DEFAULT 0,
    
    -- CONSTRAINT UQ_product_variants_barcode UNIQUE(barcode)
);

-- 商品圖片
CREATE TABLE product_images (
    image_id INT IDENTITY(1,1) PRIMARY KEY,
    product_id INT NOT NULL,
    image_url NVARCHAR(1000) NULL,
    sort_order INT NULL,
    is_main BIT NULL
);

--購物車
CREATE TABLE merch_cart (
    cart_id INT IDENTITY(1,1) PRIMARY KEY,
    locked_by_user CHAR(10) NOT NULL,      -- [FK] 鎖定者 ID（防惡意劫持，記錄被誰鎖定） updated on 6/29
    variant_id INT NOT NULL,
    quantity INT NOT NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
    updated_at DATETIME2 DEFAULT GETDATE(),
    product_expire_date DATETIME2 NULL
);

-- =========================================
-- 訂單
-- =========================================
-- 票券訂單主檔
CREATE TABLE ticket_orders (
    t_order_id CHAR(10) PRIMARY KEY,   
    user_id CHAR(10) NOT NULL,             
    total_amount DECIMAL(10,2) NOT NULL,
    merchant_trade_no VARCHAR(20) unique,    -- 串接綠界

    -- ▼▼ 實名制：聯絡人資訊 ▼▼
    contact_name NVARCHAR(50) NOT NULL,      -- 聯絡人姓名 (建議用 NVARCHAR 支援中文)
    contact_email VARCHAR(100) NOT NULL,     -- 聯絡人信箱
    contact_phone VARCHAR(20) NOT NULL,      -- 聯絡人手機
    -- ▲▲ 實名制：聯絡人資訊 ▲▲    

    payment_status VARCHAR(20) DEFAULT 'UNPAID',
    failed_at  DATETIME2 NULL,
    created_at DATETIME2 DEFAULT GETDATE(),
);

-- 票券訂單明細
CREATE TABLE ticket_order_details (
    t_detail_id NVARCHAR(50) PRIMARY KEY,   
    t_order_id CHAR(10) NOT NULL,       
    ticket_id BIGINT NOT NULL,  

    unit_price DECIMAL(10,2) NOT NULL,       --成交單價

    -- ▼▼ 實名制：入場人資訊 ▼▼
    real_name NVARCHAR(255) NOT NULL,        -- 入場人姓名
    identity_number VARCHAR(20) NOT NULL,    -- 證件字號
    is_modified BIT DEFAULT 0,               -- 是否已修改過實名資料 (管控修改次數)
    -- ▲▲ 實名制：入場人資訊 ▲▲   

    item_status NVARCHAR(50) DEFAULT 'NORMAL',           
    qr_code_hash NVARCHAR(255) NULL UNIQUE,     
    is_used NVARCHAR(50) DEFAULT 'Unredeemed' NOT NULL,                  
    used_at DATETIME2 NULL
);

-- 商城訂單主檔
CREATE TABLE merch_orders (
    m_order_id          CHAR(10)      PRIMARY KEY, 
    user_id             CHAR(10)      NOT NULL,     
    total_amount        DECIMAL(10,2) NOT NULL, 
    payment_status      VARCHAR(20)   DEFAULT 'UNPAID',
    paid_at             DATETIME2     NULL,     
    merchant_trade_no   VARCHAR(20)   unique,    -- 串接綠界
    recipient_name      NVARCHAR(50)  NULL,
    recipient_phone     VARCHAR(20)   NULL,
    recipient_email     VARCHAR(100)  NULL,
    recipient_address   NVARCHAR(255) NULL,
    identity_number     VARCHAR(20)   NULL,
    failed_at           DATETIME2     NULL,
    created_at          DATETIME2     NOT NULL DEFAULT GETDATE()
);

-- 商城訂單明細
CREATE TABLE merch_order_details (
    m_detail_id   NVARCHAR(50)  PRIMARY KEY, 
    m_order_id    CHAR(10)      NOT NULL,
    product_id    INT           NOT NULL, 
    variant_id    INT           NOT NULL, 
    quantity      INT           NOT NULL, 
    unit_price    DECIMAL(10,2) NOT NULL, 
    item_status   NVARCHAR(50)  DEFAULT 'NORMAL'
);

-- 廠商結算
CREATE TABLE merchant_settlements (
    settlement_id        NVARCHAR(50)  PRIMARY KEY, 
    organizer_id         CHAR(10)      NOT NULL,     
    period_start         DATE          NOT NULL, 
    period_end           DATE          NOT NULL, 
    total_orders_amount  DECIMAL(10,2) NOT NULL, 
    final_payout_amount  DECIMAL(10,2) NOT NULL, 
    item_status          NVARCHAR(50)  NOT NULL DEFAULT 'PENDING', 
    processed_at         DATETIME2     NULL,     
    created_at           DATETIME2     NOT NULL DEFAULT GETDATE()
);


-- 退款申請單 (退票/退貨審核制：會員送出申請，主辦方於 B2B 後台核准後才執行實際退款)
CREATE TABLE refund_request (
    request_id    INT IDENTITY(1,1) PRIMARY KEY,
    request_type  NVARCHAR(10)  NOT NULL,                    -- TICKET 退票 | MERCH 退貨
    order_id      CHAR(10)      NOT NULL,                    -- t_order_id 或 m_order_id
    detail_id     NVARCHAR(50)  NOT NULL,                    -- t_detail_id 或 m_detail_id
    user_id       CHAR(10)      NOT NULL,                    -- 申請會員
    organizer_id  CHAR(10)      NOT NULL,                    -- 建立時由關聯鏈推得，供 B2B 過濾
    reason        NVARCHAR(500) NOT NULL,                    -- 申退理由
    status        NVARCHAR(10)  NOT NULL DEFAULT 'PENDING',  -- PENDING/APPROVED/REJECTED
    reject_note   NVARCHAR(500) NULL,                        -- 駁回原因
    created_at    DATETIME2     NOT NULL DEFAULT GETDATE(),  -- 申請時間
    processed_at  DATETIME2     NULL                         -- 審核時間
);

-- =========================================
-- 建立外鍵約束 (Foreign Key Constraints)
-- =========================================

/* --- Location --- */

/* --- Zone --- */
ALTER TABLE zone ADD CONSTRAINT fk_zone_location FOREIGN KEY (location_id) REFERENCES location(location_id);

/* --- Seat --- */
ALTER TABLE seat ADD CONSTRAINT fk_seat_zone FOREIGN KEY (zone_id) REFERENCES zone(zone_id);


--=============================================================================================================================
/* --- theme --- */
ALTER TABLE theme ADD CONSTRAINT fk_theme_organizer FOREIGN KEY (organizer_id) REFERENCES organizer(organizer_id);

/* --- session --- */
ALTER TABLE session ADD CONSTRAINT fk_session_theme FOREIGN KEY (theme_id) REFERENCES theme(theme_id);
ALTER TABLE session ADD CONSTRAINT fk_session_location FOREIGN KEY (location_id) REFERENCES location(location_id);


--=============================================================================================================================
/* --- Ticket_Type --- */
ALTER TABLE ticket_type ADD CONSTRAINT fk_tickettype_theme FOREIGN KEY (theme_id) REFERENCES theme(theme_id);

/* --- session_zone_mapping --- */
ALTER TABLE session_zone_mapping ADD CONSTRAINT fk_tickettype_created_by FOREIGN KEY (created_by) REFERENCES [user](user_id);
ALTER TABLE session_zone_mapping ADD CONSTRAINT fk_tickettype_updated_by FOREIGN KEY (updated_by) REFERENCES [user](user_id);


ALTER TABLE session_zone_mapping ADD CONSTRAINT FK_Mapping_Session FOREIGN KEY (session_id) REFERENCES session(session_id);
ALTER TABLE session_zone_mapping ADD CONSTRAINT FK_Mapping_Zone FOREIGN KEY (zone_id) REFERENCES zone(zone_id);
ALTER TABLE session_zone_mapping ADD CONSTRAINT FK_Mapping_TicketType FOREIGN KEY (ticket_type_id) REFERENCES ticket_type(ticket_type_id);


/* --- Ticket --- */
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_session FOREIGN KEY (session_id) REFERENCES session(session_id);
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_seat FOREIGN KEY (seat_id) REFERENCES seat(seat_id);
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_tickettype FOREIGN KEY (ticket_type_id) REFERENCES ticket_type(ticket_type_id);
ALTER TABLE ticket ADD CONSTRAINT fk_ticket_locked_by FOREIGN KEY (locked_by_user) REFERENCES [user](user_id);

--=============================================================================================================================
/* --- Product_Categories --- */
ALTER TABLE product_categories ADD CONSTRAINT fk_product_categories_parent FOREIGN KEY(parent_id) REFERENCES product_categories(category_id);

/* --- Products --- */
ALTER TABLE products ADD CONSTRAINT fk_products_category FOREIGN KEY(category_id) REFERENCES product_categories(category_id);
ALTER TABLE products ADD CONSTRAINT fk_products_theme FOREIGN KEY(theme_id) REFERENCES theme(theme_id);

/* --- Product_Variants --- */
ALTER TABLE product_variants ADD CONSTRAINT fk_product_variants_product FOREIGN KEY(product_id) REFERENCES products(product_id);

/* --- Product_Images --- */
ALTER TABLE product_images ADD CONSTRAINT fk_product_images_product FOREIGN KEY(product_id) REFERENCES products(product_id);

/* --- Merch_Cart --- */
ALTER TABLE merch_cart ADD CONSTRAINT fk_merch_cart_variant FOREIGN KEY(variant_id) REFERENCES product_variants(variant_id);
ALTER TABLE merch_cart ADD CONSTRAINT fk_merch_cart_locked_by FOREIGN KEY(locked_by_user) REFERENCES [user](user_id);

--=============================================================================================================================
/* --- Ticket_Orders --- */
ALTER TABLE ticket_orders ADD CONSTRAINT fk_ticket_orders_user FOREIGN KEY (user_id) REFERENCES [user](user_id);

/* --- Ticket_Order_Details --- */
ALTER TABLE ticket_order_details ADD CONSTRAINT fk_ticket_order_details_order FOREIGN KEY (t_order_id) REFERENCES ticket_orders(t_order_id);
ALTER TABLE ticket_order_details ADD CONSTRAINT fk_ticket_order_details_ticket FOREIGN KEY (ticket_id) REFERENCES ticket(ticket_id); 

/* --- Merch_Orders --- */
ALTER TABLE merch_orders ADD CONSTRAINT fk_merch_orders_user FOREIGN KEY (user_id) REFERENCES [user](user_id);

/* --- Merch_Order_Details --- */
ALTER TABLE merch_order_details ADD CONSTRAINT fk_merch_order_details_order FOREIGN KEY (m_order_id) REFERENCES merch_orders(m_order_id);
ALTER TABLE merch_order_details ADD CONSTRAINT fk_merch_order_details_product FOREIGN KEY (product_id) REFERENCES products(product_id);
-- 【已補上】商品款式外鍵關聯
ALTER TABLE merch_order_details ADD CONSTRAINT fk_merch_order_details_variant FOREIGN KEY (variant_id) REFERENCES product_variants(variant_id);

/* --- Merchant_Settlements --- */
ALTER TABLE merchant_settlements ADD CONSTRAINT fk_merchant_settlements_organizer FOREIGN KEY (organizer_id) REFERENCES organizer(organizer_id);