# Visual assets：儀表板與 SaaS 介面指引（illustration / icon / card / background）

**日期：** 2026-04-09  
**狀態：** 已結案並歸檔於 `doc/specs/done/`  
**適用：** `frontend/`（React + Vite + Tailwind；shadcn/ui 政策見 `.cursor/rules/frontend-shadcn-saas-dashboard.mdc`）  
**產品區域：** `/client`、`/merchant`、`/system` 視覺語彙應一致；各區 copy 仍依 domain 區分（見 `.cursor/skills/booking-uiux/SKILL.md`）。

---

## 結案摘要（本輪視覺刷新）

**狀態：已結案**（以下為本票交付範圍與紀錄，供驗收與後續擴充對照。）

| 項目 | 說明 |
|------|------|
| **交付頁面** | `HomeIntroPage`、`MerchantDashboard`、`ClientTodoPage`、`SystemDashboard` |
| **程式位置** | `frontend/src/components/shell/DashboardPageShell.jsx`、`components/ui/button.jsx`、`card.jsx`、`components/illustrations/*`、`frontend/src/booking-ui.css`；根節點 `id="booking-ui-root"`（非 demo）見 `App.jsx` |
| **依賴** | `frontend/package.json`：`lucide-react`、`clsx`、`tailwind-merge` |
| **本輪插圖來源** | `components/illustrations/` 內 SVG 為專案內扁平示意向量；**未**嵌入 unDraw／Storyset 下載檔。若日後改採第三方圖庫，請依下文「授權與來源」於 `NOTICE` 或資產 README 註記 |
| **另票／例外** | 尚未執行 **shadcn CLI 完整初始化**（`components.json`、Radix）；與規格「Skeleton 優先」相較，首頁 destinations **loading** 目前以 **Loader2** 為主，Skeleton 可獨立開票補強 |

---

## 存放位置建議：規格檔 vs `booking-uiux` SKILL

**建議以獨立規格檔為主，不要將全文塞進 `SKILL.md`。**（本檔已歸檔，連結見 `booking-uiux` SKILL。）

| 方式 | 理由 |
|------|------|
| **`doc/specs/done/YYYY-MM-DD_*.md`（結案後）** | 歷史單一真相；進行中規格留在 `doc/specs/` 根目錄；見 `.cursor/rules/spec-done-archive.mdc`。 |
| **僅擴充 `SKILL.md`** | 易使 skill 過長、難維護；illustration 授權與 Tailwind 範例對「日常 routing/i18n」agent 負擔偏大。 |

**連結：** `.cursor/skills/booking-uiux/SKILL.md` → 視覺資產指引路徑指向本檔。

---

## 決策矩陣：何時用 illustration、icon、card 圖、背景裝飾

| 資產類型 | 用途 | 典型位置 | 密度原則 |
|----------|------|----------|----------|
| **Illustration（扁平向量）** | 建立情境、降低空白焦慮、intro／行銷感較強的區塊 | 首頁／intro hero、**empty state** 主視覺、少數 onboarding | **每屏至多一個主 illustration**；列表內不重複堆疊 |
| **Icon（lucide-react）** | 動作、導覽、狀態、表格欄位語意、按鈕與 label 輔助 | 全區；`Button`、`DropdownMenuItem`、`Tabs`、表格標頭 | 主力；尺寸 **16px–20px**（`h-4 w-4`～`h-5 w-5`）為預設 |
| **Card 圖（16:9）** | 模組化入口／儀表板區塊：**裝置、使用者、事件** 等「可辨識主題」 | 儀表板 module card、功能導向 tile | 僅在「一張卡代表一個主題」時使用；避免同一捲動區內每張卡都上大圖 |
| **背景裝飾（gradient / blob / grid）** | Stripe-like 層次、不搶內容焦點 | `Card` 外層、`section`、dialog 外層、auth 背景 | **極淡**；對比與可讀性優先；動效若有則需尊重 `prefers-reduced-motion` |

**總則：** 圖像與裝飾是**輔助**；資訊與操作仍以 typography、spacing、shadcn 元件為主。**不要過度使用圖片**；寧可留白與一致間距。

---

## Illustration 風格與來源（Hero／empty／intro）

- **風格：** 一致 **flat illustration**、線條與色面簡潔、與產品 **SaaS 中性色**（`background`、`muted`、`border`）協調；避免寫實照片、雜訊過多的 3D。
- **候選來源（免費、偏 SaaS）：**
  - **unDraw** — 可改主題色；適合 empty、hero 補圖。
  - **Storyset**（Freepik 體系）— 情境敘事強；挑選與 unDraw **同一語彙**的扁平套系，避免混用多種畫風。
- **實作：** 以 **SVG** 或優化 **PNG/WebP** 為主；固定 **同一組 palette**（與 Tailwind theme／CSS variables 對齊），全站 1–2 種主色變體即可。

### 授權與來源（高層次，非法律意見）

- **unDraw：** 常見為允許商業使用之授權（以官網／各下載頁條款為準）；改色通常允許；仍應保留授權頁截圖或專案 `NOTICE`／資產 README 註記來源。
- **Storyset／Freepik：** 依各素材標示（Free／Premium、是否需署名）；**下載前確認授權頁**，示範專案與正式產品條款可能不同。
- **內部規範：** 新增批次素材時，在 repo 內固定一處註明 **來源 URL、授權類型、日期**。

---

## Icons

- **必備：** `lucide-react` — 與 **shadcn/ui** 生態一致（官方範例多為 Lucide）；**actions、nav、label 輔助、empty state 小圖示** 優先使用。
- **可選次要：** `@heroicons/react` — 僅在與現有畫面一致或 Lucide 缺合意語意時少量使用；**避免同一互動列混用兩套風格**。
- **尺寸：** 介面內預設 **16–20px** → Tailwind：`size-4`（16）、`size-5`（20）；與 `text-sm`／`text-base` 並排時對齊 `gap-2`。
- **品質：** 使用者反饋「80% UI 質感來自 icon」— 重點是 **一致筆寬、對齊網格、語意正確**，而非數量。

### 整合注意（目前 repo）

- `frontend/package.json` 已含 **`lucide-react`**（與 `pnpm` 政策一致）；新增環境請 `pnpm install`。
- 若後續以 shadcn CLI 加入元件，文件中的 `import { ... } from "lucide-react"` 需與已安裝依賴一致。

---

## Card with image（儀表板模組：device / user / event）

- **版型：** 圖在上或左（響應式 stack），文字 **title + 一行說明 + optional meta**；使用 shadcn **`Card`** 組合。
- **圖片比例：** **16:9** 固定裁切，避免 layout shift：

```html
<!-- 語意：外層比例盒 + 內層 object-cover -->
<div class="relative w-full overflow-hidden rounded-t-lg aspect-video bg-muted">
  <img class="h-full w-full object-cover" src="..." alt="..." />
</div>
```

- **內容範例：** 裝置示意、簡化網路拓樸、日曆／事件插圖；**與全站 illustration 同一 flat 風格**。
- **克制：** 儀表板若已有背景 blob／grid，card 圖片**飽和度略降**或邊框 `border` 收斂，避免視覺打架。

---

## 背景裝飾（subtle：gradient、blob、grid）

原則：**低對比、大面積淡色、不影響文字對比（WCAG）**。

**Gradient（頁面或 section 底）：**

```txt
bg-gradient-to-b from-background via-background to-muted/40
```

**Blur blob（裝飾用，非互動）：**

```txt
pointer-events-none absolute -top-24 right-0 h-72 w-72 rounded-full bg-primary/10 blur-3xl
pointer-events-none absolute bottom-0 left-0 h-64 w-64 rounded-full bg-muted-foreground/5 blur-3xl
```

**細 grid（極淡）：**

```txt
bg-[linear-gradient(to_right,hsl(var(--border))_1px,transparent_1px),linear-gradient(to_bottom,hsl(var(--border))_1px,transparent_1px)] bg-[size:24px_24px] opacity-[0.15]
```

（若專案使用 CSS variables 命名不同，改為對應 `border` token；重點是 **opacity 低、線細**。）

**疊加：** 裝飾層加 `pointer-events-none`，避免遮擋點擊；前景內容維持 `relative z-10`。

---

## Empty state 與 Loading：結構與 i18n（zh-TW / en-US）

對齊 `frontend/src/i18n/index.js`：**每條使用者可見字串需 zh-TW + en-US**；鍵名建議 **階層式**（例：`merchantDashboard.empty.*`）。

### Empty state（建議四段式）

| 區塊 | 用途 | zh-TW 語氣提示 | en-US 語氣提示 |
|------|------|----------------|----------------|
| **Title** | 一句話說明「目前沒有什麼」 | 簡短、不指責使用者 | Short, neutral |
| **Description** | 為何為空／下一步 | 可含「若您預期應有…請聯繫…」類說明（參考 `introDestinationsEmpty`） | Same intent, concise |
| **Primary CTA** | 建立、篩選、重新整理 | 動詞開頭 | Verb-led |
| **Secondary** | 說明文件、聯繫管理員 | 可選 | Optional |

- **視覺：** 一個 **flat illustration** 或 **單一 Lucide**（較小場景）；**不要**同時巨大 illustration + 滿版背景圖。
- **路由語意：** `/merchant` 偏營運；`/system` 偏權限與稽核；`/client` 偏預約步驟—copy 避免混用「平台使用者」與「終端客戶」用語（見 `booking-uiux`）。

### Loading

- **首屏／區塊：** `Skeleton`（shadcn）優先；**少用**全頁僅插圖無進度。
- **文案：** 簡短、可重複使用（參考 `introDestinationsLoading` 型態）；避免閃爍過長句子。
- **圖示：** `Loader2` + `animate-spin`（Lucide）置於按鈕或 inline；與 **16–20px** 規則一致。

---

## 無障礙（a11y）

| 情境 | 作法 |
|------|------|
| **純裝飾**（blob、背景 grid、重複花紋） | `aria-hidden="true"` 或 CSS background 無需 alt；**不**加入 `role="img"` 敘述 |
| **有意義的 illustration／card 圖** | `<img alt="..." />`：描述**功能或情境**，非檔名；若旁邊已有同等文字，可用 `alt=""` 避免重複唸讀（decorative in context） |
| **Icon 僅裝飾、旁邊有文字** | `aria-hidden="true"` on SVG；按鈕僅 icon 時必須 `aria-label`（i18n） |
| **動態** | `prefers-reduced-motion: reduce` 時關閉或弱化 pulse／過強動畫 |

---

## 與 shadcn / Tailwind 的對齊（摘要）

- 版面與互動：**shadcn** `Card`、`Button`、`Skeleton`、`Empty` 類組合；裝飾層僅 Tailwind utility。
- **禁止** 為裝飾使用 `style={{}}`（除非規則允許之例外）；優先 `className` + `cn()`。
- **SaaS（Stripe-like）：** 中性底、單一 primary、清楚層級；視覺資產**不**搶奪 CTA。

---

## 實作檢查清單（簡版）— 本輪已驗收

- [x] 每屏主 illustration ≤ 1；empty／intro 可豁免但避免連續多屏重複同一張。（四頁已對齊；首頁 empty 單一主視覺。）
- [x] 操作與導覽 icon：**lucide-react**，`size-4`–`size-5` 為主。
- [x] 儀表板 module card 圖：**16:9**、`aspect-video`（商戶模組卡；SVG 滿版裁切語意等同 `object-cover`）。
- [x] 背景：gradient／blob／grid **夠淡** + `pointer-events-none`（`DashboardPageShell`、首頁裝飾層）。
- [x] 所有新字串：**zh-TW + en-US**（`frontend/src/i18n/index.js` 本輪新增鍵）。
- [x] 裝飾與圖片：**decorative vs meaningful** 區分；純文字旁 icon 使用 `aria-hidden`；需標籤處使用 `aria-label`（例：系統儀表重新整理）。
- [x] 依賴：`lucide-react`（及 `clsx`、`tailwind-merge`）已列入 `package.json`。

---

*本文件為 UI/UX 與前端實作對齊用；授權細節以各素材官方條款為準。本輪已結案；擴充至其他路由或 shadcn CLI 化請另開需求／票。*
