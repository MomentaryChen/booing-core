# booking-core

全端 **預約／訂位** 平台：商家可管理服務、排程與預約；一般使用者透過公開店面預約；系統管理員可檢視商家、網域範本、設定與稽核紀錄。

**語言：** [English](README.md) · [繁體中文](README.zh-TW.md)

## 平台介紹

booking-core 是一套 **多租戶（multi-tenant）預約平台**，目標是讓不同產業共用同一個核心引擎，而不是為單一業種硬寫固定流程。

平台主要有三個使用介面：

- **Client（客戶端）**：終端使用者查看可用時段並建立預約
- **Merchant（商家端）**：管理可預約資源、排程與預約作業
- **System Admin（系統管理端）**：負責租戶治理、RBAC 權限與稽核可追溯性

核心能力包含 **Resource/Slot 預約模型**、**RBAC 權限控管**、**可稽核（auditability）**，以及透過策略與 metadata 擴充的 **extensibility**。

技術選型以可維護與可落地為優先：後端採 **Java 21 + Spring Boot**，前端採 **React + Vite**。這樣的設計讓團隊在新增新領域時，多數情況可以用「加類型、加規則」擴充，而不必重寫核心預約流程。

## 系統概覽

booking-core 是一個以「**可配置規則 + 可插拔策略**」為核心的預約平台，目標是用同一套引擎支援多產業的預約情境，而非為單一業種客製。它把可預約的實體抽象為 **Resource**，把時間與容量抽象為 **Slot**，並以可組合的可用性規則與例外（exceptions）描述「何時可預約、可預約多少」。系統同時提供一致的預約生命週期（建立、確認/取消、逾期、完成等）與合法狀態轉移，讓前台體驗、商家營運與平台治理能在同一條管線上協作。目標使用者包含：**Client**（終端預約者）、**Merchant**（提供資源與管理供給者）、**System Admin**（平台設定、租戶治理與稽核者）。快速心智模型：先定義 Resource 類型與 metadata，再用規則算出可用 Slot，最後用狀態機驅動 Booking 從請求走到結束。

- **核心能力**：Resource/Slot 抽象、可用性規則 + exceptions、即時計算可用時段
- **預約流程**：以狀態機管理生命週期與合法轉移，避免任意改狀態
- **角色分工**：Client 搜尋/下單；Merchant 配置供給與例外；System 管治理/稽核/多租戶
- **非目標**：不內建特定業種名詞與固定流程；不以大量預先展開 slot 資料為設計前提
- **可擴充性**：新增業務以「加 ResourceType / metadata schema / 策略」為主，盡量不改核心管線

## 架構原則／領域模型

booking-core 以「**Resource（可被預約的實體）**」與「**Slot（時間/容量單位）**」作為核心抽象，避免將特定業務名詞與流程寫死在服務層。差異性透過 `resource_type` 與 `metadata`（JSON）承載，讓新領域多以「加類型/加策略/加資料」擴充，而非修改核心管線。規則與限制採 **策略模式**（Validation Strategy）掛載於資源類型，維持可插拔與可測試。可用性由 **Slot Engine 即時計算**：以營業/開放規則與例外（exceptions）推導候選時段，再與已預約/鎖定資料做集合運算，**不預先展開大量未來 slot 列**，確保規則變更可立即生效。生命週期以 **狀態機**集中管理，禁止任意層直接改狀態以繞過合法轉移。全系統採 **多租戶隔離**：資料表含 `tenant_id`，所有查寫預設強制 tenant 範圍，確保隔離不可被遺漏。

- **Resource/Slot 抽象**：用通用模型承載差異，靠 `resource_type` + `metadata` 擴充
- **策略式驗證**：依類型選擇 Validation Strategy，避免 service 內長串 `if/else`
- **即時計算可用性**：Slot Engine 由規則+例外推導，不做全年/全量預展開
- **狀態轉移合法性**：集中 transition 入口與條件，拒絕非法狀態跳轉
- **多租戶強制範圍**：所有查寫必帶 `tenant_id` 條件，避免跨租戶資料洩漏

## 技術棧


| 層級  | 技術                                                 |
| --- | -------------------------------------------------- |
| 後端  | Java 21、Spring Boot 3.3、Spring Data JPA、MySQL      |
| 前端  | React 18、Vite 5、React Router 6、介面雙語（zh-TW / en-US） |


## 目錄結構

```
booking-core/
├── backend/    # Spring Boot API（本機：埠 28080）
└── frontend/   # Vite + React 單頁應用（開發：埠 25173）
```

## 規格生命週期（open -> progress -> closed）

- 新規格一律先放在 `doc/specs/open/`，檔名格式為 `YYYY-MM-DD_<kebab-case-topic>.md`。
- 一旦開始實作，必須立即移動到 `doc/specs/progress/`。
  - 觸發條件任一成立即需移動：切工單並指派 owner、建立實作任務、開始 coding、開 PR、或產生第一個實作 commit。
- 規格結案後，使用相同檔名移動到 `doc/specs/closed/`。
- 同一份 spec 只能有一個進行中的主檔，不可同時存在於 `open/` 與 `progress/`。

## 環境需求

- **JDK 21** 與 **Maven 3.6+**（後端）
- **Node.js 18+** 與 **pnpm**（前端；版本可參考 `frontend/package.json` 的 `packageManager`）

## 啟動後端

```bash
cd backend
mvn spring-boot:run
```

- REST API 基底路徑：`http://localhost:28080/api`

後端預設使用 **dev profile**（`spring.profiles.default=dev`）並連線到 **MySQL**。可透過環境變數調整：

- `DB_HOST` / `DB_PORT` / `DB_NAME`
- `DB_USERNAME` / `DB_PASSWORD`
- `JWT_SECRET`（dev 有預設值；prod 必須提供）

## 啟動前端

```bash
cd frontend
pnpm install
pnpm dev
```

瀏覽器開啟 Vite 顯示的網址（預設 `http://localhost:25173`）。開發時前端會請求 `http://localhost:28080/api`；後端已對 `http://localhost:25173` 開放 CORS。

**正式建置：**

```bash
pnpm build
pnpm preview   # 可選：本機預覽建置結果
```

## 功能區塊（路由）


| 路徑                                                                 | 說明                                |
| ------------------------------------------------------------------ | --------------------------------- |
| `/system`                                                          | 系統管理後台                            |
| `/merchant`、`/merchant/appointments`、`/merchant/settings/schedule` | 商家端                               |
| `/client`                                                          | 客戶端流程（待實作頁）                       |
| `/client/booking/:slug`                                            | 公開店面（依商家 slug） |
| `/store/:slug`                                                     | 重新導向至 `/client/booking/:slug`     |


## API 概要

所有路徑皆在 `/api` 之下，涵蓋商家 CRUD、服務、營業時間、預約、自訂樣式、動態欄位、資源、可用性例外、公開店面預約（`**/api/client/...**`）、登入發放 JWT（`**/api/auth/login**`）與系統端（`/api/system/...`）等。

當設定 `booking.platform.jwt.secret`（建議至少 256 位元強度的金鑰）時，會對 `/api/merchant/**` 強制 JWT（角色 `MERCHANT`、`SUB_MERCHANT` 或 `SYSTEM_ADMIN`），以及對 `/api/system/**` 要求 `SYSTEM_ADMIN`。此模式下仍可用 `booking.platform.system-admin-token` 存取 `/api/system/**`。

## 內部 admin 與手動 demo seed

- **內部 `SYSTEM_ADMIN`（營運／內部人員）：** 啟動時若尚無任何 `SYSTEM_ADMIN` 平台使用者，後端可依 `INTERNAL_SYSTEM_ADMIN_*` 自動建立第一位管理員（見 `application-dev.yml` / `application-prod.yml`）。若改由 SQL 或外部 IAM 管理，請設 `INTERNAL_SYSTEM_ADMIN_AUTO_PROVISION=false`。
- **Demo 商家／客戶：** 已移除應用程式 bootstrap，請一律在 Flyway 之後手動執行 SQL：

1. 先確認 Flyway migration 已完成；若依賴應用程式建立內部 admin，請至少讓後端成功啟動過一次。
2. 開啟 `backend/src/main/resources/db/manual/seed_manual_baseline.sql`。
3. （可選）依本機需求調整 `INPUTS` 區段（商家名稱／slug／帳號與密碼雜湊）。
4. 在 MySQL 手動執行腳本。
5. 執行腳本末段的驗證查詢確認結果。

此腳本具 idempotent 設計，可重複執行；**不會**再插入 `SYSTEM_ADMIN`（避免與內部 admin 自動建立重複）。

## 授權

若有授權條款請在此補充。