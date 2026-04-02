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
    introHeroTitle: "Booking Core 預約平台",
    introHeroSubtitle: "商家排程、公開店面與系統管理，一站式示範專案。以下以圖像快速認識各角色入口。",
    introGalleryLabel: "平台介紹圖集",
    introSectionTitle: "用圖像認識四大入口",
    introTileMerchantTitle: "商戶後台",
    introTileMerchantCaption: "服務、班表與預約管理",
    introTileMerchantAlt: "商戶後台示意：服務與排程管理介面",
    introTileClientTitle: "客戶端",
    introTileClientCaption: "預約流程示範（開發中）",
    introTileClientAlt: "客戶手機預約流程示意",
    introTileSystemTitle: "系統管理",
    introTileSystemCaption: "商家、範本與稽核總覽",
    introTileSystemAlt: "系統管理儀表板示意",
    introTileStoreTitle: "公開預約頁",
    introTileStoreCaption: "顧客選服務與時段並下單",
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
    introHeroTitle: "Booking Core scheduling platform",
    introHeroSubtitle:
      "Merchant scheduling, public storefront, and system administration in one demo. Explore each role below—mostly in pictures.",
    introGalleryLabel: "Platform gallery",
    introSectionTitle: "Four entry points, in images",
    introTileMerchantTitle: "Merchant console",
    introTileMerchantCaption: "Services, hours, and bookings",
    introTileMerchantAlt: "Illustration: merchant services and schedule UI",
    introTileClientTitle: "Client flow",
    introTileClientCaption: "Booking demo (work in progress)",
    introTileClientAlt: "Illustration: mobile booking confirmation",
    introTileSystemTitle: "System admin",
    introTileSystemCaption: "Merchants, templates, and audit",
    introTileSystemAlt: "Illustration: admin dashboard overview",
    introTileStoreTitle: "Public storefront",
    introTileStoreCaption: "Customers pick services and time slots",
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

