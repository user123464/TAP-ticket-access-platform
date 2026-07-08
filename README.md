# TAP — Ticket Access Platform

售票 / 拍賣 / 周邊商品的整合平台。採用「一個後端 + 兩個前端(一般使用者站、管理後台) + 基礎設施(資料庫 / 快取 / 訊息佇列 / 監控)」的 monorepo 架構。

> 這是一份可公開的乾淨版原始碼:已移除開發日誌、實名認證文件等個資與真實金鑰。所有機密改由環境變數注入,預設值僅供本機開發,**正式環境務必覆蓋**。

---

## 目錄

- [技術棧](#技術棧)
- [專案結構](#專案結構)
- [前置需求](#前置需求)
- [快速開始](#快速開始)
- [連接埠一覽](#連接埠一覽)
- [環境變數](#環境變數)
- [資料庫初始化行為(重要)](#資料庫初始化行為重要)
- [對外展示 / online 部署](#對外展示--online-部署)
- [監控(選用)](#監控選用)
- [壓力測試(選用)](#壓力測試選用)
- [安全注意事項](#安全注意事項)

---

## 技術棧

| 層 | 技術 |
|---|---|
| 後端 | Java 21、Spring Boot 3.5、Spring Data JPA、Spring Security、JWT、Spring Data Redis、Spring AMQP(RabbitMQ)、Spring Mail、Cloudinary、Actuator + Micrometer/Prometheus。打包為 **war**,Maven Wrapper 同梱。 |
| 資料庫 | Microsoft SQL Server 2025(容器內建置) |
| 一般使用者前端 | Vue 3 + Vite、Pinia、Vue Router、Axios、Bootstrap、Element Plus、SweetAlert2、html5-qrcode(掃碼入場)、Cropper.js、Swiper |
| 管理後台前端 | Vue 3 + Vite、Chart.js、md-editor-v3、xlsx(匯出 Excel)、dayjs |
| 基礎設施 | SQL Server、Redis、RabbitMQ、MailDev(開發用信箱)、Prometheus、Grafana |

---

## 專案結構

```
.
├── backend/              # Spring Boot 後端 (Java 21, Maven)
│   ├── src/main/resources/
│   │   ├── application.properties        # 主要組態 (吃 ${ENV} 環境變數)
│   │   ├── application-prod.properties    # 正式環境覆蓋範例
│   │   ├── schema.sql                     # 每次啟動 DROP+CREATE 所有資料表
│   │   └── data.sql                       # 測試/展示用種子資料
│   └── documents/        # 後端讀寫的檔案 (系統文件、Email 樣板、公開圖片種子)
├── frontend/             # 一般使用者站 (Vue 3 + Vite)
├── frontend-admin/       # 管理後台 (Vue 3 + Vite)
├── sqlserver/            # SQL Server 容器 (Dockerfile / entrypoint / init.sql)
├── monitoring/           # Prometheus 設定
├── k6/                   # 壓力測試腳本 (.js;k6 執行檔請自行安裝)
├── docker-compose.yml            # 基礎設施 (DB / Redis / RabbitMQ / MailDev)
└── docker-compose.monitoring.yml # 監控 (Prometheus / Grafana / redis-exporter)
```

---

## 前置需求

- **Docker Desktop**(啟動 SQL Server / Redis / RabbitMQ / MailDev)
- **JDK 21**
- **Node.js** `^20.19.0 || >=22.12.0`
- (選用)ngrok 或 cloudflared — 對外展示金流回呼時使用

---

## 快速開始

### 1. 啟動基礎設施

```bash
docker compose build
docker compose up -d
# 等 SQL Server 完成初始化
docker logs -f sqlserver   # 看到 "Database initialization completed." 即可 Ctrl+C
```

### 2. 啟動後端(port 8080)

```bash
cd backend
# 本機開發:直接跑即可,application.properties 內有開發用預設值
./mvnw spring-boot:run        # Windows: mvnw.cmd spring-boot:run
```

> 後端每次啟動都會依 `schema.sql` 重建所有資料表、載入 `data.sql` 種子資料,並清空 Redis。詳見[資料庫初始化行為](#資料庫初始化行為重要)。

### 3. 啟動一般使用者前端(port 5173)

```bash
cd frontend
npm install
npm run dev
```

### 4. 啟動管理後台前端(port 5174)

```bash
cd frontend-admin
npm install
npm run dev
```

開啟瀏覽器:一般站 http://localhost:5173 、後台 http://localhost:5174 。

---

## 連接埠一覽

| 服務 | 位址 / 埠 | 說明 |
|---|---|---|
| 後端 API | http://localhost:8080 | Spring Boot |
| 使用者前端 | http://localhost:5173 | Vite dev |
| 管理後台前端 | http://localhost:5174 | Vite dev |
| SQL Server | localhost:9433 | DB(容器內 1433) |
| Redis | localhost:9979 | 快取 / 分散式鎖 |
| RabbitMQ | http://localhost:15672 | 管理介面(MQ 本體 5672) |
| MailDev | http://localhost:9080 | 開發用信箱(SMTP 9025) |
| Prometheus | http://localhost:9090 | 監控(選用) |
| Grafana | http://localhost:3000 | 儀表板(選用) |

---

## 環境變數

**本專案 clone 下來零設定即可啟動**,所有設定都有內建預設值(localhost 那一套)。後端組態集中在 `backend/src/main/resources/application.properties`,以 `${VAR:預設值}` 形式讀取——要換環境(例如正式部署或對外展示)時,只需設定對應環境變數覆蓋,**不需修改程式碼**:

```bash
# Linux/macOS 範例;Windows 用 setx 或系統環境變數
export BACKEND_URL='https://你的公開網域'    # 對外展示時金流回呼用
./mvnw spring-boot:run
```

前端設定放在各自的 `.env`(開發)/ `.env.online`(對外)。

> ⚠️ **Vite 前端變數在 build 時就編譯進 bundle**,runtime 無法覆蓋。切換後端位址需重新 build。所有 `VITE_*` 值本來就會出現在瀏覽器,屬公開值。

---

## 資料庫初始化行為(重要)

本專案預設為「**每次啟動後端都重置資料庫**」的開發模式:

- `spring.sql.init.mode=always` → 每次啟動執行 `schema.sql`(先 `DROP TABLE` 所有資料表再 `CREATE`)與 `data.sql`(重新載入種子資料)。
- `app.redis.flush-on-startup=true` → 啟動時清空 Redis,與資料庫重置同步。

**這代表後端每次重啟,資料庫內容都會被清空並還原成種子資料。** 若要保留資料(例如正式環境),請改用 `application-prod.properties` 的設定(`spring.sql.init.mode=never`、Redis flush 關閉),或以 `--spring.profiles.active=prod` 啟動。

SQL Server 容器本身(`sqlserver/`)只在第一次啟動時建立 `ProjectDB` 資料庫並以 marker 檔記錄,之後不重複執行;資料表層級的重建則由後端負責。

---

## 對外展示 / online 部署

前端 `npm run demo` = `build:online` + `preview`(以 online 模式編譯後啟動預覽伺服器)。

金流(綠界 ECPay)回呼需要「公開可達的後端網址」。展示時:

1. 用 ngrok 或 cloudflared 取得一個公開網址指向本機後端 8080。
2. 設定後端環境變數 `BACKEND_URL`(公開網址)、`FRONTEND_URL`(前端公開網址)再啟動 — 所有 ECPay 回呼 / 導轉網址會自動跟著改。
3. 前端以 `npm run demo` 或 `npm run build:online` 產出對應 API 位址的版本。

> 詳細的 online 隧道與真實金流展示 SOP 屬內部資料,不在本公開版中。

---

## 監控(選用)

```bash
# 監控需接到本體的 tap-network,故須先啟動 docker-compose.yml
docker compose -f docker-compose.monitoring.yml up -d
```

Prometheus http://localhost:9090 、Grafana http://localhost:3000 。監控服務刻意與主體分離,讓 `docker compose down -v` 重置後端時不會清掉 Grafana 儀表板設定。

---

## 壓力測試(選用)

`k6/` 內含 k6 測試腳本(`.js`)。請自行安裝 [k6](https://k6.io/docs/get-started/installation/) 後執行,例如:

```bash
k6 run k6/v1-warmup-100vu.js
```

---

## 安全注意事項

這是一份 **DEMO 專案**,repo 內的密碼(`P@ssw0rd`、`auth_dev_pwd`、`guest`)、JWT 金鑰、綠界 ECPay 沙盒值、Google/Turnstile 公開金鑰都是**開發用公開值**,只保護 localhost 容器,可以被看到。正式上線時再以環境變數覆蓋即可。

真正該保護、**絕不可放進 repo** 的只有「綁個人帳戶或會計費的 API 金鑰」,例如:

- Cloudflare API / tunnel token
- Google Gemini(或其他 AI)API key
- 正式的金流商店金鑰、雲端儲存(Cloudinary 等)的 API secret、寄信服務金鑰

這類金鑰請一律用環境變數注入,不要 commit。
