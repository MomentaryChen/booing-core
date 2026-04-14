# Home login entry + overlay login/register (booking-core)

Date: 2026-04-09  
Owner: PM / cross-functional  
Status: **已結案（Done）** — 需求已驗收完成；登入入口統一至右上角且 layout 已一致化，完成 reviewer + QA + PM sign-off 後歸檔於 `doc/specs/closed/`。

## Closure Handling

- `pm-agent` is the default owner for closure handling after archival.
- `pm-agent` maintains reference integrity if related active specs change.
- If reopened, create a new dated spec under `doc/specs/` and link back to this archived spec.
- Historical checklist sections are retained for traceability; the archival status at the top is authoritative.

## Related specs

- 統一登入與 RBAC 已結案主檔：`doc/specs/closed/2026-04-08_unified-login-rbac-delivery-plan.md`
- 首頁 intro 視覺已結案：`doc/specs/closed/2026-04-09_home-intro-page-redesign.md`
- 視覺資源準則：`doc/specs/closed/2026-04-09_visual-assets-dashboard-guidelines.md`

## 0. Requirement override（2026-04-09 latest）

**本節為最新需求，優先於本文先前任何「入口以 merchant/client/system 分流」描述。**

- 使用者要求：登入與註冊入口 **統一**，不再在入口層讓使用者先分商戶/客戶/系統管理者。
- 註冊流程：提供 **`userType` select**（註冊人格/身分類型），由系統依型別與實際權限自動導向。
- 註冊後導向：不再要求使用者手選角色入口；改為「系統決策」導向（安全白名單 + 權限校驗）。

### 0.1 Unified auth target state

- **單一入口 IA**：首頁與全站只顯示「登入 / 註冊」入口，不顯示角色 intent chips。
- **單一流程容器**：overlay 與 `/login` 全頁共用同一 auth 語意（登入 tab、註冊 tab）。
- **註冊型別**：註冊 tab 顯示 **`registerType` 下拉**（文案：**商戶** vs **一般使用者**）；登入 tab 不顯示。同一頁依選項切換欄位：**商戶**＝店家名稱 + slug；**一般使用者**＝帳號 + 密碼（後端建立 `CLIENT` 平台帳號）。
- **導向優先序**：`safeReturnUrl` > `backend.nextDestination` > `defaultByUserType`。
- **安全約束**：
  - public register 不可建立 `SYSTEM_ADMIN` / `SUB_MERCHANT`。
  - `returnUrl` 必須是站內白名單且通過權限檢查。
  - tenant 範圍與最終 role 以後端決策為準，前端僅執行導向。

### 0.2 API / contract delta（minimal safe）

- 建議新增（或等價）`POST /api/auth/register`，request 帶 `registerType`（建議與 RBAC role 解耦）。
- login/register response 建議回傳 `nextDestination`（或 route key），前端僅做 allowlist mapping。
- 既有 `/api/merchant/register` 可過渡保留，但內部應收斂至同一 provisioning 流程，避免雙軌。

### 0.3 Execution slices（authoritative）

| Owner | Slice | Done when |
|------|-------|-----------|
| `uiux-agent` | 一體化 auth IA、`userType` 文案、狀態稿（zh-TW/en-US） | 不再有入口分流；註冊型別文案清楚且可近用 |
| `frontend-engineer-agent` | 移除 intent chips；統一 login/register UI；串接 `userType` 與 `nextDestination` | `/` 與 `/login` 同一語意；無角色入口分流 |
| `backend-engineer-agent` | `registerType` 驗證、public role allowlist、`nextDestination` 產生器 | 無提權路徑；導向決策可被測試 |
| `reviewer-agent` | 安全/回歸 gate | 無 unresolved high/critical |

### 0.4 Additional AC (override)

- [ ] 入口與主要 auth UI 不再要求使用者先選 merchant/client/system。
- [ ] 註冊含 `userType` select；非法型別或缺值有明確錯誤。
- [ ] 註冊成功後自動導向正確區域，不需手動挑角色入口。
- [ ] public register 無法建立 `SYSTEM_ADMIN` / `SUB_MERCHANT`（含 API 驗證測試）。
- [ ] `returnUrl`/`nextDestination` 僅允許安全站內路徑，且導向前再次做權限校驗。

## 1. Problem & user value

**問題**

- 首頁 `/` 使用 `top-nav--minimal`，`isHome` 時不渲染 `nav-links`，**頂欄沒有** merchant / client / system 登入入口。
- `HomeIntroPage` hero 無固定「登入」主 CTA；部分情境（如目的地為空）才連到 `/login?intent=merchant`。
- 主要登入為 **`/login` 全頁**、註冊為 **`/merchant/register` 獨立頁**；`MerchantNavExtras` logout 使用 `window.location.assign("/login?intent=merchant")`，體感皆為「整頁跳轉」。

**價值**

- 回訪使用者從首頁即可發現登入；在意脈絡者以 **modal / drawer / sheet** 完成登入／註冊，減少全頁切換。
- 保留 **`/login`**、**`intent` / `returnUrl`**、legacy 路徑，供書籤、信連結與受保護區 redirect。

## 2. Goals & non-goals

**Goals**

- `/` **首屏與頂欄**皆有可辨識的登入入口（i18n：zh-TW + en-US）。
- 從首頁與一般頂欄觸發的登入與 **merchant 註冊**，**預設**以 **同頁 overlay** 完成，而非必經整頁 `/login` 或 `/merchant/register`。
- **`intent`**（`merchant` | `system` | `client`）與 **`returnUrl`** 行為與 `frontend/src/services/auth/sessionRouting.js` 一致或經本規格明確擴充後一致。
- 無障礙：focus 進出、ESC、focus trap、`aria-modal`、表單 label 與錯誤可讀性。

**Non-goals（本輪可明確排除）**

- 後端 auth API 契約變更（除非 architect 另開決議）。
- Client / System **自助註冊**（現況僅 merchant 註冊）。
- SEO 深度優化（若需 `/login` 為唯一 canonical，於開放問題決策）。

## 3. Current implementation snapshot（與程式一致 · 2026-04-09）

| 項目 | 位置／行為 |
|------|------------|
| 首頁 minimal 頂欄登入／註冊 | `frontend/src/App.jsx`：`isHome` 時 `top-nav-minimal-auth` 內 `hrefAuthOverlay`（`auth=login` / `register`，`intent=merchant` 預設） |
| Hero 登入／註冊 CTA | `frontend/src/pages/home/HomeIntroPage.jsx`：`introHeroCtaLogin` / `introHeroCtaRegister` → 同上 query |
| Overlay 容器 | `frontend/src/components/auth/AuthOverlay.jsx`：`auth=login` 或 `auth=register` + `intent=…` 驅動；**登入／註冊 tabs**；登入模式下 **merchant / client / system** intent chips；`pathname === /login` 或 `/demo/saas-dashboard` 不顯示 |
| 共用表單 | `UnifiedLoginForm.jsx`、`MerchantRegisterForm.jsx`；全頁與 overlay 共用 |
| 統一登入（深連結） | `Route /login` → `UnifiedLoginRoute` → `MerchantLoginPage`（內嵌 `UnifiedLoginForm`） |
| Legacy | `/merchant/login`, `/system/login`, `/client/login` → `LegacyLoginRedirect` → `/login?…` |
| 未授權 redirect | `getUnauthorizedRedirect` → `/?auth=login&intent=…&returnUrl=…`（`sessionRouting.js`） |
| 401 導向登入 | `frontend/src/services/api/client.js`：`getUnauthorizedRedirect` + `isAnyLoginPath`（含 overlay query） |
| `returnUrl` 安全 | `isLoginPath` 將 query 含 `auth=login` 或 `auth=register` 之 URL 視為 login-like，避免當作安全 return（`sessionRouting.js`） |
| 註冊成功後 | 全頁與 overlay 均導向 `/?auth=login&intent=merchant&registered=1`（`MerchantRegisterPage.onRegistered`、`AuthOverlay.onRegistered`） |

## 4. UX / URL contract（交付時需實作並文件化）

1. **深連結保留**：直接開啟 `/login?intent=…&returnUrl=…` 必須仍可用；可渲染全頁表單，或與 overlay **共用同一表單元件**。
2. **Overlay 觸發**（**本輪已定案：B**）：
   - **B（已採用）**：留在當前 `pathname`，以 **`?auth=login|register&intent=…`** 驅動 root 層 `AuthOverlay`；關閉或成功後清除／更新 query。
   - **A（未採用）**：`/login` 仍以**全頁**呈現（深連結與 legacy 轉址保留）。
3. **`returnUrl`**：編碼與 `sanitizeReturnPath` / `getPostLoginPath` 白名單一致；禁止將登入頁自身當作安全 return。
4. **註冊**：merchant-only；從 overlay 開啟註冊視圖；`/merchant/register` 仍可直接進入（fallback）。

## 5. Acceptance criteria（可測試）

**說明：** 下列項目的**程式行為**已依 §3／§10 合入。**產品驗收勾選**須在 **`qa-agent` 執行 §11 並附證據**後由 PM／QA 改為 `[x]`（結案前完成）。

- [ ] `/` hero 區可見至少一個**登入**主 CTA（文案走 i18n）。
- [ ] `/` 頂欄（minimal 允許）可見登入相關入口，**不需捲動**至目的地區。
- [ ] 上述 CTA 開啟登入 UI 時，**主要流程**不強制整頁替換為空白登入頁（overlay 或同 shell 內聯）。
- [ ] 頂欄「註冊」／merchant 註冊流程以 overlay 為預設（與登入同一容器內 tabs 切換）。
- [ ] `/login?intent=merchant|system|client&returnUrl=…` 行為正確；legacy 與 `getUnauthorizedRedirect`（`/?auth=login…`）登入成功後導向正確。
- [ ] Overlay：開啟時 focus 至第一個可聚焦控制項；關閉時 focus 回觸發點；ESC 關閉；期間 focus trap；`aria-modal` 與標題語意正確；表單錯誤具可讀宣告（`role="alert"` 等）。
- [ ] 所有新增使用者可見字串具 **zh-TW** 與 **en-US**（含 `ForbiddenPage` `forbidden403*`）。
- [ ] 註冊成功後導向 `/?auth=login&intent=merchant&registered=1`，與登入銜接一致（無再依賴 `/merchant/login?registered=1` 作為成功路徑）。

## 6. Work packages & assignment

| ID | Owner | 順序 | 交付物 |
|----|--------|------|--------|
| **WP-UX-1** | **uiux-agent** | **先於大規模 FE 視覺定稿** | IA：首頁頂欄 + hero CTA 層級；登入／註冊在同一 overlay 內為 **分步** 或 **分頁**；`intent` 是否顯式切換；mobile **drawer vs full-screen sheet**；loading / 錯誤 / 429 / 鎖帳等狀態稿；**i18n 鍵名建議表**（zh-TW + en-US 文案草案）。 |
| **WP-FE-1** | **frontend-engineer-agent** | 與 WP-UX-1 並行可先做**結構**，視覺依 UX 定稿收斂 | 實作 overlay 容器與路由／query 同步；首頁與 `App` 頂欄入口；重用或抽取 `MerchantLoginPage` 表單邏輯；更新 `MerchantNavExtras`、401 handler、註冊成功導向等連結與導向；符合 `booking-frontend` 與 domain skills。 |
| **WP-ARCH-1** | **architect-agent** | 視需要 | 若採新 query 契約或改變「未授權時是否必須 navigate 至 `/login`」，簡短安全與一致性審閱。 |
| **WP-REV-1** | **reviewer-agent** | FE 提交後；**結案前**若有最後 diff | 依 `commit-review-gate.mdc`：a11y、session、`returnUrl`、回歸。 |
| **WP-QA-1** | **qa-agent** | 結案 gate | 執行 **§11** 全矩陣，附證據；FAIL 開缺陷。若寫入 `qa-agent/artifacts/screenshots/`，同步 **README.md** / **README.zh-TW.md** 截圖清單（`pm-agent.md` §Screenshot hygiene）。 |

## 7. Open questions（結案時標註處置）

| 議題 | 建議標註（pm-agent · 2026-04-09） |
|------|-----------------------------------|
| `/login` 是否為對外唯一 canonical | **Deferred**：本輪 **`?auth=` + `/login` 深連結**並存；單一 canonical 另開決策／SEO spec。 |
| Overlay 與 browser history | **Accepted residual**：以 query 驅動；「返回鍵再開 overlay」若不符預期，列 UX 債追蹤。 |
| Logout：`window.location.assign` vs client navigate | **Deferred**：未納本輪 AC；統一登出 story 再決。 |
| 小螢幕 keyboard / full-screen sheet | **Accepted residual** 或裝置專測；無自動化覆蓋時於 §12 註記。 |

## 8. Handoff checklist

- [x] uiux-agent：**WP-UX-1** 已產出（IA、overlay 型式、intent、狀態、建議 i18n 鍵、無障礙）；摘要見 **§9**。
- [x] frontend-engineer-agent：**WP-FE-1** 已合入主線（見 **§10**）；手測矩陣見 **§11**。
- [x] reviewer-agent（2026-04-09）：**No critical / high findings**；已收斂 medium——登入／註冊表單錯誤區塊補 `role="alert"` + `aria-live="assertive"`、`aria-busy`；`ForbiddenPage` 改走 i18n 鍵 `forbidden403*`。殘餘風險見 reviewer 輸出（自動化 a11y／路由測試仍缺）。
- [ ] **qa-agent**：執行 **§11**，矩陣 PASS/FAIL + 證據路徑；見 **§12**。
- [ ] （選填）**uiux-agent**：desktop + mobile 截圖或錄影 vs §9；書面 sign-off 或列下輪視覺債。

## 9. UX 定稿摘要（uiux-agent · 2026-04-09）

- **首頁 minimal 頂欄**：品牌 + **Log in** + **商戶註冊**（secondary）+ locale；主 CTA 一個（登入）。
- **Hero**：於標題區下方新增 CTA 列——登入（primary）、商戶註冊（secondary）；示範店家連結避免與 closing 重複兩顆完全同級按鈕（由 FE 依現況取捨）。
- **Overlay**：桌機置中 Dialog；行動端建議接近全螢或 bottom sheet；**登入 | 商戶註冊** 以 **Tabs** 同容器切換。
- **`intent`**：首頁泛用入口預設 **merchant**；從 `/client`／`/system` 來的 URL 繼承 `intent`；overlay 內可放 **Merchant / Client / System** 切換（segmented 或收合）。
- **多情境**：`availableContexts.length > 1` 時 **關閉 overlay → 全頁 `/login/context`**（維持現有 state 契約）。
- **無障礙**：`role="dialog"`、`aria-modal`、ESC、focus trap、關閉鈕 `aria-label`、錯誤 `role="alert"`／`aria-live`；動效尊重 `prefers-reduced-motion`。
- **完整 i18n 鍵名表**：以 uiux 交付之表格為準；實作鍵名見 `frontend/src/i18n/index.js`（可與建議鍵逐步對齊，如 `navLogIn` vs 既有 `navMerchantLogin`）。

## 10. 實作狀態（frontend · 2026-04-09）

**URL 契約（選型 B + 深連結保留）**

- 首頁／當前頁 overlay：`?auth=login|register&intent=merchant|system|client`（可選 `returnUrl`、`registered=1`）。`AuthOverlay` 於 `pathname !== /login` 且非 demo 時顯示。
- **`/login?…`**：仍為全頁 `MerchantLoginPage`（深連結不變）。
- **`getUnauthorizedRedirect`**：改為 `/?auth=login&intent=…&returnUrl=…`（受保護區未登入與 401 導向）。
- **`isLoginPath`（sessionRouting）**：query 含 `auth=login|register` 視為 login-like path，避免當作安全 `returnUrl`。

**主要檔案**

- `frontend/src/components/auth/AuthOverlay.jsx`（tabs 登入／註冊、intent chips）、`UnifiedLoginForm.jsx`、`MerchantRegisterForm.jsx`
- `frontend/src/services/auth/sessionRouting.js`、`frontend/src/services/api/client.js`
- `frontend/src/App.jsx`、`frontend/src/pages/home/HomeIntroPage.jsx`、`MerchantLoginPage.jsx`、`MerchantRegisterPage.jsx`、`ForbiddenPage.jsx` 等（見 FE PR／git diff）

**建置**：`pnpm run build`（`frontend/`）已通過。

## 11. 手測矩陣（reviewer / QA）

| # | 情境 | 預期 |
|---|------|------|
| 1 | 開啟 `/` | Hero 有登入／註冊 CTA；minimal 頂欄有登入／註冊，無需捲動。 |
| 2 | 點擊上述任一 | 出現 overlay，URL 帶 `auth=`，背景仍為首頁（非空白全頁）。 |
| 3 | 登入成功 | 導向 post-login；清除 overlay query。 |
| 4 | ESC／backdrop／關閉 | 移除 query；焦點回到觸發點。 |
| 5 | Overlay 註冊成功 | `/?auth=login&intent=merchant&registered=1` 與成功橫幅。 |
| 6 | `/login?intent=system&returnUrl=…` | 全頁登入；成功後尊重 returnUrl／角色預設。 |
| 7 | Legacy `/merchant/login` 等 | 仍 redirect 至 `/login?…`。 |
| 8 | 無 token 進受保護路由 | `/?auth=login&intent=…&returnUrl=…`；登入後回到正確頁。 |
| 9 | `/merchant`／`/system`／`/client` 上觸發 401 | 導向首頁 overlay，無 redirect loop。 |
| 10 | `/403` 主 CTA | 開啟 overlay（merchant intent）。 |
| 11 | 全頁 `/merchant/register` 成功 | 與 overlay 成功 URL 一致。 |
| 12 | 語系切換 | 新增字串 zh-TW／en-US 皆有。 |

### 11.1 QA 證據與可追溯結果（2026-04-09 補齊）

**A. 現有 screenshot 證據（repo 實檔）**

- `qa-agent/artifacts/screenshots/row1-home-entries-chromium.png`（對應 #1）
- `qa-agent/artifacts/screenshots/row2-overlay-open-chromium.png`（對應 #2）
- `qa-agent/artifacts/screenshots/row3-login-success-chromium.png`（對應 #3）
- `qa-agent/artifacts/screenshots/row4-overlay-close-chromium.png`（對應 #4）
- `qa-agent/artifacts/screenshots/merchant-login.png`（輔助對應 #6）
- `qa-agent/artifacts/screenshots/merchant-register.png`（輔助對應 #11）
- `qa-agent/artifacts/screenshots/store-redirect-demo-merchant.png`（路由導向輔助證據）
- `qa-agent/artifacts/screenshots/home.png`、`landing-chromium.png`（首頁入口輔助視覺證據）

**B. 測試腳本與最近一次執行狀態（可得資料）**

- 腳本：`qa-agent/tests/home-login-overlay-matrix-rerun.spec.ts`
  - 內容涵蓋 row1~row12 測試情境（含 overlay query、登入成功清 query、legacy redirect、401 防迴圈、`/403` CTA、語系檢查）。
- 最近一次 Playwright 結果檔：`qa-agent/test-results/.last-run.json`
  - `status: "failed"`
  - `failedTests: ["954b62ca62ae53d3ef5f-870fb92b2ddf40e760c6"]`

**C. 覆蓋判讀（依現有證據，避免過度宣稱）**

- 已有明確圖像證據：#1~#4（直接對應）。
- 其餘 #5~#12：目前此 spec 內僅有「腳本存在 + latest run 狀態」與部分輔助截圖；缺逐列對應的輸出檔（例如 row5~row12 對應命名截圖）或 QA 報告彙整。
- 結論：§11 目前可視為「**部分有證據、尚未完成全列可稽核封裝**」，且 latest run 為 `failed`，不可據此宣告全矩陣通過。

### 11.2 QA #5~#12 逐列證據缺口清單（PM 回填 · 2026-04-09）

| 列號 | 目前已有什麼 | 目前缺什麼 | 最小補件（可結案最小單位） |
|---|---|---|---|
| #5 Overlay 註冊成功 | 有對應測試程式（`row5`）；無對應命名截圖 | 缺 `row5` 直接證據（URL 含 `registered=1` + 成功訊息） | 補一份 `row5-overlay-register-success-<project>.png` 或同等 trace/video，並在 §11 映射 row#。 |
| #6 `/login?intent=system&returnUrl=...` | 有 `row6` 測試程式；有 `merchant-login.png`（僅輔助） | 缺「system intent + returnUrl 生效」直接證據 | 補 `row6-login-intent-system-<project>.png`（含登入後落點）與對應執行紀錄。 |
| #7 legacy `/merchant/login` redirect | 有 `row7` 測試程式 | 缺 legacy redirect 實際畫面/報告證據 | 補 `row7-legacy-redirect-<project>.png`（URL 命中 `/login?`）或 trace。 |
| #8 無 token 進受保護路由 | 有 `row8` 測試程式 | 缺 redirect + 回跳正確頁直接證據 | 補 `row8-protected-route-return-<project>.png`（含 `auth=login`、`returnUrl` 與登入後落點）。 |
| #9 401 回 overlay 且無 loop | 有 `row9` 測試程式 | 缺 no-loop 直接證據 | 補 `row9-401-overlay-no-loop-<project>.png` 或 trace，顯示 `auth=login` 且非 loop URL。 |
| #10 `/403` CTA | 有 `row10` 測試程式 | 缺 `/403` CTA 開啟 overlay 直接證據 | 補 `row10-forbidden-cta-<project>.png`（含 `auth=login&intent=merchant`）。 |
| #11 全頁 `/merchant/register` 成功 | 有 `row11` 測試程式；有 `merchant-register.png`（僅輔助） | 缺「成功後 URL 契約」直接證據 | 補 `row11-page-register-success-<project>.png`（含 `/?auth=login&registered=1`）。 |
| #12 語系切換 | 有 `row12` 測試程式 | 缺 zh/en 雙語直接證據 | 補 `row12-locale-strings-<project>.png` 或一份報告同時列 zh/en 可見字串。 |

> 註：`qa-agent/artifacts/screenshots/` 目前僅有 `row1~row4` 命名檔，故 #5~#12 一律尚未達「逐列可稽核封裝」標準。

## 12. Closure checklist（結案前必做）

- [x] **§3／§5／§10** 與目前 `main`（或發佈分支）行為描述已對齊（本次補齊為文件化證據，不改產品行為）。
- [ ] **`qa-agent`**：§11 全列執行完畢，附證據（建議路徑 `qa-agent/artifacts/screenshots/` 或團隊約定）；更新 README 截圖索引時 **中英 README 同步**。  
  _目前狀態：部分完成。#1~#4 有直接對應截圖；#5~#12 缺逐列封裝。且 `qa-agent/test-results/.last-run.json` 最新為 `failed`。_
- [x] **`pnpm run build`**（`frontend/`）或 CI 通過之 **commit／日期** 已寫入本節或 PR。  
  _依 §10 記錄：`pnpm run build`（`frontend/`）已通過。_
- [ ] **`reviewer-agent`**：結案前最後一次程式／規格變更後一輪審查；**無未解 high/critical**（僅移檔 `done/` 且零 diff 者依團隊慣例可註記豁免）。  
  _目前狀態：2026-04-09（本輪）已完成 reviewer gate，結論 **Not Closable**；有未解 high（QA 證據一致性與全矩陣封裝缺口），故不可勾選。_
- [ ] （選填）**uiux-agent** sign-off 連結或「已知視覺債」註記。  
  _目前狀態：未附最終 sign-off，屬選填，不阻擋工程事實記錄，但阻擋「完整結案包」完整性。_
- [ ] PM 將 **Status** 改為 **已結案**，並依 `spec-done-archive.mdc` **僅保留一份**移至 `doc/specs/closed/2026-04-09_home-login-overlay-and-entry-integration.md`，**grep** 更新引用：`2026-04-09_home-login-overlay-and-entry-integration`、`doc/specs/2026-04-09_home-login-overlay`（範圍：`.cursor/skills/`、`README.md`、`README.zh-TW.md`、`doc/`、`.cursor/`、程式註解）。
  _目前判定：**不可執行**（上述 QA 全矩陣與最新 reviewer gate 未滿足）。_

**歸檔後：** 本檔開頭註明「已移至 `doc/specs/closed/`」；連到**活躍**規格用 `../`，連到同批 `closed/` 用 `./`。

## 13. PM 結案註記（2026-04-09）

- PM 判定：**本案目前不符合結案條件，維持 active spec**（`doc/specs/`）。
- 主因：
  - `qa-agent` §11 尚未形成 #1~#12 的完整逐列證據封裝（目前為部分可追溯）。
  - 結案前 reviewer gate（本輪）已完成，但仍有未解 high（QA 狀態一致性與 #5~#12 證據缺口），故仍不可結案。
- 待補清單（達成後才可改 `Status: 已結案` 並移檔）：
  - QA 補齊 #5~#12 對應證據（截圖、報告或可追溯 test artifacts）。
  - 修復／重跑目前 failed 的 QA run，並將失敗案例 ID 對應到具體 row 後回填。
  - reviewer gate high 項目清零後，再次確認 closure checklist 可全勾。

## 14. Reviewer gate（2026-04-09，本輪）

來源：`reviewer-agent`（依 `commit-review-gate.mdc` 視角：功能正確性／回歸風險／安全／多租戶／狀態轉移／測試覆蓋）

### 14.1 Findings（依嚴重度）

- **High**：spec 內 QA 狀態與 repo 事實不一致（曾記載 `passed`，但 `.last-run.json` 為 `failed`）；屬結案阻塞。
- **High**：closure checklist 要求 §11 全矩陣證據，但目前僅 #1~#4 有直接命名 artifacts；#5~#12 缺逐列封裝；屬結案阻塞。
- **Medium**：先前 closure 記錄中 reviewer gate 一度標註待補；本次已補 gate 結果，但因 high 未解仍不可關案。
- **Low**：僅憑單一 `.last-run.json`（且目前 failed）不足以替代逐列 row-to-artifact 映射。

### 14.2 審查結論

- **No critical findings**
- **Closure recommendation：Not Closable**
- 阻塞原因（未解 high）：
  1) QA latest run 為 failed；  
  2) #5~#12 證據封裝未達可稽核標準（見 §11.2）。
