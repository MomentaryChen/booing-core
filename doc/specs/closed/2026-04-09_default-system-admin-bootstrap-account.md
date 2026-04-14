# Default platform bootstrap: SYSTEM_ADMIN, merchant, and merchant user (booking-core)

Date: 2026-04-09  
Owner: PM Agent  
Status: Done (archived)

## 1) Problem statement

新環境初始化時，平台需要：

1. **可登入的 `SYSTEM_ADMIN`**，以完成系統級設定與權限管理。  
2. **至少一筆 `merchants` 資料**（預設商家／示範租戶），讓 `/api/merchant/**`、店家範圍資料與 `/api/client/...` 依 slug 的公開流程有可預期的起點。  
3. **至少一筆與該商家綁定的 `platform_users`（建議角色 `MERCHANT`）**，以便用 JWT 登入後台、驗收 RBAC 與 `merchant_id` 範圍。

若僅有 admin 而無商家與商家帳號，會阻塞商家後台與多租戶示範；若僅靠 dev profile 的 `devUsers` 而 production 無對應流程，則環境落差過大。需求是提供 **可驗收、可重複部署、production-safe** 的預設種子與 bootstrap（含 idempotent 與密碼治理）。

## 2) Scope / out of scope

**In scope**

- 在既有 **`merchants`**、**`platform_users`**、**RBAC**（`rbac_*` / `platform_user_rbac_bindings`）下，定義並實作：
  - 預設 **SYSTEM_ADMIN** 帳號（與原 spec 一致）。
  - 預設 **一筆 merchant**（固定或可設定之 `name` / `slug`，slug 全環境唯一）。
  - 預設 **一筆 platform user**（建議 `MERCHANT`，`merchant_id` 指向上述 merchant），含正確 RBAC 綁定。
- Flyway（結構／安全種子若需要）+ 啟動時 **條件式** bootstrap（idempotent）。
- 明確區分 **dev/local** 與 **production**（安全預設、fail-fast、無 repo 內明文 production 密碼）。
- 密碼政策、輪替、停用指引（runbook 級別），三類帳號皆適用。

**Out of scope**

- 不新增 `/admin` 路由（仍使用 `/system` 與 `/api/system/*`）。
- 不重做 IAM/SSO。
- 不把任何 **production** 預設密碼寫入 migration 或 git。
- 不要求一次種子多名子帳號（`SUB_MERCHANT` 可列為後續迭代）；本 spec 以「一個預設商家 + 一個預設商家主帳」為最小交付。

## 3) Proposed behavior (dev vs prod)

### Dev / Local（快速啟動優先，可關閉）

- 可啟用統一或分項 bootstrap 開關（見 §6），例如：
  - `booking.platform.auth.bootstrap-system-admin.*`
  - `booking.platform.auth.bootstrap-default-merchant.*`
  - `booking.platform.auth.bootstrap-default-merchant-user.*`
- **預設 merchant**：若啟用且尚無約定 slug（或約定名稱）之商家，則建立一筆（行為對齊現有 `DevBootstrap` 之「Demo Merchant」/`demo-merchant` 意圖，但須 **idempotent**，不可每次啟動重複 insert 失敗或產生重複 slug）。
- **預設 MERCHANT user**：若啟用且該 username 不存在，則建立 `platform_users` 一筆，`merchant_id` 指向上述 merchant，角色 `MERCHANT`，並具備與現行 RBAC 一致的 **ACTIVE binding**（`merchant.portal.access`、`me.navigation.read` 等，與 `V4__rbac_core.sql` 中 `MERCHANT` 一致）。
- **SYSTEM_ADMIN**：同原 spec——首次建立、重啟 idempotent、不無條件重設密碼。
- 可沿用 `application-dev.yml` 的便捷預設；明文密碼僅限 **local/dev** profile 且文件需標示風險。

### Production（安全優先）

- 各項 bootstrap **預設關閉**；僅在明確開啟且必要 secret／識別欄位完整時執行。
- **Merchant**：可由 migration 插入「零密碼」metadata 列，或由 bootstrap 依設定建立；**slug 必須唯一**，與現有 `uk_merchants_slug` 一致。
- **MERCHANT user**：密碼僅能來自 env/secret，以 **hash** 寫入 `platform_users`；缺值且開關為 on 時 **fail fast**。
- **SYSTEM_ADMIN**：同原 spec（env/secret、fail-fast、首登輪替建議）。

## 4) Acceptance criteria checklist

**SYSTEM_ADMIN（沿用並保留）**

- [x] 啟用且 env 完整時，首次啟動建立「且僅一個」SYSTEM_ADMIN 與正確 RBAC 綁定；重啟 idempotent；prod 缺 secret 時 fail fast；密碼 hash 儲存；可驗證停用／輪替路徑。

**Default merchant**

- [x] 啟用 bootstrap 時，環境中會存在 **一筆**約定之預設 `merchants`（約定 `slug`／`name` 可設定，預設值需在 spec 或設定文件中載明）。
- [x] 重複啟動 **不** 產生第二筆相同 slug、不因唯一鍵拋錯中斷（idempotent：已存在則 skip 或 no-op）。
- [x] 與既有 `merchant_id` FK（`platform_users`、`platform_user_rbac_bindings`、業務表）相容。

**Default merchant user（platform user）**

- [x] 啟用時建立（或確保存在）一筆 `platform_users`：`role = MERCHANT`（或團隊明定之等同角色）、`merchant_id` 指向預設 merchant。
- [x] 具 **ACTIVE** 之 `platform_user_rbac_bindings`，有效 permission 與現行 `MERCHANT` 角色 baseline 一致（可登入 `/api/merchant/**` 範圍內合法操作）。
- [x] 密碼不以明文進 migration；prod 必須 env/secret；重啟不覆寫既有密碼（除非另有顯式「強制重置」開關且預設關閉）。

**整體**

- [x] Flyway migration 位於 `backend/src/main/resources/db/migration`，**不含** plaintext production secret。
- [x] 測試覆蓋：admin / merchant / user 三者 happy path、idempotency、prod fail-fast（適用項）、merchant slug 唯一性、RBAC 綁定存在性。

## 5) Risks and mitigations

- **固定 demo slug 與公開 storefront**  
  **緩解：** 文件標示僅供示範；prod 可改 slug 或關閉公開 client 入口至該 slug；考慮 `active=false` 預設商家列（若產品允許）。

- **預設 MERCHANT 與 SYSTEM_ADMIN 權限混淆**  
  **緩解：** 角色與 permission 嚴格分離；MERCHANT 不得具 `system.*` write 除非明確授予（本 spec 不授予）。

- **其餘**同原 spec：外洩、重啟重置、長期未治理、繞過 RBAC——以 env-only secret、idempotent 實作、輪替/停用 runbook、僅綁 catalog 角色等方式緩解。

## 6) Minimal delivery plan (with suggested names)

1. **DB migration (DBA + Backend)**  
   - 建議延續或拆分版本號（與現有 V1–V6 銜接）：例如 `V7__bootstrap_platform_defaults.sql`（或拆分 V7 admin seed、V8 merchant metadata，由團隊選擇）。  
   - 內容：僅 **無 secret** 之結構/安全種子（若需要固定 rbac_role id 則需與既有 migration 相容）；**避免**在 SQL 中寫 dev 密碼。

2. **Bootstrap config + initializer (Backend + DevOps)**  
   - `booking.platform.auth.bootstrap-system-admin.*`（原 spec）。  
   - 建議新增：`booking.platform.auth.bootstrap-default-merchant.enabled|name|slug`  
   - 建議新增：`booking.platform.auth.bootstrap-default-merchant-user.enabled|username|password`（password 僅 env）  
   - 實作可合併為單一 `PlatformBootstrapInitializer` 或分類別，但 **執行順序**須保證：merchant 先於 MERCHANT user；RBAC binding 在 user persist 之後。

3. **與現有 `DevBootstrap` 對齊**  
   - `DevBootstrap`（`@Profile("dev")`）目前已 best-effort 建立 Demo Merchant 並 seed `devUsers`。交付時應 **二選一或明確分工**：避免重複邏輯與雙重建立；建議以「統一 bootstrap 模組 + dev profile 僅覆寫預設值」為目標。

4. **Ops & QA & Reviewer**  
   - DevOps：prod secret 清單（admin + merchant user 至少兩組 secret）、fail-fast 設定範例。  
   - QA：驗證預設 slug 可從 client API 讀取、MERCHANT 可登入且 `assertMerchantScope` 正確。  
   - Reviewer：無 hardcoded prod 密碼、無權限繞過、migration 可重跑。

## Affected areas and handoff slices

- **Backend（auth、`/api/merchant`、`/api/system`、bootstrap）**：`backend-engineer-agent`  
- **Database migration**：`dba-agent`  
- **Environment / secrets**：`devops-agent`  
- **驗收與迴歸**：`qa-agent`  
- **合併前審查**：`reviewer-agent`  
- **商家／客戶端示範路徑**：`merchant-agent`、`client-agent`（文件與 demo slug 一致性）

## 7) Implementation delta (finalized)

### Completed implementation

- 已整合統一 bootstrap 流程：`PlatformBootstrapRunner` + `PlatformBootstrapService`，順序為 merchant -> merchant user -> system admin。
- 三項 bootstrap 設定已落地：`bootstrap-system-admin`、`bootstrap-default-merchant`、`bootstrap-default-merchant-user`（dev/prod 分流）。
- `PlatformBootstrapValidator` 已提供 fail-fast：非 dev 的敏感設定/不合法設定會阻擋啟動。
- 已限制 dev credential logging 僅可於 dev 使用；非 dev 開啟時拒絕啟動。
- RBAC 權限與 binding 已對齊，並補強 fail-closed 授權路徑與 method-level `@PreAuthorize`。
- 已補齊驗收關鍵測試：idempotency、prod-like fail-fast、system endpoint 403/200 邊界、logging 行為、bootstrap 帳號 lockout/rate-limit。

### Verification artifacts

- Backend tests (targeted + full suite) 已通過，涵蓋本 spec AT matrix 的關鍵路徑。
- `README.md` 已補齊 bootstrap env/secret matrix、owner、one-time bootstrap + rollback window runbook、staging 契約（prod-equivalent）。
- Reviewer gate 最終無 critical/high 未解風險，可進入規格結案。

## 8) Prioritized task breakdown

| Priority | Owner | Task slice | Deliverable |
|---|---|---|---|
| P0 | `backend-engineer-agent` | 統一 bootstrap 流程與順序（merchant -> merchant user -> bindings；admin 一致路徑） | 單一、可預測 initializer/service |
| P0 | `backend-engineer-agent` | 補齊 bootstrap 設定契約與啟動驗證 | typed config + fail-fast 驗證 |
| P0 | `dba-agent` | 確認 Flyway seed 策略（無 production 明文 secret）與重跑安全 | migration 調整計畫與風險說明 |
| P1 | `backend-engineer-agent` | 以 `slug` / `username` 實作 ensure/upsert 型 idempotent | 重啟/重部署無重複資料 |
| P1 | `devops-agent` | 定義 dev/staging/prod 的 env/secret 契約與預設值策略 | 部署模板與 runbook |
| P1 | `qa-agent` | 執行驗收矩陣（見 §9） | 測試證據（API、log、DB assertions） |
| P2 | `reviewer-agent` | 安全與回歸 gate | findings-first 審查，high/critical 未解不得合併 |

## 9) Acceptance test matrix (API / behavior)

| ID | Environment | Preconditions | Steps | Expected |
|---|---|---|---|---|
| AT-01 | Dev | bootstrap 啟用且 dev defaults 完整 | 啟動兩次 | default merchant / merchant user / system admin 各存在一筆；第二次 no-op |
| AT-02 | Dev | default merchant user 存在 | `POST /api/auth/login`，再呼叫代表性 `/api/merchant/**` | 登入成功、可用 merchant 權限、merchant scope 正確 |
| AT-03 | Dev | system admin 存在 | admin token 呼叫 `/api/system/**`；merchant token 呼叫同 endpoint | admin 可通過，merchant 403 |
| AT-04 | Staging/Prod-like | bootstrap=ON 但必要 secret 缺失 | 啟動服務 | 啟動 fail-fast；不得留下半套不安全狀態 |
| AT-05 | Dev/Staging | credential logging=OFF | 啟動服務 | 不出現明文帳密 |
| AT-06 | Dev only | credential logging=ON 且 dev profile | 啟動服務 | 出現明文帳密與 dev-only 警示 |
| AT-07 | Staging/Prod | credential logging 誤開 | 啟動服務 | 不可輸出明文（強制 suppress 或阻擋啟動） |
| AT-08 | Dev/Staging | 既有資料已存在 | 重啟/重部署 | 不重設密碼、不重複建帳 |
| AT-09 | Dev/Staging | login 防護開啟 | 針對 bootstrap 帳號反覆錯密碼 | rate-limit/lockout 與一般帳號一致 |
| AT-10 | Dev/Staging | RBAC seed 完整 | 透過現有 me/auth 能力檢查權限 | MERCHANT baseline 存在，無 system write 超權 |

## 10) Release checklist (dev -> staging -> prod)

### Dev

- [x] 啟用 bootstrap 以便本機快速啟動。
- [x] `booking.platform.auth.log-dev-bootstrap-credentials` 預設關閉；只在排錯時短暫開啟。
- [x] 通過 AT-01 / 02 / 03 / 05 / 06 / 09。

### Staging

- [x] admin 與 merchant-user 密碼均由 secret manager/env 注入。
- [x] 部署兩次驗證 idempotent。
- [x] 通過 AT-03 / 04 / 05 / 07 / 08 / 10。
- [x] 安全 gate：證明 logs 無明文憑證。

### Production

- [x] bootstrap 一次性策略與 rollback window 有明確 owner。
- [x] repo / migration / image / config 無 production 明文密碼。
- [x] credential logging 仍為 dev-only opt-in 且有環境護欄。
- [x] post-deploy smoke：admin 與 merchant 登入 + scope 驗證。
- [x] `reviewer-agent` 無未解 high/critical findings。

## 11) Definition of done and blockers

### Definition of done (DoD)

- [x] bootstrap 行為可預測、idempotent、且符合 dev/prod 安全分流。
- [x] default `SYSTEM_ADMIN` + default merchant + default merchant user 在啟用時皆可正確建置。
- [x] default merchant user 與 merchant 綁定正確且 RBAC 為 `MERCHANT` baseline。
- [x] bootstrap 開關開啟但必要 secret 缺失時，確實 fail-fast。
- [x] QA 矩陣於 dev/staging 完成並有證據。
- [x] `reviewer-agent` 無未解 high/critical 問題。
- [x] ops 文件包含 env vars、secret owner、回退流程。

### Current blockers / risks

- [x] `DevBootstrap` 與新 bootstrap 雙軌問題已收斂為統一 bootstrap 主責流程。
- [x] credential logging 已以 profile guard + validator 保證不外溢到非 dev 環境。
- [x] RBAC binding 來源已與 migration/catalog 對齊，缺失時 fail-fast。
- [x] migration 與 bootstrap 執行順序已驗證，並補齊 runbook 操作窗口與回退流程。
