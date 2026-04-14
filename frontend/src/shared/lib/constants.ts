export const AUTH_TOKEN_KEY = 'booking_auth_token'
export const LANGUAGE_KEY = 'booking_language'

export const SUPPORTED_LANGUAGES = ['en-US', 'zh-TW'] as const
export type SupportedLanguage = (typeof SUPPORTED_LANGUAGES)[number]

export const DEFAULT_LANGUAGE: SupportedLanguage = 'en-US'
