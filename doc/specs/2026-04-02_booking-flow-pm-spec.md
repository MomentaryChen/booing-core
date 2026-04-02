# 建立預約流程 PM 規格（可執行）

日期：2026-04-02  
文件角色：PM 單一事實來源（Single Source of Truth）  
適用範圍：`/client`、`/merchant`、`/system` 與對應 `/api/*`

## 1) Problem Statement / User Value

目前平台缺少一套完整、可跨業種重用的端到端預約流程定義，導致前後端與設計在流程節點、狀態處理、通知與取消規則上容易各自實作，增加回歸與維護成本。

本需求目標是建立「以 `resource/slot` 為核心」的通用預約流程，讓團隊可直接進入設計與開發，並保證：
- Client 可快速完成查詢、建立、確認、修改/取消與後續追蹤。
- Merchant 可穩定管理可用時段、例外規則與履約處理。
- System 可統一治理策略、稽核與多租戶安全邊界。

---

## 2) 一頁式流程總覽（文字流程圖）

```text
[Client 查詢可用時段]
  -> 輸入 resource 條件/日期區間
  -> Slot Engine 即時計算可用 slot（規則 - exceptions - 已佔用 - 有效鎖）
  -> 顯示可選時段

[Client 建立預約]
  -> 選定 resource + slot + 基本資料
  -> 建立 Booking (Draft/Pending) + 短鎖 (TTL)
  -> 執行 BookingValidationStrategy（資格/容量/時窗/政策）
  -> 需要付款?
      -> Yes: Pending Payment -> 支付成功 -> Confirmed
      -> No: 直接 Confirmed
  -> 發送 BookingConfirmed 通知

[後續流程分支]
  A. 修改：允許條件下重選 slot -> 重新驗證 -> 更新通知
  B. 取消：Client/Merchant 觸發 -> Cancelled -> 依政策進 Refund/Compensation
  C. 逾期：未完成必要動作 -> Expired（自動）
  D. No-show：到時未履約 -> NoShow
  E. 完成：履約完成 -> Completed
  F. 退款/補償：依 RefundCompensationStrategy -> Refunded 或補償記錄

[治理與保護]
  -> 全狀態轉移走單一 State Machine 入口（禁止直接改 status）
  -> 全流程 tenant scope 強制帶入、跨租戶回 404
  -> 全事件寫入 audit log（who/when/tenant/from/to/reason）
```

---

## 3) 角色與觸點（Client / Merchant / System）

### Client（終端使用者）
- 路由：`/client`、`/client/booking/:slug`
- API（高層）：`/api/client/resources`、`/api/client/availability`、`/api/client/bookings`
- 主要觸點：查詢可用 slot、建立預約、查看狀態、修改/取消、接收通知、查看退款/補償結果。

### Merchant（商家營運）
- 路由：`/merchant`、`/merchant/appointments`、`/merchant/settings/schedule`
- API（高層）：`/api/merchant/resources`、`/api/merchant/schedules`、`/api/merchant/availability-exceptions`、`/api/merchant/bookings`
- 主要觸點：維護 resource 與可用規則、處理確認/取消/No-show/完成、檢視爭議與補償。

### System（平台管理）
- 路由：`/system`
- API（高層）：`/api/system/resource-types`、`/api/system/policies`、`/api/system/notifications`、`/api/system/audits`
- 主要觸點：策略模板治理（驗證/通知/退款補償）、多租戶與稽核監控、風險配置。

---

## 4) 詳細需求條列（User Stories + Acceptance Criteria）

### Epic A：可用時段查詢（Availability）

**Story A1（Client）**  
作為 Client，我要依 `resource` 與日期區間查詢可用 `slot`，以便找到可預約時段。

**Acceptance Criteria**
- 系統回傳的可用 slot 需反映：營運規則、exceptions、已確認占用、有效鎖。
- Merchant 新增或修改 exception 後，Client 查詢結果在下一次請求立即生效。
- 若 `resource` 不存在或不屬於當前 tenant 範圍，回應 404。

**Story A2（Merchant）**  
作為 Merchant，我要設定每週可用規則與 exceptions（關閉/加開），以便控制供給。

**Acceptance Criteria**
- 可設定週期規則（例如週一至週五某時段）與日期區間 exceptions。
- exceptions 優先於週期規則。
- 修改後不可要求重建全年預展開 slot 資料。

---

### Epic B：建立預約與確認（Create & Confirm）

**Story B1（Client）**  
作為 Client，我要建立預約請求，避免同時段被重複預約。

**Acceptance Criteria**
- 建立請求時產生 `Draft` 或 `Pending` 狀態與短鎖（TTL）。
- 高併發下，同一 `resource + time_range` 最多僅 1 筆可進入 `Confirmed`。
- 重送相同 idempotency key 不可重複建立新預約。

**Story B2（System/Engine）**  
作為平台，我要依 `ResourceType` 套用 `BookingValidationStrategy`，確保可擴充。

**Acceptance Criteria**
- 新增 `ResourceType` 時可透過新增 strategy 實作完成驗證，不修改核心 flow service。
- 驗證失敗時回傳可識別錯誤碼（例如容量不足、超出可預約時窗）。
- 禁止以硬編碼業種名詞分支控制核心流程。

**Story B3（Client）**  
作為 Client，我要在付款成功（若需要）後取得確認結果與通知。

**Acceptance Criteria**
- 需付款流程：`Pending -> Confirmed` 僅在支付成功事件後發生。
- 免付款流程：可直接轉 `Confirmed`。
- 轉 `Confirmed` 後必觸發一次通知事件，重試不重複發送。

---

### Epic C：修改與取消（Modify & Cancel）

**Story C1（Client）**  
作為 Client，我要在規則允許下修改既有預約時段。

**Acceptance Criteria**
- 僅指定狀態允許修改（例如 `Confirmed` 在截止時間前）。
- 修改需重新經過 slot 可用性與驗證策略檢查。
- 修改成功後需通知相關方（Client/Merchant）。

**Story C2（Client/Merchant）**  
作為 Client 或 Merchant，我要取消預約並依政策處理退款或補償。

**Acceptance Criteria**
- 取消後狀態轉移至 `Cancelled`，不得直接跳至 `Refunded`。
- 若涉及金流，需走 `RefundCompensationStrategy` 決策（全退/部分退/補償）。
- 退款/補償重試需冪等，避免重複退款。

---

### Epic D：逾期、No-show、完成（Expired / No-show / Completed）

**Story D1（System）**  
作為平台，我要自動處理逾期預約，避免待處理狀態長期堆積。

**Acceptance Criteria**
- 達到逾期條件後自動轉 `Expired`。
- `Expired` 的轉移有稽核紀錄與觸發通知（若政策要求）。

**Story D2（Merchant）**  
作為 Merchant，我要標記 No-show 或完成履約，反映實際服務結果。

**Acceptance Criteria**
- `Confirmed` 才可轉 `NoShow` 或 `Completed`。
- 非法轉移（如 `Draft -> Completed`）必須被拒絕並回應錯誤碼。
- 每次轉移都記錄操作者、租戶、原因與時間。

---

### Epic E：狀態機與治理（State Machine / Multi-tenant / Audit）

**Story E1（System）**  
作為平台，我要以單一狀態機入口管理狀態轉移，防止任意改值。

**Acceptance Criteria**
- 合法狀態至少涵蓋：`Draft`、`Pending`、`Confirmed`、`Cancelled`、`Expired`、`NoShow`、`Completed`、`Refunded`。
- 所有狀態變更透過集中 transition service，禁止繞過。
- 合法轉移矩陣需有單元測試覆蓋（含非法路徑）。

**Story E2（System）**  
作為平台，我要確保多租戶隔離不可被繞過。

**Acceptance Criteria**
- tenant 資料表均帶 `tenant_id`（含 booking/resource/rule/exception/lock/audit）。
- tenant 僅能從安全上下文解析，API 不接受可覆寫 tenant 參數。
- 缺 tenant context 請求需 fail-closed（拒絕處理）。

---

## 5) 路由/API 範圍標記（高層）

### `/client` + `/api/client/*`
- `GET /api/client/resources`
- `GET /api/client/availability`
- `POST /api/client/bookings`
- `PATCH /api/client/bookings/{id}`（修改）
- `POST /api/client/bookings/{id}/cancel`
- `GET /api/client/bookings/{id}`

### `/merchant` + `/api/merchant/*`
- `GET/POST/PATCH /api/merchant/resources`
- `GET/POST/PATCH /api/merchant/schedules`
- `GET/POST/PATCH /api/merchant/availability-exceptions`
- `GET /api/merchant/bookings`
- `POST /api/merchant/bookings/{id}/no-show`
- `POST /api/merchant/bookings/{id}/complete`
- `POST /api/merchant/bookings/{id}/cancel`

### `/system` + `/api/system/*`
- `GET/POST/PATCH /api/system/resource-types`
- `GET/POST/PATCH /api/system/policies`（validation/notification/refund）
- `GET /api/system/audits`
- `GET /api/system/tenants/{id}/health`（可選監控）

---

## 6) 優先級切分（MVP vs Phase 2）

### MVP（必做）
- 可用時段查詢（Slot Engine 即時計算 + exceptions）
- 建立預約（含鎖、冪等、防超賣）
- 確認流程（含需付款/免付款兩路）
- 修改與取消（含基本退款或補償決策）
- 逾期/No-show/完成狀態處理
- 狀態機集中轉移 + audit log
- 多租戶硬隔離（tenant scope fail-closed）

### Phase 2（次階段）
- 進階通知策略（多通道排程、提醒模板）
- 進階退款補償策略（多支付通道、局部退款細則）
- 系統治理儀表板（跨租戶品質指標）
- 更細粒度策略編輯器（可視化規則）

---

## 7) 依賴、風險、開放問題

### 依賴
- 支付事件回調機制（若有付款路徑）
- 通知通道服務（Email/SMS/Push/Webhook）
- 身份與租戶上下文來源（JWT/Token）
- 稽核儲存與查詢能力

### 主要風險
- 高併發下鎖與唯一約束不完整，導致超賣。
- exceptions 優先級規則不明，導致 availability 錯誤。
- 狀態轉移分散在多處，出現非法狀態。
- tenant 邊界漏控，造成跨租戶資料風險。
- 通知/退款重試非冪等造成重複執行。

### 開放問題（需在開發前定案）
- 付款是否為所有 `ResourceType` 必填，或由策略決定？
- 修改預約的截止時間與費率規則由誰配置（Merchant vs System）？
- No-show 後是否允許補償而不退款？哪些條件觸發？
- 逾期計時基準（建立後 N 分鐘、服務前 N 分鐘）是否可配置？
- 通知失敗重試次數與退避策略的預設值？

---

## 8) 可交付項目清單（按角色）

### Backend
- 建立 `Resource/Slot/Booking/BookingLock` 核心模型與資料邊界（含 tenant_id）。
- 實作 Slot Engine（規則 + exceptions 即時計算）。
- 實作 Booking State Machine 與集中 transition service。
- 實作 `BookingValidationStrategy`、`NotificationStrategy`、`RefundCompensationStrategy` 擴充點。
- 實作 `/api/client`、`/api/merchant`、`/api/system` 高層 API 與冪等/鎖機制。

### Frontend
- Client 頁面：查詢可用 slot、建立/修改/取消、狀態與通知回饋。
- Merchant 頁面：規則與 exceptions 設定、預約清單、No-show/完成/取消操作。
- System 頁面：政策設定、稽核查詢（最小可用版）。

### UI/UX
- 三角色流程稿（Client/Merchant/System）與狀態文案矩陣。
- 關鍵錯誤態與空態（時段售罄、驗證失敗、逾期、退款處理中）。
- 通知模板資訊架構（確認、提醒、取消、No-show、退款結果）。

### DevOps
- 事件重試與冪等觀測（通知/退款）指標與告警。
- 背景任務排程（逾期轉移、提醒通知）部署配置。
- 多環境配置範本（租戶、金流、通知通道）。

### QA
- 端到端測試資料設計（多租戶、多 resource type、併發情境）。
- 狀態機合法/非法轉移測試。
- 退款/補償與通知冪等重試測試。

---

## 9) 驗收測試清單（Given / When / Then）

1. **可用時段即時計算**
   - Given Merchant 設定每週開放規則與某日關閉 exception  
   - When Client 查詢該日 availability  
   - Then 回傳結果不包含被關閉 slot，且不需重建預存 slot。

2. **高併發防超賣**
   - Given 兩位 Client 同時提交同一 `resource + time_range` 建立請求  
   - When 系統處理並確認預約  
   - Then 最多只有一筆進入 `Confirmed`，另一筆得到可辨識失敗原因。

3. **新增 ResourceType 可擴充**
   - Given 新增一個 `ResourceType` 與對應 validation strategy  
   - When Client 走建立預約流程  
   - Then 核心 booking flow 不需修改即可完成驗證與建立。

4. **確認通知冪等**
   - Given 預約已轉 `Confirmed` 且通知發送任務重試  
   - When 任務因暫時錯誤重跑  
   - Then Client 僅收到一次確認通知（不重複）。

5. **修改預約需重新驗證**
   - Given 一筆 `Confirmed` 預約在允許修改時窗內  
   - When Client 改約到新 slot  
   - Then 系統重新執行 availability 與 validation，成功後更新狀態與通知。

6. **取消後走退款/補償策略**
   - Given 一筆可退款的已確認預約被取消  
   - When 系統執行退款流程  
   - Then 先轉 `Cancelled`，再依策略轉 `Refunded` 或建立補償記錄。

7. **逾期自動處理**
   - Given 一筆 `Pending` 預約超過逾期門檻  
   - When 逾期排程執行  
   - Then 狀態自動轉 `Expired` 並寫入稽核紀錄。

8. **No-show/完成合法轉移**
   - Given 一筆 `Confirmed` 預約  
   - When Merchant 標記 `NoShow` 或 `Completed`  
   - Then 轉移成功；若從 `Draft` 直接標記則被拒絕。

9. **多租戶隔離**
   - Given A 租戶使用者持有 B 租戶的 booking id  
   - When 呼叫查詢或修改 API  
   - Then 系統回 404 且不洩漏資源存在資訊。

10. **缺 tenant context fail-closed**
    - Given 請求無有效 tenant context  
    - When 呼叫任一 tenant-scoped booking API  
    - Then 系統拒絕請求，不進行任何讀寫。

---

## 10) Handoff（下一步責任分配）

- `architect-agent`：確認狀態機轉移矩陣、策略介面與 slot engine 邊界（ADR 級別）。
- `backend-engineer-agent`：MVP API 與 domain flow 落地、併發與冪等實作。
- `frontend-engineer-agent`：Client/Merchant/System 對應頁面與流程互動。
- `uiux-agent`：三角色流程稿、狀態文案、錯誤態與通知體驗。
- `devops-agent`：排程、監控、告警與多環境參數治理。
- `reviewer-agent`：每次 commit 後執行缺陷與風險審查（合併前品質閘門）。
