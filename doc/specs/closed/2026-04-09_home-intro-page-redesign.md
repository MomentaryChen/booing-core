# Public home page (`/`) redesign — PM spec

**Status:** **已結案**（歸檔於 `doc/specs/closed/`）  
**Surface:** `frontend/src/pages/home/HomeIntroPage.jsx` (route `/`)  
**Locales:** `en-US`, `zh-TW`  
**Delivered（摘要）：** 與本規格及 benchmark 附檔對齊之版面／文案已於 `HomeIntroPage` + `i18n` 落地；目的地載入採 **Loader2**（nice-to-have Skeleton 未納入本票必達，與 `doc/specs/closed/2026-04-09_visual-assets-dashboard-guidelines.md` 結案註記一致）。分析事件埋設未納入。

---

## 1. Problem statement & target users

**Problem**

The landing page communicates that Booking Core is a multi-role demo platform, but the hero and tile copy are generic (“four entry points, in images”) and do not quickly answer *why* someone should click through. Visual hierarchy is limited to hero illustration + a flat grid of four tiles. For authenticated merchants, tiles already filter by navigation/RBAC; the redesign must preserve that behavior while making the page feel more polished and purposeful—not busier.

**Primary audiences**

| Audience | Intent on `/` | What they need |
|----------|-----------------|----------------|
| **Evaluators / developers** | Understand what the repo demonstrates in one screen | Clear value line + obvious paths to merchant, client, system, and demo storefront |
| **Merchant users (logged in)** | Jump to allowed areas only | Same four conceptual destinations, visibility tied to granted routes; no dead-end or forbidden links surfaced as primary CTAs |
| **Casual visitors** | Try the public booking experience | Prominent, trustworthy path to the **public demo storefront** (`/client/booking/demo-merchant`) without confusing it with “client app” signed-in flows |

**Non-goals**

- Replacing or redefining booking domain models (resource/slot) on this page.
- Adding new product namespaces or forbidden route prefixes (see §5).

---

## 2. Messaging hierarchy

Copy is **directional** for UI/UX and i18n to finalize; tone: confident, short, demo-aware.

**Headline (H1)**

- **Job:** State the product in one line—multi-tenant scheduling + three operator surfaces + customer-facing storefront.
- **Example direction (en):** “One platform. Merchant ops, client journeys, and system control.”
- **Example direction (zh-TW):** 「一套平台：商家營運、客戶預約、系統治理。」

**Subhead (one paragraph under H1, ~1–2 sentences)**

- **Job:** Clarify this is a **reference / demo** implementation and name the four entry types without repeating the tile titles verbatim.
- **Example direction:** Explain that visitors can open the merchant console, client area, system admin, or try the public demo booking page—aligned with `/merchant`, `/client`, `/system`, and the demo storefront link.

**Optional supporting bullets (max 3, only if layout supports them)**

1. **Namespaces:** Short clarification that **Client**, **Merchant**, and **System** are separate areas (consistent with UI/UX route rules).
2. **Demo:** “Try booking” points to the **public demo merchant** storefront (not a generic marketing claim).
3. **Access:** When signed in as merchant, “You only see areas your account can open” (or equivalent)—sets expectation for RBAC-filtered tiles without legalistic wording.

**Section title above tiles (replaces or refines “four entry points”)**

- **Job:** Label the grid as **actions**, not a gallery—e.g. “Choose where to go” / 「選擇前往區域」.

**Per-tile**

- Keep **title + one-line caption** per tile; tighten captions so each role is distinct (merchant vs client app vs system vs public demo).

---

## 3. Success metrics & acceptance criteria (“done”)

**Qualitative acceptance (required for v1)** — **本票已驗收**

- [x] **Value in 5 seconds:** A new visitor can state what the product is and that four distinct entry types exist (merchant / client / system / public demo).
- [x] **Demo discoverability:** The public demo storefront is at least as visible as today (ideally stronger visual or copy emphasis without hiding other tiles).
- [x] **Namespace clarity:** Copy does not imply `/client` is only “customer booking”—if needed, microcopy distinguishes **client app area** vs **public storefront demo**.
- [x] **i18n parity:** All new/changed strings exist in **both** `en-US` and `zh-TW` with comparable meaning and length that does not break layout at common breakpoints.
- [x] **RBAC parity:** Behavior matches current logic: if `isMerchantAuthRequired()`, user has a token, and navigation has loaded, only tiles whose `routeKey` is in `routeKeys` render; loading state shows no misleading CTAs (or an explicit loading pattern—see scope).
- [x] **Routes unchanged:** No new routes; links remain `/merchant`, `/client`, `/system`, `/client/booking/demo-merchant` (or whatever the canonical `ROUTE_KEYS.STORE_PUBLIC` target is today).
- [x] **Accessibility:** Semantic structure preserved or improved (one H1, section label for tile region, images with meaningful `alt` in both locales).

**Success metrics (lightweight, if analytics exist or can be added later)** — **未導入（非本票必達）**

- [ ] Click-through to demo storefront from `/` (optional event).
- [ ] Bounce from `/` without any tile click (baseline vs after—only if instrumentation is in scope).

---

## 4. Scope: must-have vs nice-to-have (v1)

**Must-have (v1)**

- Revised **headline, subhead, section title, and tile titles/captions** (i18n keys updated in both languages).
- **Richer layout/visual treatment** within the existing page (spacing, typography scale, optional secondary band or background treatment, card hover/focus states)—implemented in existing CSS module or global home styles **without** new forbidden routes.
- **Explicit visual or typographic emphasis** on the demo storefront tile **or** a short secondary CTA that still routes only to allowed paths.
- **Documentation handoff:** UI/UX + `frontend-engineer` implement; `reviewer-agent` before merge per project gate.

**Nice-to-have (v1 if time; otherwise backlog)**

- New or refined **hero illustration** (SVG) aligned with updated copy. **（沿用既有 `/intro/hero-booking.svg`，未強制替換。）**
- **Skeleton/loading** state for the tile grid when `loading === true` and merchant filter applies—clear “loading destinations” instead of empty grid. **（已交付：Loader2 + 文案；非 shadcn Skeleton。）**
- **Micro-interactions** (motion) respecting `prefers-reduced-motion`. **（卡片 hover／背景裝飾已部分具備；`prefers-reduced-motion` 於 Loader／裝飾有考量。）**
- **Analytics** on tile clicks (if product agrees). **（未做。）**

**Out of scope for this redesign**

- Backend/API changes, auth model changes, new `PlatformPage` entries solely for marketing.
- Replacing the entire app shell or global navigation.

---

## 5. Risks & constraints

| Risk / constraint | Mitigation |
|-------------------|------------|
| **i18n length** | zh-TW headlines often shorter or longer than en-US; design with flexible wrap and max-width; avoid fixed-height text blocks. |
| **RBAC tile visibility** | Do not add tiles that are not backed by `routeKeys` when merchant auth is required—any “marketing-only” link must still use allowed routes or be shown only when it has a defined `routeKey` in navigation. |
| **Terminology** | “Client” in URLs means **end-customer area**; avoid mixing with “platform user” language (per UI/UX skill). |
| **Demo vs client home** | Users may confuse `/client` with the demo storefront; copy and visual hierarchy should reduce ambiguity. |
| **No forbidden routes** | Do not introduce `/admin` or other disallowed prefixes; stay within **client / merchant / system** conventions. |
| **Loading state** | Empty grid while `loading` can feel broken; address in must-have or nice-to-have explicitly. |

---

## 6. Alignment note for `architect-agent`

**Resource/slot abstraction:** Not applicable to this landing page.

**Tenancy / RBAC:** The page is **read-only marketing + deep links**. The only authorization-sensitive behavior today is **filtering tiles by navigation `routeKeys` when merchant authentication is required**. Any redesign must:

- Preserve **server-driven or app-navigation-driven** visibility (no client-only guessing of permissions).
- Avoid introducing new coupling between `/` and booking engines.

If a future change moved “which links appear on `/`” to the API, that would be a small **cross-cutting auth/navigation** decision—worth a quick architect pass before implementation. For copy and layout-only changes, **no architect blocker** beyond confirming no accidental bypass of existing navigation contracts.

---

## Handoff (recommended owners)

| Deliverable | Owner |
|-------------|--------|
| Copy finalization & hierarchy | **UI/UX** (with PM sign-off) |
| Visual design / responsive behavior | **UI/UX** + **frontend-engineer** |
| i18n strings (`en-US` / `zh-TW`) | **frontend-engineer** |
| RBAC + loading UX parity | **frontend-engineer** (verify against `NavigationContext` / `useVisibleIntroTiles`) |
| Pre-merge review | **reviewer-agent** |

---

*本 initiative 已結案並歸檔至 `doc/specs/closed/`。若重開首頁疊代，請於 `doc/specs/` 根目錄新增日期前綴規格並註明與本檔關係。*
