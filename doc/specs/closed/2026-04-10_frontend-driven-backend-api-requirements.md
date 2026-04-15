# booking-core 前端導向後端 API 需求清單（PM）

> Closeout Note (2026-04-14): 本規格主要交付範圍（建立預約、重疊防護、查詢分頁、取消預約、前端操作 UI）已完成並驗證通過；檔案將歸檔至 `doc/specs/closed/2026-04-10_frontend-driven-backend-api-requirements.md`。剩餘 UI 文案細化將以新規格追蹤，不再阻擋本案結案。

## 1. 問題與目標

- 目前 `frontend` 已有可見頁面與流程（`auth`、`/client`、`/merchant`、`/admin`），但多數資料仍為 mock，尚未與後端正式契約對接。
- 本文件以「現有前端設計與互動」為準，整理可直接交付後端的 API 需求清單，避免過度理想化擴充。
- 命名空間遵循專案慣例：`/api/auth`、`/api/client`、`/api/merchant`、`/api/system`。
- 注意：前端 UI 路由為 `/admin/*`，後端 API 一律走 `/api/system/*`（不新增 `/api/admin/*`）。

## 2. 範圍（以現有前端流程為準）

- Auth：登入、註冊、忘記密碼、登出、目前使用者資訊。
- Client：首頁/搜尋、資源詳情與可用時段、建立預約、我的預約、個人資料。
- Merchant：儀表板、資源管理、營業時間與例外、預約管理、商家設定。
- System（對應前端 `/admin`）：儀表板、租戶管理、使用者管理、角色權限、審計日誌。

## 3. 前端現況摘要（對接可行性）

- `frontend/src/apps/*/pages` 已具備完整頁面結構與主要互動元件（搜尋、篩選、列表、表單、狀態切換）。
- 目前幾乎無真實 API 呼叫，需求可直接由頁面欄位反推契約。
- Auth 現在使用 demo token 流程；正式化時需改為 JWT + refresh/session 模式。
- 高風險契約點（先標記，避免後續返工）：
  - `/admin` UI 與 `/api/system` API 命名差異。
  - booking 狀態字典需先固定（建議最小集合：`PENDING/CONFIRMED/CANCELLED/COMPLETED`）。
  - 可用時段採查詢計算，不依賴全年預展開 slot 表。

## 4. Domain API 清單（可直接貼後端規格）

狀態標記說明：
- `可直接對接`：前端頁面欄位與行為已完整，可直接接 API。
- `待後端新增`：前端有需求，但後端尚需提供新端點或能力。
- `可能需前端調整`：目前前端流程/欄位與建議契約有落差。

---

### 4.1 Auth Domain（`/api/auth`）

#### A1. 使用者登入

- 前端流程：`/`、`/login`（`LoginPage`）。
- 建議 API：`POST /api/auth/login`
- Method：`POST`
- Request
  - body: `{ email: string, password: string }`
- Response（成功）
  - `200`: `{ accessToken: string, refreshToken?: string, user: { id, email, name, role, tenantId?, permissions: string[] } }`
- 常見錯誤
  - `400`: 欄位缺漏/格式錯誤
  - `401`: 帳密錯誤
  - `423`: 帳號停用/鎖定
- 權限/角色：公開端點（匿名可呼叫）
- 狀態：`待後端新增`
- 備註：前端目前是 demo 帳號比對與 mock token，正式接入需改寫 `useAuth`。

#### A2. 註冊

- 前端流程：`/register`（`RegisterPage`）
- 建議 API：`POST /api/auth/register`
- Method：`POST`
- Request
  - body: `{ name: string, email: string, password: string, role?: "CLIENT" }`
- Response（成功）
  - `201`: `{ userId: string, email: string, requiresEmailVerification: boolean }`
- 常見錯誤
  - `400`: 密碼規則不符、欄位錯誤
  - `409`: email 已存在
- 權限/角色：公開端點
- 狀態：`待後端新增`
- 備註：建議先限制註冊為 `CLIENT`，避免前端誤開放高權限角色。

#### A3. 忘記密碼（寄送重設信）

- 前端流程：`/forgot-password`（`ForgotPasswordPage`）
- 建議 API：`POST /api/auth/forgot-password`
- Method：`POST`
- Request
  - body: `{ email: string }`
- Response（成功）
  - `202`: `{ accepted: true }`
- 常見錯誤
  - `400`: email 格式錯誤
  - 其他情境回 `202`（避免帳號枚舉）
- 權限/角色：公開端點
- 狀態：`待後端新增`

#### A4. 取得目前登入者資訊

- 前端流程：所有 ProtectedRoute/Layout 需要 user profile + role + permissions。
- 建議 API：`GET /api/auth/me`
- Method：`GET`
- Request
  - header: `Authorization: Bearer <token>`
- Response（成功）
  - `200`: `{ id, email, name, role, tenantId?, permissions: string[] }`
- 常見錯誤
  - `401`: token 無效/過期
  - `403`: 角色不符
- 權限/角色：已登入使用者
- 狀態：`待後端新增`

#### A5. 登出（可選：refresh token 撤銷）

- 建議 API：`POST /api/auth/logout`
- Method：`POST`
- Request
  - body: `{ refreshToken?: string }`
- Response（成功）
  - `200`: `{ success: true }`
- 常見錯誤
  - `401`: 未登入或 token 無效
- 權限/角色：已登入使用者
- 狀態：`待後端新增`

---

### 4.2 Client Domain（`/api/client`）

#### C1. 首頁精選與分類

- 前端流程：`/client/`（`HomePage`，精選服務、分類卡）
- 建議 API
  - `GET /api/client/resources/featured`
  - `GET /api/client/categories`
- Method：`GET`
- Request
  - query（featured）: `limit?`
  - query（categories）: `includeCounts=true|false`
- Response（成功）
  - featured: `[{ id, name, category, price, durationMinutes, rating, imageUrl, merchantName }]`
  - categories: `[{ key, label, count }]`
- 常見錯誤
  - `400`: query 無效
  - `500`: 系統錯誤
- 權限/角色：`CLIENT`
- 狀態：`待後端新增`

#### C2. 搜尋資源

- 前端流程：`/client/search`（搜尋字串、分類、排序）
- 建議 API：`GET /api/client/resources`
- Method：`GET`
- Request
  - query: `q?`, `category?`, `sort?` (`relevance|priceAsc|priceDesc|rating`), `page?`, `size?`
- Response（成功）
  - `200`: `{ items: [{ id, name, category, price, durationMinutes, rating, imageUrl, merchantName }], page, size, total }`
- 常見錯誤
  - `400`: sort/category 不合法
- 權限/角色：`CLIENT`
- 狀態：`可直接對接`

#### C3. 資源詳情

- 前端流程：`/client/booking/:resourceId` 上半部資訊
- 建議 API：`GET /api/client/resources/{resourceId}`
- Method：`GET`
- Request
  - path: `resourceId`
- Response（成功）
  - `200`: `{ id, name, description, category, price, durationMinutes, rating, merchant: { id, name }, imageUrl }`
- 常見錯誤
  - `404`: 資源不存在或無可見權限
- 權限/角色：`CLIENT`
- 狀態：`可直接對接`

#### C4. 查詢可預約時段（slot availability）

- 前端流程：`/client/booking/:resourceId` 選日期後載入時段
- 建議 API：`GET /api/client/resources/{resourceId}/availability`
- Method：`GET`
- Request
  - path: `resourceId`
  - query: `date`（YYYY-MM-DD）, `timezone?`
- Response（成功）
  - `200`: `{ date: string, slots: [{ startAt, endAt, isAvailable, capacityRemaining? }] }`
- 常見錯誤
  - `400`: date 格式錯誤
  - `404`: resource 不存在
- 權限/角色：`CLIENT`
- 狀態：`待後端新增`
- 備註：依規則引擎即時計算（營業規則 + exceptions + 已預約占用）。

#### C5. 建立預約

- 前端流程：`/client/booking/:resourceId` 送出預約
- 建議 API：`POST /api/client/bookings`
- Method：`POST`
- Request
  - body: `{ resourceId: string, startAt: string(ISO), notes?: string }`
- Response（成功）
  - `201`: `{ id, bookingNo, status, resourceId, resourceName, merchantName, startAt, endAt, price, notes?, createdAt }`
- 常見錯誤
  - `400`: 欄位不完整
  - `409`: 時段已被預約 / 非法狀態轉移
  - `422`: 預約規則驗證不通過
- 權限/角色：`CLIENT`
- 狀態：`待後端新增`

#### C6. 我的預約列表

- 前端流程：`/client/my-bookings`（tabs：upcoming/past/cancelled）
- 建議 API：`GET /api/client/bookings`
- Method：`GET`
- Request
  - query: `status?`（可多值）, `range?`（upcoming|past）, `page?`, `size?`
- Response（成功）
  - `200`: `{ items: [{ id, serviceName, providerName, date, time, durationMinutes, status, price }], page, size, total }`
- 常見錯誤
  - `400`: 篩選參數不合法
- 權限/角色：`CLIENT`
- 狀態：`可直接對接`

#### C7. 取消預約（client）

- 前端流程：`/client/my-bookings` upcoming 卡片的 cancel action
- 建議 API：`PATCH /api/client/bookings/{bookingId}/cancel`
- Method：`PATCH`
- Request
  - path: `bookingId`
  - body: `{ reason?: string }`
- Response（成功）
  - `200`: `{ id, status: "CANCELLED", cancelledAt }`
- 常見錯誤
  - `403`: 非本人預約
  - `409`: 目前狀態不可取消
  - `404`: booking 不存在
- 權限/角色：`CLIENT`
- 狀態：`待後端新增`

#### C8. 改期預約（client）

- 前端流程：`/client/my-bookings` upcoming 卡片的 reschedule action
- 建議 API：`PATCH /api/client/bookings/{bookingId}/reschedule`
- Method：`PATCH`
- Request
  - body: `{ newStartAt: string(ISO) }`
- Response（成功）
  - `200`: `{ id, status, startAt, endAt, updatedAt }`
- 常見錯誤
  - `409`: 新時段衝突 / 狀態不可改期
- 權限/角色：`CLIENT`
- 狀態：`待後端新增`

#### C9. 個人資料與偏好

- 前端流程：`/client/profile`（personal/preferences/notifications/security）
- 建議 API
  - `GET /api/client/profile`
  - `PATCH /api/client/profile`
  - `PATCH /api/client/profile/password`
- Method：`GET`、`PATCH`
- Request（PATCH profile）
  - body: `{ name?, phone?, language?, timezone?, currency?, notificationPrefs?: { email: boolean, sms: boolean } }`
- Request（PATCH password）
  - body: `{ currentPassword: string, newPassword: string }`
- Response（成功）
  - `200`: 更新後 profile 或 `{ success: true }`
- 常見錯誤
  - `400`: 密碼規則不符/欄位錯誤
  - `401`: currentPassword 錯誤
- 權限/角色：`CLIENT`
- 狀態：`待後端新增`
- 備註：avatar 目前僅 UI 按鈕，若要上線需補檔案上傳 API（可列 P2）。

---

### 4.3 Merchant Domain（`/api/merchant`）

#### M1. Merchant 儀表板摘要

- 前端流程：`/merchant/`（統計卡 + upcoming bookings）
- 建議 API：`GET /api/merchant/dashboard/summary`
- Method：`GET`
- Request
  - query: `from?`, `to?`（可選）
- Response（成功）
  - `200`: `{ stats: { totalBookings, todayBookings, revenue, avgRating }, upcomingBookings: [{ id, customerName, resourceName, startAt, status }] }`
- 常見錯誤
  - `403`: 非商家角色
- 權限/角色：`MERCHANT`
- 狀態：`待後端新增`

#### M2. 資源列表與搜尋

- 前端流程：`/merchant/resources`
- 建議 API：`GET /api/merchant/resources`
- Method：`GET`
- Request
  - query: `q?`, `status?`, `page?`, `size?`
- Response（成功）
  - `200`: `{ items: [{ id, name, category, price, durationMinutes, status }], page, size, total }`
- 常見錯誤
  - `400`: 篩選參數錯誤
- 權限/角色：`MERCHANT`
- 狀態：`可直接對接`

#### M3. 建立/編輯/刪除資源

- 前端流程：`/merchant/resources`（create/edit/delete action）
- 建議 API
  - `POST /api/merchant/resources`
  - `PATCH /api/merchant/resources/{resourceId}`
  - `DELETE /api/merchant/resources/{resourceId}`
- Method：`POST`、`PATCH`、`DELETE`
- Request（POST/PATCH）
  - body: `{ name, category, price, durationMinutes, status, description?, metadata? }`
- Response（成功）
  - `201/200`: `{ id, ...resource }`
  - `204`（delete）
- 常見錯誤
  - `400`: 驗證失敗
  - `404`: resource 不存在
  - `409`: 有既有預約不可刪除（可改為 inactive）
- 權限/角色：`MERCHANT`
- 狀態：`待後端新增`

#### M4. 營業時間（規則）與例外（exceptions）

- 前端流程：`/merchant/schedule`（weekly working hours + exceptions）
- 建議 API
  - `GET /api/merchant/schedule/rules`
  - `PUT /api/merchant/schedule/rules`
  - `GET /api/merchant/schedule/exceptions`
  - `POST /api/merchant/schedule/exceptions`
  - `DELETE /api/merchant/schedule/exceptions/{exceptionId}`
- Method：`GET`、`PUT`、`POST`、`DELETE`
- Request（rules）
  - body: `{ weeklyRules: [{ dayOfWeek, enabled, startTime?, endTime?, breakStart?, breakEnd? }] }`
- Request（exceptions）
  - body: `{ date, type: "closed"|"modified", reason?, startTime?, endTime? }`
- Response（成功）
  - 規則/例外物件清單或更新結果
- 常見錯誤
  - `400`: 時間區間不合法
  - `409`: 例外衝突
- 權限/角色：`MERCHANT`
- 狀態：`待後端新增`

#### M5. 預約列表與狀態操作

- 前端流程：`/merchant/bookings`（搜尋、tab 篩選、confirm/reject/complete）
- 建議 API
  - `GET /api/merchant/bookings`
  - `PATCH /api/merchant/bookings/{bookingId}/confirm`
  - `PATCH /api/merchant/bookings/{bookingId}/reject`
  - `PATCH /api/merchant/bookings/{bookingId}/complete`
- Method：`GET`、`PATCH`
- Request（GET）
  - query: `q?`（客戶名）, `status?`, `dateFrom?`, `dateTo?`, `page?`, `size?`
- Request（PATCH）
  - body: `{ reason?: string }`（reject 可用）
- Response（成功）
  - `200`: 更新後 booking（含 `status`）
- 常見錯誤
  - `403`: 非本租戶 booking
  - `409`: 非法狀態轉移
  - `404`: booking 不存在
- 權限/角色：`MERCHANT`
- 狀態：`待後端新增`

#### M6. 商家設定（business/payment/notifications/team）

- 前端流程：`/merchant/settings`
- 建議 API
  - `GET /api/merchant/settings`
  - `PATCH /api/merchant/settings`
- Method：`GET`、`PATCH`
- Request（PATCH）
  - body: `{ businessName?, description?, address?, phone?, email?, website?, notificationPrefs?, paymentProviders? }`
- Response（成功）
  - `200`: 更新後 settings
- 常見錯誤
  - `400`: 欄位格式錯誤
- 權限/角色：`MERCHANT`
- 狀態：`待後端新增`
- 備註：team management UI 存在但功能未啟用，可先不做 CRUD（P2）。

---

### 4.4 System Domain（`/api/system`，對應前端 `/admin`）

#### S1. System 儀表板摘要

- 前端流程：`/admin/`（統計、圖表、recent activity）
- 建議 API：`GET /api/system/dashboard/summary`
- Method：`GET`
- Request
  - query: `from?`, `to?`
- Response（成功）
  - `200`: `{ stats: { totalUsers, totalTenants, totalBookings, systemHealth }, chartData?: {...}, recentActivities: [...] }`
- 常見錯誤
  - `403`: 非 `SYSTEM_ADMIN`
- 權限/角色：`SYSTEM_ADMIN`
- 狀態：`待後端新增`

#### S2. 租戶管理

- 前端流程：`/admin/tenants`（search/list/create/edit/suspend/activate）
- 建議 API
  - `GET /api/system/tenants`
  - `POST /api/system/tenants`
  - `PATCH /api/system/tenants/{tenantId}`
  - `PATCH /api/system/tenants/{tenantId}/status`
- Method：`GET`、`POST`、`PATCH`
- Request（GET）
  - query: `q?`, `status?`, `plan?`, `page?`, `size?`
- Request（status）
  - body: `{ status: "ACTIVE"|"SUSPENDED", reason?: string }`
- Response（成功）
  - list + tenant detail
- 常見錯誤
  - `409`: 重複租戶代碼/名稱
  - `404`: tenant 不存在
- 權限/角色：`SYSTEM_ADMIN`
- 狀態：`待後端新增`

#### S3. 系統使用者管理

- 前端流程：`/admin/users`（search/list/add/edit/reset password/suspend/activate）
- 建議 API
  - `GET /api/system/users`
  - `POST /api/system/users`
  - `PATCH /api/system/users/{userId}`
  - `PATCH /api/system/users/{userId}/status`
  - `POST /api/system/users/{userId}/reset-password`
- Method：`GET`、`POST`、`PATCH`
- Request（GET）
  - query: `q?`, `role?`, `status?`, `tenantId?`, `page?`, `size?`
- Request（POST）
  - body: `{ name, email, role, tenantId?, initialPassword? }`
- Response（成功）
  - user list/detail 或 `{ success: true }`
- 常見錯誤
  - `409`: email 重複
  - `400`: role/tenant 組合不合法（例如跨租戶綁定）
  - `404`: user 不存在
- 權限/角色：`SYSTEM_ADMIN`
- 狀態：`待後端新增`

#### S4. 角色與權限

- 前端流程：`/admin/roles`（角色清單、權限矩陣）
- 建議 API
  - `GET /api/system/roles`
  - `POST /api/system/roles`
  - `PATCH /api/system/roles/{roleId}`
- Method：`GET`、`POST`、`PATCH`
- Request（POST/PATCH）
  - body: `{ name, code, permissions: string[] }`
- Response（成功）
  - role detail/list
- 常見錯誤
  - `409`: role code 重複
  - `400`: permission key 不合法
- 權限/角色：`SYSTEM_ADMIN`
- 狀態：`待後端新增`

#### S5. 審計日誌查詢與匯出

- 前端流程：`/admin/audit-logs`（search、level filter、export）
- 建議 API
  - `GET /api/system/audit-logs`
  - `GET /api/system/audit-logs/export`
- Method：`GET`
- Request
  - query: `q?`, `level?`（info|warning|error）, `action?`, `from?`, `to?`, `page?`, `size?`
  - export query 同上（回 csv/xlsx）
- Response（成功）
  - list: `{ items: [{ id, timestamp, user, action, resource, details, level, correlationId? }], page, size, total }`
  - export: 檔案串流
- 常見錯誤
  - `400`: 篩選參數不合法
  - `403`: 非 `SYSTEM_ADMIN`
- 權限/角色：`SYSTEM_ADMIN`
- 狀態：`待後端新增`

## 5. 可能需前端調整（契約落差）

- `/admin` 對接 `/api/system`：前端若預設 `/api/admin` 需統一改名（建議先在 API client 層做 namespace mapping）。
- booking 狀態字典：前端 tabs/status badge 需與後端 enum 完全一致（建議集中常數檔）。
- booking 建立流程目前以 `resourceId + date + time` UI 表現，後端建議以 `startAt(ISO)` 作為單一真值；前端需做時區與組裝。
- Profile/Settings 多 tab 欄位可能分批上線，前端需接受部分欄位尚未可寫（disabled 或 feature flag）。
- roles/permissions 前端目前為示意鍵值，需改為後端回傳 permission keys 驅動。

## 6. 優先級與建議開發順序（後端分批交付）

### Wave 1（P0，先打通可登入 + 可預約最小閉環）

- Auth：A1/A4/A5（login/me/logout）
- Client：C2/C3/C4/C5/C6（搜尋、資源詳情、可用時段、建立預約、我的預約）
- Merchant：M2/M5（資源列表、預約列表與狀態操作）
- 系統需求：最小 RBAC + tenant 隔離 + booking 狀態機（非法轉移要回 409）

交付結果：可完成「client 下單 + merchant 處理」主流程。

### Wave 2（P1，補齊營運必要管理）

- Auth：A3（forgot-password）
- Client：C7/C8/C9（取消/改期/個人資料）
- Merchant：M1/M3/M4/M6（儀表板、資源 CRUD、排程規則/例外、商家設定）
- System：S5（audit logs 查詢）

交付結果：流程可營運，且有基本稽核可追。

### Wave 3（P2，平台治理與進階管理）

- Auth：A2（register，依商務策略可延後）
- System：S1/S2/S3/S4（完整 system admin 套件）
- 其他：avatar upload、team management、進階匯出/圖表查詢

交付結果：平台治理能力完整化。

## 7. 驗收準則（本需求清單的 Done 條件）

- 每個 P0 API 均有 OpenAPI 契約與範例（成功 + 常見錯誤）。**實作對照**：見 `doc/api/booking-core-p0.openapi.yaml`（規範性契約 + 錯誤範例）；執行時可由 `GET /v3/api-docs`（非 prod profile）取得與程式同步的 schema。
- 前端可在不改版面前提下完成 P0 對接（僅替換 mock data 與 action handlers）。
- `client/merchant/system` API 命名空間與角色權限一致，且跨租戶存取被拒絕。
- booking 狀態轉移有單一入口，非法轉移可重現且回傳一致錯誤碼。

## 8. Handoff（給後端/相關角色）

- `backend-engineer-agent`：
  - 依本文件 Wave 1 -> Wave 2 -> Wave 3 實作 API 與 migration。
  - 明確實作 booking state machine 與 tenant 隔離守衛。
- `architect-agent`（已同輪校準）：
  - 審核 Resource/Slot 與狀態機落地是否偏離規則引擎原則。
- `frontend-engineer-agent`：
  - 先接 P0 API，統一錯誤處理（401/403/404/409）。
- `qa-agent`（最小範圍）：
  - 驗證 P0 主流程 + 直接影響路徑 smoke，不擴全量回歸。
- `reviewer-agent`：
  - 每次 commit 後執行風險審查（功能正確性、回歸、安全、租戶隔離）。

## 9. 進度檢查清單（PM 追蹤）

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| 前端頁面與互動需求盤點完成 | pm-agent | done | 本文件 §2~§4 |
| PM+Architect 同輪邊界校準完成 | pm-agent + architect-agent | done | 本文件 §3、§5（命名與架構約束） |
| Wave 0 前置盤點（狀態機/租戶/策略註冊）封包發佈 | pm-agent | done | 本文件 §10.1 |
| P0 API 契約草案（OpenAPI） | backend-engineer-agent | done | `doc/api/booking-core-p0.openapi.yaml`；dev `springdoc` + `/v3/api-docs`（prod 關閉 UI） |
| P0 API 實作與單元測試 | backend-engineer-agent | todo | 測試報告、CI log |
| 前端 P0 對接與 smoke 驗證 | frontend-engineer-agent | done | Round 7：`useAuth` 真實 login/logout、`SearchPage`/`BookingPage` API、`BookingsPage` merchant API；`pnpm build` |
| Commit 後 reviewer gate（本 spec 專屬） | reviewer-agent | in_progress | 本文件 §10.3；待首輪 findings |

## 10. Round 2 Implementation Preparation Packets

### 10.1 Wave 0 + P0 執行封包（backend-engineer-agent）

範圍原則：
- API 僅允許：`/api/auth`、`/api/client`、`/api/merchant`、`/api/system`。
- 禁止新增：`/api/admin`。
- 所有資料/Schema/Migration/Index/Rollback 由 `backend-engineer-agent` 全權負責。

Wave 0 前置交付（先完成才進 P0 coding）：
- 狀態機轉移表（Booking）：
  - `PENDING -> CONFIRMED | CANCELLED`
  - `CONFIRMED -> COMPLETED | CANCELLED`
  - `COMPLETED`、`CANCELLED` 為終態，不可再轉移
  - `PATCH /api/merchant/bookings/{bookingId}/reject` 語義固定為「轉移到 `CANCELLED`」（不引入 `REJECTED` 新狀態）
  - 非法轉移統一回 `409` + 穩定錯誤碼
- 租戶強制隔離：
  - `/api/client`、`/api/merchant` 資料查寫皆必帶 tenant scope
  - `/api/system` 採 system scope；若存取 tenant-scoped 資料，必須顯式帶 tenant context 並記錄審計
  - 跨租戶存取統一拒絕 `404`（避免資源枚舉），OpenAPI 與測試必須一致
- 策略註冊表（Strategy Registry）：
  - 建立 `ResourceType -> BookingValidationStrategy` 對應
  - 無對應策略時 fail-fast（`422` 或配置錯誤碼），禁止 fallback 到硬編碼 if/else

P0 實作交付（本輪優先）：
- Auth：`POST /api/auth/login`、`GET /api/auth/me`、`POST /api/auth/logout`
- Client：`GET /api/client/resources`、`GET /api/client/resources/{resourceId}`、`GET /api/client/resources/{resourceId}/availability`、`POST /api/client/bookings`、`GET /api/client/bookings`
- Merchant：`GET /api/merchant/resources`、`GET /api/merchant/bookings`、`PATCH /api/merchant/bookings/{bookingId}/confirm|reject|complete`

Definition of Done（backend）：
- OpenAPI：P0 端點皆有成功/錯誤範例（至少 `400/401/403/404/409` 相關）
- 測試：狀態機合法/非法轉移、租戶隔離、RBAC、關鍵 happy path 皆有自動化測試
- Migration：含 up/down（rollback）與必要 index，並附 migration 說明
- 錯誤一致性：同類錯誤回應結構與錯誤碼一致，供前端可機械化處理

### 10.2 Frontend Integration 封包（frontend-engineer-agent）

先接線順序（wire first）：
- Step 1：Auth 骨幹（`login -> me -> logout`），先替換 `useAuth` mock 流程
- Step 2：Client 主閉環（resources list/detail -> availability -> create booking -> my bookings）
- Step 3：Merchant 操作（bookings list + confirm/reject/complete）

Fallback / 失敗處理（先實作統一層）：
- `401`：清理本地 session 並導向登入頁（保留 return path）
- `403`：顯示無權限提示頁或 toast，不重試
- `404`：資源不存在或不可見，導向列表並提示
- `409`：顯示衝突訊息（時段被占用/非法狀態），觸發列表或可用時段刷新
- `5xx / timeout`：顯示可重試 UI（retry button + skeleton/empty state）

整合交付物：
- API client namespace mapping 明確使用 `/api/system`（對應前端 `/admin` 路由）
- status dictionary 集中常數化，與後端 enum 一致
- smoke 證據：client 下單與 merchant 狀態處理最小閉環截圖或測試紀錄

### 10.3 Reviewer Gate Checklist（reviewer-agent，本 spec 專屬）

- Namespace 合規：僅 `/api/auth|client|merchant|system`，無 `/api/admin`
- 狀態機完整性：只允許定義轉移，非法轉移必回 `409` 且無 side effect
- 多租戶隔離：跨租戶查寫不可成功（含 list/detail/action）
- 策略註冊：無硬編碼長 if/else；新資源型別可透過 registry 擴充
- 狀態變更唯一入口：不得直接更新 booking `status` 欄位，必須走 state machine transition service
- Migration 風險：升降版腳本、index、rollback 路徑可執行且順序正確
- 前後端錯誤契約：`401/403/404/409` 是否可被前端 fallback 規則直接消化
- 測試覆蓋：至少涵蓋 P0 happy path + tenancy + illegal transition + RBAC

## 11. Round 2 ownerAssignments（更新）

| Workstream | Owner Agent | Status | Deliverable |
|---|---|---|---|
| Wave 0 前置（狀態機/租戶/策略） | backend-engineer-agent | in_progress | 設計 + OpenAPI/錯誤碼草案 + 測試計畫 |
| P0 API 契約與實作 | backend-engineer-agent | in_progress | API + migration + tests |
| P0 前端對接與 fallback | frontend-engineer-agent | done | Round 7 §19：`pnpm build`；401/錯誤以頁面內訊息呈現（統一 toast 可後續補） |
| 風險審查與品質閘門 | reviewer-agent | in_progress | Findings（每 commit/里程碑） |
| 規格單一真值維護 | pm-agent | done | 本文件 Round 2 更新 |

## 12. Acceptance Tests（Given / When / Then）

1) Client 建立預約成功：
- Given 有效 `CLIENT` token、可用 `resourceId` 與可預約 `startAt`
- When 呼叫 `POST /api/client/bookings`
- Then 回 `201` 且回傳 booking `status=PENDING`（或系統定義初始狀態），可在 `GET /api/client/bookings` 查到

2) Client 建立預約衝突：
- Given 同一時段已被他人預約
- When 呼叫 `POST /api/client/bookings`
- Then 回 `409` 且錯誤碼可讓前端觸發「刷新 availability」fallback

3) Merchant 非法狀態轉移：
- Given booking 已為 `CANCELLED`
- When merchant 呼叫 `PATCH /api/merchant/bookings/{id}/complete`
- Then 回 `409`，且 booking 狀態維持 `CANCELLED`（無 side effect）

4) 跨租戶阻擋：
- Given `MERCHANT` 使用者 A 嘗試操作 tenant B 的 booking
- When 呼叫 `PATCH /api/merchant/bookings/{id}/confirm`
- Then 回 `404`，且資料不變更

5) System 命名空間一致性：
- Given 前端 `/admin` 頁面觸發後端管理 API
- When 發送請求
- Then 僅命中 `/api/system/*`，不存在 `/api/admin/*` 端點依賴

6) Strategy fail-fast：
- Given `resourceId` 對應到未註冊 `BookingValidationStrategy` 的 `ResourceType`
- When 呼叫 `POST /api/client/bookings`
- Then 回 `422`（或規格定義錯誤碼），且不建立 booking（無 side effect）

## 13. Round 4 最小執行切片（`POST /api/client/bookings`）

### 13.1 In-scope 行為與端點契約

- 端點：`POST /api/client/bookings`
- 目標：只打通「Client 建立預約」最小閉環，不含付款、優惠、通知、改期。
- 身分：僅 `CLIENT` 可呼叫（需有效 bearer token）。
- Request（最小）：
  - body: `{ resourceId: string, startAt: string(ISO-8601), notes?: string(max 500) }`
- Response（成功）：
  - `201`: `{ id, bookingNo, status, resourceId, startAt, endAt, tenantId, createdAt }`
  - 初始狀態固定 `status=PENDING`。
- Out of scope（本輪不做）：
  - waitlist、超額候補、批次建立、跨資源聯單、支付鎖單。

### 13.2 必要驗證與錯誤碼（含 409/422）

- `400 BAD_REQUEST`
  - 欄位缺漏、`startAt` 非 ISO、過去時間、`notes` 長度超限。
- `401 UNAUTHORIZED`
  - token 無效或過期。
- `403 FORBIDDEN`
  - 非 `CLIENT` 角色。
- `404 NOT_FOUND`
  - `resourceId` 不存在或對該 tenant 不可見（避免資源枚舉）。
- `409 CONFLICT`（業務衝突）
  - 同一 `resourceId + startAt` 已被占用（含鎖定/已建立 booking）。
  - 若衝突發生，資料不得有 side effect（不得建立半成品 booking）。
- `422 UNPROCESSABLE_ENTITY`（規則驗證失敗）
  - `BookingValidationStrategy` 判定不通過（例如資源類型規則不符）。
  - `ResourceType` 未註冊 strategy（fail-fast），不可 fallback 硬編碼流程。

### 13.3 必要狀態機與租戶隔離約束

- 狀態機：
  - 建立成功唯一入口為 `createBooking()`；建立後僅允許進入 `PENDING`。
  - 不允許在 create path 直接寫入其他狀態（如 `CONFIRMED/CANCELLED`）。
- 多租戶隔離：
  - 從 auth context 解析 `tenantId`，查 `resource` 與寫 `booking` 均必帶 tenant scope。
  - 跨租戶資源存取一律 `404`（不回 `403`，避免探測）。
  - 寫入 `booking.tenantId` 必須等於 caller tenant，禁止由 request body 指定 tenant。

### 13.4 本輪 must-have 測試

- Happy path：有效 `CLIENT` + 可用時段 -> `201`，且 `status=PENDING`。
- 衝突測試：同時段已占用 -> `409`，且 booking 筆數不增加（無 side effect）。
- 規則拒絕：strategy 驗證失敗 -> `422`，且不建立 booking。
- 未註冊 strategy：`ResourceType` 無對應策略 -> `422` fail-fast。
- 租戶隔離：tenant A 對 tenant B 資源下單 -> `404`。
- RBAC：`MERCHANT/SYSTEM_ADMIN` 呼叫 -> `403`；未登入 -> `401`。

### 13.5 Round Exit Criteria（本輪退出條件）

- API 契約固定：`POST /api/client/bookings` 的 request/response 與 `400/401/403/404/409/422` 錯誤碼已寫入 OpenAPI。
- 自動化測試：13.4 六類測試全綠（至少 1 個整合測試覆蓋衝突與 side effect）。
- 程式結構：create booking 走單一 service 入口 + strategy registry，無長串業務硬編碼。
- 租戶保證：跨租戶案例可重現 `404`，且 DB 無異常寫入。

## 14. Round 4 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 4：`POST /api/client/bookings` 最小執行切片定稿 | pm-agent | done | 本文件 §13（契約/驗證/狀態機/測試/exit criteria） |

## 15. Round 5（最後一輪）：`GET /api/client/bookings` + 前端我的預約

- 後端：`GET /api/client/bookings?tab=upcoming|past|cancelled&page&size`；僅 `CLIENT` / `CLIENT_USER`；預約需關聯 `platform_user_id`（migration `V12`）。
- 建立預約時寫入目前登入之 `PlatformUser`，列表僅回傳本人預約。
- 前端：`MyBookingsPage` 改為呼叫 `/api/client/bookings`（`clientBookingApi.ts`），取代 mock。
- 測試：`ClientBookingListApiTest`（401/403/建立後列表）。

## 16. Round 5 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 5：我的預約 API + FE 對接 | backend + frontend | done | `ClientBookingListApiTest`、MyBookingsPage |

## 17. Round 6：P0 OpenAPI（契約 + 執行時文件）

- **規範性契約**：`doc/api/booking-core-p0.openapi.yaml` — P0 路徑以**目前後端實作**為準（含 login 使用 `username`、merchant 以 `PUT .../bookings/{id}/status` 做狀態轉移等與 §4 表格之差異說明）；每個收錄端點附成功/常見錯誤範例（400/401/403/404/409/422 等適用處）。
- **執行時**：`springdoc-openapi-starter-webmvc-ui`（2.6.0，對齊 Spring Boot 3.3.x）、`OpenApiConfig` JWT bearer scheme 說明、`@Tag` 分組；**prod** `application-prod.yml` 關閉 `api-docs` / `swagger-ui`。
- **測試**：`mvn test` 全綠（springdoc 不影響現有測試）。

## 18. Round 6 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 6：P0 OpenAPI 交付 | backend-engineer-agent | done | 本節 §17、`booking-core-p0.openapi.yaml`、`mvn test` |

## 19. Round 7：前端 P0 對接（wire-first）

- **Auth**：`POST /api/auth/login`（表單欄位為 **username**，沿用既有輸入框 state 傳遞）、成功後 `GET /api/auth/me` 灌入 `applyMeResponse`；`logout` 先 `POST /api/auth/logout` 再清本地；後端未啟用 JWT 時主路徑失敗仍可用示範 mock 登入。
- **Client**：`clientCatalogApi`（可見商家、storefront）、`SearchPage` 載入商家並展開資源、`BookingPage` 以 `resources/{id}/availability` + `POST /bookings` 完成預約；`MyBookingsPage` 既有串接維持。
- **Merchant**：`merchantBookingsApi` + `BookingsPage` 以 `tenantId`（JWT `merchantId`）拉 `/api/merchant/{id}/bookings`，操作選單對齊現有狀態機（PENDING 僅取消；CONFIRMED 報到/取消；CHECKED_IN/IN_SERVICE 可完成）。
- **驗證**：`pnpm run build`（tsc + vite）通過。

## 20. Round 7 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 7：前端 P0 wire-first | frontend-engineer-agent | done | 本節 §19、`pnpm build` |

## 21. Round 8：Merchant Service/Resource 語意收斂（progress continuation）

- 需求背景：
  - merchant 端 `resources` 頁面目前同時管理 service 與 resource，語意容易混淆。
  - 產品確認方向為「resource 需可對應 staff assignment」，作為後續避免 double booking 的核心能力之一。
- 本輪已完成（Frontend/UI）：
  - 頁面標題與導覽文案改為 `Service & Resource Management`（zh-TW/en-US 同步）。
  - Resources 區塊新增管理開關（enable/disable）。
  - Resources 區塊加入 service 依賴 UI 規則：
    - 無 service 時，resource 管理開關與相關操作不可用，並顯示導引文案。
    - 開關關閉時，resource 清單與搜尋不啟用。
- 本輪已完成（PM/Spec）：
  - 新增 staff assignment 導向規格，明確定義 `Service / Resource / Staff / Assignment` 語意與 API 邊界：
    - `doc/specs/closed/2026-04-14_merchant-resources-crud.md`
- 待續（Backend + QA）：
  - Resource CRUD 擴充 `assignedStaffIds` 寫入與查詢回傳。
  - 新增 assignment command API（assign/reassign/release）與 `409` 衝突測試。
  - 加上 tenant-scoped 唯一鍵與狀態轉移合法性測試。

## 22. Round 8 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 8：Service/Resource UI 語意收斂 | frontend-engineer-agent | done | `frontend/src/apps/merchant/pages/ResourcesPage.tsx` + i18n 變更 |
| Round 8：staff assignment 規格封包建立 | pm-agent | done | `doc/specs/closed/2026-04-14_merchant-resources-crud.md` |
| Round 8：Resource-assignment API 實作 | backend-engineer-agent | todo | 待下一輪 auto-dev 實作 |
| Round 8：assignment QA 測試矩陣 | qa-agent | todo | 待 API 契約凍結後執行 |

## 23. Round 9：Resource staff assignment（Backend 第一版）

- 本輪完成（backend）：
  - `resource_items` 新增 `assigned_staff_ids_json` 欄位（migration `V18__resource_items_assigned_staff_ids.sql`）。
  - Merchant resource API 支援 `assignedStaffIds`：
    - `POST /api/merchant/{merchantId}/resources`
    - `PATCH /api/merchant/{merchantId}/resources/{resourceId}`
    - `GET /api/merchant/{merchantId}/resources` 回傳 `assignedStaffIds`
  - 指派驗證規則：
    - 僅允許指派該 merchant 下 `ACTIVE` 的 team members（以 `platformUserId` 判定）。
    - 無效/跨租戶/非 active 成員會回 `400`。
    - 會自動去重與過濾非正整數 id。
- 本輪測試：
  - `MerchantResourceCrudApiTest` 新增案例：
    - 可成功建立含 `assignedStaffIds` 的 resource。
    - 指派非成員或無效成員時回 `400`。
  - 驗證命令：
    - `mvn -Dtest=MerchantResourceCrudApiTest test`（PASS）。

## 24. Round 9 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 8：Resource-assignment API 實作 | backend-engineer-agent | done | `V18__resource_items_assigned_staff_ids.sql`、`MerchantPortalController`、`ApiDtos`、`ResourceItem` |
| Round 8：assignment QA 測試矩陣 | qa-agent | in_progress | `MerchantResourceCrudApiTest` 已補兩個 assignment 測試；待補 E2E 與衝突情境 |

## 25. Round 10：Resource staff assignment（Frontend 串接）

- 本輪完成（frontend）：
  - `merchantPortalApi` 補齊 `assignedStaffIds` 契約欄位（Resource DTO、create/update payload）。
  - `ResourcesPage` 串接 team/member 資料：
    - 透過 `listMerchantTeams` + `listMerchantTeamMembers` 載入可指派員工清單（active members）。
  - 建立資源可直接傳 `assignedStaffIds`（multi-select）。
  - 編輯資源可更新 `assignedStaffIds`（現階段以 prompt comma-list 形式）。
  - 資源表格新增 `Assigned Staff` 欄位顯示已指派人員。

## 26. Round 10 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 10：Frontend assignment 串接 | frontend-engineer-agent | done | `ResourcesPage.tsx` + `merchantPortalApi.ts` |
| Round 10：UI/UX polish（專用對話框與欄位文案 i18n） | frontend-engineer-agent + uiux-agent | todo | 待下一輪優化 |

## 27. Round 11：Client booking cancellation + validation closure

- 本輪完成（backend）：
  - 新增 client 取消預約端點：`PATCH /api/client/bookings/{bookingId}/cancel`
  - 僅允許本人預約且狀態為 `PENDING/CONFIRMED` 取消，其他狀態回 `409`。
  - DTO 新增：
    - `ClientBookingCancelRequest`
    - `ClientBookingStatusResponse`
  - `BookingRepository` 新增 `findByIdAndPlatformUserId` 供 ownership 驗證。
- 本輪完成（frontend）：
  - `clientBookingApi` 新增 `cancelClientBooking(...)`。
  - `MyBookingsPage` upcoming 卡片的 `Cancel` 動作改為實際呼叫 API，成功後自動刷新三個 tab 列表。
- 本輪測試與驗證：
  - 新增測試：`ClientBookingListApiTest.cancelOwnBooking_movesToCancelledAndAppearsInCancelledTab`。
  - 修正 client booking 相關測試登入帳號為 email 形式（符合目前 auth 規則）。
  - 執行：
    - `mvn "-Dtest=ClientBookingCreateApiTest,ClientBookingListApiTest,ClientResourceAvailabilityApiTest" test` -> PASS
    - `pnpm build` -> PASS

## 28. Round 11 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| 使用者可取消預約（client） | backend-engineer-agent + frontend-engineer-agent | done | `ClientBookingController` / `ClientBookingService` / `clientBookingApi` / `MyBookingsPage` |
| 建立、衝突、查詢分頁、取消流程驗證 | qa-agent | done | 指定測試集 PASS + frontend build PASS |

## 29. Round 12：Merchant Resources 編輯 UX polish（移除 prompt）

- 本輪完成（frontend）：
  - 移除 `ResourcesPage` 內的 `window.prompt` 編輯流程。
  - 以 inline 編輯面板取代：
    - 可編輯欄位：`name/category/price/capacity/active/assignedStaffIds`
    - 指派員工維持 multi-select（沿用 Round 10 staff options）
    - 提供 Save/Cancel 行為並呼叫既有 `updateMerchantResource`
- 驗證：
  - `pnpm build` PASS。

## 30. Round 12 PM 追蹤更新

| Checklist Item | Owner | Status | Completion Evidence |
|---|---|---|---|
| Round 10：UI/UX polish（專用對話框與欄位文案 i18n） | frontend-engineer-agent + uiux-agent | in_progress | 已移除 prompt 並改正式編輯面板；i18n 文案細化待下一輪 |

