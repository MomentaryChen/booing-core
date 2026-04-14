# 規格：使用者是否可成為 Merchant（MVP）

## 1. 問題與決策

### 1.1 問題定義

在既有 `CLIENT_USER / MERCHANT_* / SYSTEM_ADMIN` 角色與多租戶模型下，需要明確定義：是否允許「每個使用者都可以成為 merchant」，且不破壞既有 booking 與租戶隔離。

### 1.2 可行性結論

**有條件可以。**

允許同一 `user_id` 同時擁有 `CLIENT_USER` 與 `MERCHANT_*` 身分，但 merchant 能力必須是 **tenant-scoped**，不可變成全域 merchant 權限。

### 1.3 決策原則

- 身分採「同一使用者、多角色、多租戶 membership」模型。
- 既有 booking 核心模型維持 `Resource + Slot + Booking`，不複製流程。
- 狀態流轉必須走單一 transition 入口，不允許任意改 status。
- 所有 tenant-scoped 查寫必須由後端安全上下文注入 tenant 範圍。

---

## 2. 範圍（Scope）

### 2.1 In Scope（本次）

- 定義 `CLIENT_USER` 升級為 merchant（或雙身分）規則。
- 定義 tenant 建立時機與 owner 指派規則。
- 定義權限切換與 UI 入口行為。
- 定義防濫用、隔離與相容性限制。
- 提供 `auth/client/merchant/system` API 草案。
- 制定 P0/P1/P2 上線路線。

### 2.2 Out of Scope（本次不做）

- 全量 KYC/金流商戶合規流程。
- 跨租戶協作與共享資源模型。
- 進階委派、代理操作（impersonation）完整治理。

---

## 3. MVP 角色與租戶規則

### 3.1 CLIENT 轉 MERCHANT（或雙身分）

- 使用者不更換帳號，不重建 `user_id`。
- 使用「啟用 merchant 身分」流程建立 membership：
  - `membership(user_id, tenant_id, role)`
  - 初始角色為 `MERCHANT_OWNER`（建立新租戶時）
- `CLIENT_USER` 身分保留；同一使用者可同時持有 `CLIENT_USER + MERCHANT_OWNER/STAFF`。

### 3.2 Tenant 建立時機

- **首次啟用 merchant** 時：
  - 若選擇「建立新商戶」，建立新 `tenant` 並綁定 owner membership。
  - 若選擇「接受邀請加入既有商戶」，不建立 tenant，只建立對應 membership。

### 3.3 Owner 指派規則

- 建立 tenant 的發起人預設為該 tenant 首位 `MERCHANT_OWNER`。
- 每個 tenant 至少要有一位 owner（不可產生無主租戶）。
- `SYSTEM_ADMIN` 可治理 tenant 與帳號，但不自動成為 tenant owner。

### 3.4 權限切換與 UI 入口

- 單一登入態（single identity），可切換 active context：
  - `activeRole`（client / merchant）
  - `activeTenantId`（merchant 場景）
- UI 入口：
  - `/client/*`：客戶功能
  - `/merchant/*`：商戶功能（需有效 merchant membership）
  - `/admin/*`：平台治理（僅 system admin）
- 後端只信任安全上下文，不信任前端直接傳入 tenant/role 作最終授權依據。

---

## 4. 必要限制（Guardrails）

### 4.1 防濫用

- 啟用 merchant 需完成基礎驗證（至少 email；可擴充 phone）。
- 對「建立 tenant / 發送邀請 / 角色變更」施加 rate limit 與冷卻時間。
- 高風險動作必須寫入 audit log（who/when/what/before/after）。
- 可配置審核開關：
  - MVP 預設「低風險自助開通」
  - 風控模式可改為「待審核才啟用 merchant」

### 4.2 多租戶隔離

- merchant 資料表與業務資料一律 tenant-scoped（帶 `tenant_id`）。
- API 查寫必須由後端上下文注入 tenant 條件，禁止僅依前端傳值。
- 跨租戶存取一律 `403`，並記錄審計事件。
- 背景任務/批次也必須攜帶 tenant context，避免旁路洩漏。

### 4.3 既有 booking 與資料相容性

- 升級 merchant 不得改寫既有 client booking 擁有關係與歷史狀態。
- 新增 merchant 能力僅影響「新建立 tenant 內」的資源、規則、預約。
- booking 狀態流轉維持既有狀態機入口，不因 onboarding 直接改 status。

---

## 5. API 草案（MVP）

> 命名空間遵循：`/api/auth/*`、`/api/client/*`、`/api/merchant/*`、`/api/system/*`

### 5.1 Auth

- `GET /api/auth/me`
  - 回傳使用者基本資料、可用角色、可用 tenant memberships、目前 active context。
- `POST /api/auth/context/switch`
  - 請求切換 `activeRole` 與 `activeTenantId`（後端驗證 membership）。
- `POST /api/auth/merchant/enable`
  - 啟用 merchant 身分（建立新 tenant 或兌換 invitation token）。

### 5.2 Client

- `GET /api/client/profile`
- `GET /api/client/bookings`
- `POST /api/client/merchant/apply`
  - 從 client 入口發起「我要成為 merchant」流程（導向/觸發 enable）。

### 5.3 Merchant

- `GET /api/merchant/bootstrap`
  - 取得 merchant 首次引導狀態（是否有 tenant、是否 owner）。
- `POST /api/merchant/tenants`
  - 建立 tenant（僅在未綁定 merchant tenant 且通過限制時）。
- `GET /api/merchant/members`
- `POST /api/merchant/members/invitations`
- `PATCH /api/merchant/members/{membershipId}/role`
- `GET /api/merchant/resources`
- `POST /api/merchant/resources`

### 5.4 System

- `GET /api/system/tenants`
- `GET /api/system/merchant-onboarding/requests`
- `PATCH /api/system/merchant-onboarding/requests/{id}`
  - 審核啟用（核准/駁回/封鎖）
- `GET /api/system/audit-logs`

---

## 6. 驗收標準（Acceptance Criteria）

1. 同一使用者可同時以 `CLIENT_USER` 下單，並在有 membership 時進入 `/merchant/*`。
2. 首次啟用 merchant 可成功建立 tenant 並自動成為該 tenant owner。
3. 非該 tenant 成員無法查寫 `/api/merchant/*` 資料（回 `403`）。
4. 升級 merchant 後，既有 client booking 不變更擁有權、不改歷史狀態。
5. 權限切換後，後端授權判斷與資料範圍正確反映 active context。
6. 建租戶、邀請與角色調整可在 audit log 查到完整 before/after。

---

## 7. 分階段上線建議

## P0（必備，先可用）

- 啟用 merchant + 建立 tenant + owner 指派。
- active context 切換（role/tenant）。
- tenant 隔離強制化與 `403` 行為。
- 基本驗證、rate limit、audit log。

## P1（可營運）

- merchant 成員邀請與 `OWNER/STAFF` 分級權限。
- onboarding 審核流程（system 可核准/駁回）。
- 跨租戶回歸測試與壓力下濫用防護驗證。

## P2（可擴充）

- 更完整風控（風險評分、異常告警）。
- 多業種 metadata + 策略擴充能力（不改 booking 核心流程）。
- 進階治理（通知觸發器、批次 exception 管理、可觀測性）。

---

## 8. 風險與緩解

- **權限旁路風險**：前端傳 tenant/role 被誤信任。  
  **緩解**：後端以 security context + membership 二次驗證。
- **跨租戶資料洩漏**：少數查詢漏 tenant 條件。  
  **緩解**：repository/service 層強制 tenant filter，補 integration tests。
- **歷史資料破壞**：升級 merchant 觸發舊 booking 回寫。  
  **緩解**：明確禁止 onboarding 觸碰既有 booking，僅新增 tenant/membership。

---

## 9. 交接與進度清單（本輪）

| 項目 | Owner Agent | 狀態 | 完成證據 |
|---|---|---|---|
| 本規格與架構邊界確認 | pm-agent + architect-agent | done | 本文件 v1 + 架構一致性檢核完成 |
| API 與授權模型落地（BE） | backend-engineer-agent | todo | PR + API test |
| 角色切換與入口 UX（FE） | frontend-engineer-agent + uiux-agent | todo | PR + 截圖 + i18n 文案 |
| 風控/限流/審計治理（System） | system-admin-agent + backend-engineer-agent | todo | PR + 稽核樣本 |
| 交付前風險審查 | reviewer-agent | todo | Findings 報告（No critical findings） |

---

## 10. 受影響產品面

- `/client`：新增「成為 merchant」入口與導流。
- `/merchant`：新增首次啟用與切換後首頁流程。
- `/system`：新增/擴充 merchant onboarding 治理與審計檢視。

