import { useTranslation } from 'react-i18next'
import { Globe } from 'lucide-react'
import { Button } from '@/components/ui/button'
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu'
import { LANGUAGE_KEY, SUPPORTED_LANGUAGES, type SupportedLanguage } from '@/shared/lib/constants'
import { cn } from '@/shared/lib/utils'

type LanguageSwitcherProps = {
  /** `toggle`: segmented EN/中文; `globe`: icon + dropdown (marketing nav). */
  variant?: 'toggle' | 'globe'
  className?: string
}

export function LanguageSwitcher({ variant = 'toggle', className }: LanguageSwitcherProps) {
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

  if (variant === 'globe') {
    return (
      <DropdownMenu>
        <DropdownMenuTrigger asChild>
          <Button
            type="button"
            variant="ghost"
            size="icon"
            className={cn('h-9 w-9 shrink-0 text-muted-foreground', className)}
            aria-label="Language"
          >
            <Globe className="size-4" />
          </Button>
        </DropdownMenuTrigger>
        <DropdownMenuContent align="end" className="min-w-[9rem]">
          {SUPPORTED_LANGUAGES.map((lang) => (
            <DropdownMenuItem
              key={lang}
              onClick={() => handleLanguageChange(lang)}
              className={cn(isActiveLanguage(lang) && 'bg-accent')}
            >
              {lang === 'en-US' ? 'English (US)' : '中文（台灣）'}
            </DropdownMenuItem>
          ))}
        </DropdownMenuContent>
      </DropdownMenu>
    )
  }

  return (
    <div className={cn('relative z-[60] inline-flex items-center gap-1 rounded-md border p-1', className)}>
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
