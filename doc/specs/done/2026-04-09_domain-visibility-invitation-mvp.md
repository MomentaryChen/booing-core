# Domain 重新定義 MVP（商戶可見性、邀請加入、跨租戶系統代管）

Date: 2026-04-09  
Owner: PM + Architect（同回合規劃）  
Status: **Done** (archived to `doc/specs/done/`)

## Closure Handling

- `pm-agent` is the default owner for closure handling after this spec is moved to `doc/specs/done/`.
- `pm-agent` is responsible for keeping archive references correct when related active specs are updated.
- If this topic is reopened, `pm-agent` should create a new active spec under `doc/specs/` and link it back to this archived document.

## 1. 目標與決策基線

本規格為三域整合後的單一來源（`/client`、`/merchant`、`/system`），目標是將「商戶公開性、邀請加入、跨租戶系統代管」定義為一致 domain，而非分散在 controller/service 的條件判斷。

本次已鎖定決策：

- 管理者可跨 tenant 操作所有商戶，但僅限 `/system` 且必須強制審計。
- 非公開商戶對未加入使用者可見「名稱卡片」，但只能透過邀請碼/邀請連結加入後才可進一步操作。

## 2. Domain 邊界與名詞（統一定義）

### 2.1 Bounded Context

- `MerchantDirectory`：商戶清單與可見性輸出（公開、非公開名稱曝光、加入狀態）。
- `InvitationMembership`：邀請生命週期與 membership 狀態。
- `BookingAccess`：行為授權策略（誰能看、誰能做 transition、在哪個 scope）。
- `BookingCore`（既有）：`Resource + Slot + Booking` 核心，不吸收可見性業務細節。

### 2.2 主要名詞

- `MerchantVisibility`：`PUBLIC` 或 `INVITE_ONLY`。
- `PublicMerchant`：任何使用者可進入詳情與預約入口。
- `InviteOnlyMerchant`：未加入者僅可見名稱卡片，無法查看敏感內容、不可預約。
- `Invitation`：商戶發出的邀請，支援 `PENDING/ACCEPTED/REVOKED/EXPIRED`。
- `Membership`：使用者與商戶關係，支援 `ACTIVE/LEFT/SUSPENDED`。
- `BookingAccessPolicy`：統一授權入口，判斷 actor、scope、action 是否允許。

## 3. 角色與責任邊界

### 3.1 User（一般使用者）

- 可見：公開商戶 + 非公開商戶名稱卡片 + 自己已加入的非公開商戶。
- 可做：透過邀請碼加入非公開商戶、在被授權商戶進行預約。
- 不可做：直接存取未加入非公開商戶詳情、預約入口、商戶後台。

### 3.2 Merchant（商戶）

- 可見/可做：僅本商戶範圍的可見性設定、邀請管理、預約管理。
- 不可做：跨商戶讀寫、跨 tenant 代操作、繞過狀態機直接改 booking 狀態。

### 3.3 Admin（System Admin）

- 可見/可做：在 `/system` 跨 tenant 管理商戶後台資料與預約代操作。
- 限制：每次操作必須顯式帶 `targetTenant` + `targetMerchant`，並寫入審計。
- 不可做：無目標 scope 的全域寫入、繞過 transition service。

## 4. 架構落地原則（工程硬約束）

- `Resource/Slot/Booking` 核心模型不分叉；可見性只影響目錄查詢與授權層。
- 可見性與授權採策略模式，不允許長鏈 `if/else` 散落在多個服務。
- 所有 booking 狀態變更必經單一 transition service。
- 預設 tenant-scoped；只有 `/system` 支援跨 tenant，且需白名單動作 + 強制審計。

## 5. 使用者旅程（E2E）

### Journey 1：User 瀏覽公開商戶並預約

1. User 進入 `/client/merchants`。
2. 系統回傳公開商戶清單。
3. User 進入公開商戶詳情，完成預約流程。

### Journey 2：User 看見非公開商戶名稱但未加入

1. User 在 `/client/merchants` 看到 `INVITE_ONLY` 商戶名稱卡片。
2. 點入時僅顯示「需邀請碼加入」引導，不回傳敏感營運資訊。
3. 未輸入有效邀請碼前不可預約。

### Journey 3：User 透過邀請碼加入非公開商戶

1. User 進入 `/client/merchants/join` 輸入邀請碼。
2. 系統驗證 invitation，有效則建立/啟用 membership。
3. 商戶出現在 `/client/merchants/joined`，可進入預約入口。

### Journey 4：Merchant 設定可見性與邀請客戶

1. Merchant 在 `/merchant/settings/visibility` 切換 `PUBLIC/INVITE_ONLY`。
2. Merchant 在 `/merchant/invitations` 建立或撤銷邀請。
3. 系統更新 invitation/membership 狀態並反映在 client 端可見性。

### Journey 5：Admin 跨 tenant 代操作預約

1. Admin 進入 `/system/tenants/{tenantId}/merchants/{merchantId}/bookings`。
2. Admin 執行合法 transition（例如 confirm/cancel）。
3. 系統透過統一 transition service 執行，並落完整審計記錄。

### Journey 6：越權與非法狀態轉移拒絕

1. 任一角色嘗試越權或非法轉移。
2. 系統經 `BookingAccessPolicy` + transition legality 檢查拒絕請求。
3. 回傳穩定錯誤碼且不改變資料。

## 6. MVP 範圍與非目標

### 6.1 MVP 範圍

- 商戶可設定 `MerchantVisibility`（`PUBLIC`、`INVITE_ONLY`）。
- 商戶可管理邀請，使用者可透過邀請碼加入。
- 使用者可瀏覽公開商戶、查看非公開商戶名稱、查看已加入商戶。
- 系統管理者可跨 tenant 代操作商戶預約（受白名單與審計約束）。
- 三域 API 皆導入統一授權與可見性策略。

### 6.2 非目標

- 自訂可見性規則編輯器（僅保留策略擴充點）。
- 群組/多層級邀請模型。
- 會員分級權益系統與可見性聯動。
- 新增獨立 private booking 流程。

## 7. 權限矩陣（角色 x 能力）

| 能力 | User | Merchant | Admin |
|---|---|---|---|
| 查看公開商戶列表 | 允許 | 允許 | 允許 |
| 查看非公開商戶名稱卡片（未加入） | 允許 | N/A | 允許 |
| 查看未加入非公開商戶敏感詳情/預約入口 | 拒絕 | N/A | 允許（管理視角） |
| 查看已加入非公開商戶 | 允許（限本人 `ACTIVE` membership） | N/A | 允許 |
| 使用邀請碼加入非公開商戶 | 允許 | N/A | 允許（代操作時需審計） |
| 設定商戶可見性 | 拒絕 | 允許（限本商戶） | 允許 |
| 發送/撤銷邀請 | 拒絕 | 允許（限本商戶） | 允許 |
| 操作預約 transition | 允許（限被授權商戶） | 允許（限本商戶） | 允許（跨 tenant，需 scope） |
| 直接更新 booking status 欄位 | 拒絕 | 拒絕 | 拒絕 |
| 跨 tenant 商戶後台管理 | 拒絕 | 拒絕 | 允許（僅 `/system`） |

## 8. MVP API 與頁面語意

### 8.1 Client（`/client`, `/api/client/**`）

- 頁面：
  - `/client/merchants`：公開商戶 + 非公開名稱卡片混合列表。
  - `/client/merchants/joined`：已加入非公開商戶。
  - `/client/merchants/join`：邀請碼加入入口。
  - `/client/merchants/:merchantId`：商戶詳情（依 membership/visibility 決定回應）。
- API：
  - `GET /api/client/merchants`（含 `visibilityState`、`joinState`）。
  - `GET /api/client/merchants/joined`。
  - `POST /api/client/merchant-memberships/join-code`。
  - `GET /api/client/merchants/{merchantId}`（未加入 invite-only 時僅回最小可見資訊）。

### 8.2 Merchant（`/merchant`, `/api/merchant/**`）

- 頁面：
  - `/merchant/settings/visibility`。
  - `/merchant/invitations`。
  - `/merchant/bookings`。
- API：
  - `PUT /api/merchant/profile/visibility`。
  - `GET /api/merchant/invitations`。
  - `POST /api/merchant/invitations`。
  - `PATCH /api/merchant/invitations/{invitationId}`（revoke/resend/expire）。
  - `GET /api/merchant/bookings`。
  - `POST /api/merchant/bookings/{bookingId}/transitions`。

### 8.3 System（`/system`, `/api/system/**`）

- 頁面：
  - `/system/merchants`。
  - `/system/tenants/:tenantId/merchants/:merchantId/bookings`。
  - `/system/audit-logs`。
- API：
  - `GET /api/system/merchants`。
  - `GET /api/system/tenants/{tenantId}/merchants/{merchantId}/bookings`。
  - `POST /api/system/tenants/{tenantId}/bookings/{bookingId}/transitions`。
  - `GET /api/system/audit-logs`。

## 9. 安全與風險控制（Guardrails）

- IDOR 防護：任何依 `merchantId/bookingId` 的請求，都必經 `BookingAccessPolicy`。
- 非公開一致性：搜尋、列表、詳情 API 使用同一可見性策略，禁止旁路外洩。
- 系統權限隔離：`/system` 路徑獨立授權策略，不能共用 merchant/client 放寬規則。
- 審計強制：跨 tenant 或高風險操作必記錄 `actorId`、`targetTenant`、`targetMerchant`、`action`、`reason`、`result`、`timestamp`、`requestId`。
- 狀態合法性：`Invitation`、`Membership`、`Booking` 皆走 transition 入口，拒絕直接更新狀態欄位。

## 10. 驗收標準（Given / When / Then，含負向）

### AC-01 公開商戶可見且可進入

- **Given** 商戶 A 為 `PUBLIC`
- **When** 任一登入 User 進入 `/client/merchants`
- **Then** 商戶 A 出現在清單且可進入詳情與預約。

### AC-02 非公開商戶名稱可見但內容受限

- **Given** 商戶 B 為 `INVITE_ONLY`，User X 無 `ACTIVE` membership
- **When** User X 進入 `/client/merchants`
- **Then** 看得到商戶 B 名稱卡片，但進入詳情僅顯示加入引導，不回傳敏感資訊。

### AC-03 邀請碼加入後可正常存取

- **Given** User Y 具有商戶 B 的有效邀請碼
- **When** User Y 呼叫 `POST /api/client/merchant-memberships/join-code`
- **Then** membership 轉 `ACTIVE`，且商戶 B 出現在 joined 清單並可預約。

### AC-04 無效邀請碼拒絕

- **Given** User X 使用過期或撤銷邀請碼
- **When** 呼叫加入 API
- **Then** 回傳 4xx + 穩定錯誤碼，membership 不建立。

### AC-05 Merchant 僅能管理本商戶

- **Given** Tenant A 的 Merchant 嘗試操作 Tenant B 商戶預約
- **When** 呼叫 `/api/merchant/**`
- **Then** 回傳 `403/404`，不得讀取或更新任何跨 tenant 資料。

### AC-06 Admin 跨 tenant 代操作成功且可追溯

- **Given** Admin 對指定 tenant/merchant/booking 發起合法 transition
- **When** 呼叫 `/api/system/tenants/{tenantId}/bookings/{bookingId}/transitions`
- **Then** transition 成功且產生完整審計紀錄。

### AC-07 非法狀態轉移拒絕

- **Given** 任一角色對 booking 發起非法 transition
- **When** API 收到請求
- **Then** 回傳 4xx + 穩定錯誤碼，booking 狀態不變。

### AC-08 UI 隱藏不等於授權

- **Given** 某操作在 UI 不可見
- **When** 使用者直接呼叫對應 API
- **Then** 後端仍拒絕，證明授權在伺服器端生效。

## 11. 與既有 system-user 管理規格的關聯

- `System user management MVP` 聚焦平台使用者與 RBAC 管理。
- 本規格補齊商戶可見性、邀請、跨 tenant 代操作 booking 的 domain 邊界。
- 兩者共同依賴同一授權與審計原則，但關注對象不同，不互相覆蓋 API 契約。

## 12. 交付切分（MVP 第一版）

### A. Backend 任務（API、授權、狀態機、審計、測試）

- **A1. API 契約定稿（`/api/system` 為主，必要時只讀影響 `/api/merchant`、`/api/client`）**
  - 定義商戶可見性、邀請加入、跨 tenant 代操作的 request/response/error code。
  - 禁止新增平行流程 API（沿用既有 domain 與 auth context）。
  - **DoD**：DTO 與錯誤碼穩定；前後端契約對齊；契約測試通過。

- **A2. 授權與租戶邊界（deny-by-default）**
  - 以 policy/authz 層集中判斷：誰可看、誰可邀請、誰可跨 tenant 代操作。
  - 所有 tenant-scoped 查寫強制帶 `tenant_id`；跨 tenant 只允許顯式授權路徑。
  - **DoD**：非授權 caller 一律 forbidden；跨 tenant 未授權一律拒絕；負向測試通過。

- **A3. 狀態機與轉移入口（邀請/membership/預約）**
  - 建立單一 transition service，不得在 controller/repository 直接改狀態。
  - 明確合法/非法轉移與穩定錯誤碼。
  - **DoD**：僅單一入口可改狀態；非法轉移測試覆蓋；無繞過路徑。

- **A4. 審計日誌（高風險寫操作全覆蓋）**
  - 記錄 actor、target、target tenant、action、before/after 摘要、correlation id、timestamp。
  - 跨 tenant 代操作必落審計。
  - **DoD**：每個 mutation API 都有 audit 事件；欄位完整；QA 可驗證。

- **A5. 後端測試（MVP 必要覆蓋）**
  - happy path、forbidden、tenant isolation、idempotency、concurrency、illegal transition。
  - **DoD**：新增測試在 CI 綠燈；關鍵負向案例皆有 automated test。

### B. Frontend 任務（`/client`、`/merchant`、`/system`）

- **B1. `/system`：代操作與治理入口（最小可用）**
  - 提供代操作入口、當前操作身分與目標 tenant 標示、退出代操作。
  - 僅顯示 MVP 需要的控制與狀態提示（loading/forbidden/error）。
  - **DoD**：system admin 可完成主流程；越權按鈕不可操作；UI 與後端授權一致。

- **B2. `/merchant`：邀請與可見性管理**
  - 顯示邀請狀態（待處理/接受/撤銷/過期）與必要 action。
  - 顯示商戶可見性結果，不做大幅 IA 重構。
  - **DoD**：商戶端可完成邀請管理；狀態即時反映後端真實值；錯誤提示清楚。

- **B3. `/client`：可見性一致性調整（不擴流程）**
  - 調整商戶列表與詳情可見性（公開可進入、非公開名稱可見但需邀請碼）。
  - 保持既有預約主流程。
  - **DoD**：client 不出現不應可見資訊；既有預約流程無回歸。

- **B4. 三域共通 UX 最低要求**
  - loading/empty/no-result/forbidden/error 狀態齊備；zh-TW/en-US 文案一致。
  - **DoD**：QA 可逐項驗證；無阻斷式 UX 缺口。

### C. DBA 任務（migration、索引、約束）

- **C1. Migration 設計與落地（Flyway）**
  - 補齊 invitation/membership/access-audit 所需欄位（僅 MVP 最小欄位）。
  - migration 可重放、具 idempotent 特性。
  - **DoD**：本地/CI 可重複執行成功；版本順序清楚；無破壞性副作用。

- **C2. 索引策略（查詢與審計）**
  - 規劃 tenant + status + actor/target + created_at 關鍵查詢索引。
  - **DoD**：核心查詢 explain 合理；無明顯全表掃描熱點。

- **C3. 約束與一致性**
  - 唯一鍵（避免重複邀請/綁定）、必要外鍵、狀態欄位合法值約束。
  - **DoD**：重複與非法資料可被 DB 層阻擋；與 backend idempotent 語意一致。

### D. QA 任務（AC 對應測試矩陣、負向/安全）

- **D1. AC 對應測試矩陣**
  - 每條 AC 映射 API case、UI case、預期結果與證據連結。
  - **DoD**：AC 全覆蓋且可追溯，無未對應項。

- **D2. 負向與安全測試**
  - 越權訪問、跨 tenant 未授權代操作、非法狀態轉移、重放請求/idempotency。
  - **DoD**：高風險負向案例皆被拒絕且錯誤碼穩定。

- **D3. 三域回歸**
  - `/client` 既有預約主流程、`/merchant` 主要營運流程、`/system` 既有 RBAC 操作。
  - **DoD**：無 high/critical 回歸；中低風險有註記與風險接受說明。

### 實作優先順序（Phase）

- **Phase 1：邊界先行**
  - 完成 policy、tenant scope、audit 骨架、transition 單一入口與 migration/約束。
- **Phase 2：核心能力**
  - 交付可見性 + 邀請加入（backend API + `/merchant` 必要 UI + `/client` 一致性）。
- **Phase 3：高權限能力收斂**
  - 完成 `/system` 跨 tenant 代操作入口、授權細節與完整審計。

