import i18n from 'i18next'
import { initReactI18next } from 'react-i18next'
import LanguageDetector from 'i18next-browser-languagedetector'

// Import translations
import enUSCommon from './locales/en-US/common.json'
import enUSAuth from './locales/en-US/auth.json'
import enUSClient from './locales/en-US/client.json'
import enUSMerchant from './locales/en-US/merchant.json'
import enUSAdmin from './locales/en-US/admin.json'
import enUSHomepage from './locales/en-US/homepage.json'

import zhTWCommon from './locales/zh-TW/common.json'
import zhTWAuth from './locales/zh-TW/auth.json'
import zhTWClient from './locales/zh-TW/client.json'
import zhTWMerchant from './locales/zh-TW/merchant.json'
import zhTWAdmin from './locales/zh-TW/admin.json'
import zhTWHomepage from './locales/zh-TW/homepage.json'

const resources = {
  'en-US': {
    common: enUSCommon,
    auth: enUSAuth,
    client: enUSClient,
    merchant: enUSMerchant,
    admin: enUSAdmin,
    homepage: enUSHomepage,
  },
  'zh-TW': {
    common: zhTWCommon,
    auth: zhTWAuth,
    client: zhTWClient,
    merchant: zhTWMerchant,
    admin: zhTWAdmin,
    homepage: zhTWHomepage,
  },
}

i18n
  .use(LanguageDetector)
  .use(initReactI18next)
  .init({
    resources,
    fallbackLng: 'en-US',
    supportedLngs: ['en-US', 'zh-TW'],
    defaultNS: 'common',
    ns: ['common', 'auth', 'client', 'merchant', 'admin', 'homepage'],
    interpolation: {
      escapeValue: false,
    },
    detection: {
      order: ['localStorage', 'navigator'],
      caches: ['localStorage'],
      lookupLocalStorage: 'booking_language',
    },
  })

export default i18n
