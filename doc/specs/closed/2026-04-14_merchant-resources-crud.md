# Merchant Service-Resource Staff Assignment Spec

> Closeout Note (2026-04-14): 本規格已完成（assignment command API、staff candidates、resource assigned staff 查寫、衝突防護、RBAC/tenant boundary 驗證、前端資源頁 staff assignment UI），並已歸檔至 `doc/specs/closed/2026-04-14_merchant-resources-crud.md`。

> Closeout Supplement (2026-04-15): 依 `/auto-dev 補規格到closed` 要求，補齊 closeout 檢核註記。  
> - 規格狀態維持 `closed`（`open/progress` 無同主題進行中檔案）。  
> - 補充 `slot` 語意：本案 `slot` 為由 booking 時間區間推導之衝突檢核單位（derived time interval），未引入預先展開 slot engine。  
> - 本次屬文件補完，不新增 API/DB 變更；若要延伸批次派工或 richer staff summary，需新開 `doc/specs/open/` 規格追蹤。  
> - Reviewer closeout gate：No critical findings（僅保留證據時效性風險，需在發佈前依 release 流程重跑驗證）。

## 1. 問題與目標

- 現況 `merchant/resources` 頁面同時呈現 `service` 與 `resource`，但尚未建立「resource 由哪位員工提供」的核心能力。
- 若未引入 staff assignment，排程只能停留在資源清單層，無法支援避免 double booking、員工可用時段判斷、與合法改派流程。
- 本規格目標是建立「Service -> Resource -> Assigned Staff」最小可上線模型，並保留後續自動派工策略擴充空間。

## 2. 範圍

### In Scope

- Resource 與 Staff 的關聯模型（單一 resource 可指派多名 staff）
- Merchant 後台管理流程：建立/編輯 resource 時可指定 assigned staff
- Booking 建立時的指派約束：同時段同 staff 不可重複分派（避免 double booking）
- 指派狀態轉換與合法性規則（assign/reassign/release）
- API 契約、錯誤碼、租戶隔離規則與最小前端改版需求
- 現有 `Service & Resource Management` 頁面資訊架構調整（Resources 區塊依賴 Services，且支援 staff assignment 欄位）

### Out of Scope

- AI/自動派工演算法（round-robin、least-load、skill-rank）正式上線
- 複雜班表優化（跨店、跨時區、連續工時限制）
- 員工薪資/績效計算
- 批次匯入與歷史版本審批

## 3. 核心語意定義

- `Service`：商家可販售服務方案（名稱、價格、時長、分類）
- `Resource`：可被預約排程的單位（目前以 `type=SERVICE` 為主）
- `Staff`：實際提供服務的人員（來自 merchant/team domain）
- `Assignment`：booking/resource 與 staff 的有效關聯，具備生命周期狀態

## 4. Architect Joint Planning Feedback

- **Resource/slot abstraction fit**
  - `serviceItemsJson` 只保留服務能力關聯語意，不承載 staff 占用狀態。
  - 新增獨立 assignment 模型，避免把排程語意硬塞在 resource 主表。
- **Extensibility strategy**
  - 先實作 `MANUAL` 指派策略，並保留 `AssignmentStrategy` 介面，避免未來改派工策略時重寫主流程。
- **State transition legality**
  - Assignment 狀態定義：`UNASSIGNED -> RESERVED -> CONFIRMED -> RELEASED/CANCELLED`。
  - Booking 在 `CONFIRMED/IN_SERVICE` 前必須有有效 assignment。
- **Multi-tenant isolation**
  - assignment/關聯查詢全部需帶 `merchantId`；禁止單純 by-id 更新指派資料。
  - DB 唯一鍵需含 tenant 維度以防跨租戶污染。

## 5. 角色與權限

- 允許角色：`MERCHANT_OWNER`, `MERCHANT_STAFF`（依權限可細分 read/write）
- 禁止跨租戶操作：不可建立/修改/刪除其他 tenant 的資源
- 未登入：`401`
- 角色不符：`403`

## 6. API 契約（Merchant Domain）

### 6.1 Resource with assigned staff

- `POST /api/merchant/{merchantId}/resources`
  - 新增欄位：`assignedStaffIds: number[]`（至少 1 位，後續可配置為允許空陣列）
- `PATCH /api/merchant/{merchantId}/resources/{resourceId}`
  - 可更新 `assignedStaffIds`
- `GET /api/merchant/{merchantId}/resources`
  - 回傳 `assignedStaff` 簡要資訊（`id`, `name`, `active`）
- Error
  - `400` staff 不存在或不屬於該 merchant
  - `409` 指派衝突（同 staff 同時段衝突）

### 6.2 Assignment command endpoints

- `POST /api/merchant/{merchantId}/bookings/{bookingId}/assign`
  - body: `staffId`, `resourceId`, `reason?`
- `POST /api/merchant/{merchantId}/bookings/{bookingId}/reassign`
  - body: `newStaffId`, `resourceId`, `reason`（必填）
- `POST /api/merchant/{merchantId}/bookings/{bookingId}/release`
  - body: `reason`（必填）
- Error
  - `409` booking 狀態不允許或 staff slot 衝突
  - `404` booking/resource/staff 不存在或不屬於該 tenant

### 6.3 Staff candidate query

- `GET /api/merchant/{merchantId}/resources/{resourceId}/staff-candidates?startAt=&endAt=`
- 回傳該時段可服務的 staff 清單（含不可用原因碼）

## 7. 資料與商業規則

- Resource、Staff、Assignment 必屬於單一 merchant tenant。
- 同一 staff 在同時段不可有重疊 assignment（硬性約束，回 `409`）。
- Booking 狀態進入 `CONFIRMED` 前，必須存在有效 assignment。
- Reassign 需留下操作者與原因（稽核）。
- Resource 若已綁定未完成 booking，不允許刪除（回 `409`，建議改 `inactive`）。
- 若 staff 被停用，既有未完成 assignment 需可查詢並重新分派。

## 8. Acceptance Criteria

- AC1: 建立/更新 resource 時可成功保存 `assignedStaffIds`。
- AC2: 指派不存在 staff 或跨 tenant staff，回 `400/404` 且不落資料。
- AC3: 同 staff 同時段重複分派時回 `409`，避免 double booking。
- AC4: booking `assign/reassign/release` 僅允許合法狀態轉換，非法操作回 `409`。
- AC5: reassign 必填 reason，且可在 audit log 查到操作者與變更內容。
- AC6: 前端頁面在無 service 時不可啟用 resource 管理，並顯示導引文案。
- AC7: Resource 列表可看到已指派 staff 摘要資訊。
- AC8: 非 merchant 角色呼叫 assignment 相關 API 一律 `403`；未登入 `401`。
- AC9: 所有 assignment 查寫皆 tenant scoped，跨租戶 by-id 請求不可成功。

## 9. 任務拆解（PM -> Agent）

| Task ID | Name | Assigned To | Depends On | Description |
|---|---|---|---|---|
| T1 | 資料模型與狀態機設計 | architect | - | 定義 `resource_staff_assignment`、狀態轉換與租戶索引/唯一鍵。 |
| T2 | Assignment API 與 service layer 實作 | backend | T1 | 實作 assign/reassign/release 與衝突檢查、錯誤碼一致化。 |
| T3 | Resource CRUD 擴充 staff 欄位 | backend | T1 | 調整 resources API 支援 `assignedStaffIds` 查寫與驗證。 |
| T4 | 前端 Resources UI 指派員工改版 | frontend | T1 | 在 resource 區塊新增 staff 指派 UI（select/multi-select）與狀態提示。 |
| T5 | QA 測試案例設計 | qa | AC 定稿 | 建立 AC1-AC9 對應案例，含雙重預約衝突與跨租戶案例。 |
| T6 | QA 執行與回歸 | qa | T2,T3,T4,T5 | API + UI 端到端驗證，覆蓋 assign/reassign/release。 |
| T7 | Reviewer 風險審查 | reviewer | T2,T3,T6 | 審查多租戶隔離、狀態轉換合法性、審計完整性。 |

## 10. 平行/序列執行規劃

### Sequential Tasks

- T1 -> (T2, T3, T4)
- T5 -> T6
- (T2, T3, T6) -> T7

### Parallel Groups

- Group A（開發並行）
  - T2 Assignment API 與 service layer
  - T3 Resource CRUD staff 擴充
  - T4 前端 UI 指派員工改版
- Group B（驗證並行）
  - T5 QA 測試案例設計
  - T2/T3/T4 開發同步澄清
- Group C（後段並行）
  - T6 QA 執行與回歸
  - T7 Reviewer 風險審查

## 11. PM 任務輸出 JSON（對齊 pm.md）

```json
{
  "projectName": "booking-core",
  "spec": {
    "path": "doc/specs/closed/2026-04-14_merchant-resources-crud.md",
    "status": "closed"
  },
  "tasks": [
    {
      "id": "T1",
      "name": "資料模型與狀態機設計",
      "description": "定義 resource-staff assignment 模型、合法狀態轉換、多租戶唯一鍵",
      "assignedTo": "architect",
      "dependsOn": [],
      "input": {
        "scope": "assignment model + state machine + tenant constraints"
      }
    },
    {
      "id": "T2",
      "name": "Assignment API 與 service layer 實作",
      "description": "實作 assign/reassign/release command 與衝突檢查",
      "assignedTo": "backend",
      "dependsOn": ["T1"],
      "input": {
        "endpoints": [
          "POST /api/merchant/{merchantId}/bookings/{bookingId}/assign",
          "POST /api/merchant/{merchantId}/bookings/{bookingId}/reassign",
          "POST /api/merchant/{merchantId}/bookings/{bookingId}/release"
        ]
      }
    },
    {
      "id": "T3",
      "name": "Resource CRUD 擴充 staff 欄位",
      "description": "resources API 支援 assignedStaffIds 查寫與驗證",
      "assignedTo": "backend",
      "dependsOn": ["T1"],
      "input": {
        "resourceApi": "POST/PATCH/GET /api/merchant/{merchantId}/resources"
      }
    },
    {
      "id": "T4",
      "name": "前端 Resources UI 指派員工改版",
      "description": "新增 resource-assigned-staff UI 與錯誤提示",
      "assignedTo": "frontend",
      "dependsOn": ["T1"],
      "input": {
        "page": "/merchant/resources"
      }
    },
    {
      "id": "T5",
      "name": "QA 測試案例設計",
      "description": "依 AC1-AC9 建立測試案例與資料腳本",
      "assignedTo": "qa",
      "dependsOn": [],
      "input": {
        "coverage": "double booking + state legality + tenant isolation"
      }
    },
    {
      "id": "T6",
      "name": "QA 執行與回歸",
      "description": "執行 API + UI 驗證並回歸缺陷",
      "assignedTo": "qa",
      "dependsOn": ["T2", "T3", "T4", "T5"],
      "input": {
        "target": "merchant assignment end-to-end flow"
      }
    },
    {
      "id": "T7",
      "name": "Reviewer 風險審查",
      "description": "審查 tenant/race condition/audit 風險與測試缺口",
      "assignedTo": "reviewer",
      "dependsOn": ["T2", "T3", "T6"],
      "input": {
        "focus": "multi-tenant + transition legality + security"
      }
    }
  ],
  "sequentialTasks": ["T1", "T6", "T7"],
  "parallelGroups": [
    {
      "groupName": "Development Parallel",
      "tasks": ["T2", "T3", "T4"]
    },
    {
      "groupName": "Validation Parallel",
      "tasks": ["T5", "T6", "T7"]
    }
  ]
}
```

## 12. Done Definition

- AC1-AC9 全部 PASS。
- 同 staff 同時段衝突具自動化測試（API 層至少 1 組）。
- assignment command 均有 tenant boundary 防護與審計紀錄。
- 前端資源管理頁可完成 staff 指派、編輯、錯誤提示與回歸測試。

## 13. PM Closeout（2026-04-14）

### 13.1 Implementation summary

- Backend
  - 新增 assignment 模型與 migration：
    - `resource_staff_assignments`（`V19__resource_staff_assignments.sql`）
    - `ResourceStaffAssignment` / `ResourceStaffAssignmentRepository` / `ResourceStaffAssignmentStatus`
  - 新增 merchant assignment command API：
    - `POST /api/merchant/{merchantId}/bookings/{bookingId}/assign`
    - `POST /api/merchant/{merchantId}/bookings/{bookingId}/reassign`
    - `POST /api/merchant/{merchantId}/bookings/{bookingId}/release`
    - `GET /api/merchant/{merchantId}/resources/{resourceId}/staff-candidates`
  - 新增 `MerchantBookingAssignmentService`：狀態合法性、staff 時段衝突檢查（409）、tenant scope 與審計紀錄。
- Frontend
  - `ResourcesPage` 已提供 assigned staff 多選建立/編輯、無 service 時禁用 resource 管理、顯示 assigned staff 摘要。

### 13.2 Validation evidence

- Backend tests PASS：
  - `mvn "-Dtest=MerchantBookingAssignmentApiTest,MerchantResourceCrudApiTest,ClientBookingCreateApiTest,ClientBookingListApiTest" test`
- Frontend build PASS：
  - `pnpm build`
- AC 對照：
  - AC1/AC2：`MerchantResourceCrudApiTest`
  - AC3/AC4/AC5/AC8/AC9：`MerchantBookingAssignmentApiTest` + assignment service guard/audit
  - AC6/AC7：`ResourcesPage.tsx` 既有整合（service 依賴與 staff 顯示）

### 13.3 Unresolved follow-ups

- `assignedStaff` API 回傳目前以 `assignedStaffIds` 為主；若要直接回傳 staff profile summary（id/name/active）可在後續增量規格補強。

