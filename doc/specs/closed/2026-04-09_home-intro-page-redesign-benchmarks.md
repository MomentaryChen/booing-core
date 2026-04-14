# Home intro (`/`) — competitor benchmark addendum

**Status:** **已結案**（歸檔於 `doc/specs/closed/`）— extends [`2026-04-09_home-intro-page-redesign.md`](./2026-04-09_home-intro-page-redesign.md)  
**Surface:** `frontend/src/pages/home/HomeIntroPage.jsx` (route `/` only)  
**Locales:** `en-US`, `zh-TW` (unchanged)

**Relationship to base spec:** 主規格與本附檔一併結案。以下 IA／驗收條款仍為歷史依據；實作對照見 `HomeIntroPage`。

---

## 1. Purpose

Guide a **v2 polish pass** on the public landing by borrowing **scannable LP patterns** observed on reference sites—without expanding booking-core into a full SaaS marketing product or copying unverifiable claims.

**Reference patterns (not feature parity)**

| Source | Locale / positioning | Patterns to study (IA only) |
|--------|----------------------|-----------------------------|
| **TinyBook** (tinybook.cc) | zh-TW SMB LP | Hero + strong subhead; vertical **use-case chips** (space, fitness, clinic…); **numbered** feature blocks (calendar, forms, deposit, sync, notifications); social proof / pricing / footer CTA **rhythm**; LINE + mobile emphasis typical of TW market |
| **MENU店+** (menushop.tw) | TW merchant storefront tool | **Trust-forward** framing; **feature grid** + primary CTA; merchant-tool positioning (thin crawl—assume common TW SaaS LP structure) |
| **Omcean Booking** (omceanbooking.com) | EN-first enterprise LP | Hero with **proof-point style** sub-bullets; **industry tag** strip; case study / Q&A style blocks; “why us” **bullet grid**; pricing tiers; consultant form; app badges; **bilingual toggle** rhythm |

---

## 2. Product reality & guardrails (non-negotiable)

- **Reference/demo codebase:** Copy and layout must read as a **demonstration platform** (multi-role surfaces + sample public storefront), not a marketed production SaaS.
- **Route namespaces:** Preserve `/merchant`, `/client`, `/system`, and the **canonical demo booking URL** (e.g. `/client/booking/demo-merchant` or current `ROUTE_KEYS.STORE_PUBLIC` target). **No new product routes** for v2 of this initiative.
- **Honesty:** No **fake pricing**, **fake customer counts**, **fake awards**, or **unimplemented integrations** (e.g. do not claim LINE Official Account, payments, deposits, or 24/7 automation unless the repo actually exposes them on this landing’s linked flows).
- **RBAC:** When a merchant is signed in, **tiles may be hidden** per navigation / `routeKeys` (base spec). v2 sections that repeat CTAs must **not** imply destinations exist when tiles are filtered out—use neutral demo framing or omit redundant buttons for hidden destinations.

---

## 3. Borrow vs reject

### 3.1 Borrow (information architecture & rhythm)

| Pattern | How it applies to booking-core `/` |
|---------|-----------------------------------|
| **Hero structure** | H1 + subhead + optional **one-line proof-style line** that is **true** (e.g. “Open-source reference implementation” / “多角色示範平台”)—not “trusted by N brands.” |
| **Scannable benefit row** | Short **3–4 bullets or icon row** under hero: namespaces, demo storefront, RBAC-aware entry (aligned with base spec bullets). |
| **“Who it’s for” strip** | **Evaluator / developer** vs **merchant trying the console** vs **visitor booking demo**—as **intent labels**, not industry verticals we do not model. Optional **tag-style** chips (inspired by TinyBook / Omcean **visual** pattern only). |
| **Numbered or stepped feature blocks** | Re-frame as **“What this demo includes”** (e.g. 1 Merchant console · 2 Client area · 3 System admin · 4 Public demo booking)—**content is our four surfaces**, not competitor feature lists. |
| **Trust / demos clarity** | **Explicit “demo” and “sample data”** language; link to README or in-app note if PM/engineering agree (still one page—could be a text link to repo docs, not a new app route). |
| **Footer or closing CTA rhythm** | Repeat **one primary action** (e.g. try demo storefront) + secondary links to the same four destinations as tiles—**no** pricing table, **no** consultant form unless product scope changes. |
| **Bilingual presentation** | Omcean-style **parity of structure** across `en-US` / `zh-TW` (same sections, comparable scan path)—implemented via existing i18n, not a marketing-site-only language toggle widget unless FE already has a pattern. |

### 3.2 Reject

| Anti-pattern | Reason |
|--------------|--------|
| Full **pricing tiers**, **invoices**, **deposits** as promises | Not product truth for this repo’s landing scope. |
| **Social proof numbers** (“10,000+ merchants”) | Unverifiable; violates §2. |
| **LINE**, **SMS**, **payment gateways** as hero claims | Only if explicitly implemented and linked from this codebase; default **reject**. |
| **24/7**, **full automation**, **enterprise SLA** language | Enterprise LP fluff; misleading for a reference demo. |
| **Industry vertical cloud** implying we ship those verticals | Omcean-style industries are **visual noise** unless mapped to **generic** demo language (“salons, clinics, studios…” as *examples of who could build on the model*—optional and must be clearly hypothetical, not vertical SKU claims). |
| **Case studies / client logos** | No fabricated stories. |
| **New routes** (e.g. `/pricing`, `/contact-sales`) | Out of scope for this addendum. |

---

## 4. Revised messaging hierarchy (v2 — extends base §2)

Order is **one scrollable page**; sections are **optional** if UI/UX and FE agree to phase (prefer shipping hero + benefit row + refined tiles first).

1. **Hero (H1 + subhead)** — unchanged job vs base spec; optionally add **one honest proof line** (demo/reference).
2. **Benefit / clarity row** — max 3–4 items: namespaces, public demo, RBAC expectation (merchants see allowed areas only).
3. **Optional “Who it’s for” chips** — three intents: Developer/Evaluator, Merchant (console), Visitor (public booking demo). Chips are **filters only in copy/layout sense** (no dynamic filtering required unless FE wants zero-cost toggles).
4. **“What’s in this demo” numbered block** — maps 1:1 to the four destinations; avoids competitor feature names.
5. **Tile grid** — same four conceptual destinations; base spec section title and per-tile copy still apply; visual weight per base must-have (demo emphasis).
6. **Optional closing band** — short recap + CTA to demo storefront + text link to repo/README if approved (external URL ok).
7. **Footer area** — minimal: repeat namespace links or copyright; **no** fake pricing/footer links to non-existent product pages.

**RBAC note for copy:** When tiles are hidden, optional sections **must not** promise four visible buttons; use wording like “Available entry points depend on your account” or rely on visible tiles only.

---

## 5. Optional new sections (v2 checklist — still one page)

| Section | Include? | Notes |
|---------|----------|--------|
| Proof-style honest line under H1 | **已啟用** | `introHeroProofLine`（zh-TW / en-US）。 |
| Icon/benefit row under hero | **已啟用** | `introValueSectionTitle` + 三卡 benefit 區。 |
| Intent chips (“who it’s for”) | **已啟用** | `introIntentSectionTitle` + 三枚 chip。 |
| Numbered “what’s included” | **已啟用** | `introIncludedTitle` + 四項有序列表。 |
| Closing CTA band | **已啟用** | `introClosing*`；demo CTA 依 RBAC 顯示。 |
| README/repo link | **已啟用** | 外部連結 `rel="noopener noreferrer"`。 |

---

## 6. Acceptance criteria — **delta** vs base spec §3

**Inherits:** All checkboxes in base [`2026-04-09_home-intro-page-redesign.md`](./2026-04-09_home-intro-page-redesign.md) §3 remain required unless explicitly superseded below.

**Additions / clarifications for v2 (benchmark-informed)** — **已驗收**

- [x] **No false commercial claims:** Page does not state or imply pricing, customer counts, LINE/payment/deposit features, 24/7 operations, or integrations not present in the linked demo flows.
- [x] **Benchmark IA without scope creep:** If new blocks (benefit row, chips, numbered list, closing band) ship, they use **only** messaging allowed in §2–§4 and do not introduce **new in-app routes**.
- [x] **RBAC-aware messaging:** Copy in optional sections remains accurate when **one or more tiles are hidden** for authenticated merchants (no “four ways in” if fewer tiles render).
- [x] **Scan path:** A visitor can, without scrolling the full viewport height on a typical laptop, identify (1) demo/reference nature, (2) where to try public booking, (3) that merchant/client/system are separate areas—**in addition to** base “value in 5 seconds” for the four entry types.
- [x] **Chips / tags (if present):** Decorative or static labels only unless product later specifies interaction; either way, **no** claim that chips filter live product data.
- [x] **External links (if any):** Open in new tab with `rel` appropriate for security/accessibility per FE standards; README/repo link is clearly labeled (not disguised as in-app navigation).

**Explicit non-regression**

- [x] **Demo discoverability** and **route list** from base spec are not weakened by new sections (demo storefront remains ≥ as prominent as base requires).

---

## 7. Risks (addendum-specific)

| Risk | Mitigation |
|------|------------|
| LP creep feels like “fake SaaS” | PM + UI/UX review against §2 and §3.2 before implementation. |
| zh-TW length with extra sections | Same as base: flexible layout; avoid fixed-height bands. |
| Confusion between “industry examples” and product SKUs | If vertical examples appear, prefix with “例如” / “For example” and tie to **generic scheduling**, not packaged verticals. |

---

## 8. Alignment with `architect-agent`

No change to resource/slot models. If any future work moved landing links to **API-driven** marketing endpoints, follow base spec §6 architect note. **This addendum:** copy/layout only; **no architect blocker** beyond confirming no new navigation contracts.

**Architect — do not imply on `/` (unless the product truly exposes it today)**

- **Payments:** in-flow pay, deposits, refunds, invoicing, gateway branding.
- **Messaging channels:** LINE Official / SMS / WhatsApp as product claims.
- **CRM / marketing:** segmentation, remarketing, EDM, review management.
- **POS / in-store ops** not present in the repo (ordering, kitchen, inventory).
- **“All-in-one”** positioning if the codebase is scheduling core + extension points only.
- **Vertical-only guarantees** if the model is generic resource/slot scheduling.
- **Native / white-label apps, SLA, go-live promises** if not documented or delivered.
- **Chain / HQ** capabilities if multi-tenant ≠ full franchise product.

**Architect — RBAC / narrative**

- Landing is **static story + deep links**; do not imply post-login features without sign-in context.
- Avoid CTAs that contradict **tile / page grant** visibility after login.
- Avoid hyper-realistic fake screenshots (reports, PII, money) that imply built-in modules.

**Architect — resource/slot in copy**

- Do **not** use internal terms (Resource, Slot, rule engine) on the public LP.
- At most **one neutral line**, e.g. configurable bookable items and time rules—no depth promises.

---

## 9. Handoff

### UI/UX (`uiux-agent`)

- Finalize **section order** for v2 (§4) within one page; decide which optional blocks ship in first slice vs backlog (§5).
- Produce **final zh-TW / en-US copy** for hero, benefit row, optional chips, numbered block, and closing band; enforce **no** claims from §3.2.
- Specify **visual rhythm** (hero → scan row → tiles → optional footer CTA) inspired by reference LPs **without** pricing/social-proof modules.
- Define **loading / reduced-motion** behavior consistent with base spec nice-to-haves.
- Document **RBAC copy rules** when tile count < 4 (§4).

### Frontend (`frontend-engineer-agent`)

- Implement approved sections in `HomeIntroPage` + i18n keys; **no new product routes**.
- Preserve **`routeKeys` / merchant tile filtering** and loading behavior per base spec; extend skeleton/empty states if new sections reference tile count.
- Ensure **one H1**, logical heading order for new sections, and **accessible** chips/tags (roles/semantics as appropriate).
- Wire any **external** README link per UI/UX; verify security attributes.
- Run **`reviewer-agent`** before merge per project gate.

---

*已與主規格一併結案歸檔。若訊息規則變更，請新開 `doc/specs/` 根目錄規格並明確 supersede。*
