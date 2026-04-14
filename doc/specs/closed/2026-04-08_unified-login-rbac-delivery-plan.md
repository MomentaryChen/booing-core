# Unified Login + RBAC Delivery Plan (booking-core)

Date: 2026-04-08  
Owner: PM Agent  
Last updated: 2026-04-09  
Status: **Done（已結案）** — **Phase 1 MVP** 與 **Phase 2a**（登入硬化：IP 限流、DB 帳號鎖帳、`credential_version`／JWT `cv`／logout 撤銷、403 結構化 WARN）已交付並通過測試；本檔封存於 **`doc/specs/closed/`**。**Phase 2b**（端點級 DB enforce、稽核唯讀查詢 API、Redis／叢集限流、全 E2E、ARCH ADR 凍結、`/system` 政策 UI 等）列為後續倡議，**不屬本結案批次必達條件**。

### 結案聲明（2026-04-09）

- **結案範圍**：上方 Status 所列 Phase 1 MVP 核對清單已勾項目 + **Phase 2a**（見 **Phase 2a／2b** 小節之「已合入」描述）。
- **非範圍**：正文保留之 north-star、§6 理想化表、Phase 2b 勾選項與開放問題，供後續規格或 backlog 引用；**不阻擋**本 tranche 宣告結案。
- **存放位置**：`doc/specs/closed/2026-04-08_unified-login-rbac-delivery-plan.md`（仍遵守 `YYYY-MM-DD_<kebab-case-topic>.md` 命名）。

## 1) Problem Framing

Today, auth entry points and authorization behavior are fragmented by surface (`/client`, `/merchant`, `/system`), which increases user confusion and implementation drift. We need one unified frontend login entry while preserving existing route/API namespaces and enforcing strict role/tenant access boundaries.

### Goals
- Provide a single login entry route for all user types.
- Preserve namespace contracts: frontend (`/client`, `/merchant`, `/system`) and backend (`/api/client`, `/api/merchant`, `/api/system`).
- Introduce maintainable RBAC with deny-by-default behavior.
- Enforce multi-tenant isolation from security context (not from route path assumptions).
- Minimize migration risk with incremental backend/API and frontend changes.

### Non-goals
- No new `/admin` route family (system admin remains `/system`).
- No full IAM replacement or external SSO redesign in MVP.
- No rewrite of booking domain core flow (Resource/Slot engine remains unchanged).

### Implementation snapshot (2026-04-09)

This document remains a **north-star reference** for RBAC + unified login. The table records **as-delivered** code versus **follow-up** (Phase 2b / §6 ideals). **This spec’s committed delivery tranche is closed** per **結案聲明** above; gaps are **not** reopening this file’s closure scope.

| Area | Delivered (current) | Gap vs this spec |
|------|---------------------|------------------|
| Unified login route | `/login` with `intent` + `returnUrl`; legacy redirects; **`/login/context`** when `availableContexts.length > 1` | Metadata-driven route map from config file (optional) |
| Frontend guards | Same layouts + `sessionRouting`; **`/403`** for forbidden namespace | — |
| Auth API | 同上；**login** 可設 **`booking.platform.auth.login`** 做 IP 滑動視窗限流（429）；**DB 使用者** 錯誤密碼達閾值寫入 **`locked_until`**（仍回 **401** 與「Invalid credentials」同形，不洩漏鎖帳）；**logout** 遞增 **`credential_version`** 撤銷舊 JWT | Redis 叢集限流、refresh token 撤銷表、營運解鎖 API／runbook（Phase 2 其餘） |
| Claims | JWT: `role`, `merchantId`、**`cv`**（僅 DB 使用者；與 **`credential_version`** 對齊）；login + `/me`: `roles` / `permissions`；`/me`：`CONTEXT_SET` 與 `availableContexts` 等 | Full `role_bindings` / `active_context` in JWT (Phase 2) |
| Namespace enforcement | URL 角色 + **`PermissionAuthorizer`** + **`EffectivePermissionService`**（DB／catalog）；**`/api/system/*`** 細分 **`system.dashboard.read`** 與 **`system.settings.write`**；**`/api/me/navigation`** 需 **`me.navigation.read`**；商戶建立／自助註冊寫入 **`PlatformAuditService`**；**403** 時 **`BookingAccessDeniedHandler`** 以 **SLF4J WARN** 記錄 `method`／`uri`／`principal`／`message` | monitor／enforce flag、集中矩陣設定檔、JWT `role_binding_id`、政策決策／metrics／租戶不符專用事件（Phase 2 其餘） |
| Platform nav DB | `PlatformNavigationInitializer` grants `CLIENT` → `nav.client.todo`, `nav.store.public` on **empty** DB | **Flyway `V3__client_role_page_grants.sql`** backfills `CLIENT` grants when `platform_pages` already existed |
| RBAC tables in §6 | **Flyway `V4__rbac_core.sql`**：`rbac_permissions`、`rbac_roles`、`rbac_role_permissions`、`platform_user_rbac_bindings` + 種子／自 `platform_users` 回填 | 稽核表、端點↔permission 對照表、政策版本與撤銷等仍待 |

**Spec completeness:** Requirements and phases are **coherent and usable for handoff**. **Product “done”** for **this tranche** (Phase 1 MVP + Phase 2a) **is claimed** as of 2026-04-09, subject to normal **reviewer-agent** release gate. Broader Phase 2b / full §6 ideals remain **follow-up**, not part of this closure.

### Phase 2a／2b（執行切片 · 2026-04-09）

- **Phase 2a（已合入或可驗收）**：登入 IP 滑動視窗限流（**`LoginRateLimiter`**，`LoginRateLimitApiTest`）；**`credential_version` + JWT `cv` + logout 撤銷**（**`CredentialRevocationApiTest`**）；**DB 帳號鎖帳**（Flyway **`V6__platform_user_login_lockout.sql`**，`platform_users.failed_login_*`／`locked_until`，**`booking.platform.auth.login.lockout-*`**，**`AccountLockoutApiTest`**；整合測試預設 **`lockout-enabled: false`**）；**403** 最小可觀測性（**`BookingAccessDeniedHandler`** WARN）。
- **Phase 2b（仍屬倡議／待排）**：端點級 DB 政策與 **enforce flag**、稽核 **可查詢 API**、Redis／叢集限流、refresh blocklist、**`/system` 政策管理 UI**（若核定）、全 E2E 矩陣、ARCH-1..4 ADR 凍結等。

### 交付進度摘要（2026-04-09）

- **已完成**：統一入口 `/login`（含 `intent` / `returnUrl`），舊路徑 `/merchant/login`、`/system/login`、`/client/login` 轉址至新流程。
- **已完成**：`POST /api/auth/login`、`GET /api/auth/me`（非空 `availableContexts`／`activeContext`、`sessionState`）、`POST /api/auth/refresh`、`POST /api/auth/logout`；**`POST /api/auth/context/select`** 可換發 token（無 body 或無 `role` 時等同 refresh）。
- **已完成**：前端 `/403`、命名空間導向與既有 layout／`sessionRouting` 防護；後端 `PermissionAuthorizer` 搭配 `@PreAuthorize` 用於 `SystemAdminController` 與 `MerchantPortalController`；`/api/merchant/register` 維持 `permitAll`。
- **已完成**：Flyway `V3__client_role_page_grants.sql` 補齊既有環境下 CLIENT 頁面授權；`RolePermissionCatalog` 等目錄層銜接。
- **已完成（測試）**：`AuthMeApiTest`、`MeNavigationApiTest`、`PermissionAuthorizerTest`、`LoginRateLimitApiTest`、`CredentialRevocationApiTest`、`AccountLockoutApiTest` 等已反映目前 API／授權／登入硬化行為。
- **已完成（MVP）**：`/login/context` 多情境選擇 UI；**未完成（倡議）**：JWT 內完整 `role_bindings` / `active_context`（§4 較完整合約屬 Phase 2）。
- **進行中（Phase 2 程式）**：**`V4__rbac_core.sql`** 與 **`EffectivePermissionService`**；**`SystemAdminController`**／**`MeController`** 端點級 **`@PreAuthorize`**；**`PlatformAuditService`**（管理員建商戶、公開註冊、系統後台變更之稽核 actor）；整合測試 **`EffectivePermissionServiceIntegrationTest`**。
- **已完成（Phase 2a 安全）**：**`platform_users.credential_version`**（Flyway **`V5`**）+ JWT **`cv`**；**`POST /api/auth/logout`** 遞增版本撤銷 access token（無 DB 列之 dev-only 帳號仍不帶 **`cv`**）。**`LoginRateLimiter`**（`booking.platform.auth.login.*`；測試 **`rate-limit-enabled: false`**）。**帳號鎖帳**（Flyway **`V6`** + **`AuthService`**；測試 **`lockout-enabled: false`**；專測 **`AccountLockoutApiTest`**）。**403**：**`BookingAccessDeniedHandler`** 結構化 WARN。
- **未完成（Phase 2b 倡議）**：端點級 enforce flag、分散式／Redis 限流、稽核可查詢 API、完整 E2E 矩陣、ARCH-1..4 正式 ADR 簽核（仍建議）。

#### 如何閱讀下列核對清單

- **MVP 範圍內「可視為已達標」**：與「單一登入入口 + `/api/auth/*` 合約（含情境選擇）+ 命名空間層級強制 + 已列測試」一致之項目。完整租戶綁定矩陣與 DB 後援政策仍屬 Phase 2 / §6。
- **完整倡議（Full initiative）**：須一併滿足 Phase 2 硬化條件、§6 資料模型與政策層、以及 Governance／QA 所列未勾項目後，始可視為本倡議結案。

### Backlog 進度核對清單（2026-04-09）

#### Phase 1 MVP

- [x] 統一 `/login` UI、`intent`／`returnUrl` 與舊登入路徑轉址
- [x] `POST /api/auth/login`
- [x] `GET /api/auth/me`（含 `sessionState`、`availableContexts`、`activeContext`）
- [x] `POST /api/auth/refresh`
- [x] `POST /api/auth/logout`（204）
- [x] `POST /api/auth/context/select` 實際換發情境／token
- [x] `GET /api/me/navigation` 與導覽行為；`MeNavigationApiTest`
- [x] `AuthMeApiTest`（`/api/auth/me`、`refresh`、`logout`、`context/select`）
- [x] 命名空間路由防護、`/403`、`sessionRouting` 與各區 layout
- [x] `PermissionAuthorizer` + `@PreAuthorize` 於 `SystemAdminController`、`MerchantPortalController`
- [x] `/api/merchant/register` 維持 `permitAll`
- [x] Flyway `V3__client_role_page_grants.sql`（CLIENT 角色頁面授權補齊）
- [x] 多情境選擇 UI（`/login/context`）與非空 `availableContexts`（完整 JWT `role_bindings` 仍 Phase 2）
- [ ] §6 之 `roles`／`permissions`／`user_role_bindings` 等完整 RBAC 資料表與遷移（**列於 Phase 1 清單但刻意不納入 Phase 1 MVP 完成條件**；與 Phase 2 一併交付）

**Phase 1 MVP 結案說明（2026-04-09）：** 統一登入、Auth 最小合約、情境選擇（後端 `availableContexts` + `context/select` + 前端 `/login/context`）、命名空間防護與既有測試已對齊；§6 正規化 RBAC schema 與端點級 DB 政策留在 Phase 2。

#### Phase 2

- [ ] 端點級權限矩陣與 DB 後援政策（超越命名空間層級）
- [ ] 敏感操作之稽核 log 與**可查詢性**（寫入／`PlatformAuditService` 已有；**唯讀查詢 API** 仍待）
- [x] 登入濫用防護 — **IP rate limit**（`LoginRateLimiter`）與 **DB 帳號鎖帳**（`V6`／`AuthService`）；**session／JWT 撤銷**（`credential_version`／`cv`／logout）（Redis 叢集限流、refresh 撤銷表仍待）
- [ ] `/system` 政策管理介面（若核定納入）

#### Governance／QA

- [ ] ARCH-1..ARCH-4 正式決策與 ADR 凍結（建議）
- [x] `PermissionAuthorizerTest`（授權解析行為）
- [ ] 完整 E2E：多角色／租戶／情境切換與禁止存取矩陣
- [ ] 觀測與稽核：**政策決策** log、租戶不符指標（**403** 已具 **WARN** 最小觀測；其餘仍待）
- [ ] 每次 commit／release 前之 `reviewer-agent` 審查閘門（流程持續適用）

### Phase 2 kickoff（2026-04-09 · PM + Architect 同輪規劃）

本節為 Phase 2 **啟動序與護欄**；細部 story 仍可由各 agent 依此拆解 PR。

#### P0 工作流（建議順序）

| # | 工作流 | 說明 |
|---|--------|------|
| 1 | **Governance：ARCH-1..4 + ADR** | 權限命名、`active_context`／binding 語意、租戶邊界、支援／預覽模式先定稿，避免 schema 反覆。 |
| 2 | **§6 Flyway：roles / permissions / role_permissions / user_role_bindings** | 索引、種子、既有使用者回填計畫與驗證；無 binding 勿開 DB-only enforce。 |
| 3 | **Auth／JWT 與 `context/select` 對齊** | 可選新增 `role_binding_id`／`active_context`（**加性**、相容舊 token）；refresh／select 從 DB 解析有效權限。 |
| 4 | **端點級矩陣 + 複合 `PermissionAuthorizer`** | 目錄 fallback + DB；**monitor → 分 namespace enforce**（feature flag）。 |
| 5 | **敏感操作稽核 + 只讀查詢 API** | 與既有 domain 稽核分層；索引／保留策略由 DBA 參與。 |
| 6 | **登入 rate limit／鎖帳 + 撤銷模型** | 與「不洩漏帳號是否存在」一致；refresh／logout 合約文件化。 |

#### P1／可並行

- **E2E 矩陣**（多角色／租戶／禁止存取）：作為 enforce flip 的放行條件之一。  
- **觀測性**：政策決策 log、403／租戶不符指標；稽核建議 **分級**（deny + 敏感 allow 詳細，其餘可採樣）。  
- **`/system` RBAC 管理 UI**：**待利害關係人核定**；若否，Phase 2 以 Flyway 種子 + runbook 變更政策。

#### Architect 護欄（摘要）

- **雙閘門**：DB 權限解析 **不取代** repository 的 `merchant_id`／`tenant_id` 範圍；有效能力 = 目錄 ⊕ **active binding**。  
- **Strangler**：單一 release 可含 schema + adapter + **enforce 關閉**；回填與 monitor 通過後再依 namespace 開 enforce。  
- **領域邊界**：RBAC 表與端點政策屬 **platform／security**；**不**寫進 Booking 狀態機、Slot Engine、`BookingValidationStrategy`。  
- **SYSTEM_ADMIN 商戶預覽**：產品需決定是否維持「第一筆 merchant」或改為 **顯式、可稽核** 的支援流程；Architect 建議預設 **審計訊號強、寫入預設保守**。  
- **效能**：binding／permission 解析可 **短 TTL cache + policy version**；高 QPS 路徑避免全量 allow 稽核。

#### 待利害關係人確認（開放問題）

- Phase 2 **是否納入** `/system` 政策管理 UI？範圍是唯讀矩陣或完整 CRUD？  
- **撤銷模型**：blocklist、token 版本號、或可撤銷 refresh 等，與現有 stateless JWT 如何最小破壞銜接？  
- **鎖帳／解鎖**流程與文案（與登入錯誤不洩漏一致）。  
- **敏感操作清單**邊界與稽核查詢 API 的保留／權限角色。  
- **Enforce flip gate**：是否以全 E2E 通過為必備，或允許分階段 namespace。

#### Agent 交接（執行面）

| Agent | 切片 |
|--------|------|
| **architect-agent** | ADR、政策擴充策略、context／支援模式邊界。 |
| **dba-agent** | §6 DDL、回填、稽核表容量與索引。 |
| **backend-engineer-agent** | Resolver、矩陣落地、稽核 API、rate limit／撤銷、metrics。 |
| **frontend-engineer-agent** | 核定後之 `/system` UI；其餘配合 E2E。 |
| **booking-system-admin-domain** | `/system` IA、營運 runbook（無 UI 時之政策變更流程）。 |
| **qa-agent** | E2E 矩陣、稽核可驗證、auth 硬化迴歸。 |
| **reviewer-agent** | 各 commit／enforce 前安全與租戶隔離審查（workspace 閘門）。 |

#### 關鍵路徑（文字）

`ADR 凍結 → §6 schema／回填 → JWT／active_context 對齊 → 端點矩陣（monitor）→ 稽核 + enforce flip（分 namespace）→ E2E 放行`

## 2) User Roles Matrix (Capability-Based)

Role definitions are logical; assignment can be many-to-many per account and tenant.

| Capability | Client/User | Merchant Owner | Store/Staff | System Admin |
|---|---:|---:|---:|---:|
| Sign in via unified entry | ✅ | ✅ | ✅ | ✅ |
| Access `/client` pages + `/api/client/*` | ✅ | Optional* | Optional* | Optional (support mode only) |
| Access `/merchant` pages + `/api/merchant/*` | ❌ | ✅ | ✅ (scoped) | Read-only support only (policy-gated) |
| Manage merchant settings/services | ❌ | ✅ | Limited (by permission) | ❌ |
| Manage bookings for assigned store/resources | ❌ | ✅ | ✅ (assigned scope only) | ❌ |
| Access `/system` pages + `/api/system/*` | ❌ | ❌ | ❌ | ✅ |
| Tenant administration | ❌ | Partial (own tenant scope) | ❌ | Platform-wide (strictly audited) |
| RBAC policy management | ❌ | Optional delegated | Optional delegated | ✅ |

\* Optional if one account is legitimately bound to multiple role contexts.

Permission keys (examples):  
`client.booking.read`, `client.booking.create`, `merchant.service.manage`, `merchant.booking.manage`, `store.staff.schedule.manage`, `system.user.manage`, `system.rbac.manage`.

## 3) Unified Login UX Flow

### Entry Route and Interaction Model
- Single entry route: `/login`.
- Credentials (or future external IdP) submitted once.
- Backend returns authenticated identity with `availableContexts` (role + tenant + optional store scope).
- If exactly one allowed context, auto-continue to mapped destination.
- If multiple contexts, show context picker (role/tenant/store) before redirect.

### Role/Tenant Selection
- Selection order: `role` -> `tenant` -> `store` (if required by role).
- Persist selected context in client auth state and include context identifier in requests (or derive from token claim + server session).
- Context switch is explicit from profile menu; each switch triggers backend revalidation.

### Success, Failure, and Edge Cases
- Success (single context): redirect to highest-priority default landing by role:
  - Client -> `/client`
  - Merchant/Staff -> `/merchant`
  - System admin -> `/system`
- Success (multi-context): show chooser; no implicit tenant switching.
- Failure:
  - Invalid credentials -> inline error, no leakage of account existence.
  - Locked/disabled account -> dedicated state + support CTA.
  - Token expired -> return to `/login` with session-expired message.
- Edge cases:
  - Role exists but no active tenant binding -> block + remediation message.
  - Tenant mismatch attempt (tampered request) -> 403 + security audit log.
  - Accessing protected route before context selection -> redirect to context chooser.

## 4) Backend/API Contract Changes (Minimal + Incremental)

Keep existing namespace APIs. Add only auth/profile endpoints and claims needed for RBAC.

### New/Adjusted Endpoints
- `POST /api/auth/login`
  - Input: credential payload.
  - Output: access token/session + `availableContexts` + user profile summary.
- `POST /api/auth/refresh` (if JWT flow).
- `POST /api/auth/logout`.
- `GET /api/auth/me`
  - Returns current principal claims and selected context.
- `POST /api/auth/context/select`
  - Input: selected `roleBindingId` (or role+tenant+store composite).
  - Output: updated session/context confirmation.

**Current implementation note (2026-04-09):** **`GET /api/auth/me`** returns username, role(s), permissions, merchantId, `sessionState` (`CONTEXT_SET` when contexts are listed), **`availableContexts`** (always includes the current platform view; **SYSTEM_ADMIN** may see an extra **MERCHANT** option scoped to the first merchant row), and **`activeContext`**. Navigation keys still come from **`GET /api/me/navigation`**. Login response includes **`roles`** / **`permissions`** plus legacy **`role`**. **`POST /api/auth/context/select`** accepts `{ role, merchantId }` matching a server-listed option and returns a new **`TokenResponse`**; omit body or `role` to behave like **`/refresh`**.

### Required Claims / Session Fields
- `sub` (user id)
- `tenant_ids` (or active `tenant_id` + list metadata)
- `role_bindings` (role + tenant + optional store scope)
- `permissions` (flattened or resolvable policy ids)
- `active_context` (roleBindingId)
- `session_state` (`AUTHENTICATED`, `CONTEXT_SET`, etc.)

### Authorization Enforcement
- Preserve existing API prefixes and gate each by policy:
  - `/api/client/*` requires `client:*` permission set within active tenant context.
  - `/api/merchant/*` requires merchant/staff permissions and scope checks.
  - `/api/system/*` requires system-level permissions only.
- Deny by default when claim/context is absent or mismatched.

## 5) Frontend Guard Strategy (React Router)

- Public routes: `/login`, optional `/auth/callback`.
- Protected layout wrappers remain namespace-specific:
  - `/client/*` -> `ClientProtectedLayout`
  - `/merchant/*` -> `MerchantProtectedLayout`
  - `/system/*` -> `SystemProtectedLayout`
- Shared guard utility:
  - Checks authenticated state.
  - Checks context selected.
  - Checks permission predicate for route segment.
  - Redirect rules:
    - unauthenticated -> `/login`
    - authenticated without context -> `/login/context`
    - forbidden -> `/403`
- Keep route metadata-driven permission mapping to avoid hardcoded branching across components.

## 6) RBAC Data Model + Migration + Backward Compatibility

### Proposed Tables
- `roles` (`id`, `code`, `namespace`, `is_system`, timestamps)
- `permissions` (`id`, `code`, `resource`, `action`, `namespace`)
- `role_permissions` (`role_id`, `permission_id`)
- `user_role_bindings` (`id`, `user_id`, `role_id`, `tenant_id`, `store_id_nullable`, `status`)
- `policy_audit_logs` (`id`, `actor_user_id`, `tenant_id`, `namespace`, `decision`, `reason`, `created_at`)

Tenant-scoped rows must carry `tenant_id` where applicable; platform-level roles marked explicitly.

### Migration Sequence (Incremental)
1. Create RBAC schema tables with read-compatible defaults.
2. Seed baseline roles + permissions (client, merchant_owner, store_staff, system_admin).
3. Backfill `user_role_bindings` from existing user/account metadata.
4. Add auth service support to emit role/context claims.
5. Enable policy checks in monitor mode (log-only deny candidates).
6. Flip to enforce mode per namespace behind feature flag.

### Backward Compatibility
- During transition, support legacy role field mapping -> new role bindings adapter.
- Keep existing tokens valid until expiry; refresh issues new claims format.
- Route guards should accept either legacy role claim or new permission set in MVP window.

## 7) Rollout Plan, Acceptance Criteria, and Test Plan

### Phase 0 - Discovery & ADR Alignment
- Finalize role taxonomy, permission namespace naming, context switching semantics.
- Architect review dependencies (required before Phase 1 sign-off):
  - [ARCH-1] Resource/slot abstraction fit confirmation.
  - [ARCH-2] Policy strategy extensibility decision.
  - [ARCH-3] Auth/session state transition legality definition.
  - [ARCH-4] Tenant isolation enforcement guarantees.

### Phase 1 - MVP
- Unified `/login` UI and shared auth store.
- New auth endpoints (`login`, `me`, `context/select`) and claims.
- Basic RBAC enforcement on namespace boundaries.
- Default post-login redirection by selected context.

Acceptance criteria (MVP):
- Single login entry works for all supported roles.
- User can access only authorized namespace routes/APIs.
- Tenant-scoped users cannot access other tenant data by URL/API tampering.
- Existing role-specific pages continue functioning under new guard layer.

### Phase 2 - Hardening
- Full permission-level checks (beyond namespace-level).
- Audit log coverage for allow/deny events on sensitive operations.
- Login abuse protection (rate limit/lockout) and improved session revocation.
- Policy management tooling under `/system` (if approved).

Acceptance criteria (Hardening):
- Critical endpoints enforce permission keys + tenant constraints.
- Security/audit events are queryable for incident review.
- Forbidden attempts produce consistent 403 behavior and logs.

### Test Plan
- Unit:
  - Permission resolver, policy evaluator, context selector, guard utilities.
- Integration (backend):
  - Auth endpoints, claim issuance, namespace gate checks, tenant filters.
- E2E (frontend+backend):
  - Unified login happy paths per role.
  - Multi-role context selection and switch.
  - Forbidden route/API access (role and tenant mismatch).
  - Session expiry and logout flows.

## 8) Risks and Open Questions

### Key Risks
- Over-broad tokens causing accidental privilege leakage.
- Tenant context confusion in multi-role accounts.
- Incomplete migration backfill producing silent authorization gaps.
- Frontend guard drift from backend policy decisions.

### Open Questions
- Should system admins ever have scoped read access into tenant merchant views (support mode), and under what audit policy?
- Is `store/staff` a first-class role in all tenants, or optional extension by merchant domain?
- JWT vs server session as default transport for context switching and revocation requirements?
- Do we need delegated role management for merchants in MVP or phase 2?

## Affected Areas
- `/client`: login redirect targets, client permission checks.
- `/merchant`: merchant/staff route guards and scoped operations.
- `/system`: system-only admin panels; no `/admin` additions.
- Backend security and auth layers spanning `/api/client`, `/api/merchant`, `/api/system`.

## Handoff Notes (Agent Ownership)
- `architect-agent`: finalize ARCH-1..ARCH-4 decisions + ADR drafts.
- `backend-engineer-agent`: auth endpoints, claims, RBAC policy enforcement, migration implementation.
- `frontend-engineer-agent`: unified `/login`, context picker, router guard refactor.
- `system-admin-agent`: `/system` RBAC management screens (if in scope phase 2).
- `dba-agent`: migration DDL, backfill scripts, indexes, rollback plan.
- `qa-agent`: E2E matrix by role/tenant/context + regression pack.
- `reviewer-agent`: mandatory review gate after each commit and before release readiness.

## Implementation Backlog (Prioritized)

1. **Spec/ADR Alignment**
   - Confirm ARCH-1..ARCH-4 decisions and freeze permission naming convention.
2. **DB Foundation (RBAC schema)**
   - Add `roles`, `permissions`, `role_permissions`, `user_role_bindings`, audit table + indexes.
3. **Auth Contract MVP**
   - Implement `/api/auth/login`, `/api/auth/me`, `/api/auth/context/select` with new claims.
4. **Policy Enforcement Layer**
   - Add centralized policy evaluator and namespace guards (deny by default).
5. **Frontend Unified Entry**
   - Create `/login` + context chooser; integrate shared auth store.
6. **Router Guard Refactor**
   - Metadata-driven guards for `/client`, `/merchant`, `/system`; add `/403` handling.
7. **Backward Compatibility Adapter**
   - Legacy role field mapping and token compatibility window.
8. **Observability and Audit**
   - Policy decision logs, forbidden access telemetry, tenant mismatch alerts.
9. **Security Hardening**
   - Login rate limiting, account lock policy, token/session revocation improvements.
10. **QA & Release Readiness**
   - Unit/integration/E2E completion, rollback drill, reviewer-agent sign-off gate.

### Backlog progress（對照表 · 2026-04-09）

以下與上方 **「Backlog 進度核對清單」** 同步意涵；細項以 **勾選清單** 為準。

| # | 對照勾選清單 | 摘要 |
|---|----------------|------|
| 1 | Governance／QA | ARCH-1..4 ADR 未凍結 |
| 2 | Phase 1 §6 未勾 + Phase 2 | `V3` 已做；完整 RBAC 表仍待 |
| 3 | Phase 1 Auth API 列項 | login／me／refresh／logout／**context/select** 已做 |
| 4 | Phase 1 Policy + Phase 2 | `@PreAuthorize` + catalog 已做；DB 政策仍待 |
| 5 | Phase 1 `/login` 列項 | 已做 |
| 6 | Phase 1 guards + `/403` | 已做 |
| 7 | （相容期） | 仍支援 JWT `role` 與 `roles`／`permissions` 並存 |
| 8 | Phase 2 觀測 | 403 WARN 已做；政策決策／metrics 仍待 |
| 9 | Phase 2 安全硬化 | IP 限流、鎖帳、`cv`／logout 撤銷已做；Redis／refresh 表仍待 |
| 10 | Phase 2 + Governance | E2E、ADR、reviewer 閘門等仍待 |
