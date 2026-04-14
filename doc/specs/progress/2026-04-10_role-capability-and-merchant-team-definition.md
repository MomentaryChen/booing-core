# 角色能力與商戶服務隊定義（前後端整合基線）

## 1. 目的

- 釐清目前產品中的角色邊界與可執行操作，作為前後端整合共同契約。
- 回答並明確定義「一個商戶是否可開多個服務隊」與其資料模型方向。
- 供 `frontend`、`backend`、`qa`、`review` 在同一語意下開發與驗收。

## 2. 範圍與前提

- 目前前端已落地三大角色路由面：`/client`、`/merchant`、`/admin`。
- 現況角色型別為 `CLIENT | MERCHANT | ADMIN`（`MERCHANT_OWNER` 尚未在程式角色枚舉中獨立）。
- 本文件為「整合基線」：在不破壞現有前端流程下，補齊可實作的權限與模型定義。

## 3. 角色定義（建議落地版）

### 3.1 CLIENT_USER（客戶）

**可做**

- 瀏覽首頁精選、分類、搜尋服務。
- 查看服務詳情與可預約時段。
- 建立自己的預約。
- 查詢自己的預約（upcoming/past/cancelled）。
- 取消或改期自己的預約。
- 維護自己的個人資料、通知偏好、密碼。

**不可做**

- 管理商家資源、排程規則、商家設定。
- 查看或操作他人預約。
- 進入任何 system 平台治理功能。

### 3.2 MERCHANT_OWNER（商戶擁有者）

> 現況前端 `MERCHANT` 先視為 owner 權限。後續若導入 staff，owner 保持最高權限。

**可做**

- 管理本租戶下資源（create/read/update/delete）。
- 管理營業時間規則與例外日。
- 查詢並處理本租戶預約（confirm/reject/complete）。
- 維護商家設定（基本資料、通知、支付設定）。
- 管理本租戶團隊成員與權限（建議能力，對應 team 管理）。

**不可做**

- 跨租戶查看或修改資料。
- 平台層租戶治理（tenants/users/roles/audit logs）。

### 3.3 MERCHANT_STAFF（商戶成員，建議新增）

**可做（可配置）**

- 查看本租戶預約、執行授權內的預約狀態操作。
- 查看資源與班表。

**受限**

- 預設不得變更高風險商家設定（付款、方案、團隊權限）。
- 預設不得刪除資源或管理 owner 帳號。

### 3.4 SYSTEM_ADMIN（平台管理者）

**可做**

- 租戶管理（建立、停用、啟用、查詢）。
- 平台使用者管理。
- 角色與權限管理。
- 稽核日誌查詢與匯出。

**建議限制**

- 不直接承接商戶日常操作；若需代操作，需有可審計機制（impersonation/audit）。

## 4. Merchant Owner 與 Client User 關係

- 兩者為**平行角色**，不是上下屬或同租戶人員關係。
- `MERCHANT_*` 帳號屬於某個 `tenant`（tenant-scoped）。
- `CLIENT_USER` 為平台端使用者（通常不綁定 tenant）。
- 雙方主要透過 `Booking` 交易關聯：`clientUserId` 預約 `tenant` 底下的 `resource/slot`。

## 5. 一個商戶是否可開多個服務隊

**結論：可以，且建議支援。**

- 從產品與現有頁面方向，已具備多資源、多預約、多排程治理需求。
- 商戶多服務隊可提升：分工管理、排班精準度、績效分析、擴展多據點能力。

## 6. 建議資料模型（最小可行）

- `Tenant 1 --- N ServiceTeam`
- `ServiceTeam 1 --- N TeamMember`
- `ServiceTeam 1 --- N Resource`（或 `Resource N --- N ServiceTeam` 視指派模式）
- `Booking` 維持連結 `tenantId`、`resourceId`、`clientUserId`

建議欄位（示意）：

- `service_teams`: `id`, `tenant_id`, `name`, `code`, `status`, `created_at`
- `team_members`: `id`, `tenant_id`, `team_id`, `user_id`, `role`, `status`
- `resources`: 新增 `team_id`（若採一對多）或獨立 `resource_teams` 關聯表（若採多對多）

## 7. 最小權限規則（必須）

- `CLIENT_USER` 只能操作自己資料：profile + own bookings。
- `MERCHANT_*` 的所有查寫都必須強制 `tenant_id` 範圍。
- `MERCHANT_OWNER` 才能做高風險設定與團隊權限調整。
- 預約狀態更新僅能走 transition API（confirm/reject/complete/cancel），禁止任意 patch 狀態欄位。

## 8. API 與 RBAC 對齊建議

- 前端路由 `/admin/*` 對應後端 `"/api/system/*"`，不新增 `"/api/admin/*"`。
- 角色建議落地為：`CLIENT_USER`、`MERCHANT_OWNER`、`MERCHANT_STAFF`、`SYSTEM_ADMIN`。
- 過渡期可由後端將現有 `MERCHANT` 映射為 `MERCHANT_OWNER`，前端待下一版再細分 staff。

## 9. 驗收標準（Definition of Done）

- 後端權限判斷可明確區分四種角色責任邊界。
- 任一商戶可建立多個 `ServiceTeam` 並可將資源與成員指派到隊伍。
- 跨租戶存取被拒絕且有一致錯誤碼（例如 `403` / `TENANT_SCOPE_DENIED`）。
- booking 狀態非法轉移可穩定重現並回傳一致錯誤（例如 `409` / `BOOKING_STATE_ILLEGAL`）。

## 10. 後續落地順序（建議）

1. 先在後端完成角色與 tenant scope 守衛（不含 team UI）。
2. 實作 `ServiceTeam` 基礎模型與查詢 API。
3. 將 merchant 資源與預約操作串上 team 維度。
4. 前端再新增 team 管理頁與 staff 權限細分。

## 11. Execution Kickoff（2026-04-10）

### 11.1 Owner assignments

- `backend-engineer-agent`: role capability matrix、tenant scope guard、booking transition gateway、ServiceTeam/TeamMember API 與資料模型。
- `frontend-engineer-agent`: `/admin` -> `/api/system` 對齊、merchant/team 相關前端整合與 capability-driven UI。
- `uiux-agent`: team 管理流程 IA/copy、關鍵狀態與 a11y 驗證。
- `reviewer-agent`: 每次交付後執行 gate，high/critical 必修復後重審。
- `devops-agent` (optional): 若需要 runtime flag/config，再補 rollout checklist。

### 11.2 Architecture constraints (must follow)

- ServiceTeam 為組織維度，不可複製另一套 booking 核心流程。
- 權限檢查集中在 policy/authorizer，避免散落硬編碼 if/else。
- booking 狀態僅能透過合法 transition 入口改變，禁止直接 patch 狀態欄位。
- 所有 merchant/team 資料查寫必須 non-bypass tenant scope。
- 後端治理 API 使用 `/api/system/*`，不新增 `/api/admin/*`。

### 11.3 Tracking checklist

- [x] Spec confirmed and execution started (owner assigned).  
  Evidence: spec moved from `open` to `progress` and owner assignments recorded.
- [x] Architect alignment completed for abstraction/strategy/state/tenant dimensions.  
  Evidence: architecture constraints documented in section 11.2.
- [x] Backend data/API plan completed (migration/index/rollback included).  
  Evidence: round-2 backend implemented role canonical mapping compatibility baseline and auth payload alias support.
- [ ] Implementation delivered (backend + frontend).  
  Evidence: backend role baseline delivered; frontend/team API integration pending.
- [x] Reviewer gate passed (no unresolved high+ findings).  
  Evidence: reviewer-agent result = no critical/high findings for round-2 backend scope.

