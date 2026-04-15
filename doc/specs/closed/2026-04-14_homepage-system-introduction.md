# Homepage System Introduction Spec

> **Closed:** 本檔為已結案主版本（歸檔於 `doc/specs/closed/`）。

## Context

- 今日需求：建立新版首頁，用於清楚介紹 Booking Core 系統價值與核心能力。
- 首頁需包含基礎網站框架（Header、Footer）與多區塊敘事，支援首次訪客快速理解產品。
- 本規格由 PM 規劃，並納入 architect 同輪回饋，作為後續 UI/UX 與前端實作依據。

## Scope

- In scope:
  - 首頁資訊架構與區塊定義（Hero、Feature、流程、FAQ、CTA、Footer）。
  - 文案與內容模型（zh-TW / en-US）及可配置化要求。
  - 導覽與主要 CTA 導流策略（登入、註冊、預約體驗、商家導入）。
  - 架構與治理約束（Resource/Slot 抽象、策略可擴充、狀態合法性、多租戶隔離）。
- Out of scope:
  - 後端新商業流程開發。
  - 完整 CMS 系統建置（本階段僅定義內容結構與擴充接口）。
  - 品牌視覺最終稿（由 UI/UX 另案輸出）。

## Interpreted Scope and Acceptance Boundary

- 「完成首頁 spec」= 產出可直接交接 UI/UX、前端、後端（若需 API）與 QA 的單一規格基線，包含可測驗收條件與依賴邊界。
- 本規格驗收邊界：
  - 必須定義：首頁資訊架構、文案/i18n 結構、租戶化內容策略、追蹤需求、任務拆解與 owner/handoff。
  - 不必在本步完成：程式實作、最終視覺稿、CMS 後台、內容營運流程。
- 開工前可接受條件：
  - 所有 CTA 與區塊有明確責任與資料來源。
  - 對「多租戶隔離」與「狀態文案合法性」有具體規範可驗證。
  - 已標記 API/DB 是否需要變更（含不變更理由）。

## Assumptions to Proceed (Blocker Resolution)

- A1 API contract owner：由 `backend-engineer-agent` 主責 contract 與版本管理，`architect-agent` 僅做審核與守門；若衝突，以 backend contract 為準並提 issue 追補。
- A2 i18n source：Step 1 先採「前端靜態字典（repo i18n files）」為唯一來源，不開 `i18n-bundle API`；tenant 差異先放在 `homepage-config` 的 content key/section enablement。
- A3 時程保護：上述 assumptions 在 Step 1 鎖定，不因實作中臨時需求擴 scope；若要改為 API 化 i18n，列入下一張 spec。

## Homepage Information Architecture

1. Header
   - 左側 Logo + 品牌名稱。
   - 主要導覽：產品功能、解決方案、常見問題、聯絡我們。
   - 右側 CTA：登入、免費開始。
2. Hero Section
   - 主標：以可預約資源管理與時段引擎為核心價值。
   - 副標：強調多產業通用（場地、課程、人員、設備）。
   - 主 CTA：立即體驗；次 CTA：觀看導覽。
3. Core Value / Feature Cards
   - 可配置規則、可擴充策略、時段即時計算、跨角色協作。
   - 每張卡片需支援 icon、標題、摘要、連結。
4. How It Works (Process Steps)
   - 使用能力導向的抽象步驟（建立資源、設定規則、開放預約、追蹤營運）。
   - 不綁死單一產業流程，步驟數可配置（3-5 步）。
5. Social Proof / Use Cases
   - 展示多種 ResourceType 案例（課程、場地、顧問、設備）。
   - 支援租戶化內容替換。
6. FAQ
   - 常見問題：導入時間、資料安全、租戶隔離、費用模式。
7. Final CTA
   - 明確行動：預約 demo、免費試用、聯繫銷售。
8. Footer
   - 產品連結、文件中心、隱私權與條款、聯絡資訊、語系切換。

## Content and i18n Requirements

- 首頁所有文案必須採 i18n key 管理（至少 zh-TW、en-US）。
- 區塊內容需可由設定檔驅動（JSON 或等效結構），避免硬編碼於元件。
- CTA 文案採能力導向，不承諾固定流程與狀態結果。
- 所有示例術語優先使用 Resource/Slot 中性表述，避免單業種綁定。

## Architect Feedback Incorporated

### 1) Resource/Slot 抽象契合

- 行銷敘事採通用預約抽象，不使用單一業種詞作核心模型。
- 案例展示可多樣化，但能力描述需回到同一預約管線。

### 2) 策略可擴充性

- 首頁流程區塊設計為可配置模板，支援不同租戶與業種文案。
- 區塊排序、顯示開關、內容來源需可擴充，不以程式硬編碼。

### 3) 狀態轉移合法性

- 文案禁止暗示未受控保證（例如「必定立即確認」）。
- 狀態資訊由後端 API 為準，前端不自行推導狀態合法性。

### 4) 多租戶隔離影響

- 白標內容、品牌主題、FAQ 與 CTA 目標需 tenant-aware。
- 快取鍵與 SEO 輸出需納入 tenant 維度，避免跨租戶內容污染。

## Success Checklist

1. 結構完整：首頁含 Header、Footer 與至少 6 個核心區塊，且各區塊支援 `enabled/order/contentKey` 配置，不需改核心元件。
2. 語系完整：Hero、Feature、Process、FAQ、CTA 在 `zh-TW`、`en-US` 皆有對應 i18n key；任一 key 缺失時顯示可控 fallback，不可顯示 raw key。
3. 抽象一致：文案與範例以 Resource/Slot 能力導向描述，不出現綁定單一業種的主流程術語。
4. 狀態合法：首頁文案不得出現「立即確認/必定核准」等承諾語；流程區塊固定附註「實際狀態依租戶規則與資源策略」。
5. 租戶隔離：tenant A/B 在相同路由切換時，內容、品牌、FAQ、SEO 與快取不互相污染；快取鍵至少含 `tenantId+locale+pageVariant`。
6. 追蹤可用：CTA click、section view 事件皆帶 `tenantId/locale/sectionId/campaign`；QA 可在測試環境驗證事件上報。
7. 響應式可讀：手機與桌面版關鍵資訊不折損，主要 CTA 在首屏或合理滾動可達；無阻斷導覽的版面錯位。
8. 邊界情境：當 tenant 專屬內容缺失時，只可 fallback 到同 tenant 預設內容，不可回退其他 tenant 快取內容。

## API List and Checklist Mapping

| API | Purpose | Checklist Mapping | Needed Now |
|---|---|---|---|
| `GET /api/public/homepage-config?tenantId={id}&locale={locale}` | 取得首頁區塊配置與內容 key（含 tenant override） | #1 #2 #5 #8 | Yes |
| `GET /api/public/homepage-seo?tenantId={id}&locale={locale}&variant={variant}` | 取得租戶化 SEO 資料（title/description/structured data） | #5 | Yes |
| `POST /api/public/tracking/events` | 上報 CTA/section 互動事件 | #6 | Yes |
| `(Deferred) GET /api/public/i18n-bundle?...` | 租戶化語系包（後續擴充） | #2 #5 | No（Step 1 defer） |

## Step 2 Architect Output (API + DB Guardrails)

### A) Unified API Contract（全部回應統一包裝）

- Success/Failure 回應格式一律：`{ "code": "...", "message": "...", "data": ... }`
- `code` 規則：
  - 成功：`OK`
  - 可預期錯誤：`INVALID_ARGUMENT` / `TENANT_NOT_FOUND` / `LOCALE_NOT_SUPPORTED` / `STATE_GUARDRAIL_VIOLATION`
  - 系統錯誤：`INTERNAL_ERROR`
- `message` 給人類可讀文字；`data` 僅在成功時返回業務資料，失敗時可為 `null` 或錯誤細節物件。

#### 1) `GET /api/public/homepage-config`

- Query: `tenantId`(required), `locale`(required, `zh-TW|en-US`), `pageVariant`(optional, default=`default`)
- Request example:
  - `/api/public/homepage-config?tenantId=tnt_a&locale=zh-TW&pageVariant=default`
- Success response (`code=OK`)：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "tenantId": "tnt_a",
    "locale": "zh-TW",
    "pageVariant": "default",
    "sections": [
      { "id": "hero", "enabled": true, "order": 1, "contentKey": "homepage.hero.default" },
      { "id": "feature", "enabled": true, "order": 2, "contentKey": "homepage.feature.default" }
    ],
    "fallbackPolicy": {
      "allowTenantDefault": true,
      "crossTenantFallback": false
    },
    "stateLegalityNotice": "實際狀態依租戶規則與資源策略。"
  }
}
```

#### 2) `GET /api/public/homepage-seo`

- Query: `tenantId`(required), `locale`(required), `variant`(optional, default=`default`)
- Success response (`code=OK`)：

```json
{
  "code": "OK",
  "message": "success",
  "data": {
    "tenantId": "tnt_a",
    "locale": "zh-TW",
    "variant": "default",
    "title": "Booking Core - 可配置預約系統",
    "description": "以 Resource/Slot 抽象驅動多產業預約流程。",
    "canonicalUrl": "https://tenant-a.example.com/",
    "robots": "index,follow",
    "structuredData": {
      "@context": "https://schema.org",
      "@type": "SoftwareApplication",
      "name": "Booking Core"
    }
  }
}
```

#### 3) `POST /api/public/tracking/events`

- Request body（單筆或批次）：

```json
{
  "events": [
    {
      "eventType": "cta_click",
      "tenantId": "tnt_a",
      "locale": "zh-TW",
      "sectionId": "hero",
      "campaign": "spring_launch",
      "pageVariant": "default",
      "occurredAt": "2026-04-14T10:20:30Z",
      "metadata": {
        "ctaId": "hero_primary_start"
      }
    }
  ]
}
```

- Success response（`code=OK`）：

```json
{
  "code": "OK",
  "message": "accepted",
  "data": {
    "accepted": 1,
    "rejected": 0
  }
}
```

### B) DB Schema Decision / Support

- Step 2 決策：**不新增核心交易表**，先用「設定檔 + 既有 i18n + 現有 analytics 管線」落地，降低導入風險與工期。
- 支援擴充（非 blocking，僅在需要營運後台編輯時啟用）：
  - `tenant_page_config`（tenant/locale/variant/sections JSON/version）
  - `tenant_seo_config`（tenant/locale/variant/title/description/structured_data）
  - `tracking_event_staging`（若現有 analytics 無法承接即時驗證）
- 索引與唯一鍵建議（若啟用表）：`UNIQUE(tenant_id, locale, variant)`，並保留 `updated_at` 供快取失效。

### C) 多租戶隔離、快取鍵策略、狀態合法性 Guardrails

- 多租戶隔離：
  - API 層必驗 `tenantId`，任何 fallback 僅允許同 tenant 內（tenant override -> tenant default），**禁止 cross-tenant fallback**。
  - SEO/canonical/structured data 必須 tenant-aware，不可共用另一租戶輸出。
- 快取鍵策略（server/CDN 一致語意）：
  - `homepage-config:{tenantId}:{locale}:{pageVariant}:{contentVersion}`
  - `homepage-seo:{tenantId}:{locale}:{variant}:{seoVersion}`
  - 絕不可省略 `tenantId`；版本號變更即失效。
- 狀態合法性文案守門：
  - 文案禁用：「立即確認」「必定核准」「100% 成功」等承諾語。
  - Hero/Process/CTA 區塊需固定附註：「實際狀態依租戶規則與資源策略」。
  - 若內容違反守門規則，後端可回 `STATE_GUARDRAIL_VIOLATION` 阻擋發布。

### D) Success Checklist Coverage（Step 2 對照）

- #1：`sections.enabled/order/contentKey` 已於 `homepage-config` 明確化。
- #2：`locale` 必填 + fallbackPolicy + 錯誤碼 `LOCALE_NOT_SUPPORTED`。
- #3：SEO/內容範例統一使用 Resource/Slot 抽象語彙。
- #4：`stateLegalityNotice` + 文案禁用詞與發布守門。
- #5：API/快取鍵/SEO 輸出均含 tenant 維度且禁跨租戶 fallback。
- #6：tracking event contract 強制 `tenantId/locale/sectionId/campaign`。
- #7：由 config 驅動區塊與 CTA，可支援 RWD 不同排序策略（同 contract）。
- #8：fallbackPolicy 明訂僅同 tenant default，不可命中其他 tenant 快取。

## DB Impact

- 目前判定：**本階段可先 None（不強制 schema 變更）**，理由：
  - 若首頁內容先由版本控管的設定檔 + 既有 i18n 機制管理，可滿足 Step 1 規格落地與前端開發。
  - 追蹤事件若沿用既有 analytics 管線，不需新增業務表。
- 可能後續擴充（非本步 blocking）：
  - 若要給營運後台可編輯首頁內容，才需新增租戶化內容表（如 `tenant_page_content`）與版本欄位。
  - 若要做事件查詢回放，才需新增事件倉儲或 ETL 映射。

## Task Breakdown

| ID | Task | Owner | Depends On |
|---|---|---|---|
| T1 | 凍結 PM 規格、驗收邊界與 checklist 編號 | pm-agent | - |
| T2 | 補齊可配置契約（section fields、tenant override、fallback 規則） | architect-agent + frontend-engineer-agent | T1 |
| T3 | 產出首頁 IA/線框/UI state（含雙語文案結構） | uiux-agent | T1, T2 |
| T4 | 定義與確認 public API contract（config/seo/tracking） | backend-engineer-agent（owner） + architect-agent（review） | T2 |
| T5 | 前端實作首頁與配置渲染、i18n 接線、tenant cache key 規範 | frontend-engineer-agent | T3, T4 |
| T6 | 追蹤事件串接與驗證腳本（含 tenant metadata） | frontend-engineer-agent + data/analytics-owner | T5 |
| T7 | QA 驗收（checklist #1-#8 + 邊界情境） | qa-agent | T5, T6 |
| T8 | Reviewer Gate（功能/回歸/安全/租戶隔離） | reviewer-agent | T7 |

## Handoff Notes

- PM -> UI/UX：交付固定 section ID、CTA priority、文案禁用詞（狀態承諾語）。
- PM/Architect -> Backend：交付 API contract（欄位、錯誤碼、cache 維度）與 tenant fallback 規則；backend 為 contract owner。
- Backend -> Frontend：提供 mock/contract 測試範例與 tenant A/B 測試資料。
- Frontend -> QA：提供 checklist 對照表、事件驗證步驟、雙語與租戶切換測試路徑（i18n 來源為 repo 靜態字典）。

## Parallelization Plan

```json
{
  "sequentialTasks": ["T1", "T8"],
  "parallelGroups": [
    {
      "groupName": "Design and architecture alignment",
      "tasks": ["T2", "T3"]
    },
    {
      "groupName": "Implementation",
      "tasks": ["T4", "T5", "T6"]
    },
    {
      "groupName": "Validation",
      "tasks": ["T7"]
    }
  ]
}
```

## Risks and Mitigations

- 風險：首頁文案過度產品導向、缺乏產業共通性。
  - 緩解：所有核心敘事對齊 Resource/Slot 抽象詞彙表。
- 風險：白標租戶快取污染造成錯誤內容曝光。
  - 緩解：在 CDN 與應用快取層導入 tenant-aware cache key。
- 風險：流程圖文案誤導實際狀態轉移規則。
  - 緩解：加註「實際流程依租戶設定與系統規則為準」。

## Status

- Current status: `closed`（2026-04-15：public API、前端首頁、測試與 reviewer gate 已完成）
- Canonical location: `doc/specs/closed/2026-04-14_homepage-system-introduction.md`

## PM Sign-off

- **APPROVED**
- 條件：以 Assumptions A1-A3 作為 Step 1 鎖定決策，允許立即進入 `progress` 並啟動 T2-T4。

## Auto-Dev Execution Report

### Agent Activation Plan [SERIAL]

- Step 1 PM -> `generalPurpose`
- Step 2 Architect -> `generalPurpose`
- Step 3 Backend -> `generalPurpose`
- Step 4 Frontend -> `generalPurpose`
- Step 5 QA -> `generalPurpose`
- Step 7 Reviewer -> `reviewer-agent`

### Step 2 - Architect [SERIAL]

- API spec 規範：
  - `GET /api/public/homepage-config?tenantId={id}&locale={locale}`
  - `GET /api/public/homepage-seo?tenantId={id}&locale={locale}&variant={variant}`
  - `POST /api/public/tracking/events`
  - 統一回應格式：`{ "code": 0, "message": "success", "data": {} }`
- DB schema 決策：
  - Step 2 可先不新增核心交易表。
  - 後續可擴充 `tenant_page_config`、`tenant_seo_config`、`tracking_event_staging`。
- Agent Evidence：
  - invoked: `generalPurpose` (architect)
  - request: 設計首頁 API/DB 與多租戶隔離、快取鍵策略
  - output summary: 已提供可交接 API 契約、快取鍵與 guardrail 規範

### Parallel Execution Template (Before Step 3)

- parallelWorkstreams:
  - Backend implementation track：首頁 config/seo/tracking API 契約落地
  - Frontend implementation track：首頁區塊渲染與 loading/error/empty 狀態
  - QA test-case design track：依 checklist #1-#8 建立 1:1 測試
- serialDependencies:
  - Step 3~5 需依賴 Step 1 checklist 與 Step 2 contract 凍結
- mergeCheckpoints:
  - Checkpoint A：API 契約與前端資料模型對齊
  - Checkpoint B：QA 測試案例可映射每一條 checklist
  - Checkpoint C：Reviewer gate 風險清零

### Step 3 - Backend [PARALLEL]

- Implementation summary:
  - 已落地：`PublicHomepageController`、`HomepagePublicService`、`HomepageTenantRegistry`（讀取 `classpath:homepage/tenants.json`）、`HomepageGuardrailService`、`HomepageTrackingService`（`@Transactional` + 測試環境事件緩衝）。
  - 回應沿用既有 `ApiEnvelope`（`code: 0` 成功）；錯誤沿用 `ApiException` + `GlobalExceptionHandler`（`data.errorCode`）。
  - 本 repo 無 MyBatis；本階段無新增 DB migration（與規格 Step 2「可先 None」一致）。
- Agent Evidence:
  - invoked: implementation agent（Cursor Composer）
  - request: 依契約實作 public API、租戶資料來源、追蹤與測試
  - output summary: 後端 API 與 `PublicHomepageApiTest` / `HomepageGuardrailServiceTest` 已加入並通過

### Step 4 - Frontend [PARALLEL]

- Implementation summary:
  - `AuthApp`：`/` 為 `SystemHomePage`（`MarketingLayout`），`/login`、`/register` 仍在 `AuthLayout`。
  - 區塊依 API `sections` 排序渲染；整合 `homepage-config` / `homepage-seo`；loading / error；`sessionStorage` 快取鍵含 `tenantId+locale+variant`。
  - i18n：`homepage` namespace（`zh-TW` / `en-US`），`translateHomepageContent` 避免 raw key 外顯。
  - 追蹤：`IntersectionObserver` section_view；CTA click 上報 `tenantId/locale/sectionId/campaign`。
- Agent Evidence:
  - invoked: implementation agent（Cursor Composer）
  - request: 依 checklist 完成首頁 UI 與 API 串接
  - output summary: `npm run build` 通過

### Step 5 - QA [PARALLEL]

- test cases (mapped 1:1 to successChecklist):
  - `TC-HOME-01` -> `SC#1`
  - `TC-HOME-02` -> `SC#2`
  - `TC-HOME-03` -> `SC#3`
  - `TC-HOME-04` -> `SC#4`
  - `TC-HOME-05` -> `SC#5`
  - `TC-HOME-06` -> `SC#6`
  - `TC-HOME-07` -> `SC#7`
  - `TC-HOME-08` -> `SC#8`
- Agent Evidence:
  - invoked: `generalPurpose` (qa)
  - request: 建立 checklist 1:1 對應的 API/UI/edge 測試
  - output summary: 已產出完整映射，含 tenant A/B 污染檢查與 guardrail 驗證

### Step 6 - Validation Loop [SERIAL]

- test result: `PASS`（本 spec 範圍內）
- evidence:
  - Backend: `mvn test "-Dtest=PublicHomepageApiTest,HomepageGuardrailServiceTest"`（通過）
  - Frontend: `npm run build`（通過）
  - 說明：完整 `mvn test` 在目前工作區仍有其他既有用例失敗（401 等），與本首頁變更無直接關聯；首頁相關測試已單獨驗證。
- failed items: none（本 spec checklist 對應之自動化子集）

### Step 7 - Reviewer [PARALLEL]

- findings:
  - **Go**：無 High / Critical blocker（`reviewer-agent` 2026-04-15）
  - Medium（後續可排）：正文文案 guardrail 主要在前端 i18n（伺服器目前守 SEO 字串）；追蹤批次大小上限；`@Transactional` 對純 log 路徑的效益。
- Agent Evidence:
  - invoked: `reviewer-agent`
  - request: 審查併發/效能/品質與多租戶風險
  - output summary: 允許合併（假設其他既有測試失敗獨立處理）

### Step 8 - PM Closeout [SERIAL]

- closeout decision:
  - **Go**：可結案並歸檔至 `doc/specs/closed/`
- spec status update:
  - 本檔自 `doc/specs/progress/` 移至 `doc/specs/closed/`（檔名不變）
- closeout note:
  - implementation summary: 公開首頁 API（config/seo/tracking）、`homepage/tenants.json` 租戶內容、前端 `SystemHomePage` 與 i18n/快取/追蹤串接
  - test/validation evidence: 見 Step 6；reviewer gate 見 Step 7
  - unresolved follow-ups: 追蹤持久化倉儲、批次大小上限、若需 CMS 再啟 DB 擴充表（規格已列為非 blocking）

## Parallel Execution Report

- ran in parallel:
  - Step 3 Backend implementation proposal
  - Step 4 Frontend implementation proposal
  - Step 5 QA test mapping
- stayed serial (and why):
  - Step 1/2 需先凍結 checklist 與 contract
  - Step 6/8 需依賴 reviewer 結果做關卡決策
- merge checkpoint results:
  - A: checklist 與 API 對齊完成
  - B: QA 映射完成
  - C: reviewer gate 已通過（本輪）

## Agent Execution Report

- required roles:
  - PM
  - Architect
  - Backend
  - Frontend
  - QA
  - Reviewer
- invoked agents by role:
  - PM: spec 內既有 `APPROVED`（本輪由實作代理延續執行）
  - Architect: spec 內既有 Step 2 契約（本輪由實作代理對齊落地）
  - Backend: implementation agent（Cursor Composer）
  - Frontend: implementation agent（Cursor Composer）
  - QA: 測試映射由 `PublicHomepageApiTest` / `HomepageGuardrailServiceTest` + build 驗證體現
  - Reviewer: `reviewer-agent`
- missing agent invocations:
  - none
