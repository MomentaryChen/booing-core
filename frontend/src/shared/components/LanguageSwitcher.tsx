import { useTranslation } from 'react-i18next'
import { Button } from '@/components/ui/button'
import { LANGUAGE_KEY, SUPPORTED_LANGUAGES, type SupportedLanguage } from '@/shared/lib/constants'

export function LanguageSwitcher() {
  const { i18n } = useTranslation('common')

  const normalize = (value: string | undefined | null) => (value ?? '').toLowerCase()

  const isActiveLanguage = (lang: SupportedLanguage) => {
    const active = normalize(i18n.resolvedLanguage ?? i18n.language)
    const target = normalize(lang)
    return active === target || active.startsWith(`${target.split('-')[0]}-`)
  }

  const handleLanguageChange = (lang: SupportedLanguage) => {
    void i18n.changeLanguage(lang)
    if (typeof window !== 'undefined') {
      window.localStorage.setItem(LANGUAGE_KEY, lang)
      document.documentElement.lang = lang
    }
  }

  return (
    <div className="relative z-[60] inline-flex items-center gap-1 rounded-md border p-1">
      {SUPPORTED_LANGUAGES.map((lang) => (
        <Button
          key={lang}
          type="button"
          variant={isActiveLanguage(lang) ? 'secondary' : 'ghost'}
          size="sm"
          className="h-7 px-2 text-xs"
          onClick={() => {
            handleLanguageChange(lang)
          }}
        >
          {lang === 'en-US' ? 'EN' : '中文'}
        </Button>
      ))}
    </div>
  )
}
