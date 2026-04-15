# auto-dev — client booking MVP（continuation）結案紀錄

> **STATUS: CLOSED（已結案）**  
> 本規格已自 `doc/specs/progress/` **歸檔至** `doc/specs/closed/2026-04-15_auto-dev-client-booking-continuation-closeout.md`（檔名不變，符合 `spec-done-archive`：結案後不重複保留於 `progress/`）。  
> 結案裁定日：**2026-04-15**。Reviewer：`CLOSURE_APPROVED`（無 High 阻擋結案）；PM：**結案並宣告 MVP 範圍內 acceptance checklist 全部 PASS**。

---

## 結案範圍內 success checklist（全部 PASS）

| # | 項目 | 證據 |
|---|------|------|
| 1 | 客戶可建立預約（時間區間；`endAt` 由服務時長衍生） | `ClientBookingCreateApiTest`；`BookingPage` + `createClientBooking` |
| 2 | 同一並行範圍內時間重疊 → **409**、無側錄寫入 | `ClientBookingCreateApiTest` conflict；`ClientBookingService` + pessimistic overlap query |
| 3 | 預約列表 **分頁**（`page` / `size`、上限、未知 tab） | `ClientBookingListApiTest` |
| 4 | 客戶可 **取消** 預約；非法狀態／重複取消有穩定錯誤語意 | `ClientBookingListApiTest` cancel / double-cancel |
| 5 | 已取消預約不出現在「進行中／upcoming」語意之列表 | list tab 測試與 `cancelled` tab 行為 |
| 6 | 未授權／錯誤角色呼叫 API 被拒 | 401／403 案例於 `ClientBookingListApiTest` 等 |
| 7 | React **前端操作 UI**（建立、我的預約、載入／錯誤） | `BookingPage.tsx`、`MyBookingsPage.tsx`、`clientBookingApi.ts`；`npm run build` PASS |
| 8 | 客戶端 ID 與後端 **UUID 字串** JSON 合約一致 | 型別與路由驗證於 client API／`BookingPage` |

**明確排除於本案結案條件之外（列為後續工作，不阻擋結案）：** 雙執行緒／雙 HTTP 併發壓力測試；重複取消之 HTTP 冪等性產品決策；商家後台 `tenantId` 數值假設全面 UUID 化；`/auto-dev` 範本 MyBatis XML 與本 repo **JPA** 之流程文件對齊。

---

## Implementation summary

- Create：`POST /api/client/bookings`，`ClientBookingService`，`BookingPage.tsx`、`clientBookingApi.ts`。
- 防重疊：resource / scoped `ServiceItem` pessimistic lock + `findOverlappingBookingsForUpdate` → `BOOKING_SLOT_CONFLICT`。
- 列表分頁：`GET /api/client/bookings`；`MyBookingsPage.tsx`。
- 取消：`PATCH /api/client/bookings/{id}/cancel`。
- UI 路由：`frontend/src/apps/client/App.tsx`。

---

## Test / validation evidence（結案前重跑）

```bash
cd backend && mvn test "-Dtest=ClientBookingCreateApiTest,ClientBookingListApiTest,ClientBookingRescheduleApiTest"
```

**Result: PASS**（2026-04-15，exit code 0）。

```bash
cd frontend && npm run build
```

**Result: PASS**（2026-04-15，exit code 0）。

---

## Reviewer（結案門檻）

- **CLOSURE_APPROVED**：MVP checklist 已由指名測試與程式路徑覆蓋；無 High 阻擋結案。  
- 後續改善建議見上表「排除於結案條件之外」。

---

## PM 結案聲明

本 `/auto-dev` continuation 所涵蓋之**客戶端預約 MVP**，經 Reviewer 同意結案、且結案前驗證指令皆 **PASS**，**正式結案**。後續議題請另開 `doc/specs/open/YYYY-MM-DD_<topic>.md` 追蹤，並於內文引用本結案檔。

## 相關已結案規格（同主題脈絡）

- `doc/specs/closed/2026-04-15_client-reservation-auto-dev.md`
- `doc/specs/closed/2026-04-15_client-booking-pessimistic-cancel.md`
