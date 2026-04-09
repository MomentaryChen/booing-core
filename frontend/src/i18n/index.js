import { createContext, createElement, useContext, useMemo, useState } from "react";

const messages = {
  "zh-TW": {
    brand: "Booking Core",
    navAdmin: "系統管理",
    navMerchant: "商戶後台",
    navMerchantRegister: "商戶註冊",
    navMerchantLogin: "商戶登入",
    navMerchantLogout: "商戶登出",
    navMerchantAppointments: "預約列表",
    navMerchantSchedule: "班表設定",
    navClient: "客戶端（待規劃）",
    navStore: "公開預約頁",
    locale: "繁中",
    introHeroTitle: "一套平台：商家營運、客戶預約、系統治理",
    introHeroSubtitle:
      "參考實作示範——整合商戶後台、客戶端與系統治理。下方可先掌握重點，再選擇進入區域或開啟示範公開預約。",
    introHeroProofLine: "開放式參考實作：多角色介面與示範資料，並非對外販售的商用產品。",
    introIntentSectionTitle: "誰適合先看這個示範",
    introIntentChipDev: "開發／評估架構",
    introIntentChipMerchant: "想操作商戶後台",
    introIntentChipVisitor: "想試客戶預約流程",
    introValueSectionTitle: "快速了解",
    introValue1: "商戶、客戶端與系統管理為彼此獨立的專區。",
    introValue2: "「公開預約頁」連至示範店家，無需帳號即可試著完成預約。",
    introValue3: "已登入商戶時，僅顯示您有權限進入的入口。",
    introIncludedTitle: "示範涵蓋的區塊",
    introIncluded1: "商戶後台：服務、營業時間與預約營運。",
    introIncluded2: "客戶端：顧客端專區與預約相關入口。",
    introIncluded3: "系統管理：平台商家、範本與稽核。",
    introIncluded4: "公開示範預約：示範店家頁面，可體驗選服務與時段。",
    introSectionTitle: "選擇前往區域",
    introDestinationsLoading: "正在載入可進入的區域…",
    introDestinationsEmpty: "目前沒有可進入的區域。若您預期應有權限，請聯繫平台管理員。",
    introTileDemoBadge: "示範",
    introClosingTitle: "準備好要試了嗎？",
    introClosingLead: "無需帳號即可開啟示範公開預約頁，體驗客戶端流程。",
    introClosingLeadNoDemo: "請使用上方卡片進入您帳號目前可用的區域。",
    introClosingCtaDemo: "前往示範公開預約頁",
    introClosingReadmeLabel: "查看儲存庫說明（GitHub）",
    introFooterNote: "Booking Core 為示範專案；各區資料可能重置，實際行為以部署環境為準。",
    introTileMerchantTitle: "商戶後台",
    introTileMerchantCaption: "服務、營業時間與預約營運",
    introTileMerchantAlt: "商戶後台示意：服務與排程管理介面",
    introTileClientTitle: "客戶端",
    introTileClientCaption: "客戶端專區首頁與預約相關入口",
    introTileClientAlt: "客戶手機預約流程示意",
    introTileSystemTitle: "系統管理",
    introTileSystemCaption: "平台商家、範本與稽核總覽",
    introTileSystemAlt: "系統管理儀表板示意",
    introTileStoreTitle: "公開預約頁",
    introTileStoreCaption: "示範店家：選擇服務、時段並送出預約",
    introTileStoreAlt: "公開店面預約頁示意",

    "clientBooking.scheduleAnonymousInfo":
      "可匿名查看時段；送出預約前需通過店家邀請碼。若您已登入，系統會自動帶入可用的基本資料。",
    "clientBooking.inviteCodeLabel": "店家邀請碼",
    "clientBooking.inviteCodePlaceholder": "請輸入店家提供的邀請碼",
    "clientBooking.error.selectSlotFirst": "請先選擇可預約時段",
    "clientBooking.error.inviteInvalid": "邀請碼錯誤，請再確認後送出",
    "clientBooking.error.lockHeld": "該時段目前已被其他人鎖定，請選擇其他時間",
    "clientBooking.error.lockExpired": "預約鎖定已過期，請重新選擇時段",
    "clientBooking.error.termsRequired": "請先同意預約須知",
    "clientBooking.error.inviteCodeNotConfigured": "此店家尚未設定邀請碼，請洽詢店家",
    "clientBooking.error.outsideBusinessHours": "此時段不在店家營業時間內，請選擇其他時段",
    "clientBooking.error.blockedWindow": "此時段落在店家暫停/封鎖區間，請選擇其他時段",
    "clientBooking.error.conflict": "此時段與既有預約衝突，請選擇其他時段",
    "clientBooking.error.generic": "發生錯誤，請稍後再試",
  },
  "en-US": {
    brand: "Booking Core",
    navAdmin: "System Admin",
    navMerchant: "Merchant",
    navMerchantRegister: "Merchant signup",
    navMerchantLogin: "Merchant login",
    navMerchantLogout: "Merchant logout",
    navMerchantAppointments: "Appointments",
    navMerchantSchedule: "Schedule settings",
    navClient: "Client (To-Do)",
    navStore: "Public Store",
    locale: "EN",
    introHeroTitle: "One platform: merchant ops, client booking, and system control",
    introHeroSubtitle:
      "A reference demo that brings together the merchant console, client area, and system admin—or open the sample storefront to try booking.",
    introHeroProofLine:
      "Open reference implementation: multi-role UI and sample data—not a marketed commercial product.",
    introIntentSectionTitle: "Who this demo is for",
    introIntentChipDev: "Developers & evaluators",
    introIntentChipMerchant: "Merchants exploring the console",
    introIntentChipVisitor: "Visitors trying the booking flow",
    introValueSectionTitle: "At a glance",
    introValue1: "Merchant, client, and system areas are separate experiences.",
    introValue2:
      "The public demo storefront links to a sample merchant; no account is required to try booking.",
    introValue3: "When you're signed in as a merchant, only destinations you can access are shown.",
    introIncludedTitle: "What this demo includes",
    introIncluded1: "Merchant console: services, hours, and booking operations.",
    introIncluded2: "Client area: customer-facing home and booking entry points.",
    introIncluded3: "System admin: merchants, templates, and audit.",
    introIncluded4: "Public sample storefront: pick services and times on a demo merchant page.",
    introSectionTitle: "Choose where to go",
    introDestinationsLoading: "Loading your destinations…",
    introDestinationsEmpty:
      "No destinations are available for your account. Contact a platform administrator if this is unexpected.",
    introTileDemoBadge: "Demo",
    introClosingTitle: "Ready to try it?",
    introClosingLead:
      "Open the public sample storefront without an account to walk through the customer booking path.",
    introClosingLeadNoDemo: "Use the cards above to open destinations currently available to your account.",
    introClosingCtaDemo: "Open sample storefront",
    introClosingReadmeLabel: "View repository overview (GitHub)",
    introFooterNote:
      "Booking Core is a demo build; data in each area may reset. Behavior follows your deployment.",
    introTileMerchantTitle: "Merchant console",
    introTileMerchantCaption: "Services, hours, and appointment operations",
    introTileMerchantAlt: "Illustration: merchant services and schedule UI",
    introTileClientTitle: "Client area",
    introTileClientCaption: "Customer-facing home and booking entry points",
    introTileClientAlt: "Illustration: mobile booking confirmation",
    introTileSystemTitle: "System admin",
    introTileSystemCaption: "Merchants, templates, and audit",
    introTileSystemAlt: "Illustration: admin dashboard overview",
    introTileStoreTitle: "Sample storefront",
    introTileStoreCaption: "Pick services and times on the demo merchant page",
    introTileStoreAlt: "Illustration: public booking storefront",

    "clientBooking.scheduleAnonymousInfo":
      "Schedule browsing is anonymous. Invite code verification is required before booking. Signed-in users get basic info auto-filled when available.",
    "clientBooking.inviteCodeLabel": "Merchant invite code",
    "clientBooking.inviteCodePlaceholder": "Enter the invite code from merchant",
    "clientBooking.error.selectSlotFirst": "Please select an available timeslot first.",
    "clientBooking.error.inviteInvalid": "Invalid invite code. Please check and try again.",
    "clientBooking.error.lockHeld": "Selected slot is currently being held. Please choose another time.",
    "clientBooking.error.lockExpired": "Booking lock expired. Please re-select a timeslot.",
    "clientBooking.error.termsRequired": "Please agree to the booking terms",
    "clientBooking.error.inviteCodeNotConfigured": "This merchant has not configured an invite code yet. Please contact them.",
    "clientBooking.error.outsideBusinessHours": "This time is outside business hours. Please choose another timeslot.",
    "clientBooking.error.blockedWindow": "This timeslot falls into a blocked window. Please choose another time.",
    "clientBooking.error.conflict": "This timeslot conflicts with an existing booking. Please choose another time.",
    "clientBooking.error.generic": "Something went wrong. Please try again later.",
  },
};

const I18nContext = createContext({
  locale: "zh-TW",
  setLocale: () => {},
  t: (key) => key,
});

export function I18nProvider({ children }) {
  const [locale, setLocale] = useState("zh-TW");

  const value = useMemo(
    () => ({
      locale,
      setLocale,
      t: (key) => messages[locale]?.[key] || key,
    }),
    [locale]
  );

  return createElement(I18nContext.Provider, { value }, children);
}

export function useI18n() {
  return useContext(I18nContext);
}

